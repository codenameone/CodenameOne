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
package com.codename1.gradle;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// How a resolved file is matched to the port it is, and which ffmpeg natives a
/// host is given. Both are decided from names alone, so both are tables.
class PortJarSelectionTest {
    private static final List<String> IOS = Arrays.asList("codenameone-ios:bundle", "codenameone-parparvm:bundle");
    private static final List<String> ANDROID = Collections.singletonList("codenameone-android");

    @Test
    void aBundleIsMatchedByArtifactAndClassifier() {
        assertEquals("codenameone-ios:bundle", GradleHostFactory.portOf("codenameone-ios-8.0-SNAPSHOT-bundle.jar", IOS));
        assertEquals("codenameone-parparvm:bundle",
                GradleHostFactory.portOf("codenameone-parparvm-7.0.281-bundle.jar", IOS));
        // A timestamped snapshot, as a remote repository serves it.
        assertEquals("codenameone-ios:bundle",
                GradleHostFactory.portOf("codenameone-ios-8.0-20261111.101500-3-bundle.jar", IOS));
    }

    @Test
    void theMainJarOfABundledArtifactIsNotTheBundle() {
        assertNull(GradleHostFactory.portOf("codenameone-ios-8.0-SNAPSHOT.jar", IOS));
        assertNull(GradleHostFactory.portOf("codenameone-ios-8.0-SNAPSHOT-sources.jar", IOS));
    }

    @Test
    void anUnclassifiedPortIsItsMainJar() {
        assertEquals("codenameone-android", GradleHostFactory.portOf("codenameone-android-8.0-SNAPSHOT.jar", ANDROID));
        assertNull(GradleHostFactory.portOf("codenameone-android-8.0-SNAPSHOT-bundle.jar", ANDROID));
    }

    @Test
    void aLongerArtifactIdIsNeverTakenForAShorterOne() {
        assertNull(GradleHostFactory.portOf("codenameone-android-compat-8.0-SNAPSHOT.jar", ANDROID));
        assertNull(GradleHostFactory.portOf("codenameone-android-res-compiler-8.0-SNAPSHOT.jar", ANDROID));
        assertNull(GradleHostFactory.portOf("codenameone-core-8.0-SNAPSHOT.jar", IOS));
        assertNull(GradleHostFactory.portOf("codenameone-ios-8.0-SNAPSHOT-bundle.zip", IOS));
    }

    @Test
    void aHostGetsItsOwnFfmpegAndNoOther() {
        // A Mac takes both: an Intel JDK under Rosetta loads the x86_64 natives.
        assertEquals(Arrays.asList("macosx-arm64", "macosx-x86_64"),
                PluginInfo.hostFfmpegPlatforms("Mac OS X", "aarch64"));
        assertEquals(Arrays.asList("macosx-arm64", "macosx-x86_64"),
                PluginInfo.hostFfmpegPlatforms("Mac OS X", "x86_64"));
        assertEquals(Collections.singletonList("windows-x86_64"),
                PluginInfo.hostFfmpegPlatforms("Windows 11", "amd64"));
        assertEquals(Collections.singletonList("linux-x86_64"), PluginInfo.hostFfmpegPlatforms("Linux", "amd64"));
        assertEquals(Collections.singletonList("linux-arm64"), PluginInfo.hostFfmpegPlatforms("Linux", "aarch64"));
    }

    @Test
    void anUnknownHostGetsNone() {
        // The simulator then finds ffmpeg on the PATH, or reports that it has none.
        assertEquals(Collections.<String>emptyList(), PluginInfo.hostFfmpegPlatforms("Linux", "riscv64"));
        assertEquals(Collections.<String>emptyList(), PluginInfo.hostFfmpegPlatforms("FreeBSD", "amd64"));
        assertEquals(Collections.<String>emptyList(), PluginInfo.hostFfmpegPlatforms(null, null));
    }
}
