package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.animations.MorphTransition;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// Snapshot-mode variant of {@link MorphTransitionTest}. Same source / dest
/// layout, but the transition is built with
/// {@code MorphTransition.create(d).snapshotMode(true).morph(...)} so the
/// per-frame paint pulls from images captured at initTransition() rather
/// than re-painting the live source / dest components from the layered
/// pane.
///
/// Why a separate test rather than flipping the existing one to snapshot
/// mode: the legacy live-paint path stays the framework default for
/// backwards compatibility; both paths should have their own locked
/// baseline so a regression on one doesn't get hidden under the other's
/// tolerance.
public class MorphTransitionSnapshotTest extends AbstractTransitionScreenshotTest {

    private static final String CARD_NAME = "morph-card-snapshot";

    @Override
    protected Transition createTransition(int duration) {
        return MorphTransition.create(duration).snapshotMode(true).morph(CARD_NAME);
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
