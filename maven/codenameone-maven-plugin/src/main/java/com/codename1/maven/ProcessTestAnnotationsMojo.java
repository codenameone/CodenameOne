/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.maven.processors.BackendTests;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// PROCESS_TEST_CLASSES: makes a backend module's `@BackendTest` classes runnable.
///
/// The test counterpart of `process-annotations`. For each `@BackendTest` class it
/// generates the context and the test wiring the `com.codename1.backend.test`
/// runtime starts, and rewrites the test classes so their injected fields are set
/// without reflection -- see `BackendTestGenerator`. A module with no
/// `@BackendTest` is left untouched.
@Mojo(name = "process-test-annotations",
      defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
      requiresDependencyResolution = ResolutionScope.TEST,
      threadSafe = true)
public class ProcessTestAnnotationsMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.testOutputDirectory}", required = true)
    private File testOutputDirectory;

    @Parameter(property = "maven.test.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip || !testOutputDirectory.isDirectory()) {
            return;
        }
        List<String> roots = new ArrayList<String>();
        roots.addAll(project.getCompileSourceRoots());
        roots.addAll(project.getTestCompileSourceRoots());
        List<String> classpath;
        try {
            classpath = project.getTestClasspathElements();
        } catch (DependencyResolutionRequiredException err) {
            throw new MojoExecutionException("The test classpath is unresolved: " + err.getMessage(),
                    err);
        }
        String encoding = project.getProperties().getProperty("project.build.sourceEncoding",
                "UTF-8");
        int generated = BackendTests.process(outputDirectory, testOutputDirectory,
                new File(project.getBuild().getDirectory(), "generated-test-sources/cn1"),
                project.getBasedir(), roots, encoding, classpath, false, getLog());
        if (generated > 0) {
            getLog().info("cn1: prepared " + generated + " backend test class(es)");
        }
    }
}
