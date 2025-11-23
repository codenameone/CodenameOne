package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.DeviceRunner;
import com.codename1.testing.TestReporting;
import com.codename1.io.Log;
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
            new BrowserComponentScreenshotTest(),
            new MediaPlaybackScreenshotTest()
    };

    public void runSuite() {
        Log.p("CN1SS: starting device runner suite with " + TEST_CLASSES.length + " tests");
        for (AbstractTest testClass : TEST_CLASSES) {
            log("CN1SS:INFO:suite starting test=" + testClass);
            try {
                Log.p("CN1SS: preparing test " + testClass);
                testClass.prepare();
                testClass.runTest();
                Log.p("CN1SS: finished test " + testClass);
                log("CN1SS:INFO:suite finished test=" + testClass);
            } catch (Throwable t) {
                log("CN1SS:ERR:suite test=" + testClass + " failed=" + t);
                t.printStackTrace();
            } finally {
                if (testClass instanceof BaseTest) {
                    BaseTest base = (BaseTest) testClass;
                    base.ensureLogsFlushedOnExit();
                }
                try {
                    testClass.cleanup();
                } catch (Throwable t) {
                    log("CN1SS:ERR:suite cleanup failed for test=" + testClass + " err=" + t);
                    t.printStackTrace();
                }
            }
        }
        Log.p("CN1SS: device runner suite complete");
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
