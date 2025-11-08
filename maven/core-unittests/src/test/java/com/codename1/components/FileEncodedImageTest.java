package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class FileEncodedImageTest extends UITestBase {

    @FormTest
    void testCreateWithFileName() {
        FileEncodedImage img = FileEncodedImage.create("test-file", null, 100, 100);
        assertNotNull(img);
    }

    @FormTest
    void testGetFileNameReturnsFileName() {
        FileEncodedImage img = FileEncodedImage.create("test-file", null, 100, 100);
        assertEquals("test-file", img.getFileName());
    }

    @FormTest
    void testGetWidthReturnsWidth() {
        FileEncodedImage img = FileEncodedImage.create("test", null, 50, 60);
        assertEquals(50, img.getWidth());
    }

    @FormTest
    void testGetHeightReturnsHeight() {
        FileEncodedImage img = FileEncodedImage.create("test", null, 50, 60);
        assertEquals(60, img.getHeight());
    }

    @FormTest
    void testKeepCacheGetterAndSetter() {
        FileEncodedImage img = FileEncodedImage.create("test", null, 100, 100);
        img.setKeepCache(true);
        assertTrue(img.isKeepCache());

        img.setKeepCache(false);
        assertFalse(img.isKeepCache());
    }
}
