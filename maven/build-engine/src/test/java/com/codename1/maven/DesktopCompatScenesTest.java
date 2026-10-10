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

import com.codename1.fxml.DesktopResourceCompiler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/// The desktop compatibility screens of the screenshot suite
/// (`scripts/hellocodenameone/common/src/main/desktop`), taken through what
/// that application's build does to them and then SHOWN, without a device.
///
/// The screens are Swing, SwingX and JavaFX code in an application that has
/// a main class of its own, so there is no entry record and the build is in
/// library mode. Here the FXML document and the style sheet are compiled,
/// the sources are compiled by javac against the JDK's Swing, the real SwingX
/// jar and the layers' jars, the remap relocates them and has to generate no
/// main class, and the compliance check has to find nothing. The result is
/// loaded into a class loader that holds nothing but those classes, the
/// framework and the headless implementation. There every scene builder the
/// device tests call is called, its component is put in a form of the
/// application and painted once, and the two modal dialogs are painted
/// while open and answered from a timer, as the device tests answer them.
///
/// What a device adds to this is pixels, which is what the screenshot
/// tests are for; an exception, a scene that draws nothing or a blocking
/// call that returns the wrong answer is caught here.
public class DesktopCompatScenesTest {

    private static final String PACKAGE = "com.codenameone.examples.hellocodenameone.desktop.";
    private static final String HOST_MAIN = "com.acme.host.HostApp";

    /// Scenes class, builder method, and one string the scene draws.
    private static final String[][] SCENES = {
        {"SwingScenes", "controls", "Remember me"},
        {"SwingScenes", "tables", "Jupiter"},
        {"SwingScenes", "trees", "Jupiter  -  95 moons"},
        {"SwingScenes", "painting", "Graphics2D"},
        {"SwingScenes", "containers", "Owner: Ada Lovelace"},
        {"SwingScenes", "swingx", "File tasks"},
        {"FxScenes", "controls", "Remember me"},
        {"FxScenes", "data", "Jupiter  -  95 moons"},
        {"FxScenes", "shapes", "Canvas"},
        {"FxScenes", "fxml", "Loaded by the controller"},
        {"FxScenes", "panes", "Tile 7"},
    };

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File scratch;

    /// The desktop source directory of the screenshot application, found
    /// from this module's.
    private static File desktop() {
        File at = new File("").getAbsoluteFile();
        for (int depth = 0; at != null && depth < 6; depth++) {
            File candidate = new File(at, "scripts/hellocodenameone/common/src/main/desktop");
            if (candidate.isDirectory()) {
                return candidate;
            }
            at = at.getParentFile();
        }
        throw new AssertionError("scripts/hellocodenameone/common/src/main/desktop was not found above "
                + new File("").getAbsolutePath());
    }

    private static void sources(File dir, String rel, List<String> out) throws Exception {
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

    private static void copy(File from, File to) throws Exception {
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

    /// Compiles and remaps the screens; answers the classes directory.
    private File build() throws Exception {
        scratch = tmp.newFolder("jars");
        File swingx = RealCompatJars.jar("swingx-all", scratch);
        List<File> jars = new ArrayList<File>(Arrays.asList(RealCompatJars.swing(scratch),
                RealCompatJars.javafx(scratch), RealCompatJars.jdk(scratch), RealCompatJars.core(scratch)));

        // prepare-desktop-sources: the style sheet becomes a table. The FXML
        // document is only recorded here; the remap below compiles it,
        // against the compiled screens.
        File resources = new File(desktop(), "resources");
        File javaOut = tmp.newFolder();
        File resourcesOut = tmp.newFolder();
        final List<String> warnings = new ArrayList<String>();
        List<String> errors = new DesktopResourceCompiler(Collections.singletonList(resources), javaOut,
                resourcesOut, new DesktopResourceCompiler.Log() {
                    @Override
                    public void info(String message) {
                    }

                    @Override
                    public void warn(String message) {
                        warnings.add(message);
                    }
                }).run();
        assertTrue(errors.toString(), errors.isEmpty());
        assertTrue("The style sheet and the document use only what the layer has: " + warnings,
                warnings.isEmpty());

        // javac: the screens, what was generated from the document, and the
        // main class the application has of its own.
        List<String> all = new ArrayList<String>(Arrays.asList("com/acme/host/HostApp.java",
                "package com.acme.host;\npublic class HostApp {\n    public void init(Object context) { }\n"
                + "    public void start() { }\n    public void stop() { }\n    public void destroy() { }\n}\n"));
        sources(new File(desktop(), "java"), "", all);
        sources(javaOut, "", all);
        File classes = tmp.newFolder("classes");
        List<File> compile = new ArrayList<File>(jars);
        compile.add(swingx);
        CompatFixtures.compileAgainst(compile, tmp.newFolder(), classes, all.toArray(new String[all.size()]));
        copy(resources, classes);
        copy(resourcesOut, classes);

        // The remap, with no entry record: library mode.
        File record = DesktopSources.entryRecord(tmp.newFolder());
        assertFalse(record.exists());
        List<File> cp = new ArrayList<File>(jars);
        cp.add(swingx);
        assertTrue(new CompatRemapper(classes, cp, null, CompatRemapperTest.LOG)
                .withDesktopEntryRecord(record).withApplicationMain(HOST_MAIN)
                .withResourceDirectories(Arrays.asList(resources, resourcesOut))
                .withApplicationLibraries(Collections.<File>emptyList()).run());
        assertTrue(new File(classes, "com/codename1/desktopcompat/SwingInterop.class").isFile());
        assertTrue(new File(classes, "com/codename1/fxcompat/FxInterop.class").isFile());

        TestProjectHost host = RealCompatJars.host(classes, tmp.newFolder(), scratch);
        host.artifacts.add(new com.codename1.build.BuildArtifact("org.swinglabs.swingx", "swingx-all", "1", null,
                "jar", "provided", swingx, null));
        // Throws on a single finding, in the screens or in either runtime.
        new BytecodeCompliance(host).execute();
        return classes;
    }

    private static Object call(Object target, String name, Object... args) throws Exception {
        return invoke(target.getClass(), target, name, args);
    }

    private static Object invoke(Class<?> type, Object target, String name, Object... args) throws Exception {
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

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    /// Runs `work` on the event dispatch thread and rethrows what it threw.
    private static void onEdt(Object display, final ThrowingRunnable work) throws Exception {
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
    private static List<String> paint(ClassLoader loader, Object form) throws Exception {
        Class<?> headless = loader.loadClass("com.codename1.compat.testing.HeadlessImplementation");
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

    @Test
    public void everySceneOfTheScreenshotSuiteShowsHeadless() throws Exception {
        File classes = build();
        // Nothing of this JVM's classpath but the JDK: the remapped classes,
        // the headless implementation ahead of the framework, the framework.
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

    private void run(final ClassLoader loader) throws Exception {
        final Class<?> headless = loader.loadClass("com.codename1.compat.testing.HeadlessImplementation");
        // The headless implementation decodes an image only when asked to.
        headless.getField("pixelImages").setBoolean(null, true);
        invoke(headless, null, "install");
        assertEquals("No main class was generated over the application's own", "java.lang.Object",
                loader.loadClass(HOST_MAIN).getSuperclass().getName());
        final Class<?> displayClass = loader.loadClass("com.codename1.ui.Display");
        final Object display = invoke(displayClass, null, "getInstance");
        final Class<?> formClass = loader.loadClass("com.codename1.ui.Form");
        final Class<?> componentClass = loader.loadClass("com.codename1.ui.Component");

        // An exception inside an event or a repaint never reaches the code
        // that posted it; the framework hands it to these.
        final List<String> errors = Collections.synchronizedList(new ArrayList<String>());
        Class<?> listener = loader.loadClass("com.codename1.ui.events.ActionListener");
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[] {listener}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("actionPerformed".equals(method.getName()) && args != null && args.length == 1) {
                    Object source = DesktopCompatScenesTest.call(args[0], "getSource");
                    java.io.StringWriter trace = new java.io.StringWriter();
                    if (source instanceof Throwable) {
                        ((Throwable) source).printStackTrace(new java.io.PrintWriter(trace, true));
                    }
                    errors.add(source + "\n" + trace);
                    DesktopCompatScenesTest.call(args[0], "consume");
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

        final List<String> shown = new ArrayList<String>();
        for (final String[] scene : SCENES) {
            onEdt(display, new ThrowingRunnable() {
                @Override
                public void run() throws Exception {
                    Object component = invoke(loader.loadClass(PACKAGE + scene[0]), null, scene[1]);
                    assertTrue(scene[0] + "." + scene[1] + " answers a Codename One component",
                            componentClass.isInstance(component));
                    Object form = formClass.getConstructor(String.class).newInstance(scene[1]);
                    call(form, "setLayout",
                            loader.loadClass("com.codename1.ui.layouts.BorderLayout").getConstructor().newInstance());
                    call(form, "addComponent", "Center", component);
                    call(form, "show");
                    call(form, "revalidate");
                    assertTrue("The scene was given room", ((Integer) call(component, "getWidth")).intValue() > 0
                            && ((Integer) call(component, "getHeight")).intValue() > 0);
                    List<String> text = paint(loader, form);
                    assertTrue(scene[0] + "." + scene[1] + " paints " + scene[2] + ": " + text,
                            text.contains(scene[2]));
                    shown.add(scene[0] + "." + scene[1]);
                }
            });
            // Whatever building and painting it posted.
            onEdt(display, new ThrowingRunnable() {
                @Override
                public void run() {
                }
            });
        }
        assertEquals(SCENES.length, shown.size());

        assertEquals("yes", dialog(loader, display, "SwingScenes", new Asker() {
            @Override
            public String ask(Class<?> scenes) throws Exception {
                return (String) invoke(scenes, null, "answerName", invoke(scenes, null, "confirm"));
            }
        }, errors));
        assertEquals("ok", dialog(loader, display, "FxScenes", new Asker() {
            @Override
            public String ask(Class<?> scenes) throws Exception {
                return (String) invoke(scenes, null, "confirm");
            }
        }, errors));
        assertTrue("Nothing may be thrown on the event dispatch thread: " + errors, errors.isEmpty());
    }

    private interface Asker {
        String ask(Class<?> scenes) throws Exception;
    }

    /// Asks the question of a scenes class on the event dispatch thread,
    /// as a device test does: the call blocks while a timer waits for the
    /// dialog to be up, paints the form it is on and presses its button.
    private String dialog(final ClassLoader loader, final Object display, String name, final Asker asker,
            final List<String> errors) throws Exception {
        final Class<?> scenes = loader.loadClass(PACKAGE + name);
        final Class<?> formClass = loader.loadClass("com.codename1.ui.Form");
        final String[] answer = new String[1];
        final List<String> log = Collections.synchronizedList(new ArrayList<String>());
        onEdt(display, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                final Object host = formClass.getConstructor(String.class).newInstance("Host");
                call(host, "show");
                final Runnable[] poll = new Runnable[1];
                final int[] tries = new int[1];
                poll[0] = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            boolean up = Boolean.TRUE.equals(invoke(scenes, null, "confirmShowing"));
                            Object current = call(display, "getCurrent");
                            if ((!up || current == host) && ++tries[0] < 100) {
                                call(display, "setTimeout", 50, poll[0]);
                                return;
                            }
                            log.add("showing=" + up + " overTheHost=" + (current != host));
                            if (up && current != null) {
                                log.add("painted=" + paint(loader, current).contains("Delete the selected planet?"));
                            }
                            log.add("answered=" + invoke(scenes, null, "answerConfirm"));
                        } catch (Throwable t) {
                            errors.add("While answering: " + t);
                        }
                    }
                };
                call(display, "setTimeout", 50, poll[0]);
                answer[0] = asker.ask(scenes);
                assertTrue("The application's form is back once the dialog closed",
                        call(display, "getCurrent") == host);
            }
        });
        assertEquals(name + ": the dialog was up over the application's form, painted its question and was answered",
                "[showing=true overTheHost=true, painted=true, answered=true]", log.toString());
        return answer[0];
    }
}
