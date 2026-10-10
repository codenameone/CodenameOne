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
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/// The Swing gallery sample (`scripts/desktop-compat-samples/swing-gallery`),
/// taken through what a project's build does to it and then RUN.
///
/// The sample is an ordinary desktop project: it is compiled here by javac
/// against the JDK's own Swing and the real SwingX and MigLayout jars,
/// exactly as its own `mvn compile` does. The remap step then relocates it
/// and MigLayout onto the layer, the compliance check has to find nothing,
/// and the result is loaded into a class loader that holds nothing but those
/// classes, the framework and the headless implementation the layers' own
/// tests use. There the generated main class is started on the event
/// dispatch thread, and every tab is selected and painted into an image.
///
/// No window is opened anywhere: the display is a 1080 by 1920 buffer.
public class SwingGallerySampleTest {

    private static final String MAIN = "com.acme.gallery.GalleryMain";

    /// One string each tab draws and no other tab does, in tab order.
    private static final String[] SHOWN = {"Click me", "North", "Jupiter  -  95 moons", "Clear drawing",
        "Custom dialog", "Find primes", "Toggle busy", "/icons/star.png: 32 x 32"};

    /// The same for the tabs of the Layouts tab, by their titles.
    private static final Properties INNER = new Properties();

    static {
        INNER.setProperty("Border", "North");
        INNER.setProperty("GridBag", "First name:");
        INNER.setProperty("Group", "Connect");
        INNER.setProperty("Box", "Far right");
        INNER.setProperty("Card", "The first card");
        INNER.setProperty("Mig", "Street:");
    }

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File scratch;

    /// The sample's directory, found from this module's.
    private static File sample() {
        File at = new File("").getAbsoluteFile();
        for (int depth = 0; at != null && depth < 6; depth++) {
            File candidate = new File(at, "scripts/desktop-compat-samples/swing-gallery");
            if (candidate.isDirectory()) {
                return candidate;
            }
            at = at.getParentFile();
        }
        throw new AssertionError("scripts/desktop-compat-samples/swing-gallery was not found above "
                + new File("").getAbsolutePath());
    }

    static void sources(File dir, String rel, List<String> out) throws Exception {
        File[] files = dir.listFiles();
        assertNotNull(dir.toString(), files);
        Arrays.sort(files);
        for (File f : files) {
            if (f.isDirectory()) {
                sources(f, rel + f.getName() + "/", out);
            } else if (f.getName().endsWith(".java")) {
                out.add(rel + f.getName());
                out.add(new String(Files.readAllBytes(f.toPath()), "UTF-8"));
            }
        }
    }

    static void copy(File from, File to) throws Exception {
        File[] files = from.listFiles();
        assertNotNull(from.toString(), files);
        for (File f : files) {
            File target = new File(to, f.getName());
            if (f.isDirectory()) {
                assertTrue(target.isDirectory() || target.mkdirs());
                copy(f, target);
            } else {
                Files.write(target.toPath(), Files.readAllBytes(f.toPath()));
            }
        }
    }

    private List<File> runtimes() throws Exception {
        if (scratch == null) {
            scratch = tmp.newFolder("jars");
        }
        return new ArrayList<File>(Arrays.asList(RealCompatJars.swing(scratch), RealCompatJars.javafx(scratch),
                RealCompatJars.jdk(scratch), RealCompatJars.core(scratch)));
    }

    /// Compiles and remaps the sample; answers the classes directory.
    private File build() throws Exception {
        List<File> jars = runtimes();
        File swingx = RealCompatJars.jar("swingx-all", scratch);
        File migCore = RealCompatJars.jar("miglayout-core", scratch);
        File migSwing = RealCompatJars.jar("miglayout-swing", scratch);
        List<File> libraries = Arrays.asList(migCore, migSwing);

        File classes = tmp.newFolder("classes");
        List<String> sources = new ArrayList<String>();
        sources(new File(sample(), "src/main/java"), "", sources);
        // The JDK's Swing, and the two libraries: nothing of the layer.
        CompatFixtures.compileAgainst(Arrays.asList(swingx, migCore, migSwing), tmp.newFolder(), classes,
                sources.toArray(new String[sources.size()]));

        // The build tool's resources step, which runs before the remap.
        File resources = new File(sample(), "src/main/resources");
        copy(resources, classes);

        File record = DesktopSources.entryRecord(tmp.newFolder());
        Files.write(record.toPath(), "mainClass=com.example.gallery.GalleryApp\nkind=swing\n".getBytes("UTF-8"));
        List<File> cp = new ArrayList<File>(jars);
        cp.add(swingx);
        cp.addAll(libraries);
        assertTrue(new CompatRemapper(classes, cp, null, CompatRemapperTest.LOG)
                .withDesktopEntryRecord(record).withApplicationMain(MAIN)
                .withResourceDirectories(Collections.singletonList(resources))
                .withApplicationLibraries(libraries).run());

        // SwingX is provided -- the layer has its own -- and MigLayout ships.
        TestProjectHost host = RealCompatJars.host(classes, tmp.newFolder(), scratch);
        host.artifacts.add(new BuildArtifact("org.swinglabs.swingx", "swingx-all", "1", null, "jar", "provided",
                swingx, null));
        host.artifacts.add(new BuildArtifact("com.miglayout", "miglayout-core", "1", null, "jar", "compile",
                migCore, null));
        host.artifacts.add(new BuildArtifact("com.miglayout", "miglayout-swing", "1", null, "jar", "compile",
                migSwing, null));
        // Throws on a single finding, in the application, in MigLayout or in
        // the runtime the remap extracted.
        new BytecodeCompliance(host).execute();
        return classes;
    }

    /// The whole Swing runtime, as it ships with the smallest application
    /// there is: every class of the jar has to be something a device can
    /// link, whether or not this application reaches it.
    @Test
    public void theWholeSwingRuntimeIsCompliant() throws Exception {
        File classes = tmp.newFolder("trivial");
        CompatFixtures.compileAgainst(runtimes(), tmp.newFolder(), classes, "com/acme/tiny/Main.java",
                "package com.acme.tiny;\npublic class Main {\n"
                + "    public static void main(String[] args) { new javax.swing.JFrame(\"tiny\").setVisible(true); }\n"
                + "}\n");
        File record = DesktopSources.entryRecord(tmp.newFolder());
        Files.write(record.toPath(), "mainClass=com.acme.tiny.Main\nkind=swing\n".getBytes("UTF-8"));
        assertTrue(new CompatRemapper(classes, runtimes(), null, CompatRemapperTest.LOG)
                .withDesktopEntryRecord(record).withApplicationMain("com.acme.tiny.TinyMain").run());

        int shipped = 0;
        for (File jar : new File[] {RealCompatJars.swing(scratch), RealCompatJars.jdk(scratch)}) {
            ZipFile zip = new ZipFile(jar);
            try {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.endsWith(".class") && !name.startsWith("META-INF/")) {
                        assertTrue(name + " of " + jar.getName() + " did not ship",
                                new File(classes, name).isFile());
                        shipped++;
                    }
                }
            } finally {
                zip.close();
            }
        }
        assertTrue("The runtime is more than " + shipped + " classes", shipped > 500);
        // Zero findings: execute() throws on the first.
        new BytecodeCompliance(RealCompatJars.host(classes, tmp.newFolder(), scratch)).execute();
    }

    @Test
    public void theSampleRunsHeadless() throws Exception {
        File classes = build();
        assertTrue(new File(classes, "net/miginfocom/swing/MigLayout.class").isFile());
        assertTrue(new File(classes, "icons__star.png").isFile());
        assertFalse(new File(classes, "icons/star.png").exists());

        // Nothing of this JVM's classpath but the JDK: the remapped classes,
        // the headless implementation ahead of the framework (it supplies the
        // ImplementationFactory the framework starts on), and the framework.
        URL[] urls = {
            classes.toURI().toURL(),
            RealCompatJars.jar("codenameone-compat-testing", scratch).toURI().toURL(),
            RealCompatJars.core(scratch).toURI().toURL(),
        };
        final URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader().getParent());
        try {
            run(loader);
        } finally {
            loader.close();
        }
    }

    static Object call(Object target, String name, Object... args) throws Exception {
        return invoke(target.getClass(), target, name, args);
    }

    static Object invoke(Class<?> type, Object target, String name, Object... args) throws Exception {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(name) && m.getParameterTypes().length == args.length) {
                m.setAccessible(true);
                try {
                    return m.invoke(target, args);
                } catch (IllegalArgumentException e) {
                    // An overload with as many parameters of other types.
                    continue;
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception) {
                        throw (Exception) cause;
                    }
                    throw e;
                }
            }
        }
        throw new AssertionError(type.getName() + " has no public " + name + " with " + args.length + " arguments");
    }

    /// Runs `work` on the event dispatch thread and rethrows what it threw.
    static void onEdt(Object display, final ThrowingRunnable work) throws Exception {
        final Throwable[] failure = new Throwable[1];
        call(display, "callSeriallyAndWait", new Runnable() {
            @Override
            public void run() {
                try {
                    work.run();
                } catch (Throwable t) {
                    failure[0] = t;
                }
            }
        });
        if (failure[0] instanceof Error) {
            throw (Error) failure[0];
        }
        if (failure[0] != null) {
            throw new Exception("On the event dispatch thread: " + failure[0], failure[0]);
        }
    }

    /// Paints `form` into an image the size of the display and answers every
    /// string that was drawn.
    static List<String> paint(ClassLoader loader, Object form) throws Exception {
        Class<?> headless = loader.loadClass("com.codename1.compat.testing.HeadlessImplementation");
        // Every string a paint draws, as {text, x, y}, while recordText is set.
        List<?> drawn = (List<?>) headless.getField("drawnText").get(null);
        Object target = invoke(loader.loadClass("com.codename1.ui.Image"), null, "createImage", 1080, 1920);
        drawn.clear();
        headless.getField("recordText").setBoolean(null, true);
        try {
            call(form, "paintComponent", call(target, "getGraphics"));
        } finally {
            headless.getField("recordText").setBoolean(null, false);
        }
        List<String> text = new ArrayList<String>();
        for (Object entry : drawn) {
            text.add(String.valueOf(((Object[]) entry)[0]));
        }
        drawn.clear();
        return text;
    }

    /// The first tabbed pane inside `component`, or null.
    private static Object nestedTabs(Object component) throws Exception {
        if (component == null) {
            return null;
        }
        if (component.getClass().getName().endsWith(".JTabbedPane")) {
            return component;
        }
        Class<?> container = component.getClass().getClassLoader()
                .loadClass("com.codename1.desktopcompat.java.awt.Container");
        if (!container.isInstance(component)) {
            return null;
        }
        int n = ((Integer) call(component, "getComponentCount")).intValue();
        for (int i = 0; i < n; i++) {
            Object found = nestedTabs(call(component, "getComponent", i));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    interface ThrowingRunnable {
        void run() throws Exception;
    }

    private void run(final ClassLoader loader) throws Exception {
        final Class<?> headless = loader.loadClass("com.codename1.compat.testing.HeadlessImplementation");
        // The headless implementation decodes an image only when asked to;
        // the sample reads the size of a PNG it ships.
        headless.getField("pixelImages").setBoolean(null, true);
        invoke(headless, null, "install");
        final Class<?> displayClass = loader.loadClass("com.codename1.ui.Display");
        final Object display = invoke(displayClass, null, "getInstance");

        // An exception inside an event or a repaint never reaches the code
        // that posted it; the framework hands it to these.
        final List<String> errors = Collections.synchronizedList(new ArrayList<String>());
        Class<?> listener = loader.loadClass("com.codename1.ui.events.ActionListener");
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[] {listener}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("actionPerformed".equals(method.getName()) && args != null && args.length == 1) {
                    Object source = SwingGallerySampleTest.call(args[0], "getSource");
                    java.io.StringWriter trace = new java.io.StringWriter();
                    if (source instanceof Throwable) {
                        ((Throwable) source).printStackTrace(new java.io.PrintWriter(trace, true));
                    }
                    errors.add(source + "\n" + trace);
                    SwingGallerySampleTest.call(args[0], "consume");
                    return null;
                }
                if ("equals".equals(method.getName())) {
                    return proxy == args[0];
                }
                if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                }
                return "edt errors";
            }
        });
        call(display, "addEdtErrorHandler", handler);

        final Object lifecycle = loader.loadClass(MAIN).getConstructor().newInstance();
        assertEquals("com.codename1.desktopcompat.rt.DesktopLifecycle",
                lifecycle.getClass().getSuperclass().getName());
        onEdt(display, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                call(lifecycle, "init", new Object[] {null});
                call(lifecycle, "start");
            }
        });
        // main() posts the frame with invokeLater; let that, and whatever it
        // posts in turn, go through the queue.
        for (int i = 0; i < 3; i++) {
            onEdt(display, new ThrowingRunnable() {
                @Override
                public void run() {
                }
            });
        }

        final Properties messages = new Properties();
        InputStream in = new FileInputStream(new File(sample(), "src/main/resources/messages.properties"));
        try {
            messages.load(in);
        } finally {
            in.close();
        }
        final String[] keys = {"tab.controls", "tab.layouts", "tab.data", "tab.painting", "tab.dialogs",
            "tab.worker", "tab.swingx", "tab.resources"};
        final List<String> painted = new ArrayList<String>();
        onEdt(display, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                Object[] frames = (Object[]) invoke(
                        loader.loadClass("com.codename1.desktopcompat.java.awt.Frame"), null, "getFrames");
                assertEquals("The application shows one frame; " + errors, 1, frames.length);
                Object frame = frames[0];
                assertEquals(messages.getProperty("app.title"), call(frame, "getTitle"));
                Object form = call(frame, "cn1Form");
                assertTrue(loader.loadClass("com.codename1.ui.Form").isInstance(form));
                assertTrue("The frame's form is the one showing", form == call(display, "getCurrent"));

                Object tabs = call(call(frame, "getContentPane"), "getComponent", 0);
                assertEquals("com.codename1.desktopcompat.javax.swing.JTabbedPane", tabs.getClass().getName());
                assertEquals(keys.length, call(tabs, "getTabCount"));
                for (int i = 0; i < keys.length; i++) {
                    assertEquals(messages.getProperty(keys[i]), call(tabs, "getTitleAt", i));
                    call(tabs, "setSelectedIndex", i);
                    assertEquals(i, call(tabs, "getSelectedIndex"));
                    call(form, "revalidate");
                    List<String> text = paint(loader, form);
                    assertTrue(keys[i] + " paints " + SHOWN[i] + ": " + text, text.contains(SHOWN[i]));
                    // A tab that holds tabs of its own: each of those too.
                    Object inner = nestedTabs(call(tabs, "getComponentAt", i));
                    if (inner != null) {
                        int count = ((Integer) call(inner, "getTabCount")).intValue();
                        for (int t = 0; t < count; t++) {
                            call(inner, "setSelectedIndex", t);
                            call(form, "revalidate");
                            List<String> innerText = paint(loader, form);
                            String title = (String) call(inner, "getTitleAt", t);
                            String expected = INNER.getProperty(title);
                            assertNotNull("An inner tab this test does not know: " + title, expected);
                            assertTrue(title + " paints " + expected + ": " + innerText,
                                    innerText.contains(expected));
                            painted.add(keys[i] + "/" + title);
                        }
                    }
                    painted.add(keys[i]);
                }
            }
        });
        // Whatever selecting and painting posted.
        onEdt(display, new ThrowingRunnable() {
            @Override
            public void run() {
            }
        });
        assertEquals(Arrays.asList("tab.controls", "tab.layouts/Border", "tab.layouts/GridBag", "tab.layouts/Group",
                "tab.layouts/Box", "tab.layouts/Card", "tab.layouts/Mig", "tab.layouts", "tab.data", "tab.painting",
                "tab.dialogs", "tab.worker", "tab.swingx", "tab.resources"), painted);
        assertTrue("Nothing may be thrown on the event dispatch thread: " + errors, errors.isEmpty());
    }
}
