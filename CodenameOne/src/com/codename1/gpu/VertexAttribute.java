/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.gpu;

/// Describes a single attribute (position, normal, texture coordinate, color)
/// within a `VertexFormat`. The engine derives the generated shader's vertex
/// inputs from the attributes present in the format, which is why a fixed set of
/// well known usages is used rather than free form names.
public final class VertexAttribute {
    /// The semantic meaning of a vertex attribute. The engine binds each usage
    /// to a known shader input and a known purpose in the generated materials.
    public enum Usage {
        /// Object space vertex position. Typically 3 float components.
        POSITION,
        /// Object space vertex normal. Typically 3 float components.
        NORMAL,
        /// Primary texture coordinate. Typically 2 float components.
        TEXCOORD,
        /// Per vertex color. Typically 4 components.
        COLOR
    }

    private final Usage usage;
    private final int components;

    /// Creates a float backed attribute.
    ///
    /// #### Parameters
    ///
    /// - `usage`: the semantic usage of the attribute
    ///
    /// - `components`: the number of float components (1 to 4)
    public VertexAttribute(Usage usage, int components) {
        if (components < 1 || components > 4) {
            throw new IllegalArgumentException("components must be between 1 and 4");
        }
        this.usage = usage;
        this.components = components;
    }

    /// Returns the semantic usage of this attribute.
    public Usage getUsage() {
        return usage;
    }

    /// Returns the number of float components in this attribute.
    public int getComponents() {
        return components;
    }
}
