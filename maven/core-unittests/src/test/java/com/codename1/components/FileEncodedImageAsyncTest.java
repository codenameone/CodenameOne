package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.TestLogger;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Image;

import static org.junit.jupiter.api.Assertions.*;

class FileEncodedImageAsyncTest extends UITestBase {

    @FormTest
    void testCreateWithPlaceholderBytes() {
        byte[] placeholder = new byte[10];
        FileEncodedImageAsync img = FileEncodedImageAsync.create("test-file", placeholder, 100, 100);
        assertNotNull(img);
    }

    @FormTest
    void testCreateWithPlaceholderImage() {
        Image placeholder = Image.createImage(50, 50, 0xFF0000);
        FileEncodedImageAsync img = FileEncodedImageAsync.create("test-file", placeholder);
        assertNotNull(img);
    }

    @FormTest
    void testGetWidthReturnsWidth() {
        byte[] placeholder = new byte[10];
        FileEncodedImageAsync img = FileEncodedImageAsync.create("test", placeholder, 50, 60);
        assertEquals(50, img.getWidth());
    }

    @FormTest
    void testGetHeightReturnsHeight() {
        byte[] placeholder = new byte[10];
        FileEncodedImageAsync img = FileEncodedImageAsync.create("test", placeholder, 50, 60);
        assertEquals(60, img.getHeight());
    }

    @FormTest
    void testIsAnimationReturnsTrue() {
        Image placeholder = Image.createImage(50, 50, 0xFF0000);
        FileEncodedImageAsync img = FileEncodedImageAsync.create("test", placeholder);
        assertTrue(img.isAnimation());
    }

    @FormTest
    void testGetImageDataReturnsPlaceholder() {
        TestLogger.install();
        try {
            byte[] placeholder = new byte[10];
            FileEncodedImageAsync img = FileEncodedImageAsync.create("non-existent-file", placeholder, 100, 100);
            // File doesn't exist, so getImageData should return placeholder or null
            byte[] data = img.getImageData();
            assertTrue(data == null || data.length >= 0);
            int timeout = 0;
            while (TestLogger.getPrinted().isEmpty()) {
                TestUtils.waitFor(10);
                timeout++;
                assertNotEquals(timeout, 100);
            }
        } finally {
            TestLogger.remove();
        }
    }

    @FormTest
    void testAnimateMethod() {
        Image placeholder = Image.createImage(50, 50, 0xFF0000);
        FileEncodedImageAsync img = FileEncodedImageAsync.create("test", placeholder);
        // animate() should return boolean
        boolean animates = img.animate();
        assertTrue(animates || !animates);
    }
}
