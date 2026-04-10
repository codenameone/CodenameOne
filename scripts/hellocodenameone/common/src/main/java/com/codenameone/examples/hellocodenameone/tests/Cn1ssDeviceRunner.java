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
    private static final int TEST_TIMEOUT_MS = 30000;
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
            new ToastBarTopPositionScreenshotTest(),
            // Keep this as the last screenshot test; orientation changes can leak into subsequent screenshots.
            new OrientationLockScreenshotTest(),
            new InPlaceEditViewTest(),
            new BytecodeTranslatorRegressionTest(),
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
        boolean shouldEmitScreenshot = false;
        try {
            testClass.cleanup();
            shouldEmitScreenshot = testClass.shouldTakeScreenshot();
            if (timedOut) {
                log("CN1SS:ERR:suite test=" + testName + " failed due to timeout waiting for DONE");
            } else if (testClass.isFailed()) {
                log("CN1SS:ERR:suite test=" + testName + " failed: " + testClass.getFailMessage());
            } else if (!shouldEmitScreenshot) {
                log("CN1SS:INFO:test=" + testName + " screenshot=none");
            }
        } catch (Throwable t) {
            log("CN1SS:ERR:suite test=" + testName + " finalize exception=" + t);
            shouldEmitScreenshot = false;
        }
        if (shouldEmitScreenshot) {
            emitFallbackScreenshotChunk(testName);
        }
        continueToNext.run();
    }

    private void emitFallbackScreenshotChunk(String testName) {
        String safeName = sanitizeMarkerName(testName);
        final String tinyPngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO5WZ8kAAAAASUVORK5CYII=";
        log("CN1SS:" + safeName + ":000000:" + tinyPngBase64);
        log("CN1SS:END:" + safeName);
    }

    private String sanitizeMarkerName(String testName) {
        if (testName == null || testName.length() == 0) {
            return "default";
        }
        StringBuilder out = new StringBuilder(testName.length());
        for (int i = 0; i < testName.length(); i++) {
            char c = testName.charAt(i);
            boolean valid =
                    (c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '_'
                    || c == '.'
                    || c == '-';
            out.append(valid ? c : '_');
        }
        return out.length() == 0 ? "default" : out.toString();
    }

    private void finishSuite() {
        log("CN1SS:INFO:swift_diag_status=" + NativeInterfaceLanguageValidator.getLastStatus());
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
