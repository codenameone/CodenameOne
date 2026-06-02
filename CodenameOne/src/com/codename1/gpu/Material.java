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

/// A declarative description of how a surface should be shaded. The 3D engine is
/// responsible for translating a material into a platform shader (GLSL on OpenGL
/// ES and WebGL, Metal Shading Language on iOS); applications never write shader
/// source. A material combines a lighting model (`Type`), a base color, an
/// optional texture and pipeline `RenderState`.
public final class Material {
    /// The built in lighting model used to shade a surface.
    public enum Type {
        /// Flat, unlit shading. The fragment color is the base color modulated
        /// by the texture and vertex color. Ideal for UI, sprites and emissive
        /// surfaces.
        UNLIT,
        /// Diffuse only (Lambert) lighting using a single directional light.
        LAMBERT,
        /// Diffuse and specular (Blinn-Phong) lighting using a single
        /// directional light and the `shininess` property.
        PHONG,
        /// Unlit shading intended for screen aligned sprites and billboards.
        SPRITE,
        /// Unlit shading sampling a background cube/sky texture, rendered behind
        /// all other geometry.
        SKYBOX
    }

    private Type type = Type.UNLIT;
    private int color = 0xffffffff;
    private Texture texture;
    private float shininess = 32.0f;
    private RenderState renderState = RenderState.opaque();

    /// Creates an unlit white material.
    public Material() {
    }

    /// Creates a material of the supplied type.
    ///
    /// #### Parameters
    ///
    /// - `type`: the lighting model
    public Material(Type type) {
        this.type = type;
    }

    /// Returns the lighting model.
    public Type getType() {
        return type;
    }

    /// Sets the lighting model.
    ///
    /// #### Parameters
    ///
    /// - `type`: the lighting model
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setType(Type type) {
        this.type = type;
        return this;
    }

    /// Returns the base color as a packed ARGB integer.
    public int getColor() {
        return color;
    }

    /// Sets the base color as a packed ARGB integer (0xAARRGGBB).
    ///
    /// #### Parameters
    ///
    /// - `argb`: the packed ARGB color
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setColor(int argb) {
        this.color = argb;
        return this;
    }

    /// Returns the diffuse texture, or null when the material is untextured.
    public Texture getTexture() {
        return texture;
    }

    /// Sets the diffuse texture. Pass null for an untextured material.
    ///
    /// #### Parameters
    ///
    /// - `texture`: the diffuse texture or null
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setTexture(Texture texture) {
        this.texture = texture;
        return this;
    }

    /// Returns the Phong specular exponent.
    public float getShininess() {
        return shininess;
    }

    /// Sets the Phong specular exponent (used only by `Type.PHONG`).
    ///
    /// #### Parameters
    ///
    /// - `shininess`: the specular exponent
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setShininess(float shininess) {
        this.shininess = shininess;
        return this;
    }

    /// Returns the pipeline render state.
    public RenderState getRenderState() {
        return renderState;
    }

    /// Sets the pipeline render state.
    ///
    /// #### Parameters
    ///
    /// - `renderState`: the render state
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setRenderState(RenderState renderState) {
        this.renderState = renderState;
        return this;
    }

    /// Returns a stable string identifying the shader variant required by this
    /// material. Backends use it together with the mesh `VertexFormat` to cache
    /// generated and compiled shader programs. The key intentionally depends
    /// only on properties that change the generated source, not on values such
    /// as the color which are passed as uniforms.
    ///
    /// #### Returns
    ///
    /// a shader variant cache key
    public String getShaderKey() {
        return type.name() + (texture != null ? "|tex" : "|notex");
    }
}
