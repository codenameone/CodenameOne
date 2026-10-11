/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maven;

import com.codename1.build.BuildExecutionException;
import com.codename1.maven.annotations.JavaSourceCompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class BackendJavaLevelTest {
    @Test void defaultApplicationAndRuntimeRemainJava8(@TempDir Path dir) throws Exception {
        File jdk = jdk("8");
        File api = api(dir);
        Path runtime = Files.createDirectory(dir.resolve("runtime"));
        Files.write(runtime.resolve("Library.java"),
                "public class Library { public static int answer() { return 42; } }".getBytes("UTF-8"));
        Path source = Files.createDirectory(dir.resolve("src"));
        Files.write(source.resolve("Legacy.java"), ("public class Legacy {"
                + " public int answer() { return Library.answer(); } }").getBytes("UTF-8"));
        File classes = Files.createDirectory(dir.resolve("classes")).toFile();
        new BackendPackager(host(dir, source)).compile(jdk, api, runtime.toFile(), classes);
        assertEquals(52, major(classes.toPath().resolve("Library.class")));
        assertEquals(52, major(classes.toPath().resolve("Legacy.class")));
        try (java.net.URLClassLoader loader = new java.net.URLClassLoader(
                new java.net.URL[]{classes.toURI().toURL()}, null)) {
            Class<?> legacy = loader.loadClass("Legacy");
            assertEquals(42, legacy.getMethod("answer").invoke(legacy.newInstance()));
        }
    }

    @Test void java17ApplicationKeepsJava8Runtime(@TempDir Path dir) throws Exception {
        compileRecord(dir, "17");
    }

    @Test void java25ApplicationKeepsJava8Runtime(@TempDir Path dir) throws Exception {
        compileRecord(dir, "25");
    }

    @Test void modernSourcesIgnoreLegacyJdkOverride(@TempDir Path dir) throws Exception {
        File oldJdk = Files.createDirectories(dir.resolve("legacy/bin")).getParent().toFile();
        Files.write(new File(oldJdk, "bin/javac").toPath(), new byte[0]);
        BackendPackager baseline = new BackendPackager(TestProjectHost.empty());
        File running = baseline.resolveJdk();
        BackendTestPackager packager = new BackendTestPackager(TestProjectHost.empty()) {
            protected java.util.List<String> testSourceRoots() { return Collections.emptyList(); }
            protected File testOutputDirectory() { return null; }
        };
        packager.jdk(null, oldJdk.toString());
        packager.javaLevel(null, null, "17");
        assertEquals(running, packager.resolveJdk());
        packager.javaLevel(null, null, "8");
        packager.testJavaLevel(null, null, "25");
        assertEquals(running, packager.resolveJdk());
        packager.jdk(oldJdk.toString(), null);
        assertEquals(oldJdk, packager.resolveJdk(), "an explicit JDK choice still wins");
    }

    @Test void previewSourcesAndJava8Wiring(@TempDir Path dir) throws Exception {
        File jdk = jdk("25");
        Path source = Files.createDirectories(dir.resolve("src"));
        Files.write(source.resolve("Preview.java"), ("public class Preview {"
                + " public static int value(int input) { if(input instanceof int result) return result; return 0; } }").getBytes("UTF-8"));
        BackendPackager packager = new BackendPackager(host(dir, source));
        packager.javaLevel(null, null, "25").compilerOptions(Collections.singletonList("--enable-preview"));
        File classes = Files.createDirectory(dir.resolve("classes")).toFile();
        Path runtime = Files.createDirectory(dir.resolve("runtime"));
        Files.write(runtime.resolve("Library.java"), "public class Library {}".getBytes("UTF-8"));
        packager.compile(jdk, api(dir), runtime.toFile(), classes);
        assertEquals(52, major(classes.toPath().resolve("Library.class")));
        assertEquals(69, major(classes.toPath().resolve("Preview.class")));
        try (JavaSourceCompiler.CompilerScope ignored = JavaSourceCompiler.useJdk(jdk)) {
            JavaSourceCompiler.compile(Collections.singletonMap("Wiring",
                    "public class Wiring { public static int value() {return Preview.value(42);} }"),
                    classes, Collections.singletonList(classes));
        }
        assertEquals(52, major(classes.toPath().resolve("Wiring.class")));
    }

    @Test void discoversClasspathProcessorsOnModernJavac(@TempDir Path dir) throws Exception {
        File jdk = jdk("25");
        Path processor = dir.resolve("Generate.java");
        Files.write(processor, ("import javax.annotation.processing.*; import javax.lang.model.element.*;"
                + " @SupportedAnnotationTypes(\"*\") @SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_8)"
                + " public class Generate extends AbstractProcessor { boolean done;"
                + " public boolean process(java.util.Set<? extends TypeElement> a, RoundEnvironment r) {"
                + " if (!done) {done=true; try { java.io.Writer w=processingEnv.getFiler().createSourceFile(\"Generated\").openWriter();"
                + " w.write(\"public class Generated { public static int value() {return 42;} }\"); w.close();"
                + " } catch(java.io.IOException ex) {throw new RuntimeException(ex);} } return false; } }").getBytes("UTF-8"));
        Path processors = Files.createDirectory(dir.resolve("processors"));
        run(jdk, "javac", "-proc:none", "-d", processors.toString(), processor.toString());
        Path service = processors.resolve("META-INF/services/javax.annotation.processing.Processor");
        Files.createDirectories(service.getParent());
        Files.write(service, "Generate".getBytes("UTF-8"));
        Path source = dir.resolve("UsesGenerated.java");
        Files.write(source, "public class UsesGenerated { int value() {return Generated.value();} }".getBytes("UTF-8"));
        BackendPackager packager = new BackendPackager(host(dir, dir));
        File output = Files.createDirectory(dir.resolve("out")).toFile();
        File nativeApi = api(dir);
        packager.compileSources(jdk, nativeApi, output, Collections.singletonList(source.toString()),
                Collections.singletonList(processors.toString()), "25", "25", "25", "compile with processor");
        assertTrue(new File(output, "Generated.class").isFile());
        File disabled = Files.createDirectory(dir.resolve("disabled")).toFile();
        assertThrows(BuildExecutionException.class, () -> packager.compileSources(jdk, nativeApi, disabled,
                Collections.singletonList(source.toString()), Collections.singletonList(processors.toString()),
                "25", "25", "25", "compile without processor", Collections.singletonList("-proc:none")));
    }

    private void compileRecord(Path dir, String release) throws Exception {
        File jdk = jdk(release);
        File api = api(dir);
        Path runtime = Files.createDirectories(dir.resolve("runtime"));
        Files.write(runtime.resolve("Library.java"),
                "public class Library { public static int answer() { return 42; } }".getBytes("UTF-8"));
        Path source = Files.createDirectories(dir.resolve("src"));
        Files.write(source.resolve("Message.java"), ("public record Message(int value, String text) {"
                + " public static void main(String[] args) {"
                + " Object object = new Message(Library.answer(), null);"
                + " int value = object instanceof Message m ? m.value() : 0;"
                + " int result = switch(value) { case 42 -> 1; default -> 0; };"
                + " Message m = (Message)object;"
                + " if(result != 1 || !m.equals(new Message(42, null)) || m.equals(null)"
                + " || m.equals(new Message(43, null)) || m.hashCode() != 1302"
                + " || !m.toString().equals(\"Message[value=42, text=null]\")"
                + " || Generated.value(m) != 42) throw new AssertionError();"
                + " System.out.println(\"MODERN_BACKEND_OK\"); } }").getBytes("UTF-8"));
        // The generated class is added after compilation, as controller wiring is.
        Files.write(source.resolve("Generated.java"),
                "public class Generated { public static int value(Message m) {return m.value();} }".getBytes("UTF-8"));
        TestProjectHost host = host(dir, source);
        BackendPackager packager = new BackendPackager(host);
        packager.javaLevel(null, null, release);
        File classes = Files.createDirectories(dir.resolve("classes")).toFile();
        packager.compile(jdk, api, runtime.toFile(), classes);
        assertEquals(52, major(classes.toPath().resolve("Library.class")));
        assertEquals(Integer.parseInt(release) + 44, major(classes.toPath().resolve("Message.class")));
        try (JavaSourceCompiler.CompilerScope ignored = JavaSourceCompiler.useJdk(jdk)) {
            JavaSourceCompiler.compile(Collections.singletonMap("Generated",
                    "public class Generated { public static int value(Message m) { return m.value(); } }"),
                    classes, Collections.singletonList(classes));
        }
        assertEquals(52, major(classes.toPath().resolve("Generated.class")));
        String output = run(jdk, "java", "-cp", classes.getAbsolutePath(), "Message");
        assertTrue(output.contains("MODERN_BACKEND_OK"), output);
    }

    @Test void checksModernApplicationTypesAndMethodReferences(@TempDir Path dir) throws Exception {
        File jdk = jdk("17");
        File api = api(dir);
        TestProjectHost host = host(dir, Files.createDirectories(dir.resolve("src")));
        BackendPackager packager = new BackendPackager(host);
        for (String body : Arrays.asList(
                "java.nio.file.Path path;",
                "Object run() {return java.nio.file.Paths.get(\"x\");}",
                "java.util.function.Supplier<Object> run() {return java.nio.file.Paths::getDefault;}",
                "Object run() {return java.nio.file.Path.class;}")) {
            // A real method reference to an absent native API, not a javac error.
            body = body.replace("java.nio.file.Paths::getDefault", "java.nio.file.FileSystems::getDefault");
            Path source = dir.resolve("src/Forbidden.java");
            Files.write(source, ("class Forbidden { " + body + " }").getBytes("UTF-8"));
            File classes = Files.createDirectories(dir.resolve("checked-" + Math.abs(body.hashCode()))).toFile();
            packager.compileSources(jdk, api, classes, Collections.singletonList(source.toString()),
                    Collections.<String>emptyList(), "17", "17", null, "compile fixture");
            BuildExecutionException error = assertThrows(BuildExecutionException.class,
                    () -> packager.checkApplication(classes, api, Collections.<String>emptyList()));
            assertTrue(error.getMessage().contains("java/nio/file"), error.getMessage());
        }
    }

    @Test void generatedRecordMethodsMatchJavac(@TempDir Path dir) throws Exception {
        File jdk = jdk("17");
        Path source = dir.resolve("RecordSemantics.java");
        Files.write(source, ("public class RecordSemantics {"
                + " record All(boolean z, byte b, char c, short s, int i, long l, float f, double d, String text) {}"
                + " record Empty() {} record Array(int[] values) {}"
                + " record Custom(int x) { public boolean equals(Object other) { return false; }"
                + " public int hashCode() {return 99;} public String toString() {return \"custom\";} }"
                + " public static void main(String[] args) {"
                + " All a = new All(true, (byte)2, 'x', (short)3, 4, 5L, Float.NaN, -0.0d, null);"
                + " All b = new All(true, (byte)2, 'x', (short)3, 4, 5L, Float.NaN, -0.0d, null);"
                + " All c = new All(true, (byte)2, 'x', (short)3, 4, 5L, Float.NaN, 0.0d, null);"
                + " System.out.println(a); System.out.println(a.hashCode());"
                + " System.out.println(a.equals(b)); System.out.println(a.equals(c));"
                + " System.out.println(a.equals(null)); System.out.println(a.equals(\"wrong\"));"
                + " System.out.println(new Empty()); System.out.println(new Empty().hashCode());"
                + " System.out.println(new Empty().equals(new Empty()));"
                + " int[] array = {1}; System.out.println(new Array(array).equals(new Array(array)));"
                + " System.out.println(new Array(array).equals(new Array(new int[]{1})));"
                + " Custom custom = new Custom(1); System.out.println(custom);"
                + " System.out.println(custom.hashCode()); System.out.println(custom.equals(custom));"
                + " } }").getBytes("UTF-8"));
        File output = Files.createDirectory(dir.resolve("out")).toFile();
        run(jdk, "javac", "--release", "17", "-d", output.toString(), source.toString());
        String expected = run(jdk, "java", "-cp", output.toString(), "RecordSemantics");
        BackendRecordMethods.rewrite(output);
        assertEquals(expected, run(jdk, "java", "-cp", output.toString(), "RecordSemantics"));
    }

    @Test void floatingRecordsCanonicalizeNaNPayloadsAndPreserveSignedZero(@TempDir Path dir) throws Exception {
        File jdk = jdk("17");
        Path source = dir.resolve("FloatingRecords.java");
        try (InputStream in = getClass().getResourceAsStream("/backend/FloatingRecords.java")) {
            Files.copy(in, source);
        }
        Path runner = dir.resolve("RunFloatingRecords.java");
        Files.write(runner, ("public class RunFloatingRecords { public static void main(String[] args) {"
                + " example.FloatingRecords.verify(); System.out.println(\"FLOATING_RECORDS_OK\"); } }").getBytes("UTF-8"));
        File output = Files.createDirectory(dir.resolve("out")).toFile();
        run(jdk, "javac", "--release", "17", "-d", output.toString(), source.toString(), runner.toString());
        String expected = run(jdk, "java", "-cp", output.toString(), "RunFloatingRecords");
        BackendRecordMethods.rewrite(output);
        assertEquals(expected, run(jdk, "java", "-cp", output.toString(), "RunFloatingRecords"));
    }

    @Test void rejectsPatternSwitchBeforeTranslation(@TempDir Path dir) throws Exception {
        File jdk = jdk("25");
        Path source = dir.resolve("PatternSwitch.java");
        Files.write(source, ("class PatternSwitch { static int length(Object value) {"
                + " return switch(value) { case String s -> s.length(); default -> 0; }; } }").getBytes("UTF-8"));
        File classes = Files.createDirectory(dir.resolve("classes")).toFile();
        run(jdk, "javac", "--release", "25", "-d", classes.toString(), source.toString());
        BackendPackager packager = new BackendPackager(host(dir, dir));
        BuildExecutionException error = assertThrows(BuildExecutionException.class,
                () -> packager.checkApplication(classes, api(dir), Collections.<String>emptyList()));
        assertTrue(error.getMessage().contains("SwitchBootstraps.typeSwitch"), error.getMessage());
        File staged = Files.createDirectory(dir.resolve("staged")).toFile();
        File natives = Files.createDirectory(dir.resolve("natives")).toFile();
        // A dependency may contain dead unsupported methods: defer to translator reachability.
        assertEquals(Collections.singletonList(staged.toString()), packager.prepareDependencyClasses(
                Collections.singletonList(classes.toString()), staged, natives));
    }

    @Test void permitsLambdaAndStringConcatenationBootstraps(@TempDir Path dir) throws Exception {
        File jdk = jdk("17");
        Path source = dir.resolve("Supported.java");
        Files.write(source, ("class Supported { static java.util.function.Supplier<String> make(int value) {"
                + " return () -> \"value=\" + value; } }").getBytes("UTF-8"));
        File classes = Files.createDirectory(dir.resolve("classes")).toFile();
        run(jdk, "javac", "--release", "17", "-d", classes.toString(), source.toString());
        new BackendPackager(host(dir, dir)).checkApplication(classes, api(dir), Collections.<String>emptyList());
    }

    @Test void stagesAndMaterializesRecordDependenciesWithoutChangingOriginals(@TempDir Path dir) throws Exception {
        File jdk = jdk("17");
        Path source = dir.resolve("Shared.java");
        Files.write(source, ("public record Shared(boolean enabled) {"
                + " public static void main(String[] args) {"
                + " System.out.println(new Shared(true).hashCode());"
                + " System.out.println(new Shared(false).hashCode());"
                + " System.out.println(new Shared(true));"
                + " System.out.println(new Shared(true).equals(new Shared(true)));"
                + " System.out.println(new Shared(true).equals(new Shared(false))); } }").getBytes("UTF-8"));
        Path original = Files.createDirectory(dir.resolve("dependency"));
        run(jdk, "javac", "--release", "17", "-d", original.toString(), source.toString());
        byte[] originalBytes = Files.readAllBytes(original.resolve("Shared.class"));
        Path jar = dir.resolve("dependency.jar");
        try (java.util.jar.JarOutputStream out = new java.util.jar.JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new java.util.jar.JarEntry("Shared.class"));
            out.write(originalBytes); out.closeEntry();
        }
        byte[] originalJar = Files.readAllBytes(jar);
        BackendPackager packager = new BackendPackager(host(dir, dir));
        for (Path dependency : Arrays.asList(original, jar)) {
            File staged = Files.createDirectory(dir.resolve(dependency.getFileName() + "-staged")).toFile();
            File natives = Files.createDirectory(dir.resolve(dependency.getFileName() + "-natives")).toFile();
            assertEquals(Collections.singletonList(staged.toString()), packager.prepareDependencyClasses(
                    Collections.singletonList(dependency.toString()), staged, natives));
            assertFalse(Arrays.equals(originalBytes, Files.readAllBytes(staged.toPath().resolve("Shared.class"))));
            assertEquals("1231\n1237\nShared[enabled=true]\ntrue\nfalse\n",
                    run(jdk, "java", "-cp", staged.toString(), "Shared").replace("\r\n", "\n"));
        }
        assertArrayEquals(originalBytes, Files.readAllBytes(original.resolve("Shared.class")));
        assertArrayEquals(originalJar, Files.readAllBytes(jar));
    }

    private static TestProjectHost host(Path dir, Path source) {
        TestProjectHost host = TestProjectHost.empty();
        host.projectDir = dir.toFile();
        host.buildDir = dir.resolve("build").toFile();
        host.outputDir = dir.resolve("original").toFile();
        host.sourceRoots.add(source.toString());
        return host;
    }

    private static File api(Path dir) throws Exception {
        Path api = Files.createDirectories(dir.resolve("api"));
        for (String name : Arrays.asList("java/lang/Object", "java/lang/String", "java/lang/StringBuilder",
                "java/lang/Integer", "java/lang/Number", "java/lang/AssertionError", "java/lang/System",
                "java/io/PrintStream", "java/util/Objects", "java/util/function/Supplier")) {
            Path file = api.resolve(name + ".class");
            Files.createDirectories(file.getParent());
            try (InputStream input = Object.class.getResourceAsStream("/" + name + ".class")) {
                Files.copy(input, file);
            }
        }
        ClassWriter record = new ClassWriter(0);
        record.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "java/lang/Record", null, "java/lang/Object", null);
        record.visitMethod(Opcodes.ACC_PROTECTED, "<init>", "()V", null, null).visitEnd();
        record.visitEnd();
        Files.write(api.resolve("java/lang/Record.class"), record.toByteArray());
        return api.toFile();
    }

    private static int major(Path file) throws Exception {
        byte[] bytes = Files.readAllBytes(file);
        return (bytes[6] & 255) * 256 + (bytes[7] & 255);
    }

    private static File jdk(String feature) {
        String home = System.getenv("JDK_" + feature + "_HOME");
        if (home == null && BackendPackager.parseJavacVersion("javac " + System.getProperty("java.version")) >= Integer.parseInt(feature)) {
            home = System.getProperty("java.home");
        }
        Assumptions.assumeTrue(home != null, "Set JDK_" + feature + "_HOME to exercise modern backend compilation");
        File jdk = new File(home);
        if (!new File(jdk, "bin/javac").isFile() && "jre".equals(jdk.getName())) jdk = jdk.getParentFile();
        return jdk;
    }

    private static String run(File jdk, String tool, String... arguments) throws Exception {
        java.util.List<String> command = new java.util.ArrayList<String>();
        command.add(new File(jdk, "bin/" + tool).getAbsolutePath());
        command.addAll(Arrays.asList(arguments));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try (InputStream in = process.getInputStream()) {
            byte[] buffer = new byte[4096]; int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        }
        String text = out.toString("UTF-8");
        assertEquals(0, process.waitFor(), text);
        return text;
    }
}
