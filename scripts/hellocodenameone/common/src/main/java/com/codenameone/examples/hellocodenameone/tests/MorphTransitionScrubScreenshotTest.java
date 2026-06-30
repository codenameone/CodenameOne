package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.animations.MorphTransition;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;

/// Demonstrates that a **named-component** {@link MorphTransition} (the legacy
/// "morph the component called X from one form to the next" case) is now
/// scrubbable: every frame of the grid is produced by stepping the transition
/// to its progress fraction with `MorphTransition#setProgress(double)` rather
/// than letting the animation clock drive it.
///
/// A cyan "card" tile in the bottom-left of the source form morphs into a wide
/// card across the top of the destination form. Because the whole sweep is
/// rendered through the (live, non-snapshot) transition, the card is visible
/// travelling across all six frames -- the deterministic, scrub-driven
/// counterpart to the clock-driven {@link MorphTransitionTest}. The companion
/// {@link MorphElementMorphScreenshotTest} covers the same scrub API applied to
/// an arbitrary rendered element with opacity / rotation / scale.
public class MorphTransitionScrubScreenshotTest extends AbstractTransitionScreenshotTest {

    private static final String CARD_NAME = "morph-card-scrub";

    @Override
    protected Transition createTransition(int duration) {
        return MorphTransition.create(duration).morph(CARD_NAME);
    }

    @Override
    protected boolean paintBookendDirectly(int frameIndex) {
        // Render every frame through the scrubbed transition so the card is
        // shown travelling end to end, not just at the bookends.
        return false;
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
        cps.setBgColor(0x0f3a3a);
        cps.setFgColor(0xffffff);

        Label card = new Label("Card");
        card.setName(CARD_NAME);
        Style cs = card.getAllStyles();
        cs.setBgColor(0x22d3ee);
        cs.setFgColor(0x062b2b);
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
        cps.setBgColor(0x134e4a);
        cps.setFgColor(0xffffff);

        Label card = new Label("Card (expanded)");
        card.setName(CARD_NAME);
        Style cs = card.getAllStyles();
        cs.setBgColor(0x22d3ee);
        cs.setFgColor(0x062b2b);
        cs.setBgTransparency(255);
        cs.setPadding(20, 20, 8, 8);

        form.add(BorderLayout.NORTH, card);
    }
}
