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
package com.codename1.builders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// The macOS floor has to be read from the Xcode that will run the build.
///
/// `buildChannel()` executes the bare `xcodebuild`, so PATH chooses it. Reading the floor
/// through `DEVELOPER_DIR` instead answered for an Xcode the build never runs -- raising the
/// app for nothing when PATH was older, and writing a target the real Xcode rejects when PATH
/// was newer.
class MacOSNativeBuilderXcodebuildOnPathTest {

    @Test
    void findsTheFirstExecutableOnThePath(@TempDir Path dir) throws Exception {
        File first = new File(dir.toFile(), "a");
        File second = new File(dir.toFile(), "b");
        first.mkdirs();
        second.mkdirs();
        File winner = new File(first, "xcodebuild");
        File loser = new File(second, "xcodebuild");
        Files.write(winner.toPath(), "#!/bin/sh\n".getBytes("UTF-8"));
        Files.write(loser.toPath(), "#!/bin/sh\n".getBytes("UTF-8"));
        winner.setExecutable(true);
        loser.setExecutable(true);
        assertEquals(winner.getAbsolutePath(),
                MacOSNativeBuilder.xcodebuildOnPath(first + File.pathSeparator + second),
                "PATH order decides, exactly as the shell would resolve it");
    }

    @Test
    void skipsANonExecutableAndKeepsLooking(@TempDir Path dir) throws Exception {
        File first = new File(dir.toFile(), "a");
        File second = new File(dir.toFile(), "b");
        first.mkdirs();
        second.mkdirs();
        File notExec = new File(first, "xcodebuild");
        File real = new File(second, "xcodebuild");
        Files.write(notExec.toPath(), "not a program".getBytes("UTF-8"));
        Files.write(real.toPath(), "#!/bin/sh\n".getBytes("UTF-8"));
        notExec.setExecutable(false);
        real.setExecutable(true);
        assertEquals(real.getAbsolutePath(),
                MacOSNativeBuilder.xcodebuildOnPath(first + File.pathSeparator + second));
    }

    @Test
    void answersNullWhenThePathHasNone(@TempDir Path dir) {
        assertNull(MacOSNativeBuilder.xcodebuildOnPath(dir.toFile().getAbsolutePath()));
        assertNull(MacOSNativeBuilder.xcodebuildOnPath(""));
        assertNull(MacOSNativeBuilder.xcodebuildOnPath(null));
    }

    /// An empty PATH entry means "the current directory" to some shells; it must not be
    /// treated as a directory named "" whose child resolves relative to wherever the build
    /// happens to be running.
    @Test
    void ignoresEmptyPathEntries(@TempDir Path dir) {
        assertNull(MacOSNativeBuilder.xcodebuildOnPath(File.pathSeparator + File.pathSeparator));
    }
}
