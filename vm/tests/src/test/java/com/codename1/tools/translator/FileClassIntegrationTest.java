package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileClassIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    public void testFileClassMethods(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("file-test-sources");
        Path classesDir = Files.createTempDirectory("file-test-classes");
        Path javaFile = sourceDir.resolve("FileTestApp.java");
        Files.createDirectories(sourceDir.resolve("java/lang"));
        Files.createDirectories(sourceDir.resolve("java/io"));
        Files.createDirectories(sourceDir.resolve("java/util"));
        Files.createDirectories(sourceDir.resolve("java/net"));

        Files.write(javaFile, fileTestAppSource().getBytes(StandardCharsets.UTF_8));

        // Copy java.io.File and dependencies
        Path javaIoFileSrc = Paths.get("../JavaAPI/src/java/io/File.java");
        if (!Files.exists(javaIoFileSrc)) {
            // Fallback if running from root
            javaIoFileSrc = Paths.get("vm/JavaAPI/src/java/io/File.java");
        }
        Files.write(sourceDir.resolve("java/io/File.java"), Files.readAllBytes(javaIoFileSrc));

        Files.write(sourceDir.resolve("java/lang/Object.java"), ("package java.lang;\n" +
                "public class Object {\n" +
                "    public String toString() { return \"Object\"; }\n" +
                "    public boolean equals(Object o) { return this == o; }\n" +
                "    public int hashCode() { return 0; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        // Better String stub
        String stringSource = "package java.lang;\n" +
                "public class String extends Object {\n" +
                "   private char[] value;\n" +
                "   private int offset;\n" +
                "   private int count;\n" +
                "   public String(char[] value, int offset, int count) { this.value = value; this.offset = offset; this.count = count; }\n" +
                "   public native boolean equals(Object o);\n" +
                "   public boolean endsWith(String s) { return startsWith(s, length() - s.length()); }\n" +
                "   public boolean startsWith(String s) { return startsWith(s, 0); }\n" +
                "   public boolean startsWith(String s, int toffset) {\n" +
                "       int to = offset + toffset;\n" +
                "       int po = s.offset;\n" +
                "       int pc = s.count;\n" +
                "       if ((toffset < 0) || (toffset > count - pc)) { return false; }\n" +
                "       while (--pc >= 0) {\n" +
                "           if (value[to++] != s.value[po++]) { return false; }\n" +
                "       }\n" +
                "       return true;\n" +
                "   }\n" +
                "   public native int length();\n" +
                "   public native int lastIndexOf(int ch);\n" +
                "   public native int indexOf(int ch, int fromIndex);\n" +
                "   public String substring(int beginIndex) { return substring(beginIndex, count); }\n" +
                "   public String substring(int beginIndex, int endIndex) { return new String(value, offset + beginIndex, endIndex - beginIndex); }\n" +
                "   public int compareTo(String anotherString) {\n" +
                "       int len1 = count;\n" +
                "       int len2 = anotherString.count;\n" +
                "       int n = Math.min(len1, len2);\n" +
                "       char v1[] = value;\n" +
                "       char v2[] = anotherString.value;\n" +
                "       int i = offset;\n" +
                "       int j = anotherString.offset;\n" +
                "       if (i == j) {\n" +
                "           int k = i;\n" +
                "           int lim = n + i;\n" +
                "           while (k < lim) {\n" +
                "               char c1 = v1[k];\n" +
                "               char c2 = v2[k];\n" +
                "               if (c1 != c2) { return c1 - c2; }\n" +
                "               k++;\n" +
                "           }\n" +
                "       } else {\n" +
                "           while (n-- != 0) {\n" +
                "               char c1 = v1[i++];\n" +
                "               char c2 = v2[j++];\n" +
                "               if (c1 != c2) { return c1 - c2; }\n" +
                "           }\n" +
                "       }\n" +
                "       return len1 - len2;\n" +
                "   }\n" +
                "   public native int hashCode();\n" +
                "   public native char[] toCharArray();\n" +
                "   public static String valueOf(Object o) { return o == null ? \"null\" : o.toString(); }\n" +
                "}\n";
        Files.write(sourceDir.resolve("java/lang/String.java"), stringSource.getBytes(StandardCharsets.UTF_8));

        Files.write(sourceDir.resolve("java/lang/StringBuilder.java"), ("package java.lang;\n" +
                "public class StringBuilder {\n" +
                "    char[] value;\n" +
                "    int count;\n" +
                "    public StringBuilder() { value = new char[16]; }\n" +
                "    public native StringBuilder append(String s);\n" +
                "    public StringBuilder append(boolean b) { return append(b ? \"true\" : \"false\"); }\n" +
                "    public String toString() { return new String(value, 0, count); }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        Files.write(sourceDir.resolve("java/lang/Math.java"), ("package java.lang;\n" +
                "public class Math {\n" +
                "    public static int min(int a, int b) { return a <= b ? a : b; }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        Files.write(sourceDir.resolve("java/lang/Class.java"), CleanTargetIntegrationTest.javaLangClassSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Throwable.java"), "package java.lang; public class Throwable extends Object { public void printStackTrace() {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Exception.java"), CleanTargetIntegrationTest.javaLangExceptionSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/RuntimeException.java"), CleanTargetIntegrationTest.javaLangRuntimeExceptionSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/UnsupportedOperationException.java"), "package java.lang; public class UnsupportedOperationException extends RuntimeException { public UnsupportedOperationException() {} public UnsupportedOperationException(String s) {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/NullPointerException.java"), CleanTargetIntegrationTest.javaLangNullPointerExceptionSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Long.java"), CleanTargetIntegrationTest.javaLangLongSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Number.java"), CleanTargetIntegrationTest.javaLangNumberSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Boolean.java"), CleanTargetIntegrationTest.javaLangBooleanSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/io/Serializable.java"), CleanTargetIntegrationTest.javaIoSerializableSource().getBytes(StandardCharsets.UTF_8));

        // Add stub for IOException, URI, URL as they are used in File.java
        Files.write(sourceDir.resolve("java/io/IOException.java"), "package java.io; public class IOException extends Exception { public IOException(String s) {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/net/URI.java"), "package java.net; public class URI {}".getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/net/URL.java"), "package java.net; public class URL { public URL(String p, String h, String f) throws MalformedURLException {} }".getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/net/MalformedURLException.java"), "package java.net; import java.io.IOException; public class MalformedURLException extends IOException { public MalformedURLException(String s) { super(s); } }".getBytes(StandardCharsets.UTF_8));

        Files.write(sourceDir.resolve("java/io/PrintStream.java"), ("package java.io; public class PrintStream { \n" +
                "    public void println(String s) { printNative(s); }\n" +
                "    private native void printNative(String s);\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/System.java"), "package java.lang; import java.io.PrintStream; public class System { public static final PrintStream out = new PrintStream(); public static String getProperty(String key) { return null; } public static long currentTimeMillis() { return 0; } }".getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/IllegalArgumentException.java"), "package java.lang; public class IllegalArgumentException extends RuntimeException { public IllegalArgumentException(String s) {} }".getBytes(StandardCharsets.UTF_8));

        Files.write(sourceDir.resolve("native_test.c"), ("#include \"cn1_globals.h\"\n" +
                "#include \"java_lang_String.h\"\n" +
                "#include <stdio.h>\n" +
                "#include <sys/stat.h>\n" +
                "#include <unistd.h>\n" +
                "#include <dirent.h>\n" +
                "#include <string.h>\n" +
                "#include <limits.h>\n" +
                "\n" +
                "const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str) {\n" +
                "    static char buf[4096];\n" +
                "    if (str == JAVA_NULL) return NULL;\n" +
                "    JAVA_ARRAY_CHAR* chars = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)get_field_java_lang_String_value(str))->data;\n" +
                "    int off = get_field_java_lang_String_offset(str);\n" +
                "    int len = get_field_java_lang_String_count(str);\n" +
                "    for(int i=0; i<len && i<4095; i++) buf[i] = (char)chars[off+i];\n" +
                "    buf[len] = 0;\n" +
                "    return buf;\n" +
                "}\n" +
                "\n" +
                "JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char *str) {\n" +
                "    if (str == NULL) return JAVA_NULL;\n" +
                "    int len = strlen(str);\n" +
                "    JAVA_OBJECT charArray = allocArray(threadStateData, len, &class_array1__JAVA_CHAR, sizeof(JAVA_CHAR), 1);\n" +
                "    JAVA_ARRAY_CHAR* chars = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)charArray)->data;\n" +
                "    for(int i=0; i<len; i++) chars[i] = (JAVA_CHAR)str[i];\n" +
                "    JAVA_OBJECT s = codenameOneGcMalloc(threadStateData, sizeof(struct obj__java_lang_String), &class__java_lang_String);\n" +
                "    set_field_java_lang_String_value(threadStateData, charArray, s);\n" +
                "    set_field_java_lang_String_count(threadStateData, len, s);\n" +
                "    set_field_java_lang_String_offset(threadStateData, 0, s);\n" +
                "    return s;\n" +
                "}\n" +
                "\n" +
                "JAVA_VOID java_io_PrintStream_printNative___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT this, JAVA_OBJECT s) {\n" +
                "    if (s == JAVA_NULL) { printf(\"null\\n\"); return; }\n" +
                "    printf(\"%s\\n\", stringToUTF8(threadStateData, s));\n" +
                "}\n" +
                "JAVA_INT java_lang_String_length___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
                "    return get_field_java_lang_String_count(__cn1ThisObject);\n" +
                "}\n" +
                "JAVA_INT java_lang_String_hashCode___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
                "    return 0;\n" +
                "}\n" +
                "JAVA_BOOLEAN java_lang_String_equals___java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1) {\n" +
                "    if (__cn1ThisObject == __cn1Arg1) return JAVA_TRUE;\n" +
                "    if (__cn1Arg1 == JAVA_NULL) return JAVA_FALSE;\n" +
                "    const char* s1 = stringToUTF8(threadStateData, __cn1ThisObject);\n" +
                "    const char* s2 = stringToUTF8(threadStateData, __cn1Arg1);\n" +
                "    return strcmp(s1, s2) == 0 ? JAVA_TRUE : JAVA_FALSE;\n" +
                "}\n" +
                "JAVA_OBJECT java_lang_String_toCharArray___R_char_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
                "    return get_field_java_lang_String_value(__cn1ThisObject); \n" +
                "}\n" +
                "JAVA_INT java_lang_String_lastIndexOf___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT ch) {\n" +
                "    const char* s = stringToUTF8(threadStateData, __cn1ThisObject);\n" +
                "    char* ptr = strrchr(s, ch);\n" +
                "    if (ptr == NULL) return -1;\n" +
                "    return (JAVA_INT)(ptr - s);\n" +
                "}\n" +
                "JAVA_INT java_lang_String_indexOf___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT ch, JAVA_INT from) {\n" +
                "    const char* s = stringToUTF8(threadStateData, __cn1ThisObject);\n" +
                "    int len = strlen(s);\n" +
                "    if (from >= len) return -1;\n" +
                "    char* ptr = strchr(s + from, ch);\n" +
                "    if (ptr == NULL) return -1;\n" +
                "    return (JAVA_INT)(ptr - s);\n" +
                "}\n" +
                "// File implementations POSIX\n" +
                "JAVA_BOOLEAN java_io_File_existsImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return JAVA_FALSE;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    return access(p, F_OK) != -1;\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_isDirectoryImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return JAVA_FALSE;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    struct stat s;\n" +
                "    if (stat(p, &s) == 0) {\n" +
                "        return S_ISDIR(s.st_mode);\n" +
                "    }\n" +
                "    return JAVA_FALSE;\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_isFileImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return JAVA_FALSE;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    struct stat s;\n" +
                "    if (stat(p, &s) == 0) {\n" +
                "        return S_ISREG(s.st_mode);\n" +
                "    }\n" +
                "    return JAVA_FALSE;\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_isHiddenImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return JAVA_FALSE;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    if (p[0] == '.') return JAVA_TRUE;\n" + // Simplistic check
                "    return JAVA_FALSE;\n" +
                "}\n" +
                "\n" +
                "JAVA_LONG java_io_File_lastModifiedImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return 0;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    struct stat s;\n" +
                "    if (stat(p, &s) == 0) {\n" +
                "        return (JAVA_LONG)s.st_mtime * 1000;\n" +
                "    }\n" +
                "    return 0;\n" +
                "}\n" +
                "\n" +
                "JAVA_LONG java_io_File_lengthImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return 0;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    struct stat s;\n" +
                "    if (stat(p, &s) == 0) {\n" +
                "        return (JAVA_LONG)s.st_size;\n" +
                "    }\n" +
                "    return 0;\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_createNewFileImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return JAVA_FALSE;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    if (access(p, F_OK) != -1) return JAVA_FALSE;\n" +
                "    FILE* f = fopen(p, \"w\");\n" +
                "    if (f) {\n" +
                "        fclose(f);\n" +
                "        return JAVA_TRUE;\n" +
                "    }\n" +
                "    return JAVA_FALSE;\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_deleteImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return JAVA_FALSE;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    if (remove(p) == 0) return JAVA_TRUE;\n" +
                "    return JAVA_FALSE;\n" +
                "}\n" +
                "\n" +
                "JAVA_OBJECT java_io_File_listImpl___java_lang_String_R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    return JAVA_NULL; // Not used in test yet\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_mkdirImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return JAVA_FALSE;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    if (mkdir(p, 0755) == 0) return JAVA_TRUE;\n" +
                "    return JAVA_FALSE;\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_renameToImpl___java_lang_String_java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path, JAVA_OBJECT dest) {\n" +
                "    if(path == JAVA_NULL || dest == JAVA_NULL) return JAVA_FALSE;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    const char* d = stringToUTF8(threadStateData, dest);\n" +
                "    if (rename(p, d) == 0) return JAVA_TRUE;\n" +
                "    return JAVA_FALSE;\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_setReadOnlyImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    return JAVA_FALSE;\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_setWritableImpl___java_lang_String_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path, JAVA_BOOLEAN writable) {\n" +
                "    return JAVA_FALSE;\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_setReadableImpl___java_lang_String_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path, JAVA_BOOLEAN readable) {\n" +
                "    return JAVA_FALSE;\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_setExecutableImpl___java_lang_String_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path, JAVA_BOOLEAN executable) {\n" +
                "    return JAVA_FALSE;\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_canReadImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return JAVA_FALSE;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    return access(p, R_OK) != -1;\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_canWriteImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return JAVA_FALSE;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    return access(p, W_OK) != -1;\n" +
                "}\n" +
                "\n" +
                "JAVA_BOOLEAN java_io_File_canExecuteImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return JAVA_FALSE;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    return access(p, X_OK) != -1;\n" +
                "}\n" +
                "\n" +
                "JAVA_LONG java_io_File_getTotalSpaceImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    return 0;\n" +
                "}\n" +
                "\n" +
                "JAVA_LONG java_io_File_getFreeSpaceImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    return 0;\n" +
                "}\n" +
                "\n" +
                "JAVA_LONG java_io_File_getUsableSpaceImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    return 0;\n" +
                "}\n" +
                "\n" +
                "JAVA_OBJECT java_io_File_getAbsolutePathImpl___java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return JAVA_NULL;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    if (p[0] == '/') return path;\n" +
                "    char buf[PATH_MAX];\n" +
                "    if (getcwd(buf, sizeof(buf)) != NULL) {\n" +
                "        strcat(buf, \"/\");\n" +
                "        strcat(buf, p);\n" +
                "        return newStringFromCString(threadStateData, buf);\n" +
                "    }\n" +
                "    return path;\n" +
                "}\n" +
                "\n" +
                "JAVA_OBJECT java_io_File_getCanonicalPathImpl___java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {\n" +
                "    if(path == JAVA_NULL) return JAVA_NULL;\n" +
                "    const char* p = stringToUTF8(threadStateData, path);\n" +
                "    char buf[PATH_MAX];\n" +
                "    if (realpath(p, buf) != NULL) {\n" +
                "        return newStringFromCString(threadStateData, buf);\n" +
                "    }\n" +
                "    // Fallback\n" +
                "    return java_io_File_getAbsolutePathImpl___java_lang_String_R_java_lang_String(threadStateData, __cn1ThisObject, path);\n" +
                "}\n"
                ).getBytes(StandardCharsets.UTF_8));

        // Add native_test.m to classesDir so it gets compiled
        // CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget globs .m files
        // But ByteCodeTranslator doesn't copy it?
        // CleanTargetIntegrationTest copies native_hello.c.
        // We need to copy native_test.m to classesDir.

        // Add stub for IOException, URI, URL as they are used in File.java
        Files.write(sourceDir.resolve("java/util/HashMap.java"), "package java.util; public class HashMap { public static class Entry { } }".getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/util/Date.java"), "package java.util; public class Date { }".getBytes(StandardCharsets.UTF_8));

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
        // Add all source files
        Files.walk(sourceDir).filter(p -> p.toString().endsWith(".java")).forEach(p -> compileArgs.add(p.toString()));

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "FileTestApp.java compilation failed with " + config);

        Files.copy(sourceDir.resolve("native_test.c"), classesDir.resolve("native_test.c"));

        Path outputDir = Files.createTempDirectory("file-test-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "FileTestApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        Path srcRoot = distDir.resolve("FileTestApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);

        // Manually copy native_test.c to output because ByteCodeTranslator might skip it
        Files.copy(classesDir.resolve("native_test.c"), srcRoot.resolve("native_test.c"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Remove nativeMethods.m and cn1_globals.m as they introduce dependencies we don't have
        Files.deleteIfExists(srcRoot.resolve("nativeMethods.m"));
        Files.deleteIfExists(srcRoot.resolve("cn1_globals.m"));

        writeRuntimeStubs(srcRoot);

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

        Path executable = buildDir.resolve("FileTestApp");
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);

        // Verify output
        assertTrue(output.contains("File created: true"), "File creation failed.\nOutput:\n" + output);
        assertTrue(output.contains("File exists: true"), "File exists check failed.\nOutput:\n" + output);
        assertTrue(output.contains("File is directory: false"), "File isDirectory check failed.\nOutput:\n" + output);
        assertTrue(output.contains("File absolute path ends with testfile.txt: true"), "File getAbsolutePath check failed.\nOutput:\n" + output);
        assertTrue(output.contains("File deleted: true"), "File delete failed.\nOutput:\n" + output);
    }

    private String fileTestAppSource() {
        return "import java.io.File;\n" +
               "public class FileTestApp {\n" +
               "    public static void main(String[] args) {\n" +
               "        try {\n" +
               "            File f = new File(\"testfile.txt\");\n" +
               "            if (f.exists()) {\n" +
               "                f.delete();\n" +
               "            }\n" +
               "            boolean created = f.createNewFile();\n" +
               "            System.out.println(created ? \"File created: true\" : \"File created: false\");\n" +
               "            System.out.println(f.exists() ? \"File exists: true\" : \"File exists: false\");\n" +
               "            System.out.println(f.isDirectory() ? \"File is directory: true\" : \"File is directory: false\");\n" +
               "            String absPath = f.getAbsolutePath();\n" +
               "            // Simple check since path might vary\n" +
               "            boolean absPathCorrect = absPath.endsWith(\"testfile.txt\") && absPath.length() > \"testfile.txt\".length();\n" +
               "            System.out.println(absPathCorrect ? \"File absolute path ends with testfile.txt: true\" : \"File absolute path ends with testfile.txt: false\");\n" +
               "            boolean deleted = f.delete();\n" +
               "            System.out.println(deleted ? \"File deleted: true\" : \"File deleted: false\");\n" +
               "        } catch (Exception e) {\n" +
               "            e.printStackTrace();\n" +
               "        }\n" +
               "    }\n" +
               "}";
    }

    private void replaceLibraryWithExecutableTarget(Path cmakeLists, String sourceDirName) throws IOException {
        String content = new String(Files.readAllBytes(cmakeLists), StandardCharsets.UTF_8);
        String replacement = content.replace(
                "add_library(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})",
                "add_executable(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})\ntarget_link_libraries(${PROJECT_NAME} m)"
        );
        Files.write(cmakeLists, replacement.getBytes(StandardCharsets.UTF_8));
    }

    private void writeRuntimeStubs(Path srcRoot) throws IOException {
        Path objectHeader = srcRoot.resolve("java_lang_Object.h");
        if (!Files.exists(objectHeader)) {
            String headerContent = "#ifndef __JAVA_LANG_OBJECT_H__\n" +
                    "#define __JAVA_LANG_OBJECT_H__\n" +
                    "#include \"cn1_globals.h\"\n" +
                    "#endif\n";
            Files.write(objectHeader, headerContent.getBytes(StandardCharsets.UTF_8));
        }

        Path stubs = srcRoot.resolve("runtime_stubs.c");
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
                "extern const char * const constantPool[];\n" +
                "extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char *str);\n" +
                "\n" +
                "void initConstantPool() {\n" +
                "    if (constantPoolObjects == NULL) {\n" +
                "        struct ThreadLocalData* threadStateData = getThreadLocalData();\n" +
                "        constantPoolObjects = calloc(CN1_CONSTANT_POOL_SIZE, sizeof(JAVA_OBJECT));\n" +
                "        for (int i=0; i<CN1_CONSTANT_POOL_SIZE; i++) {\n" +
                "            constantPoolObjects[i] = newStringFromCString(threadStateData, constantPool[i]);\n" +
                "        }\n" +
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
                "void gcMarkObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) { (void)threadStateData; (void)obj; (void)force; }\n" +
                "int instanceofFunction(int sourceClass, int destId) { return 1; }\n" +
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
                "int currentGcMarkValue = 1;\n" +
                "int recursionKey = 1;\n";

        Files.write(stubs, content.getBytes(StandardCharsets.UTF_8));
    }
}
