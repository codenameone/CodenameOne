package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.system.NativeLookup;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;
import com.codenameone.examples.hellocodenameone.StatusBarTapDiagnosticNative;

/// Visualises the iOS status-bar tap-to-scroll-to-top diagnostic. Each frame
/// alternates between "scrolled to bottom" and "tap fired -> scrolled to top",
/// with a glass-pane overlay showing the rising tap counter. The tap is fired
/// through the StatusBarTapDiagnosticNative interface; on iOS that bumps the
/// real native counter and dispatches the synthesized pointer event the same
/// way scrollViewShouldScrollToTop: does, on other platforms it falls back
/// to a Form.pointerPressed dispatch so the screenshot looks identical.
public class StatusBarTapDiagnosticScreenshotTest extends AbstractAnimationScreenshotTest {
    private static class TestForm extends Form {
        TestForm(String title) {
            super(title);
        }

        @Override
        protected boolean shouldPaintStatusBar() {
            // Force the StatusBar button to be created on every platform so
            // the (displayWidth/2, 0) tap finds a responder regardless of the
            // current native theme.
            return true;
        }
    }

    private static class ScrollContainer extends Container {
        ScrollContainer(Layout l) {
            super(l);
            setScrollableY(true);
            // Synchronous scroll so each frame captures the post-scroll state
            // without waiting for the Motion-driven smooth-scroll animation.
            setSmoothScrolling(false);
        }

        void scrollTo(int y) {
            setScrollY(y);
        }
    }

    private static final int TILE_COUNT = 16;

    private TestForm scrollHost;
    private ScrollContainer scrollContainer;
    private StatusBarTapDiagnosticNative nativeInterface;
    private int maxScroll;
    private int simulatedTaps;

    @Override
    protected int getAnimationDurationMillis() {
        return 600;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        simulatedTaps = 0;
        nativeInterface = NativeLookup.create(StatusBarTapDiagnosticNative.class);

        scrollHost = new TestForm("Status Bar Tap");
        scrollHost.setLayout(new BorderLayout());
        scrollHost.setWidth(frameWidth);
        scrollHost.setHeight(frameHeight);
        scrollHost.setVisible(true);

        scrollContainer = new ScrollContainer(BoxLayout.y());
        Style cs = scrollContainer.getAllStyles();
        cs.setBgColor(0x0b132b);
        cs.setBgTransparency(255);
        cs.setPadding(4, 4, 4, 4);
        for (int i = 0; i < TILE_COUNT; i++) {
            Label tile = new Label("Item " + (i + 1));
            Style ts = tile.getAllStyles();
            ts.setBgColor(rowColor(i));
            ts.setFgColor(0xffffff);
            ts.setBgTransparency(255);
            ts.setMargin(2, 2, 2, 2);
            ts.setPadding(16, 16, 14, 14);
            scrollContainer.add(tile);
        }
        scrollHost.add(BorderLayout.CENTER, scrollContainer);
        scrollHost.layoutContainer();

        int contentHeight = scrollContainer.getScrollDimension().getHeight();
        maxScroll = Math.max(0, contentHeight - scrollContainer.getHeight());
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        // Even frames: scrolled to bottom, no tap yet.
        // Odd frames:  fire the simulated tap, then snap the standalone
        //              scrollHost to the top. The native interface call
        //              targets the test runner's live form (Display.getCurrent),
        //              not this off-screen scrollHost, so the visible scroll
        //              has to be applied explicitly here for the screenshot
        //              to be deterministic across platforms.
        if ((frameIndex & 1) == 0) {
            scrollContainer.scrollTo(maxScroll);
        } else {
            fireSimulatedTap();
            scrollContainer.scrollTo(0);
        }
        scrollHost.paintComponent(g, true);
        paintGlassPane(g, width, height, frameIndex);
    }

    private void fireSimulatedTap() {
        if (nativeInterface != null && nativeInterface.isSupported()) {
            nativeInterface.simulateStatusBarTap();
        }
        simulatedTaps++;
    }

    private void paintGlassPane(Graphics g, int width, int height, int frameIndex) {
        int barH = Math.max(48, height / 8);
        g.setColor(0x000000);
        g.setAlpha(170);
        g.fillRect(0, 0, width, barH);
        g.setAlpha(255);
        g.setColor(0xffffff);
        String headline = "Status Bar Tap Counter: " + simulatedTaps;
        String sub = ((frameIndex & 1) == 0)
                ? "scrolled to bottom"
                : "tap fired (W/2, 0) -> scroll to top";
        int pad = 8;
        g.drawString(headline, pad, pad);
        g.setColor(0xffd166);
        g.drawString(sub, pad, pad + g.getFont().getHeight() + 2);
    }

    @Override
    protected void finishCapture() {
        scrollHost = null;
        scrollContainer = null;
        nativeInterface = null;
        super.finishCapture();
    }

    private static int rowColor(int i) {
        int[] palette = {0x118ab2, 0x06d6a0, 0xffd166, 0xef476f, 0x8338ec, 0xfb5607};
        return palette[i % palette.length];
    }
}
