package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.plaf.Style;

/// Non-screenshot test (extends BaseTest but `shouldTakeScreenshot()`
/// returns false) that verifies the CSS compiler stores `filter: blur(...)`
/// and `backdrop-filter: blur(...)` radii on the corresponding Style fields.
/// Actual paint-time application is not yet wired through the component paint
/// pipeline (the radii live in Style, the platform `Graphics.gaussianBlur`
/// primitive is the building block, and Component.paint plumbing is a
/// follow-up). Taking a screenshot here would mislead - the four tiles
/// would all render identically since the blur radius isn't consumed at
/// paint time - so this test asserts the field round-trip and tells
/// `Cn1ssDeviceRunner` to skip the capture.
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
}
