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
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The watch flavour of the extension. It is a second target in the same project, embedded in
/// the watch app rather than the phone app, and it may name a different set of WidgetKit
/// families than the iOS one -- narrower in one direction and wider in the other.
///
/// The sibling IOSWidgetExtensionWatchFamilyTest pins the same separation from the iOS side.
class IOSWidgetExtensionWatchTargetTest {

    private static IOSWidgetExtensionBuilder watchBuilder(String... families) {
        return new IOSWidgetExtensionBuilder()
                .setWatchTarget(true)
                .setExtensionName("CN1WatchWidgets")
                .setHostBundleId("com.example.app.watchkitapp")
                .setAppGroupId("group.com.example.app")
                .addKind(new IOSWidgetExtensionBuilder.Kind("status")
                        .setName("Status")
                        .setDescription("d")
                        .setIosFamilies(Arrays.asList(families)));
    }

    private static String bundleOf(IOSWidgetExtensionBuilder b) throws IOException {
        return new String(b.buildFileMap().get("CN1WidgetBundle.swift"), StandardCharsets.UTF_8);
    }

    private static String settingsOf(IOSWidgetExtensionBuilder b) throws IOException {
        return new String(b.buildFileMap().get("buildSettings.properties"), StandardCharsets.UTF_8);
    }

    private static String plistOf(IOSWidgetExtensionBuilder b) throws IOException {
        return new String(b.buildFileMap().get("Info.plist"), StandardCharsets.UTF_8);
    }

    /// Apple validates an embedded bundle's versions against the app containing it, and this
    /// extension is nested two deep -- inside the watch app, inside the phone app. Pinned to
    /// 1.0/1 it was rejected at submission for every project on any other version, which is the
    /// one failure that appears after every build has already gone green.
    @Test
    void theExtensionDeclaresTheVersionsItIsToldTo() throws IOException {
        String plist = plistOf(watchBuilder("watchCircular").setVersions("3.7", "412"));

        assertTrue(plist.contains("<string>3.7</string>"), plist);
        assertTrue(plist.contains("<string>412</string>"), plist);
        assertFalse(plist.contains("<key>CFBundleShortVersionString</key>\n    <string>1.0</string>"),
                plist);
    }

    /// A caller that says nothing keeps the historical output, so this cannot change what an
    /// existing build emits on its own.
    @Test
    void theVersionsFallBackToWhatWasAlwaysEmitted() throws IOException {
        String plist = plistOf(watchBuilder("watchCircular"));

        assertTrue(plist.contains("<string>1.0</string>"), plist);
        assertTrue(plist.contains("<string>1</string>"), plist);
    }

    /// An empty resolution must not blank the key -- a plist with an empty version string is
    /// worse than one with the default.
    @Test
    void anEmptyVersionIsIgnoredRatherThanWritten() throws IOException {
        String plist = plistOf(watchBuilder("watchCircular").setVersions("", null));

        assertTrue(plist.contains("<string>1.0</string>"), plist);
        assertTrue(plist.contains("<string>1</string>"), plist);
    }

    /// The regression test for the hole this flavour was built around. WidgetFamily.systemSmall
    /// and its siblings are @available(watchOS, unavailable) -- unnameable, not merely absent --
    /// so a phone family reaching the watch bundle fails the build outright.
    @Test
    void systemFamiliesNeverReachTheWatchBundle() throws IOException {
        String swift = bundleOf(watchBuilder("small", "medium", "large", "watchCircular"));

        assertFalse(swift.contains(".systemSmall"), swift);
        assertFalse(swift.contains(".systemMedium"), swift);
        assertFalse(swift.contains(".systemLarge"), swift);
        assertTrue(swift.contains(".accessoryCircular"), swift);
    }

    /// An iPhone lock screen is not a watch face, so the portable lockscreen family has no
    /// surface here either -- even though it maps to an accessory family that watchOS does have.
    @Test
    void lockscreenIsNotAWatchFamily() throws IOException {
        String swift = bundleOf(watchBuilder("lockscreen", "watchInline"));

        assertTrue(swift.contains(".accessoryInline"), swift);
        assertFalse(swift.contains(".accessoryRectangular"), swift);
    }

    /// Inside a watchOS-only target the corner family needs no platform guard; carrying one
    /// would be noise in a file that can only ever be compiled for the watch.
    @Test
    void cornerFamilyNeedsNoPlatformGuardInTheWatchTarget() throws IOException {
        String swift = bundleOf(watchBuilder("watchCorner", "watchCircular"));

        assertTrue(swift.contains(".accessoryCorner"), swift);
        assertFalse(swift.contains("#if os(watchOS)"), swift);
    }

    @Test
    void aKindWithNoWatchFamilyIsNotHosted() throws IOException {
        IOSWidgetExtensionBuilder b = new IOSWidgetExtensionBuilder()
                .setWatchTarget(true)
                .setExtensionName("CN1WatchWidgets")
                .setHostBundleId("com.example.app.watchkitapp")
                .setAppGroupId("group.com.example.app")
                .addKind(new IOSWidgetExtensionBuilder.Kind("phone")
                        .setIosFamilies(Arrays.asList("small")))
                .addKind(new IOSWidgetExtensionBuilder.Kind("wrist")
                        .setIosFamilies(Arrays.asList("watchCircular")));

        String swift = bundleOf(b);

        assertTrue(swift.contains("CN1Widget_wrist"), swift);
        assertFalse(swift.contains("CN1Widget_phone"), swift);
    }

    @Test
    void watchOnlyManifestHasAWatchSurfaceButNoIosOne() {
        IOSWidgetExtensionBuilder b = watchBuilder("watchCircular");

        assertTrue(b.hasWatchSurface());
        assertFalse(b.hasIosSurface());
        assertTrue(b.hasSurface());
    }

    @Test
    void phoneOnlyManifestHasNoWatchSurface() {
        IOSWidgetExtensionBuilder b = watchBuilder("small", "medium");

        assertFalse(b.hasWatchSurface());
        assertFalse(b.hasSurface());
    }

    /// Generating anyway must fail loudly rather than emit a WidgetBundle with an empty body,
    /// which does not compile and would break the whole watch build.
    @Test
    void generatingAWatchExtensionWithNoComplicationIsRefused() {
        final IOSWidgetExtensionBuilder b = watchBuilder("small");

        assertThrows(IllegalStateException.class, new Executable() {
            public void execute() throws Throwable {
                b.buildFileMap();
            }
        });
    }

    @Test
    void buildSettingsDescribeAWatchTargetAndNotAPhoneOne() throws IOException {
        String props = settingsOf(watchBuilder("watchCircular"));

        assertTrue(props.contains("WATCHOS_DEPLOYMENT_TARGET=10.0"), props);
        assertTrue(props.contains("SDKROOT=watchos"), props);
        assertTrue(props.contains("SUPPORTED_PLATFORMS=watchos watchsimulator"), props);
        assertTrue(props.contains("TARGETED_DEVICE_FAMILY=4"), props);
        assertTrue(props.contains("ARCHS[sdk=watchos*]=arm64_32"), props);
        assertFalse(props.contains("IPHONEOS_DEPLOYMENT_TARGET"), props);
    }

    /// The watch app embeds the Swift runtime once for everything nested inside it. A second
    /// copy in the extension is dead weight and can fail submission validation.
    @Test
    void theNestedExtensionDoesNotEmbedItsOwnSwiftRuntime() throws IOException {
        assertTrue(settingsOf(watchBuilder("watchCircular"))
                .contains("ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES=NO"));
        assertTrue(settingsOf(new IOSWidgetExtensionBuilder()
                .setExtensionName("CN1Widgets")
                .setHostBundleId("com.example.app")
                .setAppGroupId("group.com.example.app")
                .addKind(new IOSWidgetExtensionBuilder.Kind("k")
                        .setIosFamilies(Arrays.asList("small"))))
                .contains("ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES=YES"),
                "the iOS extension is not nested and keeps its own copy");
    }

    /// watchOS has no ActivityKit, so neither the live activity widget nor the attributes it
    /// shares with the app belong in a watch target -- even asking for them.
    @Test
    void liveActivitySourcesAreNeverShippedToTheWatch() throws IOException {
        Map<String, byte[]> files = watchBuilder("watchCircular")
                .setLiveActivitiesEnabled(true)
                .buildFileMap();

        assertFalse(files.containsKey("CN1LiveActivityWidget.swift"), files.keySet().toString());
        assertFalse(files.containsKey("CN1SurfaceAttributes.swift"), files.keySet().toString());
        assertFalse(new String(files.get("CN1WidgetBundle.swift"), StandardCharsets.UTF_8)
                .contains("CN1LiveActivityWidget()"));
    }

    /// containerBackground(for:) is watchOS 10, and every generated widget applies it. Below
    /// that floor the availability check around it stops compiling, so a lower target does not
    /// merely lose the background -- it fails the build.
    @Test
    void aDeploymentTargetBelowTheWatchFloorIsRejected() {
        final IOSWidgetExtensionBuilder b = watchBuilder("watchCircular").setDeploymentTarget("9.0");

        IllegalStateException ex = assertThrows(IllegalStateException.class, new Executable() {
            public void execute() throws Throwable {
                b.buildFileMap();
            }
        });
        assertTrue(ex.getMessage().contains("containerBackground"), ex.getMessage());
    }

    /// "10.0" orders above "9.0" only under a numeric comparison; string order says otherwise.
    @Test
    void theFloorCheckComparesVersionsNumerically() throws IOException {
        assertEquals("10.0", watchBuilder("watchCircular").getDeploymentTarget());
        watchBuilder("watchCircular").setDeploymentTarget("11.2").buildFileMap();
    }
}
