import express = require('express');
export type RouteFn = (req: SMBRequest, res: express.Response, next: express.NextFunction) => Promise<any>;
import { User } from './models';
import { DropboxService } from './dropbox';

export interface SMBRequest extends express.Request {
  user: User;
  dbx: DropboxService;
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
