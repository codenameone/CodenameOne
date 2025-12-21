package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LambdaIntegrationTest {

    private String appSource() {
        return "public class LambdaApp {\n" +
                "    interface MyFunctionalInterface {\n" +
                "        int apply(int x);\n" +
                "    }\n" +
                "    \n" +
                "    public static void main(String[] args) {\n" +
                "        // Lambda\n" +
                "        MyFunctionalInterface adder = (x) -> x + 10;\n" +
                "        int result1 = adder.apply(5); // 15\n" +
                "        \n" +
                "        // Method reference static\n" +
                "        MyFunctionalInterface multiplier = LambdaApp::multiplyByTwo;\n" +
                "        int result2 = multiplier.apply(5); // 10\n" +
                "        \n" +
                "        // Method reference instance\n" +
                "        LambdaApp app = new LambdaApp();\n" +
                "        MyFunctionalInterface subtractor = app::subtractFive;\n" +
                "        int result3 = subtractor.apply(20); // 15\n" +
                "\n" +
                "        // Capturing lambda\n" +
                "        int captured = 100;\n" +
                "        MyFunctionalInterface capturer = (x) -> x + captured;\n" +
                "        int result4 = capturer.apply(5); // 105\n" +
                "        \n" +
                "        report(result1 + result2 + result3 + result4); // 15 + 10 + 15 + 105 = 145\n" +
                "    }\n" +
                "    \n" +
                "    static int multiplyByTwo(int x) {\n" +
                "        return x * 2;\n" +
                "    }\n" +
                "    \n" +
                "    int subtractFive(int x) {\n" +
                "        return x - 5;\n" +
                "    }\n" +
                "\n" +
                "    private static native void report(int value);\n" +
                "}\n";
    }

    private String nativeReportSource() {
        return "#include \"cn1_globals.h\"\n" +
                "#include <stdio.h>\n" +
                "void LambdaApp_report___int(CODENAME_ONE_THREAD_STATE, JAVA_INT value) {\n" +
                "    printf(\"RESULT=%d\\n\", value);\n" +
                "}\n";
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.8"})
    void translatesLambdaBytecodeToLLVMExecutable(String targetVersion) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("lambda-integration-sources");
        Path classesDir = Files.createTempDirectory("lambda-integration-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        Files.write(sourceDir.resolve("LambdaApp.java"), appSource().getBytes(StandardCharsets.UTF_8));

        // Compile JavaAPI for bootclasspath
        compileJavaAPI(javaApiDir);

        Path nativeReport = sourceDir.resolve("native_report.c");
        Files.write(nativeReport, nativeReportSource().getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK is required to compile test sources");

        // Compile App using JavaAPI as bootclasspath
        List<String> compileArgs = new ArrayList<>();
        compileArgs.add("-source");
        compileArgs.add(targetVersion);
        compileArgs.add("-target");
        compileArgs.add(targetVersion);
        compileArgs.add("-bootclasspath");
        compileArgs.add(javaApiDir.toString());
        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(sourceDir.resolve("LambdaApp.java").toString());

        int compileResult = compiler.run(
                null,
                null,
                null,
                compileArgs.toArray(new String[0])
        );
        assertEquals(0, compileResult, "LambdaApp should compile");

        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("lambda-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "LambdaApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        Path srcRoot = distDir.resolve("LambdaApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);
        writeRuntimeStubs(srcRoot);
        writeMissingHeadersAndImpls(srcRoot);

        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, srcRoot.getFileName().toString());

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

        Path executable = buildDir.resolve("LambdaApp");
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains("RESULT=145"), "Compiled program should print the expected result: " + output);
    }

    private void compileJavaAPI(Path outputDir) throws Exception {
        Files.createDirectories(outputDir);
        Path javaApiRoot = Paths.get("..", "JavaAPI", "src").normalize().toAbsolutePath();
        List<String> sources = new ArrayList<>();
        Files.walk(javaApiRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> sources.add(p.toString()));

        // Add stubs for java.lang.invoke
        Path stubsDir = Files.createTempDirectory("java-lang-invoke-stubs");
        sources.addAll(generateJavaLangInvokeStubs(stubsDir));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> args = new ArrayList<>();

        if (!System.getProperty("java.version").startsWith("1.")) {
             args.add("--patch-module");
             args.add("java.base=" + javaApiRoot.toString());
        } else {
            args.add("-source");
            args.add("1.5");
            args.add("-target");
            args.add("1.5");
        }

        args.add("-d");
        args.add(outputDir.toString());
        args.addAll(sources);

        int result = compiler.run(null, null, null, args.toArray(new String[0]));
        assertEquals(0, result, "JavaAPI should compile");
    }

    private List<String> generateJavaLangInvokeStubs(Path stubsDir) throws IOException {
        List<String> stubFiles = new ArrayList<>();
        Path invokePkg = stubsDir.resolve("java/lang/invoke");
        Files.createDirectories(invokePkg);

        // MethodHandle
        Path mh = invokePkg.resolve("MethodHandle.java");
        Files.write(mh, ("package java.lang.invoke;\n" +
                "public abstract class MethodHandle {\n" +
                "    public Object invoke(Object... args) throws Throwable { return null; }\n" +
                "    public Object invokeExact(Object... args) throws Throwable { return null; }\n" +
                "}").getBytes(StandardCharsets.UTF_8));
        stubFiles.add(mh.toString());

        // MethodType
        Path mt = invokePkg.resolve("MethodType.java");
        Files.write(mt, ("package java.lang.invoke;\n" +
                "public class MethodType {\n" +
                "    public static MethodType methodType(Class<?> rtype, Class<?>[] ptypes) { return null; }\n" +
                "}").getBytes(StandardCharsets.UTF_8));
        stubFiles.add(mt.toString());

        // MethodHandles
        Path mhs = invokePkg.resolve("MethodHandles.java");
        Files.write(mhs, ("package java.lang.invoke;\n" +
                "public class MethodHandles {\n" +
                "    public static Lookup lookup() { return null; }\n" +
                "    public static class Lookup {\n" +
                "        public MethodHandle findVirtual(Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException { return null; }\n" +
                "        public MethodHandle findStatic(Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException { return null; }\n" +
                "    }\n" +
                "}").getBytes(StandardCharsets.UTF_8));
        stubFiles.add(mhs.toString());

        // CallSite
        Path cs = invokePkg.resolve("CallSite.java");
        Files.write(cs, ("package java.lang.invoke;\n" +
                "public abstract class CallSite {\n" +
                "    public abstract MethodHandle getTarget();\n" +
                "    public abstract void setTarget(MethodHandle newTarget);\n" +
                "}").getBytes(StandardCharsets.UTF_8));
        stubFiles.add(cs.toString());

        // LambdaMetafactory
        Path lmf = invokePkg.resolve("LambdaMetafactory.java");
        Files.write(lmf, ("package java.lang.invoke;\n" +
                "public class LambdaMetafactory {\n" +
                "    public static CallSite metafactory(MethodHandles.Lookup caller, String invokedName, MethodType invokedType, MethodType samMethodType, MethodHandle implMethod, MethodType instantiatedMethodType) throws LambdaConversionException { return null; }\n" +
                "    public static CallSite altMetafactory(MethodHandles.Lookup caller, String invokedName, MethodType invokedType, Object... args) throws LambdaConversionException { return null; }\n" +
                "}").getBytes(StandardCharsets.UTF_8));
        stubFiles.add(lmf.toString());

        // LambdaConversionException
        Path lce = invokePkg.resolve("LambdaConversionException.java");
        Files.write(lce, ("package java.lang.invoke;\n" +
                "public class LambdaConversionException extends Exception {}\n").getBytes(StandardCharsets.UTF_8));
        stubFiles.add(lce.toString());

        // ConstantCallSite
        Path ccs = invokePkg.resolve("ConstantCallSite.java");
        Files.write(ccs, ("package java.lang.invoke;\n" +
                "public class ConstantCallSite extends CallSite {\n" +
                "    public ConstantCallSite(MethodHandle target) { }\n" +
                "    public final MethodHandle getTarget() { return null; }\n" +
                "    public final void setTarget(MethodHandle ignore) { }\n" +
                "}").getBytes(StandardCharsets.UTF_8));
        stubFiles.add(ccs.toString());

        return stubFiles;
    }

    private void writeMissingHeadersAndImpls(Path srcRoot) throws Exception {
        // java_lang_NullPointerException
        Path npeHeader = srcRoot.resolve("java_lang_NullPointerException.h");
        if (!Files.exists(npeHeader)) {
            String npeContent = "#ifndef __JAVA_LANG_NULLPOINTEREXCEPTION_H__\n" +
                    "#define __JAVA_LANG_NULLPOINTEREXCEPTION_H__\n" +
                    "#include \"cn1_globals.h\"\n" +
                    "JAVA_OBJECT __NEW_INSTANCE_java_lang_NullPointerException(CODENAME_ONE_THREAD_STATE);\n" +
                    "#endif\n";
            Files.write(npeHeader, npeContent.getBytes(StandardCharsets.UTF_8));
        }

        // java_lang_String
        Path stringHeader = srcRoot.resolve("java_lang_String.h");
        if (!Files.exists(stringHeader)) {
            String stringContent = "#ifndef __JAVA_LANG_STRING_H__\n" +
                    "#define __JAVA_LANG_STRING_H__\n" +
                    "#include \"cn1_globals.h\"\n" +
                    "extern struct clazz class__java_lang_String;\n" +
                    "extern struct clazz class_array2__java_lang_String;\n" +
                    "JAVA_OBJECT __NEW_ARRAY_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_INT size);\n" +
                    "#endif\n";
            Files.write(stringHeader, stringContent.getBytes(StandardCharsets.UTF_8));
        }

        // java_lang_Class
        Path classHeader = srcRoot.resolve("java_lang_Class.h");
        if (!Files.exists(classHeader)) {
             String classHeaderContent = "#ifndef __JAVA_LANG_CLASS_H__\n#define __JAVA_LANG_CLASS_H__\n#include \"cn1_globals.h\"\n" +
                     "extern struct clazz class__java_lang_Class;\n" +
                     "#endif\n";
             Files.write(classHeader, classHeaderContent.getBytes(StandardCharsets.UTF_8));
        }

        // java_util_Objects
        Path objectsHeader = srcRoot.resolve("java_util_Objects.h");
        if (!Files.exists(objectsHeader)) {
            String headerContent = "#ifndef __JAVA_UTIL_OBJECTS_H__\n" +
                    "#define __JAVA_UTIL_OBJECTS_H__\n" +
                    "#include \"cn1_globals.h\"\n" +
                    "extern struct clazz class__java_util_Objects;\n" +
                    "JAVA_OBJECT java_util_Objects_requireNonNull___java_lang_Object_R_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);\n" +
                    "#endif\n";
            Files.write(objectsHeader, headerContent.getBytes(StandardCharsets.UTF_8));
        }

        // java_lang_Object
        Path objectHeader = srcRoot.resolve("java_lang_Object.h");
        // Overwrite or create
        String objectContent = "#ifndef __JAVA_LANG_OBJECT_H__\n" +
                "#define __JAVA_LANG_OBJECT_H__\n" +
                "#include \"cn1_globals.h\"\n" +
                "extern struct clazz class__java_lang_Object;\n" +
                "void __FINALIZER_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);\n" +
                "void __GC_MARK_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force);\n" +
                "void java_lang_Object___INIT____(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);\n" +
                "JAVA_OBJECT __NEW_java_lang_Object(CODENAME_ONE_THREAD_STATE);\n" +
                "void __INIT_VTABLE_java_lang_Object(CODENAME_ONE_THREAD_STATE, void** vtable);\n" +
                "JAVA_OBJECT java_lang_Object_getClass___R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);\n" +
                "JAVA_OBJECT virtual_java_lang_Object_getClass___R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);\n" +
                "#endif\n";
        Files.write(objectHeader, objectContent.getBytes(StandardCharsets.UTF_8));


        // Append implementations to runtime_stubs.c or create extra_stubs.c
        Path extraStubs = srcRoot.resolve("extra_stubs.c");
        if (!Files.exists(extraStubs)) {
             String stubs = "#include \"cn1_globals.h\"\n" +
                     "#include \"java_lang_NullPointerException.h\"\n" +
                     "#include \"java_lang_String.h\"\n" +
                     "#include \"java_lang_Class.h\"\n" +
                     "#include \"java_lang_Object.h\"\n" +
                     "#include \"java_util_Objects.h\"\n" +
                     "#include <stdlib.h>\n" +
                     "#include <string.h>\n" +
                     "#include <stdio.h>\n" +
                     "\n" +
                     "// class__java_lang_String defined in runtime_stubs.c\n" +
                     "struct clazz class_array2__java_lang_String = {0};\n" +
                     "// class__java_lang_Class defined in runtime_stubs.c\n" +
                     "struct clazz class__java_lang_Object = {0};\n" +
                     "struct clazz class__java_util_Objects = {0};\n" +
                     "\n" +
                     "JAVA_OBJECT __NEW_INSTANCE_java_lang_NullPointerException(CODENAME_ONE_THREAD_STATE) {\n" +
                     "    fprintf(stderr, \"Allocating NullPointerException\\n\");\n" +
                     "    fflush(stderr);\n" +
                     "    return JAVA_NULL;\n" +
                     "}\n" +
                     "void __FINALIZER_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {}\n" +
                     "void __GC_MARK_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {}\n" +
                     "void java_lang_Object___INIT____(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {}\n" +
                     "JAVA_OBJECT __NEW_java_lang_Object(CODENAME_ONE_THREAD_STATE) {\n" +
                     "    fprintf(stderr, \"__NEW_java_lang_Object called\\n\");\n" +
                     "    fflush(stderr);\n" +
                     "    struct JavaObjectPrototype* ptr = (struct JavaObjectPrototype*)malloc(sizeof(struct JavaObjectPrototype));\n" +
                     "    if (ptr) {\n" +
                     "        memset(ptr, 0, sizeof(struct JavaObjectPrototype));\n" +
                     "        ptr->__codenameOneParentClsReference = &class__java_lang_Object;\n" +
                     "    }\n" +
                     "    return (JAVA_OBJECT)ptr;\n" +
                     "}\n" +
                     "void __INIT_VTABLE_java_lang_Object(CODENAME_ONE_THREAD_STATE, void** vtable) {}\n" +
                     "JAVA_OBJECT __NEW_ARRAY_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {\n" +
                     "    return 0;\n" +
                     "}\n" +
                     "JAVA_OBJECT java_util_Objects_requireNonNull___java_lang_Object_R_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {\n" +
                     "    if (obj == JAVA_NULL) {\n" +
                     "        fprintf(stderr, \"requireNonNull failed\\n\");\n" +
                     "        exit(1);\n" +
                     "    }\n" +
                     "    return obj;\n" +
                     "}\n" +
                     "void gcMarkObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {\n" +
                     "    // Dummy implementation\n" +
                     "}\n" +
                     "JAVA_OBJECT java_lang_Object_getClass___R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {\n" +
                     "    return JAVA_NULL; // Stub\n" +
                     "}\n" +
                     "JAVA_OBJECT virtual_java_lang_Object_getClass___R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {\n" +
                     "    return java_lang_Object_getClass___R_java_lang_Class(threadStateData, obj);\n" +
                     "}\n";
             Files.write(extraStubs, stubs.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void writeRuntimeStubs(Path srcRoot) throws java.io.IOException {
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
                "#include <stdio.h>\n" +
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
                "    fprintf(stderr, \"allocArrayInternal length=%d type=%p\\n\", length, type); fflush(stderr);\n" +
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
                "    fprintf(stderr, \"releaseForReturn locals=%d\\n\", cn1LocalsBeginInThread); fflush(stderr);\n" +
                "    threadStateData->threadObjectStackOffset = cn1LocalsBeginInThread;\n" +
                "}\n" +
                "\n" +
                "void releaseForReturnInException(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread, int methodBlockOffset) {\n" +
                "    (void)methodBlockOffset;\n" +
                "    releaseForReturn(threadStateData, cn1LocalsBeginInThread);\n" +
                "}\n" +
                "\n" +
                "void monitorEnter(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { fprintf(stderr, \"monitorEnter %p\\n\", obj); fflush(stderr); }\n" +
                "\n" +
                "void monitorExit(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { fprintf(stderr, \"monitorExit %p\\n\", obj); fflush(stderr); }\n" +
                "\n" +
                "void monitorEnterBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { fprintf(stderr, \"monitorEnterBlock %p\\n\", obj); fflush(stderr); }\n" +
                "\n" +
                "void monitorExitBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { fprintf(stderr, \"monitorExitBlock %p\\n\", obj); fflush(stderr); }\n" +
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
                "    fprintf(stderr, \"Exception thrown! obj=%p\\n\", obj);\n" +
                "    fflush(stderr);\n" +
                "    exit(1);\n" +
                "}\n" +
                "\n" +
                "struct clazz class__java_lang_Class = {0};\n" +
                "struct clazz class__java_lang_String = {0};\n" +
                "int currentGcMarkValue = 1;\n";

        Files.write(stubs, content.getBytes(StandardCharsets.UTF_8));
    }
}
