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
package com.codename1.impl.javase;

import com.codename1.project.BuildSystem;
import com.codename1.project.NativePlatform;
import com.codename1.project.ProjectKind;
import com.codename1.project.ProjectLayout;
import com.codename1.project.ProjectLayouts;
import java.io.File;
import java.io.IOException;

/// The Codename One project the simulator is running, whatever builds it.
///
/// The simulator, CSS watcher, hot reload and component inspector used to each
/// probe the working directory for `codenameone_settings.properties`, then for
/// `common/codenameone_settings.properties`, and to assume Maven's
/// `target/classes`. That is right for an Ant or Maven project and wrong for a
/// Gradle one, whose classes land in `build/classes/java/main`. Every such
/// lookup now goes through [ProjectLayout] instead.
///
/// The search is deliberately no wider than the one it replaces. The old code
/// looked at the working directory, its `common/` child and `../common`, so a
/// layout is accepted only when the working directory is its project
/// directory, its root, or a direct child of its root (the `javase` module a
/// Maven launch can start in). An unbounded walk up would let a simulator
/// started somewhere unrelated (a unit test in this repository, a temp
/// directory) adopt whatever project happens to enclose it.
public final class SimulatorProject {
    private static File cachedFor;
    private static ProjectLayout cached;

    private SimulatorProject() {
    }

    /// The working directory, as [JavaSEPort#getCWD()] reports it.
    private static File cwd() {
        return new File(System.getProperty("user.dir")).getAbsoluteFile();
    }

    /// The project around the working directory, or null when there is none.
    /// Recomputed whenever `user.dir` changes (the Executor moves it into
    /// `common/` for a Maven project).
    public static synchronized ProjectLayout current() {
        File cwd = cwd();
        if (!cwd.equals(cachedFor)) {
            cached = locate(cwd);
            cachedFor = cwd;
        }
        return cached;
    }

    /// The layout for a simulator started in `cwd`, or null. See the class
    /// comment for why the search is bounded.
    static ProjectLayout locate(File cwd) {
        ProjectLayout layout;
        try {
            // Not detectWithDescriptor: the source roots a build resolved
            // include generated ones, and the hot-reload watcher must not
            // watch (and recompile on) its own output.
            layout = ProjectLayouts.detect(cwd);
        } catch (RuntimeException ex) {
            return null;
        }
        if (layout == null || layout.kind() == ProjectKind.BACKEND) {
            return null;
        }
        File dir = canonical(cwd);
        File root = canonical(layout.rootDir());
        File project = canonical(layout.projectDir());
        if (dir.equals(project) || dir.equals(root) || root.equals(dir.getParentFile())) {
            return layout;
        }
        return null;
    }

    /// The layout whose project directory is `projectDir`, or null. For code
    /// that already holds the directory the settings file was found in.
    public static ProjectLayout forProjectDir(File projectDir) {
        if (projectDir == null) {
            return null;
        }
        ProjectLayout current = current();
        File dir = canonical(projectDir);
        if (current != null && canonical(current.projectDir()).equals(dir)) {
            return current;
        }
        ProjectLayout layout = locate(dir);
        if (layout != null && canonical(layout.projectDir()).equals(dir)) {
            return layout;
        }
        return null;
    }

    /// The project's `codenameone_settings.properties`: in the detected
    /// project directory, else in the working directory as before (which
    /// may not exist; callers check).
    public static File settingsFile() {
        return new File(projectDir(), ProjectLayout.SETTINGS_FILE);
    }

    /// The directory holding the project's settings and sources, else the
    /// working directory.
    public static File projectDir() {
        ProjectLayout layout = current();
        return layout != null ? layout.projectDir() : cwd();
    }

    /// Whether the running project is built by Gradle.
    public static boolean isGradle() {
        ProjectLayout layout = current();
        return layout != null && layout.buildSystem() == BuildSystem.GRADLE;
    }

    /// The layout of `projectDir` when its build output is somewhere other
    /// than Maven's `target/`, else null.
    ///
    /// Ant is left on the Maven paths on purpose. Every caller hard-coded
    /// `target/classes` before, which under Ant simply never existed: the Ant
    /// launcher puts `build/classes` on the classpath itself and the simulator
    /// never looked for it. Answering `build/classes` now would add a second,
    /// earlier copy of it to the class loader, a change in behaviour this
    /// refactoring is not meant to make.
    private static ProjectLayout nonMavenOutput(File projectDir) {
        // A null directory is the working directory: Simulator passes the
        // parent of a relative "codenameone_settings.properties", which is
        // null, and the paths below then stay relative, as they always were.
        ProjectLayout layout = forProjectDir(projectDir == null ? cwd() : projectDir);
        return layout != null && layout.buildSystem() == BuildSystem.GRADLE ? layout : null;
    }

    /// Where the project in `projectDir` compiles its classes:
    /// `build/classes/java/main` under Gradle, otherwise Maven's
    /// `target/classes`, which is what every caller hard-coded before.
    public static File classesDir(File projectDir) {
        ProjectLayout layout = nonMavenOutput(projectDir);
        if (layout != null) {
            return layout.classesDir();
        }
        return new File(projectDir, "target" + File.separator + "classes");
    }

    /// Where the project in `projectDir` puts its processed resources. The
    /// same as [classesDir(File)] except under Gradle, which keeps them in
    /// `build/resources/main`.
    public static File resourcesOutputDir(File projectDir) {
        ProjectLayout layout = nonMavenOutput(projectDir);
        if (layout != null) {
            return layout.resourcesOutputDir();
        }
        return classesDir(projectDir);
    }

    /// The build output directory of the project in `projectDir`: `build`
    /// under Gradle, otherwise `target`.
    public static File buildDir(File projectDir) {
        ProjectLayout layout = nonMavenOutput(projectDir);
        if (layout != null) {
            return layout.buildDir();
        }
        return new File(projectDir, "target");
    }

    /// The `hotswap-agent.properties` of a Gradle project, kept with its
    /// JavaSE native sources in `src/javase/resources`, or null when the
    /// running project is not a Gradle one. A Maven project keeps it in the
    /// `javase` module's `src/main/resources`, which the callers still find
    /// the way they always did.
    public static File gradleHotswapProperties() {
        ProjectLayout layout = current();
        if (layout == null || layout.buildSystem() != BuildSystem.GRADLE) {
            return null;
        }
        File javaseSources = layout.nativeSourceDir(NativePlatform.JAVASE);
        return new File(new File(javaseSources.getParentFile(), "resources"), "hotswap-agent.properties");
    }

    /// The directories an Ant project drops prebuilt JavaSE jars into:
    /// `native/javase` and the cn1lib extraction dir `lib/impl/native/javase`.
    /// Resolved against the Ant project when one is found, else against the
    /// working directory as before. Maven and Gradle compile native sources
    /// into the classpath, so neither has such a directory; the relative
    /// fallback keeps whatever an old launch relied on.
    public static File[] legacyNativeJarDirs() {
        ProjectLayout layout = current();
        File base = layout != null && layout.buildSystem() == BuildSystem.ANT ? layout.projectDir() : null;
        File javase = base != null ? layout.nativeSourceDir(NativePlatform.JAVASE)
                : new File("native" + File.separator + "javase");
        File libJavase = new File(base, "lib" + File.separator + "impl" + File.separator + "native"
                + File.separator + "javase");
        return new File[]{javase, libJavase};
    }

    static File canonical(File f) {
        try {
            return f.getCanonicalFile();
        } catch (IOException ex) {
            return f.getAbsoluteFile();
        }
    }

    /// Test seam: forgets the cached layout.
    static synchronized void reset() {
        cachedFor = null;
        cached = null;
    }
}
