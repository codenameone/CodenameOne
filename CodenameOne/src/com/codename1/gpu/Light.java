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
}
