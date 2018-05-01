import { Query, QueryInfo } from '@google-cloud/datastore/query';
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

export interface QueryResult<T> {
  results: T[];
  info: QueryInfo;
}

export interface AsyncIterator<T> {
  next(): Promise<T|undefined>;
  hasNext(): Promise<boolean>;
}

export interface ModelService<K, T extends Model<K>> extends Schema {
  findByIds(ids: K[]): Promise<T[]>;
  fromEntity(e: object): T;
  toEntity(t: T): object;
  query(q: Query): Promise<T[]>;
  queryIter(q: Query): AsyncIterator<T>;
  keyFromId(id: K): DatastoreKey;
  idFromKey(key: DatastoreKey): K|undefined;
  all(): Query;
  removeById(ids: K[]): Promise<void>;
  remove(t: T[]): Promise<void>;
  save(t: T[]): Promise<(K|undefined)[]>;
  validate(ts: T[]): joi.ValidationError;
}

export abstract class AbstractModelService<K, T extends Model<K>> implements ModelService<K, T> {
  abstract schema: {
    [key: string]: joi.Schema
  };
  abstract kind: string;
  abstract keyFromId(id?: K): DatastoreKey;
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

  queryIter(q: Query): AsyncIterator<T> {
    const batchSize = 100;
    let batch: T[];
    let batchIndex = 0;
    q = q.limit(batchSize);
    let lastCursor: string|undefined;
    let hasNext = true;
    let self = this;

    async function ensureBatch() {
      if ((! batch) || (batchIndex >= batch.length)) {
        if (lastCursor) {
          q = q.start(lastCursor);
        }
        let [results, info] = await datastore.runQuery(q);
        batch = results.map(e => self.fromEntity(e));
        batchIndex = 0;
        lastCursor = info.endCursor;
        hasNext = info.moreResults !== 'NO_MORE_RESULTS';
      }
    }

    return {
      async next(): Promise<T|undefined> {
        await ensureBatch();
        let ret = batch[batchIndex];
        batchIndex++;
        return ret;
      },
      async hasNext(): Promise<boolean> {
        await ensureBatch();
        return hasNext;
      }
    };
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

  toEntity(t: T): Entity {
    let data = {} as any;
    for (let f of Object.keys(this.schema)) {
      // Do not serialize the id field, it's handled by the key
      if (f !== 'id') {
        data[f] = (t as any)[f];
      }
    }

    return {
      key: this.keyFromId(t.id),
      data
    };
  }

  fromEntity(e: any) {
    let ret = {} as any;
    let key: DatastoreKey = e[datastore.KEY];
    ret.id = this.idFromKey(key);
    for (let f of Object.keys(this.schema)) {
      ret[f] = e[f];
    }
    return ret as T;
  }


  async remove(tarr: T[]): Promise<void> {
    await this.removeById(tarr.map(t => t.id));
  }

  async save(tarr: T[]): Promise<(K|undefined)[]> {
    let error = this.validate(tarr);
    if (error) {
      throw error;
    }
    let entities = tarr.map(t => this.toEntity(t));
    let savedEntities = await datastore.save(entities);
    let mutationResults = savedEntities[0].mutationResults as any[];
    // mr.key is only set (i.e. non-empty) if datastore generated a key for
    // us. If we specify a key/id ahead of time, then it's empty.
    return mutationResults.map(mr => mr.key && (mr.key.path[0].id || mr.key.path[0].name));
  }

  customizeError(error: joi.ValidationErrorItem): joi.ValidationErrorItem {
    return error;
  }

  validate(ts: T[]): joi.ValidationError {
    let ret = joi.validate(ts,
      toArraySchema(this.schema),
      {
        abortEarly: false,
        language: {
          key: '{{label}} '
        }
      }).error;
    if (ret) {
      ret.details = ret.details.map(error => this.customizeError(error));
    }
    return ret;
  }
}

export const DatastoreUtil = {
  // Combine given array of keys into a single key.
  // Expects most senior keys first.
  // i.e. [grandparentKey, parentKey, childKey]
  concat(keys: DatastoreKey[]): DatastoreKey {
    let path = [];
    let lastKeyIncomplete = false;
    for (let key of keys) {
      if (lastKeyIncomplete) {
        throw new Error('Only the last key in the array can be incomplete');
      }
      path.push(key.kind);
      if (key.name) {
        path.push(key.name);
      } else if (key.id) {
        path.push(datastore.int(key.id));
      } else {
        lastKeyIncomplete = true;
      }
    }
    return datastore.key(path);
  }
};
