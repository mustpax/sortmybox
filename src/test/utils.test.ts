import { endsWithCaseInsensitive } from '../utils';
import { assert } from 'chai';

describe("Util", function() {
  it("endsWith() every string ends with empty string", function() {
    assert.isTrue(endsWithCaseInsensitive('', ''));
    assert.isTrue(endsWithCaseInsensitive('a', ''));
    assert.isTrue(endsWithCaseInsensitive('  ', ''));
  });

  it("endsWith() every string ends with itself", function() {
    assert.isTrue(endsWithCaseInsensitive('', ''));
    assert.isTrue(endsWithCaseInsensitive('a', 'A'));
    assert.isTrue(endsWithCaseInsensitive('x! Yc', 'X! yc'));
  });

  it("endsWith() string cannot end with substring longer than itself", function() {
    assert.isFalse(endsWithCaseInsensitive('', ' '));
    assert.isFalse(endsWithCaseInsensitive('a', 'Ab'));
    assert.isFalse(endsWithCaseInsensitive('x! Yc', 'X!  yc'));
  });

  it("endsWith() basic examples", function() {
    assert.isTrue(endsWithCaseInsensitive('abc', 'C'));
    assert.isTrue(endsWithCaseInsensitive('abc', 'Bc'));
    assert.isTrue(endsWithCaseInsensitive('x! Yc', '! yc'));
    assert.isTrue(endsWithCaseInsensitive('x! Yc', ' yc'));
    assert.isTrue(endsWithCaseInsensitive('x! Yc', 'yc'));
  });
});
