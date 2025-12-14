package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;

public class OrientationLockScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() throws Exception {
        Form hi = new Form("Orientation Lock", new BoxLayout(BoxLayout.Y_AXIS));
        hi.add(new Label("Testing orientation lock..."));

        hi.show();

        // Wait for show
        UITimer.timer(500, false, () -> {
            // Lock to Landscape
            CN.lockOrientation(false);

            UITimer.timer(1000, false, () -> {
                if (CN.getDisplayWidth() < CN.getDisplayHeight()) {
                    fail("Failed to lock to landscape. Width=" + CN.getDisplayWidth() + ", Height=" + CN.getDisplayHeight());
                    return;
                }
                Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot("landscape_locked");

                // Lock to Portrait for cleanup
                CN.lockOrientation(true);
                done();
            });
        });

        return !isFailed();
    }
}
