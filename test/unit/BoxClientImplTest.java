package unit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import play.test.UnitTest;

import box.BoxClientImpl;
import box.gson.BoxItem;

import com.google.appengine.repackaged.com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class BoxClientImplTest extends UnitTest {
    public static BoxItem getJson(String file) throws IOException {
        FileReader fr = null;

        try {
            File f = new File("test/unit/" + file);
            fr = new FileReader(f);
            JsonElement ret = new JsonParser().parse(fr);
            return new Gson().fromJson(ret, BoxItem.class);
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
                     BoxClientImpl.getChild(getJson("BoxClientImplTest.folderResponse.json"),
                                                "testing.html").id);
        assertEquals("2305649799",
                     BoxClientImpl.getChild(getJson("BoxClientImplTest.folderResponse.json"),
                                                "TESTING.html").id);
    }
}
