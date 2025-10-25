package com.codename1.ui;

import com.codename1.test.UITestBase;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Rectangle2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GraphicsTest extends UITestBase {

    private Graphics graphics;
    private Object nativeGraphics;

    @BeforeEach
    void setupGraphics() throws Exception {
        graphics = createGraphics();
        nativeGraphics = getNativeGraphics(graphics);
    }

    @Test
    void testTranslateWhenTranslationUnsupportedTracksOffsetsLocally() {
        when(implementation.isTranslationSupported()).thenReturn(false);
        graphics.translate(5, 7);
        verify(implementation, never()).translate(any(), anyInt(), anyInt());
        assertEquals(5, graphics.getTranslateX());
        assertEquals(7, graphics.getTranslateY());
    }

    @Test
    void testTranslateWhenTranslationSupportedDelegatesToImplementation() {
        when(implementation.isTranslationSupported()).thenReturn(true);
        when(implementation.getTranslateX(nativeGraphics)).thenReturn(5);
        when(implementation.getTranslateY(nativeGraphics)).thenReturn(6);
        graphics.translate(3, 4);
        verify(implementation).translate(nativeGraphics, 3, 4);
        assertEquals(5, graphics.getTranslateX());
        assertEquals(6, graphics.getTranslateY());
    }

    @Test
    void testSetColorClearsPaintAndMasksAlpha() {
        DummyPaint paint = new DummyPaint();
        graphics.setColor(paint);
        assertSame(paint, graphics.getPaint());
        graphics.setColor(0x80FF0000);
        assertNull(graphics.getPaint());
        assertEquals(0xFF0000, graphics.getColor());
        verify(implementation).setColor(nativeGraphics, 0xFF0000);
    }

    @Test
    void testSetAndGetColorReturnsPreviousValue() {
        graphics.setColor(0x00112233);
        int previous = graphics.setAndGetColor(0x00ABCDEF);
        assertEquals(0x112233, previous);
        assertEquals(0xABCDEF, graphics.getColor());
    }

    @Test
    void testSetClipRectangleAppliesTranslation() {
        when(implementation.isTranslationSupported()).thenReturn(false);
        graphics.translate(2, 3);
        graphics.setClip(4, 5, 6, 7);
        verify(implementation).setClip(nativeGraphics, 6, 8, 6, 7);
    }

    @Test
    void testClipRectUsesTranslation() {
        when(implementation.isTranslationSupported()).thenReturn(false);
        graphics.translate(1, 2);
        graphics.clipRect(5, 6, 7, 8);
        verify(implementation).clipRect(nativeGraphics, 6, 8, 7, 8);
    }

    @Test
    void testGetClipAccountsForTranslation() {
        when(implementation.isTranslationSupported()).thenReturn(false);
        when(implementation.getClipX(nativeGraphics)).thenReturn(15);
        when(implementation.getClipY(nativeGraphics)).thenReturn(25);
        when(implementation.getClipWidth(nativeGraphics)).thenReturn(30);
        when(implementation.getClipHeight(nativeGraphics)).thenReturn(40);
        graphics.translate(5, 5);
        int[] clip = graphics.getClip();
        assertArrayEquals(new int[]{10, 20, 30, 40}, clip);
    }

    @Test
    void testSetClipWithTranslationWrapsShape() {
        when(implementation.isTranslationSupported()).thenReturn(false);
        graphics.translate(3, 3);
        Rectangle rectangle = new Rectangle(0, 0, 10, 10);
        ArgumentCaptor<com.codename1.ui.geom.Shape> captor = ArgumentCaptor.forClass(com.codename1.ui.geom.Shape.class);
        graphics.setClip(rectangle);
        verify(implementation).setClip(eq(nativeGraphics), captor.capture());
        assertNotSame(rectangle, captor.getValue());
    }

    @Test
    void testDrawShapeDelegatesWhenSupported() {
        when(implementation.isShapeSupported(nativeGraphics)).thenReturn(true);
        Rectangle rectangle = new Rectangle(0, 0, 5, 5);
        Stroke stroke = new Stroke(2, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 1f);
        graphics.drawShape(rectangle, stroke);
        verify(implementation).drawShape(nativeGraphics, rectangle, stroke);
    }

    @Test
    void testFillShapeWithPaintUsesCustomPaint() {
        when(implementation.isShapeSupported(nativeGraphics)).thenReturn(true);
        graphics.setColor(new DummyPaint());
        Rectangle rectangle = new Rectangle(0, 0, 10, 20);
        graphics.fillShape(rectangle);
        assertTrue(((DummyPaint) graphics.getPaint()).wasPainted());
        verify(implementation, never()).fillShape(any(), any());
    }

    @Test
    void testLighterAndDarkerColorAdjustments() {
        graphics.setColor(0x00101010);
        graphics.lighterColor(0x20);
        assertEquals(0x303030, graphics.getColor());
        graphics.darkerColor(0x10);
        assertEquals(0x202020, graphics.getColor());
    }

    private Graphics createGraphics() throws Exception {
        java.lang.reflect.Constructor<Graphics> constructor = Graphics.class.getDeclaredConstructor(Object.class);
        constructor.setAccessible(true);
        Graphics instance = constructor.newInstance(new Object());
        return instance;
    }

    private Object getNativeGraphics(Graphics g) throws Exception {
        java.lang.reflect.Field field = Graphics.class.getDeclaredField("nativeGraphics");
        field.setAccessible(true);
        return field.get(g);
    }

    private static class DummyPaint implements Paint {
        private boolean painted;

        @Override
        public void paint(Graphics g, Rectangle2D bounds) {
            painted = true;
        }

        @Override
        public void paint(Graphics g, double x, double y, double w, double h) {
            painted = true;
        }

        boolean wasPainted() {
            return painted;
        }
    }
}
