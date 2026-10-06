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
package com.codename1.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Whether this server believes what a request says about where it came from.
///
/// Behind a load balancer every connection is the load balancer's, and the
/// client's own address and scheme arrive in `X-Forwarded-For` and
/// `X-Forwarded-Proto`. Those are headers, and anybody can send headers: a
/// server that believes them from everyone lets every client choose the
/// address it is rate limited by and logged under. So they are believed only
/// from a connection whose other end is a proxy this server was told about.
///
/// | Property | Meaning |
/// |---|---|
/// | `cn1.server.forwardHeaders` | `true` to read the headers at all; `false` unless set. |
/// | `cn1.server.trustedProxies` | The proxies, as addresses and CIDR ranges separated by commas. Unless set: loopback and the private ranges -- 10/8, 172.16/12, 192.168/16, 169.254/16, 127/8, ::1, fc00::/7 and fe80::/10. |
///
/// `X-Forwarded-For` is read from the right: each proxy appends the address it
/// heard from, so the rightmost entry is the one this server's own proxy
/// vouches for. Entries are skipped while they are themselves trusted proxies,
/// and the first that is not is the client. What a client put at the left end
/// is never reached.
final class ForwardedHeaders {
    /// Read the headers at all.
    static final String ENABLED = "cn1.server.forwardHeaders";
    /// Whose word is taken for them.
    static final String TRUSTED = "cn1.server.trustedProxies";

    private static final String INTERNAL = "10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,169.254.0.0/16,"
            + "127.0.0.0/8,::1,fc00::/7,fe80::/10";

    /// {address bytes, prefix bits as a one-element array}, per trusted range.
    private final byte[][] networks;
    private final int[] prefixes;

    private ForwardedHeaders(byte[][] networks, int[] prefixes) {
        this.networks = networks;
        this.prefixes = prefixes;
    }

    /// The policy `config` describes, or null when the headers are not read.
    static ForwardedHeaders fromConfig(Config config) throws IOException {
        if (config == null || !config.getBoolean(ENABLED, false)) {
            return null;
        }
        String listed = config.get(TRUSTED);
        return of(listed == null || listed.trim().length() == 0 ? INTERNAL : listed);
    }

    /// A policy trusting these addresses and CIDR ranges.
    static ForwardedHeaders of(String ranges) throws IOException {
        List<byte[]> networks = new ArrayList<byte[]>();
        List<Integer> prefixes = new ArrayList<Integer>();
        int start = 0;
        while (start <= ranges.length()) {
            int comma = ranges.indexOf(',', start);
            int end = comma < 0 ? ranges.length() : comma;
            String range = ranges.substring(start, end).trim();
            if (range.length() > 0) {
                int slash = range.indexOf('/');
                byte[] address = parse(slash < 0 ? range : range.substring(0, slash));
                if (address == null) {
                    throw new IOException(TRUSTED + ": not an address or a CIDR range: " + range);
                }
                int bits = address.length * 8;
                if (slash >= 0) {
                    try {
                        bits = Integer.parseInt(range.substring(slash + 1));
                    } catch (NumberFormatException err) {
                        bits = -1;
                    }
                    if (bits < 0 || bits > address.length * 8) {
                        throw new IOException(TRUSTED + ": not an address or a CIDR range: "
                                + range);
                    }
                }
                networks.add(address);
                prefixes.add(Integer.valueOf(bits));
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        int[] bits = new int[prefixes.size()];
        for (int iter = 0 ; iter < bits.length ; iter++) {
            bits[iter] = prefixes.get(iter).intValue();
        }
        return new ForwardedHeaders(networks.toArray(new byte[networks.size()][]), bits);
    }

    /// Whether `address` is one of the proxies.
    boolean trusts(byte[] address) {
        if (address == null) {
            return false;
        }
        for (int iter = 0 ; iter < networks.length ; iter++) {
            byte[] network = networks[iter];
            if (network.length != address.length) {
                continue;
            }
            int bits = prefixes[iter];
            boolean match = true;
            for (int at = 0 ; at < network.length && bits > 0 && match ; at++) {
                int mask = bits >= 8 ? 0xff : (0xff << (8 - bits)) & 0xff;
                match = (network[at] & mask) == (address[at] & mask);
                bits -= 8;
            }
            if (match) {
                return true;
            }
        }
        return false;
    }

    /// The client a request is from: the peer, unless the peer is a proxy, and
    /// then what `X-Forwarded-For` says, read from the right past every proxy.
    byte[] client(byte[] peer, String forwardedFor) {
        if (forwardedFor == null || !trusts(peer)) {
            return peer;
        }
        byte[] client = peer;
        int end = forwardedFor.length();
        while (end > 0) {
            int comma = forwardedFor.lastIndexOf(',', end - 1);
            byte[] hop = parse(forwardedFor.substring(comma + 1, end).trim());
            if (hop == null) {
                // Not an address: whatever is left of it is nobody's word.
                break;
            }
            client = hop;
            if (!trusts(hop)) {
                break;
            }
            end = comma;
        }
        return client;
    }

    /// Whether a request that came from `peer` with this `X-Forwarded-Proto`
    /// reached the proxy over TLS: the peer is a proxy, and every scheme the
    /// header lists is https.
    boolean secure(byte[] peer, String forwardedProto) {
        if (forwardedProto == null || !trusts(peer)) {
            return false;
        }
        int start = 0;
        boolean any = false;
        while (start <= forwardedProto.length()) {
            int comma = forwardedProto.indexOf(',', start);
            int end = comma < 0 ? forwardedProto.length() : comma;
            if (!"https".equalsIgnoreCase(forwardedProto.substring(start, end).trim())) {
                return false;
            }
            any = true;
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        return any;
    }

    // ------------------------------------------------------------- addresses

    /// An address as text: dotted for IPv4 and for an IPv4 address carried in
    /// IPv6 (`::ffff:192.0.2.1`), eight groups of hexadecimal for IPv6. Written
    /// one way, so an address is one key whichever runtime read it.
    static String format(byte[] address) {
        if (address == null) {
            return null;
        }
        byte[] plain = unmap(address);
        StringBuilder sb = new StringBuilder();
        if (plain.length == 4) {
            for (int iter = 0 ; iter < 4 ; iter++) {
                sb.append(iter == 0 ? "" : ".").append(plain[iter] & 0xff);
            }
            return sb.toString();
        }
        for (int iter = 0 ; iter < 16 ; iter += 2) {
            sb.append(iter == 0 ? "" : ":").append(Integer.toHexString(
                    ((plain[iter] & 0xff) << 8) | (plain[iter + 1] & 0xff)));
        }
        return sb.toString();
    }

    /// The IPv4 address inside an IPv4-mapped IPv6 one; anything else as it is.
    static byte[] unmap(byte[] address) {
        if (address.length != 16) {
            return address;
        }
        for (int iter = 0 ; iter < 10 ; iter++) {
            if (address[iter] != 0) {
                return address;
            }
        }
        if (address[10] != (byte) 0xff || address[11] != (byte) 0xff) {
            return address;
        }
        return new byte[] {address[12], address[13], address[14], address[15]};
    }

    /// The bytes of an address written as text -- IPv4, or IPv6 with or without
    /// `::` and brackets -- or null when it is not one. An IPv4-mapped IPv6
    /// address comes back as the IPv4 address it carries. A port after an IPv4
    /// address or a bracketed IPv6 one is allowed and dropped: some proxies
    /// write one into `X-Forwarded-For`.
    static byte[] parse(String text) {
        if (text == null) {
            return null;
        }
        String value = text;
        if (value.startsWith("[")) {
            int close = value.indexOf(']');
            if (close < 0) {
                return null;
            }
            value = value.substring(1, close);
        } else if (value.indexOf('.') > 0 && value.indexOf(':') == value.lastIndexOf(':')
                && value.indexOf(':') > 0) {
            value = value.substring(0, value.indexOf(':'));
        }
        if (value.length() == 0 || value.length() > 45) {
            return null;
        }
        if (value.indexOf(':') < 0) {
            return parseV4(value);
        }
        byte[] parsed = parseV6(value);
        return parsed == null ? null : unmap(parsed);
    }

    private static byte[] parseV4(String value) {
        byte[] out = new byte[4];
        int part = 0;
        int number = -1;
        int digits = 0;
        for (int iter = 0 ; iter <= value.length() ; iter++) {
            char c = iter == value.length() ? '.' : value.charAt(iter);
            if (c == '.') {
                if (number < 0 || part > 3) {
                    return null;
                }
                out[part++] = (byte) number;
                number = -1;
                digits = 0;
            } else if (c >= '0' && c <= '9') {
                number = (number < 0 ? 0 : number * 10) + (c - '0');
                if (++digits > 3 || number > 255) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return part == 4 ? out : null;
    }

    private static byte[] parseV6(String value) {
        int[] groups = new int[8];
        int count = 0;
        int gap = -1;
        int at = 0;
        int length = value.length();
        if (value.startsWith("::")) {
            gap = 0;
            at = 2;
        } else if (value.startsWith(":")) {
            return null;
        }
        while (at < length) {
            if (count == 8) {
                return null;
            }
            int end = value.indexOf(':', at);
            String group = value.substring(at, end < 0 ? length : end);
            if (group.indexOf('.') >= 0) {
                // The last 32 bits written as IPv4.
                byte[] tail = end < 0 ? parseV4(group) : null;
                if (tail == null || count > 6) {
                    return null;
                }
                groups[count++] = ((tail[0] & 0xff) << 8) | (tail[1] & 0xff);
                groups[count++] = ((tail[2] & 0xff) << 8) | (tail[3] & 0xff);
                break;
            }
            if (group.length() == 0 || group.length() > 4) {
                return null;
            }
            int number = 0;
            for (int iter = 0 ; iter < group.length() ; iter++) {
                int digit = Character.digit(group.charAt(iter), 16);
                if (digit < 0) {
                    return null;
                }
                number = (number << 4) | digit;
            }
            groups[count++] = number;
            if (end < 0) {
                at = length;
            } else if (end + 1 < length && value.charAt(end + 1) == ':') {
                if (gap >= 0) {
                    return null;
                }
                gap = count;
                at = end + 2;
            } else if (end + 1 == length) {
                return null;
            } else {
                at = end + 1;
            }
        }
        if (gap < 0 ? count != 8 : count >= 8) {
            return null;
        }
        byte[] out = new byte[16];
        int missing = 8 - count;
        for (int iter = 0 ; iter < 8 ; iter++) {
            int group;
            if (gap < 0 || iter < gap) {
                group = groups[iter];
            } else if (iter < gap + missing) {
                group = 0;
            } else {
                group = groups[iter - missing];
            }
            out[iter * 2] = (byte) (group >> 8);
            out[iter * 2 + 1] = (byte) group;
        }
        return out;
    }
}
