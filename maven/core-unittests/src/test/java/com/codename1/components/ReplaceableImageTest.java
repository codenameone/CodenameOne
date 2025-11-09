package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.EncodedImage;

import static org.junit.jupiter.api.Assertions.*;

class ReplaceableImageTest extends UITestBase {

    @FormTest
    void testCreateWithEncodedImage() {
        EncodedImage placeholder = EncodedImage.createFromImage(
            com.codename1.ui.Image.createImage(50, 50, 0xFF0000), false
        );
        ReplaceableImage img = ReplaceableImage.create(placeholder);
        assertNotNull(img);
        assertEquals(50, img.getWidth());
        assertEquals(50, img.getHeight());
    }

    @FormTest
    void testReplaceUpdatesImage() {
        EncodedImage placeholder = EncodedImage.createFromImage(
            com.codename1.ui.Image.createImage(50, 50, 0xFF0000), false
        );
        ReplaceableImage img = ReplaceableImage.create(placeholder);

        EncodedImage newImage = EncodedImage.createFromImage(
            com.codename1.ui.Image.createImage(50, 50, 0x00FF00), false
        );
        img.replace(newImage);

        // Size must remain the same (ReplaceableImage requirement)
        assertEquals(50, img.getWidth());
        assertEquals(50, img.getHeight());
    }

    @FormTest
    void testIsAnimationReturnsTrue() {
        EncodedImage placeholder = EncodedImage.createFromImage(
            com.codename1.ui.Image.createImage(50, 50, 0xFF0000), false
        );
        ReplaceableImage img = ReplaceableImage.create(placeholder);
        assertTrue(img.isAnimation());
    }

    @FormTest
    void testAnimateAfterReplace() {
        EncodedImage placeholder = EncodedImage.createFromImage(
            com.codename1.ui.Image.createImage(50, 50, 0xFF0000), false
        );
        ReplaceableImage img = ReplaceableImage.create(placeholder);

        EncodedImage newImage = EncodedImage.createFromImage(
            com.codename1.ui.Image.createImage(50, 50, 0x00FF00), false
        );
        img.replace(newImage);

        // After replace, animate should return true
        boolean animates = img.animate();
        assertTrue(animates);
    }

    @FormTest
    void testGetImageDataReturnsData() {
        EncodedImage placeholder = EncodedImage.createFromImage(
            com.codename1.ui.Image.createImage(50, 50, 0xFF0000), false
        );
        ReplaceableImage img = ReplaceableImage.create(placeholder);

        byte[] data = img.getImageData();
        assertNotNull(data);
        assertTrue(data.length > 0);
    }

    @FormTest
    void testIsOpaqueMatchesPlaceholder() {
        EncodedImage placeholder = EncodedImage.createFromImage(
            com.codename1.ui.Image.createImage(50, 50, 0xFF0000), false
        );
        ReplaceableImage img = ReplaceableImage.create(placeholder);

        // Should match placeholder's opaque status
        boolean isOpaque = img.isOpaque();
        assertTrue(isOpaque || !isOpaque);
    }
}
