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

import com.codename1.gradle.tasks.ProcessAnnotationsAction;
import com.codename1.gradle.tasks.RunBackendTask;
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

    private BackendSupport() {
    }

    static void apply(final Project project, final ProjectLayout layout, final CodenameOneExtension ext,
                      final Provider<Map<String, String>> userProperties) {
        AppSupport.addFramework(project, "implementation", ext.getVersion(), "codenameone-backend");
        project.getDependencies().add("runtimeOnly", SQLITE_JDBC);

        final SourceSet main = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets()
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final File stubs = new File(layout.buildDir(), "generated/sources/cn1-annotations");
        // The @RestController router and the entry point are generated here, from
        // the compiled classes, exactly as the Maven module's process-annotations
        // execution does.
        project.getTasks().named(main.getCompileJavaTaskName(), JavaCompile.class, compile -> {
            AppSupport.processingInputs(compile, layout, userProperties);
            compile.doLast("processCn1Annotations", new ProcessAnnotationsAction(
                    compile.getDestinationDirectory().get().getAsFile(), stubs, layout.projectDir(),
                    layout.settingsFile(), AppSupport.sourceRoots(main, layout),
                    "UTF-8", userProperties.get(), main.getCompileClasspath())
                    .withSourceEncoding(AppSupport.javaEncoding(project, main)));
            compile.doLast("cn1SplitOutputCheck", new com.codename1.gradle.tasks.SplitOutputCheck(
                    compile.getDestinationDirectory().get().getAsFile(), main.getOutput().getClassesDirs()));
        });
        // Kotlin controllers and entities are processed like Java ones: in a pure
        // Kotlin backend compileJava has no sources and would generate no router or
        // entry point at all. (backendPackage compiles Java sources itself, so a
        // Kotlin backend runs on the JVM with runBackend; see BackendPackager.)
        project.getPluginManager().withPlugin("org.jetbrains.kotlin.jvm", kotlin ->
                project.getTasks().named("compileKotlin").configure(compile -> {
                    AppSupport.processingInputs(compile, layout, userProperties);
                    compile.doLast("processCn1Annotations", new ProcessAnnotationsAction(
                            AppSupport.kotlinDestination(compile, layout), stubs,
                            layout.projectDir(), layout.settingsFile(), AppSupport.sourceRoots(main, layout),
                            "UTF-8", userProperties.get(), main.getCompileClasspath())
                            .withPendingJavaSources(main.getJava().getSrcDirs())
                            .withSourceEncoding(AppSupport.javaEncoding(project, main)));
                }));

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
        });
        BackendPackageSupport.register(project, layout, main, ext);
        UpdateSupport.register(project, layout);
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
            if (settings.isFile()) {
                try {
                    String text = new String(Files.readAllBytes(settings.toPath()), StandardCharsets.UTF_8);
                    if (!text.contains("com.codenameone")) {
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
