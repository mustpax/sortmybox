import raven = require('raven');
raven.config(process.env.RAVEN_DSN).install();

export function reportError(fn: ((...args: any[]) => Promise<any>)): any {
  return function(...args: any[]) {
    return fn(...args).catch(function(error) {
      raven.captureException(error);
    });
  };
}

export { raven };
