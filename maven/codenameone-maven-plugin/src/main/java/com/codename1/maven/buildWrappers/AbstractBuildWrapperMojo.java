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
package com.codename1.maven.buildWrappers;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Base class for build wrapper Mojos.
 *
 * Wrapper mojos were created to make it easier to build Codename One targets
 * directly in IDEs like IntelliJ without having to provide any properties or profiles.
 *
 * The user will see "buildAndroid", "buildIos" and "buildIosXcodeProject" goals, etc..
 * listed in the Maven sidebar, and they can just double-click them to invoke them
 * directly.
 */
public abstract class AbstractBuildWrapperMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /// Forwarded to the child build, which otherwise never sees it: without this,
    /// `mvn cn1:buildIosXcodeProject -Dopen=false` still opened Xcode. Unset, the
    /// child decides (it skips opening under an agent or CI).
    @Parameter(property = "open")
    private Boolean open;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!project.isExecutionRoot()) {
            getLog().info("Skipping execution for non-root project");
            return;
        }
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("pom.xml"));
        request.setGoals(getGoals());

        Properties properties = new Properties();
        properties.setProperty("skipTests", "true");
        properties.setProperty("codename1.platform", getPlatform());
        if (getBuildTarget() != null) {
            properties.setProperty("codename1.buildTarget", getBuildTarget());
        }
        if (open != null) {
            properties.setProperty("open", open.toString());
        }
        // The child build is a separate Maven run and sees none of this one's -D
        // options, so the ones a build reads are passed on: cn1.* (cn1.board, the
        // device options) and codename1.arg.* (build hints given on the command line).
        for (String key : System.getProperties().stringPropertyNames()) {
            if ((key.startsWith("cn1.") || key.startsWith("codename1.arg.")) && !properties.containsKey(key)) {
                properties.setProperty(key, System.getProperty(key));
            }
        }
        request.setProperties(properties);

        if (buildExecutableJar()) {
            request.setProfiles(Collections.singletonList("executable-jar"));
        }

        Invoker invoker = new DefaultInvoker();

        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoFailureException("Failed to build project with exit code " + result.getExitCode());
            }
            afterBuild();
        } catch (MavenInvocationException e) {
            throw new MojoExecutionException("Failed to invoke Maven", e);
        }
    }

    /// Runs in this Maven run once the child build has succeeded.
    protected void afterBuild() throws MojoExecutionException {
    }

    /// The project this goal was started in.
    protected MavenProject getProject() {
        return project;
    }

    protected abstract String getPlatform();
    protected abstract String getBuildTarget();

    protected List<String> getGoals() {
        return Collections.singletonList("package");
    }

    protected boolean buildExecutableJar() {
        return false;
    }

}
