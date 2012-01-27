package unit.models;

import play.test.*;
import org.junit.*;
import models.*;
import models.Rule;
import models.Rule.RuleType;
 
public class RuleTest extends UnitTest {

    @Test
    public void testCRUD() throws Exception {
        Rule rule = new Rule(RuleType.GLOB, "a", "b", 1, 2L);
        
        // verify insert
        rule.insert();
        long id = rule.id;
        assertEquals(rule, Rule.findById(id));

        // verify update
        RuleType type2 = RuleType.NAME_CONTAINS;
        rule.type = type2;
        rule.update();
        assertEquals(rule, Rule.findById(id));

        // verify delete
        rule.delete();
        assertNull(Rule.findById(id));
    }

    @Test
    public void testNameContains() {
        Rule r = new Rule();
        r.pattern = "foo";
        r.type = RuleType.NAME_CONTAINS;
        r.dest = "/tmp";

        assertFalse(r.matches("bar"));
        assertTrue(r.matches("foobar"));
        assertTrue(r.matches("barfoo"));
    }

    @Test
    public void testGlob() {
        Rule r = new Rule();
        r.type = RuleType.GLOB;
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

    @Test
    public void testExtEquals() {
        Rule r = new Rule();
        r.type = RuleType.EXT_EQ;
        r.dest = "/tmp";

        r.pattern = null;
        assertFalse(r.matches("foo"));
        assertFalse(r.matches(null));
        assertFalse(r.matches(""));

        r.pattern = "pdf";
        assertFalse(r.matches("foo"));
        assertFalse(r.matches(""));
        assertFalse(r.matches(null));
        assertFalse(r.matches(".pdf"));
        assertFalse(r.matches("pdf"));
        assertFalse(r.matches("pdf.txt"));
        assertFalse(r.matches("foo.pdf.txt"));
        assertFalse(r.matches("file. pdf"));
        assertFalse(r.matches("file.pdf "));

        assertTrue(r.matches("x pdf.pdf"));
        assertTrue(r.matches("x pdf .pDf"));
        assertTrue(r.matches("file.name.PdF"));

        r.pattern = "p d f";
        assertTrue(r.matches("file.p d F"));
    }
}
