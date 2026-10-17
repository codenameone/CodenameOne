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

/// cn1:import-unity-project in a project generated before Unity
/// compatibility existed: its common pom has no runtime dependency and no
/// goal, and the imported project would not build.
public class UnityPomUpdaterTest {

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), "UTF-8");
    }

    @Test
    public void wiresALegacyArchetypePom() throws Exception {
        String legacy = read("src/test/resources/android-pom/legacy-common-pom.xml");
        UnityPomUpdater u = new UnityPomUpdater(legacy);
        assertTrue(u.manual.isEmpty(), u.manual.toString());
        assertTrue(u.changed);
        assertTrue(u.pom.contains("<artifactId>codenameone-unity-compat</artifactId>"));
        assertTrue(u.pom.lastIndexOf("</profiles>") > u.pom.indexOf("<id>unity-compat</id>"));
        // The goal is one of the Codename One plugin's executions.
        int plugin = u.pom.indexOf("<artifactId>codenameone-maven-plugin</artifactId>");
        int goal = u.pom.indexOf("<goal>compile-unity</goal>");
        assertTrue(goal > plugin && goal < u.pom.indexOf("</plugin>", plugin));
        // The runtime ships with the application: it must not be `provided`,
        // which would compile the game and leave every device without it.
        int dep = u.pom.indexOf("<artifactId>codenameone-unity-compat</artifactId>");
        assertFalse(u.pom.substring(dep, u.pom.indexOf("</dependency>", dep)).contains("<scope>"));
        // Wired once: a second import changes nothing.
        UnityPomUpdater again = new UnityPomUpdater(u.pom);
        assertFalse(again.changed);
        assertTrue(again.manual.isEmpty());
        assertEquals(u.pom, again.pom);
    }

    @Test
    public void leavesTheCurrentArchetypeAlone() throws Exception {
        String current = read("../cn1app-archetype/src/main/resources/archetype-resources/common/pom.xml");
        UnityPomUpdater u = new UnityPomUpdater(current);
        assertFalse(u.changed);
        assertTrue(u.manual.isEmpty(), u.manual.toString());
        assertEquals(current, u.pom);
    }

    /// Android compatibility wired in first must not confuse this one, nor
    /// the other way around: both edit the same two places of the pom.
    @Test
    public void composesWithTheAndroidWiring() throws Exception {
        String legacy = read("src/test/resources/android-pom/legacy-common-pom.xml");
        String both = new UnityPomUpdater(new AndroidPomUpdater(legacy, false).pom).pom;
        String reversed = new AndroidPomUpdater(new UnityPomUpdater(legacy).pom, false).pom;
        for (String pom : new String[] {both, reversed}) {
            assertTrue(pom.contains("<goal>compile-unity</goal>"));
            assertTrue(pom.contains("<goal>compile-android-res</goal>"));
            assertTrue(pom.contains("<id>unity-compat</id>"));
            assertTrue(pom.contains("<id>android-compat</id>"));
            assertFalse(new UnityPomUpdater(pom).changed);
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
        UnityPomUpdater u = new UnityPomUpdater(pom);
        assertFalse(u.pom.contains("<goal>compile-unity</goal>"));
        // Neither anchor exists: both snippets are handed to the developer.
        assertEquals(2, u.manual.size(), u.manual.toString());
        assertTrue(u.manual.get(0).contains("<artifactId>codenameone-unity-compat</artifactId>"));
        assertTrue(u.manual.get(1).contains("<goal>compile-unity</goal>"));
    }
}
