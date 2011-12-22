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
}

