package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.ToastBar;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
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
        // the show if it never took, then hand off to the base settle+capture. Bounded so a
        // genuine show failure degrades to a capture instead of hanging the suite.
        awaitToastShown(parent, run, 0);
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

    private void awaitToastShown(final Form parent, final Runnable run, final int waitedMs) {
        Form f = Display.getInstance().getCurrent();
        Object tbc = (f != null) ? f.getClientProperty("ToastBarComponent") : null;
        boolean shown = (tbc instanceof Component)
                && ((Component) tbc).isVisible()
                && ((Component) tbc).getHeight() > 0;
        if (shown || waitedMs >= 15000) {
            run.run();
            return;
        }
        if (waitedMs > 0 && (waitedMs % 3000) == 0) {
            // the show did not take (toast left at height 0) -> re-issue it
            showToast();
        }
        UITimer.timer(250, false, parent, () -> awaitToastShown(parent, run, waitedMs + 250));
    }
}
