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

/// Runs a [VpnTunnel] over a [TunnelTransport]. Called by the ports.
///
/// In this package rather than `impl` because it drives the tunnel's
/// package-private entry points, which exist so an application cannot invoke
/// its own lifecycle by hand.
///
/// @hidden not part of the public API.
public final class TunnelHost {

    private final VpnTunnel tunnel;
    private final TunnelTransport transport;
    private boolean stopped;

    /// @hidden not part of the public API.
    public TunnelHost(VpnTunnel tunnel, TunnelTransport transport) {
        this.tunnel = tunnel;
        this.transport = transport;
    }

    /// Starts the tunnel and, on a blocking transport, its read loop.
    ///
    /// Returns once the loop ends on Android, and immediately on iOS, where
    /// the provider is callback-driven and blocking would deadlock it --
    /// which is why [TunnelTransport#isBlocking] exists rather than the host
    /// testing the platform.
    ///
    /// @hidden not part of the public API.
    public void start(String server, String[] routes, String[] dns, int mtu,
            String data) {
        tunnel.attach(transport);
        tunnel.begin(new TunnelConfiguration(server, routes, dns, mtu, data));
        if (transport.isBlocking()) {
            loop();
        }
    }

    /// Delivers one batch, for a callback-driven transport.
    ///
    /// @hidden not part of the public API.
    public void pump() {
        PacketBuffer[] buffers = transport.buffers();
        int n = transport.read(buffers);
        for (int i = 0; i < n; i++) {
            tunnel.deliver(buffers[i]);
        }
    }

    /// Stops the tunnel, once.
    ///
    /// @hidden not part of the public API.
    public void stop(int reasonOrdinal) {
        synchronized (this) {
            if (stopped) {
                return;
            }
            stopped = true;
        }
        TunnelStopReason[] values = TunnelStopReason.values();
        tunnel.finish(reasonOrdinal < 0 || reasonOrdinal >= values.length
                ? TunnelStopReason.UNKNOWN : values[reasonOrdinal]);
        tunnel.attach(null);
        transport.close();
    }

    private void loop() {
        PacketBuffer[] buffers = transport.buffers();
        while (true) {
            synchronized (this) {
                if (stopped) {
                    return;
                }
            }
            int n = transport.read(buffers);
            if (n <= 0) {
                // Zero is how a blocking transport says the link is going
                // down; anything else and the loop would spin on a closed
                // descriptor for as long as the process lived.
                return;
            }
            for (int i = 0; i < n; i++) {
                tunnel.deliver(buffers[i]);
            }
        }
    }
}
