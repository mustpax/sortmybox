import { Query } from '@google-cloud/datastore/query';
import { DatastoreKey } from '@google-cloud/datastore/entity';
import joi = require('joi');
import datastore from './datastore';

export function toArraySchema(schema: joi.SchemaMap): joi.Schema {
  // Add optional id field to every schema
  let schemaWithId = joi.object(schema).keys({id: joi.optional()});
  return joi.array().items(schemaWithId);
}

export interface Entity {
  key: DatastoreKey;
  data: any;
}

export interface Model<K> {
  id?: K;
}

export interface Schema {
  schema: {
    [key: string]: joi.Schema
  };
  kind: string;
}

export interface ModelService<K, T extends Model<K>> extends Schema {
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

export abstract class AbstractModelService<K, T extends Model<K>> implements ModelService<K, T> {
  abstract schema: {
    [key: string]: joi.Schema
  };
  abstract kind: string;
  abstract makeNew(): T;
  abstract fromEntity(e: object): T;
  abstract toEntity(t: T): object;
  abstract keyFromId(id: K): DatastoreKey;
  abstract idFromKey(key: DatastoreKey): K|undefined;

  async findByIds(ids: K[]) {
    let keys = ids.map(id => this.keyFromId(id));
    // datastore.get() returns [results]
    let [entities] = await datastore.get(keys);
    return entities.map(e => this.fromEntity(e));
  }


  async query(q: Query) {
    let resp = await datastore.runQuery(q);
    let results = resp[0];
    return results.map(e => this.fromEntity(e));
  }

  all(): Query {
    return datastore.createQuery(this.kind);
  }

  async removeById(ids: (K|undefined)[]) {
    let keys: DatastoreKey[] = [];
    for (let id of ids) {
      if (id) {
        keys.push(this.keyFromId(id));
      }
    }
    await datastore.delete(keys);
  }

  // TODO
  async remove(tarr: T[]): Promise<void> {
    await this.removeById(tarr.map(t => t.id));
  }

  async save(tarr: T[]): Promise<K[]> {
    let error = this.validate(tarr);
    if (error) {
      throw error;
    }
    let entities = tarr.map(t => this.toEntity(t));
    let savedEntities = await datastore.save(entities);
    let mutationResults = savedEntities[0].mutationResults as any[];
    // mr.key is only set (i.e. non-empty) if datastore generated a key for
    // us. If we specify a key/id ahead of time, then it's empty.
    return mutationResults.map(mr => mr.key && mr.key.path[0].id);
  }


  validate(ts: T[]): joi.ValidationError {
    return joi.validate(ts, toArraySchema(this.schema)).error;
  }

}