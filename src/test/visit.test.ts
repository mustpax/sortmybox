import { datastore as ds, VisitService as VS, addIdToSchema } from '../models';
import joi = require('joi');
import { expect, assert } from 'chai';

describe('Visit', function() {
  it('validation: valid object', function() {
    let visit = VS.makeNew();
    visit.id = '123';
    let { error } = joi.validate(visit, addIdToSchema(VS.schema));
    expect(error).be.null;
  });

  it('validation: missing created', function() {
    let visit = VS.makeNew();
    visit.created = undefined;
    let { error } = joi.validate(visit, VS.schema);
    expect(error).not.be.null;
  });

  it('save(): invalid object', async function() {
    let visit = VS.makeNew();
    visit.created = undefined;
    let thrown = true;
    try {
      await VS.save([visit]);
      thrown = false;
    } catch (e) {}
    expect(thrown).be.true;
  });

  it('fromEntity()', function() {
    let id = '10';
    let kind = 'foo';
    let date = new Date();
    let entity = {
      [ds.KEY]: ds.key([kind, id]),
      created: date
    };
    expect(VS.fromEntity(entity)).deep.equal({
      id: '10',
      created: date
    });
  });

  it('toEntity()', function() {
    let visit = VS.makeNew();
    visit.id = '12';
    let entity = VS.toEntity(visit);
    expect(entity).to.deep.equal({
      key: VS.keyFromId(visit.id),
      data: {
        created: visit.created
      }
    });
  });

  it('makeNew()', function() {
    let visit = VS.makeNew();
    expect(visit.created).to.be.ok;
    expect(visit.id).to.be.not.ok;
  });

  it('idFromKey string', function() {
    let expectedId = '123';
    let k = ds.key(['a', expectedId]);
    let actual = VS.idFromKey(k);
    assert.deepEqual(actual, expectedId);
  });

  it('idFromKey int', function() {
    let expectedId = 123;
    let k = ds.key(['a', expectedId]);
    let actual = VS.idFromKey(k);
    assert.deepEqual(actual, '123');
  });

  it('idFromKey ds.int(number)', function() {
    let expectedId = ds.int(123);
    let k = ds.key(['a', expectedId]);
    let actual = VS.idFromKey(k);
    assert.deepEqual(actual, '123');
  });

  it('idFromKey ds.int(string)', function() {
    let expectedId = ds.int('123');
    let k = ds.key(['a', expectedId]);
    let actual = VS.idFromKey(k);
    assert.deepEqual(actual, '123');
  });

  it('keyFromId', function() {
    let id = '123';
    let k = VS.keyFromId(id);
    assert.deepEqual(k, ds.key([VS.kind, ds.int(id)]));
  });

  it("save() with no id then findByIds()", async function() {
    let dates = [1522522000482, 1522522060482];
    let visits = dates.map((date) => {
      let visit = VS.makeNew();
      visit.created = new Date(date);
      return visit;
    });
    let ids = await VS.save(visits);
    let fromDS = await VS.findByIds(ids);
    expect(fromDS.map(v => v.created)).deep.equal(visits.map(v => v.created));
  });

  it("save() with id then findByIds()", async function() {
    let dates = [1522522000482, 1522522060482];
    let ids = ['5', '15'];
    let visits = dates.map((date, i) => {
      let visit = VS.makeNew();
      visit.id = ids[i];
      visit.created = new Date(date);
      return visit;
    });
    await VS.save(visits);
    let fromDS = await VS.findByIds(ids);
    expect(fromDS.map(v => v.created)).deep.equal(visits.map(v => v.created));
  });

  it("save() then findByIds() then save()", async function() {
    const date1 = new Date(1522522000482);
    const date2 = new Date(1522522060482);
    let visit = VS.makeNew();

    visit.created = date1;
    let [id] = await VS.save([visit]);
    let [fromDS] = await VS.findByIds([id]);
    assert.deepEqual(fromDS.created, visit.created);

    fromDS.created = date2;
    await VS.save([fromDS]);
    [fromDS] = await VS.findByIds([id]);
    assert.deepEqual(fromDS.created, date2);
  });
});
