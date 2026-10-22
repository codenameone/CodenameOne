/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// The Android builder turns push on, and then requires google-services.json, for any
/// app whose classes name com/codename1/push. The transpiler copies a plugin pack into
/// an app that names it, and firebase_auth alone pulls in the whole firebase pack, so
/// no pack and no runtime class may: the generated entry point installs push hooks only
/// for an app that depends on firebase_messaging (FirebaseMessaging.tokenSource /
/// topicSink).
class NoPushReferenceTest {
    @Test
    void noRuntimeClassNamesThePushPackage() throws IOException {
        File root = new File("target/classes/com/codename1/flutter");
        assertTrue(root.isDirectory(), "run after compile: " + root.getAbsolutePath());
        // The packs compile with the tests (src/main/plugins is a test source root).
        File packs = new File("target/test-classes/com/codename1/flutter/plugins");
        assertTrue(packs.isDirectory(), "run after test-compile: " + packs.getAbsolutePath());
        List<String> offenders = new ArrayList<String>();
        scan(root, offenders);
        scan(packs, offenders);
        assertTrue(offenders.isEmpty(), "classes naming com/codename1/push: " + offenders);
    }

    /// A pack compiled into the runtime jar would ship in every app again, whatever it
    /// imports: the packs reach an app only as sources the transpiler copies.
    @Test
    void noPackIsCompiledIntoTheRuntime() {
        File compiled = new File("target/classes/com/codename1/flutter/plugins");
        assertTrue(!compiled.exists(), "plugin packs compiled into the runtime: " + compiled);
        File shipped = new File("target/classes/META-INF/dart/java/com/codename1/flutter/plugins");
        assertTrue(shipped.isDirectory(), "plugin pack sources not packaged: " + shipped);
    }

    private static void scan(File dir, List<String> offenders) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                scan(f, offenders);
            } else if (f.getName().endsWith(".class")) {
                String bytes = new String(Files.readAllBytes(f.toPath()), StandardCharsets.ISO_8859_1);
                if (bytes.contains("com/codename1/push")) {
                    offenders.add(f.getPath());
                }
            }
        }
    }
}
