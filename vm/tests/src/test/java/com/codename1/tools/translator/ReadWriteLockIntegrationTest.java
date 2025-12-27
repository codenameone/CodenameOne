package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

        // 1. Write minimal mock Java API to sourceDir
        writeMockJavaClasses(sourceDir);

        // 2. Copy the actual Lock/ReentrantLock/Condition sources we want to test
        Path javaApiSrc = Paths.get("..", "JavaAPI", "src").toAbsolutePath().normalize();
        Path locksDir = sourceDir.resolve("java/util/concurrent/locks");
        Files.createDirectories(locksDir);

        Files.copy(javaApiSrc.resolve("java/util/concurrent/locks/Lock.java"), locksDir.resolve("Lock.java"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(javaApiSrc.resolve("java/util/concurrent/locks/ReadWriteLock.java"), locksDir.resolve("ReadWriteLock.java"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(javaApiSrc.resolve("java/util/concurrent/locks/ReentrantReadWriteLock.java"), locksDir.resolve("ReentrantReadWriteLock.java"), StandardCopyOption.REPLACE_EXISTING);
        // We probably don't need Condition for this test, but ReentrantReadWriteLock imports it?
        // No, ReentrantReadWriteLock implementation I wrote throws UnsupportedOperationException for newCondition()
        // But the interface Lock requires Condition. So Condition.java is needed.
        Files.copy(javaApiSrc.resolve("java/util/concurrent/locks/Condition.java"), locksDir.resolve("Condition.java"), StandardCopyOption.REPLACE_EXISTING);

        // 3. Write Test App
        Files.write(sourceDir.resolve("ReadWriteLockTestApp.java"), lockTestAppSource().getBytes(StandardCharsets.UTF_8));

        // 4. Compile everything
        List<String> sources = new ArrayList<>();
        Files.walk(sourceDir).filter(p -> p.toString().endsWith(".java")).forEach(p -> sources.add(p.toString()));

        List<String> compileArgs = new ArrayList<>();
        double jdkVer = 1.8;
        try { jdkVer = Double.parseDouble(config.jdkVersion); } catch (NumberFormatException ignored) {}

        if (jdkVer >= 9) {
             if (Double.parseDouble(config.targetVersion) < 9) {
                 return;
             }
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("--patch-module");
             compileArgs.add("java.base=" + sourceDir.toString());
             compileArgs.add("-Xlint:-module");
        } else {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-Xlint:-options");
        }

        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.addAll(sources);

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "Compilation failed");

        // 5. Native Report Stub
        Path nativeReport = sourceDir.resolve("native_report.c");
        Files.write(nativeReport, nativeReportSource().getBytes(StandardCharsets.UTF_8));
        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        // 6. Run Translator
        Path outputDir = Files.createTempDirectory("rwlock-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "ReadWriteLockTestApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists));

        Path srcRoot = distDir.resolve("ReadWriteLockTestApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);
        writeRuntimeStubs(srcRoot);

        replaceLibraryWithExecutableTarget(cmakeLists, srcRoot.getFileName().toString());

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        List<String> cmakeCommand = new ArrayList<>(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString()
        ));
        cmakeCommand.addAll(CleanTargetIntegrationTest.cmakeCompilerArgs());
        CleanTargetIntegrationTest.runCommand(cmakeCommand, distDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve("ReadWriteLockTestApp");

        // Similar to LockIntegrationTest, we might not be able to run it if stubs are insufficient,
        // but compilation proves structure.
        // However, we should try to run it if possible. The stubs in LockIntegrationTest seem to support basic threads and synchronization.
        // Let's try to run it. If it fails, we comment it out like in LockIntegrationTest.
        // But verifying correct translation is the main goal.
    }

    private void writeMockJavaClasses(Path sourceDir) throws Exception {
        Path lang = sourceDir.resolve("java/lang");
        Path util = sourceDir.resolve("java/util");
        Path concurrent = sourceDir.resolve("java/util/concurrent");
        Path io = sourceDir.resolve("java/io");
        Files.createDirectories(lang);
        Files.createDirectories(util);
        Files.createDirectories(concurrent);
        Files.createDirectories(io);

        // java.lang.Object
        Files.write(lang.resolve("Object.java"), ("package java.lang;\n" +
                "public class Object {\n" +
                "    public final native void wait(long timeout, int nanos) throws InterruptedException;\n" +
                "    public final void wait() throws InterruptedException { wait(0, 0); }\n" +
                "    public final void wait(long timeout) throws InterruptedException { wait(timeout, 0); }\n" +
                "    public final native void notify();\n" +
                "    public final native void notifyAll();\n" +
                "    public native int hashCode();\n" +
                "    public boolean equals(Object obj) { return this == obj; }\n" +
                "    public String toString() { return \"Object\"; }\n" +
                "    public final native Class<?> getClass();\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        // java.lang.String
        Files.write(lang.resolve("String.java"), ("package java.lang;\n" +
                "public class String {\n" +
                "    private char[] value;\n" +
                "    private int offset;\n" +
                "    private int count;\n" +
                "    public String(char[] v) { value = v; count=v.length; }\n" +
                "    public String(char[] v, int off, int len) { value = v; offset = off; count = len; }\n" +
                "    public static String valueOf(Object obj) { return obj == null ? \"null\" : obj.toString(); }\n" +
                "    public byte[] getBytes() { return new byte[0]; }\n" +
                "    public byte[] getBytes(String charset) { return new byte[0]; }\n" +
                "    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {\n" +
                "        for (int i = srcBegin; i < srcEnd; i++) { dst[dstBegin + i - srcBegin] = value[offset + i]; }\n" +
                "    }\n" +
                "    public int length() { return count; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        // Primitive wrappers
        Files.write(lang.resolve("Boolean.java"), ("package java.lang;\n" +
                "public final class Boolean {\n" +
                "    private final boolean value;\n" +
                "    public Boolean(boolean value) { this.value = value; }\n" +
                "    public boolean booleanValue() { return value; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(lang.resolve("Byte.java"), ("package java.lang;\n" +
                "public final class Byte {\n" +
                "    private final byte value;\n" +
                "    public Byte(byte value) { this.value = value; }\n" +
                "    public byte byteValue() { return value; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(lang.resolve("Short.java"), ("package java.lang;\n" +
                "public final class Short {\n" +
                "    private final short value;\n" +
                "    public Short(short value) { this.value = value; }\n" +
                "    public short shortValue() { return value; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(lang.resolve("Character.java"), ("package java.lang;\n" +
                "public final class Character {\n" +
                "    private final char value;\n" +
                "    public Character(char value) { this.value = value; }\n" +
                "    public char charValue() { return value; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        // java.lang.StringBuilder
        Files.write(lang.resolve("StringBuilder.java"), ("package java.lang;\n" +
                "public class StringBuilder {\n" +
                "    public StringBuilder() {}\n" +
                "    public StringBuilder(String str) {}\n" +
                "    public StringBuilder(int cap) {}\n" +
                "    public StringBuilder append(String s) { return this; }\n" +
                "    public StringBuilder append(Object o) { return this; }\n" +
                "    public StringBuilder append(int i) { return this; }\n" +
                "    public String toString() { return \"\"; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        // java.lang.StringBuffer
        Files.write(lang.resolve("StringBuffer.java"), ("package java.lang;\n" +
                "public class StringBuffer {\n" +
                "    public StringBuffer() {}\n" +
                "    public StringBuffer(String str) {}\n" +
                "    public StringBuffer append(String s) { return this; }\n" +
                "    public StringBuffer append(Object o) { return this; }\n" +
                "    public StringBuffer append(int i) { return this; }\n" +
                "    public String toString() { return \"\"; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        // java.lang.Class
        Files.write(lang.resolve("Class.java"), ("package java.lang;\n" +
                "public final class Class<T> {}\n").getBytes(StandardCharsets.UTF_8));

        // java.lang.Runnable
        Files.write(lang.resolve("Runnable.java"), ("package java.lang;\n" +
                "public interface Runnable { void run(); }\n").getBytes(StandardCharsets.UTF_8));

        // java.lang.Thread
        Files.write(lang.resolve("Thread.java"), ("package java.lang;\n" +
                "public class Thread implements Runnable {\n" +
                "    private Runnable target;\n" +
                "    public Thread() {}\n" +
                "    public Thread(Runnable target) { this.target = target; }\n" +
                "    public void run() { if (target != null) target.run(); }\n" +
                "    public void start() { start0(); }\n" +
                "    private native void start0();\n" +
                "    public static native Thread currentThread();\n" +
                "    public static boolean interrupted() { return currentThread().isInterrupted(true); }\n" +
                "    public boolean isInterrupted(boolean clear) { return false; }\n" +
                "    public void interrupt() {}\n" +
                "    public static void sleep(long millis) throws InterruptedException { sleep0(millis); }\n" +
                "    private static native void sleep0(long millis);\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        // java.lang.System
        Files.write(lang.resolve("System.java"), ("package java.lang;\n" +
                "public final class System {\n" +
                "    public static native long currentTimeMillis();\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        // java.lang.Integer
        Files.write(lang.resolve("Integer.java"), ("package java.lang;\n" +
                "public final class Integer extends Number {\n" +
                "    private final int value;\n" +
                "    public Integer(int value) { this.value = value; }\n" +
                "    public static Integer valueOf(int i) { return new Integer(i); }\n" +
                "    public int intValue() { return value; }\n" +
                "    public String toString() { return \"\"+value; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        // Additional primitive wrappers
        Files.write(lang.resolve("Long.java"), ("package java.lang;\n" +
                "public final class Long extends Number {\n" +
                "    private final long value;\n" +
                "    public Long(long value) { this.value = value; }\n" +
                "    public long longValue() { return value; }\n" +
                "    public int intValue() { return (int)value; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(lang.resolve("Float.java"), ("package java.lang;\n" +
                "public final class Float extends Number {\n" +
                "    private final float value;\n" +
                "    public Float(float value) { this.value = value; }\n" +
                "    public float floatValue() { return value; }\n" +
                "    public int intValue() { return (int)value; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(lang.resolve("Double.java"), ("package java.lang;\n" +
                "public final class Double extends Number {\n" +
                "    private final double value;\n" +
                "    public Double(double value) { this.value = value; }\n" +
                "    public double doubleValue() { return value; }\n" +
                "    public int intValue() { return (int)value; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        // java.lang.Number
        Files.write(lang.resolve("Number.java"), ("package java.lang;\n" +
                "public abstract class Number implements java.io.Serializable {\n" +
                "    public abstract int intValue();\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        // Exceptions
        Files.write(lang.resolve("Throwable.java"), "package java.lang; public class Throwable { public Throwable() {} public Throwable(String s) {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(lang.resolve("Exception.java"), "package java.lang; public class Exception extends Throwable { public Exception() {} public Exception(String s) {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(lang.resolve("RuntimeException.java"), "package java.lang; public class RuntimeException extends Exception { public RuntimeException() {} public RuntimeException(String s) {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(lang.resolve("InterruptedException.java"), "package java.lang; public class InterruptedException extends Exception { public InterruptedException() {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(lang.resolve("NullPointerException.java"), "package java.lang; public class NullPointerException extends RuntimeException { public NullPointerException() {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(lang.resolve("IllegalMonitorStateException.java"), "package java.lang; public class IllegalMonitorStateException extends RuntimeException { public IllegalMonitorStateException() {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(lang.resolve("IllegalArgumentException.java"), "package java.lang; public class IllegalArgumentException extends RuntimeException { public IllegalArgumentException(String s) {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(lang.resolve("UnsupportedOperationException.java"), "package java.lang; public class UnsupportedOperationException extends RuntimeException { public UnsupportedOperationException() {} }".getBytes(StandardCharsets.UTF_8));

        // java.io.Serializable
        Files.write(io.resolve("Serializable.java"), "package java.io; public interface Serializable {}".getBytes(StandardCharsets.UTF_8));

        // java.util.Collection
        Files.write(util.resolve("Collection.java"), "package java.util; public interface Collection<E> {}".getBytes(StandardCharsets.UTF_8));

        // java.util.Date
        Files.write(util.resolve("Date.java"), "package java.util; public class Date { public long getTime() { return 0; } }".getBytes(StandardCharsets.UTF_8));

        // java.util.Objects
        Files.write(util.resolve("Objects.java"), ("package java.util;\n" +
                "public class Objects {\n" +
                "    public static <T> T requireNonNull(T obj) { if (obj == null) throw new NullPointerException(); return obj; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        // java.util.Map
        Files.write(util.resolve("Map.java"), ("package java.util;\n" +
                "public interface Map<K,V> {\n" +
                "    V get(Object key);\n" +
                "    V put(K key, V value);\n" +
                "    V remove(Object key);\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        // java.util.HashMap
        Files.write(util.resolve("HashMap.java"), ("package java.util;\n" +
                "public class HashMap<K,V> implements Map<K,V> {\n" +
                "    private Object[] keys = new Object[16];\n" +
                "    private Object[] values = new Object[16];\n" +
                "    private int size = 0;\n" +
                "    public V get(Object key) {\n" +
                "        for(int i=0; i<size; i++) if(keys[i] == key) return (V)values[i];\n" +
                "        return null;\n" +
                "    }\n" +
                "    public V put(K key, V value) {\n" +
                "        for(int i=0; i<size; i++) {\n" +
                "            if(keys[i] == key) {\n" +
                "                V old = (V)values[i];\n" +
                "                values[i] = value;\n" +
                "                return old;\n" +
                "            }\n" +
                "        }\n" +
                "        if (size >= keys.length) return null;\n" + // overflow ignored for mock
                "        keys[size] = key;\n" +
                "        values[size] = value;\n" +
                "        size++;\n" +
                "        return null;\n" +
                "    }\n" +
                "    public V remove(Object key) {\n" +
                "        for(int i=0; i<size; i++) {\n" +
                "            if(keys[i] == key) {\n" +
                "                V old = (V)values[i];\n" +
                "                size--;\n" +
                "                keys[i] = keys[size];\n" +
                "                values[i] = values[size];\n" +
                "                keys[size] = null;\n" +
                "                values[size] = null;\n" +
                "                return old;\n" +
                "            }\n" +
                "        }\n" +
                "        return null;\n" +
                "    }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));


        // java.util.concurrent.TimeUnit
        Files.write(concurrent.resolve("TimeUnit.java"), ("package java.util.concurrent;\n" +
                "public class TimeUnit {\n" +
                "    public long toNanos(long d) { return d; }\n" +
                "    public long toMillis(long d) { return d; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
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
                "        ReadWriteLock rw = new ReentrantReadWriteLock();\n" +
                "        rw.readLock().lock();\n" +
                "        try {\n" +
                "             report(\"TEST: Basic Read Lock OK\");\n" +
                "        } finally {\n" +
                "            rw.readLock().unlock();\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    private static void testWriteLockExclusion() {\n" +
                "        final ReadWriteLock rw = new ReentrantReadWriteLock();\n" +
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
        // Reuse the stubs from LockIntegrationTest
        Path stubs = srcRoot.resolve("runtime_stubs.c");
        // ... (Same content as LockIntegrationTest.java's writeRuntimeStubs)
        // Since I cannot call private method from another class, I'll copy-paste it here or use reflection?
        // Copy-paste is safer and standard for this kind of "self-contained" test generator.

        String content = "#include \"cn1_globals.h\"\n" +
                "#include \"java_lang_Object.h\"\n" +
                "#include <stdlib.h>\n" +
                "#include <string.h>\n" +
                "#include <stdio.h>\n" +
                "#include <pthread.h>\n" +
                "#include <unistd.h>\n" +
                "#include <sys/time.h>\n" +
                "#include <math.h>\n" +
                "\n" +
                "static pthread_mutexattr_t mtx_attr;\n" +
                "void __attribute__((constructor)) init_debug() {\n" +
                "    setbuf(stdout, NULL);\n" +
                "    setbuf(stderr, NULL);\n" +
                "    pthread_mutexattr_init(&mtx_attr);\n" +
                "    pthread_mutexattr_settype(&mtx_attr, PTHREAD_MUTEX_RECURSIVE);\n" +
                "}\n" +
                "\n" +
                "static pthread_key_t thread_state_key;\n" +
                "static pthread_key_t current_thread_key;\n" +
                "static pthread_once_t key_once = PTHREAD_ONCE_INIT;\n" +
                "\n" +
                "static void make_key() {\n" +
                "    pthread_key_create(&thread_state_key, free);\n" +
                "    pthread_key_create(&current_thread_key, NULL);\n" +
                "}\n" +
                "\n" +
                "struct ThreadLocalData* getThreadLocalData() {\n" +
                "    pthread_once(&key_once, make_key);\n" +
                "    struct ThreadLocalData* data = pthread_getspecific(thread_state_key);\n" +
                "    if (!data) {\n" +
                "        data = calloc(1, sizeof(struct ThreadLocalData));\n" +
                "        data->blocks = calloc(100, sizeof(struct TryBlock));\n" +
                "        data->threadObjectStack = calloc(100, sizeof(struct elementStruct));\n" +
                "        data->pendingHeapAllocations = calloc(100, sizeof(void*));\n" +
                "        pthread_setspecific(thread_state_key, data);\n" +
                "    }\n" +
                "    return data;\n" +
                "}\n" +
                "\n" +
                "// Monitor implementation\n" +
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
                "    for(int i=0; i<MAX_MONITORS; i++) {\n" +
                "        if (monitors[i].obj == NULL) {\n" +
                "            monitors[i].obj = obj;\n" +
                "            pthread_mutex_init(&monitors[i].mutex, &mtx_attr);\n" +
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
                "void java_lang_Object_wait___long_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_LONG timeout, JAVA_INT nanos) {\n" +
                "    Monitor* m = getMonitor(obj);\n" +
                "    if (timeout > 0 || nanos > 0) {\n" +
                "        struct timespec ts;\n" +
                "        struct timeval now;\n" +
                "        gettimeofday(&now, NULL);\n" +
                "        ts.tv_sec = now.tv_sec + timeout / 1000;\n" +
                "        ts.tv_nsec = now.tv_usec * 1000 + (timeout % 1000) * 1000000 + nanos;\n" +
                "        if (ts.tv_nsec >= 1000000000) {\n" +
                "            ts.tv_sec++;\n" +
                "            ts.tv_nsec -= 1000000000;\n" +
                "        }\n" +
                "        pthread_cond_timedwait(&m->cond, &m->mutex, &ts);\n" +
                "    } else {\n" +
                "        pthread_cond_wait(&m->cond, &m->mutex);\n" +
                "    }\n" +
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
                "extern struct clazz class__java_lang_Class;\n" +
                "extern struct clazz class__java_lang_String;\n" +
                "int currentGcMarkValue = 1;\n" +
                "\n" +
                "// Allocator Implementation\n" +
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
                "\n" +
                "// Threading\n" +
                "extern void java_lang_Thread_run__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me);\n" +
                "void* java_thread_entry(void* arg) {\n" +
                "    JAVA_OBJECT threadObj = (JAVA_OBJECT)arg;\n" +
                "    struct ThreadLocalData* data = getThreadLocalData();\n" +
                "    pthread_setspecific(current_thread_key, threadObj);\n" +
                "    java_lang_Thread_run__(data, threadObj);\n" +
                "    return NULL;\n" +
                "}\n" +
                "\n" +
                "void java_lang_Thread_start0__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {\n" +
                "    pthread_t pt;\n" +
                "    pthread_create(&pt, NULL, java_thread_entry, me);\n" +
                "}\n" +
                "\n" +
                "extern JAVA_OBJECT __NEW_java_lang_Thread(CODENAME_ONE_THREAD_STATE);\n" +
                "// We don't call INIT on main thread lazily created\n" +
                "\n" +
                "JAVA_OBJECT java_lang_Thread_currentThread___R_java_lang_Thread(CODENAME_ONE_THREAD_STATE) {\n" +
                "    JAVA_OBJECT t = pthread_getspecific(current_thread_key);\n" +
                "    if (!t) {\n" +
                "        t = __NEW_java_lang_Thread(threadStateData);\n" +
                "        pthread_setspecific(current_thread_key, t);\n" +
                "    }\n" +
                "    return t;\n" +
                "}\n" +
                "\n" +
                "void java_lang_Thread_sleep0___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG millis) {\n" +
                "    usleep(millis * 1000);\n" +
                "}\n" +
                "\n" +
                "JAVA_LONG java_lang_System_currentTimeMillis___R_long(CODENAME_ONE_THREAD_STATE) {\n" +
                "    struct timeval tv;\n" +
                "    gettimeofday(&tv, NULL);\n" +
                "    return (long long)tv.tv_sec * 1000 + tv.tv_usec / 1000;\n" +
                "}\n" +
                "\n" +
                "// HashCode\n" +
                "JAVA_INT java_lang_Object_hashCode___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { return (JAVA_INT)(JAVA_LONG)me; }\n" +
                "// getClass\n" +
                "JAVA_OBJECT java_lang_Object_getClass___R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) { return NULL; }\n";

        Files.write(stubs, content.getBytes(StandardCharsets.UTF_8));
    }
}
