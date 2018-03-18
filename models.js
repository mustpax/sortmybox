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

  constructor(fields) {
    fields = fields || {};
    Object.assign(this, Visit.defaults(), fields);
  }

  toObj() {
    return _.pick(this, Visit.fields());
  }

  async save() {
    return await datastore.save({
      key: this.key(),
      data: this.toObj()
    });
  }
}

module.exports = {
  datastore,
  Visit
};
