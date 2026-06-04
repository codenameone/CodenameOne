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
import com.codename1.gaming.physics.box2d.common.Vec2;
import com.codename1.gaming.physics.box2d.dynamics.Body;
import com.codename1.gaming.physics.box2d.dynamics.BodyDef;
import com.codename1.gaming.physics.box2d.dynamics.World;
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
    private ContactDispatcher contactDispatcher;

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
        for (int i = 0; i < bodies.size(); i++) {
            ((PhysicsBody) bodies.get(i)).syncLinked();
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
    private final class ContactDispatcher implements com.codename1.gaming.physics.box2d.callbacks.ContactListener {
        private final List listeners = new ArrayList();

        void add(ContactListener l) {
            listeners.add(l);
        }

        void remove(ContactListener l) {
            listeners.remove(l);
        }

        public void beginContact(com.codename1.gaming.physics.box2d.dynamics.contacts.Contact contact) {
            PhysicsContact c = wrap(contact);
            for (int i = 0; i < listeners.size(); i++) {
                ((ContactListener) listeners.get(i)).beginContact(c);
            }
        }

        public void endContact(com.codename1.gaming.physics.box2d.dynamics.contacts.Contact contact) {
            PhysicsContact c = wrap(contact);
            for (int i = 0; i < listeners.size(); i++) {
                ((ContactListener) listeners.get(i)).endContact(c);
            }
        }

        public void preSolve(com.codename1.gaming.physics.box2d.dynamics.contacts.Contact contact, com.codename1.gaming.physics.box2d.collision.Manifold oldManifold) {
        }

        public void postSolve(com.codename1.gaming.physics.box2d.dynamics.contacts.Contact contact, com.codename1.gaming.physics.box2d.callbacks.ContactImpulse impulse) {
        }

        private PhysicsContact wrap(com.codename1.gaming.physics.box2d.dynamics.contacts.Contact contact) {
            PhysicsBody a = (PhysicsBody) contact.getFixtureA().getBody().getUserData();
            PhysicsBody b = (PhysicsBody) contact.getFixtureB().getBody().getUserData();
            return new PhysicsContact(a, b, contact.isTouching());
        }
    }
}
