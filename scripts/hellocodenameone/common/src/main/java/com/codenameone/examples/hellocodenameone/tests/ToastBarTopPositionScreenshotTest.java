package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.ToastBar;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
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
        ToastBar tb = ToastBar.getInstance();
        tb.setPosition(Component.TOP);

        // Use a long timeout so the toast stays visible for the screenshot
        ToastBar.showMessage("Info message at top", FontImage.MATERIAL_INFO, 30000);

        // Wait for the toast slide-in to fully settle before capturing. The toast animates in
        // the global layered pane, which the base capture's form-AnimationManager settle poll
        // does NOT track, so the render is only deterministic once the slide has finished. The
        // previous 2000ms was enough on fast native platforms but not on the slow iOS/tvOS/
        // watchOS simulators, where the toast was captured mid-slide -> a delta-191 run-to-run
        // flake. 6000ms comfortably exceeds the slide duration even on the slowest sim; the
        // toast's 30000ms timeout keeps it up, so the captured frame is the settled toast.
        UITimer.timer(6000, false, parent, run);
    }
}
