package unit.models;

import java.util.List;

import models.Rule;
import models.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.test.UnitTest;
import rules.RuleType;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.Lists;
 
public class RuleTest extends UnitTest {
    private final LocalServiceTestHelper helper =
        new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    @Before
    public void setUp() {
        helper.setUp();
    }

    @After
    public void tearDown() {
        helper.tearDown();
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

    @Test
    public void testReplace() {
        User u = UserTest.newUser();
        List<Rule> rules = Lists.newArrayList();
        rules.add(new Rule(RuleType.EXT_EQ, "pdf", "/pdf", 0, u.id));
        rules.add(new Rule(RuleType.GLOB, "a*", "/a", 1, u.id));
        rules.add(new Rule(RuleType.NAME_CONTAINS, "foo", "/foo", 2, u.id));
        Rule.replace(u, rules, null);
        List<Rule> actual = Rule.findByUserId(u.id);
        assertEquals(actual, rules);
        
        rules.remove(rules.size() - 1);
        rules.set(1, new Rule(RuleType.GLOB, "b*", "/b", 3, u.id));
        Rule.replace(u, rules, null);
        actual = Rule.findByUserId(u.id);
        assertEquals(actual, rules);
    }
}
