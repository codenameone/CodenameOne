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

/// OCR output containing the complete recognized text and portable
/// block-level geometry. Block bounds use normalized top-left-origin
/// coordinates; a language tag may be absent when the backend does not expose
/// per-block language identification.
public final class TextRecognitionResult {
    public static final TextRecognitionResult EMPTY =
            new TextRecognitionResult("", new TextBlock[0]);

    private final String text;
    private final TextBlock[] blocks;
    private final VisionMetadata metadata;

    /// Creates an OCR result without backend metadata.
    /// @param text full recognized text in reading order
    /// @param blocks structured text regions, defensively copied
    public TextRecognitionResult(String text, TextBlock[] blocks) {
        this(text, blocks, null);
    }

    /// Creates an OCR result with backend diagnostics.
    /// @param text full recognized text in reading order
    /// @param blocks structured text regions, defensively copied
    /// @param metadata backend details, or {@code null}
    public TextRecognitionResult(String text, TextBlock[] blocks,
                                 VisionMetadata metadata) {
        this.text = text == null ? "" : text;
        if (blocks == null) {
            this.blocks = new TextBlock[0];
        } else {
            this.blocks = new TextBlock[blocks.length];
            System.arraycopy(blocks, 0, this.blocks, 0, blocks.length);
        }
        this.metadata = metadata;
    }

    /// @return complete recognized text in reading order
    public String getText() {
        return text;
    }

    /// @return defensive copy of recognized blocks
    public TextBlock[] getBlocks() {
        TextBlock[] out = new TextBlock[blocks.length];
        System.arraycopy(blocks, 0, out, 0, blocks.length);
        return out;
    }

    /// @return backend metadata, or {@code null} when manually constructed
    public VisionMetadata getMetadata() {
        return metadata;
    }

    /// One recognized text block with confidence and normalized bounds.
    public static final class TextBlock {
        private final String text;
        private final float confidence;
        private final VisionRect bounds;
        private final String languageTag;

        /// Creates one OCR block.
        /// @param text recognized text
        /// @param confidence recognition confidence in 0..1
        /// @param bounds normalized block bounds
        /// @param languageTag BCP-47 tag, or {@code null}
        public TextBlock(String text, float confidence, VisionRect bounds, String languageTag) {
            this.text = text == null ? "" : text;
            this.confidence = confidence;
            this.bounds = bounds == null ? VisionRect.EMPTY : bounds;
            this.languageTag = languageTag;
        }

        /// @return recognized block text
        public String getText() {
            return text;
        }

        /// @return recognition confidence in the range 0..1
        public float getConfidence() {
            return confidence;
        }

        /// @return normalized top-left-origin bounds
        public VisionRect getBounds() {
            return bounds;
        }

        /// @return BCP-47 language tag, or {@code null} when unavailable
        public String getLanguageTag() {
            return languageTag;
        }
    }
}
