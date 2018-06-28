import { FileMoveService as fms, datastore } from '../models';
import _ = require("underscore");
import moment = require('moment');

async function countDay(day: number) {
  let now = moment().utc();
  let start = now.clone().subtract(day, 'days');
  let end = start.clone().add(1, 'days');
  let query = datastore
    .createQuery(fms.kind)
    .select('__key__')
    .filter('when', '>=', start.toDate())
    .filter('when', '<', end.toDate())
    .limit(100);
  let [results]: any = await datastore.runQuery(query);
  return {
    total: results.length,
    users: _.uniq(results.map((result: any) => result[datastore.KEY].parent.id)).length,
  };
}

async function main() {
  for (let i = 1; i <= 30; i++) {
    console.log(i, await countDay(i));
  }
}

main().catch(console.error);
