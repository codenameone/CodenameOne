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
package com.codename1.camera;

/// A single frame delivered to a `FrameListener`.
///
/// **Lifetime**: the byte arrays returned by this class are valid only while
/// `FrameListener#onFrame(CameraFrame)` is on the stack. After it returns the
/// framework may reuse or release the underlying buffers. Clone any data you
/// need to keep.
public final class CameraFrame {
    private final byte[] jpegBytes;
    private final byte[] rawBytes;
    private final int width;
    private final int height;
    private final int rotationDegrees;
    private final long timestampNanos;
    private final FrameFormat format;

    /// Used by platform implementations.
    public CameraFrame(byte[] jpegBytes, byte[] rawBytes,
                       int width, int height,
                       int rotationDegrees, long timestampNanos,
                       FrameFormat format) {
        this.jpegBytes = jpegBytes;
        this.rawBytes = rawBytes;
        this.width = width;
        this.height = height;
        this.rotationDegrees = rotationDegrees;
        this.timestampNanos = timestampNanos;
        this.format = format;
    }

    /// JPEG-encoded bytes for this frame. Always non-null regardless of the
    /// requested `FrameFormat`; the framework encodes raw frames into JPEG
    /// on demand for AI module consumption.
    public byte[] getJpegBytes() {
        return jpegBytes;
    }

    /// Raw pixel bytes in the format requested via
    /// `CameraSessionOptions#frameFormat(FrameFormat)`. `null` when the
    /// requested format was `FrameFormat#JPEG`.
    public byte[] getRawBytes() {
        return rawBytes;
    }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }

    /// Clockwise rotation in degrees (0, 90, 180, 270) that should be applied
    /// to the bytes to display them upright.
    public int getRotationDegrees() {
        return rotationDegrees;
    }

    /// Monotonic timestamp in nanoseconds. Useful for measuring inter-frame
    /// intervals.
    public long getTimestampNanos() {
        return timestampNanos;
    }

    public FrameFormat getFormat() {
        return format;
    }
}
