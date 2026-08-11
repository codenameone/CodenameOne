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
package com.codename1.hardening;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** End-to-end pipeline test: demux, ProGuard rename, string encryption, repackage, mapping. */
public class HardeningEngineTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String SECRETS = "com/codename1/hardening/fixture/Secrets";
    private static final String HELPER = "com/codename1/hardening/fixture/Helper";
    // Deliberately includes NUL and high bytes to prove byte-for-byte resource preservation,
    // written explicitly so the source stays pure ASCII.
    private static final byte[] RES_BYTES = new byte[]{
            'C', 'N', '1', '-', 'B', 'L', 'O', 'B', 0x00, (byte) 0xFF, (byte) 0x80, 0x7F, 'z'};

    private File buildInputJar() throws Exception {
        File jar = tmp.newFile("app.jar");
        FileOutputStream fo = new FileOutputStream(jar);
        ZipOutputStream zos = new ZipOutputStream(fo);
        putClass(zos, SECRETS);
        putClass(zos, HELPER);
        zos.putNextEntry(new ZipEntry("theme.res"));
        zos.write(RES_BYTES);
        zos.closeEntry();
        zos.finish();
        fo.close();
        return jar;
    }

    private void putClass(ZipOutputStream zos, String internal) throws Exception {
        zos.putNextEntry(new ZipEntry(internal + ".class"));
        zos.write(resourceBytes(internal));
        zos.closeEntry();
    }

    private byte[] resourceBytes(String internal) throws Exception {
        InputStream in = getClass().getResourceAsStream("/" + internal + ".class");
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) >= 0) {
            b.write(buf, 0, r);
        }
        in.close();
        return b.toByteArray();
    }

    /** A synthetic native interface: {@code interface <internalName> extends NativeInterface}. */
    private static byte[] nativeInterface(String internalName) {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_ABSTRACT | org.objectweb.asm.Opcodes.ACC_INTERFACE,
                internalName, null, "java/lang/Object",
                new String[]{"com/codename1/system/NativeInterface"});
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** A class declaring {@code count} distinct no-arg void methods (all sharing the {@code ()V} descriptor). */
    private static byte[] classWithVoidMethods(String internal, int count) {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                internal, null, "java/lang/Object", null);
        for (int i = 0; i < count; i++) {
            org.objectweb.asm.MethodVisitor mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                    "m" + i, "()V", null, null);
            mv.visitCode();
            mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
            mv.visitMaxs(0, 1);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    @Test
    public void memberNamingScopeAccumulatesSameDescriptorMethodsAcrossClasses() {
        // Same-descriptor methods across an inheritance hierarchy cannot share an obfuscated name (that
        // would be an accidental override), so their naming scope is the SUM across classes, not the
        // per-class max. Two classes each declaring 30 ()V methods => a scope of 60; the earlier per-class
        // max (30) would undersize the dictionary and drop ProGuard back to short names.
        java.util.Map<String, byte[]> classes = new java.util.HashMap<String, byte[]>();
        classes.put("app/A", classWithVoidMethods("app/A", 30));
        classes.put("app/B", classWithVoidMethods("app/B", 30));
        assertEquals(60, HardeningEngine.maxMemberNamingScope(classes));
    }

    /** A class with a SourceFile and a static run() that instantiates each referenced type (keeping it reachable). */
    private static byte[] mainReferencing(String internal, String sourceFile, String... refs) {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(
                org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                internal, null, "java/lang/Object", null);
        cw.visitSource(sourceFile, null);
        org.objectweb.asm.MethodVisitor init = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        init.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, "java/lang/Object",
                "<init>", "()V", false);
        init.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();
        org.objectweb.asm.MethodVisitor m = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_STATIC, "run", "()V", null, null);
        m.visitCode();
        for (String ref : refs) {
            m.visitTypeInsn(org.objectweb.asm.Opcodes.NEW, ref);
            m.visitInsn(org.objectweb.asm.Opcodes.DUP);
            m.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, ref, "<init>", "()V", false);
            m.visitInsn(org.objectweb.asm.Opcodes.POP);
        }
        m.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        m.visitMaxs(2, 0);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** A class carrying a {@code SourceFile} attribute plus a constructor and a method to rename. */
    private static byte[] classWithSource(String internal, String sourceFile) {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(
                org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                internal, null, "java/lang/Object", null);
        cw.visitSource(sourceFile, null);
        org.objectweb.asm.MethodVisitor init = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        init.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, "java/lang/Object",
                "<init>", "()V", false);
        init.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();
        org.objectweb.asm.MethodVisitor m = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "doThing", "()V", null, null);
        m.visitCode();
        m.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        m.visitMaxs(0, 1);
        m.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    @Test
    public void engineMappingRecordsNonDefaultSourceFiles() throws Exception {
        org.junit.Assume.assumeTrue("ProGuard renamer needs JDK <=20", HardeningEngine.proguardCanRunHere());
        // Screen is a Kotlin class (Screen.kt) whose name can't reconstruct the file; Widget's Widget.java
        // IS the synthesized default. The engine strips SourceFile, so it must record Screen.kt in the
        // mapping (else a retrace points Screen at a non-existent Screen.java) but need not record Widget.
        File jar = tmp.newFile("srcfile.jar");
        FileOutputStream fo = new FileOutputStream(jar);
        ZipOutputStream zos = new ZipOutputStream(fo);
        zos.putNextEntry(new ZipEntry("app/Main.class"));
        // Main (kept) references Screen and Widget so ProGuard reaches and renames them.
        zos.write(mainReferencing("app/Main", "Main.java", "app/Screen", "app/Widget"));
        zos.closeEntry();
        zos.putNextEntry(new ZipEntry("app/Screen.class"));
        zos.write(classWithSource("app/Screen", "Screen.kt"));
        zos.closeEntry();
        zos.putNextEntry(new ZipEntry("app/Widget.class"));
        zos.write(classWithSource("app/Widget", "Widget.java"));
        zos.closeEntry();
        zos.finish();
        fo.close();

        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "standard");   // rename on (engine-renamed target)
        File mapping = tmp.newFile("srcfile-map.txt");
        HardeningRequest req = new HardeningRequest()
                .inputJar(jar).outputJar(tmp.newFile("srcfile-out.jar")).mappingFile(mapping)
                .workDir(tmp.newFolder("srcfile-work"))
                .config(HardeningConfig.from(hints, "ios", true))
                .mainClass("app.Main");
        HardeningResult r = HardeningEngine.harden(req);
        assertTrue(r.isHardened());
        String map = new String(java.nio.file.Files.readAllBytes(mapping.toPath()), "UTF-8");
        assertTrue("the Kotlin source file must be recorded: " + map,
                map.contains("\"fileName\":\"Screen.kt\""));
        assertFalse("the default Widget.java need not be recorded",
                map.contains("\"fileName\":\"Widget.java\""));
    }

    /** A class whose {@code run()} method loads each given string literal (and discards it). */
    private static byte[] classWithLiterals(String internal, String... literals) {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                internal, null, "java/lang/Object", null);
        org.objectweb.asm.MethodVisitor mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_STATIC, "run", "()V", null, null);
        mv.visitCode();
        for (String s : literals) {
            mv.visitLdcInsn(s);
            mv.visitInsn(org.objectweb.asm.Opcodes.POP);
        }
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private File writeJar(String name, String internal, byte[] classBytes) throws Exception {
        File jar = tmp.newFile(name);
        FileOutputStream fo = new FileOutputStream(jar);
        ZipOutputStream zos = new ZipOutputStream(fo);
        zos.putNextEntry(new ZipEntry(internal + ".class"));
        zos.write(classBytes);
        zos.closeEntry();
        zos.finish();
        fo.close();
        return jar;
    }

    private HardeningResult hardenAppWithLibrary(String platform, String appInternal, String[] appLiterals,
            String libInternal, String[] libLiterals, String suffix) throws Exception {
        File appJar = writeJar("app-" + suffix + ".jar", appInternal, classWithLiterals(appInternal, appLiterals));
        File libJar = writeJar("lib-" + suffix + ".jar", libInternal, classWithLiterals(libInternal, libLiterals));
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "aggressive");
        hints.put("harden.strings", "all");
        hints.put("harden.rename", "false");     // isolate string encryption -- no ProGuard dependency
        hints.put("harden.controlFlow", "false");
        HardeningRequest req = new HardeningRequest()
                .inputJar(appJar).outputJar(tmp.newFile("out-" + suffix + ".jar"))
                .mappingFile(tmp.newFile("map-" + suffix + ".txt"))
                .reportFile(tmp.newFile("report-" + suffix + ".json"))
                .workDir(tmp.newFolder("work-" + suffix))
                .config(HardeningConfig.from(hints, platform, true))
                .mainClass(appInternal.replace('/', '.'));
        req.addLibraryJar(libJar);
        return HardeningEngine.harden(req);
    }

    /**
     * A class with {@code public static final String FIELD = value} plus a second, unreferenced
     * {@code OTHER} constant so an Android build (where the referenced FIELD is preserved) still encrypts
     * something and is marked hardened.
     */
    private static byte[] classWithStaticFinalString(String internal, String field, String value) {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                internal, null, "java/lang/Object", null);
        cw.visitField(org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC
                | org.objectweb.asm.Opcodes.ACC_FINAL, field, "Ljava/lang/String;", null, value).visitEnd();
        cw.visitField(org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC
                | org.objectweb.asm.Opcodes.ACC_FINAL, "OTHER", "Ljava/lang/String;", null,
                "an unrelated secret constant not named by any bundled source").visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** The ConstantValue of the named static-final String field in a class, or null if stripped/absent. */
    private static String constantValueOf(byte[] classBytes, final String field) {
        final String[] holder = new String[1];
        new org.objectweb.asm.ClassReader(classBytes).accept(
                new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                    @Override
                    public org.objectweb.asm.FieldVisitor visitField(int access, String name, String desc,
                            String sig, Object value) {
                        if (field.equals(name)) {
                            holder[0] = value instanceof String ? (String) value : null;
                        }
                        return null;
                    }
                }, org.objectweb.asm.ClassReader.SKIP_CODE);
        return holder[0];
    }

    private HardeningResult hardenClassWithBundledSource(String platform, String constantValue,
            String bundledSource, String suffix) throws Exception {
        File jar = tmp.newFile("cv-" + suffix + ".jar");
        FileOutputStream fo = new FileOutputStream(jar);
        ZipOutputStream zos = new ZipOutputStream(fo);
        zos.putNextEntry(new ZipEntry("app/Constants.class"));
        zos.write(classWithStaticFinalString("app/Constants", "MODE", constantValue));
        zos.closeEntry();
        zos.putNextEntry(new ZipEntry("com/lib/Native.java"));
        zos.write(bundledSource.getBytes("UTF-8"));
        zos.closeEntry();
        zos.finish();
        fo.close();
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "aggressive");
        hints.put("harden.strings", "all");
        hints.put("harden.rename", "false");
        HardeningRequest req = new HardeningRequest()
                .inputJar(jar).outputJar(tmp.newFile("cv-out-" + suffix + ".jar"))
                .mappingFile(tmp.newFile("cv-map-" + suffix + ".txt"))
                .reportFile(tmp.newFile("cv-report-" + suffix + ".json"))
                .workDir(tmp.newFolder("cv-work-" + suffix))
                .config(HardeningConfig.from(hints, platform, false))
                .mainClass("app.Constants");
        return HardeningEngine.harden(req);
    }

    @Test
    public void staticFinalConstantReferencedByBundledSourceKeepsItsConstantValueOnAndroid() throws Exception {
        // A bundled Android .java source references app.Constants.MODE in a case label. On Android the
        // source is compiled against the transformed classes, so stripping MODE's ConstantValue would
        // break that compilation. The engine preserves it (still a compile-time constant) on Android.
        String value = "the mode constant a bundled source needs";
        String source = "package com.lib; class Native { int f(int x){ switch(x){ "
                + "case /* app.Constants. */ 0: return app.Constants.MODE.length(); default: return 0; } } }";
        HardeningResult r = hardenClassWithBundledSource("and", value, source, "and");
        assertTrue(r.isHardened());
        byte[] cls = readAll(r.getHardenedJar()).get("app/Constants.class");
        assertEquals("MODE keeps its ConstantValue so the bundled source still compiles", value,
                constantValueOf(cls, "MODE"));
        // Preservation is selective: a constant NOT named by any bundled source is still encrypted.
        assertEquals("an unreferenced constant is still stripped/encrypted", null,
                constantValueOf(cls, "OTHER"));
    }

    @Test
    public void staticFinalConstantIsStrippedWhenNoSourceCompilesAgainstIt() throws Exception {
        // iOS routes bundled .java into the resource tree and never compiles it, so there is no
        // constant-expression hazard: MODE's ConstantValue is stripped and encrypted as usual.
        String value = "the mode constant a bundled source needs";
        String source = "package com.lib; class Native { int f(){ return app.Constants.MODE.length(); } }";
        HardeningResult r = hardenClassWithBundledSource("ios", value, source, "ios");
        assertTrue(r.isHardened());
        assertEquals("on iOS the ConstantValue is stripped (encrypted), no bundled source compiles it",
                null, constantValueOf(readAll(r.getHardenedJar()).get("app/Constants.class"), "MODE"));
        assertTrue("the stripped constant is encrypted", r.getEncryptedStrings() >= 1);
    }

    @Test
    public void librarySharedLiteralsStayPlaintextOnParparVM() throws Exception {
        // On a ParparVM-C target a compile-time literal is a never-interned constant-pool object while an
        // encrypted app copy is intern()ed, so a value that ALSO appears as a literal in an unhardened
        // library class must be left plaintext or a valid literal == against the library copy (which held
        // before hardening) would break. A value unique to the app is still encrypted.
        String shared = "value shared between the app and an unhardened library class";
        String appOnly = "a secret value that appears only in the application";
        HardeningResult r = hardenAppWithLibrary("ios", "app/App", new String[]{shared, appOnly},
                "lib/Lib", new String[]{shared}, "ios");
        assertTrue(r.isHardened());
        byte[] app = readAll(r.getHardenedJar()).get("app/App.class");
        assertTrue("a library-shared literal stays plaintext to preserve == on ParparVM",
                StringEncryptTransform.containsStringLiteral(app, shared));
        assertFalse("an app-only literal is still encrypted",
                StringEncryptTransform.containsStringLiteral(app, appOnly));
    }

    @Test
    public void librarySharedLiteralsAreEncryptedOnRealJvm() throws Exception {
        // On a real-JVM target every compile-time literal is interned to the same pool intern() uses, so
        // there is no cross-boundary identity hazard: the library scan is skipped and the shared value IS
        // encrypted (full coverage, no needless plaintext).
        String shared = "value shared between the app and an unhardened library class";
        HardeningResult r = hardenAppWithLibrary("javase", "app/App", new String[]{shared},
                "lib/Lib", new String[]{shared}, "jvm");
        assertTrue(r.isHardened());
        byte[] app = readAll(r.getHardenedJar()).get("app/App.class");
        assertFalse("on a real JVM the shared literal is encrypted (no cross-boundary hazard)",
                StringEncryptTransform.containsStringLiteral(app, shared));
    }

    private HardeningResult harden(HardeningProfile profile, String platform, boolean renameSupported)
            throws Exception {
        File in = buildInputJar();
        File out = tmp.newFile("app-hardened.jar");
        File mapping = tmp.newFile("mapping.txt");
        File report = tmp.newFile("report.json");
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", profile.name().toLowerCase());
        HardeningConfig cfg = HardeningConfig.from(hints, platform, renameSupported);
        HardeningRequest req = new HardeningRequest()
                .inputJar(in).outputJar(out).mappingFile(mapping).reportFile(report)
                .workDir(tmp.newFolder("work")).config(cfg)
                // Keep Secrets so the test can load it by name; Helper still gets renamed.
                .mainClass("com.codename1.hardening.fixture.Secrets")
                .buildKey("TESTKEY");
        return HardeningEngine.harden(req);
    }

    @Test
    public void standardHardenRenamesEncryptsAndPreservesResources() throws Exception {
        // ProGuard 7.3.2 can't read JDK 21+ class files; the renamer runs on JDK <=20 in production.
        org.junit.Assume.assumeTrue("ProGuard renamer needs JDK <=20", HardeningEngine.proguardCanRunHere());
        HardeningResult r = harden(HardeningProfile.STANDARD, "ios", true);
        assertTrue(r.isHardened());

        Map<String, byte[]> outEntries = readAll(r.getHardenedJar());

        // Non-class resource carried across byte-for-byte.
        assertArrayEquals(RES_BYTES, outEntries.get("theme.res"));

        // Helper (not kept) was renamed away; Secrets (kept as main) remains.
        assertFalse("Helper should have been renamed", outEntries.containsKey(HELPER + ".class"));
        assertTrue("kept main class should remain", outEntries.containsKey(SECRETS + ".class"));
        assertTrue("a zq-prefixed renamed class should exist", hasZqClass(outEntries.keySet()));

        // Mapping records the rename and Helper is present in it.
        String mapping = new String(Files.readAllBytes(r.getMappingFile().toPath()), Charset.forName("UTF-8"));
        assertTrue(mapping.contains("com.codename1.hardening.fixture.Helper ->"));
        assertTrue(mapping.contains("# mappingId:"));
        assertEquals(64, r.getMappingId().length());

        // Standard = constants mode: the static-final API constant is encrypted (including its
        // inlined read in api()); a plain method literal like the greeting is left alone.
        // Behaviour is intact when loaded either way.
        byte[] secrets = outEntries.get(SECRETS + ".class");
        assertFalse(StringEncryptTransform.containsStringLiteral(secrets,
                "https://api.example.com/secret-endpoint"));
        assertTrue(StringEncryptTransform.containsStringLiteral(secrets, "hello secret world"));

        URLClassLoader cl = new URLClassLoader(new URL[]{r.getHardenedJar().toURI().toURL()},
                getClass().getClassLoader().getParent());
        Class<?> c = Class.forName("com.codename1.hardening.fixture.Secrets", true, cl);
        assertEquals("hello secret world", c.getMethod("greet").invoke(null));
        assertEquals("https://api.example.com/secret-endpoint", c.getMethod("api").invoke(null));
        cl.close();
    }

    @Test
    public void serviceProviderClassesAreKept() throws Exception {
        org.junit.Assume.assumeTrue("ProGuard renamer needs JDK <=20", HardeningEngine.proguardCanRunHere());
        // Build a jar where Helper is declared as a service provider; it must survive un-renamed
        // so the verbatim-copied descriptor still resolves via ServiceLoader.
        File jar = tmp.newFile("svc.jar");
        FileOutputStream fo = new FileOutputStream(jar);
        ZipOutputStream zos = new ZipOutputStream(fo);
        putClass(zos, SECRETS);
        putClass(zos, HELPER);
        zos.putNextEntry(new ZipEntry("META-INF/services/com.example.MyService"));
        zos.write("# a provider\ncom.codename1.hardening.fixture.Helper\n"
                .getBytes(Charset.forName("UTF-8")));
        zos.closeEntry();
        zos.finish();
        fo.close();

        File out = tmp.newFile("svc-hardened.jar");
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "standard");
        HardeningRequest req = new HardeningRequest()
                .inputJar(jar).outputJar(out).mappingFile(tmp.newFile("svc-map.txt"))
                .workDir(tmp.newFolder("svc-work"))
                .config(HardeningConfig.from(hints, "ios", true))
                .mainClass("com.codename1.hardening.fixture.Secrets");
        HardeningResult r = HardeningEngine.harden(req);
        assertTrue(r.isHardened());
        Map<String, byte[]> outEntries = readAll(out);
        assertTrue("service provider class must be kept, not renamed",
                outEntries.containsKey(HELPER + ".class"));
        assertArrayEquals("# a provider\ncom.codename1.hardening.fixture.Helper\n"
                        .getBytes(Charset.forName("UTF-8")),
                outEntries.get("META-INF/services/com.example.MyService"));
    }

    @Test
    public void offProfileIsSkippedAndReturnsInput() throws Exception {
        HardeningResult r = harden(HardeningProfile.OFF, "ios", true);
        assertFalse(r.isHardened());
        assertEquals(HardeningResult.Outcome.SKIPPED_NOT_REQUESTED, r.getOutcome());
    }

    @Test
    public void platformOptOutIsSkippedNotHardened() throws Exception {
        File in = buildInputJar();
        File out = tmp.newFile("optout-hardened.jar");
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "standard");
        hints.put("harden.ios.enabled", "false");
        HardeningRequest req = new HardeningRequest()
                .inputJar(in).outputJar(out).mappingFile(tmp.newFile("optout-map.txt"))
                .workDir(tmp.newFolder("optout-work"))
                .config(HardeningConfig.from(hints, "ios", true))
                .mainClass("com.codename1.hardening.fixture.Secrets");
        HardeningResult r = HardeningEngine.harden(req);
        assertFalse(r.isHardened());
        assertEquals(HardeningResult.Outcome.SKIPPED_PLATFORM_DISABLED, r.getOutcome());
        assertFalse("an opted-out platform must not count as an applied transform",
                HardeningEngine.willApplyAnyTransform(HardeningConfig.from(hints, "ios", true)));
    }

    @Test
    public void offLevelWithStaleOverrideAppliesNoTransform() throws Exception {
        // harden.level=off with a leftover harden.rename=true must not count as an applied transform,
        // so a non-entitled build that turned hardening off is skipped (harden() returns SKIPPED for
        // OFF) rather than rejected as not-entitled by the CLI's entitlement gate.
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "off");
        hints.put("harden.rename", "true");
        HardeningConfig cfg = HardeningConfig.from(hints, "ios", true);
        assertFalse("off must apply no transform even with a stale override",
                HardeningEngine.willApplyAnyTransform(cfg));
        File in = buildInputJar();
        File out = tmp.newFile("off-override-hardened.jar");
        HardeningRequest req = new HardeningRequest()
                .inputJar(in).outputJar(out).mappingFile(tmp.newFile("off-override-map.txt"))
                .workDir(tmp.newFolder("off-override-work"))
                .config(cfg)
                .mainClass("com.codename1.hardening.fixture.Secrets");
        HardeningResult r = HardeningEngine.harden(req);
        assertFalse(r.isHardened());
        assertEquals(HardeningResult.Outcome.SKIPPED_NOT_REQUESTED, r.getOutcome());
    }

    @Test
    public void androidRenameOnlyIsHardenedViaR8() throws Exception {
        // Android (renameSupported=false), standard with strings off: the engine renames nothing,
        // but R8 will, so the build must be marked hardened rather than skipped.
        File in = buildInputJar();
        File out = tmp.newFile("and-hardened.jar");
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "standard");
        hints.put("harden.strings", "off");
        HardeningRequest req = new HardeningRequest()
                .inputJar(in).outputJar(out).mappingFile(tmp.newFile("and-map.txt"))
                .workDir(tmp.newFolder("and-work"))
                .config(HardeningConfig.from(hints, "and", false))
                .mainClass("com.codename1.hardening.fixture.Secrets");
        HardeningResult r = HardeningEngine.harden(req);
        assertTrue("Android rename-only must be marked hardened (R8 renames)", r.isHardened());
        assertEquals(0, r.getRenamedClasses());
        assertTrue(r.getTransformsApplied().contains("rename:r8"));
    }

    @Test
    public void requestedTransformWithNoEligibleTargetsIsSkipped() throws Exception {
        // rename off + strings requested (constants), but the only class has no encryptable string:
        // nothing actually runs, so the build must report SKIPPED rather than stamp cn1.hardened=true
        // on a byte-unchanged app.
        File jar = tmp.newFile("noop2.jar");
        FileOutputStream fo = new FileOutputStream(jar);
        ZipOutputStream zos = new ZipOutputStream(fo);
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "app/NoStrings", null, "java/lang/Object", null);
        org.objectweb.asm.MethodVisitor m = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_STATIC, "add", "(II)I", null, null);
        m.visitCode();
        m.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD, 0);
        m.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD, 1);
        m.visitInsn(org.objectweb.asm.Opcodes.IADD);
        m.visitInsn(org.objectweb.asm.Opcodes.IRETURN);
        m.visitMaxs(2, 2);
        m.visitEnd();
        cw.visitEnd();
        zos.putNextEntry(new ZipEntry("app/NoStrings.class"));
        zos.write(cw.toByteArray());
        zos.closeEntry();
        zos.finish();
        fo.close();

        File out = tmp.newFile("noop2-hardened.jar");
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "standard");
        hints.put("harden.rename", "false");
        hints.put("harden.strings", "constants");
        HardeningRequest req = new HardeningRequest()
                .inputJar(jar).outputJar(out).mappingFile(tmp.newFile("noop2-map.txt"))
                .workDir(tmp.newFolder("noop2-work"))
                .config(HardeningConfig.from(hints, "ios", true))
                .mainClass("app.NoStrings");
        HardeningResult r = HardeningEngine.harden(req);
        assertFalse("no eligible target ran, so the build must not be marked hardened", r.isHardened());
        assertEquals(HardeningResult.Outcome.SKIPPED_NOT_REQUESTED, r.getOutcome());
    }

    @Test
    public void nonOffLevelWithAllTransformsDisabledIsSkipped() throws Exception {
        // standard, but rename off and strings off -> nothing to do -> not stamped hardened.
        File in = buildInputJar();
        File out = tmp.newFile("noop-hardened.jar");
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "standard");
        hints.put("harden.rename", "false");
        hints.put("harden.strings", "off");
        HardeningRequest req = new HardeningRequest()
                .inputJar(in).outputJar(out).mappingFile(tmp.newFile("noop-map.txt"))
                .workDir(tmp.newFolder("noop-work"))
                .config(HardeningConfig.from(hints, "ios", true))
                .mainClass("com.codename1.hardening.fixture.Secrets");
        HardeningResult r = HardeningEngine.harden(req);
        assertFalse("a config that applies no transform must not be marked hardened", r.isHardened());
        assertEquals(HardeningResult.Outcome.SKIPPED_NOT_REQUESTED, r.getOutcome());
    }

    @Test
    public void androidDoesNotRenameButStillEncrypts() throws Exception {
        // renameSupported=false models Android, where R8 is the sole renamer.
        HardeningResult r = harden(HardeningProfile.STANDARD, "and", false);
        assertTrue(r.isHardened());
        Map<String, byte[]> outEntries = readAll(r.getHardenedJar());
        // Nothing renamed: both classes keep their names.
        assertTrue(outEntries.containsKey(HELPER + ".class"));
        assertTrue(outEntries.containsKey(SECRETS + ".class"));
        assertEquals(0, r.getRenamedClasses());
        // Standard = constants mode: the static-final API constant is encrypted (including its
        // inlined copy in api()), but a plain method literal like the greeting is left alone.
        assertTrue(r.getEncryptedStrings() >= 1);
        byte[] secrets = outEntries.get(SECRETS + ".class");
        assertFalse("declared constant must be encrypted",
                StringEncryptTransform.containsStringLiteral(secrets, "https://api.example.com/secret-endpoint"));
        assertTrue("a plain (non-constant) literal is left alone in constants mode",
                StringEncryptTransform.containsStringLiteral(secrets, "hello secret world"));
    }

    @Test
    public void javascriptSkipsStringEncryption() throws Exception {
        org.junit.Assume.assumeTrue("ProGuard renamer needs JDK <=20", HardeningEngine.proguardCanRunHere());
        HardeningResult r = harden(HardeningProfile.AGGRESSIVE, "javascript", true);
        assertTrue(r.isHardened());
        // On JS the bridge could break, so string encryption is off; renaming still happens.
        assertEquals(0, r.getEncryptedStrings());
        assertTrue(r.getRenamedClasses() >= 1);
    }

    @Test
    public void reportSerializesWarnings() throws Exception {
        // On the ParparVM native ports control-flow obfuscation is skipped as unsafe, which records a
        // warning. String encryption still applies on iOS, so the build is hardened and a report is
        // written (no ProGuard/rename needed). The warning must appear in the JSON report, not only in
        // the forked-process log, or a consumer reading the report is told less than the truth.
        File in = buildInputJar();
        File out = tmp.newFile("app-hardened-warn.jar");
        File mapping = tmp.newFile("mapping-warn.txt");
        File report = tmp.newFile("report-warn.json");
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "aggressive");
        HardeningConfig cfg = HardeningConfig.from(hints, "ios", false);
        HardeningRequest req = new HardeningRequest()
                .inputJar(in).outputJar(out).mappingFile(mapping).reportFile(report)
                .workDir(tmp.newFolder("work-warn")).config(cfg)
                .mainClass("com.codename1.hardening.fixture.Secrets").buildKey("TESTKEY");
        HardeningResult r = HardeningEngine.harden(req);
        assertTrue("string encryption should harden the iOS build", r.isHardened());
        assertFalse("the skipped control-flow pass should record a warning", r.getWarnings().isEmpty());
        String json = new String(Files.readAllBytes(report.toPath()), Charset.forName("UTF-8"));
        assertTrue("report must contain a warnings array: " + json, json.indexOf("\"warnings\"") >= 0);
        assertTrue("report must serialize the warning text: " + json,
                json.indexOf("control-flow obfuscation is not applied") >= 0);
    }

    @Test
    public void scannerKeepsNativeInterfacePeers() throws Exception {
        // Phase 1: find the native interface. Phase 2: keep ITS generated Impl/Stub peer -- narrow,
        // not the old blanket **Impl / **Stub.
        Map<String, byte[]> classes = new HashMap<String, byte[]>();
        classes.put("app/MyNative", nativeInterface("app/MyNative"));
        classes.put(HELPER, resourceBytes(HELPER));
        InputJarKeepScanner scanner = new InputJarKeepScanner();
        scanner.scan(classes);
        java.util.List<String> rules = scanner.keepRules();
        assertTrue("the native interface's Impl peer must be kept",
                rules.contains("-keep class app.MyNativeImpl { *; }"));
        assertTrue("the native interface's Stub peer must be kept",
                rules.contains("-keep class app.MyNativeStub { *; }"));
        // A plain class is NOT kept -- there is no reflection to keep it for.
        for (String rule : rules) {
            assertFalse("a non-native class must not be kept: " + rule,
                    rule.contains("hardening.fixture.Helper"));
        }
    }

    @Test
    public void androidExportsNativeInterfaceKeepsToR8() throws Exception {
        // On Android the engine does not rename (R8 does), so the native-interface peer keeps plus
        // the user's harden.keep must reach the R8 keep file.
        File jar = tmp.newFile("r8.jar");
        FileOutputStream fo = new FileOutputStream(jar);
        ZipOutputStream zos = new ZipOutputStream(fo);
        putClass(zos, SECRETS);
        zos.putNextEntry(new ZipEntry("app/MyNative.class"));
        zos.write(nativeInterface("app/MyNative"));
        zos.closeEntry();
        zos.finish();
        fo.close();

        File out = tmp.newFile("r8-hardened.jar");
        File r8Keep = tmp.newFile("cn1-r8-keep.pro");
        Map<String, String> hints = new HashMap<String, String>();
        hints.put("harden.level", "standard");
        hints.put("harden.keep", "-keep class com.example.Manual { *; }");
        HardeningRequest req = new HardeningRequest()
                .inputJar(jar).outputJar(out).mappingFile(tmp.newFile("r8-map.txt"))
                .r8KeepFile(r8Keep)
                .workDir(tmp.newFolder("r8-work"))
                .config(HardeningConfig.from(hints, "and", false))
                .mainClass("com.codename1.hardening.fixture.Secrets");
        HardeningResult r = HardeningEngine.harden(req);
        assertTrue(r.isHardened());
        assertTrue("engine must emit the R8 keep file", r8Keep.isFile());
        String keep = new String(Files.readAllBytes(r8Keep.toPath()), Charset.forName("UTF-8"));
        assertTrue("native interface peer must reach R8",
                keep.contains("-keep class app.MyNativeImpl { *; }"));
        assertTrue("the main class must reach R8",
                keep.contains("com.codename1.hardening.fixture.Secrets"));
        assertTrue("the user's harden.keep must reach R8",
                keep.contains("-keep class com.example.Manual { *; }"));
    }

    private boolean hasZqClass(java.util.Set<String> names) {
        for (String n : names) {
            if (n.endsWith(".class") && n.substring(n.lastIndexOf('/') + 1).startsWith(Cn1NameFactory.PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, byte[]> readAll(File jar) throws Exception {
        Map<String, byte[]> out = new HashMap<String, byte[]>();
        ZipInputStream zis = new ZipInputStream(Files.newInputStream(jar.toPath()));
        ZipEntry e;
        while ((e = zis.getNextEntry()) != null) {
            if (e.isDirectory()) {
                continue;
            }
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r;
            while ((r = zis.read(buf)) >= 0) {
                b.write(buf, 0, r);
            }
            out.put(e.getName(), b.toByteArray());
        }
        zis.close();
        return out;
    }
}
