import express = require('express');

import  asyncRoute from '../asyncRoute';
import { UserService } from '../models';

const app: express.Router = express.Router();

import dropbox from '../dropbox';

app.use(asyncRoute(async function(req, res, next) {
  try {
    if (req.session.userId) {
      let [user] = await UserService.findByIds([req.session.userId]);
      if (user) {
        req.user = user;
        req.dbx = dropbox(user.dropboxV2Token);
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

app.get('/_ah/health', asyncRoute(async function(req, res) {
  res.send('OK');
}));

app.get('/error', asyncRoute(async function(req, res) {
  let err = new Error('Test error') as any;
  err.foo = { 'obj': 'test' };
  throw err;
}));

app.get('/dropbox/login', asyncRoute(async function(req, res) {
  let redirectURL = dropbox().getRedirectUrl(req.get('host'));
  let url = (dropbox().client.getAuthenticationUrl as any)(redirectURL, null, 'code');
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
  let redirectURL = dropbox().getRedirectUrl(req.get('host'));
  let token = await (dropbox().client as any).getAccessTokenFromCode(redirectURL, code);
  let dbx = dropbox(token);
  let acct: DropboxTypes.users.FullAccount = await dbx.client.usersGetCurrentAccount(undefined);
  let user = await UserService.upsertDropboxAcct(token, acct);
  req.session.userId = user.id;
  res.redirect('/');
}));

app.get('/faq', asyncRoute(async function(req, res) {
  res.render('faq', {
    faqTab: 'active',
    user: req.user,
    title: 'FAQ',
  });
}));

app.get('/terms', asyncRoute(async function(req, res) {
  res.render('terms', {
    user: req.user,
    title: 'Terms of Service',
  });
}));

app.get('/privacy', asyncRoute(async function(req, res) {
  res.render('privacy', {
    user: req.user,
    title: 'Privacy Policy',
  });
}));

app.get('/press', asyncRoute(async function(req, res) {
  res.render('press', {
    user: req.user,
    title: 'Press Coverage',
  });
}));

import authorizedRoutes from './authorizedRoutes';
app.use(authorizedRoutes);

export default app;
