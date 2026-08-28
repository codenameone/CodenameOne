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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The manifest a packet tunnel needs.
///
/// Every assertion here is about something that fails SILENTLY: a VpnService
/// Android will not bind, a promotion it refuses, or a generated element that
/// suppressed the developer's own.
class VpnManifestFragmentsTest {

    @Test
    public void theServiceIsBindableByTheSystemOnly() {
        String out = VpnManifestFragments.services(true, "");
        // Without android:permission Android refuses the binding, and
        // establish() then answers null on a build that looks complete.
        assertTrue(out.contains(
                "android:permission=\"android.permission.BIND_VPN_SERVICE\""),
                "the permission attribute is what makes this a VPN service");
        assertTrue(out.contains("android.net.VpnService"),
                "the action is how the system finds it");
        // Mandatory from API 31 for any component with an intent filter.
        assertTrue(out.contains("android:exported="),
                "a component with an intent filter must say so from API 31");
    }

    @Test
    public void theServiceDeclaresTheTypeItPromotesWith() {
        // Android 14 refuses a promotion whose type the manifest does not
        // declare, and an unpromoted VpnService is one the platform shuts
        // down shortly after the tunnel comes up.
        assertTrue(VpnManifestFragments.services(true, "")
                .contains("android:foregroundServiceType=\"systemExempted\""));
    }

    @Test
    public void aTunnelBuildCompilesAgainstAnSdkThatKnowsItsType() {
        // systemExempted and the foregroundServiceType attribute arrive at
        // 34 and 29, and AAPT rejects an enum value the compile SDK has
        // never heard of -- so the legacy configuration, which is still
        // supported and can sit at 28, failed on the generated manifest
        // before anything was compiled.
        assertEquals(34, AndroidGradleBuilder.TUNNEL_MIN_COMPILE_SDK);
        assertEquals(34, AndroidGradleBuilder.compileSdkInt("28", "28", "28",
                false, false, false, true),
                "a tunnel build is raised to an SDK that knows its type");
        assertEquals(28, AndroidGradleBuilder.compileSdkInt("28", "28", "28",
                false, false, false, false),
                "and an app without one keeps its legacy compile SDK");
    }

    @Test
    public void nothingIsEmittedWithoutTheTunnel() {
        assertEquals("", VpnManifestFragments.services(false, ""));
        assertEquals("x", VpnManifestFragments.injectPermissions(false, "x"));
    }

    @Test
    public void thePromotionPermissionsAreDeclared() {
        String out = VpnManifestFragments.injectPermissions(true, "");
        assertTrue(out.contains("android.permission.FOREGROUND_SERVICE\""),
                "Android refuses the promotion without it");
        assertTrue(out.contains(
                "android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED"),
                "Android 14 wants the permission matching the type");
        // BIND_VPN_SERVICE is the system's, not the app's; an app that
        // declares it is asking for something it cannot be granted.
        assertFalse(out.contains("BIND_VPN_SERVICE"),
                "the service declares that the SYSTEM holds this one");
    }

    /** A declaration the system will actually bind as a VPN. */
    private static String bindable() {
        return "        <service android:name=\""
                + VpnManifestFragments.TUNNEL_SERVICE + "\""
                + " android:permission=\""
                + VpnManifestFragments.BIND_VPN_SERVICE + "\">\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\""
                + VpnManifestFragments.VPN_ACTION + "\" />\n"
                + "            </intent-filter>\n"
                + "        </service>\n";
    }

    @Test
    public void aProjectsOwnDeclarationIsNotDuplicated() {
        // A COMPLETE one. This test used to pass a bare
        // <service android:name="..."/>, which is exactly the declaration
        // the build must not stand aside for -- the assertion was encoding
        // the defect.
        assertEquals("", VpnManifestFragments.services(true, bindable()));
        String perms = "    <uses-permission android:name="
                + "\"android.permission.FOREGROUND_SERVICE\" />\n";
        assertEquals(1, count(VpnManifestFragments.injectPermissions(true, perms),
                "\"android.permission.FOREGROUND_SERVICE\""),
                "a permission the project already declares is not repeated");
    }

    @Test
    public void aDeclarationAndroidCannotBindIsRefused() {
        // Two things make a <service> a VPN service and neither is the class
        // name: android:permission says only the system, holding
        // BIND_VPN_SERVICE, may bind it, and the android.net.VpnService
        // action is how the system finds it. Suppressing the generated
        // element on the NAME alone let a declaration carrying neither
        // replace the working one -- a build that looks complete, refuses
        // the binding, fails at establish() with nothing to say why, and
        // goes on reporting Tunnels.isSupported() as true.
        //
        // Refused rather than merged: rewriting XML the project wrote is
        // guesswork about intent, and it is what the VoIP background mode
        // does one builder over for the same reason.
        String bare = "        <service android:name=\""
                + VpnManifestFragments.TUNNEL_SERVICE + "\" />\n";
        IllegalArgumentException both = assertThrows(
                IllegalArgumentException.class,
                () -> VpnManifestFragments.services(true, bare));
        assertTrue(both.getMessage().contains("neither"),
                "the message names both: " + both.getMessage());

        String noPermission = "        <service android:name=\""
                + VpnManifestFragments.TUNNEL_SERVICE + "\">\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\""
                + VpnManifestFragments.VPN_ACTION + "\" />\n"
                + "            </intent-filter>\n"
                + "        </service>\n";
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> VpnManifestFragments.services(true, noPermission))
                        .getMessage().contains("android:permission"),
                "a filter without the permission is still unbindable");

        String noFilter = "        <service android:name=\""
                + VpnManifestFragments.TUNNEL_SERVICE + "\""
                + " android:permission=\""
                + VpnManifestFragments.BIND_VPN_SERVICE + "\" />\n";
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> VpnManifestFragments.services(true, noFilter))
                        .getMessage().contains("intent-filter"),
                "and the permission alone leaves nothing to find it by");

        // A COMMENTED-OUT declaration is not one, so it is not judged
        // either: the build supplies its own and nothing is refused.
        String commented = "        <!-- <service android:name=\""
                + VpnManifestFragments.TUNNEL_SERVICE + "\" /> -->\n";
        assertTrue(VpnManifestFragments.services(true, commented)
                .contains(VpnManifestFragments.BIND_VPN_SERVICE),
                "the generated element still goes in");
    }

    @Test
    public void aCommentedOutDeclarationIsNotADeclaration() {
        // The lesson the call fragments learned twice: commenting a
        // declaration out is how a developer disables one, and treating it
        // as supplied means the manifest ships with neither.
        String mine = "        <!-- <service android:name=\""
                + VpnManifestFragments.TUNNEL_SERVICE + "\" /> -->\n";
        assertTrue(VpnManifestFragments.services(true, mine)
                .contains(VpnManifestFragments.TUNNEL_SERVICE));
        String perms = "    <!-- <uses-permission android:name="
                + "\"android.permission.FOREGROUND_SERVICE\" /> -->\n";
        assertTrue(VpnManifestFragments.injectPermissions(true, perms)
                .contains("<uses-permission android:name="
                        + "\"android.permission.FOREGROUND_SERVICE\""));
    }

    @Test
    public void aPermissionNamedAsAValueIsNotADeclaration() {
        // android:permission on a component names one it REQUIRES -- our own
        // <service> does exactly that with BIND_VPN_SERVICE.
        String mine = "        <service android:name=\"com.example.S\""
                + " android:permission=\"android.permission.FOREGROUND_SERVICE\" />\n";
        assertTrue(VpnManifestFragments.injectPermissions(true, mine)
                .contains("<uses-permission android:name="
                        + "\"android.permission.FOREGROUND_SERVICE\""),
                "requiring a permission is not declaring it");
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int at = haystack.indexOf(needle);
        while (at >= 0) {
            n++;
            at = haystack.indexOf(needle, at + needle.length());
        }
        return n;
    }
}
