"use strict";

var required = ['GOOGLE_CLOUD_PROJECT', 'GOOGLE_APPLICATION_CREDENTIALS'];
module.exports = {
  validate() {
    required.forEach(varName => {
      if (! process.env[varName]) {
        console.error('Environment variable missing', varName);
        process.exit(1);
      }
    });
  }
};
