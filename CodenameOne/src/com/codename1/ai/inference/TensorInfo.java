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
package com.codename1.ai.inference;

/// Immutable metadata for one model input or output. Shapes may contain a
/// negative dynamic dimension until the input has been resized and tensors
/// reallocated.
public final class TensorInfo {
    private final String name;
    private final TensorType type;
    private final int[] shape;
    private final int index;

    /// Creates tensor metadata.
    /// @param name runtime tensor name, possibly empty
    /// @param type element type
    /// @param shape current tensor dimensions
    /// @param index native model index
    public TensorInfo(String name, TensorType type, int[] shape, int index) {
        this.name = name;
        this.type = type;
        this.shape = copy(shape);
        this.index = index;
    }

    /// @return the runtime tensor name
    public String getName() {
        return name;
    }

    /// @return the tensor element type
    public TensorType getType() {
        return type;
    }

    /// @return a defensive copy of current dimensions
    public int[] getShape() {
        return copy(shape);
    }

    /// @return the zero-based model input or output index
    public int getIndex() {
        return index;
    }

    private static int[] copy(int[] value) {
        if (value == null) {
            return new int[0];
        }
        int[] out = new int[value.length];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
    }
}
