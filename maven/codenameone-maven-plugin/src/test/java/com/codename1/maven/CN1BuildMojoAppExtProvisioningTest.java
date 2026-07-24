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
package com.codename1.maven;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class CN1BuildMojoAppExtProvisioningTest {

    @Test
    public void debugBuildPicksDebugQualifiedProvisionSetting() {
        Properties props = new Properties();
        props.setProperty("codename1.ios.debug.appext.CN1Widgets.provision", "/certs/dev.mobileprovision");
        props.setProperty("codename1.ios.release.appext.CN1Widgets.provision", "/certs/dist.mobileprovision");

        CN1BuildMojo.resolveAppExtensionBuildTypeQualifiers(props, "ios-device");

        assertEquals("/certs/dev.mobileprovision",
                props.getProperty("codename1.ios.appext.CN1Widgets.provision"));
        assertNull(props.getProperty("codename1.ios.debug.appext.CN1Widgets.provision"));
        assertNull(props.getProperty("codename1.ios.release.appext.CN1Widgets.provision"));
    }

    @Test
    public void releaseBuildPicksReleaseQualifiedProvisioningURLHint() {
        Properties props = new Properties();
        props.setProperty("codename1.arg.ios.debug.appext.CN1Widgets.provisioningURL", "https://example.com/dev");
        props.setProperty("codename1.arg.ios.release.appext.CN1Widgets.provisioningURL", "https://example.com/dist");

        CN1BuildMojo.resolveAppExtensionBuildTypeQualifiers(props, "ios-device-release");

        assertEquals("https://example.com/dist",
                props.getProperty("codename1.arg.ios.appext.CN1Widgets.provisioningURL"));
        assertNull(props.getProperty("codename1.arg.ios.debug.appext.CN1Widgets.provisioningURL"));
        assertNull(props.getProperty("codename1.arg.ios.release.appext.CN1Widgets.provisioningURL"));
    }

    @Test
    public void onDeviceDebugTargetCountsAsDebug() {
        Properties props = new Properties();
        props.setProperty("codename1.arg.ios.debug.appext.CN1Widgets.provisioningURL", "https://example.com/dev");

        CN1BuildMojo.resolveAppExtensionBuildTypeQualifiers(props, "ios-on-device-debug");

        assertEquals("https://example.com/dev",
                props.getProperty("codename1.arg.ios.appext.CN1Widgets.provisioningURL"));
    }

    @Test
    public void qualifiedKeyOverridesUnqualifiedFallback() {
        Properties props = new Properties();
        props.setProperty("codename1.ios.appext.CN1Widgets.provision", "/certs/fallback.mobileprovision");
        props.setProperty("codename1.ios.debug.appext.CN1Widgets.provision", "/certs/dev.mobileprovision");

        CN1BuildMojo.resolveAppExtensionBuildTypeQualifiers(props, "ios-device");

        assertEquals("/certs/dev.mobileprovision",
                props.getProperty("codename1.ios.appext.CN1Widgets.provision"));
    }

    @Test
    public void unqualifiedFallbackSurvivesWhenOnlyOtherBuildTypeIsQualified() {
        Properties props = new Properties();
        props.setProperty("codename1.ios.appext.CN1Widgets.provision", "/certs/fallback.mobileprovision");
        props.setProperty("codename1.ios.release.appext.CN1Widgets.provision", "/certs/dist.mobileprovision");

        CN1BuildMojo.resolveAppExtensionBuildTypeQualifiers(props, "ios-device");

        assertEquals("/certs/fallback.mobileprovision",
                props.getProperty("codename1.ios.appext.CN1Widgets.provision"));
        assertNull(props.getProperty("codename1.ios.release.appext.CN1Widgets.provision"));
    }

    @Test
    public void blankQualifiedValueFallsBackToUnqualified() {
        Properties props = new Properties();
        props.setProperty("codename1.ios.appext.CN1Widgets.provision", "/certs/fallback.mobileprovision");
        props.setProperty("codename1.ios.debug.appext.CN1Widgets.provision", "  ");

        CN1BuildMojo.resolveAppExtensionBuildTypeQualifiers(props, "ios-device");

        assertEquals("/certs/fallback.mobileprovision",
                props.getProperty("codename1.ios.appext.CN1Widgets.provision"));
        assertFalse(props.containsKey("codename1.ios.debug.appext.CN1Widgets.provision"));
    }

    @Test
    public void unrelatedKeysAreLeftAlone() {
        Properties props = new Properties();
        props.setProperty("codename1.ios.debug.provision", "/certs/app-dev.mobileprovision");
        props.setProperty("codename1.arg.ios.debug.teamId", "TEAM123");
        props.setProperty("codename1.arg.ios.appext.CN1Widgets.provisioningURL", "https://example.com/single");

        CN1BuildMojo.resolveAppExtensionBuildTypeQualifiers(props, "ios-device-release");

        assertEquals("/certs/app-dev.mobileprovision", props.getProperty("codename1.ios.debug.provision"));
        assertEquals("TEAM123", props.getProperty("codename1.arg.ios.debug.teamId"));
        assertEquals("https://example.com/single",
                props.getProperty("codename1.arg.ios.appext.CN1Widgets.provisioningURL"));
    }

    @Test
    public void worksGenericallyForAnyExtensionNameAndArg() {
        Properties props = new Properties();
        props.setProperty("codename1.arg.ios.release.appext.MyWalletExtension.provisioningURL",
                "https://example.com/wallet-dist");

        CN1BuildMojo.resolveAppExtensionBuildTypeQualifiers(props, "ios-device-release");

        assertEquals("https://example.com/wallet-dist",
                props.getProperty("codename1.arg.ios.appext.MyWalletExtension.provisioningURL"));
    }
}
