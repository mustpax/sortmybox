package func;

import java.util.Arrays;
import java.util.Date;
import models.FileMove;
import models.User;
import models.UserStatsUtil;
import models.User.AccountType;
import org.joda.time.DateTime;
import org.junit.Test;
import com.google.appengine.api.datastore.Key;
import unit.models.BaseModelTest;

/**
 * Tests out the uniqueFileMoveCount from the DailyUsageStats table
 * @author mojo
 *
 */
public class DailyUsageStatsTest extends BaseModelTest {
	//TODO: KM refactor this stuff in later
	private static Key key1() {
        return User.key(AccountType.DROPBOX, 1L);
    }

    private static Key key2() {
        return User.key(AccountType.DROPBOX, 2L);
    }
	
	@Test
	public void testUniqueFileMoveUserCount() {		
		//setup date stuff
		DateTime now = DateTime.now();
        Date d2 = now.toDateMidnight().toDate();
        Date d1 = now.minusDays(1).toDateMidnight().toDate();  
				
		//create a file move for a user 1
		FileMove mv1 = new FileMove(key1(), "foo", "bar", false);
		mv1.when = d2;
		
		//create a file move for a user 2 that's too old to be included
		FileMove mv2 = new FileMove(key2(), "foo", "bar", false);
		mv2.when = now.minusDays(3).toDate();
		FileMove.save(Arrays.asList(mv1, mv2));

		int count = UserStatsUtil.countUniqueFileMoveUsers("when", d1, FileMove.all());
		assertEquals(1, count);

		//create a file move for user 2
		FileMove mv3 = new FileMove(key2(), "rick", "james", false);
		FileMove.save(Arrays.asList(mv3));

		int newCount = UserStatsUtil.countUniqueFileMoveUsers("when", d1, FileMove.all());
		assertEquals(2, newCount);
	}
}
