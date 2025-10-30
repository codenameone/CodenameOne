package com.codename1.ui.geom;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Transform;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeneralPathTest extends UITestBase {


    @Test
    void testLineToWithoutMoveThrowsException() {
        GeneralPath path = new GeneralPath();
        assertThrows(IndexOutOfBoundsException.class, () -> path.lineTo(1f, 1f));
    }

    @Test
    void testMoveToOverwritesPreviousMove() {
        GeneralPath path = new GeneralPath();
        path.moveTo(5f, 7f);
        assertEquals(1, path.typeSize);
        assertEquals(PathIterator.SEG_MOVETO, path.types[0]);
        path.moveTo(9f, 11f);
        assertEquals(1, path.typeSize);
        assertEquals(9f, path.points[0]);
        assertEquals(11f, path.points[1]);
    }

    @Test
    void testAppendWithConnection() {
        GeneralPath first = new GeneralPath();
        first.moveTo(0f, 0f);
        first.lineTo(1f, 1f);

        GeneralPath second = new GeneralPath();
        second.moveTo(2f, 2f);
        second.lineTo(3f, 3f);

        first.append(second, true);

        List<Integer> segmentTypes = new ArrayList<Integer>();
        List<float[]> coords = new ArrayList<float[]>();
        PathIterator iterator = first.getPathIterator();
        float[] buffer = new float[6];
        while (!iterator.isDone()) {
            segmentTypes.add(iterator.currentSegment(buffer));
            coords.add(new float[]{buffer[0], buffer[1]});
            iterator.next();
        }

        assertEquals(4, segmentTypes.size());
        assertEquals(PathIterator.SEG_MOVETO, segmentTypes.get(0).intValue());
        assertArrayEquals(new float[]{0f, 0f}, coords.get(0), 0.0001f);
        assertEquals(PathIterator.SEG_LINETO, segmentTypes.get(1).intValue());
        assertArrayEquals(new float[]{1f, 1f}, coords.get(1), 0.0001f);
        assertEquals(PathIterator.SEG_LINETO, segmentTypes.get(2).intValue());
        assertArrayEquals(new float[]{2f, 2f}, coords.get(2), 0.0001f);
        assertEquals(PathIterator.SEG_LINETO, segmentTypes.get(3).intValue());
        assertArrayEquals(new float[]{3f, 3f}, coords.get(3), 0.0001f);
    }

    @Test
    void testSetShapeWithTransform() {
        Rectangle rectangle = new Rectangle(5, 7, 10, 20);
        Transform transform = Transform.makeScale(2f, 3f);

        GeneralPath path = new GeneralPath();
        path.setShape(rectangle, transform);

        Rectangle bounds = path.getBounds();
        assertEquals(10, bounds.getX());
        assertEquals(21, bounds.getY());
        assertEquals(20, bounds.getWidth());
        assertEquals(60, bounds.getHeight());
        assertTrue(path.isRectangle());
    }

    @Test
    void testCreateTransformedShapeReturnsScaledCopy() {
        GeneralPath original = new GeneralPath();
        original.setRect(new Rectangle(0, 0, 10, 10), null);

        Transform translation = Transform.makeTranslation(5f, 10f);
        Shape transformed = original.createTransformedShape(translation);
        assertTrue(transformed instanceof GeneralPath);
        Rectangle bounds = transformed.getBounds();
        assertEquals(5, bounds.getX());
        assertEquals(10, bounds.getY());
        assertEquals(10, bounds.getWidth());
        assertEquals(10, bounds.getHeight());
    }

    @Test
    void testArcProducesExpectedBounds() {
        GeneralPath path = new GeneralPath();
        path.arc(100f, 200f, 300f, 400f, 0f, (float) (Math.PI * 2));
        Rectangle bounds = path.getBounds();
        assertEquals(100, bounds.getX());
        assertEquals(200, bounds.getY());
        assertEquals(300, bounds.getWidth());
        assertEquals(400, bounds.getHeight());
    }

    @Test
    void testDefaultWindingRuleAndValidation() {
        GeneralPath path = new GeneralPath();
        assertEquals(GeneralPath.WIND_NON_ZERO, path.getWindingRule());
        assertThrows(IllegalArgumentException.class, () -> path.setWindingRule(3));
        path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        assertEquals(GeneralPath.WIND_EVEN_ODD, path.getWindingRule());
    }

    @Test
    void testAppendWithoutConnectionCreatesSeparateSubPath() {
        GeneralPath first = new GeneralPath();
        first.moveTo(0f, 0f);
        first.lineTo(1f, 1f);

        GeneralPath second = new GeneralPath();
        second.moveTo(2f, 2f);
        second.lineTo(3f, 3f);

        first.append(second, false);

        List<Integer> segmentTypes = new ArrayList<Integer>();
        PathIterator iterator = first.getPathIterator();
        float[] coords = new float[6];
        while (!iterator.isDone()) {
            segmentTypes.add(iterator.currentSegment(coords));
            iterator.next();
        }

        assertEquals(5, segmentTypes.size());
        assertEquals(PathIterator.SEG_MOVETO, segmentTypes.get(0).intValue());
        assertEquals(PathIterator.SEG_MOVETO, segmentTypes.get(2).intValue());
    }

    @Test
    void testTransformScalingUpdatesBounds() {
        GeneralPath path = new GeneralPath();
        path.moveTo(1f, 1f);
        path.lineTo(2f, 1f);
        path.lineTo(2f, 2f);
        path.closePath();

        path.transform(Transform.makeScale(2f, 3f));

        Rectangle bounds = path.getBounds();
        assertEquals(2, bounds.getX());
        assertEquals(3, bounds.getY());
        assertEquals(2, bounds.getWidth());
        assertEquals(3, bounds.getHeight());
    }

    @Test
    void testContainsAndIntersectsRectangle() {
        GeneralPath path = new GeneralPath();
        path.setRect(new Rectangle(0, 0, 10, 10), null);
        assertTrue(path.contains(5, 5));
        assertFalse(path.contains(20, 20));

        Rectangle overlap = new Rectangle(8, 8, 10, 10);
        assertTrue(path.intersect(overlap));
        Rectangle noOverlap = new Rectangle(30, 30, 5, 5);
        assertFalse(path.intersect(noOverlap));
    }

    @Test
    void testEqualsWithTransform() {
        GeneralPath original = new GeneralPath();
        original.setRect(new Rectangle(0, 0, 10, 10), null);

        GeneralPath translated = new GeneralPath();
        translated.setRect(new Rectangle(5, 5, 10, 10), null);

        Transform translation = Transform.makeTranslation(5f, 5f);
        assertTrue(translated.equals(original, translation));
        assertFalse(translated.equals(original, null));
    }

    @Test
    void testGetCurrentPointAfterClosePath() {
        GeneralPath path = new GeneralPath();
        path.setRect(new Rectangle(0, 0, 10, 10), null);
        float[] current = path.getCurrentPoint();
        assertArrayEquals(new float[]{0f, 0f}, current, 0.0001f);
    }

    @Test
    void testResetClearsAllSegments() {
        GeneralPath path = new GeneralPath();
        path.moveTo(0f, 0f);
        path.lineTo(10f, 0f);
        assertNotNull(path.getCurrentPoint());
        path.reset();
        assertNull(path.getCurrentPoint());
        float[] bounds = path.getBounds2D();
        assertArrayEquals(new float[]{0f, 0f, 0f, 0f}, bounds, 0.0001f);
    }

    @Test
    void testIsRectangleDetection() {
        GeneralPath rectanglePath = new GeneralPath();
        rectanglePath.setRect(new Rectangle(0, 0, 5, 5), null);
        assertTrue(rectanglePath.isRectangle());

        GeneralPath curved = new GeneralPath();
        curved.moveTo(0f, 0f);
        curved.quadTo(5f, 5f, 10f, 0f);
        assertFalse(curved.isRectangle());
    }

    @Test
    void testIntersectWithRectangleUpdatesPath() {
        GeneralPath path = new GeneralPath();
        path.setRect(new Rectangle(0, 0, 10, 10), null);

        Rectangle intersecting = new Rectangle(5, 5, 10, 10);
        assertTrue(path.intersect(intersecting));
        Rectangle bounds = path.getBounds();
        assertEquals(5, bounds.getX());
        assertEquals(5, bounds.getY());
        assertEquals(5, bounds.getWidth());
        assertEquals(5, bounds.getHeight());

        Rectangle outside = new Rectangle(50, 50, 10, 10);
        assertFalse(path.intersect(outside));
        assertNull(path.getCurrentPoint());
    }
}
