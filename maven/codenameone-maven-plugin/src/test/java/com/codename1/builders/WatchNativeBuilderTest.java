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
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pins the watch build's contract with the project: declaring a watch lifecycle
/// class is the entire opt-in, everything else is derived, and a project that
/// declares none must be left completely alone. The wearable build deliberately
/// carries no build hints, so these tests also guard against re-introducing one
/// by accident.
class WatchNativeBuilderTest {

    private static final String WATCH_MAIN = "com.mycompany.myapp.MyWatchMain";

    // ------------------------------------------------------------------
    // Enablement
    // ------------------------------------------------------------------

    @Test
    void projectWithoutAWatchMainBuildsNoWatchApp() {
        WatchNativeBuilder b = parse(request());
        assertFalse(b.isEnabled(),
                "A project that declares no watch lifecycle class must leave the iOS build untouched");
    }

    @Test
    void declaringAWatchMainIsTheEntireOptIn() {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);

        WatchNativeBuilder b = parse(req);

        assertTrue(b.isEnabled());
        assertEquals(WATCH_MAIN, b.getWatchMain());
    }

    @Test
    void retiredEnablementHintsAreIgnored() {
        // These named the old hint surface. Nothing may resurrect the watch
        // build without a watch lifecycle class to root it at.
        BuildRequest req = request();
        req.putArgument("watchNative.enabled", "true");
        req.putArgument("watchNative.mainClass", WATCH_MAIN);

        assertFalse(parse(req).isEnabled());
    }

    @Test
    void blankWatchMainBuildsNoWatchApp() {
        BuildRequest req = request();
        req.putArgument("watchMain", "   ");

        assertFalse(parse(req).isEnabled());
    }

    // ------------------------------------------------------------------
    // Distribution
    // ------------------------------------------------------------------

    @Test
    void watchAppIsACompanionByDefault() {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);

        assertFalse(parse(req).isStandalone());
    }

    @Test
    void watchStandaloneMakesTheWatchAppTheProduct() {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("watchStandalone", "true");

        assertTrue(parse(req).isStandalone());
    }

    // ------------------------------------------------------------------
    // Info.plist
    // ------------------------------------------------------------------

    @Test
    void companionPlistPinsTheWatchAppToThePhoneApp(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);

        String plist = writeInfoPlist(req, tmp);

        assertTrue(plist.contains("<key>WKApplication</key>"),
                "Modern single-target watch apps are marked with WKApplication");
        assertTrue(plist.contains("<key>WKCompanionAppBundleIdentifier</key>"),
                "A companion watch app installs with the phone app it names");
        assertTrue(plist.contains("com.mycompany.myapp"));
    }

    @Test
    void standalonePlistNamesNoCompanion(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("watchStandalone", "true");

        String plist = writeInfoPlist(req, tmp);

        assertTrue(plist.contains("<key>WKApplication</key>"));
        assertFalse(plist.contains("WKCompanionAppBundleIdentifier"),
                "A standalone watch app has no phone app to pair with");
    }

    @Test
    void plistUsesTheProjectDisplayNameAndVersion(@TempDir Path tmp) throws IOException {
        // Derived rather than configured: the watch app name and version come
        // from the settings the project already has.
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);

        String plist = writeInfoPlist(req, tmp);

        assertTrue(plist.contains("<string>My App</string>"));
        assertTrue(plist.contains("<string>2.5</string>"));
    }

    // ------------------------------------------------------------------
    // Bundle versions
    // ------------------------------------------------------------------

    /// Apple rejects an archive whose embedded watch app disagrees with its container on either
    /// version key, and companion mode embeds by default -- so a hardcoded "1" here would fail
    /// distribution for every project that sets a version at all.
    @Test
    void watchVersionsFollowThePhone(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);

        String plist = writeInfoPlist(req, tmp);

        assertTrue(plist.contains("<key>CFBundleShortVersionString</key>\n    <string>2.5</string>"),
                "The marketing version is the project's, not a placeholder: " + plist);
        assertTrue(plist.contains("<key>CFBundleVersion</key>\n    <string>2.5</string>"),
                "CFBundleVersion defaults to the same value the phone plist uses: " + plist);
        assertFalse(plist.contains("<key>CFBundleVersion</key>\n    <string>1</string>"));
    }

    @Test
    void explicitBundleVersionOverrideIsHonoured(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.bundleVersion", "417");

        String plist = writeInfoPlist(req, tmp);

        assertTrue(plist.contains("<key>CFBundleVersion</key>\n    <string>417</string>"),
                "ios.bundleVersion drives both halves of the pair: " + plist);
        assertTrue(plist.contains("<key>CFBundleShortVersionString</key>\n    <string>2.5</string>"),
                "The override is the build number only, as on the phone: " + plist);
    }

    /// The phone reformats its version when ios.twoDigitVersion is set, so the watch has to apply the
    /// same transformation or the two disagree digit for digit.
    @Test
    void twoDigitVersionMatchesThePhoneReformatting(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.twoDigitVersion", "true");

        String plist = writeInfoPlist(req, tmp);

        assertTrue(plist.contains("<string>2.50</string>"),
                "2.5 becomes 2.50 exactly as IPhoneBuilder derives it: " + plist);
    }

    // ------------------------------------------------------------------
    // Generated entry point
    // ------------------------------------------------------------------

    @Test
    void watchEntryPointIsGenerated(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        WatchNativeBuilder b = parse(req);

        File dir = tmp.toFile();
        b.writeWatchEntry(req, dir);

        String swift = read(new File(dir, "CN1WatchApp.swift"));
        assertTrue(swift.contains("@main"), "The watch app is rooted in a SwiftUI @main shell");
        assertTrue(swift.contains("#if os(watchOS)"),
                "The shell is globbed into the iOS target too, so it must compile away there");
        assertTrue(swift.contains("digitalCrownRotation"));

        String bootstrap = read(new File(dir, "CN1WatchBootstrap.m"));
        assertTrue(bootstrap.contains("#if TARGET_OS_WATCH"));
        assertTrue(bootstrap.contains("cn1_watch_app_main"));
        // The declared class reaches cn1_watch_bootstrap, but note what this does NOT assert: the
        // runtime does not yet root the app at it (cn1_watch_runtime_start discards the argument and
        // cn1_watch_app_main enters the phone's Stub.main). Rooting a second translation at watchMain
        // is scoped separately; see the "What the watch app runs today" section of the guide.
        assertTrue(bootstrap.contains(WATCH_MAIN),
                "The declared watch lifecycle class is passed to the watch runtime");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static BuildRequest request() {
        BuildRequest req = new BuildRequest();
        req.setMainClass("MyApp");
        req.setPackageName("com.mycompany.myapp");
        req.setDisplayName("My App");
        req.setVersion("2.5");
        return req;
    }

    private static WatchNativeBuilder parse(BuildRequest req) {
        WatchNativeBuilder b = new WatchNativeBuilder(new IPhoneBuilder());
        b.parseHints(req);
        return b;
    }

    private static String writeInfoPlist(BuildRequest req, Path tmp) throws IOException {
        WatchNativeBuilder b = parse(req);
        File dir = tmp.toFile();
        b.writeWatchInfoPlist(req, dir);
        return read(new File(dir, req.getMainClass() + "-Watch-Info.plist"));
    }

    private static String read(File f) throws IOException {
        if (!f.exists()) {
            throw new AssertionError("Expected generated file was not written: " + f);
        }
        return new String(Files.readAllBytes(f.toPath()));
    }
}
