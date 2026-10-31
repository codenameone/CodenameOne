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
package com.codename1.designer.css;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The incremental compile asks whether a style changed since the last build by
/// following its derives. A style whose state derives from the style itself --
/// `Tile.pressed { cn1-derive: Tile; }` -- or two styles deriving from each
/// other used to send that walk round forever: a StackOverflowError, and only
/// on a rebuild, since a first build has no cache to compare with.
public class CSSDeriveCycleCacheTest {

    @BeforeAll
    static void installHeadlessImplementation() throws Exception {
        HeadlessTestSupport.installHeadlessImplementation();
    }

    @Test
    void testStateDerivingFromItsOwnStyleIsUnchangedOnRebuild() throws Exception {
        String css = "Tile { color: #111111; }"
                + "Tile.pressed { cn1-derive: Tile; background-color: #eeeeee; }";
        assertFalse(rebuild(css, css, "Tile"), "nothing changed");
    }

    @Test
    void testStylesDerivingFromEachOtherAreUnchangedOnRebuild() throws Exception {
        String css = "Ping { color: #111111; }"
                + "Ping.pressed { cn1-derive: Pong; }"
                + "Pong { color: #222222; }"
                + "Pong.pressed { cn1-derive: Ping; }";
        assertFalse(rebuild(css, css, "Ping"), "nothing changed");
    }

    @Test
    void testChangeBehindACycleIsStillSeen() throws Exception {
        String before = "Ping { color: #111111; }"
                + "Ping.pressed { cn1-derive: Pong; }"
                + "Pong { color: #222222; }"
                + "Pong.pressed { cn1-derive: Ping; }";
        String after = before.replace("#222222", "#333333");
        assertTrue(rebuild(before, after, "Ping"), "Pong changed, and Ping derives from it");
    }

    // Saves the checksums of `before`, loads `after` against them, and asks
    // whether `id` changed.
    private static boolean rebuild(String before, String after, String id) throws Exception {
        Path cssFile = Files.createTempFile("cn1-derive-cycle", ".css");
        Path cache = Files.createTempFile("cn1-derive-cycle", ".checksums");
        try {
            Files.write(cssFile, before.getBytes(StandardCharsets.UTF_8));
            CSSTheme.load(cssFile.toUri().toURL()).saveSelectorChecksums(cache.toFile());
            Files.write(cssFile, after.getBytes(StandardCharsets.UTF_8));
            CSSTheme theme = CSSTheme.load(cssFile.toUri().toURL());
            theme.getCacheStatus(cache.toFile());
            return theme.isModified(id);
        } finally {
            delete(cssFile.toFile());
            delete(cache.toFile());
        }
    }

    private static void delete(File file) {
        if (file.exists() && !file.delete()) {
            file.deleteOnExit();
        }
    }
}
