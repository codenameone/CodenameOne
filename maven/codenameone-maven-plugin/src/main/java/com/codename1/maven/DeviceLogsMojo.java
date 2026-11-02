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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Follows the log of the app installed on a device, without redeploying it:
 * {@code mvn cn1:device-logs -Dcn1.device.host=pi.local}. The service is
 * {@code cn1.device.service}, else the name of the executable the last local
 * build produced.
 */
@Mojo(name = "device-logs", requiresProject = false)
public class DeviceLogsMojo extends AbstractMojo {
    @Parameter(property = "cn1.device.host")
    private String host;
    @Parameter(property = "cn1.device.user")
    private String user;
    @Parameter(property = "cn1.device.port", defaultValue = "22")
    private int port;
    @Parameter(property = "cn1.device.service")
    private String service;
    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File buildDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            new DeviceDeployer(MavenLog.of(getLog())).host(host).user(user).port(port).service(service)
                    .binary(buildDirectory == null ? null : DeployDeviceMojo.findBuiltBinary(buildDirectory))
                    .tailLogs();
        } catch (BuildException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }
}
