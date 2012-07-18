package unit.common.request;

import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.junit.Test;

import play.mvc.Http;
import play.test.UnitTest;

import com.google.appengine.repackaged.com.google.common.collect.Maps;
import common.request.Headers;

/**
 * Unit test for {@link Headers}.
 * 
 * @author syyang
 */
public class HeadersTest extends UnitTest {

    @Test
    public void testFirst() {
        final String KEY = "foo";
        final String VALUE = "bar";
        
        Http.Request request = mock(Http.Request.class);
        request.headers = Maps.newHashMap();
        
        assertNull(Headers.first(request, KEY));
        
        try {
            Headers.first(request, KEY, false);
            fail("should fail when allowNull=false");
        } catch (NullPointerException e) {
            // expected
        }
        
        Http.Header header = new Http.Header();
        header.values = Arrays.asList(VALUE);
        request.headers.put(KEY, header);
        assertEquals(VALUE, Headers.first(request, KEY));
    }
}
