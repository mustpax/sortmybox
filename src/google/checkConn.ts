import { UserService as us } from '../models';

async function main() {
  let q = us.all().limit(1);
  let [result] = await us.query(q);
  console.log('Success, got:', result);
}

main().catch(console.error);
