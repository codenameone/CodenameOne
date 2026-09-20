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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What a build does about the copies an EARLIER build left inside the bundle.
///
/// The deployment configuration and the suspension report used to be written into the output
/// directory, which is the application's public web root. `handleJavascriptOutput` calls
/// `mkdirs()` on an existing destination and never cleans it, so moving the new copies out
/// would otherwise leave the old ones exactly where they were -- still published, and now with
/// nothing in the build output to suggest they are there.
class JavascriptLegacyArtifactCleanupTest {

    private static File bundleWithLegacyArtifacts(String name) throws Exception {
        File dist = Files.createTempDirectory(name).toFile();
        File bundle = new File(dist, "MyApp-js");
        assertTrue(new File(bundle, "cn1-security").mkdirs());
        Files.write(new File(bundle, "suspension-report.txt").toPath(),
                "stale".getBytes(StandardCharsets.UTF_8));
        String[] generated = JavascriptSecurityHeaders.generatedFiles();
        for (int iter = 0; iter < generated.length; iter++) {
            Files.write(new File(bundle, "cn1-security/" + generated[iter]).toPath(),
                    "stale".getBytes(StandardCharsets.UTF_8));
        }
        return bundle;
    }

    @Test
    void theArtifactsAnOlderBuildLeftInTheWebRootAreRemoved() throws Exception {
        File bundle = bundleWithLegacyArtifacts("cn1-legacy");
        JavascriptBundleWriter.removeLegacyArtifacts(bundle);

        assertFalse(new File(bundle, "suspension-report.txt").exists(),
                "the stale report must go: it names translated methods");
        assertFalse(new File(bundle, "cn1-security").exists(),
                "and the stale configuration directory with it");
    }

    @Test
    void aFileTheDeveloperAddedKeepsTheDirectoryAlive() throws Exception {
        // The build removes what the build wrote. A web root is the developer's directory, and
        // deleting a file out of it because it happens to share a parent would be a far worse
        // failure than leaving one behind.
        File bundle = bundleWithLegacyArtifacts("cn1-legacy-mine");
        File mine = new File(bundle, "cn1-security/mine.conf");
        Files.write(mine.toPath(), "mine".getBytes(StandardCharsets.UTF_8));

        JavascriptBundleWriter.removeLegacyArtifacts(bundle);

        assertTrue(mine.isFile(), "a file the build never wrote is not the build's to delete");
        assertFalse(new File(bundle, "cn1-security/_headers").exists(),
                "the generated ones still go");
    }

    @Test
    void aBundleWithNothingStaleIsUntouched() throws Exception {
        File dist = Files.createTempDirectory("cn1-legacy-clean").toFile();
        File bundle = new File(dist, "MyApp-js");
        assertTrue(bundle.mkdirs());
        Files.write(new File(bundle, "index.html").toPath(),
                "<html></html>".getBytes(StandardCharsets.UTF_8));

        JavascriptBundleWriter.removeLegacyArtifacts(bundle);

        assertTrue(new File(bundle, "index.html").isFile());
    }
}
