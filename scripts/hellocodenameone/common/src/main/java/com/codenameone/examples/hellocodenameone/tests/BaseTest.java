package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Form;
import com.codename1.ui.util.UITimer;
import com.codename1.ui.layouts.Layout;
import com.codename1.testing.TestUtils;

public abstract class BaseTest extends AbstractTest {
    private boolean done;

    protected Form createForm(String title, Layout layout, final String imageName) {
        return new Form(title, layout) {
            @Override
            protected void onShowCompleted() {
                registerReadyCallback(this, () -> {
                    Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(imageName);
                    done = true;
                });
            }
        };
    }

    protected void registerReadyCallback(Form parent, Runnable run) {
        // Android misses some images when the time is lower
        UITimer.timer(1500, false, parent, run);
    }

    protected void done() {
        this.done = true;
    }

    public boolean isDone() {
        return done;
    }
}