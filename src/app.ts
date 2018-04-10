"use strict";

require('dotenv').config();

import fetch = require('node-fetch');
(global as any).fetch = fetch;

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

import cookieSession = require('cookie-session');
app.use(cookieSession({
  name: 'session',
  keys: [process.env.SECRET as string]
}));

import bodyParser = require('body-parser');
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));

app.use(express.static('public'));

import routes from './routes';
app.use(routes);

app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.log('Error', err);
  res.status(err.status || 500).json({
    error: err.message
  });
});

let port = process.env.PORT || 3000;
app.listen(port, function() {
  console.log('Express started, listening to port: ', port);
});
