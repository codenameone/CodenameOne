/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.ai.vision;

import com.codename1.junit.UITestBase;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The result-shaping helpers that turn a raw analyzer observation into
 * something an application can draw or branch on.
 */
class VisionResultHelpersTest extends UITestBase {
    @Test
    void formatMatchingAcceptsEverythingWhenNoFilterIsGiven() {
        Barcode qr = new Barcode("x", BarcodeFormat.QR_CODE, null,
                VisionRect.EMPTY, null);
        assertTrue(BarcodeFormat.matches(qr));
        assertTrue(BarcodeFormat.matches(qr, new String[0]));
        assertTrue(BarcodeFormat.matches(qr, (String[]) null));
        assertTrue(BarcodeFormat.matches(qr, BarcodeFormat.EAN_13,
                BarcodeFormat.QR_CODE));
        assertFalse(BarcodeFormat.matches(qr, BarcodeFormat.EAN_13));
        // A null entry in the filter must not match a barcode whose format is
        // itself unset, which is what a naive equals() ordering would do.
        Barcode unset = new Barcode("x", null, null, VisionRect.EMPTY, null);
        assertFalse(BarcodeFormat.matches(unset, (String) null));
        assertFalse(BarcodeFormat.matches(null, BarcodeFormat.QR_CODE));
    }

    @Test
    void normalizedGeometryMapsOntoPixels() {
        VisionRect rect = new VisionRect(.25f, .5f, .5f, .25f);
        Rectangle bounds = rect.toBounds(10, 20, 400, 200);
        assertEquals(110, bounds.getX());
        assertEquals(120, bounds.getY());
        assertEquals(200, bounds.getWidth());
        assertEquals(50, bounds.getHeight());

        Point point = new VisionPoint(.5f, .25f).toPoint(0, 0, 400, 200);
        assertEquals(200, point.getX());
        assertEquals(50, point.getY());

        assertTrue(VisionRect.EMPTY.isEmpty());
        assertFalse(rect.isEmpty());
        assertThrows(NullPointerException.class, () -> rect.toBounds(null));
        assertThrows(NullPointerException.class,
                () -> new VisionPoint(0, 0).toPoint(null));
    }

    @Test
    void rectangleEdgesRoundConsistentlySoAdjacentBoxesDoNotOverlap() {
        // Rounding each edge rather than rounding the width is what keeps a
        // box that ends where the next begins from gaining a pixel.
        VisionRect first = new VisionRect(0f, 0f, 1f / 3f, 1f);
        VisionRect second = new VisionRect(1f / 3f, 0f, 1f / 3f, 1f);
        Rectangle a = first.toBounds(0, 0, 100, 10);
        Rectangle b = second.toBounds(0, 0, 100, 10);
        assertEquals(a.getX() + a.getWidth(), b.getX());
    }

    @Test
    void namedLandmarksAreLookedUpWithoutTouchingTheMap() {
        Map<String, VisionPoint> landmarks = new HashMap<String, VisionPoint>();
        landmarks.put(FaceLandmarks.LEFT_EYE, new VisionPoint(.6f, .4f));
        Face face = new Face(new VisionRect(0, 0, 1, 1), landmarks,
                0, 0, 0, -1, -1);
        assertNotNull(face.getLandmark(FaceLandmarks.LEFT_EYE));
        assertEquals(.6f, face.getLandmark(FaceLandmarks.LEFT_EYE).getX());
        assertNull(face.getLandmark(FaceLandmarks.RIGHT_EYE),
                "an undetected landmark is absent, not a zero point");
        assertNull(face.getLandmark(null));

        Pose pose = new Pose(new Pose.Landmark[] {
            new Pose.Landmark(PoseLandmarks.RIGHT_WRIST,
                    new VisionPoint(.2f, .3f), .8f),
            null
        });
        assertNotNull(pose.getLandmark(PoseLandmarks.RIGHT_WRIST));
        assertEquals(.8f,
                pose.getLandmark(PoseLandmarks.RIGHT_WRIST).getConfidence());
        assertNull(pose.getLandmark(PoseLandmarks.LEFT_ANKLE));
        assertNull(pose.getLandmark(null));
    }

    @Test
    void maskCutsOutTheForegroundAtItsOwnResolution() {
        // A 2x1 mask describing a 4x2 image: the left half is foreground.
        SegmentationMask mask = new SegmentationMask(2, 1,
                new float[] {.9f, .1f});
        assertEquals(.9f, mask.getConfidenceAt(0, 0));
        assertThrows(IndexOutOfBoundsException.class,
                () -> mask.getConfidenceAt(0, 1));

        int[] source = new int[8];
        for (int i = 0; i < source.length; i++) {
            source[i] = 0xff102030;
        }
        Image cut = mask.cutOut(Image.createImage(source, 4, 2), .5f);
        assertEquals(4, cut.getWidth());
        assertEquals(2, cut.getHeight());
        int[] pixels = cut.getRGB();
        assertEquals(0xff102030, pixels[0]);
        assertEquals(0xff102030, pixels[1]);
        assertEquals(0, pixels[2] >>> 24, "background must be transparent");
        assertEquals(0, pixels[3] >>> 24);
        assertEquals(0, pixels[6] >>> 24, "the second row scales the same way");

        Image tint = mask.toMaskImage(0x00ff00);
        assertEquals(2, tint.getWidth());
        assertEquals(1, tint.getHeight());
        int[] tinted = tint.getRGB();
        assertEquals(0x00ff00, tinted[0] & 0xffffff);
        assertTrue((tinted[0] >>> 24) > (tinted[1] >>> 24));

        assertThrows(NullPointerException.class, () -> mask.cutOut(null, .5f));
        assertThrows(IllegalArgumentException.class,
                () -> new SegmentationMask(0, 0, new float[0]).toMaskImage(0));
    }

    @Test
    void encodedImagesReachTheAnalyzerWithoutBeingReEncoded() {
        byte[] png = onePixelPng();
        VisionImage wrapped = VisionImage.fromImage(EncodedImage.create(png));
        assertArrayEquals(png, wrapped.getEncodedBytes());
        assertNull(wrapped.getPixels());
    }

    @Test
    void otherImagesReachTheAnalyzerAsRgbaPixels() {
        VisionImage wrapped = VisionImage.fromImage(
                Image.createImage(new int[] {0x80112233, 0xff445566}, 2, 1));
        assertNull(wrapped.getEncodedBytes());
        assertEquals(2, wrapped.getWidth());
        assertEquals(1, wrapped.getHeight());
        assertArrayEquals(new byte[] {
            0x11, 0x22, 0x33, (byte) 0x80,
            0x44, 0x55, 0x66, (byte) 0xff
        }, wrapped.getPixels());
        assertThrows(NullPointerException.class,
                () -> VisionImage.fromImage(null));
    }

    private static byte[] onePixelPng() {
        // The smallest valid PNG; EncodedImage keeps the bytes verbatim, which
        // is the property under test rather than the pixels.
        return new byte[] {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
            0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1f, 0x15, (byte) 0xc4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0a, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0x9c, 0x63, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0d, 0x0a, 0x2d, (byte) 0xb4, 0x00,
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4e, 0x44, (byte) 0xae,
            0x42, 0x60, (byte) 0x82
        };
    }
}
