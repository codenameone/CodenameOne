package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class StorageImageTest extends UITestBase {

    @FormTest
    void testCreateWithByteArray() {
        byte[] data = new byte[100];
        StorageImage img = StorageImage.create("test-image", data, 100, 100);
        assertNotNull(img);
    }

    @FormTest
    void testGetWidthReturnsWidth() {
        byte[] data = new byte[100];
        StorageImage img = StorageImage.create("test", data, 50, 60);
        assertEquals(50, img.getWidth());
    }

    @FormTest
    void testGetHeightReturnsHeight() {
        byte[] data = new byte[100];
        StorageImage img = StorageImage.create("test", data, 50, 60);
        assertEquals(60, img.getHeight());
    }

    @FormTest
    void testCreateWithKeepParameter() {
        byte[] data = new byte[100];
        StorageImage img = StorageImage.create("test", data, 100, 100, true);
        assertNotNull(img);

        StorageImage img2 = StorageImage.create("test2", data, 100, 100, false);
        assertNotNull(img2);
    }

    @FormTest
    void testCreateWithFilenameOnly() {
        StorageImage img = StorageImage.create("test-file", 100, 100);
        assertNotNull(img);
    }

    @FormTest
    void testGetImageDataReturnsData() {
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        StorageImage img = StorageImage.create("test-data", data, 10, 10);
        byte[] retrieved = img.getImageData();
        assertNotNull(retrieved);
    }
}
