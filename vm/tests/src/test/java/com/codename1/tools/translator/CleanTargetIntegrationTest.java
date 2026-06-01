package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CleanTargetIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void generatesRunnableHelloWorldUsingCleanTarget(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("clean-target-sources");
        Path classesDir = Files.createTempDirectory("clean-target-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");
        Path javaFile = sourceDir.resolve("HelloWorld.java");
        Files.write(javaFile, helloWorldSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("native_hello.c"), nativeHelloSource().getBytes(StandardCharsets.UTF_8));

        List<String> compileArgs = new java.util.ArrayList<>();

        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        CompilerHelper.compileJavaAPI(javaApiDir, config);

        if (CompilerHelper.useClasspath(config)) {
             // For CleanTarget, we are compiling java.lang classes.
             // On JDK 9+, rely on the JDK's bootstrap classes but include JavaAPI in classpath
             // so non-replaced classes are found.
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
        compileArgs.add(javaFile.toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "HelloWorld.java should compile with " + config);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Files.copy(sourceDir.resolve("native_hello.c"), classesDir.resolve("native_hello.c"));

        Path outputDir = Files.createTempDirectory("clean-target-output");
        runTranslator(classesDir, outputDir, "HelloCleanApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        Path srcRoot = distDir.resolve("HelloCleanApp-src");

        replaceLibraryWithExecutableTarget(cmakeLists, srcRoot.getFileName().toString());

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        List<String> configure = new java.util.ArrayList<>(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        runCommand(configure, distDir);

        runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve(CompilerHelper.executableName("HelloCleanApp"));
        String output = runCommand(Arrays.asList(executable.toString()), buildDir);

        assertTrue(output.contains("Hello, Clean Target!"),
                "Compiled program should print hello message, actual output was:\n" + output);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void generatesRunnableExecutableForWindowsAppType(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("windows-target-sources");
        Path classesDir = Files.createTempDirectory("windows-target-classes");
        Path javaApiDir = Files.createTempDirectory("windows-java-api-classes");
        Path javaFile = sourceDir.resolve("HelloWorld.java");
        Files.write(javaFile, helloWorldSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("native_hello.c"), nativeHelloSource().getBytes(StandardCharsets.UTF_8));

        List<String> compileArgs = new java.util.ArrayList<>();

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
        compileArgs.add(javaFile.toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "HelloWorld.java should compile with " + config);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Files.copy(sourceDir.resolve("native_hello.c"), classesDir.resolve("native_hello.c"));

        Path outputDir = Files.createTempDirectory("windows-target-output");
        // The "windows" app type makes the translator emit add_executable() with the
        // platform link libraries directly, so unlike the other clean-target tests we
        // do NOT post-process the CMakeLists with replaceLibraryWithExecutableTarget.
        runTranslator(classesDir, outputDir, "WinCleanApp", "windows");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        String cmake = new String(Files.readAllBytes(cmakeLists), StandardCharsets.UTF_8);
        assertTrue(cmake.contains("add_executable(${PROJECT_NAME}"),
                "windows app type should emit an executable target, was:\n" + cmake);
        assertFalse(cmake.contains("add_library(${PROJECT_NAME}"),
                "windows app type should not emit a library target, was:\n" + cmake);
        assertTrue(cmake.contains("d2d1") && cmake.contains("dwrite"),
                "windows app type should link the Direct2D/DirectWrite stack, was:\n" + cmake);

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        List<String> configure = new java.util.ArrayList<>(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        runCommand(configure, distDir);

        runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve(CompilerHelper.executableName("WinCleanApp"));
        String output = runCommand(Arrays.asList(executable.toString()), buildDir);

        assertTrue(output.contains("Hello, Clean Target!"),
                "Compiled program should print hello message, actual output was:\n" + output);
    }

    static void runTranslator(Path classesDir, Path outputDir, String appName) throws Exception {
        runTranslator(classesDir, outputDir, appName, "ios");
    }

    static void runTranslator(Path classesDir, Path outputDir, String appName, String appType) throws Exception {
        Path translatorResources = Paths.get("..", "ByteCodeTranslator", "src").normalize().toAbsolutePath();
        ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
        URL[] systemUrls;
        if (systemLoader instanceof URLClassLoader) {
            systemUrls = ((URLClassLoader) systemLoader).getURLs();
        } else {
             // For Java 9+, we need to get the classpath from the system property
             String[] paths = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
             systemUrls = new URL[paths.length];
             for (int i=0; i<paths.length; i++) {
                 systemUrls[i] = Paths.get(paths[i]).toUri().toURL();
             }
        }

        URL[] urls = Arrays.copyOf(systemUrls, systemUrls.length + 1);
        urls[systemUrls.length] = translatorResources.toUri().toURL();
        URLClassLoader loader = new URLClassLoader(urls, null);

        ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            assertNotNull(loader.getResource("cn1_globals.h"), "Translator resources should be on the classpath");
            Class<?> translatorClass = Class.forName("com.codename1.tools.translator.ByteCodeTranslator", true, loader);
            assertEquals(loader, translatorClass.getClassLoader());
            java.lang.reflect.Field verboseField = translatorClass.getField("verbose");
            boolean originalVerbose = verboseField.getBoolean(null);
            verboseField.setBoolean(null, false);
            Method main = translatorClass.getMethod("main", String[].class);
            String[] args = new String[]{
                    "clean",
                    classesDir.toString(),
                    outputDir.toString(),
                    appName,
                    "com.example.hello",
                    "Hello App",
                    "1.0",
                    appType,
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
            Thread.currentThread().setContextClassLoader(originalLoader);
            try {
                loader.close();
            } catch (IOException ignore) {
            }
        }
    }

    static void replaceLibraryWithExecutableTarget(Path cmakeLists, String sourceDirName) throws IOException {
        String content = new String(Files.readAllBytes(cmakeLists), StandardCharsets.UTF_8);
        // The math functions live in libc/libm on POSIX, but are part of the CRT
        // under MSVC, where there is no separate libm to link against.
        String linkLine = CompilerHelper.isWindows()
                ? ""
                : "\ntarget_link_libraries(${PROJECT_NAME} m)";
        String replacement = content.replace(
                "add_library(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})",
                "add_executable(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})" + linkLine
        );
        Files.write(cmakeLists, replacement.getBytes(StandardCharsets.UTF_8));
    }

    static String runCommand(List<String> command, Path workingDir) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDir.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        int exit = process.waitFor();
        assertEquals(0, exit, "Command failed: " + String.join(" ", command) + "\nOutput:\n" + output);
        return output;
    }

    static String helloWorldSource() {
        return "public class HelloWorld {\n" +
                "    private static native void nativeHello();\n" +
                "    public static void main(String[] args) {\n" +
                "        nativeHello();\n" +
                "    }\n" +
                "}\n";
    }

    static String nativeHelloSource() {
        return "#include \"cn1_globals.h\"\n" +
                "#include <stdio.h>\n" +
                "void HelloWorld_nativeHello__(CODENAME_ONE_THREAD_STATE) {\n" +
                "    printf(\"Hello, Clean Target!\\n\");\n" +
                "}\n";
    }

}
