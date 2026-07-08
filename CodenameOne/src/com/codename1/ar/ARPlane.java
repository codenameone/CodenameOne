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

/// A real-world surface detected by the AR session, such as a floor, table or
/// wall. Planes grow and merge as the session learns more about the
/// environment; each refinement is delivered as a new immutable `ARPlane`
/// instance carrying the same `getId()`, through the session's
/// `ARPlaneListener`.
///
/// The plane's local coordinate frame is centered at `getCenterPose()` with
/// the surface spanning the local X and Z axes and the normal along local Y.
public final class ARPlane {
    /// The orientation of a detected plane.
    public enum Type {
        /// A horizontal, upward-facing surface such as a floor or table top.
        HORIZONTAL_UP,

        /// A horizontal, downward-facing surface such as a ceiling.
        HORIZONTAL_DOWN,

        /// A vertical surface such as a wall.
        VERTICAL
    }

    private final String id;
    private final Type type;
    private final ARPose centerPose;
    private final float extentX;
    private final float extentZ;
    private final float[] polygon;
    private final ARTrackingState trackingState;

    /// Creates a plane snapshot. Intended for platform implementations and
    /// tests; applications receive planes from the session.
    ///
    /// #### Parameters
    ///
    /// - `id`: the stable identifier shared by all snapshots of this plane
    ///
    /// - `type`: the surface orientation
    ///
    /// - `centerPose`: the pose of the plane center in world space; local X/Z
    ///   span the surface, local Y is the normal
    ///
    /// - `extentX`: the surface extent along local X in meters
    ///
    /// - `extentZ`: the surface extent along local Z in meters
    ///
    /// - `polygon`: an optional boundary polygon as `x, z` pairs in the
    ///   plane's local frame, or null when the platform supplies none
    ///
    /// - `trackingState`: the tracking quality of this plane
    public ARPlane(String id, Type type, ARPose centerPose, float extentX, float extentZ,
                   float[] polygon, ARTrackingState trackingState) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (centerPose == null) {
            throw new IllegalArgumentException("centerPose is required");
        }
        this.id = id;
        this.type = type;
        this.centerPose = centerPose;
        this.extentX = extentX;
        this.extentZ = extentZ;
        if (polygon == null) {
            this.polygon = null;
        } else {
            float[] copy = new float[polygon.length];
            System.arraycopy(polygon, 0, copy, 0, polygon.length);
            this.polygon = copy;
        }
        this.trackingState = trackingState == null ? ARTrackingState.TRACKING : trackingState;
    }

    /// The stable identifier shared by all snapshots of this physical plane.
    public String getId() {
        return id;
    }

    /// The surface orientation.
    public Type getType() {
        return type;
    }

    /// The pose of the plane center in world space. Local X/Z span the
    /// surface, local Y is the surface normal.
    public ARPose getCenterPose() {
        return centerPose;
    }

    /// The surface extent along the plane's local X axis in meters.
    public float getExtentX() {
        return extentX;
    }

    /// The surface extent along the plane's local Z axis in meters.
    public float getExtentZ() {
        return extentZ;
    }

    /// The boundary polygon as a newly allocated array of `x, z` pairs in the
    /// plane's local frame, or null when the platform supplies none.
    public float[] getPolygon() {
        if (polygon == null) {
            return null;
        }
        float[] copy = new float[polygon.length];
        System.arraycopy(polygon, 0, copy, 0, polygon.length);
        return copy;
    }

    /// The tracking quality of this plane.
    public ARTrackingState getTrackingState() {
        return trackingState;
    }
}
