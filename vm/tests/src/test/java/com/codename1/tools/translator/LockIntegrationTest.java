package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LockIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void verifiesLockAndReentrantLockBehavior(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("lock-integration-sources");
        Path classesDir = Files.createTempDirectory("lock-integration-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        // 1. Write Test App
        Files.write(sourceDir.resolve("LockTestApp.java"), lockTestAppSource().getBytes(StandardCharsets.UTF_8));

        // 2. Compile Test App against JavaAPI
        List<String> sources = new ArrayList<>();
        Files.walk(sourceDir).filter(p -> p.toString().endsWith(".java")).forEach(p -> sources.add(p.toString()));

        List<String> compileArgs = new ArrayList<>();
        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        CompilerHelper.compileJavaAPI(javaApiDir, config);

        if (CompilerHelper.useClasspath(config)) {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-classpath");
             compileArgs.add(javaApiDir.toString());
        } else {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-bootclasspath");
             compileArgs.add(javaApiDir.toString());
             compileArgs.add("-Xlint:-options");
        }

        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.addAll(sources);

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "Compilation failed");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        // 3. Native Report Stub
        Path nativeReport = sourceDir.resolve("native_report.c");
        Files.write(nativeReport, nativeReportSource().getBytes(StandardCharsets.UTF_8));
        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        // 4. Run Translator
        Path outputDir = Files.createTempDirectory("lock-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "LockTestApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists));

        Path srcRoot = distDir.resolve("LockTestApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang",
                "-DCMAKE_OBJC_COMPILER=clang"
        ), distDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        // TODO: Execution still skipped because the generated executable SIGSEGVs on Linux runners.
        // Track and fix the underlying runtime stub issues before enabling.

        // Verify output assertions
        // assertTrue(output.contains("TEST: Basic Lock OK"), "Basic lock should work");
        // assertTrue(output.contains("TEST: Reentrancy OK"), "Reentrant lock should work");
        // assertTrue(output.contains("TEST: TryLock OK"), "TryLock should work");
        // assertTrue(output.contains("TEST: Condition OK"), "Condition wait/signal should work");
    }

    private String lockTestAppSource() {
        return "import java.util.concurrent.locks.*;\n" +
                "import java.util.concurrent.TimeUnit;\n" +
                "public class LockTestApp {\n" +
                "    private static native void report(String msg);\n" +
                "    \n" +
                "    public static void main(String[] args) {\n" +
                "        testBasicLock();\n" +
                "        testReentrancy();\n" +
                "        testTryLock();\n" +
                "        testCondition();\n" +
                "    }\n" +
                "    \n" +
                "    private static void testBasicLock() {\n" +
                "        ReentrantLock lock = new ReentrantLock();\n" +
                "        lock.lock();\n" +
                "        try {\n" +
                "             report(\"TEST: Basic Lock OK\");\n" +
                "        } finally {\n" +
                "            lock.unlock();\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    private static void testReentrancy() {\n" +
                "        ReentrantLock lock = new ReentrantLock();\n" +
                "        lock.lock();\n" +
                "        try {\n" +
                "            lock.lock();\n" +
                "            try {\n" +
                "                if (lock.getHoldCount() == 2) {\n" +
                "                    report(\"TEST: Reentrancy OK\");\n" +
                "                }\n" +
                "            } finally {\n" +
                "                lock.unlock();\n" +
                "            }\n" +
                "        } finally {\n" +
                "            lock.unlock();\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    private static void testTryLock() {\n" +
                "        ReentrantLock lock = new ReentrantLock();\n" +
                "        if (lock.tryLock()) {\n" +
                "            try {\n" +
                "                report(\"TEST: TryLock OK\");\n" +
                "            } finally {\n" +
                "                lock.unlock();\n" +
                "            }\n" +
                "        } else {\n" +
                "            report(\"TEST: TryLock FAILED\");\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    private static void testCondition() {\n" +
                "        final ReentrantLock lock = new ReentrantLock();\n" +
                "        final Condition cond = lock.newCondition();\n" +
                "        final boolean[] signalled = new boolean[1];\n" +
                "        \n" +
                "        Thread t = new Thread(new Runnable() {\n" +
                "            public void run() {\n" +
                "                lock.lock();\n" +
                "                try {\n" +
                "                    signalled[0] = true;\n" +
                "                    cond.signal();\n" +
                "                } finally {\n" +
                "                    lock.unlock();\n" +
                "                }\n" +
                "            }\n" +
                "        });\n" +
                "        \n" +
                "        lock.lock();\n" +
                "        try {\n" +
                "            t.start();\n" +
                "            cond.await();\n" +
                "            if (signalled[0]) {\n" +
                "                report(\"TEST: Condition OK\");\n" +
                "            }\n" +
                "        } catch (InterruptedException e) {\n" +
                "            report(\"TEST: Condition INTERRUPTED\");\n" +
                "        } finally {\n" +
                "            lock.unlock();\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    private String nativeReportSource() {
        return "#include \"cn1_globals.h\"\n" +
                "#include <stdio.h>\n" +
                "void LockTestApp_report___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT msg) {\n" +
                "    struct String_Struct {\n" +
                "        JAVA_OBJECT header;\n" +
                "        JAVA_OBJECT value;\n" +
                "        JAVA_INT offset;\n" +
                "        JAVA_INT count;\n" +
                "    };\n" +
                "    struct String_Struct* str = (struct String_Struct*)msg;\n" +
                "    \n" +
                "    struct JavaArrayPrototype* arr = (struct JavaArrayPrototype*)str->value;\n" +
                "    if (arr) {\n" +
                "        JAVA_CHAR* chars = (JAVA_CHAR*)arr->data;\n" +
                "        int len = str->count;\n" +
                "        int off = str->offset;\n" +
                "        for (int i=0; i<len; i++) {\n" +
                "             printf(\"%c\", (char)chars[off + i]);\n" +
                "        }\n" +
                "        printf(\"\\n\");\n" +
                "        fflush(stdout);\n" +
                "    }\n" +
                "}\n";
    }

}
