package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.DeviceRunner;
import com.codename1.testing.TestReporting;
import com.codename1.ui.Display;
import com.codename1.ui.Form;

public final class Cn1ssDeviceRunner extends DeviceRunner {
    private static final String[] TEST_CLASSES = new String[] {
            MainScreenScreenshotTest.class.getName(),
            BrowserComponentScreenshotTest.class.getName()
    };

    public void runSuite() {
        for (String testClass : TEST_CLASSES) {
            runTest(testClass);
        }
        TestReporting.getInstance().testExecutionFinished(getClass().getName());
    }

    @Override
    protected void startApplicationInstance() {
        Display.getInstance().callSeriallyAndWait(() -> {
            Form current = Display.getInstance().getCurrent();
            if (current != null) {
                current.revalidate();
            } else {
                new Form().show();
            }
        });
        Cn1ssDeviceRunnerHelper.waitForMillis(200);
    }

    @Override
    protected void stopApplicationInstance() {
        Display.getInstance().callSeriallyAndWait(() -> {
            Form current = Display.getInstance().getCurrent();
            if (current != null) {
                current.removeAll();
                current.revalidate();
            }
        });
        Cn1ssDeviceRunnerHelper.waitForMillis(200);
    }
}
