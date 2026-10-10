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

/// The text edits that wire a compatibility layer into an application's
/// common `pom.xml`, shared by [AndroidPomUpdater] and [DesktopPomUpdater].
///
/// The pom is edited as text, at anchors every archetype-generated common pom
/// has, so its comments and formatting survive; where an anchor is missing
/// nothing is guessed, and the snippet to add by hand is recorded instead.
final class PomWiring {

    /// The goal that relocates every active layer, and the name it had while
    /// Android was the only one. Either in a pom means the step is bound.
    static final String REMAP_GOAL = "<goal>remap-compat</goal>";
    static final String LEGACY_REMAP_GOAL = "<goal>remap-android</goal>";
    private static final String COMPLIANCE_GOAL = "<goal>bytecode-compliance</goal>";
    private static final String PLUGIN = "<artifactId>codenameone-maven-plugin</artifactId>";

    /// The pom as edited so far.
    String pom;
    /// What could not be added automatically, each with the XML to add.
    final List<String> manual = new ArrayList<String>();
    boolean changed;

    PomWiring(String pom) {
        this.pom = pom;
    }

    boolean has(String text) {
        return pom.indexOf(text) >= 0;
    }

    /// Adds `profile` as the last of the pom's profiles; `manualText` is what
    /// to report when the pom has no `<profiles>` to add it to.
    void addProfile(String manualText, String profile) {
        int profiles = pom.lastIndexOf("</profiles>");
        if (profiles < 0) {
            manual.add(manualText);
        } else {
            int lineStart = pom.lastIndexOf('\n', profiles) + 1;
            pom = pom.substring(0, lineStart) + profile + pom.substring(lineStart);
            changed = true;
        }
    }

    /// Adds `dependency` as the last of the module's own dependencies, unless
    /// the pom names `artifactId` anywhere already; answers whether it was
    /// added. The module's own are the `<dependencies>` that precede every
    /// section which may hold another list -- managed versions, the build's
    /// plugins, the profiles -- which is where an archetype-generated common
    /// pom has them.
    boolean addDependency(String artifactId, String dependency) {
        if (has("<artifactId>" + artifactId + "</artifactId>")) {
            return false;
        }
        int limit = pom.length();
        for (String section : new String[] {"<dependencyManagement>", "<build>", "<profiles>"}) {
            int at = pom.indexOf(section);
            if (at >= 0 && at < limit) {
                limit = at;
            }
        }
        int open = pom.indexOf("<dependencies>");
        int close = open < 0 ? -1 : pom.indexOf("</dependencies>", open);
        if (open < 0 || open > limit || close < 0 || close > limit) {
            manual.add("the dependency the imported sources are compiled against, in the module's own "
                    + "<dependencies>:\n" + dependency);
            return false;
        }
        int lineStart = pom.lastIndexOf('\n', close) + 1;
        pom = pom.substring(0, lineStart) + dependency + pom.substring(lineStart);
        changed = true;
        return true;
    }

    /// Adds `execution` to the Codename One plugin's executions unless some
    /// execution already runs `goal`.
    void addExecution(String goal, String execution) {
        if (has("<goal>" + goal + "</goal>")) {
            return;
        }
        int plugin = pom.indexOf(PLUGIN);
        // Only this plugin's own executions block: a later plugin's would
        // otherwise receive a goal it does not have.
        int pluginEnd = plugin < 0 ? -1 : pom.indexOf("</plugin>", plugin);
        int executions = pluginEnd < 0 ? -1 : pom.indexOf("<executions>", plugin);
        if (executions > pluginEnd) {
            executions = -1;
        }
        if (executions < 0) {
            manual.add("an execution of " + goal + " in the codenameone-maven-plugin:\n" + execution);
        } else {
            int at = executions + "<executions>".length();
            pom = pom.substring(0, at) + "\n" + execution.substring(0, execution.length() - 1) + pom.substring(at);
            changed = true;
        }
    }

    /// Binds the remap goal immediately before `bytecode-compliance`, unless
    /// the pom already binds it under either of its names.
    void addRemapGoal() {
        if (has(REMAP_GOAL) || has(LEGACY_REMAP_GOAL)) {
            return;
        }
        int compliance = pom.indexOf(COMPLIANCE_GOAL);
        if (compliance < 0 || pom.indexOf(COMPLIANCE_GOAL, compliance + 1) >= 0) {
            manual.add("the remap-compat goal, in the process-classes execution, before bytecode-compliance:\n"
                    + "    " + REMAP_GOAL + "\n");
        } else {
            int lineStart = pom.lastIndexOf('\n', compliance) + 1;
            String indent = pom.substring(lineStart, compliance);
            pom = pom.substring(0, compliance) + REMAP_GOAL + "\n" + indent + pom.substring(compliance);
            changed = true;
        }
    }
}
