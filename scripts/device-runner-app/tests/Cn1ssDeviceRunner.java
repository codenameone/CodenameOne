package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.DeviceRunner;
import com.codename1.testing.TestReporting;
import com.codename1.ui.Display;
import com.codename1.ui.Form;

public final class Cn1ssDeviceRunner extends DeviceRunner {
    private static final String[] TEST_CLASSES = new String[] {
            MainScreenScreenshotTest.class.getName(),
            BrowserComponentScreenshotTest.class.getName(),
            MediaPlaybackScreenshotTest.class.getName(),
            GraphicsPipelineScreenshotTest.class.getName(),
            GraphicsShapesAndGradientsScreenshotTest.class.getName(),
            GraphicsStateAndTextScreenshotTest.class.getName(),
            GraphicsTransformationsScreenshotTest.class.getName(),
            GraphicsMethodsScreenshotTest.class.getName()
    };

    public void runSuite() {
        for (String testClass : TEST_CLASSES) {
            log("CN1SS:INFO:suite starting test=" + testClass);
            try {
                runTest(testClass);
                log("CN1SS:INFO:suite finished test=" + testClass);
            } catch (Throwable t) {
                log("CN1SS:ERR:suite test=" + testClass + " failed=" + t);
                t.printStackTrace();
                // continue with next test instead of aborting
            }
        }
        log("CN1SS:SUITE:FINISHED");
        TestReporting.getInstance().testExecutionFinished(getClass().getName());
    }

    private static void log(String msg) {
        System.out.println(msg);
    }
    
    @Override
    protected void startApplicationInstance() {
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
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
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
            Form current = Display.getInstance().getCurrent();
            if (current != null) {
                current.removeAll();
                current.revalidate();
            }
        });
        Cn1ssDeviceRunnerHelper.waitForMillis(200);
    }
}
