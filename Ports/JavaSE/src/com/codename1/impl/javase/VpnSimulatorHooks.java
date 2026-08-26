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
package com.codename1.impl.javase;

import com.codename1.impl.vpn.LocalVpnBridge;
import com.codename1.vpn.VpnStatus;

/// Simulator hooks that script the simulated VPN.
///
/// Registered in `META-INF/codenameone/simulator-hooks.properties`. Like the
/// call hooks these reproduce the awkward cases rather than the working one:
/// a declined prompt, credentials that are refused only after the profile
/// installed cleanly, and a tunnel that drops underneath a running app.
public final class VpnSimulatorHooks {

    private VpnSimulatorHooks() {
    }

    private static LocalVpnBridge bridge() {
        return JavaSEPort.getSimulatedVpn();
    }

    /// Makes the next install prompt be declined.
    ///
    /// Both platforms prompt and neither lets an app skip it, so a decline is
    /// an ordinary outcome -- and one apps routinely treat as impossible.
    public static void declineNextInstall() {
        bridge().setUserAccepts(false);
    }

    /// Accepts install prompts again.
    public static void acceptInstalls() {
        bridge().setUserAccepts(true);
    }

    /// Makes the server refuse the credentials on connect.
    ///
    /// The install and the connection fail in different places for different
    /// reasons. An app that only handles the first shows "connected" over a
    /// tunnel that never came up.
    public static void refuseCredentials() {
        bridge().setAuthenticates(false);
    }

    /// Accepts the credentials again.
    public static void acceptCredentials() {
        bridge().setAuthenticates(true);
    }

    /// Drops a running tunnel, without the app asking.
    public static void dropTheTunnel() {
        bridge().setStatus(VpnStatus.DISCONNECTED);
    }

    /// Makes the port report no VPN support.
    public static void makeVpnUnsupported() {
        bridge().setSupported(false);
    }

    /// Restores VPN support.
    public static void makeVpnSupported() {
        bridge().setSupported(true);
    }

    /// Claims custom packet-tunnel support, which no desktop really has.
    public static void enableCustomTunnel() {
        bridge().setTunnelSupported(true);
    }
}
