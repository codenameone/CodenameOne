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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// cn1:import-desktop-project in a project generated before the desktop
/// layers existed: its common pom has neither runtime dependency and none of
/// the goals, and the imported sources would not build.
public class DesktopPomUpdaterTest {

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), "UTF-8");
    }

    @Test
    public void wiresALegacyArchetypePom() throws Exception {
        String legacy = read("src/test/resources/android-pom/legacy-common-pom.xml");
        DesktopPomUpdater u = new DesktopPomUpdater(legacy, false);
        assertTrue(u.manual.isEmpty(), u.manual.toString());
        assertTrue(u.changed);
        assertTrue(u.pom.contains("<artifactId>codenameone-swing-compat</artifactId>"));
        assertTrue(u.pom.contains("<artifactId>codenameone-javafx-compat</artifactId>"));
        assertTrue(u.pom.contains("<exists>${basedir}/src/main/desktop</exists>"));
        assertTrue(u.pom.contains("<goal>prepare-desktop-sources</goal>"));
        int remap = u.pom.indexOf(PomWiring.REMAP_GOAL);
        int compliance = u.pom.indexOf("<goal>bytecode-compliance</goal>");
        assertTrue(remap > 0 && remap < compliance, "remap-compat must precede bytecode-compliance");
        assertTrue(u.pom.lastIndexOf("</profiles>") > u.pom.indexOf("<id>desktop-compat</id>"));
        // Wired once: a second import changes nothing.
        DesktopPomUpdater again = new DesktopPomUpdater(u.pom, false);
        assertFalse(again.changed);
        assertTrue(again.manual.isEmpty());
        assertEquals(u.pom, again.pom);
    }

    @Test
    public void leavesTheCurrentArchetypeAlone() throws Exception {
        String current = read("../cn1app-archetype/src/main/resources/archetype-resources/common/pom.xml");
        DesktopPomUpdater u = new DesktopPomUpdater(current, true);
        assertFalse(u.changed);
        assertTrue(u.manual.isEmpty(), u.manual.toString());
        assertEquals(current, u.pom);
    }

    /// A project that already imported an Android application binds the
    /// remap goal under its earlier name. That goal relocates every layer, so
    /// it is kept, and only the desktop wiring is added.
    @Test
    public void addsTheDesktopLayersToAProjectWiredForAndroid() throws Exception {
        String legacy = read("src/test/resources/android-pom/legacy-common-pom.xml");
        String android = new AndroidPomUpdater(legacy, false).pom
                .replace(PomWiring.REMAP_GOAL, PomWiring.LEGACY_REMAP_GOAL);
        DesktopPomUpdater u = new DesktopPomUpdater(android, false);
        assertTrue(u.manual.isEmpty(), u.manual.toString());
        assertTrue(u.changed);
        assertFalse(u.pom.contains(PomWiring.REMAP_GOAL), "the goal is bound once, under the name it already has");
        assertTrue(u.pom.contains(PomWiring.LEGACY_REMAP_GOAL));
        assertTrue(u.pom.contains("<id>android-compat</id>"));
        assertTrue(u.pom.contains("<id>desktop-compat</id>"));
        assertTrue(u.pom.contains("<goal>compile-android-res</goal>"));
        assertTrue(u.pom.contains("<goal>prepare-desktop-sources</goal>"));
    }

    /// The imported sources are compiled against the libraries the desktop
    /// project declared, so the pom that receives them has to declare them:
    /// in the module's own dependencies, once, and never a version nobody
    /// stated.
    @Test
    public void declaresTheImportedProjectsLibraries() throws Exception {
        String current = read("../cn1app-archetype/src/main/resources/archetype-resources/common/pom.xml");
        File project = Files.createTempDirectory("desktop-libs").toFile();
        Files.write(new File(project, "pom.xml").toPath(), ("<project><dependencies>\n"
                + "<dependency><groupId>com.miglayout</groupId><artifactId>miglayout-swing</artifactId>"
                + "<version>5.3</version></dependency>\n"
                + "<dependency><groupId>org.swinglabs.swingx</groupId><artifactId>swingx-all</artifactId>"
                + "<version>1.6.5-1</version></dependency>\n"
                + "<dependency><groupId>org.example</groupId><artifactId>managed</artifactId></dependency>\n"
                + "</dependencies></project>\n").getBytes("UTF-8"));
        java.util.List<com.codename1.maven.DesktopProjectImporter.Library> libraries =
                com.codename1.maven.DesktopProjectImporter.librariesOf(project);
        DesktopPomUpdater u = new DesktopPomUpdater(current, false, libraries);
        assertTrue(u.changed);
        assertEquals(java.util.Arrays.asList("com.miglayout:miglayout-swing:5.3 (compile)",
                "org.swinglabs.swingx:swingx-all:1.6.5-1 (provided)"), u.addedLibraries);
        int mig = u.pom.indexOf("<artifactId>miglayout-swing</artifactId>");
        int swingx = u.pom.indexOf("<artifactId>swingx-all</artifactId>");
        assertTrue(mig > 0 && swingx > 0);
        assertTrue(u.pom.substring(mig, mig + 200).contains("<scope>compile</scope>"), u.pom.substring(mig, mig + 200));
        assertTrue(u.pom.substring(swingx, swingx + 200).contains("<scope>provided</scope>"));
        assertFalse(u.pom.contains("<artifactId>managed</artifactId>"), "no version was stated for it");
        // In the module's dependencies: not under dependencyManagement, a
        // plugin or a profile, where it would compile nothing.
        int build = u.pom.indexOf("<build>");
        assertTrue(build < 0 || mig < build, "declared before <build>");
        assertTrue(mig < u.pom.indexOf("<profiles>"));

        DesktopPomUpdater again = new DesktopPomUpdater(u.pom, false, libraries);
        assertFalse(again.changed);
        assertTrue(again.addedLibraries.isEmpty(), again.addedLibraries.toString());
        assertEquals(u.pom, again.pom);
    }

    @Test
    public void neverWiresTheGoalIntoALaterPlugin() {
        String pom = "<project><build><plugins>\n"
                + "    <plugin>\n"
                + "        <groupId>com.codenameone</groupId>\n"
                + "        <artifactId>codenameone-maven-plugin</artifactId>\n"
                + "    </plugin>\n"
                + "    <plugin>\n"
                + "        <artifactId>maven-antrun-plugin</artifactId>\n"
                + "        <executions>\n"
                + "        </executions>\n"
                + "    </plugin>\n"
                + "</plugins></build></project>\n";
        DesktopPomUpdater u = new DesktopPomUpdater(pom, false);
        assertFalse(u.pom.contains("<goal>prepare-desktop-sources</goal>"), u.pom);
        boolean named = false;
        for (String m : u.manual) {
            named |= m.contains("prepare-desktop-sources");
        }
        assertTrue(named, u.manual.toString());
    }

    @Test
    public void namesWhatAPomWithoutTheAnchorsNeeds() {
        DesktopPomUpdater u = new DesktopPomUpdater("<project><build/></project>", true);
        assertFalse(u.changed);
        assertEquals(4, u.manual.size(), u.manual.toString());
        assertTrue(u.manual.get(0).contains("codenameone-swing-compat"), u.manual.get(0));
        assertTrue(u.manual.get(0).contains("codenameone-javafx-compat"), u.manual.get(0));
        assertTrue(u.manual.get(3).contains("src/main/desktop/kotlin"), u.manual.get(3));
    }

    /// A Kotlin project wired before the desktop layers has the Android
    /// source directories in its Kotlin execution and not the desktop ones.
    @Test
    public void asksForTheKotlinSourceDirectoriesOnlyWhenThereIsKotlin() throws Exception {
        String current = read("../cn1app-archetype/src/main/resources/archetype-resources/common/pom.xml");
        String earlier = current.replace("<sourceDir>${project.basedir}/src/main/desktop/java</sourceDir>", "");
        assertTrue(new DesktopPomUpdater(earlier, false).manual.isEmpty());
        DesktopPomUpdater u = new DesktopPomUpdater(earlier, true);
        assertEquals(1, u.manual.size(), u.manual.toString());
        assertFalse(u.changed);
    }
}
