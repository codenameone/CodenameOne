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
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;

import java.io.IOException;
import java.io.InputStream;

/// Immutable input for encoded still images or raw camera pixels. Factory
/// methods defensively copy their arrays. In particular,
/// {@link #fromCameraFrame(CameraFrame)} detaches from callback-owned buffers
/// that a camera backend may recycle when the callback returns.
///
/// Pick the factory that matches where the image came from:
///
/// ```java
/// // A file: the gallery, or CapturedPhoto.getFilePath()
/// VisionImage.fromFile(photo.getFilePath());
///
/// // An image already in memory
/// VisionImage.fromImage(label.getIcon());
///
/// // Bytes you decoded or downloaded yourself
/// VisionImage.encoded(jpegBytes);
///
/// // A live camera frame, which also carries the frame's rotation
/// VisionImage.fromCameraFrame(frame);
/// ```
///
/// Only {@link #encoded(byte[], int)} needs a rotation argument, and only when
/// the stored pixels are not already upright -- this class does not read EXIF.
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
        this(encodedBytes, pixels, width, height, rotationDegrees,
                timestampNanos, format, true);
    }

    private VisionImage(byte[] encodedBytes, byte[] pixels, int width, int height,
                        int rotationDegrees, long timestampNanos,
                        FrameFormat format, boolean copyData) {
        this.encodedBytes = copyData ? copy(encodedBytes) : encodedBytes;
        this.pixels = copyData ? copy(pixels) : pixels;
        this.width = width;
        this.height = height;
        this.rotationDegrees = normalizeRotation(rotationDegrees);
        this.timestampNanos = timestampNanos;
        this.format = format == null ? FrameFormat.JPEG : format;
    }

    static VisionImage detachedCameraData(byte[] data, boolean raw,
                                          int width, int height,
                                          int rotationDegrees,
                                          long timestampNanos,
                                          FrameFormat format) {
        return new VisionImage(raw ? null : data, raw ? data : null,
                width, height, rotationDegrees, timestampNanos,
                raw ? format : FrameFormat.JPEG, false);
    }

    /// Creates an encoded JPEG or PNG input whose stored pixels are already
    /// upright. This method does not inspect EXIF orientation metadata. Use
    /// {@link #encoded(byte[], int)} when the encoded image needs a clockwise
    /// display rotation before analysis.
    ///
    /// @param bytes complete encoded image, copied by this method
    /// @return immutable image input
    /// @throws IllegalArgumentException if {@code bytes} is {@code null} or empty
    public static VisionImage encoded(byte[] bytes) {
        return encoded(bytes, 0);
    }

    /// Creates an encoded JPEG or PNG input with display orientation metadata.
    /// The rotation describes how the stored pixels must be rotated clockwise
    /// to appear upright; for example, pass {@code 90} for a portrait JPEG
    /// whose pixels are stored in landscape orientation. Decoders on Android
    /// and Apple platforms receive this value directly, so detected bounds and
    /// points use the displayed orientation. This method does not parse EXIF;
    /// callers loading an image outside {@link #fromCameraFrame(CameraFrame)}
    /// must supply the EXIF-derived rotation when it is relevant.
    ///
    /// @param bytes complete encoded image, copied by this method
    /// @param rotationDegrees clockwise display rotation; only 0, 90, 180,
    ///  or 270 degrees are supported
    /// @return immutable image input
    /// @throws IllegalArgumentException if {@code bytes} is {@code null} or
    ///  empty, or if the rotation is not a quarter turn
    public static VisionImage encoded(byte[] bytes, int rotationDegrees) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image bytes must not be empty");
        }
        return new VisionImage(bytes, null, 0, 0, rotationDegrees, 0,
                FrameFormat.JPEG);
    }

    /// Creates raw NV21 or RGBA8888 input with display orientation metadata.
    /// @param bytes pixel buffer in {@code format}, copied by this method
    /// @param width unrotated pixel width
    /// @param height unrotated pixel height
    /// @param format supported raw frame format
    /// @param rotationDegrees clockwise display rotation; only 0, 90, 180,
    ///  or 270 degrees are supported
    /// @return immutable image input
    /// @throws IllegalArgumentException if the data or dimensions are empty,
    ///  if {@code format} is not {@link FrameFormat#NV21} or
    ///  {@link FrameFormat#RGBA8888}, or if the rotation is not a quarter turn
    public static VisionImage pixels(byte[] bytes, int width, int height,
                                     FrameFormat format, int rotationDegrees) {
        if (bytes == null || bytes.length == 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Pixels and positive dimensions are required");
        }
        if (format != FrameFormat.NV21
                && format != FrameFormat.RGBA8888) {
            throw new IllegalArgumentException(
                    "Raw pixels require NV21 or RGBA8888 format; "
                    + "use encoded() for JPEG or PNG data");
        }
        return new VisionImage(null, bytes, width, height, rotationDegrees, 0, format);
    }

    /// Copies the buffer selected by the camera frame's format, together with
    /// its timestamp and orientation. JPEG frames copy only
    /// {@link CameraFrame#getJpegBytes()}; NV21 and RGBA8888 frames copy only
    /// {@link CameraFrame#getRawBytes()} when the port supplies that buffer.
    /// If a port cannot supply the requested raw format, this method copies
    /// the always-available JPEG fallback instead. This preserves the raw
    /// pipeline without creating an empty image on JPEG-only camera ports.
    ///
    /// @param frame callback-owned frame
    /// @return detached immutable input safe for asynchronous analysis
    /// @throws IllegalArgumentException if the frame reports a rotation other
    ///  than 0, 90, 180, or 270 degrees
    public static VisionImage fromCameraFrame(CameraFrame frame) {
        if (frame == null) {
            throw new NullPointerException("frame");
        }
        FrameFormat format = frame.getFormat() == null
                ? FrameFormat.JPEG : frame.getFormat();
        byte[] raw = frame.getRawBytes();
        boolean useRaw = format != FrameFormat.JPEG
                && raw != null && raw.length > 0;
        byte[] encoded = useRaw ? null : frame.getJpegBytes();
        byte[] pixels = useRaw ? raw : null;
        if (!useRaw) {
            format = FrameFormat.JPEG;
        }
        return new VisionImage(encoded, pixels,
                frame.getWidth(), frame.getHeight(), frame.getRotationDegrees(),
                frame.getTimestampNanos(), format);
    }

    /// Reads a JPEG or PNG file from {@link FileSystemStorage} and wraps it
    /// for analysis. This is the usual bridge from the image picker or from
    /// {@link com.codename1.camera.CapturedPhoto#getFilePath()} to an
    /// analyzer.
    ///
    /// Like {@link #encoded(byte[])} this assumes the stored pixels are
    /// already upright and does not parse EXIF. Use
    /// {@link #encoded(byte[], int)} when a rotation has to be applied.
    ///
    /// @param path a FileSystemStorage path
    /// @return immutable image input
    /// @throws IOException if the file cannot be read
    /// @throws IllegalArgumentException if the file is empty
    // Util.cleanup() in the finally block closes the stream; PMD does not
    // recognize it as a close.
    @SuppressWarnings("PMD.CloseResource")
    public static VisionImage fromFile(String path) throws IOException {
        InputStream input = FileSystemStorage.getInstance().openInputStream(path);
        try {
            return encoded(Util.readInputStream(input));
        } finally {
            Util.cleanup(input);
        }
    }

    /// Wraps an in-memory Codename One image for analysis.
    ///
    /// An {@link EncodedImage} passes its original JPEG or PNG bytes straight
    /// through, which is both cheaper and higher fidelity than re-encoding.
    /// Any other image is read as RGBA pixels. Either way the caller's image
    /// is left untouched.
    ///
    /// @param image the image to analyze
    /// @return immutable image input
    /// @throws NullPointerException if {@code image} is {@code null}
    /// @throws IllegalArgumentException if the image has no pixels
    public static VisionImage fromImage(Image image) {
        if (image == null) {
            throw new NullPointerException("image");
        }
        if (image instanceof EncodedImage) {
            byte[] data = ((EncodedImage) image).getImageData();
            if (data != null && data.length > 0) {
                return encoded(data);
            }
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image has no pixels");
        }
        int[] argb = image.getRGB();
        byte[] rgba = new byte[argb.length * 4];
        for (int i = 0, offset = 0; i < argb.length; i++, offset += 4) {
            int pixel = argb[i];
            rgba[offset] = (byte) (pixel >> 16);
            rgba[offset + 1] = (byte) (pixel >> 8);
            rgba[offset + 2] = (byte) pixel;
            rgba[offset + 3] = (byte) (pixel >> 24);
        }
        return pixels(rgba, width, height, FrameFormat.RGBA8888, 0);
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

    /// @return normalized clockwise rotation: 0, 90, 180, or 270
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
        if (out < 0) {
            out += 360;
        }
        if (out % 90 != 0) {
            throw new IllegalArgumentException(
                    "Rotation must be 0, 90, 180, or 270 degrees");
        }
        return out;
    }
}
