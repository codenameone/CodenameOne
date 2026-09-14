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
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/// The SQLite engine ships only to applications that can reach it.
///
/// About 1.5 MB of `sqlite3mc.js` and `sqlite3.wasm` was being copied into the public web root of
/// every application built for this port, most of which never open a database. It is loaded
/// lazily at runtime, so it never cost page-load time -- it cost deployment size, in an artifact
/// people upload to static hosting.
///
/// The asymmetry is what these tests are really about. Shipping an engine nothing loads wastes
/// bandwidth; **not** shipping one an application needs is a database that will not open, found
/// by a user rather than by a build. So the decision has to be right in the positive direction
/// and merely useful in the negative one, and when it cannot tell it must ship.
class JavascriptOptionalAssetsTest {

    private static Path bundleWith(String translatedAppBody) throws Exception {
        Path dir = Files.createTempDirectory("cn1-optional-assets");
        Files.write(dir.resolve("translated_app.js"),
                translatedAppBody.getBytes(StandardCharsets.UTF_8));
        return dir;
    }

    @Test
    void anApplicationThatNeverTouchesADatabaseDoesNotShipTheEngine() throws Exception {
        Path dir = bundleWith("function cn1_com_example_MyApp_start__() { return 1; }\n");
        Set<String> skipped = JavascriptBundleWriter.optionalAssetsToSkip(dir.toFile());
        assertTrue(skipped.contains("sqlite3mc.js"), skipped.toString());
        assertTrue(skipped.contains("sqlite3.wasm"), skipped.toString());
        assertTrue(skipped.contains("sqlite3-opfs-async-proxy.js"), skipped.toString());
    }

    @Test
    void anApplicationThatOpensADatabaseShipsTheEngine() throws Exception {
        // The marker is the mangled name the generator emits for a SQLiteNative call, which
        // survives the dead-code pass only when something can actually reach it.
        Path dir = bundleWith("var x = " + JavascriptBundleWriter.sqliteNativePrefix()
                + "init_R_boolean;\n");
        assertTrue(JavascriptBundleWriter.optionalAssetsToSkip(dir.toFile()).isEmpty());
    }

    @Test
    void theMarkerIsTheNameTheGeneratorActuallyEmits() {
        // Pinned against the mangler rather than against a literal. A stale marker would stop
        // matching, decide that no application uses SQLite, and quietly drop the engine from the
        // builds that need it -- the failure this whole file exists to avoid.
        String fromMangler = JavascriptNameUtil.methodIdentifier(
                "com/codename1/impl/html5/database/SQLiteNative", "init", "()Z");
        assertTrue(fromMangler.startsWith(JavascriptBundleWriter.sqliteNativePrefix()),
                fromMangler + " does not start with " + JavascriptBundleWriter.sqliteNativePrefix());
    }

    @Test
    void anUnreadableBundleShipsEverything() throws Exception {
        // No translated output: this was not a translation, and nothing here has grounds to drop
        // an asset. Ship.
        Path empty = Files.createTempDirectory("cn1-optional-assets-empty");
        assertTrue(JavascriptBundleWriter.optionalAssetsToSkip(empty.toFile()).isEmpty());

        File missing = new File(empty.toFile(), "not-a-directory");
        assertTrue(JavascriptBundleWriter.optionalAssetsToSkip(missing).isEmpty());
    }

    @Test
    void everyChunkOfASplitBundleIsConsulted() throws Exception {
        // translated_app.js is split into translated_app_NN.js chunks for hosts with a per-file
        // size limit. A scan that only read the first chunk would drop the engine from any
        // application whose database call landed in a later one.
        Path dir = bundleWith("nothing interesting here\n");
        Files.write(dir.resolve("translated_app_07.js"),
                (JavascriptBundleWriter.sqliteNativePrefix() + "exists_java_lang_String_R_boolean")
                        .getBytes(StandardCharsets.UTF_8));
        assertTrue(JavascriptBundleWriter.optionalAssetsToSkip(dir.toFile()).isEmpty());
    }

    @Test
    void theEngineIsTheOnlyThingDropped() throws Exception {
        // Scoped deliberately. The other heavyweight assets under js/ are a separate question with
        // separate markers, and widening this set without one would be guessing.
        Path dir = bundleWith("function cn1_com_example_MyApp_start__() { return 1; }\n");
        Set<String> skipped = JavascriptBundleWriter.optionalAssetsToSkip(dir.toFile());
        assertFalse(skipped.contains("jquery.min.js"));
        assertFalse(skipped.contains("localforage-shim.js"));
        assertFalse(skipped.contains("fontmetrics.js"));
        assertEquals(4, skipped.size(), skipped.toString());
    }
}
