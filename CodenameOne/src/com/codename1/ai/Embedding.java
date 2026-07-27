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
package com.codename1.ai;

/// One vector returned by an embedding provider. The vector is defensively
/// copied on construction and access so callers cannot mutate stored results.
/// {@link #getIndex()} identifies the corresponding input position in a
/// batched {@link EmbeddingRequest}.
public final class Embedding {
    private final float[] vector;
    private final int index;

    /// Creates an embedding result.
    /// @param vector numeric embedding coordinates, defensively copied; a
    ///  {@code null} value creates an empty vector
    /// @param index zero-based position of the corresponding request input
    public Embedding(float[] vector, int index) {
        this.vector = copy(vector);
        this.index = index;
    }

    /// @return a defensive copy of the embedding coordinates
    public float[] getVector() {
        return copy(vector);
    }

    /// @return zero-based position of this item in the request
    public int getIndex() {
        return index;
    }

    /// @return number of coordinates in the embedding
    public int getDimensions() {
        return vector.length;
    }

    private static float[] copy(float[] value) {
        if (value == null) {
            return new float[0];
        }
        float[] result = new float[value.length];
        System.arraycopy(value, 0, result, 0, value.length);
        return result;
    }
}
