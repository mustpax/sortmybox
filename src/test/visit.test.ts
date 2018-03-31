import { VisitService as VS } from '../models';
import { expect } from 'chai';

describe('Visit', function() {
  it("save", async function(done) {
    let dates = [1522522000482, 1522522060482];
    let visits = dates.map(date => {
      let visit = VS.makeNew();
      visit.created = new Date(date);
      return visit;
    });
    let ids = await VS.save(visits);
  });
});
