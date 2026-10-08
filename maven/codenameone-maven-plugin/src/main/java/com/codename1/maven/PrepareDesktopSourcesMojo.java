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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// Brings the desktop sources of an application module into its build:
/// `src/main/desktop`, holding a Swing or JavaFX application as its own
/// project laid it out (see [DesktopSources]).
///
/// Adds `src/main/desktop/java` as a compile source root and
/// `src/main/desktop/resources` as a resource root, so the sources compile
/// unchanged -- Swing code against the JDK's own `java.awt` and `javax.swing`,
/// JavaFX code against `codenameone-javafx-compat` -- and the resources reach
/// the classes directory under the names the code loads them by. A silent
/// no-op for a project without `src/main/desktop`. See `remap-compat` for the
/// step that follows compilation.
@Mojo(name = "prepare-desktop-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class PrepareDesktopSourcesMojo extends AbstractCN1Mojo {

    @Parameter(property = "cn1.desktop.sourceDir", defaultValue = "${project.basedir}/src/main/desktop")
    private File desktopSourceDir;

    /// Where a build-time compile step writes Java sources; a compile source
    /// root once it exists.
    @Parameter(property = "cn1.desktop.outputDir", defaultValue = "${project.build.directory}/generated-sources/desktop")
    private File desktopOutputDir;

    /// Where a build-time compile step writes classpath resources; a resource
    /// root once it exists.
    @Parameter(property = "cn1.desktop.resourcesOutputDir",
            defaultValue = "${project.build.directory}/generated-resources/desktop")
    private File desktopResourcesOutputDir;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!DesktopSources.isDesktopProject(desktopSourceDir)) {
            return;
        }
        List<String> artifactIds = new ArrayList<String>();
        List<File> classpath = new ArrayList<File>();
        for (Artifact a : project.getArtifacts()) {
            artifactIds.add(a.getArtifactId());
            if (a.getFile() != null) {
                classpath.add(a.getFile());
            }
        }
        try {
            DesktopSources.requireRuntime(desktopSourceDir, artifactIds);
        } catch (com.codename1.builders.BuildException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
        // The last build relocated the classes in the output directory, and
        // javac compiles against that directory: a Kotlin class the
        // incremental compiler does not recompile would be seen relocated, so
        // the Java code calling it would not compile. Its unrelocated original
        // is in Kotlin's own output tree; put it back before anything compiles.
        copyKotlinIncrementalCompileOutputToOutputDir(true);
        File javaDir = DesktopSources.javaDir(desktopSourceDir);
        if (javaDir.isDirectory()) {
            registerSourceRoot(javaDir);
        }
        File resourcesDir = DesktopSources.resourcesDir(desktopSourceDir);
        if (resourcesDir.isDirectory()) {
            registerResourceRoot(resourcesDir);
        }
        compileDesktopResources(desktopSourceDir, resourcesDir, classpath, desktopOutputDir, desktopResourcesOutputDir);
        if (desktopOutputDir.isDirectory()) {
            registerSourceRoot(desktopOutputDir);
        }
        if (desktopResourcesOutputDir.isDirectory()) {
            registerResourceRoot(desktopResourcesOutputDir);
        }
    }

    /// EXTENSION POINT, deliberately empty: the build-time FXML and CSS
    /// compile step goes here.
    ///
    /// A device has no XML parser to spare and no reflection, so what a
    /// desktop JavaFX runtime does when it loads an `.fxml` document or a
    /// stylesheet -- parse it, look the controller class and its `fx:id`
    /// fields and `#handler` methods up by name -- has to happen while
    /// building, the way `compile-android-res` turns Android's XML into
    /// generated classes and a binary table. The compiler itself lives in its
    /// own module; this method is the one place the Maven build calls it.
    ///
    /// Called once per build, in `generate-sources`, only for a project with
    /// desktop sources, after its source and resource roots are registered
    /// and before anything compiles.
    ///
    /// - `desktopDir`: `src/main/desktop`.
    /// - `resourcesDir`: `src/main/desktop/resources`, the documents to
    ///   compile (`**/*.fxml`, `**/*.css`); it may not exist.
    /// - `classpath`: the module's resolved compile classpath, which holds
    ///   `codenameone-javafx-compat` when the JavaFX layer is available.
    /// - `javaOut`: where to write generated Java sources. Whatever is
    ///   written there is compiled with the application: the directory is
    ///   registered as a source root right after this returns, if it exists.
    /// - `resourcesOut`: where to write generated classpath resources (a
    ///   binary form of the documents); registered as a resource root the
    ///   same way.
    ///
    /// Generated sources are compiled before the application is relocated, so
    /// they name the JavaFX API as an application does (`javafx.scene...`),
    /// and `remap-compat` relocates them with everything else. An
    /// implementation should leave both directories untouched when its inputs
    /// have not changed, as `compile-android-res` does, so an unchanged build
    /// recompiles nothing.
    ///
    /// The Gradle build needs the same step as a task with declared inputs
    /// and outputs; see the matching note in the Gradle plugin's `AppSupport`.
    protected void compileDesktopResources(File desktopDir, File resourcesDir, List<File> classpath, File javaOut,
                                           File resourcesOut) throws MojoExecutionException, MojoFailureException {
        getLog().debug("No build-time FXML/CSS compile step is installed; " + resourcesDir + " is shipped as it is");
    }

    private void registerResourceRoot(File dir) {
        String path = dir.getAbsolutePath();
        for (Resource r : project.getResources()) {
            if (r.getDirectory() != null && new File(r.getDirectory()).getAbsolutePath().equals(path)) {
                return;
            }
        }
        Resource r = new Resource();
        r.setDirectory(path);
        project.addResource(r);
        getLog().debug("Added resource root " + path);
    }
}
