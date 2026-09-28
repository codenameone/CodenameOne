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
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Which Xcode the SDK questions are asked of.
///
/// resolveXcodebuild() prefers $XCODEBUILD while the developer directory used to prefer
/// $DEVELOPER_DIR, so the two disagreed whenever those named different installations. Once
/// the SDK's minimum deployment target started being read through this, that disagreement
/// wrote projects below the floor the real toolchain enforces -- a build selecting Xcode 27
/// with DEVELOPER_DIR still on 26 read the old 12.0 minimum and produced an archive Xcode 27
/// refuses.
class DeveloperDirPrecedenceTest {

    /// A directory shaped enough like an Xcode to satisfy isDeveloperDir: it wants both
    /// usr/bin/xcodebuild and Platforms, which is what separates a real developer directory
    /// from a CommandLineTools tree or the filesystem root.
    private static File developerDirOf(Path dir, String name) throws IOException {
        File developer = dir.resolve(name).resolve("Contents").resolve("Developer").toFile();
        File bin = new File(developer, "usr/bin");
        bin.mkdirs();
        new File(developer, "Platforms").mkdirs();
        new File(bin, "xcodebuild").createNewFile();
        return developer;
    }

    private static String xcodebuildIn(File developerDir) {
        return new File(developerDir, "usr/bin/xcodebuild").getAbsolutePath();
    }

    /// The finding: the resolved xcodebuild wins, and the stale environment does not.
    @Test
    void theResolvedXcodebuildBeatsTheEnvironment(@TempDir Path dir) throws Exception {
        File selected = developerDirOf(dir, "Xcode27.app");
        File stale = developerDirOf(dir, "Xcode.app");

        assertEquals(selected.getAbsolutePath(),
                IPhoneBuilder.developerDirFor(xcodebuildIn(selected), stale.getAbsolutePath()),
                "the SDK must be asked of the Xcode whose xcodebuild actually runs");
    }

    /// With nothing to derive from, the environment is still better than nothing -- this is
    /// the path a machine with only CommandLineTools takes.
    @Test
    void fallsBackToTheEnvironment(@TempDir Path dir) throws Exception {
        String env = developerDirOf(dir, "Xcode.app").getAbsolutePath();
        assertEquals(env, IPhoneBuilder.developerDirFor(null, env));
    }

    /// A path that is not a developer directory must not be believed just because it was
    /// resolved. Two levels up from /usr/bin/xcodebuild on a machine with the shim is the
    /// filesystem root, which has usr/bin and no Platforms.
    @Test
    void ignoresSomethingThatIsNotADeveloperDirectory(@TempDir Path dir) throws Exception {
        File notXcode = dir.resolve("usr").resolve("bin").resolve("xcodebuild").toFile();
        notXcode.getParentFile().mkdirs();
        notXcode.createNewFile();
        String env = developerDirOf(dir, "Xcode.app").getAbsolutePath();

        assertEquals(env, IPhoneBuilder.developerDirFor(notXcode.getAbsolutePath(), env),
                "an unusable derivation must fall through to the environment");
        assertNull(IPhoneBuilder.developerDirFor(notXcode.getAbsolutePath(), null));
    }

    @Test
    void nothingAtAllIsNull() {
        assertNull(IPhoneBuilder.developerDirFor(null, null));
        assertNull(IPhoneBuilder.developerDirFor(null, ""));
    }
}
