"use strict";

let required: string[] = ['GOOGLE_CLOUD_PROJECT', 'GOOGLE_APPLICATION_CREDENTIALS'];

export function validate() {
  required.forEach(varName => {
    if (! process.env[varName]) {
      console.error('Environment variable missing', varName);
      process.exit(1);
    }
  });
}
