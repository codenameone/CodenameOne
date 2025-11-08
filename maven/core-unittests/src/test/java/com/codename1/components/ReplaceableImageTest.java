package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;

import static org.junit.jupiter.api.Assertions.*;

class ReplaceableImageTest extends UITestBase {

    @FormTest
    void testConstructorWithImage() {
        Image placeholder = Image.createImage(50, 50, 0xFF0000);
        ReplaceableImage img = new ReplaceableImage(placeholder);
        assertNotNull(img);
        assertEquals(50, img.getWidth());
        assertEquals(50, img.getHeight());
    }

    @FormTest
    void testReplaceUpdatesImage() {
        Image placeholder = Image.createImage(50, 50, 0xFF0000);
        ReplaceableImage img = new ReplaceableImage(placeholder);

        Image newImage = Image.createImage(60, 70, 0x00FF00);
        img.replace(newImage);

        assertEquals(60, img.getWidth());
        assertEquals(70, img.getHeight());
    }

    @FormTest
    void testLockAndUnlock() {
        Image placeholder = Image.createImage(50, 50, 0xFF0000);
        ReplaceableImage img = new ReplaceableImage(placeholder);

        img.lock();
        assertTrue(img.isLocked());

        img.unlock();
        assertFalse(img.isLocked());
    }

    @FormTest
    void testReplaceWhenLockedDoesNotReplace() {
        Image placeholder = Image.createImage(50, 50, 0xFF0000);
        ReplaceableImage img = new ReplaceableImage(placeholder);

        img.lock();
        Image newImage = Image.createImage(60, 70, 0x00FF00);
        img.replace(newImage);

        // Should still be locked and original size
        assertEquals(50, img.getWidth());
        assertEquals(50, img.getHeight());
        assertTrue(img.isLocked());
    }

    @FormTest
    void testDisposeClearsImage() {
        Image placeholder = Image.createImage(50, 50, 0xFF0000);
        ReplaceableImage img = new ReplaceableImage(placeholder);
        img.dispose();
        // Disposal should not throw exception
        assertNotNull(img);
    }

    @FormTest
    void testIsAnimationReturnsFalse() {
        Image placeholder = Image.createImage(50, 50, 0xFF0000);
        ReplaceableImage img = new ReplaceableImage(placeholder);
        assertFalse(img.isAnimation());
    }
}
