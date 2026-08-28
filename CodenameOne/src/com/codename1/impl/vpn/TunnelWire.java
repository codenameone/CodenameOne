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
package com.codename1.impl.vpn;

import com.codename1.impl.call.CallWire;
import com.codename1.vpn.tunnel.TunnelSetup;

/// The encoding a `TunnelSetup` crosses the SPI in.
///
/// Tab-delimited like every other record here, and for the same reason: the
/// iOS side parses it in C, where building a Java object is expensive and
/// error-prone. Lists are carried in one field, comma-separated, because a
/// variable field count cannot be indexed by position and every reader of a
/// positional record has to agree on where the fixed fields end.
///
/// @hidden not part of the public API.
public final class TunnelWire {

    /// The separator inside a list field. A comma cannot appear in a CIDR
    /// block, an IP address or a DNS name, so nothing legal needs escaping
    /// past what the field escape already does.
    private static final char LIST = ',';

    private TunnelWire() {
    }

    /// @param s the setup to encode
    /// @return the record, never null
    public static String encodeSetup(TunnelSetup s) {
        if (s == null) {
            return "";
        }
        return CallWire.join(new String[]{
            VpnWire.escape(s.getAddress()),
            VpnWire.escape(s.getServer()),
            VpnWire.escape(list(s.getRoutes())),
            VpnWire.escape(list(s.getDnsServers())),
            VpnWire.escape(list(s.getSearchDomains())),
            String.valueOf(s.getMtu()),
            VpnWire.escape(s.getSessionName()),
            VpnWire.escape(s.getData()),
        });
    }

    /// The device's address in CIDR form.
    public static String address(String[] fields) {
        return VpnWire.unescape(CallWire.field(fields, 0));
    }

    /// The far end.
    public static String server(String[] fields) {
        return VpnWire.unescape(CallWire.field(fields, 1));
    }

    /// The routes directed into the tunnel.
    public static String[] routes(String[] fields) {
        return items(VpnWire.unescape(CallWire.field(fields, 2)));
    }

    /// The DNS servers the tunnel provides.
    public static String[] dnsServers(String[] fields) {
        return items(VpnWire.unescape(CallWire.field(fields, 3)));
    }

    /// The DNS search domains.
    public static String[] searchDomains(String[] fields) {
        return items(VpnWire.unescape(CallWire.field(fields, 4)));
    }

    /// The link MTU, or the default when the field is unusable.
    public static int mtu(String[] fields) {
        String raw = CallWire.field(fields, 5).trim();
        if (raw.length() == 0) {
            return TunnelSetup.DEFAULT_MTU;
        }
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c < '0' || c > '9') {
                // Tested rather than caught. ParparVM does not check
                // CHECKCAST and a NumberFormatException here would be a
                // parse this side cannot see; a malformed MTU is a
                // configuration mistake, and the default is the recoverable
                // answer -- a tunnel that will not start teaches nobody
                // anything.
                return TunnelSetup.DEFAULT_MTU;
            }
        }
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : TunnelSetup.DEFAULT_MTU;
        } catch (NumberFormatException tooLarge) {
            return TunnelSetup.DEFAULT_MTU;
        }
    }

    /// The session name the system shows.
    public static String sessionName(String[] fields) {
        return VpnWire.unescape(CallWire.field(fields, 6));
    }

    /// The application data handed to the tunnel.
    public static String data(String[] fields) {
        return VpnWire.unescape(CallWire.field(fields, 7));
    }

    /// Splits a record into its fields.
    /// The address half of a `host/prefix` block.
    ///
    /// @param cidr the block, with or without a prefix
    /// @return the address, never null
    public static String host(String cidr) {
        int slash = cidr.indexOf('/');
        return slash < 0 ? cidr : cidr.substring(0, slash);
    }

    /// Refuses a `host/prefix` block that is not one, in EITHER half.
    ///
    /// Here rather than in a port so the simulation refuses what a device
    /// refuses. Android hands the address to `VpnService.Builder.addRoute`,
    /// which throws on a literal it cannot parse, so `route("not-an-ip/32")`
    /// failed there and started here -- and a setup approved in the
    /// simulator that cannot come up on a phone is the divergence this whole
    /// arrangement exists to remove.
    ///
    /// @param cidr the block, with or without a prefix
    /// @param what the setup field it came from
    /// @throws IllegalArgumentException when either half is unreadable
    public static void validate(String cidr, String what) {
        String host = host(cidr);
        if (!isAddressLiteral(host)) {
            throw new IllegalArgumentException("The " + what + " '" + cidr
                    + "' does not start with an IP address");
        }
        prefix(cidr, what);
    }

    /// Refuses a bare address that is not one.
    ///
    /// A DNS server is an address with no prefix, so it cannot go through
    /// validate(): "8.8.8.8/32" is not what the platform wants there. Same
    /// reasoning otherwise -- Android hands the value to
    /// VpnService.Builder.addDnsServer, which throws on a literal it cannot
    /// parse, so a setup the simulator accepted failed on a device.
    ///
    /// @param address the literal
    /// @param what    the setup field it came from
    /// @throws IllegalArgumentException when it is not an address
    public static void validateAddress(String address, String what) {
        if (!isAddressLiteral(address)) {
            throw new IllegalArgumentException("The " + what + " '" + address
                    + "' is not an IP address");
        }
    }

    /// Whether this is an IP address literal.
    ///
    /// Both families are PARSED, not shape-checked. The v6 half was a
    /// character test at first -- hex digits, colons and the dots of a
    /// mapped tail -- on the reasoning that the address has several
    /// notations and a hand-written parser would refuse one that works. That
    /// left "1::2::3" and ":::" passing here and failing in
    /// VpnService.Builder, which is the divergence this validation exists to
    /// remove; and the grammar is small enough to write out, so the reasoning
    /// was an argument for care rather than for stopping.
    ///
    /// Core cannot borrow the platform's parser: java.net.InetAddress is not
    /// part of the API a Codename One application compiles against.
    private static boolean isAddressLiteral(String host) {
        if (host.length() == 0) {
            return false;
        }
        if (host.indexOf(':') >= 0) {
            return isIpv6Literal(host);
        }
        return isIpv4Literal(host);
    }

    /// Whether this is a dotted IPv4 quad: four decimal octets in range.
    private static boolean isIpv4Literal(String host) {
        int octets = 0;
        int at = 0;
        while (at <= host.length()) {
            int dot = host.indexOf('.', at);
            String part = dot < 0 ? host.substring(at)
                    : host.substring(at, dot);
            if (part.length() == 0 || part.length() > 3) {
                return false;
            }
            int value = 0;
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
                value = value * 10 + (c - '0');
            }
            if (value > 255) {
                return false;
            }
            octets++;
            if (dot < 0) {
                break;
            }
            at = dot + 1;
        }
        return octets == 4;
    }

    /// Whether this is an IPv6 literal, by the grammar rather than by its
    /// alphabet.
    ///
    /// One `::` at most, standing for one or more zero groups; every other
    /// group one to four hex digits; the last group may instead be a dotted
    /// IPv4 quad, which counts as two. Eight groups exactly without a `::`,
    /// and fewer than eight with one -- because `::` has to stand for at
    /// least one group of its own.
    ///
    /// A zone index (`fe80::1%en0`) is refused: it names an interface on the
    /// device that wrote it, which is not something a tunnel configuration
    /// can carry to another one.
    private static boolean isIpv6Literal(String host) {
        int compress = host.indexOf("::");
        if (compress >= 0 && host.indexOf("::", compress + 1) >= 0) {
            return false;
        }
        String head = compress < 0 ? host : host.substring(0, compress);
        String tail = compress < 0 ? "" : host.substring(compress + 2);
        int[] counted = new int[]{0};
        // The head may end in a v4 quad only when there is NO "::" at all.
        // Allowing it whenever the tail was empty accepted "1.2.3.4::",
        // where the quad is not the last group of the address -- something
        // ending in "::" continues with the zeros the "::" stands for.
        if (!countGroups(head, counted, compress < 0)) {
            return false;
        }
        if (!countGroups(tail, counted, true)) {
            return false;
        }
        return compress < 0 ? counted[0] == 8 : counted[0] < 8;
    }

    /// Adds the groups of one colon-separated run to `counted`.
    ///
    /// @param run       the run, which may be empty
    /// @param counted   the running total, added to in place
    /// @param mayEndV4  whether the last group of this run may be a dotted
    ///                  IPv4 quad -- only the LAST group of the whole
    ///                  address may be
    /// @return false when the run is not a valid sequence of groups
    private static boolean countGroups(String run, int[] counted,
            boolean mayEndV4) {
        if (run.length() == 0) {
            return true;
        }
        int at = 0;
        while (at <= run.length()) {
            int colon = run.indexOf(':', at);
            String group = colon < 0 ? run.substring(at)
                    : run.substring(at, colon);
            boolean last = colon < 0;
            if (group.length() == 0) {
                // An empty group is only ever the work of "::", which the
                // caller has already taken out.
                return false;
            }
            if (last && mayEndV4 && group.indexOf('.') >= 0) {
                if (!isIpv4Literal(group)) {
                    return false;
                }
                counted[0] += 2;
            } else {
                if (group.length() > 4) {
                    return false;
                }
                for (int i = 0; i < group.length(); i++) {
                    char c = group.charAt(i);
                    if ((c < '0' || c > '9') && (c < 'a' || c > 'f')
                            && (c < 'A' || c > 'F')) {
                        return false;
                    }
                }
                counted[0]++;
            }
            if (last) {
                return true;
            }
            at = colon + 1;
        }
        return true;
    }

    /// The prefix half of a `host/prefix` block, REFUSING text that is not
    /// one.
    ///
    /// An absent prefix is filled -- `/32` for IPv4, `/128` for IPv6 --
    /// because a bare address is the ordinary way to write a host block and
    /// `Builder.addAddress` demands a number. A prefix that is PRESENT and
    /// unreadable is refused, because the alternative was the most dangerous
    /// answer available: a port that fell back to the family width turned
    /// `route("0.0.0.0/o")` -- a typo for the default route -- into a host
    /// route, and the tunnel came up acknowledged while every packet the app
    /// believed it was protecting went out in the clear. A misread
    /// full-tunnel route has to fail, not shrink.
    ///
    /// Here rather than in a port so the SIMULATION refuses it too. A trap
    /// that only springs on a device is one an app meets after it ships.
    ///
    /// ZERO IS VALID and is the important one: `/0` is the default route,
    /// which is what a full-tunnel VPN asks for.
    ///
    /// @param cidr the block, with or without a prefix
    /// @param what the setup field it came from, so the message names the
    /// line to look at
    /// @return the prefix width
    /// @throws IllegalArgumentException when a present prefix is not a
    /// number the family allows
    public static int prefix(String cidr, String what) {
        String host = host(cidr);
        int width = host.indexOf(':') >= 0 ? 128 : 32;
        int slash = cidr.indexOf('/');
        if (slash < 0) {
            return width;
        }
        String text = cidr.substring(slash + 1).trim();
        boolean digits = text.length() > 0 && text.length() <= 3;
        int value = 0;
        for (int i = 0; digits && i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                digits = false;
            } else {
                value = value * 10 + (c - '0');
            }
        }
        if (!digits || value > width) {
            throw new IllegalArgumentException("The " + what + " prefix in '"
                    + cidr + "' is not a number from 0 to " + width);
        }
        return value;
    }

    public static String[] split(String record) {
        return CallWire.split(record);
    }

    private static String list(String[] values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(LIST);
            }
            sb.append(values[i]);
        }
        return sb.toString();
    }

    private static String[] items(String value) {
        if (value == null || value.length() == 0) {
            return new String[0];
        }
        int count = 1;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == LIST) {
                count++;
            }
        }
        String[] out = new String[count];
        int at = 0;
        int start = 0;
        for (int i = 0; i <= value.length(); i++) {
            if (i == value.length() || value.charAt(i) == LIST) {
                out[at++] = value.substring(start, i);
                start = i + 1;
            }
        }
        return out;
    }
}
