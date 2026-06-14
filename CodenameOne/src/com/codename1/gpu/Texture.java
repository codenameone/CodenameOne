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

/// A GPU texture. Instances are created by a `GraphicsDevice` from a Codename
/// One `Image` or from raw ARGB pixel data and then referenced by a `Material`.
/// The class itself is a lightweight handle; the pixel storage lives on the GPU.
public final class Texture {
    /// The texture coordinate wrapping behavior outside the 0..1 range.
    public enum Wrap {
        /// Clamp coordinates to the edge texels.
        CLAMP,
        /// Tile the texture by repeating it.
        REPEAT
    }

    /// The sampling filter applied when the texture is scaled.
    public enum Filter {
        /// Nearest texel sampling (blocky, sharp).
        NEAREST,
        /// Bilinear sampling (smooth).
        LINEAR
    }

    private final int width;
    private final int height;
    private Wrap wrap = Wrap.CLAMP;
    private Filter filter = Filter.LINEAR;
    private Object handle;

    /// Creates a texture handle of the given dimensions. Intended for backend
    /// use; applications create textures via `GraphicsDevice`.
    ///
    /// #### Parameters
    ///
    /// - `width`: the texture width in pixels
    ///
    /// - `height`: the texture height in pixels
    public Texture(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /// Returns the texture width in pixels.
    public int getWidth() {
        return width;
    }

    /// Returns the texture height in pixels.
    public int getHeight() {
        return height;
    }

    /// Returns the configured wrapping mode.
    public Wrap getWrap() {
        return wrap;
    }

    /// Sets the wrapping mode. Takes effect on the next bind by the backend.
    ///
    /// #### Parameters
    ///
    /// - `wrap`: the wrapping mode
    ///
    /// #### Returns
    ///
    /// this texture for chaining
    public Texture setWrap(Wrap wrap) {
        this.wrap = wrap;
        return this;
    }

    /// Returns the configured sampling filter.
    public Filter getFilter() {
        return filter;
    }

    /// Sets the sampling filter. Takes effect on the next bind by the backend.
    ///
    /// #### Parameters
    ///
    /// - `filter`: the sampling filter
    ///
    /// #### Returns
    ///
    /// this texture for chaining
    public Texture setFilter(Filter filter) {
        this.filter = filter;
        return this;
    }

    /// Returns the opaque backend GPU handle. Intended for backend use.
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
