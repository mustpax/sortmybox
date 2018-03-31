"use strict";

import Datastore = require('@google-cloud/datastore');
import { Query } from '@google-cloud/datastore/query';
import { DatastoreKey } from '@google-cloud/datastore/entity';
import { CommitResult } from '@google-cloud/datastore/request';

export const datastore = new Datastore({});
// const _ = require('underscore');

// interface FieldDefinition {
//   name: string;
//   type: type;
// }
//
// interface Schema {
//   kind: string;
//   fields: {
//     [fieldName: string]: FieldDefinition
//   };
// }
//
// interface Model {
//   key(): DatastoreKey;
// }
//
// class VisitSchema implements Schema {
//   readonly kind = "visit";
//   readonly
// }

export type ModelId = string|number;

export interface Schema<T extends Model> {
  fields: string[];
  kind: string;
  makeNew(): T;
  findById(id: ModelId): Promise<T>;
  fromEntity(e: object): T;
  toEntity(t: T): object;
  query(q: Query): Promise<T[]>;
  remove(t: T[]): Promise<void>;
  save(t: T[]): Promise<ModelId[]>;
}

export interface Model {
  id?: ModelId;
}

export class VisitSchema implements Schema<Visit> {
  fields = [
    'created'
  ];
  kind = 'visit';

  makeNew() {
    let ret = new Visit();
    ret.created = new Date();
    return ret;
  }

  async findById(id: ModelId) {
    let k = datastore.key([this.kind, id]);
    return this.fromEntity(await datastore.get(k));
  }

  toEntity(v: Visit): object {
    let ret = {} as any;
    ret[Datastore.KEY] = this.toKey(v);
    for (let f of this.fields) {
      ret[f] = (v as any)[f];
    }
    return ret;
  }

  fromEntity(e: any) {
    let ret = {} as any;
    let k = e[Datastore.KEY];
    ret.id = k && k[1];
    for (let f of this.fields) {
      ret[f] = e[f];
    }
    return ret as Visit;
  }

  toKey(v: Visit): DatastoreKey {
    if (v.id) {
      return datastore.key([this.kind, v.id]);
    }
    return datastore.key([this.kind]);
  }

  idFromKey(k: DatastoreKey): ModelId|null {
    let path = k.path as ModelId[];
    if (path.length > 1) {
      return path[1];
    }
    return null;
  }

  all(): Query {
    return datastore.createQuery(this.kind);
  }

  async query(q: Query) {
    let results = await datastore.runQuery(q);
    return results.map(e => this.fromEntity(e));
  }

  async remove(visits: Visit[]) {
    await datastore.delete(visits.map(visit => this.toKey(visit)));
  }

  async save(visits: Visit[]) {
    let entities = visits.map(visit => this.toEntity(visit));
    let savedEntities = await datastore.save(entities) as any[];
    return savedEntities.map(entity => entity.key.path[1]);
  }
}

export const visitSchema = new VisitSchema();

export class Visit implements Model {
  id?: number;
  created: Date;
}

// class Visit2 {
//   static kind() {
//     return 'visit';
//   }
//
//   static defaults() {
//     return {
//       created: new Date()
//     };
//   }
//
//   static fields() {
//     return [
//       'created'
//     ];
//   }
//
//
//   static key(id: number): DatastoreKey {
//     if (id) {
//       return datastore.key([Visit.kind(), id]);
//     }
//     return datastore.key([Visit.kind()]);
//   }
//
//   static async query(q: Query) {
//     let results = await datastore.runQuery(q);
//     results[0] = results[0].map(Visit.fromEntity);
//     return results;
//   }
//
//   static fromEntity(entity: any): Visit {
//     let ret = new Visit();
//     Object.assign(ret, _.pick(entity, Visit.fields()));
//     let key = entity[datastore.KEY];
//     ret.id = key && key.path && key.path[1];
//     return ret;
//   }
//
//   static all() {
//     return datastore.createQuery(Visit.kind());
//   }
//
//   static async deleteById(id: number) {
//     let key = Visit.key(id);
//     return await datastore.delete(key);
//   }
//
//   id: number;
//   created: Date;
//
//   constructor() {
//   }
//
//   toObj(): any {
//     return _.pick(this, Visit.fields());
//   }
//
//   key(): DatastoreKey {
//     return Visit.key(this.id);
//   }
//
//   async save(): Promise<CommitResult> {
//     return await datastore.save({
//       key: this.key(),
//       data: this.toObj()
//     });
//   }
//
//   async delete() {
//     datastore.delete(this.key());
//   }
// }

module.exports = {
  datastore,
  Visit
};
