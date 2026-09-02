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

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Covers the migration half of the macOS build hints.
///
/// A project that was building a Mac Catalyst app yesterday is building an
/// AppKit app today under the same target name, without editing anything. That
/// only holds if every `macNative.*` setting still means what it meant, so these
/// are the tests that say so.
public class MacOSBuildHintsTest {

    /// A hint value carries whatever whitespace the settings file had around it, and the
    /// two readers of this one used to compare it raw: "false " is neither "false" nor
    /// "none", so a project that turned push OFF got the APNs entitlement emitted anyway --
    /// and then could not sign against an App ID that does not grant it, because the
    /// signing wizard reads the same settings file trimmed and left the capability alone.
    @org.junit.Test
    public void apsEnvironmentIgnoresSurroundingWhitespace() {
        Map<String, String> raw = new HashMap<String, String>();
        raw.put("macos.entitlements.apsEnvironment", " false ");
        assertFalse("a trimmed false is still false",
                parse(raw, "com.example").entitlementsFor("developerID").push(true));

        raw.put("macos.entitlements.apsEnvironment", " none ");
        assertFalse(parse(raw, "com.example").entitlementsFor("developerID").push(true));

        raw.put("macos.entitlements.apsEnvironment", " development ");
        MacOSBuildHints.EntitlementOverrides dev = parse(raw, "com.example").entitlementsFor("developerID");
        assertTrue(dev.push(false));
        assertEquals("development", dev.getApsEnvironment());

        // All whitespace is not an environment: it reads as unset, so the class scan
        // decides, exactly as an absent hint does.
        raw.put("macos.entitlements.apsEnvironment", "   ");
        assertFalse(parse(raw, "com.example").entitlementsFor("developerID").push(false));
        assertTrue(parse(raw, "com.example").entitlementsFor("developerID").push(true));

        // And it does not shadow the migrated spelling on the way. A modern key left blank
        // counted as present, so the macNative value that actually said something was never
        // read -- a project still using its legacy hint lost the entitlement it asks for.
        raw.put("macNative.entitlements.apsEnvironment", "production");
        MacOSBuildHints.EntitlementOverrides legacy =
                parse(raw, "com.example").entitlementsFor("developerID");
        assertTrue("a blank modern key must not shadow the migrated one", legacy.push(false));
        assertEquals("production", legacy.getApsEnvironment());
    }

    private static MacOSBuildHints parse(final Map<String, String> raw, String pkg) {
        MacOSBuildHints h = new MacOSBuildHints();
        h.parse(new MacOSBuildHints.HintSource() {
            @Override
            public String get(String key, String defaultValue) {
                String v = raw.get(key);
                return v != null ? v : defaultValue;
            }
        }, pkg);
        return h;
    }

    private static Map<String, String> raw(String... kv) {
        Map<String, String> m = new HashMap<String, String>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    /// A bundle identifier that could break out of project.pbxproj is refused.
    ///
    /// PRODUCT_BUNDLE_IDENTIFIER = "<id>"; is written into the generated
    /// project and xcodebuild is then run on it, and on the build cloud the
    /// settings this comes from arrive with the request. A quote plus a newline
    /// closes that string and continues the file as more project objects --
    /// including a shell script build phase the build would execute.
    @Test
    public void aBundleIdThatCouldEscapeThePbxprojIsRefused() {
        String[] hostile = {
            "com.example.app\";\n\t\t\t\tOTHER_LDFLAGS = \"-run",
            "com.example.app\nINJECTED = 1",
            "com.example.app`id`",
            "com.example.app$(whoami)",
            "com.example app",
        };
        for (String id : hostile) {
            try {
                parse(raw("macos.bundleId", id), "com.example.app");
                fail("a bundle id carrying pbxproj syntax must be refused: " + id);
            } catch (IllegalArgumentException expected) {
                assertTrue("the message names the setting to fix",
                        expected.getMessage().indexOf("macos.bundleId") > -1);
            }
        }
    }

    /// A migrating Catalyst project keeps the channel it has been building.
    ///
    /// MacNativeBuilder defaults macNative.distribution to appStore, so a
    /// project that never set the hint has been producing an App Store pkg.
    /// Taking developerID for it would change the artifact on the build where
    /// its plugin was upgraded -- a dmg signed with a Developer ID certificate
    /// it may not have -- which is the "without editing anything" promise this
    /// class exists to keep.
    @Test
    public void aMigratingCatalystProjectKeepsTheAppStoreDefault() {
        MacOSBuildHints h = parse(raw("macNative.enabled", "true"), "com.example.app");
        assertEquals(MacOSBuildHints.DISTRIBUTION_APP_STORE, h.getDistribution());
    }

    /// A migrating project keeps the IDENTITY it already ships under.
    ///
    /// MacNativeBuilder defaults macNative.deriveBundleId to true, so a
    /// Catalyst project that never set it has been building as the package name
    /// itself. Appending ".mac" would change the application's identity on the
    /// build where its plugin was upgraded: a different record in App Store
    /// Connect, a provisioning profile that no longer matches, and an update
    /// that is not an update.
    @Test
    public void aMigratingCatalystProjectKeepsItsBundleIdentifier() {
        assertEquals("com.example.app",
                parse(raw("macNative.enabled", "true"), "com.example.app").getBundleId());
        // New to this target: the separate ".mac" identifier, because a macOS
        // app and an iOS app are distinct products.
        assertEquals("com.example.app.mac",
                parse(raw(), "com.example.app").getBundleId());
    }

    /// And it keeps automatic signing, which is what it has been using.
    ///
    /// null reads as manual through usesAutomaticSigning(), so a migrated App
    /// Store project that let Xcode resolve its profile was rejected until it
    /// staged one by hand.
    @Test
    public void aMigratingCatalystProjectKeepsAutomaticSigning() {
        assertTrue(parse(raw("macNative.enabled", "true"),
                "com.example.app").usesAutomaticSigning());
        assertFalse("a project new to this target is explicit about signing",
                parse(raw(), "com.example.app").usesAutomaticSigning());
        // An explicit choice still wins for the legacy project.
        assertFalse(parse(raw("macNative.enabled", "true",
                "macNative.signing.style", "manual"),
                "com.example.app").usesAutomaticSigning());
    }

    /// The deployment floor is NOT preserved, and that is deliberate: 11.0 is
    /// the first macOS on Apple Silicon and this port builds universal, so a
    /// Catalyst app's 10.15 is not a floor a native arm64 build can keep.
    @Test
    public void theDeploymentFloorIsNotInheritedFromCatalyst() {
        assertEquals(MacOSBuildHints.DEFAULT_DEPLOYMENT_TARGET,
                parse(raw("macNative.enabled", "true"),
                        "com.example.app").getMinDeploymentTarget());
    }

    /// A project new to this target takes developerID, which needs no App Store
    /// credentials to produce something runnable.
    @Test
    public void aProjectNewToTheTargetDefaultsToDeveloperId() {
        assertEquals(MacOSBuildHints.DISTRIBUTION_DEVELOPER_ID,
                parse(raw(), "com.example.app").getDistribution());
    }

    /// An explicit choice still wins, in either spelling and either direction.
    @Test
    public void anExplicitDistributionBeatsBothDefaults() {
        assertEquals(MacOSBuildHints.DISTRIBUTION_DEVELOPER_ID,
                parse(raw("macNative.enabled", "true",
                        "macNative.distribution", "developerID"),
                        "com.example.app").getDistribution());
        assertEquals(MacOSBuildHints.DISTRIBUTION_APP_STORE,
                parse(raw("macos.distribution", "appStore"),
                        "com.example.app").getDistribution());
    }

    /// The other hint that reaches project.pbxproj gets the same treatment.
    ///
    /// It is written as MACOSX_DEPLOYMENT_TARGET = <value>;, so a semicolon,
    /// brace or newline ends the setting and continues the file as more project
    /// content -- including a build phase xcodebuild would execute.
    @Test
    public void aDeploymentTargetThatCouldEscapeThePbxprojIsRefused() {
        String[] hostile = {
            "11.0;\n\t\t\t\tOTHER_LDFLAGS = \"-run\"",
            "11.0};",
            "11.0 ; INJECTED = 1",
            "latest",
            "11..0",
            "11.",
        };
        for (String t : hostile) {
            try {
                parse(raw("macos.minDeploymentTarget", t), "com.example.app");
                fail("a deployment target carrying pbxproj syntax must be refused: " + t);
            } catch (IllegalArgumentException expected) {
                assertTrue("the message names the setting to fix",
                        expected.getMessage().indexOf("macos.minDeploymentTarget") > -1);
            }
        }
    }

    /// Real deployment targets still parse, including the unset default.
    @Test
    public void anOrdinaryDeploymentTargetIsAccepted() {
        for (String t : new String[] {"11", "11.0", "13.1.2", "26"}) {
            assertEquals(t, parse(raw("macos.minDeploymentTarget", t),
                    "com.example.app").getMinDeploymentTarget());
        }
        assertNotNull(parse(raw(), "com.example.app").getMinDeploymentTarget());
    }

    /// And an ordinary identifier still builds, including the derived default.
    @Test
    public void anOrdinaryBundleIdIsAccepted() {
        assertEquals("com.example.app.mac",
                parse(raw(), "com.example.app").getBundleId());
        assertEquals("com.example.my-app2.mac",
                parse(raw(), "com.example.my-app2").getBundleId());
        assertEquals("com.example.Custom-1",
                parse(raw("macos.bundleId", "com.example.Custom-1"),
                        "com.example.app").getBundleId());
    }

    /// The calendar opt-out has to reach the usage descriptions, not just the
    /// entitlement.
    ///
    /// Regression: the plist flag was seeded with the scan result and the
    /// channels were ORed onto it, so no override could clear it. The
    /// entitlement was suppressed and the plist still declared every calendar
    /// and reminder description -- a privacy review for access the build hint
    /// had switched off.
    @Test
    public void calendarsOptOutSuppressesTheUsageDescriptions() {
        MacOSBuildHints h = parse(raw(
                "macos.distribution", "developer-id",
                "macos.entitlements.personalInformation.calendars", "false"),
                "com.example.app");
        List<MacOSBuildHints.EntitlementOverrides> channels =
                new ArrayList<MacOSBuildHints.EntitlementOverrides>();
        for (String c : h.getChannels()) {
            channels.add(h.entitlementsFor(c));
        }
        assertFalse("an explicit calendars=false must clear the descriptions"
                        + " even when the scan detected calendar use",
                MacOSNativeBuilder.calendarUsageDescriptionsGranted(channels, true));
        // And the entitlement agrees, which is the point of resolving both the
        // same way.
        for (MacOSBuildHints.EntitlementOverrides o : channels) {
            assertFalse("the entitlement is suppressed too", o.calendars(true));
        }
    }

    /// Unset, the scan still decides -- in both directions.
    @Test
    public void unsetCalendarsLeavesTheScanInCharge() {
        MacOSBuildHints h = parse(raw("macos.distribution", "developer-id"),
                "com.example.app");
        List<MacOSBuildHints.EntitlementOverrides> channels =
                new ArrayList<MacOSBuildHints.EntitlementOverrides>();
        for (String c : h.getChannels()) {
            channels.add(h.entitlementsFor(c));
        }
        assertTrue("detected calendar use must still declare the descriptions",
                MacOSNativeBuilder.calendarUsageDescriptionsGranted(channels, true));
        assertFalse("an app that never touches the calendar declares nothing",
                MacOSNativeBuilder.calendarUsageDescriptionsGranted(channels, false));
        assertTrue("no channels at all leaves the scan's answer standing",
                MacOSNativeBuilder.calendarUsageDescriptionsGranted(
                        new ArrayList<MacOSBuildHints.EntitlementOverrides>(), true));
    }

    @Test
    public void unsetAppCategoryTakesTheDocumentedDefault() {
        // Pinned because it drifted: the constant said developer-tools while the
        // build-hint table said utilities, so every application that did not set
        // the hint shipped classified as a developer tool. Nothing compared the
        // two, which is why nothing noticed.
        assertEquals("public.app-category.utilities", parse(raw(), "com.example.app").getAppCategory());
    }

    @Test
    public void aCatalystEraProjectCarriesOverUnchanged() {
        // Exactly what scripts/build-mac-native-app.sh writes today.
        MacOSBuildHints h = parse(raw(
                "macNative.enabled", "true",
                "macNative.teamId", "ABCDEF1234",
                "macNative.distribution", "both",
                "macNative.appCategory", "public.app-category.developer-tools",
                "macNative.fixedWindowSize", "1024x685"), "com.example.app");

        assertEquals("ABCDEF1234", h.getTeamId());
        assertEquals(MacOSBuildHints.DISTRIBUTION_BOTH, h.getDistribution());
        assertEquals("public.app-category.developer-tools", h.getAppCategory());
        assertEquals("1024x685", h.getFixedWindowSize());
        assertTrue(h.getWarnings().isEmpty());
    }

    /// The xcodebuild settings, which the builder used to read straight off the
    /// request and so ignored the legacy spelling: a project asking for
    /// macNative.configuration=Debug was archived as Release, and one asking for
    /// macNative.arch=x86_64 came out universal. Both silently.
    @Test
    public void theXcodeSettingsHonourTheLegacySpelling() {
        MacOSBuildHints h = parse(raw(
                "macNative.configuration", "Debug",
                "macNative.arch", "x86_64"), "com.example.app");
        assertEquals("Debug", h.getConfiguration());
        assertEquals("x86_64", h.getArch());
    }

    /// And keep their documented defaults when nobody says otherwise.
    @Test
    public void theXcodeSettingsHaveTheDocumentedDefaults() {
        MacOSBuildHints h = parse(raw(), "com.example.app");
        assertEquals("Release", h.getConfiguration());
        assertEquals("arm64 x86_64", h.getArch());
    }

    /// The modern spelling wins here too, like everywhere else.
    @Test
    public void theModernXcodeSettingsWinOverTheLegacyOnes() {
        MacOSBuildHints h = parse(raw(
                "macNative.configuration", "Debug",
                "macos.configuration", "Release",
                "macNative.arch", "x86_64",
                "macos.arch", "arm64"), "com.example.app");
        assertEquals("Release", h.getConfiguration());
        assertEquals("arm64", h.getArch());
    }

    @Test
    public void theModernSpellingWinsOverTheLegacyOne() {
        MacOSBuildHints h = parse(raw(
                "macNative.teamId", "OLDTEAM",
                "macos.teamId", "NEWTEAM"), "com.example");
        assertEquals("NEWTEAM", h.getTeamId());
    }

    @Test
    public void teamIdFallsBackToTheIosOne() {
        // One Apple account, one team. Asking for it twice is a setting a
        // developer can only get wrong.
        assertEquals("SHAREDTEAM",
                parse(raw("ios.release.teamId", "SHAREDTEAM"), "com.example").getTeamId());
    }

    @Test
    public void theStaleCatalystOnlyHintWarnsRatherThanFailing() {
        MacOSBuildHints h = parse(raw("macNative.iosMinDeploymentTarget", "13.1"), "com.example");
        assertEquals(1, h.getWarnings().size());
        assertTrue(h.getWarnings().get(0).contains("iosMinDeploymentTarget"));
        // The point of warning instead of failing: this line is sitting in real
        // settings files, and the developer did not put it there for this build.
        assertEquals(MacOSBuildHints.DEFAULT_DEPLOYMENT_TARGET, h.getMinDeploymentTarget());
    }

    @Test
    public void anUnrecognizedDistributionFallsBackAndSaysSo() {
        MacOSBuildHints h = parse(raw("macos.distribution", "sideload"), "com.example");
        assertEquals(MacOSBuildHints.DISTRIBUTION_DEVELOPER_ID, h.getDistribution());
        assertEquals(1, h.getWarnings().size());
        assertTrue(h.getWarnings().get(0).contains("sideload"));
    }

    @Test
    public void theBundleIdIsSeparateFromTheIosOneByDefault() {
        // A macOS app and an iOS app are distinct products in App Store Connect.
        // Sharing an identifier is a submission failure found late.
        assertEquals("com.example.app.mac", parse(raw(), "com.example.app").getBundleId());
        assertEquals("com.example.app",
                parse(raw("macos.deriveBundleId", "true"), "com.example.app").getBundleId());
        assertEquals("com.acme.custom",
                parse(raw("macos.bundleId", "com.acme.custom"), "com.example.app").getBundleId());
    }

    /// The store requires the sandbox, so an explicit macos.sandbox=false is
    /// refused there and recorded as a warning rather than applied. Honoring it
    /// would build a package rejected at submission, days later, by email.
    @Test
    public void appStoreSandboxIsNotOptional() {
        assertTrue(parse(raw("macos.distribution", "appStore"), "p").isSandboxedFor("appStore"));
        assertFalse(parse(raw("macos.distribution", "developerID"), "p")
                .isSandboxedFor("developerID"));

        MacOSBuildHints off = parse(raw("macos.distribution", "both", "macos.sandbox", "false"), "p");
        assertTrue(off.isSandboxedFor("appStore"));
        assertFalse(off.isSandboxedFor("developerID"));
        assertEquals(1, off.getWarnings().size());
        assertTrue(off.getWarnings().get(0).contains("macos.sandbox=false"));
    }

    @Test
    public void packagingFollowsTheChannelUnlessSaidOtherwise() {
        assertEquals("dmg", parse(raw("macos.distribution", "developerID"), "p")
                .getPackagingFor("developerID"));
        assertEquals("pkg", parse(raw("macos.distribution", "appStore"), "p")
                .getPackagingFor("appStore"));
        assertEquals("app", parse(raw("macos.packaging", "app"), "p")
                .getPackagingFor("developerID"));
    }

    @Test
    public void hardenedRuntimeIsOnUnlessTurnedOff() {
        assertTrue(parse(raw(), "p").isHardenedRuntime());
        assertFalse(parse(raw("macos.hardenedRuntime", "false"), "p").isHardenedRuntime());
    }

    @Test
    public void bothChannelsBuildWhenDistributionIsBoth() {
        MacOSBuildHints both = parse(raw("macos.distribution", "both"), "p");
        assertTrue(both.buildsAppStoreChannel());
        assertTrue(both.buildsDeveloperIdChannel());

        MacOSBuildHints devId = parse(raw("macos.distribution", "developerID"), "p");
        assertFalse(devId.buildsAppStoreChannel());
        assertTrue(devId.buildsDeveloperIdChannel());
    }

    /// distribution=both is two builds, and the builder loops over exactly this
    /// list. If it ever answered with one entry, "both" would quietly ship one
    /// channel -- which is what it used to do.
    @Test
    public void channelsAreEnumeratedForTheBuilderToLoopOver() {
        assertEquals(Arrays.asList("appStore", "developerID"),
                parse(raw("macos.distribution", "both"), "p").getChannels());
        assertEquals(Collections.singletonList("developerID"),
                parse(raw(), "p").getChannels());
        assertEquals(Collections.singletonList("appStore"),
                parse(raw("macos.distribution", "appStore"), "p").getChannels());
    }

    /// Each channel of a distribution=both build gets its own container: a pkg
    /// for the store because that is what you upload, a dmg for direct download.
    /// One artifact that is neither would serve neither.
    @Test
    public void packagingIsResolvedPerChannel() {
        MacOSBuildHints both = parse(raw("macos.distribution", "both"), "p");
        assertEquals("pkg", both.getPackagingFor("appStore"));
        assertEquals("dmg", both.getPackagingFor("developerID"));
    }

    /// An explicit macos.packaging steers the Developer ID channel and is
    /// ignored for the App Store one, which is always a pkg.
    ///
    /// This assertion used to read the other way, and what it pinned was a build
    /// that reported success while producing nothing submittable: App Store
    /// Connect takes a pkg, so a dmg or a zipped .app is rejected, and the
    /// developer finds out by hand at upload time rather than from the build.
    /// Same reasoning as the sandbox, which was already refused for this
    /// channel.
    /// The shared installer hint is refused only when two PACKAGES would carry
    /// it, which is not the same as two channels.
    ///
    /// It has been wrong in both directions: first answering for both channels,
    /// so an App Store package and a Developer ID package were signed with one
    /// certificate and at least one was rejected after a green build; then
    /// refusing whenever a build had two channels, which broke the ordinary
    /// distribution=both default where only the store channel produces a pkg at
    /// all.
    @Test
    public void theSharedInstallerIdentityAnswersWhileOnlyOnePackageIsSigned() {
        // One channel: unambiguous.
        assertEquals("Developer ID Installer: Someone",
                parse(raw("macos.distribution", "developerID",
                        "macos.packaging", "pkg",
                        "macos.signingIdentity.installer", "Developer ID Installer: Someone"), "p")
                        .getInstallerIdentityFor("developerID"));

        // Two channels on the DEFAULT packaging: pkg for the store, dmg for
        // direct download, so exactly one package is signed.
        MacOSBuildHints dflt = parse(raw("macos.distribution", "both",
                "macos.signingIdentity.installer", "3rd Party Mac Developer Installer: Someone"), "p");
        assertEquals("3rd Party Mac Developer Installer: Someone",
                dflt.getInstallerIdentityFor("appStore"));

        // Two channels that both produce a package: no single certificate can
        // be right, so the shared hint is refused rather than misapplied.
        MacOSBuildHints bothPkg = parse(raw("macos.distribution", "both",
                "macos.packaging", "pkg",
                "macos.signingIdentity.installer", "3rd Party Mac Developer Installer: Someone"), "p");
        assertNull(bothPkg.getInstallerIdentityFor("appStore"));
        assertNull(bothPkg.getInstallerIdentityFor("developerID"));

        // Spelled in upper case, which the builder accepts with
        // equalsIgnoreCase and therefore genuinely produces two packages. Every
        // comparison in the hints class was lowercase, so an uppercase spelling
        // used to slip past this guard and hand one installer certificate to
        // both channels -- the precise case it exists to stop.
        MacOSBuildHints shouty = parse(raw("macos.distribution", "both",
                "macos.packaging", "PKG",
                "macos.signingIdentity.installer", "3rd Party Mac Developer Installer: Someone"), "p");
        assertEquals("pkg", shouty.getPackagingFor("developerID"));
        assertNull(shouty.getInstallerIdentityFor("appStore"));
        assertNull(shouty.getInstallerIdentityFor("developerID"));

        // A per-channel identity always wins, including then.
        MacOSBuildHints perChannel = parse(raw("macos.distribution", "both",
                "macos.packaging", "pkg",
                "macos.signingIdentity.installer", "shared",
                "macos.signingIdentity.installer.appStore", "3rd Party Mac Developer Installer: A",
                "macos.signingIdentity.installer.developerID", "Developer ID Installer: B"), "p");
        assertEquals("3rd Party Mac Developer Installer: A",
                perChannel.getInstallerIdentityFor("appStore"));
        assertEquals("Developer ID Installer: B",
                perChannel.getInstallerIdentityFor("developerID"));

        // And one channel overriding is enough to leave the shared value
        // unambiguous for the other. Two packages are produced, but only one of
        // them would ever consume this certificate, so refusing it made
        // buildPkg fail for a Developer ID Installer the developer had
        // supplied. Counting packaged channels rather than sharing ones is the
        // same defect the case above already records, one level in.
        MacOSBuildHints oneOverridden = parse(raw("macos.distribution", "both",
                "macos.packaging", "pkg",
                "macos.signingIdentity.installer", "Developer ID Installer: Shared",
                "macos.signingIdentity.installer.appStore",
                "3rd Party Mac Developer Installer: A"), "p");
        assertEquals("3rd Party Mac Developer Installer: A",
                oneOverridden.getInstallerIdentityFor("appStore"));
        assertEquals("Developer ID Installer: Shared",
                oneOverridden.getInstallerIdentityFor("developerID"));

        // A blank override is not an override. It is how a settings file spells
        // "unset", so both channels are still claimants and the shared value is
        // still refused -- otherwise an empty hint would quietly re-enable the
        // misapplication this guard exists to stop.
        MacOSBuildHints blankOverride = parse(raw("macos.distribution", "both",
                "macos.packaging", "pkg",
                "macos.signingIdentity.installer", "3rd Party Mac Developer Installer: Someone",
                "macos.signingIdentity.installer.appStore", "   "), "p");
        assertNull(blankOverride.getInstallerIdentityFor("appStore"));
        assertNull(blankOverride.getInstallerIdentityFor("developerID"));
    }

    /// The build number resolves modern, then legacy, then the iOS spelling an
    /// existing Catalyst settings file actually carries. The builder read the
    /// first and third directly and skipped the second, so a project migrating on
    /// the promised macNative. spelling shipped the application version as its
    /// build number -- a duplicate one whenever the application version had not
    /// moved, which distribution rejects.
    @Test
    public void theBuildNumberHonoursTheLegacySpellingBetweenTheOtherTwo() {
        assertEquals("41", parse(raw("macos.bundleVersion", "41"), "p").getBundleVersion("1.0"));
        assertEquals("42", parse(raw("macNative.bundleVersion", "42"), "p").getBundleVersion("1.0"));
        assertEquals("43", parse(raw("ios.bundleVersion", "43"), "p").getBundleVersion("1.0"));

        // Precedence, in both directions that matter.
        assertEquals("modern", parse(raw("macos.bundleVersion", "modern",
                "macNative.bundleVersion", "legacy"), "p").getBundleVersion("1.0"));
        assertEquals("legacy", parse(raw("macNative.bundleVersion", "legacy",
                "ios.bundleVersion", "ios"), "p").getBundleVersion("1.0"));

        // None set: the application version, which is what a project that never
        // chose a build number wants.
        assertEquals("1.0", parse(raw(), "p").getBundleVersion("1.0"));
    }

    /// macOS defaults to the MODERN native theme, unlike iOS.
    ///
    /// iOS defaults to the legacy iOS 7 theme on purpose, so shipped apps and
    /// their goldens are not disturbed. This port has never shipped, so there is
    /// nothing to disturb -- and iOS7Theme.res carries no $Dark styles at all,
    /// so defaulting to it gave a new macOS port an iPhone 7 look and no dark
    /// mode whatsoever, however carefully the application asked for one. That is
    /// what made every *_dark screenshot come out light.
    @Test
    public void theNativeThemeDefaultsToModernAndIsConstrainedToKnownModes() {
        assertEquals("modern", parse(raw(), "p").getThemeMode());
        assertEquals("ios7", parse(raw("macos.themeMode", "ios7"), "p").getThemeMode());
        assertEquals("ios7", parse(raw("macNative.themeMode", "ios7"), "p").getThemeMode());

        // The cross-platform meta hint, translated the way IPhoneBuilder
        // translates it.
        assertEquals("ios7", parse(raw("nativeTheme", "legacy"), "p").getThemeMode());
        assertEquals("modern", parse(raw("nativeTheme", "modern"), "p").getThemeMode());
        assertEquals("ios7", parse(raw("cn1.nativeTheme", "legacy"), "p").getThemeMode());

        // The value is interpolated into generated Java source, so it is
        // constrained to what the runtime understands rather than passed
        // through. A settings file is an upload, and a source file is an
        // injection site.
        assertEquals("modern",
                parse(raw("macos.themeMode", "\"); System.exit(1); //"), "p").getThemeMode());
        assertEquals("modern", parse(raw("macos.themeMode", "nonsense"), "p").getThemeMode());
    }

    /// Every remaining setting the builders used to read straight off the
    /// request, swept in one pass rather than one report at a time.
    ///
    /// Each skipped the legacy macNative. spelling this class promises
    /// everywhere else, so a project migrating off Catalyst on the documented
    /// name silently got the default -- a Developer ID bundle without the
    /// library-validation exception, an archive built Release when Debug was
    /// asked for, an app with no URL schemes. Three were reported one by one
    /// before it was worth reading the rest as one class of defect.
    @Test
    public void everySettingResolvesModernThenLegacyThenTheIosSpelling() {
        assertEquals("true", parse(raw("macNative.crypto.gcm", "true"), "p").getCryptoGcm());
        assertEquals("true", parse(raw("ios.crypto.gcm", "true"), "p").getCryptoGcm());
        // Unset is ON, not off: an iOS build of the same application gets GCM
        // whether it asked or not, because IPhoneBuilder's base crypto
        // replacement uncomments the GCM line by prefix overlap. Defaulting off
        // here made CryptoApiTest's AES-GCM round trip fail on macOS alone with
        // CN1_CRYPTO_E_UNSUPPORTED while passing on iOS.
        assertEquals("true", parse(raw(), "p").getCryptoGcm());
        // Explicitly off is still honoured, in every spelling, so an application
        // that wants the smaller symbol set can still say so.
        assertEquals("false", parse(raw("macos.crypto.gcm", "false"), "p").getCryptoGcm());
        assertEquals("false", parse(raw("ios.crypto.gcm", "false"), "p").getCryptoGcm());

        assertEquals("z.a", parse(raw("macNative.add_libs", "z.a"), "p").getAddLibs());
        assertEquals("i.a", parse(raw("ios.add_libs", "i.a"), "p").getAddLibs());

        assertTrue(parse(raw("macNative.sourceOnly", "true"), "p").isSourceOnly());
        assertFalse(parse(raw(), "p").isSourceOnly());

        assertTrue(parse(raw("macNative.loadsExternalCode", "true"), "p")
                .isLoadsExternalCode());
        assertFalse(parse(raw(), "p").isLoadsExternalCode());

        assertEquals("myapp", parse(raw("macNative.urlSchemes", "myapp"), "p").getUrlSchemes());
        assertEquals("iosapp", parse(raw("ios.urlSchemes", "iosapp"), "p").getUrlSchemes());
        assertEquals("single", parse(raw("ios.urlScheme", "single"), "p").getUrlSchemes());

        assertEquals("<key>A</key>",
                parse(raw("macNative.plistInject", "<key>A</key>"), "p").getPlistInject());
        assertEquals("<key>B</key>",
                parse(raw("ios.plistInject", "<key>B</key>"), "p").getPlistInject());

        // Precedence holds throughout: the modern spelling wins over both.
        assertEquals("modern-wins", parse(raw("macos.urlSchemes", "modern-wins",
                "macNative.urlSchemes", "legacy",
                "ios.urlSchemes", "ios"), "p").getUrlSchemes());
    }

    @Test
    public void appStorePackagingIsAlwaysPkg() {
        MacOSBuildHints pinned = parse(raw("macos.distribution", "both",
                "macos.packaging", "dmg"), "p");
        assertEquals("pkg", pinned.getPackagingFor("appStore"));
        assertEquals("dmg", pinned.getPackagingFor("developerID"));
        assertTrue("the ignored override has to be reported, not applied silently",
                warningMentioning(pinned, "macos.packaging=dmg"));

        // An App-Store-only build with an unsubmittable packaging is the case
        // that produced no usable artifact at all rather than one of two.
        MacOSBuildHints storeOnly = parse(raw("macos.distribution", "appStore",
                "macos.packaging", "app"), "p");
        assertEquals("pkg", storeOnly.getPackagingFor("appStore"));
        assertTrue(warningMentioning(storeOnly, "macos.packaging=app"));

        // pkg and both already yield a pkg for the store, so neither is an
        // override being refused and neither should warn.
        MacOSBuildHints fine = parse(raw("macos.distribution", "both",
                "macos.packaging", "both"), "p");
        assertEquals("pkg", fine.getPackagingFor("appStore"));
        assertFalse(warningMentioning(fine, "macos.packaging"));
    }

    private static boolean warningMentioning(MacOSBuildHints hints, String needle) {
        for (String w : hints.getWarnings()) {
            if (w.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    /// The store requires the sandbox; a directly distributed build usually does
    /// not want it. With distribution=both the store's requirement must not
    /// follow the download into the wild, where it breaks file access.
    @Test
    public void sandboxIsResolvedPerChannel() {
        MacOSBuildHints both = parse(raw("macos.distribution", "both"), "p");
        assertTrue(both.isSandboxedFor("appStore"));
        assertFalse(both.isSandboxedFor("developerID"));

        MacOSBuildHints explicit = parse(raw("macos.distribution", "both",
                "macos.sandbox", "true"), "p");
        assertTrue(explicit.isSandboxedFor("appStore"));
        assertTrue(explicit.isSandboxedFor("developerID"));
    }

    /// Each channel signs with its own certificate. Signing the store build with
    /// the Developer ID identity is a submission rejection.
    @Test
    public void signingIdentityIsResolvedPerChannel() {
        MacOSBuildHints h = parse(raw("macos.distribution", "both",
                "macos.signingIdentity.appStore", "3rd Party Mac Developer Application: X",
                "macos.signingIdentity.developerID", "Developer ID Application: X"), "p");
        assertEquals("3rd Party Mac Developer Application: X", h.getSigningIdentityFor("appStore"));
        assertEquals("Developer ID Application: X", h.getSigningIdentityFor("developerID"));
    }

    /// A project carrying only a team id signed under the Catalyst builder,
    /// which defaults these two strings. Leaving them null here produced an
    /// unsigned dmg from a distribution build -- something you paid for and
    /// cannot ship, with nothing in the log to say so.
    @Test
    public void signingIdentitiesCarryTheCatalystDefaults() {
        MacOSBuildHints h = parse(raw("macos.distribution", "both"), "p");
        assertEquals("Apple Distribution", h.getSigningIdentityFor("appStore"));
        assertEquals("Developer ID Application", h.getSigningIdentityFor("developerID"));
    }

    /// "none" is how a smoke build asks for no signature. An empty value cannot
    /// say it, because an empty hint reads as unset and takes the default.
    @Test
    public void unsignedIsRequestedByName() {
        assertNull(parse(raw("macos.signingIdentity.developerID", "none"), "p")
                .getSigningIdentityFor("developerID"));
        assertNull(parse(raw("macNative.signingIdentity.developerID", "NONE"), "p")
                .getSigningIdentityFor("developerID"));
    }

    /// The full legacy chain, not just the first link: a project that only ever
    /// set ios.teamId or ios.debug.teamId signed under the Catalyst builder, and
    /// a missing DEVELOPMENT_TEAM either fails automatic signing or picks another
    /// team the account belongs to.
    @Test
    public void teamIdFallsBackThroughEveryIosSpelling() {
        assertEquals("REL", parse(raw("ios.release.teamId", "REL", "ios.teamId", "GEN",
                "ios.debug.teamId", "DBG"), "p").getTeamId());
        assertEquals("GEN", parse(raw("ios.teamId", "GEN", "ios.debug.teamId", "DBG"), "p")
                .getTeamId());
        assertEquals("DBG", parse(raw("ios.debug.teamId", "DBG"), "p").getTeamId());
        assertEquals("MAC", parse(raw("macos.teamId", "MAC", "ios.release.teamId", "REL"), "p")
                .getTeamId());
    }

    /// Manual by default even though the docs used to promise automatic: a build
    /// server has an installed certificate and no Xcode account session, and
    /// automatic signing there fails asking to sign in.
    @Test
    public void signingStyleIsManualUnlessAutomaticIsAskedFor() {
        assertFalse(parse(raw(), "p").usesAutomaticSigning());
        assertTrue(parse(raw("macos.signing.style", "automatic"), "p").usesAutomaticSigning());
        assertFalse(parse(raw("macos.signing.style", "manual"), "p").usesAutomaticSigning());
    }

    /// The documented macos.entitlements.* family, which nothing read: every one
    /// of these was silently dropped, so a build asking for JIT or a network
    /// server or an app group got a signature without it.
    @Test
    public void entitlementOverridesAreHonoured() {
        MacOSBuildHints h = parse(raw(
                "macos.distribution", "developerID",
                "macos.entitlements.appSandbox", "true",
                "macos.entitlements.network.server", "true",
                "macos.entitlements.files.userSelected", "readonly",
                "macos.entitlements.allowJit", "true",
                "macos.entitlements.extra", "<key>x</key><true/>"), "p");
        MacOSBuildHints.EntitlementOverrides o = h.entitlementsFor("developerID");
        assertTrue(o.isSandbox());
        assertTrue(o.networkServer(false));
        assertEquals("readonly", o.getFilesUserSelected());
        assertTrue(o.isAllowJit());
        assertEquals("<key>x</key><true/>", o.getExtra());

        // The legacy spelling still means the same thing.
        assertTrue(parse(raw("macNative.entitlements.allowJit", "true"), "p")
                .entitlementsFor("developerID").isAllowJit());
    }

    /// Tri-state: unset follows the capability scan, and an explicit value
    /// overrides it in BOTH directions -- so a cn1lib the scanner cannot see can
    /// still ask for the camera, and a linked-but-unused API can decline it.
    @Test
    public void deviceEntitlementsAreTriState() {
        // Unset follows the scan, so it answers whatever it is handed.
        assertTrue(parse(raw(), "p").entitlementsFor("appStore").camera(true));
        assertFalse(parse(raw(), "p").entitlementsFor("appStore").camera(false));
        // Explicit wins in both directions -- including over a scan that did NOT
        // find the capability, which is what lets a cn1lib ask for it.
        assertTrue(parse(raw("macos.entitlements.device.camera", "true"), "p")
                .entitlementsFor("appStore").camera(false));
        assertFalse(parse(raw("macos.entitlements.device.camera", "false"), "p")
                .entitlementsFor("appStore").camera(true));
    }

    /// The same sentence on both platforms, so nobody writes it twice: the iOS
    /// spelling is what the feature catalog and existing settings files carry.
    @Test
    public void usageDescriptionsFallBackToTheIosSpelling() {
        assertEquals("scan a code", parse(raw("ios.NSCameraUsageDescription", "scan a code"), "p")
                .getUsageDescription("NSCameraUsageDescription"));
        assertEquals("mac wording", parse(raw("ios.NSCameraUsageDescription", "scan a code",
                "macos.NSCameraUsageDescription", "mac wording"), "p")
                .getUsageDescription("NSCameraUsageDescription"));
        assertNull(parse(raw(), "p").getUsageDescription("NSCameraUsageDescription"));
    }

    /// The installer certificate is not the application certificate, so it is
    /// asked for rather than derived: productbuild signed with the application
    /// identity produces a package the store refuses.
    @Test
    public void installerIdentityHasItsOwnHintAndLegacySpelling() {
        assertEquals("3rd Party Mac Developer Installer: X",
                parse(raw("macos.signingIdentity.installer",
                        "3rd Party Mac Developer Installer: X"), "p").getInstallerIdentity());
        assertEquals("Developer ID Installer: X",
                parse(raw("macNative.signingIdentity.installer",
                        "Developer ID Installer: X"), "p").getInstallerIdentity());
        assertNull(parse(raw(), "p").getInstallerIdentity());
    }

    /// The builder submits hints.getNotarizePassword(), not the raw modern key,
    /// so a project still spelling it macNative.* notarizes rather than sending
    /// notarytool an empty password.
    @Test
    public void notarizePasswordHonoursTheLegacySpelling() {
        assertEquals("app-specific",
                parse(raw("macNative.notarize.password", "app-specific"), "p")
                        .getNotarizePassword());
        assertEquals("modern",
                parse(raw("macos.notarize.password", "modern",
                        "macNative.notarize.password", "legacy"), "p").getNotarizePassword());
    }

    /// An explicit opt-out has to clear the PRIVACY STRING, not just the
    /// entitlement.
    ///
    /// macos.entitlements.device.microphone=false is the documented escape for
    /// an application whose camera sessions are all silent. The effective
    /// capabilities were seeded from the scan and then OR-ed with the resolved
    /// values, which made the OR one-way: the resolver answered false, the
    /// entitlement was correctly omitted, and the Info.plist still declared
    /// NSMicrophoneUsageDescription -- the application declaring a device it
    /// had just been told not to use.
    @Test
    public void anExplicitOptOutClearsTheDeclaredCapability() {
        MacOSBuildHints hints = parse(raw(
                "macos.entitlements.device.microphone", "false"), "com.example.app");
        MacOSXcodeProject.MacOSCapabilities scanned = new MacOSXcodeProject.MacOSCapabilities();
        scanned.usesMicrophone = true;
        scanned.usesCamera = true;

        MacOSXcodeProject.MacOSCapabilities out =
                MacOSNativeBuilder.effectiveCapabilities(hints, scanned);
        assertFalse("an explicit false must clear the declaration", out.usesMicrophone);
        // And only the one that was opted out: the camera is untouched.
        assertTrue("the camera was not opted out", out.usesCamera);
    }

    /// With nothing opted out the scan still stands, so the fix cannot have
    /// quietly turned every capability off.
    @Test
    public void anUnhintedBuildKeepsWhatTheScanFound() {
        MacOSBuildHints hints = parse(raw(), "com.example.app");
        MacOSXcodeProject.MacOSCapabilities scanned = new MacOSXcodeProject.MacOSCapabilities();
        scanned.usesMicrophone = true;
        scanned.usesLocation = true;

        MacOSXcodeProject.MacOSCapabilities out =
                MacOSNativeBuilder.effectiveCapabilities(hints, scanned);
        assertTrue(out.usesMicrophone);
        assertTrue(out.usesLocation);
        assertFalse(out.usesBluetooth);
    }
}
