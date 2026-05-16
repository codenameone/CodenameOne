package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;

/// CSS filter / backdrop-filter screenshot test. The companion theme.css
/// declares four UIIDs that share a gradient background, but with
/// `filter: blur(...)` and `backdrop-filter: blur(...)` properties of varying
/// intensity. The test verifies the per-state Style has the expected blur
/// radius compiled in (so a CSS regression that silently drops the
/// filter property surfaces as an explicit failure), then takes a screenshot
/// to capture per-port differences in the actual blur compositing.
public class CssFilterBlurScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        Form form = createForm("css-filter-blur", new BorderLayout(), "css-filter-blur");
        form.setUIID("GraphicsForm");

        Container noneTile = tile("CssFilterBlurNone",   "no blur");
        Container lightTile = tile("CssFilterBlurLight",  "blur(2px)");
        Container heavyTile = tile("CssFilterBlurHeavy",  "blur(8px)");
        Container backdropTile = tile("CssFilterBackdrop","backdrop blur(12px)");

        Container grid = new Container(new GridLayout(2, 2));
        grid.add(noneTile);
        grid.add(lightTile);
        grid.add(heavyTile);
        grid.add(backdropTile);
        form.add(BorderLayout.CENTER, grid);

        // Sanity-assert the compiler folded each filter / backdrop-filter
        // property into the corresponding Style field. The CSS pixel unit
        // round-trips as a float radius (mm units would scale through
        // Display.convertToPixels, so we use px in theme.css to keep this
        // assertion stable).
        assertNoBlur(noneTile, "CssFilterBlurNone");
        assertFilterBlur(lightTile, "CssFilterBlurLight",  2f);
        assertFilterBlur(heavyTile, "CssFilterBlurHeavy",  8f);
        assertBackdropFilterBlur(backdropTile, "CssFilterBackdrop", 12f);

        form.show();
        return !isFailed();
    }

    private Container tile(String uiid, String label) {
        Container c = new Container();
        c.setUIID(uiid);
        c.add(new Label(label));
        return c;
    }

    private void assertNoBlur(Container c, String uiid) {
        Style s = c.getUnselectedStyle();
        if (s.getFilterBlurRadius() > 0f) {
            fail(uiid + " should have no filter:blur but found " + s.getFilterBlurRadius());
        }
        if (s.getBackdropFilterBlurRadius() > 0f) {
            fail(uiid + " should have no backdrop-filter:blur but found " + s.getBackdropFilterBlurRadius());
        }
    }

    private void assertFilterBlur(Container c, String uiid, float expected) {
        float v = c.getUnselectedStyle().getFilterBlurRadius();
        if (Math.abs(v - expected) > 0.5f) {
            fail(uiid + " expected filter:blur " + expected + " got " + v);
        }
    }

    private void assertBackdropFilterBlur(Container c, String uiid, float expected) {
        float v = c.getUnselectedStyle().getBackdropFilterBlurRadius();
        if (Math.abs(v - expected) > 0.5f) {
            fail(uiid + " expected backdrop-filter:blur " + expected + " got " + v);
        }
    }
}
