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
package com.codename1.vr;

import com.codename1.gpu.Camera;
import com.codename1.gpu.Quaternion;

/// Computes per-eye cameras for stereo rendering: a head position and
/// orientation plus a `VRSettings` interpupillary distance yield a
/// `com.codename1.gpu.Camera` per `VREye`. Pure math over the existing gpu
/// camera; usable from any thread that owns the target camera.
public final class VRCameraRig {
    private final VRSettings settings;
    private float x;
    private float y;
    private float z;
    private final float[] q = Quaternion.identity();

    // Scratch buffer, allocated once.
    private final float[] v = new float[3];

    /// Creates a rig with the supplied settings.
    ///
    /// #### Parameters
    ///
    /// - `settings`: the eye separation and lens parameters
    public VRCameraRig(VRSettings settings) {
        this.settings = settings == null ? new VRSettings() : settings;
    }

    /// The settings this rig was created with.
    public VRSettings getSettings() {
        return settings;
    }

    /// Sets the head center position in world space.
    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /// Sets the head orientation as an `{x, y, z, w}` quaternion rotating
    /// head-local vectors into world space.
    public void setOrientation(float[] quat4) {
        Quaternion.copy(quat4, q);
    }

    /// Configures `out` as the camera for the supplied eye: positioned at the
    /// head center offset by half the interpupillary distance along the head's
    /// right vector, looking along the head's forward vector.
    ///
    /// #### Parameters
    ///
    /// - `out`: the camera to configure
    ///
    /// - `eye`: which eye to compute
    ///
    /// - `aspect`: the per-eye viewport aspect ratio (width / height)
    public void apply(Camera out, VREye eye, float aspect) {
        float half = settings.getIpdMeters() * 0.5f * eye.offsetSign();
        v[0] = half;
        v[1] = 0f;
        v[2] = 0f;
        Quaternion.rotateVector(q, v);
        float ex = x + v[0];
        float ey = y + v[1];
        float ez = z + v[2];

        v[0] = 0f;
        v[1] = 0f;
        v[2] = -1f;
        Quaternion.rotateVector(q, v);
        float fx = v[0];
        float fy = v[1];
        float fz = v[2];

        v[0] = 0f;
        v[1] = 1f;
        v[2] = 0f;
        Quaternion.rotateVector(q, v);

        out.setPosition(ex, ey, ez);
        out.setTarget(ex + fx, ey + fy, ez + fz);
        out.setUp(v[0], v[1], v[2]);
        out.setPerspective(settings.getFovYDegrees(), settings.getNear(), settings.getFar());
        out.setAspect(aspect);
    }
}
