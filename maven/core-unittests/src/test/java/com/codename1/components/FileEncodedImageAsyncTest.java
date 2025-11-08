package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class FileEncodedImageAsyncTest extends UITestBase {

    @FormTest
    void testCreateWithFileName() {
        FileEncodedImageAsync img = FileEncodedImageAsync.create("test-file-async", null, 100, 100);
        assertNotNull(img);
    }

    @FormTest
    void testGetFileNameReturnsFileName() {
        FileEncodedImageAsync img = FileEncodedImageAsync.create("test-file-async", null, 100, 100);
        assertEquals("test-file-async", img.getFileName());
    }

    @FormTest
    void testGetWidthReturnsWidth() {
        FileEncodedImageAsync img = FileEncodedImageAsync.create("test", null, 50, 60);
        assertEquals(50, img.getWidth());
    }

    @FormTest
    void testGetHeightReturnsHeight() {
        FileEncodedImageAsync img = FileEncodedImageAsync.create("test", null, 50, 60);
        assertEquals(60, img.getHeight());
    }

    @FormTest
    void testKeepCacheGetterAndSetter() {
        FileEncodedImageAsync img = FileEncodedImageAsync.create("test", null, 100, 100);
        img.setKeepCache(true);
        assertTrue(img.isKeepCache());

        img.setKeepCache(false);
        assertFalse(img.isKeepCache());
    }
}
