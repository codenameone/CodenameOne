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

import com.codename1.project.BuildSystem;
import com.codename1.project.ProjectKind;
import com.codename1.project.ProjectLayout;
import com.codename1.project.ProjectLayouts;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.compile.JavaCompile;

import java.util.LinkedHashMap;
import java.util.Map;

/// The plugin applied to a project: common setup, then the kind-specific part.
final class ProjectSupport {
    /// The Java release every Codename One Gradle project compiles for.
    static final int JAVA_RELEASE = 17;

    private ProjectSupport() {
    }

    static void apply(Project project) {
        if (project.getExtensions().findByName("codenameone") != null) {
            return;
        }
        SettingsSupport.addRepositoriesIfMissing(project);
        project.getPluginManager().apply(JavaPlugin.class);

        final CodenameOneExtension ext = project.getExtensions().create("codenameone", CodenameOneExtension.class);
        ext.getVersion().convention(project.getProviders().gradleProperty("codename1.version")
                .orElse(PluginInfo.version()));
        // -Popen=false is the Maven plugin's -Dopen=false, so the switch people
        // already pass to keep Xcode or Android Studio shut works here too.
        ext.getOpenGeneratedProjects().convention(project.getProviders().gradleProperty("codename1.open")
                .orElse(project.getProviders().gradleProperty("open"))
                .map(Boolean::parseBoolean).orElse(Boolean.TRUE));

        // A com.codenameone module declared without a version gets the framework's,
        // the job ${cn1.version} does in the archetype's pom. Settings' add-on
        // catalog writes its coordinates that way, and so can a build script.
        final Provider<String> frameworkVersion = ext.getVersion();
        project.getConfigurations().configureEach(c -> c.getResolutionStrategy().eachDependency(d -> {
            String requested = d.getRequested().getVersion();
            if (PluginInfo.GROUP.equals(d.getRequested().getGroup())
                    && (requested == null || requested.isEmpty())) {
                d.useVersion(frameworkVersion.get());
                d.because("the Codename One framework version");
            }
        }));

        requireJava17(project);

        ProjectKind kind = kind(project);
        ProjectLayout layout = ProjectLayouts.of(BuildSystem.GRADLE, kind,
                project.getRootDir(), project.getProjectDir());
        Provider<Map<String, String>> userProperties = userProperties(project, ext);

        switch (kind) {
            case BACKEND:
                BackendSupport.apply(project, layout, ext, userProperties);
                break;
            case LIB:
                LibrarySupport.apply(project, layout, ext, userProperties);
                break;
            default:
                AppSupport.apply(project, layout, ext, userProperties);
                break;
        }
    }

    /// The kind from the `codename1.kind` Gradle property, else from the
    /// project's files.
    static ProjectKind kind(Project project) {
        Object explicit = project.findProperty("codename1.kind");
        if (explicit != null) {
            try {
                return ProjectKind.valueOf(String.valueOf(explicit).trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new GradleException("codename1.kind must be APP, LIB or BACKEND, not " + explicit, ex);
            }
        }
        return ProjectLayouts.gradleKind(project.getProjectDir());
    }

    /// Every compile targets Java 17. A project that asks for less is refused
    /// with the reason, rather than compiling classes the Codename One
    /// toolchain was never verified against for a Gradle build.
    private static void requireJava17(Project project) {
        project.getTasks().withType(JavaCompile.class).configureEach(compile -> {
            compile.getOptions().getRelease().convention(JAVA_RELEASE);
            compile.getOptions().setEncoding(compile.getOptions().getEncoding() == null
                    ? "UTF-8" : compile.getOptions().getEncoding());
            compile.doFirst(new RequireRelease());
        });
        // Kotlin targets the JDK Gradle runs on unless told otherwise, and the
        // Kotlin plugin refuses a Java and a Kotlin target that differ -- so a
        // Kotlin project built on JDK 21 failed at compileKotlin ("Inconsistent
        // JVM Target Compatibility"). Pinned to the same release as javac.
        // The ORM enhancer keeps a bookkeeping file in each classes directory it
        // processes, and a Kotlin project has two (Java's and Kotlin's), so an
        // archive of the main output met the same path twice and `jar` failed.
        // Only the enhancer reads the file, from the directory, never from a jar.
        project.getTasks().withType(org.gradle.api.tasks.bundling.Jar.class).configureEach(jar ->
                jar.filesMatching("META-INF/cn1/orm-enhanced-dependencies.list",
                        f -> f.setDuplicatesStrategy(org.gradle.api.file.DuplicatesStrategy.EXCLUDE)));
        project.getPluginManager().withPlugin("org.jetbrains.kotlin.jvm", kotlin ->
                project.getTasks().matching(t -> t.getName().startsWith("compile")
                        && t.getName().endsWith("Kotlin")).configureEach(ProjectSupport::pinKotlinJvmTarget));
    }

    /// The `codename1.*` properties from the command line (`-P` and `-D`) and
    /// the `codenameone { buildHints }` block, as `codename1.*` keys.
    ///
    /// These are what the Maven plugin calls user properties: they override
    /// the settings file for this build only.
    static Provider<Map<String, String>> userProperties(Project project, CodenameOneExtension ext) {
        Provider<Map<String, String>> gradleProps = project.getProviders().gradlePropertiesPrefixedBy("codename1.");
        Provider<Map<String, String>> systemProps = project.getProviders().systemPropertiesPrefixedBy("codename1.");
        // codenameone { mainClass } is the settings file's packageName + mainName,
        // so it becomes those two overrides; the command line still wins.
        Provider<String> mainClass = ext.getMainClass().orElse("");
        return ext.getBuildHints().zip(mainClass, (hints, main) -> {
            Map<String, String> out = new LinkedHashMap<String, String>();
            for (Map.Entry<String, String> e : hints.entrySet()) {
                out.put("codename1.arg." + e.getKey(), e.getValue());
            }
            out.putAll(mainClassProperties(main));
            return out;
        }).zip(gradleProps, (m, gp) -> {
            Map<String, String> out = new LinkedHashMap<String, String>(m);
            out.putAll(gp);
            return out;
        }).zip(systemProps, (m, sp) -> {
            Map<String, String> out = new LinkedHashMap<String, String>(m);
            out.putAll(sp);
            return out;
        });
    }

    /// `codename1.packageName` and `codename1.mainName` for a fully qualified
    /// main class, or nothing for an empty one.
    static Map<String, String> mainClassProperties(String mainClass) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        String main = mainClass == null ? "" : mainClass.trim();
        if (main.isEmpty()) {
            return out;
        }
        int dot = main.lastIndexOf('.');
        out.put("codename1.packageName", dot < 0 ? "" : main.substring(0, dot));
        out.put("codename1.mainName", dot < 0 ? main : main.substring(dot + 1));
        return out;
    }

    /// The release the Java compile beside a Kotlin one targets: `compileKotlin`
    /// pairs with `compileJava`, `compileTestKotlin` with `compileTestJava`.
    /// [JAVA_RELEASE] when there is none or it sets no release.
    static int javaReleaseFor(org.gradle.api.Task kotlinCompile) {
        String name = kotlinCompile.getName();
        org.gradle.api.Task java = name.endsWith("Kotlin")
                ? kotlinCompile.getProject().getTasks().findByName(
                        name.substring(0, name.length() - "Kotlin".length()) + "Java")
                : null;
        return java instanceof JavaCompile
                ? ((JavaCompile) java).getOptions().getRelease().getOrElse(JAVA_RELEASE) : JAVA_RELEASE;
    }

    /// Sets a Kotlin compile task's JVM target to the release its Java compile
    /// targets -- [JAVA_RELEASE] unless the build raises it, when Kotlin 2 would
    /// otherwise refuse the two targets as inconsistent. Reflective, because the
    /// Kotlin Gradle plugin's types are not on this plugin's classpath:
    /// `compilerOptions.jvmTarget` (Kotlin 1.8 and newer), else the older
    /// `kotlinOptions.jvmTarget` string.
    @SuppressWarnings("unchecked")
    static void pinKotlinJvmTarget(final org.gradle.api.Task task) {
        try {
            Object options = task.getClass().getMethod("getCompilerOptions").invoke(task);
            Object target = options.getClass().getMethod("getJvmTarget").invoke(options);
            final Class<? extends Enum> jvmTarget = Class.forName("org.jetbrains.kotlin.gradle.dsl.JvmTarget",
                    false, task.getClass().getClassLoader()).asSubclass(Enum.class);
            ((org.gradle.api.provider.Property<Object>) target).set(task.getProject().provider(() -> {
                try {
                    return Enum.valueOf(jvmTarget, "JVM_" + javaReleaseFor(task));
                } catch (IllegalArgumentException newerThanThisKotlin) {
                    return Enum.valueOf(jvmTarget, "JVM_" + JAVA_RELEASE);
                }
            }));
            return;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // An older Kotlin plugin: fall through to kotlinOptions.
        }
        try {
            Object options = task.getClass().getMethod("getKotlinOptions").invoke(task);
            options.getClass().getMethod("setJvmTarget", String.class).invoke(options,
                    String.valueOf(javaReleaseFor(task)));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            task.getLogger().warn("cn1: could not set " + task.getPath() + "'s JVM target to " + JAVA_RELEASE
                    + "; set kotlin { compilerOptions { jvmTarget } } in build.gradle.kts if the build refuses "
                    + "the Java and Kotlin targets as inconsistent (" + ex + ")");
        }
    }

    /// Fails a compile that targets less than [JAVA_RELEASE].
    static final class RequireRelease implements org.gradle.api.Action<org.gradle.api.Task> {
        @Override
        public void execute(org.gradle.api.Task task) {
            JavaCompile compile = (JavaCompile) task;
            Integer release = compile.getOptions().getRelease().getOrNull();
            if (release != null && release < JAVA_RELEASE) {
                throw new GradleException("Codename One Gradle projects compile for Java " + JAVA_RELEASE
                        + " or newer, but " + task.getPath() + " asks for Java " + release + ". Remove the "
                        + "release setting, or use the Maven build for a project that must target Java 8.");
            }
        }
    }
}
