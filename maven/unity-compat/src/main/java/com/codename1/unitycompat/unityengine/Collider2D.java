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

import UnityEngine.Bounds;
import UnityEngine.Vector2;
import java.util.ArrayList;

/// `UnityEngine.Collider2D`: a shape physics collides with, on the body of
/// the nearest [Rigidbody2D] at or above its object, or on a body of its own
/// that never moves when there is none.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Collider2D extends Behaviour {
    public float $offsetX;
    public float $offsetY;
    public float $bounciness;
    public float $friction = 0.4f;
    boolean trigger;
    boolean usedByComposite;
    boolean usedByEffector;
    private PlatformEffector2D platform;
    /// The object whose body carries this collider's fixtures, or null.
    GameObject owner;
    final ArrayList fixtures = new ArrayList();
    /// The world scale and, for a collider below its body's object, the
    /// place in the body the fixtures were built for. A change means they
    /// are stale.
    float builtScaleX;
    float builtScaleY;
    float builtX;
    float builtY;
    float builtRotation;
    private final float[] box = new float[4];

    public Rigidbody2D get_attachedRigidbody() {
        Transform t = gameObject.transform;
        while (t != null) {
            Rigidbody2D rb = (Rigidbody2D) t.gameObject.GetComponent(Rigidbody2D.class);
            if (rb != null) {
                return rb;
            }
            t = t.parent;
        }
        return null;
    }

    public boolean get_isTrigger() {
        return trigger;
    }

    public void set_isTrigger(boolean value) {
        if (trigger != value) {
            trigger = value;
            PhysicsWorld.changed(gameObject);
        }
    }

    public Vector2 get_offset(Vector2 ret) {
        ret.x = $offsetX;
        ret.y = $offsetY;
        return ret;
    }

    public void set_offset(Vector2 value) {
        $offsetX = value.x;
        $offsetY = value.y;
        PhysicsWorld.changed(gameObject);
    }

    /// The world rectangle around the collider, where physics last put its
    /// body; empty, at the object's position, for one that is not in the
    /// physics world.
    public Bounds get_bounds(Bounds ret) {
        if (gameObject.registered) {
            PhysicsWorld.flush();
        }
        Transform t = gameObject.transform;
        t.update();
        if (!PhysicsQuery.bounds(this, box)) {
            box[0] = t.wx;
            box[1] = t.wy;
            box[2] = t.wx;
            box[3] = t.wy;
        }
        ret.m_Center.x = (box[0] + box[2]) * 0.5f;
        ret.m_Center.y = (box[1] + box[3]) * 0.5f;
        ret.m_Center.z = t.wz;
        ret.m_Extents.x = (box[2] - box[0]) * 0.5f;
        ret.m_Extents.y = (box[3] - box[1]) * 0.5f;
        ret.m_Extents.z = 0f;
        return ret;
    }

    /// True while the collider has fixtures on a body that takes part in
    /// physics, so that a query can meet it.
    private boolean present() {
        if (gameObject == null || !gameObject.registered) {
            return false;
        }
        PhysicsWorld.flush();
        return owner != null && owner.body != null && owner.body.isActive() && !fixtures.isEmpty();
    }

    /// Whether a point of the world is inside the collider.
    public boolean OverlapPoint(Vector2 point) {
        if (!present()) {
            return false;
        }
        PhysicsQuery.point(point.x, point.y);
        return PhysicsQuery.overlaps(this);
    }

    /// Whether the last physics step left this collider in contact with
    /// another.
    public boolean IsTouching(Collider2D collider) {
        return PhysicsWorld.touching(this, collider);
    }

    /// The same for any collider on a layer of the mask.
    public boolean IsTouchingLayers(int layerMask) {
        return PhysicsWorld.touchingLayers(this, layerMask);
    }

    /// Moves the collider's own shape along a direction and fills
    /// `results`, nearest first, with what it meets -- never itself, and
    /// not the other colliders of its body unless asked. Triggers are
    /// reported when `Physics2D.queriesHitTriggers` says so; the layer
    /// collision matrix is not consulted.
    public int Cast(Vector2 direction, RaycastHit2D[] results, float distance, boolean ignoreSiblingColliders) {
        if (!present()) {
            return 0;
        }
        Physics2D.castBegin(direction, distance);
        Physics2D.castWith(this, ignoreSiblingColliders);
        return Physics2D.castEnd(results);
    }

    /// Fills `results` with the colliders this one overlaps and the filter
    /// lets through.
    public int OverlapCollider(ContactFilter2D contactFilter, Collider2D[] results) {
        if (!present()) {
            return 0;
        }
        PhysicsQuery.begin();
        PhysicsQuery.where(contactFilter);
        PhysicsQuery.overlap(this);
        return Physics2D.into(results);
    }

    /// Describes this collider's shape, in its own object's local space, to
    /// the physics world, which places it on a body.
    void shape(PhysicsWorld.Shapes out) {
    }

    /// Makes this collider's shapes for the [CompositeCollider2D] it is
    /// used by, which is the collider they will belong to: as closed
    /// outlines, or filled.
    void compose(PhysicsWorld.Shapes out, boolean asOutlines) {
        shape(out);
    }

    /// What a scene file sets: whether the composite collider on the body
    /// makes this collider's shapes, and whether an effector beside it
    /// changes how it collides.
    public void $usedBy(boolean composite, boolean effector) {
        usedByComposite = composite;
        usedByEffector = effector;
    }

    public boolean get_usedByEffector() {
        return usedByEffector;
    }

    public void set_usedByEffector(boolean value) {
        usedByEffector = value;
        platform = null;
    }

    /// The platform effector that decides which side of this collider is
    /// solid, or null.
    PlatformEffector2D platform() {
        if (!usedByEffector) {
            return null;
        }
        if (platform == null || platform.destroyed) {
            java.lang.Object e = gameObject.GetComponent(PlatformEffector2D.class);
            platform = e instanceof PlatformEffector2D ? (PlatformEffector2D) e : null;
        }
        return platform;
    }

    @Override
    void activeChanged(boolean nowLive) {
        PhysicsWorld.changed(gameObject);
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        Collider2D c = (Collider2D) source;
        $offsetX = c.$offsetX;
        $offsetY = c.$offsetY;
        $bounciness = c.$bounciness;
        $friction = c.$friction;
        trigger = c.trigger;
        usedByComposite = c.usedByComposite;
        usedByEffector = c.usedByEffector;
    }
}
