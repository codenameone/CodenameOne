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

/// Immutable description of the capabilities and limits of a `GraphicsDevice`.
/// Backends create an instance describing the underlying GPU so portable code
/// can adapt to the platform. Applications retrieve it via
/// `GraphicsDevice.getCapabilities()`.
public final class GpuCapabilities {
    private final int maxTextureSize;
    private final int maxVertexAttributes;
    private final boolean shaderLevel3;
    private final boolean depthTextureSupported;
    private final boolean intIndicesSupported;
    private final String rendererName;

    /// Constructs a capabilities descriptor. Intended to be called by platform
    /// backends only.
    ///
    /// #### Parameters
    ///
    /// - `maxTextureSize`: the maximum supported texture edge length in pixels
    ///
    /// - `maxVertexAttributes`: the maximum number of vertex attributes
    ///
    /// - `shaderLevel3`: true if GLSL ES 3 / WebGL2 class shaders are available
    ///
    /// - `depthTextureSupported`: true if sampling depth textures is supported
    ///
    /// - `intIndicesSupported`: true if 32 bit element indices are supported
    ///
    /// - `rendererName`: a human readable backend/renderer description
    public GpuCapabilities(int maxTextureSize, int maxVertexAttributes,
                           boolean shaderLevel3, boolean depthTextureSupported,
                           boolean intIndicesSupported, String rendererName) {
        this.maxTextureSize = maxTextureSize;
        this.maxVertexAttributes = maxVertexAttributes;
        this.shaderLevel3 = shaderLevel3;
        this.depthTextureSupported = depthTextureSupported;
        this.intIndicesSupported = intIndicesSupported;
        this.rendererName = rendererName;
    }

    /// Returns the maximum supported texture edge length in pixels.
    public int getMaxTextureSize() {
        return maxTextureSize;
    }

    /// Returns the maximum number of vertex attributes supported per draw.
    public int getMaxVertexAttributes() {
        return maxVertexAttributes;
    }

    /// Returns true if GLSL ES 3 / WebGL2 class shading features are available.
    public boolean isShaderLevel3() {
        return shaderLevel3;
    }

    /// Returns true if depth textures may be sampled (useful for shadow mapping).
    public boolean isDepthTextureSupported() {
        return depthTextureSupported;
    }

    /// Returns true if 32 bit (int) element indices are supported. When false an
    /// `IndexBuffer` is limited to 16 bit indices.
    public boolean isIntIndicesSupported() {
        return intIndicesSupported;
    }

    /// Returns a human readable description of the backend and GPU.
    public String getRendererName() {
        return rendererName;
    }
}
