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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// A desktop application's dependency jar that is itself written against
/// Swing: it has to be relocated with the application, and what it uses
/// beyond the device has to be reported as the library's.
public class CompatLibrariesTest {

    private static final String MAIN = "package com.acme.swingapp;\n"
            + "public class Main {\n"
            + "    public static void main(String[] args) { new org.fancy.FancyPanel().add(new javax.swing.JLabel()); }\n"
            + "}\n";

    private static final String PANEL = "package org.fancy;\n"
            + "public class FancyPanel extends javax.swing.JPanel {\n"
            + "    public String title() { return String.join(\"-\", \"fancy\", \"panel\"); }\n"
            + "}\n";

    /// As [#PANEL], and starting a process: nothing a device can do.
    private static final String BAD_PANEL = "package org.fancy;\n"
            + "public class FancyPanel extends javax.swing.JPanel {\n"
            + "    public Object launch() { return new ProcessBuilder(\"ls\"); }\n"
            + "}\n";

    /// As [#PANEL], with the corners a real Swing library has: it can save
    /// itself through long-term persistence, keeps a weak cache, and asks
    /// whether a component is one of AWT's heavyweight widgets.
    private static final String PERSISTENT_PANEL = "package org.fancy;\n"
            + "public class FancyPanel extends javax.swing.JPanel\n"
            + "        implements java.io.Externalizable, java.beans.ExceptionListener {\n"
            + "    private final java.util.WeakHashMap<Object, String> cache = new java.util.WeakHashMap<Object, String>();\n"
            + "    public boolean heavy(java.awt.Component c) {\n"
            + "        return c instanceof java.awt.Label || c instanceof java.awt.TextComponent\n"
            + "                || c instanceof java.awt.ScrollPane && ((java.awt.ScrollPane) c).getComponent(0) != null;\n"
            + "    }\n"
            + "    public void save(java.io.OutputStream out) {\n"
            + "        java.beans.XMLEncoder e = new java.beans.XMLEncoder(out);\n"
            + "        e.setExceptionListener(this);\n"
            + "        e.writeObject(this);\n"
            + "        e.close();\n"
            + "    }\n"
            + "    public boolean designing() throws Exception {\n"
            + "        java.beans.Introspector.getBeanInfo(FancyPanel.class, java.beans.Introspector.IGNORE_ALL_BEANINFO)\n"
            + "                .getBeanDescriptor().setValue(\"k\", \"v\");\n"
            + "        return java.beans.Beans.isDesignTime()\n"
            + "                || getClass().getClassLoader().loadClass(\"org.fancy.FancyPanel\") == null;\n"
            + "    }\n"
            + "    public void exceptionThrown(Exception x) { cache.put(x, \"x\"); }\n"
            + "    public void writeExternal(java.io.ObjectOutput out) throws java.io.IOException { out.writeInt(1); }\n"
            + "    public void readExternal(java.io.ObjectInput in) throws java.io.IOException { in.readInt(); }\n"
            + "}\n";

    /// An application that reaches for long-term persistence itself.
    private static final String PERSISTENT_MAIN = "package com.acme.swingapp;\n"
            + "public class Main {\n"
            + "    public static void main(String[] args) {\n"
            + "        new java.beans.XMLEncoder(new java.io.ByteArrayOutputStream()).close();\n"
            + "        System.out.println(new javax.swing.JLabel() instanceof java.awt.Component);\n"
            + "    }\n"
            + "}\n";

    private static final String PLAIN = "package org.plain;\n"
            + "public class Words {\n"
            + "    public static int count(String s) { return s.length(); }\n"
            + "}\n";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File scratch;
    private final List<String> logged = new ArrayList<String>();

    private final com.codename1.build.Log log = new com.codename1.build.Log() {
        public void debug(CharSequence c) { }
        public void debug(CharSequence c, Throwable e) { }
        public void debug(Throwable e) { }
        public void info(CharSequence c) { logged.add("info: " + c); }
        public void info(CharSequence c, Throwable e) { }
        public void info(Throwable e) { }
        public void warn(CharSequence c) { logged.add("warn: " + c); }
        public void warn(CharSequence c, Throwable e) { }
        public void warn(Throwable e) { }
        public void error(CharSequence c) { }
        public void error(CharSequence c, Throwable e) { }
        public void error(Throwable e) { }
        public boolean isDebugEnabled() { return false; }
        public boolean isInfoEnabled() { return true; }
        public boolean isWarnEnabled() { return true; }
        public boolean isErrorEnabled() { return true; }
    };

    private List<File> runtimes() throws Exception {
        if (scratch == null) {
            scratch = tmp.newFolder("jars");
        }
        return new ArrayList<File>(Arrays.asList(RealCompatJars.swing(scratch), RealCompatJars.javafx(scratch),
                RealCompatJars.jdk(scratch), RealCompatJars.core(scratch)));
    }

    /// Compiles one class against the layers and packs it as `name`, with a
    /// manifest and a resource beside it as a published jar has.
    private File library(String name, String path, String source) throws Exception {
        File classes = tmp.newFolder();
        CompatFixtures.compileAgainst(runtimes(), tmp.newFolder(), classes, path, source);
        String cls = path.replace(".java", ".class");
        return CompatRemapperTest.jar(new File(tmp.newFolder(), name),
                "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n".getBytes("UTF-8"),
                cls, Files.readAllBytes(new File(classes, cls).toPath()),
                cls.substring(0, cls.lastIndexOf('/')) + "/notes.properties", "a=b\n".getBytes("UTF-8"));
    }

    private File application(File... libraries) throws Exception {
        return application(MAIN, libraries);
    }

    private File application(String main, File... libraries) throws Exception {
        List<File> cp = runtimes();
        cp.addAll(Arrays.asList(libraries));
        File classes = tmp.newFolder();
        CompatFixtures.compileAgainst(cp, tmp.newFolder(), classes, "com/acme/swingapp/Main.java", main);
        return classes;
    }

    private CompatRemapper remapper(File classes, List<File> shipped, File... onClasspath) throws Exception {
        List<File> cp = runtimes();
        cp.addAll(Arrays.asList(onClasspath));
        File dir = tmp.newFolder();
        File record = DesktopSources.entryRecord(dir);
        Files.write(record.toPath(), "mainClass=com.acme.swingapp.Main\nkind=swing\n".getBytes("UTF-8"));
        return new CompatRemapper(classes, cp, null, log).withDesktopEntryRecord(record)
                .withApplicationMain("com.acme.MyApp").withApplicationLibraries(shipped);
    }

    private static String superName(File cls) throws Exception {
        return new ClassReader(Files.readAllBytes(cls.toPath())).getSuperName();
    }

    private TestProjectHost host(File classes, File... compileJars) throws Exception {
        TestProjectHost host = RealCompatJars.host(classes, tmp.newFolder(), scratch);
        for (File jar : compileJars) {
            host.artifacts.add(new BuildArtifact("org.example", jar.getName().replace(".jar", ""), "1", null, "jar",
                    "compile", jar, null));
        }
        return host;
    }

    @Test
    public void aLibraryWrittenAgainstSwingIsRelocatedWithTheApplication() throws Exception {
        File fancy = library("fancy-lib-1.0.jar", "org/fancy/FancyPanel.java", PANEL);
        File plain = library("plain-lib-1.0.jar", "org/plain/Words.java", PLAIN);
        File classes = application(fancy, plain);
        assertTrue(remapper(classes, Arrays.asList(fancy, plain), fancy, plain).run());

        File panel = new File(classes, "org/fancy/FancyPanel.class");
        assertEquals("com/codename1/desktopcompat/javax/swing/JPanel", superName(panel));
        assertTrue("A library's resources come with it", new File(classes, "org/fancy/notes.properties").isFile());
        assertFalse("The jar's manifest is not the application's",
                new File(classes, "META-INF/MANIFEST.MF").exists());
        // What a device lacks is redirected in the library as in the application.
        assertTrue(CompatFixtures.members(Files.readAllBytes(panel.toPath()))
                .contains("com/codename1/compat/jdk/JdkStrings.join"));
        assertFalse("A library that names no desktop toolkit stays a dependency",
                new File(classes, "org/plain/Words.class").exists());
        assertTrue(logged.toString(), logged.toString().contains(
                "info: Bundling fancy-lib-1.0.jar with the application (1 classes, 1 KB): it is written against"));
        assertFalse(logged.toString(), logged.toString().contains("plain-lib"));

        assertEquals(Collections.singleton("fancy-lib-1.0.jar"),
                CompatLibraries.bundledJarNames(Collections.singletonList(classes)));
        assertEquals(Collections.singletonMap("org/fancy/FancyPanel", "fancy-lib-1.0.jar"),
                CompatLibraries.classOrigins(classes));
        // The relocated copy is the one the check reads, although the jar is
        // still among the project's dependencies.
        new BytecodeCompliance(host(classes, fancy, plain)).execute();

        // The record is found in a jar built from the classes as well: that
        // is where the module assembling the application looks for it.
        File built = CompatRemapperTest.jar(new File(tmp.newFolder(), "common-1.0.jar"), CompatLibraries.RECORD,
                Files.readAllBytes(new File(classes, CompatLibraries.RECORD).toPath()));
        assertEquals(Collections.singleton("fancy-lib-1.0.jar"),
                CompatLibraries.bundledJarNames(Arrays.asList(built, plain, new File("no-such.jar"))));
    }

    @Test
    public void aSecondRunLeavesAnUnchangedLibraryAlone() throws Exception {
        File fancy = library("fancy-lib-1.0.jar", "org/fancy/FancyPanel.java", PANEL);
        File classes = application(fancy);
        assertTrue(remapper(classes, Collections.singletonList(fancy), fancy).run());
        File panel = new File(classes, "org/fancy/FancyPanel.class");
        byte[] relocated = Files.readAllBytes(panel.toPath());
        assertTrue(panel.setLastModified(1000000L));
        logged.clear();

        assertTrue(remapper(classes, Collections.singletonList(fancy), fancy).run());
        assertEquals(1000000L, panel.lastModified());
        assertTrue(Arrays.equals(relocated, Files.readAllBytes(panel.toPath())));
        assertFalse(logged.toString(), logged.toString().contains("Bundling"));

        // A new build of the library replaces the copy an earlier run made.
        File newer = library("fancy-lib-1.0.jar", "org/fancy/FancyPanel.java", BAD_PANEL);
        assertTrue(remapper(classes, Collections.singletonList(newer), newer).run());
        assertTrue(logged.toString(), logged.toString().contains("Bundling fancy-lib-1.0.jar"));
        assertTrue(CompatFixtures.members(Files.readAllBytes(panel.toPath())).toString(),
                CompatFixtures.members(Files.readAllBytes(panel.toPath())).contains("java/lang/ProcessBuilder.<init>"));
        assertEquals("com/codename1/desktopcompat/javax/swing/JPanel", superName(panel));
    }

    @Test
    public void aProvidedLibraryIsNotBundled() throws Exception {
        File fancy = library("fancy-lib-1.0.jar", "org/fancy/FancyPanel.java", PANEL);
        File classes = application(fancy);
        // On the classpath, and not among what the application ships.
        assertTrue(remapper(classes, Collections.<File>emptyList(), fancy).run());
        assertFalse(new File(classes, "org/fancy/FancyPanel.class").exists());
        assertFalse(new File(classes, CompatLibraries.RECORD).exists());
        assertTrue(CompatLibraries.bundledJarNames(Collections.singletonList(classes)).isEmpty());
    }

    @Test
    public void theApplicationsOwnClassWinsOverTheLibrarys() throws Exception {
        File fancy = library("fancy-lib-1.0.jar", "org/fancy/FancyPanel.java", BAD_PANEL);
        File classes = application(fancy);
        // The application ships a patched copy of the library's class.
        CompatFixtures.compileAgainst(runtimes(), tmp.newFolder(), classes, "org/fancy/FancyPanel.java", PANEL);
        assertTrue(remapper(classes, Collections.singletonList(fancy), fancy).run());
        assertFalse(CompatFixtures.members(Files.readAllBytes(new File(classes, "org/fancy/FancyPanel.class")
                .toPath())).contains("java/lang/ProcessBuilder.<init>"));
        assertTrue(CompatLibraries.classOrigins(classes).isEmpty());
        new BytecodeCompliance(host(classes, fancy)).execute();
    }

    /// MigLayout is the library this stands for: every class of it loads,
    /// and the paths that save a layout as XML are never taken on a device.
    @Test
    public void aLibraryLinksAgainstApiNoDeviceCanServe() throws Exception {
        File fancy = library("fancy-lib-1.0.jar", "org/fancy/FancyPanel.java", PERSISTENT_PANEL);
        File classes = application(fancy);
        assertTrue(remapper(classes, Collections.singletonList(fancy), fancy).run());
        byte[] panel = Files.readAllBytes(new File(classes, "org/fancy/FancyPanel.class").toPath());
        assertTrue(CompatFixtures.members(panel).toString(),
                CompatFixtures.members(panel).contains("com/codename1/compat/jdk/Resources.loadClass"));
        assertTrue(CompatFixtures.members(panel).toString(),
                CompatFixtures.members(panel).contains("com/codename1/compat/jdk/ObjectOutput.writeInt"));
        // No finding: the library names link-only classes, and it may.
        new BytecodeCompliance(host(classes, fancy)).execute();
    }

    /// The same classes are no API of the layer as far as the developer's
    /// own code goes: there they would be calls that compile and then fail.
    @Test
    public void theApplicationItselfMayNotUseLinkOnlyApi() throws Exception {
        File fancy = library("fancy-lib-1.0.jar", "org/fancy/FancyPanel.java", PERSISTENT_PANEL);
        File classes = application(PERSISTENT_MAIN, fancy);
        assertTrue(remapper(classes, Collections.singletonList(fancy), fancy).run());
        try {
            new BytecodeCompliance(host(classes, fancy)).execute();
            fail("java.beans.XMLEncoder cannot work on a device");
        } catch (Exception e) {
            String message = e.getMessage();
            assertTrue(message, message.contains(
                    "java.beans.XMLEncoder is not supported by the Codename One Swing compatibility layer"));
            assertFalse("The library's own use is not a finding: " + message, message.contains("fancy-lib-1.0.jar"));
            assertFalse(message, message.contains("java.awt.Component is not supported"));
        }
    }

    @Test
    public void whatALibraryUsesBeyondTheDeviceIsReportedAsTheLibrarys() throws Exception {
        File fancy = library("fancy-lib-1.0.jar", "org/fancy/FancyPanel.java", BAD_PANEL);
        File classes = application(fancy);
        assertTrue(remapper(classes, Collections.singletonList(fancy), fancy).run());
        try {
            new BytecodeCompliance(host(classes, fancy)).execute();
            fail("ProcessBuilder is not on a device");
        } catch (Exception e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("java/lang/ProcessBuilder"));
            assertTrue(message, message.contains("org/fancy/FancyPanel") || message.contains("org.fancy.FancyPanel"));
            assertTrue(message, message.contains(
                    "in the library fancy-lib-1.0.jar, which is bundled and relocated with the application"));
        }
    }

    @Test
    public void aLargeLibraryIsWarnedAbout() throws Exception {
        File fancy = library("fancy-lib-1.0.jar", "org/fancy/FancyPanel.java", PANEL);
        File big = new File(tmp.newFolder(), "big-lib-2.jar");
        CompatRemapperTest.jar(big, "org/fancy/FancyPanel.class", readEntry(fancy, "org/fancy/FancyPanel.class"),
                "org/fancy/blob.bin", noise((int) CompatLibraries.LARGE + 4096));
        File classes = application(fancy);
        assertTrue(remapper(classes, Collections.singletonList(big), big).run());
        assertTrue(logged.toString(), logged.toString().contains("warn: Bundling big-lib-2.jar"));
        assertTrue(logged.toString(), logged.toString().contains("large library"));
    }

    private static byte[] readEntry(File jar, String name) throws Exception {
        java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar);
        try {
            java.io.InputStream in = zip.getInputStream(zip.getEntry(name));
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            int b = in.read();
            while (b >= 0) {
                out.write(b);
                b = in.read();
            }
            return out.toByteArray();
        } finally {
            zip.close();
        }
    }

    /// Bytes a zip cannot shrink, so the jar is as large as its entry.
    private static byte[] noise(int length) {
        byte[] out = new byte[length];
        new java.util.Random(7).nextBytes(out);
        return out;
    }
}
