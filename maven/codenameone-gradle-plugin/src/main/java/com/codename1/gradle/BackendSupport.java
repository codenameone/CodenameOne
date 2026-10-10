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

import com.codename1.gradle.tasks.BackendWebAppTask;
import com.codename1.gradle.tasks.ProcessAnnotationsAction;
import com.codename1.gradle.tasks.ProcessTestAnnotationsAction;
import com.codename1.gradle.tasks.RunBackendTask;
import com.codename1.maven.BackendMigrateEntryPoint;
import com.codename1.maven.GradleProjectTemplate;
import com.codename1.project.ProjectLayout;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/// Backends: the `backend/` subproject of an application, or a backend-only
/// project at the root.
final class BackendSupport {
    /// The SQLite driver the dev profile's in-memory database uses, as the Maven
    /// backend module declares it.
    static final String SQLITE_JDBC = "org.xerial:sqlite-jdbc:3.46.1.0";
    /// The JUnit a backend's tests are written against; the compiled test run
    /// translates them against codenameone-backend-test's subset of the same API.
    static final String JUNIT_JUPITER = "org.junit.jupiter:junit-jupiter:5.9.3";
    static final String JUNIT_LAUNCHER = "org.junit.platform:junit-platform-launcher:1.9.3";
    /// The task that stages the application's browser build for this backend.
    static final String WEB_APP_TASK = "backendWebApp";
    /// The directory it stages into, under the backend's build directory.
    static final String WEB_APP_DIRECTORY = "webapp";

    private BackendSupport() {
    }

    static void apply(final Project project, final ProjectLayout layout, final CodenameOneExtension ext,
                      final Provider<Map<String, String>> userProperties) {
        AppSupport.addFramework(project, "implementation", ext.getVersion(), "codenameone-backend");
        project.getDependencies().add("runtimeOnly", SQLITE_JDBC);

        final SourceSet main = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets()
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        // The @RestController router and the entry point are generated here, from
        // the compiled classes, exactly as the Maven module's process-annotations
        // execution does.
        project.getTasks().named(main.getCompileJavaTaskName(), JavaCompile.class, compile -> {
            AppSupport.processingInputs(compile, layout, userProperties);
            backendSettingsInputs(project, compile, layout);
            compile.doLast("processCn1Annotations", new ProcessAnnotationsAction(
                    compile.getDestinationDirectory().getAsFile(), AppSupport.stubsDir(layout), layout.projectDir(),
                    layout.settingsFile(), project.provider(() -> AppSupport.sourceRoots(main, layout)),
                    "UTF-8", userProperties, compile.getClasspath())
                    .withSourceEncoding(AppSupport.javaEncoding(project, main)));
            compile.doLast("cn1SplitOutputCheck", new com.codename1.gradle.tasks.SplitOutputCheck(
                    compile.getDestinationDirectory().getAsFile(), main.getOutput().getClassesDirs()));
        });
        // Kotlin controllers and entities are processed like Java ones: in a pure
        // Kotlin backend compileJava has no sources and would generate no router or
        // entry point at all. (backendPackage compiles Java sources itself, so a
        // Kotlin backend runs on the JVM with runBackend; see BackendPackager.)
        //
        // A mixed backend whose annotated classes are in BOTH languages is not
        // served half-wired: each pass writes the entry point, its wiring record
        // (META-INF/cn1-backend-main, META-INF/cn1-backend-wiring) and same-named
        // generated classes into its own directory, and the SplitOutputCheck that
        // compileJava runs above refuses any path the two directories share. One
        // index over both outputs would need every processor that rewrites classes
        // in place -- bean weaving, the ORM enhancer -- to write across directories;
        // Maven compiles both languages into one directory and never meets this.
        project.getPluginManager().withPlugin("org.jetbrains.kotlin.jvm", kotlin ->
                project.getTasks().named("compileKotlin").configure(compile -> {
                    AppSupport.processingInputs(compile, layout, userProperties);
                    backendSettingsInputs(project, compile, layout);
                    compile.doLast("processCn1Annotations", new ProcessAnnotationsAction(
                            AppSupport.kotlinDestinationProvider(compile, layout), AppSupport.stubsDir(layout),
                            layout.projectDir(), layout.settingsFile(),
                            project.provider(() -> AppSupport.sourceRoots(main, layout)),
                            "UTF-8", userProperties, main.getCompileClasspath())
                            .withPendingJavaSources(main.getJava().getSrcDirs())
                            .withSourceEncoding(AppSupport.javaEncoding(project, main)));
                }));

        applyTests(project, layout, ext, main);

        project.getTasks().register("runBackend", RunBackendTask.class, t -> {
            t.setGroup(AppSupport.GROUP);
            t.setDescription("Runs the backend on this JVM (the fast development loop)");
            t.dependsOn(main.getClassesTaskName());
            t.getClasspath().from(main.getRuntimeClasspath());
            t.getClassesDirectories().from(main.getOutput().getClassesDirs());
            t.getMainClass().set(project.getProviders().gradleProperty("cn1.backend.mainClass"));
            t.getArgs().set(project.getProviders().gradleProperty("cn1.backend.args").map(BackendSupport::split)
                    .orElse(Collections.<String>emptyList()));
            t.getJvmArgs().set(project.getProviders().gradleProperty("cn1.backend.jvmArgs")
                    .map(BackendSupport::split).orElse(Collections.<String>emptyList()));
            t.getWorkingDirectory().set(layout.projectDir());
            // What backendWebApp staged, when it has; never a dependency, so a
            // restart does not pay for a browser build.
            t.getWebAppDirectory().set(project.getLayout().getBuildDirectory().dir(WEB_APP_DIRECTORY));
            t.mustRunAfter(WEB_APP_TASK);
        });
        registerWebApp(project, layout);
        registerMigrate(project, layout, main, "backendMigrate", "migrate",
                "Applies pending schema migrations to the configured database");
        registerMigrate(project, layout, main, "backendMigrateInfo", "info",
                "Lists every schema migration and its state in the configured database");
        registerMigrate(project, layout, main, "backendMigrateValidate", "validate",
                "Fails unless the configured database is exactly at this build's migrations");
        registerMigrate(project, layout, main, "backendMigrateRepair", "repair",
                "Removes failed rows from the schema history and realigns checksums");
        registerMigrate(project, layout, main, "backendMigrateBaseline", "baseline",
                "Marks an existing database as already being at cn1.flyway.baselineVersion");
        BackendPackageSupport.register(project, layout, main, ext);
        UpdateSupport.register(project, layout);
    }

    /// `backendWebApp`: the application's browser build, staged where
    /// `runBackend` serves it and `backendPackage` ships it.
    ///
    /// In an application's `backend/` subproject the bundle is the root
    /// project's, so the task depends on the root's [AppSupport#WEB_APP_BUILD_TASK]
    /// -- the local JavaScript build, made without the proxy servlet a backend
    /// does not have. Not when a bundle is given: staging one that is already
    /// built needs no build. A backend that is the root project has no
    /// application to build and takes a bundle only.
    private static void registerWebApp(Project project, ProjectLayout layout) {
        final Provider<String> bundle = project.getProviders().gradleProperty("cn1.backend.webapp.bundle");
        final Project root = project.getRootProject();
        final boolean hasApp = hostsApplication(project != root,
                project != root ? ProjectSupport.kind(root) : null);
        // Looked up here: a lambda that held the Project could not be stored in
        // the configuration cache.
        final org.gradle.api.file.Directory projectDirectory = project.getLayout().getProjectDirectory();
        project.getTasks().register(WEB_APP_TASK, BackendWebAppTask.class, t -> {
            t.setGroup(AppSupport.GROUP);
            t.setDescription("Builds the app for the browser and stages it for this backend to serve");
            t.getBundle().set(bundle);
            t.getWorkingDirectory().set(layout.projectDir());
            t.getOutputDirectory().set(project.getProviders().gradleProperty("cn1.backend.webapp.output")
                    .map(projectDirectory::dir)
                    .orElse(project.getLayout().getBuildDirectory().dir(WEB_APP_DIRECTORY)));
            // Staging replaces the directory with whatever the bundle holds now.
            t.getOutputs().upToDateWhen(x -> false);
            if (hasApp) {
                t.getAppBuildDirectory().set(root.getLayout().getBuildDirectory());
                if (!bundle.isPresent()) {
                    t.dependsOn(root.getPath() + AppSupport.WEB_APP_BUILD_TASK);
                }
            }
        });
    }

    /// Whether a backend has an application whose browser build it can stage:
    /// it is a subproject, and the root project is an application.
    static boolean hostsApplication(boolean subproject, com.codename1.project.ProjectKind rootKind) {
        return subproject && rootKind == com.codename1.project.ProjectKind.APP;
    }

    /// One migration command, as the Maven `cn1:migrate` goals run it: the entry point the
    /// build generates beside `cn1app.BackendMigrations`, on this JVM, with every `cn1.*`
    /// project property passed through and the processed `application.properties` as the
    /// configuration.
    ///
    /// The class is [BackendMigrateEntryPoint#CLASS_NAME], which the Maven goals read too.
    /// These tasks used to name `cn1app.BackendMigrations` themselves -- the class that
    /// holds the scripts and has no `main` -- and every one of them failed at start-up.
    private static void registerMigrate(Project project, ProjectLayout layout, SourceSet main, String name,
            String command, String description) {
        project.getTasks().register(name, RunBackendTask.class, t -> {
            t.setGroup(AppSupport.GROUP);
            t.setDescription(description);
            t.dependsOn(main.getClassesTaskName());
            t.getClasspath().from(main.getRuntimeClasspath());
            t.getClassesDirectories().from(main.getOutput().getClassesDirs());
            t.getMainClass().set(BackendMigrateEntryPoint.CLASS_NAME);
            t.getMissingMainClassMessage().set(BackendMigrateEntryPoint.missingMessage());
            t.getArgs().set(Collections.singletonList(command));
            // Read now, not inside the provider: a lambda that held the source set would
            // drag the whole project model into the configuration cache.
            final File resources = main.getOutput().getResourcesDir();
            t.getJvmArgs().set(project.getProviders().gradlePropertiesPrefixedBy("cn1.").map(given -> {
                List<String> options = new ArrayList<String>();
                for (Map.Entry<String, String> property : given.entrySet()) {
                    options.add("-D" + property.getKey() + "=" + property.getValue());
                }
                if (!given.containsKey("cn1.config.location") && resources != null) {
                    options.add("-Dcn1.config.location=" + resources.getAbsolutePath());
                }
                return options;
            }));
            t.getWorkingDirectory().set(layout.projectDir());
        });
    }

    /// `@BackendTest` support, as the Maven archetype's backend module has it: the
    /// test library and JUnit 5, a fresh JVM per test class (one server runs per
    /// process), and the test pass after the test classes compile.
    private static void applyTests(final Project project, final ProjectLayout layout,
                                   final CodenameOneExtension ext, final SourceSet main) {
        AppSupport.addFramework(project, "testImplementation", ext.getVersion(), "codenameone-backend-test");
        project.getDependencies().add("testImplementation", JUNIT_JUPITER);
        project.getDependencies().add("testRuntimeOnly", JUNIT_LAUNCHER);
        final SourceSet test = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets()
                .getByName(SourceSet.TEST_SOURCE_SET_NAME);
        final Provider<List<String>> roots = project.provider(() -> {
            List<String> all = new ArrayList<String>(AppSupport.sourceRoots(main, layout));
            for (File dir : test.getJava().getSrcDirs()) {
                all.add(dir.getAbsolutePath());
            }
            all.add(new File(layout.projectDir(), "src/test/kotlin").getAbsolutePath());
            return all;
        });
        final File stubs = new File(layout.buildDir(), "generated/sources/cn1-test-stubs");
        // A mixed Java and Kotlin test set is one hierarchy -- a Kotlin test can
        // extend a Java @BackendTest base -- so the pass reads both outputs at once.
        // Kotlin compiles first and Java after it, against the Kotlin classes: the
        // pass runs after the Java compile, over both, and after the Kotlin one only
        // when there is no Java test source (compileTestJava is then NO-SOURCE and
        // runs no action).
        final org.gradle.api.file.ConfigurableFileCollection kotlinTestClasses = project.files();
        project.getTasks().named(test.getCompileJavaTaskName(), JavaCompile.class, compile -> {
            // The generated contexts embed application.properties and the profile's
            // file, so a change to either alone must recompile and regenerate them.
            backendSettingsInputs(project, compile, layout);
            compile.doLast("processCn1TestAnnotations", new ProcessTestAnnotationsAction(
                    main.getOutput().getClassesDirs(), compile.getDestinationDirectory().getAsFile(),
                    stubs, layout.projectDir(), roots, AppSupport.javaEncoding(project, main),
                    compile.getClasspath(), kotlinTestClasses, null));
        });
        project.getPluginManager().withPlugin("org.jetbrains.kotlin.jvm", kotlin ->
                project.getTasks().named("compileTestKotlin").configure(compile -> {
                    backendSettingsInputs(project, compile, layout);
                    Provider<File> kotlinOut = AppSupport.kotlinDestinationProvider(compile, layout);
                    kotlinTestClasses.from(kotlinOut);
                    compile.doLast("processCn1TestAnnotations", new ProcessTestAnnotationsAction(
                            main.getOutput().getClassesDirs(), kotlinOut, stubs,
                            layout.projectDir(), roots, AppSupport.javaEncoding(project, main),
                            test.getCompileClasspath(), null, test.getJava()));
                }));
        project.getTasks().withType(org.gradle.api.tasks.testing.Test.class).configureEach(t -> {
            t.useJUnitPlatform();
            t.setForkEvery(1L);
        });
    }

    /// The backend's application.properties and profile files as inputs of
    /// `compile`: the build compiles them into the server, and into each test's
    /// context, so a settings-only edit must not leave the task up to date.
    private static void backendSettingsInputs(Project project, org.gradle.api.Task compile, ProjectLayout layout) {
        // Both places the processor reads them from: the project directory, and the
        // conventional src/main/resources, where most projects keep them.
        compile.getInputs().files(project.fileTree(layout.projectDir(), tree -> {
            tree.include("application.properties", "application-*.properties",
                    "src/main/resources/application.properties",
                    "src/main/resources/application-*.properties");
        })).withPropertyName("cn1BackendSettings")
                .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE);
    }

    static List<String> split(String s) {
        String t = s.trim();
        return t.isEmpty() ? Collections.<String>emptyList() : new ArrayList<String>(Arrays.asList(t.split("\\s+")));
    }

    /// `addBackend` on an application: creates the `backend/` subproject.
    static void registerAddBackend(Project project, ProjectLayout layout) {
        project.getTasks().register("addBackend", AddBackendTask.class, t -> {
            t.setGroup(AppSupport.GROUP);
            t.setDescription("Adds a backend/ subproject to this application");
            t.getRootDirectory().set(layout.rootDir());
            // Whether the settings plugin really ran, which includes backend/ and
            // applies itself there: a settings script can name the plugin without
            // applying it (pluginManagement { plugins { id(...) version ... } }).
            org.gradle.api.plugins.ExtraPropertiesExtension extra =
                    project.getGradle().getExtensions().getExtraProperties();
            t.getSettingsPluginApplied().set(extra.has(SettingsSupport.APPLIED_MARKER)
                    && Boolean.TRUE.equals(extra.get(SettingsSupport.APPLIED_MARKER)));
            t.getPackageName().set(project.provider(() -> {
                String pkg = AppSettings.read(layout.settingsFile()).getProperty("codename1.packageName", "app");
                return pkg + ".backend";
            }));
        });
    }

    /// Writes the backend skeleton the Maven archetype generates, minus the pom.
    @DisableCachingByDefault(because = "Creates project sources once")
    public abstract static class AddBackendTask extends DefaultTask {
        /// The root project directory.
        @Internal
        public abstract DirectoryProperty getRootDirectory();

        /// Whether `com.codenameone` was applied in the settings script.
        @Input
        public abstract Property<Boolean> getSettingsPluginApplied();

        /// The package of the generated `Api` controller.
        @Input
        public abstract Property<String> getPackageName();

        @TaskAction
        public void create() {
            File backend = new File(getRootDirectory().get().getAsFile(), "backend");
            if (new File(backend, "application.properties").exists()) {
                throw new GradleException(backend + " already holds a backend");
            }
            String pkg = getPackageName().get();
            try {
                // The same skeleton the initializr and the Maven generator write.
                GradleProjectTemplate.writeBackendFiles(backend, pkg, ":backend:");
            } catch (IOException ex) {
                throw new GradleException("Could not create the backend", ex);
            }
            getLogger().lifecycle("Created " + backend + ". Run it with ./gradlew :backend:runBackend");
            File root = getRootDirectory().get().getAsFile();
            File kts = new File(root, "settings.gradle.kts");
            File groovy = new File(root, "settings.gradle");
            File settings = kts.isFile() || !groovy.isFile() ? kts : groovy;
            if (!kts.isFile() && !groovy.isFile()) {
                // A project that applies the plugin from its build script needs no
                // settings script until it has a second project; this is that moment.
                // In the build script's language, naming the root as Gradle did.
                boolean groovyBuild = new File(root, "build.gradle").isFile()
                        && !new File(root, "build.gradle.kts").isFile();
                settings = groovyBuild ? groovy : kts;
                try {
                    write(settings, groovyBuild ? "rootProject.name = '" + root.getName() + "'\n"
                            : "rootProject.name = \"" + root.getName() + "\"\n");
                } catch (IOException ex) {
                    throw new GradleException("Could not create " + settings, ex);
                }
            }
            if (settings.isFile()) {
                try {
                    String text = new String(Files.readAllBytes(settings.toPath()), StandardCharsets.UTF_8);
                    if (!getSettingsPluginApplied().get()) {
                        // The settings plugin includes backend/ and applies itself there.
                        // A build that applies the plugin from the root build script has
                        // neither, so the backend is included here and applies the plugin
                        // in its own script -- without a version, since the root already
                        // put the plugin on the classpath.
                        boolean isKts = settings.getName().endsWith(".kts");
                        if (!text.contains("include(\"backend\")") && !text.contains("include 'backend'")
                                && !text.contains("include \"backend\"")) {
                            write(settings, text + (text.endsWith("\n") ? "" : "\n")
                                    + (isKts ? "include(\"backend\")\n" : "include 'backend'\n"));
                        }
                        File script = new File(backend, isKts ? "build.gradle.kts" : "build.gradle");
                        if (!script.exists()) {
                            write(script, isKts ? "plugins {\n    id(\"com.codenameone\")\n}\n\n"
                                    + GradleProjectTemplate.text("backend/build.gradle.kts.txt").replace(
                                            "// Optional: the plugin is applied from settings.gradle.kts.\n", "")
                                    : "plugins {\n    id 'com.codenameone'\n}\n");
                        }
                    }
                } catch (IOException ex) {
                    throw new GradleException("Could not update " + settings, ex);
                }
            }
        }

        private static void write(File f, String text) throws IOException {
            File parent = f.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new IOException("Could not create " + parent);
            }
            Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
