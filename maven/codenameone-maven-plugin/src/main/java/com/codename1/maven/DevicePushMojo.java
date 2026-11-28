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
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// Sends the project's compiled program to the Codename One Device Runtime app
/// on a phone or tablet, where it starts at once -- no device build, no
/// signing, no cable.
///
/// ```
/// mvn compile cn1:push                     # device on USB / the simulator
/// mvn compile cn1:push -Dcn1.push.lan      # device on the same Wi-Fi
/// mvn compile cn1:push -Dcn1.push.device=192.168.1.50
/// ```
///
/// The first push to a device prints a six-digit code to type on it; after
/// that the computer is remembered. The program's source travels with it,
/// because the app runs nothing whose source it cannot show.
///
/// Runs in the module that holds the application's sources (`common`); the
/// others skip it. The push tool lives in the `codenameone-parparvm` artifact,
/// which is resolved at the plugin's own version when the goal runs rather
/// than declared as a dependency, so a project that never pushes downloads
/// nothing for it.
@Mojo(name = "push", requiresDependencyResolution = ResolutionScope.COMPILE,
        defaultPhase = LifecyclePhase.NONE)
public class DevicePushMojo extends AbstractCN1Mojo {
    static final String TOOL_ARTIFACT = "codenameone-parparvm";
    static final String TOOL_CLASS = "com.codename1.tools.translator.DevicePush";

    /// The device is on the network rather than reached through loopback
    /// (USB forwarding or the iOS simulator).
    @Parameter(property = "cn1.push.lan", defaultValue = "false")
    private boolean lan;

    /// Connects straight to this address -- the one the app shows on its
    /// "Run from a computer" screen -- for networks where discovery fails.
    @Parameter(property = "cn1.push.device")
    private String device;

    @Parameter(property = "cn1.push.port", defaultValue = "18234")
    private int port;

    /// The class to start, when the project has more than one candidate.
    @Parameter(property = "cn1.push.main")
    private String mainClass;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!hasApplicationSources()) {
            getLog().debug("Skipping cn1:push: no application sources in " + project.getArtifactId());
            return;
        }
        File classes = new File(project.getBuild().getOutputDirectory());
        if (!classes.isDirectory()) {
            throw new MojoFailureException("No compiled classes at " + classes
                    + ". Run it after compiling: mvn compile cn1:push");
        }
        List<String> args = new ArrayList<String>();
        args.add("--classes");
        args.add(classes.getAbsolutePath());
        for (Object root : project.getCompileSourceRoots()) {
            File dir = new File(String.valueOf(root));
            if (dir.isDirectory()) {
                args.add("--source");
                args.add(dir.getAbsolutePath());
            }
        }
        if (mainClass != null && mainClass.length() > 0) {
            args.add("--main");
            args.add(mainClass);
        }
        String version = frameworkVersion();
        if (version != null) {
            args.add("--cn1-version");
            args.add(version);
        }
        addConnectionArgs(args, lan, device, port);
        runTool(this, args);
    }

    /// The Codename One version this module compiles against -- the
    /// `codenameone-core` dependency, else the `cn1.version` property. The
    /// device warns before it runs a program built for another version.
    private String frameworkVersion() {
        for (org.apache.maven.model.Dependency d : project.getDependencies()) {
            if ("com.codenameone".equals(d.getGroupId())
                    && "codenameone-core".equals(d.getArtifactId())
                    && d.getVersion() != null) {
                return d.getVersion();
            }
        }
        String prop = project.getProperties().getProperty("cn1.version");
        return prop == null || prop.length() == 0 ? null : prop;
    }

    private boolean hasApplicationSources() {
        for (Object root : project.getCompileSourceRoots()) {
            File dir = new File(String.valueOf(root));
            if (dir.isDirectory() && dir.getPath().replace('\\', '/').contains("/src/main/")) {
                return true;
            }
        }
        return false;
    }

    static void addConnectionArgs(List<String> args, boolean lan, String device, int port) {
        args.add("--port");
        args.add(Integer.toString(port));
        if (device != null && device.length() > 0) {
            args.add("--device");
            args.add(device);
        } else if (lan) {
            args.add("--lan");
        }
    }

    /// Runs the push tool in a JVM of its own -- it ends with System.exit, which
    /// must not take Maven with it -- on the JVM running this build, with its
    /// output passed straight through so the pairing code is seen as it is
    /// printed.
    static void runTool(AbstractCN1Mojo mojo, List<String> toolArgs) throws MojoExecutionException, MojoFailureException {
        File jar = mojo.resolvePortJar(TOOL_ARTIFACT, null);
        if (jar == null || !jar.isFile()) {
            throw new MojoExecutionException("Could not resolve " + TOOL_ARTIFACT
                    + " at the plugin's version; it carries the push tool.");
        }
        List<String> cmd = new ArrayList<String>();
        cmd.add(new File(new File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath());
        cmd.add("-cp");
        cmd.add(jar.getAbsolutePath());
        cmd.add(TOOL_CLASS);
        cmd.addAll(toolArgs);
        try {
            Process p = new ProcessBuilder(cmd).inheritIO().start();
            int exit = p.waitFor();
            if (exit != 0) {
                throw new MojoFailureException("The device did not accept the request (see above).");
            }
        } catch (java.io.IOException err) {
            throw new MojoExecutionException("Could not start the push tool", err);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted", err);
        }
    }
}
