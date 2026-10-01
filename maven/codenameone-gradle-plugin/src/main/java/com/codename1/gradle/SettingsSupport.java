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

import com.codename1.project.ProjectLayout;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.net.URI;

/// The plugin applied from `settings.gradle.kts`.
final class SettingsSupport {
    /// Set on the build once the settings plugin ran, so a project does not add
    /// repositories of its own on top of the settings' ones.
    static final String APPLIED_MARKER = "codename1.settingsPluginApplied";

    private SettingsSupport() {
    }

    static void apply(final Settings settings) {
        settings.getGradle().getExtensions().getExtraProperties().set(APPLIED_MARKER, Boolean.TRUE);
        addRepositories(settings.getDependencyResolutionManagement().getRepositories(),
                settings.getProviders().gradleProperty("codename1.repository"));

        // A backend/ directory beside the application is its backend subproject.
        // Including it here keeps settings.gradle.kts down to the plugin line; the
        // addBackend task creates the directory, and nothing else has to change.
        File backend = new File(settings.getRootDir(), "backend");
        if (new File(backend, ProjectLayout.BACKEND_SETTINGS_FILE).isFile()
                && settings.findProject(":backend") == null) {
            settings.include("backend");
        }

        settings.getGradle().beforeProject(project -> {
            if (project == project.getRootProject() || ":backend".equals(project.getPath())) {
                project.getPluginManager().apply(CodenameOnePlugin.class);
            }
        });
    }

    /// The Codename One repository, then Maven Central for third-party
    /// libraries. `codename1.repository` (a URL or a directory) replaces the
    /// Codename One one, for a mirror or a locally built framework.
    static void addRepositories(RepositoryHandler repositories, Provider<String> override) {
        final String url = override.isPresent() ? override.get() : PluginInfo.REPOSITORY_URL;
        repositories.maven(repo -> {
            repo.setName("CodenameOne");
            repo.setUrl(toUri(url));
        });
        repositories.mavenCentral();
    }

    /// Adds the repositories to a project that has none, when the plugin was
    /// applied without the settings plugin.
    static void addRepositoriesIfMissing(final Project project) {
        Object marker = project.getGradle().getExtensions().getExtraProperties().has(APPLIED_MARKER)
                ? project.getGradle().getExtensions().getExtraProperties().get(APPLIED_MARKER) : null;
        org.gradle.api.initialization.resolve.DependencyResolutionManagement management = resolution(project);
        org.gradle.api.initialization.resolve.RepositoriesMode mode = management == null ? null
                : management.getRepositoriesMode().getOrNull();
        boolean settingsDeclare = management != null && !management.getRepositories().isEmpty();
        boolean strict = mode == org.gradle.api.initialization.resolve.RepositoriesMode.PREFER_SETTINGS
                || mode == org.gradle.api.initialization.resolve.RepositoriesMode.FAIL_ON_PROJECT_REPOS;
        if (!Boolean.TRUE.equals(marker) && (strict || settingsDeclare)) {
            // The settings script governs resolution -- a strict mode ignores or
            // forbids project repositories, and under the default mode any project
            // repository would make Gradle drop the settings' ones (a corporate
            // mirror with them). So none is added; if the settings name no Codename
            // One repository, say what to add rather than fail later on an
            // unresolvable framework.
            if (!namesCodenameOneRepository(management, project.getProviders().gradleProperty("codename1.repository"))) {
                project.getLogger().warn("cn1: settings.gradle(.kts) declares the repositories, and none is the "
                        + "Codename One repository, which publishes the framework. Add "
                        + "maven(\"" + PluginInfo.REPOSITORY_URL + "\") to its dependencyResolutionManagement, "
                        + "or apply com.codenameone in the settings plugins block, which does.");
            }
            return;
        }
        if (!Boolean.TRUE.equals(marker) && project.getRepositories().isEmpty()) {
            addRepositories(project.getRepositories(), project.getProviders().gradleProperty("codename1.repository"));
            return;
        }
        // A project that declares repositories of its own makes Gradle ignore the
        // settings' ones, and the framework then cannot be found. Adding ours beside
        // the project's keeps `repositories { maven(...) }` in a build script from
        // being a trap.
        project.afterEvaluate(p -> {
            if (!p.getRepositories().isEmpty() && p.getRepositories().findByName("CodenameOne") == null) {
                addRepositories(p.getRepositories(), p.getProviders().gradleProperty("codename1.repository"));
            }
        });
    }

    /// The settings script's `dependencyResolutionManagement`, or null. Settings
    /// is not public from a Project, so it is reached through the Gradle object;
    /// null when that fails, which keeps the plugin adding its repositories.
    static org.gradle.api.initialization.resolve.DependencyResolutionManagement resolution(Project project) {
        try {
            Object settings = project.getGradle().getClass().getMethod("getSettings").invoke(project.getGradle());
            return settings instanceof org.gradle.api.initialization.Settings
                    ? ((org.gradle.api.initialization.Settings) settings).getDependencyResolutionManagement() : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    /// Whether the settings' repositories include the Codename One one (or the
    /// `codename1.repository` override).
    static boolean namesCodenameOneRepository(
            org.gradle.api.initialization.resolve.DependencyResolutionManagement management,
            Provider<String> override) {
        if (management == null) {
            return false;
        }
        URI wanted = toUri(override.isPresent() ? override.get() : PluginInfo.REPOSITORY_URL);
        for (org.gradle.api.artifacts.repositories.ArtifactRepository r : management.getRepositories()) {
            if (r instanceof org.gradle.api.artifacts.repositories.MavenArtifactRepository
                    && sameRepository(((org.gradle.api.artifacts.repositories.MavenArtifactRepository) r).getUrl(),
                            wanted)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameRepository(URI a, URI b) {
        String x = a.toString();
        String y = b.toString();
        return (x.endsWith("/") ? x : x + "/").equals(y.endsWith("/") ? y : y + "/");
    }

    private static URI toUri(String url) {
        if (url.contains("://")) {
            return URI.create(url);
        }
        return new File(url).getAbsoluteFile().toURI();
    }
}
