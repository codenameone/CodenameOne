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
        assertTrue(bundleReferencesLiteral(translatedApp, "jvm.setMain(", "JsHello"),
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

        assertTrue(bundleReferencesLiteral(translatedApp, "jvm.setMain(", "JsThreadingApp"),
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

        int start = findFunctionStart(translatedApp, "cn1_JsStraightLine_add_int_int_R_int__impl", "(__cn1Arg1, __cn1Arg2)");
        assertTrue(start >= 0, "Straight-line fixture should emit the add() method");
        int end = translatedApp.indexOf("\n}\n", start);
        assertTrue(end > start, "Straight-line fixture should have a bounded method body");
        String methodBody = translatedApp.substring(start, end);

        // After the post-emit param-into-locals lift (f4381c716) plus the
        // per-method ``lN/sN/pc`` → single-letter rename (aab1aebff), the
        // canonical ``let l0 = __cn1Arg1; let l1 = __cn1Arg2;`` prelude is
        // gone -- the function params ARE the locals, then renamed to
        // single letters. Verify the signature carries the right number
        // of params (one per Java arg, since add() is static) instead.
        String signature = methodBody.substring(0, methodBody.indexOf("{") + 1);
        int paramOpen = signature.indexOf('(');
        int paramClose = signature.lastIndexOf(')');
        String paramList = signature.substring(paramOpen + 1, paramClose).trim();
        int paramCount = paramList.isEmpty() ? 0 : paramList.split(",").length;
        assertEquals(2, paramCount,
                "Straight-line lowering should expose the two Java args directly as function params (signature: " + signature + ")");
        // ``stack`` / ``locals`` were renamed whole-word to ``S`` / ``L`` by
        // shortenStackAndLocals, so the renamed interpreter prelude looks
        // like ``let L = _N(N); let S = []; let <X> = 0;`` followed by a
        // ``for(;;)switch(<X>)`` loop. Check the post-rename signature.
        assertTrue(!methodBody.contains("S[") && !methodBody.contains("stack["),
                "Straight-line lowering should avoid stack-array indexing");
        assertTrue(!methodBody.contains("L = _N(") && !methodBody.contains("S = []")
                        && !methodBody.contains("const locals = new Array")
                        && !methodBody.contains("const stack = []")
                        && !methodBody.contains("for(;;)switch("),
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
        assertTrue(bundleReferencesLiteral(translatedApp, "jvm.setMain(", "JsCapturedRunnable"),
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

        assertTrue(bundleReferencesLiteral(translatedApp, "jvm.setMain(", "JsAuxiliaryMainApp"),
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

        assertTrue(bundleReferencesLiteral(translatedApp, "jvm.setMain(", "JsLambdaCapture"),
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

        // twice() has no virtual/interface dispatch and no intrinsic
        // suspension, so the CHA analysis emits it as a plain
        // ``function`` rather than ``function*``. Accept either form.
        int start = findFunctionStart(translatedApp, "cn1_JsStaticAccess_twice_R_int__impl", "()");
        assertTrue(start >= 0, "Static access fixture should emit the twice() method");
        int end = translatedApp.indexOf("\n}\n", start);
        assertTrue(end > start, "Static access fixture should have a bounded method body");
        String methodBody = translatedApp.substring(start, end);

        // ``_I(<cls>)`` is the short alias for ``jvm.ensureClassInitialized``
        // and the class-name argument may have been hoisted to a ``_qN``
        // alias by the string-hoist pass. The alias dictionary lives at
        // the top of ``translatedApp``, outside ``methodBody``, so pass
        // both: scan the method body for call sites, resolve the alias
        // in the full bundle. The assertion is AT MOST ONE -- with the
        // per-method __cn1Init cache gone (init now lives in the public
        // wrapper around __impl), zero is also valid when the analyser
        // proves the class is already initialised by the time the
        // method runs (the wrapper handled it). The bug this test
        // guards against is the OLD interpreter loop's tendency to emit
        // a fresh init guard for each static-field access in the body.
        int initChecks = countLiteralReferences(methodBody, translatedApp, "_I(", "JsStaticAccess")
                + countLiteralReferences(methodBody, translatedApp, "jvm.ensureClassInitialized(", "JsStaticAccess");
        assertTrue(initChecks <= 1,
                "Repeated static field access should emit at most one class-init check in straight-line mode "
                        + "(got " + initChecks + ")");
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

        int start = findFunctionStart(translatedApp, "cn1_JsStaticAccessFlow_pick_int_R_int__impl", "(__cn1Arg1)");
        assertTrue(start >= 0, "Interpreter static access fixture should emit the pick() method");
        int end = translatedApp.indexOf("\n}\n", start);
        assertTrue(end > start, "Interpreter static access fixture should have a bounded method body");
        String methodBody = translatedApp.substring(start, end);

        // The per-method ``__cn1Init`` cache was dropped in favour of a
        // single ``_I(cls)`` call in the public wrapper: the ``__impl``
        // body runs only through that wrapper (or through another method
        // on the same class / ancestor, which the JVM spec guarantees is
        // already initialised). Verify the wrapper still guards entry.
        int wrapperStart = findFunctionStart(translatedApp, "cn1_JsStaticAccessFlow_pick_int_R_int", "(__cn1Arg1)");
        assertTrue(wrapperStart >= 0,
                "Interpreter static access fixture should emit a public pick() wrapper around pick()__impl");
        int wrapperEnd = translatedApp.indexOf("\n}\n", wrapperStart);
        assertTrue(wrapperEnd > wrapperStart,
                "Interpreter static access fixture wrapper should have a bounded body");
        String wrapperBody = translatedApp.substring(wrapperStart, wrapperEnd);
        // Aliases for the class name live at the top of translatedApp,
        // outside wrapperBody, so pass both: scan wrapperBody for call
        // sites and resolve any ``_qN`` alias against the full bundle.
        assertTrue(bundleReferencesLiteral(wrapperBody, translatedApp, "_I(", "JsStaticAccessFlow")
                        || bundleReferencesLiteral(wrapperBody, translatedApp, "jvm.ensureClassInitialized(", "JsStaticAccessFlow"),
                "Interpreter static access wrapper should guard class init before delegating to __impl");
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

        int callerStart = findFunctionStart(translatedApp, "cn1_JsStaticInvokeFlow_pick_int_R_int__impl", "(__cn1Arg1)");
        assertTrue(callerStart >= 0, "Static invoke fixture should emit an internal implementation for pick()");
        int callerEnd = translatedApp.indexOf("\n}\n", callerStart);
        assertTrue(callerEnd > callerStart, "Static invoke fixture should have a bounded pick() body");
        String callerBody = translatedApp.substring(callerStart, callerEnd);

        // Method-level ``__cn1Init`` cache removed; class-init for the
        // containing class is elided entirely inside ``pick()`` because
        // the JVM spec guarantees JsStaticAccessFlow's clinit has run by
        // the time any of its methods execute. What matters is that the
        // caller reaches the internal ``__impl`` body directly (no
        // redundant wrapper that would re-run class init).
        assertTrue(callerBody.contains("cn1_JsStaticInvokeFlow_helper_R_int__impl"),
                "Static invoke caller should target the internal __impl implementation");

        int calleeStart = findFunctionStart(translatedApp, "cn1_JsStaticInvokeFlow_helper_R_int__impl", "()");
        assertTrue(calleeStart >= 0, "Static invoke fixture should emit an internal implementation for helper()");
        int calleeEnd = translatedApp.indexOf("\n}\n", calleeStart);
        assertTrue(calleeEnd > calleeStart, "Static invoke fixture should have a bounded helper() body");
        String calleeBody = translatedApp.substring(calleeStart, calleeEnd);

        assertTrue(!bundleReferencesLiteral(calleeBody, translatedApp, "jvm.ensureClassInitialized(", "JsStaticInvokeFlow")
                        && !bundleReferencesLiteral(calleeBody, translatedApp, "_I(", "JsStaticInvokeFlow"),
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

        int start = findFunctionStart(translatedApp, "cn1_JsVirtualInvokeFlow_repeat_JsVirtualInvokeBase_int_R_int__impl", "(__cn1Arg1, __cn1Arg2)");
        assertTrue(start >= 0, "Virtual invoke fixture should emit the repeat() method");
        int end = translatedApp.indexOf("\n}\n", start);
        assertTrue(end > start, "Virtual invoke fixture should have a bounded repeat() body");
        String methodBody = translatedApp.substring(start, end);

        // The per-method ``__cn1Virtual`` cache and its className|methodId
        // cache-key pattern moved to a global ``resolvedVirtualCache`` on
        // the runtime (see jvm.resolveVirtual in parparvm_runtime.js) —
        // one cache per running app rather than one per emitted method.
        // The bytecode-level INVOKEVIRTUAL emission simply calls the
        // ``cn1_iv*`` helper, which consults the runtime cache. Assert
        // that virtual dispatch still runs through that helper family.
        // The structured emitter spells the helpers through their short
        // runtime aliases (``_v*`` generator / ``_w*`` sync); the
        // interpreter path keeps the long ``cn1_iv*`` names. Which one a
        // fixture method gets depends on the bytecode shape the compiling
        // JDK produced, so accept the whole family.
        assertTrue(methodBody.contains("cn1_iv0(") || methodBody.contains("cn1_iv1(")
                        || methodBody.contains("cn1_iv2(") || methodBody.contains("cn1_iv3(")
                        || methodBody.contains("cn1_iv4(") || methodBody.contains("cn1_ivN(")
                        || methodBody.contains("_v0(") || methodBody.contains("_v1(")
                        || methodBody.contains("_v2(") || methodBody.contains("_v3(")
                        || methodBody.contains("_v4(") || methodBody.contains("_vN(")
                        || methodBody.contains("_w0(") || methodBody.contains("_w1(")
                        || methodBody.contains("_w2(") || methodBody.contains("_w3(")
                        || methodBody.contains("_w4(") || methodBody.contains("_wN("),
                "Virtual dispatch should route through the cn1_iv*/_v*/_w* helper family");
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
        // The fixtures assert on canonical ``cn1_*`` / Java method names in
        // the emitted bundle; the whole-bundle identifier renamer and the
        // call-site alias pass legitimately erase those names in production
        // output. Translate the fixtures with both passes off so the
        // name-based assertions keep testing what they were written for
        // (that each construct TRANSLATES) rather than the minifier.
        String prevMinify = System.getProperty("parparvm.js.minify.idents.off");
        String prevAlias = System.getProperty("parparvm.js.alias.off");
        System.setProperty("parparvm.js.minify.idents.off", "1");
        System.setProperty("parparvm.js.alias.off", "1");
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
            if (prevMinify == null) {
                System.clearProperty("parparvm.js.minify.idents.off");
            } else {
                System.setProperty("parparvm.js.minify.idents.off", prevMinify);
            }
            if (prevAlias == null) {
                System.clearProperty("parparvm.js.alias.off");
            } else {
                System.setProperty("parparvm.js.alias.off", prevAlias);
            }
            Parser.cleanup();
        }
    }

    /**
     * Find the alias used by the post-emit string-hoist pass for the
     * given literal string. The hoist rewrites repeated identifier
     * literals to ``const _qN="..."`` declarations and replaces every
     * subsequent use with the alias. Returns the alias name (e.g.
     * ``_q0M0``) or ``null`` if the literal is used only once / not
     * hoisted.
     */
    static String findStringAlias(String translatedApp, String literal) {
        String needle = "=\"" + literal + "\"";
        int idx = translatedApp.indexOf(needle);
        if (idx <= 0) {
            return null;
        }
        int aliasEnd = idx;
        int aliasStart = aliasEnd;
        while (aliasStart > 0) {
            char ch = translatedApp.charAt(aliasStart - 1);
            if (ch == '_' || ch == '$' || Character.isLetterOrDigit(ch)) {
                aliasStart--;
            } else {
                break;
            }
        }
        if (aliasStart >= aliasEnd) {
            return null;
        }
        String candidate = translatedApp.substring(aliasStart, aliasEnd);
        // Hoisted alias names always start with ``_q``. Avoid matching
        // a stray identifier (e.g. a method-suffix like ``...impl="X"``
        // emitted as a property).
        if (!candidate.startsWith("_q")) {
            return null;
        }
        return candidate;
    }

    /**
     * True if ``searchRegion`` contains a call to
     * ``invocationPrefix("literal")`` after the post-emit string-hoist
     * pass — either the direct literal form ``foo("X"`` or the aliased
     * form ``foo(_qN`` where ``_qN`` resolves to ``"X"`` in the
     * top-of-bundle alias dictionary. ``aliasSource`` is the full
     * translated_app.js (or any region that contains the
     * ``const _qN="..."`` declarations); ``searchRegion`` is the
     * substring to scan (e.g. a method body extracted via substring).
     * Pass the same string for both when scanning the whole bundle.
     */
    static boolean bundleReferencesLiteral(String searchRegion, String aliasSource, String invocationPrefix, String literal) {
        if (searchRegion.contains(invocationPrefix + "\"" + literal + "\"")) {
            return true;
        }
        String alias = findStringAlias(aliasSource, literal);
        return alias != null && searchRegion.contains(invocationPrefix + alias);
    }

    /**
     * Whole-bundle convenience: passes the bundle as both
     * ``searchRegion`` and ``aliasSource``.
     */
    static boolean bundleReferencesLiteral(String bundle, String invocationPrefix, String literal) {
        return bundleReferencesLiteral(bundle, bundle, invocationPrefix, literal);
    }

    /**
     * Count occurrences of ``invocationPrefix("literal"`` in
     * ``searchRegion``, accounting for the post-emit string hoist
     * (literals replaced by ``_qN`` aliases). The alias is looked up
     * in ``aliasSource`` — pass the whole bundle when ``searchRegion``
     * is a substring that doesn't contain the alias-definition table
     * (e.g. an extracted method body).
     */
    static int countLiteralReferences(String searchRegion, String aliasSource, String invocationPrefix, String literal) {
        String directNeedle = invocationPrefix + "\"" + literal + "\"";
        int direct = 0;
        int from = 0;
        while ((from = searchRegion.indexOf(directNeedle, from)) >= 0) {
            direct++;
            from += directNeedle.length();
        }
        String alias = findStringAlias(aliasSource, literal);
        if (alias == null) {
            return direct;
        }
        String aliasNeedle = invocationPrefix + alias;
        int aliased = 0;
        from = 0;
        while ((from = searchRegion.indexOf(aliasNeedle, from)) >= 0) {
            aliased++;
            from += aliasNeedle.length();
        }
        return direct + aliased;
    }

    /**
     * Locate a translated method's body entry given its identifier and
     * parameter list. Accepts either the ``function* name(args){`` or
     * ``function name(args){`` shape — the JS suspension analysis may
     * classify a method as synchronous and emit the non-generator form.
     * Returns the index of the first character (``f`` of ``function``)
     * or ``-1`` if neither form is found.
     */
    static int findFunctionStart(String translatedApp, String identifier, String parameterList) {
        // First try the exact canonical signature (covers the few methods
        // that bail out of the post-emit local/param rename passes:
        // synchronized methods, methods whose emit shape the rename
        // scanner doesn't recognise, etc).
        String generator = "function* " + identifier + parameterList + "{";
        int idx = translatedApp.indexOf(generator);
        if (idx >= 0) {
            return idx;
        }
        String plain = "function " + identifier + parameterList + "{";
        idx = translatedApp.indexOf(plain);
        if (idx >= 0) {
            return idx;
        }
        // Post-rename emission: the ``__cn1Arg<N>`` / ``__cn1ThisObject``
        // params are renamed to ``l<N>`` (commit f4381c716) and then
        // ``l<N>`` is further compressed to single-letter aliases
        // (commit aab1aebff). The function name is global and stays
        // stable; the parameter LIST is what changes. Match the
        // signature with any parameter list shape ``(...){``.
        for (String prefix : new String[]{"function* ", "function "}) {
            String token = prefix + identifier + "(";
            int searchFrom = 0;
            while (true) {
                int hit = translatedApp.indexOf(token, searchFrom);
                if (hit < 0) {
                    break;
                }
                int close = translatedApp.indexOf(')', hit + token.length());
                if (close > hit
                        && close + 1 < translatedApp.length()
                        && translatedApp.charAt(close + 1) == '{') {
                    return hit;
                }
                searchFrom = hit + token.length();
            }
        }
        return -1;
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
