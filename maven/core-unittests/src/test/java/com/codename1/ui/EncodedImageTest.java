package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.util.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EncodedImageTest extends UITestBase {

    @FormTest
    void testCreateFromByteArrayReturnsSameData() {
        byte[] data = new byte[]{1, 2, 3, 4};
        EncodedImage encoded = EncodedImage.create(data);
        assertSame(data, encoded.getImageData());
    }

    @FormTest
    void testCreateWithMetadataSetsOpaqueAndDimensions() {
        byte[] data = new byte[]{9, 8, 7};
        EncodedImage encoded = EncodedImage.create(data, 21, 13, true);
        assertEquals(21, encoded.getWidth());
        assertEquals(13, encoded.getHeight());
        assertTrue(encoded.isOpaque());
    }

    @FormTest
    void testCreateMultiSelectsDataForDeviceDensity() {
        byte[][] data = new byte[][]{
                {1}, {2}, {3}
        };
        int[] dpis = new int[]{Display.DENSITY_LOW, Display.DENSITY_MEDIUM, Display.DENSITY_VERY_HIGH};
        EncodedImage encoded = EncodedImage.createMulti(dpis, data);

        implementation.setDeviceDensity(Display.DENSITY_LOW);
        assertSame(data[0], encoded.getImageData());

        implementation.setDeviceDensity(Display.DENSITY_VERY_HIGH);
        assertSame(data[2], encoded.getImageData());

        implementation.setDeviceDensity(Display.DENSITY_HIGH);
        assertSame(data[1], encoded.getImageData());
    }

    @FormTest
    void testCreateFromImageUsesRequestedFormatAndCachesDimensions() {
        RecordingImageIO imageIO = new RecordingImageIO(true, true);
        implementation.setImageIO(imageIO);

        Image image = Image.createImage(12, 18);
        EncodedImage encoded = EncodedImage.createFromImage(image, true);

        assertNotNull(encoded);
        assertEquals(12, encoded.getWidth());
        assertEquals(18, encoded.getHeight());
        assertTrue(encoded.isOpaque());
        assertEquals(ImageIO.FORMAT_JPEG, imageIO.lastFormat);
        assertTrue(imageIO.savedFromImage);
    }

    @Test
    void testScaledEncodedUsesImageIoAndPreservesOpacity() {
        RecordingImageIO imageIO = new RecordingImageIO(true, true);
        implementation.setImageIO(imageIO);

        EncodedImage encoded = EncodedImage.create(new byte[]{5, 4, 3, 2}, 40, 20, true);
        EncodedImage scaled = encoded.scaledEncoded(10, 5);

        assertNotNull(scaled);
        assertEquals(10, scaled.getWidth());
        assertEquals(5, scaled.getHeight());
        assertTrue(scaled.isOpaque());
        assertEquals(ImageIO.FORMAT_JPEG, imageIO.lastFormat);
        assertEquals(10, imageIO.lastWidth);
        assertEquals(5, imageIO.lastHeight);
    }

    @FormTest
    void testCreateFromInputStreamReadsExactSize() throws IOException {
        byte[] payload = new byte[32];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i + 3);
        }
        InputStream input = new ByteArrayInputStream(payload);
        EncodedImage encoded = EncodedImage.create(input, payload.length);
        assertArrayEquals(payload, encoded.getImageData());
    }

    @FormTest
    void testLockAndUnlockPromotesCachedImage() throws Exception {
        EncodedImage encoded = EncodedImage.create(new byte[]{1, 2, 3, 4}, 6, 6, false);
        Image actual = Image.createImage(6, 6);
        Object ref = Display.getInstance().createSoftWeakRef(actual);

        Field cacheField = EncodedImage.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(encoded, ref);

        Field hardCacheField = EncodedImage.class.getDeclaredField("hardCache");
        hardCacheField.setAccessible(true);

        encoded.lock();
        assertSame(actual, hardCacheField.get(encoded));
        assertTrue(encoded.isLocked());

        encoded.unlock();
        assertFalse(encoded.isLocked());
        assertNull(hardCacheField.get(encoded));
        Object cachedRef = cacheField.get(encoded);
        assertNotNull(cachedRef);
        assertSame(actual, Display.getInstance().extractHardRef(cachedRef));
    }

    private static class RecordingImageIO extends ImageIO {
        private final boolean pngSupported;
        private final boolean jpegSupported;
        private final byte[] recordedOutput = new byte[]{1, 2, 3};
        private String lastFormat;
        private int lastWidth;
        private int lastHeight;
        private boolean savedFromImage;

        RecordingImageIO(boolean pngSupported, boolean jpegSupported) {
            this.pngSupported = pngSupported;
            this.jpegSupported = jpegSupported;
        }

        @Override
        public void save(Image img, java.io.OutputStream response, String format, float quality) throws IOException {
            this.lastFormat = format;
            this.savedFromImage = true;
            response.write(recordedOutput);
        }

        @Override
        public void save(InputStream image, java.io.OutputStream response, String format, int width, int height, float quality) throws IOException {
            this.lastFormat = format;
            this.lastWidth = width;
            this.lastHeight = height;
            this.savedFromImage = false;
            response.write(recordedOutput);
        }

        @Override
        protected void saveImage(Image img, java.io.OutputStream response, String format, float quality) throws IOException {
            response.write(recordedOutput);
        }

        @Override
        public boolean isFormatSupported(String format) {
            if (FORMAT_PNG.equals(format)) {
                return pngSupported;
            }
            if (FORMAT_JPEG.equals(format)) {
                return jpegSupported;
            }
            return false;
        }
    }
}
