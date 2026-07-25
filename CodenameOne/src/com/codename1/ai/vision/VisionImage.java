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

import com.codename1.camera.CameraFrame;
import com.codename1.camera.FrameFormat;

/// Immutable vision input. Camera-frame factories copy the callback-owned bytes
/// so asynchronous analyzers never retain recycled camera buffers.
public final class VisionImage {
    private final byte[] encodedBytes;
    private final byte[] pixels;
    private final int width;
    private final int height;
    private final int rotationDegrees;
    private final long timestampNanos;
    private final FrameFormat format;

    private VisionImage(byte[] encodedBytes, byte[] pixels, int width, int height,
                        int rotationDegrees, long timestampNanos, FrameFormat format) {
        this.encodedBytes = copy(encodedBytes);
        this.pixels = copy(pixels);
        this.width = width;
        this.height = height;
        this.rotationDegrees = normalizeRotation(rotationDegrees);
        this.timestampNanos = timestampNanos;
        this.format = format == null ? FrameFormat.JPEG : format;
    }

    public static VisionImage encoded(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image bytes must not be empty");
        }
        return new VisionImage(bytes, null, 0, 0, 0, 0, FrameFormat.JPEG);
    }

    public static VisionImage pixels(byte[] bytes, int width, int height,
                                     FrameFormat format, int rotationDegrees) {
        if (bytes == null || bytes.length == 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Pixels and positive dimensions are required");
        }
        return new VisionImage(null, bytes, width, height, rotationDegrees, 0, format);
    }

    public static VisionImage fromCameraFrame(CameraFrame frame) {
        if (frame == null) {
            throw new NullPointerException("frame");
        }
        return new VisionImage(frame.getJpegBytes(), frame.getRawBytes(),
                frame.getWidth(), frame.getHeight(), frame.getRotationDegrees(),
                frame.getTimestampNanos(), frame.getFormat());
    }

    public byte[] getEncodedBytes() {
        return copy(encodedBytes);
    }

    public byte[] getPixels() {
        return copy(pixels);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRotationDegrees() {
        return rotationDegrees;
    }

    public long getTimestampNanos() {
        return timestampNanos;
    }

    public FrameFormat getFormat() {
        return format;
    }

    private static byte[] copy(byte[] value) {
        if (value == null) {
            return null;
        }
        byte[] out = new byte[value.length];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
    }

    private static int normalizeRotation(int value) {
        int out = value % 360;
        return out < 0 ? out + 360 : out;
    }
}
