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
package com.codename1.vpn.tunnel;

import com.codename1.impl.vpn.TunnelWire;
import com.codename1.impl.vpn.VpnRequests;
import com.codename1.impl.vpn.VpnWire;
import com.codename1.impl.async.EdtResult;
import com.codename1.util.AsyncResource;
import com.codename1.vpn.VpnError;
import com.codename1.vpn.VpnException;
import com.codename1.vpn.spi.VpnBridge;

/// Starts and stops a packet tunnel this application implements.
///
/// ```java
/// if (Tunnels.isSupported()) {
///     Tunnels.start(new MyTunnel(), new TunnelSetup()
///             .address("10.0.0.2/32")
///             .route("0.0.0.0/0")
///             .dnsServer("10.0.0.1")
///             .data(sessionToken));
/// }
/// ```
///
/// #### The instance you pass is not always the one that runs
///
/// On Android it is: the tunnel runs in this process, inside the generated
/// `VpnService`. On iOS the tunnel runs in a Network Extension -- another
/// process, with its own copy of the VM -- so the instance handed here is
/// used to learn WHICH tunnel to construct, and a fresh one is built over
/// there. That is why [TunnelSetup#data] exists and why a field the app set
/// on its tunnel object before calling this is not there when `onStart`
/// runs.
///
/// The rule that follows: a tunnel takes everything it needs from
/// [VpnTunnel#onStart]'s configuration. Anything else is a static that
/// happens to work on one platform.
public final class Tunnels {

    private static VpnTunnel current;

    private Tunnels() {
    }

    /// Whether this platform can run a tunnel the application implements.
    ///
    /// False is the ordinary answer on most ports, and it is the query to
    /// branch on -- not a platform test, which would go stale the moment a
    /// port gained the capability.
    public static boolean isSupported() {
        VpnBridge b = VpnRequests.bridge();
        return b != null && b.isCustomTunnelSupported();
    }

    /// Brings the tunnel up, resolving true once the platform has it.
    ///
    /// Shows the system's VPN consent prompt where one is needed, so the
    /// result can be a [com.codename1.vpn.VpnError#USER_DECLINED] failure.
    ///
    /// @param tunnel the application's packet loop
    /// @param setup  what the platform should establish
    /// @return true once the tunnel is up
    public static AsyncResource<Boolean> start(VpnTunnel tunnel,
            TunnelSetup setup) {
        if (tunnel == null || setup == null) {
            return failed(VpnError.INVALID_CONFIGURATION,
                    "A tunnel and a setup are both required");
        }
        VpnBridge b = VpnRequests.bridge();
        if (b == null || !b.isCustomTunnelSupported()) {
            return failed(VpnError.NOT_SUPPORTED, null);
        }
        // Registered BEFORE the bridge is asked. On Android the service can
        // be running and calling back into TunnelRunner before startTunnel
        // returns, and a tunnel registered after that call had the platform
        // holding a live link with nothing to hand its packets to.
        synchronized (Tunnels.class) {
            current = tunnel;
        }
        int id = VpnRequests.nextId();
        EdtResult<Boolean> r = VpnRequests.openAck(id);
        b.startCustomTunnel(id, TunnelWire.encodeSetup(setup));
        return r;
    }

    /// Takes the tunnel down.
    ///
    /// Answers even where no tunnel is running, because an app that has lost
    /// track of its own state must be able to ask for the stopped one.
    public static AsyncResource<Boolean> stop() {
        VpnBridge b = VpnRequests.bridge();
        if (b == null || !b.isCustomTunnelSupported()) {
            return failed(VpnError.NOT_SUPPORTED, null);
        }
        int id = VpnRequests.nextId();
        EdtResult<Boolean> r = VpnRequests.openAck(id);
        b.stopCustomTunnel(id);
        return r;
    }

    /// The tunnel this application registered, or null.
    ///
    /// @hidden not part of the public API; called by the ports.
    public static VpnTunnel getRegistered() {
        synchronized (Tunnels.class) {
            return current;
        }
    }

    /// Forgets the registered tunnel once the platform has let it go.
    ///
    /// @hidden not part of the public API; called by the ports.
    public static void clearRegistered() {
        synchronized (Tunnels.class) {
            current = null;
        }
    }

    /// Answers a start or stop request.
    ///
    /// @hidden not part of the public API; called by the ports.
    public static void deliverAck(int requestId, boolean ok, int errorOrdinal,
            String message) {
        EdtResult<Boolean> r = VpnRequests.takeAck(requestId);
        if (r == null) {
            return;
        }
        if (ok) {
            r.complete(Boolean.TRUE);
        } else {
            r.error(VpnWire.decodeError(errorOrdinal, message));
        }
    }

    private static AsyncResource<Boolean> failed(VpnError e, String message) {
        EdtResult<Boolean> r = new EdtResult<Boolean>();
        r.error(message == null ? new VpnException(e)
                : new VpnException(e, message));
        return r;
    }
}
