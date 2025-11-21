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
        UITimer.timer(500, false, parent, run);
    }

    protected boolean waitForDone() {
        int timeout = 100;
        while(!done) {
            TestUtils.waitFor(20);
            timeout--;
            if(timeout == 0) {
                return false;
            }
        }
        return true;
    }
}