import { Dropbox, files } from 'dropbox';
import {
  Rule, RuleService as rs,
  User, UserService as us,
  FileMoveService as fms,
} from './models';
import _ = require('underscore');
import {
  endsWithCaseInsensitive
} from './utils';

import { DEV } from './env';

// Dropbox SDK relies on fetch, so we add it to global environment
import fetch = require('node-fetch');
(global as any).fetch = fetch;

export interface MoveResults {
  cursor: string;
  results: MoveResult[];
}

export interface MoveResult {
  fileName: string;
  fullDestPath: string;
  conflict: boolean;
}

function sleep(waitMs: number): Promise<void> {
  return new Promise(function(resolve) {
    setTimeout(resolve, waitMs);
  });
}

function normalizePath(path: string, fileName?: string): string {
  let parts = path.toLowerCase().split('/');
  // We normalize paths by filtering empty splits
  // /a/b//c/ will map to ['', 'a', 'b', '', 'c'' ,''] and .filter(x => x)
  // will remove empty strings
  parts = parts.filter(x => x);
  if (fileName) {
    parts.push(fileName);
  }
  return '/' + parts.join('/');
}

export class DropboxService {
  client: Dropbox;

  constructor(token?: string) {
    this.client = new Dropbox({clientId: process.env.DROPBOX_KEY, accessToken: token});
    (this.client as any).setClientSecret(process.env.DROPBOX_SECRET);
    this.wrapErrors();
  }

  wrapErrors() {
    let client: any = this.client;
    for (let key of Object.keys(this.client)) {
      let val: any = client[key];
      if (val instanceof Function) {
        client[key] = async function(...args: any[]) {
          try {
            return await val.apply(client, args);
          } catch (e) {
            if (e instanceof Error) {
              throw e;
            }
            let message = e && e.error && e.error.error_summary;
            if (!message && ((typeof e.error) === 'string')) {
              message = e.error;
            }
            let newErr: any = new Error(`[${key}] ${message}`);
            Object.assign(newErr, e);
            newErr.dropboxStatus = newErr.status;
            newErr.status = 500;
            newErr.arguments = args;
            throw newErr;
          }
        };
      }
    }
  }

  async listFolder(path: string): Promise<DropboxTypes.files.ListFolderResult> {
    // Dropbox expects root folder to be represented as empty string
    if (path === '/') {
      path = '';
    }
    return await this.client.filesListFolder({
      path
    });
  }

  /**
   * Run sorting rules for given user
   */
  async runRules(sortingFolder: string, rules: Rule[], cursor?: string): Promise<MoveResults> {
    rules = _.sortBy(rules, 'rank');
    let files: DropboxTypes.files.ListFolderResult;
    // TODO limit fetch size?
    if (cursor) {
      files = await this.client.filesListFolderContinue({
        cursor,
      });
    } else {
      files = await this.listFolder(sortingFolder);
    }
    // TODO handle files.has_more
    // TODO log info
    let moves = [];
    let matchedFiles: files.MetadataReference[] = [];
    for (let file of files.entries) {
      // Only move files, not folders
      if (file['.tag'] !== 'file') {
        continue;
      }
      for (let rule of rules) {
        if (normalizePath(rule.dest as string) === normalizePath(sortingFolder)) {
          // Do not permit moving files to sorting folder
          console.log('Skipping rule, destination is sorting folder', rule);
          continue;
        }
        if (rs.matches(rule, file.name)) {
          let to_path = normalizePath(rule.dest as string, file.name);
          matchedFiles.push(file);
          moves.push({
            from_path: (file.path_lower as string),
            to_path,
          });
          // Once there's a matching rule, move to next file
          break;
        }
      }
    }
    if (moves.length === 0) {
      console.log('No matching files. Skipping moves');
      return {
        results: [],
        cursor: files.cursor,
      };
    }

    console.log(`Moving ${moves.length} files.`);
    // TODO if there's only a single file to move, do not use batch
    // TODO remove any when Dropbox fixes their type annotations
    let response: any = await this.client.filesMoveBatch({
      entries: moves,
      autorename: true,
    });

    if (response['.tag'] !== 'complete') {
      let jobId = response.async_job_id;
      while (response['.tag'] !== 'complete') {
        console.log('Job in progress', jobId);
        response = await this.client.filesMoveBatchCheck({
          async_job_id: jobId
        });
        await sleep(500);
      }
    }

    console.log(`Moved ${moves.length} files, creating FileMoves`);
    return {
      cursor: files.cursor,
      results: response.entries.map((resp: any, i: number) => {
          let fileName = matchedFiles[i].name as string;
          let fullDestPath = resp.metadata.path_display as string;
          let conflict = ! endsWithCaseInsensitive(fullDestPath, fileName);
          return {
            fileName,
            fullDestPath,
            conflict,
          };
        }),
    };
  }

  /**
   * Run rules for given user and update datastore
   */
  async runRulesAndUpdateUserAndFileMoves(user: User, useCursor: boolean, rules?: Rule[]) {
    if (! rules) {
      rules = await rs.findByOwner(user.id as string);
    }
    console.log(`Running rules for user ${user.id} useCursor: ${useCursor} rules: ${rules.length}`);
    let moveResults = await this.runRules(
      user.sortingFolder as string,
      rules,
      useCursor ? user.dropboxCursor : undefined
    );
    let now = new Date();
    let fileMoves = moveResults.results.map(mv => {
      let ret = fms.makeNew(user.id as string);
      ret.fromFile = mv.fileName;
      ret.hasCollision = mv.conflict;
      let destParts = mv.fullDestPath.split('/');
      let destFileName = destParts.pop();
      ret.toDir = destParts.join('/');
      ret.resolvedName = ret.hasCollision ? destFileName : undefined;
      ret.when = now;
      return ret;
    });
    console.log(`Performed ${fileMoves.length} moves, saving FileMoves`);
    await fms.save(fileMoves);
    user.fileMoves = (user.fileMoves || 0) + fileMoves.length;
    user.lastSync = new Date();
    user.dropboxCursor = moveResults.cursor;
    await us.save([user]);
  }

  async exists(path: string): Promise<boolean> {
    try {
      await this.client.filesGetMetadata({ path });
      return true;
    } catch (e) {
      // Dropbox API has no explitic way of checking if a file exists or not
      // so we have to get metadata and expect a specific type of error
      // so we can infer that the file is missing
      if (this.isNotFoundError(e)) {
        return false;
      }
      throw e;
    }
  }

  isNotFoundError(e: any): boolean {
    // Dropbox nests these path errors deeply, so we unnest them then check
    // the innermost one
    let innerMostError = e;
    while (innerMostError.error) {
      innerMostError = innerMostError.error;
    }
    return (
      innerMostError &&
      innerMostError.path &&
      innerMostError.path['.tag'] === 'not_found'
    );
  }

  getRedirectUrl(host?: string) {
    let protocol = DEV ? 'http' : 'https';
    return `${protocol}://${host}/dropbox/cb`;
  }
}

function dbxClient(token?: string): DropboxService {
  return new DropboxService(token);
}

export default dbxClient;
