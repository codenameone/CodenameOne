package com.codename1.ui.geom;

import com.codename1.ui.Transform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class GeneralPathTest {

    @BeforeEach
    void resetPools() throws Exception {
        Field pathPoolField = GeneralPath.class.getDeclaredField("pathPool");
        pathPoolField.setAccessible(true);
        pathPoolField.set(null, null);
    }

    @Test
    void convexPolygonDetectionRecognizesConcaveShapes() {
        float[] convexX = {0f, 2f, 2f, 0f};
        float[] convexY = {0f, 0f, 2f, 2f};
        assertTrue(GeneralPath.isConvexPolygon(convexX, convexY));

        float[] concaveX = {0f, 2f, 1f, 2f, 0f};
        float[] concaveY = {0f, 0f, 1f, 2f, 2f};
        assertFalse(GeneralPath.isConvexPolygon(concaveX, concaveY));
    }

    @Test
    void polygonDetectionRejectsCurves() {
        GeneralPath polygon = new GeneralPath();
        polygon.moveTo(0, 0);
        polygon.lineTo(10, 0);
        polygon.lineTo(10, 10);
        polygon.lineTo(0, 10);
        polygon.closePath();
        assertTrue(polygon.isPolygon());
        assertTrue(polygon.isRectangle());

        GeneralPath withCurve = new GeneralPath();
        withCurve.moveTo(0, 0);
        withCurve.quadTo(5, 5, 10, 0);
        withCurve.closePath();
        assertFalse(withCurve.isPolygon());
    }

    @Test
    void appendAndBoundsPreserveGeometry() {
        GeneralPath original = new GeneralPath();
        Rectangle rect = new Rectangle(5, 6, 20, 30);
        original.setRect(rect, null);

        Rectangle bounds = original.getBounds();
        assertEquals(5, bounds.getX());
        assertEquals(6, bounds.getY());
        assertEquals(20, bounds.getWidth());
        assertEquals(30, bounds.getHeight());

        GeneralPath appended = new GeneralPath();
        appended.append(original, true);
        Rectangle appendedBounds = appended.getBounds();
        assertEquals(bounds.getX(), appendedBounds.getX());
        assertEquals(bounds.getY(), appendedBounds.getY());
        assertEquals(bounds.getWidth(), appendedBounds.getWidth());
        assertEquals(bounds.getHeight(), appendedBounds.getHeight());

        float[] bounds2d = appended.getBounds2D();
        assertArrayEquals(new float[]{5f, 6f, 20f, 30f}, bounds2d);

        Rectangle reused = new Rectangle();
        appended.getBounds(reused);
        assertEquals(appendedBounds, reused);
    }

    @Test
    void setPathAppliesTransformAndResetWorks() {
        GeneralPath source = new GeneralPath();
        source.setRect(new Rectangle(0, 0, 10, 20), null);

        GeneralPath transformed = new GeneralPath();
        Transform scale = Transform.makeScale(2, 3);
        transformed.setPath(source, scale);

        Rectangle bounds = transformed.getBounds();
        assertEquals(0, bounds.getX());
        assertEquals(0, bounds.getY());
        assertEquals(20, bounds.getWidth());
        assertEquals(60, bounds.getHeight());

        transformed.reset();
        assertEquals(0, transformed.getPointsSize());
        assertEquals(0, transformed.getTypesSize());
    }

    @Test
    void containsAndIntersectUpdatePath() {
        GeneralPath path = new GeneralPath();
        path.setRect(new Rectangle(0, 0, 10, 10), null);
        assertTrue(path.contains(5, 5));
        assertFalse(path.contains(11, 5));

        Rectangle overlap = new Rectangle(5, 5, 10, 10);
        assertTrue(path.intersect(overlap));
        Rectangle bounds = path.getBounds();
        assertEquals(5, bounds.getX());
        assertEquals(5, bounds.getY());
        assertEquals(5, bounds.getWidth());
        assertEquals(5, bounds.getHeight());

        Rectangle outside = new Rectangle(100, 100, 10, 10);
        assertFalse(path.intersect(outside));
        assertEquals(0, path.getPointsSize());
        assertEquals(0, path.getTypesSize());
    }

    @Test
    void poolingReturnsResetPath() {
        GeneralPath pooled = GeneralPath.createFromPool();
        pooled.moveTo(0, 0);
        pooled.lineTo(1, 1);
        GeneralPath.recycle(pooled);

        GeneralPath reused = GeneralPath.createFromPool();
        assertSame(pooled, reused);
        assertEquals(0, reused.getPointsSize());
        assertEquals(0, reused.getTypesSize());
        GeneralPath.recycle(reused);
    }
}
