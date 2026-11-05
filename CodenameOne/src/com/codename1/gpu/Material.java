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
        /// Physically based shading (metallic/roughness with a GGX specular
        /// lobe, the model glTF describes) using the directional light, the
        /// environment's hemisphere ambient and, when the light casts them,
        /// shadows. See `setMetallic`, `setRoughness` and `setEmissive`.
        PBR,
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
    private float metallic;
    private float roughness = 0.8f;
    private int emissive = 0xff000000;
    private boolean receiveShadows = true;
    private boolean castShadows = true;

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

    /// Returns how metallic a `Type.PBR` surface is, from 0 (a dielectric such
    /// as paint, plastic or wood) to 1 (bare metal).
    public float getMetallic() {
        return metallic;
    }

    /// Sets how metallic a `Type.PBR` surface is.
    ///
    /// #### Parameters
    ///
    /// - `metallic`: 0 for a dielectric, 1 for metal
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setMetallic(float metallic) {
        this.metallic = Math.max(0f, Math.min(1f, metallic));
        return this;
    }

    /// Returns how rough a `Type.PBR` surface is, from 0 (mirror) to 1 (chalk).
    public float getRoughness() {
        return roughness;
    }

    /// Sets how rough a `Type.PBR` surface is. Rough surfaces spread their
    /// highlight wide and dim; smooth ones make it small and bright.
    ///
    /// #### Parameters
    ///
    /// - `roughness`: 0 for a mirror, 1 for fully diffuse
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setRoughness(float roughness) {
        this.roughness = Math.max(0.04f, Math.min(1f, roughness));
        return this;
    }

    /// Returns the light the surface gives off by itself, as RGB in an ARGB
    /// int; black for none.
    public int getEmissive() {
        return emissive;
    }

    /// Sets the light the surface gives off by itself: a lamp, a screen, wet
    /// paint catching the sun. Emissive colour is added after lighting and is
    /// what a bloom post effect picks up. Used by `Type.PBR`, `Type.LAMBERT`
    /// and `Type.PHONG`.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the emitted colour; black for none
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setEmissive(int argb) {
        this.emissive = argb | 0xff000000;
        return this;
    }

    /// Returns true if the surface is darkened by shadows the light casts.
    public boolean isReceiveShadows() {
        return receiveShadows;
    }

    /// Sets whether the surface is darkened by the light's shadows. A ground
    /// plane receives them; a particle or a muzzle flash should not.
    ///
    /// #### Parameters
    ///
    /// - `receiveShadows`: true to receive shadows
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setReceiveShadows(boolean receiveShadows) {
        this.receiveShadows = receiveShadows;
        return this;
    }

    /// Returns true if geometry drawn with this material casts a shadow.
    public boolean isCastShadows() {
        return castShadows;
    }

    /// Sets whether geometry drawn with this material casts a shadow.
    /// Transparent and unlit effects normally should not.
    ///
    /// #### Parameters
    ///
    /// - `castShadows`: true to cast shadows
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setCastShadows(boolean castShadows) {
        this.castShadows = castShadows;
        return this;
    }

    /// Returns true if this material is lit by the directional light.
    public boolean isLit() {
        return type == Type.LAMBERT || type == Type.PHONG || type == Type.PBR;
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
        return type.name() + (texture != null ? "|tex" : "|notex")
                + (receiveShadows ? "|rs" : "|nrs") + ((emissive & 0xffffff) != 0 ? "|em" : "");
    }
}
