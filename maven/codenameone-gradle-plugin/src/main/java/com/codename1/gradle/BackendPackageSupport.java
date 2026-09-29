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

import com.codename1.build.BuildExecutionException;
import com.codename1.build.ProjectHost;
import com.codename1.gradle.tasks.Cn1Task;
import com.codename1.maven.BackendPackager;
import com.codename1.project.ProjectLayout;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// `backendPackage`: the backend translated to C and linked into one native
/// binary, with no JVM beneath it. The same [BackendPackager] as
/// `mvn cn1:backend-package`.
final class BackendPackageSupport {
    private BackendPackageSupport() {
    }

    static void register(Project project, ProjectLayout layout, SourceSet main, CodenameOneExtension ext) {
        final Configuration runtime = AppSupport.resolvable(project, "cn1BackendToolchain",
                "The backend runtime sources and the ParparVM translator");
        runtime.setTransitive(false);
        project.getDependencies().addProvider(runtime.getName(),
                ext.getVersion().map(v -> PluginInfo.GROUP + ":codenameone-backend:" + v + ":parparvm-sources"));
        project.getDependencies().addProvider(runtime.getName(),
                ext.getVersion().map(v -> PluginInfo.GROUP + ":codenameone-parparvm:" + v + ":bundle"));
        project.getTasks().register("backendPackage", PackageTask.class, t -> {
            AppSupport.common(t, project, layout, ext, project.provider(Collections::<String, String>emptyMap));
            t.setDescription("Builds the backend as a single native binary");
            t.dependsOn(main.getClassesTaskName());
            t.getToolchain().from(runtime);
            t.getCompileClasspath().from(main.getCompileClasspath());
            t.getArtifacts().set(project.getConfigurations().getByName(main.getCompileClasspathConfigurationName())
                    .getIncoming().artifactView(v -> v.setLenient(true)).getArtifacts().getResolvedArtifacts()
                    .map(set -> AppSupport.encode(set, "compile")));
            t.getSources().from(main.getJava().getSrcDirs());
            t.getSourceEncoding().set(AppSupport.javaEncoding(project, main));
            t.getMainClass().set(project.getProviders().gradleProperty("cn1.backend.mainClass"));
            t.getTarget().set(project.getProviders().gradleProperty("cn1.backend.target"));
            t.getJdk().set(project.getProviders().gradleProperty("cn1.backend.jdk"));
            t.getCflags().set(project.getProviders().gradleProperty("cn1.backend.cflags"));
            t.getSqlite().set(project.getProviders().gradleProperty("cn1.backend.sqlite")
                    .map(Boolean::parseBoolean).orElse(Boolean.TRUE));
            t.getCheckedCasts().set(project.getProviders().gradleProperty("cn1.backend.checkedCasts")
                    .map(Boolean::parseBoolean).orElse(Boolean.TRUE));
            t.getBinary().set(new File(layout.buildDir(), project.getName()));
        });
    }

    /// See [BackendPackageSupport].
    @DisableCachingByDefault(because = "Runs a native toolchain")
    public abstract static class PackageTask extends Cn1Task {
        /// `codenameone-backend`'s sources and the ParparVM bundle.
        @Classpath
        public abstract ConfigurableFileCollection getToolchain();

        /// The backend's compile classpath, which is translated with it.
        @Classpath
        public abstract ConfigurableFileCollection getCompileClasspath();

        /// That classpath's resolved dependencies, encoded.
        @Input
        public abstract ListProperty<String> getArtifacts();

        /// The Java sources, recompiled against the backend class library.
        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        public abstract ConfigurableFileCollection getSources();

        /// The entry point, when not the generated one.
        @Input
        @Optional
        public abstract Property<String> getMainClass();

        /// A Linux cross-compile target (not supported from this task yet).
        @Input
        @Optional
        public abstract Property<String> getTarget();

        /// The JDK to compile and translate with; defaults to Gradle's.
        @Input
        @Optional
        public abstract Property<String> getJdk();

        /// The encoding compileJava reads the sources in; the packager recompiles
        /// the same sources and must read them the same way.
        @Input
        public abstract Property<String> getSourceEncoding();

        /// Extra C compiler flags.
        @Input
        @Optional
        public abstract Property<String> getCflags();

        /// Link the bundled SQLite engine.
        @Input
        public abstract Property<Boolean> getSqlite();

        /// Make a failed cast throw.
        @Input
        public abstract Property<Boolean> getCheckedCasts();

        /// The binary.
        @OutputFile
        public abstract RegularFileProperty getBinary();

        /// The source directories the packager compiles: the source set's own, as
        /// the build script configured them, not only the conventional one -- a
        /// directory added in `sourceSets` held controllers the binary then lacked.
        private List<String> sourceRoots() {
            List<String> roots = new ArrayList<String>();
            for (File dir : getSources().getFiles()) {
                roots.add(dir.getAbsolutePath());
            }
            return roots;
        }

        @TaskAction
        public void build() {
            final ProjectLayout layout = layout();
            ProjectHost host = GradleHostFactory.create(this, log(), layout, layout.projectDir().getName(), "",
                    Collections.singletonMap("project.build.sourceEncoding", getSourceEncoding().get()),
                    Collections.<String, String>emptyMap(),
                    getCompileClasspath(), sourceRoots(),
                    getArtifacts().get(), null, getCodenameOneVersion().get());
            final java.util.Set<File> toolchain = getToolchain().getFiles();
            BackendPackager packager = new GradleBackendPackager(host, layout, toolchain);
            try {
                packager.mainClass(getMainClass().getOrNull()).output(getBinary().get().getAsFile())
                        .target(getTarget().getOrNull()).jdk(getJdk().getOrNull(), null).cflags(getCflags().getOrNull())
                        .sqlite(getSqlite().get()).checkedCasts(getCheckedCasts().get()).execute();
            } catch (BuildExecutionException ex) {
                throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
            }
        }

        /// The packager, with the Gradle project's resources and the toolchain
        /// jars the task resolved in place of Maven's repository lookup.
        private static final class GradleBackendPackager extends BackendPackager {
            private final ProjectLayout layout;
            private final java.util.Set<File> toolchain;

            GradleBackendPackager(ProjectHost host, ProjectLayout layout, java.util.Set<File> toolchain) {
                super(host);
                this.layout = layout;
                this.toolchain = toolchain;
            }

            @Override
            protected File processedResourcesDirectory() {
                return layout.resourcesOutputDir();
            }

            @Override
            protected boolean declaresResources() {
                return layout.resourcesDir().isDirectory();
            }

            @Override
            protected File resolve(String groupId, String artifactId, String version, String classifier)
                    throws BuildExecutionException {
                for (File f : toolchain) {
                    if (f.getName().startsWith(artifactId + "-") && f.getName().endsWith("-" + classifier + ".jar")) {
                        return f;
                    }
                }
                throw new BuildExecutionException("Could not resolve " + groupId + ":" + artifactId + ":"
                        + version + ":" + classifier);
            }
        }
    }
}
