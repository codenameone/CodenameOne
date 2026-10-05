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
            configure(t, project, layout, main, ext, runtime);
            t.setDescription("Builds the backend as a single native binary");
            t.getBinary().set(new File(layout.buildDir(), project.getName()));
        });

        // The compiled test run: the backend's tests translated with it into one
        // native binary, run, and reported beside the JVM run's. Opt in, as with
        // Maven's -Dcn1.backend.compiledTests: it costs what a package costs.
        final Configuration testRuntime = AppSupport.resolvable(project, "cn1BackendTestToolchain",
                "The backend test library's sources, for the compiled test run");
        testRuntime.setTransitive(false);
        project.getDependencies().addProvider(testRuntime.getName(),
                ext.getVersion().map(v -> PluginInfo.GROUP + ":codenameone-backend-test:" + v + ":parparvm-sources"));
        final SourceSet test = project.getExtensions().getByType(org.gradle.api.plugins.JavaPluginExtension.class)
                .getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
        project.getTasks().register("backendTest", TestTask.class, t -> {
            configure(t, project, layout, main, ext, runtime);
            t.setDescription("Runs the backend's tests as a native binary");
            t.dependsOn(test.getClassesTaskName());
            t.getToolchain().from(testRuntime);
            // testImplementation libraries a test imports, as the Maven goal passes
            // its test classpath; the packager drops the JVM-only ones (JUnit,
            // Mockito) and compiles the rest with the tests.
            t.getTestClasspath().from(test.getCompileClasspath());
            t.getTestSources().from(test.getJava().getSrcDirs());
            // What test annotation processors generated, as configure() adds the
            // main ones: the compiled run recompiles the tests from source.
            t.getTestSources().from(project.getTasks().named(test.getCompileJavaTaskName(),
                    org.gradle.api.tasks.compile.JavaCompile.class)
                    .flatMap(c -> c.getOptions().getGeneratedSourceOutputDirectory()));
            // The Kotlin test directory too, so the packager can refuse Kotlin
            // tests by name instead of compiling the run without them. Only the
            // conventional one: a Kotlin source set moved elsewhere is not looked
            // up (documented in the testing guide).
            t.getTestSources().from(new File(layout.projectDir(), "src/test/kotlin"));
            t.getTestResources().from(project.provider(() -> test.getOutput().getResourcesDir()));
            t.getTestResources().builtBy(test.getProcessResourcesTaskName());
            t.getStrict().set(project.getProviders().gradleProperty("cn1.backend.compiledTests.strict")
                    .map(Boolean::parseBoolean).orElse(Boolean.FALSE));
            t.getBinary().set(new File(layout.buildDir(), "cn1-backend-test/" + project.getName() + "-tests"));
        });
    }

    private static void configure(PackageTask t, Project project, ProjectLayout layout, SourceSet main,
                                  CodenameOneExtension ext, Configuration runtime) {
        AppSupport.common(t, project, layout, ext, project.provider(Collections::<String, String>emptyMap));
        t.dependsOn(main.getClassesTaskName());
        t.getToolchain().from(runtime);
        // The compile classpath, as Maven's backend-package resolves (scope
        // compile), not the runtime one: the binary is translated by ParparVM,
        // which loads nothing by name (Class.forName is banned in the backend),
        // so a runtimeOnly library -- a JDBC driver, say -- could not run in it.
        t.getCompileClasspath().from(main.getCompileClasspath());
        t.getArtifacts().set(project.getConfigurations().getByName(main.getCompileClasspathConfigurationName())
                .getIncoming().artifactView(v -> v.setLenient(true)).getArtifacts().getResolvedArtifacts()
                .map(set -> AppSupport.encode(set, "compile")));
        t.getSources().from(main.getJava().getSrcDirs());
        // What annotation processors (annotationProcessor(...)) generated: the
        // packager compiles the sources again, without javac's processor path,
        // so it needs their output the way runBackend has it from the classes.
        t.getSources().from(project.getTasks().named(main.getCompileJavaTaskName(),
                org.gradle.api.tasks.compile.JavaCompile.class)
                .flatMap(c -> c.getOptions().getGeneratedSourceOutputDirectory()));
        t.getSourceEncoding().set(AppSupport.javaEncoding(project, main));
        t.getProcessedResources().from(project.provider(() -> main.getOutput().getResourcesDir()));
        t.getProcessedResources().builtBy(main.getProcessResourcesTaskName());
        t.getMainClass().set(project.getProviders().gradleProperty("cn1.backend.mainClass"));
        t.getTarget().set(project.getProviders().gradleProperty("cn1.backend.target"));
        t.getJdk().set(project.getProviders().gradleProperty("cn1.backend.jdk"));
        t.getCflags().set(project.getProviders().gradleProperty("cn1.backend.cflags"));
        t.getSqlite().set(project.getProviders().gradleProperty("cn1.backend.sqlite")
                .map(Boolean::parseBoolean).orElse(Boolean.TRUE));
        t.getCheckedCasts().set(project.getProviders().gradleProperty("cn1.backend.checkedCasts")
                .map(Boolean::parseBoolean).orElse(Boolean.TRUE));
        t.getDevTools().set(project.getProviders().gradleProperty("cn1.backend.devTools")
                .map(Boolean::parseBoolean).orElse(Boolean.FALSE));
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

        /// processResources' output, wherever the build directory is.
        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        public abstract ConfigurableFileCollection getProcessedResources();

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

        /// Carry the development MCP tools in the binary (`-Pcn1.backend.devTools`);
        /// off by default, as for Maven's backend-package.
        @Input
        public abstract Property<Boolean> getDevTools();

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
            java.util.Iterator<File> resources = getProcessedResources().getFiles().iterator();
            File processed = resources.hasNext() ? resources.next() : layout.resourcesOutputDir();
            BackendPackager packager = packager(host, layout, toolchain, processed);
            try {
                packager.mainClass(getMainClass().getOrNull()).output(getBinary().get().getAsFile())
                        .target(getTarget().getOrNull()).jdk(getJdk().getOrNull(), null).cflags(getCflags().getOrNull())
                        .sqlite(getSqlite().get()).checkedCasts(getCheckedCasts().get())
                        .devTools(getDevTools().get()).execute();
            } catch (BuildExecutionException ex) {
                throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
            }
        }

        /// The packager, with the Gradle project's resources and the toolchain
        /// jars the task resolved in place of Maven's repository lookup.
        /// The packager this task runs.
        protected BackendPackager packager(ProjectHost host, ProjectLayout layout, java.util.Set<File> toolchain,
                                           File processed) {
            return new GradleBackendPackager(host, layout, toolchain, processed);
        }

        /// Finds a toolchain artifact by name, the way both packagers resolve.
        static File fromToolchain(java.util.Set<File> toolchain, String groupId, String artifactId, String version,
                                  String classifier) throws BuildExecutionException {
            for (File f : toolchain) {
                if (f.getName().startsWith(artifactId + "-") && f.getName().endsWith("-" + classifier + ".jar")) {
                    return f;
                }
            }
            throw new BuildExecutionException("Could not resolve " + groupId + ":" + artifactId + ":"
                    + version + ":" + classifier);
        }

        private static final class GradleBackendPackager extends BackendPackager {
            private final ProjectLayout layout;
            private final java.util.Set<File> toolchain;
            private final File processedResources;

            GradleBackendPackager(ProjectHost host, ProjectLayout layout, java.util.Set<File> toolchain,
                                  File processedResources) {
                super(host);
                this.layout = layout;
                this.toolchain = toolchain;
                this.processedResources = processedResources;
            }

            @Override
            protected File processedResourcesDirectory() {
                return processedResources;
            }

            @Override
            protected boolean declaresResources() {
                return layout.resourcesDir().isDirectory();
            }

            @Override
            protected File resolve(String groupId, String artifactId, String version, String classifier)
                    throws BuildExecutionException {
                return fromToolchain(toolchain, groupId, artifactId, version, classifier);
            }
        }
    }

    /// The test packager, answering from the Gradle task's inputs.
    private static final class GradleBackendTestPackager extends com.codename1.maven.BackendTestPackager {
        private final ProjectLayout layout;
        private final java.util.Set<File> toolchain;
        private final File processed;
        private final List<String> roots;
        private final File testResources;
        private final List<String> testClasspath;

        GradleBackendTestPackager(ProjectHost host, ProjectLayout layout, java.util.Set<File> toolchain,
                                  File processed, List<String> roots, File testResources,
                                  List<String> testClasspath) {
            super(host);
            this.layout = layout;
            this.toolchain = toolchain;
            this.processed = processed;
            this.roots = roots;
            this.testResources = testResources;
            this.testClasspath = testClasspath;
        }

        @Override
        protected List<String> testClasspathElements() {
            return testClasspath;
        }

        @Override
        protected List<String> testSourceRoots() {
            return roots;
        }

        @Override
        protected File testOutputDirectory() {
            return testResources;
        }

        @Override
        protected File processedResourcesDirectory() {
            return processed;
        }

        @Override
        protected boolean declaresResources() {
            return layout.resourcesDir().isDirectory();
        }

        @Override
        protected File resolve(String groupId, String artifactId, String version, String classifier)
                throws BuildExecutionException {
            return PackageTask.fromToolchain(toolchain, groupId, artifactId, version, classifier);
        }
    }

    /// `backendTest`: the backend's tests as a native binary, through the build
    /// engine's BackendTestPackager.
    @DisableCachingByDefault(because = "Runs a native toolchain and the tests")
    public abstract static class TestTask extends PackageTask {
        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        public abstract ConfigurableFileCollection getTestSources();

        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        public abstract ConfigurableFileCollection getTestResources();

        @Classpath
        public abstract ConfigurableFileCollection getTestClasspath();

        @Input
        public abstract Property<Boolean> getStrict();

        @Override
        protected BackendPackager packager(ProjectHost host, final ProjectLayout layout,
                                           final java.util.Set<File> toolchain, final File processed) {
            final List<String> roots = new ArrayList<String>();
            for (File dir : getTestSources().getFiles()) {
                roots.add(dir.getAbsolutePath());
            }
            java.util.Iterator<File> resources = getTestResources().getFiles().iterator();
            final File testResources = resources.hasNext() ? resources.next() : null;
            final List<String> testClasspath = new ArrayList<String>();
            for (File f : getTestClasspath().getFiles()) {
                testClasspath.add(f.getAbsolutePath());
            }
            com.codename1.maven.BackendTestPackager p = new GradleBackendTestPackager(host, layout, toolchain,
                    processed, roots, testResources, testClasspath);
            p.strict(getStrict().get());
            return p;
        }
    }
}
