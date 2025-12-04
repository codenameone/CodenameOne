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
import com.codename1.util.StringUtil;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawArc;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawLine;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawRect;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawRoundRect;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawString;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawStringDecorated;
import com.codenameone.examples.hellocodenameone.tests.graphics.FillArc;
import com.codenameone.examples.hellocodenameone.tests.graphics.FillRect;
import com.codenameone.examples.hellocodenameone.tests.graphics.FillRoundRect;

import java.util.List;

public final class Cn1ssDeviceRunner extends DeviceRunner {
    private static final BaseTest[] TEST_CLASSES = new BaseTest[] {
            new MainScreenScreenshotTest(),
            new DrawLine(),
            new FillRect(),
            new DrawRect(),
            new FillRoundRect(),
            new DrawRoundRect(),
            new FillArc(),
            new DrawArc(),
            new DrawString(),
            new DrawStringDecorated(),
            new BrowserComponentScreenshotTest(),
            new MediaPlaybackScreenshotTest()
    };

    public void runSuite() {
        CN.callSerially(() -> {
            Display.getInstance().addEdtErrorHandler(e -> {
                log("CN1SS:ERR:exception caught in EDT " + e.getSource());
                String stack = Display.getInstance().getStackTrace(Thread.currentThread(), (Throwable)e.getSource());
                log("CN1SS:ERR:exception stack of length: " + stack.length());
                for(String s : StringUtil.tokenize(stack, '\n')) {
                    if(s.length() > 200) {
                        s = s.substring(0, 200);
                    }
                    log("CN1SS:ERR:Stack:" + s);
                }
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
        if (CN.isSimulator()) {
            Display.getInstance().exitApplication();
        }
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
