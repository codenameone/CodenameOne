package com.codename1.gaming.physics;

import com.codename1.gaming.Sprite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link PhysicsBody}: transform, velocity, impulses, fixed rotation
/// and the fluent material/flag setters.
class PhysicsBodyTest {

    private static final float DT = 1f / 60f;

    private PhysicsWorld zeroG() {
        return new PhysicsWorld(0, 0);   // no gravity: isolate the effect under test
    }

    private static void steps(PhysicsWorld w, int n) {
        for (int i = 0; i < n; i++) {
            w.step(DT);
        }
    }

    @Test
    void setTransformRoundTripsPosition() {
        PhysicsWorld w = zeroG();
        PhysicsBody b = w.createBox(0, 0, 20, 20, BodyType.DYNAMIC);
        b.setTransform(50, 60, 0.5f);
        assertEquals(50, b.getX(), 0.01);
        assertEquals(60, b.getY(), 0.01);
        assertEquals(0.5, Math.abs(b.getRotation()), 0.01);
    }

    @Test
    void linearVelocityMovesTheBody() {
        PhysicsWorld w = zeroG();
        PhysicsBody b = w.createBox(100, 100, 20, 20, BodyType.DYNAMIC);
        b.setLinearVelocity(60, -30);
        assertEquals(60, b.getLinearVelocityX(), 1.0);
        assertEquals(-30, b.getLinearVelocityY(), 1.0);
        steps(w, 60);   // ~1 second
        assertTrue(b.getX() > 140, "moved right; x=" + b.getX());
        assertTrue(b.getY() < 90, "moved up; y=" + b.getY());
    }

    @Test
    void impulseImpartsVelocity() {
        PhysicsWorld w = zeroG();
        PhysicsBody b = w.createBox(100, 100, 20, 20, BodyType.DYNAMIC);
        assertEquals(0, b.getLinearVelocityX(), 0.001);
        b.applyLinearImpulse(500, 0);
        assertTrue(b.getLinearVelocityX() > 0, "impulse should impart rightward velocity");
    }

    @Test
    void angularVelocity() {
        PhysicsWorld w = zeroG();
        PhysicsBody b = w.createBox(100, 100, 20, 20, BodyType.DYNAMIC);
        b.setAngularVelocity(2f);
        assertEquals(2.0, Math.abs(b.getAngularVelocity()), 0.1);
    }

    @Test
    void fixedRotationBlocksTorque() {
        PhysicsWorld w = zeroG();
        PhysicsBody b = w.createBox(100, 100, 20, 20, BodyType.DYNAMIC);
        b.setFixedRotation(true);
        b.applyTorque(5000);
        steps(w, 30);
        assertEquals(0, b.getAngularVelocity(), 0.001);   // rotation locked
    }

    @Test
    void forceAcceleratesOverTime() {
        PhysicsWorld w = zeroG();
        PhysicsBody b = w.createBox(100, 100, 20, 20, BodyType.DYNAMIC);
        for (int i = 0; i < 30; i++) {
            b.applyForce(200, 0);   // sustained push (force must be re-applied each step)
            w.step(DT);
        }
        assertTrue(b.getLinearVelocityX() > 0);
        assertTrue(b.getX() > 100);
    }

    @Test
    void userDataAndLinkedSprite() {
        PhysicsWorld w = zeroG();
        PhysicsBody b = w.createBox(0, 0, 20, 20, BodyType.DYNAMIC);
        assertNull(b.getUserData());
        Object tag = new Object();
        b.setUserData(tag);
        assertSame(tag, b.getUserData());
        // a PhysicsLinkable (Sprite) is kept; anything else clears the link
        Sprite sprite = new Sprite();
        b.setLinkedSprite(sprite);
        assertSame(sprite, b.getLinkedSprite());
        b.setLinkedSprite(new Object());
        assertNull(b.getLinkedSprite());
        assertNotNull(b.getNativeBody());
    }

    @Test
    void fluentMaterialAndFlagSettersDoNotThrow() {
        PhysicsWorld w = new PhysicsWorld(0, 600);
        PhysicsBody b = w.createBox(100, 100, 20, 20, BodyType.DYNAMIC);
        b.setDensity(2.5f);
        b.setFriction(0.3f);
        b.setRestitution(0.6f);
        b.setSensor(true);
        b.setSensor(false);
        b.setBullet(true);
        b.setLinearDamping(0.2f);
        b.setAngularDamping(0.1f);
        b.applyForce(10, 10, 105, 105);   // force at a world point
        b.applyTorque(1f);
        steps(w, 10);                       // still simulates without error
        assertTrue(b.getY() > 100);
    }
}
