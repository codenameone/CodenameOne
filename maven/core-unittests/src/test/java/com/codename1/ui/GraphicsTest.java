package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Rectangle2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphicsTest extends UITestBase {

    private Graphics graphics;
    private Object nativeGraphics;

    @BeforeEach
    void setupGraphics() throws Exception {
        graphics = createGraphics();
        nativeGraphics = getNativeGraphics(graphics);
        implementation.resetTranslateTracking();
        implementation.resetShapeTracking();
        implementation.resetClipTracking();
        implementation.setTranslationSupported(false);
        implementation.setShapeSupported(false);
    }

    @FormTest
    void testTranslateWhenTranslationUnsupportedTracksOffsetsLocally() {
        graphics.translate(5, 7);
        assertFalse(implementation.wasTranslateInvoked());
        assertEquals(5, graphics.getTranslateX());
        assertEquals(7, graphics.getTranslateY());
    }

    @Test
    void testTranslateWhenTranslationSupportedDelegatesToImplementation() {
        implementation.setTranslationSupported(true);
        implementation.resetTranslateTracking();
        graphics.translate(3, 4);
        assertTrue(implementation.wasTranslateInvoked());
        assertEquals(3, graphics.getTranslateX());
        assertEquals(4, graphics.getTranslateY());
    }

    @Test
    void testSetColorClearsPaintAndMasksAlpha() {
        DummyPaint paint = new DummyPaint();
        graphics.setColor(paint);
        assertSame(paint, graphics.getPaint());
        graphics.setColor(0x80FF0000);
        assertNull(graphics.getPaint());
        assertEquals(0xFF0000, graphics.getColor());
        assertEquals(0xFF0000, implementation.getColor(nativeGraphics));
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
        graphics.translate(2, 3);
        graphics.setClip(4, 5, 6, 7);
        assertEquals(6, implementation.getClipX(nativeGraphics));
        assertEquals(8, implementation.getClipY(nativeGraphics));
        assertEquals(6, implementation.getClipWidth(nativeGraphics));
        assertEquals(7, implementation.getClipHeight(nativeGraphics));
    }

    @FormTest
    void testClipRectUsesTranslation() {
        graphics.translate(1, 2);
        graphics.clipRect(5, 6, 7, 8);
        assertEquals(6, implementation.getClipX(nativeGraphics));
        assertEquals(8, implementation.getClipY(nativeGraphics));
        assertEquals(7, implementation.getClipWidth(nativeGraphics));
        assertEquals(8, implementation.getClipHeight(nativeGraphics));
    }

    @FormTest
    void testGetClipAccountsForTranslation() {
        implementation.setClip(nativeGraphics, 15, 25, 30, 40);
        graphics.translate(5, 5);
        int[] clip = graphics.getClip();
        assertArrayEquals(new int[]{10, 20, 30, 40}, clip);
    }

    @FormTest
    void testSetClipWithTranslationWrapsShape() {
        graphics.translate(3, 3);
        Rectangle rectangle = new Rectangle(0, 0, 10, 10);
        graphics.setClip(rectangle);
        assertNotSame(rectangle, implementation.getLastClipShape());
    }

    @Test
    void testDrawShapeDelegatesWhenSupported() {
        implementation.setShapeSupported(true);
        Rectangle rectangle = new Rectangle(0, 0, 5, 5);
        Stroke stroke = new Stroke(2, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 1f);
        graphics.drawShape(rectangle, stroke);
        assertTrue(implementation.wasDrawShapeInvoked());
        assertSame(rectangle, implementation.getLastDrawShape());
        assertEquals(stroke, implementation.getLastDrawStroke());
    }

    @Test
    void testFillShapeWithPaintUsesCustomPaint() {
        implementation.setShapeSupported(true);
        graphics.setColor(new DummyPaint());
        Rectangle rectangle = new Rectangle(0, 0, 10, 20);
        graphics.fillShape(rectangle);
        assertTrue(((DummyPaint) graphics.getPaint()).wasPainted());
        assertFalse(implementation.wasFillShapeInvoked());
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
        Object nativeObject = implementation.getNativeGraphics();
        return constructor.newInstance(nativeObject);
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
