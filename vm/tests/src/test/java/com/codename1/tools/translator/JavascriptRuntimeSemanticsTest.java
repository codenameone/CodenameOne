package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavascriptRuntimeSemanticsTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void executesArrayCovarianceInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsArrayCovarianceApp.java", "JsArrayCovarianceApp");

        assertEquals(511, result.result, "Translated runtime should preserve CN1-relevant array covariance semantics");
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void parseDoubleAppliesExponentFromStringToRealSplit(CompilerHelper.CompilerConfig config) throws Exception {
        // Regression guard for the parparvm_runtime.js parseDblImpl binding.
        // StringToReal.parseDouble("1.4") strips the decimal point and hands
        // parseDblImpl("14", -1) to the native; the JS binding must apply 10^-1
        // to the integer value. A stale binding that ignored the exponent was
        // the root cause of Kotlin Switch pills rendering at ~600x300 instead
        // of ~50x25: Switch.getTrackScaleX() parses theme "2.5" via
        // Double.parseDouble, and 2.5 -> 25 made track width 10x too big.
        WorkerRunResult result = translateAndRunFixture(config, "JsDoubleParseApp.java", "JsDoubleParseApp");

        assertEquals(511, result.result,
                "parseDouble must apply the exponent split out by StringToReal (1.4 must not resolve to 14). raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void executesLocaleTimeZoneAndDateFormatInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsLocaleTimeZoneApp.java", "JsLocaleTimeZoneApp");

        assertEquals(511, result.result, "Translated runtime should preserve browser-safe Locale/TimeZone/DateFormat semantics");
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void executesThreadWaitSleepJoinAndInterruptInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsThreadSemanticsApp.java", "JsThreadSemanticsApp");

        assertEquals(32717, result.result, "Translated runtime should preserve CN1-relevant thread semantics");
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void executesBroaderJavaApiCoverageInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsJavaApiCoverageApp.java", "JsJavaApiCoverageApp");

        assertEquals(511, result.result, "Translated runtime should execute the broader JavaAPI coverage fixture");
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesSuperInvokeInsideConstructorInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsSuperInvokeInCtorApp.java", "JsSuperInvokeInCtorApp");

        assertEquals(1209, result.result,
                "Translated runtime should preserve constructor-time super invokes without routing through the override. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesFieldInitializersAlongsideSuperInvokeInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsFieldInitializerAndSuperInvokeApp.java", "JsFieldInitializerAndSuperInvokeApp");

        assertEquals(111, result.result,
                "Translated runtime should preserve both field initializers and constructor-time super invokes. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesFormLikeSuperAddPathInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsFormLikeSuperAddApp.java", "JsFormLikeSuperAddApp");

        assertEquals(15, result.result,
                "Translated runtime should preserve the Form-like super-add path, including virtual callbacks during the superclass add flow. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesInvokeSpecialOwnerResolutionAcrossInheritedMethodGapsInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsInvokeSpecialInheritedOwnerApp.java", "JsInvokeSpecialInheritedOwnerApp");

        assertEquals(110, result.result,
                "Translated runtime should resolve invokespecial owners to the actual declaring class when the direct superclass inherits the method. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesIteratorDispatchSemanticsInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsIteratorDispatchApp.java", "JsIteratorDispatchApp");

        assertEquals(63, result.result,
                "Translated runtime should preserve iterator segment ordering and coordinates through interface dispatch. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesIteratorTypeDispatchWithoutCoordinateCopyInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsIteratorTypeDispatchApp.java", "JsIteratorTypeDispatchApp");

        assertEquals(63, result.result,
                "Translated runtime should preserve iterator segment types through interface dispatch even without coordinate copy. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesPrimitiveArrayLiteralAndCopySemanticsInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsPrimitiveArraySemanticsApp.java", "JsPrimitiveArraySemanticsApp");

        assertEquals(255, result.result,
                "Translated runtime should preserve primitive byte[]/float[] literals and System.arraycopy semantics. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesCapturingLambdaDispatchInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsCapturingLambdaDispatchApp.java", "JsCapturingLambdaDispatchApp");

        // base(11) + seed(7)*3 + "sheet".length()(5) = 37. If the lambda synthesis
        // emits spurious aload_0 BasicInstructions (the pre-fix Parser bug), the
        // lambda's run() method dispatches on the wrong captured field and either
        // throws a VIRTUAL_FAIL or records the wrong value in out[0].
        assertEquals(37, result.result,
                "Translated lambda run() must dispatch on its first capture (enclosing this), "
                        + "not shift down to subsequent captures. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesIteratorCoordinateCopyWithHardcodedSegmentCountsInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsIteratorCoordinateCopyApp.java", "JsIteratorCoordinateCopyApp");

        assertEquals(63, result.result,
                "Translated runtime should preserve coordinate copying through interface dispatch when segment counts are hardcoded. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesIteratorPointShiftLookupInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsIteratorPointShiftDispatchApp.java", "JsIteratorPointShiftDispatchApp");

        assertEquals(63, result.result,
                "Translated runtime should preserve POINT_SHIFT[type] lookup through interface dispatch. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesGeneralPathQuadSegmentTypesInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsGeneralPathQuadIteratorApp.java", "JsGeneralPathQuadIteratorApp");

        assertEquals(127, result.result,
                "Translated runtime should preserve real GeneralPath quad segment types and coordinates. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesGeneralPathArcSegmentsInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsGeneralPathArcIteratorApp.java", "JsGeneralPathArcIteratorApp");

        assertEquals(511, result.result,
                "Translated runtime should preserve GeneralPath.arc() segment types and endpoints. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesInterfaceObjectBridgeDispatchInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsInterfaceObjectBridgeApp.java", "JsInterfaceObjectBridgeApp");

        assertEquals(511, result.result,
                "Translated runtime should preserve object-returning interface bridge dispatch used by the HTML5 render adapters. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesGenericSinkBridgeDispatchInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsGenericSinkBridgeApp.java", "JsGenericSinkBridgeApp");

        assertEquals(2047, result.result,
                "Translated runtime should preserve generic sink bridge dispatch used by the HTML5 buffered render queue. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesAnonymousCapturedSinkDispatchInWorkerRuntime(CompilerHelper.CompilerConfig config) throws Exception {
        WorkerRunResult result = translateAndRunFixture(config, "JsAnonymousSinkCaptureApp.java", "JsAnonymousSinkCaptureApp");

        assertEquals(2047, result.result,
                "Translated runtime should preserve anonymous captured sink dispatch used by BufferedGraphics. raw="
                        + result.rawMessage + " err=" + result.errorMessage);
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void executesHostCallbacksThroughWorkerProtocol(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-host-src");
        Path classesDir = Files.createTempDirectory("js-host-classes");
        Path javaApiDir = Files.createTempDirectory("js-host-javaapi");

        Path vmHostDir = sourceDir.resolve("com").resolve("codename1").resolve("impl").resolve("platform").resolve("js");
        Files.createDirectories(vmHostDir);
        Files.write(vmHostDir.resolve("VMHost.java"),
                JavascriptTargetIntegrationTest.loadFixture("com/codename1/impl/platform/js/VMHost.java").getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("JsHostCallbackApp.java"),
                JavascriptTargetIntegrationTest.loadFixture("JsHostCallbackApp.java").getBytes(StandardCharsets.UTF_8));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-host-output");
        JavascriptTargetIntegrationTest.runJavascriptTranslator(classesDir, outputDir, "JsHostCallbackApp");

        Path distDir = outputDir.resolve("dist").resolve("JsHostCallbackApp-js");
        WorkerRunResult result = runGeneratedWorkerBundleWithHostCallbacks(distDir);

        assertEquals("result", result.type, "Generated worker bundle should complete through the host callback protocol");
        assertEquals(42, result.result, "Host callback should round-trip data back into the translated VM");
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Generated worker bundle should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void injectsEventsAndIgnoresUnknownMessagesThroughWorkerProtocol(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-host-event-src");
        Path classesDir = Files.createTempDirectory("js-host-event-classes");
        Path javaApiDir = Files.createTempDirectory("js-host-event-javaapi");

        Path vmHostDir = sourceDir.resolve("com").resolve("codename1").resolve("impl").resolve("platform").resolve("js");
        Files.createDirectories(vmHostDir);
        Files.write(vmHostDir.resolve("VMHost.java"),
                JavascriptTargetIntegrationTest.loadFixture("com/codename1/impl/platform/js/VMHost.java").getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("JsHostEventApp.java"),
                JavascriptTargetIntegrationTest.loadFixture("JsHostEventApp.java").getBytes(StandardCharsets.UTF_8));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-host-event-output");
        JavascriptTargetIntegrationTest.runJavascriptTranslator(classesDir, outputDir, "JsHostEventApp");

        Path distDir = outputDir.resolve("dist").resolve("JsHostEventApp-js");
        WorkerRunResult result = runGeneratedWorkerBundleWithEventInjection(distDir);

        assertEquals("result", result.type, "Generated worker bundle should complete after host event injection");
        assertEquals(4142, result.result, "Worker should preserve host-injected event data and ignore unknown protocol messages");
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Generated worker bundle should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void preservesHostEventQueueOrderingThroughWorkerProtocol(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-host-event-queue-src");
        Path classesDir = Files.createTempDirectory("js-host-event-queue-classes");
        Path javaApiDir = Files.createTempDirectory("js-host-event-queue-javaapi");

        Path vmHostDir = sourceDir.resolve("com").resolve("codename1").resolve("impl").resolve("platform").resolve("js");
        Files.createDirectories(vmHostDir);
        Files.write(vmHostDir.resolve("VMHost.java"),
                JavascriptTargetIntegrationTest.loadFixture("com/codename1/impl/platform/js/VMHost.java").getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("JsHostEventQueueApp.java"),
                JavascriptTargetIntegrationTest.loadFixture("JsHostEventQueueApp.java").getBytes(StandardCharsets.UTF_8));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-host-event-queue-output");
        JavascriptTargetIntegrationTest.runJavascriptTranslator(classesDir, outputDir, "JsHostEventQueueApp");

        Path distDir = outputDir.resolve("dist").resolve("JsHostEventQueueApp-js");
        WorkerRunResult result = runGeneratedWorkerBundleWithQueuedEvents(distDir);

        assertEquals("result", result.type, "Generated worker bundle should complete after queued host events");
        assertEquals(4120, result.result, "Worker should preserve FIFO event ordering and return -1 when the queue is empty");
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Generated worker bundle should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void propagatesHostCallbackErrorsDeterministicallyThroughWorkerProtocol(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-host-error-src");
        Path classesDir = Files.createTempDirectory("js-host-error-classes");
        Path javaApiDir = Files.createTempDirectory("js-host-error-javaapi");

        Path vmHostDir = sourceDir.resolve("com").resolve("codename1").resolve("impl").resolve("platform").resolve("js");
        Files.createDirectories(vmHostDir);
        Files.write(vmHostDir.resolve("VMHost.java"),
                JavascriptTargetIntegrationTest.loadFixture("com/codename1/impl/platform/js/VMHost.java").getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("JsHostCallbackErrorApp.java"),
                JavascriptTargetIntegrationTest.loadFixture("JsHostCallbackErrorApp.java").getBytes(StandardCharsets.UTF_8));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-host-error-output");
        JavascriptTargetIntegrationTest.runJavascriptTranslator(classesDir, outputDir, "JsHostCallbackErrorApp");

        Path distDir = outputDir.resolve("dist").resolve("JsHostCallbackErrorApp-js");
        WorkerRunResult result = runGeneratedWorkerBundleWithHostCallbackError(distDir);

        assertEquals("error", result.type, "Generated worker bundle should surface host callback failures as worker errors");
        assertTrue(result.errorMessage != null && result.errorMessage.contains("Injected host failure 7"),
                "Worker should expose a deterministic host callback error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void executesGeneratedWorkerProtocolEndToEnd(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-worker-src");
        Path classesDir = Files.createTempDirectory("js-worker-classes");
        Path javaApiDir = Files.createTempDirectory("js-worker-javaapi");

        Files.write(sourceDir.resolve("JsWorkerProtocolApp.java"),
                JavascriptTargetIntegrationTest.loadFixture("JsWorkerProtocolApp.java").getBytes(StandardCharsets.UTF_8));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-worker-output");
        JavascriptTargetIntegrationTest.runJavascriptTranslator(classesDir, outputDir, "JsWorkerProtocolApp");

        Path distDir = outputDir.resolve("dist").resolve("JsWorkerProtocolApp-js");
        WorkerRunResult result = runGeneratedWorkerBundle(distDir);

        assertEquals("result", result.type, "Generated worker bundle should report completion through the worker protocol");
        assertEquals(321, result.result, "Generated worker bundle should execute start/result flow end-to-end");
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Generated worker bundle should not emit an error message");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void exposesStableVmProtocolHandshakeBeforeWorkerStart(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-worker-protocol-src");
        Path classesDir = Files.createTempDirectory("js-worker-protocol-classes");
        Path javaApiDir = Files.createTempDirectory("js-worker-protocol-javaapi");

        Files.write(sourceDir.resolve("JsWorkerProtocolApp.java"),
                JavascriptTargetIntegrationTest.loadFixture("JsWorkerProtocolApp.java").getBytes(StandardCharsets.UTF_8));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-worker-protocol-output");
        JavascriptTargetIntegrationTest.runJavascriptTranslator(classesDir, outputDir, "JsWorkerProtocolApp");

        Path distDir = outputDir.resolve("dist").resolve("JsWorkerProtocolApp-js");
        WorkerRunResult result = runGeneratedWorkerBundleWithProtocolHandshake(distDir);

        assertEquals("protocol-check", result.type, "Generated worker bundle should expose the VM protocol before start");
        assertEquals(1, result.protocolVersion, "VM protocol version should be stable and explicit");
        assertEquals("start", result.protocolStartType, "VM protocol should document the start message");
        assertEquals("host-callback", result.protocolHostCallbackType, "VM protocol should document host callback delivery");
        assertEquals("timer-wake", result.protocolTimerWakeType, "VM protocol should document timer wake delivery");
        assertEquals(321, result.result, "Worker should still execute normally after protocol handshake");
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message during protocol handshake");
    }

    private static WorkerRunResult translateAndRunFixture(CompilerHelper.CompilerConfig config, String fixtureName, String appName) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-runtime-src");
        Path classesDir = Files.createTempDirectory("js-runtime-classes");
        Path javaApiDir = Files.createTempDirectory("js-runtime-javaapi");

        Files.write(sourceDir.resolve(appName + ".java"),
                JavascriptTargetIntegrationTest.loadFixture(fixtureName).getBytes(StandardCharsets.UTF_8));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-runtime-output");
        JavascriptTargetIntegrationTest.runJavascriptTranslator(classesDir, outputDir, appName);

        Path distDir = outputDir.resolve("dist").resolve(appName + "-js");
        return runWorkerBundle(distDir, appName);
    }

    private static WorkerRunResult runWorkerBundle(Path distDir, String appName) throws Exception {
        Path harness = Files.createTempFile("js-worker-runtime", ".js");
        Files.write(harness, workerHarnessSource(distDir, appName).getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder("node", harness.toString()).start();
        String output = readAll(process.getInputStream());
        String errors = readAll(process.getErrorStream());
        int rc = process.waitFor();
        assertEquals(0, rc, "Node worker harness should exit cleanly. stdout: " + output + " stderr: " + errors);
        WorkerRunResult out = new WorkerRunResult();
        out.rawMessage = output.trim();
        out.type = extractJsonString(output, "type");
        String result = extractJsonNumber(output, "result");
        out.result = result == null ? Integer.MIN_VALUE : Integer.parseInt(result);
        out.errorMessage = extractJsonString(output, "message");
        return out;
    }

    static WorkerRunResult runGeneratedWorkerBundle(Path distDir) throws Exception {
        Path harness = Files.createTempFile("js-worker-protocol", ".js");
        Files.write(harness, generatedWorkerHarnessSource(distDir).getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder("node", harness.toString()).start();
        String output = readAll(process.getInputStream());
        String errors = readAll(process.getErrorStream());
        int rc = process.waitFor();
        assertEquals(0, rc, "Node worker-thread harness should exit cleanly. stdout: " + output + " stderr: " + errors);
        WorkerRunResult out = new WorkerRunResult();
        out.rawMessage = output.trim();
        out.type = extractJsonString(output, "type");
        String result = extractJsonNumber(output, "result");
        out.result = result == null ? Integer.MIN_VALUE : Integer.parseInt(result);
        out.errorMessage = extractJsonString(output, "message");
        return out;
    }

    private static WorkerRunResult runGeneratedWorkerBundleWithHostCallbacks(Path distDir) throws Exception {
        Path harness = Files.createTempFile("js-worker-host-protocol", ".js");
        Files.write(harness, generatedWorkerHarnessSourceWithHostCallbacks(distDir).getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder("node", harness.toString()).start();
        String output = readAll(process.getInputStream());
        String errors = readAll(process.getErrorStream());
        int rc = process.waitFor();
        assertEquals(0, rc, "Node worker-thread host harness should exit cleanly. stdout: " + output + " stderr: " + errors);
        WorkerRunResult out = new WorkerRunResult();
        out.rawMessage = output.trim();
        out.type = extractJsonString(output, "type");
        String result = extractJsonNumber(output, "result");
        out.result = result == null ? Integer.MIN_VALUE : Integer.parseInt(result);
        out.errorMessage = extractJsonString(output, "message");
        return out;
    }

    private static WorkerRunResult runGeneratedWorkerBundleWithProtocolHandshake(Path distDir) throws Exception {
        Path harness = Files.createTempFile("js-worker-protocol-handshake", ".js");
        Files.write(harness, generatedWorkerHarnessSourceWithProtocolHandshake(distDir).getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder("node", harness.toString()).start();
        String output = readAll(process.getInputStream());
        String errors = readAll(process.getErrorStream());
        int rc = process.waitFor();
        assertEquals(0, rc, "Node worker-thread protocol handshake harness should exit cleanly. stdout: " + output + " stderr: " + errors);
        WorkerRunResult out = new WorkerRunResult();
        out.rawMessage = output.trim();
        out.type = extractJsonString(output, "type");
        String result = extractJsonNumber(output, "result");
        out.result = result == null ? Integer.MIN_VALUE : Integer.parseInt(result);
        String version = extractJsonNumber(output, "version");
        out.protocolVersion = version == null ? Integer.MIN_VALUE : Integer.parseInt(version);
        out.protocolStartType = extractJsonString(output, "startType");
        out.protocolHostCallbackType = extractJsonString(output, "hostCallbackType");
        out.protocolTimerWakeType = extractJsonString(output, "timerWakeType");
        out.errorMessage = extractJsonString(output, "message");
        return out;
    }

    private static WorkerRunResult runGeneratedWorkerBundleWithEventInjection(Path distDir) throws Exception {
        Path harness = Files.createTempFile("js-worker-event-protocol", ".js");
        Files.write(harness, generatedWorkerHarnessSourceWithEventInjection(distDir).getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder("node", harness.toString()).start();
        String output = readAll(process.getInputStream());
        String errors = readAll(process.getErrorStream());
        int rc = process.waitFor();
        assertEquals(0, rc, "Node worker-thread event harness should exit cleanly. stdout: " + output + " stderr: " + errors);
        WorkerRunResult out = new WorkerRunResult();
        out.rawMessage = output.trim();
        out.type = extractJsonString(output, "type");
        String result = extractJsonNumber(output, "result");
        out.result = result == null ? Integer.MIN_VALUE : Integer.parseInt(result);
        out.errorMessage = extractJsonString(output, "message");
        return out;
    }

    private static WorkerRunResult runGeneratedWorkerBundleWithQueuedEvents(Path distDir) throws Exception {
        Path harness = Files.createTempFile("js-worker-queued-events", ".js");
        Files.write(harness, generatedWorkerHarnessSourceWithQueuedEvents(distDir).getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder("node", harness.toString()).start();
        String output = readAll(process.getInputStream());
        String errors = readAll(process.getErrorStream());
        int rc = process.waitFor();
        assertEquals(0, rc, "Node worker-thread queued-event harness should exit cleanly. stdout: " + output + " stderr: " + errors);
        WorkerRunResult out = new WorkerRunResult();
        out.rawMessage = output.trim();
        out.type = extractJsonString(output, "type");
        String result = extractJsonNumber(output, "result");
        out.result = result == null ? Integer.MIN_VALUE : Integer.parseInt(result);
        out.errorMessage = extractJsonString(output, "message");
        return out;
    }

    private static WorkerRunResult runGeneratedWorkerBundleWithHostCallbackError(Path distDir) throws Exception {
        Path harness = Files.createTempFile("js-worker-host-error", ".js");
        Files.write(harness, generatedWorkerHarnessSourceWithHostCallbackError(distDir).getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder("node", harness.toString()).start();
        String output = readAll(process.getInputStream());
        String errors = readAll(process.getErrorStream());
        int rc = process.waitFor();
        assertEquals(0, rc, "Node worker-thread host-error harness should exit cleanly. stdout: " + output + " stderr: " + errors);
        WorkerRunResult out = new WorkerRunResult();
        out.rawMessage = output.trim();
        out.type = extractJsonString(output, "type");
        String result = extractJsonNumber(output, "result");
        out.result = result == null ? Integer.MIN_VALUE : Integer.parseInt(result);
        out.errorMessage = extractJsonString(output, "message");
        return out;
    }

    private static String readAll(InputStream input) throws Exception {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > -1) {
                out.write(buffer, 0, len);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String extractJsonString(String json, String key) {
        json = extractLastJsonObject(json);
        String marker = "\"" + key + "\":\"";
        int start = json.lastIndexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }

    private static String extractJsonNumber(String json, String key) {
        json = extractLastJsonObject(json);
        String marker = "\"" + key + "\":";
        int start = json.lastIndexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = start;
        while (end < json.length()) {
            char ch = json.charAt(end);
            if ((ch < '0' || ch > '9') && ch != '-') {
                break;
            }
            end++;
        }
        return json.substring(start, end);
    }

    private static String extractLastJsonObject(String output) {
        if (output == null || output.isEmpty()) {
            return "";
        }
        String[] lines = output.split("\\R");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}") && line.contains("\"type\"")) {
                return line;
            }
        }
        return output;
    }

    private static String workerHarnessSource(Path distDir, String appName) {
        return ""
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const vm = require('vm');\n"
                + "const __messages = [];\n"
                + "let __timerId = 1;\n"
                + "let __now = 0;\n"
                + "const __timers = [];\n"
                + "global.self = global;\n"
                + "global.window = global;\n"
                + "global.global = global;\n"
                + "Date.now = function() { return __now; };\n"
                + "global.setTimeout = function(fn, millis) {\n"
                + "  const timer = { id: __timerId++, due: __now + Math.max(0, millis | 0), fn: fn, cleared: false };\n"
                + "  __timers.push(timer);\n"
                + "  return timer;\n"
                + "};\n"
                + "global.clearTimeout = function(timer) {\n"
                + "  if (timer) {\n"
                + "    timer.cleared = true;\n"
                + "  }\n"
                + "};\n"
                + "global.postMessage = function(msg) { __messages.push(msg); };\n"
                + "global.importScripts = function() {\n"
                + "  for (const script of arguments) {\n"
                + "    const scriptPath = path.join(" + quoteJs(distDir.toString()) + ", String(script));\n"
                + "    let src = fs.readFileSync(scriptPath, 'utf8');\n"
                + "    if (String(script) === 'translated_app.js') {\n"
                + "      src += '\\nif (typeof jvm !== \"undefined\" && jvm.mainMethod) { global.__cn1ExportedMain = eval(jvm.mainMethod); }\\n';\n"
                + "    }\n"
                + "    vm.runInThisContext(src, { filename: scriptPath });\n"
                + "  }\n"
                + "};\n"
                + "importScripts('parparvm_runtime.js');\n"
                + "importScripts('translated_app.js');\n"
                + "const mainFn = global.__cn1ExportedMain;\n"
                + "const mainThreadObject = jvm.newObject('java_lang_Thread');\n"
                + "mainThreadObject.cn1_java_lang_Thread_alive = 1;\n"
                + "mainThreadObject.cn1_java_lang_Thread_name = jvm.createStringLiteral('main');\n"
                + "jvm.spawn(mainThreadObject, mainFn(jvm.newArray(0, 'java_lang_String', 1)));\n"
                + "while (jvm.runnable.length || __timers.length) {\n"
                + "  if (jvm.runnable.length) {\n"
                + "    jvm.drain();\n"
                + "    continue;\n"
                + "  }\n"
                + "  __timers.sort(function(a, b) { return a.due - b.due || a.id - b.id; });\n"
                + "  const timer = __timers.shift();\n"
                + "  if (!timer || timer.cleared) {\n"
                + "    continue;\n"
                + "  }\n"
                + "  __now = Math.max(__now, timer.due);\n"
                + "  timer.fn();\n"
                + "}\n"
                + "const resultValue = jvm.classes[" + quoteJs(appName) + "].staticFields['result'];\n"
                + "const finalMessage = __messages.length ? __messages[__messages.length - 1] : { type: 'result', result: resultValue };\n"
                + "if (finalMessage.type !== 'error') {\n"
                + "  finalMessage.type = 'result';\n"
                + "  finalMessage.result = resultValue;\n"
                + "}\n"
                + "console.log(JSON.stringify(finalMessage));\n";
    }

    private static String generatedWorkerHarnessSource(Path distDir) {
        return ""
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const { Worker } = require('worker_threads');\n"
                + "const bootstrapPath = path.join(" + quoteJs(distDir.toString()) + ", '__node_worker_bootstrap.js');\n"
                + "fs.writeFileSync(bootstrapPath, `\n"
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const vm = require('vm');\n"
                + "const { parentPort, workerData } = require('worker_threads');\n"
                + "global.self = global;\n"
                + "global.window = global;\n"
                + "global.global = global;\n"
                + "global.postMessage = function(msg) { parentPort.postMessage(msg); };\n"
                + "global.importScripts = function() {\n"
                + "  for (const script of arguments) {\n"
                + "    const scriptPath = path.join(workerData.distDir, String(script));\n"
                + "    const src = fs.readFileSync(scriptPath, 'utf8');\n"
                + "    vm.runInThisContext(src, { filename: scriptPath });\n"
                + "  }\n"
                + "};\n"
                + "parentPort.on('message', function(data) {\n"
                + "  if (typeof self.onmessage === 'function') {\n"
                + "    self.onmessage({ data: data });\n"
                + "  }\n"
                + "});\n"
                + "const workerSrc = fs.readFileSync(path.join(workerData.distDir, 'worker.js'), 'utf8');\n"
                + "vm.runInThisContext(workerSrc, { filename: path.join(workerData.distDir, 'worker.js') });\n"
                + "`);\n"
                + "const worker = new Worker(bootstrapPath, { workerData: { distDir: " + quoteJs(distDir.toString()) + " } });\n"
                + "let done = false;\n"
                + "worker.on('message', function(msg) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  if (msg && (msg.type === 'result' || msg.type === 'error')) {\n"
                + "    done = true;\n"
                + "    console.log(JSON.stringify(msg));\n"
                + "    worker.terminate().then(function() { process.exit(0); });\n"
                + "  }\n"
                + "});\n"
                + "worker.on('error', function(err) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  done = true;\n"
                + "  console.log(JSON.stringify({ type: 'error', message: String(err) }));\n"
                + "  process.exit(1);\n"
                + "});\n"
                + "worker.postMessage({ type: 'start' });\n"
                + "setTimeout(function() {\n"
                + "  if (!done) {\n"
                + "    console.log(JSON.stringify({ type: 'error', message: 'Timed out waiting for worker result' }));\n"
                + "    worker.terminate().then(function() { process.exit(1); });\n"
                + "  }\n"
                + "}, 10000);\n";
    }

    private static String generatedWorkerHarnessSourceWithHostCallbacks(Path distDir) {
        return ""
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const { Worker } = require('worker_threads');\n"
                + "const bootstrapPath = path.join(" + quoteJs(distDir.toString()) + ", '__node_worker_bootstrap_host.js');\n"
                + "fs.writeFileSync(bootstrapPath, `\n"
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const vm = require('vm');\n"
                + "const { parentPort, workerData } = require('worker_threads');\n"
                + "global.self = global;\n"
                + "global.window = global;\n"
                + "global.global = global;\n"
                + "global.postMessage = function(msg) { parentPort.postMessage(msg); };\n"
                + "global.importScripts = function() {\n"
                + "  for (const script of arguments) {\n"
                + "    const scriptPath = path.join(workerData.distDir, String(script));\n"
                + "    const src = fs.readFileSync(scriptPath, 'utf8');\n"
                + "    vm.runInThisContext(src, { filename: scriptPath });\n"
                + "  }\n"
                + "};\n"
                + "parentPort.on('message', function(data) {\n"
                + "  if (typeof self.onmessage === 'function') {\n"
                + "    self.onmessage({ data: data });\n"
                + "  }\n"
                + "});\n"
                + "const workerSrc = fs.readFileSync(path.join(workerData.distDir, 'worker.js'), 'utf8');\n"
                + "vm.runInThisContext(workerSrc, { filename: path.join(workerData.distDir, 'worker.js') });\n"
                + "`);\n"
                + "const worker = new Worker(bootstrapPath, { workerData: { distDir: " + quoteJs(distDir.toString()) + " } });\n"
                + "let done = false;\n"
                + "worker.on('message', function(msg) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  if (msg && msg.type === 'host-call') {\n"
                + "    if (msg.symbol === 'cn1_com_codename1_impl_platform_js_VMHost_echoInt_int_R_int') {\n"
                + "      worker.postMessage({ type: 'host-callback', id: msg.id, value: (msg.args[0] | 0) + 1 });\n"
                + "      return;\n"
                + "    }\n"
                + "    worker.postMessage({ type: 'host-callback', id: msg.id, error: true, errorMessage: 'Unexpected host call ' + msg.symbol });\n"
                + "    return;\n"
                + "  }\n"
                + "  if (msg && (msg.type === 'result' || msg.type === 'error')) {\n"
                + "    done = true;\n"
                + "    console.log(JSON.stringify(msg));\n"
                + "    worker.terminate().then(function() { process.exit(0); });\n"
                + "  }\n"
                + "});\n"
                + "worker.on('error', function(err) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  done = true;\n"
                + "  console.log(JSON.stringify({ type: 'error', message: String(err) }));\n"
                + "  process.exit(1);\n"
                + "});\n"
                + "worker.postMessage({ type: 'start' });\n"
                + "setTimeout(function() {\n"
                + "  if (!done) {\n"
                + "    console.log(JSON.stringify({ type: 'error', message: 'Timed out waiting for worker result' }));\n"
                + "    worker.terminate().then(function() { process.exit(1); });\n"
                + "  }\n"
                + "}, 10000);\n";
    }

    private static String generatedWorkerHarnessSourceWithProtocolHandshake(Path distDir) {
        return ""
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const { Worker } = require('worker_threads');\n"
                + "const bootstrapPath = path.join(" + quoteJs(distDir.toString()) + ", '__node_worker_bootstrap_protocol.js');\n"
                + "fs.writeFileSync(bootstrapPath, `\n"
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const vm = require('vm');\n"
                + "const { parentPort, workerData } = require('worker_threads');\n"
                + "global.self = global;\n"
                + "global.window = global;\n"
                + "global.global = global;\n"
                + "global.postMessage = function(msg) { parentPort.postMessage(msg); };\n"
                + "global.importScripts = function() {\n"
                + "  for (const script of arguments) {\n"
                + "    const scriptPath = path.join(workerData.distDir, String(script));\n"
                + "    const src = fs.readFileSync(scriptPath, 'utf8');\n"
                + "    vm.runInThisContext(src, { filename: scriptPath });\n"
                + "  }\n"
                + "};\n"
                + "parentPort.on('message', function(data) {\n"
                + "  if (typeof self.onmessage === 'function') {\n"
                + "    self.onmessage({ data: data });\n"
                + "  }\n"
                + "});\n"
                + "const workerSrc = fs.readFileSync(path.join(workerData.distDir, 'worker.js'), 'utf8');\n"
                + "vm.runInThisContext(workerSrc, { filename: path.join(workerData.distDir, 'worker.js') });\n"
                + "`);\n"
                + "const worker = new Worker(bootstrapPath, { workerData: { distDir: " + quoteJs(distDir.toString()) + " } });\n"
                + "let done = false;\n"
                + "let protocol = null;\n"
                + "worker.on('message', function(msg) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  if (msg && msg.type === 'protocol') {\n"
                + "    protocol = msg;\n"
                + "    worker.postMessage({ type: protocol.messages.START });\n"
                + "    return;\n"
                + "  }\n"
                + "  if (msg && (msg.type === 'result' || msg.type === 'error')) {\n"
                + "    done = true;\n"
                + "    if (msg.type === 'error' || !protocol) {\n"
                + "      console.log(JSON.stringify(msg));\n"
                + "    } else {\n"
                + "      console.log(JSON.stringify({\n"
                + "        type: 'protocol-check',\n"
                + "        version: protocol.version,\n"
                + "        startType: protocol.messages.START,\n"
                + "        hostCallbackType: protocol.messages.HOST_CALLBACK,\n"
                + "        timerWakeType: protocol.messages.TIMER_WAKE,\n"
                + "        result: msg.result\n"
                + "      }));\n"
                + "    }\n"
                + "    worker.terminate().then(function() { process.exit(0); });\n"
                + "  }\n"
                + "});\n"
                + "worker.on('error', function(err) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  done = true;\n"
                + "  console.log(JSON.stringify({ type: 'error', message: String(err) }));\n"
                + "  process.exit(1);\n"
                + "});\n"
                + "worker.postMessage({ type: 'protocol-info' });\n"
                + "setTimeout(function() {\n"
                + "  if (!done) {\n"
                + "    console.log(JSON.stringify({ type: 'error', message: 'Timed out waiting for worker protocol handshake' }));\n"
                + "    worker.terminate().then(function() { process.exit(1); });\n"
                + "  }\n"
                + "}, 10000);\n";
    }

    private static String generatedWorkerHarnessSourceWithEventInjection(Path distDir) {
        return ""
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const { Worker } = require('worker_threads');\n"
                + "const bootstrapPath = path.join(" + quoteJs(distDir.toString()) + ", '__node_worker_bootstrap_event.js');\n"
                + "fs.writeFileSync(bootstrapPath, `\n"
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const vm = require('vm');\n"
                + "const { parentPort, workerData } = require('worker_threads');\n"
                + "global.self = global;\n"
                + "global.window = global;\n"
                + "global.global = global;\n"
                + "global.postMessage = function(msg) { parentPort.postMessage(msg); };\n"
                + "global.importScripts = function() {\n"
                + "  for (const script of arguments) {\n"
                + "    const scriptPath = path.join(workerData.distDir, String(script));\n"
                + "    const src = fs.readFileSync(scriptPath, 'utf8');\n"
                + "    vm.runInThisContext(src, { filename: scriptPath });\n"
                + "  }\n"
                + "};\n"
                + "parentPort.on('message', function(data) {\n"
                + "  if (typeof self.onmessage === 'function') {\n"
                + "    self.onmessage({ data: data });\n"
                + "  }\n"
                + "});\n"
                + "const workerSrc = fs.readFileSync(path.join(workerData.distDir, 'worker.js'), 'utf8');\n"
                + "vm.runInThisContext(workerSrc, { filename: path.join(workerData.distDir, 'worker.js') });\n"
                + "`);\n"
                + "const worker = new Worker(bootstrapPath, { workerData: { distDir: " + quoteJs(distDir.toString()) + " } });\n"
                + "let done = false;\n"
                + "worker.on('message', function(msg) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  if (msg && msg.type === 'host-call') {\n"
                + "    if (msg.symbol === 'cn1_com_codename1_impl_platform_js_VMHost_echoInt_int_R_int') {\n"
                + "      worker.postMessage({ type: 'host-callback', id: msg.id, value: (msg.args[0] | 0) + 1 });\n"
                + "      return;\n"
                + "    }\n"
                + "    worker.postMessage({ type: 'host-callback', id: msg.id, error: true, errorMessage: 'Unexpected host call ' + msg.symbol });\n"
                + "    return;\n"
                + "  }\n"
                + "  if (msg && (msg.type === 'result' || msg.type === 'error')) {\n"
                + "    done = true;\n"
                + "    console.log(JSON.stringify(msg));\n"
                + "    worker.terminate().then(function() { process.exit(0); });\n"
                + "  }\n"
                + "});\n"
                + "worker.on('error', function(err) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  done = true;\n"
                + "  console.log(JSON.stringify({ type: 'error', message: String(err) }));\n"
                + "  process.exit(1);\n"
                + "});\n"
                + "worker.postMessage({ type: 'unknown-protocol-message', code: 999 });\n"
                + "worker.postMessage({ type: 'event', code: 41 });\n"
                + "worker.postMessage({ type: 'start' });\n"
                + "setTimeout(function() {\n"
                + "  if (!done) {\n"
                + "    console.log(JSON.stringify({ type: 'error', message: 'Timed out waiting for worker event injection result' }));\n"
                + "    worker.terminate().then(function() { process.exit(1); });\n"
                + "  }\n"
                + "}, 10000);\n";
    }

    private static String generatedWorkerHarnessSourceWithQueuedEvents(Path distDir) {
        return ""
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const { Worker } = require('worker_threads');\n"
                + "const bootstrapPath = path.join(" + quoteJs(distDir.toString()) + ", '__node_worker_bootstrap_queue.js');\n"
                + "fs.writeFileSync(bootstrapPath, `\n"
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const vm = require('vm');\n"
                + "const { parentPort, workerData } = require('worker_threads');\n"
                + "global.self = global;\n"
                + "global.window = global;\n"
                + "global.global = global;\n"
                + "global.postMessage = function(msg) { parentPort.postMessage(msg); };\n"
                + "global.importScripts = function() {\n"
                + "  for (const script of arguments) {\n"
                + "    const scriptPath = path.join(workerData.distDir, String(script));\n"
                + "    const src = fs.readFileSync(scriptPath, 'utf8');\n"
                + "    vm.runInThisContext(src, { filename: scriptPath });\n"
                + "  }\n"
                + "};\n"
                + "parentPort.on('message', function(data) {\n"
                + "  if (typeof self.onmessage === 'function') {\n"
                + "    self.onmessage({ data: data });\n"
                + "  }\n"
                + "});\n"
                + "const workerSrc = fs.readFileSync(path.join(workerData.distDir, 'worker.js'), 'utf8');\n"
                + "vm.runInThisContext(workerSrc, { filename: path.join(workerData.distDir, 'worker.js') });\n"
                + "`);\n"
                + "const worker = new Worker(bootstrapPath, { workerData: { distDir: " + quoteJs(distDir.toString()) + " } });\n"
                + "let done = false;\n"
                + "worker.on('message', function(msg) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  if (msg && (msg.type === 'result' || msg.type === 'error')) {\n"
                + "    done = true;\n"
                + "    console.log(JSON.stringify(msg));\n"
                + "    worker.terminate().then(function() { process.exit(0); });\n"
                + "  }\n"
                + "});\n"
                + "worker.on('error', function(err) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  done = true;\n"
                + "  console.log(JSON.stringify({ type: 'error', message: String(err) }));\n"
                + "  process.exit(1);\n"
                + "});\n"
                + "worker.postMessage({ type: 'event', code: 4 });\n"
                + "worker.postMessage({ type: 'ui-event', code: 12 });\n"
                + "worker.postMessage({ type: 'start' });\n"
                + "setTimeout(function() {\n"
                + "  if (!done) {\n"
                + "    console.log(JSON.stringify({ type: 'error', message: 'Timed out waiting for queued event result' }));\n"
                + "    worker.terminate().then(function() { process.exit(1); });\n"
                + "  }\n"
                + "}, 10000);\n";
    }

    private static String generatedWorkerHarnessSourceWithHostCallbackError(Path distDir) {
        return ""
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const { Worker } = require('worker_threads');\n"
                + "const bootstrapPath = path.join(" + quoteJs(distDir.toString()) + ", '__node_worker_bootstrap_host_error.js');\n"
                + "fs.writeFileSync(bootstrapPath, `\n"
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const vm = require('vm');\n"
                + "const { parentPort, workerData } = require('worker_threads');\n"
                + "global.self = global;\n"
                + "global.window = global;\n"
                + "global.global = global;\n"
                + "global.postMessage = function(msg) { parentPort.postMessage(msg); };\n"
                + "global.importScripts = function() {\n"
                + "  for (const script of arguments) {\n"
                + "    const scriptPath = path.join(workerData.distDir, String(script));\n"
                + "    const src = fs.readFileSync(scriptPath, 'utf8');\n"
                + "    vm.runInThisContext(src, { filename: scriptPath });\n"
                + "  }\n"
                + "};\n"
                + "parentPort.on('message', function(data) {\n"
                + "  if (typeof self.onmessage === 'function') {\n"
                + "    self.onmessage({ data: data });\n"
                + "  }\n"
                + "});\n"
                + "const workerSrc = fs.readFileSync(path.join(workerData.distDir, 'worker.js'), 'utf8');\n"
                + "vm.runInThisContext(workerSrc, { filename: path.join(workerData.distDir, 'worker.js') });\n"
                + "`);\n"
                + "const worker = new Worker(bootstrapPath, { workerData: { distDir: " + quoteJs(distDir.toString()) + " } });\n"
                + "let done = false;\n"
                + "worker.on('message', function(msg) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  if (msg && msg.type === 'host-call') {\n"
                + "    worker.postMessage({ type: 'host-callback', id: msg.id, error: true, errorMessage: 'Injected host failure ' + (msg.args && msg.args.length ? msg.args[0] : '?') });\n"
                + "    return;\n"
                + "  }\n"
                + "  if (msg && (msg.type === 'result' || msg.type === 'error')) {\n"
                + "    done = true;\n"
                + "    console.log(JSON.stringify(msg));\n"
                + "    worker.terminate().then(function() { process.exit(0); });\n"
                + "  }\n"
                + "});\n"
                + "worker.on('error', function(err) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  done = true;\n"
                + "  console.log(JSON.stringify({ type: 'error', message: String(err) }));\n"
                + "  process.exit(1);\n"
                + "});\n"
                + "worker.postMessage({ type: 'start' });\n"
                + "setTimeout(function() {\n"
                + "  if (!done) {\n"
                + "    console.log(JSON.stringify({ type: 'error', message: 'Timed out waiting for host error result' }));\n"
                + "    worker.terminate().then(function() { process.exit(1); });\n"
                + "  }\n"
                + "}, 10000);\n";
    }

    private static String quoteJs(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    static final class WorkerRunResult {
        String type;
        int result;
        String errorMessage;
        String rawMessage;
        int protocolVersion;
        String protocolStartType;
        String protocolHostCallbackType;
        String protocolTimerWakeType;
    }
}
