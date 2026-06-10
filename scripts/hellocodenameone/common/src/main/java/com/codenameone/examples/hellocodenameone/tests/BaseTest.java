package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.AnimationManager;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.util.UITimer;
import com.codename1.ui.layouts.Layout;
import com.codename1.testing.TestUtils;

public abstract class BaseTest extends AbstractTest {
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

    /// After the initial 1500ms settle, poll the form's AnimationManager until
    /// it reports no in-flight animations, then take the screenshot. Guarded
    /// by a max-wait so a runaway animation can't deadlock the suite.
    private void awaitAnimationsThenScreenshot(Form form, String imageName) {
        awaitAnimationsThenScreenshot(form, imageName, 0);
    }

    private void awaitAnimationsThenScreenshot(final Form form, final String imageName, final int waitedMs) {
        if (waitedMs == 0) {
            captureStage = "settle-timer-fired";
        }
        AnimationManager am = form.getAnimationManager();
        boolean animating = (am != null && am.isAnimating())
                || Display.getInstance().isInTransition();
        if (!animating || waitedMs >= 5000) {
            markCaptureStarted();
            Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(imageName, BaseTest.this::done);
            return;
        }
        UITimer.timer(50, false, form, () -> awaitAnimationsThenScreenshot(form, imageName, waitedMs + 50));
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
