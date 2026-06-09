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

/// Holds the element indices used to assemble primitives from a `VertexBuffer`.
/// Indices are stored as 16 bit unsigned values (`short`) which is the portable
/// baseline supported on every backend including WebGL 1. A buffer therefore
/// addresses at most 65536 distinct vertices.
public final class IndexBuffer {
    private final short[] data;
    private final int indexCount;
    private Object handle;
    private boolean dirty = true;

    /// Allocates an index buffer with room for `indexCount` indices. Prefer
    /// creating buffers through `GraphicsDevice.createIndexBuffer(int)` so the
    /// GPU handle is tracked by the device.
    ///
    /// #### Parameters
    ///
    /// - `indexCount`: the number of indices the buffer can hold
    public IndexBuffer(int indexCount) {
        if (indexCount <= 0) {
            throw new IllegalArgumentException("indexCount must be positive");
        }
        this.indexCount = indexCount;
        this.data = new short[indexCount];
    }

    /// Returns the number of indices this buffer holds.
    public int getIndexCount() {
        return indexCount;
    }

    /// Returns the backing short array. Index values are treated as unsigned.
    public short[] getData() {
        return data;
    }

    /// Copies `src` into the backing array and marks the buffer dirty. Each int
    /// must fit in an unsigned 16 bit range.
    ///
    /// #### Parameters
    ///
    /// - `src`: the index values to store
    public void setData(int[] src) {
        if (src.length > data.length) {
            throw new IllegalArgumentException("source data exceeds buffer capacity");
        }
        for (int i = 0; i < src.length; i++) {
            if (src[i] < 0 || src[i] > 65535) {
                throw new IllegalArgumentException("index out of unsigned short range: " + src[i]);
            }
            data[i] = (short) src[i];
        }
        dirty = true;
    }

    /// Marks the buffer as needing re-upload to the GPU before the next draw.
    public void setDirty() {
        dirty = true;
    }

    /// Returns true if the buffer has pending changes. Intended for backend use.
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
