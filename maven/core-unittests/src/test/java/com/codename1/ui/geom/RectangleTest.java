package com.codename1.ui.geom;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RectangleTest {

    @Test
    void testCreateFromPoolReusesInstances() {
        Rectangle first = Rectangle.createFromPool(1, 2, 3, 4);
        Rectangle.recycle(first);
        Rectangle second = Rectangle.createFromPool(5, 6, 7, 8);
        assertSame(first, second);
        assertEquals(5, second.getX());
        assertEquals(6, second.getY());
        assertEquals(7, second.getWidth());
        assertEquals(8, second.getHeight());
        Rectangle.recycle(second);
    }

    @Test
    void testContainsPointAndRectangle() {
        Rectangle rect = new Rectangle(0, 0, 10, 20);
        assertTrue(rect.contains(5, 5));
        assertTrue(rect.contains(0, 0, 10, 20));
        assertFalse(rect.contains(11, 5));
        Rectangle other = new Rectangle(2, 2, 5, 5);
        assertTrue(rect.contains(other));
    }

    @Test
    void testStaticContains() {
        assertTrue(Rectangle.contains(0, 0, 10, 10, 2, 2, 4, 4));
        assertFalse(Rectangle.contains(0, 0, 5, 5, 3, 3, 4, 4));
    }

    @Test
    void testIntersects() {
        assertTrue(Rectangle.intersects(0, 0, 10, 10, 5, 5, 10, 10));
        assertFalse(Rectangle.intersects(0, 0, 5, 5, 10, 10, 2, 2));
        Rectangle rect = new Rectangle(0, 0, 10, 10);
        assertTrue(rect.intersects(5, 5, 4, 4));
        assertFalse(rect.intersects(20, 20, 2, 2));
    }

    @Test
    void testStaticIntersectionPopulatesDestination() {
        Rectangle dest = new Rectangle();
        Rectangle.intersection(0, 0, 10, 10, 5, 5, 10, 10, dest);
        assertEquals(5, dest.getX());
        assertEquals(5, dest.getY());
        assertEquals(5, dest.getWidth());
        assertEquals(5, dest.getHeight());
    }

    @Test
    void testInstanceIntersectionProducesExpectedRectangle() {
        Rectangle rect = new Rectangle(0, 0, 10, 10);
        Rectangle intersection = rect.intersection(5, 5, 10, 10);
        assertEquals(new Rectangle(5, 5, 5, 5), intersection);
    }

    @Test
    void testIntersectionWithOutputRectangle() {
        Rectangle source = new Rectangle(0, 0, 10, 10);
        Rectangle input = new Rectangle(8, -5, 10, 10);
        Rectangle output = new Rectangle();
        source.intersection(input, output);
        assertEquals(8, output.getX());
        assertEquals(0, output.getY());
        assertEquals(2, output.getWidth());
        assertEquals(5, output.getHeight());
    }

    @Test
    void testPathIteratorCreatesRectangleShape() {
        Rectangle rect = new Rectangle(1, 2, 3, 4);
        PathIterator iterator = rect.getPathIterator();
        List<Integer> segments = new ArrayList<Integer>();
        float[] coords = new float[6];
        while (!iterator.isDone()) {
            segments.add(iterator.currentSegment(coords));
            iterator.next();
        }
        assertEquals(5, segments.size());
        assertEquals(PathIterator.SEG_MOVETO, segments.get(0).intValue());
        assertEquals(PathIterator.SEG_CLOSE, segments.get(4).intValue());
    }

    @Test
    void testEqualsAndHashCode() {
        Rectangle first = new Rectangle(1, 2, 3, 4);
        Rectangle second = new Rectangle(1, 2, 3, 4);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new Rectangle(2, 2, 3, 4));
    }

    @Test
    void testGetBounds2D() {
        Rectangle rect = new Rectangle(3, 4, 5, 6);
        float[] bounds = rect.getBounds2D();
        assertArrayEquals(new float[]{3f, 4f, 5f, 6f}, bounds, 0.0001f);
    }
}
