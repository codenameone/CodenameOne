package com.codename1.ui;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.test.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Rectangle2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphicsTest extends UITestBase {

    private Graphics graphics;
    private Object nativeGraphics;
    private TestCodenameOneImplementation testImplementation;

    @Override
    protected CodenameOneImplementation createImplementation() {
        testImplementation = new TestCodenameOneImplementation();
        return testImplementation;
    }

    @BeforeEach
    void setupGraphics() throws Exception {
        graphics = createGraphics();
        nativeGraphics = getNativeGraphics(graphics);
        testImplementation.resetTranslateTracking();
        testImplementation.resetShapeTracking();
        testImplementation.resetClipTracking();
        testImplementation.setTranslationSupported(false);
        testImplementation.setShapeSupported(false);
    }

    @Test
    void testTranslateWhenTranslationUnsupportedTracksOffsetsLocally() {
        graphics.translate(5, 7);
        assertFalse(testImplementation.wasTranslateInvoked());
        assertEquals(5, graphics.getTranslateX());
        assertEquals(7, graphics.getTranslateY());
    }

    @Test
    void testTranslateWhenTranslationSupportedDelegatesToImplementation() {
        testImplementation.setTranslationSupported(true);
        testImplementation.resetTranslateTracking();
        graphics.translate(3, 4);
        assertTrue(testImplementation.wasTranslateInvoked());
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
        assertEquals(0xFF0000, testImplementation.getColor(nativeGraphics));
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
        assertEquals(6, testImplementation.getClipX(nativeGraphics));
        assertEquals(8, testImplementation.getClipY(nativeGraphics));
        assertEquals(6, testImplementation.getClipWidth(nativeGraphics));
        assertEquals(7, testImplementation.getClipHeight(nativeGraphics));
    }

    @Test
    void testClipRectUsesTranslation() {
        graphics.translate(1, 2);
        graphics.clipRect(5, 6, 7, 8);
        assertEquals(6, testImplementation.getClipX(nativeGraphics));
        assertEquals(8, testImplementation.getClipY(nativeGraphics));
        assertEquals(7, testImplementation.getClipWidth(nativeGraphics));
        assertEquals(8, testImplementation.getClipHeight(nativeGraphics));
    }

    @Test
    void testGetClipAccountsForTranslation() {
        testImplementation.setClip(nativeGraphics, 15, 25, 30, 40);
        graphics.translate(5, 5);
        int[] clip = graphics.getClip();
        assertArrayEquals(new int[]{10, 20, 30, 40}, clip);
    }

    @Test
    void testSetClipWithTranslationWrapsShape() {
        graphics.translate(3, 3);
        Rectangle rectangle = new Rectangle(0, 0, 10, 10);
        graphics.setClip(rectangle);
        assertNotSame(rectangle, testImplementation.getLastClipShape());
    }

    @Test
    void testDrawShapeDelegatesWhenSupported() {
        testImplementation.setShapeSupported(true);
        Rectangle rectangle = new Rectangle(0, 0, 5, 5);
        Stroke stroke = new Stroke(2, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 1f);
        graphics.drawShape(rectangle, stroke);
        assertTrue(testImplementation.wasDrawShapeInvoked());
        assertSame(rectangle, testImplementation.getLastDrawShape());
        assertEquals(stroke, testImplementation.getLastDrawStroke());
    }

    @Test
    void testFillShapeWithPaintUsesCustomPaint() {
        testImplementation.setShapeSupported(true);
        graphics.setColor(new DummyPaint());
        Rectangle rectangle = new Rectangle(0, 0, 10, 20);
        graphics.fillShape(rectangle);
        assertTrue(((DummyPaint) graphics.getPaint()).wasPainted());
        assertFalse(testImplementation.wasFillShapeInvoked());
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
        Object nativeObject = testImplementation.getNativeGraphics();
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
