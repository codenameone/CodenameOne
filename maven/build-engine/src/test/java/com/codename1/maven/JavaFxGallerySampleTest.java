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

/// The JavaFX gallery sample
/// (`scripts/desktop-compat-samples/javafx-gallery`), taken through what a
/// project's build does to it and then RUN.
///
/// The steps are the build's own, in its order: the resource compiler turns
/// the style sheets into their compiled form and stands one marker source in
/// for the FXML documents, javac compiles the application, and the remap
/// step compiles the documents against the application's classes -- one of
/// them is a custom control another document uses -- before it relocates
/// everything onto the layer. The compliance check then has to find nothing,
/// and the result is loaded into a class loader that holds nothing but those
/// classes, the framework and the headless implementation the layers' own
/// tests use.
///
/// The sample itself is compiled against the real OpenJFX by its own
/// `mvn compile`, which the desktop compatibility workflow runs; here it is
/// compiled against the layer, as an imported project is.
///
/// No window is opened anywhere: the display is a 1080 by 1920 buffer.
public class JavaFxGallerySampleTest {

    private static final String MAIN = "com.acme.fxgallery.GalleryMain";

    private static final String FX = "com.codename1.fxcompat.javafx.";

    /// The tabs in their order, and one string each draws that no other does.
    private static final String[][] TABS = {
        {"Controls", "Click me"},
        {"Data", "Filter by name"},
        {"Layouts", "GridPane form"},
        {"Bindings", "Clicked 0 times"},
        {"FXML", "Enter a name and press Greet"},
        {"Shapes", "Redraw canvas"},
        {"Animation", "0 frames"},
        {"Dialogs", "Second window"},
        {"Tasks", "Tick from a thread"},
    };

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File scratch;

    /// The sample's directory, found from this module's.
    private static File sample() {
        File at = new File("").getAbsoluteFile();
        for (int depth = 0; at != null && depth < 6; depth++) {
            File candidate = new File(at, "scripts/desktop-compat-samples/javafx-gallery");
            if (candidate.isDirectory()) {
                return candidate;
            }
            at = at.getParentFile();
        }
        throw new AssertionError("scripts/desktop-compat-samples/javafx-gallery was not found above "
                + new File("").getAbsolutePath());
    }

    /// Compiles and remaps the sample; answers the classes directory.
    private File build() throws Exception {
        scratch = tmp.newFolder("jars");
        List<File> cp = Arrays.asList(RealCompatJars.javafx(scratch), RealCompatJars.jdk(scratch),
                RealCompatJars.core(scratch));

        // prepare-desktop-sources.
        File resources = new File(sample(), "src/main/resources");
        File javaOut = tmp.newFolder();
        File resourcesOut = tmp.newFolder();
        final List<String> warnings = new ArrayList<String>();
        DesktopResourceCompiler.Log log = new DesktopResourceCompiler.Log() {
            @Override
            public void info(String message) {
            }

            @Override
            public void warn(String message) {
                warnings.add(message);
            }
        };
        List<String> errors = new DesktopResourceCompiler(Collections.singletonList(resources), javaOut,
                resourcesOut, log).run();
        assertTrue(errors.toString(), errors.isEmpty());
        assertTrue("The sample's style sheets and documents compile without a warning: " + warnings,
                warnings.isEmpty());

        // javac. The importer drops the module descriptor.
        List<String> all = new ArrayList<String>();
        SwingGallerySampleTest.sources(new File(sample(), "src/main/java"), "", all);
        int descriptor = all.indexOf("module-info.java");
        assertTrue("The sample is a modular project", descriptor >= 0);
        all.remove(descriptor);
        all.remove(descriptor);
        SwingGallerySampleTest.sources(javaOut, "", all);
        File classes = tmp.newFolder("classes");
        CompatFixtures.compileAgainst(cp, tmp.newFolder(), classes, all.toArray(new String[all.size()]));

        // process-resources.
        SwingGallerySampleTest.copy(resources, classes);
        SwingGallerySampleTest.copy(resourcesOut, classes);

        // remap-compat, which compiles the documents first.
        File record = DesktopSources.entryRecord(tmp.newFolder());
        Files.write(record.toPath(), "mainClass=com.example.fxgallery.GalleryApp\nkind=javafx\n".getBytes("UTF-8"));
        assertTrue(new CompatRemapper(classes, cp, null, CompatRemapperTest.LOG)
                .withDesktopEntryRecord(record).withApplicationMain(MAIN)
                .withResourceDirectories(Arrays.asList(resources, resourcesOut)).run());

        // Throws on a single finding, in the application, in the classes
        // compiled from its documents or in the runtime the remap extracted.
        new BytecodeCompliance(RealCompatJars.host(classes, tmp.newFolder(), scratch)).execute();
        return classes;
    }

    @Test
    public void theSampleRunsHeadless() throws Exception {
        File classes = build();
        // The three documents, the custom control's among them, are classes.
        assertTrue(new File(classes, "com/codename1/generated/fxml/Fxml_com__example__fxgallery__fxml__profile.class")
                .isFile());
        assertTrue(new File(classes, "com/codename1/generated/fxml/Fxml_com__example__fxgallery__fxml__address.class")
                .isFile());
        assertTrue(new File(classes,
                "com/codename1/generated/fxml/Fxml_com__example__fxgallery__control__status_mbadge.class").isFile());
        assertFalse(new File(classes, "javafx").exists());
        assertFalse(new File(classes, "com/example/fxgallery/images/logo.png").exists());

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

    private static Object call(Object target, String name, Object... args) throws Exception {
        return SwingGallerySampleTest.invoke(target.getClass(), target, name, args);
    }

    /// Lets what the last step posted -- a pulse, a `runLater` -- go through
    /// the queue.
    private static void drain(Object display) throws Exception {
        for (int i = 0; i < 3; i++) {
            SwingGallerySampleTest.onEdt(display, new SwingGallerySampleTest.ThrowingRunnable() {
                @Override
                public void run() {
                }
            });
        }
    }

    private void run(final ClassLoader loader) throws Exception {
        final Class<?> headless = loader.loadClass("com.codename1.compat.testing.HeadlessImplementation");
        // The sample decodes a PNG it ships for the window's icon.
        headless.getField("pixelImages").setBoolean(null, true);
        SwingGallerySampleTest.invoke(headless, null, "install");
        final Class<?> displayClass = loader.loadClass("com.codename1.ui.Display");
        final Object display = SwingGallerySampleTest.invoke(displayClass, null, "getInstance");

        // An exception inside an event or a repaint never reaches the code
        // that posted it; the framework hands it to these.
        final List<String> errors = Collections.synchronizedList(new ArrayList<String>());
        Class<?> listener = loader.loadClass("com.codename1.ui.events.ActionListener");
        Object handler = Proxy.newProxyInstance(loader, new Class<?>[] {listener}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("actionPerformed".equals(method.getName()) && args != null && args.length == 1) {
                    Object source = call(args[0], "getSource");
                    java.io.StringWriter trace = new java.io.StringWriter();
                    if (source instanceof Throwable) {
                        ((Throwable) source).printStackTrace(new java.io.PrintWriter(trace, true));
                    }
                    errors.add(source + "\n" + trace);
                    call(args[0], "consume");
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
        assertTrue(lifecycle.getClass().getSuperclass().getName(),
                lifecycle.getClass().getSuperclass().getName().endsWith(".FxLifecycle"));
        SwingGallerySampleTest.onEdt(display, new SwingGallerySampleTest.ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                call(lifecycle, "init", new Object[] {null});
                call(lifecycle, "start");
            }
        });
        drain(display);

        final Object[] form = new Object[1];
        final Object[] scene = new Object[1];
        final Object[] tabPane = new Object[1];
        SwingGallerySampleTest.onEdt(display, new SwingGallerySampleTest.ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                List<?> windows = (List<?>) SwingGallerySampleTest.invoke(
                        loader.loadClass(FX + "stage.Window"), null, "getWindows");
                assertEquals("The application shows one stage; " + errors, 1, windows.size());
                Object stage = windows.get(0);
                assertEquals("JavaFX Gallery", call(stage, "getTitle"));
                assertEquals("The window's icon was decoded from the application's resources",
                        1, ((List<?>) call(stage, "getIcons")).size());
                form[0] = call(display, "getCurrent");
                assertNotNull("A form is showing; " + errors, form[0]);
                assertTrue(loader.loadClass("com.codename1.ui.Form").isInstance(form[0]));
                scene[0] = call(stage, "getScene");
                tabPane[0] = call(call(scene[0], "getRoot"), "getCenter");
                assertEquals(FX + "scene.control.TabPane", tabPane[0].getClass().getName());
                assertEquals(TABS.length, ((List<?>) call(tabPane[0], "getTabs")).size());
            }
        });

        final List<String> painted = new ArrayList<String>();
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            SwingGallerySampleTest.onEdt(display, new SwingGallerySampleTest.ThrowingRunnable() {
                @Override
                public void run() throws Exception {
                    Object tab = ((List<?>) call(tabPane[0], "getTabs")).get(index);
                    assertEquals(TABS[index][0], call(tab, "getText"));
                    Object selection = call(tabPane[0], "getSelectionModel");
                    // select(int) refuses a Tab; select(T) is the one left.
                    call(selection, "select", tab);
                    assertEquals(Integer.valueOf(index), call(selection, "getSelectedIndex"));
                }
            });
            // The pulse that lays the tab out and styles it.
            drain(display);
            SwingGallerySampleTest.onEdt(display, new SwingGallerySampleTest.ThrowingRunnable() {
                @Override
                public void run() throws Exception {
                    call(form[0], "revalidate");
                    List<String> text = SwingGallerySampleTest.paint(loader, form[0]);
                    assertTrue(TABS[index][0] + " paints " + TABS[index][1] + ": " + text + errors,
                            text.contains(TABS[index][1]));
                    if (index > 0) {
                        // The application's own listener on the selection.
                        assertEquals("Showing " + TABS[index][0], call(call(scene[0], "lookup", "#status"), "getText"));
                    }
                    painted.add(TABS[index][0]);
                }
            });
        }
        assertEquals(TABS.length, painted.size());

        // The FXML tab: the document's controller, the included document's
        // and the custom control, through the button's handler.
        SwingGallerySampleTest.onEdt(display, new SwingGallerySampleTest.ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                Object tab = ((List<?>) call(tabPane[0], "getTabs")).get(4);
                call(call(tabPane[0], "getSelectionModel"), "select", tab);
            }
        });
        drain(display);
        SwingGallerySampleTest.onEdt(display, new SwingGallerySampleTest.ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                Object name = call(scene[0], "lookup", "#nameField");
                Object greet = call(scene[0], "lookup", "#greetButton");
                Object greeting = call(scene[0], "lookup", "#greeting");
                Object badge = call(scene[0], "lookup", "#badge");
                assertNotNull("fx:id is the node's id where the document sets none", name);
                assertNotNull(greet);
                assertNotNull(greeting);
                assertNotNull(badge);
                assertEquals("com.example.fxgallery.control.StatusBadge", badge.getClass().getName());
                // From the resource bundle, by the controller's initialize().
                assertEquals("Enter a name and press Greet", call(greeting, "getText"));
                assertEquals("The button is bound to the empty name field", Boolean.TRUE, call(greet, "isDisabled"));
                call(name, "setText", "Ada");
                assertEquals(Boolean.FALSE, call(greet, "isDisabled"));
                call(greet, "fire");
                String said = (String) call(greeting, "getText");
                assertEquals("Greeted 1", call(badge, "getText"));
                assertTrue(said, said.startsWith("Hello, Ada (Developer, free) from "));
                assertEquals(1, call(badge, "getLevel"));

                // Style sheets: the scene's, looked up through .root, and
                // the one the document names.
                Object status = call(scene[0], "lookup", "#status");
                assertEquals("0x0d47a1ff", String.valueOf(call(status, "getTextFill")));
                assertEquals(15.0, ((Double) call(call(greeting, "getFont"), "getSize")).doubleValue(), 0.01);
            }
        });
        drain(display);
        assertTrue("Nothing may be thrown on the event dispatch thread: " + errors, errors.isEmpty());
    }
}
