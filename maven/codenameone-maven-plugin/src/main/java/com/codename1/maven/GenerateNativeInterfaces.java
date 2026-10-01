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


import com.codename1.project.BuildSystem;
import com.codename1.project.ProjectKind;
import com.codename1.project.ProjectLayouts;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Generates native interface stubs for all native interfaces in the app. Will not overwrite existing stubs.
 *
 * <p>The scanning and generation live in {@link NativeInterfaces}, shared with the Gradle plugin; this goal
 * points it at the Maven platform modules ({@code <root>/<platform>/src/main/<lang>}).</p>
 */
@Mojo(name="generate-native-interfaces")
@Execute(phase= LifecyclePhase.COMPILE)
public class GenerateNativeInterfaces extends AbstractCN1Mojo {
    @Parameter(property = "cn1.generateNativeInterfaces.swift", defaultValue = "false")
    private boolean generateIosSwift;

    @Parameter(property = "cn1.generateNativeInterfaces.kotlin", defaultValue = "false")
    private boolean generateAndroidKotlin;

    @Parameter(property = "cn1.generateNativeInterfaces.overwrite", defaultValue = "false")
    private boolean overwrite;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            // This should only be run in the CN1 project directory.
            getLog().debug("generate-native-interfaces skipped in directory "+project.getBasedir()+" because itis not a codeame one project directory");
            return;
        }
        File classes = new File(project.getBuild().getOutputDirectory());
        List<String> classpath;
        try {
            classpath = project.getCompileClasspathElements();
        } catch (Exception ex) {
            classpath = Collections.emptyList();
        }
        File root;
        try {
            root = project.getBasedir().getCanonicalFile().getParentFile();
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to resolve the project root", ex);
        }
        try {
            new NativeInterfaces(MavenLog.of(getLog()), classes, classpath).generate(
                    ProjectLayouts.of(BuildSystem.MAVEN, ProjectKind.APP, root, project.getBasedir()),
                    null, generateIosSwift, generateAndroidKotlin, overwrite,
                    "mvn cn1:generate-native-interfaces -Dcn1.generateNativeInterfaces.overwrite=true");
        } catch (com.codename1.build.BuildFailureException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        } catch (com.codename1.build.BuildExecutionException ex) {
            throw new MojoExecutionException("Failed to generate native interfaces", ex);
        }
    }
}
