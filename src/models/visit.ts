import { Model, AbstractModelService } from './base';
import { DatastoreKey } from '@google-cloud/datastore/entity';

import datastore from './datastore';

import joi = require('joi');

export class Visit implements Model<string> {
  id?: string;
  created?: Date;
}

export class VisitSchema extends AbstractModelService<string, Visit> {
  schema = {
    created: joi.date().required()
  };
  kind = 'visit';

  makeNew() {
    let ret = new Visit();
    ret.created = new Date();
    return ret;
  }

  keyFromId(id?: string) {
    if (id) {
      return datastore.key([this.kind, datastore.int(id)]);
    }
    return datastore.key([this.kind]);
  }

  idFromKey(k: DatastoreKey): string|undefined {
    if (! k || k.path.length < 2) {
      return undefined;
    }
    return String(k.path[1]);
  }
}

export let VisitService = new VisitSchema();
