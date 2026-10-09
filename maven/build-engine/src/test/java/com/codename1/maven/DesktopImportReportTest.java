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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static com.codename1.maven.DependencyClassifierTest.cls;
import static com.codename1.maven.DependencyClassifierTest.uses;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// What an import tells the developer about the project's dependencies and
/// about the Java level it was written for.
public class DesktopImportReportTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File jar(String name, Object... entries) throws Exception {
        return CompatRemapperTest.jar(new File(tmp.newFolder(), name), entries);
    }

    @Test
    public void everyDependencyGetsItsClassAndWhatToDo() throws Exception {
        File sources = tmp.newFolder();
        Files.write(new File(sources, "Main.java").toPath(), ("package com.acme;\n"
                + "import org.look.Theme;\n"
                + "import org.words.Words;\n"
                + "import org.pipes.Pipe;\n"
                + "import com.fasterxml.jackson.databind.ObjectMapper;\n"
                + "class Main { }\n").getBytes("UTF-8"));
        List<DesktopImportReport.Row> rows = DesktopImportReport.rows(Arrays.asList(
                jar("fx-controls-1.jar", "javafx/scene/control/Button.class",
                        cls("javafx/scene/control/Button", uses())),
                jar("look-1.jar", "org/look/Theme.class", cls("org/look/Theme", uses("javax/swing/JPanel")),
                        "org/look/Icons.class", cls("org/look/Icons", uses("javax/swing/Icon"))),
                jar("words-1.jar", "org/words/Words.class", cls("org/words/Words", uses()),
                        "org/words/Extra.class", cls("org/words/Extra", uses())),
                jar("pipes-1.jar", "org/pipes/Pipe.class", cls("org/pipes/Pipe", uses()),
                        "native/libpipes.dylib", new byte[] {1}),
                jar("jackson-databind-1.jar", "com/fasterxml/jackson/databind/ObjectMapper.class",
                        cls("com/fasterxml/jackson/databind/ObjectMapper", uses())),
                jar("unused-1.jar", "org/unused/U.class", cls("org/unused/U", uses())),
                jar("kotlin-stdlib-1.jar", "kotlin/Unit.class", cls("kotlin/Unit", uses())),
                jar("icons-1.jar", "icons/a.png", new byte[] {1})), sources);

        assertEquals(8, rows.size());
        assertEquals("fx-controls-1.jar", rows.get(0).dependency());
        assertEquals("provided by a layer", rows.get(0).kind());
        assertEquals("Keep it with scope provided: the sources compile against it and the JavaFX layer's own "
                + "classes ship in its place.", rows.get(0).suggestion());

        assertEquals("UI library", rows.get(1).kind());
        assertEquals("Keep it with scope compile: all 2 classes are bundled and relocated with the application. "
                + "The build reports any API it uses that a device lacks.", rows.get(1).suggestion());

        assertEquals("pure Java", rows.get(2).kind());
        assertEquals("Keep it with scope compile: the classes the application uses are bundled (1 of 2 from the "
                + "imports), relocated and checked. The build reports any API it uses that a device lacks.",
                rows.get(2).suggestion());

        assertEquals("Replace it: it ships native libraries, which cannot run on a device. Put what it does behind "
                + "an interface with a device implementation.", rows.get(3).suggestion());

        // A library the alternatives table knows is told what to use instead.
        assertTrue(rows.get(4).suggestion(), rows.get(4).suggestion().contains(" If the build reports it: "
                + ComplianceAlternatives.lookup("com.fasterxml.jackson.")));

        assertEquals("The sources import nothing from it: the build leaves it out unless a class that ships uses "
                + "it.", rows.get(5).suggestion());
        assertEquals("Codename One", rows.get(6).kind());
        assertEquals("Nothing to do: the Codename One build handles it.", rows.get(6).suggestion());
        assertEquals("resources only", rows.get(7).kind());

        List<String> table = DesktopImportReport.table(rows);
        assertEquals(2 + 2 * rows.size(), table.size());
        assertEquals("Dependency              Class", table.get(0));
        assertEquals("----------------------  -----", table.get(1));
        assertEquals("look-1.jar              UI library", table.get(4));
        assertTrue(table.get(5), table.get(5).startsWith("    Keep it with scope compile: all 2 classes"));
    }

    @Test
    public void aProjectNewerThanTheBuildIsToldWhatThatCosts() {
        assertNull(DesktopImportReport.javaLevelWarning(0));
        assertNull(DesktopImportReport.javaLevelWarning(8));
        assertNull(DesktopImportReport.javaLevelWarning(17));
        String warning = DesktopImportReport.javaLevelWarning(21);
        assertTrue(warning, warning.startsWith("The project is built for Java 21; a Codename One application is "
                + "compiled as Java 17."));
        assertTrue(warning, warning.contains("-source/-target 17"));
        assertTrue(warning, warning.contains("compile error"));
        assertTrue(warning, warning.contains("compliance check"));
    }

    private int level(String name, String build) throws Exception {
        File dir = tmp.newFolder();
        Files.write(new File(dir, name).toPath(), build.getBytes("UTF-8"));
        return DesktopProjectImporter.javaLevel(dir, dir);
    }

    @Test
    public void theJavaLevelIsReadFromTheProjectsBuild() throws Exception {
        // The compiler plugin's own configuration wins over the properties.
        assertEquals(21, level("pom.xml", "<project><properties>"
                + "<maven.compiler.source>25</maven.compiler.source></properties><build><plugins><plugin>"
                + "<artifactId>maven-compiler-plugin</artifactId><configuration><source>21</source>"
                + "<target>21</target></configuration></plugin></plugins></build></project>"));
        assertEquals(25, level("pom.xml", "<project><properties>"
                + "<maven.compiler.source>25</maven.compiler.source></properties></project>"));
        // release before source, and a property resolved in the same file.
        assertEquals(17, level("pom.xml", "<project><properties><java.version>17</java.version></properties>"
                + "<build><plugins><plugin><artifactId>maven-compiler-plugin</artifactId><configuration>"
                + "<source>11</source><release>${java.version}</release></configuration></plugin></plugins>"
                + "</build></project>"));
        assertEquals(8, level("pom.xml", "<project><properties>"
                + "<maven.compiler.target>1.8</maven.compiler.target></properties></project>"));
        assertEquals(0, level("pom.xml", "<project><properties>"
                + "<maven.compiler.release>${elsewhere}</maven.compiler.release></properties></project>"));
        assertEquals(0, level("pom.xml", "<project/>"));

        assertEquals(21, level("build.gradle", "java {\n  toolchain {\n"
                + "    languageVersion = JavaLanguageVersion.of(21)\n  }\n}\n"));
        assertEquals(21, level("build.gradle.kts", "kotlin {\n  jvmToolchain(21)\n}\n"));
        assertEquals(19, level("build.gradle", "sourceCompatibility = JavaVersion.VERSION_19\n"));
        assertEquals(8, level("build.gradle", "sourceCompatibility = JavaVersion.VERSION_1_8\n"));
        assertEquals(11, level("build.gradle", "sourceCompatibility = '11'\n"));
        assertEquals(0, level("build.gradle", "dependencies { }\n"));
    }

    @Test
    public void aModuleInheritsTheLevelOfTheProjectItIsIn() throws Exception {
        File root = tmp.newFolder();
        File module = new File(root, "app");
        assertTrue(module.mkdirs());
        Files.write(new File(root, "pom.xml").toPath(), ("<project><properties>"
                + "<maven.compiler.release>21</maven.compiler.release></properties></project>").getBytes("UTF-8"));
        Files.write(new File(module, "pom.xml").toPath(), "<project/>".getBytes("UTF-8"));
        assertEquals(21, DesktopProjectImporter.javaLevel(root, module));
    }
}
