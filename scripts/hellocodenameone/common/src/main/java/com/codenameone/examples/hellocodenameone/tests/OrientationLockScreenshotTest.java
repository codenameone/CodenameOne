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
    // The restore-to-portrait leg gets a LONGER budget than the capture leg
    // (200 * 100ms = 20s) and re-asserts the portrait lock every ~2s: if this
    // poll gives up while the simulator is still landscape, every later test
    // inherits the sideways screen -- VideoIODecodedFrames ships a 2556x1179
    // grid against a portrait golden and the orientation-sensitive tail tests
    // wedge (the recurring iOS Metal suite failure). A lock issued while the
    // previous rotation animation is still in flight can be swallowed, so a
    // single lockOrientation(true) call is not enough on a starved runner.
    private static final int RESTORE_POLL_ATTEMPTS = 200;
    private static final int RESTORE_RELOCK_EVERY = 20;
    private static final int POST_PORTRAIT_SETTLE_MS = 1000;

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
                            restorePortrait(this, RESTORE_POLL_ATTEMPTS, () -> {
                                revalidate();
                                UITimer.timer(POST_PORTRAIT_SETTLE_MS, false, this,
                                        OrientationLockScreenshotTest.this::done);
                            });
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
        form.revalidate();
        if (isOrientationSettled(form, portrait) || attemptsLeft <= 0) {
            onDone.run();
            return;
        }
        UITimer.timer(ORIENTATION_POLL_INTERVAL_MS, false, form, () -> waitForOrientation(form, portrait, attemptsLeft - 1, onDone));
    }

    /// Restore-to-portrait with teeth: re-asserts the portrait lock every
    /// RESTORE_RELOCK_EVERY polls (a lock call issued mid-rotation can be
    /// swallowed) and logs a CN1SS:WARN if the whole budget elapses while the
    /// device is still landscape, so a genuine restore failure attributes HERE
    /// instead of surfacing as a mysterious mismatch on whichever screenshot
    /// test happens to run next. Proceeds to onDone regardless: the per-test
    /// orientation guard in BaseTest is the downstream safety net.
    private void restorePortrait(Form form, int attemptsLeft, Runnable onDone) {
        // Fixed-orientation platforms (desktop, Linux/Mac windows, tvOS,
        // browser) never rotated in the first place and can NEVER satisfy the
        // portrait check (the window is landscape by construction), so polling
        // would burn the whole 20s budget for nothing -- and on the Linux
        // suite that silent 20s gap pushed the harness's no-new-screenshot
        // stability window over its limit, truncating the suite's tail
        // (DesktopMode/VRStereoScene/Media360Panorama went missing). Nothing
        // to restore: finish immediately.
        if (!CN.canForceOrientation()) {
            onDone.run();
            return;
        }
        form.revalidate();
        if (isOrientationSettled(form, true)) {
            onDone.run();
            return;
        }
        if (attemptsLeft <= 0) {
            System.out.println("CN1SS:WARN:test=OrientationLock restore-to-portrait timed out after "
                    + (RESTORE_POLL_ATTEMPTS * ORIENTATION_POLL_INTERVAL_MS)
                    + "ms; device may still be landscape");
            onDone.run();
            return;
        }
        if (attemptsLeft % RESTORE_RELOCK_EVERY == 0) {
            CN.lockOrientation(true);
        }
        UITimer.timer(ORIENTATION_POLL_INTERVAL_MS, false, form, () -> restorePortrait(form, attemptsLeft - 1, onDone));
    }

    private boolean isOrientationSettled(Form form, boolean portrait) {
        boolean dimensionsMatch = portrait
                ? form.getHeight() >= form.getWidth()
                : form.getWidth() > form.getHeight();
        return CN.isPortrait() == portrait && dimensionsMatch;
    }
}
