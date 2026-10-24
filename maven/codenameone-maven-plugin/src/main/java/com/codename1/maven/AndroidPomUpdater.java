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

/// Wires Android compatibility into an existing application's common
/// `pom.xml`, the way a new project from the archetype already is: the
/// runtime dependency, `compile-android-res`, and the remap goal ahead of
/// `bytecode-compliance` (which must see the relocated classes).
///
/// The pom is edited as text, at anchors every archetype-generated common pom
/// has ([PomWiring]); a pom without them is reported with the snippets to add
/// by hand rather than guessed at.
final class AndroidPomUpdater {

    static final String PROFILE = "        <!-- Android compatibility, added by cn1:import-android-project. -->\n"
            + "        <profile>\n"
            + "            <id>android-compat</id>\n"
            + "            <activation>\n"
            + "                <file>\n"
            + "                    <exists>${basedir}/src/main/android</exists>\n"
            + "                </file>\n"
            + "            </activation>\n"
            + "            <dependencies>\n"
            + "                <dependency>\n"
            + "                    <groupId>com.codenameone</groupId>\n"
            + "                    <artifactId>codenameone-android-compat</artifactId>\n"
            + "                    <version>${cn1.version}</version>\n"
            + "                    <scope>provided</scope>\n"
            + "                </dependency>\n"
            + "            </dependencies>\n"
            + "        </profile>\n";

    static final String EXECUTION = "                    <execution>\n"
            + "                        <id>compile-android-res</id>\n"
            + "                        <phase>generate-sources</phase>\n"
            + "                        <goals>\n"
            + "                            <goal>compile-android-res</goal>\n"
            + "                        </goals>\n"
            + "                    </execution>\n";

    /// The goal a pom without one receives; one that binds the goal under its
    /// earlier name, `remap-android`, is already wired.
    static final String REMAP_GOAL = PomWiring.REMAP_GOAL;

    /// The updated pom, or the same text when it is already wired.
    final String pom;
    /// What could not be added automatically, each with the XML to add.
    final List<String> manual;
    final boolean changed;

    AndroidPomUpdater(String pom, boolean kotlin) {
        PomWiring w = new PomWiring(pom);
        if (!w.has("<artifactId>codenameone-android-compat</artifactId>")) {
            w.addProfile("the codenameone-android-compat dependency (scope provided), e.g. as this profile:\n"
                    + PROFILE, PROFILE);
        }
        w.addExecution("compile-android-res", EXECUTION);
        w.addRemapGoal();
        if (kotlin && !w.has("src/main/android/java</sourceDir>")) {
            w.manual.add("the Android source directories in the Kotlin profile's compile execution, which must also "
                    + "run in the process-resources phase so it precedes javac:\n"
                    + "    <sourceDir>${project.basedir}/src/main/android/kotlin</sourceDir>\n"
                    + "    <sourceDir>${project.basedir}/src/main/android/java</sourceDir>\n"
                    + "    <sourceDir>${project.build.directory}/generated-sources/android</sourceDir>\n");
        }
        this.pom = w.pom;
        this.manual = w.manual;
        this.changed = w.changed;
    }
}
