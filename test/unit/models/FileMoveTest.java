package unit.models;

import java.util.Arrays;
import java.util.List;

import models.FileMove;

import org.junit.Test;

import com.google.appengine.repackaged.com.google.common.collect.Iterables;

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
}
