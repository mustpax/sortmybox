package unit;

import org.junit.Test;

import play.test.UnitTest;
import dropbox.Dropbox;

/**
 * 
 * @author mustpax
 */
public class FileMovePatternTest extends UnitTest {
    @Test
    public void testFileMovePattern() {
        String[] bad = { "foo:", "foo\\", "bar*", "   ?", "sd<", ">", "\"", "|"};
        String[] good = {"/a / b $#@", "a"};
        for (String str: bad) {
            assertFalse("Filename should be bad but isn't: " + str,
                        Dropbox.isValidFilename(str));
        }
        
        for (String str: good) {
            assertTrue("Filename should be good but isn't: " + str,
                        Dropbox.isValidFilename(str));
        }
    }

}
