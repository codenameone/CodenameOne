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
package com.codename1.gradle.tasks;

import com.codename1.builders.BuildException;
import com.codename1.gradle.GradleLog;
import com.codename1.maven.DeviceDeployer;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;

/// Copies a native Linux build to a device over SSH and runs it there as a
/// systemd service: `gradle cn1DeployDevice -Pcn1.device.host=pi.local`. The
/// same [DeviceDeployer] and the same `cn1.device.*` properties as the Maven
/// `deploy-device` goal.
@DisableCachingByDefault(because = "Deploys to a device")
public abstract class DeployDeviceTask extends DefaultTask {

    private boolean followLogsByDefault;

    /// Whether the service log is followed after the deploy when
    /// `cn1.device.logs` is not given: `cn1RunOnDevice` does, `cn1DeployDevice`
    /// does not.
    @Internal
    public boolean isFollowLogsByDefault() {
        return followLogsByDefault;
    }

    public void setFollowLogsByDefault(boolean followLogsByDefault) {
        this.followLogsByDefault = followLogsByDefault;
    }

    @TaskAction
    public void deploy() {
        File buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile();
        String binary = deviceProperty("binary");
        File bin = binary != null ? getProject().file(binary)
                : DeviceDeployer.findBinary(buildDir, new File(buildDir, "linux-native"));
        String port = deviceProperty("port");
        String dir = deviceProperty("dir");
        try {
            new DeviceDeployer(new GradleLog(getLogger())).host(deviceProperty("host")).user(deviceProperty("user"))
                    .port(port == null ? 22 : Integer.parseInt(port)).binary(bin)
                    .remoteDir(dir == null ? "/opt/codenameone" : dir).service(deviceProperty("service"))
                    .environment(deviceProperty("env"))
                    .profile(deviceProperty("profile") == null ? "device" : deviceProperty("profile"))
                    .followLogs(deviceProperty("logs") == null ? followLogsByDefault
                            : "true".equals(deviceProperty("logs"))).deploy();
        } catch (BuildException ex) {
            throw new GradleException(ex.getMessage(), ex);
        } catch (NumberFormatException ex) {
            throw new GradleException("cn1.device.port is not a number: " + port, ex);
        }
    }

    private String deviceProperty(String name) {
        Object v = getProject().findProperty("cn1.device." + name);
        return v == null ? null : v.toString();
    }
}
