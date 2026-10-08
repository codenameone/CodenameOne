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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// Relocates the compiled application onto whichever compatibility layers it
/// has switched on -- Android, Swing, JavaFX, in any mix -- and ships their
/// runtimes with it; see [CompatRemapper]. Must run in `process-classes`
/// before `bytecode-compliance`, which then checks the relocated runtimes
/// along with the application. For the Swing layer that order is the only one
/// that works at all: its application code is compiled against the JDK's own
/// `javax.swing`, and nothing the check could resolve those names against
/// exists until they have been relocated.
///
/// A no-op for a project that depends on none of the layers' runtimes, so it
/// is bound unconditionally.
@Mojo(name = "remap-compat", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class RemapCompatMojo extends AbstractCN1Mojo {

    @Parameter(property = "cn1.desktop.sourceDir", defaultValue = "${project.basedir}/src/main/desktop")
    private File desktopSourceDir;

    /// Where `prepare-desktop-sources` writes the resources it generates
    /// from the desktop sources; they are flattened and indexed with the
    /// application's own.
    @Parameter(property = "cn1.desktop.resourcesOutputDir",
            defaultValue = "${project.build.directory}/generated-resources/desktop")
    private File desktopResourcesOutputDir;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        List<File> classpath = new ArrayList<File>();
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.getFile() != null) {
                classpath.add(artifact.getFile());
            }
        }
        // By the jars alone: whether a desktop layer is also used can only be
        // read from the classes, and Kotlin's are not all in place yet.
        if (CompatLayers.active(classpath).isEmpty()) {
            return;
        }
        File classes = new File(project.getBuild().getOutputDirectory());
        if (!classes.isDirectory()) {
            return;
        }
        // Freshly compiled Kotlin (incremental compilation keeps its own
        // output tree) must be in place before it is relocated.
        copyKotlinIncrementalCompileOutputToOutputDir();
        File onClick = AndroidResourceRunner.onClickNamesFile(new File(project.getBuild().getDirectory()));
        List<File> desktopResources = new ArrayList<File>();
        if (desktopSourceDir != null) {
            desktopResources.add(DesktopSources.resourcesDir(desktopSourceDir));
        }
        desktopResources.add(desktopResourcesOutputDir);
        try {
            new CompatRemapper(classes, classpath, onClick, MavenLog.of(getLog()))
                    .withResourceDirectories(desktopResources)
                    .withDesktopEntryRecord(desktopSourceDir == null ? null
                            : DesktopSources.entryRecord(desktopSourceDir))
                    .run();
        } catch (com.codename1.builders.BuildException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }
}
