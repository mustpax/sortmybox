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

import {} from '../redis';
import crypto = require('crypto');

app.post('/dropbox/webhook', function(req, res, next) {
  let chunks: Buffer[] = [];
  req.on('data', function(chunk) {
    chunks.push(chunk as Buffer);
  });
  req.on('end', async function() {
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
    // We respond right away to keep Dropbox happy, we're not really done
    // procesing the request
    res.send('OK');

    const json = JSON.parse(body);
    console.log('Good sig!', json);
    let ids: string[] = json.list_folder.accounts;
    console.log('ids', ids);
    for (let id of ids) {
      let user = await UserService.findByDropboxId(id);
      if (user) {
        let dbx = dropbox(user.dropboxV2Token);
        let cursor = user.dropboxCursor;
        let res: any;
        if (cursor) {
          res = await dbx.client.filesListFolderContinue({
            cursor
          });
        } else {
          res = await dbx.client.filesListFolder({ path: user.sortingFolder as string });
        }
        user.dropboxCursor = res.cursor;
        await UserService.save([user]);
        let entries: any[] = res.entries.filter((entry: any) => entry['.tag'] === 'file');
        console.log(id, entries);
      }
    }
  });
});

export default app;
