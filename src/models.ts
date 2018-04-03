"use strict";

import Datastore = require('@google-cloud/datastore');
import { Query } from '@google-cloud/datastore/query';
import { DatastoreKey, DatastoreInt, PathElement } from '@google-cloud/datastore/entity';
import joi = require('joi');

// import { CommitResult } from '@google-cloud/datastore/request';

export const datastore = new Datastore({});

export type ModelId = PathElement;
export type Int = DatastoreInt;

export interface Schema<T extends Model> {
  schema: {
    [key: string]: joi.Schema
  };
  kind: string;
  makeNew(): T;
  findByIds(ids: ModelId[]): Promise<T[]>;
  fromEntity(e: object): T;
  toEntity(t: T): object;
  query(q: Query): Promise<T[]>;
  keyFromId(id: ModelId): DatastoreKey;
  idFromKey(key: DatastoreKey): ModelId|undefined;
  all(): Query;
  removeById(ids: ModelId[]): Promise<void>;
  remove(t: T[]): Promise<void>;
  save(t: T[]): Promise<ModelId[]>;
  validate?(t: T): joi.ValidationError;
  validate?(ts: T[]): joi.ValidationError;
}

export interface Model {
  id?: ModelId;
}

export interface Entity {
  key: DatastoreKey;
  data: any;
}

export class Visit implements Model {
  id?: Int;
  created?: Date;
}

function toArraySchema(schema: joi.SchemaMap): joi.Schema {
  // Add optional id field to every schema
  let schemaWithId = joi.object(schema).keys({id: joi.optional()});
  return joi.array().items(schemaWithId);
}

export class VisitSchema implements Schema<Visit> {
  schema = {
    created: joi.date().required()
  };
  kind = 'visit';

  makeNew() {
    let ret = new Visit();
    ret.created = new Date();
    return ret;
  }

  async findByIds(ids: ModelId[]) {
    let keys = ids.map(id => this.keyFromId(id));
    // datastore.get() returns [results]
    let [entities] = await datastore.get(keys);
    return entities.map(e => this.fromEntity(e));
  }

  toEntity(v: Visit): Entity {
    let data = {} as any;
    for (let f of Object.keys(this.schema)) {
      data[f] = (v as any)[f];
    }

    return {
      key: this.keyFromId(v.id),
      data
    };
  }

  fromEntity(e: any) {
    let ret = {} as any;
    ret.id = this.idFromKey(e[datastore.KEY]);
    for (let f of Object.keys(this.schema)) {
      ret[f] = e[f];
    }
    return ret as Visit;
  }

  keyFromId(id?: ModelId) {
    if (id) {
      if (typeof id === 'string') {
        id = datastore.int(id);
      }
      return datastore.key([this.kind, id]);
    }
    return datastore.key([this.kind]);
  }

  idFromKey(k: DatastoreKey): ModelId|undefined {
    if (! k || k.path.length < 2) {
      return undefined;
    }
    return k.path[1];
  }

  all(): Query {
    return datastore.createQuery(this.kind);
  }

  async query(q: Query) {
    let resp = await datastore.runQuery(q);
    let results = resp[0];
    return results.map(e => this.fromEntity(e));
  }

  async removeById(ids: (ModelId|undefined)[]) {
    let keys: DatastoreKey[] = [];
    for (let id of ids) {
      if (id) {
        keys.push(datastore.key([this.kind, parseInt(id as string)]));
      }
    }
    await datastore.delete(keys);
  }

  async remove(visits: Visit[]) {
    await this.removeById(visits.map(visit => visit.id));
  }

  async save(visits: Visit[]) {
    let schema = toArraySchema(this.schema);
    joi.assert(visits, schema);
    let entities = visits.map(visit => this.toEntity(visit));
    let savedEntities = await datastore.save(entities);
    let mutationResults = savedEntities[0].mutationResults as any[];
    // mr.key is only set (i.e. non-empty) if datastore generated a key for
    // us. If we specify a key/id ahead of time, then it's empty.
    return mutationResults.map(mr => mr.key && mr.key.path[0].id);
  }
}

export let VisitService = new VisitSchema();
