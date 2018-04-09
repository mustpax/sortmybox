import express = require('express');
export type RouteFn = (req: express.Request, res: express.Response, next: express.NextFunction) => Promise<any>;

export let asyncRoute = (route: RouteFn): RouteFn => {
  return async (req: express.Request, res: express.Response, next: express.NextFunction) => {
    try {
      await route(req, res, next);
    } catch (e) {
      next(e);
    }
  };
};
