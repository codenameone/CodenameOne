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

/// Fixed function pipeline state attached to a `Material`: depth testing, alpha
/// blending and face culling. Sensible defaults are provided for opaque 3D
/// geometry (depth test and write on, no blending, back faces culled).
public final class RenderState {
    /// The alpha blending mode applied when a fragment is written.
    public enum BlendMode {
        /// No blending; the fragment overwrites the destination.
        NONE,
        /// Standard source-over alpha blending.
        ALPHA,
        /// Additive blending, useful for particles and glows.
        ADDITIVE
    }

    /// Which triangle faces are discarded before rasterization.
    public enum CullMode {
        /// Render both faces.
        NONE,
        /// Discard back faces (counter clockwise winding is front).
        BACK,
        /// Discard front faces.
        FRONT
    }

    private boolean depthTest = true;
    private boolean depthWrite = true;
    private BlendMode blendMode = BlendMode.NONE;
    private CullMode cullMode = CullMode.BACK;

    /// Returns a render state suitable for opaque geometry.
    public static RenderState opaque() {
        return new RenderState();
    }

    /// Returns a render state suitable for alpha blended, non depth writing
    /// transparent geometry.
    public static RenderState transparent() {
        return new RenderState()
                .setBlendMode(BlendMode.ALPHA)
                .setDepthWrite(false);
    }

    /// Returns true if depth testing is enabled.
    public boolean isDepthTest() {
        return depthTest;
    }

    /// Enables or disables depth testing.
    ///
    /// #### Parameters
    ///
    /// - `depthTest`: true to enable depth testing
    ///
    /// #### Returns
    ///
    /// this state for chaining
    public RenderState setDepthTest(boolean depthTest) {
        this.depthTest = depthTest;
        return this;
    }

    /// Returns true if writing to the depth buffer is enabled.
    public boolean isDepthWrite() {
        return depthWrite;
    }

    /// Enables or disables writing to the depth buffer.
    ///
    /// #### Parameters
    ///
    /// - `depthWrite`: true to write depth values
    ///
    /// #### Returns
    ///
    /// this state for chaining
    public RenderState setDepthWrite(boolean depthWrite) {
        this.depthWrite = depthWrite;
        return this;
    }

    /// Returns the configured blend mode.
    public BlendMode getBlendMode() {
        return blendMode;
    }

    /// Sets the blend mode.
    ///
    /// #### Parameters
    ///
    /// - `blendMode`: the blend mode
    ///
    /// #### Returns
    ///
    /// this state for chaining
    public RenderState setBlendMode(BlendMode blendMode) {
        this.blendMode = blendMode;
        return this;
    }

    /// Returns the configured cull mode.
    public CullMode getCullMode() {
        return cullMode;
    }

    /// Sets the face culling mode.
    ///
    /// #### Parameters
    ///
    /// - `cullMode`: the cull mode
    ///
    /// #### Returns
    ///
    /// this state for chaining
    public RenderState setCullMode(CullMode cullMode) {
        this.cullMode = cullMode;
        return this;
    }
}
