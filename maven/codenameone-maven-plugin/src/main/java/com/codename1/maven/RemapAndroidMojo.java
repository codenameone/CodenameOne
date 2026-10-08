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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/// Relocates the compiled Android code onto the compatibility runtime and
/// ships the runtime with the application; see [AndroidRemapper]. Must run in
/// `process-classes` before `bytecode-compliance`, which then checks the
/// relocated runtime along with the application. A no-op for a project that
/// does not depend on `codenameone-android-compat`.
@Mojo(name = "remap-android", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class RemapAndroidMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File jar = CompileAndroidResMojo.compatJar(project.getArtifacts());
        if (jar == null) {
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
        try {
            java.util.List<File> classpath = new java.util.ArrayList<File>();
            for (org.apache.maven.artifact.Artifact artifact : project.getArtifacts()) {
                if (artifact.getFile() != null) {
                    classpath.add(artifact.getFile());
                }
            }
            new AndroidRemapper(classes, jar, onClick, MavenLog.of(getLog()))
                    .withSupportJars(java.util.Collections.singletonList(CompatLayers.jdkJar(classpath))).run();
        } catch (com.codename1.builders.BuildException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }
}
