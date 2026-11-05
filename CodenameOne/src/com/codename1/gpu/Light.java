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

/// A single directional light plus a global ambient term, consumed by lit
/// materials (`Material.Type.LAMBERT` and `Material.Type.PHONG`). The direction
/// is the direction the light travels, in world space. Set the active light on
/// the device with `GraphicsDevice.setLight(Light)`.
public final class Light {
    private float dirX = -0.5f;
    private float dirY = -1.0f;
    private float dirZ = -0.5f;
    private int color = 0xffffffff;
    private int ambientColor = 0xff404040;
    private boolean shadows;
    private int shadowMapSize = 1024;
    private float shadowExtent = 20f;
    private float shadowCenterX;
    private float shadowCenterY;
    private float shadowCenterZ;
    private float shadowBias = 0.0015f;
    private float shadowStrength = 0.8f;

    /// Sets the world space direction the light travels.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x component
    ///
    /// - `y`: the y component
    ///
    /// - `z`: the z component
    ///
    /// #### Returns
    ///
    /// this light for chaining
    public Light setDirection(float x, float y, float z) {
        this.dirX = x;
        this.dirY = y;
        this.dirZ = z;
        return this;
    }

    /// Returns the x component of the light direction.
    public float getDirectionX() {
        return dirX;
    }

    /// Returns the y component of the light direction.
    public float getDirectionY() {
        return dirY;
    }

    /// Returns the z component of the light direction.
    public float getDirectionZ() {
        return dirZ;
    }

    /// Returns the light color as a packed ARGB integer.
    public int getColor() {
        return color;
    }

    /// Sets the light color as a packed ARGB integer.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the packed ARGB color
    ///
    /// #### Returns
    ///
    /// this light for chaining
    public Light setColor(int argb) {
        this.color = argb;
        return this;
    }

    /// Returns the ambient color as a packed ARGB integer.
    public int getAmbientColor() {
        return ambientColor;
    }

    /// Sets the ambient color as a packed ARGB integer.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the packed ARGB color
    ///
    /// #### Returns
    ///
    /// this light for chaining
    public Light setAmbientColor(int argb) {
        this.ambientColor = argb;
        return this;
    }

    /// Turns shadow casting on or off. With shadows on, a `GraphicsDevice`
    /// that supports them (`GpuCapabilities.isShadowsSupported`) renders the
    /// scene from the light into a shadow map before each frame, and lit
    /// materials that receive shadows are darkened where it is blocked.
    ///
    /// The map covers a square `extent` units either side of the shadow centre,
    /// seen along the light's direction; move the centre with the camera
    /// (`setShadowCenter`) and keep the extent as small as the view allows,
    /// since the map's resolution is spread over it.
    ///
    /// #### Parameters
    ///
    /// - `shadows`: true to cast shadows
    ///
    /// #### Returns
    ///
    /// this light for chaining
    public Light setShadows(boolean shadows) {
        this.shadows = shadows;
        return this;
    }

    /// Returns true if the light casts shadows.
    public boolean isShadows() {
        return shadows;
    }

    /// Sets the area and resolution of the shadow map.
    ///
    /// #### Parameters
    ///
    /// - `size`: the width and height of the map in texels; clamped to 256 to 4096
    ///
    /// - `extent`: the half width of the square it covers, in world units
    ///
    /// #### Returns
    ///
    /// this light for chaining
    public Light setShadowMap(int size, float extent) {
        this.shadowMapSize = Math.max(256, Math.min(4096, size));
        this.shadowExtent = Math.max(0.01f, extent);
        return this;
    }

    /// Returns the shadow map's width and height in texels.
    public int getShadowMapSize() {
        return shadowMapSize;
    }

    /// Returns the x of the point the shadow map is centred on.
    public float getShadowCenterX() {
        return shadowCenterX;
    }

    /// Returns the z of the point the shadow map is centred on.
    public float getShadowCenterZ() {
        return shadowCenterZ;
    }

    /// Returns the half width of the area the shadow map covers.
    public float getShadowExtent() {
        return shadowExtent;
    }

    /// Centres the shadow map on a world point, normally the one the camera
    /// looks at.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate
    ///
    /// - `y`: the y coordinate
    ///
    /// - `z`: the z coordinate
    ///
    /// #### Returns
    ///
    /// this light for chaining
    public Light setShadowCenter(float x, float y, float z) {
        this.shadowCenterX = x;
        this.shadowCenterY = y;
        this.shadowCenterZ = z;
        return this;
    }

    /// Sets how dark a shadow is and how far a surface must be behind the
    /// shadow map before it counts as shadowed.
    ///
    /// #### Parameters
    ///
    /// - `strength`: 0 for no darkening, 1 for no direct light at all
    ///
    /// - `bias`: the depth offset in the map's 0..1 range; raise it if
    ///   surfaces shadow themselves in stripes ("acne"), lower it if shadows
    ///   detach from the objects that cast them
    ///
    /// #### Returns
    ///
    /// this light for chaining
    public Light setShadowStyle(float strength, float bias) {
        this.shadowStrength = Math.max(0f, Math.min(1f, strength));
        this.shadowBias = Math.max(0f, bias);
        return this;
    }

    /// Returns how dark a shadow is, from 0 to 1.
    public float getShadowStrength() {
        return shadowStrength;
    }

    /// Returns the shadow depth bias.
    public float getShadowBias() {
        return shadowBias;
    }

    /// Computes the matrix that takes a world position to the shadow map's
    /// clip space: an orthographic view along the light's direction, centred
    /// on the shadow centre. Every backend uses this one, so shadows land in
    /// the same place on each.
    ///
    /// #### Parameters
    ///
    /// - `out`: a 16 element array that receives the column-major matrix
    public void computeShadowMatrix(float[] out) {
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        float dx = len > 0 ? dirX / len : 0;
        float dy = len > 0 ? dirY / len : -1;
        float dz = len > 0 ? dirZ / len : 0;
        float back = shadowExtent * 2f;
        float upX = 0;
        float upY = 1;
        float upZ = 0;
        if (Math.abs(dy) > 0.99f) {
            upY = 0;
            upZ = 1;
        }
        float[] view = Matrix4.lookAt(shadowCenterX - dx * back, shadowCenterY - dy * back,
                shadowCenterZ - dz * back, shadowCenterX, shadowCenterY, shadowCenterZ, upX, upY, upZ);
        // Snap the centre to whole texels in light space, so the map does not
        // shimmer as the camera moves.
        float texel = shadowExtent * 2f / shadowMapSize;
        view[12] = (float) Math.floor(view[12] / texel) * texel;
        view[13] = (float) Math.floor(view[13] / texel) * texel;
        float[] proj = Matrix4.ortho(-shadowExtent, shadowExtent, -shadowExtent, shadowExtent, 0.05f, back * 2f);
        Matrix4.multiply(proj, view, out);
    }
}
