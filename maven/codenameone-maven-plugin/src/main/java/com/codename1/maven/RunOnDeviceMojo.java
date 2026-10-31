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

import com.codename1.builders.BuildException;
import com.codename1.maven.buildWrappers.AbstractBuildWrapperMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/**
 * Builds the app for a board on this machine, installs it on the device and
 * follows its log -- one command from source to a running device:
 * {@code mvn cn1:runOnDevice -Dcn1.board=rpi5 -Dcn1.device.host=pi.local}. The
 * build is {@code cn1:buildLinuxLocal}, the install is {@code cn1:deploy-device},
 * and every {@code cn1.device.*} option of that goal applies.
 */
@Mojo(name = "runOnDevice", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class RunOnDeviceMojo extends AbstractBuildWrapperMojo {
    @Parameter(property = "cn1.device.host")
    private String host;
    @Parameter(property = "cn1.device.user")
    private String user;
    @Parameter(property = "cn1.device.port", defaultValue = "22")
    private int port;
    @Parameter(property = "cn1.device.dir", defaultValue = "/opt/codenameone")
    private String remoteDir;
    @Parameter(property = "cn1.device.service")
    private String service;
    @Parameter(property = "cn1.device.env", defaultValue = "")
    private String environment;
    @Parameter(property = "cn1.device.profile", defaultValue = "device")
    private String profile;
    /** Follow the log after the restart; on by default here, since this is the run goal. */
    @Parameter(property = "cn1.device.logs", defaultValue = "true")
    private boolean logs;

    @Override
    protected String getPlatform() {
        return "linux";
    }

    @Override
    protected String getBuildTarget() {
        return "local-linux-device";
    }

    private final long started = System.currentTimeMillis();

    @Override
    protected void afterBuild() throws MojoExecutionException {
        File bin = DeployDeviceMojo.findBuiltBinary(new File(getProject().getBuild().getDirectory()));
        // The search takes the newest executable it finds, so a build that wrote
        // none would deploy an earlier one; two seconds of slack for file systems
        // that keep modification times to the second.
        if (bin != null && bin.lastModified() < started - 2000) {
            throw new MojoExecutionException("The build produced no new executable; " + bin
                    + " is from an earlier build");
        }
        try {
            new DeviceDeployer(MavenLog.of(getLog())).host(host).user(user).port(port)
                    .binary(bin).remoteDir(remoteDir).service(service)
                    .environment(environment).profile(profile).followLogs(logs).deploy();
        } catch (BuildException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }
}
