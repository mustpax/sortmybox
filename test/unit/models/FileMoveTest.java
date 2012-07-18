package unit.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import models.FileMove;
import models.User;
import models.User.AccountType;

import org.joda.time.DateTime;
import org.junit.Test;

import tasks.FileMoveDeleter;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class FileMoveTest extends BaseModelTest {
    private static Key key1() {
        return User.key(AccountType.DROPBOX, 1L);
    }

    private static Key key2() {
        return User.key(AccountType.DROPBOX, 2L);
    }

    @Test
    public void testDml() throws Exception {
        FileMove mv1 = new FileMove(key1(), "foo", "bar", true);
        FileMove mv2 = new FileMove(key2(), "tom", "jerry", false);
        FileMove.save(Arrays.asList(mv1, mv2));
        
        List<FileMove> fileMoves = FileMove.findByOwner(key1(), 2);
        FileMove mv = Iterables.getFirst(fileMoves, null);
        assertEquals("foo", mv.fromFile);
        assertEquals("bar", mv.toDir);
        assertTrue(mv.hasCollision);

        fileMoves = FileMove.findByOwner(key2(), 2);
        mv = Iterables.getFirst(fileMoves, null);
        assertEquals("tom", mv.fromFile);
        assertEquals("jerry", mv.toDir);
        assertFalse(mv.hasCollision);
    }

    @Test
    public void testDeleteBefore() {
        List<FileMove> moves = Lists.newArrayList();
        int count = 10;

        Date now = new Date();
        Date lastDate = null;

        for (int i = 0; i< count; i++) {
            Date when = new DateTime(now).plusDays(i).toDate();
            lastDate = when;
            FileMove m = new FileMove(key1(), "from" + i, "/dest/to" + i, (i % 2) == 0);
            m.when = when;
            moves.add(m);
        }

        FileMove.save(moves);
        assertEquals(10, FileMove.findByOwner(key1(), count + 1).size());

        FileMoveDeleter.deleteMovesBefore(key1(), lastDate);
        assertEquals(1, FileMove.findByOwner(key1(), count + 1).size());
    }

    @Test
    public void testOrdering() {
        List<FileMove> moves = Lists.newArrayList();
        int count = 10;

        // Create moves in increasing chronological order
        for (int i = 0; i< count; i++) {
            Date when = DateTime.now().plusDays(i).toDate();
            FileMove m = new FileMove(key1(), "from" + i, "/dest/to" + i, (i % 2) == 0);
            m.when = when;
            moves.add(m);
        }
        FileMove.save(moves);

        // Datastore fetch should flip the ordering to reverse chrono order
        Collections.reverse(moves);
        assertEquals(moves, FileMove.findByOwner(key1(), count));
    }
    
    /**
     * Verify that FileMoves are queriable via ancestor.
     */
    @Test
    public void testGetByParent() {
        FileMove mv1 = new FileMove(key1(), "foo", "bar", true);
        FileMove mv2 = new FileMove(key1(), "tom", "jerry", false);
        FileMove.save(Arrays.asList(mv1, mv2));
        Query q = new Query(FileMove.KIND).setAncestor(key1());
        assertEquals(2, Iterables.size(DatastoreServiceFactory.getDatastoreService().prepare(q).asIterable()));
    }
    
    /**
     * Ensure that the successful column
     * gets migrated properly
     */
    @Test
    public void testSuccess() {
        FileMove mv = new FileMove(key1(), "foo", "bar", true);
        mv.id = 1L;
        FileMove.save(Arrays.asList(mv));
        
        // ignore successful if hasCollision is set
        setSuccess(mv, true, true);
        mv = FileMove.findByOwner(key1(), 1).get(0);
        assertTrue(mv.hasCollision);

        // read successful if hasCollision is null
        setSuccess(mv, null, false);
        mv = FileMove.findByOwner(key1(), 1).get(0);
        assertTrue(mv.hasCollision);
        
        // read successful if hasCollision is null
        setSuccess(mv, null, true);
        mv = FileMove.findByOwner(key1(), 1).get(0);
        assertFalse(mv.hasCollision);
        
        // default to false if both are null
        setSuccess(mv, null, null);
        mv = FileMove.findByOwner(key1(), 1).get(0);
        assertFalse(mv.hasCollision);
    }
    
    private static void setSuccess(FileMove fm, Boolean hasCollision, Boolean successful) {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        try {
            Entity e = ds.get(FileMove.key(fm.owner, fm.id));
            e.setProperty("successful", successful);
            e.setProperty("hasCollision", hasCollision);
            ds.put(e);
        } catch (EntityNotFoundException e) {
            fail("Could not find record we just created.");
        }
    }
}
