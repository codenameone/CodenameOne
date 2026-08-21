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

import com.codename1.ui.Image;

/// Dense per-pixel foreground confidence mask. The confidence array is
/// row-major with exactly {@code width * height} values in the range 0..1.
/// It is defensively copied on construction and access.
///
/// The mask's resolution is the segmentation model's, not the source image's,
/// so it is normally smaller than the frame it describes.
/// {@link #cutOut(Image, float)} and {@link #toMaskImage(int)} rescale it for
/// you; when reading {@link #getConfidence()} directly, index it with
/// {@link #getWidth()} and {@link #getHeight()} rather than the frame size.
///
/// ```java
/// SelfieSegmenter segmenter = new SelfieSegmenter();
/// segmenter.process(VisionImage.encoded(jpeg)).ready(mask -> {
///     Image person = mask.cutOut(EncodedImage.create(jpeg), 0.6f);
///     background.getStyle().setBgImage(person);
/// }).except(error -> Log.e(error));
/// ```
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

    /// Reads one mask pixel.
    ///
    /// @param x column in the range 0 to {@link #getWidth()} - 1
    /// @param y row in the range 0 to {@link #getHeight()} - 1
    /// @return foreground probability in the range 0..1
    /// @throws IndexOutOfBoundsException if the position is outside the mask
    public float getConfidenceAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IndexOutOfBoundsException(
                    "Position " + x + "," + y + " is outside the "
                    + width + "x" + height + " mask");
        }
        return confidence[y * width + x];
    }

    /// Keeps the foreground of an image and makes the rest transparent.
    ///
    /// The mask is sampled with nearest-neighbour scaling, so the result has
    /// {@code source}'s dimensions no matter what resolution the segmentation
    /// model produced. Pixels whose confidence is below {@code threshold}
    /// become fully transparent; the rest keep their original color and alpha.
    ///
    /// @param source the image the mask was computed from
    /// @param threshold minimum foreground confidence to keep, clamped to 0..1
    /// @return a new image with the background removed
    /// @throws NullPointerException if {@code source} is {@code null}
    /// @throws IllegalArgumentException if this mask has no pixels
    public Image cutOut(Image source, float threshold) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        requireNonEmpty();
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int[] pixels = source.getRGB();
        float limit = clamp(threshold);
        for (int y = 0; y < sourceHeight; y++) {
            int row = y * sourceWidth;
            int maskRow = sample(y, sourceHeight, height) * width;
            for (int x = 0; x < sourceWidth; x++) {
                if (confidence[maskRow + sample(x, sourceWidth, width)]
                        < limit) {
                    pixels[row + x] &= 0x00ffffff;
                }
            }
        }
        return Image.createImage(pixels, sourceWidth, sourceHeight);
    }

    /// Renders the mask itself as a translucent tint, for drawing over the
    /// frame it describes as a foreground highlight.
    ///
    /// Each pixel takes {@code rgb} as its color and the mask confidence as
    /// its alpha, so a fully confident foreground pixel is opaque and a
    /// background pixel is invisible. The result has the mask's own
    /// resolution; draw it scaled to the frame's bounds.
    ///
    /// @param rgb tint color as {@code 0xRRGGBB}; any alpha in the value is
    ///        ignored in favor of the mask
    /// @return a new image at this mask's resolution
    /// @throws IllegalArgumentException if this mask has no pixels
    public Image toMaskImage(int rgb) {
        requireNonEmpty();
        int color = rgb & 0x00ffffff;
        int[] pixels = new int[confidence.length];
        for (int i = 0; i < pixels.length; i++) {
            int alpha = Math.round(clamp(confidence[i]) * 255);
            pixels[i] = (alpha << 24) | color;
        }
        return Image.createImage(pixels, width, height);
    }

    /// @return backend metadata, or {@code null} when manually constructed
    public VisionMetadata getMetadata() {
        return metadata;
    }

    private void requireNonEmpty() {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Mask has no pixels");
        }
    }

    private static int sample(int position, int sourceSize, int maskSize) {
        int value = position * maskSize / sourceSize;
        return value >= maskSize ? maskSize - 1 : value;
    }

    private static float clamp(float value) {
        if (Float.isNaN(value)) {
            return 0;
        }
        return value < 0 ? 0 : (value > 1 ? 1 : value);
    }
}
