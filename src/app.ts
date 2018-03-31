"use strict";

import { validate } from './env';
validate();

import express = require('express');

let app = express();
app.enable('trust proxy');

let hbs = require('express-handlebars')({
  defaultLayout: 'main',
  extname: '.hbs'
});
app.engine('hbs', hbs);
app.set('view engine', 'hbs');


app.use(require('morgan')('tiny'));

import bodyParser = require('body-parser');
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));

app.use(express.static('public'));

import { VisitService as VS } from './models';

type RouteFn = (req: express.Request, res: express.Response, next: express.NextFunction) => Promise<any>;

let asyncRoute = (route: RouteFn): RouteFn => {
  return async (req: express.Request, res: express.Response, next: express.NextFunction) => {
    try {
      await route(req, res, next);
    } catch (e) {
      next(e);
    }
  };
};

app.get('/', asyncRoute(async function(_req, res) {
  let visit = VS.makeNew();
  await VS.save([visit]);
  let visits = await VS.query(VS.all());
  res.render('index', {
    visits: visits
  });
}));

// app.get('/delete', async function(_req, res) {
//   let visits = await VS.query(VS.all());
//   let keys = visits.map(visit => VS.toKey(visit));
//   let [del] = await datastore.delete(keys);
//   res.json({
//     deleted: del
//   });
// });

app.post('/delete/:id', asyncRoute(async function(req, res) {
  if (! req.params.id || isNaN(req.params.id)) {
    res.status(400).send('Missing id parameter');
    return;
  }
  let id = parseInt(req.params.id);
  await VS.removeById([id]);
  res.redirect('/');
}));

app.use((err: any, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  console.log('Error', err);
  res.status(err.status || 500).json({
    error: err.message
  });
});

let port = process.env.PORT || 3000;
app.listen(port, function() {
  console.log('Express started, listening to port: ', port);
});
