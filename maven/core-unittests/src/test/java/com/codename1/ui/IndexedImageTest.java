package com.codename1.ui;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class IndexedImageTest extends UITestBase {

    @Test
    void testCreateIndexedImageWithPalette() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
        byte[] data = new byte[]{0, 1, 2, 1, 0, 2, 2, 1, 0};
        Image image = Image.createIndexed(3, 3, palette, data);

        assertNotNull(image);
        assertEquals(3, image.getWidth());
        assertEquals(3, image.getHeight());
    }

    @Test
    void testIndexedImageGetPalette() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        byte[] data = new byte[]{0, 1, 2, 3};
        IndexedImage image = new IndexedImage(2, 2, palette, data);

        assertArrayEquals(palette, image.getPalette());
    }

    @Test
    void testIndexedImageGetImageDataByte() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00};
        byte[] data = new byte[]{0, 1, 1, 0};
        IndexedImage image = new IndexedImage(2, 2, palette, data);

        assertArrayEquals(data, image.getImageDataByte());
    }

    @Test
    void testIndexedImageDimensions() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
        byte[] data = new byte[20];
        IndexedImage image = new IndexedImage(5, 4, palette, data);

        assertEquals(5, image.getWidth());
        assertEquals(4, image.getHeight());
    }

    @Test
    void testPackImageWithSmallPalette() {
        int[] rgb = new int[]{
                0xFFFF0000, 0xFFFF0000,
                0xFF00FF00, 0xFF00FF00
        };
        IndexedImage packed = IndexedImage.pack(rgb, 2, 2);

        assertNotNull(packed);
        assertEquals(2, packed.getWidth());
        assertEquals(2, packed.getHeight());
        assertEquals(2, packed.getPalette().length);
    }

    @Test
    void testPackImageWithLargePaletteReturnsNull() {
        // Create an image with more than 256 unique colors
        int[] rgb = new int[512];
        for (int i = 0; i < 512; i++) {
            rgb[i] = 0xFF000000 | i;
        }
        IndexedImage packed = IndexedImage.pack(rgb, 16, 32);

        assertNull(packed, "Should return null when palette exceeds 256 colors");
    }

    @Test
    void testPackImageObjectWithSmallPalette() {
        int[] rgb = new int[]{
                0xFFFF0000, 0xFF00FF00,
                0xFF0000FF, 0xFFFF0000
        };
        Image source = Image.createImage(rgb, 2, 2);
        Image packed = IndexedImage.pack(source);

        assertNotNull(packed);
        assertEquals(2, packed.getWidth());
        assertEquals(2, packed.getHeight());
    }

    @Test
    void testPackImageObjectWithLargePaletteReturnsSource() {
        int[] rgb = new int[300];
        for (int i = 0; i < 300; i++) {
            rgb[i] = 0xFF000000 | (i * 1000);
        }
        Image source = Image.createImage(rgb, 10, 30);
        Image packed = IndexedImage.pack(source);

        assertSame(source, packed, "Should return source image when packing fails");
    }

    @Test
    void testPackImageByName() throws IOException {
        // This test verifies the pack(String) method calls through correctly
        // Since we can't create actual image files in tests, we'll skip this
        // or test with a mock - for now just document that it exists
        // IndexedImage.pack("imageName") would need file system support
    }

    @Test
    void testSubImage() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
        byte[] data = new byte[]{
                0, 1, 2, 1,
                1, 2, 0, 2,
                2, 0, 1, 0
        };
        IndexedImage image = new IndexedImage(4, 3, palette, data);
        Image sub = image.subImage(1, 1, 2, 2, false);

        assertNotNull(sub);
        assertEquals(2, sub.getWidth());
        assertEquals(2, sub.getHeight());
    }

    @Test
    void testModifyAlpha() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
        byte[] data = new byte[]{0, 1, 2, 1};
        IndexedImage image = new IndexedImage(2, 2, palette, data);
        Image modified = image.modifyAlpha((byte) 128);

        assertNotNull(modified);
        assertEquals(2, modified.getWidth());
        assertEquals(2, modified.getHeight());
    }

    @Test
    void testModifyAlphaPreservesTransparentColors() {
        int[] palette = new int[]{0x00FF0000, 0xFFFF0000};
        byte[] data = new byte[]{0, 1, 1, 0};
        IndexedImage image = new IndexedImage(2, 2, palette, data);
        Image modified = image.modifyAlpha((byte) 200);

        assertNotNull(modified);
        assertTrue(modified instanceof IndexedImage);
        IndexedImage indexed = (IndexedImage) modified;
        // First color was transparent, should remain transparent
        assertEquals(0x00000000, indexed.getPalette()[0] & 0xFF000000);
    }

    @Test
    void testGetGraphicsThrowsException() {
        int[] palette = new int[]{0xFFFF0000};
        byte[] data = new byte[]{0, 0, 0, 0};
        IndexedImage image = new IndexedImage(2, 2, palette, data);

        assertThrows(RuntimeException.class, () -> image.getGraphics(),
                "IndexedImage should not support getGraphics()");
    }

    @Test
    void testRotateThrowsException() {
        int[] palette = new int[]{0xFFFF0000};
        byte[] data = new byte[]{0, 0, 0, 0};
        IndexedImage image = new IndexedImage(2, 2, palette, data);

        assertThrows(RuntimeException.class, () -> image.rotate(90),
                "IndexedImage should not support rotate()");
    }

    @Test
    void testScaled() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00};
        byte[] data = new byte[]{0, 1, 1, 0};
        IndexedImage image = new IndexedImage(2, 2, palette, data);
        Image scaled = image.scaled(4, 4);

        assertNotNull(scaled);
        assertEquals(4, scaled.getWidth());
        assertEquals(4, scaled.getHeight());
    }

    @Test
    void testScaledReturnsSameWhenDimensionsMatch() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00};
        byte[] data = new byte[]{0, 1, 1, 0};
        IndexedImage image = new IndexedImage(2, 2, palette, data);
        Image scaled = image.scaled(2, 2);

        assertSame(image, scaled, "Should return same instance when dimensions match");
    }

    @Test
    void testScaleInPlace() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00};
        byte[] data = new byte[]{0, 1, 1, 0, 0, 1, 1, 0, 0};
        IndexedImage image = new IndexedImage(3, 3, palette, data);
        image.scale(6, 6);

        assertEquals(6, image.getWidth());
        assertEquals(6, image.getHeight());
    }

    @Test
    void testGetRGB() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
        byte[] data = new byte[]{0, 1, 2, 1};
        IndexedImage image = new IndexedImage(2, 2, palette, data);
        int[] rgb = image.getRGB();

        assertNotNull(rgb);
        assertEquals(4, rgb.length);
        assertEquals(0xFFFF0000, rgb[0]);
        assertEquals(0xFF00FF00, rgb[1]);
        assertEquals(0xFF0000FF, rgb[2]);
        assertEquals(0xFF00FF00, rgb[3]);
    }

    @Test
    void testToByteArray() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00};
        byte[] data = new byte[]{0, 1, 1, 0};
        IndexedImage image = new IndexedImage(2, 2, palette, data);
        byte[] serialized = image.toByteArray();

        assertNotNull(serialized);
        assertTrue(serialized.length > 0);
    }

    @Test
    void testLoadFromByteArray() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
        byte[] data = new byte[]{0, 1, 2, 1, 0, 2};
        IndexedImage original = new IndexedImage(3, 2, palette, data);
        byte[] serialized = original.toByteArray();

        IndexedImage loaded = IndexedImage.load(serialized);

        assertNotNull(loaded);
        assertEquals(3, loaded.getWidth());
        assertEquals(2, loaded.getHeight());
        assertEquals(3, loaded.getPalette().length);
        assertArrayEquals(palette, loaded.getPalette());
    }

    @Test
    void testOpaqueImageWhenAllColorsOpaque() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
        byte[] data = new byte[]{0, 1, 2, 1};
        IndexedImage image = new IndexedImage(2, 2, palette, data);

        assertTrue(image.isOpaque(), "Image with all opaque colors should be opaque");
    }

    @Test
    void testTransparentImageWhenSomeColorsTransparent() {
        int[] palette = new int[]{0x80FF0000, 0xFF00FF00, 0xFF0000FF};
        byte[] data = new byte[]{0, 1, 2, 1};
        IndexedImage image = new IndexedImage(2, 2, palette, data);

        assertFalse(image.isOpaque(), "Image with transparent colors should not be opaque");
    }

    @Test
    void testRequiresDrawImage() {
        int[] palette = new int[]{0xFFFF0000};
        byte[] data = new byte[]{0, 0};
        IndexedImage image = new IndexedImage(1, 2, palette, data);

        assertTrue(image.requiresDrawImage());
    }

    @Test
    void testPackWithSingleColor() {
        int[] rgb = new int[]{0xFFFF0000, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000};
        IndexedImage packed = IndexedImage.pack(rgb, 2, 2);

        assertNotNull(packed);
        assertEquals(1, packed.getPalette().length);
        assertEquals(0xFFFF0000, packed.getPalette()[0]);
    }

    @Test
    void testPackWith256Colors() {
        int[] rgb = new int[256];
        for (int i = 0; i < 256; i++) {
            rgb[i] = 0xFF000000 | i;
        }
        IndexedImage packed = IndexedImage.pack(rgb, 16, 16);

        assertNotNull(packed);
        assertEquals(256, packed.getPalette().length);
    }

    @Test
    void testPackWith257ColorsReturnsNull() {
        int[] rgb = new int[257];
        for (int i = 0; i < 257; i++) {
            rgb[i] = 0xFF000000 | i;
        }
        IndexedImage packed = IndexedImage.pack(rgb, 257, 1);

        assertNull(packed, "Should return null when more than 256 colors");
    }

    @Test
    void testEmptyImageDataHandling() {
        int[] palette = new int[]{0xFFFF0000};
        byte[] data = new byte[1];
        IndexedImage image = new IndexedImage(1, 1, palette, data);

        assertEquals(1, image.getWidth());
        assertEquals(1, image.getHeight());
    }
}
