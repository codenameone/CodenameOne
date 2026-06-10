package com.codename1.gaming.physics.box2d.common;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/// Covers the remaining MathUtils helpers (rounding, sqrt, angle reduction, random)
/// and the OBBViewportTransform camera math.
class Box2dMathUtilsViewportTest {

    private static final float EPS = 1e-3f;

    @Test
    void roundingAndAbs() {
        assertEquals(3, MathUtils.floor(3.9f));
        assertEquals(-4, MathUtils.floor(-3.1f));
        assertEquals(4, MathUtils.ceil(3.1f));
        assertEquals(4, MathUtils.round(3.6f));
        assertEquals(3.5f, MathUtils.abs(-3.5f), EPS);
        assertEquals(7, MathUtils.abs(-7));
        assertEquals(3f, MathUtils.sqrt(9f), EPS);
    }

    @Test
    void trigAndAngleReduction() {
        assertEquals((float) Math.sin(1.0), MathUtils.sin(1f), 1e-2);
        assertEquals((float) Math.sin(1.0), MathUtils.sinLUT(1f), 1e-2);
        assertEquals((float) Math.cos(1.0), MathUtils.cos(1f), 1e-2);
        float reduced = MathUtils.reduceAngle((float) (3 * Math.PI));   // -> within [-pi, pi]
        assertTrue(reduced >= -MathUtils.PI - EPS && reduced <= MathUtils.PI + EPS);
    }

    @Test
    void randomInRange() {
        for (int i = 0; i < 20; i++) {
            float v = MathUtils.randomFloat(-2f, 5f);
            assertTrue(v >= -2f && v <= 5f);
        }
        float seeded = MathUtils.randomFloat(new Random(42), 0f, 1f);
        assertTrue(seeded >= 0f && seeded <= 1f);
    }

    @Test
    void clampToOut() {
        Vec2 dest = new Vec2();
        MathUtils.clampToOut(new Vec2(10, -10), new Vec2(0, 0), new Vec2(3, 3), dest);
        assertEquals(3f, dest.x, EPS);
        assertEquals(0f, dest.y, EPS);
    }

    @Test
    void viewportCameraTransform() {
        OBBViewportTransform vp = new OBBViewportTransform();
        vp.setExtents(400, 300);
        vp.setCenter(0, 0);
        vp.setCamera(0, 0, 1f);
        assertEquals(400f, vp.getExtents().x, EPS);
        assertEquals(0f, vp.getCenter().x, EPS);
        vp.setYFlip(true);
        assertTrue(vp.isYFlip());
        vp.setYFlip(false);
        assertFalse(vp.isYFlip());

        // world <-> screen should round-trip
        Vec2 screen = new Vec2();
        vp.getWorldToScreen(new Vec2(1, 2), screen);
        Vec2 world = new Vec2();
        vp.getScreenToWorld(screen, world);
        assertEquals(1f, world.x, 1e-2);
        assertEquals(2f, world.y, 1e-2);

        OBBViewportTransform copy = new OBBViewportTransform();
        copy.set(vp);
        assertEquals(vp.getCenter().x, copy.getCenter().x, EPS);
        vp.setExtents(new Vec2(200, 150));
        assertEquals(200f, vp.getExtents().x, EPS);
        vp.mulByTransform(vp.getTransform());
    }
}
