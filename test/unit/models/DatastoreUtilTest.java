package unit.models;

import java.util.Arrays;
import java.util.Date;

import models.DailyUsageStats;
import models.DatastoreUtil;
import models.UserStatsUtil;

import org.joda.time.DateTime;
import org.junit.Test;

/**
 * Tests for {@link DatastoreUtil}
 * 
 * @author syyang
 */
public class DatastoreUtilTest extends BaseModelTest {

    @Test
    public void testCountOverDateRange() {
        DateTime now = DateTime.now();
        Date d3 = now.toDateMidnight().toDate();
        Date d2 = now.minusDays(1).toDateMidnight().toDate();  
        Date d1 = now.minusDays(2).toDateMidnight().toDate();  

        DailyUsageStats s1 = new DailyUsageStats(3L, 2L, 4L, 4L, d1);
        DailyUsageStats s2 = new DailyUsageStats(3L, 2L, 4L, 4L, d2);
        DailyUsageStats s3 = new DailyUsageStats(3L, 2L, 4L, 4L, d3);

        DatastoreUtil.put(Arrays.asList(s1, s2, s3), DailyUsageStats.MAPPER);
        
        assertEquals(3, DatastoreUtil.count("created", d1, d3, DailyUsageStats.all()));
        assertEquals(2, DatastoreUtil.count("created", d2, d3, DailyUsageStats.all()));
        assertEquals(1, DatastoreUtil.count("created", d3, d3, DailyUsageStats.all()));
    }

}
