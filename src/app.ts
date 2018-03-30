"use strict";

import { validate } from './env';
validate();

import * as express from 'express';

let app = express();
app.enable('trust proxy');

let hbs = require('express-handlebars')({
  defaultLayout: 'main',
  extname: '.hbs'
});
app.engine('hbs', hbs);
app.set('view engine', 'hbs');

let bodyParser = require('body-parser');

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(express.static('public'));

let { datastore, Visit } = require('./models');

app.get('/', async function(_req, res) {
  try {
    let visit = Visit.create();
    await visit.save();
    let visits = await datastore.runQuery(Visit.all());
    res.render('index', {
      visits: visits[0].map(Visit.fromEntity)
    });
  } catch (e) {
    console.log(e);
    res.status(500).json(e);
  }
});

app.get('/delete', async function(_req, res) {
  let results = await datastore.runQuery(Visit.all().select(['__key__']));
  let keys = results[0].map((result: any) => result[datastore.KEY]);
  let [del] = await datastore.delete(keys);
  res.json({
    deleted: del
  });
});

app.post('/delete/:id', async function(req, res) {
  if (! req.params.id || isNaN(req.params.id)) {
    res.status(400).send('Missing id parameter');
    return;
  }
  let id = parseInt(req.params.id);
  await Visit.deleteById(id);
  res.redirect('/');
});

let port = process.env.PORT || 3000;
app.listen(port, function() {
  console.log('Express started, listening to port: ', port);
});
