import express = require('express');
const app: express.Router = express.Router();
export default app;

import { asyncRoute } from '../utils';

const auth = asyncRoute(async function(req, res, next) {
  if (! req.user || ! req.dbx) {
    res.redirect('/');
    return;
  }
  next();
});

app.get('/rules', auth, asyncRoute(async function(req, res) {
  // TODO handle creating sortmybox dir
  res.render('rules', {
    user: req.user,
    title: 'Logged In - Organize your Dropbox',
    rules: [],
  });
}));

app.get('/logout', auth, asyncRoute(async function(req, res) {
  req.session.userId = null;
  res.redirect('/');
}));
