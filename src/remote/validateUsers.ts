// Read all users from the datastore and verify that they pass our new
// validation

import { UserService as us } from '../models';

async function main() {
  let qr = us.queryIter(us.all());
  let count = 0;
  while (await qr.hasNext()) {
    let user = await qr.next();
    if (user) {
      let err = us.validate([user]);
      if (err) {
        console.log('Error validating', err.details.map(e => e.message), user);
        return;
      }
    }
    count++;
    if ((count % 100) === 0) {
      process.stdout.write('.');
    }
  }
}

main().catch(console.error);
