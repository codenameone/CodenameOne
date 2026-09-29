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
package com.codename1.gradle;

import com.codename1.maven.Cn1libPomProfiles;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/// Consuming Maven-published cn1libs.
///
/// A project declares a cn1lib the way it would declare any library:
///
/// ```kotlin
/// dependencies {
///     cn1lib("com.codenameone:googlemaps-lib:1.0")
/// }
/// ```
///
/// Its common jar and `cn1css` bundle arrive through the ordinary dependency
/// graph, because `implementation` extends `cn1lib`. Its per-platform jars do
/// not: Maven selects them with profiles activated by `codename1.platform`,
/// which Gradle never activates. So each platform gets a configuration
/// (`cn1libAndroid`, `cn1libIos`, ...) filled from those same profiles, read
/// by [Cn1libPomProfiles] -- the metadata Maven uses, not a naming guess.
final class Cn1libs {
    /// The configuration a project declares cn1libs in.
    static final String DECLARED = "cn1lib";

    /// The platform ids a cn1lib's profiles are keyed by.
    static final String[] PLATFORMS = {"javase", "android", "ios", "javascript", "win", "linux"};

    private Cn1libs() {
    }

    /// The name of `platform`'s configuration, e.g. `cn1libIos`.
    static String configurationName(String platform) {
        return DECLARED + platform.substring(0, 1).toUpperCase(Locale.ROOT) + platform.substring(1);
    }

    /// Creates the `cn1lib` configuration and one resolvable configuration per
    /// platform, and fills the latter once the build script has declared its
    /// dependencies.
    static void configure(final Project project) {
        Configuration declared = project.getConfigurations().create(DECLARED, c -> {
            c.setCanBeResolved(false);
            c.setCanBeConsumed(false);
            c.setDescription("Codename One libraries (cn1libs) published to a Maven repository");
        });
        project.getConfigurations().getByName("implementation").extendsFrom(declared);
        for (String platform : PLATFORMS) {
            project.getConfigurations().create(configurationName(platform), c -> {
                c.setCanBeResolved(true);
                c.setCanBeConsumed(false);
                c.setDescription("The " + platform + " artifacts of this project's cn1libs");
            });
        }
        project.afterEvaluate(p -> {
            // Maven activates codename1.platform in EVERY pom of the graph, so a
            // cn1lib used by another cn1lib contributes its platform jars too. Walk
            // the same graph: each library's -common module, and the pom-type
            // dependencies through which one cn1lib names another.
            //
            // Exclusions travel with the walk, as they do through Gradle's own
            // graph: cn1lib("a-lib") { exclude(module = "b-lib") } leaves B off the
            // classpath, so B's platform jars must not be packaged either. The
            // configurations' own excludes apply to every declaration.
            java.util.List<String[]> shared = new ArrayList<String[]>();
            for (String name : new String[] {DECLARED, "implementation"}) {
                for (org.gradle.api.artifacts.ExcludeRule r : p.getConfigurations().getByName(name).getExcludeRules()) {
                    shared.add(new String[] {r.getGroup(), r.getModule()});
                }
            }
            java.util.Deque<Pending> queue = new java.util.ArrayDeque<Pending>();
            for (Dependency d : p.getConfigurations().getByName(DECLARED).getAllDependencies()) {
                if (!(d instanceof ExternalModuleDependency)) {
                    continue;
                }
                String version = d.getVersion();
                if ((version == null || version.isEmpty()) && PluginInfo.GROUP.equals(d.getGroup())) {
                    // Settings writes com.codenameone cn1libs without a version; the
                    // resolution strategy (ProjectSupport) gives them the framework's,
                    // and so must this walk, or their platform jars are never found.
                    version = p.getExtensions().getByType(CodenameOneExtension.class).getVersion().get();
                }
                if (version != null && !version.isEmpty()) {
                    java.util.List<String[]> excludes = new ArrayList<String[]>(shared);
                    for (org.gradle.api.artifacts.ExcludeRule r : ((ExternalModuleDependency) d).getExcludeRules()) {
                        excludes.add(new String[] {r.getGroup(), r.getModule()});
                    }
                    queue.add(new Pending(d.getGroup(), d.getName(), version, excludes));
                }
            }
            java.util.Set<String> visited = new java.util.HashSet<String>();
            java.util.Set<String> added = new java.util.HashSet<String>();
            while (!queue.isEmpty()) {
                Pending m = queue.removeFirst();
                if (!visited.add(m.group + ":" + m.name + ":" + m.version)) {
                    continue;
                }
                String pom = pomText(p, m.group, m.name, m.version);
                if (pom == null) {
                    continue;
                }
                Cn1libPomProfiles.ParentResolver parents = (g, a, v) -> pomText(p, g, a, v);
                for (Map.Entry<String, List<Cn1libPomProfiles.Coordinate>> e
                        : Cn1libPomProfiles.read(pom, parents).entrySet()) {
                    Configuration target = p.getConfigurations().findByName(configurationName(e.getKey()));
                    if (target == null) {
                        continue;
                    }
                    for (Cn1libPomProfiles.Coordinate c : e.getValue()) {
                        if (m.excludes(c.groupId, c.artifactId)) {
                            continue;
                        }
                        if (added.add(e.getKey() + "|" + c.toNotation())) {
                            target.getDependencies().add(p.getDependencies().create(c.toNotation()));
                        }
                    }
                }
                for (Cn1libPomProfiles.Coordinate c : Cn1libPomProfiles.dependencies(pom, parents)) {
                    if (c.version != null && c.groupId != null && c.classifier == null
                            && ("pom".equals(c.type) || c.artifactId.endsWith("-common")
                                    || c.artifactId.endsWith("-lib"))
                            && !m.excludes(c.groupId, c.artifactId)) {
                        queue.add(new Pending(c.groupId, c.artifactId, c.version, m.exclusions));
                    }
                }
            }
        });
    }

    /// A module still to walk, with the exclusions of the declaration it came from.
    static final class Pending {
        final String group;
        final String name;
        final String version;
        final java.util.List<String[]> exclusions;

        Pending(String group, String name, String version, java.util.List<String[]> exclusions) {
            this.group = group;
            this.name = name;
            this.version = version;
            this.exclusions = exclusions;
        }

        /// Whether an exclusion ({group, module}, either null for "any") matches.
        boolean excludes(String g, String a) {
            for (String[] x : exclusions) {
                if ((x[0] == null || x[0].equals(g)) && (x[1] == null || x[1].equals(a))) {
                    return true;
                }
            }
            return false;
        }
    }

    /// A module's pom, fetched from the project's repositories, or null.
    static String pomText(Project project, String group, String name, String version) {
        Configuration c = project.getConfigurations().detachedConfiguration(
                project.getDependencies().create(group + ":" + name + ":" + version + "@pom"));
        c.setTransitive(false);
        try {
            Set<File> files = c.resolve();
            for (File f : files) {
                return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            }
        } catch (RuntimeException | IOException ex) {
            project.getLogger().info("Could not read the pom of " + group + ":" + name + ":" + version + ": "
                    + ex.getMessage());
        }
        return null;
    }
}
