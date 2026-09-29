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
import com.codename1.gradle.tasks.Cn1Task;
import com.codename1.maven.CodenameOneLogin;
import com.codename1.maven.DesktopTool;
import com.codename1.project.ProjectDescriptor;
import com.codename1.project.ProjectLayout;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/// `settings`, `guibuilder`, `gameBuilder` and `certificateWizard`: the desktop
/// tools, bound to this project.
///
/// The same tools, launched the same way ([DesktopTool]), as the Maven goals.
/// The binding they read is the project descriptor with the tool's own keys
/// added, so each tool knows it is editing a Gradle project -- which is how
/// Settings, for one, adds a cn1lib to `build.gradle.kts` rather than a pom.
final class ToolSupport {
    private ToolSupport() {
    }

    static void register(Project project, ProjectLayout layout, SourceSet main, CodenameOneExtension ext,
                         Provider<Map<String, String>> userProperties) {
        // Evaluated when the task runs, after the build script has had its say
        // about the source directories.
        Provider<List<String>> roots = project.provider(() -> sourceRoots(main));
        register(project, layout, ext, userProperties, roots, "settings", DesktopTool.SETTINGS,
                "Opens Codename One Settings for this project");
        register(project, layout, ext, userProperties, roots, "guibuilder", DesktopTool.GUI_BUILDER,
                "Opens the Codename One GUI Builder for this project");
        register(project, layout, ext, userProperties, roots, "gameBuilder", DesktopTool.GAME_BUILDER,
                "Opens the Codename One Game Builder for this project");
        register(project, layout, ext, userProperties, roots, "certificateWizard", DesktopTool.CERTIFICATE_WIZARD,
                "Opens the iOS Certificate Wizard for this project");
    }

    /// The main source set's Java and Kotlin directories as the build resolved
    /// them, a `sourceSets { }` block included. Settings treats the descriptor's
    /// roots as the whole compiled-source set, so the conventional layout's alone
    /// would hide a relocated main class and its annotations from it.
    static List<String> sourceRoots(SourceSet main) {
        java.util.LinkedHashSet<String> roots = new java.util.LinkedHashSet<String>();
        for (File dir : main.getJava().getSrcDirs()) {
            roots.add(dir.getAbsolutePath());
        }
        Object kotlin = main.getExtensions().findByName("kotlin");
        if (kotlin instanceof org.gradle.api.file.SourceDirectorySet) {
            for (File dir : ((org.gradle.api.file.SourceDirectorySet) kotlin).getSrcDirs()) {
                roots.add(dir.getAbsolutePath());
            }
        }
        return new ArrayList<String>(roots);
    }

    private static void register(Project project, ProjectLayout layout, CodenameOneExtension ext,
                                 Provider<Map<String, String>> userProperties, Provider<List<String>> sourceRoots,
                                 String name, DesktopTool tool, String description) {
        final Configuration classpath = AppSupport.resolvable(project, "cn1Tool"
                + name.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + name.substring(1),
                "The " + tool.displayName());
        // The tools are Codename One applications, so they run on the JavaSE port,
        // but their published poms carry codenameone-javase in TEST scope only.
        // Maven's legacy resolver, which the cn1: goals use, ignores scopes and
        // picks it up by accident; Gradle honours them and would start the tool
        // without the port (NoClassDefFoundError: JavaSEPort). Declared here.
        AppSupport.addFramework(project, classpath.getName(), ext.getVersion(), tool.artifactId(),
                "codenameone-javase");
        project.getTasks().register(name, ToolTask.class, t -> {
            AppSupport.common(t, project, layout, ext, userProperties);
            t.setDescription(description);
            t.getToolName().set(name);
            t.getSourceRoots().set(sourceRoots);
            t.getToolClasspath().from(classpath);
            t.getDetached().set(project.getProviders().gradleProperty("spawn").map(Boolean::parseBoolean)
                    .orElse(Boolean.TRUE));
            t.getInitialForm().set(project.getProviders().gradleProperty("className"));
            t.getToken().set(project.getProviders().gradleProperty("token"));
            t.getUser().set(project.getProviders().gradleProperty("user"));
            t.getBaseUrl().set(project.getProviders().gradleProperty("baseUrl")
                    .orElse("https://cloud.codenameone.com"));
            t.getOutputs().upToDateWhen(x -> false);
        });
    }

    /// See [ToolSupport].
    @DisableCachingByDefault(because = "Opens a desktop tool")
    public abstract static class ToolTask extends Cn1Task {
        /// Which tool: settings, guibuilder, gameBuilder or certificateWizard.
        @Input
        public abstract Property<String> getToolName();

        /// The tool and its runtime.
        @Classpath
        public abstract ConfigurableFileCollection getToolClasspath();

        /// The main source set's source directories; see [ToolSupport#sourceRoots].
        @Input
        public abstract org.gradle.api.provider.ListProperty<String> getSourceRoots();

        /// Return at once (the default) rather than wait for the window to close.
        @Input
        public abstract Property<Boolean> getDetached();

        /// The GUI Builder form, or Game Builder scene, to open first.
        @Input
        @Optional
        public abstract Property<String> getInitialForm();

        /// A Codename One signing API token for the Certificate Wizard.
        @Input
        @Optional
        public abstract Property<String> getToken();

        /// The account for the Certificate Wizard.
        @Input
        @Optional
        public abstract Property<String> getUser();

        /// The Codename One cloud the Certificate Wizard signs in to.
        @Input
        public abstract Property<String> getBaseUrl();

        @TaskAction
        public void open() {
            DesktopTool tool = toolFor(getToolName().get());
            ProjectLayout layout = layout();
            List<File> classpath = new ArrayList<File>(getToolClasspath().getFiles());
            File primary = tool.primaryJar(classpath);
            if (primary == null) {
                throw new GradleException("Could not resolve " + tool.displayName() + " (com.codenameone:"
                        + tool.artifactId() + ":" + getCodenameOneVersion().get() + ")");
            }
            Properties effective = effectiveSettings();
            ProjectDescriptor binding = ProjectDescriptor.fromLayout(layout);
            if (!getSourceRoots().get().isEmpty()) {
                binding.set("sourceRoot", null);
                for (String root : getSourceRoots().get()) {
                    binding.add("sourceRoot", root);
                }
            }
            binding.set("sourceEncoding", "UTF-8");
            binding.set("mainName", effective.getProperty("codename1.mainName"));
            binding.set("packageName", effective.getProperty("codename1.packageName"));
            List<String> extra = new ArrayList<String>();
            try {
                if (tool == DesktopTool.GUI_BUILDER) {
                    ensureDir(layout.guiBuilderDir());
                    binding.set("guiDir", layout.guiBuilderDir().getAbsolutePath());
                    binding.set("sourceDir", layout.javaSourceDir().getAbsolutePath());
                    binding.set("cssFile", layout.themeCss().getAbsolutePath());
                    binding.set("initialForm", getInitialForm().getOrNull());
                } else if (tool == DesktopTool.GAME_BUILDER) {
                    ensureDir(layout.gamesDir());
                    binding.set("gamesDir", layout.gamesDir().getAbsolutePath());
                    binding.set("sourceDir", layout.javaSourceDir().getAbsolutePath());
                    binding.set("output", new File(tool.runtimeDir(), java.util.UUID.randomUUID() + ".output")
                            .getAbsolutePath());
                    if (getInitialForm().isPresent()) {
                        extra.add("-Dgamebuilder.scene=" + getInitialForm().get());
                    }
                } else if (tool == DesktopTool.CERTIFICATE_WIZARD) {
                    File certs = new File(layout.projectDir(), "iosCerts");
                    ensureDir(certs);
                    CodenameOneLogin.LoginResult signIn = new CodenameOneLogin(log()).resolve(
                            getToken().getOrNull(), getUser().getOrNull(), true, 180, getBaseUrl().get());
                    if (signIn.token.isEmpty()) {
                        getLogger().warn("No Codename One bearer token was found. The wizard opens in offline mode "
                                + "unless you pass -Ptoken=<jwt> or sign in.");
                    }
                    binding.set("outputDir", certs.getAbsolutePath());
                    binding.set("output", new File(tool.runtimeDir(), java.util.UUID.randomUUID() + ".output")
                            .getAbsolutePath());
                    binding.set("user", signIn.user);
                    binding.set("token", signIn.token);
                    binding.set("baseUrl", getBaseUrl().get());
                }
                File input = tool.writeBinding(binding.format());
                getLogger().lifecycle("Launching " + tool.displayName() + " bound to " + layout.projectDir());
                tool.launch(classpath, primary, input, layout.projectDir(), getDetached().get(), extra, log());
            } catch (IOException ex) {
                throw new GradleException("Could not write the " + tool.displayName() + " binding", ex);
            } catch (BuildExecutionException ex) {
                throw new GradleException(ex.getMessage(), ex);
            }
        }

        private static void ensureDir(File dir) throws IOException {
            if (!dir.isDirectory() && !dir.mkdirs()) {
                throw new IOException("Could not create " + dir);
            }
        }

        private static DesktopTool toolFor(String name) {
            switch (name) {
                case "settings":
                    return DesktopTool.SETTINGS;
                case "guibuilder":
                    return DesktopTool.GUI_BUILDER;
                case "gameBuilder":
                    return DesktopTool.GAME_BUILDER;
                default:
                    return DesktopTool.CERTIFICATE_WIZARD;
            }
        }
    }
}
