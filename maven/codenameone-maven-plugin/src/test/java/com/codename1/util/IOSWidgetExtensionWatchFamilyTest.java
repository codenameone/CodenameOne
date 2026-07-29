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
package com.codename1.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A watch complication is a WidgetKit widget in an accessory family, so the surfaces watch families
/// have to reach the generated widget bundle as those families. The awkward one is
/// `.accessoryCorner`: it exists only on watchOS, so naming it unguarded would fail to compile the
/// iOS extension even though that code would never run.
class IOSWidgetExtensionWatchFamilyTest {

    @Test
    void watchFamiliesBecomeAccessoryFamilies() throws IOException {
        String bundle = bundleFor("watchCircular", "watchRectangular", "watchInline");

        assertTrue(bundle.contains(".accessoryCircular"));
        assertTrue(bundle.contains(".accessoryRectangular"));
        assertTrue(bundle.contains(".accessoryInline"));
        assertFalse(bundle.contains("#if os(watchOS)"),
                "No watch-only family was declared, so no platform guard is needed");
    }

    @Test
    void cornerFamilyIsGuardedToWatchOS() throws IOException {
        String bundle = bundleFor("watchCircular", "watchCorner");

        assertTrue(bundle.contains("#if os(watchOS)"),
                "accessoryCorner exists only on watchOS and must be declared behind a guard");
        assertTrue(bundle.contains(".accessoryCorner"));
        // The #else arm keeps the iOS extension compiling with the families it does have.
        assertTrue(bundle.contains("#else"));
        assertTrue(bundle.contains("#endif"));
    }

    @Test
    void phoneOnlyKindIsUnaffected() throws IOException {
        String bundle = bundleFor("small", "medium", "large");

        assertTrue(bundle.contains(".systemSmall, .systemMedium, .systemLarge"));
        assertFalse(bundle.contains("accessory"),
                "A kind that declares no watch family must not gain one");
        assertFalse(bundle.contains("#if os(watchOS)"));
    }

    @Test
    void watchFamilyDetectionDrivesTheWatchExtension() {
        assertTrue(IOSWidgetExtensionBuilder.hasWatchFamily(
                new IOSWidgetExtensionBuilder.Kind("steps")
                        .setIosFamilies(Arrays.asList("small", "watchCircular"))));
        assertFalse(IOSWidgetExtensionBuilder.hasWatchFamily(
                new IOSWidgetExtensionBuilder.Kind("steps")
                        .setIosFamilies(Arrays.asList("small", "medium"))));
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private static String bundleFor(String... families) throws IOException {
        IOSWidgetExtensionBuilder b = new IOSWidgetExtensionBuilder()
                .setHostBundleId("com.mycompany.myapp")
                .setAppGroupId("group.com.mycompany.myapp")
                .addKind(new IOSWidgetExtensionBuilder.Kind("steps")
                        .setName("Steps")
                        .setIosFamilies(Arrays.asList(families)));
        Map<String, byte[]> files = b.buildFileMap();
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            if (e.getKey().endsWith("CN1WidgetBundle.swift")) {
                return new String(e.getValue(), StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("The generated widget bundle was not produced: " + files.keySet());
    }
}
