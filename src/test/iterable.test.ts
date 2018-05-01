import _ = require('underscore');
import { assert } from 'chai';
import { AsyncIterator } from '../models';

let lastIndex = 0;

function dummyData() {
  let arr = [];
  for (let i = 0; i < 12; i++) {
    arr.push(lastIndex++);
  }
  return Promise.resolve(arr);
}

function gen(max: number): Iterable<Promise<number>> {
  let curBatch: number[];
  let ret: Iterable<Promise<number>> = {
    [Symbol.iterator]() {
      return {
        next(): IteratorResult<Promise<number>> {
          return {
            done: false,
            value: new Promise(function(resolve, reject) {
              if (curBatch && curBatch.length > 0) {
                resolve(curBatch.shift());
              } else {
                dummyData().then(batch => {
                  curBatch = batch;
                  resolve(curBatch.shift());
                });
              }
            })
          };
        }
      };
    }
  };
  return ret;
}

function getIterable(max: number): AsyncIterator<number> {
  let curBatch: number[];
  let curIndex = 0;
  let produced = 0;
  return {
    async next() {
      if (produced >= max) {
        return undefined;
      }
      if ((! curBatch) || curIndex >= curBatch.length) {
        curBatch = await dummyData();
        curIndex = 0;
      }
      let ret = curBatch[curIndex];
      curIndex++;
      produced++;
      return ret;
    },
    async hasNext() {
      return produced < max;
    }
  };
}

describe("Iterable", function() {
  beforeEach(function() {
    lastIndex = 0;
  });
  it("using Symbol.iterator", async function() {
    let arr = [];
    let seq = gen(1000);
    for (let n of seq) {
      if (arr.length >= 100) {
        break;
      }
      arr.push(await n);
    }
    assert.deepEqual(arr, _.range(100));
  });
  it("using custom AsyncIterable", async function() {
    let iter = getIterable(100);
    let arr = [];
    while (await iter.hasNext()) {
      arr.push(await iter.next());
    }
    assert.deepEqual(arr, _.range(100));
  });
});
