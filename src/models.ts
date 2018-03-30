"use strict";

import Datastore = require('@google-cloud/datastore');
import { Query } from '@google-cloud/datastore/query';
import { DatastoreKey } from '@google-cloud/datastore/entity';
import { CommitResult } from '@google-cloud/datastore/request';

const datastore = new Datastore({});
const _ = require('underscore');

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

class Visit {
  static kind() {
    return 'visit';
  }

  static defaults() {
    return {
      created: new Date()
    };
  }

  static fields() {
    return [
      'created'
    ];
  }


  static key(id: number): DatastoreKey {
    if (id) {
      return datastore.key([Visit.kind(), id]);
    }
    return datastore.key([Visit.kind()]);
  }

  static async query(q: Query) {
    let results = await datastore.runQuery(q);
    results[0] = results[0].map(Visit.fromEntity);
    return results;
  }

  static fromEntity(entity: any): Visit {
    let ret = new Visit();
    Object.assign(ret, _.pick(entity, Visit.fields()));
    let key = entity[datastore.KEY];
    ret.id = key && key.path && key.path[1];
    return ret;
  }

  static all() {
    return datastore.createQuery(Visit.kind());
  }

  static async deleteById(id: number) {
    let key = Visit.key(id);
    return await datastore.delete(key);
  }

  id: number;
  created: Date;

  constructor() {
  }

  toObj(): any {
    return _.pick(this, Visit.fields());
  }

  key(): DatastoreKey {
    return Visit.key(this.id);
  }

  async save(): Promise<CommitResult> {
    return await datastore.save({
      key: this.key(),
      data: this.toObj()
    });
  }

  async delete() {
    datastore.delete(this.key());
  }
}

module.exports = {
  datastore,
  Visit
};
