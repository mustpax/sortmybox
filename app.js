"use strict";

require('dotenv').config();
var express = require('express');
var path = require('path');

var app = express();

var hbs = require('express-handlebars')({
  defaultLayout: 'main',
  extname: '.hbs'
});
app.engine('hbs', hbs);
app.set('view engine', 'hbs');

var bodyParser = require('body-parser');

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(express.static(path.join(__dirname, 'public')));

var { datastore, Visit } = require('./models');

app.get('/', async function(req, res) {
  var visit = new Visit();
  await visit.save();
  var visits = await datastore.runQuery(Visit.all());
  res.render('index', {
    visits
  });
});

var port = process.env.PORT || 3000;
app.listen(port, function() {
  console.log('Express started, listening to port: ', port);
});
