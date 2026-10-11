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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Wires Flutter support into an existing application's common `pom.xml`.
///
/// A project generated from the current archetype already has the
/// `transcode-flutter` execution bound unconditionally (it is a no-op while
/// `src/main/flutter` does not exist), and the `codenameone-flutter-runtime`
/// dependency present but **commented out** -- see the archetype's own
/// `common/pom.xml`. So, unlike [AndroidPomUpdater], there are two different
/// "not wired yet" shapes to recognize: the archetype's own commented-out
/// dependency (uncomment it in place) and a pom that predates the feature
/// entirely, or that had the block deleted (insert it, or the execution,
/// fresh). Either way the pom is edited as text, at anchors every
/// archetype-generated common pom has; a pom without them is reported with
/// the snippets to add by hand rather than guessed at.
final class FlutterPomUpdater {

    static final String DEPENDENCY = "        <dependency>\n"
            + "            <groupId>com.codenameone</groupId>\n"
            + "            <artifactId>codenameone-flutter-runtime</artifactId>\n"
            + "            <version>${cn1.version}</version>\n"
            + "        </dependency>\n";

    static final String EXECUTION = "                    <execution>\n"
            + "                        <!-- Transpiles src/main/flutter/**.dart into Java\n"
            + "                             running on the Codename One Flutter runtime.\n"
            + "                             Silent no-op when the directory is absent. -->\n"
            + "                        <id>transcode-flutter</id>\n"
            + "                        <phase>generate-sources</phase>\n"
            + "                        <goals>\n"
            + "                            <goal>transcode-flutter</goal>\n"
            + "                        </goals>\n"
            + "                    </execution>\n";

    private static final String RUNTIME_ARTIFACT = "<artifactId>codenameone-flutter-runtime</artifactId>";
    private static final String TRANSCODE_GOAL = "<goal>transcode-flutter</goal>";
    private static final String PLUGIN = "<artifactId>codenameone-maven-plugin</artifactId>";

    /// Matches the archetype's own commented-out dependency block, capturing
    /// the dependency itself (group 1) without the `<!--`/`-->` wrapper
    /// around it, so uncommenting it is a plain substring replace.
    private static final Pattern COMMENTED_DEPENDENCY = Pattern.compile(
            "<!--\\s*\\n(\\s*<dependency>\\s*\\n\\s*<groupId>com\\.codenameone</groupId>\\s*\\n"
                    + "\\s*<artifactId>codenameone-flutter-runtime</artifactId>\\s*\\n"
                    + "\\s*<version>\\$\\{cn1\\.version\\}</version>\\s*\\n\\s*</dependency>\\s*\\n)\\s*-->");

    /// The updated pom, or the same text when it is already wired.
    final String pom;
    /// What could not be added automatically, each with the XML to add.
    final List<String> manual = new ArrayList<String>();
    boolean changed;

    FlutterPomUpdater(String pom) {
        String p = pom;
        if (activeOccurrence(p, RUNTIME_ARTIFACT) < 0) {
            Matcher commented = COMMENTED_DEPENDENCY.matcher(p);
            if (commented.find()) {
                // The archetype ships this dependency commented out with
                // instructions to uncomment it; do exactly that; strip just
                // the comment markers so the explanatory text above it stays.
                p = p.substring(0, commented.start()) + commented.group(1) + p.substring(commented.end());
                changed = true;
            } else {
                int deps = p.indexOf("<dependencies>");
                if (deps < 0) {
                    manual.add("the codenameone-flutter-runtime dependency:\n" + DEPENDENCY);
                } else {
                    int at = deps + "<dependencies>".length();
                    p = p.substring(0, at) + "\n" + DEPENDENCY.substring(0, DEPENDENCY.length() - 1)
                            + p.substring(at);
                    changed = true;
                }
            }
        }
        if (activeOccurrence(p, TRANSCODE_GOAL) < 0) {
            int plugin = p.indexOf(PLUGIN);
            // Only this plugin's own executions block: a later plugin's would
            // otherwise receive a goal it does not have.
            int pluginEnd = plugin < 0 ? -1 : p.indexOf("</plugin>", plugin);
            int executions = pluginEnd < 0 ? -1 : p.indexOf("<executions>", plugin);
            if (executions > pluginEnd) {
                executions = -1;
            }
            if (executions < 0) {
                manual.add("an execution of transcode-flutter in the codenameone-maven-plugin:\n" + EXECUTION);
            } else {
                int at = executions + "<executions>".length();
                p = p.substring(0, at) + "\n" + EXECUTION.substring(0, EXECUTION.length() - 1) + p.substring(at);
                changed = true;
            }
        }
        this.pom = p;
    }

    /// `needle`'s first position outside an XML comment, or -1. Unlike a
    /// plain `indexOf`, this is not fooled by the archetype's own
    /// commented-out dependency block, which contains the exact artifact id
    /// text while contributing nothing to the build.
    private static int activeOccurrence(String xml, String needle) {
        return stripComments(xml).indexOf(needle);
    }

    private static String stripComments(String xml) {
        return xml.replaceAll("(?s)<!--.*?-->", "");
    }
}
