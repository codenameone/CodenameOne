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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Every public member of a {@code Ports/CLDC11} class must exist in {@code vm/JavaAPI}.
 *
 * CLDC11 is the API applications compile against and the index the build's
 * compliance check reads; JavaAPI is what a device runs. A member CLDC11 declares
 * and JavaAPI lacks compiles, passes the compliance check, and then fails the
 * translation (or links to nothing) on the device. So JavaAPI must be a superset.
 *
 * The rule, member by member: for each public or protected class of CLDC11, each
 * public or protected field, method and constructor it declares must exist in
 * JavaAPI with the same name and descriptor, at least as accessible, and with the
 * same static-ness. Methods and fields may be inherited there (a JavaAPI class may
 * put a method in a superclass or a default method in an interface); constructors
 * may not. Synthetic and bridge members are ignored.
 *
 * Existing gaps are listed in {@code cldc-javaapi-parity-baseline.txt}, a ratchet:
 * a NEW gap fails, and so does a listed gap that has since been closed, so the
 * file only ever shrinks. Regenerate it after closing gaps with
 * {@code -DcldcParity.writeBaseline=true}; never to admit a new one.
 *
 * It also holds the four copies of {@code com.codename1.impl.JdkHost} byte for byte
 * identical, as that class's documentation promises.
 */
class CldcJavaApiParityTest {
    private static final Path REPO_ROOT = Paths.get("..", "..").toAbsolutePath().normalize();
    private static final Path CLDC_SRC = REPO_ROOT.resolve(Paths.get("Ports", "CLDC11", "src"));
    private static final Path JAVAAPI_SRC = REPO_ROOT.resolve(Paths.get("vm", "JavaAPI", "src"));
    private static final Path BASELINE = REPO_ROOT.resolve(Paths.get(
            "vm", "tests", "src", "test", "resources", "cldc-javaapi-parity-baseline.txt"));

    private static final String[] JDK_HOST_COPIES = {
        "vm/JavaAPI/src/com/codename1/impl/JdkHost.java",
        "Ports/CLDC11/src/com/codename1/impl/JdkHost.java",
        "Ports/JavaSE/src/com/codename1/impl/JdkHost.java",
        "Ports/Android/src/com/codename1/impl/JdkHost.java",
    };

    @Test
    void jdkHostCopiesAreIdentical() throws IOException {
        byte[] canonical = Files.readAllBytes(REPO_ROOT.resolve(JDK_HOST_COPIES[0]));
        for (int i = 1; i < JDK_HOST_COPIES.length; i++) {
            Path copy = REPO_ROOT.resolve(JDK_HOST_COPIES[i]);
            assertTrue(Files.isRegularFile(copy), "missing " + JDK_HOST_COPIES[i]);
            assertArrayEquals(canonical, Files.readAllBytes(copy),
                    JDK_HOST_COPIES[i] + " differs from " + JDK_HOST_COPIES[0]
                            + "; edit the JavaAPI copy and copy it over the other three");
        }
    }

    @Test
    void everyCldcMemberExistsInJavaApi() throws Exception {
        Path cldcClasses = Files.createTempDirectory("cldc-parity-cldc");
        Path javaApiClasses = Files.createTempDirectory("cldc-parity-javaapi");
        compile(CLDC_SRC, cldcClasses);
        compile(JAVAAPI_SRC, javaApiClasses);

        Map<String, ClassShape> cldc = load(cldcClasses);
        Map<String, ClassShape> javaApi = load(javaApiClasses);
        assertTrue(cldc.containsKey("java/lang/Object") && javaApi.containsKey("java/lang/Object"),
                "both class libraries should have compiled");

        TreeSet<String> violations = new TreeSet<String>();
        for (ClassShape c : cldc.values()) {
            if (!c.visible) {
                continue;
            }
            ClassShape j = javaApi.get(c.name);
            if (j == null) {
                violations.add("MISSING_CLASS " + c.name);
                continue;
            }
            for (Member m : c.members.values()) {
                Member found = m.constructor ? j.members.get(m.key) : find(javaApi, j, m.key, m.isStatic());
                if (found == null) {
                    violations.add("MISSING " + c.name + "#" + m.key);
                } else if (!atLeastAsVisible(found, m) || found.isStatic() != m.isStatic()) {
                    violations.add("DIFFERS " + c.name + "#" + m.key);
                }
            }
        }

        if (Boolean.getBoolean("cldcParity.writeBaseline")) {
            Files.write(BASELINE, header().concat(String.join("\n", violations)).concat("\n")
                    .getBytes(StandardCharsets.UTF_8));
            return;
        }

        TreeSet<String> baseline = readBaseline();
        List<String> added = new ArrayList<String>();
        for (String v : violations) {
            if (!baseline.contains(v)) {
                added.add(v);
            }
        }
        List<String> closed = new ArrayList<String>();
        for (String b : baseline) {
            if (!violations.contains(b)) {
                closed.add(b);
            }
        }
        if (!added.isEmpty()) {
            fail(added.size() + " CLDC11 API member(s) missing from vm/JavaAPI. Add them to "
                    + "JavaAPI (never to the baseline):\n  " + String.join("\n  ", added));
        }
        if (!closed.isEmpty()) {
            fail(closed.size() + " baseline entr(ies) no longer occur -- the gap was closed. "
                    + "Remove them from " + REPO_ROOT.relativize(BASELINE)
                    + " (or rerun with -DcldcParity.writeBaseline=true):\n  "
                    + String.join("\n  ", closed));
        }
    }

    private static String header() {
        return "# CLDC11 API members vm/JavaAPI does not provide. A RATCHET, not an\n"
                + "# allow-list: CldcJavaApiParityTest fails on a new entry and on a stale\n"
                + "# one. Close a gap by adding the member to JavaAPI, then delete its line.\n";
    }

    private static TreeSet<String> readBaseline() throws IOException {
        TreeSet<String> out = new TreeSet<String>();
        if (!Files.exists(BASELINE)) {
            return out;
        }
        for (String line : Files.readAllLines(BASELINE, StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (t.length() > 0 && !t.startsWith("#")) {
                out.add(t);
            }
        }
        return out;
    }

    private static boolean atLeastAsVisible(Member javaApi, Member cldc) {
        if ((javaApi.access & Opcodes.ACC_PUBLIC) != 0) {
            return true;
        }
        return (cldc.access & Opcodes.ACC_PROTECTED) != 0 && (javaApi.access & Opcodes.ACC_PROTECTED) != 0;
    }

    /** Looks {@code key} up in {@code start}, its superclasses and its interfaces. */
    private static Member find(Map<String, ClassShape> all, ClassShape start, String key, boolean isStatic) {
        List<ClassShape> work = new ArrayList<ClassShape>();
        work.add(start);
        java.util.Set<String> seen = new java.util.HashSet<String>();
        while (!work.isEmpty()) {
            ClassShape c = work.remove(0);
            if (!seen.add(c.name)) {
                continue;
            }
            Member m = c.members.get(key);
            // A static interface method is not inherited; anything else is.
            if (m != null && (c == start || !(c.isInterface && m.isStatic()))) {
                return m;
            }
            if (c.superName != null && all.containsKey(c.superName)) {
                work.add(all.get(c.superName));
            }
            for (String i : c.interfaces) {
                if (all.containsKey(i)) {
                    work.add(all.get(i));
                }
            }
        }
        return null;
    }

    private static void compile(Path srcRoot, Path out) throws IOException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertNotNull(javac, "the tests must run on a JDK, not a JRE");
        List<String> args = new ArrayList<String>();
        String spec = System.getProperty("java.specification.version");
        if (spec.startsWith("1.")) {
            args.addAll(Arrays.asList("-source", "1.8", "-target", "1.8"));
        } else {
            // No modules at release 8, so a source tree may declare java.lang.
            args.addAll(Arrays.asList("--release", "8"));
        }
        args.addAll(Arrays.asList("-nowarn", "-encoding", "UTF-8", "-proc:none",
                "-Xmaxerrs", "100000", "-d", out.toString()));
        try (Stream<Path> files = Files.walk(srcRoot)) {
            args.addAll(files.filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toString).sorted().collect(Collectors.toList()));
        }
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int rc = javac.run(null, null, err, args.toArray(new String[0]));
        assertEquals(0, rc, "compiling " + srcRoot + " failed:\n" + new String(err.toByteArray(), StandardCharsets.UTF_8));
    }

    private static Map<String, ClassShape> load(Path classesRoot) throws IOException {
        Map<String, ClassShape> out = new HashMap<String, ClassShape>();
        List<Path> files;
        try (Stream<Path> s = Files.walk(classesRoot)) {
            files = s.filter(p -> p.toString().endsWith(".class")).collect(Collectors.toList());
        }
        for (Path p : files) {
            try (InputStream in = Files.newInputStream(p)) {
                ClassShape shape = new ClassShape();
                new ClassReader(in).accept(shape, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                out.put(shape.name, shape);
            }
        }
        // A nested class is visible only when it and every class around it is.
        for (ClassShape c : out.values()) {
            c.visible = c.ownVisible;
            String outer = c.outerName;
            while (c.visible && outer != null) {
                ClassShape o = out.get(outer);
                c.visible = o != null && o.ownVisible;
                outer = o == null ? null : o.outerName;
            }
        }
        return out;
    }

    private static final class Member {
        final String key;
        final int access;
        final boolean constructor;

        Member(String key, int access, boolean constructor) {
            this.key = key;
            this.access = access;
            this.constructor = constructor;
        }

        boolean isStatic() {
            return (access & Opcodes.ACC_STATIC) != 0;
        }
    }

    private static final class ClassShape extends ClassVisitor {
        String name;
        String superName;
        List<String> interfaces = Collections.emptyList();
        boolean isInterface;
        boolean ownVisible;
        boolean visible;
        String outerName;
        final Map<String, Member> members = new LinkedHashMap<String, Member>();

        ClassShape() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.name = name;
            this.superName = superName;
            this.interfaces = interfaces == null ? Collections.<String>emptyList() : Arrays.asList(interfaces);
            this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
            this.ownVisible = (access & Opcodes.ACC_PUBLIC) != 0;
        }

        @Override
        public void visitInnerClass(String innerName, String outer, String simpleName, int access) {
            // The InnerClasses entry for this class itself carries its real
            // (source) access; the class-level flags of a nested class do not.
            if (innerName.equals(name)) {
                ownVisible = (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0;
                outerName = outer;
                if (outer == null) {
                    // A local or anonymous class.
                    ownVisible = false;
                }
            }
        }

        @Override
        public FieldVisitor visitField(int access, String fname, String descriptor, String signature, Object value) {
            if (exposed(access)) {
                members.put(fname + ":" + descriptor, new Member(fname + ":" + descriptor, access, false));
            }
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String mname, String descriptor, String signature, String[] exceptions) {
            if (exposed(access) && (access & Opcodes.ACC_BRIDGE) == 0 && !"<clinit>".equals(mname)) {
                members.put(mname + descriptor, new Member(mname + descriptor, access, "<init>".equals(mname)));
            }
            return null;
        }

        private static boolean exposed(int access) {
            return (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0
                    && (access & Opcodes.ACC_SYNTHETIC) == 0;
        }
    }
}
