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
 * Copies a native Linux build to a device over SSH and runs it there as a
 * systemd service that starts on boot: {@code mvn cn1:deploy-device
 * -Dcn1.device.host=pi.local}. Redeploying replaces the binary and restarts the
 * service. See {@link DeviceDeployer}.
 */
@Mojo(name = "deploy-device", requiresProject = false)
public class DeployDeviceMojo extends AbstractMojo {

    /** The device: an address, a host name or an {@code ~/.ssh/config} alias. */
    @Parameter(property = "cn1.device.host")
    private String host;

    /** The SSH user; the default is whatever ssh picks for the host. */
    @Parameter(property = "cn1.device.user")
    private String user;

    /** The SSH port. */
    @Parameter(property = "cn1.device.port", defaultValue = "22")
    private int port;

    /**
     * The executable to deploy. By default the newest Linux executable under
     * {@code target}, which is where a local linux-native build leaves it.
     */
    @Parameter(property = "cn1.device.binary")
    private File binary;

    /** Where the binary is installed on the device. */
    @Parameter(property = "cn1.device.dir", defaultValue = "/opt/codenameone")
    private String remoteDir;

    /** The systemd service name; defaults to the binary's file name. */
    @Parameter(property = "cn1.device.service")
    private String service;

    /** Environment for the service, e.g. {@code CN1_ROTATION=90,CN1_DISPLAY=fbdev}. */
    @Parameter(property = "cn1.device.env")
    private String environment;

    /** Streams the service's log after the restart. */
    /** The backend profile the service runs under ({@code CN1_PROFILE}); empty for none. */
    @Parameter(property = "cn1.device.profile", defaultValue = "device")
    private String profile;

    @Parameter(property = "cn1.device.logs", defaultValue = "false")
    private boolean logs;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File buildDirectory;

    /// The newest executable a local Linux build left: `target/linux-native` of this
    /// module, or of the `linux` module beside it when the goal runs at the root.
    static File findBuiltBinary(File buildDirectory) {
        File root = buildDirectory.getParentFile();
        return DeviceDeployer.findBinary(buildDirectory, new File(buildDirectory, "linux-native"),
                root == null ? null : new File(root, "linux" + File.separator + "target" + File.separator + "linux-native"));
    }

    @Override
    public void execute() throws MojoExecutionException {
        File bin = binary;
        if (bin == null && buildDirectory != null) {
            bin = findBuiltBinary(buildDirectory);
        }
        try {
            new DeviceDeployer(MavenLog.of(getLog())).host(host).user(user).port(port).binary(bin)
                    .remoteDir(remoteDir).service(service).environment(environment).profile(profile).followLogs(logs).deploy();
        } catch (BuildException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }
}
