package com.codename1.ui;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.Paint;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GraphicsTest {
    private CodenameOneImplementation originalImpl;
    private CodenameOneImplementation impl;
    private Constructor<Graphics> constructor;

    @BeforeEach
    void setUp() throws Exception {
        Field implField = Display.class.getDeclaredField("impl");
        implField.setAccessible(true);
        originalImpl = (CodenameOneImplementation) implField.get(null);
        impl = mock(CodenameOneImplementation.class, withSettings().lenient());
        when(impl.isTranslationSupported()).thenReturn(false);
        when(impl.isShapeSupported()).thenReturn(true);
        when(impl.getClipX(any())).thenReturn(0);
        when(impl.getClipY(any())).thenReturn(0);
        when(impl.getClipWidth(any())).thenReturn(100);
        when(impl.getClipHeight(any())).thenReturn(100);
        implField.set(null, impl);

        constructor = Graphics.class.getDeclaredConstructor(Object.class);
        constructor.setAccessible(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        Field implField = Display.class.getDeclaredField("impl");
        implField.setAccessible(true);
        implField.set(null, originalImpl);
    }

    private Graphics newGraphics() throws Exception {
        return constructor.newInstance(new Object());
    }

    @Test
    void translateWithoutNativeSupportAdjustsLocalState() throws Exception {
        Graphics graphics = newGraphics();
        graphics.translate(5, 6);
        assertEquals(5, graphics.getTranslateX());
        assertEquals(6, graphics.getTranslateY());

        graphics.drawLine(1, 2, 3, 4);
        verify(impl).drawLine(eq(graphics.getGraphics()), eq(6), eq(8), eq(8), eq(10));
    }

    @Test
    void translateWithNativeSupportDelegates() throws Exception {
        when(impl.isTranslationSupported()).thenReturn(true);
        when(impl.getTranslateX(any())).thenReturn(11);
        when(impl.getTranslateY(any())).thenReturn(7);

        Graphics graphics = newGraphics();
        graphics.translate(2, 3);
        verify(impl).translate(eq(graphics.getGraphics()), eq(2), eq(3));
        assertEquals(11, graphics.getTranslateX());
        assertEquals(7, graphics.getTranslateY());
    }

    @Test
    void setColorResetsPaintAndReturnsPreviousColor() throws Exception {
        Graphics graphics = newGraphics();
        Paint paint = mock(Paint.class);
        graphics.setColor(paint);
        assertSame(paint, graphics.getPaint());

        graphics.setColor(0x123456);
        assertEquals(0x123456, graphics.getColor());
        assertNull(graphics.getPaint());
        verify(impl).setColor(eq(graphics.getGraphics()), eq(0x123456));

        int previous = graphics.setAndGetColor(0xABCDEF);
        assertEquals(0x123456, previous);
        assertEquals(0xABCDEF, graphics.getColor());
    }

    @Test
    void fillShapeWithPaintUsesCustomPainter() throws Exception {
        Graphics graphics = newGraphics();
        GeneralPath shape = new GeneralPath();
        shape.setRect(new Rectangle(0, 0, 10, 20), null);

        Paint paint = mock(Paint.class);
        graphics.setColor(paint);
        graphics.fillShape(shape);

        verify(impl).setClip(eq(graphics.getGraphics()), same(shape));
        verify(impl).clipRect(eq(graphics.getGraphics()), eq(0), eq(0), eq(100), eq(100));
        verify(impl).setClip(eq(graphics.getGraphics()), eq(0), eq(0), eq(100), eq(100));
        ArgumentCaptor<Double> x = ArgumentCaptor.forClass(Double.class);
        verify(paint).paint(eq(graphics), x.capture(), x.capture(), x.capture(), x.capture());
        assertEquals(0.0, x.getAllValues().get(0), 1e-6);
        assertEquals(0.0, x.getAllValues().get(1), 1e-6);
        assertEquals(10.0, x.getAllValues().get(2), 1e-6);
        assertEquals(20.0, x.getAllValues().get(3), 1e-6);
    }

    @Test
    void pushAndPopClipDelegateToImplementation() throws Exception {
        Graphics graphics = newGraphics();
        graphics.pushClip();
        graphics.popClip();
        verify(impl).pushClip(eq(graphics.getGraphics()));
        verify(impl).popClip(eq(graphics.getGraphics()));
    }
}
