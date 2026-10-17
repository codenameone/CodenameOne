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
import com.codename1.builders.BuildException;
import com.codename1.impl.compat.ResourceNames;
import com.codename1.impl.compat.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Desktop resources through the build: flattened, indexed and registered,
/// and -- in the last test -- looked up by a javac-compiled application
/// that has been relocated, has passed the compliance check against the real
/// device library, and runs against a bundle that holds flat names only.
public class CompatResourcesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Locale saved;

    @Before
    public void useUsLocale() {
        saved = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void restore() {
        Locale.setDefault(saved);
        Resources.cn1SetProvider(null);
        Resources.cn1ClearIndex();
    }

    private static File write(File root, String path, String text) throws IOException {
        File f = new File(root, path);
        assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
        Files.write(f.toPath(), text.getBytes("UTF-8"));
        return f;
    }

    private static String text(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), "UTF-8");
    }

    /// Every file under `dir` by its path, as text of its bytes.
    private static Map<String, byte[]> snapshot(File dir) throws IOException {
        Map<String, byte[]> out = new TreeMap<String, byte[]>();
        snapshot(dir, "", out);
        return out;
    }

    private static void snapshot(File dir, String rel, Map<String, byte[]> out) throws IOException {
        File[] files = dir.listFiles();
        assertNotNull(files);
        for (File f : files) {
            if (f.isDirectory()) {
                snapshot(f, rel + f.getName() + "/", out);
            } else {
                out.put(rel + f.getName(), Files.readAllBytes(f.toPath()));
            }
        }
    }

    private static void assertSameTree(Map<String, byte[]> expected, Map<String, byte[]> actual) {
        assertEquals(expected.keySet(), actual.keySet());
        for (Map.Entry<String, byte[]> e : expected.entrySet()) {
            assertArrayEquals(e.getKey(), e.getValue(), actual.get(e.getKey()));
        }
    }

    private void ship(File classes, File resources) throws Exception {
        new CompatResources(classes, Collections.singletonList(resources), Collections.<File>emptyList(),
                CompatRemapperTest.LOG).run();
    }

    // ------------------------------------------------------------------
    // Flattening
    // ------------------------------------------------------------------

    @Test
    public void nestedResourcesMoveToTheirFlatNames() throws Exception {
        File classes = tmp.newFolder("classes");
        File resources = tmp.newFolder("resources");
        write(resources, "com/acme/img/my_logo.png", "LOGO");
        write(resources, "com/acme/Messages.properties", "a=source");
        write(resources, "root.txt", "ROOT");
        write(resources, "com/acme/.DS_Store", "junk");
        // What a build tool's resources step left: copies, one of them
        // filtered, beside the application's classes.
        write(classes, "com/acme/img/my_logo.png", "LOGO");
        write(classes, "com/acme/Messages.properties", "a=filtered");
        write(classes, "root.txt", "ROOT");
        Files.write(write(classes, "com/acme/Main.class", "").toPath(),
                CompatRemapperTest.emptyClass("com/acme/Main", "java/lang/Object"));

        ship(classes, resources);

        assertEquals("com__acme__img__my_ulogo.png", ResourceNames.flatName("com/acme/img/my_logo.png"));
        assertEquals("LOGO", text(new File(classes, "com__acme__img__my_ulogo.png")));
        // The build tool's copy is the one that ships.
        assertEquals("a=filtered", text(new File(classes, "com__acme__Messages.properties")));
        assertEquals("ROOT", text(new File(classes, "root.txt")));
        assertFalse(new File(classes, "com/acme/img").exists());
        assertFalse(new File(classes, "com/acme/Messages.properties").exists());
        assertTrue("A directory still holding classes stays", new File(classes, "com/acme/Main.class").isFile());
        assertFalse(new File(classes, "com__acme__.DS_Store").exists());
        assertEquals("com__acme__Messages.properties\ncom__acme__img__my_ulogo.png\n",
                text(CompatResources.manifest(classes)));
        assertFalse("The list of flat names is not shipped",
                CompatResources.manifest(classes).getAbsolutePath().startsWith(classes.getAbsolutePath() + File.separator));
    }

    /// Gradle keeps resources out of the classes directory, so there is no
    /// nested copy to take: the source file is.
    @Test
    public void aResourceWithNoCopyInTheClassesDirectoryIsTakenFromItsSource() throws Exception {
        File classes = tmp.newFolder("classes");
        File resources = tmp.newFolder("resources");
        write(resources, "x/y.txt", "why");
        ship(classes, resources);
        assertEquals("why", text(new File(classes, "x__y.txt")));
    }

    @Test
    public void aSecondRunChangesNothingAndADeletedResourceLosesItsFlatCopy() throws Exception {
        File classes = tmp.newFolder("classes");
        File resources = tmp.newFolder("resources");
        write(resources, "x/y.txt", "why");
        File doomed = write(resources, "x/gone.txt", "gone");
        ship(classes, resources);
        Map<String, byte[]> first = snapshot(classes);
        long stamp = new File(classes, "x__y.txt").lastModified();
        assertTrue(first.containsKey("x__gone.txt"));

        ship(classes, resources);
        assertSameTree(first, snapshot(classes));
        assertEquals(stamp, new File(classes, "x__y.txt").lastModified());

        assertTrue(doomed.delete());
        ship(classes, resources);
        assertFalse(new File(classes, "x__gone.txt").exists());
        assertEquals("x__y.txt\n", text(CompatResources.manifest(classes)));

        assertTrue(new File(resources, "x/y.txt").delete());
        ship(classes, resources);
        assertFalse(new File(classes, "x__y.txt").exists());
        assertFalse(CompatResources.manifest(classes).exists());
    }

    @Test
    public void aFlatNameTakenByARootResourceFailsTheBuild() throws Exception {
        File classes = tmp.newFolder("classes");
        File resources = tmp.newFolder("resources");
        write(resources, "img/a.png", "nested");
        write(resources, "img__a.png", "root");
        try {
            ship(classes, resources);
            fail("Two resources would ship under one name");
        } catch (BuildException expected) {
            String message = expected.getMessage();
            assertTrue(message, message.contains("The resource img/a.png ships as img__a.png"));
            assertTrue(message, message.contains("already taken by"));
            assertTrue(message, message.contains(new File(resources, "img__a.png").toString()));
            assertTrue(message, message.contains("Rename one of the two"));
        }
    }

    /// A file some other step put at the root -- from the ordinary resource
    /// directory, say -- is not ours to overwrite.
    @Test
    public void aFlatNameTakenByAnotherFileAtTheRootFailsTheBuild() throws Exception {
        File classes = tmp.newFolder("classes");
        File resources = tmp.newFolder("resources");
        write(resources, "img/a.png", "nested");
        File foreign = write(classes, "img__a.png", "somebody else's");
        try {
            ship(classes, resources);
            fail("A file of another origin would be overwritten");
        } catch (BuildException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(foreign.toString()));
        }
        assertEquals("somebody else's", text(foreign));

        // The same bytes are the same resource, whoever put them there.
        write(classes, "img__a.png", "nested");
        ship(classes, resources);
        assertEquals("nested", text(foreign));
    }

    @Test
    public void theSameResourceInTwoDirectoriesFailsTheBuild() throws Exception {
        File classes = tmp.newFolder("classes");
        File one = tmp.newFolder("one");
        File two = tmp.newFolder("two");
        write(one, "x/y.txt", "1");
        write(two, "x/y.txt", "2");
        try {
            new CompatResources(classes, Arrays.asList(one, two, new File(tmp.getRoot(), "absent")),
                    Collections.<File>emptyList(), CompatRemapperTest.LOG).run();
            fail("Only one of the two can ship");
        } catch (BuildException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("The resource x/y.txt exists twice"));
        }
    }

    // ------------------------------------------------------------------
    // End to end
    // ------------------------------------------------------------------

    private static final String FOO = "package p;\n"
            + "import java.io.InputStream;\n"
            + "import java.net.URL;\n"
            + "import java.util.HashMap;\n"
            + "import java.util.Locale;\n"
            + "import java.util.Map;\n"
            + "import java.util.Properties;\n"
            + "import java.util.ResourceBundle;\n"
            + "public class Foo {\n"
            + "    javax.swing.JLabel label;\n"
            + "    public String image() throws Exception {\n"
            + "        URL url = getClass().getResource(\"img/a.png\");\n"
            + "        return url.toExternalForm() + \"=\" + read(url.openStream());\n"
            + "    }\n"
            + "    public static String text() throws Exception {\n"
            + "        return read(Foo.class.getResourceAsStream(\"/x/y.txt\"));\n"
            + "    }\n"
            + "    public static String root() throws Exception {\n"
            + "        return read(Foo.class.getResourceAsStream(\"/root.txt\"));\n"
            + "    }\n"
            + "    public static String missing() {\n"
            + "        return Foo.class.getResource(\"/x/none.txt\") + \",\" + Foo.class.getResourceAsStream(\"none.png\");\n"
            + "    }\n"
            + "    public static String bundle() {\n"
            + "        return ResourceBundle.getBundle(\"p.Messages\").getString(\"greeting\");\n"
            + "    }\n"
            + "    public static String french() {\n"
            + "        ResourceBundle b = ResourceBundle.getBundle(\"p.Messages\", Locale.FRENCH);\n"
            + "        return b.getString(\"greeting\") + \",\" + b.getString(\"bye\");\n"
            + "    }\n"
            + "    public static String listed() {\n"
            + "        return ResourceBundle.getBundle(\"p.Labels\").getString(\"ok\");\n"
            + "    }\n"
            + "    public static String settings() throws Exception {\n"
            + "        Properties p = new Properties();\n"
            + "        InputStream in = Foo.class.getResourceAsStream(\"Messages.properties\");\n"
            + "        p.load(in);\n"
            + "        in.close();\n"
            + "        return p.getProperty(\"bye\") + \",\" + p.getProperty(\"absent\", \"default\");\n"
            + "    }\n"
            + "    public static int compute() {\n"
            + "        Map<String, Integer> m = new HashMap<>();\n"
            + "        m.computeIfAbsent(\"a\", k -> k.length() + 6);\n"
            + "        m.computeIfAbsent(\"a\", k -> 100);\n"
            + "        return m.get(\"a\");\n"
            + "    }\n"
            + "    static String read(InputStream in) throws Exception {\n"
            + "        StringBuilder sb = new StringBuilder();\n"
            + "        for (int c = in.read(); c >= 0; c = in.read()) {\n"
            + "            sb.append((char) c);\n"
            + "        }\n"
            + "        in.close();\n"
            + "        return sb.toString();\n"
            + "    }\n"
            + "}\n";

    private static final String LABELS = "package p;\n"
            + "import java.util.ListResourceBundle;\n"
            + "public class Labels extends ListResourceBundle {\n"
            + "    protected Object[][] getContents() {\n"
            + "        return new Object[][] {{\"ok\", \"OK\"}};\n"
            + "    }\n"
            + "}\n";

    /// A use of a class loader that nothing stands in for: one made at run
    /// time, to load classes the application was not built with.
    private static final String LOADS = "package p;\n"
            + "public class Loads {\n"
            + "    javax.swing.JLabel label;\n"
            + "    public static Class<?> load() throws Exception {\n"
            + "        return new java.net.URLClassLoader(new java.net.URL[0]).loadClass(\"p.Plugin\");\n"
            + "    }\n"
            + "}\n";

    /// Asking the application's own loader for one of its classes, which is
    /// how desktop code finds out whether an optional class is present.
    private static final String ASKS = "package p;\n"
            + "public class Asks {\n"
            + "    javax.swing.JLabel label;\n"
            + "    public static boolean has(String name) {\n"
            + "        try {\n"
            + "            return Asks.class.getClassLoader().loadClass(name) != null;\n"
            + "        } catch (ClassNotFoundException e) {\n"
            + "            return false;\n"
            + "        }\n"
            + "    }\n"
            + "}\n";

    /// The jar or class directory on the test classpath that holds `marker`
    /// and belongs to `artifactId`, as a jar named the way a build finds it.
    private File artifact(String artifactId, String marker) throws IOException {
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            File f = new File(entry);
            if (f.isFile() && f.getName().startsWith(artifactId + "-") && f.getName().endsWith(".jar")) {
                return f;
            }
            if (f.isDirectory() && new File(f, marker).isFile()) {
                // A reactor build has the module's classes, not its jar.
                File jar = new File(tmp.getRoot(), artifactId + "-1.jar");
                if (!jar.isFile()) {
                    ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jar));
                    try {
                        for (Map.Entry<String, byte[]> e : snapshot(f).entrySet()) {
                            zip.putNextEntry(new ZipEntry(e.getKey()));
                            zip.write(e.getValue());
                            zip.closeEntry();
                        }
                    } finally {
                        zip.close();
                    }
                }
                return jar;
            }
        }
        fail(artifactId + " is not on the test classpath: " + System.getProperty("java.class.path"));
        return null;
    }

    private File swingJar() throws IOException {
        String label = CompatRemapperTest.SWING + "javax/swing/JLabel";
        return CompatRemapperTest.jar(new File(tmp.getRoot(), "codenameone-swing-compat-1.jar"),
                label + ".class", CompatRemapperTest.emptyClass(label, "java/lang/Object"));
    }

    /// The compliance check as a build runs it, against the real device
    /// library and the real core.
    private void checkCompliance(File classes, File swing) throws Exception {
        TestProjectHost host = TestProjectHost.empty();
        host.log = CompatRemapperTest.LOG;
        host.buildDir = tmp.newFolder();
        host.outputDir = classes;
        host.artifacts.add(new BuildArtifact("com.codenameone", "java-runtime", "1", null, "jar", "provided",
                artifact("java-runtime", "java/lang/Object.class"), null));
        host.artifacts.add(new BuildArtifact("com.codenameone", "codenameone-core", "1", null, "jar", "provided",
                artifact("codenameone-core", "com/codename1/ui/Display.class"), null));
        host.artifacts.add(new BuildArtifact("com.codenameone", "codenameone-swing-compat", "1", null, "jar",
                "compile", swing, null));
        new BytecodeCompliance(host).execute();
    }

    /// Installs into `resources` (the `Resources` class `loader` defined)
    /// what the JavaSE and Android ports install on a real JDK: `cn1res:`
    /// URLs whose stream reads through `Resources.openUrlPath`, and a loader
    /// that answers a nested name the build flattened from the index.
    private static void installJdkSupport(ClassLoader loader, final Class<?> resources) throws Exception {
        final Method openUrlPath = resources.getMethod("openUrlPath", String.class);
        final Method exists = resources.getMethod("exists", String.class);
        final Method systemResource = resources.getMethod("getSystemResource", String.class);
        final URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) {
                return new URLConnection(u) {
                    @Override
                    public void connect() {
                        connected = true;
                    }

                    @Override
                    public java.io.InputStream getInputStream() throws IOException {
                        try {
                            java.io.InputStream in = (java.io.InputStream) openUrlPath.invoke(null, getURL().getPath());
                            if (in == null) {
                                throw new java.io.FileNotFoundException(getURL().toExternalForm());
                            }
                            return in;
                        } catch (ReflectiveOperationException e) {
                            throw new IOException(e);
                        }
                    }
                };
            }
        };
        Class<?> factoryType = loader.loadClass("com.codename1.impl.compat.Resources$UrlFactory");
        Object factory = Proxy.newProxyInstance(loader, new Class<?>[] {factoryType}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (!"create".equals(method.getName())) {
                    return method.invoke(this, args);
                }
                return new URL("cn1res", null, -1, (String) args[0], handler);
            }
        });
        resources.getMethod("cn1SetUrlFactory", factoryType).invoke(null, factory);
        ClassLoader flat = new ClassLoader(loader) {
            @Override
            protected URL findResource(String name) {
                try {
                    return Boolean.TRUE.equals(exists.invoke(null, name)) ? (URL) systemResource.invoke(null, name)
                            : null;
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        resources.getMethod("cn1SetClassLoader", ClassLoader.class).invoke(null, flat);
    }

    @Test
    public void aDesktopApplicationFindsItsResourcesInAFlatBundle() throws Exception {
        File classes = tmp.newFolder("classes");
        CompatFixtures.compile(tmp.newFolder("src"), classes, "p/Foo.java", FOO, "p/Labels.java", LABELS);
        File desktop = tmp.newFolder("desktop");
        File resources = DesktopSources.resourcesDir(desktop);
        String[][] files = {
            {"p/img/a.png", "PNG"},
            {"x/y.txt", "why"},
            {"root.txt", "ROOT"},
            {"p/Messages.properties", "greeting=Hello\nbye=Good\\\n    bye\n"},
            {"p/Messages_fr.properties", "greeting=Bonjour\n"},
        };
        for (String[] file : files) {
            write(resources, file[0], file[1]);
            // As Maven's resources step leaves them, beside the classes.
            write(classes, file[0], file[1]);
        }
        File swing = swingJar();
        List<File> classpath = Collections.singletonList(swing);

        assertTrue(new CompatRemapper(classes, classpath, null, CompatRemapperTest.LOG)
                .withDesktopSourceDirectory(desktop).run());

        // Flat, and only flat.
        for (String flat : new String[] {"p__img__a.png", "x__y.txt", "root.txt", "p__Messages.properties",
            "p__Messages_ufr.properties"}) {
            assertTrue(flat, new File(classes, flat).isFile());
        }
        assertFalse(new File(classes, "p/img").exists());
        assertFalse(new File(classes, "x").exists());
        assertFalse(new File(classes, "p/Messages.properties").exists());
        assertTrue(new File(classes, ApplicationRegistry.NAME + ".class").isFile());

        // A second run is a no-op, the registry included.
        Map<String, byte[]> first = snapshot(classes);
        assertTrue(new CompatRemapper(classes, classpath, null, CompatRemapperTest.LOG)
                .withDesktopSourceDirectory(desktop).run());
        assertSameTree(first, snapshot(classes));

        // Everything that ships exists on the device.
        checkCompliance(classes, swing);

        // The device bundle: the files at the root of the classes
        // directory and nothing else. The application's loader sees the
        // classes and the core, and the JDK is the class library -- as on
        // the JavaSE and Android ports, which install what a real JDK needs
        // to read a flat bundle: a URL handler for `cn1res:` and a loader
        // that falls back to the index. `installJdkSupport` is the same.
        final File bundle = tmp.newFolder("bundle");
        for (File f : classes.listFiles()) {
            if (f.isFile()) {
                Files.copy(f.toPath(), new File(bundle, f.getName()).toPath());
            }
        }
        final List<String> asked = new ArrayList<String>();
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL(),
            artifact("codenameone-core", "com/codename1/ui/Display.class").toURI().toURL(),
            RealCompatJars.jdkHost(tmp.newFolder()).toURI().toURL()}, null);
        try {
            Class<?> provider = loader.loadClass("com.codename1.impl.compat.Resources$Provider");
            Object flat = Proxy.newProxyInstance(loader, new Class<?>[] {provider}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (!"open".equals(method.getName())) {
                        return method.invoke(this, args);
                    }
                    asked.add((String) args[0]);
                    File f = new File(bundle, (String) args[0]);
                    return f.isFile() ? new FileInputStream(f) : null;
                }
            });
            Class<?> resourcesClass = loader.loadClass("com.codename1.impl.compat.Resources");
            resourcesClass.getMethod("cn1SetProvider", provider).invoke(null, flat);
            installJdkSupport(loader, resourcesClass);
            // What the application's lifecycle does first.
            loader.loadClass(ApplicationRegistry.NAME.replace('/', '.')).getMethod("cn1Install").invoke(null);

            Class<?> foo = loader.loadClass("p.Foo");
            Object instance = foo.getConstructor().newInstance();
            assertEquals("cn1res:/p/img/a.png=PNG", foo.getMethod("image").invoke(instance));
            assertEquals("why", foo.getMethod("text").invoke(null));
            assertEquals("ROOT", foo.getMethod("root").invoke(null));
            assertEquals(Arrays.asList("p__img__a.png", "x__y.txt", "root.txt"), asked);
            asked.clear();
            assertEquals("null,null", foo.getMethod("missing").invoke(null));
            assertTrue("Absence comes from the index: " + asked, asked.isEmpty());

            assertEquals("Hello", foo.getMethod("bundle").invoke(null));
            assertEquals("Bonjour,Goodbye", foo.getMethod("french").invoke(null));
            assertEquals("OK", foo.getMethod("listed").invoke(null));
            assertEquals("Goodbye,default", foo.getMethod("settings").invoke(null));
            assertEquals(Integer.valueOf(7), foo.getMethod("compute").invoke(null));
        } finally {
            loader.close();
        }
    }

    @Test
    public void loadingClassesTheApplicationWasNotBuiltWithIsStillABuildError() throws Exception {
        File classes = tmp.newFolder("classes");
        CompatFixtures.compile(tmp.newFolder("src"), classes, "p/Loads.java", LOADS);
        File swing = swingJar();
        assertTrue(new CompatRemapper(classes, Collections.singletonList(swing), null, CompatRemapperTest.LOG).run());
        try {
            checkCompliance(classes, swing);
            fail("A device loads no classes at run time");
        } catch (BuildFailureException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("URLClassLoader"));
        }
    }

    /// `loadClass` on the loader the application already has is
    /// `Class.forName`, which a device does have.
    @Test
    public void askingTheApplicationsLoaderForAClassIsNot() throws Exception {
        File classes = tmp.newFolder("classes");
        CompatFixtures.compile(tmp.newFolder("src"), classes, "p/Asks.java", ASKS);
        File swing = swingJar();
        assertTrue(new CompatRemapper(classes, Collections.singletonList(swing), null, CompatRemapperTest.LOG).run());
        assertTrue(CompatFixtures.members(Files.readAllBytes(new File(classes, "p/Asks.class").toPath()))
                .contains("com/codename1/impl/compat/Resources.loadClass"));
        checkCompliance(classes, swing);
    }
}
