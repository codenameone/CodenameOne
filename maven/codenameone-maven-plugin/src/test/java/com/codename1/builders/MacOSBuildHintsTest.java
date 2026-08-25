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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    @Test
    public void sandboxFollowsTheChannelUnlessSaidOtherwise() {
        assertTrue(parse(raw("macos.distribution", "appStore"), "p").isSandboxed());
        assertFalse(parse(raw("macos.distribution", "developerID"), "p").isSandboxed());
        // Explicit wins either way.
        assertTrue(parse(raw("macos.distribution", "developerID", "macos.sandbox", "true"), "p")
                .isSandboxed());
        assertFalse(parse(raw("macos.distribution", "appStore", "macos.sandbox", "false"), "p")
                .isSandboxed());
    }

    @Test
    public void packagingFollowsTheChannelUnlessSaidOtherwise() {
        assertEquals("dmg", parse(raw("macos.distribution", "developerID"), "p").getPackaging());
        assertEquals("pkg", parse(raw("macos.distribution", "appStore"), "p").getPackaging());
        assertEquals("both", parse(raw("macos.distribution", "both"), "p").getPackaging());
        assertEquals("app", parse(raw("macos.packaging", "app"), "p").getPackaging());
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
}
