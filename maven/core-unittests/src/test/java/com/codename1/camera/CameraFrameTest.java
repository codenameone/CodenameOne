/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic coverage for the immutable {@link CameraFrame} value object that
 * a {@code FrameListener} receives.
 */
class CameraFrameTest {

    @Test
    void allFieldsAreExposed() {
        byte[] jpeg = {1, 2, 3};
        byte[] raw = {4, 5};
        CameraFrame f = new CameraFrame(jpeg, raw, 640, 480, 90, 123456789L, FrameFormat.JPEG);
        assertSame(jpeg, f.getJpegBytes());
        assertSame(raw, f.getRawBytes());
        assertEquals(640, f.getWidth());
        assertEquals(480, f.getHeight());
        assertEquals(90, f.getRotationDegrees());
        assertEquals(123456789L, f.getTimestampNanos());
        assertEquals(FrameFormat.JPEG, f.getFormat());
    }

    @Test
    void rawBytesMayBeNullForJpegOnlyFrames() {
        CameraFrame f = new CameraFrame(new byte[]{9}, null, 1, 1, 0, 0L, FrameFormat.JPEG);
        assertNull(f.getRawBytes());
        assertNotNull(f.getJpegBytes());
    }

    @Test
    void rawAnalysisDoesNotEncodeJpegAndExplicitJpegAccessIsCached() {
        final int[] calls = {0};
        byte[] raw = {16, 32, 48, 64, (byte) 128, (byte) 128};
        byte[] jpeg = {1, 2, 3};
        CameraFrame frame = new CameraFrame(null, raw, 2, 2, 90, 7,
                FrameFormat.NV21, () -> { calls[0]++; return jpeg; });
        com.codename1.ai.vision.VisionImage image =
                com.codename1.ai.vision.VisionImage.fromCameraFrame(frame);
        assertEquals(FrameFormat.NV21, image.getFormat());
        assertArrayEquals(raw, image.getPixelsUnsafe());
        assertEquals(0, calls[0]);
        assertSame(jpeg, frame.getJpegBytes());
        assertSame(jpeg, frame.getJpegBytes());
        assertEquals(1, calls[0]);
    }

    @Test
    void failedLazyEncodingCanBeRetried() {
        final int[] calls = {0};
        CameraFrame frame = new CameraFrame(null, new byte[6], 2, 2, 0, 0,
                FrameFormat.NV21, () -> ++calls[0] == 1 ? null : new byte[] {1});
        assertThrows(IllegalStateException.class, frame::getJpegBytes);
        assertArrayEquals(new byte[] {1}, frame.getJpegBytes());
        assertEquals(2, calls[0]);
    }
}
