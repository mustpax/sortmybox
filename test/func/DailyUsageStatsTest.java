package func;

import java.util.Arrays;
import java.util.Date;

import models.FileMove;
import models.UserStatsUtil;

import org.joda.time.DateTime;
import org.junit.Test;

import cron.DailyUsageStatsGatherer;

import unit.models.BaseModelTest;

/**
 * Tests out the uniqueFileMoveCount from the DailyUsageStats table
 * @author mojo
 *
 */
public class DailyUsageStatsTest extends BaseModelTest {
	
	@Test
	public void testUniqueFileMoveUserCount() {		
		//setup date stuff
		DateTime now = DateTime.now();
        Date d2 = now.toDateMidnight().toDate();
        Date d1 = now.minusDays(1).toDateMidnight().toDate();  
				
		//create a file move for a user 1
		FileMove mv1 = new FileMove(1L, "foo", "bar", false);
		mv1.when = d2;
		
		//create a file move for a user 2
		FileMove mv2 = new FileMove(2L, "tom", "jerry", false);
		mv2.when = d2;
		FileMove.save(Arrays.asList(mv1, mv2));
		
		//now get the user count
		int count = UserStatsUtil.countUniqueFileMoveUsers("when", d1, d2, FileMove.all());
		assertEquals(2,count);
		//create a second file move for user 1
		FileMove mv3 = new FileMove(1L, "rick", "james", false);
		FileMove.save(Arrays.asList(mv3));

		//now get the user count
		int newCount = UserStatsUtil.countUniqueFileMoveUsers("when", d1, d2, FileMove.all());
		assertEquals(2,newCount);
	}

	
	
}
