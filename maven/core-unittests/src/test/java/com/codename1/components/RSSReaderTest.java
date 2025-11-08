package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class RSSReaderTest extends UITestBase {

    @FormTest
    void testDefaultConstructor() {
        RSSReader reader = new RSSReader();
        assertNotNull(reader);
    }

    @FormTest
    void testSetURLUpdatesURL() {
        RSSReader reader = new RSSReader();
        reader.setURL("https://example.com/feed.xml");
        assertEquals("https://example.com/feed.xml", reader.getURL());
    }

    @FormTest
    void testLimitGetterAndSetter() {
        RSSReader reader = new RSSReader();
        reader.setLimit(10);
        assertEquals(10, reader.getLimit());
    }

    @FormTest
    void testIconPlaceholderGetterAndSetter() {
        RSSReader reader = new RSSReader();
        com.codename1.ui.Image placeholder = com.codename1.ui.Image.createImage(20, 20, 0xFF0000);
        reader.setIconPlaceholder(placeholder);
        assertSame(placeholder, reader.getIconPlaceholder());
    }

    @FormTest
    void testGetProgressPercentage() {
        RSSReader reader = new RSSReader();
        int progress = reader.getProgressPercentage();
        assertTrue(progress >= 0 && progress <= 100);
    }
}
