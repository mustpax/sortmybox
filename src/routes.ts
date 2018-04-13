import express = require('express');

import { asyncRoute } from './utils';

const app: express.Router = express.Router();

import dropbox from './dropbox';

const REDIRECT_URI = 'http://localhost:3000/dropbox/cb';

app.get('/', asyncRoute(async function(req, res) {
  let dbx: any = dropbox(req.session.token);
  let account;
  if (req.session.token) {
    account = await dbx.usersGetCurrentAccount();
  }
  // TODO remove this
  res.render('index', {
    account,
    title: 'Organize your Dropbox',
  });
}));

app.get('/dropbox/login', asyncRoute(async function(req, res) {
  let url = (dropbox().getAuthenticationUrl as any)(REDIRECT_URI, null, 'code');
  res.redirect(url);
}));

app.get('/dropbox/cb', asyncRoute(async function(req, res, next) {
  let { code } = req.query;
  if (! code) {
    let err: any = new Error('Missing code parameter');
    err.status = 400;
    next(err);
    return;
  }
  let token = await (dropbox() as any).getAccessTokenFromCode(REDIRECT_URI, code);
  req.session.token = token;
  res.redirect('/');
}));

export default app;
