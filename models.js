"use strict";

const Datastore = require('@google-cloud/datastore');
const datastore = Datastore();
var _ = require('underscore');

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

  static key(id) {
    if (id) {
      return datastore.key([Visit.kind(), id]);
    }
    return datastore.key([Visit.kind()]);
  }

  static async query(q) {
    var results = await datastore.runQuery(q);
    results[0] = results[0].map(Visit.fromEntity);
    return results;
  }

  static fromEntity(entity) {
    var ret = new Visit();
    Object.assign(ret, _.pick(entity, Visit.fields()));
    var key = entity[datastore.KEY];
    ret.id = key && key.path && key.path[1];
    return ret;
  }

  static all() {
    return datastore.createQuery(Visit.kind());
  }

  static create(fields) {
    fields = Object.assign(fields, Visit.defaults(), fields);
    return new Visit(fields);
  }

  constructor(fields) {
    fields = fields || {};
    Object.assign(this, fields);
  }

  toObj() {
    return _.pick(this, Visit.fields());
  }

  async save() {
    return await datastore.save({
      key: Visit.key(this.id),
      data: this.toObj()
    });
  }
}

module.exports = {
  datastore,
  Visit
};
