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

/// Dense per-pixel foreground confidence mask. The confidence array is
/// row-major with exactly {@code width * height} values in the range 0..1.
/// It is defensively copied on construction and access.
public final class SegmentationMask {
    private final int width;
    private final int height;
    private final float[] confidence;
    private final VisionMetadata metadata;

    /// Creates a row-major confidence mask without backend metadata.
    /// @param width mask width in pixels
    /// @param height mask height in pixels
    /// @param confidence one foreground probability per pixel, defensively copied
    /// @throws IllegalArgumentException if either dimension is negative, the
    ///  pixel count exceeds the maximum Java array length, or
    ///  {@code confidence} does not contain exactly one value per pixel
    public SegmentationMask(int width, int height, float[] confidence) {
        this(width, height, confidence, null);
    }

    /// Creates a row-major confidence mask with backend diagnostics.
    /// @param width mask width in pixels
    /// @param height mask height in pixels
    /// @param confidence one foreground probability per pixel, defensively copied
    /// @param metadata backend details, or {@code null}
    /// @throws IllegalArgumentException if either dimension is negative, the
    ///  pixel count exceeds the maximum Java array length, or
    ///  {@code confidence} does not contain exactly one value per pixel
    public SegmentationMask(int width, int height, float[] confidence,
                            VisionMetadata metadata) {
        long pixelCount = (long) width * (long) height;
        if (width < 0 || height < 0 || pixelCount > Integer.MAX_VALUE
                || confidence == null || confidence.length != pixelCount) {
            throw new IllegalArgumentException("Mask dimensions do not match its data");
        }
        this.width = width;
        this.height = height;
        this.confidence = new float[confidence.length];
        System.arraycopy(confidence, 0, this.confidence, 0, confidence.length);
        this.metadata = metadata;
    }

    /// @return mask width, which may differ from source image width
    public int getWidth() {
        return width;
    }

    /// @return mask height, which may differ from source image height
    public int getHeight() {
        return height;
    }

    /// @return defensive copy of row-major foreground confidences
    public float[] getConfidence() {
        float[] out = new float[confidence.length];
        System.arraycopy(confidence, 0, out, 0, confidence.length);
        return out;
    }

    /// @return backend metadata, or {@code null} when manually constructed
    public VisionMetadata getMetadata() {
        return metadata;
    }
}
