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

import com.codename1.build.BuildArtifact;
import com.codename1.build.BuildFailureException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The port report of a desktop application, end to end: a small Swing
/// application with two libraries is relocated and checked, and what the
/// check writes is compared with the report kept beside this test.
public class DesktopPortReportTest {

    private static final String MAIN = "package com.acme.port;\n"
            + "public class Main {\n"
            + "    public static void main(String[] args) {\n"
            + "        javax.swing.JFrame f = new javax.swing.JFrame(\"Port\");\n"
            + "        f.add(new org.fancy.FancyPanel());\n"
            + "        f.add(new javax.swing.JLabel(String.valueOf(org.plain.Words.count(new Clean().title()))));\n"
            + "        f.setVisible(true);\n"
            + "    }\n"
            + "}\n";

    private static final String CLEAN = "package com.acme.port;\n"
            + "public class Clean {\n"
            + "    String title() { return \"ok\"; }\n"
            + "}\n";

    private static final String GIT = "package com.acme.port;\n"
            + "public class Git {\n"
            + "    public Process status() throws java.io.IOException {\n"
            + "        return new ProcessBuilder(\"git\", \"status\").start();\n"
            + "    }\n"
            + "    public Process log() throws java.io.IOException {\n"
            + "        return new ProcessBuilder(\"git\", \"log\").start();\n"
            + "    }\n"
            + "    public java.sql.Connection index() throws java.sql.SQLException {\n"
            + "        return java.sql.DriverManager.getConnection(\"jdbc:index\");\n"
            + "    }\n"
            + "    public java.net.Socket remote() throws java.io.IOException {\n"
            + "        return new java.net.Socket(\"example.com\", 9418);\n"
            + "    }\n"
            + "}\n";

    private static final String TRAY = "package com.acme.port;\n"
            + "public class Tray {\n"
            + "    void show() throws Exception {\n"
            + "        java.awt.SystemTray.getSystemTray();\n"
            + "        new java.awt.Robot().delay(1);\n"
            + "    }\n"
            + "}\n";

    private static final String PANEL = "package org.fancy;\n"
            + "public class FancyPanel extends javax.swing.JPanel {\n"
            + "    public String title() { return \"fancy\"; }\n"
            + "}\n";

    /// As [#PANEL], and starting a process: nothing a device can do.
    private static final String BAD_PANEL = "package org.fancy;\n"
            + "public class FancyPanel extends javax.swing.JPanel {\n"
            + "    public Object launch() { return new ProcessBuilder(\"ls\"); }\n"
            + "}\n";

    private static final String WORDS = "package org.plain;\n"
            + "public class Words {\n"
            + "    public static int count(String s) { return s.length(); }\n"
            + "}\n";

    private static final String UNUSED = "package org.unused;\n"
            + "public class Unused {\n"
            + "}\n";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File scratch;

    private List<File> runtimes() throws Exception {
        if (scratch == null) {
            scratch = tmp.newFolder("jars");
        }
        return new ArrayList<File>(Arrays.asList(RealCompatJars.swing(scratch), RealCompatJars.javafx(scratch),
                RealCompatJars.jdk(scratch), RealCompatJars.core(scratch)));
    }

    private File library(String name, String path, String source) throws Exception {
        File classes = tmp.newFolder();
        CompatFixtures.compileAgainst(runtimes(), tmp.newFolder(), classes, path, source);
        String cls = path.replace(".java", ".class");
        return CompatRemapperTest.jar(new File(tmp.newFolder(), name), cls,
                Files.readAllBytes(new File(classes, cls).toPath()));
    }

    /// The application, relocated; `sources` are what it is made of beside
    /// its main class.
    private TestProjectHost application(String... sources) throws Exception {
        return applicationWith(PANEL, sources);
    }

    private TestProjectHost applicationWith(String panel, String... sources) throws Exception {
        File fancy = library("fancy-lib-1.0.jar", "org/fancy/FancyPanel.java", panel);
        File plain = library("plain-lib-1.0.jar", "org/plain/Words.java", WORDS);
        File unused = library("unused-lib-1.0.jar", "org/unused/Unused.java", UNUSED);
        List<File> cp = runtimes();
        cp.addAll(Arrays.asList(fancy, plain, unused));
        File classes = tmp.newFolder();
        List<String> all = new ArrayList<String>(Arrays.asList("com/acme/port/Main.java", MAIN,
                "com/acme/port/Clean.java", CLEAN));
        all.addAll(Arrays.asList(sources));
        CompatFixtures.compileAgainst(cp, tmp.newFolder(), classes, all.toArray(new String[0]));
        File dir = tmp.newFolder();
        File record = DesktopSources.entryRecord(dir);
        Files.write(record.toPath(), "mainClass=com.acme.port.Main\nkind=swing\n".getBytes("UTF-8"));
        assertTrue(new CompatRemapper(classes, cp, null, CompatRemapperTest.LOG).withDesktopEntryRecord(record)
                .withApplicationMain("com.acme.MyApp").withApplicationLibraries(Arrays.asList(fancy, plain, unused))
                .run());
        TestProjectHost host = RealCompatJars.host(classes, tmp.newFolder(), scratch);
        host.projectDir = tmp.newFolder();
        for (File jar : new File[] {fancy, plain, unused}) {
            host.artifacts.add(new BuildArtifact("org.example", jar.getName().replace(".jar", ""), "1", null, "jar",
                    "compile", jar, null));
        }
        return host;
    }

    private static String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), "UTF-8");
    }

    private static File defaultReport(TestProjectHost host) {
        return new File(host.buildDir, "codenameone/desktop-port-report.md");
    }

    private static String golden(String name) throws Exception {
        InputStream in = DesktopPortReportTest.class.getResourceAsStream(name);
        assertNotNull(name + " is kept beside this test", in);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(DependencyClassifier.readAll(in));
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    /// Compares with the kept report, leaving what was written where a
    /// developer can read it and, if it is right, keep it.
    private static void assertGolden(String name, String actual) throws Exception {
        File out = new File("target/port-report-actual/" + name);
        out.getParentFile().mkdirs();
        Files.write(out.toPath(), actual.getBytes("UTF-8"));
        assertEquals("The report differs from src/test/resources/com/codename1/maven/" + name + "; what was written "
                + "is in " + out, golden(name), actual);
    }

    @Test
    public void anApplicationThatPassesGetsAReportToo() throws Exception {
        TestProjectHost host = application();
        new BytecodeCompliance(host).execute();
        String report = read(defaultReport(host));
        assertGolden("desktop-port-report-clean.md", report);
        assertTrue(report, report.contains("- The compliance check passes: "));
    }

    @Test
    public void theReportSaysWhatToChangeFileByFile() throws Exception {
        TestProjectHost host = applicationWith(BAD_PANEL, "com/acme/port/Git.java", GIT, "com/acme/port/Tray.java",
                TRAY);
        String message;
        try {
            new BytecodeCompliance(host).execute();
            fail("The application starts processes");
            return;
        } catch (BuildFailureException expected) {
            message = expected.getMessage();
        }
        File file = defaultReport(host);
        String report = read(file);
        assertGolden("desktop-port-report.md", report);
        // More findings than fit a console: the failure summarizes them and
        // says where the rest is.
        assertTrue(message, message.contains("What to change, file by file: " + file.getAbsolutePath()));
        assertTrue(message, message.contains("  The application: "));
        assertTrue(message, message.contains("\n  fancy-lib-1.0.jar: 2 call sites, 2 APIs -- processes 2"));
        String check = read(new File(host.buildDir, "codenameone/compliance_check.txt"));
        assertTrue(check, check.contains("The port report, which lists the same findings by source file, is "
                + file.getAbsolutePath()));

        // The same application is the same report: nothing in it depends on
        // where or when it was built.
        TestProjectHost again = applicationWith(BAD_PANEL, "com/acme/port/Git.java", GIT, "com/acme/port/Tray.java",
                TRAY);
        try {
            new BytecodeCompliance(again).execute();
            fail();
        } catch (BuildFailureException expected) {
            assertEquals(report, read(defaultReport(again)));
        }
    }

    @Test
    public void theReportGoesWhereItIsAskedFor() throws Exception {
        TestProjectHost host = application();
        // A relative path is the project's, as a build file would mean it.
        host.userProperties.setProperty(DesktopPortReport.PROPERTY, "docs/port.md");
        new BytecodeCompliance(host).execute();
        assertTrue(new File(host.projectDir, "docs/port.md").isFile());
        assertFalse(defaultReport(host).exists());
        // What a plugin was given as a parameter wins over the property.
        File chosen = new File(tmp.newFolder(), "chosen.md");
        new BytecodeCompliance(host).portReport(chosen).execute();
        assertTrue(chosen.isFile());
        assertEquals(read(new File(host.projectDir, "docs/port.md")), read(chosen));
    }

    /// A class with one method whose body is an `invokedynamic` through
    /// `owner`.
    private static byte[] indy(int version, String name, String owner, String bootstrap) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(version, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        cw.visitSource(name.substring(name.lastIndexOf('/') + 1) + ".java", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        org.objectweb.asm.Label start = new org.objectweb.asm.Label();
        mv.visitLabel(start);
        mv.visitLineNumber(42, start);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInvokeDynamicInsn("toString", "(L" + name + ";)Ljava/lang/String;", new Handle(Opcodes.H_INVOKESTATIC,
                owner, bootstrap, "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                + "Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;"
                + "[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;", false), org.objectweb.asm.Type.getObjectType(name),
                "");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void put(File classes, String name, byte[] bytes) throws Exception {
        File f = new File(classes, name + ".class");
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), bytes);
    }

    @Test
    public void whatInvokedynamicCannotBeBuiltAheadOfTimeIsAFinding() throws Exception {
        TestProjectHost host = application();
        // Not the bootstrap method javac calls, so not one the build rewrites.
        put(host.outputDir, "com/acme/port/Point", indy(Opcodes.V17, "com/acme/port/Point",
                "java/lang/runtime/ObjectMethods", "another"));
        put(host.outputDir, "com/acme/port/Odd", indy(Opcodes.V17, "com/acme/port/Odd", "org/odd/Bootstraps",
                "make"));
        try {
            new BytecodeCompliance(host).execute();
            fail("Neither bootstrap exists on a device");
        } catch (BuildFailureException expected) {
            String message = expected.getMessage();
            assertTrue(message, message.contains("a record's generated equals, hashCode and toString "
                    + "(invokedynamic through java.lang.runtime.ObjectMethods) is not supported on a device"));
            assertTrue(message, message.contains("this class was not rewritten"));
            assertTrue(message, message.contains("invokedynamic through org.odd.Bootstraps.make is not supported "
                    + "on a device"));
        }
        String report = read(defaultReport(host));
        assertTrue(report, report.contains("## What cannot run on a device"));
        assertTrue(report, report.contains("java.lang.runtime.ObjectMethods"));
    }

    /// A record of a project that uses no layer is given its methods by the
    /// check itself, since no remap went over it.
    @Test
    public void aPlainProjectsRecordIsRewrittenToo() throws Exception {
        File classes = tmp.newFolder();
        put(classes, "com/acme/plain/Point", indy(Opcodes.V17, "com/acme/plain/Point",
                "java/lang/runtime/ObjectMethods", "bootstrap"));
        scratch = tmp.newFolder("jars");
        // Both layers are among its dependencies, as they are in every
        // generated project, and none of its classes uses either.
        TestProjectHost host = RealCompatJars.host(classes, tmp.newFolder(), scratch);
        new BytecodeCompliance(host).execute();
        assertFalse("Nothing to port, so no port report", defaultReport(host).exists());
        final boolean[] dynamic = new boolean[1];
        new org.objectweb.asm.ClassReader(Files.readAllBytes(new File(classes, "com/acme/plain/Point.class").toPath()))
                .accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                            String[] exceptions) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrap,
                                    Object... arguments) {
                                dynamic[0] = true;
                            }
                        };
                    }
                }, 0);
        assertFalse("The record's toString is a plain method now", dynamic[0]);
    }

    /// A bootstrap nobody implements fails every project, with a layer or
    /// without, and the failure says where it is.
    @Test
    public void anUnknownBootstrapFailsAPlainProjectAndSaysWhere() throws Exception {
        File classes = tmp.newFolder();
        put(classes, "com/acme/plain/Odd", indy(Opcodes.V17, "com/acme/plain/Odd", "org/odd/Bootstraps", "make"));
        scratch = tmp.newFolder("jars");
        TestProjectHost host = RealCompatJars.host(classes, tmp.newFolder(), scratch);
        try {
            new BytecodeCompliance(host).execute();
            fail("org.odd.Bootstraps exists on no device");
        } catch (BuildFailureException expected) {
            String message = expected.getMessage();
            assertTrue(message, message.contains("invokedynamic through org.odd.Bootstraps.make is not supported "
                    + "on a device"));
            assertTrue(message, message.contains("com/acme/plain/Odd"));
            assertTrue(message, message.contains("toString"));
            assertTrue(message, message.contains("Odd.java:42"));
        }
    }

    @Test
    public void aClassFileNewerThanTheBuildIsRewrittenAndCheckedLikeAnyOther() throws Exception {
        TestProjectHost host = application();
        // Java 21 class files: one that is fine, one that calls an API Java
        // 21 added.
        put(host.outputDir, "com/acme/port/Fine", DependencyClassifierTest.cls(Opcodes.V21, "com/acme/port/Fine",
                DependencyClassifierTest.uses()));
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/acme/port/Newer", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "last",
                "(Ljava/util/List;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "getLast", "()Ljava/lang/Object;", true);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();
        put(host.outputDir, "com/acme/port/Newer", cw.toByteArray());
        try {
            new BytecodeCompliance(host).execute();
            fail("List.getLast() is Java 21's");
        } catch (BuildFailureException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("getLast"));
        }
        byte[] rewritten = Files.readAllBytes(new File(host.outputDir, "com/acme/port/Newer.class").toPath());
        assertEquals("The class file is Java 17's now", Opcodes.V17, rewritten[7] & 0xff);
        String check = read(new File(host.buildDir, "codenameone/compliance_check.txt"));
        assertTrue(check, check.contains("Rewritten class files to Java 17 major version: 2"));
        assertTrue(check, check.contains("2 class file(s) were compiled for a Java newer than 17 (up to Java 21) and "
                + "were rewritten to the Java 17 class format. What they call, by instruction or by invokedynamic, is checked like any other class's."));
        assertTrue(read(defaultReport(host)), read(defaultReport(host)).contains("Java 21"));
    }
}
