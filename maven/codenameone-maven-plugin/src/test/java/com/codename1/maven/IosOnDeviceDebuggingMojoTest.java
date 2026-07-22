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
package com.codename1.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class IosOnDeviceDebuggingMojoTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void resolvesProxyFromPluginDependenciesInsteadOfArbitraryStandaloneVersion() throws Exception {
        File staleJar = tmp.newFile("cn1-debug-proxy-7.0.259-standalone.jar");
        File bundledJar = tmp.newFile("cn1-debug-proxy-7.0.260.jar");

        IosOnDeviceDebuggingMojo mojo = new IosOnDeviceDebuggingMojo();
        mojo.pluginArtifacts = Arrays.asList(
                artifact("7.0.259", "standalone", staleJar),
                artifact("7.0.260", null, bundledJar));

        assertEquals(bundledJar, mojo.resolveProxyJar());
    }

    @Test
    public void launchesBundledJarByMainClassWithoutRequiringStandaloneManifest() throws Exception {
        File bundledJar = tmp.newFile("cn1-debug-proxy.jar");
        IosOnDeviceDebuggingMojo mojo = new IosOnDeviceDebuggingMojo();

        List<String> command = mojo.proxyCommand(bundledJar);

        assertEquals("-cp", command.get(1));
        assertEquals(bundledJar.getAbsolutePath(), command.get(2));
        assertEquals("com.codename1.debug.proxy.ProxyMain", command.get(3));
    }

    private static Artifact artifact(String version, String classifier, File file) {
        DefaultArtifact artifact = new DefaultArtifact(
                "com.codenameone", "cn1-debug-proxy", version, "runtime", "jar",
                classifier, new DefaultArtifactHandler("jar"));
        artifact.setFile(file);
        return artifact;
    }
}
