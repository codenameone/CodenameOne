package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.DeviceRunner;
import com.codename1.testing.TestReporting;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.util.StringUtil;
import com.codenameone.examples.hellocodenameone.NativeInterfaceLanguageValidator;
import com.codenameone.examples.hellocodenameone.tests.graphics.AffineScale;
import com.codenameone.examples.hellocodenameone.tests.graphics.Clip;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawArc;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawGradient;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawImage;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawLine;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawRect;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawRoundRect;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawShape;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawString;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawStringDecorated;
import com.codenameone.examples.hellocodenameone.tests.graphics.FillArc;
import com.codenameone.examples.hellocodenameone.tests.graphics.FillPolygon;
import com.codenameone.examples.hellocodenameone.tests.graphics.FillRect;
import com.codenameone.examples.hellocodenameone.tests.graphics.FillRoundRect;
import com.codenameone.examples.hellocodenameone.tests.graphics.FillShape;
import com.codenameone.examples.hellocodenameone.tests.graphics.FillTriangle;
import com.codenameone.examples.hellocodenameone.tests.graphics.Rotate;
import com.codenameone.examples.hellocodenameone.tests.graphics.Scale;
import com.codenameone.examples.hellocodenameone.tests.graphics.StrokeTest;
import com.codenameone.examples.hellocodenameone.tests.graphics.TileImage;
import com.codenameone.examples.hellocodenameone.tests.graphics.TransformCamera;
import com.codenameone.examples.hellocodenameone.tests.graphics.TransformPerspective;
import com.codenameone.examples.hellocodenameone.tests.graphics.TransformRotation;
import com.codenameone.examples.hellocodenameone.tests.graphics.TransformTranslation;
import com.codenameone.examples.hellocodenameone.tests.accessibility.AccessibilityTest;

public final class Cn1ssDeviceRunner extends DeviceRunner {
    // Previously 30_000. In the JavaScript port each test's onShowCompleted -> UITimer
    // -> emitCurrentFormScreenshot -> done() chain typically completes in ~1500ms
    // (see BaseTest.registerReadyCallback). Tests that never reach done() shouldn't
    // block the whole suite for 30s each — the overall CI browser lifetime is only
    // ~150s, so even three stuck tests used to consume the entire budget and prevent
    // later tests from ever running. 10s is still 6× the normal budget which is plenty
    // of margin for the rare genuinely slow form; iOS/Android are unaffected (they use
    // their own deadline logic in their respective runners).
    private static final int TEST_TIMEOUT_MS = 10000;
    private static final int TEST_POLL_INTERVAL_MS = 50;

    private static final BaseTest[] DEFAULT_TEST_CLASSES = new BaseTest[]{
            new MainScreenScreenshotTest(),
            new DrawLine(),
            new FillRect(),
            new DrawRect(),
            new FillRoundRect(),
            new DrawRoundRect(),
            new FillArc(),
            new DrawArc(),
            new DrawString(),
            new DrawImage(),
            new DrawStringDecorated(),
            new DrawGradient(),
            new FillPolygon(),
            new AffineScale(),
            new Scale(),
            new FillTriangle(),
            new DrawShape(),
            new FillShape(),
            new StrokeTest(),
            new Clip(),
            new TileImage(),
            new Rotate(),
            new TransformTranslation(),
            new TransformRotation(),
            new TransformPerspective(),
            new TransformCamera(),
            new BrowserComponentScreenshotTest(),
            new MediaPlaybackScreenshotTest(),
            new SheetScreenshotTest(),
            new ImageViewerNavigationScreenshotTest(),
            new TabsScreenshotTest(),
            new TextAreaAlignmentScreenshotTest(),
            new ValidatorLightweightPickerScreenshotTest(),
            new LightweightPickerButtonsScreenshotTest(),
            new ToastBarTopPositionScreenshotTest(),
            // Keep this as the last screenshot test; orientation changes can leak into subsequent screenshots.
            new OrientationLockScreenshotTest(),
            new InPlaceEditViewTest(),
            new BytecodeTranslatorRegressionTest(),
            new SimdApiTest(),
            new StreamApiTest(),
            new TimeApiTest(),
            new Java17Tests(),
            new BackgroundThreadUiAccessTest(),
            new VPNDetectionAPITest(),
            new CallDetectionAPITest(),
            new LocalNotificationOverrideTest(),
            new Base64NativePerformanceTest(),
            new AccessibilityTest()
    };
    private static BaseTest prependedTest;

    public static void addTest(BaseTest test) {
        prependedTest = test;
    }

    public void runSuite() {
        CN.callSerially(() -> {
            Display.getInstance().addEdtErrorHandler(e -> {
                log("CN1SS:ERR:exception caught in EDT " + e.getSource());
                logThrowable("EDT", (Throwable)e.getSource());
            });
        });
        runNextTest(0);
    }

    private void runNextTest(int index) {
        int offset = prependedTest != null ? 1 : 0;
        int total = DEFAULT_TEST_CLASSES.length + offset;
        if (index >= total) {
            finishSuite();
            return;
        }
        BaseTest testClass = (offset == 1 && index == 0) ? prependedTest : DEFAULT_TEST_CLASSES[index - offset];
        String testName = testClass.getClass().getSimpleName();
        CN.callSerially(() -> {
            log("CN1SS:INFO:suite starting test=" + testName);
            if (shouldForceTimeoutInHtml5(testName)) {
                log("CN1SS:ERR:suite test=" + testName + " forced timeout (HTML5 fallback)");
                finalizeTest(index, testClass, testName, true);
                return;
            }
            try {
                testClass.prepare();
                testClass.runTest();
            } catch (Throwable t) {
                log("CN1SS:ERR:suite test=" + testName + " failed=" + t);
                t.printStackTrace();
                logThrowable("runTest:" + testName, t);
                testClass.fail(String.valueOf(t));
            }
            awaitTestCompletion(index, testClass, testName, System.currentTimeMillis() + TEST_TIMEOUT_MS);
        });
    }

    private boolean shouldForceTimeoutInHtml5(String testName) {
        String platformName = Display.getInstance().getPlatformName();
        if (!"HTML5".equals(platformName)) {
            return false;
        }
        return "MediaPlaybackScreenshotTest".equals(testName)
                || "BytecodeTranslatorRegressionTest".equals(testName)
                || "BackgroundThreadUiAccessTest".equals(testName)
                || "VPNDetectionAPITest".equals(testName)
                || "CallDetectionAPITest".equals(testName)
                || "LocalNotificationOverrideTest".equals(testName)
                || "Base64NativePerformanceTest".equals(testName)
                || "AccessibilityTest".equals(testName);
    }

    private void awaitTestCompletion(int index, BaseTest testClass, String testName, long deadline) {
        if (testClass.isDone()) {
            finalizeTest(index, testClass, testName, false);
            return;
        }
        if (System.currentTimeMillis() >= deadline) {
            finalizeTest(index, testClass, testName, true);
            return;
        }
        CN.setTimeout(TEST_POLL_INTERVAL_MS, () -> awaitTestCompletion(index, testClass, testName, deadline));
    }

    private void finalizeTest(int index, BaseTest testClass, String testName, boolean timedOut) {
        final Runnable continueToNext = () -> {
            log("CN1SS:INFO:suite finished test=" + testName);
            runNextTest(index + 1);
        };
        try {
            testClass.cleanup();
            if (timedOut) {
                log("CN1SS:ERR:suite test=" + testName + " failed due to timeout waiting for DONE");
            } else if (testClass.isFailed()) {
                log("CN1SS:ERR:suite test=" + testName + " failed: " + testClass.getFailMessage());
            } else if (!testClass.shouldTakeScreenshot()) {
                log("CN1SS:INFO:test=" + testName + " screenshot=none");
            }
        } catch (Throwable t) {
            log("CN1SS:ERR:suite test=" + testName + " finalize exception=" + t);
        }
        // The real screenshot is captured by BaseTest.createForm() →
        // onShowCompleted() → Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot().
        // Do NOT emit a fallback placeholder here — it would create a duplicate
        // CN1SS stream under the class simple name (e.g. "AffineScale") which
        // doesn't match the reference screenshot name (e.g. "graphics-affine-scale")
        // and breaks iOS/Android comparison results.
        continueToNext.run();
    }

    private void finishSuite() {
        try {
            String status;
            try {
                status = NativeInterfaceLanguageValidator.getLastStatus();
            } catch (Throwable t) {
                status = "error:" + t;
            }
            log("CN1SS:INFO:swift_diag_status=" + status);
        } finally {
            log("CN1SS:SUITE:FINISHED");
        }
        try {
            TestReporting.getInstance().testExecutionFinished(getClass().getName());
        } catch (Throwable t) {
            log("CN1SS:ERR:testExecutionFinished exception=" + t);
        }
        if (CN.isSimulator()) {
            Display.getInstance().exitApplication();
        }
    }

    private static void log(String msg) {
        System.out.println(msg);
    }

    private static void logThrowable(String context, Throwable t) {
        if (t == null) {
            log("CN1SS:ERR:throwable context=" + context + " value=null");
            return;
        }
        log("CN1SS:ERR:throwable context=" + context + " type=" + t.getClass().getName());
        log("CN1SS:ERR:throwable context=" + context + " message=" + String.valueOf(t.getMessage()));
        String stack = Display.getInstance().getStackTrace(Thread.currentThread(), t);
        if (stack == null) {
            log("CN1SS:ERR:throwable context=" + context + " stack=null");
            return;
        }
        log("CN1SS:ERR:throwable context=" + context + " stackLength=" + stack.length());
        for (String line : StringUtil.tokenize(stack, '\n')) {
            if (line.length() > 200) {
                line = line.substring(0, 200);
            }
            log("CN1SS:ERR:throwable context=" + context + " stack=" + line);
        }
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
