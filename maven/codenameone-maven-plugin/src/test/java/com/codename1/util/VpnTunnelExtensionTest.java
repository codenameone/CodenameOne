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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The generated packet-tunnel extension.
///
/// This target is unlike the other generated extensions in one way that
/// matters: it HOSTS A VM and runs the application's own Java. So the checks
/// here are about the three things that make that work and fail silently when
/// they do not -- the VM being initialised, the writer being installed before
/// the tunnel can forward anything, and the read being re-armed.
class VpnTunnelExtensionTest {

    private static String provider() {
        return IOSVpnTunnelExtensionBuilder.providerSource(
                "com.example.MyTunnel");
    }

    private static String text(Map<String, byte[]> files, String name)
            throws Exception {
        return new String(files.get(name), "UTF-8");
    }

    @Test
    void theVmIsStartedBeforeAnyJavaRuns() {
        String src = provider();
        assertTrue(src.contains("initConstantPool()"),
                "the extension has no UIApplicationMain to do this for it");
        assertTrue(src.contains("dispatch_once"),
                "start and stop repeat within one process; initialising twice"
                + " would reset every static the tunnel holds");
    }

    @Test
    void theWriterIsInstalledBeforeTheTunnelIsConstructed() {
        String src = provider();
        int install = src.indexOf("IOSExtensionTunnel_install__(");
        // The CONSTRUCTION, not the extern that declares the allocator: the
        // extern necessarily comes first, and matching it made this compare
        // the wrong two positions and pass whatever the order really was.
        int construct = src.indexOf("JAVA_OBJECT tunnel = __NEW_");
        assertTrue(install >= 0, "the writer has to be installed");
        assertTrue(construct >= 0, "the app's tunnel has to be constructed");
        assertTrue(install < construct,
                "onStart may forward a packet, and a forward before the"
                + " writer is installed is dropped with nothing to say so");
    }

    @Test
    void theReadIsReArmedFromInsideItsOwnHandler() {
        String src = provider();
        // Twice: the initial arm after the settings are applied, and again
        // inside the completion handler. readPacketsWithCompletionHandler
        // delivers ONE batch, so an extension that does not ask again stops
        // receiving traffic and looks like a tunnel that hung.
        int first = src.indexOf("cn1ReadPackets");
        assertTrue(first >= 0);
        assertTrue(src.indexOf("cn1ReadPackets", first + 1) > 0,
                "the handler must arm the next batch");
    }

    @Test
    void theTunnelClassIsNamedRatherThanLookedUp() {
        // Class.forName would not survive obfuscation, which is why the
        // framework bans it -- so the class is baked in as a symbol at
        // build time.
        String src = provider();
        assertTrue(src.contains("com_example_MyTunnel"),
                "the tunnel is reached as a translated symbol");
        // A CALL, not the word. The comment above the extern explains why
        // reflection is not used here and names Class.forName doing it, so a
        // substring test for the name matches the explanation and reports
        // the opposite of what it meant to check.
        assertFalse(src.contains("forName("),
                "a name looked up at run time would be gone by then");
    }

    @Test
    void settingsAreAppliedBeforePacketsAreRead() {
        String src = provider();
        int settings = src.indexOf("setTunnelNetworkSettings");
        int read = src.indexOf("cn1ReadPackets");
        assertTrue(settings >= 0 && read > settings,
                "reading before the settings land returns nothing, for ever,"
                + " with no error");
    }

    @Test
    void anIpv6SetupRoutesTraffic() {
        // Addresses establish the interface and route nothing, so a v6
        // tunnel that assigned only addresses came up carrying nothing --
        // including one that asked for the default route.
        String src = provider();
        assertTrue(src.contains("v6s.includedRoutes"),
                "the v6 branch has to install routes, like the v4 one");
        assertTrue(src.contains("NEIPv6Route"),
                "v6 routes are their own class; the v4 helper cannot make"
                + " them");
    }

    @Test
    void searchDomainsReachTheLink() {
        // TunnelSetup documents iOS applying these, and field 4 was carried
        // across the wire and then never read -- so a short hostname that
        // resolved on Android did not here.
        String src = provider();
        assertTrue(src.contains("searchDomains"),
                "the documented behaviour has to be the implemented one");
        assertTrue(src.contains("cn1tnField(f, 4)"),
                "field 4 is where the wire puts them");
    }

    @Test
    void theInfoPlistDeclaresAPacketTunnel() throws Exception {
        Map<String, byte[]> files = IOSVpnTunnelExtensionBuilder.buildFileMap(
                "com.example.app", "My VPN", "1.0", "17",
                "com.example.MyTunnel");
        String plist = text(files, "Info.plist");
        assertTrue(plist.contains(
                "com.apple.networkextension.packet-tunnel"),
                "a bundle whose extension point does not match is never"
                + " started, and nothing reports that it was not");
        assertTrue(plist.contains("CN1VpnTunnelProvider"),
                "iOS instantiates the principal class directly");
        assertTrue(plist.contains("$(EXECUTABLE_NAME)"),
                "a project may rename the product; the plist has to follow");
    }

    @Test
    void theBundleIdIsUnderTheHostApp() {
        // An extension's identifier has to be prefixed by the host's, or the
        // App ID cannot be created and codesigning fails.
        assertEquals("com.example.app.vpntunnel",
                IOSVpnTunnelExtensionBuilder.bundleId("com.example.app"));
    }

    @Test
    void theClassSymbolIsMangledTheWayParparvmDoesIt() {
        assertEquals("com_example_MyTunnel",
                IOSVpnTunnelExtensionBuilder.mangle("com.example.MyTunnel"));
    }
}
