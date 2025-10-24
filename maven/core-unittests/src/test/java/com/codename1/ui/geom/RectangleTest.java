package com.codename1.ui.geom;

import com.codename1.ui.geom.PathIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class RectangleTest {

    @BeforeEach
    void resetPool() throws Exception {
        Field poolField = Rectangle.class.getDeclaredField("pool");
        poolField.setAccessible(true);
        poolField.set(null, new java.util.ArrayList<Rectangle>());
    }

    @Test
    void poolingReusesInstances() {
        Rectangle first = Rectangle.createFromPool(0, 0, 5, 5);
        Rectangle.recycle(first);
        Rectangle second = Rectangle.createFromPool(1, 2, 3, 4);
        assertSame(first, second);
        assertEquals(1, second.getX());
        assertEquals(2, second.getY());
        assertEquals(3, second.getWidth());
        assertEquals(4, second.getHeight());
    }

    @Test
    void containsAndIntersectsBehaveAsExpected() {
        Rectangle rect = new Rectangle(0, 0, 10, 10);
        assertTrue(rect.contains(2, 2));
        assertFalse(rect.contains(12, 5));
        assertTrue(rect.contains(new Rectangle(2, 2, 5, 5)));
        assertFalse(rect.contains(new Rectangle(-1, -1, 5, 5)));

        assertTrue(Rectangle.contains(0, 0, 10, 10, 2, 2, 4, 4));
        assertFalse(Rectangle.contains(0, 0, 10, 10, -5, 2, 4, 4));

        assertTrue(Rectangle.intersects(0, 0, 10, 10, 5, 5, 10, 10));
        assertFalse(Rectangle.intersects(0, 0, 10, 10, 20, 20, 5, 5));
    }

    @Test
    void intersectionProducesExpectedBounds() {
        Rectangle base = new Rectangle(0, 0, 10, 10);
        Rectangle result = base.intersection(5, 5, 10, 10);
        assertEquals(5, result.getX());
        assertEquals(5, result.getY());
        assertEquals(5, result.getWidth());
        assertEquals(5, result.getHeight());

        Rectangle reusable = new Rectangle();
        base.intersection(new Rectangle(3, 3, 4, 4), reusable);
        assertEquals(3, reusable.getX());
        assertEquals(3, reusable.getY());
        assertEquals(4, reusable.getWidth());
        assertEquals(4, reusable.getHeight());
    }

    @Test
    void pathIteratorEmitsRectangleAndEqualityWorks() {
        Rectangle rect = new Rectangle(1, 2, 3, 4);
        PathIterator iterator = rect.getPathIterator();
        float[] coords = new float[6];
        int moves = 0;
        int lines = 0;
        boolean closed = false;
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO) {
                moves++;
            } else if (type == PathIterator.SEG_LINETO) {
                lines++;
            } else if (type == PathIterator.SEG_CLOSE) {
                closed = true;
            }
            iterator.next();
        }
        assertEquals(1, moves);
        assertEquals(3, lines);
        assertTrue(closed);

        Rectangle equalRect = new Rectangle(rect);
        assertEquals(rect, equalRect);
        assertEquals(rect.hashCode(), equalRect.hashCode());
    }
}
