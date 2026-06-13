package com.codename1.gaming.physics.box2d.dynamics;

import com.codename1.gaming.physics.box2d.collision.shapes.CircleShape;
import com.codename1.gaming.physics.box2d.collision.shapes.EdgeShape;
import com.codename1.gaming.physics.box2d.collision.shapes.PolygonShape;
import com.codename1.gaming.physics.box2d.collision.shapes.ShapeType;
import com.codename1.gaming.physics.box2d.common.Vec2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Drives the different narrow-phase collision routines (circle-circle,
/// polygon-circle, edge-polygon, edge-circle) by simulating each shape combination,
/// plus the fixture material accessors.
class Box2dCollisionScenariosTest {

    private static void steps(World w, int n) {
        for (int i = 0; i < n; i++) {
            w.step(1f / 60f, 8, 3);
        }
    }

    private static Body polyFloor(World w) {
        BodyDef bd = new BodyDef();
        bd.type = BodyType.STATIC;
        bd.position.set(0, 0);
        Body b = w.createBody(bd);
        PolygonShape s = new PolygonShape();
        s.setAsBox(20, 0.5f);
        b.createFixture(s, 0f);
        return b;
    }

    private static Body edgeGround(World w) {
        BodyDef bd = new BodyDef();
        bd.type = BodyType.STATIC;
        bd.position.setZero();
        Body b = w.createBody(bd);
        EdgeShape e = new EdgeShape();
        e.set(new Vec2(-20, 0), new Vec2(20, 0));
        b.createFixture(e, 0f);
        return b;
    }

    private static Body circle(World w, float x, float y, float r) {
        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(x, y);
        Body b = w.createBody(bd);
        CircleShape c = new CircleShape();
        c.m_radius = r;
        c.m_p.set(0, 0);
        b.createFixture(c, 1f);
        return b;
    }

    private static Body boxBody(World w, float x, float y) {
        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(x, y);
        Body b = w.createBody(bd);
        PolygonShape s = new PolygonShape();
        s.setAsBox(0.5f, 0.5f);
        b.createFixture(s, 1f);
        return b;
    }

    @Test
    void circleRestsOnPolygonFloor() {
        World w = new World(new Vec2(0, -10));
        polyFloor(w);
        Body c = circle(w, 0, 5, 0.5f);
        steps(w, 200);
        assertTrue(c.getPosition().y > 0.4f && c.getPosition().y < 2f,
                "circle should rest on the floor; y=" + c.getPosition().y);
    }

    @Test
    void twoCirclesCollide() {
        World w = new World(new Vec2(0, 0));
        Body a = circle(w, 0, 0, 0.5f);
        Body b = circle(w, 1, 0, 0.5f);
        a.setLinearVelocity(new Vec2(5, 0));   // drive a into b
        steps(w, 60);
        // momentum transfer pushes b to the right
        assertTrue(b.getPosition().x > 1.1f, "circle b should be pushed; x=" + b.getPosition().x);
    }

    @Test
    void boxRestsOnEdgeGround() {
        World w = new World(new Vec2(0, -10));
        edgeGround(w);
        Body box = boxBody(w, 0, 5);
        steps(w, 220);
        assertTrue(box.getPosition().y > 0.4f && box.getPosition().y < 2f,
                "box should rest on the edge; y=" + box.getPosition().y);
    }

    @Test
    void circleRestsOnEdgeGround() {
        World w = new World(new Vec2(0, -10));
        edgeGround(w);
        Body c = circle(w, 0, 5, 0.5f);
        steps(w, 220);
        assertTrue(c.getPosition().y > 0.4f && c.getPosition().y < 2f,
                "circle should rest on the edge; y=" + c.getPosition().y);
    }

    @Test
    void fixtureAccessors() {
        World w = new World(new Vec2(0, -10));
        Body b = boxBody(w, 0, 5);
        Fixture f = b.getFixtureList();
        assertNotNull(f);
        assertSame(b, f.getBody());
        assertEquals(ShapeType.POLYGON, f.getShape().getType());
        assertEquals(1f, f.getDensity(), 1e-3);
        assertFalse(f.isSensor());
        f.setFriction(0.7f);
        assertEquals(0.7f, f.getFriction(), 1e-3);
        f.setRestitution(0.3f);
        assertEquals(0.3f, f.getRestitution(), 1e-3);
        f.setSensor(true);
        assertTrue(f.isSensor());
    }
}
