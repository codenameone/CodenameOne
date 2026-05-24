/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Launches the desktop on-device-debug proxy and prints instructions for
 * attaching jdb.
 *
 * Run this AFTER {@code mvn cn1:buildIosXcodeProject -Dcodename1.arg.ios.onDeviceDebug=true},
 * which generates an Xcode project pre-flipped with CN1_ON_DEVICE_DEBUG and
 * the proxy host/port in Info.plist. Open that project in Xcode and run it
 * on the simulator (or a tethered device on the same WiFi). The device-side
 * cn1_debugger thread will dial out to localhost:55333; attach jdb to
 * localhost:8000.
 *
 * The mojo blocks until the proxy exits (typically when the user disposes
 * jdb).
 *
 * Properties:
 *   -Dcn1.onDeviceDebug.symbolsFile=path   override sidecar location (else autodetect)
 *   -Dcn1.onDeviceDebug.devicePort=55333
 *   -Dcn1.onDeviceDebug.jdwpPort=8000
 *   -Dcn1.onDeviceDebug.proxyJar=path      override proxy jar location
 */
@Mojo(name="ios-on-device-debugging")
public class IosOnDeviceDebuggingMojo extends AbstractCN1Mojo {

    @Parameter(property = "cn1.onDeviceDebug.symbolsFile")
    private String symbolsFile;

    @Parameter(property = "cn1.onDeviceDebug.devicePort", defaultValue = "55333")
    private int devicePort;

    @Parameter(property = "cn1.onDeviceDebug.jdwpPort", defaultValue = "8000")
    private int jdwpPort;

    @Parameter(property = "cn1.onDeviceDebug.proxyJar")
    private String proxyJar;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File commonDir = getCN1ProjectDir();
        if (commonDir == null) {
            throw new MojoFailureException("Could not locate Codename One project root");
        }
        File rootMavenProjectDir = commonDir.getParentFile();

        File symbols = resolveSymbols(rootMavenProjectDir);
        File jar = resolveProxyJar();

        getLog().info("On-device-debug proxy starting:");
        getLog().info("  symbols : " + symbols);
        getLog().info("  device  : listening on tcp://0.0.0.0:" + devicePort);
        getLog().info("  jdwp    : listening on tcp://0.0.0.0:" + jdwpPort);
        getLog().info("");
        getLog().info("Next steps:");
        getLog().info("  1. Open the generated Xcode project in iOS Simulator or on device");
        getLog().info("     (must be on the same network for a physical device).");
        getLog().info("  2. Once the app boots it will dial in and the proxy will log a HELLO.");
        getLog().info("  3. Attach a debugger:  jdb -attach localhost:" + jdwpPort);
        getLog().info("");

        ProcessBuilder pb = new ProcessBuilder(
                javaBinary(),
                "-jar", jar.getAbsolutePath(),
                "--symbols=" + symbols.getAbsolutePath(),
                "--device-port=" + devicePort,
                "--jdwp-port=" + jdwpPort);
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

    private File resolveSymbols(File rootMavenProjectDir) throws MojoFailureException {
        if (symbolsFile != null && !symbolsFile.isEmpty()) {
            File f = new File(symbolsFile);
            if (!f.isFile()) throw new MojoFailureException("Configured symbols file does not exist: " + f);
            return f;
        }
        // Autodetect: walk common/target/codenameone for cn1-symbols.txt.
        File common = new File(rootMavenProjectDir, "common");
        if (!common.isDirectory()) common = rootMavenProjectDir;
        File target = new File(common, "target");
        if (!target.isDirectory()) {
            throw new MojoFailureException("No target/ found under " + common
                    + ". Did you run 'mvn cn1:buildIosXcodeProject -Dcodename1.arg.ios.onDeviceDebug=true' first?");
        }
        try (Stream<Path> walk = Files.walk(target.toPath())) {
            List<Path> hits = new ArrayList<>();
            walk.filter(p -> p.getFileName().toString().equals("cn1-symbols.txt")).forEach(hits::add);
            if (hits.isEmpty()) {
                throw new MojoFailureException("No cn1-symbols.txt found under " + target
                        + ". The translator only emits it when -Dcodename1.arg.ios.onDeviceDebug=true is set.");
            }
            // Pick the most recently modified.
            hits.sort(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed());
            return hits.get(0).toFile();
        } catch (IOException e) {
            throw new MojoFailureException("Failed to scan for cn1-symbols.txt: " + e.getMessage(), e);
        }
    }

    private File resolveProxyJar() throws MojoFailureException {
        if (proxyJar != null && !proxyJar.isEmpty()) {
            File f = new File(proxyJar);
            if (!f.isFile()) throw new MojoFailureException("Configured proxy jar does not exist: " + f);
            return f;
        }
        // Try the locally-built standalone shaded jar (developer workflow).
        String userHome = System.getProperty("user.home", "");
        File m2 = new File(userHome, ".m2/repository/com/codenameone/cn1-debug-proxy");
        if (m2.isDirectory()) {
            File[] versions = m2.listFiles(File::isDirectory);
            if (versions != null) {
                for (File v : versions) {
                    File jar = new File(v, "cn1-debug-proxy-" + v.getName() + "-standalone.jar");
                    if (jar.isFile()) return jar;
                }
            }
        }
        throw new MojoFailureException(
                "Could not locate cn1-debug-proxy standalone jar in ~/.m2. "
                        + "Build it once with 'cd maven/cn1-debug-proxy && mvn install', "
                        + "or pass -Dcn1.onDeviceDebug.proxyJar=<path>.");
    }

    private String javaBinary() {
        String home = System.getProperty("java.home");
        File bin = new File(home, "bin/java");
        return bin.isFile() ? bin.getAbsolutePath() : "java";
    }
}
