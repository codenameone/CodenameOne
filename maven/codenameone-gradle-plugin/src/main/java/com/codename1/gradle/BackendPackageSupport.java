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
import com.codename1.maven.BackendWebApp;
import com.codename1.project.ProjectLayout;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
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
            // The application backendWebApp staged ships beside the binary. Not
            // an input: the binary does not change with it, and a new browser
            // build should not cost a native link.
            t.getWebAppDirectory().set(project.getLayout().getBuildDirectory()
                    .dir(BackendSupport.WEB_APP_DIRECTORY));
            t.mustRunAfter(BackendSupport.WEB_APP_TASK);
            t.getOutputs().upToDateWhen(task -> {
                PackageTask p = (PackageTask) task;
                return webAppIsBeside(p.getWebAppDirectory().get().getAsFile(), p.getBinary().get().getAsFile());
            });
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
            org.gradle.api.tasks.TaskProvider<org.gradle.api.tasks.compile.JavaCompile> testCompile =
                    project.getTasks().named(test.getCompileJavaTaskName(), org.gradle.api.tasks.compile.JavaCompile.class);
            t.getTestCompilerOptions().set(testCompile.map(c -> c.getOptions().getAllCompilerArgs()));
            t.getTestAnnotationProcessorPath().from(testCompile.map(c -> c.getOptions().getAnnotationProcessorPath()));
            t.getTestJavaSource().set(testCompile.map(c -> c.getSourceCompatibility()));
            t.getTestJavaTarget().set(testCompile.map(c -> c.getTargetCompatibility()));
            t.getTestJavaRelease().set(testCompile.flatMap(c -> c.getOptions().getRelease()).map(String::valueOf));
            t.getJdk().set(project.getProviders().gradleProperty("cn1.backend.jdk").orElse(
                    project.getTasks().named(main.getCompileJavaTaskName(), org.gradle.api.tasks.compile.JavaCompile.class)
                            .flatMap(c -> c.getJavaCompiler())
                            .zip(testCompile.flatMap(c -> c.getJavaCompiler()), (mainCompiler, testCompiler) ->
                                    mainCompiler.getMetadata().getLanguageVersion().asInt()
                                            >= testCompiler.getMetadata().getLanguageVersion().asInt()
                                            ? mainCompiler : testCompiler)
                            .map(c -> c.getMetadata().getInstallationPath().getAsFile().getAbsolutePath())));
            t.getTestSources().from(test.getJava().getSrcDirs());
            t.getTestGeneratedSources().from(testCompile.flatMap(c -> c.getOptions().getGeneratedSourceOutputDirectory()));
            t.getTestSources().from(t.getTestGeneratedSources());
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

    private static List<String> withProcessors(List<String> arguments, ConfigurableFileCollection processors) {
        List<String> options = new ArrayList<String>(arguments);
        if (!processors.isEmpty()) {
            options.add("-processorpath");
            options.add(processors.getAsPath());
        }
        return options;
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
        // Retain prior processor output only when processing is disabled. The
        // engine excludes these directories when it reruns the processors.
        t.getGeneratedSources().from(project.getTasks().named(main.getCompileJavaTaskName(),
                org.gradle.api.tasks.compile.JavaCompile.class)
                .flatMap(c -> c.getOptions().getGeneratedSourceOutputDirectory()));
        t.getSources().from(t.getGeneratedSources());
        t.getSourceEncoding().set(AppSupport.javaEncoding(project, main));
        org.gradle.api.tasks.TaskProvider<org.gradle.api.tasks.compile.JavaCompile> compile =
                project.getTasks().named(main.getCompileJavaTaskName(), org.gradle.api.tasks.compile.JavaCompile.class);
        t.getJavaSource().set(compile.map(c -> c.getSourceCompatibility()));
        t.getJavaTarget().set(compile.map(c -> c.getTargetCompatibility()));
        t.getCompilerOptions().set(compile.map(c -> c.getOptions().getAllCompilerArgs()));
        t.getAnnotationProcessorPath().from(compile.map(c -> c.getOptions().getAnnotationProcessorPath()));
        t.getJavaRelease().set(compile.flatMap(c -> c.getOptions().getRelease()).map(String::valueOf));
        t.getJdk().set(project.getProviders().gradleProperty("cn1.backend.jdk").orElse(
                compile.flatMap(c -> c.getJavaCompiler())
                        .map(c -> c.getMetadata().getInstallationPath().getAsFile().getAbsolutePath())));
        t.getProcessedResources().from(project.provider(() -> main.getOutput().getResourcesDir()));
        t.getProcessedResources().builtBy(main.getProcessResourcesTaskName());
        t.getMainClass().set(project.getProviders().gradleProperty("cn1.backend.mainClass"));
        t.getTarget().set(project.getProviders().gradleProperty("cn1.backend.target"));
        t.getCflags().set(project.getProviders().gradleProperty("cn1.backend.cflags"));
        t.getSqlite().set(project.getProviders().gradleProperty("cn1.backend.sqlite")
                .map(Boolean::parseBoolean).orElse(Boolean.TRUE));
        t.getCheckedCasts().set(project.getProviders().gradleProperty("cn1.backend.checkedCasts")
                .map(Boolean::parseBoolean).orElse(Boolean.TRUE));
        t.getDevTools().set(project.getProviders().gradleProperty("cn1.backend.devTools")
                .map(Boolean::parseBoolean).orElse(Boolean.FALSE));
    }

    /// Where the web app staged in `staged` belongs for a binary at `binary`:
    /// the `webapp` directory beside it, or null when nothing is staged.
    static File webAppBeside(File staged, File binary) {
        if (staged == null || !new File(staged, BackendWebApp.INDEX).isFile()) {
            return null;
        }
        File parent = binary.getAbsoluteFile().getParentFile();
        return parent == null ? staged : new File(parent, BackendSupport.WEB_APP_DIRECTORY);
    }

    /// Whether the binary already has the staged web app beside it, so a
    /// package whose inputs are unchanged has nothing left to do. A copy keeps
    /// each file's time, so the copy is current when it holds the same files,
    /// each the same length and time, and nothing else. Every file is compared,
    /// not only index.html: a browser build may change a script or a style
    /// and leave the page as it was, and the staged app is not a task input
    /// that Gradle would compare on its own.
    static boolean webAppIsBeside(File staged, File binary) {
        File beside = webAppBeside(staged, binary);
        if (beside == null || beside.getAbsoluteFile().equals(staged.getAbsoluteFile())) {
            return true;
        }
        return sameTree(staged, beside);
    }

    private static boolean sameTree(File from, File to) {
        File[] children = from.listFiles();
        String[] copied = to.list();
        if (children == null || copied == null || children.length != copied.length) {
            return false;
        }
        for (File child : children) {
            File other = new File(to, child.getName());
            if (child.isDirectory()) {
                if (!other.isDirectory() || !sameTree(child, other)) {
                    return false;
                }
            } else if (!other.isFile() || other.length() != child.length()
                    || other.lastModified() != child.lastModified()) {
                return false;
            }
        }
        return true;
    }

    /// Puts the web app `backendWebApp` staged beside the binary.
    ///
    /// A packaged server looks for its application in `webapp` in its working
    /// directory, the way it looks for application.properties, so a deployment
    /// is the binary with that directory next to it. The binary is written to
    /// the build directory and the app is staged in `webapp` there, so they are
    /// neighbours already; a binary written anywhere else has the app copied to
    /// it. The files are not linked into the binary: it serves them straight
    /// from the page cache, which needs them to be files.
    ///
    /// @return the directory to deploy, or null when no web app is staged
    static File webAppBesideTheBinary(com.codename1.build.Log log, File staged, File binary)
            throws BuildExecutionException {
        File beside = webAppBeside(staged, binary);
        if (beside == null) {
            return null;
        }
        new BackendWebApp(log).copy(staged, beside);
        log.info("The web app is in " + beside + "; deploy that directory beside the binary");
        return beside;
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

        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        public abstract ConfigurableFileCollection getGeneratedSources();

        /// The entry point, when not the generated one.
        @Input
        @Optional
        public abstract Property<String> getMainClass();

        /// A Linux cross-compile target (not supported from this task yet).
        @Input
        @Optional
        public abstract Property<String> getTarget();

        /// The JDK to compile and translate with; defaults to compileJava's toolchain.
        @Input
        @Optional
        public abstract Property<String> getJdk();

        @Input public abstract Property<String> getJavaSource();
        @Input public abstract Property<String> getJavaTarget();
        @Input @Optional public abstract Property<String> getJavaRelease();
        @Input public abstract ListProperty<String> getCompilerOptions();
        @Classpath public abstract ConfigurableFileCollection getAnnotationProcessorPath();

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

        /// Where `backendWebApp` stages the application's browser build, which
        /// is deployed beside the binary. Unset for a task whose binary is not
        /// a server to deploy.
        @Internal
        public abstract DirectoryProperty getWebAppDirectory();

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
                packager.compilerOptions(withProcessors(getCompilerOptions().get(), getAnnotationProcessorPath()))
                        .generatedSources(new ArrayList<File>(getGeneratedSources().getFiles()));
                packager.javaLevel(getJavaSource().get(), getJavaTarget().get(), getJavaRelease().getOrNull());
                packager.mainClass(getMainClass().getOrNull()).output(getBinary().get().getAsFile())
                        .target(getTarget().getOrNull()).jdk(getJdk().getOrNull(), null).cflags(getCflags().getOrNull())
                        .sqlite(getSqlite().get()).checkedCasts(getCheckedCasts().get())
                        .devTools(getDevTools().get()).execute();
                if (getWebAppDirectory().isPresent()) {
                    webAppBesideTheBinary(log(), getWebAppDirectory().get().getAsFile(),
                            getBinary().get().getAsFile());
                }
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
        @Input public abstract Property<String> getTestJavaSource();
        @Input public abstract Property<String> getTestJavaTarget();
        @Input @Optional public abstract Property<String> getTestJavaRelease();
        @Input public abstract ListProperty<String> getTestCompilerOptions();
        @Classpath public abstract ConfigurableFileCollection getTestAnnotationProcessorPath();

        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        public abstract ConfigurableFileCollection getTestSources();

        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        public abstract ConfigurableFileCollection getTestGeneratedSources();

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
            p.testGeneratedSources(new ArrayList<File>(getTestGeneratedSources().getFiles()));
            p.testCompilerOptions(withProcessors(getTestCompilerOptions().get(), getTestAnnotationProcessorPath()));
            p.testJavaLevel(getTestJavaSource().get(), getTestJavaTarget().get(), getTestJavaRelease().getOrNull());
            return p;
        }
    }
}
