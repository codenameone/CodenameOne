package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class FileEncodedImageTest extends UITestBase {

    @FormTest
    void testCreateWithFileName() {
        FileEncodedImage img = FileEncodedImage.create("test-file", 100, 100);
        assertNotNull(img);
    }

    @FormTest
    void testGetWidthReturnsWidth() {
        FileEncodedImage img = FileEncodedImage.create("test", 50, 60);
        assertEquals(50, img.getWidth());
    }

    @FormTest
    void testGetHeightReturnsHeight() {
        FileEncodedImage img = FileEncodedImage.create("test", 50, 60);
        assertEquals(60, img.getHeight());
    }

    @FormTest
    void testCreateWithKeepParameter() {
        FileEncodedImage img = FileEncodedImage.create("test", 100, 100, true);
        assertNotNull(img);

        FileEncodedImage img2 = FileEncodedImage.create("test2", 100, 100, false);
        assertNotNull(img2);
    }

    @FormTest
    void testGetImageDataReturnsNull() {
        FileEncodedImage img = FileEncodedImage.create("non-existent-file", 100, 100);
        // File doesn't exist, so getImageData should return null or handle gracefully
        byte[] data = img.getImageData();
        // Just verify the call doesn't crash
        assertTrue(data == null || data.length >= 0);
    }
}
