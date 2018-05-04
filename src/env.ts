"use strict";

const required = [
  'GOOGLE_CLOUD_PROJECT',
  'SECRET',
  'DROPBOX_KEY',
  'DROPBOX_SECRET',
  'RAVEN_DSN',
  'REDIS_HOST',
  'REDIS_PASSWORD',
];
const requiredDev = [
  'GOOGLE_APPLICATION_CREDENTIALS',
];

export const DEV = process.env.NODE_ENV !== 'production';

export function validate() {
  required.forEach(varName => {
    if (! process.env[varName]) {
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
