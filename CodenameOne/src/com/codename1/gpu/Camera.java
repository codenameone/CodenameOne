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

/// A perspective or orthographic camera. The camera builds a view matrix from an
/// eye position, a look-at target and an up vector, and a projection matrix from
/// its lens parameters. Combine the two through `getViewProjection()` which the
/// device multiplies with each model matrix.
public final class Camera {
    private boolean perspective = true;
    private float fovYRadians = (float) Math.toRadians(60.0);
    private float aspect = 1.0f;
    private float near = 0.1f;
    private float far = 100.0f;
    private float orthoHeight = 2.0f;

    private float eyeX = 0.0f;
    private float eyeY = 0.0f;
    private float eyeZ = 5.0f;
    private float targetX = 0.0f;
    private float targetY = 0.0f;
    private float targetZ = 0.0f;
    private float upX = 0.0f;
    private float upY = 1.0f;
    private float upZ = 0.0f;

    private final float[] view = Matrix4.identity();
    private final float[] projection = Matrix4.identity();
    private final float[] viewProjection = Matrix4.identity();
    private boolean dirty = true;

    /// Configures a perspective projection.
    ///
    /// #### Parameters
    ///
    /// - `fovYDegrees`: the vertical field of view in degrees
    ///
    /// - `near`: the near clip plane distance
    ///
    /// - `far`: the far clip plane distance
    ///
    /// #### Returns
    ///
    /// this camera for chaining
    public Camera setPerspective(float fovYDegrees, float near, float far) {
        this.perspective = true;
        this.fovYRadians = (float) Math.toRadians(fovYDegrees);
        this.near = near;
        this.far = far;
        dirty = true;
        return this;
    }

    /// Configures an orthographic projection.
    ///
    /// #### Parameters
    ///
    /// - `height`: the visible world height; width is derived from the aspect
    ///
    /// - `near`: the near clip plane distance
    ///
    /// - `far`: the far clip plane distance
    ///
    /// #### Returns
    ///
    /// this camera for chaining
    public Camera setOrthographic(float height, float near, float far) {
        this.perspective = false;
        this.orthoHeight = height;
        this.near = near;
        this.far = far;
        dirty = true;
        return this;
    }

    /// Sets the viewport aspect ratio (width / height). The `RenderView`
    /// normally calls this from `Renderer.onResize`.
    ///
    /// #### Parameters
    ///
    /// - `aspect`: the width / height ratio
    ///
    /// #### Returns
    ///
    /// this camera for chaining
    public Camera setAspect(float aspect) {
        this.aspect = aspect;
        dirty = true;
        return this;
    }

    /// Sets the eye (camera) world position.
    public Camera setPosition(float x, float y, float z) {
        this.eyeX = x;
        this.eyeY = y;
        this.eyeZ = z;
        dirty = true;
        return this;
    }

    /// Sets the world space point the camera looks at.
    public Camera setTarget(float x, float y, float z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        dirty = true;
        return this;
    }

    /// Sets the camera up vector.
    public Camera setUp(float x, float y, float z) {
        this.upX = x;
        this.upY = y;
        this.upZ = z;
        dirty = true;
        return this;
    }

    /// Returns the 16 element column-major view matrix.
    public float[] getViewMatrix() {
        recompute();
        return view;
    }

    /// Returns the 16 element column-major projection matrix.
    public float[] getProjectionMatrix() {
        recompute();
        return projection;
    }

    /// Returns the 16 element column-major combined projection * view matrix.
    public float[] getViewProjection() {
        recompute();
        return viewProjection;
    }

    private void recompute() {
        if (!dirty) {
            return;
        }
        float[] v = Matrix4.lookAt(eyeX, eyeY, eyeZ, targetX, targetY, targetZ, upX, upY, upZ);
        Matrix4.copy(v, view);
        float[] p;
        if (perspective) {
            p = Matrix4.perspective(fovYRadians, aspect, near, far);
        } else {
            float halfH = orthoHeight * 0.5f;
            float halfW = halfH * aspect;
            p = Matrix4.ortho(-halfW, halfW, -halfH, halfH, near, far);
        }
        Matrix4.copy(p, projection);
        Matrix4.multiply(projection, view, viewProjection);
        dirty = false;
    }
}
