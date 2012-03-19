package unit.models;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import models.FileMove;
import models.Rule;
import models.Rule.RuleType;
import models.User;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;

import play.Logger;
import play.modules.siena.SienaFixtures;
import play.test.UnitTest;

public class FileMoveTest extends UnitTest {
    private static final long USER_ID = 1L;
    
    @BeforeClass
    public static void loadFixtures() throws Exception {
        SienaFixtures.deleteAllModels();
    }

    @Test
    public void testDeleteStale() {
        Date now = new Date();
        Date yesterday = DateTime.now().minusDays(FileMove.RETENTION_DAYS - 1).toDate();
        Date lastWeek  = DateTime.now().minusDays(FileMove.RETENTION_DAYS + 1).toDate();
        List<FileMove> retained = Arrays.asList(createMove(lastWeek, 2L),
	                                            createMove(now),
	                                            createMove(yesterday));
        List<FileMove> deleted = Arrays.asList(createMove(lastWeek));
        
        // Check that all moves exist
        for (FileMove m: Iterables.concat(retained, deleted)) {
            assertNotNull(m.findById(m.id));
        }
        Logger.info("count %d", FileMove.all().count());

        // Delete stale then check again
        assertTrue(FileMove.deleteStaleForUser(USER_ID) > 0);

        Logger.info("count %d", FileMove.all().count());
        for (FileMove m: retained) {
            assertNotNull(m.findById(m.id));
        }
        for (FileMove m: deleted) {
            assertNull(m.findById(m.id));
        }
    }
    
    public static FileMove createMove(Date when, long owner) {
        Rule r = new Rule(RuleType.EXT_EQ, "txt", "/txt", 0, owner);
        FileMove m = new FileMove(r, "foo.txt", true);
        m.when = when;
        m.insert();
        return m;
    }
        
    public static FileMove createMove(Date when) {
        return createMove(when, USER_ID);
    }
}
