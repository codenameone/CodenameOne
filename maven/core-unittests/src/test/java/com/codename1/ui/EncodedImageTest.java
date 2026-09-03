/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
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
    void testDecodeIsCachedWhenNothingInvalidatesIt() {
        EncodedImage encoded = EncodedImage.create(new byte[]{1, 2, 3, 4});
        assertSame(encoded.getImage(), encoded.getImage(),
                "a picture nobody invalidated must not be decoded twice");
    }

    @FormTest
    void testInvalidateDecodedImagesForcesRedecode() {
        EncodedImage encoded = EncodedImage.create(new byte[]{1, 2, 3, 4});
        Object first = encoded.getImage();

        EncodedImage.invalidateDecodedImages();

        assertNotSame(first, encoded.getImage(),
                "after the platform may have thrown the decode away, the picture has to be decoded again");
    }

    @FormTest
    void testInvalidateDecodedImagesReachesLockedImages() {
        EncodedImage encoded = EncodedImage.create(new byte[]{5, 6, 7, 8});
        encoded.lock();
        Object first = encoded.getImage();
        assertSame(first, encoded.getImage(), "a locked picture is held hard, so it decodes once");

        EncodedImage.invalidateDecodedImages();

        // The point of the generation: a locked image keeps its stale decode
        // until it is next used, and then discards it unused. Locking must not
        // be able to pin a decode the platform has invalidated.
        assertNotSame(first, encoded.getImage(),
                "a lock must not keep a decode alive that the platform has invalidated");
        assertTrue(encoded.isLocked(), "invalidation must not disturb the lock protocol");
    }

    @FormTest
    void testInvalidateDecodedImagesKeepsDimensions() {
        EncodedImage encoded = EncodedImage.create(new byte[]{9, 8, 7}, 21, 13, true);

        EncodedImage.invalidateDecodedImages();

        // Width and height are a property of the ENCODED bytes, which have not
        // changed; only the decoded form is suspect. Resetting them would make
        // every layout that measured this image wrong until it re-decoded.
        assertEquals(21, encoded.getWidth());
        assertEquals(13, encoded.getHeight());
    }

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
    void testDisposeReleasesEncodedBytes() {
        EncodedImage encoded = EncodedImage.create(new byte[]{1, 2, 3, 4}, 8, 8, true);
        assertFalse(encoded.isDisposed());
        encoded.dispose();
        assertTrue(encoded.isDisposed());
        assertThrows(IllegalStateException.class, encoded::getImageData);
        encoded.dispose();
        assertTrue(encoded.isDisposed());
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
