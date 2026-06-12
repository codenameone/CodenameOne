package com.codename1.gaming.physics.box2d.dynamics;

import com.codename1.gaming.physics.box2d.collision.shapes.MassData;
import com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape;
import com.codename1.gaming.physics.box2d.common.Vec2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Exercises the broad accessor/mutator surface of Body and World -- the many small
/// getters, setters, *ToOut variants and world settings that the simulation tests
/// only touch incidentally.
class Box2dBodyWorldApiTest {

    private Body dynamicBox(World w) {
        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(3, 4);
        Body b = w.createBody(bd);
        PolygonShape s = new PolygonShape();
        s.setAsBox(0.5f, 0.5f);
        b.createFixture(s, 1f);
        return b;
    }

    @Test
    void bodyTransformAndPointConversions() {
        World w = new World(new Vec2(0, -10));
        Body b = dynamicBox(w);
        b.setTransform(new Vec2(5, 6), (float) (Math.PI / 4));
        assertEquals(5, b.getPosition().x, 1e-3);
        assertEquals((float) (Math.PI / 4), b.getAngle(), 1e-3);
        assertNotNull(b.getTransform());
        assertNotNull(b.getWorldCenter());
        assertNotNull(b.getLocalCenter());

        // local <-> world point round-trips
        Vec2 local = new Vec2(0.25f, 0.1f);
        Vec2 world = b.getWorldPoint(local);
        Vec2 back = b.getLocalPoint(world);
        assertEquals(local.x, back.x, 1e-3);
        assertEquals(local.y, back.y, 1e-3);
        Vec2 out = new Vec2();
        b.getWorldPointToOut(local, out);
        assertEquals(world.x, out.x, 1e-3);
        b.getLocalPointToOut(world, new Vec2());

        // vectors (rotation only)
        Vec2 wv = b.getWorldVector(new Vec2(1, 0));
        assertEquals(b.getLocalVector(wv).x, 1f, 1e-3);
        b.getWorldVectorToOut(new Vec2(1, 0), new Vec2());
        b.getLocalVectorToOut(wv, new Vec2());
    }

    @Test
    void bodyVelocityForcesAndImpulses() {
        World w = new World(new Vec2(0, 0));
        Body b = dynamicBox(w);
        b.setLinearVelocity(new Vec2(2, 3));
        assertEquals(2, b.getLinearVelocity().x, 1e-3);
        b.setAngularVelocity(1.5f);
        assertEquals(1.5f, b.getAngularVelocity(), 1e-3);
        b.applyForce(new Vec2(1, 0), b.getWorldCenter());
        b.applyForceToCenter(new Vec2(0, 1));
        b.applyTorque(0.5f);
        b.applyLinearImpulse(new Vec2(1, 1), b.getWorldCenter());
        b.applyAngularImpulse(0.3f);
        assertNotNull(b.getLinearVelocityFromWorldPoint(new Vec2(0, 0)));
        assertNotNull(b.getLinearVelocityFromLocalPoint(new Vec2(0, 0)));
        b.getLinearVelocityFromWorldPointToOut(new Vec2(0, 0), new Vec2());
        b.getLinearVelocityFromLocalPointToOut(new Vec2(0, 0), new Vec2());
    }

    @Test
    void bodyMassProperties() {
        World w = new World(new Vec2(0, -10));
        Body b = dynamicBox(w);
        assertTrue(b.getMass() > 0);
        assertTrue(b.getInertia() > 0);
        MassData md = new MassData();
        b.getMassData(md);
        assertTrue(md.mass > 0);
        md.mass = 5f;
        md.I = 2f;
        b.setMassData(md);
        assertEquals(5f, b.getMass(), 1e-3);
        b.resetMassData();   // recompute from fixtures
        assertTrue(b.getMass() > 0);
    }

    @Test
    void bodyFlagsAndDamping() {
        World w = new World(new Vec2(0, -10));
        Body b = dynamicBox(w);
        b.setLinearDamping(0.4f);
        assertEquals(0.4f, b.getLinearDamping(), 1e-3);
        b.setAngularDamping(0.2f);
        assertEquals(0.2f, b.getAngularDamping(), 1e-3);
        b.setGravityScale(0.5f);
        assertEquals(0.5f, b.getGravityScale(), 1e-3);
        b.setBullet(true);
        assertTrue(b.isBullet());
        b.setFixedRotation(true);
        assertTrue(b.isFixedRotation());
        b.setAwake(true);
        assertTrue(b.isAwake());
        b.setActive(true);
        assertTrue(b.isActive());
        b.setSleepingAllowed(true);
        assertTrue(b.isSleepingAllowed());
        assertEquals(BodyType.DYNAMIC, b.getType());
        b.setType(BodyType.KINEMATIC);
        assertEquals(BodyType.KINEMATIC, b.getType());
        assertSame(w, b.getWorld());
        b.setUserData("tag");
        assertEquals("tag", b.getUserData());
        assertNotNull(b.getFixtureList());
        assertNull(b.getJointList());
    }

    @Test
    void worldSettingsAndCounters() {
        World w = new World(new Vec2(0, -10));
        w.setAllowSleep(false);
        assertFalse(w.isAllowSleep());
        w.setSubStepping(true);
        assertTrue(w.isSubStepping());
        w.setWarmStarting(false);
        assertFalse(w.isWarmStarting());
        w.setContinuousPhysics(false);
        assertFalse(w.isContinuousPhysics());
        w.setAutoClearForces(false);
        assertFalse(w.getAutoClearForces());
        w.setSleepingAllowed(true);
        assertTrue(w.isSleepingAllowed());

        w.setGravity(new Vec2(0, -20));
        assertEquals(-20, w.getGravity().y, 1e-3);

        dynamicBox(w);
        dynamicBox(w);
        w.step(1f / 60f, 8, 3);
        assertEquals(2, w.getBodyCount());
        assertEquals(0, w.getJointCount());
        assertEquals(2, w.getProxyCount());
        assertTrue(w.getTreeHeight() >= 0);
        w.getTreeBalance();
        w.getTreeQuality();
        assertTrue(w.getContactCount() >= 0);
        assertNotNull(w.getBodyList());
        assertNull(w.getJointList());
        assertFalse(w.isLocked());
        assertNotNull(w.getPool());
        // listener hooks accept null without error
        w.setContactFilter(null);
        w.setContactListener(null);
        w.setDestructionListener(null);
    }
}
