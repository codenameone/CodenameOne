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

/// `UnityEngine.RaycastHit2D`: what a ray or a moving shape met. A struct,
/// written by hand because it holds a [Collider2D], which the translated
/// value types cannot name; it follows the protocol the translator expects
/// of one.
///
/// `point` is where the two surfaces touch, `normal` the hit collider's
/// surface normal there, `centroid` where the centre of the moving shape
/// was at that moment, `distance` how far it had travelled and `fraction`
/// that as a part of the distance asked for (zero when that is infinite).
/// A hit that found nothing has no collider, and converts to `false`.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class RaycastHit2D implements Struct {
    float centroidX;
    float centroidY;
    float pointX;
    float pointY;
    float normalX;
    float normalY;
    float distance;
    float fraction;
    Collider2D collider;

    public Vector2 get_centroid(Vector2 ret) {
        ret.x = centroidX;
        ret.y = centroidY;
        return ret;
    }

    public void set_centroid(Vector2 value) {
        centroidX = value.x;
        centroidY = value.y;
    }

    public Vector2 get_point(Vector2 ret) {
        ret.x = pointX;
        ret.y = pointY;
        return ret;
    }

    public void set_point(Vector2 value) {
        pointX = value.x;
        pointY = value.y;
    }

    public Vector2 get_normal(Vector2 ret) {
        ret.x = normalX;
        ret.y = normalY;
        return ret;
    }

    public void set_normal(Vector2 value) {
        normalX = value.x;
        normalY = value.y;
    }

    public float get_distance() {
        return distance;
    }

    public void set_distance(float value) {
        distance = value;
    }

    public float get_fraction() {
        return fraction;
    }

    public void set_fraction(float value) {
        fraction = value;
    }

    public Collider2D get_collider() {
        return collider;
    }

    public Rigidbody2D get_rigidbody() {
        return collider == null ? null : collider.get_attachedRigidbody();
    }

    /// The transform of the hit collider's rigidbody, or of the collider
    /// itself when it has none.
    public Transform get_transform() {
        if (collider == null) {
            return null;
        }
        Rigidbody2D rb = collider.get_attachedRigidbody();
        return rb == null ? collider.gameObject.transform : rb.gameObject.transform;
    }

    public static boolean op_Implicit(RaycastHit2D hit) {
        return hit.collider != null;
    }

    public RaycastHit2D $copy() {
        RaycastHit2D h = new RaycastHit2D();
        h.$assign(this);
        return h;
    }

    @Override
    public java.lang.Object $copyValue() {
        return $copy();
    }

    public void $assign(RaycastHit2D other) {
        centroidX = other.centroidX;
        centroidY = other.centroidY;
        pointX = other.pointX;
        pointY = other.pointY;
        normalX = other.normalX;
        normalY = other.normalY;
        distance = other.distance;
        fraction = other.fraction;
        collider = other.collider;
    }

    @Override
    public void $clear() {
        centroidX = 0f;
        centroidY = 0f;
        pointX = 0f;
        pointY = 0f;
        normalX = 0f;
        normalY = 0f;
        distance = 0f;
        fraction = 0f;
        collider = null;
    }

    public static void $store(RaycastHit2D[] array, int index, RaycastHit2D value) {
        array[index].$assign(value);
    }

    public static RaycastHit2D[] $newArray(int length) {
        RaycastHit2D[] array = new RaycastHit2D[length];
        for (int i = 0; i < length; i++) {
            array[i] = new RaycastHit2D();
        }
        return array;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (!(o instanceof RaycastHit2D)) {
            return false;
        }
        RaycastHit2D h = (RaycastHit2D) o;
        return h.collider == collider && same(h.centroidX, centroidX) && same(h.centroidY, centroidY) // NOPMD CompareObjectsWithEquals
                && same(h.pointX, pointX) && same(h.pointY, pointY) && same(h.normalX, normalX)
                && same(h.normalY, normalY) && same(h.distance, distance) && same(h.fraction, fraction);
    }

    /// Bit for bit, which is how .NET compares the fields of a struct.
    private static boolean same(float a, float b) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b);
    }

    @Override
    public int hashCode() {
        int h = Float.floatToIntBits(pointX);
        h = h * 31 + Float.floatToIntBits(pointY);
        h = h * 31 + Float.floatToIntBits(distance);
        return h;
    }

    @Override
    public String toString() {
        return "RaycastHit2D(" + pointX + ", " + pointY + ")";
    }
}
