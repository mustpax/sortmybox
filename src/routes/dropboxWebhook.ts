import express = require('express');
const app: express.Router = express.Router();
import dropbox from '../dropbox';
import { UserService } from '../models';

// These two routes have to come before bodyParser to be able
// to read the raw body and confirm the request signature
app.get('/dropbox/webhook', function(req, res) {
  res.set({
    'Content-Type': 'text/plain',
    'X-Content-Type-Options': 'nosniff',
  });
  res.send(req.query.challenge || 'OK');
});

import {
  shouldSortUser,
  startQueueProcessor,
  onUserReadyForSort,
} from '../redis';
import crypto = require('crypto');

import { reportError } from '../raven';

async function processUser(userDbxId: string) {
  let user = await UserService.findByDropboxId(userDbxId);
  if (user) {
    let dbx = dropbox(user.dropboxV2Token);
    try {
      if (user.periodicSort) {
        await dbx.runRulesAndUpdateUserAndFileMoves(user, true);
      } else {
        console.error(`Not sorting user, periodic sort disabled: ${user.id}`);
      }
    } catch (e) {
      if (dbx.isNotFoundError(e)) {
        console.log(`Sorting folder missing for user ${user.id}, disabling periodic sync`);
        user.periodicSort = false;
        await UserService.save([user]);
      } else {
        throw e;
      }
    }
  } else {
    console.error(`Attempting to sort user that is not in our Database: ${userDbxId}`);
  }
}


app.post('/dropbox/webhook', function(req, res, next) {
  let chunks: Buffer[] = [];
  req.on('data', function(chunk) {
    chunks.push(chunk as Buffer);
  });
  req.on('end', reportError(async function() {
    const body = Buffer.concat(chunks).toString('utf8');
    let hmac = crypto.createHmac('sha256', process.env.DROPBOX_SECRET as string);
    hmac.update(body);
    let expectedSig = hmac.digest('hex');
    let sig = req.header('X-Dropbox-Signature');
    if (expectedSig !== sig) {
      res.status(401).send('Bad signature');
      let err: any = new Error('Bad signature');
      err.status = 401;
      next(err);
      return;
    }
    // Now we know that the signature is good, let's process users
    // We respond right away to keep Dropbox happy, we're not really done
    // procesing the request
    res.send('OK');

    const json = JSON.parse(body);
    let ids: string[] = json.list_folder.accounts;
    for (let id of ids) {
      if (! await shouldSortUser(id)) {
        console.log(`Not processing users ${id}, throttled.`);
        return;
      }
      await processUser(id);
    }
  }));
});

onUserReadyForSort(reportError(async function(userId) {
  console.log(`Processing ${userId} from queue`);
  await processUser(userId);
}));

startQueueProcessor();

export default app;
