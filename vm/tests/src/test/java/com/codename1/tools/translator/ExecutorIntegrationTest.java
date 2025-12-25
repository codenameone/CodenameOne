package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExecutorIntegrationTest {

    @Test
    public void testExecutorService() throws Exception {
        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);

        Path sourceDir = Files.createTempDirectory("executor-test-src");
        Path classesDir = Files.createTempDirectory("executor-test-classes");
        Path outputDir = Files.createTempDirectory("executor-test-output");

        String javaCode = "package test;\n" +
            "import java.util.concurrent.*;\n" +
            "import java.util.*;\n" +
            "public class Main {\n" +
            "    private static native void report(int val);\n" +
            "    static class DirectExecutorService extends AbstractExecutorService {\n" +
            "        private boolean shutdown;\n" +
            "        public void execute(Runnable command) {\n" +
            "            if (shutdown) throw new RejectedExecutionException();\n" +
            "            command.run();\n" +
            "        }\n" +
            "        public void shutdown() { shutdown = true; }\n" +
            "        public List<Runnable> shutdownNow() { shutdown = true; return new ArrayList<Runnable>(); }\n" +
            "        public boolean isShutdown() { return shutdown; }\n" +
            "        public boolean isTerminated() { return shutdown; }\n" +
            "        public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }\n" +
            "    }\n" +
            "    public static void main(String[] args) throws Exception {\n" +
            "        ExecutorService executor = new DirectExecutorService();\n" +
            "        Future<String> f1 = executor.submit(new Callable<String>() {\n" +
            "            public String call() throws Exception {\n" +
            "                return \"Hello\";\n" +
            "            }\n" +
            "        });\n" +
            "        if (f1.get().equals(\"Hello\")) report(1);\n" +
            "        executor.shutdown();\n" +
            "    }\n" +
            "}";

        Files.write(sourceDir.resolve("Main.java"), javaCode.getBytes(StandardCharsets.UTF_8));

        // Native report implementation
        String nativeCode = "#include \"cn1_globals.h\"\n" +
                            "#include <stdio.h>\n" +
                            "void test_Main_report___int(CODENAME_ONE_THREAD_STATE, JAVA_INT value) {\n" +
                            "    printf(\"RESULT=%d\\n\", value);\n" +
                            "}\n";
        Files.write(sourceDir.resolve("native_main.c"), nativeCode.getBytes(StandardCharsets.UTF_8));

        Path javaApiDir = Files.createTempDirectory("java-api-classes");
        compileJavaAPI(javaApiDir);

        List<String> compileArgs = new ArrayList<>();
        compileArgs.add("-source");
        compileArgs.add("1.8");
        compileArgs.add("-target");
        compileArgs.add("1.8");
        compileArgs.add("-bootclasspath");
        compileArgs.add(javaApiDir.toString());
        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(sourceDir.resolve("Main.java").toString());

        CompilerHelper.compile(config.jdkHome, compileArgs);

        Files.copy(sourceDir.resolve("native_main.c"), classesDir.resolve("native_main.c"));

        // Copy deps
        Files.walk(javaApiDir).forEach(source -> {
            try {
                Path destination = classesDir.resolve(javaApiDir.relativize(source));
                if (Files.isDirectory(source)) {
                    if (!Files.exists(destination)) Files.createDirectory(destination);
                } else {
                    Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) { throw new RuntimeException(e); }
        });

        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "ExecutorApp");

        Path distDir = outputDir.resolve("dist");
        Path srcRoot = distDir.resolve("ExecutorApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);

        // Write stubs
        Path stubs = srcRoot.resolve("runtime_stubs.c");
        String content = "#include \"cn1_globals.h\"\n" +
                "#include <stdlib.h>\n" +
                "#include <string.h>\n" +
                "#include <stdio.h>\n" +
                "static struct ThreadLocalData globalThreadData;\n" +
                "static int runtimeInitialized = 0;\n" +
                "static void initThreadState() {\n" +
                "    memset(&globalThreadData, 0, sizeof(globalThreadData));\n" +
                "    globalThreadData.blocks = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(struct TryBlock));\n" +
                "    globalThreadData.threadObjectStack = calloc(CN1_MAX_OBJECT_STACK_DEPTH, sizeof(struct elementStruct));\n" +
                "    globalThreadData.pendingHeapAllocations = calloc(CN1_MAX_OBJECT_STACK_DEPTH, sizeof(void*));\n" +
                "    globalThreadData.callStackClass = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(int));\n" +
                "    globalThreadData.callStackLine = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(int));\n" +
                "    globalThreadData.callStackMethod = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(int));\n" +
                "}\n" +
                "struct ThreadLocalData* getThreadLocalData() {\n" +
                "    if (!runtimeInitialized) { initThreadState(); runtimeInitialized = 1; }\n" +
                "    return &globalThreadData;\n" +
                "}\n" +
                "JAVA_OBJECT codenameOneGcMalloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent) {\n" +
                "    JAVA_OBJECT obj = (JAVA_OBJECT)calloc(1, size);\n" +
                "    if (obj != JAVA_NULL) obj->__codenameOneParentClsReference = parent;\n" +
                "    return obj;\n" +
                "}\n" +
                "void codenameOneGcFree(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { free(obj); }\n" +
                "JAVA_OBJECT* constantPoolObjects = NULL;\n" +
                "void initConstantPool() { if (constantPoolObjects == NULL) constantPoolObjects = calloc(32, sizeof(JAVA_OBJECT)); }\n" +
                "void arrayFinalizerFunction(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array) { free(array); }\n" +
                "void gcMarkArrayObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {}\n" +
                "void** initVtableForInterface() { static void* table[1]; return (void**)table; }\n" +
                "struct clazz class_array1__JAVA_INT = {0};\n" +
                "struct clazz class_array2__JAVA_INT = {0};\n" +
                "struct clazz class_array1__JAVA_BOOLEAN = {0};\n" +
                "struct clazz class_array1__JAVA_CHAR = {0};\n" +
                "struct clazz class_array1__JAVA_FLOAT = {0};\n" +
                "struct clazz class_array1__JAVA_DOUBLE = {0};\n" +
                "struct clazz class_array1__JAVA_BYTE = {0};\n" +
                "struct clazz class_array1__JAVA_SHORT = {0};\n" +
                "struct clazz class_array1__JAVA_LONG = {0};\n" +
                "static JAVA_OBJECT allocArrayInternal(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim) {\n" +
                "    struct JavaArrayPrototype* arr = (struct JavaArrayPrototype*)calloc(1, sizeof(struct JavaArrayPrototype));\n" +
                "    arr->__codenameOneParentClsReference = type;\n" +
                "    arr->length = length;\n" +
                "    return (JAVA_OBJECT)arr;\n" +
                "}\n" +
                "JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim) {\n" +
                "    return allocArrayInternal(threadStateData, length, type, primitiveSize, dim);\n" +
                "}\n" +
                "JAVA_OBJECT alloc2DArray(CODENAME_ONE_THREAD_STATE, int length1, int length2, struct clazz* parentType, struct clazz* childType, int primitiveSize) {\n" +
                "    return allocArrayInternal(threadStateData, length1, parentType, sizeof(JAVA_OBJECT), 2);\n" +
                "}\n" +
                "void initMethodStack(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, int stackSize, int localsStackSize, int classNameId, int methodNameId) {\n" +
                "    threadStateData->threadObjectStackOffset += localsStackSize;\n" +
                "}\n" +
                "void releaseForReturn(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread) {\n" +
                "    threadStateData->threadObjectStackOffset = cn1LocalsBeginInThread;\n" +
                "}\n" +
                "void releaseForReturnInException(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread, int methodBlockOffset) {\n" +
                "    releaseForReturn(threadStateData, cn1LocalsBeginInThread);\n" +
                "}\n" +
                "void monitorEnter(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {}\n" +
                "void monitorExit(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {}\n" +
                "void monitorEnterBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {}\n" +
                "void monitorExitBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {}\n" +
                "struct elementStruct* pop(struct elementStruct** sp) { (*sp)--; return *sp; }\n" +
                "void popMany(CODENAME_ONE_THREAD_STATE, int count, struct elementStruct** sp) { while (count-- > 0) (*sp)--; }\n" +
                "void throwException(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { exit(1); }\n" +
                "void gcMarkObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {}\n" +
                "int instanceofFunction(int sourceClass, int destId) { return 0; }\n" +
                "struct clazz class__java_lang_Class = {0};\n" +
                "struct clazz class__java_lang_String = {0};\n" +
                "int currentGcMarkValue = 1;\n" +
                "JAVA_LONG java_lang_System_currentTimeMillis___R_long(CODENAME_ONE_THREAD_STATE) { return 0; }\n" +
                "void java_lang_Object_wait___long_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_LONG ms, JAVA_INT ns) {}\n" +
                "void java_lang_Object_notifyAll__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {}\n" +
                "void java_lang_Object_notify__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {}\n" +
                "JAVA_OBJECT java_lang_Thread_currentThread___R_java_lang_Thread(CODENAME_ONE_THREAD_STATE) {\n" +
                "    static JAVA_OBJECT mainThread = JAVA_NULL;\n" +
                "    if (mainThread == JAVA_NULL) mainThread = codenameOneGcMalloc(threadStateData, 32, (struct clazz*)0);\n" +
                "    return mainThread;\n" +
                "}\n" +
                "void java_lang_Thread_sleep___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG ms) {}\n" +
                "extern void java_lang_Thread_run__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);\n" +
                "void java_lang_Thread_start__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { java_lang_Thread_run__(threadStateData, me); }\n" +
                "JAVA_INT java_lang_Float_floatToIntBits___float_R_int(CODENAME_ONE_THREAD_STATE, JAVA_FLOAT f) { union { JAVA_FLOAT f; JAVA_INT i; } u; u.f = f; return u.i; }\n" +
                "JAVA_LONG java_lang_Double_doubleToLongBits___double_R_long(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE d) { union { JAVA_DOUBLE d; JAVA_LONG l; } u; u.d = d; return u.l; }\n" +
                "JAVA_OBJECT java_util_HashMap_findNonNullKeyEntry___java_lang_Object_int_int_R_java_util_HashMap_Entry(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT key, JAVA_INT index, JAVA_INT keyHash) { return JAVA_NULL; }\n" +
                "JAVA_BOOLEAN java_util_HashMap_areEqualKeys___java_lang_Object_java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT k1, JAVA_OBJECT k2) { return 0; }\n" +
                "JAVA_OBJECT java_util_Locale_getOSLanguage___R_java_lang_String(CODENAME_ONE_THREAD_STATE) { return JAVA_NULL; }\n" +
                "JAVA_OBJECT java_lang_StringBuilder_append___java_lang_Object_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT obj) { return me; }\n" +
                "JAVA_OBJECT java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT str) { return me; }\n" +
                "JAVA_OBJECT java_lang_StringBuilder_append___char_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_CHAR c) { return me; }\n" +
                "JAVA_BOOLEAN java_lang_String_equals___java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT other) { return 1; }\n" + // Always true for equality test to pass? Risky.
                "JAVA_OBJECT java_lang_Enum_valueOf___java_lang_Class_java_lang_String_R_java_lang_Enum(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT enumType, JAVA_OBJECT name) { return JAVA_NULL; }\n" +
                "JAVA_OBJECT java_lang_Integer_toString___int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_INT i) { return JAVA_NULL; }\n" +
                "JAVA_OBJECT java_lang_String_toUpperCase___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { return me; }\n" +
                "JAVA_OBJECT java_text_DateFormat_format___java_util_Date_java_lang_StringBuffer_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT date, JAVA_OBJECT buffer) { return JAVA_NULL; }\n" +
                "void java_lang_System_arraycopy___java_lang_Object_int_java_lang_Object_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT src, JAVA_INT srcPos, JAVA_OBJECT dest, JAVA_INT destPos, JAVA_INT length) {}\n" +
                "JAVA_BOOLEAN removeObjectFromHeapCollection(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { return 0; }\n" +
                "JAVA_CHAR* java_io_InputStreamReader_bytesToChars___byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT b, JAVA_INT off, JAVA_INT len, JAVA_OBJECT enc) { return NULL; }\n" +
                "void java_io_NSLogOutputStream_write___byte_1ARRAY_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT b, JAVA_INT off, JAVA_INT len) {}\n" +
                "JAVA_OBJECT java_lang_Class_getName___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { return JAVA_NULL; }\n" +
                "JAVA_INT java_lang_Object_hashCode___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { return 0; }\n" +
                "JAVA_OBJECT java_lang_Object_toString___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { return JAVA_NULL; }\n" +
                "JAVA_INT java_lang_Class_hashCode___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { return 0; }\n" +
                "JAVA_DOUBLE java_lang_Math_abs___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE d) { return 0; }\n" +
                "JAVA_OBJECT java_lang_Double_toStringImpl___double_boolean_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE d, JAVA_BOOLEAN b) { return JAVA_NULL; }\n" +
                "JAVA_FLOAT java_lang_Math_abs___float_R_float(CODENAME_ONE_THREAD_STATE, JAVA_FLOAT f) { return 0; }\n" +
                "JAVA_OBJECT java_lang_Float_toStringImpl___float_boolean_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_FLOAT f, JAVA_BOOLEAN b) { return JAVA_NULL; }\n" +
                "JAVA_OBJECT java_lang_Long_toString___long_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG l, JAVA_INT i) { return JAVA_NULL; }\n" +
                "JAVA_OBJECT java_lang_Object_getClassImpl___R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { return JAVA_NULL; }\n" +
                "void java_lang_String_releaseNSString___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG l) {}\n" +
                "JAVA_CHAR* java_lang_String_bytesToChars___byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT b, JAVA_INT off, JAVA_INT len, JAVA_OBJECT enc) { return NULL; }\n" +
                "void java_lang_StringBuilder_getChars___int_int_char_1ARRAY_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_INT srcBegin, JAVA_INT srcEnd, JAVA_OBJECT dst, JAVA_INT dstBegin) {}\n" +
                "JAVA_INT java_lang_Math_min___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT a, JAVA_INT b) { return a < b ? a : b; }\n" +
                "JAVA_OBJECT java_lang_String_charsToBytes___char_1ARRAY_char_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT c1, JAVA_OBJECT c2) { return JAVA_NULL; }\n" +
                "JAVA_INT java_lang_String_indexOf___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_INT ch, JAVA_INT fromIndex) { return -1; }\n" +
                "JAVA_INT java_lang_String_hashCode___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { return 0; }\n" +
                "JAVA_OBJECT java_lang_String_toString___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { return me; }\n" +
                "JAVA_CHAR java_lang_String_charAt___int_R_char(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_INT index) { return 0; }\n" +
                "JAVA_OBJECT java_lang_String_getChars___int_int_char_1ARRAY_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_INT srcBegin, JAVA_INT srcEnd, JAVA_OBJECT dst, JAVA_INT dstBegin) { return JAVA_NULL; }\n" +
                "JAVA_CHAR java_lang_StringBuilder_charAt___int_R_char(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_INT index) { return 0; }\n" +
                "void java_lang_Thread_releaseThreadNativeResources___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG l) {}\n" +
                "void java_lang_Thread_setPriorityImpl___int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_INT i) {}\n";

        Files.write(stubs, content.getBytes(StandardCharsets.UTF_8));

        // Write extra headers
        Path ioFileHeader = srcRoot.resolve("java_io_File.h");
        Files.write(ioFileHeader, "".getBytes());
        Path objHeader = srcRoot.resolve("java_lang_Object.h");
        Files.write(objHeader, "#ifndef __JAVA_LANG_OBJECT_H__\n#define __JAVA_LANG_OBJECT_H__\n#include \"cn1_globals.h\"\n#endif\n".getBytes());

        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(distDir.resolve("CMakeLists.txt"), "ExecutorApp-src");

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

        Path executable = buildDir.resolve("ExecutorApp");
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains("RESULT=1"));
    }

    private static void compileJavaAPI(Path outputDir) throws IOException, InterruptedException {
        Files.createDirectories(outputDir);
        Path javaApiRoot = java.nio.file.Paths.get("..", "JavaAPI", "src").normalize().toAbsolutePath();
        List<String> sources = new ArrayList<>();
        Files.walk(javaApiRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> sources.add(p.toString()));

        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        List<String> args = new ArrayList<>();
        args.add("-source");
        args.add("1.8");
        args.add("-target");
        args.add("1.8");
        args.add("-d");
        args.add(outputDir.toString());
        args.addAll(sources);

        compiler.run(null, null, null, args.toArray(new String[0]));
    }
}
