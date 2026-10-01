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

import com.codename1.build.BuildExecutionException;
import com.codename1.build.ProjectHost;
import com.codename1.gradle.GradleHostFactory;
import com.codename1.gradle.GradleLog;
import com.codename1.maven.BytecodeCompliance;
import com.codename1.project.BuildSystem;
import com.codename1.project.ProjectKind;
import com.codename1.project.ProjectLayouts;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Runs the bytecode compliance check over `compileJava`'s output, as the task's
/// last step before annotation processing -- the order the Maven poms bind the
/// two goals in. A `doLast` for the same reason as [ProcessAnnotationsAction]:
/// the check rewrites the compiled classes in place.
///
/// `-PskipComplianceCheck=true` skips it, which is what hot reload's recompile
/// passes, as Maven's does with `-DskipComplianceCheck`.
public final class ComplianceAction implements Action<Task> {
    private final File rootDir;
    private final File projectDir;
    /// Resolved when the action runs: a build may set the compile task's
    /// destination after the plugin configured it.
    private final Provider<File> classesDir;
    private final String projectName;
    private final FileCollection compileClasspath;
    private final Provider<List<String>> artifacts;
    private final Map<String, String> projectProperties;
    private final List<File> siblingClassRoots = new ArrayList<File>();
    private FileCollection siblingClassDirs;
    private final List<File> pendingJavaSources = new ArrayList<File>();

    /// @param compileClasspath the classes the check resolves references against
    /// @param artifacts the same classpath's resolved dependencies, encoded as
    ///        `GradleProjectHost` task inputs carry them (every one in the
    ///        `provided` scope, which keeps the framework in the scan the way
    ///        Maven's provided scope does), so the check can tell
    ///        `codenameone-core` and `java-runtime` apart. Strings, not
    ///        `ResolvedArtifactResult`s: Gradle 8.5, the minimum supported, cannot
    ///        store those in the configuration cache.
    public ComplianceAction(File rootDir, File projectDir, Provider<File> classesDir, String projectName,
                            FileCollection compileClasspath, Provider<List<String>> artifacts,
                            Map<String, String> projectProperties) {
        this.rootDir = rootDir;
        this.projectDir = projectDir;
        this.classesDir = classesDir;
        this.projectName = projectName;
        this.compileClasspath = compileClasspath;
        this.artifacts = artifacts;
        this.projectProperties = new java.util.HashMap<String, String>(projectProperties);
    }

    /// Adds directories of the project's other compiled classes -- Kotlin's,
    /// for the Java pass -- which the checked classes may use.
    public ComplianceAction withSiblingClasses(File... roots) {
        siblingClassRoots.addAll(Arrays.asList(roots));
        return this;
    }

    /// Adds every classes directory of the source set except the one checked
    /// here -- the real ones, wherever the build put them, not a guessed path.
    public ComplianceAction withSiblingClasses(FileCollection classesDirs) {
        this.siblingClassDirs = classesDirs;
        return this;
    }

    /// Declares the Java sources javac has not compiled yet, for the Kotlin pass,
    /// which runs first; see [BytecodeCompliance#pendingProjectClasses(Set)].
    public ComplianceAction withPendingJavaSources(java.util.Collection<File> javaSourceDirs) {
        this.pendingJavaSources.addAll(javaSourceDirs);
        return this;
    }

    /// The internal name of every `.java` file under `root`, by path: a public
    /// top-level class lives in the file named after it.
    static Set<String> javaTypesUnder(File root) {
        Set<String> out = new HashSet<String>();
        collectJavaTypes(root, "", out);
        return out;
    }

    private static void collectJavaTypes(File dir, String prefix, Set<String> out) {
        File[] children = dir == null ? null : dir.listFiles();
        if (children == null) {
            return;
        }
        for (File c : children) {
            if (c.isDirectory()) {
                collectJavaTypes(c, prefix + c.getName() + "/", out);
            } else if (c.getName().endsWith(".java") && !"package-info.java".equals(c.getName())) {
                out.add(prefix + c.getName().substring(0, c.getName().length() - ".java".length()));
            }
        }
    }

    @Override
    public void execute(Task task) {
        File classesDir = this.classesDir.get();
        List<String> encoded = artifacts.get();
        // The check indexes dependencies from the resolved module artifacts, which a
        // project(":shared") or files(...) dependency is not; those are on javac's
        // classpath all the same, so they are indexed too, or a reference to one
        // of their classes read as an unsupported API and failed a valid build.
        Set<File> moduleFiles = new HashSet<File>();
        for (String e : encoded) {
            com.codename1.build.BuildArtifact a = GradleHostFactory.decode(e);
            if (a != null && a.getFile() != null) {
                moduleFiles.add(a.getFile().getAbsoluteFile());
            }
        }
        List<File> siblings = new ArrayList<File>(siblingClassRoots);
        if (siblingClassDirs != null) {
            for (File dir : siblingClassDirs) {
                if (!dir.getAbsoluteFile().equals(classesDir.getAbsoluteFile())) {
                    siblings.add(dir);
                }
            }
        }
        for (File f : compileClasspath) {
            if (!moduleFiles.contains(f.getAbsoluteFile())) {
                siblings.add(f);
            }
        }
        final File buildDir = new File(projectDir, "build");
        ProjectHost host = GradleHostFactory.create(task, new GradleLog(task.getLogger()),
                ProjectLayouts.of(BuildSystem.GRADLE, ProjectKind.APP, rootDir, projectDir), projectName, "",
                projectProperties, Collections.<String, String>emptyMap(), compileClasspath,
                Collections.<String>emptyList(), encoded, null, "");
        BytecodeCompliance check = new BytecodeCompliance(new ClassesDirHost(host, classesDir, buildDir))
                .siblingClassRoots(siblings);
        if (!pendingJavaSources.isEmpty()) {
            Set<String> pending = new HashSet<String>();
            for (File root : pendingJavaSources) {
                pending.addAll(javaTypesUnder(root));
            }
            check.pendingProjectClasses(pending);
        }
        try {
            check.execute();
        } catch (BuildExecutionException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }

    /// The host with `compileJava`'s own destination as the output directory
    /// (the layout's default names the main source set's, which is the same
    /// directory unless a build script moved it).
    private static final class ClassesDirHost extends com.codename1.gradle.DelegatingProjectHost {
        private final File classesDir;
        private final File buildDir;

        ClassesDirHost(ProjectHost delegate, File classesDir, File buildDir) {
            super(delegate);
            this.classesDir = classesDir;
            this.buildDir = buildDir;
        }

        @Override
        public File outputDirectory() {
            return classesDir;
        }

        @Override
        public File buildDirectory() {
            return buildDir;
        }

        @Override
        public long sourcesModificationTime() {
            return Long.MAX_VALUE;
        }
    }
}
