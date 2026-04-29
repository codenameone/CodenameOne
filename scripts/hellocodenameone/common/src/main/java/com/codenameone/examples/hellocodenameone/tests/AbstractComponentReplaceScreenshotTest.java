package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;

/// Component-scoped `Container.replace` screenshot tests.
///
/// The composite is built in one paint, not six. We assemble a host form whose
/// content pane is itself a `GridLayout(GRID_ROWS, GRID_COLS)` with six
/// independent slots. Each slot owns its own `currentCard` / `nextCard` /
/// `Transition` triple. By staggering each transition's start time via
/// [AnimationTime] before triggering its first `updateAnimationState`, every
/// transition can run "in parallel" yet land on a different progress fraction
/// (0%, 20%, 40%, 60%, 80%, 100%) when the global clock is set to a single
/// shared end time. The resulting screenshot is a single paint of the form
/// with the four mid-progress transitions overlaid - no per-cell capture and
/// no scaling.
///
/// Frame 0 paints just the source card (no transition wired), the last frame
/// paints just the destination card, and the four middle frames render the
/// source card with the transition overlaid at the appropriate progress.
public abstract class AbstractComponentReplaceScreenshotTest extends AbstractAnimationScreenshotTest {
    private static final int FRAME_COUNT = 6;
    private static final int LAST_FRAME_INDEX = 5;

    private Form replaceHost;
    private final Container[] slots = new Container[FRAME_COUNT];
    private final Component[] currentCards = new Component[FRAME_COUNT];
    private final Component[] nextCards = new Component[FRAME_COUNT];
    private final Transition[] transitions = new Transition[FRAME_COUNT];
    private final ComponentAnimation[] anims = new ComponentAnimation[FRAME_COUNT];

    protected abstract Transition createTransition(int duration);

    /// Build the component being replaced. Default returns a coloured "Before"
    /// card; override for a different layout or content. A fresh instance must
    /// be returned every call because each cell needs its own independent
    /// component graph.
    protected Component buildCurrentCard() {
        return makeCard("Before", 0x1f4068, 0xffffff);
    }

    /// Build the replacement component. Default returns a coloured "After"
    /// card. As with [buildCurrentCard] a new instance must be returned every
    /// call.
    protected Component buildNextCard() {
        return makeCard("After", 0x9c1d1d, 0xffffff);
    }

    private static Container makeCard(String label, int bgColor, int fgColor) {
        // GridLayout(3, 1) splits the cell vertically into three equal bands so
        // the heading/body/footer stay visually balanced no matter what aspect
        // ratio the host's grid cell turns out to be.
        Container card = new Container(new GridLayout(3, 1));
        Style cs = card.getAllStyles();
        cs.setBgColor(bgColor);
        cs.setBgTransparency(255);
        cs.setPadding(16, 16, 14, 14);
        cs.setMargin(6, 6, 6, 6);

        Label heading = new Label(label);
        heading.getAllStyles().setFgColor(fgColor);
        card.add(heading);

        Label body = new Label("Card body");
        body.getAllStyles().setFgColor(fgColor);
        card.add(body);

        Label footer = new Label("Tap to act");
        footer.getAllStyles().setFgColor(fgColor);
        card.add(footer);
        return card;
    }

    @Override
    protected Image buildScreenshot(int width, int height) {
        replaceHost = new Form();
        replaceHost.setWidth(width);
        replaceHost.setHeight(height);
        replaceHost.setVisible(true);
        // Strip the title chrome so the content pane fills the entire form,
        // making each grid cell exactly width/GRID_COLS x height/GRID_ROWS.
        stripFormChrome(replaceHost);
        replaceHost.setLayout(new GridLayout(GRID_ROWS, GRID_COLS));

        // Bookend cells (frame 0 and frame LAST) skip the transition entirely:
        // frame 0 just shows the source card, frame LAST just shows the
        // destination card. Middle cells start with the source card and the
        // transition will be overlaid at paint time.
        for (int i = 0; i < FRAME_COUNT; i++) {
            slots[i] = new Container(new BorderLayout());
            Style slotStyle = slots[i].getAllStyles();
            slotStyle.setBgColor(0xffffff);
            slotStyle.setBgTransparency(255);

            if (i == LAST_FRAME_INDEX) {
                nextCards[i] = buildNextCard();
                slots[i].add(BorderLayout.CENTER, nextCards[i]);
            } else {
                currentCards[i] = buildCurrentCard();
                slots[i].add(BorderLayout.CENTER, currentCards[i]);
            }
            replaceHost.add(slots[i]);
        }
        replaceHost.layoutContainer();

        int duration = getAnimationDurationMillis();
        long endTime = getAnimationStartTime() + duration;

        // Stagger each middle cell's transition start time so that, when the
        // shared clock is later parked at endTime, each motion has elapsed
        // exactly progress * duration. Initialising while the clock is at
        // startTime[i] makes Motion.start() (called lazily inside
        // TransitionAnimation.updateState's first call) capture that value as
        // its baseline.
        for (int i = 1; i < LAST_FRAME_INDEX; i++) {
            double progress = (double) i / (double) (FRAME_COUNT - 1);
            long startTime = endTime - (long) Math.round(progress * (double) duration);
            AnimationTime.setTime(startTime);

            nextCards[i] = buildNextCard();
            transitions[i] = createTransition(duration);
            anims[i] = slots[i].createReplaceTransition(currentCards[i], nextCards[i], transitions[i]);
            if (anims[i] != null) {
                // Lazily invokes Transition.init / initTransition.
                anims[i].updateAnimationState();
            }
        }

        // Park the global clock at endTime so each motion advances to its
        // pre-staged progress fraction in a single call.
        AnimationTime.setTime(endTime);
        for (int i = 1; i < LAST_FRAME_INDEX; i++) {
            if (anims[i] != null) {
                anims[i].updateAnimationState();
            }
        }

        Image screenshot = Image.createImage(width, height, 0xffffffff);
        Graphics g = screenshot.getGraphics();
        // Single full-form paint - the cells are already sized correctly by
        // GridLayout and will be drawn at native resolution.
        replaceHost.paintComponent(g, true);

        // Container.paint doesn't paint cmpTransitions automatically (those
        // normally render via Display.repaint(t) in the running app); since
        // we're not in the live paint loop we have to overlay each transition
        // ourselves. Each transition.paint() uses its own source.absoluteX/Y
        // so they land on the correct cell.
        for (int i = 1; i < LAST_FRAME_INDEX; i++) {
            if (transitions[i] != null) {
                transitions[i].paint(g);
            }
        }

        cleanupTransitions();
        return screenshot;
    }

    private void cleanupTransitions() {
        for (int i = 0; i < FRAME_COUNT; i++) {
            if (transitions[i] != null) {
                try {
                    transitions[i].cleanup();
                } catch (Throwable ignore) {
                    // best effort
                }
                transitions[i] = null;
            }
            slots[i] = null;
            currentCards[i] = null;
            nextCards[i] = null;
            anims[i] = null;
        }
        replaceHost = null;
    }

    private static void stripFormChrome(Form form) {
        Container titleArea = form.getTitleArea();
        titleArea.removeAll();
        titleArea.setVisible(false);
        titleArea.setPreferredSize(new Dimension(0, 0));
        Style titleStyle = titleArea.getAllStyles();
        titleStyle.setPadding(0, 0, 0, 0);
        titleStyle.setMargin(0, 0, 0, 0);
        Style contentStyle = form.getContentPane().getAllStyles();
        contentStyle.setPadding(0, 0, 0, 0);
        contentStyle.setMargin(0, 0, 0, 0);
    }
}
