package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.animations.MorphTransition;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// Regression test for the "source inside a scrolling parent" case of
/// {@link MorphTransition}.
///
/// The source form has a long scrolling list of placeholder cards plus one
/// named "card-of-interest" that lives partway down the list. Before the
/// transition starts the source's scroll position is advanced so the named
/// card sits near the top edge of the viewport -- meaning the off-viewport
/// cards above it are clipped by the form bounds, not the parent's content
/// bounds. The destination form positions the same-named card at the top
/// of the form, full-width.
///
/// The bug this test guards against: an earlier {@link MorphTransition}
/// implementation re-painted the source component live during animate()
/// using {@code paintComponent(g)}, which renders the *full* component
/// including off-viewport pixels. With a scrolled parent this caused the
/// morph to briefly show pixels the user could not see at the moment they
/// tapped. The fix is to capture the source as a clipped Image at
/// initTransition() and tween that. Any regression that puts the source
/// back on the live-paint path produces a different grid image and the
/// screenshot diff catches it.
public class MorphTransitionScrolledSourceTest extends AbstractTransitionScreenshotTest {

    private static final String CARD_NAME = "morph-card-scrolled";
    /// How far down the long list the named card sits. Picked large enough
    /// that on every supported skin the card is at least one viewport
    /// below the top of the list before scrolling.
    private static final int LEADING_FILLER = 12;
    private static final int TRAILING_FILLER = 12;

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

        Container list = new Container(BoxLayout.y());
        list.setScrollableY(true);
        for (int i = 0; i < LEADING_FILLER; i++) {
            list.add(buildFiller("Filler " + (i + 1), 0x2a4a78));
        }
        Label card = new Label("Card");
        card.setName(CARD_NAME);
        Style cs = card.getAllStyles();
        cs.setBgColor(0xef4444);
        cs.setFgColor(0xffffff);
        cs.setBgTransparency(255);
        cs.setPadding(12, 12, 8, 8);
        cs.setMargin(2, 2, 4, 4);
        list.add(card);

        for (int i = 0; i < TRAILING_FILLER; i++) {
            list.add(buildFiller("Filler " + (LEADING_FILLER + i + 1), 0x2a4a78));
        }
        form.add(BorderLayout.CENTER, list);

        // Force a layout so the list has real bounds, then scroll so the
        // named card sits near the top of the viewport. Without this, the
        // scroll-out-of-view condition the test depends on doesn't occur.
        form.layoutContainer();
        list.scrollComponentToVisible(card);
    }

    private Label buildFiller(String text, int bgColor) {
        Label l = new Label(text);
        Style s = l.getAllStyles();
        s.setBgColor(bgColor);
        s.setFgColor(0xffffff);
        s.setBgTransparency(255);
        s.setPadding(12, 12, 8, 8);
        s.setMargin(2, 2, 4, 4);
        return l;
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
        cs.setPadding(24, 24, 12, 12);
        cs.setMargin(0, 8, 8, 8);

        form.add(BorderLayout.NORTH, card);
    }
}
