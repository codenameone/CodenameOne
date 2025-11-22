package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Log;
import com.codename1.testing.AbstractTest;
import com.codename1.ui.Form;
import com.codename1.ui.util.UITimer;
import com.codename1.ui.layouts.Layout;
import com.codename1.testing.TestUtils;

public abstract class BaseTest extends AbstractTest {
    private boolean done;
    private String currentScreenshotName = "default";

    protected Form createForm(String title, Layout layout, final String imageName) {
        currentScreenshotName = Cn1ssDeviceRunnerHelper.sanitizeTestName(imageName);
        return new Form(title, layout) {
            @Override
            protected void onShowCompleted() {
                Log.p("CN1SS: form ready for screenshot -> " + imageName);
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

    protected boolean waitForDone() {
        int timeout = 100;
        while(!done) {
            TestUtils.waitFor(20);
            timeout--;
            if(timeout == 0) {
                Log.p("CN1SS: timeout waiting for screenshot emission");
                Cn1ssDeviceRunnerHelper.emitLogChannel(currentScreenshotName);
                return false;
            }
        }
        // give the test a few additional milliseconds for the screenshot emission
        TestUtils.waitFor(100);
        Log.p("CN1SS: screenshot emission completed");
        return true;
    }
}