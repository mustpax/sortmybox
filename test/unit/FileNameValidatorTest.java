package unit;

import org.junit.Test;

import play.test.UnitTest;
import dropbox.Dropbox;

/**
 * Verifies that file names are validated properly
 * @author mustpax
 */
public class FileNameValidatorTest extends UnitTest {
    @Test
    public void testFileMovePattern() {
        String[] bad = { "f/oo:", "foo\\", "bar*", "   ?", "sd<", ">", "\"", "|"};
        String[] good = {"/a:/ b $#@", "a", "", null};
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
