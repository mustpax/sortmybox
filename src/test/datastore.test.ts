import { datastore as ds } from '../models';
import { expect } from 'chai';

describe("datastore", function() {
  it("save then get", async function() {
    const kind = 'test_entity';
    const toSave = {
      key: ds.key([kind]),
      data: {
        name: 'moose',
        fave: 7,
        happy: true
      }
    };
    let [{ mutationResults }] = await ds.save(toSave);
    let keys = mutationResults.map(mr => ds.key([kind, ds.int((mr.key.path[0] as any).id)]));
    let [[fromDS]]: any = await ds.get(keys);
    expect(toSave.data).deep.equal(fromDS);
  });
});
