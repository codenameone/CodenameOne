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

/// Wires Python support into an existing application's common `pom.xml`:
/// the runtime dependency and the `transpile-python` goal.
///
/// The pom is edited as text, at anchors every archetype-generated common pom
/// has; a pom without them is reported with the snippets to add by hand
/// rather than guessed at.
final class PythonPomUpdater {

    /// Compile scope: the generated classes call the runtime on the device,
    /// and it reaches there the way any library does, as a dependency.
    static final String PROFILE = "        <!-- Python support, added by cn1:import-python-project. -->\n"
            + "        <profile>\n"
            + "            <id>python</id>\n"
            + "            <activation>\n"
            + "                <file>\n"
            + "                    <exists>${basedir}/src/main/python</exists>\n"
            + "                </file>\n"
            + "            </activation>\n"
            + "            <dependencies>\n"
            + "                <dependency>\n"
            + "                    <groupId>com.codenameone</groupId>\n"
            + "                    <artifactId>codenameone-python-runtime</artifactId>\n"
            + "                    <version>${cn1.version}</version>\n"
            + "                </dependency>\n"
            + "            </dependencies>\n"
            + "        </profile>\n";

    static final String EXECUTION = "                    <execution>\n"
            + "                        <id>transpile-python</id>\n"
            + "                        <phase>generate-sources</phase>\n"
            + "                        <goals>\n"
            + "                            <goal>transpile-python</goal>\n"
            + "                        </goals>\n"
            + "                    </execution>\n";

    private static final String PLUGIN = "<artifactId>codenameone-maven-plugin</artifactId>";

    /// The updated pom, or the same text when it is already wired.
    final String pom;
    /// What could not be added automatically, each with the XML to add.
    final List<String> manual = new ArrayList<String>();
    boolean changed;

    /// The pom without comments and `<dependencyManagement>` blocks: a
    /// runtime named only there is no dependency of the build.
    static String effective(String pom) {
        StringBuilder sb = new StringBuilder();
        int at = 0;
        while (at < pom.length()) {
            int comment = pom.indexOf("<!--", at);
            int managed = pom.indexOf("<dependencyManagement>", at);
            int next = comment < 0 ? managed : managed < 0 ? comment : Math.min(comment, managed);
            if (next < 0) {
                sb.append(pom, at, pom.length());
                break;
            }
            sb.append(pom, at, next);
            String close = next == comment ? "-->" : "</dependencyManagement>";
            int end = pom.indexOf(close, next);
            at = end < 0 ? pom.length() : end + close.length();
        }
        return sb.toString();
    }

    PythonPomUpdater(String pom) {
        String p = pom;
        if (effective(p).indexOf("<artifactId>codenameone-python-runtime</artifactId>") < 0) {
            int profiles = p.lastIndexOf("</profiles>");
            if (profiles < 0) {
                manual.add("the codenameone-python-runtime dependency, e.g. as this profile:\n" + PROFILE);
            } else {
                int lineStart = p.lastIndexOf('\n', profiles) + 1;
                p = p.substring(0, lineStart) + PROFILE + p.substring(lineStart);
                changed = true;
            }
        }
        if (p.indexOf("<goal>transpile-python</goal>") < 0) {
            int plugin = p.indexOf(PLUGIN);
            // Only this plugin's own executions block: a later plugin's would
            // otherwise receive a goal it does not have.
            int pluginEnd = plugin < 0 ? -1 : p.indexOf("</plugin>", plugin);
            int executions = pluginEnd < 0 ? -1 : p.indexOf("<executions>", plugin);
            if (executions > pluginEnd) {
                executions = -1;
            }
            if (executions < 0) {
                manual.add("an execution of transpile-python in the codenameone-maven-plugin:\n" + EXECUTION);
            } else {
                int at = executions + "<executions>".length();
                p = p.substring(0, at) + "\n" + EXECUTION.substring(0, EXECUTION.length() - 1) + p.substring(at);
                changed = true;
            }
        }
        this.pom = p;
    }
}
