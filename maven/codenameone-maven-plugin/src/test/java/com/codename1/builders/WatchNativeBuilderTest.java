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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    /// An embedded watch app whose versions differ from its container is rejected by App Store
    /// validation, and ios.plistInject REPLACES the phone's default version injection rather than
    /// adding to it -- so a project that sets the version there must not leave the watch behind.
    @Test
    void watchVersionsFollowPlistInject(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.plistInject",
                "<key>CFBundleShortVersionString</key><string>9.9.9</string>"
                        + "<key>CFBundleVersion</key><string>4242</string>");
        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("<string>9.9.9</string>"),
                "watch CFBundleShortVersionString must follow the injected phone value: " + plist);
        assertTrue(plist.contains("<string>4242</string>"),
                "watch CFBundleVersion must follow the injected phone value: " + plist);
    }

    /// ios.bundleVersion still applies when the injection does not name CFBundleVersion.
    @Test
    void watchVersionsFallBackWhenNotInjected(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.plistInject",
                "<key>CFBundleShortVersionString</key><string>7.7.7</string>");
        req.putArgument("ios.bundleVersion", "31");
        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("<string>7.7.7</string>"),
                "injected short version must reach the watch plist: " + plist);
        assertTrue(plist.contains("<string>31</string>"),
                "ios.bundleVersion must still win for CFBundleVersion: " + plist);
    }

    /// The two version keys are independent. An injected marketing version must NOT become the
    /// watch's CFBundleVersion, because the phone's still comes from the build version -- deriving
    /// it from the injected string reintroduces the mismatch as phone 1.0 against watch 9.9.
    @Test
    void injectedShortVersionDoesNotBecomeTheBundleVersion(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.plistInject",
                "<key>CFBundleShortVersionString</key><string>9.9</string>");
        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("<key>CFBundleShortVersionString</key>\n    <string>9.9</string>"),
                "injected short version must reach the watch plist: " + plist);
        assertFalse(plist.contains("<key>CFBundleVersion</key>\n    <string>9.9</string>"),
                "CFBundleVersion must not follow the injected marketing version: " + plist);
    }

    /// A standalone bundle has to declare itself watch-only; omitting the companion key is not the
    /// same statement, and the difference shows up at install and App Store validation.
    @Test
    void standaloneWatchAppIsMarkedWatchOnly(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("watchStandalone", "true");
        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("<key>WKWatchOnly</key>"),
                "a standalone watch app must declare WKWatchOnly: " + plist);
        assertFalse(plist.contains("WKCompanionAppBundleIdentifier"),
                "a standalone watch app must not name a companion: " + plist);
    }

    /// And the companion build must NOT claim to be watch-only.
    @Test
    void companionWatchAppIsNotWatchOnly(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        String plist = writeInfoPlist(req, tmp);
        assertFalse(plist.contains("<key>WKWatchOnly</key>"),
                "a companion watch app must not declare WKWatchOnly: " + plist);
        assertTrue(plist.contains("WKCompanionAppBundleIdentifier"),
                "a companion watch app must name its container: " + plist);
    }

    /// The watch team must follow ios.buildType exactly as the phone's does. Pairing a debug
    /// profile with the release team's DEVELOPMENT_TEAM fails manual signing of the embedded target.
    @Test
    void watchTeamFollowsBuildType() {
        BuildRequest debug = request();
        debug.putArgument("watchMain", WATCH_MAIN);
        debug.putArgument("ios.debug.teamId", "DEBUGTEAM");
        debug.putArgument("ios.release.teamId", "RELTEAM");
        debug.putArgument("ios.buildType", "debug");
        assertEquals("DEBUGTEAM", parse(debug).getTeamId(),
                "a debug build must use the debug team");

        BuildRequest release = request();
        release.putArgument("watchMain", WATCH_MAIN);
        release.putArgument("ios.debug.teamId", "DEBUGTEAM");
        release.putArgument("ios.release.teamId", "RELTEAM");
        release.putArgument("ios.buildType", "release");
        assertEquals("RELTEAM", parse(release).getTeamId(),
                "a release build must use the release team");

        BuildRequest plain = request();
        plain.putArgument("watchMain", WATCH_MAIN);
        plain.putArgument("ios.teamId", "PLAINTEAM");
        assertEquals("PLAINTEAM", parse(plain).getTeamId(),
                "ios.teamId remains the fallback for both");
    }

    /// The watch bundle needs its OWN purpose strings: the phone's plist does not cover an API the
    /// watch app exercises, and watchOS terminates the app rather than merely refusing access.
    @Test
    void watchPlistCarriesEveryPrivacyDescription(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.NSLocationWhenInUseUsageDescription", "Shows nearby stops");
        req.putArgument("ios.NSMicrophoneUsageDescription", "Records a voice note");
        req.putArgument("ios.NSHealthShareUsageDescription", "Reads your heart rate");
        req.putArgument("ios.NSMotionUsageDescription", "   ");
        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("<key>NSLocationWhenInUseUsageDescription</key>"),
                "location description must reach the watch plist: " + plist);
        assertTrue(plist.contains("<key>NSMicrophoneUsageDescription</key>"),
                "microphone description must reach the watch plist: " + plist);
        assertTrue(plist.contains("<key>NSHealthShareUsageDescription</key>"),
                "the HealthKit pair must still be carried: " + plist);
        assertFalse(plist.contains("NSMotionUsageDescription"),
                "a whitespace-only description is absent, not blank: " + plist);
    }

    /// ios.locationUsageDescription is a supported hint the phone translates into an NS key later,
    /// after this plist is written -- so the watch has to translate it itself or ship without one.
    @Test
    void watchPlistTranslatesTheLocationFallback(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.locationUsageDescription", "Finds nearby stops");
        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("<key>NSLocationWhenInUseUsageDescription</key>"),
                "the location fallback must become an NS key in the watch plist: " + plist);
        assertTrue(plist.contains("Finds nearby stops"), plist);
    }

    /// And an explicit NS key wins, rather than being emitted twice.
    @Test
    void explicitLocationKeyIsNotDuplicatedByTheFallback(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.locationUsageDescription", "fallback text");
        req.putArgument("ios.NSLocationWhenInUseUsageDescription", "explicit text");
        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("explicit text"), plist);
        assertFalse(plist.contains("fallback text"),
                "the explicit key wins and the fallback is not also emitted: " + plist);
    }

    /// ios.plistInject is a supported way to set a purpose string, and it is a raw fragment rather
    /// than an argument -- so a loop over ios.NS* never sees it and the watch ships without one.
    @Test
    void watchPlistCarriesInjectedPrivacyStrings(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.plistInject",
                "<key>NSMicrophoneUsageDescription</key><string>Records a note</string>"
                        + "<key>UIRequiresFullScreen</key><true/>");
        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("<key>NSMicrophoneUsageDescription</key>"),
                "an injected purpose string must reach the watch plist: " + plist);
        assertTrue(plist.contains("Records a note"), plist);
        assertFalse(plist.contains("UIRequiresFullScreen"),
                "only privacy keys are mirrored, not the whole fragment: " + plist);
    }

    /// An injected NSLocation key must suppress the ios.locationUsageDescription fallback, or the
    /// plist carries the key twice and a default overwrites the developer's own disclosure.
    @Test
    void injectedLocationKeySuppressesTheFallback(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.plistInject",
                "<key>NSLocationWhenInUseUsageDescription</key><string>injected text</string>");
        req.putArgument("ios.locationUsageDescription", "fallback text");
        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("injected text"), plist);
        assertFalse(plist.contains("fallback text"),
                "the injected key wins over the fallback: " + plist);
        assertEquals(1, countOccurrences(plist, "<key>NSLocationWhenInUseUsageDescription</key>"),
                "the key must appear exactly once: " + plist);
    }

    /// A HealthKit purpose string supplied through the injection satisfies the validation that
    /// otherwise aborts the build -- it used to read arguments only and reject its own plist.
    @Test
    void injectedHealthDescriptionSatisfiesValidation(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("watchNative.health", "true");
        req.putArgument("ios.plistInject",
                "<key>NSHealthShareUsageDescription</key><string>Reads heart rate</string>"
                        + "<key>NSHealthUpdateUsageDescription</key><string>Saves workouts</string>");
        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("Reads heart rate"),
                "the injected HealthKit string must reach the plist: " + plist);
    }

    /// A value read out of ios.plistInject is serialized text, so re-emitting it through the
    /// escaper without decoding first shows the entity literally in the permission dialog.
    @Test
    void injectedEntitiesAreNotDoubleEscaped(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.plistInject", "<key>NSHealthShareUsageDescription</key>"
                + "<string>Uses Health &amp; Fitness data</string>");
        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("Uses Health &amp; Fitness data"),
                "the ampersand must be escaped exactly once: " + plist);
        assertFalse(plist.contains("&amp;amp;"),
                "the entity must not be escaped a second time: " + plist);
    }

    /// CDATA is not an entity, so the entity decoder leaves it exactly as written. Read as text it
    /// reaches the watch plist as literal markup, while an XML parser reading the phone's plist
    /// resolves it -- the two bundles then disagree about a version string or a purpose string.
    @Test
    void injectedCdataIsResolvedNotEmitted(@TempDir Path tmp) throws IOException {
        assertEquals("1.2", WatchNativeBuilder.plistStringContent("<![CDATA[1.2]]>"));
        assertEquals("Health & Fitness",
                WatchNativeBuilder.plistStringContent("<![CDATA[Health & Fitness]]>"),
                "inside CDATA an ampersand is data, not the start of a reference");
        assertEquals("a & b",
                WatchNativeBuilder.plistStringContent("a &amp; <![CDATA[b]]>"),
                "entities outside a CDATA section are still decoded");
        assertEquals("a &amp; b",
                WatchNativeBuilder.plistStringContent("<![CDATA[a &amp; b]]>"),
                "and entities inside one are not");
        assertEquals("</string>",
                WatchNativeBuilder.plistStringContent("<![CDATA[</string>]]>"),
                "an end tag inside CDATA is data");

        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.plistInject", "<key>NSHealthShareUsageDescription</key>"
                + "<string><![CDATA[Health & Fitness]]></string>");
        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("Uses Health &amp; Fitness data")
                        || plist.contains("Health &amp; Fitness"),
                "the CDATA content must be escaped once, as text: " + plist);
        assertFalse(plist.contains("CDATA"),
                "the CDATA markup itself must not reach the watch plist: " + plist);
    }

    /// Numeric references are ordinary XML, and a single left-to-right pass must not decode its
    /// own output -- "&amp;#38;" is an author writing a literal "&#38;", not an escaped ampersand.
    @Test
    void injectedNumericReferencesAreDecodedOnce(@TempDir Path tmp) throws IOException {
        assertEquals("Health & Fitness",
                WatchNativeBuilder.decodeXmlEntities("Health &#38; Fitness"));
        assertEquals("Health & Fitness",
                WatchNativeBuilder.decodeXmlEntities("Health &#x26; Fitness"));
        assertEquals("Health & Fitness",
                WatchNativeBuilder.decodeXmlEntities("Health &amp; Fitness"));
        assertEquals("a &#38; b", WatchNativeBuilder.decodeXmlEntities("a &amp;#38; b"),
                "a literal reference must survive, not be decoded a second time");
        assertEquals("100% & more", WatchNativeBuilder.decodeXmlEntities("100% & more"),
                "a bare ampersand is left exactly as written");

        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.plistInject", "<key>NSHealthShareUsageDescription</key>"
                + "<string>Health &#38; Fitness</string>");
        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("Health &amp; Fitness"), plist);
        assertFalse(plist.contains("&amp;#38;"), plist);
    }

    /// The plist pass and the code-signing setting must reach the SAME HealthKit verdict. They
    /// used to resolve it from different inputs, so a purpose string supplied only through
    /// ios.plistInject produced a bundle that declared HealthKit and was signed without the
    /// entitlement -- authorization then fails on device. Detected usage is the single source of
    /// truth, matching the BuildDaemon mirror, so a stale privacy string entitles nothing.
    @Test
    void plistAndEntitlementsAgreeOnHealth(@TempDir Path tmp) throws IOException {
        BuildRequest stale = request();
        stale.putArgument("watchMain", WATCH_MAIN);
        stale.putArgument("ios.plistInject", "<key>NSHealthShareUsageDescription</key>"
                + "<string>Reads your heart rate</string>");
        String stalePlist = writeInfoPlist(stale, tmp);
        assertTrue(stalePlist.contains("NSHealthShareUsageDescription"),
                "the description is still carried -- it is the ENTITLEMENT that needs evidence");
        assertFalse(stalePlist.contains("com.apple.developer.healthkit"), stalePlist);
        assertEquals("", parse(stale).watchEntitlementsSetting(stale, stale.getMainClass()),
                "a privacy string alone must not sign the watch target with HealthKit");

        BuildRequest declared = request();
        declared.putArgument("watchMain", WATCH_MAIN);
        declared.putArgument("watchNative.health", "true");
        declared.putArgument("ios.NSHealthShareUsageDescription", "Reads your heart rate");
        assertTrue(parse(declared).watchEntitlementsSetting(declared, declared.getMainClass())
                        .contains("CODE_SIGN_ENTITLEMENTS"),
                "declared health usage must sign the watch target with the entitlements file");
    }

    /// A project whose health access lives in native code declares it through the capability
    /// hints, not through anything the bytecode scan can see. The phone builder has always treated
    /// those hints as HealthKit use; when the watch read the scanner flags alone the same app --
    /// running the SAME lifecycle class on both slices -- got an entitled phone and an unentitled
    /// watch, and only the watch failed authorization.
    @Test
    void explicitHealthCapabilitiesEntitleTheSharedLifecycleWatchToo() {
        for (String hint : new String[] {
                "ios.health.backgroundDelivery",
                "ios.health.recalibrateEstimates",
                "ios.entitlements.com.apple.developer.healthkit.background-delivery",
                "ios.entitlements.com.apple.developer.healthkit.recalibrate-estimates",
                // The plainest declaration of all, and the one the sub-capability list missed.
                "ios.entitlements.com.apple.developer.healthkit"}) {
            BuildRequest req = request();
            req.putArgument("watchMain", "com.mycompany.myapp.MyApp");
            req.putArgument(hint, "true");
            assertTrue(parse(req).watchEntitlementsSetting(req, req.getMainClass())
                            .contains("CODE_SIGN_ENTITLEMENTS"),
                    hint + " must entitle the watch target as it does the phone");

            // A distinct watchMain is entitled the same way. This used to be the opposite -- the
            // watch was judged against its own translation root by a class walk -- and that walk
            // is gone: it protected against a compile failure that happens regardless of what any
            // stub names. The declaration is app-wide, so it entitles the app, and a watch that
            // should NOT carry HealthKit says so with watchNative.health=false.
            BuildRequest distinct = request();
            distinct.putArgument("watchMain", WATCH_MAIN);
            distinct.putArgument(hint, "true");
            assertTrue(parse(distinct).watchEntitlementsSetting(distinct, distinct.getMainClass())
                            .contains("CODE_SIGN_ENTITLEMENTS"),
                    hint + " entitles the watch as it does the phone");

            BuildRequest optedOut = request();
            optedOut.putArgument("watchMain", WATCH_MAIN);
            optedOut.putArgument(hint, "true");
            optedOut.putArgument("watchNative.health", "false");
            assertEquals("",
                    parse(optedOut).watchEntitlementsSetting(optedOut, optedOut.getMainClass()),
                    "watchNative.health=false is how a watch opts out of " + hint);
        }
        BuildRequest none = request();
        none.putArgument("watchMain", "com.mycompany.myapp.MyApp");
        assertEquals("", parse(none).watchEntitlementsSetting(none, none.getMainClass()),
                "a project that asks for nothing is still not entitled");
    }

    /// "false" is the established opt-out for a privacy hint -- the phone's generic injector skips
    /// a usage description with exactly that value. The watch treated it as an ordinary string and
    /// would have shown the literal word in a watchOS permission prompt.
    @Test
    void falseSuppressesAPurposeStringAsItDoesOnThePhone(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.NSMicrophoneUsageDescription", "false");
        req.putArgument("ios.plistInject", "<key>NSCameraUsageDescription</key><string>false</string>"
                + "<key>NSMotionUsageDescription</key><string>Counts your steps</string>");
        String plist = writeInfoPlist(req, tmp);
        assertFalse(plist.contains("NSMicrophoneUsageDescription"),
                "an opted-out argument must not reach the watch plist: " + plist);
        assertFalse(plist.contains("NSCameraUsageDescription"),
                "an opted-out injected key must not reach the watch plist: " + plist);
        assertFalse(plist.contains("<string>false</string>"), plist);
        assertTrue(plist.contains("Counts your steps"),
                "an ordinary description is unaffected: " + plist);
    }

    private static int countOccurrences(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static WatchNativeBuilder parse(BuildRequest req) {
        WatchNativeBuilder b = new WatchNativeBuilder(new IPhoneBuilder());
        b.parseHints(req);
        return b;
    }

    /// A watch that boots its OWN class must compile its OWN translation.
    ///
    /// Sharing the phone's tree is what made watchMain not really an entry point: the watch binary
    /// carried everything the PHONE reaches, and the phone stub's main had to be defined away to
    /// stop the two colliding. So the generated project must reference the staged watch sources,
    /// must not copy the app target's, and must not carry the -Dmain neutraliser.
    @Test
    void aDistinctWatchMainCompilesItsOwnTranslation(@TempDir Path tmp) throws Exception {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        WatchNativeBuilder b = parse(req);
        assertTrue(b.needsOwnTranslation(),
                "a watch lifecycle class distinct from the phone's needs its own translation");

        String ruby = b.buildXcodeScript(req, tmp.toFile(), "1.0",
                java.util.Arrays.asList("MyAppWatchStub.m", "java_lang_String.m"));
        assertTrue(ruby.contains("MyAppWatchStub.m"), ruby);
        // The FULL path, relative to the project. Asserting only "watch-src" passed while the
        // reference pointed at a directory that does not exist -- and since most translated files
        // share a basename with the phone's, Xcode resolved those against the phone tree and
        // compiled the wrong sources, with only the watch-only names failing.
        assertTrue(ruby.contains("MyApp-src/watch-src"),
                "the staged tree is addressed from the project root: " + ruby);
        // The watch slice must compile against ITS OWN prefix header. The phone's pch includes
        // "cn1_class_method_index.h" relative to its own directory, so using it made every watch
        // source see the phone's class index -- which does not declare the watch stub's id.
        assertTrue(ruby.contains("GCC_PREFIX_HEADER"),
                "the watch target needs its own prefix header: " + ruby);
        assertTrue(ruby.contains("watch-src/MyAppWatch-Prefix.pch"), ruby);
        // The stub's C main is defined away in BOTH modes -- the watch app is SwiftUI-rooted, so
        // Swift's @main is the entry and a second C main is a duplicate symbol. What differs is
        // WHICH stub gets the flag: here, the watch's own.
        assertTrue(ruby.contains("-Dmain=cn1_watch_phone_main_unused"), ruby);
        assertTrue(ruby.contains("MyAppWatchStub.m"),
                "the neutraliser targets the stub this target compiles: " + ruby);
        // The app target IS walked -- for its file LIST. The watch translation's dist also holds
        // native sources the app target never compiles (UIWebViewEventDelegate.m calls
        // UIApplication, absent on watchOS), so taking every emitted .m compiled files the phone
        // build itself excludes. What must not happen is compiling the phone's translated BODIES:
        // each name present in the staged tree is swapped for its watch-src counterpart.
        // The watch target compiles the watch translation's OWN file set. The phone's list would
        // omit the watch lifecycle class, which the phone never reaches and its translation
        // therefore shakes out -- the link then fails on that class's symbols.
        assertTrue(ruby.contains("watch_sources.each"), ruby);
        // Quoted strings, not a %w[] word list: a translated source named after a class with a
        // space in it -- `My Bridge.m` -- was split into two words, so the target referenced two
        // files that do not exist and linked without the symbols the real one carries.
        assertTrue(ruby.contains("watch_sources = ['"),
                "the staged names have to be quoted: " + ruby);
        assertFalse(ruby.contains("watch_sources = %w["), ruby);
        // An asset catalog is judged by what is in it. Dropping every .xcassets left a project
        // that keeps its images in a custom catalog with none of them on the watch, so
        // UIImage(named:) returned nil at runtime with nothing in the build to say why.
        assertTrue(ruby.contains("cn1_watch_catalog_for_watch"), ruby);
        assertTrue(ruby.contains("appiconset,launchimage"), ruby);
        assertFalse(ruby.contains("res_skip = %w[.xcassets"),
                "the blanket catalog filter is what dropped the usable ones: " + ruby);
        assertTrue(ruby.contains("watch_group_path + '/' + name"), ruby);
    }

    /// The same class on both slices is ONE app, so it keeps one translation -- and there the
    /// phone stub's main still has to be defined away.
    @Test
    void asharedWatchMainKeepsTheSingleTranslation(@TempDir Path tmp) throws Exception {
        BuildRequest req = request();
        req.putArgument("watchMain", "com.mycompany.myapp.MyApp");
        WatchNativeBuilder b = parse(req);
        assertFalse(b.needsOwnTranslation(),
                "the same entry point on both slices must not be translated twice");

        String ruby = b.buildXcodeScript(req, tmp.toFile(), "1.0",
                java.util.Collections.<String>emptyList());
        assertTrue(ruby.contains("app_target.source_build_phase.files.to_a.each"),
                "the shared translation is reused: " + ruby);
        assertTrue(ruby.contains("-Dmain=cn1_watch_phone_main_unused"),
                "one binary cannot define main twice: " + ruby);
    }

    /**
     * Swift Package Manager products reach the watch target too.
     *
     * <p>A build file for a package product carries a {@code product_ref} and no {@code file_ref},
     * so the framework-mirroring loop -- which skips anything without a path -- passed straight
     * over them. A project declaring ios.spm.packages linked them into the phone and not the
     * watch, and a watch lifecycle calling into one failed at link time with undefined symbols and
     * nothing in the build naming the missing dependency.</p>
     */
    @Test
    void swiftPackageProductsReachTheWatchTarget(@TempDir Path tmp) throws Exception {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        String ruby = parse(req).buildXcodeScript(req, tmp.toFile(), "1.0",
                java.util.Collections.<String>emptyList());

        assertTrue(ruby.contains("app_target.package_product_dependencies.to_a.each"),
                "the phone's package products are the source of the mirror: " + ruby);
        // Mirrored only when a staged watch source imports the product. Copying the phone's whole
        // dependency set made Xcode resolve and build every one of them for watchOS, so an iOS-only
        // package used solely by the phone broke the watch build for code the watch never touches.
        assertTrue(ruby.contains("watch_import_text"), ruby);
        assertTrue(ruby.contains("no staged watch source imports it"),
                "a skip has to say so, or the link error that follows names nothing: " + ruby);
        // On module boundaries, not as a substring: a product Foo must not look used by a source
        // importing FooBar, which mirrored an unrelated iOS-only package onto the watch.
        assertTrue(ruby.contains("Regexp.escape(name)"), ruby);
        assertFalse(ruby.contains("watch_import_text.include?(\"import #{name}\")"),
                "the raw substring test is what matched a prefix: " + ruby);
        // And Swift's declaration-scoped form names the module just as the plain one does; reading
        // only `import Foo` dropped a product the source genuinely needs.
        assertTrue(ruby.contains("typealias|struct|class|enum|protocol|let|var|func"), ruby);
        // Any attribute, not one hard-coded name: @preconcurrency, @_implementationOnly and
        // @_spi(Name) are all valid in front of an import, and naming only @_exported classified
        // those lines as unused and dropped a module the source cannot compile without. Access-level
        // imports are the same story.
        assertTrue(ruby.contains("(?:@\\w+(?:\\([^)]*\\))?\\s*)*"),
                "the attribute prefix has to be general: " + ruby);
        assertTrue(ruby.contains("public|package|internal|fileprivate|private"), ruby);
        // A product name is not always its module name -- package FooKit can export module Foo.
        // The gate can tell when that is the case here (an import attributable to no product and no
        // watchOS framework) and steps aside rather than withholding a module the sources import.
        assertTrue(ruby.contains("strict_products"), ruby);
        assertTrue(ruby.contains("unattributed"), ruby);
        // Conditional-compilation regions: an import the Swift compiler excludes is not a watch
        // dependency, and once a watchOS arm is taken the other arms are excluded too -- a single
        // "am I suppressed" flag kept both arms of `#if os(watchOS) ... #else ... #endif`.
        assertTrue(ruby.contains("cn1_watch_strip_non_watch"), ruby);
        assertTrue(ruby.contains("decided << true") && ruby.contains("decided[i]"),
                "the taken-arm state is what drops the else of a watch-first branch: " + ruby);
        assertTrue(ruby.contains("cn1_watch_selects_watch"), ruby);
        // Selection is the only direction that can silence another arm, so it takes the whole
        // expression being demonstrably true: `os(watchOS) && FEATURE` mentions watchOS and is not
        // therefore true, and suppressing its #else dropped a package imported only there.
        assertTrue(ruby.contains("\\Aos\\(\\s*watchOS\\s*\\)\\z"),
                "only a bare watchOS test may close a branch: " + ruby);
        // A disjunction is decided operand by operand: one os() test inside an || proves nothing
        // on its own, but every operand rejecting the watch does settle it. The behaviour is
        // asserted by running the predicate in theGeneratedPredicatesDecideCorrectly; this only
        // checks the emitter still splits rather than bailing out on the first pipe it sees.
        assertTrue(ruby.contains("cn1_watch_or_operands"), ruby);
        assertFalse(ruby.contains("return false if c.include?('||')"),
                "an || no longer ends the question: " + ruby);
        // Objective-C guards its platforms with TargetConditionals, not Swift os() expressions, and
        // `#if !TARGET_OS_WATCH` around a phone-only @import is the standard spelling.
        assertTrue(ruby.contains("TARGET_OS_WATCH"), ruby);
        assertTrue(ruby.contains("TARGET_OS_\n" ) || ruby.contains("(IOS|OSX|TV|MACCATALYST|VISION)"),
                "the excluding macros have to be named: " + ruby);
        // A NEGATED platform test is the opposite answer: `!TARGET_OS_IOS` is true on the watch,
        // so that arm is the one it compiles and suppressing it dropped a package.
        assertTrue(ruby.contains("!TARGET_OS_(IOS|OSX|TV|MACCATALYST|VISION)"), ruby);
        // And `#elif` is the C spelling of `#elseif`; reading only the Swift form left an
        // Objective-C watch arm suppressed behind an excluded first arm.
        assertTrue(ruby.contains("t.start_with?('#elif')"), ruby);
        // TARGET_OS_IPHONE is 1 on watchOS, so a block guarded by it DOES compile there and must
        // not be treated as excluding.
        assertFalse(ruby.contains("IPHONE"),
                "TARGET_OS_IPHONE is true on the watch and cannot exclude it: " + ruby);
        // Only a demonstrably watchOS arm closes a branch: marking an unevaluable #elseif as
        // decided suppressed the #else, which is the arm the watch compiles when the flags are off.
        assertTrue(ruby.contains("decided[i] = true if cn1_watch_selects_watch(t)"), ruby);
        // And an import the compiler never sees is not a dependency at all.
        assertTrue(ruby.contains("cn1_watch_strip_comments"),
                "a commented-out import must not attach its package to the watch: " + ruby);
        assertTrue(ruby.contains("matches no product name"),
                "stepping aside has to be logged, or a mirrored iOS-only package looks arbitrary: "
                        + ruby);
        // Both halves are required. Listing the product on the target is what makes Xcode resolve
        // the package for it; the frameworks-phase build file is what links it. Either alone
        // produces a project that still fails, and differently.
        assertTrue(ruby.contains("watch_target.package_product_dependencies << mirrored"), ruby);
        assertTrue(ruby.contains("pbf.product_ref = mirrored"), ruby);
        assertTrue(ruby.contains("watch_target.frameworks_build_phase.files << pbf"), ruby);
        // A dependency object per target, as Xcode itself writes -- not the phone's object listed
        // under two targets.
        assertTrue(ruby.contains(
                "Xcodeproj::Project::Object::XCSwiftPackageProductDependency"), ruby);
        // Re-running the generator must not add the product twice.
        assertTrue(ruby.contains("next if watch_target.package_product_dependencies.any?"), ruby);
    }

    /**
     * A vendored framework declaring a watchOS slice reaches the watch target.
     *
     * <p>Only a system framework can be checked against the SDK, so everything referenced from a
     * group was skipped outright -- including a third-party binary that ships a perfectly good
     * watchOS slice, whose absence surfaced as undefined symbols with nothing naming what was left
     * out. The bundle's own Info.plist is what decides now.</p>
     */
    @Test
    void vendoredFrameworksDeclaringWatchosAreLinkedEmbeddedAndFound(@TempDir Path tmp)
            throws Exception {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        String ruby = parse(req).buildXcodeScript(req, tmp.toFile(), "1.0",
                java.util.Collections.<String>emptyList());

        // Both spellings of a declaration, because an .xcframework records it differently.
        assertTrue(ruby.contains("CFBundleSupportedPlatforms"), ruby);
        assertTrue(ruby.contains("AvailableLibraries"), ruby);
        assertTrue(ruby.contains("cn1_watch_bundle_supports_watchos(ref)"),
                "a non-SDKROOT reference must be judged by its own bundle: " + ruby);
        // EITHER watch variant, not both. This generator hands the developer an Xcode project
        // and cannot know which destination they will build for, so a bundle or archive carrying
        // only the device slice is usable -- requiring both rejected it and the framework phase
        // then skipped it, failing the watch target on undefined symbols. The cloud builder does
        // know its destination and asks for exactly the matching one.
        assertTrue(ruby.contains("SupportedPlatformVariant"), ruby);
        assertTrue(ruby.contains("device || simulator"), ruby);
        assertFalse(ruby.contains("device && simulator"),
                "requiring both slices is what rejected a valid device-only archive: " + ruby);
        assertTrue(ruby.contains("names.include?('watchsimulator')"), ruby);
        // Linking alone is not enough. The linker has to be told where the binary lives, and a
        // dynamic framework has to be copied into the watch bundle or it fails at install time.
        assertTrue(ruby.contains("config.build_settings['FRAMEWORK_SEARCH_PATHS'] = paths"), ruby);
        assertTrue(ruby.contains("symbol_dst_subfolder_spec == :frameworks"), ruby);
        assertTrue(ruby.contains("'CodeSignOnCopy'"), ruby);
        // Whether to embed is READ from the phone target rather than decided here: if the app
        // embeds it, it is dynamic. A static framework must not be copied.
        assertTrue(ruby.contains("app_target.copy_files_build_phases"), ruby);
        // And a project with no vendored framework must produce the project it did before.
        assertTrue(ruby.contains("if vendored_linked"), ruby);
        // A developer's own static archive is judged on what is in it, not excluded outright:
        // arm64_32 and armv7k exist on watchOS and nowhere else, so either is unambiguous proof.
        // Skipping every .a left the watch compiling the caller and failing on its symbols.
        // The declaration, not a probe of the SDK. Every framework watchOS lacks is named in
        // WATCH_OPTIONAL_FRAMEWORKS -- the same list ParparVM weak-links -- and the port's sources
        // are #ifdef'd around them, which is how conditional system libraries work on every other
        // platform here. Probing the SDK directories asked which destination the build targets, a
        // question a generated project should not depend on.
        // A VENDORED raw library is inspected, not assumed. The declared list names what the
        // PORT links, so it says nothing about a developer's own dylib -- accepting one because
        // its basename happened not to be in the list weak-linked an iOS-only binary into the
        // watch target, and weak linkage does not save a platform mismatch at link time.
        assertTrue(ruby.contains("cn1_watch_tbd_supports_watchos"), ruby);
        assertTrue(ruby.contains("elsif ref.source_tree == 'SDKROOT'"),
                "a raw library from the SDK is declared, a vendored one is inspected: " + ruby);
        assertTrue(ruby.contains("watch_unavailable = %w["), ruby);
        // Downcased in the script -- IPhoneBuilder spells one of these "JavascriptCore.framework",
        // which only resolves because macOS is case-insensitive, so the match is too.
        assertTrue(ruby.contains("opengles.framework") && ruby.contains("carplay.framework"),
                "the declared list has to reach the script: " + ruby);
        assertTrue(ruby.contains("present = !watch_unavailable.include?(base.downcase)"),
                "casing must not decide whether a framework reaches the watch link: " + ruby);
        // The three ByteCodeTranslator puts in every project's link phase that watchOS does not
        // have. Their headers are already #if !TARGET_OS_WATCH guarded, so the watch target
        // compiles and then fails at the link with "framework 'SystemConfiguration' not found" --
        // an absent framework cannot be weak-linked, only left out.
        // Every framework watchOS lacks that this builder can put on the app target. Vision and
        // the rest of the conditional ones are added by IPhoneBuilder's API scan, so they only
        // reach the watch link in a project that uses the feature -- which is why they outlived
        // the first audit of the translator's base list.
        for (String absent : new String[] {"systemconfiguration.framework",
                "audiotoolbox.framework", "quicklook.framework", "vision.framework",
                "coreimage.framework", "corenfc.framework", "coretelephony.framework",
                "javascriptcore.framework", "adsupport.framework"}) {
            assertTrue(ruby.contains(absent),
                    absent + " is absent on watchOS and must not reach the watch link phase: "
                            + ruby);
        }
        assertTrue(ruby.contains("present = !watch_unavailable.include?(base.downcase)"), ruby);
        assertFalse(ruby.contains("watch_fw_dirs"),
                "the SDK directory probe is gone: " + ruby);
        assertTrue(ruby.contains("cn1_watch_archive_has_watch_slice(ref)"), ruby);
        assertTrue(ruby.contains("arm64_32") && ruby.contains("armv7k"), ruby);
        assertTrue(ruby.contains("base.end_with?('.a')"), ruby);
    }

    /**
     * Readiness is not published from the bootstrap, because that code cannot run.
     *
     * <p>The stub's main reaches Display.init -> postInit -> IOSNative.initVM, whose watch branch
     * blocks its thread forever exactly as UIApplicationMain does on the phone. A readiness call
     * placed after the stub's main was dead code, so the flag stayed false and every background and
     * foreground transition queued for ever -- which is the stop()/start() the watch app was
     * missing to begin with. It is published from inside that branch instead.</p>
     */
    @Test
    void theBootstrapDoesNotTryToPublishReadinessAfterMain(@TempDir Path tmp) throws Exception {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        File dir = tmp.toFile();
        parse(req).writeWatchEntry(req, dir);
        String boot = read(new File(dir, "CN1WatchBootstrap.m"));
        assertFalse(boot.contains("cn1_watch_runtime_markJavaReady"),
                "unreachable after the stub's main, which never returns: " + boot);
    }

    /**
     * A key spelled with CDATA is the same key.
     *
     * <p>The phone's plist is read by an XML parser, so
     * {@code <key><![CDATA[CFBundleShortVersionString]]></key>} suppresses its default -- while a
     * literal text match here found nothing and gave the watch its fallback version. The pair then
     * shipped with different marketing versions, which archive validation rejects.</p>
     */
    @Test
    void injectedKeysAreMatchedAsContentNotAsMarkup(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.plistInject",
                "<key><![CDATA[CFBundleShortVersionString]]></key><string>2.0</string>");

        assertEquals("2.0",
                WatchNativeBuilder.injectedPlistString(req, "CFBundleShortVersionString"),
                "a CDATA-spelled key names the same key an XML parser sees");
        assertTrue(WatchNativeBuilder.injectedPlistKeys(req).contains("CFBundleShortVersionString"),
                "and the key scan has to agree with the lookup");

        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("<key>CFBundleShortVersionString</key>\n    <string>2.0</string>"),
                "the watch carries the injected version, not its fallback: " + plist);
    }

    /**
     * A key present with a non-string value is not an absent key.
     *
     * <p>{@code injectedPlistString} answers null for both, and the two need opposite handling:
     * an absent key takes the generated default, while a key the fragment already carries
     * suppresses that default in the renderer -- so treating {@code <false/>} as absent shipped a
     * plist whose purpose string was the boolean false. The Matter privacy validation refuses on
     * this tag.</p>
     */
    @Test
    void injectedValueTagTellsAbsentFromNonString() {
        BuildRequest req = request();
        req.putArgument("ios.plistInject",
                "<key>NSBluetoothAlwaysUsageDescription</key><false/>"
                + "<key>NSLocalNetworkUsageDescription</key> <!-- why --> <string>find</string>"
                + "<key>NSBonjourServices</key><array><string>_matter._tcp</string></array>");

        assertEquals("false",
                WatchNativeBuilder.injectedPlistValueTag(req, "NSBluetoothAlwaysUsageDescription"),
                "<false/> is a value the fragment carries, not a missing key");
        assertEquals("string",
                WatchNativeBuilder.injectedPlistValueTag(req, "NSLocalNetworkUsageDescription"),
                "a comment between the key and its value is not the value");
        assertEquals("array",
                WatchNativeBuilder.injectedPlistValueTag(req, "NSBonjourServices"),
                "the element that follows the key, not the next string anywhere after it");
        assertNull(WatchNativeBuilder.injectedPlistValueTag(req, "CFBundleVersion"),
                "a key the fragment does not carry is absent, and takes the default");
    }

    /**
     * The array belongs to the key, not to whatever mentioned its name first.
     *
     * <p>Read by finding the name with {@code indexOf} and taking the next {@code <array>}, a
     * fragment that mentions {@code NSBonjourServices} in a comment took the array of the key that
     * came after the comment -- so a plist that listed both Matter services perfectly well was
     * refused for listing neither, and the same shape suppressed the generated
     * {@code com.apple.Home} query scheme.</p>
     */
    @Test
    void injectedArraysBelongToTheirOwnKey() {
        BuildRequest req = request();
        req.putArgument("ios.plistInject",
                "<!-- NSBonjourServices goes here one day -->"
                + "<key>LSApplicationQueriesSchemes</key>"
                + "<array><string>com.apple.Home</string></array>"
                + "<key>NSBonjourServices</key><array>"
                + "<string>_matter._tcp.</string><string>_matterc._udp.</string></array>");

        assertEquals(Arrays.asList("com.apple.Home"),
                WatchNativeBuilder.injectedPlistStringArray(
                        req, "LSApplicationQueriesSchemes"),
                "a comment naming another key is not that key's value");
        assertEquals(Arrays.asList("_matter._tcp.", "_matterc._udp."),
                WatchNativeBuilder.injectedPlistStringArray(req, "NSBonjourServices"),
                "the array that follows the real key, wherever the name appeared before it");
        assertTrue(WatchNativeBuilder.injectedPlistStringArray(req, "CFBundleVersion").isEmpty(),
                "a key the fragment does not carry has no array");
        assertTrue(WatchNativeBuilder.injectedPlistStringArray(
                        req, "NSBluetoothAlwaysUsageDescription").isEmpty(),
                "and neither has one whose value is not an array");
    }

    /**
     * A key's value is the element that follows it, not the next string in the fragment.
     *
     * <p>Scanning forward for the next {@code <string>} anywhere after the key, a key given
     * {@code <false/>} answered with an unrelated later key's string -- so the HomeKit
     * purpose-string check passed on a value the plist renderer keeps as the boolean false, and
     * the app was terminated on the device for a disclosure the build had just approved.</p>
     */
    @Test
    void aKeysValueIsItsOwnElement() {
        BuildRequest req = request();
        req.putArgument("ios.plistInject",
                "<key>NSHomeKitUsageDescription</key><false/>"
                + "<key>NSLocalNetworkUsageDescription</key><string>find them</string>");

        assertNull(WatchNativeBuilder.injectedPlistString(req, "NSHomeKitUsageDescription"),
                "<false/> is not a purpose string, and neither is the next key's value");
        assertEquals("find them",
                WatchNativeBuilder.injectedPlistString(req, "NSLocalNetworkUsageDescription"),
                "a key whose own value is a string still resolves");
    }

    /**
     * A key inside an XML comment is not a key.
     *
     * <p>A plist fragment routinely carries an example in a comment, and the phone's parser ignores
     * it -- so the watch taking the commented version while the app it embeds keeps its real one is
     * a version mismatch archive validation rejects.</p>
     */
    @Test
    void commentedOutKeysAreNotHonoured(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.plistInject",
                "<!-- <key>CFBundleVersion</key><string>9.9</string> -->");

        assertNull(WatchNativeBuilder.injectedPlistString(req, "CFBundleVersion"),
                "a commented key is markup the phone's parser never sees");
        assertFalse(WatchNativeBuilder.injectedPlistKeys(req).contains("CFBundleVersion"), 
                "and the key scan has to agree with the lookup");

        String plist = writeInfoPlist(req, tmp);
        assertFalse(plist.contains("<string>9.9</string>"),
                "the watch must not take a version from a comment: " + plist);
        assertTrue(plist.contains("<key>CFBundleVersion</key>\n    <string>2.5</string>"),
                "it keeps the project's own version: " + plist);
    }

    /**
     * Every phase of a drag reaches the pointer pipeline, not just the completed tap.
     *
     * <p>A SpatialTapGesture reports one point when the finger lifts, which the host turned into a
     * press immediately followed by a release -- so nothing reached {@code pointerDraggedToX} and
     * every drag-driven control was inert: a Slider could not be moved, a scrollable container
     * could not be dragged, a swipe never fired.</p>
     */
    @Test
    void dragPhasesReachTheWatchPointerPipeline(@TempDir Path tmp) throws Exception {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        File dir = tmp.toFile();
        parse(req).writeWatchEntry(req, dir);
        String swift = read(new File(dir, "CN1WatchApp.swift"));

        // Zero minimum distance, so the same gesture still covers a plain tap.
        assertTrue(swift.contains("DragGesture(minimumDistance: 0)"), swift);
        // The names Swift actually imports these ObjC selectors under. pointerPressedAtX:y:
        // becomes pointerPressedAt(x:y:), the same shape as the tapAtX:y: call that already
        // worked -- guessing pointerPressed(atX:) instead compiled as valid Swift and failed to
        // resolve, which broke every iOS job.
        assertTrue(swift.contains("pointerPressedAt(x:"), swift);
        assertTrue(swift.contains("pointerDraggedTo(x:"),
                "the middle of a drag has to arrive, or a Slider cannot move: " + swift);
        assertTrue(swift.contains("pointerReleasedAt(x:"), swift);
        // A press exactly once per gesture: the flag is what separates the first onChanged from
        // the rest, and it also covers a gesture that ends without any onChanged at all.
        assertTrue(swift.contains("if dragging {") && swift.contains("if !dragging {"), swift);
        assertFalse(swift.contains("SpatialTapGesture"),
                "the tap-only gesture is what could not express a drag: " + swift);
    }

    /**
     * Whitespace inside a tag does not make it a different element.
     *
     * <p>{@code <key >CFBundleVersion</key >} is the same element to an XML parser, which is what
     * reads the phone's plist -- so the phone took the override while the watch fell back to its
     * generated version, and archive validation rejects that mismatch.</p>
     */
    @Test
    void tagsAreMatchedAsElementsNotAsLiteralText(@TempDir Path tmp) throws IOException {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.putArgument("ios.plistInject",
                "<key >CFBundleVersion</key ><string >42</string >");

        assertEquals("42", WatchNativeBuilder.injectedPlistString(req, "CFBundleVersion"),
                "a spaced opening tag names the same key an XML parser sees");
        assertTrue(WatchNativeBuilder.injectedPlistKeys(req).contains("CFBundleVersion"),
                "and the key scan has to agree with the lookup");

        String plist = writeInfoPlist(req, tmp);
        assertTrue(plist.contains("<key>CFBundleVersion</key>\n    <string>42</string>"),
                "the watch takes the injected version: " + plist);
    }

    /// The bootstrap has to call the stub that actually exists in the watch binary.
    @Test
    void theBootstrapEntersTheWatchStub(@TempDir Path tmp) throws Exception {
        BuildRequest own = request();
        own.putArgument("watchMain", WATCH_MAIN);
        File dir = tmp.resolve("own").toFile();
        dir.mkdirs();
        parse(own).writeWatchEntry(own, dir);
        String boot = read(new File(dir, "CN1WatchBootstrap.m"));
        assertTrue(boot.contains("MyAppWatchStub_main___java_lang_String_1ARRAY"),
                "the watch translation's own stub is the entry: " + boot);

        BuildRequest shared = request();
        shared.putArgument("watchMain", "com.mycompany.myapp.MyApp");
        File dir2 = tmp.resolve("shared").toFile();
        dir2.mkdirs();
        parse(shared).writeWatchEntry(shared, dir2);
        String boot2 = read(new File(dir2, "CN1WatchBootstrap.m"));
        assertTrue(boot2.contains("MyAppStub_main___java_lang_String_1ARRAY"),
                "the shared translation boots the phone stub: " + boot2);
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

    /**
     * Runs the generated Ruby predicates instead of matching their source text.
     *
     * <p>Every other assertion in this file checks that a helper is <em>mentioned</em>, which is
     * all a Java test can see -- and three separate bugs lived through exactly that check. The
     * catalog filter said "contains an app icon" and skipped whole mixed catalogs; the archive
     * filter called any {@code arm64} slice watch-simulator-compatible when it is equally an
     * iPhone's; and the conditional-arm reader recognized {@code #if os(watchOS)} but not the
     * parenthesized spelling, so a phone-only import was mirrored into the watch target. All three
     * are behaviour of the emitted script, so the only test that can catch them runs it.</p>
     *
     * <p>Skipped where there is no ruby: the generator's own output is unaffected by whether this
     * machine can execute it, and the interpreter is not a build dependency of this module.</p>
     */
    @Test
    void theGeneratedPredicatesDecideCorrectly(@TempDir Path tmp) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(rubyAvailable(), "no ruby on this machine");

        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        String ruby = parse(req).buildXcodeScript(req, tmp.toFile(), "1.0",
                java.util.Arrays.asList("MyAppWatchStub.m"));

        StringBuilder defs = new StringBuilder("require 'fileutils'\n");
        for (String name : new String[] {"cn1_watch_balanced", "cn1_watch_normalize_condition",
                "cn1_watch_excludes_watch", "cn1_watch_atom_selects_watch",
                "cn1_watch_or_operands", "cn1_watch_unwrap", "cn1_watch_selects_watch",
                "cn1_watch_catalog_stage_name", "cn1_watch_catalog_for_watch",
                "cn1_watch_sdk_module_names"}) {
            defs.append(definitionOf(ruby, name)).append('\n');
        }

        // The reported failure and its neighbours: redundant parentheses are valid Swift and say
        // nothing about the platform, so they must not change which arm is selected or excluded.
        StringBuilder driver = new StringBuilder(defs);
        driver.append("Ref = Struct.new(:real_path)\n")
                .append("def chk(l, g, w); puts(g == w ? \"ok #{l}\" : "
                        + "\"FAIL #{l}: got #{g.inspect} want #{w.inspect}\"); end\n")
                .append("chk('paren selects', cn1_watch_selects_watch('#if (os(watchOS))'), true)\n")
                .append("chk('spaced paren selects', "
                        + "cn1_watch_selects_watch('#if ( os(watchOS) )'), true)\n")
                .append("chk('bare selects', cn1_watch_selects_watch('#if os(watchOS)'), true)\n")
                .append("chk('paren objc selects', "
                        + "cn1_watch_selects_watch('#if (TARGET_OS_WATCH)'), true)\n")
                // A conjunction is still not a selection, parenthesized or not: with the feature
                // off Swift compiles the #else, and suppressing it drops what that arm imports.
                .append("chk('paren conjunction does not select', "
                        + "cn1_watch_selects_watch('#if (os(watchOS)) && FEATURE'), false)\n")
                // A definedness test never selects, and only the DIRECTIVE says so. Normalizing
                // first left the bare macro behind, so #ifndef TARGET_OS_WATCH -- which is FALSE
                // on the watch, whose arm is the #else -- read as a positive watch test and the
                // watch's own arm was suppressed.
                .append("chk('ifndef does not select', "
                        + "cn1_watch_selects_watch('#ifndef TARGET_OS_WATCH'), false)\n")
                .append("chk('ifdef does not select', "
                        + "cn1_watch_selects_watch('#ifdef TARGET_OS_WATCH'), false)\n")
                .append("chk('plain objc macro still selects', "
                        + "cn1_watch_selects_watch('#if TARGET_OS_WATCH'), true)\n")
                // A disjunction with a watchOS operand IS unconditionally true on the watch, so
                // the arm is selected however the other operand evaluates. Left undecided, its
                // #else survived and mirrored an iOS-only product into the watch target.
                .append("chk('disjunction with a watch operand selects', "
                        + "cn1_watch_selects_watch('#if os(watchOS) || FEATURE'), true)\n")
                .append("chk('the operand may come second', "
                        + "cn1_watch_selects_watch('#if FEATURE || os(watchOS)'), true)\n")
                .append("chk('and may be parenthesized', "
                        + "cn1_watch_selects_watch('#if (os(watchOS)) || FEATURE'), true)\n")
                // && binds tighter, so this is (os(watchOS) && F) || X -- neither operand is
                // unconditionally true on the watch.
                .append("chk('a conjunction inside a disjunction does not', "
                        + "cn1_watch_selects_watch('#if os(watchOS) && F || X'), false)\n")
                .append("chk('nor a disjunction nested in a conjunction', "
                        + "cn1_watch_selects_watch('#if (os(watchOS) || F) && G'), false)\n")
                // `== 0` is the same statement as `!`, and TRUE on the watch -- reading the bare
                // macro as a positive iOS test suppressed the arm the watch actually compiles.
                .append("chk('a zero comparison does not exclude', "
                        + "cn1_watch_excludes_watch('#if TARGET_OS_IOS == 0'), false)\n")
                .append("chk('but a one comparison still does', "
                        + "cn1_watch_excludes_watch('#if TARGET_OS_IOS == 1'), true)\n")
                // Two catalogs sharing a basename must not share a staging directory: the second
                // replaced the first and both watch references resolved to it.
                .append("chk('same basename stages distinctly', "
                        + "cn1_watch_catalog_stage_name('/p/A/Assets.xcassets') != "
                        + "cn1_watch_catalog_stage_name('/p/B/Assets.xcassets'), true)\n")
                .append("chk('and the same catalog is stable', "
                        + "cn1_watch_catalog_stage_name('/p/A/Assets.xcassets') == "
                        + "cn1_watch_catalog_stage_name('/p/A/Assets.xcassets'), true)\n")
                // Every arm rejecting the watch settles a disjunction: `#if os(iOS) || os(macOS)`
                // is false on watchOS, so the compiler excludes that arm and the import inside it
                // must not reach the watch target.
                .append("chk('a disjunction of non-watch platforms excludes', "
                        + "cn1_watch_excludes_watch('#if os(iOS) || os(macOS)'), true)\n")
                .append("chk('one undecidable arm is enough to keep it', "
                        + "cn1_watch_excludes_watch('#if os(iOS) || FEATURE'), false)\n")
                .append("chk('and a watchOS arm certainly is', "
                        + "cn1_watch_excludes_watch('#if os(iOS) || os(watchOS)'), false)\n")
                .append("chk('the objc spelling too', "
                        + "cn1_watch_excludes_watch('#if TARGET_OS_IOS || TARGET_OS_OSX'), true)\n")
                // Read from the SDK's own module maps. Probing usr/lib/swift/<M>.swiftmodule
                // found the Swift overlays and missed 66 of the 78 C modules the shared
                // usr/include/module.modulemap declares -- SQLite3, zlib, MachO among them -- and
                // one unattributed import switches strict package filtering off entirely.
                //
                // Only where there IS a watchOS SDK. These plugin tests also run on Linux, where
                // there is no xcrun to ask -- and an emptiness guard does not cover that, because
                // the helper always seeds the Swift standard names and so never returns empty.
                .append("sdk_paths = begin\n")
                .append("  [`xcrun --sdk watchos --show-sdk-path 2>/dev/null`.strip]"
                        + ".reject(&:empty?)\n")
                .append("rescue StandardError\n")
                .append("  []\n")
                .append("end\n")
                .append("if sdk_paths.empty?\n")
                .append("  puts 'ok no watchOS SDK on this host, SDK attribution not exercised'\n")
                .append("else\n")
                .append("  sdk_names = cn1_watch_sdk_module_names(sdk_paths)\n")
                .append("  chk('a shared-modulemap C module is attributed', "
                        + "sdk_names.key?('SQLite3'), true)\n")
                .append("  chk('so is a swift overlay', sdk_names.key?('Darwin'), true)\n")
                .append("  chk('a real package is not', sdk_names.key?('Alamofire'), false)\n")
                .append("end\n")
                // `== 1` and `!= 0` say what the bare macro says. Accepting only the bare form
                // left `#if TARGET_OS_WATCH == 1` undecided, so its #else survived and the
                // phone-only package in there reached the watch target.
                .append("chk('a true comparison selects', "
                        + "cn1_watch_selects_watch('#if TARGET_OS_WATCH == 1'), true)\n")
                .append("chk('so does != 0', "
                        + "cn1_watch_selects_watch('#if TARGET_OS_WATCH != 0'), true)\n")
                .append("chk('but == 0 does not', "
                        + "cn1_watch_selects_watch('#if TARGET_OS_WATCH == 0'), false)\n")
                // A conjunction is false on the watch as soon as one operand is, so
                // `os(watchOS) && os(iOS)` is false everywhere -- nothing is both -- and that arm
                // never compiles. The dual of the disjunction rule: there every operand had to
                // exclude, here any one does.
                // A parenthesized disjunction is not nesting, it is punctuation: the top-level
                // `||` split saw one operand and settled the condition before the conjunction rule
                // could observe the group is false on the watch.
                .append("chk('paren disjunction in a conjunction excludes', "
                        + "cn1_watch_excludes_watch('#if (os(iOS) || os(macOS)) && FEATURE'), "
                        + "true)\n")
                .append("chk('either side of the conjunction', "
                        + "cn1_watch_excludes_watch('#if FEATURE && (os(iOS) || os(macOS))'), "
                        + "true)\n")
                // Unwrapping must not swallow a real top-level operator.
                .append("chk('unwrap keeps a top-level conjunction', "
                        + "cn1_watch_excludes_watch('#if (FEATURE) && (OTHER)'), false)\n")
                // Reaching the rest of the function instead of returning makes negated groups
                // reachable, and every scan below reads a positive platform mention as exclusion.
                // `!(os(iOS))` is TRUE on the watch, so that arm is the one it compiles.
                .append("chk('a negated group stays undecidable', "
                        + "cn1_watch_excludes_watch('#if !(os(iOS) || os(macOS))'), false)\n")
                // `== 0` and `!= 1` are the unary `!` written out. The same reading exists for
                // the other platforms, where it means the opposite -- there a false test is the
                // arm the watch DOES compile.
                .append("chk('TARGET_OS_WATCH == 0 excludes', "
                        + "cn1_watch_excludes_watch('#if TARGET_OS_WATCH == 0'), true)\n")
                .append("chk('TARGET_OS_WATCH != 1 excludes', "
                        + "cn1_watch_excludes_watch('#if TARGET_OS_WATCH != 1'), true)\n")
                .append("chk('the true forms still do not', "
                        + "cn1_watch_excludes_watch('#if TARGET_OS_WATCH == 1'), false)\n")
                .append("chk('nor does a bare macro', "
                        + "cn1_watch_excludes_watch('#if TARGET_OS_WATCH'), false)\n")
                .append("chk('== 0 does not select either', "
                        + "cn1_watch_selects_watch('#if TARGET_OS_WATCH == 0'), false)\n")
                .append("chk('a contradiction excludes', "
                        + "cn1_watch_excludes_watch('#if os(watchOS) && os(iOS)'), true)\n")
                .append("chk('either order', "
                        + "cn1_watch_excludes_watch('#if os(iOS) && os(watchOS)'), true)\n")
                // Still satisfiable on the watch when the other operand is merely unknown.
                .append("chk('a feature flag beside it does not', "
                        + "cn1_watch_excludes_watch('#if os(watchOS) && FEATURE'), false)\n")
                .append("chk('paren other platform excludes', "
                        + "cn1_watch_excludes_watch('#if (os(iOS))'), true)\n")
                .append("chk('paren negated watch excludes', "
                        + "cn1_watch_excludes_watch('#if !(os(watchOS))'), true)\n")
                .append("chk('paren watch not excluded', "
                        + "cn1_watch_excludes_watch('#if (os(watchOS))'), false)\n")
                // Only REDUNDANT parentheses go. A disjunction can still be true on the watch
                // through its other operand, and wrapping one operand must not hide that.
                .append("chk('parenthesized disjunction still proves nothing', "
                        + "cn1_watch_excludes_watch('#if (os(iOS)) || FEATURE'), false)\n")
                // Definedness is not a platform test: TargetConditionals defines every macro on
                // every platform, so the watch compiles this arm.
                .append("chk('ifdef is not a platform test', "
                        + "cn1_watch_excludes_watch('#ifdef TARGET_OS_IOS'), false)\n")
                // A mixed catalog keeps its usable sets. Skipping the container cost the watch
                // every image in it.
                .append("mixed = cn1_watch_catalog_for_watch(Ref.new('")
                .append(IPhoneBuilder.escapeRubyStr(catalog(tmp, "Assets", true, true)))
                .append("'), '").append(IPhoneBuilder.escapeRubyStr(
                        tmp.resolve("stage").toString())).append("')\n")
                .append("chk('mixed catalog is staged', !mixed.nil?, true)\n")
                .append("chk('staged keeps the imageset', "
                        + "File.directory?(File.join(mixed.to_s, 'logo.imageset')), true)\n")
                .append("chk('staged drops the app icon', "
                        + "File.exist?(File.join(mixed.to_s, 'AppIcon.appiconset')), false)\n")
                // Icon-only is still skipped -- there is nothing in it the watch can use, and
                // staging an empty catalog helps nobody.
                .append("chk('icon-only catalog is skipped', cn1_watch_catalog_for_watch(Ref.new('")
                .append(IPhoneBuilder.escapeRubyStr(catalog(tmp, "IconOnly", true, false)))
                .append("'), '").append(IPhoneBuilder.escapeRubyStr(
                        tmp.resolve("stage").toString())).append("'), nil)\n")
                // No icon at all: shared as it stands, with no copy made.
                .append("plain = '")
                .append(IPhoneBuilder.escapeRubyStr(catalog(tmp, "Plain", false, true)))
                .append("'\n")
                .append("chk('plain catalog is shared as-is', "
                        + "cn1_watch_catalog_for_watch(Ref.new(plain), '")
                .append(IPhoneBuilder.escapeRubyStr(tmp.resolve("stage").toString()))
                .append("'), plain)\n");

        Path script = tmp.resolve("predicates.rb");
        Files.write(script, driver.toString().getBytes("UTF-8"));
        Process p = new ProcessBuilder("ruby", script.toString())
                .redirectErrorStream(true).start();
        String out = new String(readFully(p.getInputStream()), "UTF-8");
        p.waitFor();
        assertFalse(out.contains("FAIL"), out);
        assertTrue(out.contains("ok mixed catalog is staged"),
                "the driver did not run to completion: " + out);
    }


    /**
     * The watch target gets an app icon of its own.
     *
     * <p>An iOS AppIcon set declares iPhone and iPad idioms, so it is filtered out of the catalog
     * staged for the watch -- and nothing replaced it, which left every watch product iconless.
     * Building, running and testing are unaffected; App Store submission is not, and that is the
     * one place it cannot be worked around.</p>
     */
    @Test
    void theWatchTargetCarriesItsOwnAppIcon(@TempDir Path tmp) throws Exception {
        BuildRequest req = request();
        req.putArgument("watchMain", WATCH_MAIN);
        req.setIcon(pngBytes(96));
        WatchNativeBuilder b = parse(req);
        File appSrc = tmp.toFile();

        b.writeWatchAppIcon(req, appSrc);
        File set = new File(appSrc, "WatchImages.xcassets/AppIcon.appiconset");
        assertTrue(new File(set, "AppIcon.png").isFile(), "the scaled icon is written");
        String contents = new String(Files.readAllBytes(
                new File(set, "Contents.json").toPath()), "UTF-8");
        // The single-size form, not the per-device idiom list: that one has to enumerate every
        // watch size ever shipped and needs a new entry for each new one.
        assertTrue(contents.contains("\"platform\" : \"watchos\""), contents);
        assertTrue(contents.contains("1024x1024"), contents);
        assertTrue(new File(appSrc, "WatchImages.xcassets/Contents.json").isFile(),
                "actool only treats the directory as a catalog when it has one");

        String ruby = b.buildXcodeScript(req, tmp.toFile(), "1.0",
                java.util.Arrays.asList("MyAppWatchStub.m"));
        // Compiling the catalog is not enough. actool promotes a set to the app icon only when the
        // target names it, and nothing named one for the watch.
        assertTrue(ruby.contains("ASSETCATALOG_COMPILER_APPICON_NAME"), ruby);
        // Referenced under <Main>-src, where writeWatchAppIcon actually put it. The project sits
        // one level up in dist, so a bare "WatchImages.xcassets" resolved to nothing: the script's
        // directory test failed silently and the catalog never joined the resources phase, leaving
        // the build setting naming a set the target did not contain.
        assertTrue(ruby.contains("MyApp-src/WatchImages.xcassets"),
                "the icon catalog has to be addressed the way the watch plist is: " + ruby);
    }


    /** A square PNG, so the icon scaler has something real to read. */
    private static byte[] pngBytes(int size) throws IOException {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.BLUE);
        g.fillRect(0, 0, size, size);
        g.dispose();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    /** The {@code def NAME ... end} block for one generated helper. */
    private static String definitionOf(String ruby, String name) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^def " + name + "\\(.*?^end$", java.util.regex.Pattern.MULTILINE
                        | java.util.regex.Pattern.DOTALL)
                .matcher(ruby);
        assertTrue(m.find(), "the generated script no longer defines " + name);
        return m.group();
    }

    /** A catalog on disk, optionally carrying an app icon and optionally ordinary sets. */
    private static String catalog(Path tmp, String name, boolean icon, boolean images)
            throws IOException {
        Path root = tmp.resolve(name + ".xcassets");
        Files.createDirectories(root);
        Files.write(root.resolve("Contents.json"), "{}".getBytes("UTF-8"));
        if (icon) {
            Files.createDirectories(root.resolve("AppIcon.appiconset"));
            Files.write(root.resolve("AppIcon.appiconset").resolve("Contents.json"),
                    "{}".getBytes("UTF-8"));
        }
        if (images) {
            Files.createDirectories(root.resolve("logo.imageset"));
            Files.write(root.resolve("logo.imageset").resolve("Contents.json"),
                    "{}".getBytes("UTF-8"));
        }
        return root.toString();
    }

    private static byte[] readFully(java.io.InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

    private static boolean rubyAvailable() {
        try {
            Process p = new ProcessBuilder("ruby", "-e", "exit 0")
                    .redirectErrorStream(true).start();
            readFully(p.getInputStream());
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Every framework IPhoneBuilder can put on the app target is classified for the watch.
     *
     * <p>The watch target mirrors the app target's frameworks phase and drops what
     * WATCH_OPTIONAL_FRAMEWORKS names. That is a DENY list, so it is silent about the framework
     * nobody thought of: an unclassified one is kept, and if watchOS does not have it the watch
     * link fails with "framework 'X' not found". It happened three times in a row, one framework
     * per CI round -- SystemConfiguration, then AudioToolbox and QuickLook, then Vision -- because
     * each was only reachable in a project that used the feature that adds it.
     *
     * <p>This asserts the two lists PARTITION what the builder can emit, so the commit that adds a
     * framework fails here rather than in an iOS job forty minutes later. It cannot see
     * ios.add_libs (a project's own hint) or ByteCodeTranslator's base list, which lives in
     * another module -- the base list is audited in WATCH_OPTIONAL_FRAMEWORKS' comment.
     */
    @Test
    public void everyEmittedFrameworkIsClassifiedForTheWatch() throws Exception {
        File base = new File(System.getProperty("basedir", "."));
        // Three sources, because a framework reaches the link phase three ways and only the first
        // is a quoted "X.framework" literal. PlatformFeatureCatalog names them BARE and
        // IPhoneBuilder appends the suffix, so grepping only for literals missed VisionKit and
        // Speech -- a fourth red CI round after the six this test was written for.
        File[] sources = {
            new File(base, "src/main/java/com/codename1/builders/IPhoneBuilder.java"),
            new File(base, "src/main/java/com/codename1/builders/MapsProviderInjector.java"),
            new File(base, "../platform-feature-catalog/src/main/java/com/codename1/build/shared/"
                    + "PlatformFeatureCatalog.java"),
        };
        StringBuilder all = new StringBuilder();
        for (File src : sources) {
            assertTrue(src.isFile(), "cannot find a source to scan: " + src.getAbsolutePath());
            all.append(new String(Files.readAllBytes(src.toPath()), "UTF-8")).append('\n');
        }
        String source = all.toString();

        // Read REFLECTIVELY, not as WatchNativeBuilder.WATCH_OPTIONAL_FRAMEWORKS. Those are
        // static final Strings, so javac inlines them into this class at compile time: editing
        // the list without touching this file leaves a stale copy compiled in, and the test
        // passes on a list it is no longer reading. CI builds clean and never sees it, which is
        // exactly what makes it worth ruling out here.
        Set<String> classified = new HashSet<String>();
        for (String field : new String[] {"WATCH_OPTIONAL_FRAMEWORKS", "WATCH_LINKABLE_FRAMEWORKS"}) {
            java.lang.reflect.Field f = WatchNativeBuilder.class.getDeclaredField(field);
            f.setAccessible(true);
            for (String s : ((String) f.get(null)).split(";")) {
                classified.add(s.trim().toLowerCase());
            }
        }

        // Quoted literals only, so "-Doptional.frameworks=" and the like are not mistaken for one.
        // Case-insensitive throughout: IPhoneBuilder writes "JavascriptCore.framework".
        Set<String> unclassified = new TreeSet<String>();
        Matcher m = Pattern.compile("\"([A-Za-z][A-Za-z0-9_]*\\.framework)\"").matcher(source);
        while (m.find()) {
            if (!classified.contains(m.group(1).toLowerCase())) {
                unclassified.add(m.group(1));
            }
        }
        // The catalog's bare names: .iosFrameworks("VisionKit", "Vision", "CoreImage").
        Matcher bare = Pattern.compile("\\.iosFrameworks\\(([^)]*)\\)").matcher(source);
        Matcher quoted = Pattern.compile("\"([A-Za-z][A-Za-z0-9_]*)\"").matcher("");
        while (bare.find()) {
            quoted.reset(bare.group(1));
            while (quoted.find()) {
                String name = quoted.group(1);
                if (name.endsWith(".framework")) {
                    continue;
                }
                if (!classified.contains((name + ".framework").toLowerCase())) {
                    unclassified.add(name + ".framework");
                }
            }
        }
        assertTrue(unclassified.isEmpty(),
                "IPhoneBuilder can link " + unclassified + " but WatchNativeBuilder classifies "
                + "neither as unavailable on watchOS nor as linkable there. Check each against "
                + "`ls \"$(xcrun --sdk watchos --show-sdk-path)/System/Library/Frameworks\"` and "
                + "add it to WATCH_OPTIONAL_FRAMEWORKS (absent, or present but unused by the "
                + "watch) or WATCH_LINKABLE_FRAMEWORKS (present and wanted).");
    }

    /**
     * Everything declared LINKABLE really is in both watch SDKs.
     *
     * <p>The partition test proves every framework is classified; it cannot say the
     * classification is true. BackgroundTasks was declared linkable on the strength of the device
     * SDK alone and is absent from the SIMULATOR one -- the only framework where the two disagree
     * -- so every watch simulator build failed to link. That is the fifth framework-not-found CI
     * round in this branch and the first that a correct-looking declaration caused.
     *
     * <p>Skipped where the SDKs are not installed, which is every Linux leg. It runs on the
     * machine where the declaration gets edited, which is where the mistake is made.
     */
    @Test
    public void everyLinkableFrameworkExistsInBothWatchSdks() throws Exception {
        Map<String, File> sdks = new LinkedHashMap<String, File>();
        for (String sdk : new String[] {"watchos", "watchsimulator"}) {
            File root = sdkPath(sdk);
            org.junit.jupiter.api.Assumptions.assumeTrue(root != null,
                    "no " + sdk + " SDK on this machine");
            sdks.put(sdk, root);
        }
        java.lang.reflect.Field f =
                WatchNativeBuilder.class.getDeclaredField("WATCH_LINKABLE_FRAMEWORKS");
        f.setAccessible(true);
        Set<String> missing = new TreeSet<String>();
        for (String name : ((String) f.get(null)).split(";")) {
            String bare = name.trim();
            if (bare.length() == 0) {
                continue;
            }
            for (Map.Entry<String, File> sdk : sdks.entrySet()) {
                if (!new File(sdk.getValue(), "System/Library/Frameworks/" + bare).isDirectory()) {
                    missing.add(bare + " (" + sdk.getKey() + ")");
                }
            }
        }
        assertTrue(missing.isEmpty(), "declared linkable on the watch but not in the SDK: "
                + missing + ". A framework the watch does not have must go in "
                + "WATCH_OPTIONAL_FRAMEWORKS instead -- and one that only some watch SDKs have "
                + "belongs there too, because the list is applied to both.");
    }

    /// The SDK root, or null when this machine has no Xcode.
    private static File sdkPath(String sdk) {
        try {
            Process p = new ProcessBuilder("xcrun", "--sdk", sdk, "--show-sdk-path")
                    .redirectErrorStream(true).start();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r;
            while ((r = p.getInputStream().read(buf)) > 0) {
                out.write(buf, 0, r);
            }
            if (!p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS) || p.exitValue() != 0) {
                return null;
            }
            File root = new File(out.toString("UTF-8").trim());
            return root.isDirectory() ? root : null;
        } catch (Exception noXcode) {
            return null;
        }
    }

    /// The complication extension is embedded in the watch app, so a watch that cannot install
    /// the app cannot show its complication. WidgetKit itself goes back to watchOS 9 and the
    /// extension builds there, which is where its own floor sits -- but the DEFAULT the builder
    /// is given has to be the app's, or the extension advertises support that does not exist.
    /// Pinned as a relationship rather than a number so lowering the app's floor later needs no
    /// change here.
    @Test
    public void theWatchAppNeverRequiresMoreThanItsComplicationAdvertises() {
        String app = WatchNativeBuilder.MIN_DEPLOYMENT_TARGET;
        String extension = com.codename1.util.IOSWidgetExtensionBuilder.WATCH_MIN_DEPLOYMENT_TARGET;

        assertTrue(Double.parseDouble(app) >= Double.parseDouble(extension),
                "the watch app requires watchOS " + app + " while its complication extension "
                        + "claims to support " + extension + "; the extension is embedded in the "
                        + "app, so the app's floor is the one users actually meet");
    }
}
