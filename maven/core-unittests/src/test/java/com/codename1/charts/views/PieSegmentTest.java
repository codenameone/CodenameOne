package com.codename1.charts.views;

import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.PathIterator;
import com.codename1.ui.geom.Shape;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PieSegmentTest {
    @Test
    public void testIsInSegmentWithinBounds() {
        PieSegment segment = new PieSegment(0, 10f, 45f, 90f);
        assertTrue(segment.isInSegment(60));
        assertFalse(segment.isInSegment(10));
    }

    @Test
    public void testIsInSegmentHandlesWrapAround() {
        PieSegment segment = new PieSegment(1, 15f, 300f, 120f);
        assertTrue(segment.isInSegment(30));
        assertTrue(segment.isInSegment(350));
        assertFalse(segment.isInSegment(150));
    }

    @Test
    public void testAccessorsExposeValues() {
        PieSegment segment = new PieSegment(2, 20f, 10f, 45f);
        assertEquals(10f, segment.getStartAngle(), 1e-6f);
        assertEquals(55f, segment.getEndAngle(), 1e-6f);
        assertEquals(2, segment.getDataIndex());
        assertEquals(20f, segment.getValue(), 1e-6f);
    }

    @Test
    public void testToStringIncludesAllFields() {
        PieSegment segment = new PieSegment(3, 5f, 0f, 90f);
        String text = segment.toString();
        assertTrue(text.contains("mDataIndex=3"));
        assertTrue(text.contains("mValue=5.0"));
        assertTrue(text.contains("mStartAngle=0.0"));
        assertTrue(text.contains("mEndAngle=90.0"));
    }

    @Test
    public void testGetShapeBuildsWedgePath() {
        PieSegment segment = new PieSegment(4, 12f, 0f, 90f);
        Shape shape = segment.getShape(50f, 60f, 10f);
        assertTrue(shape instanceof GeneralPath);
        GeneralPath path = (GeneralPath) shape;

        PathIterator iterator = path.getPathIterator();
        float[] coords = new float[6];

        assertEquals(PathIterator.SEG_MOVETO, iterator.currentSegment(coords));
        assertEquals(50f, coords[0], 1e-4f);
        assertEquals(60f, coords[1], 1e-4f);

        iterator.next();

        boolean sawStartLine = false;
        boolean sawArcSegment = false;
        boolean sawEndLine = false;
        boolean sawClose = false;
        float[] arcEnd = new float[2];

        float expectedStartX = 50f + 10f;
        float expectedStartY = 60f;
        float expectedEndX = 50f;
        float expectedEndY = 60f + 10f;

        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_LINETO:
                    if (!sawStartLine) {
                        assertEquals(expectedStartX, coords[0], 1e-4f);
                        assertEquals(expectedStartY, coords[1], 1e-4f);
                        sawStartLine = true;
                    } else {
                        assertEquals(expectedEndX, coords[0], 1e-4f);
                        assertEquals(expectedEndY, coords[1], 1e-4f);
                        sawEndLine = true;
                    }
                    break;
                case PathIterator.SEG_QUADTO:
                case PathIterator.SEG_CUBICTO:
                    sawArcSegment = true;
                    int endIndex = type == PathIterator.SEG_QUADTO ? 2 : 4;
                    arcEnd[0] = coords[endIndex];
                    arcEnd[1] = coords[endIndex + 1];
                    break;
                case PathIterator.SEG_CLOSE:
                    sawClose = true;
                    break;
                default:
                    break;
            }
            iterator.next();
        }

        assertTrue(sawStartLine);
        assertTrue(sawArcSegment);
        assertEquals(expectedEndX, arcEnd[0], 1e-3f);
        assertEquals(expectedEndY, arcEnd[1], 1e-3f);
        assertTrue(sawEndLine);
        assertTrue(sawClose);
    }
}
