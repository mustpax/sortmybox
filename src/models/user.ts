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
    name: joi.string().allow(null),
    nameLower: joi.string().allow(null),
    email: joi.string().allow(null),
    periodicSort: joi.boolean().required(),
    created: joi.date().required(),
    modified: joi.date().required(),
    lastSync: joi.date().allow(null),
    lastLogin: joi.date().required(),
    token: joi.string().allow(null),
    secret: joi.string().allow(null),
    fileMoves: joi.number().required(),
    sortingFolder: joi.string().required(),
    tokenExpiration: joi.date().allow(null),
    refreshToken: joi.string().allow(null),
    dropboxV2Token: joi.string().allow(null),
    dropboxV2Id: joi.string().allow(null),
    dropboxV2Migrated: joi.boolean().allow(null),
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
    ret.dropboxV2Migrated = true;
    return ret;
  }

  fromEntity(e: any) {
    this.logUnregisteredKeys(e);
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

  async findByDropboxId(dropboxId: string): Promise<User|undefined> {
    let q = this.all().filter('dropboxV2Id', '=', dropboxId).limit(1);
    let [result] = await this.query(q);
    return result;
  }

  async upsertDropboxAcct(token: string, account: DropboxTypes.users.FullAccount): Promise<User> {
    let dbxId = account.account_id;
    let q = this.all().filter('dropboxV2Id', '=', dbxId).limit(1);
    let [user] = await this.query(q);
    if (user) {
      user.lastLogin = new Date();
    } else {
      user = this.makeNew();
    }
    user.name = account.name.display_name;
    user.nameLower = user.name && user.name.toLowerCase();
    user.email = account.email;
    user.dropboxV2Id = dbxId;
    user.dropboxV2Token = token;
    let [id] = await this.save([user]);
    if (id) {
      user.id = id;
    }
    return user;
  }

  async save(users: User[]): Promise<(string|undefined)[]> {
    let now = new Date();
    return await super.save(users.map(user => {
      // If the user already has an id (i.e. exists in datastore)
      // update modified timestamp
      if (user.id) {
        user.modified = now;
      }
      return user;
    }));
  }
}

export let UserService = new UserSchema();
