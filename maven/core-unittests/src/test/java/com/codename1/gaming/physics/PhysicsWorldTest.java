package com.codename1.gaming.physics;

import com.codename1.gaming.Sprite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link PhysicsWorld}. These drive the shaded Box2D engine through a
/// few real simulations (gravity, resting on a floor, contacts, sprite syncing), so
/// they exercise a large slice of the physics stack end to end.
class PhysicsWorldTest {

    private static final float DT = 1f / 60f;

    private static void steps(PhysicsWorld w, int n) {
        for (int i = 0; i < n; i++) {
            w.step(DT);
        }
    }

    @Test
    void defaultsAndPixelsPerMeter() {
        PhysicsWorld w = new PhysicsWorld(0, 600);
        assertEquals(30f, w.getPixelsPerMeter(), 0.001);
        assertNotNull(w.getNativeWorld());
        w.setPixelsPerMeter(50f);
        assertEquals(50f, w.getPixelsPerMeter(), 0.001);
    }

    @Test
    void dynamicBodyFallsUnderGravity() {
        PhysicsWorld w = new PhysicsWorld(0, 600);   // y+ is down the screen
        PhysicsBody box = w.createBox(100, 100, 20, 20, BodyType.DYNAMIC);
        float startY = box.getY();
        steps(w, 30);
        assertTrue(box.getY() > startY + 5, "box should fall downward; y=" + box.getY());
        assertEquals(100, box.getX(), 2.0, "no horizontal drift expected");
    }

    @Test
    void staticBodyDoesNotMove() {
        PhysicsWorld w = new PhysicsWorld(0, 600);
        PhysicsBody floor = w.createBox(160, 400, 320, 40, BodyType.STATIC);
        steps(w, 60);
        assertEquals(400, floor.getY(), 0.001);
        assertEquals(160, floor.getX(), 0.001);
    }

    @Test
    void dynamicBodyRestsOnStaticFloor() {
        PhysicsWorld w = new PhysicsWorld(0, 600);
        w.createBox(160, 400, 320, 40, BodyType.STATIC);     // floor, top edge at y=380
        PhysicsBody box = w.createBox(160, 100, 20, 20, BodyType.DYNAMIC);
        steps(w, 300);
        float y = box.getY();
        // it should settle on top of the floor (~370), not pass through it
        assertTrue(y > 350 && y < 390, "box should rest on the floor; y=" + y);
    }

    @Test
    void contactListenerFiresOnCollision() {
        PhysicsWorld w = new PhysicsWorld(0, 600);
        w.createBox(160, 400, 320, 40, BodyType.STATIC);
        w.createBox(160, 300, 20, 20, BodyType.DYNAMIC);
        final boolean[] began = {false};
        ContactListener l = new ContactListener() {
            @Override
            public void beginContact(PhysicsContact c) {
                began[0] = true;
                assertNotNull(c.getBodyA());
                assertNotNull(c.getBodyB());
            }
            @Override
            public void endContact(PhysicsContact c) {
            }
        };
        w.addContactListener(l);
        steps(w, 240);
        assertTrue(began[0], "the falling box should have contacted the floor");
        w.removeContactListener(l);
    }

    @Test
    void circleAndPolygonBodies() {
        PhysicsWorld w = new PhysicsWorld(0, 600);
        PhysicsBody circle = w.createCircle(100, 100, 15, BodyType.DYNAMIC);
        assertNotNull(circle.getNativeBody());
        PhysicsBody poly = w.createPolygon(200, 100,
                new float[]{0, 0, 20, 0, 10, 20}, BodyType.DYNAMIC);
        assertNotNull(poly.getNativeBody());
        steps(w, 20);
        assertTrue(circle.getY() > 100);   // both fall
        assertTrue(poly.getY() > 100);
    }

    @Test
    void removeBodyStopsItSimulating() {
        PhysicsWorld w = new PhysicsWorld(0, 600);
        PhysicsBody a = w.createBox(100, 100, 20, 20, BodyType.DYNAMIC);
        PhysicsBody b = w.createBox(200, 100, 20, 20, BodyType.DYNAMIC);
        w.removeBody(a);
        steps(w, 30);   // must not throw with a removed body
        assertTrue(b.getY() > 100);   // the surviving body still falls
    }

    @Test
    void stepSyncsLinkedSprite() {
        PhysicsWorld w = new PhysicsWorld(0, 600);
        PhysicsBody box = w.createBox(100, 100, 20, 20, BodyType.DYNAMIC);
        Sprite sprite = new Sprite();
        box.setLinkedSprite(sprite);
        assertSame(sprite, box.getLinkedSprite());
        steps(w, 30);
        assertTrue(sprite.getY() > 100, "the linked sprite should follow the body down");
        assertEquals(box.getY(), sprite.getY(), 0.5, "sprite tracks the body");
    }

    @Test
    void setGravityRedirectsFall() {
        PhysicsWorld w = new PhysicsWorld(0, 0);
        PhysicsBody box = w.createBox(100, 100, 20, 20, BodyType.DYNAMIC);
        w.setGravity(600, 0);   // gravity to the right, none vertically
        steps(w, 30);
        assertTrue(box.getX() > 105, "box should drift right; x=" + box.getX());
    }
}
