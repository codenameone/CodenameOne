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

import com.codename1.gaming.physics.box2d.collision.shapes.CircleShape;
import com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape;
import com.codename1.gaming.physics.box2d.callbacks.DebugDraw;
import com.codename1.gaming.physics.box2d.common.Vec2;
import com.codename1.gaming.physics.box2d.dynamics.Body;
import com.codename1.gaming.physics.box2d.dynamics.BodyDef;
import com.codename1.gaming.physics.box2d.dynamics.World;
import com.codename1.gaming.physics.box2d.dynamics.joints.DistanceJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.Joint;
import com.codename1.gaming.physics.box2d.dynamics.joints.JointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.MouseJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.PrismaticJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.RevoluteJointDef;
import com.codename1.gaming.physics.box2d.dynamics.joints.WeldJointDef;
import com.codename1.ui.Graphics;
import java.util.ArrayList;
import java.util.List;

/// A 2D rigid body physics world, wrapping a shaded Box2D (JBox2D) simulation in an
/// idiomatic Codename One API.
///
/// The world works in **pixels** on the outside and **meters** internally (Box2D is
/// tuned for objects a few meters in size, so feeding it pixels directly produces a
/// sluggish, unstable simulation). The conversion is governed by
/// `#setPixelsPerMeter(float)` (default 30). The screen y axis points down while
/// Box2D's points up; the wrapper flips y so application code stays in screen
/// coordinates -- so a positive gravity y value pulls bodies *down* the screen.
///
/// Drive the simulation from a `com.codename1.gaming.GameView` update loop:
///
/// ```java
/// PhysicsWorld world = new PhysicsWorld(0, 600); // gravity 600 px/s^2 downward
/// PhysicsBody ground = world.createBox(0, 460, 320, 40, BodyType.STATIC);
/// PhysicsBody crate = world.createBox(160, 0, 32, 32, BodyType.DYNAMIC);
/// crate.setLinkedSprite(crateSprite);
/// // in update(dt):
/// world.step((float) dt);   // integrates and syncs linked sprites
/// ```
public class PhysicsWorld {
    private final World world;
    private float pixelsPerMeter = 30f;
    private int velocityIterations = 8;
    private int positionIterations = 3;
    private final List bodies = new ArrayList();
    private final List joints = new ArrayList();
    private ContactDispatcher contactDispatcher;
    private PhysicsDebugDraw debugDraw;
    private int debugFlags = DebugDraw.e_shapeBit | DebugDraw.e_jointBit;

    /// Creates a world with the given gravity in pixels per second squared. A
    /// positive y pulls bodies down the screen.
    public PhysicsWorld(float gravityXPx, float gravityYPx) {
        world = new World(new Vec2(0, 0));
        setGravity(gravityXPx, gravityYPx);
    }

    /// The pixels-per-meter scale used to convert between screen and simulation
    /// units. Set this once before creating bodies.
    public void setPixelsPerMeter(float ppm) {
        if (ppm > 0) {
            this.pixelsPerMeter = ppm;
        }
    }

    public float getPixelsPerMeter() {
        return pixelsPerMeter;
    }

    public void setVelocityIterations(int velocityIterations) {
        this.velocityIterations = velocityIterations;
    }

    public void setPositionIterations(int positionIterations) {
        this.positionIterations = positionIterations;
    }

    /// Sets gravity in pixels per second squared (positive y is downward).
    public void setGravity(float gxPx, float gyPx) {
        world.setGravity(new Vec2(toMeters(gxPx), -toMeters(gyPx)));
    }

    /// Advances the simulation by the given time step (seconds) and then syncs every
    /// body's transform into its linked `PhysicsLinkable` (typically a
    /// `com.codename1.gaming.Sprite`). Call once per frame from the game loop.
    public void step(float deltaSeconds) {
        world.step(deltaSeconds, velocityIterations, positionIterations);
        syncSprites();
    }

    /// Pushes each body's current transform into its linked object, converting
    /// meters to pixels and flipping the y axis. Called automatically by
    /// `#step(float)`.
    public void syncSprites() {
        for (Object body : bodies) {
            ((PhysicsBody) body).syncLinked();
        }
    }

    // ---- body creation ---------------------------------------------------

    private PhysicsBody createBody(float xPx, float yPx, BodyType type) {
        BodyDef def = new BodyDef();
        def.type = toBox2d(type);
        def.position = new Vec2(toMeters(xPx), -toMeters(yPx));
        Body body = world.createBody(def);
        PhysicsBody pb = new PhysicsBody(this, body);
        body.setUserData(pb);
        bodies.add(pb);
        return pb;
    }

    /// Creates a rectangular body centered at the given pixel position.
    public PhysicsBody createBox(float xPx, float yPx, float widthPx, float heightPx, BodyType type) {
        PhysicsBody pb = createBody(xPx, yPx, type);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(toMeters(widthPx) / 2f, toMeters(heightPx) / 2f);
        pb.addFixture(shape, type);
        return pb;
    }

    /// Creates a circular body centered at the given pixel position.
    public PhysicsBody createCircle(float xPx, float yPx, float radiusPx, BodyType type) {
        PhysicsBody pb = createBody(xPx, yPx, type);
        CircleShape shape = new CircleShape();
        shape.m_radius = toMeters(radiusPx);
        pb.addFixture(shape, type);
        return pb;
    }

    /// Creates a convex polygon body. The vertices are pixel offsets relative to the
    /// body center, as alternating x,y pairs (so `verticesPx.length` must be even).
    public PhysicsBody createPolygon(float xPx, float yPx, float[] verticesPx, BodyType type) {
        PhysicsBody pb = createBody(xPx, yPx, type);
        int n = verticesPx.length / 2;
        Vec2[] verts = new Vec2[n];
        for (int i = 0; i < n; i++) {
            verts[i] = new Vec2(toMeters(verticesPx[i * 2]), -toMeters(verticesPx[i * 2 + 1]));
        }
        PolygonShape shape = new PolygonShape();
        shape.set(verts, n);
        pb.addFixture(shape, type);
        return pb;
    }

    /// Removes a body from the world.
    public void removeBody(PhysicsBody body) {
        if (bodies.remove(body)) {
            world.destroyBody(body.getNativeBody());
        }
    }

    /// Registers a contact listener notified when bodies start and stop touching.
    public void addContactListener(ContactListener listener) {
        if (contactDispatcher == null) {
            contactDispatcher = new ContactDispatcher();
            world.setContactListener(contactDispatcher);
        }
        contactDispatcher.add(listener);
    }

    public void removeContactListener(ContactListener listener) {
        if (contactDispatcher != null) {
            contactDispatcher.remove(listener);
        }
    }

    /// Returns the underlying shaded Box2D world for advanced use. Coordinates on
    /// the native world are in meters with y pointing up.
    public World getNativeWorld() {
        return world;
    }

    // ---- joints ----------------------------------------------------------

    /// A pin/hinge joint: the two bodies rotate freely around the shared pixel anchor
    /// (e.g. a wheel on an axle, a ragdoll elbow).
    public PhysicsJoint createRevoluteJoint(PhysicsBody a, PhysicsBody b,
            float anchorXPx, float anchorYPx) {
        RevoluteJointDef def = new RevoluteJointDef();
        def.initialize(a.getNativeBody(), b.getNativeBody(), point(anchorXPx, anchorYPx));
        return addJoint(def, a, b);
    }

    /// A spring/rod joint that keeps the two pixel anchor points a fixed distance
    /// apart. `frequencyHz` 0 makes it a rigid rod; a positive value makes it a
    /// softer spring (with `dampingRatio` 0..1).
    public PhysicsJoint createDistanceJoint(PhysicsBody a, PhysicsBody b,
            float anchorAXPx, float anchorAYPx, float anchorBXPx, float anchorBYPx,
            float frequencyHz, float dampingRatio) {
        DistanceJointDef def = new DistanceJointDef();
        def.initialize(a.getNativeBody(), b.getNativeBody(),
                point(anchorAXPx, anchorAYPx), point(anchorBXPx, anchorBYPx));
        def.frequencyHz = frequencyHz;
        def.dampingRatio = dampingRatio;
        return addJoint(def, a, b);
    }

    /// A weld joint: the two bodies are locked rigidly together at the pixel anchor
    /// (as if glued).
    public PhysicsJoint createWeldJoint(PhysicsBody a, PhysicsBody b,
            float anchorXPx, float anchorYPx) {
        WeldJointDef def = new WeldJointDef();
        def.initialize(a.getNativeBody(), b.getNativeBody(), point(anchorXPx, anchorYPx));
        return addJoint(def, a, b);
    }

    /// A prismatic (slider) joint: body `b` may only translate along the given pixel
    /// axis relative to body `a` (e.g. a piston or an elevator).
    public PhysicsJoint createPrismaticJoint(PhysicsBody a, PhysicsBody b,
            float anchorXPx, float anchorYPx, float axisX, float axisY) {
        PrismaticJointDef def = new PrismaticJointDef();
        def.initialize(a.getNativeBody(), b.getNativeBody(),
                point(anchorXPx, anchorYPx), new Vec2(axisX, -axisY));
        return addJoint(def, a, b);
    }

    /// A mouse joint that pulls `target` toward a moving pixel point with a capped
    /// force -- the basis for dragging a body with a finger. `ground` is any body
    /// (typically a static one); update the point each frame with
    /// `PhysicsJoint#setTarget(float, float)`.
    public PhysicsJoint createMouseJoint(PhysicsBody ground, PhysicsBody target,
            float targetXPx, float targetYPx, float maxForce) {
        MouseJointDef def = new MouseJointDef();
        def.bodyA = ground.getNativeBody();
        def.bodyB = target.getNativeBody();
        def.target.set(point(targetXPx, targetYPx));
        def.maxForce = maxForce;
        return addJoint(def, ground, target);
    }

    /// Removes a joint from the world.
    public void destroyJoint(PhysicsJoint joint) {
        world.destroyJoint(joint.getNativeJoint());
        joints.remove(joint);
    }

    private PhysicsJoint addJoint(JointDef def, PhysicsBody a, PhysicsBody b) {
        Joint j = world.createJoint(def);
        PhysicsJoint pj = new PhysicsJoint(this, j, a, b);
        joints.add(pj);
        return pj;
    }

    private Vec2 point(float xPx, float yPx) {
        return new Vec2(toMeters(xPx), -toMeters(yPx));
    }

    /// Selects what `#debugDraw(com.codename1.ui.Graphics)` renders. By default it
    /// draws collision shapes and joints; bounding boxes are off.
    public void setDebugDrawFlags(boolean shapes, boolean joints, boolean boundingBoxes) {
        debugFlags = (shapes ? DebugDraw.e_shapeBit : 0)
                | (joints ? DebugDraw.e_jointBit : 0)
                | (boundingBoxes ? DebugDraw.e_aabbBit : 0);
        if (debugDraw != null) {
            debugDraw.setFlags(debugFlags);
        }
    }

    /// Renders the world's collision shapes, joints and (optionally) bounding boxes
    /// onto the given `com.codename1.ui.Graphics`, for debugging. Coordinates use the
    /// same pixel scale and y-flip as the bodies, so the overlay lines up with the
    /// sprites they drive. Call it from a component's `paint`, onto an off-screen
    /// `com.codename1.ui.Image`, or any other 2D drawing surface. See
    /// `#setDebugDrawFlags(boolean, boolean, boolean)` to choose what is drawn.
    public void debugDraw(Graphics g) {
        if (debugDraw == null) {
            debugDraw = new PhysicsDebugDraw(this);
            world.setDebugDraw(debugDraw);
        }
        debugDraw.setFlags(debugFlags);
        debugDraw.setGraphics(g);
        world.drawDebugData();
    }

    /// The debug renderer used by `#debugDraw(com.codename1.ui.Graphics)`, created on
    /// first use. Exposed so you can tune it (e.g. `PhysicsDebugDraw#setFillAlpha(int)`).
    public PhysicsDebugDraw getDebugDraw() {
        if (debugDraw == null) {
            debugDraw = new PhysicsDebugDraw(this);
            world.setDebugDraw(debugDraw);
        }
        return debugDraw;
    }

    // ---- unit conversion -------------------------------------------------

    float toMeters(float px) {
        return px / pixelsPerMeter;
    }

    float toPixels(float meters) {
        return meters * pixelsPerMeter;
    }

    static com.codename1.gaming.physics.box2d.dynamics.BodyType toBox2d(BodyType type) {
        switch (type) {
            case STATIC:
                return com.codename1.gaming.physics.box2d.dynamics.BodyType.STATIC;
            case KINEMATIC:
                return com.codename1.gaming.physics.box2d.dynamics.BodyType.KINEMATIC;
            default:
                return com.codename1.gaming.physics.box2d.dynamics.BodyType.DYNAMIC;
        }
    }

    /// Fans Box2D contact callbacks out to the registered CN1 listeners. Box2D calls
    /// these from inside step(), i.e. on the game loop / EDT, so no marshalling is
    /// needed.
    private static final class ContactDispatcher implements com.codename1.gaming.physics.box2d.callbacks.ContactListener {
        private final List listeners = new ArrayList();

        void add(ContactListener l) {
            listeners.add(l);
        }

        void remove(ContactListener l) {
            listeners.remove(l);
        }

        @Override
        public void beginContact(com.codename1.gaming.physics.box2d.dynamics.contacts.Contact contact) {
            PhysicsContact c = wrap(contact);
            for (Object l : listeners) {
                ((ContactListener) l).beginContact(c);
            }
        }

        @Override
        public void endContact(com.codename1.gaming.physics.box2d.dynamics.contacts.Contact contact) {
            PhysicsContact c = wrap(contact);
            for (Object l : listeners) {
                ((ContactListener) l).endContact(c);
            }
        }

        @Override
        public void preSolve(com.codename1.gaming.physics.box2d.dynamics.contacts.Contact contact, com.codename1.gaming.physics.box2d.collision.Manifold oldManifold) {
        }

        @Override
        public void postSolve(com.codename1.gaming.physics.box2d.dynamics.contacts.Contact contact, com.codename1.gaming.physics.box2d.callbacks.ContactImpulse impulse) {
        }

        private PhysicsContact wrap(com.codename1.gaming.physics.box2d.dynamics.contacts.Contact contact) {
            PhysicsBody a = (PhysicsBody) contact.getFixtureA().getBody().getUserData();
            PhysicsBody b = (PhysicsBody) contact.getFixtureB().getBody().getUserData();
            return new PhysicsContact(a, b, contact.isTouching());
        }
    }
}
