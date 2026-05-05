package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.system.NativeLookup;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codenameone.examples.hellocodenameone.StatusBarTapDiagnosticNative;

/// Single-frame regression screenshot for the iOS status-bar tap diagnostic.
/// Builds a scrollable form, fires three taps through StatusBarTapDiagnosticNative
/// (which on iOS hits the same cn1FireStatusBarTap path scrollViewShouldScrollToTop:
/// runs), then composes the result side-by-side with a synthetic "before" capture
/// so the same image shows the counter rising from 0 to 3 and the scroll position
/// jumping from bottom to top. Native-interface support is reflected in the glass
/// pane footer ("native: yes/no") so iOS regressions surface as a real counter
/// label change rather than a stub-vs-real branch.
public class StatusBarTapDiagnosticScreenshotTest extends BaseTest {
    private static final int TAPS_TO_FIRE = 3;
    private static final int TILE_COUNT = 16;
    private static final int GLASS_PANE_HEIGHT = 200;
    private static final int LABEL_PADDING = 12;

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
        ScrollContainer() {
            super(BoxLayout.y());
            setScrollableY(true);
            // Synchronous scroll so each frame captures the post-scroll state
            // without waiting for the Motion-driven smooth-scroll animation.
            setSmoothScrolling(false);
        }

        void scrollTo(int y) {
            setScrollY(y);
        }
    }

    @Override
    public boolean runTest() throws Exception {
        StatusBarTapDiagnosticNative nativeInterface = NativeLookup.create(StatusBarTapDiagnosticNative.class);
        boolean nativeSupported = nativeInterface != null && nativeInterface.isSupported();
        int displayWidth = Display.getInstance().getDisplayWidth();
        int displayHeight = Display.getInstance().getDisplayHeight();

        TestForm form = new TestForm("Status Bar Tap Diagnostic");
        form.setLayout(new BorderLayout());
        form.setWidth(displayWidth);
        form.setHeight(displayHeight);
        form.setVisible(true);

        ScrollContainer scrollContainer = new ScrollContainer();
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
        form.add(BorderLayout.CENTER, scrollContainer);
        form.layoutContainer();

        int contentHeight = scrollContainer.getScrollDimension().getHeight();
        int maxScroll = Math.max(0, contentHeight - scrollContainer.getHeight());

        // Capture the "before" frame: scrolled to the bottom, counter = 0.
        scrollContainer.scrollTo(maxScroll);
        Image beforeFrame = paintFrame(form, displayWidth, displayHeight, 0, "Before tapping", "scroll: bottom", nativeSupported);

        // Fire taps through the native interface; on iOS this drives the real
        // cn1FireStatusBarTap() path (counter++ + synthesized pointer event).
        // Other platforms either stub or dispatch through Form.pointerPressed.
        // Either way, we explicitly snap the visible scroll state to (0) for
        // the screenshot below so the result is deterministic across platforms.
        for (int i = 0; i < TAPS_TO_FIRE; i++) {
            if (nativeSupported) {
                nativeInterface.simulateStatusBarTap();
            }
        }
        scrollContainer.scrollTo(0);
        Image afterFrame = paintFrame(form, displayWidth, displayHeight, TAPS_TO_FIRE, "After " + TAPS_TO_FIRE + " taps", "scroll: top", nativeSupported);

        // Compose side-by-side: before on the left, after on the right.
        Image composite = Image.createImage(displayWidth, displayHeight, 0xff101010);
        Graphics cg = composite.getGraphics();
        int halfWidth = displayWidth / 2;
        Image scaledBefore = beforeFrame.scaled(halfWidth, displayHeight);
        Image scaledAfter = afterFrame.scaled(displayWidth - halfWidth, displayHeight);
        cg.drawImage(scaledBefore, 0, 0);
        cg.drawImage(scaledAfter, halfWidth, 0);
        cg.setColor(0x303030);
        cg.drawLine(halfWidth, 0, halfWidth, displayHeight - 1);
        scaledBefore.dispose();
        scaledAfter.dispose();
        beforeFrame.dispose();
        afterFrame.dispose();

        Cn1ssDeviceRunnerHelper.emitImage(composite, "StatusBarTapDiagnosticScreenshotTest", this::done);
        return true;
    }

    private Image paintFrame(Form form, int width, int height, int counter, String headline, String scrollLabel, boolean nativeSupported) {
        Image frame = Image.createImage(width, height, 0xffffffff);
        Graphics g = frame.getGraphics();
        form.paintComponent(g, true);
        paintGlassPane(g, width, counter, headline, scrollLabel, nativeSupported);
        return frame;
    }

    private void paintGlassPane(Graphics g, int width, int counter, String headline, String scrollLabel, boolean nativeSupported) {
        g.setAlpha(195);
        g.setColor(0x000000);
        g.fillRect(0, 0, width, GLASS_PANE_HEIGHT);
        g.setAlpha(255);

        int lineH = g.getFont().getHeight() + 4;
        int y = LABEL_PADDING;

        g.setColor(0xffffff);
        g.drawString(headline, LABEL_PADDING, y);
        y += lineH;

        g.setColor(0xffd166);
        g.drawString("Counter: " + counter, LABEL_PADDING, y);
        y += lineH;

        g.setColor(0x9bf6ff);
        g.drawString(scrollLabel, LABEL_PADDING, y);
        y += lineH;

        g.setColor(0xa0a0a0);
        g.drawString("native: " + (nativeSupported ? "yes" : "no"), LABEL_PADDING, y);
    }

    private static int rowColor(int i) {
        int[] palette = {0x118ab2, 0x06d6a0, 0xffd166, 0xef476f, 0x8338ec, 0xfb5607};
        return palette[i % palette.length];
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return true;
    }
}
