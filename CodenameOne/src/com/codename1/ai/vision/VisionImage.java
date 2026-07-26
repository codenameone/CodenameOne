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

/// Immutable input for encoded still images or raw camera pixels. Factory
/// methods defensively copy their arrays. In particular,
/// {@link #fromCameraFrame(CameraFrame)} detaches from callback-owned buffers
/// that a camera backend may recycle when the callback returns.
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

    /// Creates an encoded JPEG or PNG input.
    /// @param bytes complete encoded image, copied by this method
    /// @return immutable image input
    public static VisionImage encoded(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image bytes must not be empty");
        }
        return new VisionImage(bytes, null, 0, 0, 0, 0, FrameFormat.JPEG);
    }

    /// Creates raw NV21 or RGBA8888 input with display orientation metadata.
    /// @param bytes pixel buffer in {@code format}, copied by this method
    /// @param width unrotated pixel width
    /// @param height unrotated pixel height
    /// @param format supported raw frame format
    /// @param rotationDegrees clockwise display rotation
    /// @return immutable image input
    public static VisionImage pixels(byte[] bytes, int width, int height,
                                     FrameFormat format, int rotationDegrees) {
        if (bytes == null || bytes.length == 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Pixels and positive dimensions are required");
        }
        return new VisionImage(null, bytes, width, height, rotationDegrees, 0, format);
    }

    /// Copies a camera frame, including timestamp, format, and orientation.
    /// @param frame callback-owned frame
    /// @return detached immutable input safe for asynchronous analysis
    public static VisionImage fromCameraFrame(CameraFrame frame) {
        if (frame == null) {
            throw new NullPointerException("frame");
        }
        return new VisionImage(frame.getJpegBytes(), frame.getRawBytes(),
                frame.getWidth(), frame.getHeight(), frame.getRotationDegrees(),
                frame.getTimestampNanos(), frame.getFormat());
    }

    /// @return a defensive copy of encoded bytes, or {@code null} for raw input
    public byte[] getEncodedBytes() {
        return copy(encodedBytes);
    }

    /// Returns the immutable object's backing encoded buffer to a port
    /// implementation. Application code must use {@link #getEncodedBytes()},
    /// which preserves the class's defensive-copy contract.
    ///
    /// @return the backing encoded buffer, or {@code null} for a raw image
    /// @hidden
    public byte[] getEncodedBytesUnsafe() {
        return encodedBytes;
    }

    /// @return a defensive copy of raw pixels, or {@code null} for encoded input
    public byte[] getPixels() {
        return copy(pixels);
    }

    /// Returns the immutable object's backing pixel buffer to a port
    /// implementation. The returned array must never be modified or retained
    /// beyond the native call that consumes it.
    ///
    /// @return the backing pixel buffer, or {@code null} for an encoded image
    /// @hidden
    public byte[] getPixelsUnsafe() {
        return pixels;
    }

    /// @return raw pixel width, or zero for encoded input
    public int getWidth() {
        return width;
    }

    /// @return raw pixel height, or zero for encoded input
    public int getHeight() {
        return height;
    }

    /// @return normalized clockwise rotation in the range 0..359
    public int getRotationDegrees() {
        return rotationDegrees;
    }

    /// @return capture timestamp in nanoseconds, or zero for manual input
    public long getTimestampNanos() {
        return timestampNanos;
    }

    /// @return encoded/raw frame format
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
