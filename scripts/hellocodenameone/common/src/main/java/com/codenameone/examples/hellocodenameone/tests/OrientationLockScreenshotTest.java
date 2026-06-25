package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;

public class OrientationLockScreenshotTest extends BaseTest {
    private static final int ORIENTATION_POLL_INTERVAL_MS = 100;
    // Wait up to ~8s (not 2.5s) for the simulator rotation to actually land
    // before capturing. On starved CI runners the device rotation + layout pass
    // can take well over 2.5s; when the poll gave up early the test snapped the
    // still-portrait frame and labelled it "landscape", producing a guaranteed
    // mismatch against the landscape reference (the classic ~50% flake on this
    // test). 80 * 100ms = 8s still leaves comfortable headroom under the 30s
    // native test timeout even with the trailing wait-back-to-portrait poll.
    private static final int ORIENTATION_POLL_ATTEMPTS = 80;

    @Override
    public boolean runTest() {
        // The JS port cannot lock orientation (the Screen Orientation API needs
        // fullscreen, which a worker-hosted app can't request). CN.lockOrientation
        // is a harmless no-op there, so the form simply stays in its current
        // (portrait) position -- exactly like Mac Native, which has no orientation
        // changing either. We still run the test and capture the "landscape"
        // screenshot in the current position rather than skipping it.
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
                        markCaptureStarted();
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
