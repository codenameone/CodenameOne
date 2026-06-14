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
package com.codename1.gaming.physics;

import com.codename1.gaming.physics.box2d.collision.shapes.Shape;
import com.codename1.gaming.physics.box2d.common.Vec2;
import com.codename1.gaming.physics.box2d.dynamics.Body;
import com.codename1.gaming.physics.box2d.dynamics.Fixture;
import com.codename1.gaming.physics.box2d.dynamics.FixtureDef;

/// A rigid body in a `PhysicsWorld`, wrapping a shaded Box2D body.
///
/// Positions and velocities are expressed in **pixels** (with the screen y axis
/// pointing down); the wrapper converts to and from Box2D's meters/y-up internally.
/// A body may be linked to a `PhysicsLinkable` (typically a
/// `com.codename1.gaming.Sprite`) via `#setLinkedSprite(Object)`; after each
/// `PhysicsWorld#step(float)` the body pushes its transform into that object so the
/// sprite follows the simulation.
public class PhysicsBody {
    private final PhysicsWorld world;
    private final Body body;
    private Fixture fixture;
    private PhysicsLinkable linked;
    private Object userData;

    PhysicsBody(PhysicsWorld world, Body body) {
        this.world = world;
        this.body = body;
    }

    void addFixture(Shape shape, BodyType type) {
        FixtureDef def = new FixtureDef();
        def.shape = shape;
        def.density = type == BodyType.STATIC ? 0f : 1f;
        def.friction = 0.3f;
        def.restitution = 0.1f;
        this.fixture = body.createFixture(def);
    }

    /// Pushes the body transform into the linked object (called by the world).
    void syncLinked() {
        if (linked != null) {
            Vec2 p = body.getPosition();
            linked.setPhysicsPosition(world.toPixels(p.x), -world.toPixels(p.y));
            linked.setPhysicsRotation(-body.getAngle());
        }
    }

    /// Links this body to an object the simulation drives -- typically a
    /// `com.codename1.gaming.Sprite`. If the object implements `PhysicsLinkable` its
    /// position and rotation are updated every step.
    public void setLinkedSprite(Object sprite) {
        this.linked = sprite instanceof PhysicsLinkable ? (PhysicsLinkable) sprite : null;
    }

    public Object getLinkedSprite() {
        return linked;
    }

    /// The body center x position in pixels.
    public float getX() {
        return world.toPixels(body.getPosition().x);
    }

    /// The body center y position in pixels (screen space, y down).
    public float getY() {
        return -world.toPixels(body.getPosition().y);
    }

    /// The body rotation in radians (clockwise positive, screen space).
    public float getRotation() {
        return -body.getAngle();
    }

    /// Teleports the body to the given pixel position and rotation (radians).
    public void setTransform(float xPx, float yPx, float rotationRadians) {
        body.setTransform(new Vec2(world.toMeters(xPx), -world.toMeters(yPx)), -rotationRadians);
    }

    /// Sets the linear velocity in pixels per second (y down).
    public void setLinearVelocity(float vxPx, float vyPx) {
        body.setLinearVelocity(new Vec2(world.toMeters(vxPx), -world.toMeters(vyPx)));
    }

    public float getLinearVelocityX() {
        return world.toPixels(body.getLinearVelocity().x);
    }

    public float getLinearVelocityY() {
        return -world.toPixels(body.getLinearVelocity().y);
    }

    /// Sets the angular velocity in radians per second (clockwise positive).
    public void setAngularVelocity(float radiansPerSecond) {
        body.setAngularVelocity(-radiansPerSecond);
    }

    public float getAngularVelocity() {
        return -body.getAngularVelocity();
    }

    /// Applies a continuous force (in pixel based units, y down) at the body center.
    public void applyForce(float fxPx, float fyPx) {
        body.applyForceToCenter(new Vec2(world.toMeters(fxPx), -world.toMeters(fyPx)));
    }

    /// Applies a continuous force at a world point (pixels, y down).
    public void applyForce(float fxPx, float fyPx, float worldXPx, float worldYPx) {
        body.applyForce(new Vec2(world.toMeters(fxPx), -world.toMeters(fyPx)),
                new Vec2(world.toMeters(worldXPx), -world.toMeters(worldYPx)));
    }

    /// Applies an instantaneous impulse (pixel based units, y down) at the body
    /// center.
    public void applyLinearImpulse(float ixPx, float iyPx) {
        body.applyLinearImpulse(new Vec2(world.toMeters(ixPx), -world.toMeters(iyPx)),
                body.getWorldCenter());
    }

    /// Applies an angular impulse / torque (clockwise positive).
    public void applyTorque(float torque) {
        body.applyTorque(-torque);
    }

    public void setFixedRotation(boolean fixed) {
        body.setFixedRotation(fixed);
    }

    /// Enables continuous collision detection, preventing fast bodies from tunneling
    /// through thin walls.
    public void setBullet(boolean bullet) {
        body.setBullet(bullet);
    }

    public void setLinearDamping(float damping) {
        body.setLinearDamping(damping);
    }

    public void setAngularDamping(float damping) {
        body.setAngularDamping(damping);
    }

    public void setDensity(float density) {
        if (fixture != null) {
            fixture.setDensity(density);
            body.resetMassData();
        }
    }

    public void setFriction(float friction) {
        if (fixture != null) {
            fixture.setFriction(friction);
        }
    }

    public void setRestitution(float restitution) {
        if (fixture != null) {
            fixture.setRestitution(restitution);
        }
    }

    /// Makes the body a sensor: it detects contacts (via `ContactListener`) but does
    /// not produce a collision response.
    public void setSensor(boolean sensor) {
        if (fixture != null) {
            fixture.setSensor(sensor);
        }
    }

    public Object getUserData() {
        return userData;
    }

    public void setUserData(Object userData) {
        this.userData = userData;
    }

    /// Returns the underlying shaded Box2D body (meters, y up) for advanced use.
    public Body getNativeBody() {
        return body;
    }
}
