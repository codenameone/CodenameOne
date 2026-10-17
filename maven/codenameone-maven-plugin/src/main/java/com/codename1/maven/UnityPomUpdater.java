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

import java.util.ArrayList;
import java.util.List;

/// Wires Unity compatibility into an existing application's common
/// `pom.xml`, the way a new project from the archetype already is: the
/// runtime dependency and the `compile-unity` goal.
///
/// The pom is edited as text, at anchors every archetype-generated common pom
/// has; a pom without them is reported with the snippets to add by hand
/// rather than guessed at.
final class UnityPomUpdater {

    /// Compile scope, unlike the Android runtime's `provided`: nothing
    /// relocates this runtime into the application's classes, so it reaches a
    /// device the way any library does, as a dependency.
    static final String PROFILE = "        <!-- Unity compatibility, added by cn1:import-unity-project. -->\n"
            + "        <profile>\n"
            + "            <id>unity-compat</id>\n"
            + "            <activation>\n"
            + "                <file>\n"
            + "                    <exists>${basedir}/src/main/unity</exists>\n"
            + "                </file>\n"
            + "            </activation>\n"
            + "            <dependencies>\n"
            + "                <dependency>\n"
            + "                    <groupId>com.codenameone</groupId>\n"
            + "                    <artifactId>codenameone-unity-compat</artifactId>\n"
            + "                    <version>${cn1.version}</version>\n"
            + "                </dependency>\n"
            + "            </dependencies>\n"
            + "        </profile>\n";

    static final String EXECUTION = "                    <execution>\n"
            + "                        <id>compile-unity</id>\n"
            + "                        <phase>generate-sources</phase>\n"
            + "                        <goals>\n"
            + "                            <goal>compile-unity</goal>\n"
            + "                        </goals>\n"
            + "                    </execution>\n";

    private static final String PLUGIN = "<artifactId>codenameone-maven-plugin</artifactId>";

    /// The updated pom, or the same text when it is already wired.
    final String pom;
    /// What could not be added automatically, each with the XML to add.
    final List<String> manual = new ArrayList<String>();
    boolean changed;

    UnityPomUpdater(String pom) {
        String p = pom;
        if (p.indexOf("<artifactId>codenameone-unity-compat</artifactId>") < 0) {
            int profiles = p.lastIndexOf("</profiles>");
            if (profiles < 0) {
                manual.add("the codenameone-unity-compat dependency, e.g. as this profile:\n" + PROFILE);
            } else {
                int lineStart = p.lastIndexOf('\n', profiles) + 1;
                p = p.substring(0, lineStart) + PROFILE + p.substring(lineStart);
                changed = true;
            }
        }
        if (p.indexOf("<goal>compile-unity</goal>") < 0) {
            int plugin = p.indexOf(PLUGIN);
            // Only this plugin's own executions block: a later plugin's would
            // otherwise receive a goal it does not have.
            int pluginEnd = plugin < 0 ? -1 : p.indexOf("</plugin>", plugin);
            int executions = pluginEnd < 0 ? -1 : p.indexOf("<executions>", plugin);
            if (executions > pluginEnd) {
                executions = -1;
            }
            if (executions < 0) {
                manual.add("an execution of compile-unity in the codenameone-maven-plugin:\n" + EXECUTION);
            } else {
                int at = executions + "<executions>".length();
                p = p.substring(0, at) + "\n" + EXECUTION.substring(0, EXECUTION.length() - 1) + p.substring(at);
                changed = true;
            }
        }
        this.pom = p;
    }
}
