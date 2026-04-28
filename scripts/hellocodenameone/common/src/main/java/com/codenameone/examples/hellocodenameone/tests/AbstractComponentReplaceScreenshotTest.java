package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// Drives a component-scoped `Container.replace` transition through six
/// deterministic frames. The transition only morphs a single child (the "card")
/// in the middle of an otherwise static form, so the surrounding chrome stays
/// put while the swapped content animates.
public abstract class AbstractComponentReplaceScreenshotTest extends AbstractAnimationScreenshotTest {
    private Form replaceHost;
    private Container slot;
    private Component currentCard;
    private Component nextCard;
    private Transition transition;
    private ComponentAnimation replaceAnim;

    protected abstract Transition createTransition(int duration);

    /// Build the component being replaced. Default returns a coloured "Before"
    /// card; override for a different layout or content.
    protected Component buildCurrentCard() {
        return makeCard("Before", 0x1f4068, 0xffffff);
    }

    /// Build the replacement component. Default returns a coloured "After" card.
    protected Component buildNextCard() {
        return makeCard("After", 0x9c1d1d, 0xffffff);
    }

    private static Container makeCard(String label, int bgColor, int fgColor) {
        Container card = new Container(BoxLayout.y());
        Style cs = card.getAllStyles();
        cs.setBgColor(bgColor);
        cs.setBgTransparency(255);
        cs.setPadding(18, 18, 18, 18);
        cs.setMargin(8, 8, 8, 8);
        Label heading = new Label(label);
        heading.getAllStyles().setFgColor(fgColor);
        Label body = new Label("Card content");
        body.getAllStyles().setFgColor(fgColor);
        card.add(heading);
        card.add(body);
        return card;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        replaceHost = new Form("Component Replace");
        replaceHost.setLayout(new BorderLayout());
        replaceHost.setWidth(frameWidth);
        replaceHost.setHeight(frameHeight);
        replaceHost.setVisible(true);

        Container chrome = new Container(BoxLayout.y());
        Style chromeStyle = chrome.getAllStyles();
        chromeStyle.setBgColor(0xeeeeee);
        chromeStyle.setBgTransparency(255);
        chromeStyle.setPadding(8, 8, 8, 8);
        Label header = new Label("Static surroundings");
        header.getAllStyles().setFgColor(0x222222);
        chrome.add(header);

        slot = new Container(new BorderLayout());
        Style slotStyle = slot.getAllStyles();
        slotStyle.setBgColor(0xffffff);
        slotStyle.setBgTransparency(255);

        currentCard = buildCurrentCard();
        slot.add(BorderLayout.CENTER, currentCard);
        chrome.add(slot);

        Label footer = new Label("Footer stays put");
        footer.getAllStyles().setFgColor(0x222222);
        chrome.add(footer);

        replaceHost.add(BorderLayout.CENTER, chrome);
        replaceHost.layoutContainer();

        nextCard = buildNextCard();
        transition = createTransition(getAnimationDurationMillis());
        // createReplaceTransition wires nextCard's parent to slot internally
        // (setParent is package-private) and returns a ComponentAnimation we
        // can drive frame-by-frame instead of letting the manager run it.
        replaceAnim = slot.createReplaceTransition(currentCard, nextCard, transition);
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        if (replaceAnim != null) {
            // First call lazily invokes Transition.init / initTransition;
            // subsequent calls advance Transition.animate using AnimationTime.
            replaceAnim.updateAnimationState();
        }
        // Repaint the static surroundings, then overlay the transition
        // (its paint() clips itself to the source component's rect).
        replaceHost.paintComponent(g, true);
        if (transition != null) {
            transition.paint(g);
        }
    }

    @Override
    protected void finishCapture() {
        if (transition != null) {
            try {
                transition.cleanup();
            } catch (Throwable ignore) {
                // best effort cleanup
            }
        }
        replaceHost = null;
        slot = null;
        currentCard = null;
        nextCard = null;
        transition = null;
        replaceAnim = null;
        super.finishCapture();
    }
}
