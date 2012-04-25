package unit.models;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import models.FileMove;
import models.Rule;
import models.User;

import org.joda.time.DateTime;
import org.junit.Test;

import play.modules.objectify.Datastore;
import rules.RuleType;
import unit.TestUtil;

import com.google.common.collect.Iterables;

public class FileMoveTest extends BaseModelTest {
    
    private User user;
    private User user2;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        user = TestUtil.createUser(1);
        user2 = TestUtil.createUser(2);
    }
    
    @Test
    public void testDeleteStale() throws Exception {
        Date now = new Date();
        Date yesterday = DateTime.now().minusDays(FileMove.RETENTION_DAYS - 1).toDate();
        Date lastWeek  = DateTime.now().minusDays(FileMove.RETENTION_DAYS + 1).toDate();
        List<FileMove> retained = Arrays.asList(createMove(lastWeek, user2),
	                                            createMove(now),
	                                            createMove(yesterday));
        List<FileMove> deleted = Arrays.asList(createMove(lastWeek));

        // Check that all moves exist
        for (FileMove m: Iterables.concat(retained, deleted)) {
            User user = User.findById(m.owner.getId());
            assertNotNull(FileMove.findById(user.id, m.id));
        }

        FileMove.truncateFileMoves(user.id);

        for (FileMove m: retained) {
            User user = User.findById(m.owner.getId());
            assertNotNull(FileMove.findById(user.id, m.id));
        }
        for (FileMove m: deleted) {
            User user = User.findById(m.owner.getId());
            assertNull(FileMove.findById(user.id, m.id));
        }
    }
    
    public FileMove createMove(Date when, User owner) throws Exception {
        Rule r = new Rule(RuleType.EXT_EQ, "txt", "/txt", 0, owner.id);
        FileMove m = new FileMove(owner.id, r, "foo.txt", true);
        m.when = when;
        Datastore.put(m);
        return m;
    }
        
    public FileMove createMove(Date when) throws Exception {
        return createMove(when, user);
    }
}
