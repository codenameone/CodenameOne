package com.codename1.tools.translator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Helper class to manage external JDK compilers.
 */
public class CompilerHelper {

    private static final Map<String, Path> availableJdks = new TreeMap<>();

    static {
        // Detect JDKs from environment variables set by CI or local setup
        checkAndAddJdk("8", System.getenv("JDK_8_HOME"));
        checkAndAddJdk("11", System.getenv("JDK_11_HOME"));
        checkAndAddJdk("17", System.getenv("JDK_17_HOME"));
        checkAndAddJdk("21", System.getenv("JDK_21_HOME"));
        checkAndAddJdk("25", System.getenv("JDK_25_HOME"));

        // Fallback: If no env vars, assume current JVM is JDK 8 (or whatever is running)
        // This ensures tests pass locally or in environments not fully configured with all JDKs
        if (availableJdks.isEmpty()) {
            String currentJavaHome = System.getProperty("java.home");
            // If it's a JRE, try to find JDK
            if (currentJavaHome.endsWith("jre")) {
                currentJavaHome = currentJavaHome.substring(0, currentJavaHome.length() - 4);
            }
            availableJdks.put(System.getProperty("java.specification.version"), Paths.get(currentJavaHome));
        }
    }

    private static void checkAndAddJdk(String version, String path) {
        if (path != null && !path.isEmpty() && new File(path).exists()) {
            availableJdks.put(version, Paths.get(path));
        }
    }

    public static List<CompilerConfig> getAvailableCompilers(String targetVersion) {
        List<CompilerConfig> compilers = new ArrayList<>();

        for (Map.Entry<String, Path> entry : availableJdks.entrySet()) {
            String jdkVersion = entry.getKey();
            Path jdkHome = entry.getValue();

            if (canCompile(jdkVersion, targetVersion)) {
                compilers.add(new CompilerConfig(jdkVersion, jdkHome, targetVersion));
            }
        }

        // If we are running in a constrained environment (e.g. local dev without env vars),
        // we might not have found the specific JDK requested.
        // If the list is empty, and target is 1.5 or 1.8, and we have *some* JDK, try to use it
        // if it supports the target.
        if (compilers.isEmpty() && !availableJdks.isEmpty()) {
             Map.Entry<String, Path> defaultJdk = availableJdks.entrySet().iterator().next();
             if (canCompile(defaultJdk.getKey(), targetVersion)) {
                 compilers.add(new CompilerConfig(defaultJdk.getKey(), defaultJdk.getValue(), targetVersion));
             }
        }

        return compilers;
    }

    private static boolean canCompile(String compilerVersion, String targetVersion) {
        try {
            double compilerVer = Double.parseDouble(compilerVersion);
            double targetVer = Double.parseDouble(targetVersion);

            // Java 9+ (version 9, 11, etc) dropped support for 1.5
            if (targetVer == 1.5) {
                return compilerVer < 9;
            }
            // Java 21? dropped support for 1.6/1.7?
            // Generally newer JDKs support 1.8+
            return compilerVer >= targetVer || (compilerVer >= 1.8 && targetVer <= 1.8);
        } catch (NumberFormatException e) {
            // Handle "1.8" format
            if (compilerVersion.startsWith("1.")) {
                return true; // Old JDKs support old targets
            }
            // Fallback for "25-ea"
            if (compilerVersion.contains("-")) {
                 // Assume it's a new JDK
                 return !"1.5".equals(targetVersion);
            }
            return true;
        }
    }

    public static int compile(Path jdkHome, List<String> args) throws IOException, InterruptedException {
        String javac = jdkHome.resolve("bin").resolve("javac").toString();
        // On Windows it might be javac.exe
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            javac += ".exe";
        }

        List<String> command = new ArrayList<>();
        command.add(javac);

        // Filter out flags that might be unsupported on newer JDKs if target is old,
        // but generally we rely on the caller to provide correct flags.
        // However, we might need to suppress warnings for obsolete targets.
        // args.add("-Xlint:-options"); // Added by caller?

        command.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(command);
        // Inherit IO so we see errors in the log
        pb.inheritIO();
        Process p = pb.start();
        return p.waitFor();
    }

    public static class CompilerConfig {
        public final String jdkVersion;
        public final Path jdkHome;
        public final String targetVersion;

        public CompilerConfig(String jdkVersion, Path jdkHome, String targetVersion) {
            this.jdkVersion = jdkVersion;
            this.jdkHome = jdkHome;
            this.targetVersion = targetVersion;
        }

        @Override
        public String toString() {
            return "JDK " + jdkVersion + " (Target " + targetVersion + ")";
        }
    }

    public static boolean compileAndRun(String code, String expectedOutput) throws Exception {
        // Find a suitable compiler (e.g. JDK 8 targeting 1.8)
        List<CompilerConfig> compilers = getAvailableCompilers("1.8");
        if (compilers.isEmpty()) {
            // Fallback for environment where maybe we just run with what we have
            compilers = getAvailableCompilers("1.5");
        }
        if (compilers.isEmpty()) {
             throw new RuntimeException("No suitable compiler found");
        }
        CompilerConfig config = compilers.get(0);

        java.nio.file.Path sourceDir = java.nio.file.Files.createTempDirectory("executor-test-src");
        java.nio.file.Path classesDir = java.nio.file.Files.createTempDirectory("executor-test-classes");
        java.nio.file.Path outputDir = java.nio.file.Files.createTempDirectory("executor-test-output");

        try {
            java.nio.file.Files.write(sourceDir.resolve("Main.java"), code.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            java.nio.file.Path javaApiDir = java.nio.file.Files.createTempDirectory("java-api-classes");
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

            if (compile(config.jdkHome, compileArgs) != 0) {
                return false;
            }

            // Merge javaApiDir into classesDir so translator finds dependencies
            java.nio.file.Files.walk(javaApiDir)
                .forEach(source -> {
                    try {
                        java.nio.file.Path destination = classesDir.resolve(javaApiDir.relativize(source));
                        if (java.nio.file.Files.isDirectory(source)) {
                            if (!java.nio.file.Files.exists(destination)) {
                                java.nio.file.Files.createDirectory(destination);
                            }
                        } else {
                            java.nio.file.Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

            CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "ExecutorApp");

            java.nio.file.Path distDir = outputDir.resolve("dist");
            java.nio.file.Path srcRoot = distDir.resolve("ExecutorApp-src");
            CleanTargetIntegrationTest.patchCn1Globals(srcRoot);

            // Write basic stubs
            java.nio.file.Path ioFileHeader = srcRoot.resolve("java_io_File.h");
            if (!java.nio.file.Files.exists(ioFileHeader)) {
                java.nio.file.Files.write(ioFileHeader, "".getBytes());
            }

            java.nio.file.Path objectHeader = srcRoot.resolve("java_lang_Object.h");
            if (!java.nio.file.Files.exists(objectHeader)) {
                String headerContent = "#ifndef __JAVA_LANG_OBJECT_H__\n" +
                        "#define __JAVA_LANG_OBJECT_H__\n" +
                        "#include \"cn1_globals.h\"\n" +
                        "#endif\n";
                java.nio.file.Files.write(objectHeader, headerContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            java.nio.file.Path stubs = srcRoot.resolve("runtime_stubs.c");
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
                "    threadStateData->threadObjectStackOffset += localsStackSize;\n" +
                "}\n" +
                "\n" +
                "void releaseForReturn(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread) {\n" +
                "    threadStateData->threadObjectStackOffset = cn1LocalsBeginInThread;\n" +
                "}\n" +
                "\n" +
                "void releaseForReturnInException(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread, int methodBlockOffset) {\n" +
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
                "void gcMarkObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) { (void)obj; (void)force; }\n" +
                "// Stub instanceofFunction. Note: signature in cn1_globals.h might differ (int vs pointers) in some versions.\n" +
                "// If cn1_globals.h declares it as (int, int), we must match or stub differently.\n" +
                "// But typically for C output it takes ThreadState + Object + Class.\n" +
                "// The error said: previous declaration 'int instanceofFunction(int, int)'.\n" +
                "// This implies the translator generated a legacy declaration or something specific to the test environment.\n" +
                "// However, runtime_stubs.c is C code. If we include cn1_globals.h, we must match it.\n" +
                "// Let's rely on the fact that if we don't implement it, we get undefined reference.\n" +
                "// If we implement it with wrong signature, we get error.\n" +
                "// The error output showed cn1_globals.h having: extern int instanceofFunction(int sourceClass, int destId);\n" +
                "// This suggests we are in a mode where objects are ints? No, that's likely for old CLDC/C++ target?\n" +
                "// Or maybe `ExecutorApp` is configured with `none` or `ios` but generated headers use this?\n" +
                "// Let's try to match the signature from the error message.\n" +
                "int instanceofFunction(int sourceClass, int destId) { return 0; }\n" +
                "\n" +
                // "struct clazz class__java_lang_Class = {0};\n" +
                // "struct clazz class__java_lang_String = {0};\n" +
                "int currentGcMarkValue = 1;\n" +
                "JAVA_LONG java_lang_System_currentTimeMillis___R_long(CODENAME_ONE_THREAD_STATE) { return 0; }\n" +
                "// JAVA_LONG java_lang_System_nanoTime___R_long(CODENAME_ONE_THREAD_STATE) { return 0; }\n" +
                "void java_lang_Object_wait___long_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_LONG ms, JAVA_INT ns) {}\n" +
                "void java_lang_Object_notifyAll__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {}\n" +
                "void java_lang_Object_notify__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {}\n" +
                "JAVA_OBJECT java_lang_Thread_currentThread___R_java_lang_Thread(CODENAME_ONE_THREAD_STATE) {\n" +
                "    return JAVA_NULL; // Simplification\n" +
                "}\n" +
                "// void java_lang_Thread_interrupt__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {}\n" +
                "// void java_lang_Thread_yield__(CODENAME_ONE_THREAD_STATE) {}\n" +
                "void java_lang_Thread_sleep___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG ms) {}\n" +
                "void java_lang_Thread_start__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { extern void java_lang_Thread_run__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me); java_lang_Thread_run__(threadStateData, me); }\n";

            String mockThreadContent = content +
                "extern void java_lang_Thread_run__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);\n" +
                // Remove redundant java_lang_Thread_start__ since it's already in content
                // "void java_lang_Thread_start__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {\n" +
                // "    java_lang_Thread_run__(threadStateData, me);\n" +
                // "}\n";
                // Wait, the content definition was empty "{}". I want to override it.
                // But redefinition is an error.
                // I should have removed it from "content" string above.
                "";

            // Rewrite content to use the active start implementation
            // content = content.replace(
            //    "void java_lang_Thread_start__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {}",
            //    "extern void java_lang_Thread_run__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);\n" +
            //    "void java_lang_Thread_start__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { java_lang_Thread_run__(threadStateData, me); }"
            // );

            // Stub float/double bits
            content += "\n" +
                "JAVA_INT java_lang_Float_floatToIntBits___float_R_int(CODENAME_ONE_THREAD_STATE, JAVA_FLOAT f) {\n" +
                "    union { JAVA_FLOAT f; JAVA_INT i; } u; u.f = f; return u.i;\n" +
                "}\n" +
                "JAVA_LONG java_lang_Double_doubleToLongBits___double_R_long(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE d) {\n" +
                "    union { JAVA_DOUBLE d; JAVA_LONG l; } u; u.d = d; return u.l;\n" +
                "}\n" +
                "JAVA_OBJECT java_util_HashMap_findNonNullKeyEntry___java_lang_Object_int_int_R_java_util_HashMap_Entry(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT key, JAVA_INT index, JAVA_INT keyHash) {\n" +
                "    return JAVA_NULL; // Stub\n" +
                "}\n" +
                "JAVA_BOOLEAN java_util_HashMap_areEqualKeys___java_lang_Object_java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT k1, JAVA_OBJECT k2) {\n" +
                "    return 0; // Stub\n" +
                "}\n" +
                "JAVA_OBJECT java_util_Locale_getOSLanguage___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {\n" +
                "    return JAVA_NULL; // Stub\n" +
                "}\n" +
                "JAVA_OBJECT java_lang_StringBuilder_append___java_lang_Object_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT obj) { return me; }\n" +
                "JAVA_OBJECT java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT str) { return me; }\n" +
                "JAVA_OBJECT java_lang_StringBuilder_append___char_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_CHAR c) { return me; }\n" +
                "JAVA_BOOLEAN java_lang_String_equals___java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT other) { return 0; }\n" +
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

            java.nio.file.Files.write(stubs, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(outputDir.resolve("dist").resolve("CMakeLists.txt"), "ExecutorApp-src");

            java.nio.file.Path buildDir = distDir.resolve("build");
            java.nio.file.Files.createDirectories(buildDir);

            CleanTargetIntegrationTest.runCommand(Arrays.asList(
                    "cmake",
                    "-S", distDir.toString(),
                    "-B", buildDir.toString(),
                    "-DCMAKE_C_COMPILER=clang",
                    "-DCMAKE_OBJC_COMPILER=clang"
            ), distDir);

            CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

            java.nio.file.Path executable = buildDir.resolve("ExecutorApp");
            String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
            return output.contains(expectedOutput);

        } finally {
            // cleanup?
        }
    }

    private static void compileJavaAPI(java.nio.file.Path outputDir) throws IOException, InterruptedException {
        java.nio.file.Files.createDirectories(outputDir);
        java.nio.file.Path javaApiRoot = java.nio.file.Paths.get("..", "JavaAPI", "src").normalize().toAbsolutePath();
        List<String> sources = new ArrayList<>();
        java.nio.file.Files.walk(javaApiRoot)
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
