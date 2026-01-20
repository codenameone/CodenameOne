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

        double jdkVer = 1.8;
        try { jdkVer = Double.parseDouble(config.jdkVersion); } catch (NumberFormatException ignored) {}
        double targetVer = 1.8;
        try { targetVer = Double.parseDouble(config.targetVersion); } catch (NumberFormatException ignored) {}

        if (jdkVer >= 9 && targetVer < 9) {
            return;
        }

        CompilerHelper.compileJavaAPI(javaApiDir, config);

        if (jdkVer >= 9) {
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

        Files.copy(sourceDir.resolve("native_hello.c"), classesDir.resolve("native_hello.c"));

        Path outputDir = Files.createTempDirectory("clean-target-output");
        runTranslator(classesDir, outputDir, "HelloCleanApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        Path srcRoot = distDir.resolve("HelloCleanApp-src");
        patchCn1Globals(srcRoot);
        writeRuntimeStubs(srcRoot);
        writeMissingHeadersAndImpls(srcRoot);

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

    static void runTranslator(Path classesDir, Path outputDir, String appName) throws Exception {
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

    static void replaceLibraryWithExecutableTarget(Path cmakeLists, String sourceDirName) throws IOException {
        String content = new String(Files.readAllBytes(cmakeLists), StandardCharsets.UTF_8);
        String globWithObjc = String.format("file(GLOB TRANSLATOR_SOURCES \"%s/*.c\" \"%s/*.m\")", sourceDirName, sourceDirName);
        String globCOnly = String.format("file(GLOB TRANSLATOR_SOURCES \"%s/*.c\")", sourceDirName);
        content = content.replace(globWithObjc, globCOnly);
        String replacement = content.replace(
                "add_library(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})",
                "add_executable(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})\ntarget_link_libraries(${PROJECT_NAME} m)"
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

    static void patchCn1Globals(Path srcRoot) throws IOException {
        Path cn1Globals = srcRoot.resolve("cn1_globals.h");
        String content = new String(Files.readAllBytes(cn1Globals), StandardCharsets.UTF_8);
        if (!content.contains("@class NSString;")) {
            content = content.replace("#ifdef __OBJC__\n", "#ifdef __OBJC__\n@class NSString;\n");
            Files.write(cn1Globals, content.getBytes(StandardCharsets.UTF_8));
        }
        if (!content.contains("#include <string.h>")) {
            content = content.replace("#include <stdlib.h>\n", "#include <stdlib.h>\n#include <string.h>\n#include <math.h>\n#include <limits.h>\n");
            Files.write(cn1Globals, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    static void writeRuntimeStubs(Path srcRoot) throws IOException {
        Path objectHeader = srcRoot.resolve("java_lang_Object.h");
        if (!Files.exists(objectHeader)) {
            String headerContent = "#ifndef __JAVA_LANG_OBJECT_H__\n" +
                    "#define __JAVA_LANG_OBJECT_H__\n" +
                    "#include \"cn1_globals.h\"\n" +
                    "#endif\n";
            Files.write(objectHeader, headerContent.getBytes(StandardCharsets.UTF_8));
        }

        Path stubs = srcRoot.resolve("runtime_stubs.c");
        if (Files.exists(stubs)) {
            return;
        }
        String content = "#include \"cn1_globals.h\"\n" +
                "#include <stdlib.h>\n" +
                "#include <string.h>\n" +
                "#include <math.h>\n" +
                "#include <limits.h>\n" +
                "\n" +
                "static struct ThreadLocalData globalThreadData;\n" +
                "static int runtimeInitialized = 0;\n" +
                "\n" +
                "static void initThreadState() {\n" +
                "    memset(&globalThreadData, 0, sizeof(globalThreadData));\n" +
                "    globalThreadData.blocks = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(struct TryBlock));\n" +
                "    globalThreadData.threadObjectStack = calloc(CN1_MAX_OBJECT_STACK_DEPTH, sizeof(struct elementStruct));\n" +
                "    globalThreadData.pendingHeapAllocations = calloc(CN1_MAX_OBJECT_STACK_DEPTH, sizeof(void*));\n" +
                "    globalThreadData.callStackClass = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(int));\n" +
                "    globalThreadData.callStackLine = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(int));\n" +
                "    globalThreadData.callStackMethod = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(int));\n" +
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
                "JAVA_OBJECT* constantPoolObjects = NULL;\n" +
                "\n" +
                "void initConstantPool() {\n" +
                "    if (constantPoolObjects == NULL) {\n" +
                "        constantPoolObjects = calloc(32, sizeof(JAVA_OBJECT));\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "void arrayFinalizerFunction(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array) {\n" +
                "    (void)threadStateData;\n" +
                "    free(array);\n" +
                "}\n" +
                "\n" +
                "void gcMarkArrayObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {\n" +
                "    (void)threadStateData;\n" +
                "    (void)obj;\n" +
                "    (void)force;\n" +
                "}\n" +
                "\n" +
                "void** initVtableForInterface() {\n" +
                "    static void* table[1];\n" +
                "    return (void**)table;\n" +
                "}\n" +
                "\n" +
                "struct clazz class_array1__JAVA_INT = {0};\n" +
                "struct clazz class_array2__JAVA_INT = {0};\n" +
                "struct clazz class_array1__JAVA_BOOLEAN = {0};\n" +
                "struct clazz class_array1__JAVA_CHAR = {0};\n" +
                "struct clazz class_array1__JAVA_FLOAT = {0};\n" +
                "struct clazz class_array1__JAVA_DOUBLE = {0};\n" +
                "struct clazz class_array1__JAVA_BYTE = {0};\n" +
                "struct clazz class_array1__JAVA_SHORT = {0};\n" +
                "struct clazz class_array1__JAVA_LONG = {0};\n" +
                "\n" +
                "static JAVA_OBJECT allocArrayInternal(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim) {\n" +
                "    struct JavaArrayPrototype* arr = (struct JavaArrayPrototype*)calloc(1, sizeof(struct JavaArrayPrototype));\n" +
                "    arr->__codenameOneParentClsReference = type;\n" +
                "    arr->length = length;\n" +
                "    arr->dimensions = dim;\n" +
                "    arr->primitiveSize = primitiveSize;\n" +
                "    if (length > 0) {\n" +
                "        int elementSize = primitiveSize > 0 ? primitiveSize : sizeof(JAVA_OBJECT);\n" +
                "        arr->data = calloc((size_t)length, (size_t)elementSize);\n" +
                "    }\n" +
                "    return (JAVA_OBJECT)arr;\n" +
                "}\n" +
                "\n" +
                "JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim) {\n" +
                "    return allocArrayInternal(threadStateData, length, type, primitiveSize, dim);\n" +
                "}\n" +
                "\n" +
                "JAVA_OBJECT alloc2DArray(CODENAME_ONE_THREAD_STATE, int length1, int length2, struct clazz* parentType, struct clazz* childType, int primitiveSize) {\n" +
                "    struct JavaArrayPrototype* outer = (struct JavaArrayPrototype*)allocArrayInternal(threadStateData, length1, parentType, sizeof(JAVA_OBJECT), 2);\n" +
                "    JAVA_OBJECT* rows = (JAVA_OBJECT*)outer->data;\n" +
                "    for (int i = 0; i < length1; i++) {\n" +
                "        rows[i] = allocArrayInternal(threadStateData, length2, childType, primitiveSize, 1);\n" +
                "    }\n" +
                "    return (JAVA_OBJECT)outer;\n" +
                "}\n" +
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
                "void monitorEnterBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { (void)obj; }\n" +
                "\n" +
                "void monitorExitBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { (void)obj; }\n" +
                "\n" +
                "struct elementStruct* pop(struct elementStruct** sp) {\n" +
                "    (*sp)--;\n" +
                "    return *sp;\n" +
                "}\n" +
                "\n" +
                "void popMany(CODENAME_ONE_THREAD_STATE, int count, struct elementStruct** sp) {\n" +
                "    while (count-- > 0) {\n" +
                "        (*sp)--;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "void throwException(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {\n" +
                "    (void)obj;\n" +
                "    exit(1);\n" +
                "}\n" +
                "\n" +
                "struct clazz class__java_lang_Class = {0};\n" +
                "int currentGcMarkValue = 1;\n";

        Files.write(stubs, content.getBytes(StandardCharsets.UTF_8));
    }

    static void writeMissingHeadersAndImpls(Path srcRoot) throws IOException {
        Path npeHeader = srcRoot.resolve("java_lang_NullPointerException.h");
        if (!Files.exists(npeHeader)) {
            String npeContent = "#ifndef __JAVA_LANG_NULLPOINTEREXCEPTION_H__\n" +
                    "#define __JAVA_LANG_NULLPOINTEREXCEPTION_H__\n" +
                    "#include \"cn1_globals.h\"\n" +
                    "JAVA_OBJECT __NEW_INSTANCE_java_lang_NullPointerException(CODENAME_ONE_THREAD_STATE);\n" +
                    "#endif\n";
            Files.write(npeHeader, npeContent.getBytes(StandardCharsets.UTF_8));
        }
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
