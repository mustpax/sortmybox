import express = require('express');
const app: express.Router = express.Router();
export default app;

import _ = require('underscore');
import { asyncRoute } from '../utils';
import { User, RuleService as rs } from '../models';

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

app.post('/rules', auth, asyncRoute(async function(req, res, next) {
  if (! _.isArray(req.body.rules)) {
    let err: any = new Error('Missing or invalid rules array');
    err.status = 400;
    next(err);
    return;
  }
  let rules = [];
  let i = 0;
  let ownerId = (req.user as User).id as string;
  for (let ruleJson of req.body.rules) {
    let rule = rs.makeNew(ownerId);
    rule.rank = i++;
    rule.pattern = ruleJson.pattern;
    rule.type = ruleJson.type;
    rule.dest = ruleJson.dest;
    rules.push(rule);
  }
  let errors = rs.validate(rules);
  if (errors) {
    let errorsJson: any[][] = _.range(rules.length).map(() => []);
    for (let error of errors.details) {
      let msg = error.message;
      errorsJson[(error.path[0] as any)].push({
        field: error.path[1],
        msg
      });
    }
    res.json(errorsJson);
  } else {
    let rulesToDelete = await rs.findByOwner(ownerId);
    await rs.removeById(rulesToDelete.map(rule => rule.id));
    await rs.save(rules);
  }
}));

app.get('/logout', auth, asyncRoute(async function(req, res) {
  req.session.userId = null;
  res.redirect('/');
}));
