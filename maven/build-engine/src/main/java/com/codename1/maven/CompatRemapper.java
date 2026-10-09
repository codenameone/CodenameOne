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
package com.codename1.maven;

import com.codename1.build.Log;
import com.codename1.builders.BuildException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Relocates an application's compiled classes onto whichever compatibility
/// layers it has switched on, and ships their runtimes with it.
///
/// The one entry point a build plugin needs: it works out the active layers
/// from the compile classpath and the compiled classes
/// ([CompatLayers#active(Iterable, Iterable)]), relocates by all of
/// their rules in a single pass ([ClassRelocator]) and extracts each runtime
/// jar, and the shared JDK classes, exactly once. An application with no
/// layer is left untouched, so the step can run unconditionally.
///
/// Only the layers that are active are applied, never [CompatLayers#EVERY]:
/// an application without the Swing layer keeps its `java/awt/` references as
/// written, and the compliance check then reports them with the instruction
/// to enable the layer.
///
/// Running it again over the same directory changes nothing. The relocation
/// rules only match names an application is compiled against, extracted
/// runtimes are skipped by the directory walk, and every extracted file is
/// compared before it is written.
public final class CompatRemapper {

    private final File classesDir;
    private final List<File> classpath;
    private final File onClickNames;
    private final Log log;
    /// Worked out on first use, from everything the caller configured by
    /// then: the handler directories are examined too.
    private List<Relocation> active;
    private ClassRelocator relocator;
    private final List<File> handlerDirs = new ArrayList<File>();
    private boolean shipRuntime = true;
    private File desktopEntry;
    private String applicationMain;
    private boolean entryGenerated;
    private List<File> applicationLibraries;
    /// Null until [#withResourceDirectories] is called; see
    /// [#resourceDirectories()].
    private List<File> resourceDirs;
    private File fxmlSources;
    /// The shared JDK classes' jar of the run in progress, and whether that
    /// run has shipped the desktop resources yet: two layers ask for it.
    private File jdkJar;
    private boolean resourcesShipped;

    /// `classesDir` is the application's output directory, rewritten in
    /// place. `classpath` is its compile classpath, which decides the active
    /// layers and supplies their jars. `onClickNames` is the Android resource
    /// compiler's list of `android:onClick` names, or null when the
    /// application has no Android sources.
    public CompatRemapper(File classesDir, Iterable<File> classpath, File onClickNames, Log log) {
        this.classesDir = classesDir;
        this.classpath = new ArrayList<File>();
        if (classpath != null) {
            for (File f : classpath) {
                if (f != null) {
                    this.classpath.add(f);
                }
            }
        }
        this.onClickNames = onClickNames;
        this.log = log;
    }

    /// Only relocates the classes, without copying in any runtime or
    /// generating anything: for a second output directory of the same
    /// application (Gradle's Kotlin classes), which the main pass covers.
    public CompatRemapper relocateOnly() {
        shipRuntime = false;
        return this;
    }

    /// Further class directories of the same application, whose classes the
    /// generators consider beside those of the classes directory.
    public CompatRemapper withHandlerDirectories(List<File> dirs) {
        if (dirs != null) {
            for (File d : dirs) {
                if (d != null && d.isDirectory() && !d.equals(classesDir)) {
                    handlerDirs.add(d);
                }
            }
        }
        return this;
    }

    /// The record of the desktop application's entry point
    /// ([DesktopSources#entryRecord]), which the Swing and JavaFX entry point
    /// generators read; null, or a file that does not exist, for an
    /// application that has none.
    public CompatRemapper withDesktopEntryRecord(File record) {
        this.desktopEntry = record;
        return this;
    }

    /// The project's main class, as `codename1.packageName` and
    /// `codename1.mainName` name it (`com.example.MyApp`): the class the
    /// entry point of a desktop application is generated as. Without it the
    /// class is generated only for an application with an entry record, as
    /// [DesktopEntryPoints#DEFAULT_MAIN].
    public CompatRemapper withApplicationMain(String className) {
        this.applicationMain = className;
        return this;
    }

    /// The directories holding the files a desktop application loads by
    /// name (`src/main/desktop/resources`, and wherever the build generates
    /// more of them). The files in a directory are flattened to the root of
    /// the classes directory and indexed for the runtime; see
    /// [CompatResources]. A directory that does not exist is ignored.
    ///
    /// A caller that never calls this gets the `resources` directory beside
    /// the desktop entry record, which is where a desktop application's
    /// resources are unless the build was told otherwise.
    public CompatRemapper withResourceDirectories(List<File> dirs) {
        resourceDirs = new ArrayList<File>();
        if (dirs != null) {
            for (File d : dirs) {
                if (d != null && !resourceDirs.contains(d)) {
                    resourceDirs.add(d);
                }
            }
        }
        return this;
    }

    /// The directories [#withResourceDirectories] was given, or its default.
    List<File> resourceDirectories() {
        if (resourceDirs != null) {
            return resourceDirs;
        }
        List<File> out = new ArrayList<File>();
        if (desktopEntry != null && desktopEntry.getAbsoluteFile().getParentFile() != null) {
            out.add(DesktopSources.resourcesDir(desktopEntry.getAbsoluteFile().getParentFile()));
        }
        return out;
    }

    /// The file given to [#withDesktopEntryRecord], or null.
    File desktopEntryRecord() {
        return desktopEntry;
    }

    /// The layers this application has switched on, in the order their rules
    /// are tried; empty when it has none. A desktop layer counts only when the
    /// classes directory, or one of the handler directories, refers to it.
    public List<Relocation> activeLayers() throws BuildException {
        if (active == null) {
            bundleLibraries();
            List<File> dirs = new ArrayList<File>();
            dirs.add(classesDir);
            dirs.addAll(handlerDirs);
            try {
                active = Collections.unmodifiableList(CompatLayers.active(classpath, dirs));
            } catch (IOException e) {
                throw new BuildException("Could not read the compiled classes: " + e.getMessage(), e);
            }
            relocator = new ClassRelocator(active);
            if (relocator.hasDesktopLayer()) {
                // Generated by shipDesktopResources, not copied from the jar.
                relocator.generating(CompatResources.REGISTRY);
                // Generated by generateFxmlDispatch; the layer's own is an
                // empty placeholder. Named as the runtime jar holds it, and
                // harmless when no active layer ships it.
                relocator.generating(com.codename1.fxml.FxmlDispatchGenerator.REGISTRY);
                // Generated by generatePropertyAccess, over a placeholder
                // in the same way.
                relocator.generating(PropertyAccessGenerator.REGISTRY);
            }
        }
        return active;
    }

    /// The dependency jars that are part of the application -- Maven's
    /// `compile` scope, Gradle's `implementation` -- as opposed to the ones
    /// something else provides. What the application uses of them is
    /// unpacked into the classes directory and relocated with it: a library
    /// written against a desktop layer whole, a pure-Java one by the classes
    /// the application reaches. See [CompatLibraries], which classifies each
    /// jar itself, so the whole list can be passed as it is.
    public CompatRemapper withApplicationLibraries(List<File> jars) {
        this.applicationLibraries = jars == null ? null : new ArrayList<File>(jars);
        return this;
    }

    /// Unpacks the libraries written against a desktop layer, before the
    /// classes directory is read for the layers it uses: a library's use of
    /// Swing is the application's.
    private void bundleLibraries() throws BuildException {
        // An empty list is still a list: the last library an application
        // dropped has classes here to take away.
        if (!shipRuntime || applicationLibraries == null) {
            return;
        }
        List<Relocation> desktop = new ArrayList<Relocation>();
        java.util.Set<File> runtimes = new java.util.HashSet<File>();
        for (Relocation layer : CompatLayers.active(classpath)) {
            runtimes.add(CompatLayers.runtimeJar(layer, classpath));
            if (layer.isDesktop()) {
                desktop.add(layer);
            }
        }
        runtimes.add(CompatLayers.jdkJar(classpath));
        try {
            CompatLibraries.bundle(classesDir, applicationLibraries, desktop, runtimes, log);
        } catch (IOException e) {
            throw new BuildException("Could not bundle the application's libraries: " + e.getMessage(), e);
        }
    }

    /// Whether `layer` is among the active ones.
    public boolean isActive(Relocation layer) throws BuildException {
        return activeLayers().contains(layer);
    }

    /// The relocator composed of every active layer's rules.
    public ClassRelocator relocator() throws BuildException {
        activeLayers();
        return relocator;
    }

    /// Where the Java sources generated from the application's FXML
    /// documents are kept for a reader (a stack trace names their lines).
    /// Never a directory the application's own javac run compiles from.
    /// Without one the sources are compiled from memory and kept nowhere.
    public CompatRemapper withFxmlSourceDirectory(File dir) {
        this.fxmlSources = dir;
        return this;
    }

    /// Compiles the application's FXML documents into its classes directory:
    /// [com.codename1.fxml.FxmlClassCompiler], which says why this is the
    /// one moment for it -- after javac, so that a document can name a class
    /// of the application (a custom control), and before anything here
    /// relocates the classes its generated source is compiled against.
    ///
    /// Called first by a full [#run()], for an application with the JavaFX
    /// layer's jar on its class path. A relocate-only run never compiles
    /// documents: it is the second directory of an application whose main
    /// pass did.
    private void compileFxmlDocuments() throws BuildException {
        if (CompatLayers.runtimeJar(CompatLayers.JAVAFX, classpath) == null) {
            return;
        }
        com.codename1.fxml.FxmlClassCompiler compiler = new com.codename1.fxml.FxmlClassCompiler(
                resourceDirectories(), classpath, handlerDirs, classesDir, fxmlSources,
                new com.codename1.fxml.DesktopResourceCompiler.Log() {
                    @Override
                    public void info(String message) {
                        log.info(message);
                    }

                    @Override
                    public void warn(String message) {
                        log.warn(message);
                    }
                });
        List<String> errors;
        try {
            errors = compiler.run();
        } catch (IOException e) {
            throw new BuildException("Could not compile the application's FXML documents: " + e.getMessage(), e);
        }
        if (!errors.isEmpty()) {
            StringBuilder all = new StringBuilder();
            for (String error : errors) {
                log.error(error);
                all.append('\n').append(error);
            }
            throw new BuildException(errors.size() + " error(s) in the application's FXML documents:" + all);
        }
    }

    /// Relocates and ships. Answers false, having done nothing, when the
    /// application has no compatibility layer.
    public boolean run() throws BuildException {
        if (shipRuntime) {
            // Before the layers are worked out: that unpacks the libraries
            // into the classes directory, and what follows relocates it.
            compileFxmlDocuments();
        }
        if (activeLayers().isEmpty()) {
            log.debug("No compatibility layer on the classpath; nothing to relocate");
            return false;
        }
        try {
            jdkJar = CompatLayers.jdkJar(classpath);
            resourcesShipped = false;
            entryGenerated = false;
            List<String> appClasses;
            int runtime = 0;
            boolean android = isActive(AndroidRemapper.RELOCATION);
            if (android) {
                // The Android step relocates the directory itself, with the
                // composed rules, and extracts its own runtime and the JDK
                // classes before generating its dispatchers.
                AndroidRemapper remapper = new AndroidRemapper(classesDir,
                        CompatLayers.runtimeJar(AndroidRemapper.RELOCATION, classpath), onClickNames, log)
                        .withRelocator(relocator)
                        .withHandlerDirectories(handlerDirs)
                        .withSupportJars(Collections.singletonList(jdkJar));
                if (!shipRuntime) {
                    remapper.relocateOnly();
                }
                remapper.run();
                appClasses = remapper.applicationClasses();
            } else {
                appClasses = new ArrayList<String>();
                relocator.remapDirectory(classesDir, appClasses, log);
                if (shipRuntime && jdkJar != null) {
                    runtime += relocator.extractRuntime(jdkJar, classesDir);
                }
            }
            if (!shipRuntime) {
                if (!android) {
                    log.info("Relocated " + appClasses.size() + " application classes");
                }
                return true;
            }
            StringBuilder names = new StringBuilder();
            for (Relocation layer : active) {
                if (layer == AndroidRemapper.RELOCATION) {
                    continue;
                }
                runtime += relocator.extractRuntime(CompatLayers.runtimeJar(layer, classpath), classesDir);
                names.append(names.length() == 0 ? "" : ", ").append(layer.name());
            }
            List<String> unmodifiable = Collections.unmodifiableList(appClasses);
            if (isActive(CompatLayers.SWING)) {
                generateSwingEntryPoint(unmodifiable);
                generateSwingResources(unmodifiable);
            }
            if (isActive(CompatLayers.JAVAFX)) {
                generateJavaFxEntryPoint(unmodifiable);
                generateFxmlDispatch(unmodifiable);
                generatePropertyAccess(unmodifiable);
                generateJavaFxResources(unmodifiable);
            }
            if (names.length() > 0) {
                log.info("Relocated " + appClasses.size() + " application classes and " + runtime + " "
                        + names + " runtime classes");
            }
            return true;
        } catch (IOException e) {
            throw new BuildException("Compatibility layer remapping failed: " + e.getMessage(), e);
        }
    }

    /// Generates the class that starts a Swing application -- the Codename
    /// One lifecycle class that calls the application's `main`, since nothing
    /// on a device runs a `main` method: [DesktopEntryPoints]. `appClasses`
    /// are the application's relocated internal names. Called after every
    /// runtime is in place, on a full (not relocate-only) run with the Swing
    /// layer active. Whatever it writes must be written with
    /// [ClassRelocator#writeIfDifferent], to keep a second run a no-op.
    void generateSwingEntryPoint(List<String> appClasses) throws IOException, BuildException {
        generateDesktopEntryPoint(appClasses);
    }

    /// The one main class of a desktop application, generated once per run
    /// however many desktop layers are active: which of the two kinds it is
    /// comes from the entry record, not from the layer that asked.
    private void generateDesktopEntryPoint(List<String> appClasses) throws IOException, BuildException {
        if (entryGenerated) {
            return;
        }
        entryGenerated = true;
        new DesktopEntryPoints(classesDir, handlerDirs, relocator, active, jdkJar != null, log)
                .generate(DesktopEntryPoints.read(desktopEntry), applicationMain, appClasses);
    }

    /// Ships what a Swing application loads by name -- `Class.getResource`
    /// images, `ResourceBundle` properties -- in the form the runtime looks
    /// them up: [#shipDesktopResources()]. Same contract as
    /// [#generateSwingEntryPoint].
    void generateSwingResources(List<String> appClasses) throws IOException {
        shipDesktopResources();
    }

    /// Flattens the desktop resources and generates the registry the runtime
    /// reads them through ([CompatResources]), once per run however many
    /// desktop layers are active: the resources are the application's, not a
    /// layer's.
    ///
    /// #### What starts it on the device
    ///
    /// The generated class is `com.codename1.compat.jdk.CompatRegistry`, and
    /// `com.codename1.compat.jdk.CompatBoot.cn1Init()` installs it. The
    /// resource and bundle lookups call that themselves on first use, so
    /// nothing is required of an entry point; one that wants the cost paid
    /// at startup calls `CompatBoot.cn1Init()` before the application's own
    /// code.
    private void shipDesktopResources() throws IOException {
        if (resourcesShipped || jdkJar == null) {
            // Without the shared JDK classes there is no runtime to read
            // an index, and the build fails on their absence elsewhere.
            return;
        }
        resourcesShipped = true;
        try {
            new CompatResources(classesDir, resourceDirectories(), handlerDirs, relocator, log).run();
        } catch (BuildException e) {
            // The hooks' contract is IOException; run() reports either as
            // the build failure it is, with this message.
            throw new IOException(e.getMessage(), e);
        }
    }

    /// Generates the class that starts a JavaFX application, instantiating
    /// its `javafx.application.Application` subclass with `new` rather than
    /// by reflection: [DesktopEntryPoints]. Same contract as
    /// [#generateSwingEntryPoint], for the JavaFX layer.
    void generateJavaFxEntryPoint(List<String> appClasses) throws IOException, BuildException {
        generateDesktopEntryPoint(appClasses);
    }

    /// Generates what FXML resolves by name at run time on a desktop --
    /// the compiled documents, controller classes, `fx:id` fields and
    /// `onAction="#handler"` methods -- as direct calls, the way
    /// [AndroidRemapper] generates the `android:onClick` dispatcher:
    /// [com.codename1.fxml.FxmlDispatchGenerator]. It replaces the layer's
    /// placeholder registry, which is why [#activeLayers()] keeps that
    /// class from being copied out of the runtime jar. Same contract as
    /// [#generateSwingEntryPoint], for the JavaFX layer.
    void generateFxmlDispatch(List<String> appClasses) throws IOException {
        com.codename1.fxml.FxmlDispatchGenerator generator = new com.codename1.fxml.FxmlDispatchGenerator(
                classesDir, new com.codename1.fxml.FxmlDispatchGenerator.Names(
                        relocator.map(com.codename1.fxml.FxmlDispatchGenerator.REGISTRY),
                        relocator.map(com.codename1.fxml.FxmlDispatchGenerator.DISPATCH),
                        relocator.map(com.codename1.fxml.FxmlDispatchGenerator.CONTEXT),
                        relocator.map(com.codename1.fxml.FxmlDispatchGenerator.ANNOTATION)));
        generator.run(appClasses);
        if (generator.documents() > 0 || generator.controllers() > 0) {
            log.info("Generated the FXML dispatcher for " + generator.documents() + " document(s) and "
                    + generator.controllers() + " controller class(es)");
        }
    }

    /// Generates what `PropertyValueFactory` resolves by name at run time
    /// on a desktop -- the `xxxProperty()` and getter methods of the bean
    /// classes -- as direct calls: [PropertyAccessGenerator]. It replaces
    /// the layer's placeholder registry, which [#activeLayers()] keeps from
    /// being copied out of the runtime jar. Same contract as
    /// [#generateSwingEntryPoint], for the JavaFX layer.
    void generatePropertyAccess(List<String> appClasses) throws IOException {
        PropertyAccessGenerator generator = new PropertyAccessGenerator(classesDir,
                relocator.map(PropertyAccessGenerator.REGISTRY), relocator.map(PropertyAccessGenerator.ACCESS),
                relocator.map(PropertyAccessGenerator.FACTORY));
        generator.run(appClasses);
        if (generator.beans() > 0) {
            log.info("Generated the property accessors of " + generator.beans() + " bean class(es)");
        }
    }

    /// Ships the FXML documents, stylesheets and images a JavaFX application
    /// loads by name: [#shipDesktopResources()]. Same contract as
    /// [#generateSwingEntryPoint], for the JavaFX layer.
    void generateJavaFxResources(List<String> appClasses) throws IOException {
        shipDesktopResources();
    }
}
