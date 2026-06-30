package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.animations.MorphTransition;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;

/// Snapshot-mode {@link MorphTransition} that also tweens the named target's
/// rotation, scale and opacity, driven through the scrub API
/// (`MorphTransition#setProgress(double)`) rather than the animation clock.
///
/// Same card layout as {@link MorphTransitionTest} / {@link MorphTransitionSnapshotTest}
/// (a red tile in the bottom-left expanding across the top) but the captured
/// snapshot is additionally rotated up to 20 degrees, scaled to 1.1x and faded
/// to 70% as it travels -- the position / size morph composed with the new
/// per-target transforms.
public class MorphTransitionScaleRotateScreenshotTest extends AbstractTransitionScreenshotTest {

    private static final String CARD_NAME = "morph-card-rotate";

    @Override
    protected Transition createTransition(int duration) {
        return MorphTransition.create(duration)
                .snapshotMode(true)
                .morph(CARD_NAME)
                .rotation(CARD_NAME, 0f, 20f)
                .scale(CARD_NAME, 1f, 1.1f)
                .opacity(CARD_NAME, 1f, 0.7f);
    }

    @Override
    protected void advanceTransition(double progress) {
        ((MorphTransition) getTransition()).setProgress(progress);
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

        form.add(BorderLayout.NORTH, card);
    }
}
