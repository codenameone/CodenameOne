package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
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

    @Test
    void generatesRunnableHelloWorldUsingCleanTarget() throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("clean-target-sources");
        Path classesDir = Files.createTempDirectory("clean-target-classes");
        Path javaFile = sourceDir.resolve("HelloWorld.java");
        Files.createDirectories(sourceDir.resolve("java/lang"));
        Files.write(javaFile, helloWorldSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Object.java"), javaLangObjectSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/String.java"), javaLangStringSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Class.java"), javaLangClassSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Throwable.java"), javaLangThrowableSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Exception.java"), javaLangExceptionSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/RuntimeException.java"), javaLangRuntimeExceptionSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/NullPointerException.java"), javaLangNullPointerExceptionSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("native_hello.c"), nativeHelloSource().getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK is required to compile test sources");
        int compileResult = compiler.run(
                null,
                null,
                null,
                "-d", classesDir.toString(),
                javaFile.toString(),
                sourceDir.resolve("java/lang/Object.java").toString(),
                sourceDir.resolve("java/lang/String.java").toString(),
                sourceDir.resolve("java/lang/Class.java").toString(),
                sourceDir.resolve("java/lang/Throwable.java").toString(),
                sourceDir.resolve("java/lang/Exception.java").toString(),
                sourceDir.resolve("java/lang/RuntimeException.java").toString(),
                sourceDir.resolve("java/lang/NullPointerException.java").toString()
        );
        assertEquals(0, compileResult, "HelloWorld.java should compile");

        Files.copy(sourceDir.resolve("native_hello.c"), classesDir.resolve("native_hello.c"));

        Path outputDir = Files.createTempDirectory("clean-target-output");
        runTranslator(classesDir, outputDir, "HelloCleanApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        Path srcRoot = distDir.resolve("HelloCleanApp-src");
        patchCn1Globals(srcRoot);
        writeRuntimeStubs(srcRoot);

        replaceLibraryWithExecutableTarget(cmakeLists, srcRoot.getFileName().toString());

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        runCommand(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang",
                "-DCMAKE_OBJC_COMPILER=clang"
        ), distDir);

        runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve("HelloCleanApp");
        String output = runCommand(Arrays.asList(executable.toString()), buildDir);

        assertTrue(output.contains("Hello, Clean Target!"),
                "Compiled program should print hello message, actual output was:\n" + output);
    }

    private void runTranslator(Path classesDir, Path outputDir, String appName) throws Exception {
        Path translatorResources = Paths.get("..", "ByteCodeTranslator", "src").normalize().toAbsolutePath();
        URLClassLoader systemLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        URL[] systemUrls = systemLoader.getURLs();
        URL[] urls = Arrays.copyOf(systemUrls, systemUrls.length + 1);
        urls[systemUrls.length] = translatorResources.toUri().toURL();
        URLClassLoader loader = new URLClassLoader(urls, null);

        ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            assertNotNull(loader.getResource("cn1_globals.h"), "Translator resources should be on the classpath");
            Class<?> translatorClass = Class.forName("com.codename1.tools.translator.ByteCodeTranslator", true, loader);
            assertEquals(loader, translatorClass.getClassLoader());
            Method main = translatorClass.getMethod("main", String[].class);
            String[] args = new String[]{
                    "clean",
                    classesDir.toString(),
                    outputDir.toString(),
                    appName,
                    "com.example.hello",
                    "Hello App",
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
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalLoader);
            try {
                loader.close();
            } catch (IOException ignore) {
            }
        }
    }

    private void replaceLibraryWithExecutableTarget(Path cmakeLists, String sourceDirName) throws IOException {
        String content = new String(Files.readAllBytes(cmakeLists), StandardCharsets.UTF_8);
        String globWithObjc = String.format("file(GLOB TRANSLATOR_SOURCES \"%s/*.c\" \"%s/*.m\")", sourceDirName, sourceDirName);
        String globCOnly = String.format("file(GLOB TRANSLATOR_SOURCES \"%s/*.c\")", sourceDirName);
        content = content.replace(globWithObjc, globCOnly);
        String replacement = content.replace(
                "add_library(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})",
                "add_executable(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})"
        );
        Files.write(cmakeLists, replacement.getBytes(StandardCharsets.UTF_8));
    }

    private String runCommand(List<String> command, Path workingDir) throws Exception {
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

    private void patchCn1Globals(Path srcRoot) throws IOException {
        Path cn1Globals = srcRoot.resolve("cn1_globals.h");
        String content = new String(Files.readAllBytes(cn1Globals), StandardCharsets.UTF_8);
        if (!content.contains("@class NSString;")) {
            content = content.replace("#ifdef __OBJC__\n", "#ifdef __OBJC__\n@class NSString;\n");
            Files.write(cn1Globals, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void writeRuntimeStubs(Path srcRoot) throws IOException {
        Path stubs = srcRoot.resolve("runtime_stubs.c");
        if (Files.exists(stubs)) {
            return;
        }
        String content = "#include \"cn1_globals.h\"\n" +
                "#include <stdlib.h>\n" +
                "#include <string.h>\n" +
                "\n" +
                "static struct ThreadLocalData globalThreadData;\n" +
                "static int runtimeInitialized = 0;\n" +
                "\n" +
                "static void initThreadState() {\n" +
                "    memset(&globalThreadData, 0, sizeof(globalThreadData));\n" +
                "    globalThreadData.threadObjectStack = calloc(64, sizeof(struct elementStruct));\n" +
                "    globalThreadData.pendingHeapAllocations = calloc(64, sizeof(void*));\n" +
                "    globalThreadData.callStackClass = calloc(64, sizeof(int));\n" +
                "    globalThreadData.callStackLine = calloc(64, sizeof(int));\n" +
                "    globalThreadData.callStackMethod = calloc(64, sizeof(int));\n" +
                "}\n" +
                "\n" +
                "struct ThreadLocalData* getThreadLocalData() {\n" +
                "    if (!runtimeInitialized) {\n" +
                "        initThreadState();\n" +
                "        runtimeInitialized = 1;\n" +
                "    }\n" +
                "    return &globalThreadData;\n" +
                "}\n" +
                "\n" +
                "JAVA_OBJECT codenameOneGcMalloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent) {\n" +
                "    JAVA_OBJECT obj = (JAVA_OBJECT)calloc(1, size);\n" +
                "    if (obj != JAVA_NULL) {\n" +
                "        obj->__codenameOneParentClsReference = parent;\n" +
                "    }\n" +
                "    return obj;\n" +
                "}\n" +
                "\n" +
                "void codenameOneGcFree(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {\n" +
                "    free(obj);\n" +
                "}\n" +
                "\n" +
                "void initConstantPool() {}\n" +
                "\n" +
                "void initMethodStack(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, int stackSize, int localsStackSize, int classNameId, int methodNameId) {\n" +
                "    (void)__cn1ThisObject;\n" +
                "    (void)stackSize;\n" +
                "    (void)classNameId;\n" +
                "    (void)methodNameId;\n" +
                "    threadStateData->threadObjectStackOffset += localsStackSize;\n" +
                "}\n" +
                "\n" +
                "void releaseForReturn(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread) {\n" +
                "    threadStateData->threadObjectStackOffset = cn1LocalsBeginInThread;\n" +
                "}\n" +
                "\n" +
                "void releaseForReturnInException(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread, int methodBlockOffset) {\n" +
                "    (void)methodBlockOffset;\n" +
                "    releaseForReturn(threadStateData, cn1LocalsBeginInThread);\n" +
                "}\n" +
                "\n" +
                "void monitorEnter(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { (void)obj; }\n" +
                "\n" +
                "void monitorExit(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { (void)obj; }\n" +
                "\n" +
                "struct clazz class__java_lang_Class = {0};\n" +
                "int currentGcMarkValue = 1;\n";

        Files.write(stubs, content.getBytes(StandardCharsets.UTF_8));
    }

    private String helloWorldSource() {
        return "public class HelloWorld {\n" +
                "    private static native void nativeHello();\n" +
                "    public static void main(String[] args) {\n" +
                "        nativeHello();\n" +
                "    }\n" +
                "}\n";
    }

    private String javaLangObjectSource() {
        return "package java.lang;\n" +
                "public class Object {\n" +
                "}\n";
    }

    private String javaLangStringSource() {
        return "package java.lang;\n" +
                "public class String extends Object {\n" +
                "}\n";
    }

    private String javaLangClassSource() {
        return "package java.lang;\n" +
                "public final class Class extends Object {\n" +
                "}\n";
    }

    private String javaLangThrowableSource() {
        return "package java.lang;\n" +
                "public class Throwable extends Object {\n" +
                "}\n";
    }

    private String javaLangExceptionSource() {
        return "package java.lang;\n" +
                "public class Exception extends Throwable {\n" +
                "}\n";
    }

    private String javaLangRuntimeExceptionSource() {
        return "package java.lang;\n" +
                "public class RuntimeException extends Exception {\n" +
                "}\n";
    }

    private String javaLangNullPointerExceptionSource() {
        return "package java.lang;\n" +
                "public class NullPointerException extends RuntimeException {\n" +
                "}\n";
    }

    private String nativeHelloSource() {
        return "#include \"cn1_globals.h\"\n" +
                "#include <stdio.h>\n" +
                "void HelloWorld_nativeHello__(CODENAME_ONE_THREAD_STATE) {\n" +
                "    printf(\"Hello, Clean Target!\\n\");\n" +
                "}\n";
    }
}
