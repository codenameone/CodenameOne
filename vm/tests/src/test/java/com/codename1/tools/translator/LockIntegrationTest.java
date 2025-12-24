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

        Files.write(sourceDir.resolve("LockTestApp.java"), lockTestAppSource().getBytes(StandardCharsets.UTF_8));

        // Compile JavaAPI for bootclasspath
        compileJavaAPI(javaApiDir);

        Path nativeReport = sourceDir.resolve("native_report.c");
        Files.write(nativeReport, nativeReportSource().getBytes(StandardCharsets.UTF_8));

        List<String> compileArgs = new ArrayList<>();

        double jdkVer = 1.8;
        try { jdkVer = Double.parseDouble(config.jdkVersion); } catch (NumberFormatException ignored) {}

        if (jdkVer >= 9) {
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
        compileArgs.add(sourceDir.resolve("LockTestApp.java").toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "LockTestApp should compile with " + config);

        // Copy JavaAPI classes to classesDir so translator can see them
        copyDirectory(javaApiDir, classesDir);

        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("lock-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "LockTestApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists));

        Path srcRoot = distDir.resolve("LockTestApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);
        writeRuntimeStubs(srcRoot);
        writeMissingHeadersAndImpls(srcRoot);

        replaceLibraryWithExecutableTarget(cmakeLists, srcRoot.getFileName().toString());

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

        Path executable = buildDir.resolve("LockTestApp");
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);

        // Verify output assertions
        assertTrue(output.contains("TEST: Basic Lock OK"), "Basic lock should work");
        assertTrue(output.contains("TEST: Reentrancy OK"), "Reentrant lock should work");
        assertTrue(output.contains("TEST: TryLock OK"), "TryLock should work");
        assertTrue(output.contains("TEST: Condition OK"), "Condition wait/signal should work");
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
                "        Lock lock = new ReentrantLock();\n" +
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
                "        Lock lock = new ReentrantLock();\n" +
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
                "        final Lock lock = new ReentrantLock();\n" +
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

    private void compileJavaAPI(Path outputDir) throws Exception {
        Files.createDirectories(outputDir);
        Path javaApiRoot = java.nio.file.Paths.get("..", "JavaAPI", "src").normalize().toAbsolutePath();
        List<String> sources = new ArrayList<>();
        Files.walk(javaApiRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> sources.add(p.toString()));

        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
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

    private String nativeReportSource() {
        return "#include \"cn1_globals.h\"\n" +
                "#include <stdio.h>\n" +
                "void LockTestApp_report___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT msg) {\n" +
                "    // Simple string printing logic for test\n" +
                "    // Assume standard ASCII char array in String\n" +
                "    // struct String { Object header; char* value; int offset; int count; ... }\n" +
                "    \n" +
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
                "    }\n" +
                "}\n";
    }

    private void writeMissingHeadersAndImpls(Path srcRoot) throws Exception {
        // We need java.lang.Thread for our test
        Path threadHeader = srcRoot.resolve("java_lang_Thread.h");
        if (!Files.exists(threadHeader)) {
             String content = "#ifndef __JAVA_LANG_THREAD_H__\n" +
                     "#define __JAVA_LANG_THREAD_H__\n" +
                     "#include \"cn1_globals.h\"\n" +
                     "extern struct clazz class__java_lang_Thread;\n" +
                     "JAVA_OBJECT __NEW_java_lang_Thread(CODENAME_ONE_THREAD_STATE);\n" +
                     "void java_lang_Thread___INIT____(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);\n" +
                     "void java_lang_Thread___INIT_____java_lang_Runnable(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT runnable);\n" +
                     "void java_lang_Thread_start__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);\n" +
                     "JAVA_OBJECT java_lang_Thread_currentThread___R_java_lang_Thread(CODENAME_ONE_THREAD_STATE);\n" +
                     "JAVA_BOOLEAN java_lang_Thread_interrupted___R_boolean(CODENAME_ONE_THREAD_STATE);\n" +
                     "void java_lang_Thread_interrupt__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);\n" +
                     "void java_lang_Thread_sleep___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG millis);\n" +
                     "#endif\n";
             Files.write(threadHeader, content.getBytes(StandardCharsets.UTF_8));
        }

        Path extraStubs = srcRoot.resolve("extra_stubs.c");
        // We use runtime_stubs.c for the main logic, so extra_stubs can be empty or supplementary
        String stubs = "";
        Files.write(extraStubs, stubs.getBytes(StandardCharsets.UTF_8));
    }

    static void copyDirectory(Path source, Path target) throws java.io.IOException {
        Files.walk(source).forEach(sourcePath -> {
            Path targetPath = target.resolve(source.relativize(sourcePath));
            try {
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(targetPath)) Files.createDirectory(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static void replaceLibraryWithExecutableTarget(Path cmakeLists, String sourceDirName) throws java.io.IOException {
        String content = new String(Files.readAllBytes(cmakeLists), StandardCharsets.UTF_8);
        String globWithObjc = String.format("file(GLOB TRANSLATOR_SOURCES \"%s/*.c\" \"%s/*.m\")", sourceDirName, sourceDirName);
        String globCOnly = String.format("file(GLOB TRANSLATOR_SOURCES \"%s/*.c\")", sourceDirName);
        content = content.replace(globWithObjc, globCOnly);
        String replacement = content.replace(
                "add_library(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})",
                "add_executable(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})\ntarget_link_libraries(${PROJECT_NAME} m pthread)"
        );
        Files.write(cmakeLists, replacement.getBytes(StandardCharsets.UTF_8));
    }

    private void writeRuntimeStubs(Path srcRoot) throws java.io.IOException {
        // Ensure standard headers exist
        Path objectHeader = srcRoot.resolve("java_lang_Object.h");
        if (!Files.exists(objectHeader)) {
            String headerContent = "#ifndef __JAVA_LANG_OBJECT_H__\n" +
                    "#define __JAVA_LANG_OBJECT_H__\n" +
                    "#include \"cn1_globals.h\"\n" +
                    "extern struct clazz class__java_lang_Object;\n" +
                    "void __FINALIZER_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);\n" +
                    "void __GC_MARK_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force);\n" +
                    "void java_lang_Object___INIT____(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);\n" +
                    "JAVA_OBJECT __NEW_java_lang_Object(CODENAME_ONE_THREAD_STATE);\n" +
                    "void __INIT_VTABLE_java_lang_Object(CODENAME_ONE_THREAD_STATE, void** vtable);\n" +
                    "JAVA_BOOLEAN java_lang_Object_equals___java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT other);\n" +
                    "JAVA_OBJECT java_lang_Object_getClass___R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);\n" +
                    "JAVA_INT java_lang_Object_hashCode___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);\n" +
                    "void java_lang_Object_notify__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);\n" +
                    "void java_lang_Object_notifyAll__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);\n" +
                    "void java_lang_Object_wait__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);\n" +
                    "void java_lang_Object_wait___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_LONG ms);\n" +
                    "void java_lang_Object_wait___long_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_LONG ms, JAVA_INT ns);\n" +
                    "#endif\n";
            Files.write(objectHeader, headerContent.getBytes(StandardCharsets.UTF_8));
        }

        Path stringHeader = srcRoot.resolve("java_lang_String.h");
        if (!Files.exists(stringHeader)) {
             String content = "#ifndef __JAVA_LANG_STRING_H__\n#define __JAVA_LANG_STRING_H__\n#include \"cn1_globals.h\"\nextern struct clazz class__java_lang_String;\n#endif\n";
             Files.write(stringHeader, content.getBytes(StandardCharsets.UTF_8));
        }

        // Custom runtime stubs supporting pthreads and monitors
        Path stubs = srcRoot.resolve("runtime_stubs.c");
        String content = "#include \"cn1_globals.h\"\n" +
                "#include \"java_lang_Object.h\"\n" +
                "#include <stdlib.h>\n" +
                "#include <string.h>\n" +
                "#include <stdio.h>\n" +
                "#include <pthread.h>\n" +
                "#include <unistd.h>\n" +
                "#include <sys/time.h>\n" +
                "\n" +
                "static pthread_key_t thread_state_key;\n" +
                "static pthread_once_t key_once = PTHREAD_ONCE_INIT;\n" +
                "\n" +
                "static void make_key() {\n" +
                "    pthread_key_create(&thread_state_key, free);\n" +
                "}\n" +
                "\n" +
                "struct ThreadLocalData* getThreadLocalData() {\n" +
                "    pthread_once(&key_once, make_key);\n" +
                "    struct ThreadLocalData* data = pthread_getspecific(thread_state_key);\n" +
                "    if (!data) {\n" +
                "        data = calloc(1, sizeof(struct ThreadLocalData));\n" +
                "        // Init stacks...\n" +
                "        data->blocks = calloc(100, sizeof(struct TryBlock));\n" +
                "        data->threadObjectStack = calloc(100, sizeof(struct elementStruct));\n" +
                "        data->pendingHeapAllocations = calloc(100, sizeof(void*));\n" +
                "        // ... minimal init\n" +
                "        pthread_setspecific(thread_state_key, data);\n" +
                "    }\n" +
                "    return data;\n" +
                "}\n" +
                "\n" +
                "// Monitor implementation using a global hash table or added field?\n" +
                "// Since we can't easily change the object layout (defined by translator),\n" +
                "// we can use a side-table for monitors.\n" +
                "// Simple side table:\n" +
                "#define MAX_MONITORS 1024\n" +
                "typedef struct {\n" +
                "    JAVA_OBJECT obj;\n" +
                "    pthread_mutex_t mutex;\n" +
                "    pthread_cond_t cond;\n" +
                "} Monitor;\n" +
                "static Monitor monitors[MAX_MONITORS];\n" +
                "static pthread_mutex_t global_monitor_lock = PTHREAD_MUTEX_INITIALIZER;\n" +
                "\n" +
                "static Monitor* getMonitor(JAVA_OBJECT obj) {\n" +
                "    pthread_mutex_lock(&global_monitor_lock);\n" +
                "    for(int i=0; i<MAX_MONITORS; i++) {\n" +
                "        if (monitors[i].obj == obj) {\n" +
                "            pthread_mutex_unlock(&global_monitor_lock);\n" +
                "            return &monitors[i];\n" +
                "        }\n" +
                "    }\n" +
                "    // Create new\n" +
                "    for(int i=0; i<MAX_MONITORS; i++) {\n" +
                "        if (monitors[i].obj == NULL) {\n" +
                "            monitors[i].obj = obj;\n" +
                "            pthread_mutex_init(&monitors[i].mutex, NULL);\n" +
                "            pthread_cond_init(&monitors[i].cond, NULL);\n" +
                "            pthread_mutex_unlock(&global_monitor_lock);\n" +
                "            return &monitors[i];\n" +
                "        }\n" +
                "    }\n" +
                "    pthread_mutex_unlock(&global_monitor_lock);\n" +
                "    fprintf(stderr, \"Too many monitors!\\n\");\n" +
                "    exit(1);\n" +
                "}\n" +
                "\n" +
                "void monitorEnter(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {\n" +
                "    if (!obj) return;\n" +
                "    Monitor* m = getMonitor(obj);\n" +
                "    pthread_mutex_lock(&m->mutex);\n" +
                "}\n" +
                "\n" +
                "void monitorExit(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {\n" +
                "    if (!obj) return;\n" +
                "    Monitor* m = getMonitor(obj);\n" +
                "    pthread_mutex_unlock(&m->mutex);\n" +
                "}\n" +
                "\n" +
                "void java_lang_Object_wait___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_LONG timeout) {\n" +
                "    // Timeout ignored for simplicity or implemented?\n" +
                "    Monitor* m = getMonitor(obj);\n" +
                "    // wait releases mutex and waits\n" +
                "    if (timeout > 0) {\n" +
                "        struct timespec ts;\n" +
                "        // clock_gettime(CLOCK_REALTIME, &ts); // requires linking librt sometimes\n" +
                "        // simple wait for now\n" +
                "        pthread_cond_wait(&m->cond, &m->mutex);\n" +
                "    } else {\n" +
                "        pthread_cond_wait(&m->cond, &m->mutex);\n" +
                "    }\n" +
                "}\n" +
                "void java_lang_Object_wait__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {\n" +
                "    java_lang_Object_wait___long(threadStateData, obj, 0);\n" +
                "}\n" +
                "void java_lang_Object_wait___long_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_LONG ms, JAVA_INT ns) {\n" +
                "    java_lang_Object_wait___long(threadStateData, obj, ms);\n" +
                "}\n" +
                "\n" +
                "void java_lang_Object_notify__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {\n" +
                "    Monitor* m = getMonitor(obj);\n" +
                "    pthread_cond_signal(&m->cond);\n" +
                "}\n" +
                "\n" +
                "void java_lang_Object_notifyAll__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {\n" +
                "    Monitor* m = getMonitor(obj);\n" +
                "    pthread_cond_broadcast(&m->cond);\n" +
                "}\n" +
                "\n" +
                "// Other required stubs\n" +
                "JAVA_OBJECT* constantPoolObjects = NULL;\n" +
                "void initConstantPool() {\n" +
                "    if (constantPoolObjects == NULL) {\n" +
                "        constantPoolObjects = calloc(1024, sizeof(JAVA_OBJECT));\n" +
                "    }\n" +
                "}\n" +
                "JAVA_OBJECT codenameOneGcMalloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent) {\n" +
                "    JAVA_OBJECT obj = (JAVA_OBJECT)calloc(1, size);\n" +
                "    if (obj != JAVA_NULL) {\n" +
                "        obj->__codenameOneParentClsReference = parent;\n" +
                "    }\n" +
                "    return obj;\n" +
                "}\n" +
                "void codenameOneGcFree(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { free(obj); }\n" +
                "void arrayFinalizerFunction(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array) { free(array); }\n" +
                "void gcMarkArrayObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {}\n" +
                "void gcMarkObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {}\n" +
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
                "void initMethodStack(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, int stackSize, int localsStackSize, int classNameId, int methodNameId) {}\n" +
                "void releaseForReturn(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread) {}\n" +
                "void releaseForReturnInException(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread, int methodBlockOffset) {}\n" +
                "void monitorEnterBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {}\n" +
                "void monitorExitBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {}\n" +
                "struct elementStruct* pop(struct elementStruct** sp) { (*sp)--; return *sp; }\n" +
                "void popMany(CODENAME_ONE_THREAD_STATE, int count, struct elementStruct** sp) { while(count--) (*sp)--; }\n" +
                "void throwException(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { exit(1); }\n" +
                "int instanceofFunction(int sourceClass, int destId) { return 1; }\n" +
                "struct clazz class__java_lang_Class = {0};\n" +
                "struct clazz class__java_lang_String = {0};\n" +
                "int currentGcMarkValue = 1;\n" +
                "\n" +
                "// Implement Thread.start and Thread.currentThread\n" +
                "void virtual_java_lang_Runnable_run__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);\n" +
                "void virtual_java_lang_Thread_run__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);\n" +
                "\n" +
                "void* java_thread_entry(void* arg) {\n" +
                "    JAVA_OBJECT threadObj = (JAVA_OBJECT)arg;\n" +
                "    struct ThreadLocalData* data = getThreadLocalData();\n" +
                "    virtual_java_lang_Thread_run__(data, threadObj);\n" +
                "    return NULL;\n" +
                "}\n" +
                "\n" +
                "void java_lang_Thread_start__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {\n" +
                "    pthread_t pt;\n" +
                "    pthread_create(&pt, NULL, java_thread_entry, me);\n" +
                "}\n" +
                "JAVA_OBJECT java_lang_Thread_currentThread___R_java_lang_Thread(CODENAME_ONE_THREAD_STATE) {\n" +
                "    return JAVA_NULL; // Stub\n" +
                "}\n" +
                "JAVA_BOOLEAN java_lang_Thread_interrupted___R_boolean(CODENAME_ONE_THREAD_STATE) {\n" +
                "    return 0;\n" +
                "}\n" +
                "void java_lang_Thread_interrupt__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {}\n" +
                "void java_lang_Thread_interrupt0__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {}\n" +
                "JAVA_BOOLEAN java_lang_Thread_isInterrupted___boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_BOOLEAN clear) {\n" +
                "    return 0;\n" +
                "}\n" +
                "void java_lang_Thread_sleep___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG millis) {\n" +
                "    usleep(millis * 1000);\n" +
                "}\n" +
                "JAVA_LONG java_lang_System_currentTimeMillis___R_long(CODENAME_ONE_THREAD_STATE) {\n" +
                "    struct timeval tv;\n" +
                "    gettimeofday(&tv, NULL);\n" +
                "    return (long long)tv.tv_sec * 1000 + tv.tv_usec / 1000;\n" +
                "}\n" +
                "JAVA_BOOLEAN java_lang_String_equals___java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT other) { return 0; }\n" +
                "JAVA_OBJECT java_lang_Enum_valueOf___java_lang_Class_java_lang_String_R_java_lang_Enum(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls, JAVA_OBJECT name) { return JAVA_NULL; }\n" +
                "JAVA_OBJECT java_util_HashMap_findNonNullKeyEntry___java_lang_Object_int_int_R_java_util_HashMap_Entry(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT key, JAVA_INT hash, JAVA_INT index) { return JAVA_NULL; }\n" +
                "JAVA_BOOLEAN java_util_HashMap_areEqualKeys___java_lang_Object_java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT k1, JAVA_OBJECT k2) { return 0; }\n" +
                "JAVA_OBJECT java_util_Locale_getOSLanguage___R_java_lang_String(CODENAME_ONE_THREAD_STATE) { return JAVA_NULL; }\n" +
                "JAVA_OBJECT java_lang_Integer_toString___int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_INT i) { return JAVA_NULL; }\n" +
                "JAVA_OBJECT java_lang_StringBuilder_append___java_lang_Object_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT obj) { return me; }\n" +
                "JAVA_OBJECT java_lang_String_toUpperCase___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { return me; }\n" +
                "JAVA_OBJECT java_text_DateFormat_format___java_util_Date_java_lang_StringBuffer_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT date, JAVA_OBJECT buffer) { return JAVA_NULL; }\n" +
                "JAVA_INT java_lang_Float_floatToIntBits___float_R_int(CODENAME_ONE_THREAD_STATE, JAVA_FLOAT f) { union { float f; int i; } u; u.f = f; return u.i; }\n" +
                "JAVA_LONG java_lang_Double_doubleToLongBits___double_R_long(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE d) { union { double d; long long l; } u; u.d = d; return u.l; }\n" +
                "JAVA_OBJECT java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT str) { return me; }\n" +
                "JAVA_CHAR* java_lang_String_bytesToChars___byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT b, JAVA_INT off, JAVA_INT len, JAVA_OBJECT encoding) { return NULL; }\n" +
                "JAVA_OBJECT java_lang_String_charsToBytes___char_1ARRAY_char_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT arr, JAVA_OBJECT encoding) { return NULL; }\n" +
                "JAVA_INT java_lang_String_indexOf___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_INT ch, JAVA_INT fromIndex) { return -1; }\n" +
                "JAVA_INT java_lang_Math_min___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT a, JAVA_INT b) { return a < b ? a : b; }\n" +
                "JAVA_INT java_lang_String_hashCode___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { return 0; }\n" +
                "JAVA_CHAR java_lang_String_charAt___int_R_char(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_INT index) { return 0; }\n" +
                "JAVA_OBJECT java_lang_String_toString___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { return me; }\n" +
                "void java_lang_Thread_releaseThreadNativeResources___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG id) {}\n" +
                "JAVA_OBJECT java_lang_StringBuilder_getChars___int_int_char_1ARRAY_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_INT srcBegin, JAVA_INT srcEnd, JAVA_OBJECT dst, JAVA_INT dstBegin) { return JAVA_NULL; }\n" +
                "JAVA_OBJECT java_lang_StringBuilder_append___char_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_CHAR c) { return me; }\n" +
                "JAVA_OBJECT java_lang_StringBuilder_charAt___int_R_char(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_INT index) { return 0; }\n" +
                "JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim) {\n" +
                "    struct JavaArrayPrototype* arr = (struct JavaArrayPrototype*)calloc(1, sizeof(struct JavaArrayPrototype));\n" +
                "    arr->__codenameOneParentClsReference = type;\n" +
                "    arr->length = length;\n" +
                "    arr->dimensions = dim;\n" +
                "    arr->primitiveSize = primitiveSize;\n" +
                "    int size = primitiveSize ? primitiveSize : sizeof(JAVA_OBJECT);\n" +
                "    arr->data = calloc(length, size);\n" +
                "    return (JAVA_OBJECT)arr;\n" +
                "}\n" +
                "void java_lang_System_arraycopy___java_lang_Object_int_java_lang_Object_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT src, JAVA_INT srcPos, JAVA_OBJECT dest, JAVA_INT destPos, JAVA_INT length) {\n" +
                "    if (!src || !dest) return;\n" +
                "    struct JavaArrayPrototype* s = (struct JavaArrayPrototype*)src;\n" +
                "    struct JavaArrayPrototype* d = (struct JavaArrayPrototype*)dest;\n" +
                "    int size = s->primitiveSize ? s->primitiveSize : sizeof(JAVA_OBJECT);\n" +
                "    char* sData = (char*)s->data;\n" +
                "    char* dData = (char*)d->data;\n" +
                "    if (sData && dData) memmove(dData + destPos * size, sData + srcPos * size, length * size);\n" +
                "}\n";

        Files.write(stubs, content.getBytes(StandardCharsets.UTF_8));
    }
}
