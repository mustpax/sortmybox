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
  findByIds(ids: ModelId[]): Promise<T[]>;
  fromEntity(e: object): T;
  toEntity(t: T): object;
  query(q: Query): Promise<T[]>;
  all(): Query;
  removeById(ids: ModelId[]): Promise<void>;
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

  async findByIds(ids: ModelId[]) {
    let keys = ids.map(id => this.keyFromId(id));
    return (await datastore.get(keys)).map(e => this.fromEntity(e));
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
    ret.id = k && k.path[1];
    for (let f of this.fields) {
      ret[f] = e[f];
    }
    return ret as Visit;
  }

  keyFromId(id: ModelId) {
    return datastore.key([this.kind, parseInt(id as string)]);
  }

  toKey(v: Visit): DatastoreKey {
    if (v.id) {
      return this.keyFromId(v.id);
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
    let entities = visits.map(visit => this.toEntity(visit));
    let savedEntities = await datastore.save(entities);
    let mutationResults = savedEntities[0].mutationResults as any[];
    return mutationResults.map(mr => mr.key.path[0].id) as ModelId[];
  }
}

export let VisitService = new VisitSchema();
