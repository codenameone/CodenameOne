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
package com.codename1.ar;

/// Describes which AR features the current device supports. Individual
/// features are hardware-gated on both platforms - for example face tracking
/// needs a capable front camera - so check the specific capability before
/// opening a session in that mode. Obtain through `AR#getCapabilities()`.
public final class ARCapabilities {
    /// A capabilities instance with every feature unsupported, returned on
    /// platforms without an AR backend.
    public static final ARCapabilities UNSUPPORTED =
            new ARCapabilities(false, false, false, false, false);

    private final boolean worldTracking;
    private final boolean planeDetection;
    private final boolean imageTracking;
    private final boolean faceTracking;
    private final boolean lightEstimation;

    /// Creates a capabilities descriptor. Intended for platform
    /// implementations and tests; applications obtain capabilities through
    /// `AR#getCapabilities()`.
    public ARCapabilities(boolean worldTracking, boolean planeDetection,
                          boolean imageTracking, boolean faceTracking,
                          boolean lightEstimation) {
        this.worldTracking = worldTracking;
        this.planeDetection = planeDetection;
        this.imageTracking = imageTracking;
        this.faceTracking = faceTracking;
        this.lightEstimation = lightEstimation;
    }

    /// True when world tracking (`ARTrackingMode#WORLD`) is supported.
    public boolean isWorldTrackingSupported() {
        return worldTracking;
    }

    /// True when plane detection is supported in world tracking sessions.
    public boolean isPlaneDetectionSupported() {
        return planeDetection;
    }

    /// True when reference image detection is supported.
    public boolean isImageTrackingSupported() {
        return imageTracking;
    }

    /// True when face tracking (`ARTrackingMode#FACE`) is supported on this
    /// device.
    public boolean isFaceTrackingSupported() {
        return faceTracking;
    }

    /// True when the session can estimate real-world lighting.
    public boolean isLightEstimationSupported() {
        return lightEstimation;
    }
}
