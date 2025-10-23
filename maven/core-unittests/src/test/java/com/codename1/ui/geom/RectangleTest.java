package com.codename1.ui.geom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class RectangleTest {

    @BeforeEach
    void resetPool() throws Exception {
        setPool(new ArrayList<Rectangle>());
    }

    @AfterEach
    void clearPool() throws Exception {
        setPool(null);
    }

    private void setPool(Object value) throws Exception {
        Field poolField = Rectangle.class.getDeclaredField("pool");
        poolField.setAccessible(true);
        poolField.set(null, value);
    }

    @Test
    void testStaticContains() {
        assertTrue(Rectangle.contains(0, 0, 10, 10, 2, 2, 3, 3));
        assertFalse(Rectangle.contains(0, 0, 5, 5, 4, 4, 3, 3));
    }

    @Test
    void testStaticIntersection() {
        Rectangle dest = new Rectangle();
        Rectangle.intersection(0, 0, 10, 10, 5, 5, 10, 10, dest);
        assertEquals(5, dest.getX());
        assertEquals(5, dest.getY());
        assertEquals(5, dest.getWidth());
        assertEquals(5, dest.getHeight());
    }

    @Test
    void testPoolReuse() {
        Rectangle first = Rectangle.createFromPool(1, 2, 3, 4);
        Rectangle.recycle(first);
        Rectangle second = Rectangle.createFromPool(5, 6, 7, 8);
        assertSame(first, second, "Recycled rectangle should be reused from the pool");
        assertEquals(5, second.getX());
        assertEquals(6, second.getY());
        assertEquals(7, second.getWidth());
        assertEquals(8, second.getHeight());
        Rectangle.recycle(second);
    }

    @Test
    void testInstanceContainsAndIntersects() {
        Rectangle rect = new Rectangle(0, 0, 10, 10);
        assertTrue(rect.contains(2, 2));
        assertTrue(rect.contains(0, 0, 5, 5));
        assertFalse(rect.contains(9, 9, 5, 5));
        assertTrue(rect.intersects(8, 8, 5, 5));
        assertFalse(rect.intersects(20, 20, 2, 2));
    }

    @Test
    void testIntersectionResults() {
        Rectangle base = new Rectangle(0, 0, 10, 10);
        Rectangle overlap = base.intersection(5, 5, 10, 10);
        assertEquals(new Rectangle(5, 5, 5, 5), overlap);

        Rectangle disjoint = base.intersection(20, 20, 5, 5);
        assertEquals(0, disjoint.getWidth());
        assertEquals(0, disjoint.getHeight());
    }

    @Test
    void testPathIteratorProducesRectangle() {
        Rectangle rect = new Rectangle(1, 2, 3, 4);
        PathIterator iterator = rect.getPathIterator();
        int[] expectedTypes = new int[]{PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO, PathIterator.SEG_LINETO,
                PathIterator.SEG_LINETO, PathIterator.SEG_CLOSE};
        float[][] expectedPoints = new float[][]{
                {1f, 2f},
                {4f, 2f},
                {4f, 6f},
                {1f, 6f}
        };
        float[] coords = new float[6];
        int index = 0;
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            assertEquals(expectedTypes[index], type);
            if (type != PathIterator.SEG_CLOSE) {
                assertEquals(expectedPoints[index][0], coords[0], 1e-6f);
                assertEquals(expectedPoints[index][1], coords[1], 1e-6f);
            }
            iterator.next();
            index++;
        }
        assertEquals(expectedTypes.length, index);
    }

    @Test
    void testEqualsAndHashCode() {
        Rectangle first = new Rectangle(0, 0, 5, 5);
        Rectangle second = new Rectangle(first);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        second.setX(1);
        assertNotEquals(first, second);
    }
}
