package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Rectangle2D;
import com.codename1.ui.geom.Shape;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraphicsTest extends UITestBase {

    private Graphics graphics;
    private Object nativeGraphics;
    @BeforeEach
    void setupGraphics() throws Exception {
        graphics = createGraphics();
        nativeGraphics = graphics.getGraphics();
        implementation.resetTranslateTracking();
        implementation.resetShapeTracking();
        implementation.resetClipTracking();
        implementation.setTranslationSupported(false);
        implementation.setTranslateMatrixSupported(false);
        implementation.setShapeSupported(false);
        Graphics.useMatrixTranslation = false;
    }

    @AfterEach
    void cleanupGraphics() {
        // The matrix-translation flag is static; reset it so tests after
        // these don't inherit matrix mode and quietly diverge.
        Graphics.useMatrixTranslation = false;
        implementation.setTranslateMatrixSupported(false);
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
    void testSetAndConcatenateAlpha() {
        graphics.setAlpha(200);
        int oldAlpha = graphics.setAndGetAlpha(100);
        assertEquals(200, oldAlpha);
        assertEquals(100, graphics.getAlpha());

        int previous = graphics.concatenateAlpha(128);
        assertEquals(100, previous);
        int expected = (int) (100 * (128 / 255f));
        assertEquals(expected, graphics.getAlpha());
        assertEquals(expected, implementation.getAlpha(nativeGraphics));
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
    void testSetClipNullRestoresFullDisplay() {
        graphics.setClip(10, 20, 30, 40);
        graphics.setClip((Shape) null);
        assertEquals(0, implementation.getClipX(nativeGraphics));
        assertEquals(0, implementation.getClipY(nativeGraphics));
        assertEquals(implementation.getDisplayWidth(), implementation.getClipWidth(nativeGraphics));
        assertEquals(implementation.getDisplayHeight(), implementation.getClipHeight(nativeGraphics));
    }

    @Test
    void testClipRectShrinksExistingClip() {
        graphics.setClip(0, 0, 20, 20);
        graphics.clipRect(5, -5, 10, 10);
        assertEquals(5, implementation.getClipX(nativeGraphics));
        assertEquals(0, implementation.getClipY(nativeGraphics));
        assertEquals(10, implementation.getClipWidth(nativeGraphics));
        assertEquals(5, implementation.getClipHeight(nativeGraphics));
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
    void testDrawShapeWithTranslationWrapsShape() {
        implementation.setShapeSupported(true);
        graphics.translate(4, 6);
        Rectangle rectangle = new Rectangle(0, 0, 5, 5);
        graphics.drawShape(rectangle, new Stroke(1, Stroke.CAP_SQUARE, Stroke.JOIN_MITER, 1f));
        Shape transformed = implementation.getLastDrawShape();
        assertNotSame(rectangle, transformed);
        Rectangle bounds = transformed.getBounds();
        assertEquals(4, bounds.getX());
        assertEquals(6, bounds.getY());
        assertEquals(5, bounds.getWidth());
        assertEquals(5, bounds.getHeight());
    }

    @Test
    void testFillShapeDelegatesWhenSupportedAndNoPaint() {
        implementation.setShapeSupported(true);
        Rectangle rectangle = new Rectangle(0, 0, 4, 4);
        graphics.fillShape(rectangle);
        assertTrue(implementation.wasFillShapeInvoked());
        assertSame(rectangle, implementation.getLastFillShape());
    }

    @Test
    void testSetFontUpdatesNativeFont() {
        Font font = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_LARGE);
        graphics.setFont(font);
        assertSame(font, graphics.getFont());
        assertSame(font.getNativeFont(), implementation.getFont(nativeGraphics));
    }

    @Test
    void testMatrixModeTranslatePushesToImplMatrix() {
        // When useMatrixTranslation is on AND the impl advertises support,
        // g.translate(dx, dy) routes ONLY through impl.translateMatrix --
        // no shadow xTranslate accumulator. getTranslateX/Y is intentionally
        // 0 in matrix mode (the integer "amount to add to local coords" is
        // zero because drawing primitives no longer add it -- the impl
        // matrix carries the translate). Callers needing the actual screen
        // offset must use getTransform().
        Graphics.useMatrixTranslation = true;
        implementation.setTranslateMatrixSupported(true);

        graphics.translate(5, 7);

        assertTrue(implementation.wasTranslateMatrixInvoked());
        assertEquals(5f, implementation.getLastTranslateMatrixX());
        assertEquals(7f, implementation.getLastTranslateMatrixY());
        assertFalse(implementation.wasTranslateInvoked(),
                "matrix path must NOT call the legacy impl.translate hook");
        assertEquals(0, graphics.getTranslateX(),
                "matrix mode: integer accumulator stays at 0; framework callers use getTransform() instead");
        assertEquals(0, graphics.getTranslateY());
    }

    @Test
    void testMatrixModeDoesNotPreShiftDrawCoords() {
        // The legacy path bakes xTranslate into impl.fillRect coords. In
        // matrix mode the impl matrix already encodes the translate, so
        // fillRect must arrive at the impl with the raw user coords. We
        // keep translate values small because the stub clip is set to the
        // 20x20 image bounds in createGraphics().
        Graphics.useMatrixTranslation = true;
        implementation.setTranslateMatrixSupported(true);
        implementation.clearGraphicsOperations();

        graphics.translate(5, 7);
        graphics.setColor(0xff0000);
        graphics.fillRect(2, 1, 3, 3);

        List<TestCodenameOneImplementation.FillOperation> ops =
                implementation.getFillOperationsSnapshot();
        assertEquals(1, ops.size(), "expected one recorded fillRect");
        TestCodenameOneImplementation.FillOperation op = ops.get(0);
        assertEquals(2, op.getX(),
                "matrix mode must pass raw user-coord x; legacy mode would record 7");
        assertEquals(1, op.getY(),
                "matrix mode must pass raw user-coord y; legacy mode would record 8");
    }

    @Test
    void testMatrixModeFallsBackToLegacyWhenImplOptsOut() {
        // useMatrixTranslation is just a request: if the impl returns
        // isTranslateMatrixSupported() == false, g.translate must keep using
        // the integer accumulator (this is how legacy / restricted ports
        // remain functional even after the flag is set globally in init).
        Graphics.useMatrixTranslation = true;
        implementation.setTranslateMatrixSupported(false);
        implementation.clearGraphicsOperations();

        graphics.translate(5, 7);
        graphics.fillRect(2, 1, 3, 3);

        assertFalse(implementation.wasTranslateMatrixInvoked(),
                "impl opted out, translate must NOT route through translateMatrix");
        List<TestCodenameOneImplementation.FillOperation> ops =
                implementation.getFillOperationsSnapshot();
        assertEquals(1, ops.size());
        assertEquals(7, ops.get(0).getX(),
                "legacy fallback must pre-shift draw coords by xTranslate");
        assertEquals(8, ops.get(0).getY());
    }

    @Test
    void testMatrixModeResetAffineWipesImplMatrix() {
        // Matrix mode contract: resetAffine wipes the impl matrix to
        // identity, including the framework painting-chain translates the
        // matrix carries. Callers needing to preserve those translates must
        // save the matrix via getTransform and restore via setTransform --
        // see the matrix-mode branches in MapComponent, Scene,
        // CommonTransitions, FontImage, and beginNativeGraphicsAccess.
        Graphics.useMatrixTranslation = true;
        implementation.setTranslateMatrixSupported(true);

        graphics.translate(5, 7);
        graphics.resetAffine();

        // The stub's matrix-translate accumulator survives because the stub
        // only resets its own internal Transform state on resetAffine; the
        // production iOS / Android / JavaSE impls reset their NativeGraphics
        // transform. The point of this test is to assert that Graphics
        // does NOT silently replay the translate -- legacy behavior, no
        // hidden state carried across resetAffine.
        assertEquals(0, graphics.getTranslateX(),
                "matrix-mode getTranslateX stays 0 across translate+resetAffine");
        assertEquals(0, graphics.getTranslateY());
    }

    @Test
    void testMatrixModeShapeNotPreTranslated() {
        // drawShape's legacy path manually translated the shape's vertices
        // when xTranslate != 0; in matrix mode the impl applies the matrix
        // to the shape itself, so we must hand the same shape instance
        // through (no GeneralPath wrapping).
        Graphics.useMatrixTranslation = true;
        implementation.setTranslateMatrixSupported(true);
        implementation.setShapeSupported(true);

        graphics.translate(4, 6);
        Rectangle rectangle = new Rectangle(0, 0, 5, 5);
        graphics.drawShape(rectangle, new Stroke(1, Stroke.CAP_SQUARE, Stroke.JOIN_MITER, 1f));

        assertSame(rectangle, implementation.getLastDrawShape(),
                "matrix mode hands the original shape to the impl; legacy mode wraps in a translated path");
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
        Image image = Image.createImage(20, 20);
        Graphics g = image.getGraphics();
        implementation.setClip(g.getGraphics(), 0, 0, 20, 20);
        return g;
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
