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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Launches the desktop on-device-debug proxy and prints instructions for
 * attaching jdb.
 *
 * Build the app first with {@code ios.onDeviceDebug=true} — either the local
 * Xcode path ({@code cn1:buildIosXcodeProject}) or a cloud build
 * ({@code cn1:buildIosOnDeviceDebug}). Either way the binary is flipped with
 * CN1_ON_DEVICE_DEBUG and carries its own symbol table; when the app boots it
 * dials out to the proxy on the device port and streams that table over, so
 * there is no sidecar file to locate. Run the app (native simulator or a
 * tethered device on the same Wi-Fi) and attach jdb to localhost:8000.
 *
 * The mojo blocks until the proxy exits (typically when the user disposes
 * jdb).
 *
 * Properties:
 *   -Dcn1.onDeviceDebug.devicePort=55333
 *   -Dcn1.onDeviceDebug.jdwpPort=8000
 *   -Dcn1.onDeviceDebug.proxyJar=path      override proxy jar location
 */
@Mojo(name="ios-on-device-debugging")
public class IosOnDeviceDebuggingMojo extends AbstractCN1Mojo {

    @Parameter(property = "cn1.onDeviceDebug.devicePort", defaultValue = "55333")
    private int devicePort;

    @Parameter(property = "cn1.onDeviceDebug.jdwpPort", defaultValue = "8000")
    private int jdwpPort;

    @Parameter(property = "cn1.onDeviceDebug.proxyJar")
    private String proxyJar;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File jar = resolveProxyJar();

        getLog().info("On-device-debug proxy starting:");
        getLog().info("  device  : listening on tcp://0.0.0.0:" + devicePort);
        getLog().info("  jdwp    : listening on tcp://0.0.0.0:" + jdwpPort);
        getLog().info("  symbols : streamed from the device on connect (no local file)");
        getLog().info("");
        getLog().info("Next steps:");
        getLog().info("  1. Run the on-device-debug build in the iOS Simulator or on device");
        getLog().info("     (must be on the same network for a physical device).");
        getLog().info("  2. Once the app boots it will dial in and the proxy will log a HELLO.");
        getLog().info("  3. Attach a debugger:  jdb -attach localhost:" + jdwpPort);
        getLog().info("");

        ProcessBuilder pb = new ProcessBuilder(proxyCommand(jar));
        pb.inheritIO();
        try {
            Process proc = pb.start();
            int rc = proc.waitFor();
            if (rc != 0) {
                getLog().warn("Proxy exited with code " + rc);
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Failed to run proxy: " + e.getMessage(), e);
        }
    }

    File resolveProxyJar() throws MojoFailureException {
        if (proxyJar != null && !proxyJar.isEmpty()) {
            File f = new File(proxyJar);
            if (!f.isFile()) throw new MojoFailureException("Configured proxy jar does not exist: " + f);
            return f;
        }
        if (pluginArtifacts != null) {
            for (Artifact artifact : pluginArtifacts) {
                if ("com.codenameone".equals(artifact.getGroupId())
                        && "cn1-debug-proxy".equals(artifact.getArtifactId())
                        && artifact.getClassifier() == null) {
                    File jar = getJar(artifact);
                    if (jar != null && (jar.isFile() || jar.isDirectory())) {
                        return jar;
                    }
                }
            }
        }
        throw new MojoFailureException(
                "Could not resolve the cn1-debug-proxy bundled with this Codename One Maven plugin. "
                        + "Reinstall the plugin or pass -Dcn1.onDeviceDebug.proxyJar=<path>.");
    }

    List<String> proxyCommand(File jar) {
        List<String> command = new ArrayList<String>();
        command.add(javaBinary());
        command.add("-cp");
        command.add(jar.getAbsolutePath());
        command.add("com.codename1.debug.proxy.ProxyMain");
        command.add("--device-port=" + devicePort);
        command.add("--jdwp-port=" + jdwpPort);
        return command;
    }

    private String javaBinary() {
        String home = System.getProperty("java.home");
        File bin = new File(home, "bin/java");
        return bin.isFile() ? bin.getAbsolutePath() : "java";
    }
}
