"use strict";

require('dotenv').config();

import fetch = require('node-fetch');
(global as any).fetch = fetch;

import { validate } from './env';
validate();

import express = require('express');

let app = express();
app.enable('trust proxy');

app.locals.prod = (process.env.NODE_ENV === 'production');

// TODO export config values to the templates
// email

import jsesc = require("jsesc");
let hbs = require('express-handlebars')({
  defaultLayout: 'main',
  extname: '.hbs',
  helpers: {
    static(path: string) {
      return path;
    },
    escapeJSString(str: string) {
      if (! str) {
        return null;
      }
      return jsesc(str, {
        escapeEverything: true, // escape everything to \xFF hex sequences so we don't have to worry about script tags and whatnot
        wrap: true // wrap output with single quotes
      });
    },
    dateToSeconds(date?: Date) {
      if (! date) {
        return null;
      }
      return Math.round(date.getTime() / 1e3);
    }
  }
});
app.engine('hbs', hbs);
app.set('view engine', 'hbs');

import raven = require('raven');
raven.config(process.env.RAVEN_DSN).install();
app.use(raven.requestHandler());

app.use(require('morgan')('tiny'));

import cookieSession = require('cookie-session');
app.use(cookieSession({
  name: 'session',
  keys: [process.env.SECRET as string]
}));

import bodyParser = require('body-parser');
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));

import csrf = require('csurf');
app.use(csrf());
app.use((req: any, res: express.Response, next: express.NextFunction) => {
  // Make sure the template variable csrfToken is available to all templates
  res.locals.csrfToken = req.csrfToken;
  next();
});

app.use(express.static('public'));

import routes from './routes';
app.use(routes);

app.use(raven.errorHandler());
app.use((req: express.Request, res: express.Response, next: express.NextFunction) => {
  let error: any = new Error('Not found');
  error.status = 404;
  next(error);
});

app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.log('Error', err);
  // TODO display error page
  res.status(err.status || 500).json({
    error: err.message
  });
});

let port = process.env.PORT || 3000;
app.listen(port, function() {
  console.log('Express started, listening to port: ', port);
});
