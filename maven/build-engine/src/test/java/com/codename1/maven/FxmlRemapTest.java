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
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// FXML and style sheets through the whole of a build: the resource
/// compiler writes Java source and a binary table, javac compiles them with
/// a fixture application against the layers' real runtime jars, and the
/// remap relocates everything, ships the WHOLE JavaFX runtime and generates
/// the dispatcher. The result has to pass the compliance check with no
/// finding at all, as a project's build would run it next, and the
/// dispatcher has to reach the controller's private members.
public class FxmlRemapTest {

    private static final String APP = "package com.acme.fx;\n"
            + "import javafx.application.Application;\n"
            + "import javafx.fxml.FXMLLoader;\n"
            + "import javafx.scene.Parent;\n"
            + "import javafx.scene.Scene;\n"
            + "import javafx.stage.Stage;\n"
            + "public class Shop extends Application {\n"
            + "    public void start(Stage stage) throws Exception {\n"
            + "        Parent root = FXMLLoader.load(getClass().getResource(\"/ui/shop.fxml\"));\n"
            + "        Scene scene = new Scene(root, 300, 200);\n"
            + "        scene.getStylesheets().add(getClass().getResource(\"/ui/shop.css\").toExternalForm());\n"
            + "        stage.setScene(scene);\n"
            + "        stage.show();\n"
            + "    }\n"
            + "}\n";

    private static final String BASE = "package com.acme.fx;\n"
            + "import javafx.fxml.FXML;\n"
            + "import javafx.scene.control.Label;\n"
            + "public class BaseController {\n"
            + "    @FXML private Label status;\n"
            + "    Label status() { return status; }\n"
            + "}\n";

    private static final String CONTROLLER = "package com.acme.fx;\n"
            + "import javafx.event.ActionEvent;\n"
            + "import javafx.fxml.FXML;\n"
            + "import javafx.scene.control.Button;\n"
            + "import javafx.scene.control.TextField;\n"
            + "import javafx.scene.layout.HBox;\n"
            + "public class ShopController extends BaseController {\n"
            + "    @FXML private TextField name;\n"
            + "    @FXML private Button buy;\n"
            + "    @FXML private HBox footer;\n"
            + "    @FXML private FooterController footerController;\n"
            + "    private int bought;\n"
            + "    private boolean ready;\n"
            + "    private ShopController() { }\n"
            + "    @FXML private void initialize() { ready = name != null && status() != null && footer != null; }\n"
            + "    @FXML private void onBuy(ActionEvent e) { bought++; }\n"
            + "    @FXML private void onClear() { bought = 0; }\n"
            + "}\n";

    private static final String FOOTER = "package com.acme.fx;\n"
            + "public class FooterController implements javafx.fxml.Initializable {\n"
            + "    @javafx.fxml.FXML private javafx.scene.control.Label note;\n"
            + "    boolean initialized;\n"
            + "    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {\n"
            + "        initialized = note != null;\n"
            + "    }\n"
            + "}\n";

    private static final String SHOP_FXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<?import javafx.geometry.Insets?>\n"
            + "<?import javafx.scene.control.*?>\n"
            + "<?import javafx.scene.layout.*?>\n"
            + "<VBox xmlns:fx=\"http://javafx.com/fxml/1\" fx:controller=\"com.acme.fx.ShopController\""
            + " spacing=\"8\" alignment=\"CENTER\" styleClass=\"shop\" stylesheets=\"@shop.css\">\n"
            + "  <padding><Insets top=\"4\" right=\"4\" bottom=\"4\" left=\"4\"/></padding>\n"
            + "  <fx:define><ToggleGroup fx:id=\"group\"/></fx:define>\n"
            + "  <TextField fx:id=\"name\" promptText=\"who\"/>\n"
            + "  <Label fx:id=\"status\" text=\"${name.text}\" VBox.vgrow=\"ALWAYS\"/>\n"
            + "  <RadioButton text=\"a\" toggleGroup=\"$group\"/>\n"
            + "  <Button fx:id=\"buy\" text=\"Buy\" onAction=\"#onBuy\"/>\n"
            + "  <Button text=\"Clear\" onAction=\"#onClear\"/>\n"
            + "  <fx:include fx:id=\"footer\" source=\"footer.fxml\"/>\n"
            + "</VBox>\n";

    private static final String FOOTER_FXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<?import javafx.scene.control.Label?>\n"
            + "<?import javafx.scene.layout.HBox?>\n"
            + "<HBox xmlns:fx=\"http://javafx.com/fxml/1\" fx:controller=\"com.acme.fx.FooterController\">\n"
            + "  <Label fx:id=\"note\" text=\"note\"/>\n"
            + "</HBox>\n";

    private static final String SHOP_CSS = ".root { -brand: #336699; -fx-font-size: 14px; }\n"
            + ".shop > .button:hover { -fx-text-fill: derive(-brand, 20%); -fx-padding: 0.5em 1em; }\n"
            + ".label { -fx-text-fill: -brand;"
            + " -fx-background-color: linear-gradient(to bottom, white, -brand); }\n";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File scratch;

    private List<File> classpath() throws Exception {
        if (scratch == null) {
            scratch = tmp.newFolder("jars");
        }
        return Arrays.asList(RealCompatJars.javafx(scratch), RealCompatJars.jdk(scratch), RealCompatJars.core(scratch));
    }

    private static void write(File f, String text) throws Exception {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), text.getBytes("UTF-8"));
    }

    private static void sources(File dir, String prefix, List<String> into) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                sources(f, prefix + f.getName() + "/", into);
            } else if (f.getName().endsWith(".java")) {
                into.add(prefix + f.getName());
                into.add(new String(Files.readAllBytes(f.toPath()), "UTF-8"));
            }
        }
    }

    /// What `prepare-desktop-sources`, javac and `process-resources` leave
    /// in a project's classes directory, before the remap.
    private File build() throws Exception {
        File resources = tmp.newFolder();
        write(new File(resources, "ui/shop.fxml"), SHOP_FXML);
        write(new File(resources, "ui/footer.fxml"), FOOTER_FXML);
        write(new File(resources, "ui/shop.css"), SHOP_CSS);
        File javaOut = tmp.newFolder();
        File resourcesOut = tmp.newFolder();
        final List<String> warnings = new ArrayList<String>();
        List<String> errors = new DesktopResourceCompiler(Collections.singletonList(resources), classpath(), javaOut,
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
        assertTrue(warnings.toString(), warnings.isEmpty());

        List<String> all = new ArrayList<String>(Arrays.asList("com/acme/fx/Shop.java", APP,
                "com/acme/fx/BaseController.java", BASE, "com/acme/fx/ShopController.java", CONTROLLER,
                "com/acme/fx/FooterController.java", FOOTER));
        sources(javaOut, "", all);
        assertEquals("Two documents were compiled to source", 8 + 4, all.size());
        File classes = tmp.newFolder();
        CompatFixtures.compileAgainst(classpath(), tmp.newFolder(), classes, all.toArray(new String[0]));
        // The resource roots, as the build copies them beside the classes.
        for (File root : new File[] {resources, resourcesOut}) {
            copy(root, classes);
        }
        return classes;
    }

    private static void copy(File from, File to) throws Exception {
        File[] files = from.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            File target = new File(to, f.getName());
            if (f.isDirectory()) {
                target.mkdirs();
                copy(f, target);
            } else {
                Files.copy(f.toPath(), target.toPath());
            }
        }
    }

    private CompatRemapper remapper(File classes) throws Exception {
        File dir = tmp.newFolder();
        File record = DesktopSources.entryRecord(dir);
        Files.write(record.toPath(), "mainClass=com.acme.fx.Shop\nkind=javafx\n".getBytes("UTF-8"));
        return new CompatRemapper(classes, classpath(), null, CompatRemapperTest.LOG)
                .withDesktopEntryRecord(record).withApplicationMain("com.acme.MyApp");
    }

    private static Object field(Object target, Class<?> owner, String name) throws Exception {
        java.lang.reflect.Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    @Test
    public void theWholeRuntimeWithCompiledDocumentsAndSheetsIsCompliant() throws Exception {
        File classes = build();
        assertTrue(remapper(classes).run());

        // The whole runtime shipped, relocated, with the generated registry
        // in place of the layer's placeholder.
        assertTrue(new File(classes, "com/codename1/fxcompat/rt/CssEngine.class").isFile());
        assertTrue(new File(classes, "com/codename1/fxcompat/javafx/fxml/FXMLLoader.class").isFile());
        assertFalse(new File(classes, "javafx").exists());
        File registry = new File(classes, "com/codename1/fxcompat/rt/FxmlRegistry.class");
        assertTrue(registry.isFile());
        Set<String> members = CompatFixtures.members(Files.readAllBytes(registry.toPath()));
        assertTrue(members.toString(), members.contains("com/codename1/generated/fxml/Fxml_ui__shop.load"));
        assertTrue(members.toString(), members.contains("com/codename1/generated/fxml/Fxml_ui__footer.load"));
        assertTrue(members.toString(), members.contains("com/acme/fx/ShopController.<init>"));
        // The compiled sheet ships under its flat name.
        assertTrue(new File(classes, "ui_shop.css.cn1css").isFile()
                || new File(classes, com.codename1.compat.jdk.ResourceNames.flatName("ui/shop.css") + ".cn1css")
                        .isFile());

        new BytecodeCompliance(RealCompatJars.host(classes, tmp.newFolder(), scratch)).execute();
    }

    @Test
    public void theGeneratedDispatcherReachesPrivateMembersOfTheRelocatedController() throws Exception {
        File classes = build();
        assertTrue(remapper(classes).run());
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                FxmlRemapTest.class.getClassLoader());
        try {
            Class<?> dispatchType = loader.loadClass("com.codename1.fxcompat.rt.FxmlDispatch");
            Object dispatch = loader.loadClass("com.codename1.fxcompat.rt.FxmlRegistry").newInstance();
            assertTrue(dispatchType.isInstance(dispatch));
            Method has = dispatchType.getMethod("has", String.class);
            assertEquals(Boolean.TRUE, has.invoke(dispatch, "ui/shop.fxml"));
            assertEquals(Boolean.TRUE, has.invoke(dispatch, "ui/footer.fxml"));
            assertEquals(Boolean.FALSE, has.invoke(dispatch, "ui/other.fxml"));

            // The controller's constructor is private in the source.
            Object controller = dispatchType.getMethod("create", String.class)
                    .invoke(dispatch, "com.acme.fx.ShopController");
            assertNotNull(controller);
            Class<?> type = controller.getClass();
            assertEquals("com.acme.fx.ShopController", type.getName());

            Method inject = dispatchType.getMethod("inject", Object.class, String.class, Object.class);
            Object footerController = dispatchType.getMethod("create", String.class)
                    .invoke(dispatch, "com.acme.fx.FooterController");
            assertEquals(Boolean.TRUE, inject.invoke(dispatch, controller, "footerController", footerController));
            assertSame(footerController, field(controller, type, "footerController"));
            assertEquals(Boolean.FALSE, inject.invoke(dispatch, controller, "nothing", footerController));
            // A private field of the superclass, and one of no class.
            try {
                inject.invoke(dispatch, controller, "status", footerController);
                org.junit.Assert.fail("A controller is not a Label");
            } catch (java.lang.reflect.InvocationTargetException e) {
                assertTrue(String.valueOf(e.getCause()), e.getCause() instanceof IllegalArgumentException);
                assertTrue(e.getCause().getMessage(), e.getCause().getMessage().contains("status"));
            }

            Method invoke = dispatchType.getMethod("invoke", Object.class, String.class, Object.class);
            Object event = loader.loadClass("com.codename1.fxcompat.javafx.event.ActionEvent").newInstance();
            assertEquals(Boolean.TRUE, invoke.invoke(dispatch, controller, "onBuy", event));
            assertEquals(Boolean.TRUE, invoke.invoke(dispatch, controller, "onBuy", event));
            assertEquals(2, field(controller, type, "bought"));
            assertEquals(Boolean.TRUE, invoke.invoke(dispatch, controller, "onClear", event));
            assertEquals(0, field(controller, type, "bought"));
            assertEquals(Boolean.FALSE, invoke.invoke(dispatch, controller, "onNothing", event));

            assertEquals(Boolean.TRUE, dispatchType.getMethod("init", Object.class).invoke(dispatch, controller));
            assertEquals(Boolean.FALSE, field(controller, type, "ready"));
        } finally {
            loader.close();
        }
    }

    @Test
    public void aSecondRunChangesNothing() throws Exception {
        File classes = build();
        assertTrue(remapper(classes).run());
        File registry = new File(classes, "com/codename1/fxcompat/rt/FxmlRegistry.class");
        File controller = new File(classes, "com/acme/fx/ShopController.class");
        byte[] first = Files.readAllBytes(registry.toPath());
        byte[] widened = Files.readAllBytes(controller.toPath());
        long stamp = registry.lastModified();
        assertTrue(remapper(classes).run());
        assertTrue(Arrays.equals(first, Files.readAllBytes(registry.toPath())));
        assertTrue(Arrays.equals(widened, Files.readAllBytes(controller.toPath())));
        assertEquals(stamp, registry.lastModified());
    }

    @Test
    public void anApplicationWithoutDocumentsStillGetsARegistry() throws Exception {
        File classes = tmp.newFolder();
        CompatFixtures.compileAgainst(classpath(), tmp.newFolder(), classes, "com/acme/fx/Shop.java",
                "package com.acme.fx;\npublic class Shop extends javafx.application.Application {\n"
                        + "    public void start(javafx.stage.Stage stage) { }\n}\n");
        assertTrue(remapper(classes).run());
        // FxmlDispatch names the registry, so it has to be there to link.
        assertTrue(new File(classes, "com/codename1/fxcompat/rt/FxmlRegistry.class").isFile());
        new BytecodeCompliance(RealCompatJars.host(classes, tmp.newFolder(), scratch)).execute();
    }
}
