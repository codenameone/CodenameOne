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

import com.codename1.gradle.tasks.Cn1BuildTask;
import com.codename1.gradle.tasks.Cn1CssTask;
import com.codename1.gradle.tasks.NativeInterfacesTask;
import com.codename1.gradle.tasks.PrepareSimulatorTask;
import com.codename1.gradle.tasks.ProcessAnnotationsAction;
import com.codename1.gradle.tasks.TranscodeSvgTask;
import com.codename1.maven.SimulatorSupport;
import com.codename1.project.NativePlatform;
import com.codename1.project.ProjectLayout;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Everything an application project gets.
final class AppSupport {
    /// The task group the Codename One tasks are listed under.
    static final String GROUP = "codename one";
    /// The task group the build targets are listed under.
    static final String BUILD_GROUP = "codename one build";

    /// Each named build task: task name, `codename1.platform`,
    /// `codename1.buildTarget`, description. The same pairs as the Maven
    /// build wrappers and the archetype's build.sh.
    static final String[][] BUILD_TARGETS = {
        {"buildAndroid", "android", "android-device", "Sends an Android build to the build server"},
        {"buildAndroidGradleProject", "android", "android-source", "Generates an Android Studio project locally"},
        {"buildIos", "ios", "ios-device", "Sends an iOS debug build to the build server"},
        {"buildIosRelease", "ios", "ios-device-release", "Sends an iOS App Store build to the build server"},
        {"buildIosXcodeProject", "ios", "ios-source", "Generates an Xcode project locally"},
        {"buildMacNative", "ios", "mac-os-x-native", "Sends a native macOS build to the build server"},
        {"buildMacDesktop", "javase", "mac-os-x-desktop", "Sends a macOS desktop (JVM) build to the build server"},
        {"buildWindowsDesktop", "javase", "windows-desktop", "Sends a Windows desktop (JVM) build to the build server"},
        {"buildWindowsDevice", "win", "windows-device", "Sends a native Windows build to the build server"},
        {"buildLinuxDevice", "linux", "linux-device", "Sends a native Linux build to the build server"},
        {"buildJavascript", "javascript", "javascript", "Sends a JavaScript build to the build server"},
        {"buildJavascriptLocal", "javascript", "local-javascript", "Builds the JavaScript port locally"},
    };

    private AppSupport() {
    }

    static void apply(final Project project, final ProjectLayout layout, final CodenameOneExtension ext,
                      final Provider<Map<String, String>> userProperties) {
        final Provider<String> version = ext.getVersion();
        SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        final SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        // The framework is `provided`: compiled against, supplied by the build server
        // or the simulator, never part of the upload.
        addFramework(project, "compileOnly", version, "codenameone-core", "java-runtime");
        addFramework(project, "testImplementation", version, "codenameone-core", "codenameone-javase");
        Configuration framework = resolvable(project, "cn1Framework", "codenameone-core and java-runtime, for local builds");
        framework.setTransitive(false);
        addFramework(project, framework.getName(), version, "codenameone-core", "java-runtime");
        Configuration simulator = resolvable(project, "cn1Simulator", "The simulator: the JavaSE port and its natives");
        addFramework(project, simulator.getName(), version, "codenameone-core", "codenameone-javase");
        project.getDependencies().addProvider(simulator.getName(),
                version.map(v -> PluginInfo.GROUP + ":cn1-binaries-javase:" + v));
        Configuration cssCompiler = resolvable(project, "cn1CssCompiler", "The Codename One CSS compiler");
        addFramework(project, cssCompiler.getName(), version, "codenameone-css-cli");

        Cn1libs.configure(project);

        // The simulator's native-interface implementations: a real source set, so
        // src/javase/java compiles against the application and the JavaSE port and
        // lands on the simulator's classpath the next time it runs. Declared always;
        // Gradle is happy with a directory that does not exist yet.
        final SourceSet javase = sourceSets.create("javase", ss -> {
            ss.getJava().setSrcDirs(Collections.singletonList(layout.nativeSourceDir(NativePlatform.JAVASE)));
            ss.getResources().setSrcDirs(Collections.singletonList(
                    new File(layout.projectDir(), "src" + File.separator + "javase" + File.separator + "resources")));
            ss.setCompileClasspath(main.getOutput().plus(main.getCompileClasspath()).plus(simulator)
                    .plus(project.getConfigurations().getByName(Cn1libs.configurationName("javase"))));
        });

        TaskProvider<TranscodeSvgTask> svg = project.getTasks().register("transcodeSvg", TranscodeSvgTask.class, t -> {
            common(t, project, layout, ext, userProperties);
            t.setDescription("Transcodes SVG and Lottie assets into Java sources");
            // Every directory the transcoder reads, or adding an asset to one of
            // them leaves the task up to date and its generated classes stale.
            t.getSources().from(layout.cssDir(), new File(layout.projectDir(), "src/main/svg"),
                    new File(layout.projectDir(), "src/main/lottie"));
            t.getOutputDirectory().set(new File(layout.buildDir(), "generated/sources/cn1-svg"));
            t.getPlaceholderDirectory().set(new File(layout.buildDir(), "css-resources"));
        });
        main.getJava().srcDir(svg.flatMap(TranscodeSvgTask::getOutputDirectory));

        registerGuiSources(project, layout, ext, userProperties, main);

        TaskProvider<Cn1CssTask> css = project.getTasks().register("cn1Css", Cn1CssTask.class, t -> {
            common(t, project, layout, ext, userProperties);
            t.setDescription("Compiles src/main/css into theme.res");
            t.getSources().from(layout.cssDir(), layout.l10nDir(), layout.settingsFile());
            t.getCompilerClasspath().from(cssCompiler);
            t.getOutputDirectory().set(new File(layout.buildDir(), "generated/resources/cn1-css"));
            t.getWorkDirectory().set(cssWorkDir(layout));
            Provider<Set<ResolvedArtifactResult>> resolved = project.getConfigurations().getByName("compileClasspath")
                    .getIncoming().artifactView(v -> v.setLenient(true)).getArtifacts().getResolvedArtifacts();
            t.getLibraryCss().set(resolved.map(AppSupport::cssBundles));
            t.getLibraryCssFiles().from(resolved.map(set -> filesOf(set, true)));
        });
        main.getResources().srcDir(css.flatMap(Cn1CssTask::getOutputDirectory));

        final File stubs = new File(layout.buildDir(), "generated/sources/cn1-annotations");
        final Provider<List<String>> compileArtifacts = project.getConfigurations()
                .getByName(main.getCompileClasspathConfigurationName()).getIncoming()
                .artifactView(v -> v.setLenient(true)).getArtifacts().getResolvedArtifacts()
                .map(set -> AppSupport.encode(set, "provided"));
        final Map<String, String> complianceProperties = new java.util.HashMap<String, String>();
        // -P, or -D: BytecodeCompliance also honours the JVM system property, so
        // it has to be an input too, or a -D skipped compile would satisfy the
        // next ordinary build as up to date.
        Object skip = skipComplianceCheck(project);
        if (skip != null) {
            complianceProperties.put("skipComplianceCheck", String.valueOf(skip));
        }
        // The two post-compile steps depend on more than the sources: hot reload
        // compiles with -PskipComplianceCheck=true, and the codename1.* overrides
        // reach the annotation processors. As inputs, a change in either reruns the
        // compile -- otherwise a native build after a hot-reload compile found
        // compileJava up to date and uploaded classes nobody had checked.
        final String skipInput = String.valueOf(skip);
        project.getTasks().named(main.getCompileJavaTaskName(), JavaCompile.class, compile -> {
            compile.getInputs().property("cn1SkipComplianceCheck", skipInput);
            processingInputs(compile, layout, userProperties);
            List<String> roots = sourceRoots(main, layout);
            // Compliance first (it caps and rewrites classes in place), then the
            // annotation processors, which stamp the result -- the order the Maven
            // poms bind process-classes in.
            // Kotlin's classes (compiled first, into a directory of their own) are
            // the Java pass's siblings, so Java calling Kotlin resolves.
            compile.doLast("cn1Compliance", new com.codename1.gradle.tasks.ComplianceAction(layout.rootDir(),
                    layout.projectDir(), compile.getDestinationDirectory().get().getAsFile(), project.getName(),
                    main.getCompileClasspath(), compileArtifacts, complianceProperties)
                    .withSiblingClasses(main.getOutput().getClassesDirs()));
            compile.doLast("processCn1Annotations", new ProcessAnnotationsAction(
                    compile.getDestinationDirectory().get().getAsFile(), stubs, layout.projectDir(),
                    layout.settingsFile(), roots, "UTF-8", userProperties.get(), main.getCompileClasspath()));
            // Last: in a Java and Kotlin project, both passes have run by now.
            compile.doLast("cn1SplitOutputCheck", new com.codename1.gradle.tasks.SplitOutputCheck(
                    compile.getDestinationDirectory().get().getAsFile(), main.getOutput().getClassesDirs()));
        });

        // Kotlin compiles into a directory of its own, before javac. The same two
        // steps run over it, so a Kotlin application is checked and processed like a
        // Java one. Wired by name because the Kotlin plugin's types are not on this
        // plugin's classpath.
        project.getPluginManager().withPlugin("org.jetbrains.kotlin.jvm", kotlin ->
                project.getTasks().named("compileKotlin").configure(compile -> {
                    compile.getInputs().property("cn1SkipComplianceCheck", skipInput);
                    processingInputs(compile, layout, userProperties);
                    File kotlinClasses = kotlinDestination(compile, layout);
                    List<String> roots = sourceRoots(main, layout);
                    // javac has not run yet, so the Java classes Kotlin calls are
                    // known by their sources.
                    compile.doLast("cn1Compliance", new com.codename1.gradle.tasks.ComplianceAction(
                            layout.rootDir(), layout.projectDir(), kotlinClasses, project.getName(),
                            main.getCompileClasspath(), compileArtifacts, complianceProperties)
                            .withPendingJavaSources(main.getJava().getSrcDirs()));
                    compile.doLast("processCn1Annotations", new ProcessAnnotationsAction(kotlinClasses, stubs,
                            layout.projectDir(), layout.settingsFile(), roots, "UTF-8", userProperties.get(),
                            main.getCompileClasspath())
                            .withPendingJavaSources(main.getJava().getSrcDirs()));
                }));

        project.getTasks().register("cn1Compile", t -> {
            t.setGroup(GROUP);
            t.setDescription("Compiles the application and its simulator native code (used by hot reload)");
            t.dependsOn(main.getClassesTaskName(), javase.getClassesTaskName());
        });

        TaskProvider<PrepareSimulatorTask> prepare = project.getTasks().register("prepareSimulator",
                PrepareSimulatorTask.class, t -> {
                    common(t, project, layout, ext, userProperties);
                    t.setDescription("Writes the files the simulator reads at startup");
                    t.getCompileClasspath().from(main.getCompileClasspath());
                    t.getCssCompilerClasspath().from(cssCompiler);
                    t.getSimulatorProperties().set(SimulatorSupport.simulatorPropertiesFile(layout.buildDir()));
                    t.getDescriptor().set(layout.descriptorFile());
                    // The main class and package the descriptor records come from here.
                    t.getInputs().files(layout.settingsFile()).withPropertyName("settingsFile");
                });

        registerSimulator(project, "run", "Runs the application in the Codename One simulator", false,
                layout, main, javase, simulator, prepare, css, ext, userProperties);
        registerSimulator(project, "debug", "Runs the simulator suspended, waiting for a debugger on port 5005", true,
                layout, main, javase, simulator, prepare, css, ext, userProperties);

        project.getTasks().register("generateNativeInterfaces",
                NativeInterfacesTask.class, t -> {
                    nativeCommon(t, project, layout, ext, userProperties, main);
                    t.setDescription("Writes implementation stubs for every NativeInterface into src/<platform>/");
                    t.getOnly().set(project.getProviders().gradleProperty("cn1.nativeInterface"));
                    t.getSwift().set(flag(project, "cn1.swift"));
                    t.getKotlin().set(flag(project, "cn1.kotlin"));
                    t.getOverwrite().set(flag(project, "cn1.overwrite"));
                    t.getOutputs().upToDateWhen(x -> false);
                });

        final Provider<BuildQueue> queue = project.getGradle().getSharedServices().registerIfAbsent(
                BuildQueue.NAME, BuildQueue.class, spec -> spec.getMaxParallelUsages().set(1));
        for (String[] target : BUILD_TARGETS) {
            registerBuild(project, target[0], target[1], target[2], target[3], layout, main, javase, framework, ext,
                    userProperties, queue);
        }
        // The generic form, for any target: -Pcodename1.platform=... -Pcodename1.buildTarget=...
        registerBuild(project, "cn1Build", null, null,
                "Runs the build given by -Pcodename1.platform and -Pcodename1.buildTarget",
                layout, main, javase, framework, ext, userProperties, queue);

        final SourceSet test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
        project.getTasks().register("cn1Test", com.codename1.gradle.tasks.Cn1TestTask.class, t -> {
            common(t, project, layout, ext, userProperties);
            t.setGroup("verification");
            t.setDescription("Runs the Codename One unit tests in the simulator's test runner");
            t.dependsOn(test.getClassesTaskName(), javase.getClassesTaskName(), css);
            t.getTestClassesDirectories().from(test.getOutput().getClassesDirs());
            t.getRuntimeClasspath().from(test.getRuntimeClasspath(), javase.getOutput(), simulator,
                    project.getConfigurations().getByName(Cn1libs.configurationName("javase")));
            t.getReportsDirectory().set(new File(layout.buildDir(), "cn1-reports"));
        });

        BackendSupport.registerAddBackend(project, layout);
        ToolSupport.register(project, layout, main, ext, userProperties);
        UpdateSupport.register(project, layout);
    }

    private static void registerSimulator(final Project project, String name, String description, final boolean debug,
                                          final ProjectLayout layout, final SourceSet main, final SourceSet javase,
                                          final Configuration simulator,
                                          final TaskProvider<PrepareSimulatorTask> prepare,
                                          final TaskProvider<Cn1CssTask> css, final CodenameOneExtension ext,
                                          final Provider<Map<String, String>> userProperties) {
        project.getTasks().register(name, JavaExec.class, t -> {
            t.setGroup(GROUP);
            t.setDescription(description);
            t.dependsOn(prepare, css, main.getClassesTaskName(), javase.getClassesTaskName());
            t.getMainClass().set(SimulatorSupport.SIMULATOR_MAIN_CLASS);
            t.setClasspath(main.getRuntimeClasspath().plus(javase.getOutput()).plus(simulator)
                    .plus(project.getConfigurations().getByName(Cn1libs.configurationName("javase"))));
            t.setWorkingDir(layout.projectDir());
            t.setMaxHeapSize("1024M");
            t.setDebug(debug);
            // Live CSS reload recompiles into the resources the simulator runs from.
            // The cn1libs' stylesheets first, as cn1Css compiled them; a reload from
            // theme.css alone would drop every library style.
            t.getJvmArgumentProviders().add(new CssInputArgument(
                    new File(cssWorkDir(layout), Cn1CssTask.SIMULATOR_INPUTS),
                    layout.themeCss()));
            t.systemProperty(SimulatorSupport.CSS_OUTPUT_PROPERTY,
                    new File(layout.resourcesOutputDir(), "theme.res").getAbsolutePath());
            t.systemProperty(SimulatorSupport.CSS_MERGE_PROPERTY,
                    new File(cssWorkDir(layout), "theme.css").getAbsolutePath());
            t.getArgumentProviders().add(new MainClassArgument(layout.settingsFile(), userProperties));
        });
    }

    private static void registerBuild(final Project project, String name, final String platform,
                                      final String buildTarget, String description, final ProjectLayout layout,
                                      final SourceSet main, final SourceSet javase, final Configuration framework,
                                      final CodenameOneExtension ext,
                                      final Provider<Map<String, String>> userProperties,
                                      final Provider<BuildQueue> queue) {
        final Provider<String> platformProvider = platform != null ? project.provider(() -> platform)
                : project.getProviders().gradleProperty("codename1.platform");
        final Provider<String> targetProvider = buildTarget != null ? project.provider(() -> buildTarget)
                : project.getProviders().gradleProperty("codename1.buildTarget");

        TaskProvider<NativeInterfacesTask> verify = project.getTasks().register("verifyNativeInterfaces"
                + name.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + name.substring(1),
                NativeInterfacesTask.class, t -> {
                    nativeCommon(t, project, layout, ext, userProperties, main);
                    t.setGroup(null);
                    t.setDescription("Checks every NativeInterface is implemented for " + name);
                    t.getVerifyPlatform().set(platformProvider.map(AppSupport::nativePlatformOf));
                    t.getSwift().set(false);
                    t.getKotlin().set(false);
                    t.getOverwrite().set(false);
                });

        project.getTasks().register(name, Cn1BuildTask.class, t -> {
            common(t, project, layout, ext, userProperties);
            t.setGroup(BUILD_GROUP);
            t.setDescription(description);
            t.dependsOn(main.getClassesTaskName(), javase.getClassesTaskName(), verify);
            t.usesService(queue);
            t.getPlatform().set(platformProvider);
            t.getBuildTarget().set(targetProvider);
            t.getAutomated().set(project.getProviders().gradleProperty("automated").map(Boolean::parseBoolean)
                    .orElse(Boolean.FALSE));
            t.getStageOnly().set(project.getProviders().gradleProperty("codename1.stageOnly")
                    .map(Boolean::parseBoolean).orElse(Boolean.FALSE));
            t.getOpen().set(ext.getOpenGeneratedProjects());
            t.getFrameworkJars().from(framework);
            t.getSourceRoots().set(Collections.singletonList(layout.javaSourceDir().getAbsolutePath()));
            t.getFinalName().set(project.getName());
            t.getGroupId().set(project.provider(() -> String.valueOf(project.getGroup())));
            // A Maven platform module declares codename1.projectPlatform, and the
            // build runs only in the module whose platform matches. A Gradle
            // project is every platform at once, so each build task says which.
            t.getProjectProperties().set(platformProvider.map(
                    p -> Collections.singletonMap("codename1.projectPlatform", p)));

            ConfigurableFileCollection upload = t.getUploadClasspath();
            upload.from(main.getOutput());
            upload.from(platformProvider.map(p -> platformSources(layout, p, javase)));
            upload.from(project.getConfigurations().getByName(main.getRuntimeClasspathConfigurationName()));
            final Configuration runtime = project.getConfigurations().getByName(main.getRuntimeClasspathConfigurationName());
            Provider<Set<ResolvedArtifactResult>> runtimeArtifacts = runtime.getIncoming()
                    .artifactView(v -> v.setLenient(true)).getArtifacts().getResolvedArtifacts();
            Provider<List<String>> encoded = runtimeArtifacts.map(set -> encode(set, "compile"));
            upload.from(platformProvider.map(p -> {
                Configuration c = project.getConfigurations().findByName(Cn1libs.configurationName(p));
                return c == null ? Collections.emptyList() : c;
            }));
            t.getArtifacts().set(encoded.zip(platformProvider, (list, p) -> {
                List<String> all = new ArrayList<String>(list);
                Configuration c = project.getConfigurations().findByName(Cn1libs.configurationName(p));
                if (c != null) {
                    all.addAll(encode(c.getIncoming().artifactView(v -> v.setLenient(true)).getArtifacts()
                            .getArtifacts(), "compile"));
                }
                return all;
            }));
        });
    }

    /// The platform's own sources that ride the upload: the simulator's
    /// compiled native code for the JVM targets, the native source directory
    /// (as resources, exactly as Maven packages a platform module) otherwise.
    private static Object platformSources(ProjectLayout layout, String platform, SourceSet javase) {
        if ("javase".equals(platform)) {
            return javase.getOutput();
        }
        NativePlatform p = NativePlatform.fromId(platform);
        if (p == null) {
            return Collections.emptyList();
        }
        // The directories whether or not they exist yet. Asking here would be
        // answered once and cached with the configuration, so a directory that
        // generateNativeInterfaces creates later would never reach the upload; the
        // engine skips a classpath element that does not exist. Beside the native
        // sources, the platform's resources -- a Maven platform module's
        // src/main/resources, which the conversion moves to src/<platform>/resources.
        return java.util.Arrays.asList(layout.nativeSourceDir(p),
                new File(layout.projectDir(), "src" + File.separator + p.id() + File.separator + "resources"));
    }

    /// The native platform whose implementations a build platform needs; the
    /// JVM desktop targets use the simulator's.
    static String nativePlatformOf(String platform) {
        return platform;
    }

    /// `skipComplianceCheck` as given with -P or -D, or null.
    static String skipComplianceCheck(Project project) {
        Object skip = project.findProperty("skipComplianceCheck");
        return skip != null ? String.valueOf(skip)
                : project.getProviders().systemProperty("skipComplianceCheck").getOrNull();
    }

    /// `generateGuiSources`, whose generated views are a main source root. An
    /// application's and a cn1lib's alike: a library's own views extend the
    /// `Abstract*` classes it generates, exactly as an application's do.
    static void registerGuiSources(final Project project, final ProjectLayout layout, final CodenameOneExtension ext,
                                   final Provider<Map<String, String>> userProperties, SourceSet main) {
        TaskProvider<com.codename1.gradle.tasks.GenerateGuiSourcesTask> gui = project.getTasks().register(
                "generateGuiSources", com.codename1.gradle.tasks.GenerateGuiSourcesTask.class, t -> {
                    common(t, project, layout, ext, userProperties);
                    t.setDescription("Generates sources from GUI builder XML and CodeRAD view templates");
                    t.getSources().from(layout.guiBuilderDir(), layout.radViewsDir());
                    // The simulator's hot reload regenerates views into the same place.
                    t.getRadOutputDirectory().set(new File(layout.buildDir(), "generated-sources/rad-views"));
                });
        main.getJava().srcDir(gui.flatMap(com.codename1.gradle.tasks.GenerateGuiSourcesTask::getRadOutputDirectory));
    }

    static void common(com.codename1.gradle.tasks.Cn1Task t, Project project, ProjectLayout layout,
                       CodenameOneExtension ext, Provider<Map<String, String>> userProperties) {
        if (t.getGroup() == null) {
            t.setGroup(GROUP);
        }
        t.getRootDirectory().set(layout.rootDir());
        t.getProjectDirectory().set(layout.projectDir());
        t.getKind().set(layout.kind().name());
        t.getUserProperties().set(userProperties);
        t.getCodenameOneVersion().set(ext.getVersion());
    }

    private static void nativeCommon(NativeInterfacesTask t, Project project, ProjectLayout layout,
                                     CodenameOneExtension ext, Provider<Map<String, String>> userProperties,
                                     SourceSet main) {
        common(t, project, layout, ext, userProperties);
        t.dependsOn(main.getClassesTaskName());
        t.getClassesDirectories().from(main.getOutput().getClassesDirs());
        t.getCompileClasspath().from(main.getCompileClasspath());
    }

    private static Provider<Boolean> flag(Project project, String name) {
        return project.getProviders().gradleProperty(name).map(v -> v.isEmpty() || Boolean.parseBoolean(v))
                .orElse(Boolean.FALSE);
    }

    static Configuration resolvable(Project project, String name, String description) {
        return project.getConfigurations().create(name, c -> {
            c.setCanBeResolved(true);
            c.setCanBeConsumed(false);
            c.setDescription(description);
        });
    }

    /// Where a Kotlin compile task writes its classes: the task's own
    /// `destinationDirectory`, which follows a relocated build directory, read by
    /// reflection because the Kotlin plugin's types are not on this plugin's
    /// classpath. The conventional path only if the task will not say.
    static File kotlinDestination(org.gradle.api.Task compile, ProjectLayout layout) {
        try {
            Object dir = compile.getClass().getMethod("getDestinationDirectory").invoke(compile);
            if (dir instanceof org.gradle.api.file.DirectoryProperty) {
                return ((org.gradle.api.file.DirectoryProperty) dir).get().getAsFile();
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            compile.getLogger().info("cn1: " + compile.getPath() + " did not report its destination: " + ex);
        }
        return new File(layout.buildDir(), "classes" + File.separator + "kotlin" + File.separator + "main");
    }

    /// What the annotation processors read besides the compiled classes: the
    /// codename1.* overrides and the settings file -- its mainName and
    /// packageName pick the entry point the manifest is stamped for. As inputs,
    /// changing either reruns the compile and so the processing.
    static void processingInputs(org.gradle.api.Task compile, ProjectLayout layout,
                                 Provider<Map<String, String>> userProperties) {
        compile.getInputs().property("cn1UserProperties", userProperties);
        compile.getInputs().files(layout.settingsFile()).withPropertyName("cn1Settings")
                .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE);
    }

    /// The main source set's source directories, as the build really has them:
    /// Java's and, with the Kotlin plugin, Kotlin's -- including any a build
    /// script added. The processors use them to tell live classes from stale
    /// output, so a directory missing here makes its classes look orphaned.
    static List<String> sourceRoots(SourceSet main, ProjectLayout layout) {
        java.util.LinkedHashSet<String> roots = new java.util.LinkedHashSet<String>();
        roots.add(layout.javaSourceDir().getAbsolutePath());
        for (File dir : main.getJava().getSrcDirs()) {
            roots.add(dir.getAbsolutePath());
        }
        Object kotlin = main.getExtensions().findByName("kotlin");
        if (kotlin instanceof org.gradle.api.file.SourceDirectorySet) {
            for (File dir : ((org.gradle.api.file.SourceDirectorySet) kotlin).getSrcDirs()) {
                roots.add(dir.getAbsolutePath());
            }
        } else {
            roots.add(new File(layout.projectDir(), "src/main/kotlin").getAbsolutePath());
        }
        return new ArrayList<String>(roots);
    }

    static void addFramework(Project project, String configuration, Provider<String> version, String... artifactIds) {
        for (final String artifactId : artifactIds) {
            project.getDependencies().addProvider(configuration,
                    version.map(v -> PluginInfo.GROUP + ":" + artifactId + ":" + v));
        }
    }

    static List<String> encode(Set<ResolvedArtifactResult> set, String scope) {
        List<String> out = new ArrayList<String>();
        for (ResolvedArtifactResult r : set) {
            String e = GradleHostFactory.encode(r, scope);
            if (e != null) {
                out.add(e);
            }
        }
        return out;
    }

    private static List<String> cssBundles(Set<ResolvedArtifactResult> set) {
        List<String> out = new ArrayList<String>();
        for (String e : encode(set, "compile")) {
            com.codename1.build.BuildArtifact a = GradleHostFactory.decode(e);
            if (a != null && "cn1css".equals(a.getClassifier())) {
                out.add(e);
            }
        }
        return out;
    }

    private static List<File> filesOf(Set<ResolvedArtifactResult> set, boolean cssOnly) {
        List<File> out = new ArrayList<File>();
        for (ResolvedArtifactResult r : set) {
            if (!cssOnly || r.getFile().getName().endsWith("-cn1css.zip")) {
                out.add(r.getFile());
            }
        }
        return out;
    }

    /// The simulator's one argument: the application's main class, from the
    /// effective settings so a `-Pcodename1.mainName` override is honoured.
    /// cn1Css's work directory, which the simulator reads its CSS inputs from.
    static File cssWorkDir(ProjectLayout layout) {
        return new File(layout.buildDir(), "css");
    }

    /// The simulator's live CSS reload inputs: what cn1Css recorded when it ran,
    /// else the application's theme.css alone (CSS switched off, so no list).
    static final class CssInputArgument implements org.gradle.process.CommandLineArgumentProvider {
        private final File recorded;
        private final File themeCss;

        CssInputArgument(File recorded, File themeCss) {
            this.recorded = recorded;
            this.themeCss = themeCss;
        }

        @Override
        public Iterable<String> asArguments() {
            String inputs = themeCss.getAbsolutePath();
            if (recorded.isFile()) {
                try {
                    String listed = new String(java.nio.file.Files.readAllBytes(recorded.toPath()),
                            java.nio.charset.StandardCharsets.UTF_8).trim();
                    if (!listed.isEmpty()) {
                        inputs = listed;
                    }
                } catch (java.io.IOException ignored) {
                    // Falls back to the application's own stylesheet.
                }
            }
            return Collections.singletonList("-D" + SimulatorSupport.CSS_INPUT_PROPERTY + "=" + inputs);
        }
    }

    static final class MainClassArgument implements org.gradle.process.CommandLineArgumentProvider {
        private final File settingsFile;
        private final Provider<Map<String, String>> userProperties;

        MainClassArgument(File settingsFile, Provider<Map<String, String>> userProperties) {
            this.settingsFile = settingsFile;
            this.userProperties = userProperties;
        }

        @Override
        public Iterable<String> asArguments() {
            java.util.Properties p = new java.util.Properties();
            if (settingsFile.isFile()) {
                try (java.io.InputStream in = new java.io.FileInputStream(settingsFile)) {
                    p.load(in);
                } catch (java.io.IOException ignored) {
                    // The simulator reports a missing main class itself.
                }
            }
            for (Map.Entry<String, String> e : userProperties.get().entrySet()) {
                p.setProperty(e.getKey(), e.getValue());
            }
            String main = p.getProperty("codename1.mainName", "").trim();
            String pkg = p.getProperty("codename1.packageName", "").trim();
            return Collections.singletonList(pkg.isEmpty() ? main : pkg + "." + main);
        }
    }

}
