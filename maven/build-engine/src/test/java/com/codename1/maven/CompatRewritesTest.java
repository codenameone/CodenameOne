/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.impl.compat.Resources;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// The member rewrites: a class javac compiled against the JDK is relocated
/// and then RUN, so a rule that produced a call the JVM rejects, or one that
/// lands on the wrong method, fails here rather than on a device.
public class CompatRewritesTest {

    private static final String SOURCE = "package q;\n"
            + "import java.io.InputStream;\n"
            + "import java.util.ArrayList;\n"
            + "import java.util.List;\n"
            + "import java.util.Locale;\n"
            + "import java.util.Objects;\n"
            + "import java.util.function.BiFunction;\n"
            + "import java.util.function.DoubleUnaryOperator;\n"
            + "import java.util.function.Predicate;\n"
            + "public class R {\n"
            + "    public static Object[] run() throws Exception {\n"
            + "        List<Object> out = new ArrayList<>();\n"
            + "        out.add(Objects.isNull(null));\n"
            + "        Predicate<Object> isNull = Objects::isNull;\n"
            + "        out.add(isNull.test(\"x\"));\n"
            + "        Locale fr = new Locale(\"fr\", \"FR\");\n"
            + "        out.add(fr.toLanguageTag());\n"
            + "        out.add(String.valueOf(R.class.getResource(\"data/r.txt\")));\n"
            + "        out.add(read(R.class.getResourceAsStream(\"/q/data/r.txt\")));\n"
            + "        ClassLoader loader = Thread.currentThread().getContextClassLoader();\n"
            + "        out.add(String.valueOf(loader.getResource(\"q/data/r.txt\")));\n"
            + "        out.add(read(loader.getResourceAsStream(\"q/data/r.txt\")));\n"
            + "        out.add(String.valueOf(ClassLoader.getSystemResource(\"q/none.txt\")));\n"
            + "        out.add(R.class.getClassLoader().getResources(\"q/data/r.txt\").hasMoreElements());\n"
            + "        out.add(\"a1b22c\".matches(\"[a-c0-9]+\"));\n"
            + "        out.add(String.join(\"|\", \"a,b,,c\".split(\",\")));\n"
            + "        out.add(\"a,b,c\".split(\",\", 2).length);\n"
            + "        out.add(\"x1y22\".replaceAll(\"[0-9]+\", \"#\"));\n"
            + "        out.add(\"x1y22\".replaceFirst(\"[0-9]+\", \"#\"));\n"
            + "        BiFunction<String, String, Boolean> matches = String::matches;\n"
            + "        out.add(matches.apply(\"abc\", \"a.c\"));\n"
            + "        out.add(Math.round(Math.pow(2, 10)));\n"
            + "        out.add(Math.round(StrictMath.exp(0) * 1000));\n"
            + "        DoubleUnaryOperator log = Math::log10;\n"
            + "        out.add(Math.round(log.applyAsDouble(1000)));\n"
            + "        return out.toArray();\n"
            + "    }\n"
            + "    static String read(InputStream in) throws Exception {\n"
            + "        StringBuilder sb = new StringBuilder();\n"
            + "        for (int c = in.read(); c >= 0; c = in.read()) {\n"
            + "            sb.append((char) c);\n"
            + "        }\n"
            + "        in.close();\n"
            + "        return sb.toString();\n"
            + "    }\n"
            + "    static Class<?> load(ClassLoader loader) throws Exception {\n"
            + "        return loader.loadClass(\"q.Other\");\n"
            + "    }\n"
            + "    static Object bundle() {\n"
            + "        return java.util.ResourceBundle.getBundle(\"q.Texts\", Locale.ROOT);\n"
            + "    }\n"
            + "    static Object services() {\n"
            + "        return java.util.ServiceLoader.load(Runnable.class);\n"
            + "    }\n"
            + "    static void handler() {\n"
            + "        Thread.setDefaultUncaughtExceptionHandler(null);\n"
            + "    }\n"
            + "    static Object exec() throws Exception {\n"
            + "        return Runtime.getRuntime().exec(new String[] {\"true\"});\n"
            + "    }\n"
            + "}\n";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @After
    public void restore() {
        Resources.cn1SetProvider(null);
        Resources.cn1SetUrlFactory(null);
        Resources.cn1ClearIndex();
    }

    private byte[] compiled() throws IOException {
        File classes = tmp.newFolder();
        CompatFixtures.compile(tmp.newFolder(), classes, "q/R.java", SOURCE);
        return Files.readAllBytes(new File(classes, "q/R.class").toPath());
    }

    /// Running the class links it, so the JVM verifies every rewritten call
    /// -- `Runtime.exec`'s among them, whose stand-in answers the
    /// framework's `Process` that `java.lang.Process` is relocated to.
    @Test
    public void aRewrittenClassRunsAndReachesTheStandIns() throws Exception {
        byte[] relocated = new ClassRelocator(CompatLayers.SWING).remap(compiled());

        Resources.cn1SetUrlFactory(CompatFixtures.cn1resUrls());
        Resources.cn1BeginIndex();
        Resources.cn1AddResource("q/data/r.txt");
        Resources.cn1SetProvider(new Resources.Provider() {
            @Override
            public InputStream open(String flatName) throws IOException {
                return "q__data__r.txt".equals(flatName) ? new ByteArrayInputStream("DATA".getBytes("UTF-8")) : null;
            }
        });
        Class<?> r = new CompatFixtures.Defining(getClass().getClassLoader()).define(relocated);
        Object[] out = (Object[]) r.getMethod("run").invoke(null);

        assertEquals(Arrays.asList(
                Boolean.TRUE, Boolean.FALSE, "fr-FR",
                "cn1res:/q/data/r.txt", "DATA", "cn1res:/q/data/r.txt", "DATA", "null", Boolean.TRUE,
                Boolean.TRUE, "a|b||c", Integer.valueOf(2), "x#y#", "x#y22", Boolean.TRUE,
                Long.valueOf(1024), Long.valueOf(1000), Long.valueOf(3)),
                Arrays.asList(out));
    }

    @Test
    public void everyRuleIsAppliedAndNothingElseIsTouched() throws Exception {
        byte[] original = compiled();
        Set<String> before = CompatFixtures.members(original);
        ClassRelocator swing = new ClassRelocator(CompatLayers.SWING);
        byte[] relocated = swing.remap(original);
        Set<String> after = CompatFixtures.members(relocated);

        for (String gone : new String[] {"java/lang/Class.getResource", "java/lang/Class.getResourceAsStream",
            "java/lang/ClassLoader.getResource", "java/lang/ClassLoader.getResourceAsStream",
            "java/lang/ClassLoader.getResources", "java/lang/ClassLoader.getSystemResource",
            "java/lang/ClassLoader.loadClass", "java/lang/Thread.getContextClassLoader",
            "java/lang/String.matches", "java/lang/String.split", "java/lang/String.replaceAll",
            "java/lang/String.replaceFirst", "java/lang/Math.pow", "java/lang/StrictMath.exp",
            "java/lang/Math.log10", "java/util/ResourceBundle.getBundle", "java/util/ServiceLoader.load",
            "java/lang/Thread.setDefaultUncaughtExceptionHandler", "java/lang/Runtime.exec"}) {
            assertTrue("The fixture does not use " + gone, before.contains(gone));
            assertFalse(gone + " survived: " + after, after.contains(gone));
        }
        String resources = "com/codename1/impl/compat/Resources.";
        for (String there : new String[] {resources + "getResource", resources + "getResourceAsStream",
            resources + "getResources", resources + "getSystemResource", resources + "getContextClassLoader",
            resources + "getBundle", resources + "load",
            "com/codename1/impl/compat/UncaughtExceptions.setDefaultUncaughtExceptionHandler",
            "com/codename1/util/regex/StringRegex.matches", "com/codename1/util/regex/StringRegex.split",
            "com/codename1/util/MathUtil.pow", "com/codename1/util/MathUtil.exp",
            "com/codename1/util/MathUtil.log10", "com/codename1/impl/process/RuntimeExec.exec",
            // Left exactly as written: the device's class library has them
            // under the JDK's names.
            "java/util/Objects.isNull", "java/util/Locale.<init>", "java/util/Locale.toLanguageTag",
            "java/lang/Math.round", "java/lang/Class.getClassLoader", "java/lang/String.join",
            // Asking the one loader for a class by name is Class.forName.
            resources + "loadClass"}) {
            assertTrue(there + " is missing: " + after, after.contains(there));
        }
        // A second pass finds nothing left to do.
        assertArrayEquals(relocated, swing.remap(relocated));
    }

    /// The Android layer's applications were never compiled against these
    /// members, and its build must not start referring to classes only a
    /// desktop layer needs.
    @Test
    public void theAndroidLayerAloneRewritesNoMember() throws Exception {
        Set<String> after = CompatFixtures.members(new ClassRelocator(AndroidRemapper.RELOCATION).remap(compiled()));
        assertTrue(after.toString(), after.contains("java/lang/Class.getResource"));
        assertTrue(after.toString(), after.contains("java/lang/String.split"));
        for (String member : after) {
            assertFalse(member, member.startsWith("com/codename1/impl/compat/"));
        }
        // With a desktop layer beside it, the rules apply.
        Set<String> mixed = CompatFixtures.members(
                new ClassRelocator(AndroidRemapper.RELOCATION, CompatLayers.JAVAFX).remap(compiled()));
        assertFalse(mixed.toString(), mixed.contains("java/lang/Class.getResource"));
    }

    /// The Android layer's runtime asks for resources by their flat names
    /// already, and a rule applied to it would change what it reads.
    @Test
    public void theAndroidRuntimeIsNotRewritten() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, AndroidRemapper.RELOCATION.target() + "android/Probe", null,
                "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "open",
                "(Ljava/lang/Class;)Ljava/io/InputStream;", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("/x");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getResourceAsStream",
                "(Ljava/lang/String;)Ljava/io/InputStream;", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
        cw.visitEnd();
        Set<String> after = CompatFixtures.members(
                new ClassRelocator(AndroidRemapper.RELOCATION, CompatLayers.SWING).remap(cw.toByteArray()));
        assertEquals(after.toString(), 1, after.size());
        assertTrue(after.contains("java/lang/Class.getResourceAsStream"));
    }
}
