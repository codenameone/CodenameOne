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

/// An ordered, interleaved layout of `VertexAttribute`s describing how the
/// floats of a `VertexBuffer` are grouped into vertices. All attributes are
/// tightly packed in declaration order; the stride is the sum of the component
/// counts. A handful of common formats are provided as constants.
public final class VertexFormat {
    /// Position only (3 floats).
    public static final VertexFormat POSITION = new VertexFormat(new VertexAttribute[]{
            new VertexAttribute(VertexAttribute.Usage.POSITION, 3)
    });

    /// Position and texture coordinate (3 + 2 floats).
    public static final VertexFormat POSITION_TEXCOORD = new VertexFormat(new VertexAttribute[]{
            new VertexAttribute(VertexAttribute.Usage.POSITION, 3),
            new VertexAttribute(VertexAttribute.Usage.TEXCOORD, 2)
    });

    /// Position and normal (3 + 3 floats).
    public static final VertexFormat POSITION_NORMAL = new VertexFormat(new VertexAttribute[]{
            new VertexAttribute(VertexAttribute.Usage.POSITION, 3),
            new VertexAttribute(VertexAttribute.Usage.NORMAL, 3)
    });

    /// Position, normal and texture coordinate (3 + 3 + 2 floats). The common
    /// format for lit, textured meshes.
    public static final VertexFormat POSITION_NORMAL_TEXCOORD = new VertexFormat(new VertexAttribute[]{
            new VertexAttribute(VertexAttribute.Usage.POSITION, 3),
            new VertexAttribute(VertexAttribute.Usage.NORMAL, 3),
            new VertexAttribute(VertexAttribute.Usage.TEXCOORD, 2)
    });

    private final VertexAttribute[] attributes;
    private final int floatsPerVertex;

    /// Creates a vertex format from the supplied attributes in interleaved order.
    ///
    /// #### Parameters
    ///
    /// - `attributes`: the attributes that make up a single vertex
    public VertexFormat(VertexAttribute[] attributes) {
        if (attributes == null || attributes.length == 0) {
            throw new IllegalArgumentException("at least one attribute is required");
        }
        this.attributes = new VertexAttribute[attributes.length];
        int total = 0;
        for (int i = 0; i < attributes.length; i++) {
            this.attributes[i] = attributes[i];
            total += attributes[i].getComponents();
        }
        this.floatsPerVertex = total;
    }

    /// Returns the number of attributes in this format.
    public int getAttributeCount() {
        return attributes.length;
    }

    /// Returns the attribute at the supplied index in declaration order.
    public VertexAttribute getAttribute(int index) {
        return attributes[index];
    }

    /// Returns the float offset of the attribute at the supplied index within a
    /// vertex.
    public int getAttributeOffset(int index) {
        int offset = 0;
        for (int i = 0; i < index; i++) {
            offset += attributes[i].getComponents();
        }
        return offset;
    }

    /// Returns the first attribute matching the supplied usage, or null when the
    /// format does not contain it.
    public VertexAttribute findByUsage(VertexAttribute.Usage usage) {
        for (VertexAttribute a : attributes) {
            if (a.getUsage() == usage) {
                return a;
            }
        }
        return null;
    }

    /// Returns the number of floats that make up a single vertex (the stride
    /// measured in floats).
    public int getFloatsPerVertex() {
        return floatsPerVertex;
    }

    /// Returns the stride of a vertex in bytes.
    public int getStrideBytes() {
        return floatsPerVertex * 4;
    }
}
