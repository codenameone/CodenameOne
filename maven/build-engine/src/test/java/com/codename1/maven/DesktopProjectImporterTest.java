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

import com.codename1.builders.BuildException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// cn1:import-desktop-project over small Maven and Gradle desktop projects:
/// what is copied where, how the entry point is found, the main class that
/// is written to start it, what a second import does, and how the project's
/// dependencies are reported.
public class DesktopProjectImporterTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

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

    private static File write(File root, String path, String text) throws IOException {
        File f = new File(root, path);
        assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
        Files.write(f.toPath(), text.getBytes("UTF-8"));
        return f;
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), "UTF-8");
    }

    private static final String FX_APP = "package com.acme.fx;\n\n"
            + "import javafx.application.Application;\n"
            + "import javafx.stage.Stage;\n\n"
            + "// class Decoy extends Application\n"
            + "public class App extends Application {\n"
            + "    @Override\n    public void start(Stage stage) {\n        stage.show();\n    }\n\n"
            + "    public static void main(String[] args) {\n        launch(args);\n    }\n}\n";

    private static final String SWING_MAIN = "package com.acme.swing;\n\n"
            + "import javax.swing.JFrame;\n\n"
            + "public class Main {\n"
            + "    public static void main(final String... args) {\n"
            + "        new JFrame(\"class Other extends Application\").setVisible(true);\n    }\n}\n";

    /// A JavaFX Maven project with a module descriptor, FXML and a manifest.
    private File javafxMavenProject() throws IOException {
        File p = tmp.newFolder("fxproject");
        write(p, "pom.xml", "<project>\n  <properties>\n    <main.class>com.acme.fx.App</main.class>\n  </properties>\n"
                + "  <dependencyManagement><dependencies><dependency>\n"
                + "    <groupId>managed</groupId><artifactId>only</artifactId>\n"
                + "  </dependency></dependencies></dependencyManagement>\n"
                + "  <dependencies>\n"
                + "    <dependency><groupId>org.openjfx</groupId><artifactId>javafx-controls</artifactId>"
                + "<version>21</version></dependency>\n"
                + "    <dependency>\n      <groupId>org.openjfx</groupId>\n      <artifactId>javafx-fxml</artifactId>\n"
                + "    </dependency>\n"
                + "    <dependency><groupId>org.openjfx</groupId><artifactId>javafx-web</artifactId></dependency>\n"
                + "    <dependency><groupId>com.google.code.gson</groupId><artifactId>gson</artifactId></dependency>\n"
                + "    <dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId>"
                + "<scope>test</scope></dependency>\n"
                + "    <dependency><groupId>org.example</groupId><artifactId>testkit</artifactId>"
                + "<scope>test</scope></dependency>\n"
                + "  </dependencies>\n"
                + "  <build><plugins><plugin><artifactId>javafx-maven-plugin</artifactId>\n"
                + "    <dependencies><dependency><groupId>plugin</groupId><artifactId>dep</artifactId></dependency>"
                + "</dependencies>\n"
                + "    <configuration><mainClass>${main.class}</mainClass></configuration>\n"
                + "  </plugin></plugins></build>\n</project>\n");
        write(p, "src/main/java/module-info.java", "module com.acme.fx {\n    requires javafx.controls;\n}\n");
        write(p, "src/main/java/com/acme/fx/App.java", FX_APP);
        write(p, "src/main/java/com/acme/fx/Model.java", "package com.acme.fx;\n\npublic class Model {\n}\n");
        write(p, "src/main/resources/com/acme/fx/main.fxml", "<VBox xmlns:fx=\"http://javafx.com/fxml\"/>\n");
        write(p, "src/main/resources/style.css", ".root { }\n");
        write(p, "src/main/resources/META-INF/MANIFEST.MF", "Main-Class: com.acme.fx.App\n");
        write(p, "src/test/java/com/acme/fx/AppTest.java", "package com.acme.fx;\n\nclass AppTest {\n}\n");
        return p;
    }

    private File common() throws IOException {
        File common = new File(tmp.getRoot(), "app/common");
        if (!common.isDirectory()) {
            assertTrue(common.mkdirs());
        }
        return common;
    }

    private static Map<String, String> tree(File dir) throws IOException {
        Map<String, String> out = new TreeMap<String, String>();
        tree(dir, dir, out);
        return out;
    }

    private static void tree(File root, File dir, Map<String, String> out) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                tree(root, f, out);
            } else {
                out.put(root.toURI().relativize(f.toURI()).getPath(), read(f));
            }
        }
    }

    @Test
    public void importsAJavaFxMavenProject() throws Exception {
        File source = javafxMavenProject();
        File common = common();
        DesktopProjectImporter.Result r = new DesktopProjectImporter(log).importProject(source, null, common, null);

        File desktop = new File(common, "src/main/desktop");
        assertEquals(FX_APP, read(new File(desktop, "java/com/acme/fx/App.java")));
        assertTrue(new File(desktop, "java/com/acme/fx/Model.java").isFile());
        assertTrue(new File(desktop, "resources/com/acme/fx/main.fxml").isFile());
        assertTrue(new File(desktop, "resources/style.css").isFile());
        assertEquals(4, r.copiedFiles);
        // Not a module, and packaged by the Codename One build.
        assertFalse(new File(desktop, "java/module-info.java").exists());
        assertEquals(Collections.singletonList("java/module-info.java"), r.droppedModuleInfo);
        assertFalse(new File(desktop, "resources/META-INF").exists());
        // Tests are not the application.
        assertFalse(tree(desktop).toString(), tree(desktop).toString().contains("AppTest"));

        assertEquals("com.acme.fx.App", r.mainClass);
        assertEquals("javafx", r.kind);
        // Not told the application's main class, so none is written; and
        // nothing in the desktop directory says how the application starts.
        assertNull(r.generatedMain);
        assertFalse(new File(common, "src/main/java").exists());
        assertEquals(new java.util.TreeSet<String>(Arrays.asList("java", "resources",
                DesktopProjectImporter.IMPORT_RECORD)),
                new java.util.TreeSet<String>(Arrays.asList(desktop.list())));

        assertEquals(Arrays.asList("org.openjfx:javafx-controls (JavaFX controls)", "org.openjfx:javafx-fxml (FXML)"),
                r.covered);
        // Reported, and nothing of theirs copied: no jar, no pom entry.
        assertEquals(Arrays.asList("org.openjfx:javafx-web", "com.google.code.gson:gson"), r.uncovered);
        assertFalse(r.kotlin);
        assertFalse(new File(common, "src/main/kotlin").exists());
        assertFalse(new File(common, "pom.xml").exists());
    }

    @Test
    public void importsASwingGradleModule() throws Exception {
        File source = tmp.newFolder("swingproject");
        write(source, "settings.gradle", "include 'app'\n");
        write(source, "app/build.gradle", "plugins { id 'application' }\n"
                + "application {\n    mainClass = 'com.acme.swing.Main'\n}\n"
                + "dependencies {\n"
                + "    implementation 'org.swinglabs.swingx:swingx-core:1.6.5-1'\n"
                + "    implementation(\"com.miglayout:miglayout-swing:11.3\")\n"
                + "    implementation 'com.formdev:flatlaf:3.4'\n"
                + "    testImplementation 'junit:junit:4.13.2'\n}\n");
        write(source, "app/src/main/java/com/acme/swing/Main.java", SWING_MAIN);
        write(source, "app/src/main/resources/messages.properties", "title=Hello\n");
        File common = common();

        DesktopProjectImporter.Result r = new DesktopProjectImporter(log).importProject(source, null, common, null);
        File desktop = new File(common, "src/main/desktop");
        assertEquals(SWING_MAIN, read(new File(desktop, "java/com/acme/swing/Main.java")));
        assertEquals("title=Hello\n", read(new File(desktop, "resources/messages.properties")));
        assertEquals("com.acme.swing.Main", r.mainClass);
        assertEquals("swing", r.kind);
        assertEquals(Collections.singletonList("org.swinglabs.swingx:swingx-core (SwingX)"), r.covered);
        // MiG Layout is a library written against Swing, not part of a layer:
        // the build relocates it with the application.
        assertEquals(Arrays.asList("com.miglayout:miglayout-swing", "com.formdev:flatlaf"), r.uncovered);
        // The sources are compiled against all three, so the project that
        // receives them has to declare all three: SwingX to compile against
        // only, since the layer has its own, and the others to ship.
        StringBuilder libraries = new StringBuilder();
        for (DesktopProjectImporter.Library lib : r.libraries) {
            libraries.append(lib.coordinate()).append(':').append(lib.version)
                    .append(lib.provided ? " provided\n" : " compile\n");
        }
        assertEquals("org.swinglabs.swingx:swingx-core:1.6.5-1 provided\n"
                + "com.miglayout:miglayout-swing:11.3 compile\n"
                + "com.formdev:flatlaf:3.4 compile\n", libraries.toString());
        assertEquals(3, DesktopProjectImporter.librariesOf(new File(source, "app")).size());
        assertFalse(DesktopProjectImporter.isToolkitModule("com.miglayout:miglayout-swing"));
        assertTrue(DesktopProjectImporter.BUNDLED_NOTE.contains("bundled and relocated"));
        assertEquals(source.getName(), DesktopProjectImporter.moduleDir(source, null).getParentFile().getName());
        assertEquals("Swing", DesktopSources.toolkitsNamedIn(desktop));
    }

    /// A Maven project's dependencies, with a version held in a property and
    /// one that only the parent's dependency management knows.
    @Test
    public void aMavenProjectsLibrariesAreReadWithTheirVersions() throws Exception {
        File source = tmp.newFolder("mavenlibs");
        write(source, "pom.xml", "<project>\n<properties><mig.version>5.3</mig.version></properties>\n"
                + "<dependencyManagement><dependencies><dependency><groupId>org.managed</groupId>"
                + "<artifactId>bom</artifactId><version>1</version></dependency></dependencies>"
                + "</dependencyManagement>\n"
                + "<dependencies>\n"
                + "<dependency><groupId>com.miglayout</groupId><artifactId>miglayout-swing</artifactId>"
                + "<version>${mig.version}</version></dependency>\n"
                + "<dependency><groupId>org.swinglabs.swingx</groupId><artifactId>swingx-all</artifactId>"
                + "<version>1.6.5-1</version></dependency>\n"
                + "<dependency><groupId>org.example</groupId><artifactId>managed</artifactId></dependency>\n"
                + "<dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version>"
                + "<scope>test</scope></dependency>\n"
                + "</dependencies>\n</project>\n");
        StringBuilder libraries = new StringBuilder();
        for (DesktopProjectImporter.Library lib : DesktopProjectImporter.librariesOf(source)) {
            libraries.append(lib.coordinate()).append(':').append(lib.version)
                    .append(lib.provided ? " provided\n" : " compile\n");
        }
        assertEquals("com.miglayout:miglayout-swing:5.3 compile\n"
                + "org.swinglabs.swingx:swingx-all:1.6.5-1 provided\n"
                + "org.example:managed:null compile\n", libraries.toString());
    }

    /// What the desktop host supplies -- Maven's provided scope, Gradle's
    /// compileOnly -- stays provided, so it is never bundled into the app;
    /// declared both ways, it ships.
    @Test
    public void hostSuppliedLibrariesStayProvided() throws Exception {
        File maven = tmp.newFolder("providedmaven");
        write(maven, "pom.xml", "<project>\n<dependencies>\n"
                + "<dependency><groupId>org.host</groupId><artifactId>api</artifactId><version>2</version>"
                + "<scope>provided</scope></dependency>\n"
                + "<dependency><groupId>org.app</groupId><artifactId>lib</artifactId><version>3</version>"
                + "</dependency>\n"
                + "</dependencies>\n</project>\n");
        assertEquals("org.host:api:2 provided\norg.app:lib:3 compile\n", described(maven));
        File gradle = tmp.newFolder("providedgradle");
        write(gradle, "build.gradle", "dependencies {\n"
                + "    compileOnly 'org.host:api:2'\n"
                + "    implementation(\"org.app:lib:3\")\n"
                + "    compileOnly 'org.both:lib:4'\n"
                + "    runtimeOnly 'org.both:lib:4'\n"
                + "}\n");
        assertEquals("org.host:api:2 provided\norg.app:lib:3 compile\norg.both:lib:4 compile\n",
                described(gradle));
    }

    private static String described(File moduleDir) throws Exception {
        StringBuilder libraries = new StringBuilder();
        for (DesktopProjectImporter.Library lib : DesktopProjectImporter.librariesOf(moduleDir)) {
            libraries.append(lib.coordinate()).append(':').append(lib.version)
                    .append(lib.provided ? " provided\n" : " compile\n");
        }
        return libraries.toString();
    }

    private File swingProject(String folder) throws IOException {
        File source = tmp.newFolder(folder);
        write(source, "src/main/java/com/acme/swing/Main.java", SWING_MAIN);
        return source;
    }

    /// The application's main class is written to start the Swing
    /// application, and the one the project template wrote is kept beside it.
    @Test
    public void theTemplatesMainClassIsRewrittenToStartTheApplication() throws Exception {
        File common = common();
        String template = "package com.acme;\npublic class MyApp extends com.codename1.system.Lifecycle {\n}\n";
        File main = write(common, "src/main/java/com/acme/MyApp.java", template);

        DesktopProjectImporter.Result r = new DesktopProjectImporter(log).importProject(swingProject("rewrite"),
                null, common, null, "com.acme", "MyApp");
        assertEquals("com.acme.MyApp", r.generatedMain);
        assertEquals(main, r.mainSource);
        assertFalse(r.library);
        String written = read(main);
        assertEquals(DesktopProjectImporter.mainClassSource("com.acme.MyApp",
                new DesktopProjectImporter.Candidate("com.acme.swing.Main"), false), written);
        assertTrue(written, written.startsWith(DesktopProjectImporter.WRITTEN_BY + "\npackage com.acme;\n"));
        assertTrue(written, written.contains("public class MyApp extends "
                + "com.codename1.desktopcompat.rt.DesktopLifecycle {\n"));
        assertTrue(written, written.contains("    protected void runMain() throws Exception {\n"
                + "        com.acme.swing.Main.main(new String[0]);\n    }\n"));
        File aside = new File(main.getPath() + DesktopProjectImporter.SET_ASIDE_SUFFIX);
        assertEquals(aside, r.setAside);
        assertEquals(template, read(aside));

        // Importing again writes the same class, and keeps the first copy.
        r = new DesktopProjectImporter(log).importProject(swingProject("rewrite2"), null, common, null, "com.acme",
                "MyApp");
        assertEquals("com.acme.MyApp", r.generatedMain);
        assertNull(r.setAside);
        assertEquals(written, read(main));
        assertEquals(template, read(aside));

        // A main class the developer has changed since is theirs.
        String own = "package com.acme;\npublic class MyApp extends com.codename1.desktopcompat.rt.DesktopLifecycle {\n"
                + "    protected void runMain() { com.acme.swing.Main.main(new String[] {\"-x\"}); }\n}\n";
        write(common, "src/main/java/com/acme/MyApp.java", own);
        logged.clear();
        r = new DesktopProjectImporter(log).importProject(swingProject("rewrite3"), null, common, null, "com.acme",
                "MyApp");
        assertEquals(own, read(main));
        assertNull(r.generatedMain);
        assertEquals(template, read(aside));
        assertTrue(logged.toString(), logged.toString().contains("warn: MyApp.java was changed after an earlier"));
    }

    /// A JavaFX application's main class creates the Application subclass.
    @Test
    public void aJavaFxApplicationIsCreatedByTheMainClass() throws Exception {
        File common = common();
        DesktopProjectImporter.Result r = new DesktopProjectImporter(log).importProject(javafxMavenProject(), null,
                common, null, "com.acme", "MyApp");
        File main = new File(common, "src/main/java/com/acme/MyApp.java");
        assertEquals(main, r.mainSource);
        assertNull("there was nothing to keep", r.setAside);
        String written = read(main);
        assertTrue(written, written.contains("public class MyApp extends com.codename1.fxcompat.runtime.FxLifecycle {\n"
                + "    @Override\n    protected javafx.application.Application createApplication() {\n"
                + "        return new com.acme.fx.App();\n    }\n}\n"));
        // The desktop main is not what starts a JavaFX application.
        assertFalse(written, written.contains("main("));
    }

    /// A main class that is not public is reached through a launcher in its
    /// own package; a Kotlin main without parameters is called as one.
    @Test
    public void whatTheMainClassCannotNameIsCalledThroughALauncher() throws Exception {
        File source = tmp.newFolder("hidden");
        write(source, "src/main/java/com/acme/swing/Hidden.java", "package com.acme.swing;\n\nclass Hidden {\n"
                + "    public static void main(String[] args) {\n    }\n}\n");
        File common = common();
        DesktopProjectImporter.Result r = new DesktopProjectImporter(log).importProject(source, null, common, null,
                "com.acme", "MyApp");
        assertEquals("com.acme.swing.Hidden", r.mainClass);
        String main = read(r.mainSource);
        assertTrue(main, main.contains("        com.acme.swing." + DesktopProjectImporter.LAUNCHER + ".launch();\n"));
        String launcher = read(new File(common, "src/main/java/com/acme/swing/" + DesktopProjectImporter.LAUNCHER
                + ".java"));
        assertTrue(launcher, launcher.contains("public static void launch() throws Exception {\n"
                + "        com.acme.swing.Hidden.main(new String[0]);\n"));

        Map<String, DesktopProjectImporter.Candidate> out = new TreeMap<String, DesktopProjectImporter.Candidate>();
        DesktopProjectImporter.scanKotlin("App", "package k\n\nfun main() {\n}\n", out);
        DesktopProjectImporter.Candidate kotlin = out.get("k.AppKt");
        assertTrue(kotlin.noArgMain);
        assertTrue(DesktopProjectImporter.mainClassSource("a.B", kotlin, false), DesktopProjectImporter
                .mainClassSource("a.B", kotlin, false).contains("        k.AppKt.main();\n"));
        assertFalse(DesktopProjectImporter.javaPublic("package a;\nfinal class X {\n}\n", "X"));
        assertTrue(DesktopProjectImporter.javaPublic("package a;\n@Deprecated public final class X {\n"
                + "    public static class Y {\n    }\n}\n", "X$Y"));
        assertFalse(DesktopProjectImporter.javaPublic("package a;\npublic class X {\n    static class Y {\n"
                + "    }\n}\n", "X$Y"));
    }

    /// Imported as a library, the sources are copied and the application's
    /// main class is left alone; a project with nothing to start is fine.
    @Test
    public void aLibraryImportLeavesTheMainClassAlone() throws Exception {
        File common = common();
        String template = "package com.acme;\npublic class MyApp extends com.codename1.system.Lifecycle {\n}\n";
        File main = write(common, "src/main/java/com/acme/MyApp.java", template);
        File source = tmp.newFolder("screens");
        write(source, "src/main/java/com/acme/screens/Screen.java", "package com.acme.screens;\n\n"
                + "public class Screen extends javax.swing.JPanel {\n}\n");
        DesktopProjectImporter.Result r = new DesktopProjectImporter(log).importProject(source, null, common, null,
                "com.acme", "MyApp", true);
        assertTrue(r.library);
        assertNull(r.generatedMain);
        assertNull(r.mainClass);
        assertEquals(template, read(main));
        assertTrue(new File(common, "src/main/desktop/java/com/acme/screens/Screen.java").isFile());
    }

    /// A project whose main class starts an imported Android application
    /// starts that one: the desktop sources are its library.
    @Test
    public void anAndroidMainClassMakesTheDesktopSourcesALibrary() throws Exception {
        File common = common();
        String android = "package com.acme;\n\npublic class MyApp extends "
                + "com.codename1.androidcompat.runtime.AndroidLifecycle {\n"
                + "    public MyApp() {\n        super(new com.codename1.generated.android.AndroidAppImpl());\n"
                + "    }\n}\n";
        File main = write(common, "src/main/java/com/acme/MyApp.java", android);
        DesktopProjectImporter.Result r = new DesktopProjectImporter(log).importProject(swingProject("both"), null,
                common, null, "com.acme", "MyApp");
        assertTrue(r.library);
        assertNull(r.generatedMain);
        assertEquals(android, read(main));
        assertFalse(new File(main.getPath() + DesktopProjectImporter.SET_ASIDE_SUFFIX).exists());
        assertTrue(new File(common, "src/main/desktop/java/com/acme/swing/Main.java").isFile());
    }

    /// The main class cannot share a name with a class of the imported
    /// application: nothing is copied, and the message says what to change.
    @Test
    public void anImportedClassNamedLikeTheMainClassStopsTheImport() throws Exception {
        File common = common();
        try {
            new DesktopProjectImporter(log).importProject(swingProject("clash"), null, common, null, "com.acme.swing",
                    "Main");
            fail("The import must stop");
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Nothing was imported"));
            assertTrue(e.getMessage(), e.getMessage().contains("codename1.mainName"));
        }
        assertFalse(new File(common, "src/main/desktop").exists());
    }

    /// A launcher beside the Application subclass is how a JavaFX project is
    /// made runnable from a plain jar. The build names the launcher; the
    /// application is the subclass.
    @Test
    public void aLauncherNamedByTheBuildMeansTheApplicationClass() throws Exception {
        File source = tmp.newFolder("launcher");
        write(source, "build.gradle.kts", "plugins { application; id(\"org.openjfx.javafxplugin\") }\n"
                + "javafx {\n    modules = listOf(\"javafx.controls\", \"javafx.media\")\n}\n"
                + "application {\n    mainClass.set(\"com.acme.fx.Launcher\")\n}\n"
                + "dependencies {\n    implementation(libs.commons.lang)\n}\n");
        write(source, "src/main/java/com/acme/fx/App.java", FX_APP);
        write(source, "src/main/java/com/acme/fx/Launcher.java", "package com.acme.fx;\n\npublic class Launcher {\n"
                + "    public static void main(String[] args) {\n        App.main(args);\n    }\n}\n");

        DesktopProjectImporter.Result r = new DesktopProjectImporter(log).importProject(source, null, common(), null);
        assertEquals("com.acme.fx.App", r.mainClass);
        assertEquals("javafx", r.kind);
        assertEquals(Collections.singletonList("org.openjfx:javafx-controls (JavaFX controls)"), r.covered);
        assertEquals(Collections.singletonList("org.openjfx:javafx-media"), r.uncovered);
        assertTrue(DesktopProjectImporter.isToolkitModule(r.uncovered.get(0)));
        assertEquals(1, r.unresolved.size());
        assertTrue(r.unresolved.get(0), r.unresolved.get(0).contains("version catalog"));
        assertEquals("JavaFX", DesktopSources.toolkitsNamedIn(new File(common(), "src/main/desktop")));
    }

    @Test
    public void severalEntryPointsAndNoMainClassSettingFailBeforeCopying() throws Exception {
        File source = tmp.newFolder("two");
        write(source, "pom.xml", "<project/>\n");
        write(source, "src/main/java/com/acme/swing/Main.java", SWING_MAIN);
        write(source, "src/main/java/com/acme/swing/Tool.java", "package com.acme.swing;\n\npublic class Tool {\n"
                + "    static public void main(String args[]) {\n    }\n}\n");
        File common = common();
        try {
            new DesktopProjectImporter(log).importProject(source, null, common, null);
            fail("two main classes and nothing to choose by");
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("com.acme.swing.Main (swing)"));
            assertTrue(e.getMessage(), e.getMessage().contains("com.acme.swing.Tool (swing)"));
            assertTrue(e.getMessage(), e.getMessage().contains("-Dcn1.desktop.mainClass="));
        }
        assertFalse("nothing is imported", new File(common, "src").exists());

        DesktopProjectImporter.Result r = new DesktopProjectImporter(log)
                .importProject(source, null, common, "com.acme.swing.Tool");
        assertEquals("com.acme.swing.Tool", r.mainClass);

        try {
            new DesktopProjectImporter(log).importProject(source, null, common, "com.acme.swing.Missing");
            fail("the override names no entry point");
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("com.acme.swing.Missing"));
            assertTrue(e.getMessage(), e.getMessage().contains("com.acme.swing.Main"));
        }
    }

    @Test
    public void theBuildsMainClassDecidesBetweenSeveral() throws Exception {
        File source = tmp.newFolder("two");
        write(source, "pom.xml", "<project><properties><exec.mainClass>com.acme.swing/com.acme.swing.Tool"
                + "</exec.mainClass></properties></project>\n");
        write(source, "src/main/java/com/acme/swing/Main.java", SWING_MAIN);
        write(source, "src/main/java/com/acme/swing/Tool.java", "package com.acme.swing;\n\npublic class Tool {\n"
                + "    public static void main(String[] args) {\n    }\n}\n");
        assertEquals(Collections.singletonList("com.acme.swing.Tool"),
                DesktopProjectImporter.declaredMainClasses(source, source));
        DesktopProjectImporter.Result r = new DesktopProjectImporter(log).importProject(source, null, common(), null);
        assertEquals("com.acme.swing.Tool", r.mainClass);
        assertEquals("swing", r.kind);
    }

    @Test
    public void aProjectWithNothingToStartIsRefused() throws Exception {
        File source = tmp.newFolder("library");
        write(source, "src/main/java/com/acme/Util.java", "package com.acme;\n\npublic class Util {\n}\n");
        try {
            new DesktopProjectImporter(log).importProject(source, null, common(), null);
            fail("no entry point");
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("javafx.application.Application"));
        }
        assertFalse(new File(common(), "src").exists());
    }

    @Test
    public void namesTheModulesWhenSeveralHaveSources() throws Exception {
        File source = tmp.newFolder("multi");
        write(source, "ui/src/main/java/com/acme/swing/Main.java", SWING_MAIN);
        write(source, "core/src/main/java/com/acme/Core.java", "package com.acme;\n\npublic class Core {\n}\n");
        try {
            DesktopProjectImporter.moduleDir(source, null);
            fail("ambiguous");
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("core, ui"));
            assertTrue(e.getMessage(), e.getMessage().contains("-Dcn1.desktop.module="));
        }
        assertEquals(new File(source, "ui"), DesktopProjectImporter.moduleDir(source, "ui"));
        try {
            DesktopProjectImporter.moduleDir(source, "missing");
            fail("no such module");
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("missing"));
        }
        DesktopProjectImporter.Result r = new DesktopProjectImporter(log).importProject(source, "ui", common(), null);
        assertEquals("com.acme.swing.Main", r.mainClass);
        assertFalse(new File(common(), "src/main/desktop/java/com/acme/Core.java").exists());
    }

    /// A file that was in the tree before any import is the developer's: an
    /// import does not write over it, and never takes it for its own.
    @Test
    public void aFileNoImportWroteIsNotOverwritten() throws Exception {
        File source = javafxMavenProject();
        File common = common();
        File desktop = new File(common, "src/main/desktop");
        File mine = write(desktop, "java/com/acme/fx/Model.java",
                "package com.acme.fx;\n\npublic class Model {\n    int mine;\n}\n");
        new DesktopProjectImporter(log).importProject(source, null, common, null);
        assertTrue(read(mine).contains("int mine;"));
        assertTrue(logged.toString(), logged.toString().contains("Kept " + mine));

        // Not on the record, so a later import leaves it too, and one from
        // a project that dropped the path does not remove it.
        assertTrue(new File(source, "src/main/java/com/acme/fx/Model.java").delete());
        new DesktopProjectImporter(log).importProject(source, null, common, null);
        assertTrue(read(mine).contains("int mine;"));
    }

    /// Importing again updates what the first import wrote and nothing else:
    /// the developer's edits and additions stay, and a file that left the
    /// source project is removed.
    @Test
    public void aSecondImportUpdatesOnlyUntouchedFiles() throws Exception {
        File source = javafxMavenProject();
        File common = common();
        new DesktopProjectImporter(log).importProject(source, null, common, null);
        File desktop = new File(common, "src/main/desktop");
        Map<String, String> first = tree(desktop);

        // Unchanged source, unchanged result.
        DesktopProjectImporter.Result again = new DesktopProjectImporter(log).importProject(source, null, common, null);
        assertEquals(first, tree(desktop));
        assertEquals("com.acme.fx.App", again.mainClass);

        File edited = new File(desktop, "java/com/acme/fx/Model.java");
        Files.write(edited.toPath(), "package com.acme.fx;\n\npublic class Model {\n    int mine;\n}\n".getBytes("UTF-8"));
        File added = write(desktop, "java/com/acme/fx/Mine.java", "package com.acme.fx;\n\nclass Mine {\n}\n");
        write(source, "src/main/java/com/acme/fx/Model.java", "package com.acme.fx;\n\npublic class Model {\n"
                + "    int upstream;\n}\n");
        write(source, "src/main/resources/style.css", ".root { -fx-font-size: 12; }\n");
        assertTrue(new File(source, "src/main/resources/com/acme/fx/main.fxml").delete());
        write(source, "src/main/resources/com/acme/fx/other.fxml", "<HBox/>\n");

        logged.clear();
        new DesktopProjectImporter(log).importProject(source, null, common, null);
        assertTrue("the developer's edit is kept", read(edited).contains("int mine;"));
        assertTrue(logged.toString(), logged.toString().contains("Kept " + edited));
        assertTrue("the developer's own file is kept", added.isFile());
        assertEquals(".root { -fx-font-size: 12; }\n", read(new File(desktop, "resources/style.css")));
        assertFalse("removed upstream, untouched here", new File(desktop, "resources/com/acme/fx/main.fxml").exists());
        assertTrue(new File(desktop, "resources/com/acme/fx/other.fxml").isFile());

        // Deleting the kept file takes the project's copy on the next import.
        assertTrue(edited.delete());
        new DesktopProjectImporter(log).importProject(source, null, common, null);
        assertTrue(read(edited).contains("int upstream;"));
    }

    @Test
    public void kotlinSourcesSwitchTheKotlinBuildOn() throws Exception {
        File source = tmp.newFolder("kt");
        write(source, "src/main/kotlin/com/acme/kt/App.kt", "package com.acme.kt\n\n"
                + "import javafx.application.Application\nimport javafx.stage.Stage\n\n"
                + "class App : Application() {\n    override fun start(stage: Stage) {\n        stage.show()\n    }\n}\n");
        write(source, "src/main/kotlin/com/acme/kt/main.kt", "package com.acme.kt\n\n"
                + "fun main(args: Array<String>) {\n    Application.launch(App::class.java, *args)\n}\n");
        write(source, "build.gradle.kts", "application {\n    mainClass.set(\"com.acme.kt.MainKt\")\n}\n"
                + "dependencies {\n    implementation(\"org.jetbrains.kotlin:kotlin-stdlib:2.0.0\")\n}\n");
        File common = common();
        DesktopProjectImporter.Result r = new DesktopProjectImporter(log).importProject(source, null, common, null);
        assertTrue(r.kotlin);
        assertTrue(new File(common, "src/main/kotlin/.keep").isFile());
        assertTrue(new File(common, "src/main/desktop/kotlin/com/acme/kt/App.kt").isFile());
        // The facade class is a launcher; the application is the subclass.
        assertEquals("com.acme.kt.App", r.mainClass);
        assertEquals("javafx", r.kind);
        assertEquals(Collections.singletonList("org.jetbrains.kotlin:kotlin-stdlib (Kotlin standard library)"),
                r.covered);
    }

    @Test
    public void findsEntryPointsInSourceText() {
        Map<String, DesktopProjectImporter.Candidate> out = new TreeMap<String, DesktopProjectImporter.Candidate>();
        DesktopProjectImporter.scanJava("App", FX_APP, out);
        assertEquals(Collections.singleton("com.acme.fx.App"), out.keySet());
        assertTrue(out.get("com.acme.fx.App").javafx);

        // Another class called Application is not JavaFX's.
        out.clear();
        DesktopProjectImporter.scanJava("Server", "package a;\nimport org.other.Application;\n"
                + "public class Server extends Application {\n}\n", out);
        assertTrue(out.toString(), out.isEmpty());

        out.clear();
        DesktopProjectImporter.scanJava("Outer", "import javafx.application.*;\n"
                + "public class Outer {\n    public static class Inner extends Application {\n    }\n"
                + "    void main(String[] a) {\n    }\n}\n", out);
        assertEquals(Collections.singleton("Outer$Inner"), out.keySet());

        // The class a main is written in, not the one the file is named for.
        out.clear();
        DesktopProjectImporter.scanJava("Outer", "package a;\npublic class Outer {\n    int[] sizes = {1, 2};\n"
                + "    static class Launcher {\n        public static void main(String[] args) {\n        }\n    }\n}\n",
                out);
        assertEquals(Collections.singleton("a.Outer$Launcher"), out.keySet());
        out.clear();
        DesktopProjectImporter.scanJava("Model", "package a;\npublic class Model {\n    Class<?> c = Model.class;\n}\n"
                + "class Start {\n    public static void main(String... args) {\n    }\n}\n", out);
        assertEquals(Collections.singleton("a.Start"), out.keySet());
        out.clear();
        DesktopProjectImporter.scanJava("Plain", "package a;\npublic class Plain {\n"
                + "    public static void main(String[] args) {\n    }\n}\n", out);
        assertEquals(Collections.singleton("a.Plain"), out.keySet());

        out.clear();
        DesktopProjectImporter.scanJava("Q", "package q;\npublic class Q extends javafx.application.Application {\n}\n",
                out);
        assertTrue(out.get("q.Q").javafx);

        out.clear();
        DesktopProjectImporter.scanKotlin("Tool", "@file:JvmName(\"Start\")\npackage k\n\nfun main() {\n}\n", out);
        assertEquals(Collections.singleton("k.Start"), out.keySet());
        assertFalse(out.get("k.Start").javafx);

        out.clear();
        DesktopProjectImporter.scanKotlin("Boot", "package k\n\nobject Boot {\n    @JvmStatic fun main(a: Array<String>) {\n"
                + "    }\n}\n", out);
        assertEquals(Collections.singleton("k.Boot"), out.keySet());
        assertNull(out.get("k.BootKt"));
    }

    @Test
    public void desktopSourcesWithoutARuntimeAreRefused() throws Exception {
        File desktop = tmp.newFolder("desktop");
        try {
            DesktopSources.requireRuntime(desktop, Arrays.asList("codenameone-core", "java-runtime"));
            fail("neither runtime is a dependency");
        } catch (BuildException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("codenameone-swing-compat"));
            assertTrue(e.getMessage(), e.getMessage().contains("codenameone-javafx-compat"));
        }
        DesktopSources.requireRuntime(desktop, Arrays.asList("codenameone-core", "codenameone-javafx-compat"));
        DesktopSources.requireRuntime(desktop, Collections.singletonList("codenameone-swing-compat"));
        // No desktop sources, nothing to require.
        DesktopSources.requireRuntime(new File(desktop, "missing"), Collections.<String>emptyList());
        assertFalse(DesktopSources.isDesktopProject(new File(desktop, "missing")));
        assertTrue(DesktopSources.isDesktopProject(desktop));
        assertEquals("", DesktopSources.toolkitsNamedIn(desktop));
    }

    private static String libraries(File module) throws Exception {
        StringBuilder libraries = new StringBuilder();
        for (DesktopProjectImporter.Library lib : DesktopProjectImporter.librariesOf(module)) {
            libraries.append(lib.coordinate()).append(':').append(lib.version)
                    .append(lib.provided ? " provided\n" : " compile\n");
        }
        return libraries.toString();
    }

    /// Maven hands a parent's `<dependencies>` down to every module: they
    /// are the module's although its own POM does not spell them out.
    @Test
    public void aParentsDependenciesAreItsModules() throws Exception {
        File source = tmp.newFolder("inherited");
        write(source, "pom.xml", "<project>\n<groupId>com.acme</groupId><artifactId>root</artifactId>\n"
                + "<properties><gson.version>2.10</gson.version><mig.version>11.3</mig.version></properties>\n"
                + "<dependencyManagement><dependencies><dependency><groupId>org.managed</groupId>"
                + "<artifactId>only-managed</artifactId><version>1</version></dependency></dependencies>"
                + "</dependencyManagement>\n"
                + "<dependencies>\n"
                + "<dependency><groupId>com.google.code.gson</groupId><artifactId>gson</artifactId>"
                + "<version>${gson.version}</version></dependency>\n"
                + "<dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version>"
                + "<scope>test</scope></dependency>\n"
                + "</dependencies>\n</project>\n");
        write(source, "ui/pom.xml", "<project>\n<parent><groupId>com.acme</groupId><artifactId>root</artifactId>"
                + "<version>1</version></parent>\n<artifactId>ui-parent</artifactId>\n"
                + "<dependencies>\n"
                + "<dependency><groupId>com.formdev</groupId><artifactId>flatlaf</artifactId>"
                + "<version>3.4</version></dependency>\n"
                + "<dependency><groupId>com.miglayout</groupId><artifactId>miglayout-swing</artifactId>"
                + "<version>5.0</version></dependency>\n"
                + "</dependencies>\n</project>\n");
        write(source, "ui/app/pom.xml", "<project>\n<parent><groupId>com.acme</groupId>"
                + "<artifactId>ui-parent</artifactId><version>1</version></parent>\n<artifactId>app</artifactId>\n"
                + "<dependencies>\n"
                + "<dependency><groupId>com.miglayout</groupId><artifactId>miglayout-swing</artifactId>"
                + "<version>${mig.version}</version></dependency>\n"
                + "</dependencies>\n</project>\n");
        // The module's own first, then each parent's; what the module says
        // itself wins, and a property is inherited like a dependency.
        assertEquals("com.miglayout:miglayout-swing:11.3 compile\n"
                + "com.formdev:flatlaf:3.4 compile\n"
                + "com.google.code.gson:gson:2.10 compile\n", libraries(new File(source, "ui/app")));
        DesktopProjectImporter.Result all = new DesktopProjectImporter.Result();
        DesktopProjectImporter.readDependencies(new File(source, "ui/app"), all);
        assertTrue(all.unresolved.toString(), all.unresolved.isEmpty());

        // A dependency kept commented out is not one of the project's.
        write(source, "commented/pom.xml", "<project>\n<dependencies>\n"
                + "<!-- <dependency><groupId>org.old</groupId><artifactId>gone</artifactId>"
                + "<version>1.0</version></dependency> -->\n"
                + "<dependency><groupId>com.formdev</groupId><artifactId>flatlaf</artifactId>"
                + "<version>3.4</version></dependency>\n<!--\n<dependency>\n<groupId>org.old</groupId>\n"
                + "<artifactId>other</artifactId>\n</dependency>\n-->\n</dependencies>\n</project>\n");
        assertEquals("com.formdev:flatlaf:3.4 compile\n", libraries(new File(source, "commented")));

        // A parent named somewhere else in the tree.
        write(source, "elsewhere/pom.xml", "<project>\n<parent><groupId>com.acme</groupId>"
                + "<artifactId>ui-parent</artifactId><version>1</version>"
                + "<relativePath>../ui</relativePath></parent>\n<artifactId>other</artifactId>\n</project>\n");
        assertEquals("com.formdev:flatlaf:3.4 compile\n"
                + "com.miglayout:miglayout-swing:5.0 compile\n"
                + "com.google.code.gson:gson:2.10 compile\n", libraries(new File(source, "elsewhere")));

        // A parent that is not a file of the project is not fetched, and
        // the import says that it does not know what it declares: one that
        // is said to be in a repository, and one that is simply not there.
        for (String relative : new String[] {"<relativePath/>", ""}) {
            File lone = tmp.newFolder();
            write(lone, "module/pom.xml", "<project>\n<parent><groupId>org.springframework.boot</groupId>"
                    + "<artifactId>spring-boot-starter-parent</artifactId><version>3</version>" + relative
                    + "</parent>\n<artifactId>module</artifactId>\n"
                    + "<dependencies>\n<dependency><groupId>com.formdev</groupId><artifactId>flatlaf</artifactId>"
                    + "<version>3.4</version></dependency>\n</dependencies>\n</project>\n");
            // The directory above holds a POM, of some other project.
            write(lone, "pom.xml", "<project><artifactId>unrelated</artifactId><dependencies><dependency>"
                    + "<groupId>org.stray</groupId><artifactId>stray</artifactId><version>1</version></dependency>"
                    + "</dependencies></project>\n");
            DesktopProjectImporter.Result r = new DesktopProjectImporter.Result();
            DesktopProjectImporter.readDependencies(new File(lone, "module"), r);
            assertEquals(Collections.singletonList("com.formdev:flatlaf"), r.uncovered);
            assertEquals(1, r.unresolved.size());
            assertTrue(r.unresolved.get(0), r.unresolved.get(0).startsWith(
                    "the parent POM org.springframework.boot:spring-boot-starter-parent is not among"));
        }
    }

    /// A file of the parent's artifact id where the parent is looked for is
    /// the parent only when its group and version are the ones asked for;
    /// Maven goes to a repository for any other, and so nothing of that
    /// file is the module's.
    @Test
    public void aLocalPomOfAnotherGroupOrVersionIsNotTheParent() throws Exception {
        String[][] cases = {
            // what the file above declares, and whether it is the parent
            {"<groupId>com.acme</groupId><artifactId>root</artifactId><version>1</version>", "yes"},
            {"<groupId>org.other</groupId><artifactId>root</artifactId><version>1</version>", "no"},
            {"<groupId>com.acme</groupId><artifactId>root</artifactId><version>2</version>", "no"},
            // Group and version inherited from a parent of its own; the
            // group id of its dependency is not the project's.
            {"<parent><groupId>com.acme</groupId><artifactId>top</artifactId><version>1</version>"
                    + "<relativePath/></parent><artifactId>root</artifactId>", "yes"},
            {"<parent><groupId>org.other</groupId><artifactId>top</artifactId><version>1</version>"
                    + "<relativePath/></parent><artifactId>root</artifactId>", "no"},
            {"<groupId>com.acme</groupId><artifactId>root</artifactId><version>${revision}</version>", "yes"},
        };
        for (String[] c : cases) {
            File source = tmp.newFolder();
            write(source, "pom.xml", "<project>\n" + c[0] + "\n<dependencies>\n<dependency>"
                    + "<groupId>com.miglayout</groupId><artifactId>miglayout-swing</artifactId>"
                    + "<version>5.0</version></dependency>\n</dependencies>\n</project>\n");
            write(source, "app/pom.xml", "<project>\n<parent><groupId>com.acme</groupId>"
                    + "<artifactId>root</artifactId><version>1</version></parent>\n<artifactId>app</artifactId>\n"
                    + "</project>\n");
            DesktopProjectImporter.Result r = new DesktopProjectImporter.Result();
            DesktopProjectImporter.readDependencies(new File(source, "app"), r);
            boolean parent = "yes".equals(c[1]);
            // The file's own parent, `top`, is nowhere and is reported too.
            assertEquals(c[0], !parent, r.unresolved.toString().contains("the parent POM com.acme:root "));
            assertEquals(c[0], parent, !r.libraries.isEmpty());
        }
    }

    /// `public` spelled out after `@JvmStatic` is the same entry point.
    @Test
    public void aKotlinJvmStaticMainMayBePublic() {
        Map<String, DesktopProjectImporter.Candidate> out =
                new java.util.LinkedHashMap<String, DesktopProjectImporter.Candidate>();
        DesktopProjectImporter.scanKotlin("Boot", "package k\n\nobject Boot {\n    @JvmStatic public fun main("
                + "a: Array<String>) {\n    }\n}\n", out);
        assertEquals(Collections.singleton("k.Boot"), out.keySet());
    }

    /// Whether the generated main class's name is taken is a question of
    /// what the Kotlin sources declare, not of what their files are called.
    @Test
    public void aKotlinClassIsFoundByItsDeclaration() throws Exception {
        File main = tmp.newFolder();
        write(main, "kotlin/com/foo/MyApp.kt", "package com.foo\n\nfun helper() = 1\n");
        assertFalse(DesktopProjectImporter.declares(main, "com.foo.MyApp"));
        write(main, "kotlin/com/foo/Other.kt", "package com.foo\n\n// class MyApp\ndata class MyApp(val a: Int)\n");
        assertTrue(DesktopProjectImporter.declares(main, "com.foo.MyApp"));
        assertFalse(DesktopProjectImporter.declares(main, "com.bar.MyApp"));
        File named = tmp.newFolder();
        write(named, "kotlin/x/Start.kt", "@file:JvmName(\"Launcher\")\npackage x\n\nfun main() {}\n");
        assertTrue(DesktopProjectImporter.declares(named, "x.Launcher"));
        File nested = tmp.newFolder();
        write(nested, "kotlin/x/A.kt", "package x\n\nclass A {\n    class MyApp\n}\n");
        assertFalse(DesktopProjectImporter.declares(nested, "x.MyApp"));
        File java = tmp.newFolder();
        write(java, "java/x/MyApp.java", "package x; class MyApp {}\n");
        assertTrue(DesktopProjectImporter.declares(java, "x.MyApp"));
    }

    /// `public` is Kotlin's default visibility written out, and a
    /// `suspend` main gets the same static facade method.
    @Test
    public void aKotlinTopLevelMainMayCarryModifiers() {
        Map<String, DesktopProjectImporter.Candidate> out =
                new java.util.LinkedHashMap<String, DesktopProjectImporter.Candidate>();
        for (String main : new String[] {"public fun main() {\n}\n", "suspend fun main() {\n}\n",
            "public suspend fun main(args: Array<String>) {\n}\n"}) {
            out.clear();
            DesktopProjectImporter.scanKotlin("Tool", "package k\n\n" + main, out);
            assertEquals(main, Collections.singleton("k.ToolKt"), out.keySet());
        }
        for (String main : new String[] {"private fun main() {\n}\n",
            "class A {\n    public fun main() {\n    }\n}\n"}) {
            out.clear();
            DesktopProjectImporter.scanKotlin("Tool", "package k\n\n" + main, out);
            assertTrue(main, out.isEmpty());
        }
    }

    /// A Kotlin `@JvmStatic main` is a static method of the class it is
    /// declared in, whatever the file is called.
    @Test
    public void aKotlinEntryClassIsTheDeclarationNotTheFile() {
        Map<String, DesktopProjectImporter.Candidate> out =
                new TreeMap<String, DesktopProjectImporter.Candidate>();
        DesktopProjectImporter.scanKotlin("Launcher", "package k\n\n// object Wrong {\n"
                + "private val log = java.util.logging.Logger.getLogger(App::class.java.name)\n\n"
                + "object App {\n    val title = \"object Title {\"\n"
                + "    fun describe(): String { return title }\n"
                + "    @JvmStatic fun main(a: Array<String>) {\n    }\n}\n", out);
        assertEquals(Collections.singleton("k.App"), out.keySet());

        // The static method of a companion object is on the class.
        out.clear();
        DesktopProjectImporter.scanKotlin("Start", "package k\n\nclass Tool(val name: String) : Runnable {\n"
                + "    override fun run() { println(name) }\n"
                + "    companion object {\n        @JvmStatic\n        fun main(a: Array<String>) {\n        }\n"
                + "    }\n}\n", out);
        assertEquals(Collections.singleton("k.Tool"), out.keySet());

        out.clear();
        DesktopProjectImporter.scanKotlin("Start", "class Outer {\n    object Inner {\n"
                + "        @JvmStatic fun main(a: Array<String>) {\n        }\n    }\n}\n", out);
        assertEquals(Collections.singleton("Outer$Inner"), out.keySet());

        // A JavaFX application nested in another class is Outer$Nested.
        out.clear();
        DesktopProjectImporter.scanKotlin("Start", "package k\n\nimport javafx.application.Application\n\n"
                + "class Host {\n    fun other() { }\n    class App : Application() {\n"
                + "        override fun start(s: javafx.stage.Stage) { }\n    }\n}\n"
                + "class Top : Application() {\n}\n", out);
        assertEquals(new java.util.TreeSet<String>(java.util.Arrays.asList("k.Host$App", "k.Top")),
                new java.util.TreeSet<String>(out.keySet()));

        // In no declaration this can read: the file's name, as before.
        out.clear();
        DesktopProjectImporter.scanKotlin("Odd", "package k\n@JvmStatic fun main(a: Array<String>) {\n}\n", out);
        assertTrue(out.keySet().toString(), out.containsKey("k.Odd"));
    }
}
