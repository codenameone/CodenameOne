/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.gpu.shader;

/// The value type of a `ShaderNode`. Every shading language the backends
/// target has these four, so a graph typed with them prints the same way on
/// all of them.
public enum ShaderType {
    /// One float.
    FLOAT(1),
    /// Two floats.
    VEC2(2),
    /// Three floats: a position, a direction or a colour.
    VEC3(3),
    /// Four floats: a colour with alpha, or a sampled texel.
    VEC4(4);

    private final int components;

    ShaderType(int components) {
        this.components = components;
    }

    /// Returns the number of float components, 1 to 4.
    ///
    /// #### Returns
    ///
    /// the component count
    public int getComponents() {
        return components;
    }

    /// Returns the type with the given number of components.
    ///
    /// #### Parameters
    ///
    /// - `n`: 1 to 4
    ///
    /// #### Returns
    ///
    /// the matching type
    public static ShaderType ofComponents(int n) {
        switch (n) {
            case 1:
                return FLOAT;
            case 2:
                return VEC2;
            case 3:
                return VEC3;
            case 4:
                return VEC4;
            default:
                throw new IllegalArgumentException("A shader value has 1 to 4 components, not " + n);
        }
    }
}
