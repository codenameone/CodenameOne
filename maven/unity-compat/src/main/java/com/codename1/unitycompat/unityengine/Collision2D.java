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

import UnityEngine.ContactPoint2D;
import UnityEngine.Vector2;

/// `UnityEngine.Collision2D`: what a script is told about a contact, from
/// the side of the one being told. `collider` and `rigidbody` are the other
/// party's, `otherCollider` and `otherRigidbody` the receiver's own -- the
/// names are Unity's.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Collision2D {
    Collider2D theirs;
    Collider2D mine;
    float relativeX;
    float relativeY;
    int points;
    final float[] pointX = new float[2];
    final float[] pointY = new float[2];
    /// Pointing at the receiver, away from the other party.
    float normalX;
    float normalY;

    /// The object of the other party's rigidbody, or of its collider when
    /// it has none.
    public GameObject get_gameObject() {
        Rigidbody2D rb = theirs.get_attachedRigidbody();
        return rb != null ? rb.gameObject : theirs.gameObject;
    }

    public Transform get_transform() {
        return get_gameObject().transform;
    }

    public Collider2D get_collider() {
        return theirs;
    }

    public Collider2D get_otherCollider() {
        return mine;
    }

    public Rigidbody2D get_rigidbody() {
        return theirs.get_attachedRigidbody();
    }

    public Rigidbody2D get_otherRigidbody() {
        return mine.get_attachedRigidbody();
    }

    public Vector2 get_relativeVelocity(Vector2 ret) {
        ret.x = relativeX;
        ret.y = relativeY;
        return ret;
    }

    public int get_contactCount() {
        return points;
    }

    public ContactPoint2D GetContact(int index, ContactPoint2D ret) {
        if (index < 0 || index >= points) {
            throw new IndexOutOfBoundsException("Collision2D contact " + index + " of " + points);
        }
        ret.m_Point.x = pointX[index];
        ret.m_Point.y = pointY[index];
        ret.m_Normal.x = normalX;
        ret.m_Normal.y = normalY;
        return ret;
    }
}
