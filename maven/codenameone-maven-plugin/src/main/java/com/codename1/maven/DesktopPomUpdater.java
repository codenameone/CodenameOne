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

import java.util.List;

/// Wires the desktop compatibility layers into an existing application's
/// common `pom.xml`, the way a new project from the archetype already is: the
/// two runtime dependencies, `prepare-desktop-sources`, and the remap goal
/// ahead of `bytecode-compliance` (which must see the relocated classes).
///
/// Edited as text at the anchors every archetype-generated common pom has,
/// exactly as [AndroidPomUpdater] does and through the same code
/// ([PomWiring]); a pom without them is reported with the snippets to add by
/// hand rather than guessed at.
final class DesktopPomUpdater {

    static final String PROFILE = "        <!-- Swing and JavaFX compatibility, added by cn1:import-desktop-project. -->\n"
            + "        <profile>\n"
            + "            <id>desktop-compat</id>\n"
            + "            <activation>\n"
            + "                <file>\n"
            + "                    <exists>${basedir}/src/main/desktop</exists>\n"
            + "                </file>\n"
            + "            </activation>\n"
            + "            <dependencies>\n"
            + "                <dependency>\n"
            + "                    <groupId>com.codenameone</groupId>\n"
            + "                    <artifactId>codenameone-swing-compat</artifactId>\n"
            + "                    <version>${cn1.version}</version>\n"
            + "                    <scope>provided</scope>\n"
            + "                </dependency>\n"
            + "                <dependency>\n"
            + "                    <groupId>com.codenameone</groupId>\n"
            + "                    <artifactId>codenameone-javafx-compat</artifactId>\n"
            + "                    <version>${cn1.version}</version>\n"
            + "                    <scope>provided</scope>\n"
            + "                </dependency>\n"
            + "            </dependencies>\n"
            + "        </profile>\n";

    static final String EXECUTION = "                    <execution>\n"
            + "                        <id>prepare-desktop-sources</id>\n"
            + "                        <phase>generate-sources</phase>\n"
            + "                        <goals>\n"
            + "                            <goal>prepare-desktop-sources</goal>\n"
            + "                        </goals>\n"
            + "                    </execution>\n";

    /// The updated pom, or the same text when it is already wired.
    final String pom;
    /// What could not be added automatically, each with the XML to add.
    final List<String> manual;
    final boolean changed;

    DesktopPomUpdater(String pom, boolean kotlin) {
        PomWiring w = new PomWiring(pom);
        // Either jar says the profile is there; a pom carrying only one was
        // edited by hand and is left as its author wrote it.
        if (!w.has("<artifactId>codenameone-swing-compat</artifactId>")
                && !w.has("<artifactId>codenameone-javafx-compat</artifactId>")) {
            w.addProfile("the codenameone-swing-compat and codenameone-javafx-compat dependencies (scope provided), "
                    + "e.g. as this profile:\n" + PROFILE, PROFILE);
        }
        w.addExecution("prepare-desktop-sources", EXECUTION);
        w.addRemapGoal();
        if (kotlin && !w.has("src/main/desktop/java</sourceDir>")) {
            w.manual.add("the desktop source directories in the Kotlin profile's compile execution, which must also "
                    + "run in the process-resources phase so it precedes javac:\n"
                    + "    <sourceDir>${project.basedir}/src/main/desktop/kotlin</sourceDir>\n"
                    + "    <sourceDir>${project.basedir}/src/main/desktop/java</sourceDir>\n");
        }
        this.pom = w.pom;
        this.manual = w.manual;
        this.changed = w.changed;
    }
}
