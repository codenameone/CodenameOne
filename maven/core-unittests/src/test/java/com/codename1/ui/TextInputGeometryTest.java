package com.codename1.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextInputGeometryTest {
    @Test void snapshotDoesNotRetainMutableArrays() {
        int[] points = {0,10,20,2,12, 1,20,20,2,12, 2,30,20,2,12};
        int[] selection = {10,20,20,12};
        int[] fallback = {30,20,2,12};
        TextInputGeometry geometry = new TextInputGeometry(points, 0, 2, selection, fallback);
        points[1] = 900; selection[0] = 900; fallback[0] = 900;
        assertArrayEquals(new int[]{10,20,2,12}, geometry.rectForOffset(0));
        assertArrayEquals(new int[]{10,20,20,12}, geometry.selectionRects(2,0));
        assertArrayEquals(new int[]{30,20,2,12}, geometry.rectForOffset(99));
        int[] returned = geometry.rectForOffset(0); returned[0] = 400;
        assertEquals(10, geometry.rectForOffset(0)[0]);
    }

    @Test void hitTestingChoosesNearestLineThenCaretAndSupportsRtlPositions() {
        TextInputGeometry geometry = new TextInputGeometry(new int[]{
            0,10,20,2,12, 1,30,20,2,12, 2,20,20,2,12,
            3,10,40,2,12, 4,30,40,2,12}, 0,0,new int[0],new int[]{10,20,2,12});
        assertEquals(2, geometry.offsetAtPoint(21,24));
        assertEquals(4, geometry.offsetAtPoint(29,45));
        assertEquals(3, geometry.offsetAtPoint(-100,500));
        assertArrayEquals(new int[0], geometry.selectionRects(1,1));
        assertArrayEquals(new int[]{20,20,10,12}, geometry.selectionRects(1,2));
    }
}
