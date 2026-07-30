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
/// map onto those families -- but only in a watch target. The generated extension is the iOS one, so
/// a complication must not surface there: a kind that asked for a complication and got an iPhone
/// lock-screen or home-screen widget is a wrong surface in front of the user, not an approximation.
class IOSWidgetExtensionWatchFamilyTest {

    @Test
    void watchOnlyKindIsNotEmittedIntoTheIosExtension() throws IOException {
        String bundle = bundleFor("watchCircular", "watchRectangular", "watchInline");

        // No struct, and therefore nothing in the bundle body: there is no watchOS extension target
        // to host it yet, and the iOS one has no matching surface.
        assertFalse(bundle.contains("CN1Widget_steps"),
                "A kind declaring only complication families has no surface in the iOS extension");
        assertFalse(bundle.contains("accessory"));
        assertFalse(bundle.contains(".systemSmall"),
                "Falling back to the home-screen sizes would ship a widget the manifest never asked for");
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
