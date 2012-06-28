package unit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import play.test.UnitTest;

import box.BoxClientImpl;

import com.google.appengine.repackaged.com.google.common.base.Throwables;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class BoxClientImplTest extends UnitTest {
    public static JsonElement getJson(String file) throws IOException {
        FileReader fr = null;

        try {
            File f = new File("test/unit/" + file);
            fr = new FileReader(f);
            JsonElement ret = new JsonParser().parse(fr);
            return ret;
        } catch (FileNotFoundException e) {
            Throwables.propagate(e);
        } finally {
            if (fr != null) {
                fr.close();
            }
        }

        return null;
    }
    
    @Test
    public void testGetIdOfChild() throws IOException {
        assertEquals("2305649799",
                     BoxClientImpl.getIdOfChild(getJson("BoxClientImplTest.folderResponse.json"),
                                                "testing.html"));
    }
}
