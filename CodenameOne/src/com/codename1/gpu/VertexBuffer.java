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

import com.codename1.ui.CN;

/// Holds interleaved vertex data for a `Mesh`. The backing store is allocated
/// through the platform SIMD allocator (`Simd.allocFloat(int)`) so that on
/// ParparVM the array lives at a fixed, aligned native address and can be handed
/// to the GPU with no intermediate copy. On other platforms the same array is an
/// ordinary `float[]`.
///
/// Mutate the data through `setData` or by writing into `getData()` and then
/// calling `setDirty()`; the bound `GraphicsDevice` re-uploads dirty buffers
/// before the next draw.
public final class VertexBuffer {
    private final VertexFormat format;
    private final int vertexCount;
    private final float[] data;
    private final int floatCount;
    private Object handle;
    private boolean dirty = true;

    /// Allocates a vertex buffer for the supplied format and vertex count. The
    /// backing array is SIMD aligned. Prefer creating buffers through
    /// `GraphicsDevice.createVertexBuffer(VertexFormat, int)` so the GPU handle
    /// is tracked by the device.
    ///
    /// #### Parameters
    ///
    /// - `format`: the interleaved vertex layout
    ///
    /// - `vertexCount`: the number of vertices the buffer can hold
    public VertexBuffer(VertexFormat format, int vertexCount) {
        if (format == null) {
            throw new IllegalArgumentException("format is required");
        }
        if (vertexCount <= 0) {
            throw new IllegalArgumentException("vertexCount must be positive");
        }
        this.format = format;
        this.vertexCount = vertexCount;
        this.floatCount = vertexCount * format.getFloatsPerVertex();
        int allocSize = floatCount < 16 ? 16 : floatCount;
        this.data = CN.getSimd().allocFloat(allocSize);
    }

    /// Returns the vertex layout of this buffer.
    public VertexFormat getFormat() {
        return format;
    }

    /// Returns the number of vertices this buffer holds.
    public int getVertexCount() {
        return vertexCount;
    }

    /// Returns the number of meaningful floats in the backing array
    /// (`vertexCount * format.getFloatsPerVertex()`). The array itself may be
    /// padded to a larger SIMD friendly size.
    public int getFloatCount() {
        return floatCount;
    }

    /// Returns the SIMD aligned backing array. Write vertex floats directly here
    /// for maximum throughput, then call `setDirty()`.
    public float[] getData() {
        return data;
    }

    /// Copies `src` into the backing array starting at float index 0 and marks
    /// the buffer dirty.
    ///
    /// #### Parameters
    ///
    /// - `src`: the interleaved float data; length must not exceed the buffer
    public void setData(float[] src) {
        if (src.length > data.length) {
            throw new IllegalArgumentException("source data exceeds buffer capacity");
        }
        for (int i = 0; i < src.length; i++) {
            data[i] = src[i];
        }
        dirty = true;
    }

    /// Marks the buffer as needing re-upload to the GPU before the next draw.
    public void setDirty() {
        dirty = true;
    }

    /// Returns true if the buffer has pending changes that must be uploaded.
    /// Intended for backend use.
    public boolean isDirty() {
        return dirty;
    }

    /// Clears the dirty flag. Intended for backend use after an upload.
    public void clearDirty() {
        dirty = false;
    }

    /// Returns the opaque backend GPU handle, or null if not yet uploaded.
    /// Intended for backend use.
    public Object getHandle() {
        return handle;
    }

    /// Stores the opaque backend GPU handle. Intended for backend use.
    ///
    /// #### Parameters
    ///
    /// - `handle`: the backend specific GPU resource handle
    public void setHandle(Object handle) {
        this.handle = handle;
    }
}
