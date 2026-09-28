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

import java.io.File;
import java.util.*;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.invoker.*;
import org.apache.tools.ant.taskdefs.Java;

/**
 * This mojo runs tests in the Codename One Test Runner.  This goal should be used in place of
 * the surefire plugin.
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class RunTestsMojo extends AbstractCN1Mojo {
    private static final int VERSION = 1;
    
    private File getMetaDataFile() {
        return new File(project.getBuild().getTestOutputDirectory()+ File.separator + "tests.dat");
    }

    private boolean isJavaSEProject() {
        return project.getArtifactId().endsWith("-javase");
    }

    private boolean isCommonProject() {
        return project.getArtifactId().endsWith("-common");
    }

    private File getCommonProjectBaseDir() {
        if (isJavaSEProject()) {
            return new File(project.getParent().getBasedir(), "common");
        }
        if (isCommonProject()) {
            return project.getBasedir();
        }
        throw new IllegalStateException("Cannot get common project in this context");
    }

    private boolean prepareTests() throws MojoExecutionException {
        // At this point, we are running inside the common project.
        List<File> paths = new ArrayList<File>();
        paths.add(new File(project.getBuild().getOutputDirectory()));
        for (Artifact artifact : project.getArtifacts()) {
            File jar = getJar(artifact);
            if (jar != null) {
                paths.add(jar);
            } else {
                getLog().warn("Failed to resolve artifact "+artifact);
            }
        }
        try {
            return new Cn1TestRunner(MavenLog.of(getLog())).prepare(
                    new File(project.getBuild().getTestOutputDirectory()), paths);
        } catch (com.codename1.build.BuildExecutionException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex.getCause());
        }
    }

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        copyKotlinIncrementalCompileOutputToOutputDir();
        if (!prepareTests()) {
            getLog().info("No tests were found.");
            return;
        }
        List<File> cp = new ArrayList<File>();
        cp.add(new File(project.getBuild().getTestOutputDirectory()));
        cp.add(new File(project.getBuild().getOutputDirectory()));
        for (Artifact artifact : project.getArtifacts()) {
            if ("provided".equals(artifact.getScope())) {
                continue;
            }
            File jar = getJar(artifact);
            if (jar != null) {
                cp.add(jar);
            } else {
                getLog().warn("Failed to resolve artifact: "+artifact);
            }
        }
        cp.add(getProjectInternalTmpJar());
        Java java = createJava();
        getLog().debug("Executing Test Runner");
        int result = Cn1TestRunner.run(antProject, java, cp,
                properties.getProperty("codename1.packageName")+"."+properties.getProperty("codename1.mainName"),
                new File(project.getBuild().getDirectory(), "cn1-reports"));
        if (result != 0) {
            throw new MojoExecutionException("Tests failed");
        }
    }
}
