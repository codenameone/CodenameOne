package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.plaf.Style;

/// Non-screenshot test (extends BaseTest but `shouldTakeScreenshot()`
/// returns false) that verifies the CSS compiler stores `filter:` and
/// `backdrop-filter:` declarations on the corresponding Style fields:
/// blur as a radius, and the color-style filters (brightness, contrast,
/// grayscale, invert, sepia, plus the chain composition) as a 4x5
/// ColorMatrix. Paint-time application is a follow-up; taking a
/// screenshot here would mislead - the tiles all render identically
/// since neither path is consumed during paint yet.
public class CssFilterBlurScreenshotTest extends BaseTest {

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        assertNoBlur("CssFilterBlurNone");
        assertFilterBlur("CssFilterBlurLight", 2f);
        assertFilterBlur("CssFilterBlurHeavy", 8f);
        assertBackdropFilterBlur("CssFilterBackdrop", 12f);

        // Color-matrix filters. We don't assert each cell exactly -
        // the matrix math lives in the CSS compiler and we don't want
        // the test to recompute it - so just assert the matrix is
        // present, well-shaped (20 floats), and not the identity.
        assertHasColorMatrix("CssFilterBrightness");
        assertHasColorMatrix("CssFilterContrast");
        assertHasColorMatrix("CssFilterGrayscale");
        assertHasColorMatrix("CssFilterInvert");
        assertHasColorMatrix("CssFilterSepia");
        assertHasColorMatrix("CssFilterChain");

        // CssFilterBlurNone should not carry a color matrix either.
        Style none = style("CssFilterBlurNone");
        if (none.getFilterColorMatrix() != null) {
            fail("CssFilterBlurNone should have no color matrix");
        }

        // CssFilterGrayscale(1) maps every R/G/B channel to the same
        // luminance, so the three diagonal cells become equal to one
        // another within rounding. Verify that.
        float[] gray = style("CssFilterGrayscale").getFilterColorMatrix();
        if (gray != null) {
            float r = gray[0];
            float g = gray[6];
            float b = gray[12];
            if (Math.abs(r - 0.2126f) > 0.01f
                    || Math.abs(g - 0.7152f) > 0.01f
                    || Math.abs(b - 0.0722f) > 0.01f) {
                fail("CssFilterGrayscale(1) diagonal should be Rec 709 luma weights; got "
                        + r + "," + g + "," + b);
            }
        }

        done();
        return !isFailed();
    }

    private Style style(String uiid) {
        Container c = new Container();
        c.setUIID(uiid);
        return c.getUnselectedStyle();
    }

    private void assertNoBlur(String uiid) {
        Style s = style(uiid);
        if (s.getFilterBlurRadius() > 0f) {
            fail(uiid + " expected no filter:blur, got " + s.getFilterBlurRadius());
        }
        if (s.getBackdropFilterBlurRadius() > 0f) {
            fail(uiid + " expected no backdrop-filter:blur, got " + s.getBackdropFilterBlurRadius());
        }
    }

    private void assertFilterBlur(String uiid, float expected) {
        float actual = style(uiid).getFilterBlurRadius();
        if (Math.abs(actual - expected) > 0.5f) {
            fail(uiid + " expected filter:blur " + expected + ", got " + actual);
        }
    }

    private void assertBackdropFilterBlur(String uiid, float expected) {
        float actual = style(uiid).getBackdropFilterBlurRadius();
        if (Math.abs(actual - expected) > 0.5f) {
            fail(uiid + " expected backdrop-filter:blur " + expected + ", got " + actual);
        }
    }

    private void assertHasColorMatrix(String uiid) {
        float[] m = style(uiid).getFilterColorMatrix();
        if (m == null) {
            fail(uiid + " missing filter color matrix");
            return;
        }
        if (m.length != 20) {
            fail(uiid + " color matrix has " + m.length + " floats, expected 20");
            return;
        }
        if (isIdentity(m)) {
            fail(uiid + " color matrix collapsed to identity");
        }
    }

    private static boolean isIdentity(float[] m) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 5; col++) {
                float expected = (row == col) ? 1f : 0f;
                if (Math.abs(m[row * 5 + col] - expected) > 1e-4f) return false;
            }
        }
        return true;
    }
}
