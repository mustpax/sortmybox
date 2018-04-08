import { datastore as ds, User, UserService as us } from '../models';
import { assert } from 'chai';

describe('User', function() {
  it('validation: user with only required fields is valid', function() {
    let user = {} as User;
    user.id = '123';
    user.periodicSort = true;
    user.modified = user.created = user.lastLogin = new Date();
    user.sortingFolder = 'test';
    user.accountType = 'BOX';
    user.fileMoves = 0;
    let error = us.validate([user]);
    assert.notOk(error);
  });

  it('validation: invalid account type', function() {
    let user = us.makeNew();
    user.accountType = 'inval';
    let error = us.validate([user]);
    assert.ok(error);
  });

  it('validation: user with all fields is falid', function() {
    let user = {} as User;
    user.id = '123';
    user.periodicSort = true;
    user.created = new Date();
    user.modified = new Date();
    user.fileMoves = 0;
    user.name = 'x';
    user.nameLower = 'x';
    user.email = 'x';
    user.lastSync = new Date();
    user.lastLogin = new Date();
    user.token = 'x';
    user.secret = 'x';
    user.sortingFolder = 'x';
    user.tokenExpiration = new Date();
    user.refreshToken = 'x';
    user.dropboxV2Token = 'x';
    user.dropboxV2Id = 'x';
    user.dropboxV2Migrated = false;
    user.accountType = 'BOX';
    let error = us.validate([user]);
    assert.notOk(error);
  });

  it('validation: missing required field', function() {
    let user = {} as User;
    user.id = '123';
    user.created = new Date();
    user.modified = new Date();
    user.fileMoves = 0;
    let error = us.validate([user]);
    assert.ok(error);
  });

  it('validation: makeNew() returns valid user', function() {
    let user = us.makeNew();
    let error = us.validate([user]);
    assert.notOk(error);
  });

  it('save(): invalid object', async function() {
    let user = {} as User;
    user.id = '123';
    user.created = new Date();
    user.modified = new Date();
    let thrown = true;
    try {
      await us.save([user]);
      thrown = false;
    } catch (e) {}
    assert.ok(thrown);
  });

  //
  // it('fromEntity()', function() {
  //   let id = '10';
  //   let kind = 'foo';
  //   let date = new Date();
  //   let entity = {
  //     [ds.KEY]: ds.key([kind, id]),
  //     created: date
  //   };
  //   expect(VS.fromEntity(entity)).deep.equal({
  //     id: '10',
  //     created: date
  //   });
  // });
  //
  // it('toEntity()', function() {
  //   let visit = VS.makeNew();
  //   visit.id = '12';
  //   let entity = VS.toEntity(visit);
  //   expect(entity).to.deep.equal({
  //     key: VS.keyFromId(visit.id),
  //     data: {
  //       created: visit.created
  //     }
  //   });
  // });
  //
  // it('makeNew()', function() {
  //   let visit = VS.makeNew();
  //   expect(visit.created).to.be.ok;
  //   expect(visit.id).to.be.not.ok;
  // });
  //
  // it('idFromKey string', function() {
  //   let expectedId = '123';
  //   let k = ds.key(['a', expectedId]);
  //   let actual = VS.idFromKey(k);
  //   assert.deepEqual(actual, expectedId);
  // });
  //
  // it('idFromKey int', function() {
  //   let expectedId = 123;
  //   let k = ds.key(['a', expectedId]);
  //   let actual = VS.idFromKey(k);
  //   assert.deepEqual(actual, '123');
  // });
  //
  // it('idFromKey ds.int(number)', function() {
  //   let expectedId = ds.int(123);
  //   let k = ds.key(['a', expectedId]);
  //   let actual = VS.idFromKey(k);
  //   assert.deepEqual(actual, '123');
  // });
  //
  // it('idFromKey ds.int(string)', function() {
  //   let expectedId = ds.int('123');
  //   let k = ds.key(['a', expectedId]);
  //   let actual = VS.idFromKey(k);
  //   assert.deepEqual(actual, '123');
  // });
  //
  // it('keyFromId', function() {
  //   let id = '123';
  //   let k = VS.keyFromId(id);
  //   assert.deepEqual(k, ds.key([VS.kind, ds.int(id)]));
  // });
  //
  // it("save() with no id then findByIds()", async function() {
  //   let dates = [1522522000482, 1522522060482];
  //   let visits = dates.map((date) => {
  //     let visit = VS.makeNew();
  //     visit.created = new Date(date);
  //     return visit;
  //   });
  //   let ids = await VS.save(visits);
  //   let fromDS = await VS.findByIds(ids);
  //   expect(fromDS.map(v => v.created)).deep.equal(visits.map(v => v.created));
  // });
  //
  // it("save() with id then findByIds()", async function() {
  //   let dates = [1522522000482, 1522522060482];
  //   let ids = ['5', '15'];
  //   let visits = dates.map((date, i) => {
  //     let visit = VS.makeNew();
  //     visit.id = ids[i];
  //     visit.created = new Date(date);
  //     return visit;
  //   });
  //   await VS.save(visits);
  //   let fromDS = await VS.findByIds(ids);
  //   expect(fromDS.map(v => v.created)).deep.equal(visits.map(v => v.created));
  // });
  //
  // it("save() then findByIds() then save()", async function() {
  //   const date1 = new Date(1522522000482);
  //   const date2 = new Date(1522522060482);
  //   let visit = VS.makeNew();
  //
  //   visit.created = date1;
  //   let [id] = await VS.save([visit]);
  //   let [fromDS] = await VS.findByIds([id]);
  //   assert.deepEqual(fromDS.created, visit.created);
  //
  //   fromDS.created = date2;
  //   await VS.save([fromDS]);
  //   [fromDS] = await VS.findByIds([id]);
  //   assert.deepEqual(fromDS.created, date2);
  // });
});
