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

            // A watch with its OWN root shakes from that root, so the phone's usage says nothing
            // about it -- entitling it anyway fails codesigning for an ordinary watch app whose
            // App ID has no HealthKit capability. Unchanged by this fix, and worth pinning next
            // to it so the two rules are not confused for each other.
            BuildRequest distinct = request();
            distinct.putArgument("watchMain", WATCH_MAIN);
            distinct.putArgument(hint, "true");
            assertEquals("",
                    parse(distinct).watchEntitlementsSetting(distinct, distinct.getMainClass()),
                    hint + " must not entitle a watch app with its own lifecycle class");
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
}
