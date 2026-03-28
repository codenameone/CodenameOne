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

        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);
        String runtime = new String(Files.readAllBytes(distDir.resolve("parparvm_runtime.js")), StandardCharsets.UTF_8);
        String worker = new String(Files.readAllBytes(distDir.resolve("worker.js")), StandardCharsets.UTF_8);

        assertTrue(translatedApp.contains("function*") && translatedApp.contains("JsHello"),
                "Main class should contribute translated JS generator functions");
        assertTrue(translatedApp.contains("jvm.setMain(\"JsHello\""),
                "Bundle should register the translated main entrypoint");
        assertTrue(runtime.contains("cn1_java_lang_Thread_start") || runtime.contains("cn1_java_lang_Thread_start__"),
                "Runtime should provide JS native implementations for thread start");
        assertTrue(runtime.contains("cn1_java_lang_Object_wait_long_int") || runtime.contains("cn1_java_lang_Object_wait___long_int"),
                "Runtime should provide JS native implementations for wait()");
        assertTrue(!translatedApp.contains("Missing javascript native method cn1_java_lang_Object_wait_long_int"),
                "Translated bundle should not emit generic fallback stubs for runtime-implemented natives");
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

        String marker = "function* cn1_JsStraightLine_add_int_int_R_int(__cn1Arg1, __cn1Arg2){";
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
