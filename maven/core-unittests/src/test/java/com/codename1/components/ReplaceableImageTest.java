package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ReplaceableImageTest extends UITestBase {

    /**
     * Helper method to create a simple PNG-encoded image for testing.
     * Creates minimal valid PNG data.
     */
    private EncodedImage createTestEncodedImage(int width, int height) throws IOException {
        // Create a simple mutable image and encode it as PNG
        Image img = Image.createImage(width, height, 0xFF0000);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        com.codename1.ui.ImageIO.getImageIO().save(img, baos, com.codename1.ui.ImageIO.FORMAT_PNG, 1.0f);
        byte[] data = baos.toByteArray();
        return EncodedImage.create(data, width, height);
    }

    @FormTest
    void testIsAnimationReturnsTrue() throws IOException {
        EncodedImage placeholder = createTestEncodedImage(50, 50);
        ReplaceableImage img = ReplaceableImage.create(placeholder);
        assertTrue(img.isAnimation());
    }

    @FormTest
    void testGetImageDataReturnsData() throws IOException {
        EncodedImage placeholder = createTestEncodedImage(50, 50);
        ReplaceableImage img = ReplaceableImage.create(placeholder);

        byte[] data = img.getImageData();
        assertNotNull(data);
        assertTrue(data.length > 0);
    }
}
