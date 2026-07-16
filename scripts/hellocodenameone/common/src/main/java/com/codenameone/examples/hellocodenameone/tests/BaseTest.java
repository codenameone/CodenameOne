package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.AnimationManager;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.util.UITimer;
import com.codename1.ui.layouts.Layout;
import com.codename1.testing.TestUtils;

public abstract class BaseTest extends AbstractTest {
    /// The device orientation (portrait-ness) observed at the suite's FIRST
    /// capture -- the orientation every subsequent screenshot baseline was
    /// recorded in. Phone suites start portrait; desktop/tv suites record
    /// their fixed landscape aspect and the guard below never engages.
    private static Boolean suiteBaselinePortrait;
    private volatile boolean done;
    private volatile boolean failed;
    private volatile String failMessage;
    /// Set the moment the capture pipeline hands off to
    /// Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot. The runner consults
    /// it on a timeout: false means the test died silently BEFORE any capture
    /// was requested (the iOS Metal flake where a transient render-pipeline
    /// stall swallows the whole show -> UITimer -> screenshot chain without a
    /// single error line) and a one-shot retry is safe; true means a capture
    /// is in flight and re-running would risk a duplicate/mislabelled emit.
    private volatile boolean captureStarted;
    /// Last stage the screenshot pipeline reached, logged by the runner when
    /// a test times out so a silent stall pinpoints itself: created ->
    /// show-completed -> settle-timer-fired -> capture-requested.
    private volatile String captureStage = "created";

    public boolean shouldTakeScreenshot() {
        return true;
    }

    public synchronized void fail(String message) {
        this.failed = true;
        this.failMessage = message;
        done();
    }

    public synchronized boolean isFailed() {
        return failed;
    }

    public synchronized String getFailMessage() {
        return failMessage;
    }

    protected Form createForm(String title, Layout layout, final String imageName) {
        return new Form(title, layout) {
            @Override
            protected void onShowCompleted() {
                captureStage = "show-completed";
                registerReadyCallback(this, () -> awaitAnimationsThenScreenshot(this, imageName));
            }
        };
    }

    protected void registerReadyCallback(Form parent, Runnable run) {
        // Android misses some images when the time is lower
        UITimer.timer(1500, false, parent, run);
    }

    /// Wraps a component so a callback can run after its subtree has actually PAINTED, a
    /// deterministic "content is on screen" signal. Event-driven components (the pure editors
    /// report ready as soon as their backend initializes, typically before the first paint
    /// flushes on the slower ports) capture too early with a plain ready callback and too
    /// flakily with fixed settle timers; gating on the first real paint is exact on every port.
    protected static final class FirstPaintGate extends com.codename1.ui.Container {
        private Runnable pending;

        public FirstPaintGate(com.codename1.ui.Component content) {
            super(new com.codename1.ui.layouts.BorderLayout());
            add(com.codename1.ui.layouts.BorderLayout.CENTER, content);
        }

        /// Schedules {@code r} (serially on the EDT) after the next completed paint of this
        /// container's subtree.
        public void runAfterNextPaint(Runnable r) {
            pending = r;
            repaint();
        }

        @Override
        public void paint(com.codename1.ui.Graphics g) {
            super.paint(g);
            if (pending != null) {
                Runnable r = pending;
                pending = null;
                com.codename1.ui.CN.callSerially(r);
            }
        }
    }

    /// After the initial 1500ms settle, poll the form's AnimationManager until
    /// it reports no in-flight animations, then take the screenshot. Guarded
    /// by a max-wait so a runaway animation can't deadlock the suite.
    private void awaitAnimationsThenScreenshot(Form form, String imageName) {
        awaitSettledThenCapture(form, imageName, 0, this::done);
    }

    /// Capture {@code form} as {@code imageName} once it settles, then invoke
    /// {@code onComplete} (instead of the default done()). Lets a single test
    /// chain several screenshots - e.g. a watch-form-factor test that emits one
    /// full-screen capture per variant rather than a single multi-tile grid.
    protected void captureWhenSettled(Form form, String imageName, Runnable onComplete) {
        awaitSettledThenCapture(form, imageName, 0, onComplete);
    }

    private void awaitSettledThenCapture(final Form form, final String imageName, final int waitedMs,
                                         final Runnable onComplete) {
        if (waitedMs == 0) {
            captureStage = "settle-timer-fired";
        }
        AnimationManager am = form.getAnimationManager();
        // Do not capture until the form we are meant to shoot is actually the current form.
        // On the slow watchOS/tvOS simulators a form switch can lag onShowCompleted, so
        // Display.screenshot() would grab the PREVIOUS test's form (observed: css-gradients
        // capturing PaletteOverrideTheme_dark -> a "duplicate_image_with" wrong-form flake).
        boolean wrongForm = Display.getInstance().getCurrent() != form;
        // A leaked landscape orientation is "not settled": wait for the
        // re-asserted portrait lock to land (its own, longer budget -- a
        // starved simulator rotation can exceed the 5s animation cap).
        boolean wrongOrientation = captureBlockedByOrientation(imageName, waitedMs);
        boolean animating = wrongForm
                || (am != null && am.isAnimating())
                || Display.getInstance().isInTransition();
        int waitCapMs = wrongOrientation ? 15000 : 5000;
        if ((!animating && !wrongOrientation) || waitedMs >= waitCapMs) {
            long extra = extraSettleBeforeCaptureMillis();
            if (extra > 0) {
                // Heavy forms on the iOS Metal backend can have their first
                // frame presented a beat after onShowCompleted + the animation
                // settle, so Display.screenshot() reads the PREVIOUS form's
                // still-current framebuffer -- the "DesktopMode captures the
                // wrong form" race. Force a fresh paint and give the GPU a
                // moment to present it before capturing. Opt-in per test
                // (default 0) so no other baseline shifts.
                form.repaint();
                UITimer.timer((int) extra, false, form, () -> {
                    markCaptureStarted();
                    Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(imageName, onComplete);
                });
                return;
            }
            markCaptureStarted();
            Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(imageName, onComplete);
            return;
        }
        UITimer.timer(50, false, form, () -> awaitSettledThenCapture(form, imageName, waitedMs + 50, onComplete));
    }

    /// Extra delay (ms) inserted AFTER the form has settled and BEFORE the
    /// screenshot, during which the form is repainted. Defaults to 0 (capture
    /// immediately, unchanged behaviour). A test whose heavy form trips the iOS
    /// Metal late-present race -- the screenshot grabbing the previous form's
    /// framebuffer -- overrides this to force a fresh, fully-presented frame.
    protected long extraSettleBeforeCaptureMillis() {
        return 0;
    }

    /// Tests that INTENTIONALLY capture in a non-baseline orientation (the
    /// landscape VR/360 tests) override this to true so the orientation guard
    /// below does not fight their deliberate rotation.
    protected boolean allowNonBaselineOrientationCapture() {
        return false;
    }

    /// Orientation leak guard. An orientation-changing test whose
    /// restore-to-portrait silently times out (observed: OrientationLock on a
    /// starved iOS Metal runner) leaves the simulator landscape for every test
    /// after it -- the next capture ships sideways pixels (VideoIODecodedFrames
    /// delivered 2556x1179 against a portrait golden) and orientation-sensitive
    /// tests wedge. Returns true while the device is NOT in the suite's
    /// baseline orientation, and actively re-asserts the portrait lock (an
    /// earlier lock issued mid-rotation-animation can be swallowed), so the
    /// suite self-heals instead of failing on the innocent downstream test.
    /// Callers poll this from their capture path and log a CN1SS:WARN so a
    /// genuine restore failure attributes to the right place.
    protected final boolean captureBlockedByOrientation(String imageName, int waitedMs) {
        if (suiteBaselinePortrait == null) {
            suiteBaselinePortrait = Boolean.valueOf(CN.isPortrait());
        }
        if (!suiteBaselinePortrait.booleanValue()
                || !CN.canForceOrientation()
                || allowNonBaselineOrientationCapture()
                || CN.isPortrait()) {
            return false;
        }
        // Re-assert the portrait lock about once a second while blocked; the
        // 50ms/250ms pollers both hit the ==0 first pass immediately.
        if (waitedMs % 1000 == 0) {
            System.out.println("CN1SS:WARN:test=" + imageName
                    + " capture blocked: device left in landscape by an earlier test;"
                    + " re-asserting portrait lock (waited " + waitedMs + "ms)");
            CN.lockOrientation(true);
        }
        return true;
    }

    protected synchronized void done() {
        this.done = true;
    }

    public synchronized boolean isDone() {
        return done;
    }

    public boolean isCaptureStarted() {
        return captureStarted;
    }

    /// Tests that bypass createForm()'s capture chain and invoke
    /// Cn1ssDeviceRunnerHelper.emit* directly must call this right before the
    /// emit so the runner's silent-timeout retry never re-runs a test whose
    /// capture is already in flight (a late emit after the rerun's form is up
    /// would ship the wrong pixels under this test's name).
    protected void markCaptureStarted() {
        captureStarted = true;
        captureStage = "capture-requested";
    }

    public String getCaptureStage() {
        return captureStage;
    }

    /// Re-arms the instance so the runner can re-invoke prepare()/runTest()
    /// after a silent timeout (timeout with no capture started). Only the
    /// harness flags are reset; subclasses create a fresh Form per runTest()
    /// call so no UI state needs unwinding here.
    public synchronized void resetForRetry() {
        done = false;
        failed = false;
        failMessage = null;
        captureStarted = false;
        captureStage = "retry-created";
    }
}
