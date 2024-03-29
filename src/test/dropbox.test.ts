import { RuleService as rs, Rule } from "../models";
import dropbox, { MoveResult } from "../dropbox";
import _ = require("underscore");
import { assert } from "chai";

const { DROPBOX_TEST_TOKEN } = process.env;

const testingFolder = "/testing";
const protoFolder = "/proto testing";

const rule1Files = [
  "testfoo.txt",
  "testfoo (1).txt",
  "testfoo (2).txt",
  "testfoo (3).txt",
  "testfoo (4).txt",
  "testfoo (5).txt",
];
const rule2Files = ["moose.json", "moose (2).json", "peach.json"];

describe("Dropbox", function () {
  if (!DROPBOX_TEST_TOKEN) {
    assert.fail("Missing env variable DROPBOX_TEST_TOKEN");
  }
  const dbx = dropbox(DROPBOX_TEST_TOKEN);

  let uniqueId: string;
  let testFolder: string;
  let testSortingFolder: string;

  function rules(): Rule[] {
    let ret: Rule[] = [];
    let rule = rs.makeNew("user" + uniqueId);
    // This first rule should be skipped since the destination is the sorting
    // folder
    rule.type = "EXT_EQ";
    rule.pattern = "json";
    rule.dest = testSortingFolder; // << not allowed!
    ret.push(rule);
    rule = rs.makeNew("user" + uniqueId);
    rule.type = "NAME_CONTAINS";
    rule.pattern = "testf";
    rule.dest = testFolder + "/rule1";
    ret.push(rule);
    rule = rs.makeNew("user" + uniqueId);
    rule.type = "EXT_EQ";
    rule.pattern = "json";
    rule.dest = testFolder + "/rule2/";
    ret.push(rule);
    return ret;
  }

  beforeEach(async function () {
    uniqueId = String(Math.random()).split(".")[1];
    testFolder = testingFolder + `/t${uniqueId}`;
    testSortingFolder = testFolder + "/SortMyBox";

    await dbx.client.filesCopyV2({
      from_path: protoFolder,
      to_path: testSortingFolder,
    });
  });

  afterEach(async function () {
    await dbx.client.filesDeleteV2({ path: testFolder });
  });

  it("End to end test", async function () {
    let moveResult = await dbx.runRules(testSortingFolder, rules());
    let expected: MoveResult[] = [];
    rule1Files.forEach((f) => {
      expected.push({
        fileName: f,
        fullDestPath: testFolder + "/rule1/" + f,
        conflict: false,
      });
    });

    rule2Files.forEach((f) => {
      expected.push({
        fileName: f,
        fullDestPath: testFolder + "/rule2/" + f,
        conflict: false,
      });
    });

    let moves = _.sortBy(moveResult.results, "fullDestPath");
    expected = _.sortBy(expected, "fullDestPath");
    assert.deepEqual(moves, expected);
  });
});
