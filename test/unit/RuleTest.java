package unit;

import play.test.*;
import org.junit.*;
import models.*;
 
public class RuleTest extends UnitTest {
    @Test
    public void testNameContains() {
        models.Rule r = new models.Rule();
        r.pattern = "foo";
        r.type = models.Rule.RuleType.NAME_CONTAINS;
        r.dest = "/tmp";

        assertFalse(r.matches("bar"));
        assertTrue(r.matches("foobar"));
        assertTrue(r.matches("barfoo"));
    }

    @Test
    public void testGlob() {
        models.Rule r = new models.Rule();
        r.type = models.Rule.RuleType.GLOB;
        r.dest = "/tmp";

        r.pattern = "foo";
        assertFalse(r.matches("bar"));
        assertFalse(r.matches("foobar"));
        assertFalse(r.matches("barfoo"));
        assertTrue(r.matches("foo"));

        r.pattern = "fo.o";
        assertFalse(r.matches("foxo"));
        assertFalse(r.matches("foo"));
        assertTrue(r.matches("fo.o"));

        r.pattern = "foo?";
        assertFalse(r.matches("bar"));
        assertFalse(r.matches("fooxx"));
        assertFalse(r.matches("xfoox"));
        assertTrue(r.matches("foo "));
        assertTrue(r.matches("foox"));
        assertTrue(r.matches("foo\u2605"));

        r.pattern = "*foo";
        assertFalse(r.matches("foobar"));
        assertFalse(r.matches("  foobar"));
        assertTrue(r.matches("foo"));
        assertTrue(r.matches("bar foo"));
        assertTrue(r.matches("   foo"));

        r.pattern = "*foo*";
        assertFalse(r.matches("fo"));
        assertFalse(r.matches("bar"));
        assertFalse(r.matches(""));
        assertTrue(r.matches("foo"));
        assertTrue(r.matches("bar foo baz"));
        assertTrue(r.matches("   foo"));
        assertTrue(r.matches(" foo&."));
    }
}
