package com.codename1.ui;

import com.codename1.test.UITestBase;
import com.codename1.ui.Paint;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GraphicsTest extends UITestBase {

    private Object nativeGraphics;

    @BeforeEach
    @Override
    protected void setUpDisplay() throws Exception {
        super.setUpDisplay();
        nativeGraphics = new Object();
        when(implementation.isTranslationSupported()).thenReturn(false);
        when(implementation.getClipX(any())).thenReturn(0);
        when(implementation.getClipY(any())).thenReturn(0);
        when(implementation.getClipWidth(any())).thenReturn(100);
        when(implementation.getClipHeight(any())).thenReturn(100);
        when(implementation.isShapeSupported(any())).thenReturn(true);
        when(implementation.isShapeClipSupported(any())).thenReturn(true);
    }

    @AfterEach
    @Override
    protected void tearDownDisplay() throws Exception {
        flushSerialCalls();
        super.tearDownDisplay();
    }

    private Graphics newGraphics() throws Exception {
        Constructor<Graphics> constructor = Graphics.class.getDeclaredConstructor(Object.class);
        constructor.setAccessible(true);
        return constructor.newInstance(nativeGraphics);
    }

    @Test
    void testTranslateDelegatesWhenSupported() throws Exception {
        when(implementation.isTranslationSupported()).thenReturn(true);
        when(implementation.getTranslateX(nativeGraphics)).thenReturn(3);
        when(implementation.getTranslateY(nativeGraphics)).thenReturn(4);
        Graphics graphics = newGraphics();
        graphics.translate(5, 6);
        verify(implementation).translate(nativeGraphics, 5, 6);
        assertEquals(3, graphics.getTranslateX());
        assertEquals(4, graphics.getTranslateY());
    }

    @Test
    void testTranslateWithoutSupportAccumulates() throws Exception {
        when(implementation.isTranslationSupported()).thenReturn(false);
        Graphics graphics = newGraphics();
        graphics.translate(2, 3);
        graphics.translate(1, -1);
        verify(implementation, never()).translate(any(), anyInt(), anyInt());
        assertEquals(3, graphics.getTranslateX());
        assertEquals(2, graphics.getTranslateY());
    }

    @Test
    void testSetColorAndSetAndGetColor() throws Exception {
        Graphics graphics = newGraphics();
        graphics.setColor(0x123456);
        verify(implementation).setColor(nativeGraphics, 0x123456);
        int previous = graphics.setAndGetColor(0xABCDEF);
        assertEquals(0x123456, previous);
        verify(implementation).setColor(nativeGraphics, 0xABCDEF & 0xFFFFFF);
    }

    @Test
    void testClipRectUsesTranslationOffsets() throws Exception {
        Graphics graphics = newGraphics();
        graphics.translate(5, 7);
        graphics.clipRect(1, 2, 3, 4);
        verify(implementation).clipRect(nativeGraphics, 6, 9, 3, 4);
    }

    @Test
    void testSetClipShapeAppliesTranslation() throws Exception {
        Graphics graphics = newGraphics();
        graphics.translate(10, 5);
        Rectangle shape = new Rectangle(0, 0, 10, 10);
        graphics.setClip(shape);
        ArgumentCaptor<Shape> shapeCaptor = ArgumentCaptor.forClass(Shape.class);
        verify(implementation).setClip(eq(nativeGraphics), shapeCaptor.capture());
        Rectangle bounds = shapeCaptor.getValue().getBounds();
        assertEquals(10, bounds.getX());
        assertEquals(5, bounds.getY());
    }

    @Test
    void testDrawShapeTranslatesWhenNeeded() throws Exception {
        Graphics graphics = newGraphics();
        graphics.translate(3, 4);
        Rectangle shape = new Rectangle(0, 0, 5, 5);
        Stroke stroke = new Stroke(1, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1f);
        graphics.drawShape(shape, stroke);
        ArgumentCaptor<Shape> shapeCaptor = ArgumentCaptor.forClass(Shape.class);
        verify(implementation).drawShape(eq(nativeGraphics), shapeCaptor.capture(), eq(stroke));
        Rectangle bounds = shapeCaptor.getValue().getBounds();
        assertEquals(3, bounds.getX());
        assertEquals(4, bounds.getY());
    }

    @Test
    void testFillShapeWithPaintUsesCustomPaint() throws Exception {
        Graphics graphics = newGraphics();
        Paint paint = mock(Paint.class);
        graphics.setColor(paint);
        Rectangle shape = new Rectangle(0, 0, 10, 10);
        graphics.fillShape(shape);
        verify(paint).paint(same(graphics), eq(0.0), eq(0.0), eq(10.0), eq(10.0));
        verify(implementation).setClip(eq(nativeGraphics), any(Shape.class));
        verify(implementation).clipRect(nativeGraphics, 0, 0, 100, 100);
        verify(implementation).setClip(nativeGraphics, 0, 0, 100, 100);
    }

    @Test
    void testPushAndPopClipDelegates() throws Exception {
        Graphics graphics = newGraphics();
        graphics.pushClip();
        graphics.popClip();
        verify(implementation).pushClip(nativeGraphics);
        verify(implementation).popClip(nativeGraphics);
    }

    @Test
    void testDrawLineAppliesTranslation() throws Exception {
        Graphics graphics = newGraphics();
        graphics.translate(2, 3);
        graphics.drawLine(1, 1, 4, 4);
        verify(implementation).drawLine(nativeGraphics, 3, 4, 6, 7);
    }

    @Test
    void testGetClipReflectsCurrentTranslation() throws Exception {
        when(implementation.getClipX(nativeGraphics)).thenReturn(50);
        when(implementation.getClipY(nativeGraphics)).thenReturn(60);
        Graphics graphics = newGraphics();
        graphics.translate(5, 7);
        assertEquals(45, graphics.getClipX());
        assertEquals(53, graphics.getClipY());
    }

    @Test
    void testSetAlphaDelegatesToImplementation() throws Exception {
        Graphics graphics = newGraphics();
        graphics.setAlpha(128);
        verify(implementation).setAlpha(nativeGraphics, 128);
    }
}
