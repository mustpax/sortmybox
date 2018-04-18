import express = require('express');
const app: express.Router = express.Router();
export default app;

import { asyncRoute } from '../utils';

app.get('/rules', asyncRoute(async function(req, res) {
  // TODO handle creating sortmybox dir
  res.render('rules', {
    user: req.user,
    title: 'Logged In - Organize your Dropbox',
    rules: [],
  });
}));
