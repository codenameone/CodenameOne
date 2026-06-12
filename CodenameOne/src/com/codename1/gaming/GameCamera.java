/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.gaming;

import com.codename1.gpu.Camera;

/// The camera a `GameView` renders through. It has two modes:
///
/// - **`#MODE_ORTHO_2D`** (the default) is the classic 2D mode: an orthographic
///   camera mapping one world unit to one screen pixel, origin at the top left with
///   y pointing down. Sprites are flat, positioned in pixels, and the camera scrolls
///   via `Scene#setCamera(int, int)`. You never need to touch `GameCamera` for a 2D
///   game.
///
/// - **`#MODE_PERSPECTIVE`** turns the same scene into a 3D world: sprites become
///   camera-facing **billboards** positioned in 3D space (`Sprite#setPosition(double,
///   double, double)`), and 3D meshes can be drawn alongside them. You drive the view
///   with `#setPerspective(float, float, float)`, `#setPosition(float, float, float)`
///   and `#setTarget(float, float, float)` -- e.g. an over-the-shoulder or top-down
///   perspective camera that moves with the player.
///
/// ```java
/// // switch a GameView into 3D and place the camera
/// getCamera()
///     .setPerspective(60, 0.1f, 500f)
///     .setPosition(0, 6, 12)
///     .setTarget(0, 0, 0);
/// ```
///
/// In 3D mode world coordinates are right-handed with y up (the convention the
/// `com.codename1.gpu` package uses), and sprite sizes are world units rather than
/// pixels -- use `Sprite#setSize(float, float)` or `Sprite#setScale(float)` to pick a
/// world-space size.
public class GameCamera {
    /// Orthographic, pixel-space 2D rendering (the default).
    public static final int MODE_ORTHO_2D = 0;
    /// Perspective 3D rendering with billboarded sprites.
    public static final int MODE_PERSPECTIVE = 1;

    private int mode = MODE_ORTHO_2D;

    private float fov = 60f;
    private float near = 0.1f;
    private float far = 1000f;

    private float eyeX;
    private float eyeY;
    private float eyeZ = 10f;
    private float targetX;
    private float targetY;
    private float targetZ;
    private float upX;
    private float upY = 1f;
    private float upZ;

    // billboard basis, recomputed by #updateBasis(); columns right | up | toCamera
    private final float[] basis = new float[16];

    public int getMode() {
        return mode;
    }

    /// Switches to perspective 3D rendering and sets the lens.
    ///
    /// #### Parameters
    ///
    /// - `fovYDegrees`: vertical field of view in degrees (e.g. 60)
    ///
    /// - `near`: near clip distance (> 0)
    ///
    /// - `far`: far clip distance
    public GameCamera setPerspective(float fovYDegrees, float near, float far) {
        this.mode = MODE_PERSPECTIVE;
        this.fov = fovYDegrees;
        this.near = near;
        this.far = far;
        return this;
    }

    /// Switches back to the default orthographic 2D mode.
    public GameCamera setOrthographic2D() {
        this.mode = MODE_ORTHO_2D;
        return this;
    }

    /// The eye position in world space (perspective mode).
    public GameCamera setPosition(float x, float y, float z) {
        eyeX = x;
        eyeY = y;
        eyeZ = z;
        return this;
    }

    /// The point the camera looks at, in world space (perspective mode).
    public GameCamera setTarget(float x, float y, float z) {
        targetX = x;
        targetY = y;
        targetZ = z;
        return this;
    }

    /// The camera up vector (defaults to 0,1,0).
    public GameCamera setUp(float x, float y, float z) {
        upX = x;
        upY = y;
        upZ = z;
        return this;
    }

    public float getEyeX() {
        return eyeX;
    }

    public float getEyeY() {
        return eyeY;
    }

    public float getEyeZ() {
        return eyeZ;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public float getTargetZ() {
        return targetZ;
    }

    public float getFov() {
        return fov;
    }

    /// Configures the underlying `com.codename1.gpu.Camera` from this camera's
    /// current state. In 2D mode it sets up the pixel-space orthographic projection
    /// the `SpriteRenderer` expects; in 3D mode it sets the perspective lens and
    /// view. Called by the renderer each frame.
    void apply(Camera cam, int viewWidth, int viewHeight) {
        float aspect = (float) viewWidth / Math.max(1, viewHeight);
        if (mode == MODE_PERSPECTIVE) {
            cam.setPerspective(fov, near, far)
                    .setAspect(aspect)
                    .setPosition(eyeX, eyeY, eyeZ)
                    .setTarget(targetX, targetY, targetZ)
                    .setUp(upX, upY, upZ);
            updateBasis();
        } else {
            // pixel-space orthographic: 1 world unit == 1 pixel, y up internally;
            // the SpriteRenderer flips y when it places sprites.
            cam.setOrthographic(viewHeight, -1000f, 1000f)
                    .setAspect(aspect)
                    .setPosition(0f, 0f, 1f)
                    .setTarget(0f, 0f, 0f)
                    .setUp(0f, 1f, 0f);
        }
    }

    /// Recomputes the billboard basis (camera right/up/toward-camera axes) from the
    /// current eye, target and up vectors.
    private void updateBasis() {
        float fx = targetX - eyeX;
        float fy = targetY - eyeY;
        float fz = targetZ - eyeZ;
        float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fl < 1e-6f) {
            fz = -1f;
            fl = 1f;
        }
        fx /= fl;
        fy /= fl;
        fz /= fl;

        // right = normalize(forward x up)
        float rx = fy * upZ - fz * upY;
        float ry = fz * upX - fx * upZ;
        float rz = fx * upY - fy * upX;
        float rl = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rl < 1e-6f) {
            rx = 1f;
            ry = 0f;
            rz = 0f;
            rl = 1f;
        }
        rx /= rl;
        ry /= rl;
        rz /= rl;

        // up' = right x forward
        float ux = ry * fz - rz * fy;
        float uy = rz * fx - rx * fz;
        float uz = rx * fy - ry * fx;

        // columns: right | up' | toCamera(-forward)
        basis[0] = rx;
        basis[1] = ry;
        basis[2] = rz;
        basis[3] = 0f;
        basis[4] = ux;
        basis[5] = uy;
        basis[6] = uz;
        basis[7] = 0f;
        basis[8] = -fx;
        basis[9] = -fy;
        basis[10] = -fz;
        basis[11] = 0f;
        basis[12] = 0f;
        basis[13] = 0f;
        basis[14] = 0f;
        basis[15] = 1f;
    }

    /// The billboard orientation matrix (column-major float[16]) that makes a quad in
    /// the XY plane face the camera. Valid after `#apply` in perspective mode. The
    /// returned array is reused -- copy it if you need to keep it.
    float[] getBillboardBasis() {
        return basis;
    }
}
