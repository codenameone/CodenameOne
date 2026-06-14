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

/// Renderable geometry: a `VertexBuffer`, an optional `IndexBuffer` and the
/// `PrimitiveType` that ties the vertices into shapes. A mesh carries no
/// material; the same mesh can be drawn with different materials through
/// `GraphicsDevice.draw(Mesh, Material, float[])`.
public final class Mesh {
    private final VertexBuffer vertices;
    private final IndexBuffer indices;
    private final PrimitiveType primitiveType;

    /// Creates a non indexed mesh.
    ///
    /// #### Parameters
    ///
    /// - `vertices`: the vertex data
    ///
    /// - `primitiveType`: how the vertices are assembled into primitives
    public Mesh(VertexBuffer vertices, PrimitiveType primitiveType) {
        this(vertices, null, primitiveType);
    }

    /// Creates an indexed mesh.
    ///
    /// #### Parameters
    ///
    /// - `vertices`: the vertex data
    ///
    /// - `indices`: the element indices, or null for a non indexed mesh
    ///
    /// - `primitiveType`: how the vertices are assembled into primitives
    public Mesh(VertexBuffer vertices, IndexBuffer indices, PrimitiveType primitiveType) {
        if (vertices == null) {
            throw new IllegalArgumentException("vertices are required");
        }
        if (primitiveType == null) {
            throw new IllegalArgumentException("primitiveType is required");
        }
        this.vertices = vertices;
        this.indices = indices;
        this.primitiveType = primitiveType;
    }

    /// Returns the vertex buffer.
    public VertexBuffer getVertices() {
        return vertices;
    }

    /// Returns the index buffer, or null for a non indexed mesh.
    public IndexBuffer getIndices() {
        return indices;
    }

    /// Returns true if this mesh is drawn with an index buffer.
    public boolean isIndexed() {
        return indices != null;
    }

    /// Returns the primitive assembly type.
    public PrimitiveType getPrimitiveType() {
        return primitiveType;
    }
}
