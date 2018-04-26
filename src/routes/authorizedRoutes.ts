import express = require('express');
import moment = require('moment');
import _ = require('underscore');

import asyncRoute from '../asyncRoute';
import { User, RuleService as rs, FileMoveService as fms } from '../models';

const app: express.Router = express.Router();
export default app;

const auth = asyncRoute(async function(req, res, next) {
  if (! req.user || ! req.dbx) {
    res.redirect('/');
    return;
  }
  next();
});

app.get('/rules', auth, asyncRoute(async function(req, res) {
  let rules = await rs.findByOwner((req.user as User).id as string);
  res.render('rules', {
    user: req.user,
    title: 'Logged In - Organize your Dropbox',
    rules,
  });
}));

app.post('/rules', auth, asyncRoute(async function(req, res, next) {
  if (! _.isArray(req.body.rules)) {
    let err: any = new Error('Missing or invalid rules array');
    err.status = 400;
    next(err);
    return;
  }
  if (req.body.rules.length > rs.MAX_RULES) {
    let err: any = new Error('Too many rules');
    err.status = 400;
    next(err);
    return;
  }

  let ownerId = (req.user as User).id as string;

  // Create rule models from request
  let rules = req.body.rules.map((ruleJson: any, i: number) => {
    let rule = rs.makeNew(ownerId);
    rule.rank = i++;
    rule.pattern = ruleJson.pattern;
    rule.type = ruleJson.type;
    rule.dest = ruleJson.dest;
    return rule;
  });

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
    let moveResult = await req.dbx.runRules(req.user, rules);
    let now = new Date();
    let fileMoves = moveResult.map(mv => {
      let ret = fms.makeNew(req.user.id as string);
      ret.fromFile = mv.fileName;
      ret.hasCollision = mv.conflict;
      let destParts = mv.fullDestPath.split('/');
      let destFileName = destParts.pop();
      ret.toDir = destParts.join('/');
      ret.resolvedName = ret.hasCollision ? destFileName : undefined;
      ret.when = now;
      return ret;
    });
    await fms.save(fileMoves);
    res.json([]);
  }
}));


app.get('/activity', auth, asyncRoute(async function(req, res) {
  let fileMoves = await fms.findByOwner(req.user.id as string);
  res.json(fileMoves.map(fm => {
    (fm as any).when = moment(fm.when).fromNow();
    return fm;
  }));
}));

app.get('/logout', auth, asyncRoute(async function(req, res) {
  req.session.userId = null;
  res.redirect('/');
}));
