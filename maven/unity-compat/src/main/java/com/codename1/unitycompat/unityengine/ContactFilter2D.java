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

import UnityEngine.Vector2;
import com.codename1.unitycompat.system.Struct;

/// `UnityEngine.ContactFilter2D`: which colliders a query of the physics
/// world reports -- by trigger, layer, depth and the angle of the surface
/// that was hit. A struct written by hand, as [LayerMask] is and for the
/// same reason.
///
/// As in Unity, one made with `new ContactFilter2D()` leaves triggers out
/// and filters nothing else; `NoFilter()` lets everything through.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class ContactFilter2D implements Struct {
    public boolean useTriggers;
    public boolean useLayerMask;
    public boolean useDepth;
    public boolean useOutsideDepth;
    public boolean useNormalAngle;
    public boolean useOutsideNormalAngle;
    /// Not final: translated code stores a fresh struct into a field.
    public LayerMask layerMask = new LayerMask();
    public float minDepth;
    public float maxDepth;
    public float minNormalAngle;
    public float maxNormalAngle;

    private static final float UPPER = 359.9999f;

    public boolean get_isFiltering() {
        return !useTriggers || useLayerMask || useDepth || useNormalAngle;
    }

    public ContactFilter2D NoFilter(ContactFilter2D ret) {
        useTriggers = true;
        useLayerMask = false;
        layerMask.m_Bits = -1;
        useDepth = false;
        useOutsideDepth = false;
        minDepth = Float.NEGATIVE_INFINITY;
        maxDepth = Float.POSITIVE_INFINITY;
        useNormalAngle = false;
        useOutsideNormalAngle = false;
        minNormalAngle = 0f;
        maxNormalAngle = UPPER;
        ret.$assign(this);
        return ret;
    }

    public void SetLayerMask(LayerMask mask) {
        layerMask.m_Bits = mask.m_Bits;
        useLayerMask = true;
    }

    public void ClearLayerMask() {
        useLayerMask = false;
    }

    public void SetDepth(float min, float max) {
        minDepth = min;
        maxDepth = max;
        useDepth = true;
    }

    public void ClearDepth() {
        minDepth = Float.NEGATIVE_INFINITY;
        maxDepth = Float.POSITIVE_INFINITY;
        useDepth = false;
    }

    public void SetNormalAngle(float min, float max) {
        minNormalAngle = min;
        maxNormalAngle = max;
        useNormalAngle = true;
    }

    public void ClearNormalAngle() {
        minNormalAngle = 0f;
        maxNormalAngle = UPPER;
        useNormalAngle = false;
    }

    /// Whether the filter leaves a collider out for being a trigger.
    public boolean IsFilteringTrigger(Collider2D collider) {
        return !useTriggers && collider.trigger;
    }

    /// Whether the filter leaves an object out for its layer.
    public boolean IsFilteringLayerMask(GameObject obj) {
        return useLayerMask && (layerMask.m_Bits & (1 << (obj.layer & 31))) == 0;
    }

    /// Whether the filter leaves an object out for the z of its transform.
    public boolean IsFilteringDepth(GameObject obj) {
        if (!useDepth) {
            return false;
        }
        Transform t = obj.transform;
        t.update();
        return depthFiltered(t.wz);
    }

    boolean depthFiltered(float z) {
        float min = minDepth < maxDepth ? minDepth : maxDepth;
        float max = minDepth < maxDepth ? maxDepth : minDepth;
        boolean outside = z < min || z > max;
        return useOutsideDepth ? !outside : outside;
    }

    /// Whether the filter leaves a hit out for the direction its surface
    /// faces.
    public boolean IsFilteringNormalAngle(Vector2 normal) {
        double degrees = com.codename1.unitycompat.system.Math.Atan2(normal.y, normal.x) * Transform.RAD2DEG;
        return IsFilteringNormalAngle((float) degrees);
    }

    /// The same for an angle in degrees, counter-clockwise from the x axis.
    public boolean IsFilteringNormalAngle(float angle) {
        if (!useNormalAngle) {
            return false;
        }
        float a = angle - 360f * (float) Math.floor(angle / 360f);
        float min = minNormalAngle < maxNormalAngle ? minNormalAngle : maxNormalAngle;
        float max = minNormalAngle < maxNormalAngle ? maxNormalAngle : minNormalAngle;
        boolean outside = a < min || a > max;
        return useOutsideNormalAngle ? !outside : outside;
    }

    public ContactFilter2D $copy() {
        ContactFilter2D f = new ContactFilter2D();
        f.$assign(this);
        return f;
    }

    @Override
    public java.lang.Object $copyValue() {
        return $copy();
    }

    public void $assign(ContactFilter2D other) {
        useTriggers = other.useTriggers;
        useLayerMask = other.useLayerMask;
        useDepth = other.useDepth;
        useOutsideDepth = other.useOutsideDepth;
        useNormalAngle = other.useNormalAngle;
        useOutsideNormalAngle = other.useOutsideNormalAngle;
        layerMask.m_Bits = other.layerMask.m_Bits;
        minDepth = other.minDepth;
        maxDepth = other.maxDepth;
        minNormalAngle = other.minNormalAngle;
        maxNormalAngle = other.maxNormalAngle;
    }

    @Override
    public void $clear() {
        useTriggers = false;
        useLayerMask = false;
        useDepth = false;
        useOutsideDepth = false;
        useNormalAngle = false;
        useOutsideNormalAngle = false;
        layerMask.m_Bits = 0;
        minDepth = 0f;
        maxDepth = 0f;
        minNormalAngle = 0f;
        maxNormalAngle = 0f;
    }

    public static void $store(ContactFilter2D[] array, int index, ContactFilter2D value) {
        array[index].$assign(value);
    }

    public static ContactFilter2D[] $newArray(int length) {
        ContactFilter2D[] array = new ContactFilter2D[length];
        for (int i = 0; i < length; i++) {
            array[i] = new ContactFilter2D();
        }
        return array;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (!(o instanceof ContactFilter2D)) {
            return false;
        }
        ContactFilter2D f = (ContactFilter2D) o;
        return f.useTriggers == useTriggers && f.useLayerMask == useLayerMask && f.useDepth == useDepth
                && f.useOutsideDepth == useOutsideDepth && f.useNormalAngle == useNormalAngle
                && f.useOutsideNormalAngle == useOutsideNormalAngle && f.layerMask.m_Bits == layerMask.m_Bits
                && same(f.minDepth, minDepth) && same(f.maxDepth, maxDepth) && same(f.minNormalAngle, minNormalAngle)
                && same(f.maxNormalAngle, maxNormalAngle);
    }

    /// Bit for bit, which is how .NET compares the fields of a struct.
    private static boolean same(float a, float b) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b);
    }

    @Override
    public int hashCode() {
        int h = layerMask.m_Bits;
        h = h * 31 + (useTriggers ? 1 : 0) + (useLayerMask ? 2 : 0) + (useDepth ? 4 : 0) + (useNormalAngle ? 8 : 0);
        h = h * 31 + Float.floatToIntBits(minDepth);
        h = h * 31 + Float.floatToIntBits(maxDepth);
        return h;
    }

    @Override
    public String toString() {
        return "ContactFilter2D(" + layerMask.m_Bits + ")";
    }
}
