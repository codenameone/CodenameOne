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

import com.codename1.build.AntSupport;
import com.codename1.build.BuildExecutionException;
import com.codename1.build.CodenameOneUpdater;
import com.codename1.project.ProjectLayout;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// `cn1Update`: moves the project to the newest Codename One release.
///
/// Under Gradle there is one version to change -- the plugin's, in
/// `settings.gradle.kts` (or wherever the build declares it) -- because the
/// framework version defaults to it. The build client in `~/.codenameone` is
/// refreshed at the same time, as `mvn cn1:update` does.
final class UpdateSupport {
    /// The plugin marker's metadata, which lists every published version.
    static final String METADATA_URL = PluginInfo.REPOSITORY_URL
            + "/com/codenameone/com.codenameone.gradle.plugin/maven-metadata.xml";

    /// `id("com.codenameone") version "X"`, Kotlin or Groovy DSL.
    static final Pattern PLUGIN_VERSION = Pattern.compile(
            "(id\\s*\\(?\\s*[\"']com\\.codenameone[\"']\\s*\\)?\\s*version\\s*\\(?\\s*[\"'])([^\"']+)([\"'])");

    private UpdateSupport() {
    }

    static void register(Project project, ProjectLayout layout) {
        project.getTasks().register("cn1Update", UpdateTask.class, t -> {
            t.setGroup(AppSupport.GROUP);
            t.setDescription("Updates the Codename One plugin version and the build client to the latest release");
            t.getRootDirectory().set(layout.rootDir());
            t.getProjectDirectory().set(layout.projectDir());
            t.getVersion().set(project.getProviders().gradleProperty("codename1.updateTo"));
            t.getMetadataUrl().set(project.getProviders().gradleProperty("cn1.metadataUrl").orElse(METADATA_URL));
        });
    }

    /// The version a metadata document names as the release, else its latest.
    static String releaseOf(String metadata) {
        Matcher m = Pattern.compile("<release>([^<]+)</release>").matcher(metadata);
        if (m.find()) {
            return m.group(1).trim();
        }
        m = Pattern.compile("<latest>([^<]+)</latest>").matcher(metadata);
        return m.find() ? m.group(1).trim() : null;
    }

    /// `script` with the com.codenameone plugin version set to `version`, or
    /// null when the script does not declare one.
    static String withPluginVersion(String script, String version) {
        Matcher m = PLUGIN_VERSION.matcher(script);
        if (!m.find()) {
            return null;
        }
        return m.replaceFirst(Matcher.quoteReplacement(m.group(1) + version + m.group(3)));
    }

    /// See [UpdateSupport].
    @DisableCachingByDefault(because = "Edits the build and downloads the build client")
    public abstract static class UpdateTask extends DefaultTask {
        /// The root project directory.
        @Internal
        public abstract DirectoryProperty getRootDirectory();

        /// This project's directory.
        @Internal
        public abstract DirectoryProperty getProjectDirectory();

        /// The version to move to (`-Pcodename1.updateTo`), else the latest.
        @Input
        @Optional
        public abstract Property<String> getVersion();

        /// Where the published versions are listed.
        @Input
        public abstract Property<String> getMetadataUrl();

        @TaskAction
        public void update() {
            String version = getVersion().getOrNull();
            if (version == null) {
                try (InputStream in = new URL(getMetadataUrl().get()).openStream()) {
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                    }
                    version = releaseOf(new String(out.toByteArray(), StandardCharsets.UTF_8));
                } catch (IOException ex) {
                    throw new GradleException("Could not read the published versions from "
                            + getMetadataUrl().get() + "; pass -Pcodename1.updateTo=<version>", ex);
                }
            }
            if (version == null) {
                throw new GradleException("No release is listed at " + getMetadataUrl().get());
            }
            boolean changed = false;
            File root = getRootDirectory().get().getAsFile();
            File project = getProjectDirectory().get().getAsFile();
            for (File script : new File[] {new File(root, "settings.gradle.kts"), new File(root, "settings.gradle"),
                    new File(project, "build.gradle.kts"), new File(project, "build.gradle")}) {
                if (!script.isFile()) {
                    continue;
                }
                try {
                    String text = new String(Files.readAllBytes(script.toPath()), StandardCharsets.UTF_8);
                    String updated = withPluginVersion(text, version);
                    if (updated != null && !updated.equals(text)) {
                        Files.write(script.toPath(), updated.getBytes(StandardCharsets.UTF_8));
                        getLogger().lifecycle("Set the Codename One plugin to " + version + " in " + script);
                        changed = true;
                    } else if (updated != null) {
                        changed = true;
                    }
                } catch (IOException ex) {
                    throw new GradleException("Could not update " + script, ex);
                }
            }
            if (!changed) {
                getLogger().warn("No plugin declaration with a version was found; set it to " + version
                        + " by hand: id(\"com.codenameone\") version \"" + version + "\"");
            }
            try {
                new CodenameOneUpdater(new GradleLog(getLogger()), AntSupport.newProject(project)).update(true,
                        new File(project, "build/codenameone"), project);
            } catch (BuildExecutionException ex) {
                throw new GradleException(ex.getMessage(), ex);
            }
        }
    }
}
