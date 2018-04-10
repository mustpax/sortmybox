"use strict";

const required = ['GOOGLE_CLOUD_PROJECT', 'SECRET', 'DROPBOX_KEY', 'DROPBOX_SECRET'];
const requiredDev = ['GOOGLE_APPLICATION_CREDENTIALS'];

export function validate() {
  required.forEach(varName => {
    if (! process.env[varName]) {
      console.error('Environment variable missing', varName);
      process.exit(1);
    }
  });

  if (process.env.NODE_ENV !== 'production') {
    requiredDev.forEach(varName => {
      if (! process.env[varName]) {
        console.error('Environment DEV-ONLY variable missing', varName);
        process.exit(1);
      }
    });
  }
}
