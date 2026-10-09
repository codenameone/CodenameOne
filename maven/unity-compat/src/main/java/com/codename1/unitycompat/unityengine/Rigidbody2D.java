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
import com.codename1.gaming.physics.box2d.common.Vec2;
import com.codename1.gaming.physics.box2d.dynamics.Body;

/// `UnityEngine.Rigidbody2D` over the Box2D body of its game object.
///
/// Unity's 2D physics is Box2D, so most of this is a change of units: Unity
/// speaks degrees and Box2D radians, Unity gives a body a mass and Box2D
/// derives one from its fixtures (see [PhysicsWorld]). Values set before the
/// body exists are kept here and applied when it is created.
///
/// The pose is the transform's: `position` and `rotation` read it, and
/// writing them moves the transform and the body together.
///
/// Not implemented: interpolation, collision detection modes and sleep
/// modes are accepted from a scene and ignored. `FreezePositionX` and
/// `FreezePositionY` put the coordinate back after each step instead of
/// constraining the solver, so a frozen body can still be pushed into for
/// the length of one step.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Rigidbody2D extends Component {
    /// 0 dynamic, 1 kinematic, 2 static: `RigidbodyType2D`, and the numbers
    /// a scene file stores.
    public int $bodyType;
    float mass = 1f;
    float gravityScale = 1f;
    float drag;
    float angularDrag = 0.05f;
    /// `RigidbodyConstraints2D`: 1 freeze x, 2 freeze y, 4 freeze rotation.
    int constraints;
    /// `CollisionDetectionMode2D`: zero is Discrete, one Continuous.
    int detection;
    boolean simulated = true;
    private float pendingVx;
    private float pendingVy;
    /// Degrees a second.
    private float pendingSpin;
    boolean moving;
    float moveX;
    float moveY;
    boolean turning;
    float turnTo;
    /// What the body was doing before a `MovePosition` step, to go back to.
    float restoreVx;
    float restoreVy;
    float restoreSpin;
    private static final Vec2 scratch = new Vec2();

    /// The body, created now if the object has entered the scene and its
    /// physics has not been built yet.
    private Body body() {
        GameObject go = gameObject;
        if (go == null || !go.registered) {
            return null;
        }
        if (go.physicsDirty) {
            PhysicsWorld.flush();
        }
        return go.body;
    }

    /// The body was created or its fixtures rebuilt: give it what this
    /// component holds. Velocities are handed over once, at creation.
    void configure(Body b, boolean created) {
        b.setGravityScale(gravityScale);
        b.setLinearDamping(drag);
        b.setAngularDamping(angularDrag);
        boolean pinned = b.m_invMass == 0f;
        b.setFixedRotation((constraints & 4) != 0);
        if (!created && pinned != ((constraints & 3) == 3) && gameObject != null && gameObject.body == b) { // NOPMD CompareObjectsWithEquals
            PhysicsWorld.applyMass(gameObject, this);
        } else {
            PhysicsWorld.pin(b, constraints);
        }
        if (created) {
            scratch.set(pendingVx, pendingVy);
            b.setLinearVelocity(scratch);
            b.setAngularVelocity((float) (pendingSpin * Transform.DEG2RAD));
        }
    }

    @Override
    public Component $new() {
        return new Rigidbody2D();
    }

    @Override
    public void $copyFrom(Component source) {
        Rigidbody2D r = (Rigidbody2D) source;
        $bodyType = r.$bodyType;
        mass = r.mass;
        gravityScale = r.gravityScale;
        drag = r.drag;
        angularDrag = r.angularDrag;
        constraints = r.constraints;
        detection = r.detection;
        simulated = r.simulated;
        Vector2 v = r.get_velocity(new Vector2());
        pendingVx = v.x;
        pendingVy = v.y;
        pendingSpin = r.get_angularVelocity();
    }

    // -------------------------------------------------------------- motion

    public Vector2 get_velocity(Vector2 ret) {
        Body b = body();
        if (b == null) {
            ret.x = pendingVx;
            ret.y = pendingVy;
        } else {
            Vec2 v = b.getLinearVelocity();
            ret.x = v.x;
            ret.y = v.y;
        }
        return ret;
    }

    public void set_velocity(Vector2 value) {
        Body b = body();
        if (b == null) {
            pendingVx = value.x;
            pendingVy = value.y;
        } else {
            scratch.set(value.x, value.y);
            b.setLinearVelocity(scratch);
        }
    }

    /// Degrees a second, counter-clockwise.
    public float get_angularVelocity() {
        Body b = body();
        return b == null ? pendingSpin : (float) (b.getAngularVelocity() * Transform.RAD2DEG);
    }

    public void set_angularVelocity(float value) {
        Body b = body();
        if (b == null) {
            pendingSpin = value;
        } else {
            b.setAngularVelocity((float) (value * Transform.DEG2RAD));
        }
    }

    public Vector2 get_position(Vector2 ret) {
        Transform t = gameObject.transform;
        t.update();
        ret.x = t.wx;
        ret.y = t.wy;
        return ret;
    }

    public void set_position(Vector2 value) {
        Transform t = gameObject.transform;
        t.update();
        t.setWorldPosition(value.x, value.y, t.wz);
        if (body() != null) {
            PhysicsWorld.push(gameObject);
        }
    }

    /// Degrees, counter-clockwise; not wrapped.
    public float get_rotation() {
        Transform t = gameObject.transform;
        t.update();
        return t.wrot;
    }

    public void set_rotation(float value) {
        gameObject.transform.setWorldRotation(value);
        if (body() != null) {
            PhysicsWorld.push(gameObject);
        }
    }

    /// The body is carried to `position` by the next physics step, pushing
    /// what is in the way, and keeps the velocity it had.
    public void MovePosition(Vector2 position) {
        moving = true;
        moveX = position.x;
        moveY = position.y;
    }

    public void MoveRotation(float angle) {
        turning = true;
        turnTo = angle;
    }

    public void AddForce(Vector2 force) {
        AddForce(force, 0);
    }

    /// `mode` is `ForceMode2D`: 0 a force, 1 an impulse.
    public void AddForce(Vector2 force, int mode) {
        Body b = body();
        if (b == null) {
            return;
        }
        scratch.set(force.x, force.y);
        if (mode == 1) {
            b.applyLinearImpulse(scratch, b.getWorldCenter());
        } else {
            b.applyForceToCenter(scratch);
        }
    }

    public void AddTorque(float torque) {
        AddTorque(torque, 0);
    }

    public void AddTorque(float torque, int mode) {
        Body b = body();
        if (b == null) {
            return;
        }
        if (mode == 1) {
            b.applyAngularImpulse(torque);
        } else {
            b.applyTorque(torque);
        }
    }

    public void Sleep() {
        Body b = body();
        if (b != null) {
            b.setAwake(false);
        }
    }

    public void WakeUp() {
        Body b = body();
        if (b != null) {
            b.setAwake(true);
        }
    }

    public boolean IsSleeping() {
        Body b = body();
        return b != null && !b.isAwake();
    }

    // ---------------------------------------------------------- properties

    public float get_mass() {
        return mass;
    }

    /// The mass of the whole body, whatever the size of its colliders.
    public void set_mass(float value) {
        mass = value;
        if (gameObject != null && gameObject.body != null && !gameObject.physicsDirty) {
            PhysicsWorld.applyMass(gameObject, this);
        }
    }

    public float get_inertia() {
        Body b = body();
        return b == null ? 0f : b.getInertia();
    }

    public float get_drag() {
        return drag;
    }

    public void set_drag(float value) {
        drag = value;
        reconfigure();
    }

    public float get_angularDrag() {
        return angularDrag;
    }

    public void set_angularDrag(float value) {
        angularDrag = value;
        reconfigure();
    }

    public float get_gravityScale() {
        return gravityScale;
    }

    public void set_gravityScale(float value) {
        gravityScale = value;
        reconfigure();
    }

    private void reconfigure() {
        if (gameObject != null && gameObject.body != null) {
            configure(gameObject.body, false);
        }
    }

    public int get_bodyType() {
        return $bodyType;
    }

    public void set_bodyType(int value) {
        if ($bodyType != value) {
            $bodyType = value;
            PhysicsWorld.changed(gameObject);
        }
    }

    public boolean get_isKinematic() {
        return $bodyType == 1;
    }

    public void set_isKinematic(boolean value) {
        set_bodyType(value ? 1 : 0);
    }

    public boolean get_simulated() {
        return simulated;
    }

    public void set_simulated(boolean value) {
        if (simulated != value) {
            simulated = value;
            PhysicsWorld.changed(gameObject);
        }
    }

    public boolean get_freezeRotation() {
        return (constraints & 4) != 0;
    }

    public void set_freezeRotation(boolean value) {
        set_constraints(value ? constraints | 4 : constraints & ~4);
    }

    public int get_constraints() {
        return constraints;
    }

    public void set_constraints(int value) {
        constraints = value;
        reconfigure();
    }

    public int get_collisionDetectionMode() {
        return detection;
    }

    /// Discrete, Unity's default, finds contacts once a step, so a body
    /// that moves fast enough passes through a thin one. Continuous also
    /// finds the first time of impact within the step. Box2D decides that
    /// for a whole world, so every body is swept while any is Continuous.
    public void set_collisionDetectionMode(int value) {
        detection = value;
        PhysicsWorld.detectionChanged();
    }

    // -------------------------------------------------------------- queries

    /// The colliders on this body, or null when it has none in the world.
    private java.util.ArrayList attached() {
        return body() == null || !gameObject.body.isActive() ? null : gameObject.fixturesOf;
    }

    /// Whether a point of the world is inside any collider of the body.
    public boolean OverlapPoint(Vector2 point) {
        java.util.ArrayList colliders = attached();
        if (colliders == null) {
            return false;
        }
        PhysicsQuery.point(point.x, point.y);
        for (int i = 0; i < colliders.size(); i++) { // NOPMD ForLoopCanBeForeach
            if (PhysicsQuery.overlaps((Collider2D) colliders.get(i))) {
                return true;
            }
        }
        return false;
    }

    /// Whether the last physics step left any collider of the body in
    /// contact with another.
    public boolean IsTouching(Collider2D collider) {
        java.util.ArrayList colliders = attached();
        for (int i = 0; colliders != null && i < colliders.size(); i++) { // NOPMD ForLoopCanBeForeach
            if (PhysicsWorld.touching((Collider2D) colliders.get(i), collider)) {
                return true;
            }
        }
        return false;
    }

    public boolean IsTouchingLayers(int layerMask) {
        java.util.ArrayList colliders = attached();
        for (int i = 0; colliders != null && i < colliders.size(); i++) { // NOPMD ForLoopCanBeForeach
            if (PhysicsWorld.touchingLayers((Collider2D) colliders.get(i), layerMask)) {
                return true;
            }
        }
        return false;
    }

    /// Moves every collider of the body along a direction and fills
    /// `results`, nearest first, with what they meet; the body's own
    /// colliders are never among them.
    public int Cast(Vector2 direction, RaycastHit2D[] results, float distance) {
        java.util.ArrayList colliders = attached();
        if (colliders == null) {
            return 0;
        }
        Physics2D.castBegin(direction, distance);
        // Read again: bringing the world up to date may have rebuilt it.
        colliders = gameObject.fixturesOf;
        for (int i = 0; colliders != null && i < colliders.size(); i++) { // NOPMD ForLoopCanBeForeach
            Physics2D.castWith((Collider2D) colliders.get(i), true);
        }
        return Physics2D.castEnd(results);
    }
}
