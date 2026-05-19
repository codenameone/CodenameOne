package com.codename1.designer.css;

import com.codename1.io.Util;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.ConicGradient;
import com.codename1.ui.Gradient;
import com.codename1.ui.LinearGradient;
import com.codename1.ui.RadialGradient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.css.sac.LexicalUnit;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// End-to-end tests for the build-time CSS gradient parser. Loads a CSS
/// snippet through `CSSTheme.load` (using the Flute SAC parser the same
/// way the maven plugin does at build time), then inspects the resulting
/// `Gradient` descriptor on each Element.
class CSSThemeGradientTest {

    @BeforeAll
    static void initCn1Impl() {
        // CSSTheme.load -> Util.readToString -> Util.copy needs
        // Util.getImplementation() to be non-null.
        Util.setImplementation(new TestCodenameOneImplementation(true));
    }

    private CSSTheme loadCss(String css) throws Exception {
        File f = File.createTempFile("cn1-test-", ".css");
        f.deleteOnExit();
        FileWriter w = new FileWriter(f);
        try {
            w.write(css);
        } finally {
            w.close();
        }
        return CSSTheme.load(f.toURI().toURL());
    }

    private Map<String, LexicalUnit> unselected(CSSTheme t, String uiid) {
        CSSTheme.Element el = t.elements.get(uiid);
        assertNotNull(el, "Missing UIID: " + uiid);
        return el.getUnselected().getFlattenedStyle();
    }

    @Test
    void linearGradientMultiStop() throws Exception {
        CSSTheme t = loadCss("Foo { background: linear-gradient(45deg, #ff0000 0%, #00ff00 50%, #0000ff 100%); }");
        Gradient g = t.elements.get("Foo").getThemeGradient(unselected(t, "Foo"));
        assertNotNull(g);
        assertEquals(Gradient.KIND_LINEAR, g.getKind());
        LinearGradient lg = (LinearGradient) g;
        assertEquals(45f, lg.getAngleDegrees(), 0.001f);
        assertEquals(3, lg.getColors().length);
        assertEquals(0xffff0000, lg.getColors()[0]);
        assertEquals(0xff00ff00, lg.getColors()[1]);
        assertEquals(0xff0000ff, lg.getColors()[2]);
        assertEquals(0f, lg.getPositions()[0], 1e-4f);
        assertEquals(0.5f, lg.getPositions()[1], 1e-4f);
        assertEquals(1f, lg.getPositions()[2], 1e-4f);
    }

    @Test
    void linearGradientToSide() throws Exception {
        CSSTheme t = loadCss("Foo { background: linear-gradient(to bottom right, red, blue); }");
        LinearGradient lg = (LinearGradient) t.elements.get("Foo")
                .getThemeGradient(unselected(t, "Foo"));
        assertEquals(135f, lg.getAngleDegrees(), 0.001f);
    }

    @Test
    void linearGradientMismatchedAlphaAccepted() throws Exception {
        CSSTheme t = loadCss("Foo { background: linear-gradient(90deg, rgba(255,0,0,0.6), blue); }");
        LinearGradient lg = (LinearGradient) t.elements.get("Foo")
                .getThemeGradient(unselected(t, "Foo"));
        assertEquals(2, lg.getColors().length);
        // Alpha of first stop should be ~ 0.6 * 255.
        int a0 = (lg.getColors()[0] >>> 24) & 0xff;
        assertTrue(a0 >= 150 && a0 <= 160, "alpha was " + a0);
    }

    @Test
    void radialGradientCircleFarthestCorner() throws Exception {
        CSSTheme t = loadCss(
                "Foo { background: radial-gradient(circle farthest-corner at 30% 70%, #ffffff, #001 70%); }");
        RadialGradient rg = (RadialGradient) t.elements.get("Foo")
                .getThemeGradient(unselected(t, "Foo"));
        assertNotNull(rg);
        assertEquals(RadialGradient.SHAPE_CIRCLE, rg.getShape());
        assertEquals(RadialGradient.EXTENT_FARTHEST_CORNER, rg.getExtent());
        assertEquals(0.30f, rg.getRelativeCenterX(), 1e-4f);
        assertEquals(0.70f, rg.getRelativeCenterY(), 1e-4f);
    }

    @Test
    void radialGradientEllipseClosestSide() throws Exception {
        CSSTheme t = loadCss(
                "Foo { background: radial-gradient(ellipse closest-side at 50% 50%, #ffeeff, #002233); }");
        RadialGradient rg = (RadialGradient) t.elements.get("Foo")
                .getThemeGradient(unselected(t, "Foo"));
        assertEquals(RadialGradient.SHAPE_ELLIPSE, rg.getShape());
        assertEquals(RadialGradient.EXTENT_CLOSEST_SIDE, rg.getExtent());
    }

    @Test
    void conicGradientFromAngleAndCenter() throws Exception {
        CSSTheme t = loadCss(
                "Foo { background: conic-gradient(from 90deg at 50% 50%, red, yellow, green, blue, red); }");
        ConicGradient cg = (ConicGradient) t.elements.get("Foo")
                .getThemeGradient(unselected(t, "Foo"));
        assertEquals(90f, cg.getFromAngleDegrees(), 0.001f);
        assertEquals(0.5f, cg.getRelativeCenterX(), 1e-4f);
        assertEquals(0.5f, cg.getRelativeCenterY(), 1e-4f);
        assertEquals(5, cg.getColors().length);
    }

    @Test
    void repeatingLinearGradientCycleRepeat() throws Exception {
        CSSTheme t = loadCss(
                "Foo { background: repeating-linear-gradient(45deg, #eeeeee 0%, #cc3344 10%); }");
        Gradient g = t.elements.get("Foo").getThemeGradient(unselected(t, "Foo"));
        assertEquals(Gradient.KIND_LINEAR, g.getKind());
        assertEquals(Gradient.CYCLE_REPEAT, g.getCycleMethod());
    }

    @Test
    void repeatingRadialGradientCycleRepeat() throws Exception {
        CSSTheme t = loadCss(
                "Foo { background: repeating-radial-gradient(circle at center, #ffffff 0%, #cc3344 16%); }");
        Gradient g = t.elements.get("Foo").getThemeGradient(unselected(t, "Foo"));
        assertEquals(Gradient.KIND_RADIAL, g.getKind());
        assertEquals(Gradient.CYCLE_REPEAT, g.getCycleMethod());
    }

    @Test
    void autoDistributesUnsetStopPositions() throws Exception {
        CSSTheme t = loadCss("Foo { background: linear-gradient(45deg, red, green, blue, black); }");
        LinearGradient lg = (LinearGradient) t.elements.get("Foo")
                .getThemeGradient(unselected(t, "Foo"));
        float[] pos = lg.getPositions();
        assertEquals(4, pos.length);
        assertEquals(0f, pos[0], 1e-4f);
        assertEquals(1f / 3f, pos[1], 1e-3f);
        assertEquals(2f / 3f, pos[2], 1e-3f);
        assertEquals(1f, pos[3], 1e-4f);
    }

    @Test
    void unselectedStateGradientApplied() throws Exception {
        // Verify the unselected state of a UIID picks up its declared gradient.
        CSSTheme t = loadCss("Foo { background: linear-gradient(45deg, red, blue); }");
        CSSTheme.Element el = t.elements.get("Foo");
        LinearGradient unsel = (LinearGradient) el.getThemeGradient(el.getUnselected().getFlattenedStyle());
        assertNotNull(unsel);
        assertEquals(45f, unsel.getAngleDegrees(), 0.001f);
    }

    @Test
    void backgroundColorWithoutGradientReturnsNull() throws Exception {
        CSSTheme t = loadCss("Foo { background: #ff0000; }");
        assertNull(t.elements.get("Foo").getThemeGradient(unselected(t, "Foo")));
    }

    @Test
    void filterBlurStoredOnStyle() throws Exception {
        CSSTheme t = loadCss("Foo { filter: blur(4px); }");
        CSSTheme.Element el = t.elements.get("Foo");
        Map<String, LexicalUnit> style = el.getUnselected().getFlattenedStyle();
        assertEquals(4f, el.getFilterBlurRadius(style), 1e-4f);
        // No color filters in the chain - matrix should be null.
        assertNull(el.getFilterColorMatrix(style));
    }

    @Test
    void backdropFilterBlurStoredOnStyle() throws Exception {
        CSSTheme t = loadCss("Foo { backdrop-filter: blur(12px); }");
        CSSTheme.Element el = t.elements.get("Foo");
        Map<String, LexicalUnit> style = el.getUnselected().getFlattenedStyle();
        assertEquals(12f, el.getBackdropFilterBlurRadius(style), 1e-4f);
    }

    @Test
    void filterGrayscaleReducesToRec709Diagonal() throws Exception {
        CSSTheme t = loadCss("Foo { filter: grayscale(1); }");
        CSSTheme.Element el = t.elements.get("Foo");
        Map<String, LexicalUnit> style = el.getUnselected().getFlattenedStyle();
        float[] m = el.getFilterColorMatrix(style);
        assertNotNull(m);
        assertEquals(20, m.length);
        // Diagonal R/G/B cells == Rec 709 luma weights.
        assertEquals(0.2126f, m[0], 1e-3f);
        assertEquals(0.7152f, m[6], 1e-3f);
        assertEquals(0.0722f, m[12], 1e-3f);
    }

    @Test
    void filterChainComposesNonTrivialMatrix() throws Exception {
        CSSTheme t = loadCss("Foo { filter: brightness(1.2) contrast(0.9) saturate(1.3); }");
        CSSTheme.Element el = t.elements.get("Foo");
        Map<String, LexicalUnit> style = el.getUnselected().getFlattenedStyle();
        float[] m = el.getFilterColorMatrix(style);
        assertNotNull(m);
        assertEquals(20, m.length);
        // Not the identity.
        assertTrue(Math.abs(m[0] - 1f) > 1e-3f || Math.abs(m[1]) > 1e-3f);
    }

    @Test
    void filterBlurAndColorMatrixCoexist() throws Exception {
        CSSTheme t = loadCss("Foo { filter: blur(3px) grayscale(1); }");
        CSSTheme.Element el = t.elements.get("Foo");
        Map<String, LexicalUnit> style = el.getUnselected().getFlattenedStyle();
        assertEquals(3f, el.getFilterBlurRadius(style), 1e-4f);
        assertNotNull(el.getFilterColorMatrix(style));
    }

    @Test
    void filterNoneIsNoOp() throws Exception {
        CSSTheme t = loadCss("Foo { color: red; }");
        CSSTheme.Element el = t.elements.get("Foo");
        Map<String, LexicalUnit> style = el.getUnselected().getFlattenedStyle();
        assertEquals(0f, el.getFilterBlurRadius(style), 1e-4f);
        assertNull(el.getFilterColorMatrix(style));
    }

    @Test
    void invertOneInvertsRgb() throws Exception {
        CSSTheme t = loadCss("Foo { filter: invert(1); }");
        CSSTheme.Element el = t.elements.get("Foo");
        Map<String, LexicalUnit> style = el.getUnselected().getFlattenedStyle();
        float[] m = el.getFilterColorMatrix(style);
        assertNotNull(m);
        // R-row diagonal = 1 - 2*1 = -1, offset column = 255.
        assertEquals(-1f, m[0], 1e-3f);
        assertEquals(255f, m[4], 1e-3f);
    }

    @Test
    void brightness1IsIdentityCollapsedToNull() throws Exception {
        // brightness(1) alone is the identity and should collapse to null.
        CSSTheme t = loadCss("Foo { filter: brightness(1); }");
        CSSTheme.Element el = t.elements.get("Foo");
        Map<String, LexicalUnit> style = el.getUnselected().getFlattenedStyle();
        // Either null OR all 20 floats matching identity. Both representations
        // are correct for "no color transform"; the optimizer-aware path is
        // null, but if the compiler decides to keep the matrix we accept it
        // as long as it's the identity.
        float[] m = el.getFilterColorMatrix(style);
        if (m != null) {
            float[] id = new float[]{
                    1, 0, 0, 0, 0,
                    0, 1, 0, 0, 0,
                    0, 0, 1, 0, 0,
                    0, 0, 0, 1, 0
            };
            for (int i = 0; i < 20; i++) {
                assertEquals(id[i], m[i], 1e-4f, "cell " + i);
            }
        }
    }
}
