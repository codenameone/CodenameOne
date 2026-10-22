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
/// jar exactly once. An application with no
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
    private File desktopDir;
    /// Null until [#withResourceDirectories] is called; see
    /// [#resourceDirectories()].
    private List<File> resourceDirs;
    private File fxmlSources;
    /// Whether the run in progress has shipped the desktop resources yet:
    /// two layers ask for it.
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

    /// The module's desktop source directory (`src/main/desktop`, see
    /// [DesktopSources]); null, or a directory that does not exist, for an
    /// application that has none. Only its `resources` directory is read,
    /// and only when [#withResourceDirectories] is not called.
    ///
    /// Nothing here starts the application: its Codename One main class is a
    /// source file of the project, which `cn1:import-desktop-project` writes
    /// ([DesktopProjectImporter]), and the remap relocates it like any other
    /// class.
    public CompatRemapper withDesktopSourceDirectory(File dir) {
        this.desktopDir = dir;
        return this;
    }

    /// The directories holding the files a desktop application loads by
    /// name (`src/main/desktop/resources`, and wherever the build generates
    /// more of them). The files in a directory are flattened to the root of
    /// the classes directory and indexed for the runtime; see
    /// [CompatResources]. A directory that does not exist is ignored.
    ///
    /// A caller that never calls this gets the `resources` directory of the
    /// desktop source directory ([#withDesktopSourceDirectory]), which is
    /// where a desktop application's resources are unless the build was told
    /// otherwise.
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
        if (desktopDir != null) {
            out.add(DesktopSources.resourcesDir(desktopDir.getAbsoluteFile()));
        }
        return out;
    }

    /// The directory given to [#withDesktopSourceDirectory], or null.
    File desktopSourceDirectory() {
        return desktopDir;
    }

    /// The layers this application has switched on, in the order their rules
    /// are tried; empty when it has none. A desktop layer counts only when the
    /// classes directory, or one of the handler directories, refers to it.
    public List<Relocation> activeLayers() throws BuildException {
        if (active == null) {
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
                // Generated by shipDesktopResources; each desktop layer's jar
                // holds an empty placeholder for its lifecycle to compile
                // against, never copied.
                relocator.generating(ApplicationRegistry.NAME);
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
            // Before the layers are worked out: the generated documents are
            // application classes, and what follows relocates them.
            compileFxmlDocuments();
        }
        if (activeLayers().isEmpty()) {
            log.debug("No compatibility layer on the classpath; nothing to relocate");
            return false;
        }
        try {
            resourcesShipped = false;
            List<String> appClasses;
            int runtime = 0;
            boolean android = isActive(AndroidRemapper.RELOCATION);
            if (android) {
                // The Android step relocates the directory itself, with the
                // composed rules, and extracts its own runtime before
                // generating its dispatchers.
                AndroidRemapper remapper = new AndroidRemapper(classesDir,
                        CompatLayers.runtimeJar(AndroidRemapper.RELOCATION, classpath), onClickNames, log)
                        .withRelocator(relocator)
                        .withHandlerDirectories(handlerDirs);
                if (!shipRuntime) {
                    remapper.relocateOnly();
                }
                remapper.run();
                appClasses = remapper.applicationClasses();
            } else {
                appClasses = new ArrayList<String>();
                relocator.remapDirectory(classesDir, appClasses, log);
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
            if (relocator.hasDesktopLayer()) {
                runtime += relocator.extractRuntime(desktopRuntimeJar(), classesDir);
            }
            // The registry an earlier run generated is not the application's:
            // read as one, its class literals would change what this run
            // generates from them, and a second run would not be a no-op.
            ApplicationRegistry.removeGenerated(appClasses);
            List<String> unmodifiable = Collections.unmodifiableList(appClasses);
            if (isActive(CompatLayers.SWING)) {
                generateSwingResources(unmodifiable);
            }
            if (isActive(CompatLayers.JAVAFX)) {
                generateFxmlDispatch(unmodifiable);
                generatePropertyAccess(unmodifiable);
                generateJavaFxResources(unmodifiable);
            }
            if (relocator.hasDesktopLayer()) {
                // A desktop application hands class literals to
                // getListeners and Array.newInstance; see the generator
                // for why that needs a class of its own on a device.
                int arrays = new ArrayClassGenerator(classesDir).run(unmodifiable);
                log.debug("Generated the array classes of " + arrays + " class literal(s)");
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

    /// The runtime both desktop layers share
    /// ([CompatLayers#DESKTOP_RUNTIME]): a dependency of each layer's jar, so
    /// it is on the class path of any application that has one. A class path
    /// that names no copy -- a layer jar declared without its dependencies --
    /// gets the build's own, which this module depends on and which is
    /// therefore of the same version.
    private File desktopRuntimeJar() throws BuildException {
        File jar = null;
        for (File f : classpath) {
            String n = f.getName();
            if (n.startsWith(CompatLayers.DESKTOP_RUNTIME + "-") && n.endsWith(".jar")) {
                jar = f;
            }
        }
        if (jar == null) {
            jar = ownDesktopRuntime();
        }
        if (jar == null) {
            throw new BuildException(CompatLayers.DESKTOP_RUNTIME + " is not on the class path. It is a "
                    + "dependency of the desktop compatibility layers; declare it beside them.");
        }
        return jar;
    }

    private static File ownDesktopRuntime() {
        try {
            java.security.CodeSource source = com.codename1.desktopcompat.shared.ResourceNames.class
                    .getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return null;
            }
            File f = new File(source.getLocation().toURI());
            // Only the module's own jar: a build that bundled it into a
            // larger one would extract everything else with it.
            return f.isFile() && f.getName().startsWith(CompatLayers.DESKTOP_RUNTIME + "-") ? f : null;
        } catch (java.net.URISyntaxException e) {
            return null;
        } catch (SecurityException e) {
            return null;
        }
    }

    /// Ships what a Swing application loads by name -- `Class.getResource`
    /// images, `ResourceBundle` properties -- in the form the runtime looks
    /// them up: [#shipDesktopResources()]. `appClasses` are the application's
    /// relocated internal names. Called after every runtime is in place, on
    /// a full (not relocate-only) run with the Swing layer active. Whatever
    /// it writes must be written with [ClassRelocator#writeIfDifferent], to
    /// keep a second run a no-op.
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
    /// The generated class is `com.codename1.generated.compat.CompatRegistry`
    /// ([ApplicationRegistry]), and its static `cn1Install()` installs it.
    /// The desktop lifecycles (`DesktopLifecycle`, `FxLifecycle`) call that
    /// before the application's own code, and library mode calls it the
    /// first time a desktop component is wrapped or a window shown.
    private void shipDesktopResources() throws IOException {
        if (resourcesShipped) {
            return;
        }
        resourcesShipped = true;
        try {
            new CompatResources(classesDir, resourceDirectories(), handlerDirs, log).run();
        } catch (BuildException e) {
            // The hooks' contract is IOException; run() reports either as
            // the build failure it is, with this message.
            throw new IOException(e.getMessage(), e);
        }
    }

    /// Generates what FXML resolves by name at run time on a desktop --
    /// the compiled documents, controller classes, `fx:id` fields and
    /// `onAction="#handler"` methods -- as direct calls, the way
    /// [AndroidRemapper] generates the `android:onClick` dispatcher:
    /// [com.codename1.fxml.FxmlDispatchGenerator]. It replaces the layer's
    /// placeholder registry, which is why [#activeLayers()] keeps that
    /// class from being copied out of the runtime jar. Same contract as
    /// [#generateSwingResources], for the JavaFX layer.
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
    /// [#generateSwingResources], for the JavaFX layer.
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
    /// [#generateSwingResources], for the JavaFX layer.
    void generateJavaFxResources(List<String> appClasses) throws IOException {
        shipDesktopResources();
    }
}
