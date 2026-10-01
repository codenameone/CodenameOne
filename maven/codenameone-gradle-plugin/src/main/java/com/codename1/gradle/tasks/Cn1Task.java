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
package com.codename1.gradle.tasks;

import com.codename1.build.Log;
import com.codename1.project.BuildSystem;
import com.codename1.project.ProjectKind;
import com.codename1.project.ProjectLayout;
import com.codename1.project.ProjectLayouts;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/// What every Codename One task knows about the project it runs in.
///
/// All of it is plain task input set at configuration time, so an action never
/// reaches for the `Project` and the task stays configuration-cache safe.
public abstract class Cn1Task extends DefaultTask {
    /// The Gradle root project directory.
    @Internal
    public abstract DirectoryProperty getRootDirectory();

    /// The project's build directory, wherever the build script put it.
    @Internal
    public abstract DirectoryProperty getBuildDirectory();

    /// This project's directory.
    @Internal
    public abstract DirectoryProperty getProjectDirectory();

    /// `APP`, `LIB` or `BACKEND`.
    @Input
    public abstract Property<String> getKind();

    /// The `codename1.*` properties given on the command line (`-P` or `-D`)
    /// and the `codenameone { buildHints }` block, as `codename1.*` keys.
    @Input
    public abstract MapProperty<String, String> getUserProperties();

    /// The Codename One version the project builds against.
    @Input
    public abstract Property<String> getCodenameOneVersion();

    /// The project's layout.
    protected ProjectLayout layout() {
        File root = getRootDirectory().get().getAsFile();
        File dir = getProjectDirectory().get().getAsFile();
        ProjectLayout layout = ProjectLayouts.of(BuildSystem.GRADLE, ProjectKind.valueOf(getKind().get()), root, dir);
        return getBuildDirectory().isPresent() ? layout.withBuildDir(getBuildDirectory().get().getAsFile()) : layout;
    }

    /// The engine's logger over this task's.
    protected Log log() {
        return new com.codename1.gradle.GradleLog(getLogger());
    }

    /// The settings file exactly as on disk, or empty.
    protected Properties rawSettings() {
        Properties p = new Properties();
        File f = layout().settingsFile();
        if (f.isFile()) {
            try (InputStream in = new FileInputStream(f)) {
                p.load(in);
            } catch (IOException ex) {
                getLogger().warn("Could not read " + f + ": " + ex.getMessage());
            }
        }
        return p;
    }

    /// The settings file with the command-line `codename1.*` overrides on top:
    /// what the build actually uses.
    protected Properties effectiveSettings() {
        Properties p = rawSettings();
        for (Map.Entry<String, String> e : getUserProperties().get().entrySet()) {
            p.setProperty(e.getKey(), e.getValue());
        }
        return p;
    }

    /// `packageName.mainName` from the effective settings, or null.
    protected String mainClass() {
        Properties p = effectiveSettings();
        String main = p.getProperty("codename1.mainName");
        String pkg = p.getProperty("codename1.packageName");
        if (main == null || main.trim().isEmpty()) {
            return null;
        }
        return pkg == null || pkg.trim().isEmpty() ? main.trim() : pkg.trim() + "." + main.trim();
    }
}
