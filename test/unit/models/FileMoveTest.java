package unit.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import models.FileMove;

import org.joda.time.DateTime;
import org.junit.Test;

import com.google.appengine.repackaged.com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class FileMoveTest extends BaseModelTest {

    @Test
    public void testDml() throws Exception {
        FileMove mv1 = new FileMove(1L, "foo", "bar", true);
        FileMove mv2 = new FileMove(2L, "tom", "jerry", false);
        FileMove.save(Arrays.asList(mv1, mv2));
        
        List<FileMove> fileMoves = FileMove.findByOwner(1L, 2);
        FileMove mv = Iterables.getFirst(fileMoves, null);
        assertEquals("foo", mv.fromFile);
        assertEquals("bar", mv.toDir);
        assertTrue(mv.successful);

        fileMoves = FileMove.findByOwner(2L, 2);
        mv = Iterables.getFirst(fileMoves, null);
        assertEquals("tom", mv.fromFile);
        assertEquals("jerry", mv.toDir);
        assertFalse(mv.successful);
    }

    @Test
    public void testOrdering() {
        List<FileMove> moves = Lists.newArrayList();
        int count = 10;

        // Create moves in increasing chronological order
        for (int i = 0; i< count; i++) {
            Date when = DateTime.now().plusDays(i).toDate();
            FileMove m = new FileMove(1L, "from" + i, "/dest/to" + i, (i % 2) == 0);
            m.when = when;
            moves.add(m);
        }
        FileMove.save(moves);

        // Datastore fetch should flip the ordering to reverse chrono order
        Collections.reverse(moves);
        assertEquals(moves, FileMove.findByOwner(1L, count));
    }
}
