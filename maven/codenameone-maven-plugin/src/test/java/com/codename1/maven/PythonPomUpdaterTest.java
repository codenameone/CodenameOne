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

/// cn1:import-python-project in a project generated before Python
/// compatibility existed: its common pom has no runtime dependency and no
/// goal, and the imported project would not build.
public class PythonPomUpdaterTest {

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), "UTF-8");
    }

    @Test
    public void wiresALegacyArchetypePom() throws Exception {
        String legacy = read("src/test/resources/android-pom/legacy-common-pom.xml");
        PythonPomUpdater u = new PythonPomUpdater(legacy);
        assertTrue(u.manual.isEmpty(), u.manual.toString());
        assertTrue(u.changed);
        assertTrue(u.pom.contains("<artifactId>codenameone-python-runtime</artifactId>"));
        assertTrue(u.pom.lastIndexOf("</profiles>") > u.pom.indexOf("<id>python</id>"));
        // The goal is one of the Codename One plugin's executions.
        int plugin = u.pom.indexOf("<artifactId>codenameone-maven-plugin</artifactId>");
        int goal = u.pom.indexOf("<goal>transpile-python</goal>");
        assertTrue(goal > plugin && goal < u.pom.indexOf("</plugin>", plugin));
        // The runtime ships with the application: it must not be `provided`,
        // which would compile the game and leave every device without it.
        int dep = u.pom.indexOf("<artifactId>codenameone-python-runtime</artifactId>");
        assertFalse(u.pom.substring(dep, u.pom.indexOf("</dependency>", dep)).contains("<scope>"));
        // Wired once: a second import changes nothing.
        PythonPomUpdater again = new PythonPomUpdater(u.pom);
        assertFalse(again.changed);
        assertTrue(again.manual.isEmpty());
        assertEquals(u.pom, again.pom);
    }

    @Test
    public void leavesTheCurrentArchetypeAlone() throws Exception {
        String current = read("../cn1app-archetype/src/main/resources/archetype-resources/common/pom.xml");
        PythonPomUpdater u = new PythonPomUpdater(current);
        assertFalse(u.changed);
        assertTrue(u.manual.isEmpty(), u.manual.toString());
        assertEquals(current, u.pom);
    }

    /// A runtime named only in a comment or in dependency management is not
    /// a dependency of the build, so the profile still goes in.
    @Test
    public void aManagedOrCommentedRuntimeIsNotADependency() throws Exception {
        String legacy = read("src/test/resources/android-pom/legacy-common-pom.xml");
        String mention = "<!-- <artifactId>codenameone-python-runtime</artifactId> -->\n"
                + "<dependencyManagement><dependencies><dependency><groupId>com.codenameone</groupId>"
                + "<artifactId>codenameone-python-runtime</artifactId></dependency></dependencies>"
                + "</dependencyManagement>\n";
        int at = legacy.indexOf("<dependencies>");
        PythonPomUpdater u = new PythonPomUpdater(legacy.substring(0, at) + mention + legacy.substring(at));
        assertTrue(u.pom.contains("<id>python</id>"));
    }

    /// Android compatibility wired in first must not confuse this one, nor
    /// the other way around: both edit the same two places of the pom.
    @Test
    public void composesWithTheAndroidWiring() throws Exception {
        String legacy = read("src/test/resources/android-pom/legacy-common-pom.xml");
        String both = new PythonPomUpdater(new AndroidPomUpdater(legacy, false).pom).pom;
        String reversed = new AndroidPomUpdater(new PythonPomUpdater(legacy).pom, false).pom;
        for (String pom : new String[] {both, reversed}) {
            assertTrue(pom.contains("<goal>transpile-python</goal>"));
            assertTrue(pom.contains("<goal>compile-android-res</goal>"));
            assertTrue(pom.contains("<id>python</id>"));
            assertTrue(pom.contains("<id>android-compat</id>"));
            assertFalse(new PythonPomUpdater(pom).changed);
            assertFalse(new AndroidPomUpdater(pom, false).changed);
        }
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
        PythonPomUpdater u = new PythonPomUpdater(pom);
        assertFalse(u.pom.contains("<goal>transpile-python</goal>"));
        // Neither anchor exists: both snippets are handed to the developer.
        assertEquals(2, u.manual.size(), u.manual.toString());
        assertTrue(u.manual.get(0).contains("<artifactId>codenameone-python-runtime</artifactId>"));
        assertTrue(u.manual.get(1).contains("<goal>transpile-python</goal>"));
    }
}
