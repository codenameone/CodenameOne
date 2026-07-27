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

/// Portable ranked image-classification label. Confidence is normalized to
/// 0..1. The numeric index is a backend/model class index and should not be
/// treated as stable across different backends; portable code should prefer
/// the text label.
public final class ImageLabel {
    private final String text;
    private final float confidence;
    private final int index;
    private final VisionMetadata metadata;

    /// Creates a label without backend metadata.
    /// @param text normalized label text
    /// @param confidence provider confidence, normally from zero to one
    /// @param index provider label index, or a negative value when unavailable
    public ImageLabel(String text, float confidence, int index) {
        this(text, confidence, index, null);
    }

    /// Creates a label with backend diagnostics.
    /// @param text normalized label text
    /// @param confidence provider confidence, normally from zero to one
    /// @param index provider label index, or a negative value when unavailable
    /// @param metadata backend details, or {@code null}
    public ImageLabel(String text, float confidence, int index,
                      VisionMetadata metadata) {
        this.text = text == null ? "" : text;
        this.confidence = confidence;
        this.index = index;
        this.metadata = metadata;
    }

    /// @return human-readable class label
    public String getText() {
        return text;
    }

    /// @return classification confidence in the range 0..1
    public float getConfidence() {
        return confidence;
    }

    /// Returns the backend/model class index when the selected classifier
    /// exposes one. The index identifies a class in that specific model; it is
    /// not portable across models or backends. Apple Vision does not expose a
    /// numeric class index and therefore returns {@code -1}. Prefer
    /// {@link #getText()} when writing backend-independent application logic.
    ///
    /// @return backend/model class index, or {@code -1} when unavailable
    public int getIndex() {
        return index;
    }

    /// @return backend metadata, or {@code null} when manually constructed
    public VisionMetadata getMetadata() {
        return metadata;
    }
}
