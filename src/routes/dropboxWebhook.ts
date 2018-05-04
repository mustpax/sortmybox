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

import crypto = require('crypto');
app.post('/dropbox/webhook', function(req, res) {
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
      return;
    }

    const json = JSON.parse(body);
    console.log('Good sig!', json);
    let ids: string[] = json.list_folder.accounts;
    console.log('ids', ids);
    let user = await UserService.findByDropboxId(ids[0]);
    if (user) {
      let dbx = dropbox(user.dropboxV2Token);
      let res = await dbx.client.filesListFolder({ path: user.sortingFolder as string });
      console.log(res);
    }
    res.send('OK');
  });
});

export default app;
