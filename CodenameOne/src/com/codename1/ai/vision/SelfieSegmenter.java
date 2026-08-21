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

/// Separates a person from the background, for background replacement or blur.
///
/// ```java
/// SelfieSegmenter segmenter = new SelfieSegmenter();
/// EncodedImage photo = EncodedImage.create(jpegBytes);
///
/// segmenter.process(VisionImage.encoded(jpegBytes)).ready(mask -> {
///     // Everything below 60% foreground confidence becomes transparent, so
///     // whatever is painted behind this image shows through.
///     Image person = mask.cutOut(photo, 0.6f);
///     preview.setIcon(person);
///     segmenter.close();
/// }).except(error -> {
///     Log.e(error);
///     segmenter.close();
/// });
/// ```
///
/// The mask has the model's own resolution rather than the frame's;
/// {@link SegmentationMask#cutOut(com.codename1.ui.Image, float)} and
/// {@link SegmentationMask#toMaskImage(int)} rescale it for you.
public final class SelfieSegmenter extends AbstractVisionAnalyzer<SegmentationMask> {
    /// Creates an analyzer using the platform default backend and options.
    /// @see VisionOptions
    public SelfieSegmenter() {
        this(null);
    }

    /// Creates a reusable analyzer with explicit backend and result options.
    /// @param options configuration captured by this analyzer; {@code null}
    ///        uses defaults
    public SelfieSegmenter(VisionOptions options) {
        super(VisionFeature.SELFIE_SEGMENTATION, options);
    }
}
