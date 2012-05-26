package unit;

import org.junit.Test;

import play.test.UnitTest;
import dropbox.client.DropboxClientImpl;

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
            assertTrue("Filename should be bad but isn't: " + str,
                        DropboxClientImpl.DISALLOWED_FILENAME_CHARS.matcher(str).find());
        }
        
        for (String str: good) {
            assertFalse("Filename should be good but isn't: " + str,
                        DropboxClientImpl.DISALLOWED_FILENAME_CHARS.matcher(str).find());
        }
    }

}
