package com.codename1.gaming.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Tests the pixel-space joint wrapper added on top of Box2D: creation, body
/// linkage, the mouse-joint target, and destruction.
class PhysicsJointTest {

    private static void steps(PhysicsWorld w, int n) {
        for (int i = 0; i < n; i++) {
            w.step(1f / 60f);
        }
    }

    private PhysicsWorld world() {
        return new PhysicsWorld(0, 600);
    }

    @Test
    void revoluteJointLinksBodies() {
        PhysicsWorld w = world();
        PhysicsBody a = w.createBox(100, 100, 20, 20, BodyType.STATIC);
        PhysicsBody b = w.createBox(140, 100, 20, 20, BodyType.DYNAMIC);
        PhysicsJoint j = w.createRevoluteJoint(a, b, 120, 100);
        assertSame(a, j.getBodyA());
        assertSame(b, j.getBodyB());
        assertTrue(j.isActive());
        assertNotNull(j.getNativeJoint());
        steps(w, 30);
        j.destroy();
    }

    @Test
    void distanceJointHoldsBodiesApart() {
        PhysicsWorld w = world();
        PhysicsBody a = w.createBox(100, 100, 20, 20, BodyType.STATIC);
        PhysicsBody b = w.createBox(100, 200, 20, 20, BodyType.DYNAMIC);
        PhysicsJoint j = w.createDistanceJoint(a, b, 100, 100, 100, 200, 0f, 0f);
        steps(w, 120);
        // the rigid distance joint should keep b roughly 100px below a
        float dy = (float) (b.getY() - a.getY());
        assertTrue(dy > 80 && dy < 120, "distance not maintained: " + dy);
        j.destroy();
    }

    @Test
    void weldJointLocksBodies() {
        PhysicsWorld w = world();
        PhysicsBody a = w.createBox(100, 100, 20, 20, BodyType.STATIC);
        PhysicsBody b = w.createBox(140, 100, 20, 20, BodyType.DYNAMIC);
        PhysicsJoint j = w.createWeldJoint(a, b, 120, 100);
        steps(w, 60);
        // welded to a static body, b must barely move
        assertEquals(140, b.getX(), 5);
        assertEquals(100, b.getY(), 5);
        j.destroy();
    }

    @Test
    void prismaticJointConstrainsToAxis() {
        PhysicsWorld w = world();
        PhysicsBody a = w.createBox(100, 100, 20, 20, BodyType.STATIC);
        PhysicsBody b = w.createBox(100, 100, 20, 20, BodyType.DYNAMIC);
        PhysicsJoint j = w.createPrismaticJoint(a, b, 100, 100, 1, 0);   // horizontal slider
        steps(w, 60);
        // gravity is vertical but the joint only allows horizontal motion -> y stays put
        assertEquals(100, b.getY(), 3);
        j.destroy();
    }

    @Test
    void mouseJointDragsTowardTarget() {
        PhysicsWorld w = world();
        PhysicsBody ground = w.createBox(0, 0, 20, 20, BodyType.STATIC);
        PhysicsBody b = w.createBox(100, 300, 20, 20, BodyType.DYNAMIC);
        PhysicsJoint j = w.createMouseJoint(ground, b, 100, 300, 1000f * 1f);
        j.setTarget(100, 100);   // pull it up
        steps(w, 120);
        assertTrue(b.getY() < 280, "mouse joint should pull the body up; y=" + b.getY());
        j.destroy();
    }
}
