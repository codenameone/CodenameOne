package com.codename1.ui.geom;

import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.Shape;
import com.codename1.impl.CodenameOneImplementation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeometryTest {
    private CodenameOneImplementation originalDisplayImpl;
    private CodenameOneImplementation impl;

    @BeforeEach
    void setUp() throws Exception {
        Field implField = com.codename1.ui.Display.class.getDeclaredField("impl");
        implField.setAccessible(true);
        originalDisplayImpl = (CodenameOneImplementation) implField.get(null);
        impl = mock(CodenameOneImplementation.class, withSettings().lenient());
        implField.set(null, impl);
        when(impl.isShapeSupported()).thenReturn(true);
        when(impl.getClipX(any())).thenReturn(0);
        when(impl.getClipY(any())).thenReturn(0);
        when(impl.getClipWidth(any())).thenReturn(100);
        when(impl.getClipHeight(any())).thenReturn(100);
        when(impl.isTranslationSupported()).thenReturn(false);
    }

    @AfterEach
    void tearDown() throws Exception {
        Field implField = com.codename1.ui.Display.class.getDeclaredField("impl");
        implField.setAccessible(true);
        implField.set(null, originalDisplayImpl);
    }

    @Test
    void factorialHandlesValidAndInvalidInputs() throws Exception {
        Method factorial = Geometry.class.getDeclaredMethod("factorial", int.class);
        factorial.setAccessible(true);
        assertEquals(1, factorial.invoke(null, 0));
        assertEquals(120, factorial.invoke(null, 5));
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> factorial.invoke(null, -1));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void bezierCurvePropertiesAndSegmentation() {
        Geometry.BezierCurve curve = new Geometry.BezierCurve(0, 0, 5, 10, 10, 0, 15, 5);
        assertEquals(3, curve.n());
        assertEquals(0.0, curve.getStartPoint().getX(), 1e-6);
        assertEquals(0.0, curve.getStartPoint().getY(), 1e-6);
        assertEquals(15.0, curve.getEndPoint().getX(), 1e-6);
        assertEquals(5.0, curve.getEndPoint().getY(), 1e-6);

        double midX = curve.x(0.5);
        double midY = curve.y(0.5);
        assertNotEquals(curve.getStartPoint().getX(), midX);
        assertNotEquals(curve.getStartPoint().getY(), midY);

        double[] derivativeX = curve.getDerivativeCoefficientsX();
        double[] derivativeY = curve.getDerivativeCoefficientsY();
        assertEquals(3, derivativeX.length);
        assertEquals(3, derivativeY.length);

        List<Geometry.BezierCurve> segments = new ArrayList<Geometry.BezierCurve>();
        curve.segment(0.5, segments);
        assertEquals(2, segments.size());
        assertTrue(segments.get(0).getEndPoint().equals(segments.get(1).getStartPoint()));

        Geometry.BezierCurve reversed = curve.reverse();
        assertEquals(curve.getStartPoint().getX(), reversed.getEndPoint().getX(), 1e-6);
        assertEquals(curve.getEndPoint().getX(), reversed.getStartPoint().getX(), 1e-6);
    }

    @Test
    void bezierCurveBoundingRectAndEquals() {
        Geometry.BezierCurve curve = new Geometry.BezierCurve(0, 0, 10, 15, 20, -5);
        Rectangle2D bounds = curve.getBoundingRect();
        assertTrue(bounds.getWidth() > 0);
        assertTrue(bounds.getHeight() > 0);

        Geometry.BezierCurve copy = new Geometry.BezierCurve(curve);
        assertTrue(curve.equals(copy, 1e-6));
        Geometry.BezierCurve adjusted = new Geometry.BezierCurve(0, 0, 10, 16, 20, -5);
        assertFalse(curve.equals(adjusted, 1e-6));
    }

    @Test
    void extractBezierCurvesFromPath() {
        GeneralPath path = new GeneralPath();
        path.moveTo(0, 0);
        path.quadTo(10, 10, 20, 0);
        path.curveTo(25, 5, 30, -5, 40, 0);

        List<Geometry.BezierCurve> curves = new ArrayList<Geometry.BezierCurve>();
        Geometry.BezierCurve.extractBezierCurvesFromPath(path, curves);
        assertEquals(2, curves.size());
        assertEquals(0.0, curves.get(0).getStartPoint().getX(), 1e-6);
        assertEquals(0.0, curves.get(0).getStartPoint().getY(), 1e-6);
        assertEquals(40.0, curves.get(1).getEndPoint().getX(), 1e-6);
    }

    @Test
    void segmentingByRectangleSplitsCurve() {
        Geometry.BezierCurve curve = new Geometry.BezierCurve(0, 0, 10, 15, 20, 5);
        Rectangle2D rect = new Rectangle2D(5, 0, 10, 10);
        List<Geometry.BezierCurve> result = new ArrayList<Geometry.BezierCurve>();
        curve.segment(rect, result);
        assertFalse(result.isEmpty());
        for (Geometry.BezierCurve c : result) {
            Rectangle2D b = c.getBoundingRect();
            assertTrue(b.getX() >= rect.getX() - 0.01);
            assertTrue(b.getX() + b.getWidth() <= rect.getX() + rect.getWidth() + 0.01);
        }
    }

    @Test
    void strokeUsesGraphicsDrawShape() throws Exception {
        Constructor<Graphics> ctor = Graphics.class.getDeclaredConstructor(Object.class);
        ctor.setAccessible(true);
        Graphics graphics = ctor.newInstance(new Object());

        Geometry.BezierCurve curve = new Geometry.BezierCurve(0, 0, 5, 5, 10, 0);
        Stroke stroke = new Stroke(2, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1f);
        curve.stroke(graphics, stroke, 3, 4);

        ArgumentCaptor<Shape> shapeCaptor = ArgumentCaptor.forClass(Shape.class);
        verify(impl).drawShape(eq(graphics.getGraphics()), shapeCaptor.capture(), eq(stroke));
        Shape drawnShape = shapeCaptor.getValue();
        assertNotNull(drawnShape);
        Rectangle bounds = drawnShape.getBounds();
        assertEquals(3, bounds.getX());
        assertEquals(4, bounds.getY());
    }
}
