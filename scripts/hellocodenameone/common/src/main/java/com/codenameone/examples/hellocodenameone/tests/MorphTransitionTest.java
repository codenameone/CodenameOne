package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.animations.MorphTransition;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// Baseline {@link MorphTransition} test. Source places a 6mm-square red
/// "card" tile in the bottom-left of the form; destination places the same
/// "card" expanded across the top. The morph tweens that one named
/// component between the two layouts.
///
/// This is the simplest possible morph case -- both components are fully
/// on-screen at all times. See {@link MorphTransitionScrolledSourceTest}
/// for the source-in-scrolling-container variant that exercises the
/// clipped-capture path.
public class MorphTransitionTest extends AbstractTransitionScreenshotTest {

    private static final String CARD_NAME = "morph-card";

    @Override
    protected Transition createTransition(int duration) {
        return MorphTransition.create(duration).morph(CARD_NAME);
    }

    @Override
    protected void buildSourceForm(Form form) {
        form.setLayout(new BorderLayout());
        Style cps = form.getContentPane().getAllStyles();
        cps.setBgTransparency(255);
        cps.setBgColor(0x1f4068);
        cps.setFgColor(0xffffff);

        Label card = new Label("Card");
        card.setName(CARD_NAME);
        Style cs = card.getAllStyles();
        cs.setBgColor(0xef4444);
        cs.setFgColor(0xffffff);
        cs.setBgTransparency(255);
        cs.setPadding(8, 8, 8, 8);
        cs.setMargin(20, 4, 4, 20);

        // The card lives in the SOUTH region as a compact tile.
        Container row = new Container(new BorderLayout());
        row.add(BorderLayout.WEST, card);
        form.add(BorderLayout.SOUTH, row);
    }

    @Override
    protected void buildDestForm(Form form) {
        form.setLayout(new BorderLayout());
        Style cps = form.getContentPane().getAllStyles();
        cps.setBgTransparency(255);
        cps.setBgColor(0x9c1d1d);
        cps.setFgColor(0xffffff);

        Label card = new Label("Card (expanded)");
        card.setName(CARD_NAME);
        Style cs = card.getAllStyles();
        cs.setBgColor(0xef4444);
        cs.setFgColor(0xffffff);
        cs.setBgTransparency(255);
        cs.setPadding(20, 20, 8, 8);

        // The card lives in NORTH and stretches across the full width.
        form.add(BorderLayout.NORTH, card);
    }
}
