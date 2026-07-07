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

/// Configuration for a `VRView`. Fluent builder.
public final class VRSettings {
    private float ipdMeters = 0.064f;
    private float fovYDegrees = 90f;
    private float near = 0.1f;
    private float far = 100f;
    private boolean stereo = true;

    /// The interpupillary distance - the world-space separation between the
    /// two eyes - in meters. Default `0.064` (the human average).
    public VRSettings ipdMeters(float ipd) {
        this.ipdMeters = ipd;
        return this;
    }

    /// The vertical field of view per eye in degrees. Default `90`.
    public VRSettings fovYDegrees(float fov) {
        this.fovYDegrees = fov;
        return this;
    }

    /// The near and far clip plane distances. Defaults `0.1` and `100`.
    public VRSettings nearFar(float near, float far) {
        this.near = near;
        this.far = far;
        return this;
    }

    /// Whether the view renders side-by-side stereo (true, the default) or a
    /// single centered viewpoint.
    public VRSettings stereo(boolean stereo) {
        this.stereo = stereo;
        return this;
    }

    /// The interpupillary distance in meters.
    public float getIpdMeters() {
        return ipdMeters;
    }

    /// The vertical field of view per eye in degrees.
    public float getFovYDegrees() {
        return fovYDegrees;
    }

    /// The near clip plane distance.
    public float getNear() {
        return near;
    }

    /// The far clip plane distance.
    public float getFar() {
        return far;
    }

    /// True when the view renders side-by-side stereo.
    public boolean isStereo() {
        return stereo;
    }
}
