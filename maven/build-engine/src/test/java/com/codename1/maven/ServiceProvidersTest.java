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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// `ServiceLoader` on a device, end to end: an application and a library
/// jar that list providers in `META-INF/services` are compiled by javac,
/// remapped, checked for compliance, and then RUN from the remapped classes
/// alone -- no services file is on that class path, so every provider the
/// application sees came from the registry the build generated.
public class ServiceProvidersTest {

    private static final String CODEC = "package org.codecs;\n"
            + "public interface Codec { String name(); }\n";

    private static final String UNUSED = "package org.codecs;\n"
            + "public interface Unused { }\n";

    /// What the application calls; the only class of the library it names.
    private static final String CODECS = "package org.codecs;\n"
            + "public class Codecs {\n"
            + "    public static final java.util.List<String> MADE = new java.util.ArrayList<String>();\n"
            + "    public static String all() {\n"
            + "        StringBuilder sb = new StringBuilder();\n"
            + "        java.util.Iterator<Codec> it = java.util.ServiceLoader.load(Codec.class).iterator();\n"
            + "        sb.append(it.hasNext()).append(MADE);\n"
            + "        while (it.hasNext()) {\n"
            + "            sb.append(' ').append(it.next().name()).append(MADE.size());\n"
            + "        }\n"
            + "        return sb.toString();\n"
            + "    }\n"
            + "    public static String resource(String name) throws java.io.IOException {\n"
            + "        java.io.InputStream in = Codecs.class.getResourceAsStream(name);\n"
            + "        if (in == null) { return \"none\"; }\n"
            + "        StringBuilder sb = new StringBuilder();\n"
            + "        for (int c = in.read(); c >= 0; c = in.read()) { sb.append((char) c); }\n"
            + "        in.close();\n"
            + "        return sb.toString();\n"
            + "    }\n"
            + "}\n";

    private static String provider(String pkg, String name, String modifiers, String constructor, String answer) {
        return "package " + pkg + ";\n"
                + "public " + modifiers + "class " + name + " implements org.codecs.Codec {\n"
                + "    " + constructor + " " + name + "() { org.codecs.Codecs.MADE.add(\"" + answer + "\"); }\n"
                + "    public String name() { return \"" + answer + "\"; }\n"
                + "}\n";
    }

    private static final String MAIN = "package com.acme.swingapp;\n"
            + "public class Main {\n"
            + "    public static void main(String[] args) { new javax.swing.JLabel(\"x\"); }\n"
            + "    public static String report() throws Exception {\n"
            + "        int listeners = 0;\n"
            + "        for (java.awt.event.ActionListener l\n"
            + "                : java.util.ServiceLoader.load(java.awt.event.ActionListener.class)) {\n"
            + "            listeners += l instanceof Clicker ? 1 : 100;\n"
            + "        }\n"
            + "        return org.codecs.Codecs.all() + \"|\" + listeners + \"|\"\n"
            + "                + org.codecs.Codecs.resource(\"table.txt\") + \"|\"\n"
            + "                + org.codecs.Codecs.resource(\"/META-INF/resources/codecs/glyphs.txt\") + \"|\"\n"
            + "                + java.util.ServiceLoader.load(Runnable.class).iterator().hasNext();\n"
            + "    }\n"
            + "}\n";

    private static final String CLICKER = "package com.acme.swingapp;\n"
            + "public class Clicker implements java.awt.event.ActionListener {\n"
            + "    public void actionPerformed(java.awt.event.ActionEvent e) { }\n"
            + "}\n";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File scratch;
    private final List<String> logged = new ArrayList<String>();

    private final com.codename1.build.Log log = new com.codename1.build.Log() {
        public void debug(CharSequence c) { }
        public void debug(CharSequence c, Throwable e) { }
        public void debug(Throwable e) { }
        public void info(CharSequence c) { }
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

    /// The library: a service, the class that loads it, and providers of
    /// which two can be created, one is abstract, one has no public
    /// constructor and one is listed but not in the jar.
    private File library() throws Exception {
        File out = tmp.newFolder();
        CompatFixtures.compileAgainst(runtimes(), tmp.newFolder(), out,
                "org/codecs/Codec.java", CODEC, "org/codecs/Unused.java", UNUSED, "org/codecs/Codecs.java", CODECS,
                "org/codecs/impl/Hex.java", provider("org.codecs.impl", "Hex", "", "public", "hex"),
                "org/codecs/impl/Base64.java", provider("org.codecs.impl", "Base64", "", "public", "base64"),
                "org/codecs/impl/Half.java", provider("org.codecs.impl", "Half", "abstract ", "public", "half"),
                "org/codecs/impl/Hidden.java", provider("org.codecs.impl", "Hidden", "", "", "hidden"),
                "org/codecs/impl/Spare.java", provider("org.codecs.impl", "Spare", "", "public", "spare"));
        List<Object> entries = new ArrayList<Object>();
        for (String name : new String[] {"Codec", "Unused", "Codecs", "impl/Hex", "impl/Base64", "impl/Half",
            "impl/Hidden", "impl/Spare"}) {
            entries.add("org/codecs/" + name + ".class");
            entries.add(Files.readAllBytes(new File(out, "org/codecs/" + name + ".class").toPath()));
        }
        entries.add("META-INF/services/org.codecs.Codec");
        entries.add(("# the codecs of this library\n\n  org.codecs.impl.Hex   # the first\n"
                + "org.codecs.impl.Gone\norg.codecs.impl.Half\n\t\norg.codecs.impl.Base64\n"
                + "org.codecs.impl.Hidden\norg.codecs.impl.Hex\n").getBytes("UTF-8"));
        // A service nothing names: its provider is not reached and not missed.
        entries.add("META-INF/services/org.codecs.Unused");
        entries.add("org.codecs.impl.Spare\norg.codecs.impl.AlsoGone\n".getBytes("UTF-8"));
        entries.add("org/codecs/table.txt");
        entries.add("0123".getBytes("UTF-8"));
        entries.add("META-INF/resources/codecs/glyphs.txt");
        entries.add("glyphs".getBytes("UTF-8"));
        entries.add("META-INF/MANIFEST.MF");
        entries.add("Manifest-Version: 1.0\n".getBytes("UTF-8"));
        return CompatRemapperTest.jar(new File(tmp.newFolder(), "codecs-1.0.jar"), entries.toArray());
    }

    private CompatRemapper remapper(File classes, File resources, File lib) throws Exception {
        List<File> cp = runtimes();
        cp.add(lib);
        File record = DesktopSources.entryRecord(tmp.newFolder());
        Files.write(record.toPath(), "mainClass=com.acme.swingapp.Main\nkind=swing\n".getBytes("UTF-8"));
        return new CompatRemapper(classes, cp, null, log).withDesktopEntryRecord(record)
                .withApplicationMain("com.acme.MyApp").withResourceDirectories(Collections.singletonList(resources))
                .withApplicationLibraries(Collections.singletonList(lib));
    }

    /// Calls the application's `report()` in a loader that holds the
    /// remapped classes, the headless implementation and the framework,
    /// and nothing of this JVM's class path.
    private String report(File classes) throws Exception {
        URL[] urls = {
            classes.toURI().toURL(),
            RealCompatJars.jar("codenameone-compat-testing", scratch).toURI().toURL(),
            RealCompatJars.core(scratch).toURI().toURL(),
        };
        URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader().getParent());
        try {
            loader.loadClass("com.codename1.compat.testing.HeadlessImplementation").getMethod("install").invoke(null);
            return (String) loader.loadClass("com.acme.swingapp.Main").getMethod("report").invoke(null);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        } finally {
            loader.close();
        }
    }

    @Test
    public void theProvidersAServicesFileListsAreCreatedByTheGeneratedRegistry() throws Exception {
        File lib = library();
        List<File> cp = runtimes();
        cp.add(lib);
        File classes = tmp.newFolder();
        CompatFixtures.compileAgainst(cp, tmp.newFolder(), classes, "com/acme/swingapp/Main.java", MAIN,
                "com/acme/swingapp/Clicker.java", CLICKER,
                "com/acme/swingapp/Rot13.java", provider("com.acme.swingapp", "Rot13", "", "public", "rot13"));
        // The application's own services files, as its build copies them.
        File resources = tmp.newFolder();
        String[][] own = {
            {"META-INF/services/org.codecs.Codec", "com.acme.swingapp.Rot13\n"},
            // A service the remap moves: the file names the JDK's interface.
            {"META-INF/services/java.awt.event.ActionListener", "com.acme.swingapp.Clicker\n"},
        };
        for (String[] file : own) {
            for (File root : new File[] {resources, classes}) {
                File f = new File(root, file[0]);
                assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
                Files.write(f.toPath(), file[1].getBytes("UTF-8"));
            }
        }

        assertTrue(remapper(classes, resources, lib).run());

        // A provider is named by no instruction: the services file of a
        // service the application reaches is what reaches it.
        assertTrue(new File(classes, "org/codecs/impl/Hex.class").isFile());
        assertTrue(new File(classes, "org/codecs/impl/Base64.class").isFile());
        assertFalse("A provider of a service nothing names stays out",
                new File(classes, "org/codecs/impl/Spare.class").exists());
        assertFalse("No services file is left for a class path search a device does not have",
                new File(classes, "META-INF/services").exists());
        assertTrue("A library's resources ship under their flat names",
                new File(classes, "org__codecs__table.txt").isFile());
        assertTrue(new File(classes, "META-INF__resources__codecs__glyphs.txt").isFile());
        assertFalse(new File(classes, "org/codecs/table.txt").exists());

        // One warning for each listed provider that cannot be created,
        // naming the file and the class, and the build went on.
        List<String> warnings = new ArrayList<String>();
        for (String line : logged) {
            if (line.contains("service provider")) {
                warnings.add(line);
            }
        }
        assertEquals(warnings.toString(), 3, warnings.size());
        assertEquals("warn: The service provider org.codecs.impl.Gone, listed in META-INF/services/org.codecs.Codec"
                + " of codecs-1.0.jar, is not among the classes the application ships. ServiceLoader will not"
                + " offer it on a device.", warnings.get(0));
        assertTrue(warnings.get(1), warnings.get(1).startsWith("warn: The service provider org.codecs.impl.Half, "
                + "listed in META-INF/services/org.codecs.Codec of codecs-1.0.jar, cannot be created"));
        assertTrue(warnings.get(2), warnings.get(2).startsWith("warn: The service provider org.codecs.impl.Hidden,"));

        // Nothing the registry or the providers name is missing on a device.
        TestProjectHost host = RealCompatJars.host(classes, tmp.newFolder(), scratch);
        host.artifacts.add(new BuildArtifact("org.example", "codecs", "1", null, "jar", "compile", lib, null));
        new BytecodeCompliance(host).execute();

        // The application's file first, then the library's in its order;
        // hasNext() created nothing and each next() created one provider.
        // The moved service found its provider under the name it ships by.
        String expected = "true[] rot131 hex2 base643|1|0123|glyphs|false";
        assertEquals(expected, report(classes));

        // A second build over the same directory changes nothing.
        byte[] registry = Files.readAllBytes(new File(classes, CompatResources.REGISTRY + ".class").toPath());
        logged.clear();
        assertTrue(remapper(classes, resources, lib).run());
        assertArrayEquals(registry,
                Files.readAllBytes(new File(classes, CompatResources.REGISTRY + ".class").toPath()));
        assertTrue(new File(classes, "org__codecs__table.txt").isFile());
        assertFalse(new File(classes, "org/codecs/table.txt").exists());
        assertEquals(expected, report(classes));
    }

    /// A services file that lists nothing that ships is three warnings'
    /// worth of nothing: the build succeeds and the loader is empty.
    @Test
    public void aListedProviderThatIsAbsentIsAWarningNotAFailure() throws Exception {
        File classes = tmp.newFolder();
        CompatFixtures.compileAgainst(runtimes(), tmp.newFolder(), classes, "com/acme/swingapp/Main.java",
                "package com.acme.swingapp;\n"
                + "public class Main {\n"
                + "    public static void main(String[] args) { new javax.swing.JLabel(\"x\"); }\n"
                + "    public static String report() {\n"
                + "        return String.valueOf(java.util.ServiceLoader.load(Runnable.class).iterator().hasNext());\n"
                + "    }\n"
                + "}\n");
        File resources = tmp.newFolder();
        for (File root : new File[] {resources, classes}) {
            File f = new File(root, "META-INF/services/java.lang.Runnable");
            assertTrue(f.getParentFile().mkdirs());
            Files.write(f.toPath(), "com.acme.swingapp.Nowhere # never written\n".getBytes("UTF-8"));
        }
        List<File> cp = runtimes();
        File record = DesktopSources.entryRecord(tmp.newFolder());
        Files.write(record.toPath(), "mainClass=com.acme.swingapp.Main\nkind=swing\n".getBytes("UTF-8"));
        assertTrue(new CompatRemapper(classes, cp, null, log).withDesktopEntryRecord(record)
                .withApplicationMain("com.acme.MyApp").withResourceDirectories(Collections.singletonList(resources))
                .withApplicationLibraries(Collections.<File>emptyList()).run());
        assertEquals(Collections.singletonList("warn: The service provider com.acme.swingapp.Nowhere, listed in "
                + "META-INF/services/java.lang.Runnable, is not among the classes the application ships. "
                + "ServiceLoader will not offer it on a device."), logged);
        new BytecodeCompliance(RealCompatJars.host(classes, tmp.newFolder(), scratch)).execute();
        assertEquals("false", report(classes));
    }

    /// The registry itself: a provider's number is its place in the list,
    /// and the two factory methods answer for exactly the numbers given.
    @Test
    public void theRegistryCreatesEachProviderByItsNumber() throws Exception {
        List<String[]> services = new ArrayList<String[]>();
        services.add(new String[] {"java.lang.CharSequence", "java/lang/StringBuilder"});
        services.add(new String[] {"java.lang.Object", "java/util/ArrayList"});
        byte[] registry = CompatResources.registry(new ArrayList<String>(),
                new java.util.TreeMap<String, String>(), new ArrayList<String>(), services)
                .get(CompatResources.REGISTRY);
        // Defined ahead of the placeholder of the same name on this class path.
        Class<?> generated = new CompatFixtures.Defining(getClass().getClassLoader()).define(registry);
        Object factory = generated.getConstructor().newInstance();
        assertTrue(factory instanceof com.codename1.compat.jdk.ServiceLoader.Cn1Factory);
        com.codename1.compat.jdk.ServiceLoader.Cn1Factory f = (com.codename1.compat.jdk.ServiceLoader.Cn1Factory) factory;
        assertEquals(StringBuilder.class, f.cn1ServiceType(0));
        assertEquals(ArrayList.class, f.cn1ServiceType(1));
        assertEquals(null, f.cn1ServiceType(2));
        assertEquals(null, f.cn1ServiceType(-1));
        assertTrue(f.cn1CreateService(0) instanceof StringBuilder);
        assertTrue(f.cn1CreateService(1) instanceof ArrayList);
        assertTrue(f.cn1CreateService(1) != f.cn1CreateService(1));
        assertEquals(null, f.cn1CreateService(2));
    }
}
