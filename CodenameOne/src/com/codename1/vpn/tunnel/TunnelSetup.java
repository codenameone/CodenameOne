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

/// What an application asks the platform to set up before its tunnel runs.
///
/// The mirror of [TunnelConfiguration]: this is what goes DOWN to the
/// platform, that is what comes back up to [VpnTunnel#onStart]. They are
/// separate types because they are not the same thing -- a setup is a
/// request, a configuration is what the platform actually established, and
/// a host that runs the tunnel in its own process decides the two apart.
///
/// Every field has a usable default, so the smallest tunnel is
/// `new TunnelSetup().address("10.0.0.2/32").route("0.0.0.0/0")`.
public final class TunnelSetup {

    /// The MTU a link gets when the application does not choose one.
    ///
    /// 1400 rather than 1500: a tunnel adds its own encapsulation, and a
    /// link sized for the physical MTU fragments every full-size packet.
    public static final int DEFAULT_MTU = 1400;

    private String address = "";
    private String server = "";
    private String[] routes = new String[0];
    private String[] dnsServers = new String[0];
    private String[] searchDomains = new String[0];
    private int mtu = DEFAULT_MTU;
    private String data = "";
    private String sessionName = "";

    /// The address this device takes on the tunnel, in CIDR form.
    ///
    /// The prefix may be left off -- `/32` for IPv4, `/128` for IPv6 -- but
    /// one that is written and unreadable FAILS the start with
    /// `INVALID_CONFIGURATION`; see [#route].
    public TunnelSetup address(String cidr) {
        this.address = cidr == null ? "" : cidr;
        return this;
    }

    /// The far end, which the platform excludes from the tunnel's own routes
    /// so the tunnel's own traffic does not loop back into it.
    public TunnelSetup server(String host) {
        this.server = host == null ? "" : host;
        return this;
    }

    /// A route directed into the tunnel, in CIDR form. Repeatable.
    ///
    /// `0.0.0.0/0` and `::/0` are the full tunnel. An unreadable prefix
    /// fails the start with `INVALID_CONFIGURATION` rather than being
    /// narrowed to a host route: `"0.0.0.0/o"` is a typo for the default
    /// route, and a tunnel that started and carried one address would let
    /// every packet the app believed it was protecting out in the clear.
    /// Checked in the simulation as well, so the refusal is not something
    /// found first on a device.
    public TunnelSetup route(String cidr) {
        routes = append(routes, cidr);
        return this;
    }

    /// A DNS server the tunnel provides. Repeatable.
    public TunnelSetup dnsServer(String address) {
        dnsServers = append(dnsServers, address);
        return this;
    }

    /// A DNS search domain. Repeatable.
    ///
    /// Android applies these. A platform that cannot express one ignores it
    /// rather than
    /// refusing the tunnel, because a search domain is a convenience and
    /// losing the tunnel over one is not a trade an app would choose.
    public TunnelSetup searchDomain(String domain) {
        searchDomains = append(searchDomains, domain);
        return this;
    }

    /// The link MTU. See [#DEFAULT_MTU].
    public TunnelSetup mtu(int bytes) {
        this.mtu = bytes > 0 ? bytes : DEFAULT_MTU;
        return this;
    }

    /// The name the system shows for this VPN session.
    public TunnelSetup sessionName(String name) {
        this.sessionName = name == null ? "" : name;
        return this;
    }

    /// Application data handed to [VpnTunnel#onStart] as
    /// [TunnelConfiguration#getData].
    ///
    /// The one thing that reaches the tunnel from the app WITHOUT relying
    /// on the two sharing a process. Android's does, so a static works
    /// there; a host that constructed the tunnel elsewhere shares no
    /// statics, no singletons and no open connections with the app. A
    /// tunnel that takes its token, server list or key from here is the one
    /// that does not have to be rewritten.
    public TunnelSetup data(String value) {
        this.data = value == null ? "" : value;
        return this;
    }

    /// @return the device's tunnel address in CIDR form
    public String getAddress() {
        return address;
    }

    /// @return the far end
    public String getServer() {
        return server;
    }

    /// @return the routes directed into the tunnel
    public String[] getRoutes() {
        return copy(routes);
    }

    /// @return the DNS servers the tunnel provides
    public String[] getDnsServers() {
        return copy(dnsServers);
    }

    /// @return the DNS search domains
    public String[] getSearchDomains() {
        return copy(searchDomains);
    }

    /// @return the link MTU
    public int getMtu() {
        return mtu;
    }

    /// @return the session name the system shows
    public String getSessionName() {
        return sessionName;
    }

    /// @return the application data, never null
    public String getData() {
        return data;
    }

    private static String[] append(String[] existing, String value) {
        if (value == null || value.length() == 0) {
            return existing;
        }
        String[] out = new String[existing.length + 1];
        System.arraycopy(existing, 0, out, 0, existing.length);
        out[existing.length] = value;
        return out;
    }

    private static String[] copy(String[] source) {
        String[] out = new String[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }
}
