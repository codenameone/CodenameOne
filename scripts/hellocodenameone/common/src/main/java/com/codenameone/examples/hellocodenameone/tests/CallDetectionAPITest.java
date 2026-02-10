package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Display;

public class CallDetectionAPITest extends BaseTest {
    @Override
    public boolean runTest() {
        try {
            Display display = Display.getInstance();
            display.isCallDetectionSupported();
            display.isInCall();
            done();
        } catch (Throwable t) {
            fail("Call detection API invocation failed: " + t);
        }
        return true;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
