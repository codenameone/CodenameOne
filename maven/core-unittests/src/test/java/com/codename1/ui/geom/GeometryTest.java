package com.codename1.ui.geom;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.Geometry.BezierCurve;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeometryTest extends UITestBase {

    @Test
    void testBezierCurveRequiresEvenNumberOfCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new BezierCurve(0d, 0d, 1d));
    }

    @Test
    void testCopyConstructorCreatesIndependentCurve() {
        BezierCurve original = new BezierCurve(0d, 0d, 5d, 10d, 10d, 0d);
        BezierCurve copy = new BezierCurve(original);
        assertNotSame(original, copy);
        assertEquals(original.getStartPoint().getX(), copy.getStartPoint().getX(), 0.0001);
        assertEquals(original.getEndPoint().getY(), copy.getEndPoint().getY(), 0.0001);
    }

    @Test
    void testExtractBezierCurvesFromPath() {
        GeneralPath path = new GeneralPath();
        path.moveTo(0f, 0f);
        path.quadTo(10f, 20f, 20f, 0f);
        path.curveTo(30f, 30f, 40f, -10f, 50f, 0f);
        List<BezierCurve> curves = new ArrayList<BezierCurve>();
        BezierCurve.extractBezierCurvesFromPath(path, curves);
        assertEquals(2, curves.size());
        BezierCurve first = curves.get(0);
        assertEquals(0d, first.getStartPoint().getX(), 0.0001);
        assertEquals(0d, first.getStartPoint().getY(), 0.0001);
        BezierCurve second = curves.get(1);
        assertEquals(20d, second.getStartPoint().getX(), 0.0001);
    }

    @Test
    void testParametricEvaluationForQuadraticCurve() {
        BezierCurve curve = new BezierCurve(0d, 0d, 50d, 100d, 100d, 0d);
        assertEquals(0d, curve.x(0d), 0.0001);
        assertEquals(50d, curve.x(0.5d), 0.0001);
        assertEquals(100d, curve.x(1d), 0.0001);
        assertEquals(0d, curve.y(0d), 0.0001);
        assertEquals(50d, curve.y(0.5d), 0.0001);
        assertEquals(0d, curve.y(1d), 0.0001);
    }

    @Test
    void testDerivativeCoefficientsForQuadraticCurve() {
        BezierCurve curve = new BezierCurve(0d, 0d, 50d, 100d, 100d, 0d);
        double[] derivativeX = curve.getDerivativeCoefficientsX();
        assertEquals(100d, derivativeX[0], 0.0001);
        assertEquals(0d, derivativeX[1], 0.0001);
        assertEquals(0d, derivativeX[2], 0.0001);
        double[] derivativeY = curve.getDerivativeCoefficientsY();
        assertEquals(200d, derivativeY[0], 0.0001);
        assertEquals(-400d, derivativeY[1], 0.0001);
        assertEquals(0d, derivativeY[2], 0.0001);
    }

    @Test
    void testDerivativeCoefficientsUnsupportedOrder() {
        BezierCurve curve = new BezierCurve(0d, 0d, 1d, 1d, 2d, 0d, 3d, -1d, 4d, 0d);
        assertThrows(IllegalArgumentException.class, curve::getDerivativeCoefficientsX);
        assertThrows(IllegalArgumentException.class, curve::getDerivativeCoefficientsY);
    }

    @Test
    void testReverseSwapsStartAndEndPoints() {
        BezierCurve curve = new BezierCurve(0d, 0d, 10d, 0d);
        BezierCurve reversed = curve.reverse();
        assertEquals(curve.getStartPoint().getX(), reversed.getEndPoint().getX(), 0.0001);
        assertEquals(curve.getEndPoint().getY(), reversed.getStartPoint().getY(), 0.0001);
    }

    @Test
    void testSegmentSplitsCurveIntoTwoParts() {
        BezierCurve curve = new BezierCurve(0d, 0d, 50d, 100d, 100d, 0d);
        List<BezierCurve> segments = new ArrayList<BezierCurve>();
        curve.segment(0.5d, segments);
        assertEquals(2, segments.size());
        BezierCurve firstHalf = segments.get(0);
        BezierCurve secondHalf = segments.get(1);
        assertEquals(curve.getStartPoint().getX(), firstHalf.getStartPoint().getX(), 0.0001);
        assertEquals(curve.getEndPoint().getY(), secondHalf.getEndPoint().getY(), 0.0001);
        assertEquals(firstHalf.getEndPoint().getX(), secondHalf.getStartPoint().getX(), 0.0001);
    }

    @Test
    void testSegmentRejectsInvalidParameters() {
        BezierCurve curve = new BezierCurve(0d, 0d, 50d, 100d, 100d, 0d);
        assertThrows(IllegalArgumentException.class, () -> curve.segment(0d, new ArrayList<BezierCurve>()));
        assertThrows(IllegalArgumentException.class, () -> curve.segment(1d, new ArrayList<BezierCurve>()));
    }

    @Test
    void testBoundingRectIncludesCurveExtrema() {
        BezierCurve curve = new BezierCurve(0d, 0d, 50d, 100d, 100d, 0d);
        Rectangle2D bounds = curve.getBoundingRect();
        assertEquals(0d, bounds.getX(), 0.0001);
        assertEquals(0d, bounds.getY(), 0.0001);
        assertEquals(100d, bounds.getWidth(), 0.0001);
        assertEquals(50d, bounds.getHeight(), 0.0001);
    }

    @Test
    void testSegmentWithRectangleOutsideReturnsOriginal() {
        BezierCurve curve = new BezierCurve(0d, 0d, 50d, 100d, 100d, 0d);
        Rectangle2D rect = new Rectangle2D(200d, 200d, 10d, 10d);
        List<BezierCurve> segments = new ArrayList<BezierCurve>();
        curve.segment(rect, segments);
        assertEquals(1, segments.size());
        assertTrue(curve.equals(segments.get(0), 0.0001));
    }

    @Test
    void testFindTValuesForXFiltersOutsideRange() {
        BezierCurve curve = new BezierCurve(0d, 0d, 50d, 100d, 100d, 0d);
        double[] results = new double[3];
        int count = curve.findTValuesForX(50d, 30d, 80d, results);
        assertEquals(1, count);
        assertEquals(0.5d, results[0], 0.0001);
    }

    @Test
    void testFindTValuesForYFiltersOutsideRange() {
        BezierCurve curve = new BezierCurve(0d, 0d, 50d, 100d, 100d, 0d);
        double[] results = new double[3];
        int count = curve.findTValuesForY(50d, 20d, 80d, results);
        assertEquals(1, count);
        assertEquals(0.5d, results[0], 0.0001);
    }

    @Test
    void testSegmentRectangleSplitsAtIntersections() {
        BezierCurve curve = new BezierCurve(0d, 0d, 50d, 120d, 100d, 0d);
        Rectangle2D rect = new Rectangle2D(20d, 20d, 60d, 60d);
        List<BezierCurve> segments = new ArrayList<BezierCurve>();
        curve.segment(rect, segments);
        assertTrue(segments.size() >= 2);
    }

    @Test
    void testStrokeInvokesDrawShapeWithTranslation() {
        BezierCurve curve = new BezierCurve(0d, 0d, 5d, 5d, 10d, 10d);
        Graphics graphics = Image.createImage(10, 10).getGraphics();
        implementation.setShapeSupported(true);
        implementation.resetShapeTracking();
        curve.stroke(graphics, new Stroke(1, Stroke.CAP_SQUARE, Stroke.JOIN_MITER, 1f), 2, 3);
        Shape shape = implementation.getLastDrawShape();
        assertNotNull(shape);
        Rectangle bounds = shape.getBounds();
        assertEquals(2, bounds.getX());
        assertEquals(3, bounds.getY());
        assertTrue(implementation.wasDrawShapeInvoked());
    }

    @Test
    void testAddToPathJoinsExistingPath() {
        BezierCurve curve = new BezierCurve(0d, 0d, 10d, 10d);
        GeneralPath path = new GeneralPath();
        path.moveTo(-5f, -5f);
        curve.addToPath(path, true);
        PathIterator iterator = path.getPathIterator();
        float[] coords = new float[6];
        iterator.next();
        assertEquals(PathIterator.SEG_LINETO, iterator.currentSegment(coords));
    }

    @Test
    void testEqualsRespectsEpsilon() {
        BezierCurve baseline = new BezierCurve(0d, 0d, 50d, 100d, 100d, 0d);
        BezierCurve almost = new BezierCurve(0d, 0d, 50.0005d, 100d, 100d, 0d);
        assertTrue(baseline.equals(almost, 0.001d));
        assertFalse(baseline.equals(almost, 0.0001d));
    }
}
