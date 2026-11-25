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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

/// Stops the program the Codename One Device Runtime app is running, from the
/// computer that pushed it: `mvn cn1:push-stop` (with `-Dcn1.push.lan` or
/// `-Dcn1.push.device=...` as for `cn1:push`).
///
/// Authenticated like a push, so only a paired computer can stop a program.
/// On the device itself, the floating Stop button does the same.
@Mojo(name = "push-stop", requiresProject = false, aggregator = true)
public class DevicePushStopMojo extends AbstractCN1Mojo {
    @Parameter(property = "cn1.push.lan", defaultValue = "false")
    private boolean lan;

    @Parameter(property = "cn1.push.device")
    private String device;

    @Parameter(property = "cn1.push.port", defaultValue = "18234")
    private int port;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        args.add("--stop");
        DevicePushMojo.addConnectionArgs(args, lan, device, port);
        DevicePushMojo.runTool(this, args);
    }
}
