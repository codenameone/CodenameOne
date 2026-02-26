package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;

public class OrientationLockScreenshotTest extends BaseTest {
    private static final int ORIENTATION_POLL_INTERVAL_MS = 100;
    private static final int ORIENTATION_POLL_ATTEMPTS = 25;

    @Override
    public boolean runTest() {
        Form hi = new Form("Orientation Lock", new BoxLayout(BoxLayout.Y_AXIS)) {
            @Override
            protected void onShowCompleted() {
                CN.lockOrientation(false);
                waitForOrientation(this, false, () -> {
                    Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot("landscape");
                    CN.lockOrientation(true);
                    waitForOrientation(this, true, OrientationLockScreenshotTest.this::done);
                });
            }
        };
        hi.add(new Label("Testing orientation lock..."));
        hi.show();
        return true;
    }
    
    private void waitForOrientation(Form form, boolean portrait, Runnable onDone) {
        waitForOrientation(form, portrait, ORIENTATION_POLL_ATTEMPTS, onDone);
    }

    private void waitForOrientation(Form form, boolean portrait, int attemptsLeft, Runnable onDone) {
        if (CN.isPortrait() == portrait || attemptsLeft <= 0) {
            onDone.run();
            return;
        }
        UITimer.timer(ORIENTATION_POLL_INTERVAL_MS, false, form, () -> waitForOrientation(form, portrait, attemptsLeft - 1, onDone));
    }
}
