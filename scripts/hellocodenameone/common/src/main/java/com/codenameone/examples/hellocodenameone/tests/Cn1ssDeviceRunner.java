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
import com.codenameone.examples.hellocodenameone.tests.graphics.EmptyClip;
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
    private static final int TEST_TIMEOUT_MS_HTML5 = 10000;
    private static final int TEST_TIMEOUT_MS_NATIVE = 30000;
    private static final int TEST_POLL_INTERVAL_MS = 50;

    // The index of the test the suite is currently running. Read by
    // Cn1ssDeviceRunnerHelper to drop a screenshot whose callback fires after
    // the runner has moved on (see the set-site in runNextTest).
    static volatile int sCurrentTestSeq = -1;

    private static int testTimeoutMs(BaseTest testClass) {
        // DualAppearanceBaseTest needs more wall time on HTML5 (light + dark
        // phases serially, each paying registerReadyCallback's 1500ms + settle
        // + capture). Other tests stay at the tighter HTML5 default so a hung
        // test doesn't eat the suite-level budget.
        if ("HTML5".equals(Display.getInstance().getPlatformName())
                && testClass instanceof DualAppearanceBaseTest) {
            return TEST_TIMEOUT_MS_NATIVE;
        }
        return "HTML5".equals(Display.getInstance().getPlatformName())
                ? TEST_TIMEOUT_MS_HTML5
                : TEST_TIMEOUT_MS_NATIVE;
    }

    // Calling Display.getInstance() at static-init time was tripping the iOS
    // class loader (Cn1ssDeviceRunner failed to load before runSuite could
    // log a single starting test=...). Keep the array as a plain literal -
    // every test ends up in the jar regardless, and the platform-specific
    // skipping is handled at runtime by shouldForceTimeoutInHtml5 below.
    private static final BaseTest[] DEFAULT_TEST_CLASSES = new BaseTest[]{
            new MainScreenScreenshotTest(),
            // Advertising API: renders a banner + native-ad feed via the
            // deterministic MockAdProvider (cn1-ads-mock) for a pixel-stable shot.
            new AdsScreenshotTest(),
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
            // Regression guard for issue #5263: an empty clip (two
            // non-overlapping clipRects) must cull everything. The iOS Metal
            // backend used to open the whole framebuffer instead, flooding the
            // screen with the fully-clipped-out draws.
            new EmptyClip(),
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
            // Portable 3D / shader API (com.codename1.gpu): a Phong-lit cube, a
            // textured cube, a loaded glTF model, and a behavioral animation-loop
            // test. Positioned immediately before OrientationLock on purpose, to
            // satisfy two constraints at once:
            //   - iOS: a 2D form shown right after a GPU peer keeps the previous
            //     form's drawable for one capture (a pre-existing iOS present
            //     quirk). OrientationLock is the one test that recovers from this
            //     -- it forces a full-screen orientation change + revalidate
            //     before capturing -- so it absorbs the staleness cleanly, and
            //     DesktopMode (the last screenshot test) still sees OrientationLock
            //     as its predecessor exactly like on master, so every baseline
            //     matches.
            //   - JavaScript: the glTF model is the heaviest 3D capture; running
            //     it here (rather than dead last) keeps it out of the JS port's
            //     late-suite worker-barrier danger zone where it intermittently
            //     failed to emit.
            // The 3D tests render through their own GPU peer and capture correctly
            // regardless of what precedes them.
            new Gpu3DCubeScreenshotTest(),
            new Gpu3DTexturedCubeScreenshotTest(),
            new Gpu3DModelScreenshotTest(),
            new Gpu3DAnimationTest(),
            // Keep this as the last screenshot test; orientation changes can leak into subsequent screenshots.
            new OrientationLockScreenshotTest(),
            new InPlaceEditViewTest(),
            new BytecodeTranslatorRegressionTest(),
            new SimdApiTest(),
            new SimdBenchmarkTest(),
            new SecureStorageTest(),
            // Exercises com.codename1.camera.* end-to-end against the
            // JavaSE simulator's synthetic camera backend (no permission
            // prompts). Self-skips on iOS / Android / JS where the open
            // call would surface an OS dialog.
            new CameraApiTest(),
            new SimdLargeAllocaTest(),
            new StreamApiTest(),
            new StringApiTest(),
            new TimeApiTest(),
            new NanoTimeApiTest(),
            new CryptoApiTest(),
            new Java17Tests(),
            new BackgroundThreadUiAccessTest(),
            new BridgeBulkTransferGuardTest(),
            new VPNDetectionAPITest(),
            new CallDetectionAPITest(),
            new LocalNotificationOverrideTest(),
            new Base64NativePerformanceTest(),
            new AccessibilityTest(),
            new FileSystemStorageOpenInputStreamMissingTest(),
            new MutableImageReadbackTest(),
            new MutableImageClipReadbackTest(),
            // Desktop integration demo. Placed LAST on purpose: it shows a Toolbar with text
            // and a populated list, which warms the font cache / shifts suite timing, and the
            // earlier graphics screenshot tests (DrawString, DrawStringDecorated, inscribed
            // triangle grid) paint text directly during a frame that races the async font load -
            // running this test before them changes whether those fonts are loaded at capture
            // time and flips their baselines. Last = the rest of the suite matches master exactly.
            // Inert on phone/tablet ports (plain Toolbar + hamburger side menu + faded scrollbar);
            // on the Mac native build it enables desktop mode (commands move to the native menu
            // bar, interactive always-visible scrollbar), reverting its global toggles after capture.
            new DesktopModeScreenshotTest()
    };

    private static BaseTest prependedTest;

    /// Index of the test that has consumed its one-shot silent-timeout retry
    /// (see finalizeTest). -1 until the first retry fires; comparing against
    /// the index guarantees at most one retry per test so a genuinely broken
    /// test still fails after ~2x its timeout instead of looping.
    private int retriedTestIndex = -1;

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
        if (!matchesFilter(testName)) {
            // Optional subset run: -Dcn1ss.filter=<substr> or CN1SS_FILTER=<substr>
            // runs only tests whose class simple name contains the (case-
            // insensitive) substring. Lets a targeted run (e.g. a single
            // form-factor or graphics subset) skip the full ~120-test suite.
            log("CN1SS:INFO:suite skipping test=" + testName + " (filter)");
            runNextTest(index + 1);
            return;
        }
        CN.callSerially(() -> {
            // Mark which test the suite is currently on. A screenshot whose
            // async callback fires after the runner has advanced (the heavy 4K
            // graphics tests can be GPU-bound long enough to time out at
            // capture-requested) would otherwise grab the NEXT test's frame and
            // save it under this test's name (visible symptom: FillRoundRect.png
            // carrying a later test's title bar). Cn1ssDeviceRunnerHelper checks
            // this seq in the capture callback and drops such late frames, so a
            // timed-out test is recorded as missing (tolerated) rather than
            // mislabeled.
            sCurrentTestSeq = index;
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
        if (deadline <= 0L) {
            // Sentinel from the JS-port bridge (port.js runCn1ssResolvedTest):
            // it can't see testTimeoutMs()'s DualAppearance widening and used to
            // hard-code a flat 10s, which guillotined dual-appearance tests
            // mid-dark-phase so their pending emit spilled into the NEXT test
            // (ChatInput_dark captured the following ImageViewer form). Compute
            // the type-aware deadline here instead, so a DualAppearanceBaseTest
            // gets its full 30s on HTML5 too.
            deadline = System.currentTimeMillis() + testTimeoutMs(testClass);
        }
        if (testClass.isDone()) {
            finalizeTest(index, testClass, testName, false);
            return;
        }
        if (System.currentTimeMillis() >= deadline) {
            finalizeTest(index, testClass, testName, true);
            return;
        }
        final long fixedDeadline = deadline;
        CN.setTimeout(TEST_POLL_INTERVAL_MS, () -> awaitTestCompletion(index, testClass, testName, fixedDeadline));
    }

    private void finalizeTest(int index, BaseTest testClass, String testName, boolean timedOut) {
        final Runnable continueToNext = () -> {
            log("CN1SS:INFO:suite finished test=" + testName);
            runNextTest(index + 1);
        };
        try {
            testClass.cleanup();
            if (timedOut) {
                log("CN1SS:ERR:suite test=" + testName + " failed due to timeout waiting for DONE stage="
                        + testClass.getCaptureStage());
                if (shouldRetryAfterSilentTimeout(index, testClass)) {
                    // The test timed out without EVER requesting a capture and
                    // without reporting a failure: the show -> settle-timer ->
                    // screenshot chain was silently swallowed. Observed on the
                    // iOS Metal CI job (graphics-fill-shape produced no PNG, no
                    // error, while the very next test rendered fine ~2s later),
                    // i.e. a transient render-pipeline stall rather than a bug
                    // in the test itself. The pipeline is healthy again by the
                    // time the timeout poll fires, so one re-run reliably
                    // recovers the screenshot instead of failing the whole job
                    // on a missing tile.
                    retriedTestIndex = index;
                    log("CN1SS:WARN:suite test=" + testName
                            + " retrying once: timed out before any capture started");
                    testClass.resetForRetry();
                    runNextTest(index);
                    return;
                }
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

    /// A retry is only safe when the timeout was truly silent. Gates:
    /// - one retry per test (retriedTestIndex);
    /// - native ports only: on HTML5 the suite advancement is co-driven by
    ///   port.js (runCn1ssResolvedTest dispatches per index) and known-bad
    ///   tests are parked via its forced-timeout lists, so a Java-side rerun
    ///   would fight that machinery;
    /// - no failure was reported (a real failure should surface, not retry);
    /// - no capture was started (an in-flight capture could emit after the
    ///   rerun's form is up and ship the wrong pixels under this test's name);
    /// - the test actually takes a screenshot (non-screenshot tests may have
    ///   side effects that aren't safe to repeat, and a missing tile is the
    ///   only failure mode this retry exists to prevent).
    private boolean shouldRetryAfterSilentTimeout(int index, BaseTest testClass) {
        return retriedTestIndex != index
                && !"HTML5".equals(Display.getInstance().getPlatformName())
                && !testClass.isFailed()
                && !testClass.isCaptureStarted()
                && testClass.shouldTakeScreenshot();
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

    /// True when the test should run under the optional cn1ss.filter subset
    /// selector (system property cn1ss.filter). Empty/unset runs everything.
    /// Read at call time (no static field) to avoid the static-init
    /// class-loading pitfalls noted above. System.getenv is intentionally NOT
    /// used - it is outside the Codename One runtime API and trips the build's
    /// bytecode-compliance check.
    private static boolean matchesFilter(String testName) {
        String filter = System.getProperty("cn1ss.filter");
        if (filter == null || filter.length() == 0) {
            return true;
        }
        return testName != null
                && testName.toLowerCase().indexOf(filter.toLowerCase()) >= 0;
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
