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
        AnimationManager am = form.getAnimationManager();
        boolean animating = (am != null && am.isAnimating())
                || Display.getInstance().isInTransition();
        if (!animating || waitedMs >= 5000) {
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
}
