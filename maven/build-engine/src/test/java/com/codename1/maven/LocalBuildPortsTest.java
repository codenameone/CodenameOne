/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the LICENSE file that accompanied this code.
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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The ports each local target reads.
///
/// The ports are not on the build tool's class path: a simulator run would
/// download all of them. A builder is handed the ones in this table instead, and
/// a target missing from it fails in the middle of its build with "Resource not
/// found". The table is therefore spelled out here a second time, so that a
/// change to it is a change someone made on purpose.
class LocalBuildPortsTest {
    private static final String VM = "codenameone-parparvm:bundle";

    private static List<String> ports(String target) {
        return AppBuilder.portArtifacts(target);
    }

    @Test
    void iosAndMacReadTheirPortAndTheTranslator() {
        assertEquals(Arrays.asList("codenameone-ios:bundle", VM), ports("ios-source"));
        assertEquals(Arrays.asList("codenameone-mac:bundle", VM), ports("mac-source"));
        assertEquals(Arrays.asList("codenameone-mac:bundle", VM), ports("local-mac-device"));
    }

    @Test
    void windowsLinuxAndSteamReadTheirPortAndTheTranslator() {
        assertEquals(Arrays.asList("codenameone-windows:bundle", VM), ports("windows-source"));
        assertEquals(Arrays.asList("codenameone-windows:bundle", VM), ports("local-windows-device"));
        assertEquals(Arrays.asList("codenameone-linux:bundle", VM), ports("local-linux-device"));
        assertEquals(Arrays.asList("codenameone-linux:bundle", VM), ports("local-steam"));
    }

    @Test
    void androidReadsItsPortAlone() {
        assertEquals(Collections.singletonList("codenameone-android"), ports("android-source"));
    }

    @Test
    void javascriptReadsTheTranslatorBundleAlone() {
        // The JavaScript port and the proxy servlets travel in the translator's bundle.
        assertEquals(Collections.singletonList(VM), ports("local-javascript"));
    }

    @Test
    void aCloudBuildReadsNone() {
        // The build server has its own ports; fetching ours would be a download for nothing.
        for (String target : new String[] {"ios-device", "ios-device-release", "android-device",
                "javascript", "windows-device", "linux-device", "mac-os-x-native",
                "mac-os-x-desktop", "windows-desktop"}) {
            assertEquals(Collections.<String>emptyList(), ports(target), target);
        }
    }

    @Test
    void everyLocalTargetHasPorts() {
        // The reverse of the above: a target that builds here and names no port
        // has a builder with nothing to read.
        for (String target : new String[] {"ios-source", "mac-source", "android-source",
                "windows-source", "local-windows-device", "local-linux-device", "local-mac-device",
                "local-steam", "local-javascript"}) {
            assertEquals(true, AppBuilder.isLocalBuildTarget(target), target);
            assertEquals(false, ports(target).isEmpty(), target);
        }
    }
}
