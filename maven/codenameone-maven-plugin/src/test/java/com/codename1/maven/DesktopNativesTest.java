/*
 * Copyright (c) 2021, 2026, Codename One and/or its affiliates. All rights reserved.
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
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// The desktop jar is built on one platform and run on all of them, so it takes the
/// ffmpeg natives the project's own class path -- the building machine's -- lacks.
public class DesktopNativesTest {
    private static Artifact artifact(String group, String id, String version, String classifier) {
        return new DefaultArtifact(group, id, version, "runtime", "jar", classifier, new DefaultArtifactHandler("jar"));
    }

    private static List<String> missing(Artifact... artifacts) {
        List<String> out = new ArrayList<String>();
        for (String[] gav : JavaSEExecutableJarMojo.missingDesktopNatives(Arrays.asList(artifacts))) {
            out.add(gav[0] + ":" + gav[1] + ":" + gav[2] + ":" + gav[3]);
        }
        return out;
    }

    @Test
    public void aMacBuildTakesTheLinuxAndWindowsNatives() {
        assertEquals(Arrays.asList(
                "org.bytedeco:ffmpeg:7.1-1.5.11:linux-arm64",
                "org.bytedeco:ffmpeg:7.1-1.5.11:linux-x86_64",
                "org.bytedeco:ffmpeg:7.1-1.5.11:windows-x86_64",
                "org.bytedeco:javacpp:1.5.11:linux-arm64",
                "org.bytedeco:javacpp:1.5.11:linux-x86_64",
                "org.bytedeco:javacpp:1.5.11:windows-x86_64"),
                missing(artifact("org.bytedeco", "ffmpeg", "7.1-1.5.11", null),
                        artifact("org.bytedeco", "ffmpeg", "7.1-1.5.11", "macosx-arm64"),
                        artifact("org.bytedeco", "ffmpeg", "7.1-1.5.11", "macosx-x86_64"),
                        artifact("org.bytedeco", "javacpp", "1.5.11", null),
                        artifact("org.bytedeco", "javacpp", "1.5.11", "macosx-arm64"),
                        artifact("org.bytedeco", "javacpp", "1.5.11", "macosx-x86_64")));
    }

    @Test
    public void aProjectThatAlreadyHasEveryPlatformTakesNothing() {
        List<Artifact> all = new ArrayList<Artifact>();
        all.add(artifact("org.bytedeco", "ffmpeg", "7.1-1.5.11", null));
        for (String platform : JavaSEExecutableJarMojo.DESKTOP_NATIVE_PLATFORMS) {
            all.add(artifact("org.bytedeco", "ffmpeg", "7.1-1.5.11", platform));
        }
        // Android's, which the old aggregator brought, are neither needed nor asked for.
        all.add(artifact("org.bytedeco", "ffmpeg", "7.1-1.5.11", "android-arm64"));
        assertTrue(JavaSEExecutableJarMojo.missingDesktopNatives(all).isEmpty());
    }

    @Test
    public void aProjectWithoutFfmpegGetsNone() {
        assertEquals(Collections.<String>emptyList(),
                missing(artifact("com.codenameone", "codenameone-core", "1.0", null),
                        artifact("org.bytedeco", "javacv", "1.5.11", null)));
    }

    @Test
    public void theVersionIsTheProjectsOwn() {
        assertEquals("org.bytedeco:ffmpeg:6.0-1.5.9:linux-arm64",
                missing(artifact("org.bytedeco", "ffmpeg", "6.0-1.5.9", null)).get(0));
    }
}
