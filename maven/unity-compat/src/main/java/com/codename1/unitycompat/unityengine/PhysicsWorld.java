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

import com.codename1.gaming.physics.box2d.callbacks.ContactImpulse;
import com.codename1.gaming.physics.box2d.callbacks.ContactListener;
import com.codename1.gaming.physics.box2d.collision.Manifold;
import com.codename1.gaming.physics.box2d.collision.WorldManifold;
import com.codename1.gaming.physics.box2d.collision.shapes.ChainShape;
import com.codename1.gaming.physics.box2d.collision.shapes.CircleShape;
import com.codename1.gaming.physics.box2d.collision.shapes.MassData;
import com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape;
import com.codename1.gaming.physics.box2d.collision.shapes.Shape;
import com.codename1.gaming.physics.box2d.common.Settings;
import com.codename1.gaming.physics.box2d.common.Vec2;
import com.codename1.gaming.physics.box2d.dynamics.Body;
import com.codename1.gaming.physics.box2d.dynamics.BodyDef;
import com.codename1.gaming.physics.box2d.dynamics.BodyType;
import com.codename1.gaming.physics.box2d.dynamics.Fixture;
import com.codename1.gaming.physics.box2d.dynamics.FixtureDef;
import com.codename1.gaming.physics.box2d.dynamics.World;
import com.codename1.gaming.physics.box2d.dynamics.contacts.Contact;
import java.util.ArrayList;

/// Unity's 2D physics over Box2D: which objects have a body, what fixtures
/// are on it, how a body and a [Transform] keep each other informed, and
/// which contacts become the messages a script receives.
///
/// #### Bodies
///
/// An object with a [Rigidbody2D] has a body, and that body carries the
/// colliders of the object and of everything below it down to the next
/// rigidbody. A collider with no rigidbody above it gets a static body of
/// its own. Bodies are built lazily: a change marks the object, and the
/// marks are worked off before the next step or the next question a script
/// asks of a rigidbody.
///
/// #### Mass
///
/// Unity gives the body a mass and Box2D derives one from the density and
/// area of each fixture. Fixtures are created with a density of one, and
/// the mass Box2D arrives at is then replaced by the rigidbody's, with the
/// rotational inertia scaled by the same factor -- which is the body Box2D
/// would have computed had the density been chosen to give that mass.
///
/// #### Poses
///
/// The transform is where a script reads and writes; the body is where
/// physics does. Before a step, a transform that is not where the body last
/// left it was moved by a script, and the body is put there. After a step,
/// the transform of every body that moved is put where the body is. Scale
/// is baked into fixtures, so a collider whose world scale changed has its
/// fixtures rebuilt; a shape scaled to nothing has none.
///
/// #### Messages
///
/// Box2D reports contacts between fixtures while it steps. They are counted
/// per pair of colliders -- a concave polygon is several fixtures and must
/// still enter once -- queued, and delivered after the step, so a script is
/// free to create and destroy objects in `OnCollisionEnter2D`.
final class PhysicsWorld {
    static World world;
    static float gravityX;
    static float gravityY = -9.81f;
    static int velocityIterations = 8;
    static int positionIterations = 3;
    /// For each layer, a bit for every layer it collides with.
    static final int[] layerMask = new int[32];

    /// Objects that have a body, in the order the bodies were created.
    private static final ArrayList owners = new ArrayList();
    private static final ArrayList dirty = new ArrayList();
    private static final ArrayList pairs = new ArrayList();
    private static final ArrayList events = new ArrayList();
    /// A body came, went or changed how it detects collisions since the
    /// world was last told whether to sweep.
    private static boolean detectionDirty;
    private static java.lang.Object[] touching = new java.lang.Object[16]; // NOPMD UnnecessaryFullyQualifiedName
    private static final Vec2 scratch = new Vec2();
    private static final MassData massData = new MassData();
    private static final WorldManifold worldManifold = new WorldManifold();
    private static final Shapes shapes = new Shapes();
    /// Outlines whose convex pieces are known: the path array generated
    /// code passes, then its pieces.
    private static final ArrayList decomposed = new ArrayList();

    /// A circle below this radius, or a polygon below this area, is too
    /// small for Box2D to collide reliably and gets no fixture.
    private static final float MIN_RADIUS = 0.0001f;
    private static final float MIN_AREA = 0.000001f;

    private PhysicsWorld() {
    }

    /// Two colliders that are touching, and how many pairs of their
    /// fixtures are.
    private static final class Pair {
        Collider2D a;
        Collider2D b;
        int count;
        boolean sensor;
        /// Entered during the step being delivered, so not yet staying.
        boolean fresh;
        /// A one-way platform is letting the other body through.
        boolean passing;
        /// The pair began by passing, so no message was sent for it.
        boolean silent;
        Contact contact;
        /// What each side is told while the two stay in touch, filled in
        /// again for every frame rather than made anew: Unity reuses the
        /// object it passes too (`Physics2D.reuseCollisionCallbacks`).
        Collision2D stayA;
        Collision2D stayB;
    }

    private static final class Event {
        Pair pair;
        boolean exit;
        Collision2D forA;
        Collision2D forB;
    }

    static void reset() {
        owners.clear();
        dirty.clear();
        pairs.clear();
        events.clear();
        gravityX = 0f;
        gravityY = -9.81f;
        velocityIterations = 8;
        positionIterations = 3;
        for (int i = 0; i < 32; i++) {
            layerMask[i] = -1;
        }
        Physics2D.reset();
        PhysicsQuery.reset();
        world = new World(new Vec2(gravityX, gravityY));
        world.setContinuousPhysics(false);
        detectionDirty = true;
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                began(contact);
            }

            @Override
            public void endContact(Contact contact) {
                ended(contact);
            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {
                oneWay(contact);
            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {
            }
        });
    }

    static void setGravity(float x, float y) {
        gravityX = x;
        gravityY = y;
        if (world != null) {
            scratch.set(x, y);
            world.setGravity(scratch);
        }
    }

    // ------------------------------------------------------------ bookkeeping

    /// Something about the physics of `go` changed; rebuild it before the
    /// next step.
    static void changed(GameObject go) {
        if (go == null || !go.registered || go.physicsDirty) {
            return;
        }
        go.physicsDirty = true;
        dirty.add(go);
    }

    private static boolean hasPhysics(GameObject go) {
        int n = go.components.size();
        for (int i = 0; i < n; i++) {
            java.lang.Object c = go.components.get(i);
            if (c instanceof Collider2D || c instanceof Rigidbody2D) {
                return true;
            }
        }
        return false;
    }

    /// `go` and everything below it entered the scene, was activated or
    /// deactivated, or was moved to another parent.
    static void subtreeChanged(GameObject go) {
        if (hasPhysics(go)) {
            // A collider that was on a body above may now belong elsewhere.
            int n = go.components.size();
            for (int i = 0; i < n; i++) {
                java.lang.Object c = go.components.get(i);
                if (c instanceof Collider2D && ((Collider2D) c).owner != null) {
                    changed(((Collider2D) c).owner);
                }
            }
            changed(go);
        }
        ArrayList children = go.transform.children;
        if (children != null) {
            for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach
                subtreeChanged(((Transform) children.get(i)).gameObject);
            }
        }
    }

    static void reparented(GameObject go) {
        subtreeChanged(go);
    }

    /// `go` was destroyed. Its body goes, and a body above that carried
    /// its colliders is rebuilt without them.
    static void removed(GameObject go) {
        int n = go.components.size();
        for (int i = 0; i < n; i++) {
            java.lang.Object c = go.components.get(i);
            if (c instanceof Collider2D) {
                GameObject owner = ((Collider2D) c).owner;
                if (owner != null && owner != go) { // NOPMD CompareObjectsWithEquals
                    changed(owner);
                }
            }
        }
        destroyBody(go);
        go.physicsDirty = false;
    }

    /// The layer matrix changed: every fixture's filter is stale.
    static void refilter() {
        for (int i = 0; i < owners.size(); i++) { // NOPMD ForLoopCanBeForeach
            changed((GameObject) owners.get(i));
        }
    }

    static int ownerCount() {
        return owners.size();
    }

    /// An object that has a body; they are numbered in the order the bodies
    /// were made.
    static GameObject owner(int index) {
        return (GameObject) owners.get(index);
    }

    /// Whether two colliders are in contact, as the last step found them.
    static boolean touching(Collider2D a, Collider2D b) {
        if (a == null || b == null) {
            return false;
        }
        Pair p = pair(a, b);
        return p != null && p.count > 0;
    }

    /// Whether a collider is in contact with one on any layer of a mask.
    static boolean touchingLayers(Collider2D a, int mask) {
        if (a == null) {
            return false;
        }
        for (int i = 0; i < pairs.size(); i++) { // NOPMD ForLoopCanBeForeach
            Pair p = (Pair) pairs.get(i);
            if (p.count <= 0) {
                continue;
            }
            Collider2D other = p.a == a ? p.b : p.b == a ? p.a : null; // NOPMD CompareObjectsWithEquals
            if (other != null && other.gameObject != null && (mask & (1 << (other.gameObject.layer & 31))) != 0) {
                return true;
            }
        }
        return false;
    }

    static void flush() {
        while (!dirty.isEmpty()) {
            GameObject go = (GameObject) dirty.remove(dirty.size() - 1);
            if (go.physicsDirty) {
                rebuild(go);
            }
        }
    }

    /// The collider a point of the world is inside, or null. Of several,
    /// the one whose object is nearest a camera looking along z: the least
    /// z, and among equals the first created.
    static Collider2D at(float x, float y) {
        flush();
        scratch.x = x;
        scratch.y = y;
        Collider2D best = null;
        float bestZ = 0f;
        for (int i = 0; i < owners.size(); i++) { // NOPMD ForLoopCanBeForeach
            GameObject go = (GameObject) owners.get(i);
            ArrayList colliders = go.fixturesOf;
            if (colliders == null || go.body == null || !go.body.isActive()) {
                continue;
            }
            for (int j = 0; j < colliders.size(); j++) { // NOPMD ForLoopCanBeForeach
                Collider2D c = (Collider2D) colliders.get(j);
                if (!covers(c)) {
                    continue;
                }
                Transform t = c.gameObject.transform;
                t.update();
                if (best == null || t.wz < bestZ) {
                    best = c;
                    bestZ = t.wz;
                }
            }
        }
        return best;
    }

    /// Whether the point in `scratch` is inside one of a collider's shapes.
    private static boolean covers(Collider2D c) {
        for (int k = 0; k < c.fixtures.size(); k++) {
            if (((Fixture) c.fixtures.get(k)).testPoint(scratch)) {
                return true;
            }
        }
        return false;
    }

    private static GameObject bodyAbove(Transform t) {
        while (t != null) {
            if (t.gameObject.GetComponent(Rigidbody2D.class) != null) {
                return t.gameObject;
            }
            t = t.parent;
        }
        return null;
    }

    private static void collect(GameObject go, boolean below, ArrayList out) {
        int n = go.components.size();
        for (int i = 0; i < n; i++) {
            java.lang.Object c = go.components.get(i);
            if (c instanceof Collider2D && ((Collider2D) c).live()) {
                out.add(c);
            }
        }
        ArrayList children = go.transform.children;
        if (below && children != null) {
            for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach
                GameObject child = ((Transform) children.get(i)).gameObject;
                if (child.GetComponent(Rigidbody2D.class) == null) {
                    collect(child, true, out);
                }
            }
        }
    }

    /// A [Rigidbody2D] was set to detect collisions another way.
    static void detectionChanged() {
        detectionDirty = true;
    }

    /// Tells the world to find times of impact when a body in it asks for
    /// that, and not otherwise: Unity sweeps only a body set to Continuous,
    /// and a level of bodies resting on a tilemap spent most of its step
    /// sweeping contacts that could not be missed.
    private static void sweepIfAnyAsks() {
        detectionDirty = false;
        boolean any = false;
        for (int i = 0; i < owners.size() && !any; i++) { // NOPMD ForLoopCanBeForeach
            Rigidbody2D rb = (Rigidbody2D) ((GameObject) owners.get(i)).GetComponent(Rigidbody2D.class);
            any = rb != null && rb.detection == 1;
        }
        world.setContinuousPhysics(any);
    }

    private static void destroyBody(GameObject go) {
        if (go.body == null) {
            return;
        }
        releaseFixtures(go, false);
        world.destroyBody(go.body);
        detectionDirty = true;
        go.body = null;
        go.fixturesOf = null;
        owners.remove(go);
    }

    private static void releaseFixtures(GameObject go, boolean destroy) {
        ArrayList colliders = go.fixturesOf;
        if (colliders == null) {
            return;
        }
        for (int i = 0; i < colliders.size(); i++) { // NOPMD ForLoopCanBeForeach
            Collider2D c = (Collider2D) colliders.get(i);
            if (destroy) {
                for (int j = 0; j < c.fixtures.size(); j++) {
                    go.body.destroyFixture((Fixture) c.fixtures.get(j));
                }
            }
            c.fixtures.clear();
            c.owner = null;
        }
    }

    private static void rebuild(GameObject go) {
        go.physicsDirty = false;
        if (go.destroyed || !go.registered) {
            destroyBody(go);
            return;
        }
        Rigidbody2D rb = (Rigidbody2D) go.GetComponent(Rigidbody2D.class);
        if (rb == null) {
            GameObject above = bodyAbove(go.transform.parent);
            if (above != null) {
                // Its colliders ride on a rigidbody further up.
                destroyBody(go);
                rebuild(above);
                return;
            }
        }
        ArrayList colliders = new ArrayList();
        collect(go, rb != null, colliders);
        boolean active = go.activeInHierarchy();
        if (rb == null && (colliders.isEmpty() || !active)) {
            destroyBody(go);
            return;
        }
        Transform t = go.transform;
        t.update();
        BodyType type = rb == null || rb.$bodyType == 2 ? BodyType.STATIC
                : rb.$bodyType == 1 ? BodyType.KINEMATIC : BodyType.DYNAMIC;
        boolean created = go.body == null;
        if (created) {
            BodyDef def = new BodyDef();
            def.type = type;
            def.position = new Vec2(t.wx, t.wy);
            def.angle = (float) (t.wrot * Transform.DEG2RAD);
            Body body = world.createBody(def);
            detectionDirty = true;
            body.setUserData(go);
            go.body = body;
            go.syncX = t.wx;
            go.syncY = t.wy;
            go.syncRotation = t.wrot;
            go.bodyAngle = body.getAngle();
            owners.add(go);
        } else {
            releaseFixtures(go, true);
            if (go.body.getType() != type) {
                go.body.setType(type);
            }
        }
        Body body = go.body;
        if (rb != null) {
            rb.configure(body, created);
        }
        go.fixturesOf = colliders;
        // With a composite collider among them, the colliders it uses make
        // no shapes of their own: it makes theirs.
        boolean composed = false;
        for (int i = 0; i < colliders.size() && !composed; i++) {
            composed = colliders.get(i) instanceof CompositeCollider2D;
        }
        shapes.body = body;
        shapes.bodyAt = t;
        shapes.density = type == BodyType.DYNAMIC ? 1f : 0f;
        for (int i = 0; i < colliders.size(); i++) { // NOPMD ForLoopCanBeForeach
            Collider2D c = (Collider2D) colliders.get(i);
            // One that was on another body is taken from it.
            if (c.owner != null && c.owner != go) { // NOPMD CompareObjectsWithEquals
                changed(c.owner);
                for (int j = 0; j < c.fixtures.size(); j++) {
                    c.owner.body.destroyFixture((Fixture) c.fixtures.get(j));
                }
                c.owner.fixturesOf.remove(c);
                c.fixtures.clear();
            }
            c.owner = go;
            Transform ct = c.gameObject.transform;
            ct.update();
            shapes.collider = c;
            shapes.at = ct;
            c.builtScaleX = ct.wsx;
            c.builtScaleY = ct.wsy;
            if (ct != t) { // NOPMD CompareObjectsWithEquals
                shapes.place(0f, 0f);
                c.builtX = shapes.outX;
                c.builtY = shapes.outY;
                c.builtRotation = ct.wrot - t.wrot;
            }
            if (!c.usedByComposite || !composed) {
                c.shape(shapes);
            }
        }
        if (rb != null) {
            applyMass(go, rb);
        }
        boolean on = active && (rb == null || rb.simulated);
        if (body.isActive() != on) {
            body.setActive(on);
        }
    }

    /// Replaces the mass Box2D derived from the fixtures with the
    /// rigidbody's own.
    static void applyMass(GameObject go, Rigidbody2D rb) {
        Body body = go.body;
        if (body.getType() != BodyType.DYNAMIC) {
            return;
        }
        body.resetMassData();
        if (rb.mass > 0f) {
            body.getMassData(massData);
            if (massData.mass > 0f) {
                float k = rb.mass / massData.mass;
                massData.I = massData.I * k;
            }
            massData.mass = rb.mass;
            body.setMassData(massData);
        }
        pin(body, rb.constraints);
    }

    /// A body whose position is frozen on both axes cannot be pushed: what
    /// runs into it is stopped as a wall stops it. Putting it back after
    /// the step, which is what holds a single frozen axis, would let it
    /// give way during the step and take half of every correction -- a
    /// floor a player sinks into. Called after anything that recomputes
    /// the body's mass.
    static void pin(Body body, int constraints) {
        if (body.getType() == BodyType.DYNAMIC && (constraints & 3) == 3) {
            body.m_invMass = 0f;
        }
    }

    /// Puts the body of `go` where its transform is.
    static void push(GameObject go) {
        Body body = go.body;
        if (body == null) {
            return;
        }
        Transform t = go.transform;
        t.update();
        float angle = t.wrot == go.syncRotation ? go.bodyAngle : (float) (t.wrot * Transform.DEG2RAD);
        scratch.set(t.wx, t.wy);
        body.setTransform(scratch, angle);
        body.setAwake(true);
        go.syncX = t.wx;
        go.syncY = t.wy;
        go.syncRotation = t.wrot;
        go.bodyAngle = angle;
    }

    // ------------------------------------------------------------------ step

    /// Brings the body of `go` up to date with its transform: fixtures
    /// rebuilt where a scale or a collider below the body changed, and the
    /// body moved to where a script put the transform. False when the
    /// object no longer has a body afterwards.
    private static boolean sync(GameObject go) {
        Transform t = go.transform;
        t.update();
        ArrayList colliders = go.fixturesOf;
        boolean stale = false;
        for (int j = 0; j < colliders.size() && !stale; j++) {
            Collider2D c = (Collider2D) colliders.get(j);
            Transform ct = c.gameObject.transform;
            ct.update();
            if (ct.wsx != c.builtScaleX || ct.wsy != c.builtScaleY) {
                stale = true;
            } else if (ct != t) { // NOPMD CompareObjectsWithEquals
                shapes.bodyAt = t;
                shapes.at = ct;
                shapes.place(0f, 0f);
                float dx = shapes.outX - c.builtX;
                float dy = shapes.outY - c.builtY;
                float dr = (ct.wrot - t.wrot) - c.builtRotation;
                stale = dx > 0.00001f || dx < -0.00001f || dy > 0.00001f || dy < -0.00001f || dr > 0.001f
                        || dr < -0.001f;
            }
        }
        if (stale) {
            rebuild(go);
            if (go.body == null) {
                return false;
            }
        }
        if (t.wx != go.syncX || t.wy != go.syncY || t.wrot != go.syncRotation) {
            push(go);
        }
        return true;
    }

    /// What `Physics2D.SyncTransforms()` does: every body is put where its
    /// transform is, without stepping.
    static void syncTransforms() {
        flush();
        // Backwards, because a rebuild may take an object off the list.
        for (int i = owners.size() - 1; i >= 0; i--) {
            sync((GameObject) owners.get(i));
        }
        flush();
    }

    static void step(float dt) {
        flush();
        // Backwards, because a rebuild may take an object off the list.
        for (int i = owners.size() - 1; i >= 0; i--) {
            GameObject go = (GameObject) owners.get(i);
            if (!sync(go)) {
                continue;
            }
            Rigidbody2D rb = (Rigidbody2D) go.GetComponent(Rigidbody2D.class);
            if (rb != null && (rb.moving || rb.turning) && dt > 0f) {
                // Carried there by a velocity that lasts one step, so that
                // what is in the way is pushed, not passed through.
                Body body = go.body;
                Vec2 v = body.getLinearVelocity();
                rb.moveX = rb.moving ? rb.moveX : go.syncX;
                rb.moveY = rb.moving ? rb.moveY : go.syncY;
                rb.turnTo = rb.turning ? rb.turnTo : go.syncRotation;
                savedVx = v.x;
                savedVy = v.y;
                savedSpin = body.getAngularVelocity();
                carry(go, rb, dt);
            }
        }
        flush();
        if (detectionDirty) {
            sweepIfAnyAsks();
        }
        world.step(dt, velocityIterations, positionIterations);
        for (int i = 0; i < owners.size(); i++) { // NOPMD ForLoopCanBeForeach
            GameObject go = (GameObject) owners.get(i);
            Body body = go.body;
            if (body.getType() == BodyType.STATIC || !body.isActive()) {
                continue;
            }
            Rigidbody2D rb = (Rigidbody2D) go.GetComponent(Rigidbody2D.class);
            if (rb != null && (rb.moving || rb.turning)) {
                arrive(go, rb);
            } else if (rb != null && (rb.constraints & 3) != 0) {
                freeze(go, rb);
            }
            Vec2 p = body.getPosition();
            float angle = body.getAngle();
            if (p.x == go.syncX && p.y == go.syncY && angle == go.bodyAngle) {
                continue;
            }
            Transform t = go.transform;
            t.update();
            float degrees = angle == go.bodyAngle ? go.syncRotation : (float) (angle * Transform.RAD2DEG);
            t.setWorldPosition(p.x, p.y, t.wz);
            t.setWorldRotation(degrees);
            t.update();
            // Read back: under a parent the transform holds the pose as
            // local values, and what it reports may differ from what it
            // was given in the last bit.
            go.syncX = t.wx;
            go.syncY = t.wy;
            go.syncRotation = t.wrot;
            go.bodyAngle = angle;
        }
    }

    // MovePosition and MoveRotation move one body per call, so one set of
    // saved values would do only if the two loops of a step handled a body
    // start to finish; they do not, so the velocity to restore is kept on
    // the rigidbody's own pending fields instead. These three carry it from
    // step() into carry().
    private static float savedVx;
    private static float savedVy;
    private static float savedSpin;

    private static void carry(GameObject go, Rigidbody2D rb, float dt) {
        Body body = go.body;
        rb.restoreVx = savedVx;
        rb.restoreVy = savedVy;
        rb.restoreSpin = savedSpin;
        scratch.set((rb.moveX - go.syncX) / dt, (rb.moveY - go.syncY) / dt);
        body.setLinearVelocity(scratch);
        body.setAngularVelocity((float) ((rb.turnTo - go.syncRotation) * Transform.DEG2RAD) / dt);
    }

    private static void arrive(GameObject go, Rigidbody2D rb) {
        Body body = go.body;
        scratch.set(rb.restoreVx, rb.restoreVy);
        body.setLinearVelocity(scratch);
        body.setAngularVelocity(rb.restoreSpin);
        rb.moving = false;
        rb.turning = false;
    }

    private static void freeze(GameObject go, Rigidbody2D rb) {
        Body body = go.body;
        Vec2 p = body.getPosition();
        Vec2 v = body.getLinearVelocity();
        float px = (rb.constraints & 1) != 0 ? go.syncX : p.x;
        float py = (rb.constraints & 2) != 0 ? go.syncY : p.y;
        float vx = (rb.constraints & 1) != 0 ? 0f : v.x;
        float vy = (rb.constraints & 2) != 0 ? 0f : v.y;
        if (px != p.x || py != p.y) {
            scratch.set(px, py);
            body.setTransform(scratch, body.getAngle());
        }
        scratch.set(vx, vy);
        body.setLinearVelocity(scratch);
    }

    // --------------------------------------------------------------- shapes

    /// Receives a collider's shape in the collider's own space and puts it
    /// on a body: scaled by the collider's world scale and, for a collider
    /// on an object below the body's, moved to where that object is in the
    /// body.
    static final class Shapes {
        Body body;
        Transform bodyAt;
        Transform at;
        Collider2D collider;
        float density;
        float outX;
        float outY;
        /// Where a composite collider keeps the outlines made for it.
        ArrayList paths;

        /// A point of the collider's space, in the body's.
        void place(float px, float py) {
            float sx = px * at.wsx;
            float sy = py * at.wsy;
            if (at == bodyAt) { // NOPMD CompareObjectsWithEquals
                outX = sx;
                outY = sy;
                return;
            }
            float a = sx * at.wcos;
            float b = sy * at.wsin;
            float c = sx * at.wsin;
            float d = sy * at.wcos;
            float dx = at.wx + (a - b) - bodyAt.wx;
            float dy = at.wy + (c + d) - bodyAt.wy;
            float e = dx * bodyAt.wcos;
            float f = dy * bodyAt.wsin;
            float g = dy * bodyAt.wcos;
            float h = dx * bodyAt.wsin;
            outX = e + f;
            outY = g - h;
        }

        void circle(float cx, float cy, float radius) {
            float sx = at.wsx < 0f ? -at.wsx : at.wsx;
            float sy = at.wsy < 0f ? -at.wsy : at.wsy;
            float r = radius * (sx > sy ? sx : sy);
            if (!(r >= MIN_RADIUS)) { // NOPMD LogicInversion
                return;
            }
            place(cx, cy);
            CircleShape s = new CircleShape();
            s.m_radius = r;
            s.m_p.set(outX, outY);
            add(s);
        }

        /// A convex polygon of three to eight corners, as x and y pairs,
        /// moved by an offset.
        void convex(float[] points, float offsetX, float offsetY) {
            int n = points.length / 2;
            if (n < 3 || n > 8) {
                return;
            }
            Vec2[] v = new Vec2[n];
            for (int i = 0; i < n; i++) {
                place(points[i * 2] + offsetX, points[i * 2 + 1] + offsetY);
                v[i] = new Vec2(outX, outY);
            }
            float twice = 0f;
            for (int i = 0; i < n; i++) {
                Vec2 p = v[i];
                Vec2 q = v[i + 1 == n ? 0 : i + 1];
                float l = p.x * q.y;
                float m = q.x * p.y;
                twice += l - m;
            }
            if (twice < 0f) {
                twice = -twice;
            }
            if (!(twice * 0.5f >= MIN_AREA)) { // NOPMD LogicInversion
                return;
            }
            PolygonShape s = new PolygonShape();
            s.set(v, n);
            add(s);
        }

        /// A capsule of a size about a centre, upright for `direction` 0
        /// and lying down for 1: a box with a circle at each end. The size
        /// is scaled a side at a time, as a box's is, and the ends are as
        /// wide as the scaled short side; a capsule no longer than it is
        /// wide is one circle.
        void capsule(float cx, float cy, float width, float height, int direction) {
            float sx = at.wsx < 0f ? -at.wsx : at.wsx;
            float sy = at.wsy < 0f ? -at.wsy : at.wsy;
            float w = (width < 0f ? -width : width) * sx;
            float h = (height < 0f ? -height : height) * sy;
            float r = (direction == 1 ? h : w) * 0.5f;
            float half = (direction == 1 ? w : h) * 0.5f - r;
            if (!(r >= MIN_RADIUS)) { // NOPMD LogicInversion
                return;
            }
            place(cx, cy);
            float mx = outX;
            float my = outY;
            // The long axis in the body: where a step along it lands.
            if (direction == 1) {
                place(cx + (at.wsx == 0f ? 0f : 1f / sx), cy);
            } else {
                place(cx, cy + (at.wsy == 0f ? 0f : 1f / sy));
            }
            float ax = outX - mx;
            float ay = outY - my;
            float al = (float) Math.sqrt(ax * ax + ay * ay);
            if (!(half > 0f) || !(al > 0f)) { // NOPMD LogicInversion
                CircleShape s = new CircleShape();
                s.m_radius = r;
                s.m_p.set(mx, my);
                add(s);
                return;
            }
            ax = ax / al * half;
            ay = ay / al * half;
            float bx = -ay / half * r;
            float by = ax / half * r;
            if (half * r * 4f >= MIN_AREA) {
                Vec2[] v = new Vec2[4];
                v[0] = new Vec2(mx - ax - bx, my - ay - by);
                v[1] = new Vec2(mx + ax - bx, my + ay - by);
                v[2] = new Vec2(mx + ax + bx, my + ay + by);
                v[3] = new Vec2(mx - ax + bx, my - ay + by);
                PolygonShape box = new PolygonShape();
                box.set(v, 4);
                add(box);
            }
            CircleShape one = new CircleShape();
            one.m_radius = r;
            one.m_p.set(mx - ax, my - ay);
            add(one);
            CircleShape other = new CircleShape();
            other.m_radius = r;
            other.m_p.set(mx + ax, my + ay);
            add(other);
        }

        /// An open line through points, as x and y pairs, moved by an
        /// offset: a surface with no inside. A point closer to the one
        /// before it than Box2D can tell apart is left out, and a line of
        /// fewer than two points is nothing.
        void chain(float[] points, float offsetX, float offsetY) {
            int n = points.length / 2;
            Vec2[] v = new Vec2[n];
            int kept = 0;
            float least = Settings.linearSlop * Settings.linearSlop;
            for (int i = 0; i < n; i++) {
                place(points[i * 2] + offsetX, points[i * 2 + 1] + offsetY);
                if (kept > 0) {
                    float dx = outX - v[kept - 1].x;
                    float dy = outY - v[kept - 1].y;
                    float l = dx * dx;
                    float m = dy * dy;
                    // Twice Box2D's own limit, so that rounding cannot put
                    // a point it would refuse back in.
                    if (l + m < least * 4f) {
                        continue;
                    }
                }
                v[kept++] = new Vec2(outX, outY);
            }
            if (kept < 2) {
                return;
            }
            ChainShape s = new ChainShape();
            s.createChain(v, kept);
            add(s);
        }

        /// A closed chain of edges through `count` points, x then y.
        void loop(float[] points, int count, float offsetX, float offsetY) {
            Vec2[] v = new Vec2[count];
            int kept = 0;
            float least = Settings.linearSlop * Settings.linearSlop;
            for (int i = 0; i < count; i++) {
                place(points[i * 2] + offsetX, points[i * 2 + 1] + offsetY);
                if (kept > 0) {
                    float dx = outX - v[kept - 1].x;
                    float dy = outY - v[kept - 1].y;
                    float l = dx * dx;
                    float m = dy * dy;
                    if (l + m < least * 4f) {
                        continue;
                    }
                }
                v[kept++] = new Vec2(outX, outY);
            }
            if (kept > 1) {
                float dx = v[0].x - v[kept - 1].x;
                float dy = v[0].y - v[kept - 1].y;
                float l = dx * dx;
                float m = dy * dy;
                if (l + m < least * 4f) {
                    kept--;
                }
            }
            if (kept < 3) {
                return;
            }
            if (paths != null) {
                float[] path = new float[kept * 2];
                for (int i = 0; i < kept; i++) {
                    path[i * 2] = v[i].x;
                    path[i * 2 + 1] = v[i].y;
                }
                paths.add(path);
            }
            ChainShape s = new ChainShape();
            s.createLoop(v, kept);
            add(s);
        }

        private void add(Shape s) {
            FixtureDef fd = new FixtureDef();
            fd.shape = s;
            fd.density = density;
            fd.friction = collider.$friction;
            fd.restitution = collider.$bounciness;
            fd.isSensor = collider.trigger;
            int layer = collider.gameObject.layer & 31;
            fd.filter.categoryBits = 1 << layer;
            fd.filter.maskBits = layerMask[layer];
            fd.userData = collider;
            collider.fixtures.add(body.createFixture(fd));
        }
    }

    // ------------------------------------------------------------- contacts

    private static Pair pair(Collider2D a, Collider2D b) {
        for (int i = 0; i < pairs.size(); i++) { // NOPMD ForLoopCanBeForeach
            Pair p = (Pair) pairs.get(i);
            if ((p.a == a && p.b == b) || (p.a == b && p.b == a)) { // NOPMD CompareObjectsWithEquals
                return p;
            }
        }
        return null;
    }

    /// Whether a platform effector on either side lets the other through
    /// where the two touch now.
    private static boolean passes(Contact contact, Collider2D a, Collider2D b) {
        PlatformEffector2D onA = a.platform();
        PlatformEffector2D onB = b.platform();
        if (onA == null && onB == null) {
            return false;
        }
        if (contact.getManifold().pointCount == 0) {
            return false;
        }
        contact.getWorldManifold(worldManifold);
        float nx = worldManifold.normal.x;
        float ny = worldManifold.normal.y;
        return (onA != null && onA.passes(nx, ny, b.gameObject.layer))
                || (onB != null && onB.passes(-nx, -ny, a.gameObject.layer));
    }

    /// Turns off, for this step, a contact with a one-way platform that
    /// the other body is passing through. A pair that starts passing goes
    /// on passing until it has parted.
    private static void oneWay(Contact contact) {
        java.lang.Object ua = contact.getFixtureA().getUserData();
        java.lang.Object ub = contact.getFixtureB().getUserData();
        if (!(ua instanceof Collider2D) || !(ub instanceof Collider2D)) {
            return;
        }
        Collider2D a = (Collider2D) ua;
        Collider2D b = (Collider2D) ub;
        if (!a.usedByEffector && !b.usedByEffector) {
            return;
        }
        Pair p = pair(a, b);
        if (p == null) {
            return;
        }
        if (!p.passing && passes(contact, a, b)) {
            p.passing = true;
        }
        if (p.passing) {
            contact.setEnabled(false);
        }
    }

    private static void began(Contact contact) {
        java.lang.Object ua = contact.getFixtureA().getUserData();
        java.lang.Object ub = contact.getFixtureB().getUserData();
        if (!(ua instanceof Collider2D) || !(ub instanceof Collider2D)) {
            return;
        }
        Collider2D a = (Collider2D) ua;
        Collider2D b = (Collider2D) ub;
        Pair p = pair(a, b);
        if (p == null) {
            p = new Pair();
            p.a = a;
            p.b = b;
            pairs.add(p);
        }
        p.count++;
        p.contact = contact;
        if (p.count > 1) {
            return;
        }
        p.sensor = contact.getFixtureA().isSensor() || contact.getFixtureB().isSensor();
        p.fresh = true;
        // A body that meets a one-way platform from the side it lets
        // through has not collided with it: no message says it did.
        p.passing = !p.sensor && (a.usedByEffector || b.usedByEffector) && passes(contact, a, b);
        p.silent = p.passing;
        if (p.silent) {
            return;
        }
        Event e = new Event();
        e.pair = p;
        if (!p.sensor) {
            // Now, while the bodies still have the velocities they met at.
            boolean flipped = p.a != a; // NOPMD CompareObjectsWithEquals
            e.forA = describe(p.a, p.b, contact, flipped);
            e.forB = describe(p.b, p.a, contact, !flipped);
        }
        events.add(e);
    }

    private static void ended(Contact contact) {
        java.lang.Object ua = contact.getFixtureA().getUserData();
        java.lang.Object ub = contact.getFixtureB().getUserData();
        if (!(ua instanceof Collider2D) || !(ub instanceof Collider2D)) {
            return;
        }
        Pair p = pair((Collider2D) ua, (Collider2D) ub);
        if (p == null) {
            return;
        }
        if (p.contact == contact) { // NOPMD CompareObjectsWithEquals
            p.contact = null;
        }
        p.count--;
        if (p.count > 0) {
            return;
        }
        pairs.remove(p);
        if (p.silent) {
            return;
        }
        Event e = new Event();
        e.pair = p;
        e.exit = true;
        events.add(e);
    }

    /// The contact as `mine` is told of it. `mineIsB` says which of the
    /// contact's two fixtures is the receiver's: Box2D's normal points from
    /// its first fixture to its second, and Unity's at the receiver.
    private static Collision2D describe(Collider2D mine, Collider2D theirs, Contact contact, boolean mineIsB) {
        return describe(new Collision2D(), mine, theirs, contact, mineIsB);
    }

    /// As above, into an object that may have described an earlier frame.
    private static Collision2D describe(Collision2D c, Collider2D mine, Collider2D theirs, Contact contact,
            boolean mineIsB) {
        c.mine = mine;
        c.theirs = theirs;
        c.relativeX = 0f;
        c.relativeY = 0f;
        c.points = 0;
        c.normalX = 0f;
        c.normalY = 0f;
        Body mb = mine.owner == null ? null : mine.owner.body;
        Body tb = theirs.owner == null ? null : theirs.owner.body;
        if (mb != null && tb != null) {
            Vec2 mv = mb.getLinearVelocity();
            Vec2 tv = tb.getLinearVelocity();
            c.relativeX = tv.x - mv.x;
            c.relativeY = tv.y - mv.y;
        }
        if (contact != null) {
            int n = contact.getManifold().pointCount;
            contact.getWorldManifold(worldManifold);
            c.points = n > 2 ? 2 : n;
            for (int i = 0; i < c.points; i++) {
                c.pointX[i] = worldManifold.points[i].x;
                c.pointY[i] = worldManifold.points[i].y;
            }
            c.normalX = mineIsB ? worldManifold.normal.x : -worldManifold.normal.x;
            c.normalY = mineIsB ? worldManifold.normal.y : -worldManifold.normal.y;
        }
        return c;
    }

    private static boolean gone(Collider2D c) {
        return c.destroyed || c.gameObject == null || c.gameObject.destroyed;
    }

    /// Delivers what the last step, and any destruction since the one
    /// before, produced. Called outside the step, so a script may do
    /// anything in reply.
    static void dispatch() {
        if (events.isEmpty() && pairs.isEmpty()) {
            return;
        }
        java.lang.Object[] queued = events.toArray(); // NOPMD UnnecessaryFullyQualifiedName
        events.clear();
        // A copy, because a script told of one pair may end another; into
        // an array that is kept, because this runs every frame.
        int touchingCount = pairs.size();
        if (touching.length < touchingCount) {
            touching = new java.lang.Object[touchingCount * 2]; // NOPMD UnnecessaryFullyQualifiedName
        }
        for (int i = 0; i < touchingCount; i++) {
            touching[i] = pairs.get(i);
        }
        for (int i = 0; i < queued.length; i++) { // NOPMD ForLoopCanBeForeach
            Event e = (Event) queued[i];
            Pair p = e.pair;
            if (gone(p.a) || gone(p.b)) {
                continue;
            }
            int kind = e.exit ? 2 : 0;
            Collision2D forA = e.forA;
            Collision2D forB = e.forB;
            if (e.exit && !p.sensor) {
                forA = describe(p.a, p.b, null, false);
                forB = describe(p.b, p.a, null, true);
            }
            send(p.a, p.b, kind, p.sensor, forA);
            send(p.b, p.a, kind, p.sensor, forB);
        }
        for (int i = 0; i < touchingCount; i++) {
            Pair p = (Pair) touching[i];
            touching[i] = null;
            if (p.fresh) {
                p.fresh = false;
                continue;
            }
            if (p.count <= 0 || p.silent || gone(p.a) || gone(p.b)) {
                continue;
            }
            Collision2D forA = null;
            Collision2D forB = null;
            if (!p.sensor) {
                Contact contact = p.contact;
                boolean flipped = contact != null && contact.getFixtureA().getUserData() != p.a; // NOPMD CompareObjectsWithEquals
                if (p.stayA == null) {
                    p.stayA = new Collision2D();
                    p.stayB = new Collision2D();
                }
                forA = describe(p.stayA, p.a, p.b, contact, flipped);
                forB = describe(p.stayB, p.b, p.a, contact, !flipped);
            }
            send(p.a, p.b, 1, p.sensor, forA);
            send(p.b, p.a, 1, p.sensor, forB);
        }
    }

    /// Tells the scripts on a collider's object, and on its rigidbody's
    /// when that is another object, as Unity does. A script that is
    /// switched off is told too; Unity's documentation says so, so that a
    /// script can switch itself on in reply.
    private static void send(Collider2D mine, Collider2D theirs, int kind, boolean sensor, Collision2D collision) {
        deliver(mine.gameObject, theirs, kind, sensor, collision);
        GameObject owner = mine.owner;
        if (owner != null && owner != mine.gameObject && owner.GetComponent(Rigidbody2D.class) != null) { // NOPMD CompareObjectsWithEquals
            deliver(owner, theirs, kind, sensor, collision);
        }
    }

    private static void deliver(GameObject to, Collider2D theirs, int kind, boolean sensor, Collision2D collision) {
        if (to.destroyed || !to.activeInHierarchy()) {
            return;
        }
        // Counted and read by index, without a copy: this runs for every
        // pair in touch on every frame. A script that adds a component in
        // reply is not told of this contact, and one that takes a component
        // away leaves a shorter list, which the bound follows.
        ArrayList receivers = to.components;
        int count = receivers.size();
        for (int i = 0; i < count && i < receivers.size(); i++) {
            java.lang.Object receiver = receivers.get(i); // NOPMD UnnecessaryFullyQualifiedName
            if (!(receiver instanceof MonoBehaviour)) {
                continue;
            }
            MonoBehaviour m = (MonoBehaviour) receiver;
            if (m.destroyed) {
                continue;
            }
            if (sensor) {
                if (kind == 0) {
                    m.$onTriggerEnter2D(theirs);
                } else if (kind == 1) {
                    m.$onTriggerStay2D(theirs);
                } else {
                    m.$onTriggerExit2D(theirs);
                }
            } else if (kind == 0) {
                m.$onCollisionEnter2D(collision);
            } else if (kind == 1) {
                m.$onCollisionStay2D(collision);
            } else {
                m.$onCollisionExit2D(collision);
            }
        }
    }

    // -------------------------------------------------------- decomposition

    /// The convex pieces of paths generated code holds in a constant, found
    /// once however many colliders are made from it.
    static float[][] sharedPieces(float[][] paths) {
        for (int i = 0; i + 1 < decomposed.size(); i += 2) {
            if (decomposed.get(i) == paths) { // NOPMD CompareObjectsWithEquals
                return (float[][]) decomposed.get(i + 1);
            }
        }
        float[][] pieces = decompose(paths);
        decomposed.add(paths);
        decomposed.add(pieces);
        return pieces;
    }

    /// Cuts closed outlines into convex polygons of at most eight corners.
    ///
    /// Each outline is triangulated by clipping ears -- a corner whose
    /// triangle with its two neighbours holds no other corner is cut off,
    /// until three remain -- and the triangles are then joined back, two
    /// pieces at a time across a shared edge, wherever the result is still
    /// convex and small enough. The outline may wind either way; an outline
    /// that crosses itself gives pieces that overlap, which Box2D accepts.
    static float[][] decompose(float[][] paths) {
        ArrayList out = new ArrayList();
        for (int i = 0; i < paths.length; i++) { // NOPMD ForLoopCanBeForeach
            decomposePath(paths[i], out);
        }
        float[][] pieces = new float[out.size()][];
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = (float[]) out.get(i);
        }
        return pieces;
    }

    private static float cross(float[] p, int a, int b, int c) {
        float abx = p[b * 2] - p[a * 2];
        float aby = p[b * 2 + 1] - p[a * 2 + 1];
        float acx = p[c * 2] - p[a * 2];
        float acy = p[c * 2 + 1] - p[a * 2 + 1];
        float l = abx * acy;
        float m = aby * acx;
        return l - m;
    }

    private static boolean inside(float[] p, int a, int b, int c, int q) {
        return cross(p, a, b, q) >= 0f && cross(p, b, c, q) >= 0f && cross(p, c, a, q) >= 0f;
    }

    private static void decomposePath(float[] raw, ArrayList out) {
        // Drop a point that repeats the one before it; the first and last
        // are neighbours too.
        int n = 0;
        float[] p = new float[raw.length];
        for (int i = 0; i + 1 < raw.length; i += 2) {
            if (n > 0 && raw[i] == p[n * 2 - 2] && raw[i + 1] == p[n * 2 - 1]) {
                continue;
            }
            p[n * 2] = raw[i];
            p[n * 2 + 1] = raw[i + 1];
            n++;
        }
        if (n > 1 && p[0] == p[n * 2 - 2] && p[1] == p[n * 2 - 1]) {
            n--;
        }
        if (n < 3) {
            return;
        }
        float twice = 0f;
        for (int i = 0; i < n; i++) {
            int j = i + 1 == n ? 0 : i + 1;
            float l = p[i * 2] * p[j * 2 + 1];
            float m = p[j * 2] * p[i * 2 + 1];
            twice += l - m;
        }
        // Counter-clockwise from here on.
        int[] ring = new int[n];
        for (int i = 0; i < n; i++) {
            ring[i] = twice < 0f ? n - 1 - i : i;
        }
        ArrayList polygons = new ArrayList();
        int left = n;
        while (left > 3) {
            int ear = -1;
            for (int i = 0; i < left && ear < 0; i++) {
                int a = ring[(i + left - 1) % left];
                int b = ring[i];
                int c = ring[(i + 1) % left];
                if (!(cross(p, a, b, c) > 0f)) { // NOPMD LogicInversion
                    continue;
                }
                boolean empty = true;
                for (int k = 0; k < left && empty; k++) {
                    int q = ring[k];
                    if (q != a && q != b && q != c && inside(p, a, b, c, q)) {
                        empty = false;
                    }
                }
                if (empty) {
                    ear = i;
                }
            }
            if (ear < 0) {
                // Nothing is an ear: the outline is degenerate here. Drop a
                // corner so that the loop ends, and add no triangle for it.
                ear = 0;
            } else {
                polygons.add(new int[] {ring[(ear + left - 1) % left], ring[ear], ring[(ear + 1) % left]});
            }
            for (int k = ear; k + 1 < left; k++) {
                ring[k] = ring[k + 1];
            }
            left--;
        }
        if (cross(p, ring[0], ring[1], ring[2]) > 0f) {
            polygons.add(new int[] {ring[0], ring[1], ring[2]});
        }
        boolean joined = true;
        while (joined) {
            joined = false;
            for (int i = 0; i < polygons.size() && !joined; i++) {
                for (int j = i + 1; j < polygons.size() && !joined; j++) {
                    int[] both = join(p, (int[]) polygons.get(i), (int[]) polygons.get(j));
                    if (both != null) {
                        polygons.set(i, both);
                        polygons.remove(j);
                        joined = true;
                    }
                }
            }
        }
        for (int i = 0; i < polygons.size(); i++) { // NOPMD ForLoopCanBeForeach
            int[] poly = (int[]) polygons.get(i);
            float[] piece = new float[poly.length * 2];
            for (int k = 0; k < poly.length; k++) {
                piece[k * 2] = p[poly[k] * 2];
                piece[k * 2 + 1] = p[poly[k] * 2 + 1];
            }
            out.add(piece);
        }
    }

    /// Two counter-clockwise polygons that share an edge, as one, or null
    /// when they share none or the union is not convex or has too many
    /// corners.
    private static int[] join(float[] p, int[] a, int[] b) {
        if (a.length + b.length - 2 > 8) {
            return null;
        }
        for (int i = 0; i < a.length; i++) {
            int a0 = a[i];
            int a1 = a[(i + 1) % a.length];
            for (int j = 0; j < b.length; j++) {
                if (b[j] != a1 || b[(j + 1) % b.length] != a0) {
                    continue;
                }
                // a from a1 round to a0, then b from a0 round to a1, each
                // without its last corner, which the other begins with.
                int[] both = new int[a.length + b.length - 2];
                int n = 0;
                for (int k = 0; k < a.length - 1; k++) {
                    both[n++] = a[(i + 1 + k) % a.length];
                }
                for (int k = 0; k < b.length - 1; k++) {
                    both[n++] = b[(j + 1 + k) % b.length];
                }
                for (int k = 0; k < n; k++) {
                    if (cross(p, both[k], both[(k + 1) % n], both[(k + 2) % n]) < 0f) {
                        return null;
                    }
                }
                return both; // NOPMD AvoidBranchingStatementAsLastInLoop
            }
        }
        return null;
    }
}
