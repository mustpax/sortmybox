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
    fields = fields || {};
    fields = _.defaults(fields, Visit.defaults());
    return new Visit(fields);
  }

  static async deleteById(id) {
    var key = Visit.key(id);
    return await datastore.delete(key);
  }

  constructor(fields) {
    fields = fields || {};
    Object.assign(this, fields);
  }

  toObj() {
    return _.pick(this, Visit.fields());
  }

  key() {
    return Visit.key(this.id);
  }

  async save() {
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
