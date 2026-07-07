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

/// One intersection returned by `ARSession#hitTest(float, float)`: the world
/// pose where the ray from the screen point met real-world geometry. Call
/// `#createAnchor()` to place content at the hit.
public final class ARHitResult {
    /// The kind of real-world geometry the hit ray intersected.
    public enum Type {
        /// A detected `ARPlane`.
        PLANE,

        /// A surface the platform estimates exists but has not fully detected
        /// as a plane yet.
        ESTIMATED_PLANE,

        /// An individual tracked feature point.
        FEATURE_POINT
    }

    private final ARPose pose;
    private final float distance;
    private final Type type;
    private final ARPlane plane;
    private final Object nativeHandle;
    ARSession session;

    /// Creates a hit result. Intended for platform implementations and tests;
    /// applications receive hit results from `ARSession#hitTest(float, float)`.
    ///
    /// #### Parameters
    ///
    /// - `pose`: the world pose of the intersection, local Y along the
    ///   surface normal
    ///
    /// - `distance`: the distance from the camera in meters
    ///
    /// - `type`: the kind of geometry hit
    ///
    /// - `plane`: the plane that was hit, or null for non-plane hits
    ///
    /// - `nativeHandle`: an opaque platform token that lets
    ///   `#createAnchor()` anchor to the exact native hit, or null
    public ARHitResult(ARPose pose, float distance, Type type, ARPlane plane,
                       Object nativeHandle) {
        if (pose == null) {
            throw new IllegalArgumentException("pose is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        this.pose = pose;
        this.distance = distance;
        this.type = type;
        this.plane = plane;
        this.nativeHandle = nativeHandle;
    }

    /// The world pose of the intersection. Local Y points along the surface
    /// normal.
    public ARPose getPose() {
        return pose;
    }

    /// The distance from the camera to the hit in meters.
    public float getDistance() {
        return distance;
    }

    /// The kind of geometry the ray intersected.
    public Type getType() {
        return type;
    }

    /// The plane that was hit, or null for non-plane hits.
    public ARPlane getPlane() {
        return plane;
    }

    /// The opaque platform token backing this hit. Intended for platform
    /// implementations.
    public Object getNativeHandle() {
        return nativeHandle;
    }

    /// Creates an anchor at this hit, letting the platform anchor to the
    /// exact native raycast result when available.
    ///
    /// #### Returns
    ///
    /// the new anchor, registered with the session
    public ARAnchor createAnchor() {
        if (session == null) {
            throw new IllegalStateException(
                    "this hit result is not attached to a session");
        }
        return session.createAnchorFromHitInternal(this);
    }
}
