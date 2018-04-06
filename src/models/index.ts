import { Query } from '@google-cloud/datastore/query';
import { DatastoreKey } from '@google-cloud/datastore/entity';
import joi = require('joi');

import datastore from './datastore';
export { datastore };

export interface Schema<K, T extends Model<K>> {
  schema: {
    [key: string]: joi.Schema
  };
  kind: string;
  makeNew(): T;
  findByIds(ids: K[]): Promise<T[]>;
  fromEntity(e: object): T;
  toEntity(t: T): object;
  query(q: Query): Promise<T[]>;
  keyFromId(id: K): DatastoreKey;
  idFromKey(key: DatastoreKey): K|undefined;
  all(): Query;
  removeById(ids: K[]): Promise<void>;
  remove(t: T[]): Promise<void>;
  save(t: T[]): Promise<K[]>;
  validate(ts: T[]): joi.ValidationError;
}

export interface Model<K> {
  id?: K;
}

export interface Entity {
  key: DatastoreKey;
  data: any;
}

export class Visit implements Model<string> {
  id?: string;
  created?: Date;
}

function addIdToSchema(schema: joi.SchemaMap): joi.Schema {
  // Add optional id field to every schema
  return joi.object(schema).keys({id: joi.optional()});
}

function toArraySchema(schema: joi.SchemaMap): joi.Schema {
  let schemaWithId = addIdToSchema(schema);
  return joi.array().items(schemaWithId);
}

export class VisitSchema implements Schema<string, Visit> {
  schema = {
    created: joi.date().required()
  };
  kind = 'visit';

  makeNew() {
    let ret = new Visit();
    ret.created = new Date();
    return ret;
  }

  async findByIds(ids: string[]) {
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

  keyFromId(id?: string) {
    if (id) {
      return datastore.key([this.kind, datastore.int(id)]);
    }
    return datastore.key([this.kind]);
  }

  idFromKey(k: DatastoreKey): string|undefined {
    if (! k || k.path.length < 2) {
      return undefined;
    }
    return String(k.path[1]);
  }

  all(): Query {
    return datastore.createQuery(this.kind);
  }

  async query(q: Query) {
    let resp = await datastore.runQuery(q);
    let results = resp[0];
    return results.map(e => this.fromEntity(e));
  }

  async removeById(ids: (string|undefined)[]) {
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
    let error = this.validate(visits);
    if (error) {
      throw error;
    }
    let entities = visits.map(visit => this.toEntity(visit));
    let savedEntities = await datastore.save(entities);
    let mutationResults = savedEntities[0].mutationResults as any[];
    // mr.key is only set (i.e. non-empty) if datastore generated a key for
    // us. If we specify a key/id ahead of time, then it's empty.
    return mutationResults.map(mr => mr.key && mr.key.path[0].id);
  }

  validate(visits: Visit[]): joi.ValidationError {
    return joi.validate(visits, toArraySchema(this.schema)).error;
  }
}

export let VisitService = new VisitSchema();
