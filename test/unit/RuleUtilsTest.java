package unit;

import org.junit.Test;

import com.google.appengine.repackaged.com.google.common.base.Pair;

import play.test.UnitTest;
import rules.RuleUtils;
import rules.RuleUtils.FileAndExtension;

public class RuleUtilsTest extends UnitTest {
    @Test
    public void testBasename() {
        assertEquals("foo", RuleUtils.basename("foo"));
        assertEquals("foo", RuleUtils.basename("a/foo"));
        assertEquals("foo", RuleUtils.basename("/a/foo"));
        assertEquals(" f oo", RuleUtils.basename(" f oo"));
        assertEquals(" f oo", RuleUtils.basename("/a/a/ f oo"));
        assertEquals(" f oo", RuleUtils.basename("/ f oo"));
    }

    @Test
    public void testGetExt() {
        assertEquals(" a b c", RuleUtils.getExt("a. a b c"));
        assertEquals(" a b c", RuleUtils.getExt("x.b.a. a b c"));
        assertEquals("ppt", RuleUtils.getExt("foo.ppt"));
        assertNull(RuleUtils.getExt(".bash rc"));
        assertNull(RuleUtils.getExt(".bashrc"));
    }

    @Test
    public void testSplit() {
        FileAndExtension p = RuleUtils.splitName("a. a b c");
        assertEquals("a", p.fileName.get());
        assertEquals(" a b c", p.extension.get());

        p = RuleUtils.splitName(".bashrc");
        assertEquals(".bashrc", p.fileName.get());
        assertFalse(p.extension.isPresent());
        
        p = RuleUtils.splitName("tab\ttab");
        assertEquals("tab\ttab", p.fileName.get());
        assertFalse(p.extension.isPresent());
    }

    @Test
    public void testInsert() {
        assertEquals("a insert.b", RuleUtils.insertIntoName("a.b", " insert"));
        assertEquals("a.b", RuleUtils.insertIntoName("a.b", null));
        assertEquals("a.b", RuleUtils.insertIntoName("a.b", ""));

        assertEquals("foobar", RuleUtils.insertIntoName("foo", "bar"));
        assertEquals(".foobar", RuleUtils.insertIntoName(".foo", "bar"));
        assertEquals(".foo", RuleUtils.insertIntoName(".foo", null));
    }

    @Test
    public void testNormalize() {
        assertEquals("/a", RuleUtils.normalize("A"));
        assertEquals("/", RuleUtils.normalize(""));
        assertEquals("/a/bcd/efg", RuleUtils.normalize("//a///BcD//efG/////"));
        assertEquals("/a/bcd/efg", RuleUtils.normalize("  / /a///BcD  //efG/////  "));
        assertEquals("/a/B/C", RuleUtils.normalize("/a/B/C  ", false));
    }

    @Test
    public void testGetParent() {
        assertEquals("/", RuleUtils.getParent("/a"));
        assertEquals("/", RuleUtils.getParent("///a"));
        assertEquals("a", RuleUtils.getParent("a///b"));
        assertEquals("/foo bar", RuleUtils.getParent("/foo bar///baz//"));
    }
}
