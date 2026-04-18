package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.AbstractTest;
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
                registerReadyCallback(this, () -> {
                    Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(imageName, BaseTest.this::done);
                });
            }
        };
    }

    protected void registerReadyCallback(Form parent, Runnable run) {
        // Android misses some images when the time is lower
        int delay = 1500;
        if ("HTML5".equals(com.codename1.ui.Display.getInstance().getPlatformName())
                && parent != null
                && "graphics-draw-image-rect".equals(parent.getTitle())) {
            delay = 4000;
            System.out.println("CN1JS:BaseTest.delayOverride title=" + parent.getTitle() + " delayMs=" + delay);
        }
        UITimer.timer(delay, false, parent, run);
    }

    protected synchronized void done() {
        this.done = true;
    }

    public synchronized boolean isDone() {
        return done;
    }
}
