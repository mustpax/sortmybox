import express = require('express');
const app: express.Router = express.Router();

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
  req.on('end', function() {
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
    res.send('OK');
  });
});

export default app;
