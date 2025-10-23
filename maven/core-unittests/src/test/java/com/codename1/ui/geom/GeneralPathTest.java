package com.codename1.ui.geom;

import com.codename1.ui.Transform;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeneralPathTest {

    @Test
    void testBasicPathOperations() {
        GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO);
        path.moveTo(0, 0);
        path.lineTo(10, 0);
        path.quadTo(20, 20, 30, 0);
        path.curveTo(40, 10, 50, -10, 60, 0);
        path.closePath();

        assertEquals(5, path.getTypesSize(), "Unexpected number of path commands");
        assertEquals(14, path.getPointsSize(), "Unexpected number of stored coordinates");

        byte[] types = new byte[path.getTypesSize()];
        path.getTypes(types);
        assertArrayEquals(new byte[]{PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO, PathIterator.SEG_QUADTO,
                PathIterator.SEG_CUBICTO, PathIterator.SEG_CLOSE}, types, "Unexpected command sequence");

        float[] points = new float[path.getPointsSize()];
        path.getPoints(points);
        assertEquals(60f, points[path.getPointsSize() - 2], 1e-6f, "Last x coordinate should match the final curve endpoint");
        assertEquals(0f, points[path.getPointsSize() - 1], 1e-6f, "Last y coordinate should match the final curve endpoint");

        float[] current = path.getCurrentPoint();
        assertArrayEquals(new float[]{0f, 0f}, current, 1e-6f, "Current point after closePath should be the start of the subpath");
    }

    @Test
    void testBoundsAndReset() {
        GeneralPath path = new GeneralPath();
        Rectangle rect = new Rectangle(10, 20, 30, 40);
        path.setRect(rect, null);

        Rectangle bounds = path.getBounds();
        assertEquals(10, bounds.getX());
        assertEquals(20, bounds.getY());
        assertEquals(30, bounds.getWidth());
        assertEquals(40, bounds.getHeight());
        assertTrue(path.isRectangle(), "setRect should create a rectangular path");

        path.reset();
        Rectangle empty = path.getBounds();
        assertEquals(0, empty.getWidth());
        assertEquals(0, empty.getHeight());
        assertEquals(0, path.getTypesSize());
    }

    @Test
    void testSetRectAndTransform() {
        GeneralPath path = new GeneralPath();
        Rectangle rect = new Rectangle(10, 20, 30, 40);
        path.setRect(rect, Transform.makeTranslation(5, -5));

        Rectangle translated = path.getBounds();
        assertEquals(15, translated.getX());
        assertEquals(15, translated.getY());
        assertEquals(30, translated.getWidth());
        assertEquals(40, translated.getHeight());
        assertTrue(path.isRectangle());

        path.transform(Transform.makeScale(2, 3));
        Rectangle scaled = path.getBounds();
        assertEquals(30, scaled.getX());
        assertEquals(45, scaled.getY());
        assertEquals(60, scaled.getWidth());
        assertEquals(120, scaled.getHeight());
    }

    @Test
    void testAppendAndSetPath() {
        GeneralPath base = new GeneralPath();
        base.moveTo(0, 0);
        base.lineTo(5, 0);

        GeneralPath addition = new GeneralPath();
        addition.moveTo(5, 0);
        addition.lineTo(5, 5);
        addition.closePath();

        base.append(addition, true);
        assertEquals(4, base.getTypesSize(), "Append with connect should convert initial move into a line");

        float[] currentPoint = base.getCurrentPoint();
        assertArrayEquals(new float[]{0f, 0f}, currentPoint, 1e-6f, "Closed path should report the start as current point");

        GeneralPath copy = new GeneralPath();
        copy.setPath(base, Transform.makeTranslation(10, 10));
        Rectangle bounds = copy.getBounds();
        assertEquals(10, bounds.getX());
        assertEquals(10, bounds.getY());
        assertEquals(5, bounds.getWidth());
        assertEquals(5, bounds.getHeight());
    }

    @Test
    void testIntersectionAndContains() {
        GeneralPath path = new GeneralPath();
        path.setRect(new Rectangle(0, 0, 10, 10), null);

        assertTrue(path.contains(5, 5));
        assertFalse(path.contains(15, 5));

        boolean intersected = path.intersect(new Rectangle(5, 5, 10, 10));
        assertTrue(intersected, "Intersecting rectangles should leave a non-empty path");
        Rectangle intersectionBounds = path.getBounds();
        assertEquals(5, intersectionBounds.getX());
        assertEquals(5, intersectionBounds.getY());
        assertEquals(5, intersectionBounds.getWidth());
        assertEquals(5, intersectionBounds.getHeight());

        boolean cleared = path.intersect(new Rectangle(20, 20, 5, 5));
        assertFalse(cleared, "Non-overlapping intersection should return false");
        assertEquals(0, path.getTypesSize(), "Path should be cleared when intersection is empty");
    }

    @Test
    void testConvexPolygonDetection() {
        assertTrue(GeneralPath.isConvexPolygon(new float[]{0, 6, 6, 0}, new float[]{0, 0, 6, 6}));
        assertFalse(GeneralPath.isConvexPolygon(new float[]{0, 6, 3, 6, 0}, new float[]{0, 0, 3, 6, 6}));

        assertTrue(GeneralPath.isConvexPolygon(new int[]{0, 6, 6, 0}, new int[]{0, 0, 6, 6}));
        assertFalse(GeneralPath.isConvexPolygon(new int[]{0, 6, 3, 6, 0}, new int[]{0, 0, 3, 6, 6}));
    }

    @Test
    void testCreateFromPoolReuse() {
        GeneralPath first = GeneralPath.createFromPool();
        first.moveTo(1, 1);
        GeneralPath.recycle(first);

        GeneralPath second = GeneralPath.createFromPool();
        assertSame(first, second, "Returned path should be reused from the pool");
        assertEquals(0, second.getTypesSize(), "Reused path should be reset to empty state");
        GeneralPath.recycle(second);
    }

    @Test
    void testSetShapeCopiesNonRectangleShape() {
        GeneralPath source = new GeneralPath();
        source.moveTo(0, 0);
        source.curveTo(1, 2, 3, 4, 5, 6);
        GeneralPath target = new GeneralPath();
        target.setShape(source, Transform.makeTranslation(2, 3));

        assertEquals(source.getTypesSize(), target.getTypesSize());
        float[] points = new float[target.getPointsSize()];
        target.getPoints(points);
        boolean containsTranslatedX = false;
        for (float value : points) {
            if (Math.abs(value - 2f) < 1e-6f) {
                containsTranslatedX = true;
                break;
            }
        }
        assertTrue(containsTranslatedX, "Translated shape should include the shifted x coordinate");
        Rectangle bounds = target.getBounds();
        assertEquals(2, bounds.getX());
        assertEquals(3, bounds.getY());
    }
}
