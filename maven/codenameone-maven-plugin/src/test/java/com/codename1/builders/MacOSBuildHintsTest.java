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
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// Covers the migration half of the macOS build hints.
///
/// A project that was building a Mac Catalyst app yesterday is building an
/// AppKit app today under the same target name, without editing anything. That
/// only holds if every `macNative.*` setting still means what it meant, so these
/// are the tests that say so.
public class MacOSBuildHintsTest {

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

        MacOSBuildHints pinned = parse(raw("macos.distribution", "both",
                "macos.packaging", "dmg"), "p");
        assertEquals("dmg", pinned.getPackagingFor("appStore"));
        assertEquals("dmg", pinned.getPackagingFor("developerID"));
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
}
