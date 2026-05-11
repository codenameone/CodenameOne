package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;

public class OrientationLockScreenshotTest extends BaseTest {
    private static final int ORIENTATION_POLL_INTERVAL_MS = 100;
    private static final int ORIENTATION_POLL_ATTEMPTS = 25;

    @Override
    public boolean runTest() {
        if ("HTML5".equals(Display.getInstance().getPlatformName())) {
            // Browsers expose only Screen Orientation API, which requires fullscreen
            // in a worker-hosted Codename One app the port cannot satisfy. Skip with
            // no screenshot rather than producing a broken capture.
            System.out.println("CN1SS:INFO:test=OrientationLockScreenshotTest status=SKIPPED reason=platform-unsupported");
            done();
            return true;
        }
        Form hi = new Form("Orientation Lock", new BoxLayout(BoxLayout.Y_AXIS)) {
            @Override
            protected void onShowCompleted() {
                CN.lockOrientation(false);
                waitForOrientation(this, false, () -> {
                    // The simulator rotation animation can land in slightly
                    // different sub-pixel positions if we capture the form too
                    // early after CN.isPortrait() flips. Mirror BaseTest's
                    // 1500ms readiness timer so the screenshot waits for the
                    // post-rotation layout pass to settle and revalidate the
                    // form layout once before snapping; otherwise this test
                    // produces 4-7% AA-only diffs run-over-run.
                    revalidate();
                    UITimer.timer(1500, false, this, () -> {
                        revalidate();
                        Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot("landscape", () -> {
                            CN.lockOrientation(true);
                            waitForOrientation(this, true, OrientationLockScreenshotTest.this::done);
                        });
                    });
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
