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

/// What the tunnel was started with.
///
/// Handed to [VpnTunnel#onStart]. The addresses and routes are what the
/// PLATFORM was told to set up before any packet arrives; a tunnel reads
/// them to know what it is carrying, and does not set them here -- that is
/// the profile's job, and on iOS it happens in a different process.
public final class TunnelConfiguration {
    private final String server;
    private final String[] routes;
    private final String[] dnsServers;
    private final int mtu;
    private final String data;

    TunnelConfiguration(String server, String[] routes, String[] dnsServers,
            int mtu, String data) {
        this.server = server;
        this.routes = routes == null ? new String[0] : routes;
        this.dnsServers = dnsServers == null ? new String[0] : dnsServers;
        this.mtu = mtu;
        this.data = data;
    }

    /// The tunnel server address.
    public String getServer() {
        return server;
    }

    /// The routes directed into the tunnel, in CIDR form.
    public String[] getRoutes() {
        String[] copy = new String[routes.length];
        System.arraycopy(routes, 0, copy, 0, routes.length);
        return copy;
    }

    /// The DNS servers the tunnel provides.
    public String[] getDnsServers() {
        String[] copy = new String[dnsServers.length];
        System.arraycopy(dnsServers, 0, copy, 0, dnsServers.length);
        return copy;
    }

    /// The link MTU, which bounds what [VpnTunnel#forward] can send.
    public int getMtu() {
        return mtu;
    }

    /// Whatever the app attached when it started the tunnel, untouched.
    ///
    /// On iOS the tunnel runs in another process, so this is how anything
    /// the app knows reaches it -- there are no shared statics to read.
    public String getData() {
        return data;
    }
}
