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
import com.codenameone.examples.hellocodenameone.tests.graphics.ClipUnderRotation;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawArc;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawGradient;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawGradientStops;
import com.codenameone.examples.hellocodenameone.tests.graphics.DrawImage;
import com.codenameone.examples.hellocodenameone.tests.graphics.GaussianBlur;
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
import com.codenameone.examples.hellocodenameone.tests.graphics.InscribedTriangleGrid;
import com.codenameone.examples.hellocodenameone.tests.graphics.Rotate;
import com.codenameone.examples.hellocodenameone.tests.graphics.Scale;
import com.codenameone.examples.hellocodenameone.tests.graphics.StrokeTest;
import com.codenameone.examples.hellocodenameone.tests.graphics.TileImage;
import com.codenameone.examples.hellocodenameone.tests.graphics.TransformCamera;
import com.codenameone.examples.hellocodenameone.tests.graphics.TransformPerspective;
import com.codenameone.examples.hellocodenameone.tests.graphics.TransformRotation;
import com.codenameone.examples.hellocodenameone.tests.graphics.TransformTranslation;
import com.codenameone.examples.hellocodenameone.tests.graphics.LargeStrokeDirtyClipTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartBarScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartBubbleScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartCombinedXYScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartCubicLineScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartDoughnutScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartLineScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartPieScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartRadarScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartRangeBarScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartRotatedScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartScatterScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartStackedBarScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartTimeChartScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.charts.ChartTransformScreenshotTest;
import com.codenameone.examples.hellocodenameone.tests.accessibility.AccessibilityTest;


public final class Cn1ssDeviceRunner extends DeviceRunner {
    // Per-test deadline cap. The 10s HTML5 default keeps already-
    // hanging tests (LightweightPickerButtons, ToastBarTopPosition
    // hitting the canvasContextWipe noCanvas path) from eating the
    // 1740s suite-level budget. DualAppearanceBaseTest subclasses
    // override this via {@link BaseTest#getTimeoutMs()} because they
    // run light + dark phases serially and each phase pays
    // registerReadyCallback's 1500ms UITimer + wait_for_ui_settle
    // (up to ~800ms) + capture/encode + chunked emit, so the
    // bytecode-translated path easily clears 10s on shared GHA
    // runners. At 10s the dark phase's emit fired AFTER the test had
    // already timed out and the runner had advanced to the next test
    // - the late dark emit then captured whatever form happened to
    // be on the canvas at that moment (visible symptom:
    // ToolbarTheme_dark.png showed "TabsTheme / light"). iOS /
    // Android / JavaSE keep their wider 30s cap because they don't
    // share the JS port's canvas-hang failure mode.
    // HTML5 caps trimmed once the canvas-accumulation leak was fixed: a test's
    // screenshot is now captured ~2-3s in (registerReadyCallback's HTML5 settle
    // + host-side wait_for_ui_settle), and the remaining wait is purely for a
    // done() that some tests never call. The timeout only bounds that idle
    // tail, so 7s leaves comfortable headroom over the real capture time while
    // reclaiming the bulk of the per-test idle wait. DualAppearance runs two
    // phases serially so it gets a wider 18s HTML5 cap. Native ports keep 30s.
    private static final int TEST_TIMEOUT_MS_HTML5 = 7000;
    private static final int TEST_TIMEOUT_MS_HTML5_DUAL = 18000;
    private static final int TEST_TIMEOUT_MS_NATIVE = 30000;
    private static final int TEST_POLL_INTERVAL_MS = 50;

    private static int testTimeoutMs(BaseTest testClass) {
        if (!"HTML5".equals(Display.getInstance().getPlatformName())) {
            return TEST_TIMEOUT_MS_NATIVE;
        }
        // DualAppearanceBaseTest needs more wall time on HTML5 (light + dark
        // phases serially, each paying the settle + capture).
        return (testClass instanceof DualAppearanceBaseTest)
                ? TEST_TIMEOUT_MS_HTML5_DUAL
                : TEST_TIMEOUT_MS_HTML5;
    }

    // Calling Display.getInstance() at static-init time was tripping the iOS
    // class loader (Cn1ssDeviceRunner failed to load before runSuite could
    // log a single starting test=...). Keep the array as a plain literal -
    // every test ends up in the jar regardless, and the platform-specific
    // skipping is handled at runtime by shouldForceTimeoutInHtml5 below.
    private static final BaseTest[] DEFAULT_TEST_CLASSES = new BaseTest[]{
            new MainScreenScreenshotTest(),
            // Animation/transition grid tests: each emits a 2x3 frame grid driven
            // by the AnimationTime override so iOS/Android/JavaSE produce identical
            // pixels regardless of wall-clock pacing. Skipped on HTML5 via the
            // HTML5_SKIP_TESTS set.
            new SlideHorizontalTransitionTest(),
            new SlideHorizontalBackTransitionTest(),
            new SlideVerticalTransitionTest(),
            new SlideFadeTitleTransitionTest(),
            new CoverHorizontalTransitionTest(),
            new UncoverHorizontalTransitionTest(),
            new FadeTransitionTest(),
            new FlipTransitionTest(),
            new MorphTransitionTest(),
            new MorphTransitionScrolledSourceTest(),
            new MorphTransitionSnapshotTest(),
            new TabsAnimatedIndicatorScreenshotTest(),
            new PullToRefreshSpinnerScreenshotTest(),
            new AnimateLayoutScreenshotTest(),
            new AnimateHierarchyScreenshotTest(),
            new AnimateUnlayoutScreenshotTest(),
            new SmoothScrollScreenshotTest(),
            new StickyHeaderScreenshotTest(),
            new StickyHeaderSlideTransitionScreenshotTest(),
            new StickyHeaderFadeTransitionScreenshotTest(),
            new TensileBounceScreenshotTest(),
            new StatusBarTapDiagnosticScreenshotTest(),
            new ComponentReplaceFadeScreenshotTest(),
            new ComponentReplaceSlideScreenshotTest(),
            new ComponentReplaceFlipScreenshotTest(),
            new MotionShowcaseScreenshotTest(),
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
            new DrawGradientStops(),
            new GaussianBlur(),
            new FillPolygon(),
            new AffineScale(),
            new Scale(),
            new FillTriangle(),
            new DrawShape(),
            new FillShape(),
            new InscribedTriangleGrid(),
            new StrokeTest(),
            new Clip(),
            new ClipUnderRotation(),
            new TileImage(),
            new Rotate(),
            new TransformTranslation(),
            new TransformRotation(),
            new TransformPerspective(),
            new TransformCamera(),
            // Standalone repro for the iOS form-Graphics dirty-region
            // clipping edge case that makes the XY chart screenshot tests
            // come back blank: a single Component in BorderLayout.CENTER
            // whose paint() draws a large stroked GeneralPath via
            // g.drawShape(...). If iOS captures a non-blank PNG with the
            // polyline visible the bug is specific to ChartComponent's
            // paint cycle; if it captures a blank PNG we have a minimal
            // reproduction the iOS-port fix can iterate against without
            // spinning up the entire chart-package.
            new LargeStrokeDirtyClipTest(),
            // ChartComponent coverage. The 2026-05-09 conjugation refactor in
            // Graphics.setTransform / iOS / Android / JavaSE / JS dropped
            // ChartComponent.paint's manual T(absX) * X * T(-absX)
            // compensation; without screenshot baselines for the major chart
            // types a regression in the chart render path goes silent until
            // a user reports it. Cover one test per chart family + two
            // dedicated transform paths (scale + rotate) so the
            // ChartComponent.setTransform branch (the one the refactor
            // directly touched) has explicit visual coverage.
            new ChartLineScreenshotTest(),
            new ChartCubicLineScreenshotTest(),
            new ChartBarScreenshotTest(),
            new ChartStackedBarScreenshotTest(),
            new ChartRangeBarScreenshotTest(),
            new ChartScatterScreenshotTest(),
            new ChartBubbleScreenshotTest(),
            new ChartPieScreenshotTest(),
            new ChartDoughnutScreenshotTest(),
            new ChartRadarScreenshotTest(),
            new ChartTimeChartScreenshotTest(),
            new ChartCombinedXYScreenshotTest(),
            new ChartTransformScreenshotTest(),
            new ChartRotatedScreenshotTest(),
            new BrowserComponentScreenshotTest(),
            new MediaPlaybackScreenshotTest(),
            new SheetScreenshotTest(),
            new SheetSlideUpAnimationScreenshotTest(),
            // ChatView (new AI UI primitive). Renders an assistant
            // conversation + typing indicator so the iOS Modern and
            // Android Material themes have a baseline for ChatBubbleUser,
            // ChatBubbleAssistant, ChatBubbleSystem, and ChatTypingIndicator
            // UIIDs the moment the cn1-ai PR adds them.
            new ChatViewScreenshotTest(),
            // ChatInput on its own (attach + voice + send all visible) so
            // the input-row UIIDs (ChatInput, ChatInputField, ChatSendButton,
            // ChatAttachButton, ChatVoiceButton) have a baseline independent
            // of the surrounding ChatView.
            new ChatInputScreenshotTest(),
            new ImageViewerNavigationScreenshotTest(),
            new TabsScreenshotTest(),
            new TextAreaAlignmentScreenshotTest(),
            new ValidatorLightweightPickerScreenshotTest(),
            new LightweightPickerButtonsScreenshotTest(),
            new PickerCancelRestoreTest(),
            new ToastBarTopPositionScreenshotTest(),
            // Native-theme fidelity tests (Phase 7): each emits a light+dark PNG pair
            // so the iOS Modern and Android Material themes get exercised per UIID.
            new ButtonThemeScreenshotTest(),
            new TextFieldThemeScreenshotTest(),
            new CheckBoxRadioThemeScreenshotTest(),
            new SwitchThemeScreenshotTest(),
            new PickerThemeScreenshotTest(),
            new ToolbarThemeScreenshotTest(),
            new TabsThemeScreenshotTest(),
            new MultiButtonThemeScreenshotTest(),
            new ListThemeScreenshotTest(),
            new DialogThemeScreenshotTest(),
            new FloatingActionButtonThemeScreenshotTest(),
            new SpanLabelThemeScreenshotTest(),
            new DarkLightShowcaseThemeScreenshotTest(),
            new PaletteOverrideThemeScreenshotTest(),
            new CssGradientsScreenshotTest(),
            new CssFilterBlurScreenshotTest(),
            // Build-time SVG transcoder coverage: the static test renders
            // shapes / gradients / paths, the animated test pins
            // AnimationTime so the captured frame is deterministic.
            new SVGStaticScreenshotTest(),
            new SVGAnimatedScreenshotTest(),
            // Build-time Lottie transcoder -- same pipeline as SVG, lowers
            // the Bodymovin JSON into the SVG model and reuses SVGRegistry.
            new LottieAnimatedScreenshotTest(),
            // Keep this as the last screenshot test; orientation changes can leak into subsequent screenshots.
            new OrientationLockScreenshotTest(),
            new InPlaceEditViewTest(),
            new BytecodeTranslatorRegressionTest(),
            new SimdApiTest(),
            // Exercises com.codename1.camera.* end-to-end against the
            // JavaSE simulator's synthetic camera backend (no permission
            // prompts). Self-skips on iOS / Android / JS where the open
            // call would surface an OS dialog.
            new CameraApiTest(),
            new SimdLargeAllocaTest(),
            new StreamApiTest(),
            new StringApiTest(),
            new TimeApiTest(),
            new CryptoApiTest(),
            new Java17Tests(),
            new BackgroundThreadUiAccessTest(),
            new VPNDetectionAPITest(),
            new CallDetectionAPITest(),
            new LocalNotificationOverrideTest(),
            new Base64NativePerformanceTest(),
            new AccessibilityTest(),
            new FileSystemStorageOpenInputStreamMissingTest(),
            new MutableImageReadbackTest()
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
            // Pre-warm the modern theme .res here, at suite start, before any
            // test has run and before canvas accumulation builds up. The 14
            // DualAppearanceBaseTest subclasses all read this same resource;
            // doing it once up front means none of them performs the risky
            // late-suite asset read that intermittently hard-stalled the JS
            // port worker at the first theme test (ButtonTheme, ~index 85).
            try {
                DualAppearanceBaseTest.prewarmModernTheme();
            } catch (Throwable t) {
                log("CN1SS:WARN:modern theme prewarm failed " + t);
            }
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
            awaitTestCompletion(index, testClass, testName, System.currentTimeMillis() + testTimeoutMs(testClass));
        });
    }

    private boolean shouldForceTimeoutInHtml5(String testName) {
        if (!"HTML5".equals(Display.getInstance().getPlatformName())) {
            return false;
        }
        // This list mirrors the authoritative skip set maintained in
        // Ports/JavaScriptPort/src/main/webapp/port.js
        // (cn1ssForcedTimeoutTestClasses + cn1ssForcedTimeoutTestNames).
        // The Java gate runs first: anything skipped here never reaches the
        // port.js bridge, so keep the two lists in sync. When a test gets
        // un-parked in port.js (e.g. after a runtime fix lands), remove it
        // here too — otherwise the suite stays silent on tests that the
        // JS-side comments believe should now run.
        //
        // The list is intentionally an inline `||` chain rather than a
        // static HashSet/Set field. Earlier revisions of this file used a
        // static collection initialised via a static method call (or a
        // method-call initializer for DEFAULT_TEST_CLASSES); both broke iOS
        // class loading - Cn1ssDeviceRunner failed to load before runSuite()
        // could even log a single starting test=... entry, leaving the suite
        // to time out at the 300s end-marker deadline. Keep all skip lookups
        // inline to avoid triggering the same static-init failure path.
        return isJsSkippedNativeTest(testName)
                || isJsSkippedKnownRuntimeBug(testName);
    }

    private static boolean isJsSkippedNativeTest(String testName) {
        // Native APIs / platform bridges that the JavaScript port doesn't
        // implement. These would surface as exceptions or hangs the moment
        // the test starts; coverage stays on iOS/Android/JavaSE.
        return "MediaPlaybackScreenshotTest".equals(testName)
                || "BytecodeTranslatorRegressionTest".equals(testName)
                || "BackgroundThreadUiAccessTest".equals(testName)
                || "VPNDetectionAPITest".equals(testName)
                || "CallDetectionAPITest".equals(testName)
                || "LocalNotificationOverrideTest".equals(testName)
                || "Base64NativePerformanceTest".equals(testName)
                || "AccessibilityTest".equals(testName)
                // CryptoApiTest exercises AES/RSA/Signature/SecureRandom which
                // route through CodenameOneImplementation overrides; the
                // JavaScript port doesn't yet provide a crypto bridge.
                || "CryptoApiTest".equals(testName)
                // BrowserComponent's iframe ``load`` event isn't routed
                // through the worker-callback transport, so the test waits on
                // its own ``readyRunnable`` indefinitely. Tracked under
                // ``browserComponentLoadEvent`` in port.js.
                || "BrowserComponentScreenshotTest".equals(testName);
    }

    private static boolean isJsSkippedKnownRuntimeBug(String testName) {
        // Tests parked because of specific JS-port runtime bugs. Each entry
        // has a matching comment in port.js explaining the symptom and the
        // working-name we track it under. When the underlying bug is fixed
        // the matching entry is removed from BOTH lists.
        return
                // ``simdLargeAllocaCorrupt``: SimdLargeAllocaTest corrupts the
                // HTML5Implementation instance state via its large-alloca
                // pattern, propagating ``{}`` receivers into subsequent
                // canvas / paintDirty calls. Needs its own investigation.
                "SimdLargeAllocaTest".equals(testName)
                // ``chatInputEmitHijack`` / ``chatViewEmitHijack``:
                // emitChannel host-bridges to a capture of the visible
                // browser canvas instead of the test-supplied off-screen
                // Image, so the dual-appearance dark/light streams contain
                // the previous test's pixels.
                || "ChatInputScreenshotTest".equals(testName)
                || "ChatViewScreenshotTest".equals(testName)
                // ``canvasContextWipe``: a leftover Canvas2D state mutation
                // from a prior test wipes the test's context once we hit
                // the canvas-accumulation tail. The targeted no-op recovery
                // covers most subjects but these four still hang their
                // SCREENSHOT_DONE wait on half of CI runs.
                || "ToastBarTopPositionScreenshotTest".equals(testName)
                || "CssGradientsScreenshotTest".equals(testName)
                || "SheetScreenshotTest".equals(testName)
                // ``sheetTearDownLeak``: TextAreaAlignmentStates' form
                // renders correctly, but the screenshot captures it under
                // a leftover Sheet overlay because Sheet teardown doesn't
                // complete before the next test starts.
                || "TextAreaAlignmentScreenshotTest".equals(testName)
                // ``chartCombinedXyCapture``: ChartCombinedXY hangs the
                // SUITE in canvasToBlob retry loop after ~88 fallback-path
                // captures. Transform + Rotated weren't reached on the
                // unpark-all run because CombinedXY took down the suite
                // first; parked under the same suspicion.
                || "ChartCombinedXYScreenshotTest".equals(testName)
                || "ChartTransformScreenshotTest".equals(testName)
                || "ChartRotatedScreenshotTest".equals(testName)
                // ``graphicsTransform3dCanvasHang``: the 3D perspective /
                // camera transform tests render into a canvas the worker-side
                // screenshot path can't resolve (SCREENSHOT_START reports
                // canvasCandidates=0), so the suite re-dispatches the same
                // index indefinitely and never reaches the per-test deadline.
                // The 2D transform tests (rotation, translation, affine) are
                // unaffected and keep running. Tracked in port.js under the
                // same name.
                || "TransformPerspective".equals(testName)
                || "TransformCamera".equals(testName);
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
