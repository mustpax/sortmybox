import express = require('express');
export type RouteFn = (req: SMBRequest, res: express.Response, next: express.NextFunction) => Promise<any>;
import { User } from './models';

export interface SMBRequest extends express.Request {
  user?: User;
  dbx?: DropboxTypes.Dropbox;
}

export let asyncRoute = (route: RouteFn): RouteFn => {
  return async (req: SMBRequest, res: express.Response, next: express.NextFunction) => {
    try {
      await route(req, res, next);
    } catch (e) {
      next(e);
    }
  };
};
