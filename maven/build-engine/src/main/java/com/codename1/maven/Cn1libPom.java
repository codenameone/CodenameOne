/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import java.util.Collection;

/// The `-lib` pom a cn1lib is consumed through.
///
/// The shape `cn1lib-archetype` publishes: `<name>-lib`, packaging `pom`,
/// depending on `<name>-common` and its `cn1css` bundle, with one profile per
/// platform -- activated by `-Dcodename1.platform=<platform>` -- adding
/// `<name>-<platform>`. A Maven application picks the platform's jar through
/// the profile; the Gradle plugin reads the same profiles
/// ([Cn1libPomProfiles]). The Gradle plugin writes this for a cn1lib built
/// with Gradle, so either build tool can consume a library built by either.
///
/// Unlike the archetype's, the coordinates are written literally rather than as
/// `${project.groupId}` expressions against a parent, so the pom stands alone.
public final class Cn1libPom {
    private Cn1libPom() {
    }

    /// The pom.
    ///
    /// @param name the library's base name; artifacts are `<name>-common`,
    ///        `<name>-<platform>` and `<name>-lib`
    /// @param platforms the platforms the library ships a jar for
    /// @param hasCss whether it ships a `cn1css` bundle
    public static String render(String groupId, String name, String version, Collection<String> platforms,
                                boolean hasCss) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 ")
                .append("http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        sb.append("  <modelVersion>4.0.0</modelVersion>\n");
        element(sb, "  ", "groupId", groupId);
        element(sb, "  ", "artifactId", name + "-lib");
        element(sb, "  ", "version", version);
        element(sb, "  ", "packaging", "pom");
        element(sb, "  ", "name", name + "-lib");
        sb.append("  <dependencies>\n");
        dependency(sb, "    ", groupId, name + "-common", version, null, null);
        if (hasCss) {
            dependency(sb, "    ", groupId, name + "-common", version, "cn1css", "zip");
        }
        sb.append("  </dependencies>\n");
        if (!platforms.isEmpty()) {
            sb.append("  <profiles>\n");
            for (String platform : platforms) {
                sb.append("    <profile>\n");
                element(sb, "      ", "id", platform);
                sb.append("      <activation>\n        <property>\n");
                element(sb, "          ", "name", "codename1.platform");
                element(sb, "          ", "value", platform);
                sb.append("        </property>\n      </activation>\n");
                sb.append("      <dependencies>\n");
                dependency(sb, "        ", groupId, name + "-" + platform, version, null, null);
                sb.append("      </dependencies>\n");
                sb.append("    </profile>\n");
            }
            sb.append("  </profiles>\n");
        }
        sb.append("</project>\n");
        return sb.toString();
    }

    private static void dependency(StringBuilder sb, String indent, String groupId, String artifactId,
                                   String version, String classifier, String type) {
        sb.append(indent).append("<dependency>\n");
        element(sb, indent + "  ", "groupId", groupId);
        element(sb, indent + "  ", "artifactId", artifactId);
        element(sb, indent + "  ", "version", version);
        if (classifier != null) {
            element(sb, indent + "  ", "classifier", classifier);
        }
        if (type != null) {
            element(sb, indent + "  ", "type", type);
        }
        sb.append(indent).append("</dependency>\n");
    }

    private static void element(StringBuilder sb, String indent, String name, String value) {
        sb.append(indent).append('<').append(name).append('>').append(escape(value)).append("</")
                .append(name).append(">\n");
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
