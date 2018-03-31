"use strict";

import Datastore = require('@google-cloud/datastore');
import { Query } from '@google-cloud/datastore/query';
import { DatastoreKey } from '@google-cloud/datastore/entity';
// import { CommitResult } from '@google-cloud/datastore/request';

export const datastore = new Datastore({});

export type ModelId = string|number;

export interface Schema<T extends Model> {
  fields: string[];
  kind: string;
  makeNew(): T;
  findById(id: ModelId): Promise<T>;
  fromEntity(e: object): T;
  toEntity(t: T): object;
  query(q: Query): Promise<T[]>;
  all(): Query;
  remove(t: T[]): Promise<void>;
  save(t: T[]): Promise<ModelId[]>;
}

export interface Model {
  id?: ModelId;
}

export interface Entity {
  key: DatastoreKey;
  data: any;
}

export class Visit implements Model {
  id?: number;
  created: Date;
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

  toEntity(v: Visit): Entity {
    let data = {} as any;
    for (let f of this.fields) {
      data[f] = (v as any)[f];
    }

    return {
      key: this.toKey(v),
      data
    };
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
    datastore.save(entities).then(console.error).catch(console.error);
    let savedEntities = await datastore.save(entities);
    let mutationResults = savedEntities[0].mutationResults as any[];
    return mutationResults.map(mr => mr.key.path[0].id) as ModelId[];
  }
}

export let VisitService = new VisitSchema();
