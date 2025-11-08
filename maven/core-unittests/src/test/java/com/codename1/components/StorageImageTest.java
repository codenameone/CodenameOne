package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class StorageImageTest extends UITestBase {

    @FormTest
    void testCreateWithStorageName() {
        StorageImage img = StorageImage.create("test-image", null, 100, 100);
        assertNotNull(img);
    }

    @FormTest
    void testGetFileNameReturnsFileName() {
        StorageImage img = StorageImage.create("test-image", null, 100, 100);
        assertEquals("test-image", img.getFileName());
    }

    @FormTest
    void testGetWidthReturnsWidth() {
        StorageImage img = StorageImage.create("test", null, 50, 60);
        assertEquals(50, img.getWidth());
    }

    @FormTest
    void testGetHeightReturnsHeight() {
        StorageImage img = StorageImage.create("test", null, 50, 60);
        assertEquals(60, img.getHeight());
    }

    @FormTest
    void testKeepCacheGetterAndSetter() {
        StorageImage img = StorageImage.create("test", null, 100, 100);
        img.setKeepCache(true);
        assertTrue(img.isKeepCache());

        img.setKeepCache(false);
        assertFalse(img.isKeepCache());
    }
}
