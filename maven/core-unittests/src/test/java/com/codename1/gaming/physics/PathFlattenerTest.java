package com.codename1.gaming.physics;

import com.codename1.ui.geom.GeneralPath;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link PathFlattener}, the converter that turns a Codename One
/// {@code Shape}/{@code GeneralPath} into the polylines {@code PhysicsWorld.createShape}
/// feeds Box2D. Pure geometry -- no engine, no Display.
class PathFlattenerTest {

    private static PathFlattener.Subpath only(List subpaths) {
        assertEquals(1, subpaths.size(), "expected exactly one subpath");
        return (PathFlattener.Subpath) subpaths.get(0);
    }

    @Test
    void closedSquareIsOneClosedConvexQuad() {
        GeneralPath p = new GeneralPath();
        p.moveTo(0, 0);
        p.lineTo(10, 0);
        p.lineTo(10, 10);
        p.lineTo(0, 10);
        p.closePath();

        PathFlattener.Subpath s = only(PathFlattener.flatten(p));
        assertTrue(s.closed, "square should be a closed loop");
        assertEquals(8, s.xy.length, "4 vertices => 8 floats (no closing duplicate)");
        assertTrue(PathFlattener.isConvex(s.xy, 4), "a square is convex");
    }

    @Test
    void explicitClosingVertexIsDropped() {
        // lines all the way back to the start before closePath -> the duplicate must go
        GeneralPath p = new GeneralPath();
        p.moveTo(0, 0);
        p.lineTo(10, 0);
        p.lineTo(10, 10);
        p.lineTo(0, 10);
        p.lineTo(0, 0);
        p.closePath();

        PathFlattener.Subpath s = only(PathFlattener.flatten(p));
        assertTrue(s.closed);
        assertEquals(8, s.xy.length, "the repeated start vertex should be removed");
    }

    @Test
    void openPathStaysOpen() {
        GeneralPath p = new GeneralPath();
        p.moveTo(0, 0);
        p.lineTo(10, 0);
        p.lineTo(10, 10);

        PathFlattener.Subpath s = only(PathFlattener.flatten(p));
        assertFalse(s.closed, "no closePath => open chain");
        assertEquals(6, s.xy.length);
    }

    @Test
    void multipleSubpathsAreSeparate() {
        GeneralPath p = new GeneralPath();
        p.moveTo(0, 0);
        p.lineTo(10, 0);
        p.lineTo(10, 10);
        p.closePath();
        p.moveTo(20, 20);
        p.lineTo(30, 20);
        p.lineTo(30, 30);
        p.closePath();

        List subs = PathFlattener.flatten(p);
        assertEquals(2, subs.size());
        assertTrue(((PathFlattener.Subpath) subs.get(0)).closed);
        assertTrue(((PathFlattener.Subpath) subs.get(1)).closed);
    }

    @Test
    void curvesAreFlattenedToManySegments() {
        GeneralPath p = new GeneralPath();
        p.moveTo(0, 0);
        p.quadTo(10, 20, 20, 0);   // one quadratic bend

        PathFlattener.Subpath s = only(PathFlattener.flatten(p));
        // 1 start vertex + 12 subdivisions = 13 points = 26 floats
        assertEquals(26, s.xy.length, "the curve should be subdivided, not kept as one edge");
        // the subdivided apex should bulge away from the chord (y != 0 in the middle)
        float midY = s.xy[(s.xy.length / 2) + 1];
        assertTrue(Math.abs(midY) > 1f, "flattened curve should follow the bend");
    }

    @Test
    void convexityDistinguishesSquareFromArrow() {
        float[] square = {0, 0, 10, 0, 10, 10, 0, 10};
        assertTrue(PathFlattener.isConvex(square, 4));

        // a concave "arrow"/chevron: the notch makes one turn reverse
        float[] arrow = {0, 0, 10, 5, 0, 10, 4, 5};
        assertFalse(PathFlattener.isConvex(arrow, 4));
    }
}
