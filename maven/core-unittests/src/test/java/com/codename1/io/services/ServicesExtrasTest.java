package com.codename1.io.services;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class ServicesExtrasTest extends UITestBase {

    @FormTest
    void testRSSServiceParsing() throws Exception {
        RSSService rss = new RSSService("http://rss.example.com");
        String xml = "<rss><channel><item><title>Title</title><description>Desc</description></item></channel></rss>";

        // Invoke readResponse via reflection as it is protected
        Method readResponse = RSSService.class.getDeclaredMethod("readResponse", java.io.InputStream.class);
        readResponse.setAccessible(true);
        readResponse.invoke(rss, new ByteArrayInputStream(xml.getBytes("UTF-8")));

        assertTrue(rss.getResults().size() > 0);
        java.util.Hashtable h = (java.util.Hashtable) rss.getResults().get(0);
        assertEquals("Title", h.get("title"));
    }

    @FormTest
    void testTwitterRESTServiceParsing() throws Exception {
        TwitterRESTService twitter = new TwitterRESTService(TwitterRESTService.METHOD_USER_TIMELINE);
        String json = "{\"statuses\":[{\"id_str\":\"123\", \"text\":\"Tweet\"}]}";

        Method readResponse = TwitterRESTService.class.getDeclaredMethod("readResponse", java.io.InputStream.class);
        readResponse.setAccessible(true);
        readResponse.invoke(twitter, new ByteArrayInputStream(json.getBytes("UTF-8")));

        assertEquals(1, twitter.getStatusesCount());
        assertEquals("123", twitter.getIdStr());
        assertEquals("Tweet", twitter.getStatus(0).get("text"));
    }

    @FormTest
    void testImageDownloadService() {
        // ImageDownloadService.createImageToStorage(url, callback, cacheId)
        ImageDownloadService.createImageToStorage("http://example.com/img.png", (evt) -> {}, "cache_id");
    }
}
