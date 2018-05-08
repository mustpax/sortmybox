"use strict";

import Promise = require("bluebird");
Promise.config({
  longStackTraces: true,
  warnings: true,
});
global.Promise = Promise;

export const DEV = process.env.NODE_ENV !== 'production';

import dotenv = require('dotenv');

if (process.env.GOOGLE_CLOUD_PROJECT === 'moosepax-1248') {
  console.log('Staging environment detected. Loading configs from .env.staging');
  dotenv.config({
    path: '.env.staging'
  });
} else if (process.env.GOOGLE_CLOUD_PROJECT === 'sortmybox-hrd') {
  console.log('Prod environment detected. Loading configs from .env.prod');
  dotenv.config({
    path: '.env.prod'
  });
}

const required = [
  'GOOGLE_CLOUD_PROJECT',
  'SECRET',
  'DROPBOX_KEY',
  'DROPBOX_SECRET',
  'RAVEN_DSN',
  'REDIS_HOST',
  'REDIS_PORT',
  'REDIS_PASSWORD',
];
const requiredDev = [
  'GOOGLE_APPLICATION_CREDENTIALS',
];

export function validate() {
  required.forEach(varName => {
    if (! process.env.hasOwnProperty(varName)) {
      console.error('Environment variable missing', varName);
      process.exit(1);
    }
  });

  if (DEV) {
    requiredDev.forEach(varName => {
      if (! process.env[varName]) {
        console.error('Environment DEV-ONLY variable missing', varName);
        process.exit(1);
      }
    });
  }
}
