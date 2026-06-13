package com.codename1.gaming.physics.box2d.dynamics;

import com.codename1.gaming.physics.box2d.callbacks.QueryCallback;
import com.codename1.gaming.physics.box2d.callbacks.RayCastCallback;
import com.codename1.gaming.physics.box2d.collision.AABB;
import com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape;
import com.codename1.gaming.physics.box2d.common.Vec2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Richer Box2D world simulations that drive the parts of the engine the wrapper only
/// grazes: the solver island and contact solver (stacking), continuous collision /
/// time-of-impact (a bullet vs a thin wall), and the broad-phase dynamic tree
/// (raycast and AABB queries).
class Box2dWorldSimTest {

    private static Body box(World w, BodyType type, float x, float y, float hx, float hy) {
        BodyDef bd = new BodyDef();
        bd.type = type;
        bd.position.set(x, y);
        Body b = w.createBody(bd);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(hx, hy);
        b.createFixture(shape, 1f);
        return b;
    }

    private static void steps(World w, int n) {
        for (int i = 0; i < n; i++) {
            w.step(1f / 60f, 8, 3);
        }
    }

    @Test
    void stackSettlesOnFloor() {
        World w = new World(new Vec2(0, -10));
        box(w, BodyType.STATIC, 0, 0, 10, 0.5f);     // floor, top at y=0.5
        Body[] stack = new Body[5];
        for (int i = 0; i < stack.length; i++) {
            stack[i] = box(w, BodyType.DYNAMIC, 0, 2 + i * 1.1f, 0.5f, 0.5f);
        }
        steps(w, 300);
        // every box should come to rest above the floor, in order, none tunneling
        for (Body b : stack) {
            assertTrue(b.getPosition().y > 0.4f, "box fell through floor: " + b.getPosition().y);
            assertTrue(b.getPosition().y < 12f);
        }
        assertEquals(6, w.getBodyCount());           // floor + 5 boxes
    }

    @Test
    void bulletDoesNotTunnelThroughWall() {
        World w = new World(new Vec2(0, 0));         // no gravity
        box(w, BodyType.STATIC, 10, 0, 0.1f, 5);     // thin vertical wall at x=10
        Body bullet = box(w, BodyType.DYNAMIC, 0, 0, 0.25f, 0.25f);
        bullet.setBullet(true);
        bullet.setLinearVelocity(new Vec2(400, 0));  // very fast toward the wall
        steps(w, 30);
        // continuous collision must stop it at the wall, not let it pass through
        assertTrue(bullet.getPosition().x < 10.5f, "bullet tunneled: " + bullet.getPosition().x);
    }

    @Test
    void raycastHitsABody() {
        World w = new World(new Vec2(0, 0));
        box(w, BodyType.STATIC, 0, 0, 1, 1);
        steps(w, 1);                                  // populate the broad-phase
        final boolean[] hit = {false};
        final float[] frac = {-1f};
        RayCastCallback cb = new RayCastCallback() {
            @Override
            public float reportFixture(Fixture fixture, Vec2 point, Vec2 normal, float fraction) {
                hit[0] = true;
                frac[0] = fraction;
                return fraction;
            }
        };
        w.raycast(cb, new Vec2(-5, 0), new Vec2(5, 0));
        assertTrue(hit[0], "ray should hit the box");
        assertTrue(frac[0] >= 0 && frac[0] <= 1f);
    }

    @Test
    void raycastMissesEmptySpace() {
        World w = new World(new Vec2(0, 0));
        box(w, BodyType.STATIC, 0, 0, 1, 1);
        steps(w, 1);
        final boolean[] hit = {false};
        RayCastCallback cb = new RayCastCallback() {
            @Override
            public float reportFixture(Fixture fixture, Vec2 point, Vec2 normal, float fraction) {
                hit[0] = true;
                return fraction;
            }
        };
        w.raycast(cb, new Vec2(-5, 50), new Vec2(5, 50));   // well above the box
        assertFalse(hit[0]);
    }

    @Test
    void queryAabbFindsFixtures() {
        World w = new World(new Vec2(0, 0));
        box(w, BodyType.STATIC, 0, 0, 1, 1);
        box(w, BodyType.STATIC, 50, 50, 1, 1);
        steps(w, 1);
        final int[] count = {0};
        QueryCallback qc = new QueryCallback() {
            @Override
            public boolean reportFixture(Fixture fixture) {
                count[0]++;
                return true;
            }
        };
        w.queryAABB(qc, new AABB(new Vec2(-3, -3), new Vec2(3, 3)));
        assertEquals(1, count[0]);   // only the box at the origin
    }

    @Test
    void destroyBodyRemovesItFromTheWorld() {
        World w = new World(new Vec2(0, -10));
        Body a = box(w, BodyType.DYNAMIC, 0, 5, 0.5f, 0.5f);
        box(w, BodyType.DYNAMIC, 2, 5, 0.5f, 0.5f);
        assertEquals(2, w.getBodyCount());
        w.destroyBody(a);
        assertEquals(1, w.getBodyCount());
        steps(w, 20);   // keeps simulating cleanly after removal
    }
}
