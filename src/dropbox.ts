import { Dropbox } from 'dropbox';
import { User, Rule, RuleService as rs } from './models';
import _ = require('underscore');

export class DropboxService {
  client: Dropbox;

  constructor(token?: string) {
    this.client = new Dropbox({clientId: process.env.DROPBOX_KEY, accessToken: token});
    (this.client as any).setClientSecret(process.env.DROPBOX_SECRET);
  }

  async runRules(user: User, rules: Rule[]) {
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
    return await Promise.all(moves);
  }
}

function dbxClient(token?: string): DropboxService {
  return new DropboxService(token);
}

export default dbxClient;
