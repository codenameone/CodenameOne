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
            for (Dependency d : p.getConfigurations().getByName(DECLARED).getAllDependencies()) {
                if (!(d instanceof ExternalModuleDependency) || d.getVersion() == null) {
                    continue;
                }
                Map<String, List<Cn1libPomProfiles.Coordinate>> byPlatform = profiles(p, d.getGroup(),
                        d.getName(), d.getVersion());
                for (Map.Entry<String, List<Cn1libPomProfiles.Coordinate>> e : byPlatform.entrySet()) {
                    Configuration target = p.getConfigurations().findByName(configurationName(e.getKey()));
                    if (target == null) {
                        continue;
                    }
                    for (Cn1libPomProfiles.Coordinate c : e.getValue()) {
                        target.getDependencies().add(p.getDependencies().create(c.toNotation()));
                    }
                }
            }
        });
    }

    private static Map<String, List<Cn1libPomProfiles.Coordinate>> profiles(final Project project, String group,
                                                                          String name, String version) {
        String pom = pomText(project, group, name, version);
        if (pom == null) {
            return Collections.emptyMap();
        }
        return Cn1libPomProfiles.read(pom, (g, a, v) -> pomText(project, g, a, v));
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
