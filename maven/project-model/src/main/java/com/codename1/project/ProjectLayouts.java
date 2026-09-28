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
package com.codename1.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/// Finds the Codename One project around a directory.
///
/// These are the rules the Maven plugin, the simulator, the CSS compiler and
/// the Designer each used to carry their own copy of, merged so they cannot
/// disagree again. [detect(File)] walks up from the directory it is given
/// and, at each level, tries Gradle, then Maven, then Ant:
///
/// - **Gradle**: a `settings.gradle.kts` / `settings.gradle` /
///   `build.gradle.kts` / `build.gradle` that names the `com.codenameone`
///   plugin, or any of them beside a Codename One settings file and no
///   `pom.xml`. Gradle is tried first because a Gradle application also keeps
///   `codenameone_settings.properties` at its root, which alone would read as Ant.
/// - **Maven**: a `pom.xml` with the application (or cn1lib) in `common/`, or
///   a `pom.xml` beside the settings file itself (a single-module project, or
///   `common/` reached before its parent).
/// - **Ant**: the settings file with no `pom.xml` and no Gradle script.
///
/// A directory inside a `backend` module or subproject resolves to that
/// backend; everything else in a project resolves to the project itself.
public final class ProjectLayouts {
    private static final String[] GRADLE_SCRIPTS = {
        "settings.gradle.kts", "settings.gradle", "build.gradle.kts", "build.gradle"
    };
    private static final String GRADLE_PLUGIN_ID = "com.codenameone";

    private ProjectLayouts() {
    }

    /// A layout for a project whose shape is already known, as a build plugin
    /// knows it.
    ///
    /// @param rootDir the top of the build, see [ProjectLayout#rootDir()]
    /// @param projectDir the directory holding the sources and settings file,
    ///        see [ProjectLayout#projectDir()]
    public static ProjectLayout of(BuildSystem buildSystem, ProjectKind kind, File rootDir, File projectDir) {
        return new ProjectLayout(buildSystem, kind, rootDir, projectDir, null);
    }

    /// The project containing `start`, or null when `start` is not inside a
    /// Codename One project.
    public static ProjectLayout detect(File start) {
        if (start == null) {
            return null;
        }
        File dir = canonical(start);
        if (dir.isFile()) {
            dir = dir.getParentFile();
        }
        File origin = dir;
        while (dir != null) {
            ProjectLayout found = detectAt(dir, origin);
            if (found != null) {
                return found;
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    /// [detect(File)], reading back the [ProjectDescriptor] a build tool wrote
    /// when there is one, so that resolved source roots win over the
    /// directory-tree guess.
    public static ProjectLayout detectWithDescriptor(File start) {
        ProjectLayout layout = detect(start);
        if (layout == null) {
            return null;
        }
        File descriptorFile = layout.descriptorFile();
        if (!descriptorFile.isFile()) {
            return layout;
        }
        try {
            ProjectDescriptor d = ProjectDescriptor.read(descriptorFile);
            ProjectLayout fromDescriptor = d.toLayout();
            return fromDescriptor != null && fromDescriptor.projectDir().equals(layout.projectDir())
                    ? fromDescriptor : layout;
        } catch (IOException ex) {
            return layout;
        }
    }

    private static ProjectLayout detectAt(File dir, File origin) {
        File settings = new File(dir, ProjectLayout.SETTINGS_FILE);
        File libSettings = new File(dir, ProjectLayout.LIBRARY_SETTINGS_FILE);
        boolean hasPom = new File(dir, "pom.xml").isFile();
        boolean hasCn1Settings = settings.isFile() || libSettings.isFile();

        if (!hasPom && isGradleRoot(dir, hasCn1Settings)) {
            File parent = dir.getParentFile();
            if (!hasSettingsScript(dir) && parent != null && hasSettingsScript(parent)
                    && isGradleRoot(parent, false)) {
                // A subproject that applies the plugin in its own build script;
                // the root is the directory with the settings script.
                return gradleLayout(parent, origin);
            }
            return gradleLayout(dir, origin);
        }

        if (hasPom) {
            File common = new File(dir, "common");
            if (new File(common, ProjectLayout.SETTINGS_FILE).isFile()
                    || new File(common, ProjectLayout.LIBRARY_SETTINGS_FILE).isFile()) {
                return mavenLayout(dir, common, origin);
            }
            if (hasCn1Settings) {
                File parent = dir.getParentFile();
                if ("common".equals(dir.getName()) && parent != null && new File(parent, "pom.xml").isFile()) {
                    return mavenLayout(parent, dir, origin);
                }
                return new ProjectLayout(BuildSystem.MAVEN, settings.isFile() ? ProjectKind.APP : ProjectKind.LIB,
                        dir, dir, null);
            }
            return null;
        }

        if (hasCn1Settings) {
            return new ProjectLayout(BuildSystem.ANT, settings.isFile() ? ProjectKind.APP : ProjectKind.LIB,
                    dir, dir, null);
        }
        return null;
    }

    private static ProjectLayout mavenLayout(File root, File common, File origin) {
        File backend = new File(root, "backend");
        if (isInside(origin, backend) && new File(backend, "pom.xml").isFile()) {
            return new ProjectLayout(BuildSystem.MAVEN, ProjectKind.BACKEND, root, backend, null);
        }
        ProjectKind kind = new File(common, ProjectLayout.SETTINGS_FILE).isFile() ? ProjectKind.APP : ProjectKind.LIB;
        return new ProjectLayout(BuildSystem.MAVEN, kind, root, common, null);
    }

    private static ProjectLayout gradleLayout(File root, File origin) {
        File backend = new File(root, "backend");
        if (isInside(origin, backend) && new File(backend, ProjectLayout.BACKEND_SETTINGS_FILE).isFile()) {
            return new ProjectLayout(BuildSystem.GRADLE, ProjectKind.BACKEND, root, backend, null);
        }
        return new ProjectLayout(BuildSystem.GRADLE, gradleKind(root), root, root, null);
    }

    /// The kind of the Gradle project in `dir`, by the same files the plugin
    /// reads: an application's settings, a cn1lib's settings, or a backend's
    /// `application.properties`. Defaults to an application.
    public static ProjectKind gradleKind(File dir) {
        if (new File(dir, ProjectLayout.SETTINGS_FILE).isFile()) {
            return ProjectKind.APP;
        }
        if (new File(dir, ProjectLayout.LIBRARY_SETTINGS_FILE).isFile()) {
            return ProjectKind.LIB;
        }
        if (new File(dir, ProjectLayout.BACKEND_SETTINGS_FILE).isFile()) {
            return ProjectKind.BACKEND;
        }
        return ProjectKind.APP;
    }

    private static boolean isGradleRoot(File dir, boolean hasCn1Settings) {
        boolean anyScript = false;
        for (String name : GRADLE_SCRIPTS) {
            File script = new File(dir, name);
            if (!script.isFile()) {
                continue;
            }
            anyScript = true;
            if (mentionsPlugin(script)) {
                return true;
            }
        }
        if (!anyScript) {
            return false;
        }
        // A Gradle script with no plugin id in it can still belong to us when the
        // plugin is applied from elsewhere (buildSrc, a convention plugin). The
        // settings file beside it, or a backend-only project's
        // application.properties under a settings script, decides.
        return hasCn1Settings || new File(dir, ProjectLayout.BACKEND_SETTINGS_FILE).isFile();
    }

    private static boolean hasSettingsScript(File dir) {
        return new File(dir, "settings.gradle.kts").isFile() || new File(dir, "settings.gradle").isFile();
    }

    private static boolean mentionsPlugin(File script) {
        try {
            return readText(script).contains(GRADLE_PLUGIN_ID);
        } catch (IOException ex) {
            return false;
        }
    }

    static String readText(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            byte[] buf = new byte[(int) Math.min(f.length(), 1 << 20)];
            int off = 0;
            while (off < buf.length) {
                int n = in.read(buf, off, buf.length - off);
                if (n < 0) {
                    break;
                }
                off += n;
            }
            return new String(buf, 0, off, Charset.forName("UTF-8"));
        } finally {
            in.close();
        }
    }

    private static boolean isInside(File f, File dir) {
        File d = canonical(dir);
        for (File p = f; p != null; p = p.getParentFile()) {
            if (p.equals(d)) {
                return true;
            }
        }
        return false;
    }

    private static File canonical(File f) {
        try {
            return f.getCanonicalFile();
        } catch (IOException ex) {
            return f.getAbsoluteFile();
        }
    }
}
