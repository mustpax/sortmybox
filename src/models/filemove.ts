import { Entity, Model, AbstractModelService, DatastoreUtil as dutil } from './base';
import { DatastoreKey } from '@google-cloud/datastore/entity';
import { UserService } from './user';

import datastore from './datastore';

import joi = require('joi');

export class FileMove implements Model<FileMoveKey> {
  id?: FileMoveKey;
  fromFile?: string;
  toDir?: string;
  when?: Date;
  hasCollision?: boolean;
  resolvedName?: string;
}

export class FileMoveKey {
  ownerId?: string;
  fileMoveId?: string;
}

export class FileMoveSchema extends AbstractModelService<FileMoveKey, FileMove> {
  schema = {
    fromFile: joi.string().required(),
    toDir: joi.string().allow('').required(),
    when: joi.date().required(),
    hasCollision: joi.boolean().required(),
    resolvedName: joi.string().allow(null)
  };
  kind = 'FileMove';

  makeNew(ownerId: string): FileMove {
    let id = new FileMoveKey();
    id.ownerId = ownerId;
    let ret: FileMove = {
      when: new Date(),
      id
    };
    return ret;
  }

  toEntity(r: FileMove): Entity {
    if ((! r.id) || (! r.id.ownerId)) {
      throw new Error('Cannot convert filemove to entity. Missing ownerId ' + r);
    }
    return super.toEntity(r);
  }

  keyFromId(id?: FileMoveKey) {
    if (!id || !id.ownerId) {
      throw new Error('Cannot generate key for FileMove with no owner id. ' + this);
    }
    let userKey = UserService.keyFromId(id.ownerId);
    let fmKey;
    if (id.fileMoveId) {
      fmKey = datastore.key([this.kind, datastore.int(id.fileMoveId)]);
    } else {
      fmKey = datastore.key([this.kind]);
    }
    return dutil.concat([userKey, fmKey]);
  }

  idFromKey(k: DatastoreKey): FileMoveKey|undefined {
    if (! k) {
      throw new Error('Cannot generate a FileMoveKey from null key');
    }
    if (! k.parent) {
      throw new Error('Cannot generate a FileMoveKey from a key with no parent ' + k);
    }
    let ret = new FileMoveKey();
    ret.ownerId = k.parent.id || k.parent.name;
    ret.fileMoveId = k.id;
    return ret;
  }

  async save(fileMoves: FileMove[]): Promise<(FileMoveKey|undefined)[]> {
    let error = this.validate(fileMoves);
    if (error) {
      throw error;
    }
    let entities = fileMoves.map(t => this.toEntity(t));
    let savedEntities = await datastore.save(entities);
    let mutationResults = savedEntities[0].mutationResults as any[];
    // mr.key is only set (i.e. non-empty) if datastore generated a key for
    // us. If we specify a key/id ahead of time, then it's empty.
    return mutationResults.map(mr => {
      if (! mr.key) {
        return undefined;
      }
      let rk = new FileMoveKey();
      rk.ownerId = mr.key.path[0].id || mr.key.path[0].name;
      rk.fileMoveId = mr.key.path[1].id || mr.key.path[1].name;
      return rk;
    });
  }

  async findByOwner(ownerId: string): Promise<FileMove[]> {
    // TODO turn into method that returns query
    let q = this.all()
      .hasAncestor(UserService.keyFromId(ownerId))
      .limit(10)
      .order('when', {descending: true});
    return await this.query(q);
  }
}

export let FileMoveService = new FileMoveSchema();
