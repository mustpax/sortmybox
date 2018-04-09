import { Entity, Model, AbstractModelService, DatastoreUtil as dutil } from './base';
import { DatastoreKey } from '@google-cloud/datastore/entity';
import { UserService } from './user';

import datastore from './datastore';

import joi = require('joi');

export class Rule implements Model<RuleKey> {
  id?: RuleKey;
  type?: string;
  pattern?: string;
  dest?: string;
  rank?: number;
  created?: Date;
}

export class RuleKey {
  ownerId?: string;
  ruleId?: string;
}

export class RuleSchema extends AbstractModelService<RuleKey, Rule> {
  schema = {
    type: joi.string().required().only('NAME_CONTAINS', 'GLOB', 'EXT_EQ'),
    pattern: joi.string().required()
      .regex(/\//, {invert: true}) // disallow slash /
      .when('type', {is: 'EXT_EQ', then: joi.string().regex(/\./, { invert: true })}), // disallow period . when type is EXT_EQ
    rank: joi.number().required().min(0),
    dest: joi.string().required().regex(/^\//),
    created: joi.date().required()
  };
  kind = 'Rule';

  makeNew(ownerId: string): Rule {
    let id = new RuleKey();
    id.ownerId = ownerId;
    let ret: Rule = {
      created: new Date(),
      id
    };
    return ret;
  }

  toEntity(r: Rule): Entity {
    if ((! r.id) || (! r.id.ownerId)) {
      throw new Error('Cannot convert rule to entity. Missing ownerId ' + r);
    }
    return super.toEntity(r);
  }

  fromEntity(e: any) {
    let ret = {} as any;
    let key: DatastoreKey = e[datastore.KEY];
    ret.id = this.idFromKey(key);
    for (let f of Object.keys(this.schema)) {
      ret[f] = e[f];
    }
    return ret as Rule;
  }

  keyFromId(id?: RuleKey) {
    if (!id || !id.ownerId) {
      throw new Error('Cannot generate key for Rule with no owner id. ' + this);
    }
    let userKey = UserService.keyFromId(id.ownerId);
    let ruleKey;
    if (id.ruleId) {
      ruleKey = datastore.key([this.kind, datastore.int(id.ruleId)]);
    } else {
      ruleKey = datastore.key([this.kind]);
    }
    return dutil.concat([userKey, ruleKey]);
  }

  idFromKey(k: DatastoreKey): RuleKey|undefined {
    if (! k) {
      throw new Error('Cannot generate a RuleKey from null key');
    }
    if (! k.parent) {
      throw new Error('Cannot generate a RuleKey from a key with no parent ' + k);
    }
    let ret = new RuleKey();
    ret.ownerId = k.parent.id || k.parent.name;
    ret.ruleId = k.id;
    return ret;
  }

  async save(rules: Rule[]): Promise<(RuleKey|undefined)[]> {
    let error = this.validate(rules);
    if (error) {
      throw error;
    }
    let entities = rules.map(t => this.toEntity(t));
    let savedEntities = await datastore.save(entities);
    let mutationResults = savedEntities[0].mutationResults as any[];
    // mr.key is only set (i.e. non-empty) if datastore generated a key for
    // us. If we specify a key/id ahead of time, then it's empty.
    return mutationResults.map(mr => {
      if (! mr.key) {
        return undefined;
      }
      let rk = new RuleKey();
      rk.ownerId = mr.key.path[0].id || mr.key.path[0].name;
      rk.ruleId = mr.key.path[1].id || mr.key.path[1].name;
      return rk;
    });
  }
}

export let RuleService = new RuleSchema();
