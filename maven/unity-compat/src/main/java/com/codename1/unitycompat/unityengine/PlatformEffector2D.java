/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.unitycompat.unityengine;

/// `UnityEngine.PlatformEffector2D`: makes its colliders a platform that
/// is solid from one side only.
///
/// The platform has a surface: the directions within half of `surfaceArc`
/// of its up, which is the up of its object turned by `rotationalOffset`.
/// A body that touches it from one of those directions is stopped. A body
/// that first touches it from any other passes through, and goes on
/// passing through until the two have parted -- it is not caught halfway
/// when it comes level with the top.
///
/// A body standing on the platform is tested again every step, so turning
/// the platform over under it, which is how a game lets its player drop
/// through, lets it fall.
///
/// The side friction and side bounce of Unity's effector are not
/// implemented: the sides collide as the collider's own material says.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class PlatformEffector2D extends Effector2D {
    private boolean useOneWay = true;
    private boolean useOneWayGrouping;
    private float surfaceArc = 180f;
    private float rotationalOffset;
    private boolean useSideFriction;
    private boolean useSideBounce;
    private float sideArc = 1f;
    /// The cosine of half the arc, kept because it is asked every step.
    private float arcCosine;

    /// What a scene file sets.
    public void $setup(boolean masked, int mask, boolean oneWay, boolean grouping, float arc, float turn) {
        useColliderMask = masked;
        colliderMask = mask;
        useOneWay = oneWay;
        useOneWayGrouping = grouping;
        rotationalOffset = turn;
        set_surfaceArc(arc);
    }

    @Override
    public Component $new() {
        return new PlatformEffector2D();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        PlatformEffector2D e = (PlatformEffector2D) source;
        useOneWay = e.useOneWay;
        useOneWayGrouping = e.useOneWayGrouping;
        rotationalOffset = e.rotationalOffset;
        useSideFriction = e.useSideFriction;
        useSideBounce = e.useSideBounce;
        sideArc = e.sideArc;
        set_surfaceArc(e.surfaceArc);
    }

    public boolean get_useOneWay() {
        return useOneWay;
    }

    public void set_useOneWay(boolean value) {
        useOneWay = value;
    }

    public boolean get_useOneWayGrouping() {
        return useOneWayGrouping;
    }

    public void set_useOneWayGrouping(boolean value) {
        useOneWayGrouping = value;
    }

    public float get_surfaceArc() {
        return surfaceArc;
    }

    public void set_surfaceArc(float value) {
        surfaceArc = value < 0f ? 0f : value > 360f ? 360f : value;
        arcCosine = (float) Math.cos(surfaceArc * 0.5 * Transform.DEG2RAD);
    }

    public float get_rotationalOffset() {
        return rotationalOffset;
    }

    public void set_rotationalOffset(float value) {
        rotationalOffset = value;
    }

    public boolean get_useSideFriction() {
        return useSideFriction;
    }

    public void set_useSideFriction(boolean value) {
        useSideFriction = value;
    }

    public boolean get_useSideBounce() {
        return useSideBounce;
    }

    public void set_useSideBounce(boolean value) {
        useSideBounce = value;
    }

    public float get_sideArc() {
        return sideArc;
    }

    public void set_sideArc(float value) {
        sideArc = value;
    }

    /// Whether a body touching the platform is let through. `nx`, `ny` is
    /// the direction from the platform to the body where they touch, and
    /// `layer` the body's.
    boolean passes(float nx, float ny, int layer) {
        if (!useOneWay || !live()) {
            return false;
        }
        if (useColliderMask && (colliderMask & (1 << (layer & 31))) == 0) {
            return false;
        }
        Transform t = gameObject.transform;
        t.update();
        float turn = t.wrot + rotationalOffset;
        float upX = 0f;
        float upY = 1f;
        if (turn != 0f) {
            double r = turn * Transform.DEG2RAD;
            upX = (float) -Math.sin(r);
            upY = (float) Math.cos(r);
        }
        float a = nx * upX;
        float b = ny * upY;
        // A hair of slack, so that a body resting flat on a platform whose
        // arc is exactly a half turn is not let through by rounding.
        return a + b < arcCosine - 0.0001f;
    }
}
