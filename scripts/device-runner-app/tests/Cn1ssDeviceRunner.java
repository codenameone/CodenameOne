package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.DeviceRunner;
import com.codename1.testing.TestReporting;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.testing.AbstractTest;

public final class Cn1ssDeviceRunner extends DeviceRunner {
    private static final AbstractTest[] TEST_CLASSES = new AbstractTest[] {
            new MainScreenScreenshotTest(),
            new GraphicsPipelineScreenshotTest(),
            new GraphicsShapesAndGradientsScreenshotTest(),
            new GraphicsStateAndTextScreenshotTest(),
            new GraphicsTransformationsScreenshotTest(),
            new GraphicsMethodsScreenshotTest(),
            new BrowserComponentScreenshotTest(),
            new MediaPlaybackScreenshotTest()
    };

    public void runSuite() {
        for (AbstractTest testClass : TEST_CLASSES) {
            log("CN1SS:INFO:suite starting test=" + testClass);
            try {
                testClass.prepare();
                testClass.runTest();
                testClass.cleanup();
                log("CN1SS:INFO:suite finished test=" + testClass);
            } catch (Throwable t) {
                log("CN1SS:ERR:suite test=" + testClass + " failed=" + t);
                t.printStackTrace();
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
