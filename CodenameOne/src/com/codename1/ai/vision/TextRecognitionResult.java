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

/// OCR output with a portable block-level structure.
public final class TextRecognitionResult {
    public static final TextRecognitionResult EMPTY =
            new TextRecognitionResult("", new TextBlock[0]);

    private final String text;
    private final TextBlock[] blocks;
    private final VisionMetadata metadata;

    public TextRecognitionResult(String text, TextBlock[] blocks) {
        this(text, blocks, null);
    }

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

    public String getText() {
        return text;
    }

    public TextBlock[] getBlocks() {
        TextBlock[] out = new TextBlock[blocks.length];
        System.arraycopy(blocks, 0, out, 0, blocks.length);
        return out;
    }

    public VisionMetadata getMetadata() {
        return metadata;
    }

    public static final class TextBlock {
        private final String text;
        private final float confidence;
        private final VisionRect bounds;
        private final String languageTag;

        public TextBlock(String text, float confidence, VisionRect bounds, String languageTag) {
            this.text = text == null ? "" : text;
            this.confidence = confidence;
            this.bounds = bounds == null ? VisionRect.EMPTY : bounds;
            this.languageTag = languageTag;
        }

        public String getText() {
            return text;
        }

        public float getConfidence() {
            return confidence;
        }

        public VisionRect getBounds() {
            return bounds;
        }

        public String getLanguageTag() {
            return languageTag;
        }
    }
}
