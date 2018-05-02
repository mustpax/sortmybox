import { datastore as ds, User, UserService as us } from '../models';
import { assert } from 'chai';
import _ = require('underscore');

function testUser(id?: string) {
  let user = us.makeNew();
  if (id) {
    user.id = id;
  }
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
  return user;
}

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

  it('validation: user with null optional fields', function() {
    let user = {} as User;
    user.id = '123';
    user.periodicSort = true;
    user.modified = user.created = user.lastLogin = new Date();
    user.sortingFolder = 'test';
    user.accountType = 'BOX';
    user.fileMoves = 0;
    (user.name as any) = null;
    (user.nameLower as any) = null;
    (user.email as any) = null;
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


  it('fromEntity() populates missing fields', function() {
    let id = 'a12';
    let entity = {
      [ds.KEY]: ds.key([us.kind, id])
    };
    let user = us.fromEntity(entity);
    assert.strictEqual(user.id, id);
    assert.strictEqual(user.fileMoves, 0);
    assert.strictEqual(user.sortingFolder, '/Sortbox');
    assert.strictEqual(user.dropboxV2Migrated, false);
    assert.strictEqual(user.accountType, 'DROPBOX');
  });

  it('fromEntity() does not override present fields with defaults', function() {
    const id = 'a12';
    const fileMoves = 5;
    const sortingFolder = '/foo';
    const dropboxV2Migrated = true;
    const accountType = 'BOX';
    let entity = {
      [ds.KEY]: ds.key([us.kind, id]),
      fileMoves,
      sortingFolder,
      dropboxV2Migrated,
      accountType
    };
    let user = us.fromEntity(entity);
    assert.strictEqual(user.id, id);
    assert.strictEqual(user.fileMoves, fileMoves);
    assert.strictEqual(user.sortingFolder, sortingFolder);
    assert.strictEqual(user.dropboxV2Migrated, dropboxV2Migrated);
    assert.strictEqual(user.accountType, accountType);
  });

  it('fromEntity() populates nameLower from name', function() {
    const id = 'a12';
    let entity = {
      [ds.KEY]: ds.key([us.kind, id]),
      name: 'TEsT'
    };
    let user = us.fromEntity(entity);
    assert.strictEqual(user.name, 'TEsT');
    assert.strictEqual(user.nameLower, 'test');
  });

  it('fromEntity() populates all fields', function() {
    let id = 'a12';
    let entity = {
      [ds.KEY]: ds.key([us.kind, id]),
      name: 'Name',
      nameLower: 'name',
      email: 'foo@example.com',
      periodicSort: true,
      created: new Date(),
      modified: new Date(),
      lastSync: new Date(),
      lastLogin: new Date(),
      token: 't',
      secret: 's',
      fileMoves: 123,
      sortingFolder: '/test',
      tokenExpiration: new Date(),
      refreshToken: 'x',
      dropboxV2Token: 'y',
      dropboxV2Id: 'z',
      dropboxV2Migrated: true,
      accountType: 'DROPBOX'
    };
    let user = us.fromEntity(entity);
    // Delete key and replace with id field to convert to user from entity
    // manually
    delete entity[ds.KEY];
    entity.id = id;
    assert.deepEqual(entity, user as any);
  });


  it('toEntity()', function() {
    let id = '123';
    let user = {} as User;
    user.id = id;
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
    let entity = us.toEntity(user);
    delete user.id;
    // Delete id to convert from user to entity manually
    assert.deepEqual(entity, {
      key: us.keyFromId(id),
      data: user
    });
  });

  it('idFromKey string', function() {
    let expectedId = '123';
    let k = ds.key(['a', expectedId]);
    let actual = us.idFromKey(k);
    assert.strictEqual(actual, expectedId);
  });


  it('idFromKey int', function() {
    let expectedId = 123;
    let k = ds.key(['a', expectedId]);
    let actual = us.idFromKey(k);
    assert.strictEqual(actual, '123');
  });

  it('idFromKey ds.int(number)', function() {
    let expectedId = ds.int(123);
    let k = ds.key(['a', expectedId]);
    let actual = us.idFromKey(k);
    assert.strictEqual(actual, '123');
  });

  it('idFromKey ds.int(string)', function() {
    let expectedId = ds.int('123');
    let k = ds.key(['a', expectedId]);
    let actual = us.idFromKey(k);
    assert.deepEqual(actual, '123');
  });

  it('keyFromId numeric-id', function() {
    let id = '123';
    let k = us.keyFromId(id);
    assert.deepEqual(k, ds.key([us.kind, ds.int(id)]));
  });

  it('keyFromId string-id', function() {
    let id = 'x123';
    let k = us.keyFromId(id);
    assert.deepEqual(k, ds.key([us.kind, id]));
  });

  it("save() with NO id then read back with findByIds()", async function() {
    let user = testUser();
    let [id] = await us.save([user]);
    assert.ok(id);
    let [fromDS] = await us.findByIds([id as string]);
    // user starts out with no id, gets datastore generated id
    user.id = id;
    assert.deepEqual(fromDS, user);
  });

  it("save() with id then read back with findByIds()", async function() {
    let id = 'x555';
    let user = testUser(id);
    await us.save([user]);
    let [fromDS] = await us.findByIds([id]);
    assert.deepEqual(fromDS, user);
  });

  it("save() with numeric id then read back with findByIds()", async function() {
    let id = '555';
    let user = testUser(id);
    await us.save([user]);
    let [fromDS] = await us.findByIds([id]);
    assert.deepEqual(fromDS, user);
  });


  it("save() then findByIds() then save()", async function() {
    let user = testUser();
    let id: string;
    [id] = await us.save([user]) as string[];
    assert.ok(id);
    let [fromDS] = await us.findByIds([id]);
    // user starts out with no id, gets datastore generated id
    user.id = id;
    assert.deepEqual(fromDS, user);
    user.fileMoves = 12345;
    user.email = 'new@example.com';
    await us.save([user]);
    [fromDS] = await us.findByIds([id]);
    assert.deepEqual(fromDS, user);
  });

  it("findByDropboxId()", async function() {
    let dropboxId = String(Math.round(Math.random() * 1e7) + 1);
    let user = testUser();
    user.dropboxV2Id = dropboxId;
    [user.id] = await us.save([user]);
    let fromDS = await us.findByDropboxId(dropboxId);
    assert.deepEqual(fromDS, user);
  });

  it("queryIter()", async function() {
    let users = [];
    let name = '' + Math.random();
    name = 'testName_' + name.split('.')[1];
    for (let i = 0; i < 202; i++) {
      let user = testUser();
      user.name = name;
      users.push(user);
    }
    let ids = await us.save(users);
    for (let i = 0; i < ids.length; i++) {
      users[i].id = ids[i];
    }
    let actualUsers = [];
    let q = us.all().filter('name', name);
    let results = us.queryIter(q);
    while (await results.hasNext()) {
      actualUsers.push(await results.next());
    }
    assert.deepEqual(_.sortBy(actualUsers, 'id'), _.sortBy(users, 'id'));
  });
});
