package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.EncodedImage;

import static org.junit.jupiter.api.Assertions.*;

class ReplaceableImageTest extends UITestBase {

    @FormTest
    void testCreateWithEncodedImage() {
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        EncodedImage placeholder = EncodedImage.create(data, 50, 50, true);
        ReplaceableImage img = ReplaceableImage.create(placeholder);
        assertNotNull(img);
        assertEquals(50, img.getWidth());
        assertEquals(50, img.getHeight());
    }

    @FormTest
    void testIsAnimationReturnsTrue() {
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        EncodedImage placeholder = EncodedImage.create(data, 50, 50, true);
        ReplaceableImage img = ReplaceableImage.create(placeholder);
        assertTrue(img.isAnimation());
    }

    @FormTest
    void testGetImageDataReturnsData() {
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        EncodedImage placeholder = EncodedImage.create(data, 50, 50, true);
        ReplaceableImage img = ReplaceableImage.create(placeholder);

        byte[] imageData = img.getImageData();
        assertNotNull(imageData);
        assertTrue(imageData.length > 0);
    }

    @FormTest
    void testReplaceUpdatesImage() {
        byte[] data1 = new byte[]{1, 2, 3, 4, 5};
        EncodedImage placeholder = EncodedImage.create(data1, 50, 50, true);
        ReplaceableImage img = ReplaceableImage.create(placeholder);

        byte[] data2 = new byte[]{6, 7, 8, 9, 10};
        EncodedImage newImage = EncodedImage.create(data2, 50, 50, true);
        img.replace(newImage);

        assertEquals(50, img.getWidth());
        assertEquals(50, img.getHeight());
    }

    @FormTest
    void testAnimateAfterReplace() {
        byte[] data1 = new byte[]{1, 2, 3, 4, 5};
        EncodedImage placeholder = EncodedImage.create(data1, 50, 50, true);
        ReplaceableImage img = ReplaceableImage.create(placeholder);

        byte[] data2 = new byte[]{6, 7, 8, 9, 10};
        EncodedImage newImage = EncodedImage.create(data2, 50, 50, true);
        img.replace(newImage);

        assertTrue(img.animate());
    }

    @FormTest
    void testIsOpaqueMatchesPlaceholder() {
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        EncodedImage placeholder = EncodedImage.create(data, 50, 50, true);
        ReplaceableImage img = ReplaceableImage.create(placeholder);
        assertTrue(img.isOpaque());

        byte[] data2 = new byte[]{6, 7, 8, 9, 10};
        EncodedImage placeholder2 = EncodedImage.create(data2, 50, 50, false);
        ReplaceableImage img2 = ReplaceableImage.create(placeholder2);
        assertFalse(img2.isOpaque());
    }
}
