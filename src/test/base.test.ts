import { datastore as ds, DatastoreUtil as du } from '../models';
import { assert } from 'chai';

describe("DatastoreUtil", function() {
  it("concat() does not allow partial keys in the middle", async function() {
    let k1 = ds.key(['x']);
    let k2 = ds.key(['y']);
    assert.throws(function() {
      du.concat([k1, k2]);
    });
  });

  it("concat() works on single partial key", async function() {
    let k1 = ds.key(['x']);
    let actual = du.concat([k1]);
    assert.deepEqual(actual, k1);
  });

  it("concat() works on single complete key", async function() {
    let k1 = ds.key(['x', ds.int('123')]);
    let actual = du.concat([k1]);
    assert.deepEqual(actual, k1);
  });

  it("concat() works on multiple parents with final partial key", async function() {
    let k1 = ds.key(['x', ds.int('123')]);
    let k2 = ds.key(['y', 'name']);
    let k3 = ds.key(['z']);
    let expected = ds.key(['x', ds.int('123'), 'y', 'name', 'z']);
    let actual = du.concat([k1, k2, k3]);
    assert.deepEqual(actual, expected);
  });

  it("concat() works on multiple parents with final complete key", async function() {
    let k1 = ds.key(['x', ds.int('123')]);
    let k2 = ds.key(['y', 'name']);
    let k3 = ds.key(['z', ds.int('5123')]);
    let expected = ds.key(['x', ds.int('123'), 'y', 'name', 'z', ds.int('5123')]);
    let actual = du.concat([k1, k2, k3]);
    assert.deepEqual(actual, expected);
  });
});
