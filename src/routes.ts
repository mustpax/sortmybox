import express = require('express');

import { asyncRoute } from './utils';
import { UserService, User } from './models';

const app: express.Router = express.Router();

import dropbox from './dropbox';

const REDIRECT_URI = 'http://localhost:3000/dropbox/cb';

app.use(asyncRoute(async function(req, res, next) {
  try {
    if (req.session.userId) {
      let [user] = await UserService.findByIds([req.session.userId]);
      if (user) {
        (req as any).user = user;
        (req as any).dbx = dropbox(user.dropboxV2Token);
      }
    }
  } catch (e) {
    console.log(`Error populating req.user: ${e}`);
  }
  next();
}));

app.get('/', asyncRoute(async function(req, res) {
  if (req.user) {
    res.redirect('/rules');
    return;
  }
  res.render('index', {
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
  let dbx = dropbox(token);
  let acct: DropboxTypes.users.FullAccount = await dbx.usersGetCurrentAccount(undefined);
  let user = await UserService.upsertDropboxAcct(token, acct);
  req.session.userId = user.id;
  res.redirect('/');
}));

app.use(asyncRoute(async function(req, res, next) {
  if (! req.user || ! req.dbx) {
    res.redirect('/');
    return;
  }
  next();
}));

app.get('/rules', asyncRoute(async function(req, res) {
  // TODO handle creating sortmybox dir
  res.render('rules', {
    user: req.user,
    title: 'Logged In - Organize your Dropbox',
    rules: [],
  });
}));

export default app;
