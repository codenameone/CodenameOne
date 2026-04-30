package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavascriptTargetIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void generatesBrowserBundleForJavascriptTarget(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-target-sources");
        Path classesDir = Files.createTempDirectory("js-target-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        Files.write(sourceDir.resolve("JsHello.java"), loadFixture("JsHello.java").getBytes(StandardCharsets.UTF_8));

        compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-target-output");
        runJavascriptTranslator(classesDir, outputDir, "JsHello");

        Path distDir = outputDir.resolve("dist").resolve("JsHello-js");
        assertTrue(Files.exists(distDir.resolve("index.html")), "Translator should emit a minimal host page");
        assertTrue(Files.exists(distDir.resolve("worker.js")), "Translator should emit a worker bootstrap");
        assertTrue(Files.exists(distDir.resolve("parparvm_runtime.js")), "Translator should emit a JS runtime");
        assertTrue(Files.exists(distDir.resolve("translated_app.js")), "Translator should emit translated classes");
        assertTrue(Files.exists(distDir.resolve("vm_protocol.md")), "Translator should emit the VM protocol contract artifact");
        assertTrue(Files.exists(distDir.resolve("browser_bridge.js")), "Translator should emit a browser bootstrap bridge");
        assertTrue(Files.exists(distDir.resolve(Paths.get("js", "fontmetrics.js"))), "Translator should copy JavaScript port font metrics support");
        assertTrue(Files.exists(distDir.resolve("style.css")), "Translator should copy the JavaScript port stylesheet");

        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);
        String runtime = new String(Files.readAllBytes(distDir.resolve("parparvm_runtime.js")), StandardCharsets.UTF_8);
        String worker = new String(Files.readAllBytes(distDir.resolve("worker.js")), StandardCharsets.UTF_8);
        String protocolDoc = new String(Files.readAllBytes(distDir.resolve("vm_protocol.md")), StandardCharsets.UTF_8);
        String index = new String(Files.readAllBytes(distDir.resolve("index.html")), StandardCharsets.UTF_8);
        String browserBridge = new String(Files.readAllBytes(distDir.resolve("browser_bridge.js")), StandardCharsets.UTF_8);

        assertTrue(translatedApp.contains("function*") && translatedApp.contains("JsHello"),
                "Main class should contribute translated JS generator functions");
        assertTrue(translatedApp.contains("jvm.setMain(\"JsHello\""),
                "Bundle should register the translated main entrypoint");
        assertTrue(runtime.contains("cn1_java_lang_Thread_start") || runtime.contains("cn1_java_lang_Thread_start__"),
                "Runtime should provide JS native implementations for thread start");
        assertTrue(runtime.contains("cn1_java_lang_Object_getClass_R_java_lang_Class")
                        && runtime.contains("cn1_java_lang_Object_getClassImpl_R_java_lang_Class"),
                "Runtime should bind both Object.getClass() and its native getClassImpl() helper");
        assertTrue(runtime.contains("cn1_java_io_PrintStream_println_java_lang_String")
                        && runtime.contains("cn1_java_io_PrintStream_println_java_lang_Object"),
                "Runtime should bind PrintStream output methods so browser harness logs can observe CN1SS markers");
        assertTrue(runtime.contains("cn1_java_lang_Object_wait_long_int") || runtime.contains("cn1_java_lang_Object_wait___long_int"),
                "Runtime should provide JS native implementations for wait()");
        assertTrue(!translatedApp.contains("Missing javascript native method cn1_java_lang_Object_wait_long_int"),
                "Translated bundle should not emit generic fallback stubs for runtime-implemented natives");
        assertTrue(!translatedApp.contains("staticFields: {\"CRLF\": jvm.createStringLiteral("),
                "Static string constants should be materialized during class initialization, not at top-level class registration");
        assertTrue(!translatedApp.contains("Missing javascript native method cn1_java_util_Locale_getOSLanguage_R_java_lang_String"),
                "Translated bundle should not emit generic fallback stubs for Locale natives");
        assertTrue(!translatedApp.contains("Missing javascript native method cn1_java_util_TimeZone_getTimezoneId_R_java_lang_String"),
                "Translated bundle should not emit generic fallback stubs for TimeZone natives");
        assertTrue(!translatedApp.contains("Missing javascript native method cn1_java_text_DateFormat_format_java_util_Date_java_lang_StringBuffer_R_java_lang_String"),
                "Translated bundle should not emit generic fallback stubs for DateFormat natives");
        assertTrue(!translatedApp.contains("__args.unshift(stack.pop())"),
                "Translated invoke paths should avoid array unshift-based argument packing");
        assertTrue(!translatedApp.contains("cn1_java_io_File_")
                        || translatedApp.contains("java.io.File native filesystem access is not supported in javascript backend"),
                "Unsupported filesystem natives should fail with an explicit JS-mode message when translated");
        assertTrue(worker.contains("importScripts('parparvm_runtime.js');"),
                "Worker bootstrap should load the runtime first");
        assertTrue(worker.contains("importScripts('port.js');"),
                "Worker bootstrap should load JavaScriptPort native bindings");
        assertTrue(worker.contains("__parparInstallNativeBindings"),
                "Worker bootstrap should reapply runtime native bindings after translated app load");
        assertTrue(index.contains("browser_bridge.js") && index.contains("js/fontmetrics.js") && index.contains("codenameone-canvas"),
                "Generated host page should use the JavaScript port browser shell assets");
        assertTrue(browserBridge.contains("cn1HostBridge") && browserBridge.contains("host-callback") && browserBridge.contains("host-call"),
                "Browser bridge should expose host-call plumbing for the JavaScript port shell");
        assertTrue(protocolDoc.contains("Version: 1")
                        && protocolDoc.contains("host-call")
                        && protocolDoc.contains("host-callback")
                        && protocolDoc.contains("timer-wake"),
                "Protocol artifact should document the stable worker boundary and host hook categories");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void generatesWaitNotifyFriendlyJavascriptBundle(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-thread-sources");
        Path classesDir = Files.createTempDirectory("js-thread-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        Files.write(sourceDir.resolve("JsThreadingApp.java"), loadFixture("JsThreadingApp.java").getBytes(StandardCharsets.UTF_8));

        compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-thread-output");
        runJavascriptTranslator(classesDir, outputDir, "JsThreadingApp");

        Path distDir = outputDir.resolve("dist").resolve("JsThreadingApp-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);
        String runtime = new String(Files.readAllBytes(distDir.resolve("parparvm_runtime.js")), StandardCharsets.UTF_8);

        assertTrue(translatedApp.contains("jvm.setMain(\"JsThreadingApp\""),
                "Threaded app should register a translated main entrypoint");
        assertTrue(translatedApp.contains("waitForSignal"),
                "Inner-class wait loop should be present in translated output");
        assertTrue(runtime.contains("waitOn(thread, obj, timeout)"),
                "Runtime should include cooperative monitor wait support");
        assertTrue(runtime.contains("notifyAll(obj)"),
                "Runtime should include cooperative notifyAll support");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void representativeJavascriptBundleHasNoUncategorizedNativeFallbacks(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-fallback-sources");
        Path classesDir = Files.createTempDirectory("js-fallback-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-fallback-classes");

        Files.write(sourceDir.resolve("JsLocaleTimeZoneApp.java"), loadFixture("JsLocaleTimeZoneApp.java").getBytes(StandardCharsets.UTF_8));

        compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-fallback-output");
        runJavascriptTranslator(classesDir, outputDir, "JsLocaleTimeZoneApp");

        Path distDir = outputDir.resolve("dist").resolve("JsLocaleTimeZoneApp-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);

        assertTrue(!translatedApp.contains("Missing javascript native method "),
                "Representative JS bundles should not retain uncategorized native fallback stubs");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void broaderJavaApiBundleHasNoUncategorizedNativeFallbacks(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-javaapi-sources");
        Path classesDir = Files.createTempDirectory("js-javaapi-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-javaapi-classes");

        Files.write(sourceDir.resolve("JsJavaApiCoverageApp.java"), loadFixture("JsJavaApiCoverageApp.java").getBytes(StandardCharsets.UTF_8));

        compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-javaapi-output");
        runJavascriptTranslator(classesDir, outputDir, "JsJavaApiCoverageApp");

        Path distDir = outputDir.resolve("dist").resolve("JsJavaApiCoverageApp-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);

        assertTrue(!translatedApp.contains("Missing javascript native method "),
                "Broader JavaAPI JS bundles should not retain uncategorized native fallback stubs");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void hostHookNativesGenerateVmHostCalls(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-host-hook-sources");
        Path classesDir = Files.createTempDirectory("js-host-hook-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-host-hook-classes");

        Path vmHostDir = sourceDir.resolve("com").resolve("codename1").resolve("impl").resolve("platform").resolve("js");
        Files.createDirectories(vmHostDir);
        Files.write(vmHostDir.resolve("VMHost.java"), loadFixture("com/codename1/impl/platform/js/VMHost.java").getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("JsHostCallbackApp.java"), loadFixture("JsHostCallbackApp.java").getBytes(StandardCharsets.UTF_8));

        compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-host-hook-output");
        runJavascriptTranslator(classesDir, outputDir, "JsHostCallbackApp");

        Path distDir = outputDir.resolve("dist").resolve("JsHostCallbackApp-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);

        assertTrue(translatedApp.contains("jvm.invokeHostNative(\"cn1_com_codename1_impl_platform_js_VMHost_echoInt_int_R_int\""),
                "Host-hook natives should compile to VM host-call stubs");
        assertTrue(!translatedApp.contains("Missing javascript native method cn1_com_codename1_impl_platform_js_VMHost_echoInt_int_R_int"),
                "Host-hook natives should not compile to generic missing-native stubs");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void simpleStraightLineMethodsLowerToLocalsInsteadOfInterpreterLoop(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-straight-line-sources");
        Path classesDir = Files.createTempDirectory("js-straight-line-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-straight-line-classes");

        Files.write(sourceDir.resolve("JsStraightLine.java"), loadFixture("JsStraightLine.java").getBytes(StandardCharsets.UTF_8));

        compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-straight-line-output");
        runJavascriptTranslator(classesDir, outputDir, "JsStraightLine");

        Path distDir = outputDir.resolve("dist").resolve("JsStraightLine-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);

        String marker = "function* cn1_JsStraightLine_add_int_int_R_int__impl(__cn1Arg1, __cn1Arg2){";
        int start = translatedApp.indexOf(marker);
        assertTrue(start >= 0, "Straight-line fixture should emit the add() method");
        int end = translatedApp.indexOf("\n}\n", start);
        assertTrue(end > start, "Straight-line fixture should have a bounded method body");
        String methodBody = translatedApp.substring(start, end);

        assertTrue(methodBody.contains("let l0 = __cn1Arg1;") && methodBody.contains("let l1 = __cn1Arg2;"),
                "Straight-line lowering should use direct local variables for arguments");
        assertTrue(!methodBody.contains("stack["),
                "Straight-line lowering should avoid stack-array indexing");
        assertTrue(!methodBody.contains("const locals = new Array") && !methodBody.contains("const stack = []") && !methodBody.contains("let pc = 0"),
                "Straight-line lowering should avoid the interpreter locals/stack/pc loop");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void syntheticAccessorAnonymousRunnableFallsBackWithoutCrashing(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-captured-runnable-sources");
        Path classesDir = Files.createTempDirectory("js-captured-runnable-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-captured-runnable-classes");

        Files.write(sourceDir.resolve("JsCapturedRunnable.java"), loadFixture("JsCapturedRunnable.java").getBytes(StandardCharsets.UTF_8));

        compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-captured-runnable-output");
        runJavascriptTranslator(classesDir, outputDir, "JsCapturedRunnable");

        Path distDir = outputDir.resolve("dist").resolve("JsCapturedRunnable-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);

        assertTrue(translatedApp.contains("JsCapturedRunnable"),
                "Translator should emit a bundle for the synthetic-accessor runnable fixture");
        assertTrue(translatedApp.contains("jvm.setMain(\"JsCapturedRunnable\""),
                "Translator should complete bundle generation for the synthetic-accessor runnable fixture");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void auxiliaryMainMethodsDoNotOverrideBundleEntrypoint(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-aux-main-sources");
        Path classesDir = Files.createTempDirectory("js-aux-main-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-aux-main-classes");

        Files.write(sourceDir.resolve("JsAuxiliaryMainApp.java"), loadFixture("JsAuxiliaryMainApp.java").getBytes(StandardCharsets.UTF_8));

        compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-aux-main-output");
        runJavascriptTranslator(classesDir, outputDir, "JsAuxiliaryMainApp");

        Path distDir = outputDir.resolve("dist").resolve("JsAuxiliaryMainApp-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);

        assertTrue(translatedApp.contains("jvm.setMain(\"JsAuxiliaryMainApp\""),
                "Translator should keep the first application main as the bundle entrypoint");
        assertTrue(translatedApp.contains("HelperMain"),
                "Translator should still include auxiliary classes that happen to declare a main()");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void lambdaConstructorShapeTranslatesWithoutBasicVarOpcodeFailure(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-lambda-capture-sources");
        Path classesDir = Files.createTempDirectory("js-lambda-capture-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-lambda-capture-classes");

        Files.write(sourceDir.resolve("JsLambdaCapture.java"), loadFixture("JsLambdaCapture.java").getBytes(StandardCharsets.UTF_8));

        compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-lambda-capture-output");
        runJavascriptTranslator(classesDir, outputDir, "JsLambdaCapture");

        Path distDir = outputDir.resolve("dist").resolve("JsLambdaCapture-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);

        assertTrue(translatedApp.contains("jvm.setMain(\"JsLambdaCapture\""),
                "Translator should complete bundle generation for the lambda-capture fixture");
        assertTrue(translatedApp.contains("JsLambdaCapture"),
                "Translated output should include the lambda-capture fixture classes");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void repeatedStaticAccessesOnlyEmitOneClassInitCheckInStraightLineMode(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-static-access-sources");
        Path classesDir = Files.createTempDirectory("js-static-access-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-static-access-classes");

        Files.write(sourceDir.resolve("JsStaticAccess.java"), loadFixture("JsStaticAccess.java").getBytes(StandardCharsets.UTF_8));

        compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-static-access-output");
        runJavascriptTranslator(classesDir, outputDir, "JsStaticAccess");

        Path distDir = outputDir.resolve("dist").resolve("JsStaticAccess-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);

        String marker = "function* cn1_JsStaticAccess_twice_R_int__impl(){";
        int start = translatedApp.indexOf(marker);
        assertTrue(start >= 0, "Static access fixture should emit the twice() method");
        int end = translatedApp.indexOf("\n}\n", start);
        assertTrue(end > start, "Static access fixture should have a bounded method body");
        String methodBody = translatedApp.substring(start, end);

        String initCheck = "jvm.ensureClassInitialized(\"JsStaticAccess\");";
        assertEquals(methodBody.indexOf(initCheck), methodBody.lastIndexOf(initCheck),
                "Repeated static field access should only emit one class-init check in straight-line mode");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void repeatedStaticAccessesUseMethodLevelInitCacheInInterpreterMode(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-static-flow-sources");
        Path classesDir = Files.createTempDirectory("js-static-flow-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-static-flow-classes");

        Files.write(sourceDir.resolve("JsStaticAccessFlow.java"), loadFixture("JsStaticAccessFlow.java").getBytes(StandardCharsets.UTF_8));

        compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-static-flow-output");
        runJavascriptTranslator(classesDir, outputDir, "JsStaticAccessFlow");

        Path distDir = outputDir.resolve("dist").resolve("JsStaticAccessFlow-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);

        String marker = "function* cn1_JsStaticAccessFlow_pick_int_R_int__impl(__cn1Arg1){";
        int start = translatedApp.indexOf(marker);
        assertTrue(start >= 0, "Interpreter static access fixture should emit the pick() method");
        int end = translatedApp.indexOf("\n}\n", start);
        assertTrue(end > start, "Interpreter static access fixture should have a bounded method body");
        String methodBody = translatedApp.substring(start, end);

        assertTrue(methodBody.contains("const __cn1Init = Object.create(null);"),
                "Interpreter mode should allocate a method-level static init cache when static fields are used");
        assertTrue(methodBody.contains("if (!__cn1Init[\"JsStaticAccessFlow\"]) { jvm.ensureClassInitialized(\"JsStaticAccessFlow\"); __cn1Init[\"JsStaticAccessFlow\"] = true; }"),
                "Interpreter mode should guard repeated static field access behind the method-level init cache");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void repeatedStaticInvokesUseMethodLevelInitCacheAndInternalImpls(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-static-invoke-sources");
        Path classesDir = Files.createTempDirectory("js-static-invoke-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-static-invoke-classes");

        Files.write(sourceDir.resolve("JsStaticInvokeFlow.java"), loadFixture("JsStaticInvokeFlow.java").getBytes(StandardCharsets.UTF_8));

        compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-static-invoke-output");
        runJavascriptTranslator(classesDir, outputDir, "JsStaticInvokeFlow");

        Path distDir = outputDir.resolve("dist").resolve("JsStaticInvokeFlow-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);

        String callerMarker = "function* cn1_JsStaticInvokeFlow_pick_int_R_int__impl(__cn1Arg1){";
        int callerStart = translatedApp.indexOf(callerMarker);
        assertTrue(callerStart >= 0, "Static invoke fixture should emit an internal implementation for pick()");
        int callerEnd = translatedApp.indexOf("\n}\n", callerStart);
        assertTrue(callerEnd > callerStart, "Static invoke fixture should have a bounded pick() body");
        String callerBody = translatedApp.substring(callerStart, callerEnd);

        assertTrue(callerBody.contains("const __cn1Init = Object.create(null);"),
                "Interpreter static invoke caller should allocate a method-level init cache");
        assertTrue(callerBody.contains("typeof cn1_JsStaticInvokeFlow_helper_R_int__impl === \"function\" ? cn1_JsStaticInvokeFlow_helper_R_int__impl : cn1_JsStaticInvokeFlow_helper_R_int"),
                "Static invoke caller should target the internal implementation when available");
        assertTrue(callerBody.contains("if (!__cn1Init[\"JsStaticInvokeFlow\"]) { jvm.ensureClassInitialized(\"JsStaticInvokeFlow\"); __cn1Init[\"JsStaticInvokeFlow\"] = true; }"),
                "Static invoke caller should guard repeated class init through the method-level cache");

        String calleeMarker = "function* cn1_JsStaticInvokeFlow_helper_R_int__impl(){";
        int calleeStart = translatedApp.indexOf(calleeMarker);
        assertTrue(calleeStart >= 0, "Static invoke fixture should emit an internal implementation for helper()");
        int calleeEnd = translatedApp.indexOf("\n}\n", calleeStart);
        assertTrue(calleeEnd > calleeStart, "Static invoke fixture should have a bounded helper() body");
        String calleeBody = translatedApp.substring(calleeStart, calleeEnd);

        assertTrue(!calleeBody.contains("jvm.ensureClassInitialized(\"JsStaticInvokeFlow\")"),
                "Internal static method implementations should not repeat class-init guards");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void repeatedVirtualInvokesUseMethodLevelDispatchCacheInInterpreterMode(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("js-virtual-invoke-sources");
        Path classesDir = Files.createTempDirectory("js-virtual-invoke-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-virtual-invoke-classes");

        Files.write(sourceDir.resolve("JsVirtualInvokeFlow.java"), loadFixture("JsVirtualInvokeFlow.java").getBytes(StandardCharsets.UTF_8));

        compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-virtual-invoke-output");
        runJavascriptTranslator(classesDir, outputDir, "JsVirtualInvokeFlow");

        Path distDir = outputDir.resolve("dist").resolve("JsVirtualInvokeFlow-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);

        String marker = "function* cn1_JsVirtualInvokeFlow_repeat_JsVirtualInvokeBase_int_R_int__impl(__cn1Arg1, __cn1Arg2){";
        int start = translatedApp.indexOf(marker);
        assertTrue(start >= 0, "Virtual invoke fixture should emit the repeat() method");
        int end = translatedApp.indexOf("\n}\n", start);
        assertTrue(end > start, "Virtual invoke fixture should have a bounded repeat() body");
        String methodBody = translatedApp.substring(start, end);

        assertTrue(methodBody.contains("const __cn1Virtual = Object.create(null);"),
                "Interpreter-mode virtual dispatch should allocate a per-method cache");
        assertTrue(methodBody.contains("const __cacheKey = __target.__class + \"|cn1_JsVirtualInvokeBase_value_R_int\";"),
                "Virtual dispatch cache should key on runtime class and method id");
        assertTrue(methodBody.contains("__method = __cn1Virtual[__cacheKey];")
                        && methodBody.contains("__cn1Virtual[__cacheKey] = __method;"),
                "Virtual dispatch cache should store and reuse resolved fallback methods");
    }

    static void compileAgainstJavaApi(CompilerHelper.CompilerConfig config, Path sourceDir, Path classesDir, Path javaApiDir) throws Exception {
        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");
        CompilerHelper.compileJavaAPI(javaApiDir, config);

        List<String> sources = new ArrayList<String>();
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> sources.add(path.toString()));
        }

        List<String> compileArgs = new ArrayList<String>();
        compileArgs.add("-source");
        compileArgs.add(config.targetVersion);
        compileArgs.add("-target");
        compileArgs.add(config.targetVersion);
        if (CompilerHelper.useClasspath(config)) {
            compileArgs.add("-classpath");
            compileArgs.add(javaApiDir.toString());
        } else {
            compileArgs.add("-bootclasspath");
            compileArgs.add(javaApiDir.toString());
            compileArgs.add("-Xlint:-options");
        }
        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.addAll(sources);

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "Compilation failed for javascript target fixture with " + config);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
    }

    static void runJavascriptTranslator(Path classesDir, Path outputDir, String appName) throws Exception {
        Class<?> translatorClass = ByteCodeTranslator.class;
        try {
            java.lang.reflect.Field verboseField = translatorClass.getField("verbose");
            boolean originalVerbose = verboseField.getBoolean(null);
            verboseField.setBoolean(null, false);
            Method main = translatorClass.getMethod("main", String[].class);
            String[] args = new String[]{
                    "javascript",
                    classesDir.toString(),
                    outputDir.toString(),
                    appName,
                    "com.example.javascript",
                    appName,
                    "1.0",
                    "ios",
                    "none"
            };
            try {
                main.invoke(null, (Object) args);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                }
                throw new RuntimeException(cause);
            } finally {
                verboseField.setBoolean(null, originalVerbose);
            }
        } finally {
            Parser.cleanup();
        }
    }

    static String loadFixture(String name) throws Exception {
        InputStream input = JavascriptTargetIntegrationTest.class.getResourceAsStream("/com/codename1/tools/translator/" + name);
        if (input != null) {
            try {
                byte[] buffer = new byte[8192];
                StringBuilder out = new StringBuilder();
                int len;
                while ((len = input.read(buffer)) > -1) {
                    out.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
                }
                return out.toString();
            } finally {
                input.close();
            }
        }

        Path modulePath = Paths.get("src", "test", "resources", "com", "codename1", "tools", "translator", name);
        if (Files.exists(modulePath)) {
            return new String(Files.readAllBytes(modulePath), StandardCharsets.UTF_8);
        }

        Path repoPath = Paths.get("vm", "tests", "src", "test", "resources", "com", "codename1", "tools", "translator", name);
        if (Files.exists(repoPath)) {
            return new String(Files.readAllBytes(repoPath), StandardCharsets.UTF_8);
        }

        throw new IllegalStateException("Missing javascript test fixture " + name);
    }
}
