package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.impl.CodenameOneThread;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.testing.DeviceRunner;
import com.codename1.testing.TestReporting;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.testing.AbstractTest;

public final class Cn1ssDeviceRunner extends DeviceRunner {
    private static final BaseTest[] TEST_CLASSES = new BaseTest[] {
            new MainScreenScreenshotTest(),
            new GraphicsPipelineScreenshotTest(),
            new GraphicsShapesAndGradientsScreenshotTest(),
            new GraphicsStateAndTextScreenshotTest(),
            new GraphicsTransformationsScreenshotTest(),
            new BrowserComponentScreenshotTest(),
            new MediaPlaybackScreenshotTest()
    };

    public void runSuite() {
        CN.callSerially(() -> {
            Display.getInstance().addEdtErrorHandler(e -> {
                log("CN1SS:ERR:exception caught in EDT " + e.getSource());
                Thread thr = Thread.currentThread();
                if(thr instanceof CodenameOneThread && ((CodenameOneThread)thr).hasStackFrame()) {
                    log("CN1SS:ERR:exception stack: " + ((CodenameOneThread)thr).getStack((Throwable) e.getSource()));
                }
                Log.e((Throwable)e.getSource());
            });
        });
        for (BaseTest testClass : TEST_CLASSES) {
            CN.callSerially(() -> {
                log("CN1SS:INFO:suite starting test=" + testClass);
                try {
                    testClass.prepare();
                    testClass.runTest();
                } catch (Throwable t) {
                    log("CN1SS:ERR:suite test=" + testClass + " failed=" + t);
                    t.printStackTrace();
                }
            });
            int timeout = 9000;
            while (!testClass.isDone() && timeout > 0) {
                Util.sleep(3);
                timeout--;
            }
            testClass.cleanup();
            if(timeout == 0) {
                log("CN1SS:ERR:suite test=" + testClass + " failed due to timeout waiting for DONE");
            }
            log("CN1SS:INFO:suite finished test=" + testClass);
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
    }
}
