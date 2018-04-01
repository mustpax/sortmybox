import { datastore, VisitService as VS } from '../models';
import { expect } from 'chai';

describe('Visit', function() {
  it('fromEntity()');
  it('toEntity()');
  it('makeNew()');

  it("save read back", async function() {
    let dates = [1522522000482, 1522522060482];
    let visits = dates.map(date => {
      let visit = VS.makeNew();
      visit.created = new Date(date);
      return visit;
    });
    let ids = await VS.save(visits);
    let fromDS = await VS.findByIds(ids);
    expect(fromDS).deep.equal(visits);
  });
});
