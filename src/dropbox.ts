import { Dropbox } from 'dropbox';
import { User, Rule, RuleService as rs } from './models';
import _ = require('underscore');
import { endsWithCaseInsensitive } from './utils';

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

  async runRules(user: User, rules: Rule[]): Promise<MoveResult[]> {
    rules = _.sortBy(rules, 'rank');
    let files = await this.client.filesListFolder({
      path: (user.sortingFolder as string),
      limit: 100,
    });
    // TODO handle files.has_more
    // TODO log info
    let moves = [];
    for (let file of files.entries) {
      // Only move files, not folders
      if (file['.tag'] !== 'file') {
        continue;
      }
      for (let rule of rules) {
        if (rs.matches(rule, file.name)) {
          let to_path = [rule.dest, file.name].join('/');
          moves.push(this.client.filesMoveV2({
            from_path: (file.path_lower as string),
            to_path,
            autorename: true,
          }));
          // Once there's a matching rule, move to next file
          break;
        }
      }
    }
    // TODO handle conflicts
    let responses = await Promise.all(moves);
    return responses.map((resp, i) => {
      let fileName = files.entries[i].name as string;
      let fullDestPath = resp.metadata.path_display as string;
      let conflict = ! endsWithCaseInsensitive(fullDestPath, fileName);
      let ret: MoveResult = {
        fileName,
        fullDestPath,
        conflict
      };
      return ret;
    });
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
