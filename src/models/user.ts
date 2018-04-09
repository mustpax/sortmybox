import { Model, AbstractModelService } from './base';
import { DatastoreKey } from '@google-cloud/datastore/entity';

import datastore from './datastore';

import joi = require('joi');

export class User implements Model<string> {
  id?: string;
  name?: string;
  nameLower?: string;
  email?: string;
  periodicSort?: boolean;
  created?: Date;
  modified?: Date;
  lastSync?: Date;
  lastLogin?: Date;
  token?: string;
  secret?: string;
  fileMoves?: number;
  sortingFolder?: string;
  tokenExpiration?: Date;
  refreshToken?: string;
  dropboxV2Token?: string;
  dropboxV2Id?: string;
  dropboxV2Migrated?: boolean;
  accountType?: string;
}

export class UserSchema extends AbstractModelService<string, User> {
  schema = {
    name: joi.string(),
    nameLower: joi.string(),
    email: joi.string(),
    periodicSort: joi.boolean().required(),
    created: joi.date().required(),
    modified: joi.date().required(),
    lastSync: joi.date(),
    lastLogin: joi.date().required(),
    token: joi.string(),
    secret: joi.string(),
    fileMoves: joi.number().required(),
    sortingFolder: joi.string().required(),
    tokenExpiration: joi.date(),
    refreshToken: joi.string(),
    dropboxV2Token: joi.string(),
    dropboxV2Id: joi.string(),
    dropboxV2Migrated: joi.boolean(),
    accountType: joi.string().required().only('DROPBOX', 'BOX'),
  };
  kind = 'User';

  makeNew() {
    let ret = new User();
    ret.modified = ret.created = ret.lastLogin = new Date();
    ret.periodicSort = true;
    ret.fileMoves = 0;
    ret.accountType = 'DROPBOX';
    ret.sortingFolder = '/SortMyBox';
    return ret;
  }

  fromEntity(e: any) {
    let ret = {} as any;
    ret.id = this.idFromKey(e[datastore.KEY]);
    for (let f of Object.keys(this.schema)) {
      ret[f] = e[f];
    }
    if ((! ret.nameLower) && ret.name) {
      ret.nameLower = ret.name.toLowerCase();
    }
    ret.fileMoves = ret.fileMoves || 0;
    ret.sortingFolder = ret.sortingFolder || '/Sortbox';
    ret.dropboxV2Migrated = ret.dropboxV2Migrated || false;
    ret.accountType = ret.accountType || 'DROPBOX';
    return ret as User;
  }

  keyFromId(id?: string) {
    if (id) {
      if (! isNaN(id as any)) {
        return datastore.key([this.kind, datastore.int(id)]);
      }
      return datastore.key([this.kind, id]);
    }
    return datastore.key([this.kind]);
  }

  idFromKey(k: DatastoreKey): string|undefined {
    if (! k || k.path.length < 2) {
      return undefined;
    }
    return String(k.path[1]);
  }
}

export let UserService = new UserSchema();
