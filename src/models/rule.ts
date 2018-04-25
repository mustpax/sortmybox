import { Entity, Model, AbstractModelService, DatastoreUtil as dutil } from './base';
import { DatastoreKey } from '@google-cloud/datastore/entity';
import { UserService } from './user';

import datastore from './datastore';

import joi = require('joi');

export const MAX_RULES = 200;

function globToRegex(globStr: string): RegExp {
  let replacer = /(\W)/g;
  let regexifiedStr = globStr.replace(replacer, match => {
    if (match === '*') {
      return '.*';
    }
    if (match === '?') {
      return '.?';
    }
    return '\\\\' + match;
  });
  return RegExp(regexifiedStr, 'gi');
}

export class Rule implements Model<RuleKey> {
  id?: RuleKey;
  type?: string;
  pattern?: string;
  dest?: string;
  rank?: number;
  created?: Date;

  matches(fileName: string): boolean {
    if (! this.pattern) {
      return false;
    }

    switch (this.type) {
    case 'NAME_CONTAINS':
      return fileName.toLowerCase().indexOf(this.pattern.toLowerCase()) > -1;
    case 'GLOB':
      return !! globToRegex(this.pattern).exec(fileName);
    case 'EXT_EQ':
      let parts = fileName.split('.');
      if (parts.length < 2) {
        return false;
      }
      let ext = parts[parts.length - 1];
      return ext.toLowerCase() === this.pattern.toLowerCase();
    }
    return false;
  }
}

export class RuleKey {
  ownerId?: string;
  ruleId?: string;
}

export class RuleSchema extends AbstractModelService<RuleKey, Rule> {
  MAX_RULES = MAX_RULES;
  schema = {
    type: joi.string().required().only('NAME_CONTAINS', 'GLOB', 'EXT_EQ').label('Rule type'),
    pattern: joi.string().required()
      .regex(/\//, {invert: true}) // disallow slash /
      .when('type', {is: 'EXT_EQ', then: joi.string().regex(/\./, { invert: true })}) // disallow period . when type is EXT_EQ
      .label('Rule pattern')
      // .error(() => ({message: 'Pattern is required'} as any))
      ,
    rank: joi.number().required().min(0),
    dest: joi.string().required().regex(/^\//).label('Destination'),
    created: joi.date().required()
  };
  errorOverrides: { [key: string]: { [key: string]: string }} = {
    dest: {
      'string.regex.base': 'Destination must start with a slash /'
    },
    pattern: {
      'string.regex.invert.base': 'Pattern cannot contain period . when rule type is Extension Equals'
    }
  };

  kind = 'Rule';

  makeNew(ownerId: string): Rule {
    let id = new RuleKey();
    id.ownerId = ownerId;
    let ret: Rule = new Rule();
    ret.created = new Date();
    ret.id = id;
    return ret;
  }

  toEntity(r: Rule): Entity {
    if ((! r.id) || (! r.id.ownerId)) {
      throw new Error('Cannot convert rule to entity. Missing ownerId ' + r);
    }
    return super.toEntity(r);
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

  async findByOwner(ownerId: string): Promise<Rule[]> {
    let q = this.all()
      .hasAncestor(UserService.keyFromId(ownerId))
      .order('rank')
      .limit(MAX_RULES);
    return await this.query(q);
  }

  customizeError(error: joi.ValidationErrorItem): joi.ValidationErrorItem {
    if (this.errorOverrides[error.path[1]] && this.errorOverrides[error.path[1]][error.type]) {
      error.message = this.errorOverrides[error.path[1]][error.type];
    }
    return error;
  }
}

export let RuleService = new RuleSchema();
