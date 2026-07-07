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
        // 30s timeout so the toast stays up for the (polled) capture window.
        ToastBar.showMessage("Info message at top", FontImage.MATERIAL_INFO, 30000);
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
