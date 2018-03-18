"use strict";

var env = require('./env');
env.validate();

var express = require('express');
var path = require('path');

var app = express();
app.enable('trust proxy');

var hbs = require('express-handlebars')({
  defaultLayout: 'main',
  extname: '.hbs'
});
app.engine('hbs', hbs);
app.set('view engine', 'hbs');

var bodyParser = require('body-parser');

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(express.static('public'));

var { datastore, Visit } = require('./models');

app.get('/', async function(req, res) {
  try {
    var visit = Visit.create();
    await visit.save();
    var visits = await datastore.runQuery(Visit.all());
    res.render('index', {
      visits: visits[0].map(Visit.fromEntity)
    });
  } catch (e) {
    console.log(e);
    res.status(500).json(e);
  }
});

app.get('/delete', async function(req, res) {
  var results = await datastore.runQuery(Visit.all().select(['__key__']));
  var keys = results[0].map(result => result[datastore.KEY]);
  var [del] = await datastore.delete(keys);
  res.json({
    deleted: del
  });
});

app.post('/delete/:id', async function(req, res) {
  if (! req.params.id || isNaN(req.params.id)) {
    res.status(400).send('Missing id parameter');
    return;
  }
  var id = parseInt(req.params.id);
  await Visit.deleteById(id);
  res.redirect('/');
});

var port = process.env.PORT || 3000;
app.listen(port, function() {
  console.log('Express started, listening to port: ', port);
});
