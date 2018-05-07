"use strict";

import { validate, DEV } from './env';
validate();

import express = require('express');
import moment = require('moment');

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
    },
    ruleLabel(ruleType?: string) {
      const labels: any = {
        NAME_CONTAINS: 'Name contains',
        EXT_EQ: 'Extension equals',
        GLOB: 'Name pattern'
      };
      if (ruleType && labels[ruleType]) {
        return labels[ruleType];
      }
      return 'Name contains';
    },
    formatDateSince(date: any) {
      return moment(date).fromNow();
    },
    formatDate(date: any) {
      return moment(date).format('MMMM D, YYYY');
    },
    formatDateTime(date: any) {
      return moment(date).format('MMMM D YYYY, h:mm:ss a');
    }
  }
});
app.engine('hbs', hbs);
app.set('view engine', 'hbs');

import raven = require('raven');
raven.config(process.env.RAVEN_DSN).install();
app.use(raven.requestHandler());

app.use(express.static('public'));
app.use(require('morgan')('short', {
  skip(req: any, res: any) {
    // skip
    return req.url === '/_ah/health';
  }
}));

import cookieSession = require('cookie-session');
app.use(cookieSession({
  name: 'session',
  keys: [process.env.SECRET as string]
}));

import dropboxWebhookRoutes from './routes/dropboxWebhook';
app.use(dropboxWebhookRoutes);

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


import routes from './routes';
app.use(routes);

app.use(raven.errorHandler());
app.use((req: express.Request, res: express.Response, next: express.NextFunction) => {
  let error: any = new Error('Not found');
  error.status = 404;
  next(error);
});

app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  if (err.status !== 404) {
    console.log('Error', JSON.stringify(err, null, 2));
  }
  res.status(err.status || 500);

  if (req.accepts('html')) {
    let template = 'generic';
    if (err.status === 404) {
      template = '404';
    }
    res.render('errors/' + template, {
      dev: DEV,
      error: err,
    });
    return;
  }

  res.json({
    error: err.message
  });
});

let port = process.env.PORT || 3000;
app.listen(port, function() {
  console.log('Express started, listening to port: ', port);
});
