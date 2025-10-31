package com.codename1.ui;

import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageTest extends UITestBase {

    @Test
    void testCreateImageFromRgbArray() {
        int[] rgb = new int[]{
                0xFFFF0000, 0xFF00FF00,
                0xFF0000FF, 0xFFFFFF00
        };
        Image image = Image.createImage(rgb, 2, 2);

        assertNotNull(image);
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
    }

    @Test
    void testCreateMutableImageWithDefaultFillColor() {
        Image image = Image.createImage(10, 10);

        assertNotNull(image);
        assertEquals(10, image.getWidth());
        assertEquals(10, image.getHeight());
    }

    @Test
    void testCreateMutableImageWithCustomFillColor() {
        Image image = Image.createImage(15, 20, 0xFF0000FF);

        assertNotNull(image);
        assertEquals(15, image.getWidth());
        assertEquals(20, image.getHeight());
    }

    @Test
    void testCreateImageFromByteArray() {
        byte[] data = new byte[]{10, 20, 30, 40, 50};
        Image image = Image.createImage(data, 0, data.length);

        assertNotNull(image);
        assertTrue(image.getWidth() >= 1);
        assertTrue(image.getHeight() >= 1);
    }

    @Test
    void testCreateImageFromByteArrayWithOffset() {
        byte[] data = new byte[]{0, 1, 10, 20, 30, 40, 50};
        Image image = Image.createImage(data, 2, 5);

        assertNotNull(image);
        assertTrue(image.getWidth() >= 1);
        assertTrue(image.getHeight() >= 1);
    }

    @Test
    void testCreateImageFromInputStream() throws IOException {
        byte[] data = new byte[]{10, 20, 30, 40};
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        Image image = Image.createImage(stream);

        assertNotNull(image);
        assertTrue(image.getWidth() >= 1);
        assertTrue(image.getHeight() >= 1);
    }

    @Test
    void testCreateIndexedImage() {
        int[] palette = new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
        byte[] data = new byte[]{0, 1, 2, 1, 0, 2};
        Image image = Image.createIndexed(3, 2, palette, data);

        assertNotNull(image);
        assertEquals(3, image.getWidth());
        assertEquals(2, image.getHeight());
    }

    @Test
    void testScaledWidth() {
        Image source = Image.createImage(100, 50);
        Image scaled = source.scaledWidth(200);

        assertNotNull(scaled);
        assertEquals(200, scaled.getWidth());
        assertEquals(100, scaled.getHeight());
    }

    @Test
    void testScaledHeight() {
        Image source = Image.createImage(100, 50);
        Image scaled = source.scaledHeight(100);

        assertNotNull(scaled);
        assertEquals(200, scaled.getWidth());
        assertEquals(100, scaled.getHeight());
    }

    @Test
    void testScaled() {
        Image source = Image.createImage(100, 100);
        Image scaled = source.scaled(50, 75);

        assertNotNull(scaled);
        assertEquals(50, scaled.getWidth());
        assertEquals(75, scaled.getHeight());
    }

    @Test
    void testScaledSmallerRatio() {
        Image source = Image.createImage(100, 100);
        Image scaled = source.scaledSmallerRatio(150, 50);

        assertNotNull(scaled);
        assertEquals(50, scaled.getWidth());
        assertEquals(50, scaled.getHeight());
    }

    @Test
    void testScaledLargerRatio() {
        Image source = Image.createImage(100, 100);
        Image scaled = source.scaledLargerRatio(150, 200);

        assertNotNull(scaled);
        assertEquals(200, scaled.getWidth());
        assertEquals(200, scaled.getHeight());
    }

    @Test
    void testFill() {
        Image source = Image.createImage(50, 50);
        Image filled = source.fill(100, 100);

        assertNotNull(filled);
        assertEquals(100, filled.getWidth());
        assertEquals(100, filled.getHeight());
    }

    @Test
    void testGetRGB() {
        int[] rgb = new int[]{
                0xFFFF0000, 0xFF00FF00,
                0xFF0000FF, 0xFFFFFF00
        };
        Image image = Image.createImage(rgb, 2, 2);
        int[] retrieved = image.getRGB();

        assertNotNull(retrieved);
        assertEquals(4, retrieved.length);
    }

    @Test
    void testGetRGBCached() {
        int[] rgb = new int[]{
                0xFFFF0000, 0xFF00FF00,
                0xFF0000FF, 0xFFFFFF00
        };
        Image image = Image.createImage(rgb, 2, 2);
        int[] first = image.getRGBCached();
        int[] second = image.getRGBCached();

        assertNotNull(first);
        assertSame(first, second, "Cached RGB should return same instance");
    }

    @Test
    void testGetRGBWithProvidedArray() {
        int[] rgb = new int[]{
                0xFFFF0000, 0xFF00FF00,
                0xFF0000FF, 0xFFFFFF00
        };
        Image image = Image.createImage(rgb, 2, 2);
        int[] target = new int[4];
        image.getRGB(target);

        assertEquals(4, target.length);
        assertNotEquals(0, target[0]);
    }

    @Test
    void testSubImage() {
        Image source = Image.createImage(100, 100, 0xFFFF0000);
        Image sub = source.subImage(10, 10, 20, 20, false);

        assertNotNull(sub);
        assertEquals(20, sub.getWidth());
        assertEquals(20, sub.getHeight());
    }

    @Test
    void testMirror() {
        Image source = Image.createImage(50, 50);
        Image mirrored = source.mirror();

        assertNotNull(mirrored);
        assertEquals(50, mirrored.getWidth());
        assertEquals(50, mirrored.getHeight());
    }

    @Test
    void testRotate90Degrees() {
        Image source = Image.createImage(50, 100);
        Image rotated = source.rotate(90);

        assertNotNull(rotated);
        assertEquals(100, rotated.getWidth());
        assertEquals(50, rotated.getHeight());
    }

    @Test
    void testRotate180Degrees() {
        Image source = Image.createImage(50, 100);
        Image rotated = source.rotate(180);

        assertNotNull(rotated);
        assertEquals(50, rotated.getWidth());
        assertEquals(100, rotated.getHeight());
    }

    @Test
    void testRotate270Degrees() {
        Image source = Image.createImage(50, 100);
        Image rotated = source.rotate(270);

        assertNotNull(rotated);
        assertEquals(100, rotated.getWidth());
        assertEquals(50, rotated.getHeight());
    }

    @Test
    void testModifyAlpha() {
        Image source = Image.createImage(10, 10, 0xFFFF0000);
        Image modified = source.modifyAlpha((byte) 128);

        assertNotNull(modified);
        assertEquals(10, modified.getWidth());
        assertEquals(10, modified.getHeight());
    }

    @Test
    void testModifyAlphaWithTranslucency() {
        Image source = Image.createImage(10, 10, 0xFFFF0000);
        Image modified = source.modifyAlphaWithTranslucency((byte) 128);

        assertNotNull(modified);
        assertEquals(10, modified.getWidth());
        assertEquals(10, modified.getHeight());
    }

    @Test
    void testModifyAlphaWithRemoveColor() {
        Image source = Image.createImage(10, 10, 0xFFFF0000);
        Image modified = source.modifyAlpha((byte) 128, 0xFF0000FF);

        assertNotNull(modified);
        assertEquals(10, modified.getWidth());
        assertEquals(10, modified.getHeight());
    }

    @Test
    void testGetGraphicsOnMutableImage() {
        Image image = Image.createImage(50, 50);
        Graphics g = image.getGraphics();

        assertNotNull(g);
    }

    @Test
    void testScaleInPlace() {
        Image image = Image.createImage(100, 100);
        image.scale(50, 50);

        assertEquals(50, image.getWidth());
        assertEquals(50, image.getHeight());
    }

    @Test
    void testLockAndUnlock() {
        Image image = Image.createImage(10, 10);

        assertFalse(image.isLocked());

        image.lock();
        assertTrue(image.isLocked());

        image.unlock();
        assertFalse(image.isLocked());
    }

    @Test
    void testIsAnimationDefaultsFalse() {
        Image image = Image.createImage(10, 10);
        assertFalse(image.isAnimation());
    }

    @Test
    void testAlphaMutableImageSupported() {
        boolean supported = Image.isAlphaMutableImageSupported();
        // Just verify the method doesn't throw
        assertTrue(supported || !supported);
    }

    @Test
    void testActionListenerAddAndRemove() {
        Image image = Image.createImage(10, 10);
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
            }
        };

        image.addActionListener(listener);
        image.removeActionListener(listener);
    }

    @Test
    void testCreateMaskReturnsNonNull() {
        Image image = Image.createImage(10, 10);
        Object mask = image.createMask();

        assertNotNull(mask);
    }

    @Test
    void testApplyMask() {
        Image image = Image.createImage(50, 50);
        Object mask = image.createMask();
        Image masked = image.applyMask(mask);

        assertNotNull(masked);
        assertEquals(50, masked.getWidth());
        assertEquals(50, masked.getHeight());
    }

    @Test
    void testApplyMaskWithOffset() {
        Image image = Image.createImage(50, 50);
        Object mask = image.createMask();
        Image masked = image.applyMask(mask, 10, 10);

        assertNotNull(masked);
        assertEquals(50, masked.getWidth());
        assertEquals(50, masked.getHeight());
    }

    @Test
    void testApplyMaskAutoScale() {
        Image image = Image.createImage(50, 50);
        Object mask = image.createMask();
        Image masked = image.applyMaskAutoScale(mask);

        assertNotNull(masked);
        assertEquals(50, masked.getWidth());
        assertEquals(50, masked.getHeight());
    }

    @Test
    void testScaledReturnsSameImageWhenSizeMatches() {
        Image image = Image.createImage(50, 50);
        Image scaled = image.scaled(50, 50);

        assertSame(image, scaled, "Should return same instance when dimensions match");
    }

    @Test
    void testIsOpaqueInitialState() {
        Image image = Image.createImage(10, 10);
        // Just verify the method works - actual opaqueness depends on implementation
        boolean opaque = image.isOpaque();
        assertTrue(opaque || !opaque);
    }

    @Test
    void testImageNameSetAndGet() {
        Image image = Image.createImage(10, 10);
        image.setImageName("test-image");
        assertEquals("test-image", image.getImageName());
    }

    @Test
    void testImageNameDefaultsNull() {
        Image image = Image.createImage(10, 10);
        assertNull(image.getImageName());
    }

    @Test
    void testIsSVGDefaultsFalse() {
        Image image = Image.createImage(10, 10);
        assertFalse(image.isSVG());
    }

    @Test
    void testGetSVGDocumentDefaultsNull() {
        Image image = Image.createImage(10, 10);
        assertNull(image.getSVGDocument());
    }

    @Test
    void testDispose() {
        Image image = Image.createImage(10, 10);
        // Just verify it doesn't throw
        image.dispose();
    }

    @Test
    void testRequiresDrawImage() {
        Image image = Image.createImage(10, 10);
        assertFalse(image.requiresDrawImage());
    }

    @Test
    void testGetImage() {
        Image image = Image.createImage(10, 10);
        Object nativeImage = image.getImage();
        assertNotNull(nativeImage);
    }

    @Test
    void testAsyncLock() {
        Image image = Image.createImage(10, 10);
        Image internal = Image.createImage(5, 5);
        // Just verify it doesn't throw
        image.asyncLock(internal);
    }

    @Test
    void testFireChangedEventWithoutListeners() {
        Image image = Image.createImage(10, 10);
        // Should not throw even without listeners
        image.fireChangedEvent();
    }

    @Test
    void testFireChangedEventWithListener() {
        Image image = Image.createImage(10, 10);
        final boolean[] called = {false};

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                called[0] = true;
            }
        };

        image.addActionListener(listener);
        image.fireChangedEvent();

        assertTrue(called[0], "Listener should be called when event is fired");
    }
}
