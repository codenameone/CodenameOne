package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.ConicGradient;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Gradient;
import com.codename1.ui.Label;
import com.codename1.ui.LinearGradient;
import com.codename1.ui.RadialGradient;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;

/// End-to-end CSS gradient screenshot test. The companion `theme.css`
/// declares one UIID per supported gradient form (angled multi-stop linear,
/// `to <side1> <side2>`, mismatched-alpha linear, radial `farthest-corner`,
/// elliptical radial, conic, repeating-linear, repeating-radial). This test
/// builds a Container per UIID, lays them out in a grid, and captures a
/// screenshot.
///
/// Each tile is also asserted to carry the expected `BACKGROUND_GRADIENT_*`
/// type byte plus a `Gradient` of the expected concrete subclass (LinearGradient,
/// RadialGradient, ConicGradient). A silent CSS compiler regression that
/// drops support for one form fails here before the screenshot comparison
/// runs.
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
        // Index-aligned with UIIDS - one expected background type + Gradient
        // subclass per tile.
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
        Class<?>[] expectedKinds = {
                LinearGradient.class,
                LinearGradient.class,
                LinearGradient.class,
                RadialGradient.class,
                RadialGradient.class,
                ConicGradient.class,
                LinearGradient.class,
                RadialGradient.class
        };
        for (int i = 0; i < UIIDS.length; i++) {
            String uiid = UIIDS[i];
            Container tile = new Container();
            tile.setUIID(uiid);
            tile.add(new Label(shortName(uiid)));
            grid.add(tile);
            Style s = tile.getUnselectedStyle();
            byte actual = s.getBackgroundType();
            if (actual != expectedBgTypes[i]) {
                fail("Wrong bgType for " + uiid + ": expected " + expectedBgTypes[i] + " got " + actual);
            }
            Gradient g = s.getGradient();
            if (g == null) {
                fail("Missing gradient for " + uiid);
                continue;
            }
            if (!expectedKinds[i].isInstance(g)) {
                fail("Wrong Gradient kind for " + uiid + ": expected "
                        + expectedKinds[i].getSimpleName() + " got " + g.getClass().getSimpleName());
            }
            if (g.getColors() == null || g.getColors().length < 2) {
                fail("Invalid gradient stops for " + uiid);
            }
        }
        form.add(BorderLayout.CENTER, grid);
        form.show();
        return !isFailed();
    }

    private String shortName(String uiid) {
        return uiid.startsWith("CssGradient") ? uiid.substring("CssGradient".length()) : uiid;
    }
}
