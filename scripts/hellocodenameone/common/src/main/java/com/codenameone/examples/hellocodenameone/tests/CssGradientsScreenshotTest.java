package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.GradientDescriptor;
import com.codename1.ui.plaf.Style;

/// End-to-end CSS gradient screenshot test. The companion `theme.css`
/// declares one UIID per supported gradient form (angled multi-stop linear,
/// `to <side1> <side2>`, mismatched-alpha linear, radial `farthest-corner`,
/// elliptical radial, conic, repeating-linear, repeating-radial). This test
/// builds a Container per UIID, lays them out in a grid, and captures a
/// screenshot.
///
/// The test also asserts that each UIID actually carries the expected
/// background type byte and a non-null gradient descriptor - that way a
/// silent CSS compiler regression (e.g. dropping support for the conic form,
/// or falling back to a rasterized image because of a mismatched alpha
/// short-circuit) shows up as an explicit test failure even before the
/// screenshot comparison runs.
public class CssGradientsScreenshotTest extends BaseTest {

    private static final String[] UIIDS = {
            "CssGradientLinearAngled",
            "CssGradientLinearToSide",
            "CssGradientLinearMismatchedAlpha",
            "CssGradientRadialFarthestCorner",
            "CssGradientRadialEllipse",
            "CssGradientConic",
            "CssGradientRepeatingLinear",
            "CssGradientRepeatingRadial"
    };

    @Override
    public boolean runTest() {
        Form form = createForm("css-gradients", new BorderLayout(), "css-gradients");
        form.setUIID("GraphicsForm");

        Container grid = new Container(new GridLayout(4, 2));
        // Index-aligned with UIIDS - one expected background type per tile.
        byte[] expectedBgTypes = {
                Style.BACKGROUND_GRADIENT_LINEAR,
                Style.BACKGROUND_GRADIENT_LINEAR,
                Style.BACKGROUND_GRADIENT_LINEAR,
                Style.BACKGROUND_GRADIENT_RADIAL_FULL,
                Style.BACKGROUND_GRADIENT_RADIAL_FULL,
                Style.BACKGROUND_GRADIENT_CONIC,
                Style.BACKGROUND_GRADIENT_REPEATING_LINEAR,
                Style.BACKGROUND_GRADIENT_REPEATING_RADIAL
        };
        for (int i = 0; i < UIIDS.length; i++) {
            String uiid = UIIDS[i];
            Container tile = new Container();
            tile.setUIID(uiid);
            tile.add(new Label(shortName(uiid)));
            grid.add(tile);
            // Sanity-assert the compiled theme actually applied the new
            // gradient type and produced a descriptor. A silent CSS compiler
            // regression that drops support for one form (e.g. a mismatched
            // alpha short-circuit, or a missing conic-gradient case) will
            // fail here before the screenshot comparison runs.
            Style s = tile.getUnselectedStyle();
            byte actual = s.getBackgroundType();
            if (actual != expectedBgTypes[i]) {
                fail("Wrong bgType for " + uiid + ": expected " + expectedBgTypes[i] + " got " + actual);
            }
            GradientDescriptor g = s.getGradientDescriptor();
            if (g == null || g.getColors() == null || g.getColors().length < 2) {
                fail("Missing/invalid gradient descriptor for " + uiid);
            }
        }
        form.add(BorderLayout.CENTER, grid);

        form.show();
        return !isFailed();
    }

    private String shortName(String uiid) {
        // Trim the common "CssGradient" prefix so the in-tile label remains
        // readable on small simulators.
        return uiid.startsWith("CssGradient") ? uiid.substring("CssGradient".length()) : uiid;
    }
}
