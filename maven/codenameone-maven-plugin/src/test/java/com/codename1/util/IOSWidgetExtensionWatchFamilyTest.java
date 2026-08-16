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

import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A watch complication is a WidgetKit widget in an accessory family, so the surfaces watch families
/// map onto those families -- but only in a watch target. The generated extension is the iOS one, so
/// a complication must not surface there: a kind that asked for a complication and got an iPhone
/// lock-screen or home-screen widget is a wrong surface in front of the user, not an approximation.
class IOSWidgetExtensionWatchFamilyTest {

    /// A project declaring only complications is legitimate -- it just has no iOS surface until the
    /// watchOS extension target exists. The extension must therefore not be generated at all: an
    /// emitted-but-empty `WidgetBundle` body does not compile, and falling back to the home-screen
    /// sizes would ship a widget the manifest never asked for.
    @Test
    void watchOnlyProjectHasNoIosSurface() {
        IOSWidgetExtensionBuilder b = builderFor("watchCircular", "watchRectangular", "watchInline");

        assertFalse(b.hasIosSurface(),
                "Only complication families were declared, so there is nothing for iOS to host");
    }

    /// And if a caller ignores that and generates anyway, it fails loudly here rather than emitting
    /// Swift that breaks the whole iOS build.
    @Test
    void generatingAnEmptyBundleIsRefused() {
        IOSWidgetExtensionBuilder b = builderFor("watchCircular");

        assertThrows(IllegalStateException.class, new Executable() {
            public void execute() throws Throwable {
                b.buildFileMap();
            }
        });
    }

    @Test
    void mixedKindKeepsOnlyItsPhoneFamilies() throws IOException {
        String bundle = bundleFor("small", "lockscreen", "watchCircular", "watchCorner");

        // It does have a phone surface, so it is emitted -- with the families that exist there.
        assertTrue(bundle.contains("CN1Widget_steps"));
        assertTrue(bundle.contains(".systemSmall"));
        assertTrue(bundle.contains(".accessoryRectangular"),
                "lockscreen is an iOS family in its own right");
        assertFalse(bundle.contains(".accessoryCircular"),
                "watchCircular is a complication family and does not belong to the iOS target");
        assertFalse(bundle.contains(".accessoryCorner"));
        assertFalse(bundle.contains("#if os(watchOS)"),
                "Nothing watch-only reaches the iOS target, so no platform guard is needed");
    }

    @Test
    void watchOnlyDetectionSeparatesTheTwoCases() {
        assertTrue(IOSWidgetExtensionBuilder.isWatchOnly(
                new IOSWidgetExtensionBuilder.Kind("steps")
                        .setIosFamilies(Arrays.asList("watchCircular", "watchCorner"))));
        assertFalse(IOSWidgetExtensionBuilder.isWatchOnly(
                new IOSWidgetExtensionBuilder.Kind("steps")
                        .setIosFamilies(Arrays.asList("small", "watchCircular"))),
                "A kind with a phone family still has a surface in the iOS extension");
        assertFalse(IOSWidgetExtensionBuilder.isWatchOnly(
                new IOSWidgetExtensionBuilder.Kind("steps")
                        .setIosFamilies(Arrays.asList("small"))));
    }

    /// The accessory spellings are NOT synonyms for the portable watch names.
    ///
    /// `.accessoryCircular`, `.accessoryInline` and `.accessoryRectangular` are iPhone lock-screen
    /// families as well as watch ones -- CN1DescriptorWidget.swift renders all three under
    /// `if #available(iOS 16.0, watchOS 9.0)`, and only `.accessoryCorner` sits inside
    /// `#if os(watchOS)`. Treating them as watch-only withholds a lock-screen widget the manifest
    /// asked for; treating accessoryCorner as an iOS family emits one iOS cannot show.
    @Test
    void accessorySpellingsKeepTheirIosSurface() throws IOException {
        assertFalse(IOSWidgetExtensionBuilder.isWatchOnly(
                new IOSWidgetExtensionBuilder.Kind("steps")
                        .setIosFamilies(Arrays.asList("accessoryCircular", "accessoryInline"))),
                "the lock-screen accessory families are an iOS surface");
        assertTrue(builderFor("accessoryCircular", "accessoryInline").hasIosSurface(),
                "a kind declared only in accessory spellings still has an iOS extension to live in");
        assertTrue(builderFor("accessoryRectangular").hasIosSurface(),
                "accessoryRectangular is the lock-screen family");

        String bundle = bundleFor("accessoryCircular", "accessoryInline");
        assertTrue(bundle.contains(".accessoryCircular"), bundle);
        assertTrue(bundle.contains(".accessoryInline"), bundle);
        assertFalse(bundle.contains(".systemSmall"),
                "declared families must not fall through to the home-screen default: " + bundle);

        // accessoryCorner IS watchOS-only, so it behaves exactly like watchCorner.
        assertTrue(IOSWidgetExtensionBuilder.isWatchOnly(
                new IOSWidgetExtensionBuilder.Kind("steps")
                        .setIosFamilies(Arrays.asList("accessoryCorner"))),
                "the corner complication has no iOS surface");
        assertTrue(IOSWidgetExtensionBuilder.hasWatchFamily(
                new IOSWidgetExtensionBuilder.Kind("steps")
                        .setIosFamilies(Arrays.asList("small", "accessoryCorner"))));
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

    private static IOSWidgetExtensionBuilder builderFor(String... families) {
        return new IOSWidgetExtensionBuilder()
                .setHostBundleId("com.mycompany.myapp")
                .setAppGroupId("group.com.mycompany.myapp")
                .addKind(new IOSWidgetExtensionBuilder.Kind("steps")
                        .setName("Steps")
                        .setIosFamilies(Arrays.asList(families)));
    }

    private static String bundleFor(String... families) throws IOException {
        Map<String, byte[]> files = builderFor(families).buildFileMap();
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            if (e.getKey().endsWith("CN1WidgetBundle.swift")) {
                return new String(e.getValue(), StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("The generated widget bundle was not produced: " + files.keySet());
    }
}
