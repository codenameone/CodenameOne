package com.codename1.gaming.physics.box2d.collision.shapes;

import com.codename1.gaming.physics.box2d.collision.AABB;
import com.codename1.gaming.physics.box2d.common.Transform;
import com.codename1.gaming.physics.box2d.common.Vec2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Direct tests for the Box2D collision shapes: geometry, point tests, AABBs and mass
/// properties.
class Box2dShapesTest {

    private static final Transform IDENTITY = new Transform();
    static {
        IDENTITY.setIdentity();
    }

    @Test
    void polygonBox() {
        PolygonShape p = new PolygonShape();
        p.setAsBox(2, 1);
        assertEquals(4, p.getVertexCount());
        assertEquals(ShapeType.POLYGON, p.getType());
        assertTrue(p.testPoint(IDENTITY, new Vec2(0, 0)));      // center is inside
        assertFalse(p.testPoint(IDENTITY, new Vec2(5, 0)));     // outside the box

        AABB aabb = new AABB();
        p.computeAABB(aabb, IDENTITY, 0);
        assertTrue(aabb.isValid());
        // a 4x2 box centered at the origin (the AABB includes Box2D's small skin radius)
        assertEquals(0f, aabb.getCenter().x, 1e-3);
        assertEquals(2f, aabb.getExtents().x, 0.05);
        assertEquals(1f, aabb.getExtents().y, 0.05);

        MassData md = new MassData();
        p.computeMass(md, 1f);
        assertEquals(8f, md.mass, 1e-3);   // 4x2 box (half-extents 2,1) = area 8 * density 1
    }

    @Test
    void polygonFromVertices() {
        PolygonShape p = new PolygonShape();
        Vec2[] verts = {new Vec2(0, 0), new Vec2(2, 0), new Vec2(2, 2), new Vec2(0, 2)};
        p.set(verts, 4);
        assertEquals(4, p.getVertexCount());
        MassData md = new MassData();
        p.computeMass(md, 2f);
        assertTrue(md.mass > 0);
    }

    @Test
    void circle() {
        CircleShape c = new CircleShape();
        c.m_radius = 1.5f;
        c.m_p.set(0, 0);
        assertEquals(1, c.getVertexCount());
        assertEquals(ShapeType.CIRCLE, c.getType());
        assertEquals(1.5f, c.getRadius(), 1e-3);
        assertTrue(c.testPoint(IDENTITY, new Vec2(0, 0)));
        assertTrue(c.testPoint(IDENTITY, new Vec2(1, 0)));      // within radius
        assertFalse(c.testPoint(IDENTITY, new Vec2(3, 0)));     // outside

        AABB aabb = new AABB();
        c.computeAABB(aabb, IDENTITY, 0);
        assertEquals(1.5f, aabb.getExtents().x, 1e-3);

        MassData md = new MassData();
        c.computeMass(md, 1f);
        assertEquals((float) (Math.PI * 1.5 * 1.5), md.mass, 1e-2);
    }

    @Test
    void edge() {
        EdgeShape e = new EdgeShape();
        e.set(new Vec2(-2, 0), new Vec2(2, 0));
        assertEquals(ShapeType.EDGE, e.getType());
        AABB aabb = new AABB();
        e.computeAABB(aabb, IDENTITY, 0);
        assertTrue(aabb.isValid());
        MassData md = new MassData();
        e.computeMass(md, 1f);
        assertEquals(0f, md.mass, 1e-3);   // an edge has no area/mass
    }

    @Test
    void chain() {
        ChainShape ch = new ChainShape();
        Vec2[] verts = {new Vec2(-2, 0), new Vec2(-1, 1), new Vec2(0, 0),
                new Vec2(1, 1), new Vec2(2, 0)};
        ch.createChain(verts, verts.length);
        assertEquals(ShapeType.CHAIN, ch.getType());
        ch.setPrevVertex(new Vec2(-3, 0));
        ch.setNextVertex(new Vec2(3, 0));

        EdgeShape childEdge = new EdgeShape();
        ch.getChildEdge(childEdge, 0);
        assertEquals(ShapeType.EDGE, childEdge.getType());

        AABB aabb = new AABB();
        ch.computeAABB(aabb, IDENTITY, 0);
        assertTrue(aabb.isValid());
        MassData md = new MassData();
        ch.computeMass(md, 1f);
        assertEquals(0f, md.mass, 1e-3);   // a chain is massless

        ChainShape loop = new ChainShape();
        loop.createLoop(new Vec2[]{new Vec2(0, 0), new Vec2(2, 0),
                new Vec2(2, 2), new Vec2(0, 2)}, 4);
        assertEquals(ShapeType.CHAIN, loop.getType());
    }

    @Test
    void aabbGeometry() {
        AABB a = new AABB(new Vec2(0, 0), new Vec2(10, 6));
        assertTrue(a.isValid());
        assertEquals(5f, a.getCenter().x, 1e-3);
        assertEquals(3f, a.getCenter().y, 1e-3);
        assertEquals(5f, a.getExtents().x, 1e-3);
        assertEquals(3f, a.getExtents().y, 1e-3);

        AABB b = new AABB(new Vec2(5, 3), new Vec2(15, 9));
        assertTrue(AABB.testOverlap(a, b));
        AABB far = new AABB(new Vec2(100, 100), new Vec2(110, 110));
        assertFalse(AABB.testOverlap(a, far));

        AABB combined = new AABB();
        combined.combine(a, b);
        assertTrue(combined.isValid());
        assertEquals(0f, combined.lowerBound.x, 1e-3);
        assertEquals(15f, combined.upperBound.x, 1e-3);
    }
}
