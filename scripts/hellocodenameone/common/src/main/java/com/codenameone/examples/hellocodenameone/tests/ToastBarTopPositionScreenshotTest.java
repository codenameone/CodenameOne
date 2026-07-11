package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.ToastBar;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;

/**
 * Screenshot test for ToastBar positioned at {@link Component#TOP}.
 *
 * <p>This verifies the fix for the issue where {@code ToastBar} with
 * {@code setPosition(Component.TOP)} rendered spurious empty space above
 * the message text because the safe-area inset was double-counted when
 * the layered-pane parent was already below the safe-area boundary.</p>
 */
public class ToastBarTopPositionScreenshotTest extends BaseTest {
    private Form form;
    private int originalPosition;
    private ToastBar.Status status;

    @Override
    public boolean runTest() {
        originalPosition = ToastBar.getInstance().getPosition();

        form = createForm("ToastBar Top", new BorderLayout(), "ToastBarTopPosition");

        Container content = new Container(BoxLayout.y());
        content.add(new Label("ToastBar at TOP position"));
        content.add(new Label("No empty space should appear above the toast"));
        form.add(BorderLayout.CENTER, content);

        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        ToastBar.getInstance().setPosition(Component.TOP);
        showToast();

        // The toast is shown asynchronously and, on the slow iOS/tvOS/watchOS simulators, is
        // intermittently NOT visible when a fixed timer fires -- the screenshot then captures
        // the form WITHOUT the toast, a run-to-run "toast present vs absent" flake that no
        // golden can reconcile (max_channel_delta 191). Rather than guess a timer, poll until
        // the toast component is actually laid out + visible on the current form, re-issuing
        // the show if it never took, then capture only after a screenshot readback contains
        // the rendered top bar. Bounded so a genuine show failure degrades to a capture
        // instead of hanging the suite.
        awaitToastShown(parent, 0);
    }

    private void showToast() {
        // Keep the returned Status and give it a practically-unexpiring timeout:
        // on a starved metal runner the poll + settle + capture chain was observed
        // outliving the old 30s expiry, so the capture shipped the form with the
        // toast ALREADY DISMISSED ("toast absent" all over again, this time by
        // timeout rather than by late show). Rendering is identical to before
        // (same showMessage path the golden was captured with); the status is
        // cleared explicitly in cleanup() so it can never leak into a later
        // test's capture.
        if (status != null) {
            status.clear();
        }
        status = ToastBar.showMessage("Info message at top", FontImage.MATERIAL_INFO,
                10 * 60 * 1000);
    }

    /// The toast animates in on the GLOBAL layered pane, which the base settle
    /// poll does not track, and the metal backend can present the frame a beat
    /// late -- force a fresh, fully-presented frame before capturing (same
    /// mitigation as DesktopMode / the VR tests).
    @Override
    protected long extraSettleBeforeCaptureMillis() {
        return 700;
    }

    /// Invoked by the runner after the test completes (pass or fail): dismiss the
    /// unexpiring toast and restore the global ToastBar position so no state leaks
    /// into subsequent tests.
    @Override
    public void cleanup() {
        if (status != null) {
            status.clear();
            status = null;
        }
        ToastBar.getInstance().setPosition(originalPosition);
    }

    /// Consecutive polls in which the toast's geometry (y + height) was
    /// identical. visible && height>0 alone is NOT enough: the toast slides in
    /// on the GLOBAL layered pane, which the form settle poll does not track,
    /// and the slow watch simulator has been captured MID-SLIDE (a sliver of
    /// toast peeking from the top edge). Require the geometry stable across
    /// several polls so the animation has actually landed.
    private int stableGeometryPolls;
    private int lastToastY = Integer.MIN_VALUE;
    private int lastToastH = Integer.MIN_VALUE;

    private void awaitToastShown(final Form parent, final int waitedMs) {
        Form f = Display.getInstance().getCurrent();
        Object tbc = (f != null) ? f.getClientProperty("ToastBarComponent") : null;
        boolean shown = isToastComponentShown(tbc);
        if (shown) {
            Component c = (Component) tbc;
            if (c.getY() == lastToastY && c.getHeight() == lastToastH) {
                stableGeometryPolls++;
            } else {
                stableGeometryPolls = 0;
                lastToastY = c.getY();
                lastToastH = c.getHeight();
            }
        } else {
            stableGeometryPolls = 0;
            lastToastY = Integer.MIN_VALUE;
            lastToastH = Integer.MIN_VALUE;
        }
        if ((shown && stableGeometryPolls >= 3) || waitedMs >= 15000) {
            awaitRenderedToastFrame(parent, 0);
            return;
        }
        if (!shown && waitedMs > 0 && (waitedMs % 3000) == 0) {
            // the show did not take (toast left at height 0) -> re-issue it
            showToast();
        }
        UITimer.timer(250, false, parent, () -> awaitToastShown(parent, waitedMs + 250));
    }

    private void awaitRenderedToastFrame(final Form parent, final int waitedMs) {
        if (captureBlockedByOrientation("ToastBarTopPosition", waitedMs)) {
            UITimer.timer(250, false, parent, () -> awaitRenderedToastFrame(parent, waitedMs + 250));
            return;
        }

        Form f = Display.getInstance().getCurrent();
        Object tbc = (f != null) ? f.getClientProperty("ToastBarComponent") : null;
        if (tbc instanceof Component) {
            Component c = (Component) tbc;
            c.repaint();
            if (c.getParent() != null) {
                c.getParent().revalidate();
                c.getParent().repaint();
            }
        }
        parent.repaint();

        markCaptureStarted();
        Display.getInstance().screenshot(screen -> {
            if (screen == null) {
                Cn1ssDeviceRunnerHelper.emitPlaceholderScreenshot("ToastBarTopPosition");
                done();
                return;
            }
            boolean rendered = containsRenderedToastBar(screen);
            if (rendered || waitedMs >= 12000) {
                if (!rendered) {
                    System.out.println("CN1SS:WARN:test=ToastBarTopPosition rendered toast not detected at capture bound");
                }
                Cn1ssDeviceRunnerHelper.emitImage(screen, "ToastBarTopPosition", this::done);
                return;
            }
            screen.dispose();
            if (waitedMs > 0 && (waitedMs % 3000) == 0 && !isToastComponentShown(tbc)) {
                showToast();
                awaitToastShown(parent, 0);
                return;
            }
            UITimer.timer(250, false, parent, () -> awaitRenderedToastFrame(parent, waitedMs + 250));
        });
    }

    private boolean isToastComponentShown(Object tbc) {
        return (tbc instanceof Component)
                && ((Component) tbc).isVisible()
                && ((Component) tbc).getHeight() > 0;
    }

    private boolean containsRenderedToastBar(Image screen) {
        int width = screen.getWidth();
        int height = screen.getHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }
        int[] rgb = screen.getRGB();
        int maxY = Math.min(height, Math.max(1, height / 2));
        int requiredDarkPixels = Math.max(1, (width * 3) / 5);
        // macOS native text antialiasing leaves relatively few pure-white
        // pixels in the toast text/icon, even in the committed golden.
        int requiredBrightPixels = Math.max(16, width / 3);
        int bestBandHeight = 0;
        int bestBandBrightPixels = 0;
        int currentBandHeight = 0;
        int currentBandBrightPixels = 0;
        for (int y = 0; y < maxY; y++) {
            int darkPixels = 0;
            int brightPixels = 0;
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                int color = rgb[rowOffset + x];
                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;
                if (r < 96 && g < 96 && b < 96) {
                    darkPixels++;
                }
                if (r > 180 && g > 180 && b > 180) {
                    brightPixels++;
                }
            }
            if (darkPixels >= requiredDarkPixels) {
                currentBandHeight++;
                currentBandBrightPixels += brightPixels;
            } else {
                if (currentBandHeight > bestBandHeight) {
                    bestBandHeight = currentBandHeight;
                    bestBandBrightPixels = currentBandBrightPixels;
                }
                currentBandHeight = 0;
                currentBandBrightPixels = 0;
            }
        }
        if (currentBandHeight > bestBandHeight) {
            bestBandHeight = currentBandHeight;
            bestBandBrightPixels = currentBandBrightPixels;
        }
        return bestBandHeight > 0 && bestBandBrightPixels >= requiredBrightPixels;
    }
}
