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
/// #### Android only
///
/// [#isSupported] answers true on Android and false everywhere else,
/// including iOS: a packet tunnel there runs in a Network Extension, and the
/// translation that would give that process a virtual machine has not been
/// written. Ask [#isSupported] and keep a path for the answer being no --
/// [#start] refuses with `NOT_SUPPORTED` rather than pretending.
///
/// #### Do not assume the instance you pass is the one that runs
///
/// On Android it is: the tunnel runs in this process, inside the port's
/// `VpnService`, so everything it closed over is still there. That is a
/// property of one platform rather than of this API -- a tunnel hosted in
/// another process would be CONSTRUCTED there, with none of the app's
/// statics, which is what [TunnelSetup#data] is for.
///
/// The rule that follows costs nothing on Android and is the only thing
/// that stays portable: a tunnel takes everything it needs from
/// [VpnTunnel#onStart]'s configuration.
public final class Tunnels {

    /// The tunnel each pending start belongs to, keyed by request.
    ///
    /// A map rather than one field, because a field is a race the port
    /// cannot close: the already-authorized path releases its reservation
    /// and calls startService, which returns before the service has read
    /// anything -- so a second start overwrote the field in between and the
    /// service ran the WRONG tunnel object under the first setup, then
    /// acknowledged the wrong request. The intent already carries the
    /// request id; claiming by that id is what makes the two agree.
    private static final java.util.Map<Integer, VpnTunnel> PENDING =
            new java.util.HashMap<Integer, VpnTunnel>();

    /// The tunnel the platform is currently running, once it claimed one.
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
        int id = VpnRequests.nextId();
        // Registered BEFORE the bridge is asked, and against THIS request.
        // On Android the service can be running and reading it back before
        // startCustomTunnel returns, so a tunnel registered afterwards left
        // the platform holding a live link with nothing to hand packets to.
        synchronized (Tunnels.class) {
            PENDING.put(Integer.valueOf(id), tunnel);
        }
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

    /// Takes the tunnel a particular start belongs to.
    ///
    /// Removes it, so two service commands carrying the same request cannot
    /// both run it, and records it as the running one.
    ///
    /// @param requestId the id the start intent carried
    /// @return the tunnel, or null when that start is not pending
    ///
    /// @hidden not part of the public API; called by the ports.
    public static VpnTunnel claim(int requestId) {
        synchronized (Tunnels.class) {
            VpnTunnel t = PENDING.remove(Integer.valueOf(requestId));
            if (t != null) {
                current = t;
            }
            return t;
        }
    }

    /// The tunnel the platform is running, or null.
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

    /// Forgets every tunnel, running or pending.
    ///
    /// Reached from VpnRequests.resetForTest, so a tunnel does not outlive
    /// the bridge that was running it -- in a test, or across a Display
    /// re-init where the platform holding it has gone.
    ///
    /// @hidden not part of the public API; test support.
    public static void resetForTest() {
        synchronized (Tunnels.class) {
            PENDING.clear();
            current = null;
        }
    }

    /// Drops a start that never reached the platform.
    ///
    /// @hidden not part of the public API; called by the ports.
    public static void abandon(int requestId) {
        synchronized (Tunnels.class) {
            PENDING.remove(Integer.valueOf(requestId));
        }
    }

    /// Answers a start or stop request.
    ///
    /// @hidden not part of the public API; called by the ports.
    public static void deliverAck(int requestId, boolean ok, int errorOrdinal,
            String message) {
        // The pending entry goes HERE, because this is the one point every
        // platform passes through. claim() covers a port that runs the
        // tunnel in this process, which is the only kind there is today; a
        // port that ran it elsewhere would never call claim, and its starts
        // would accumulate a tunnel object -- and everything the application
        // had closed over -- once per reconnect for the life of the process.
        // Removing here is a no-op where claim already did it, and is what
        // keeps that from being the next port's bug to find.
        synchronized (Tunnels.class) {
            PENDING.remove(Integer.valueOf(requestId));
        }
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
