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

class ReadWriteLockIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void verifiesReadWriteLockBehavior(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("rwlock-integration-sources");
        Path classesDir = Files.createTempDirectory("rwlock-integration-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        // 1. Write Test App
        Files.write(sourceDir.resolve("ReadWriteLockTestApp.java"), lockTestAppSource().getBytes(StandardCharsets.UTF_8));

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
        Path outputDir = Files.createTempDirectory("rwlock-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "ReadWriteLockTestApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists));

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
    }

    private String lockTestAppSource() {
        return "import java.util.concurrent.locks.*;\n" +
                "import java.util.concurrent.TimeUnit;\n" +
                "public class ReadWriteLockTestApp {\n" +
                "    private static native void report(String msg);\n" +
                "    \n" +
                "    public static void main(String[] args) {\n" +
                "        testBasicReadLock();\n" +
                "        testWriteLockExclusion();\n" +
                "        testReentrancy();\n" +
                "    }\n" +
                "    \n" +
                "    private static void testBasicReadLock() {\n" +
                "        ReentrantReadWriteLock rw = new ReentrantReadWriteLock();\n" +
                "        rw.readLock().lock();\n" +
                "        try {\n" +
                "             report(\"TEST: Basic Read Lock OK\");\n" +
                "        } finally {\n" +
                "            rw.readLock().unlock();\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    private static void testWriteLockExclusion() {\n" +
                "        final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();\n" +
                "        rw.writeLock().lock();\n" +
                "        \n" +
                "        final boolean[] success = new boolean[1];\n" +
                "        Thread t = new Thread(new Runnable() {\n" +
                "            public void run() {\n" +
                "                // This should block until writer unlocks\n" +
                "                rw.readLock().lock();\n" +
                "                rw.readLock().unlock();\n" +
                "                success[0] = true;\n" +
                "            }\n" +
                "        });\n" +
                "        t.start();\n" +
                "        \n" +
                "        try { Thread.sleep(200); } catch(Exception e) {}\n" +
                "        if (success[0]) {\n" +
                "             report(\"TEST: Write Lock Exclusion FAILED (Reader acquired lock while writer held it)\");\n" +
                "             return;\n" +
                "        }\n" +
                "        \n" +
                "        rw.writeLock().unlock();\n" +
                "        try { Thread.sleep(200); } catch(Exception e) {}\n" +
                "        if (success[0]) {\n" +
                "             report(\"TEST: Write Lock Exclusion OK\");\n" +
                "        } else {\n" +
                "             report(\"TEST: Write Lock Exclusion FAILED (Reader did not acquire lock)\");\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    private static void testReentrancy() {\n" +
                "        ReentrantReadWriteLock rw = new ReentrantReadWriteLock();\n" +
                "        rw.writeLock().lock();\n" +
                "        try {\n" +
                "            rw.readLock().lock();\n" +
                "            try {\n" +
                "                if (rw.getWriteHoldCount() == 1 && rw.getReadHoldCount() == 1) {\n" +
                "                    report(\"TEST: Reentrancy (Downgrade) OK\");\n" +
                "                }\n" +
                "            } finally {\n" +
                "                rw.readLock().unlock();\n" +
                "            }\n" +
                "        } finally {\n" +
                "            rw.writeLock().unlock();\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    private String nativeReportSource() {
        return "#include \"cn1_globals.h\"\n" +
                "#include <stdio.h>\n" +
                "void ReadWriteLockTestApp_report___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT msg) {\n" +
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
