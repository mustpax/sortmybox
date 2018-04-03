import { datastore as ds, VisitService as VS } from '../models';
import joi = require('joi');
import { expect } from 'chai';

describe('Visit', function() {
  it('validation: valid object', function() {
    let visit = VS.makeNew();
    visit.id = ds.int('123');
    let { error } = joi.validate(visit, VS.schema);
    expect(error).be.null;
  });

  it('validation: invalid object', function() {
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
    visit.id = ds.int('12');
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
    expect(visit.created).to.be.not.null;
    expect(visit.id).to.be.null;
  });

  it('idFromKey');
  it('keyFromId');

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
    let ids = [5, 15].map(id => ds.int(id));
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
});
