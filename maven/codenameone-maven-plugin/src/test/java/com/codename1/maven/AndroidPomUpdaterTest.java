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

/// cn1:import-android-project in a project generated before Android
/// compatibility existed: its common pom has no runtime dependency and no
/// goals, and the imported sources would not build.
public class AndroidPomUpdaterTest {

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), "UTF-8");
    }

    @Test
    public void wiresALegacyArchetypePom() throws Exception {
        String legacy = read("src/test/resources/android-pom/legacy-common-pom.xml");
        AndroidPomUpdater u = new AndroidPomUpdater(legacy, false);
        assertTrue(u.manual.isEmpty(), u.manual.toString());
        assertTrue(u.changed);
        assertTrue(u.pom.contains("<artifactId>codenameone-android-compat</artifactId>"));
        assertTrue(u.pom.contains("<goal>compile-android-res</goal>"));
        int remap = u.pom.indexOf(AndroidPomUpdater.REMAP_GOAL);
        int compliance = u.pom.indexOf("<goal>bytecode-compliance</goal>");
        assertTrue(remap > 0 && remap < compliance, "remap-android must precede bytecode-compliance");
        assertTrue(u.pom.indexOf("<profile>") >= 0 && u.pom.lastIndexOf("</profiles>") > u.pom.indexOf("<id>android-compat</id>"));
        // Wired once: a second import changes nothing.
        AndroidPomUpdater again = new AndroidPomUpdater(u.pom, false);
        assertFalse(again.changed);
        assertTrue(again.manual.isEmpty());
    }

    @Test
    public void leavesTheCurrentArchetypeAlone() throws Exception {
        String current = read("../cn1app-archetype/src/main/resources/archetype-resources/common/pom.xml");
        AndroidPomUpdater u = new AndroidPomUpdater(current, true);
        assertFalse(u.changed);
        assertTrue(u.manual.isEmpty(), u.manual.toString());
        assertEquals(current, u.pom);
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
        AndroidPomUpdater u = new AndroidPomUpdater(pom, false);
        assertFalse(u.pom.contains("<goal>compile-android-res</goal>"), u.pom);
        boolean named = false;
        for (String m : u.manual) {
            named |= m.contains("compile-android-res");
        }
        assertTrue(named, u.manual.toString());
    }

    @Test
    public void namesWhatAPomWithoutTheAnchorsNeeds() {
        AndroidPomUpdater u = new AndroidPomUpdater("<project><build/></project>", true);
        assertFalse(u.changed);
        assertEquals(4, u.manual.size(), u.manual.toString());
    }
}
