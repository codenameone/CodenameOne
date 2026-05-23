/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.maven.buildWrappers;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.util.Collections;
import java.util.Properties;

/**
 * Builds an iOS app instrumented for on-device debugging and submits it to
 * the cloud build server. The user pairs this with {@code mvn
 * cn1:ios-on-device-debugging} (which launches the desktop proxy) and then
 * attaches jdb / IntelliJ / VS Code over JDWP — see
 * {@code docs/developer-guide/On-Device-Debugging.adoc}.
 *
 * Forces {@code codename1.arg.ios.onDeviceDebug=true} so the listener
 * thread is linked into the binary and the Info.plist gets the proxy
 * connection settings, regardless of what the project's
 * codenameone_settings.properties contains.
 */
@Mojo(name = "buildIosOnDeviceDebug",
        requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildIosOnDeviceDebugMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!project.isExecutionRoot()) {
            getLog().info("Skipping execution for non-root project");
            return;
        }
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("pom.xml"));
        request.setGoals(Collections.singletonList("package"));

        Properties properties = new Properties();
        properties.setProperty("skipTests", "true");
        properties.setProperty("codename1.platform", "ios");
        properties.setProperty("codename1.buildTarget", "ios-on-device-debug");
        // Force-on regardless of codenameone_settings.properties so this
        // goal is self-contained from the IDE menu.
        properties.setProperty("codename1.arg.ios.onDeviceDebug", "true");
        request.setProperties(properties);

        Invoker invoker = new DefaultInvoker();
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoFailureException("Failed to build project with exit code " + result.getExitCode());
            }
        } catch (MavenInvocationException e) {
            throw new MojoExecutionException("Failed to invoke Maven", e);
        }
    }
}
