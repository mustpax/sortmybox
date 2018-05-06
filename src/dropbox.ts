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

export class DropboxService {
  client: Dropbox;

  constructor(token?: string) {
    this.client = new Dropbox({clientId: process.env.DROPBOX_KEY, accessToken: token});
    (this.client as any).setClientSecret(process.env.DROPBOX_SECRET);
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
      files = await this.client.filesListFolder({
        path: sortingFolder,
      });
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
        if (rs.matches(rule, file.name)) {
          let destParts = (rule.dest as string).split('/');
          // We normalize paths by filtering empty splits
          // /a/b//c/ will map to ['', 'a', 'b', '', 'c'' ,''] and .filter(x => x)
          // will remove empty strings
          destParts = destParts.filter(x => x);
          destParts.push(file.name);
          let to_path = '/' + destParts.join('/');
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
    // TODO remove any when Dropbox fixes their type annotations
    let response: any = await this.client.filesMoveBatch({
      entries: moves,
      autorename: true,
    });

    if (response['.tag'] !== 'complete') {
      console.log('Job in progress', response);
      let jobId = response.async_job_id;
      while (response['.tag'] !== 'complete') {
        response = await this.client.filesMoveBatchCheck({
          async_job_id: jobId
        });
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
      if (e.error.error.path && e.error.error.path['.tag'] === 'not_found') {
        return false;
      }
      throw e;
    }
  }
}

function dbxClient(token?: string): DropboxService {
  return new DropboxService(token);
}

export default dbxClient;
