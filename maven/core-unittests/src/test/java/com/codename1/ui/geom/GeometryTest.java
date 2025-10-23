package com.codename1.ui.geom;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeometryTest {

    @Test
    void testBezierCurveConstructionAndCopy() {
        Geometry.BezierCurve curve = new Geometry.BezierCurve(0, 0, 1, 1, 2, 0);
        assertEquals(2, curve.n());
        assertEquals(0d, curve.getStartPoint().getX(), 1e-6);
        assertEquals(0d, curve.getStartPoint().getY(), 1e-6);
        assertEquals(2d, curve.getEndPoint().getX(), 1e-6);
        assertEquals(0d, curve.getEndPoint().getY(), 1e-6);

        Geometry.BezierCurve copy = new Geometry.BezierCurve(curve);
        assertTrue(curve.equals(copy, 1e-9));
    }

    @Test
    void testExtractBezierCurvesFromPath() {
        GeneralPath path = new GeneralPath();
        path.moveTo(0, 0);
        path.quadTo(1, 1, 2, 0);
        path.curveTo(3, 1, 4, -1, 5, 0);
        path.lineTo(6, 0);

        List<Geometry.BezierCurve> curves = new ArrayList<Geometry.BezierCurve>();
        Geometry.BezierCurve.extractBezierCurvesFromPath(path, curves);
        assertEquals(2, curves.size(), "Should extract quadratic and cubic segments only");
        assertEquals(0d, curves.get(0).getStartPoint().getX(), 1e-6);
        assertEquals(5d, curves.get(1).getEndPoint().getX(), 1e-6);
    }

    @Test
    void testPolynomialEvaluation() {
        Geometry.BezierCurve curve = new Geometry.BezierCurve(0, 0, 1, 1, 2, 0);
        assertEquals(0d, curve.x(0d), 1e-6);
        assertEquals(2d, curve.x(1d), 1e-6);
        assertEquals(1d, curve.x(0.5d), 1e-6);

        assertEquals(0d, curve.y(0d), 1e-6);
        assertEquals(0d, curve.y(1d), 1e-6);
        assertEquals(0.5d, curve.y(0.5d), 1e-6);
    }

    @Test
    void testDerivativeCoefficients() {
        Geometry.BezierCurve cubic = new Geometry.BezierCurve(0, 0, 1, 3, 2, 3, 3, 0);
        double[] dx = cubic.getDerivativeCoefficientsX();
        assertEquals(3d, dx[0], 1e-6);
        assertEquals(0d, dx[1], 1e-6);
        assertEquals(0d, dx[2], 1e-6);

        double[] dy = cubic.getDerivativeCoefficientsY();
        assertEquals(9d, dy[0], 1e-6);
        assertEquals(-18d, dy[1], 1e-6);
        assertEquals(0d, dy[2], 1e-6);

        Geometry.BezierCurve high = new Geometry.BezierCurve(0, 0, 1, 1, 2, 2, 3, 3, 4, 4);
        assertThrows(IllegalArgumentException.class, high::getDerivativeCoefficientsX);
    }

    @Test
    void testReverseAndSegment() {
        Geometry.BezierCurve curve = new Geometry.BezierCurve(0, 0, 1, 2, 2, 0);
        Geometry.BezierCurve reversed = curve.reverse();
        assertEquals(curve.getStartPoint().getX(), reversed.getEndPoint().getX(), 1e-6);
        assertEquals(curve.getStartPoint().getY(), reversed.getEndPoint().getY(), 1e-6);
        assertEquals(curve.getEndPoint().getX(), reversed.getStartPoint().getX(), 1e-6);
        assertEquals(curve.getEndPoint().getY(), reversed.getStartPoint().getY(), 1e-6);

        List<Geometry.BezierCurve> segments = new ArrayList<Geometry.BezierCurve>();
        curve.segment(0.5d, segments);
        assertEquals(2, segments.size());
        Geometry.BezierCurve first = segments.get(0);
        Geometry.BezierCurve second = segments.get(1);
        assertEquals(first.getEndPoint().getX(), second.getStartPoint().getX(), 1e-6);
        assertEquals(first.getEndPoint().getY(), second.getStartPoint().getY(), 1e-6);
    }

    @Test
    void testSegmentRectangleSplitsCurve() {
        Geometry.BezierCurve curve = new Geometry.BezierCurve(0, 0, 2, 4, 4, 0);
        Rectangle2D rect = new Rectangle2D(1d, 1d, 2d, 2d);
        List<Geometry.BezierCurve> pieces = new ArrayList<Geometry.BezierCurve>();
        curve.segment(rect, pieces);
        assertTrue(pieces.size() > 1, "Intersections with the rectangle should split the curve");
        Geometry.BezierCurve first = pieces.get(0);
        Geometry.BezierCurve last = pieces.get(pieces.size() - 1);
        assertEquals(curve.getStartPoint().getX(), first.getStartPoint().getX(), 1e-6);
        assertEquals(curve.getEndPoint().getX(), last.getEndPoint().getX(), 1e-6);
    }

    @Test
    void testFindTValuesForYWithinRange() {
        Geometry.BezierCurve curve = new Geometry.BezierCurve(0, 0, 2, 4, 4, 0);
        double[] result = new double[3];
        int count = curve.findTValuesForY(2d, 1d, 3d, result);
        assertEquals(1, count, "Expected a single tangential intersection at the chosen height");
        double t = result[0];
        assertTrue(t >= 0d && t <= 1d);
        assertEquals(0.5d, t, 1e-6);
        double x = curve.x(t);
        assertTrue(x >= 1d - 1e-6 && x <= 3d + 1e-6);
    }

    @Test
    void testBoundingRectCoversCurveExtents() {
        Geometry.BezierCurve curve = new Geometry.BezierCurve(0, 0, 1, 3, 2, -3, 3, 0);
        Rectangle2D bounds = curve.getBoundingRect();
        assertTrue(bounds.getWidth() > 0);
        assertTrue(bounds.getHeight() > 0);
        assertTrue(bounds.getX() <= 0);
        assertTrue(bounds.getX() + bounds.getWidth() >= 3);
    }
}
