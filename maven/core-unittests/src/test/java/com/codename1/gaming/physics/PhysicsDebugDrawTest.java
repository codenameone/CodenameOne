package com.codename1.gaming.physics;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Smoke tests for the physics debug renderer: it walks the world's shapes and joints
/// and draws them onto a Graphics without error, for every shape type and flag combo.
class PhysicsDebugDrawTest extends UITestBase {

    private Graphics graphics() {
        return Image.createImage(320, 480, 0xff000000).getGraphics();
    }

    @Test
    void drawsShapesAndJointsWithoutError() {
        PhysicsWorld w = new PhysicsWorld(0, 600);
        w.createBox(160, 400, 320, 40, BodyType.STATIC);     // polygon
        w.createCircle(160, 100, 20, BodyType.DYNAMIC);       // circle
        PhysicsBody a = w.createBox(100, 100, 20, 20, BodyType.STATIC);
        PhysicsBody b = w.createBox(140, 100, 20, 20, BodyType.DYNAMIC);
        w.createRevoluteJoint(a, b, 120, 100);                // joint
        w.step(1f / 60f);

        Graphics g = graphics();
        // default flags (shapes + joints)
        assertDoesNotThrow(() -> w.debugDraw(g));
        // all flags including bounding boxes
        w.setDebugDrawFlags(true, true, true);
        assertDoesNotThrow(() -> w.debugDraw(g));
        // nothing selected -> still safe
        w.setDebugDrawFlags(false, false, false);
        assertDoesNotThrow(() -> w.debugDraw(g));
    }

    @Test
    void debugDrawIsReusableAndTunable() {
        PhysicsWorld w = new PhysicsWorld(0, 600);
        w.createBox(160, 400, 320, 40, BodyType.STATIC);
        PhysicsDebugDraw dd = w.getDebugDraw();
        assertNotNull(dd);
        assertSame(dd, w.getDebugDraw());        // created once, reused
        dd.setFillAlpha(40);
        w.step(1f / 60f);
        Graphics g = graphics();
        for (int i = 0; i < 3; i++) {            // safe across repeated frames
            assertDoesNotThrow(() -> w.debugDraw(g));
        }
    }
}
