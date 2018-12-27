package org.rrd4j.core;

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

public class UriTest {

    private void test(String Uri, String scheme, String ssp, String authority, String path) {
        URI generated = RrdBackendFactory.buildGenericUri(Uri);
        Assert.assertEquals("scheme mismatch", scheme, generated.getScheme());
        if (ssp != null) {
            Assert.assertEquals("scheme specific part mismatch", ssp, generated.getSchemeSpecificPart());
        }
        Assert.assertEquals("authority part mismatch", authority, generated.getAuthority());
        if (path != null) {
            Assert.assertTrue("path mismatch: " + generated.getPath(), generated.getPath().endsWith(path));
        }
    }

    @Test
    public void buildURI() {
        test("mailto:java-net@java.sun.com", "mailto", "java-net@java.sun.com", null, null);
        test("news:comp.lang.java", "news", "comp.lang.java", null, null);
        test("http://java.sun.com/j2se/1.3/?truc=machin#24", "http", null, "java.sun.com", "/j2se/1.3/");
        test("http://java.sun.com/j2se/1.3/?q=lang#24", "http", null, "java.sun.com", "/j2se/1.3/");
        test("docs/guide/collections/designfaq.html#28", null, null, null, "docs/guide/collections/designfaq.html");
        test("../../../demo/jfc/SwingSet2/src/SwingSet2.java", null, null, null, "../../../demo/jfc/SwingSet2/src/SwingSet2.java");
        test("file:///~/calendar", "file", null, null, "/~/calendar");
        test("file:/c:/rrd4j/test.rrd", "file", null, null, "/c:/rrd4j/test.rrd");
        test("c:/rrd4j folder/test.rrd", "file", null, null, "/c:/rrd4j folder/test.rrd");
    }

}
