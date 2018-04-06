import { Query } from '@google-cloud/datastore/query';
import { DatastoreKey } from '@google-cloud/datastore/entity';
import joi = require('joi');

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

export interface ModelService<K, T extends Model<K>> {
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
