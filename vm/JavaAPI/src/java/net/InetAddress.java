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
package java.net;

/// An IP address, [Inet4Address] or [Inet6Address].
///
/// Only address LITERALS are resolved here: `getByName("10.0.0.1")`,
/// `getByName("::1")`, `getByName("[fe80::1]")`, plus `localhost` and the
/// empty or null name, which mean the loopback address as in the JDK. This
/// class library has no name resolver -- the backend's `getaddrinfo` lives
/// in its own TCP natives (`com.codename1.backend.Tcp`), which `vm/JavaAPI`
/// cannot depend on -- so any other host name throws
/// `UnknownHostException`, and [#getHostName] never does a reverse lookup.
public class InetAddress implements java.io.Serializable {

    final byte[] addr;
    private final String hostName;

    InetAddress(String hostName, byte[] addr) {
        this.hostName = hostName;
        this.addr = addr;
    }

    public static InetAddress getByName(String host) throws UnknownHostException {
        return getAllByName(host)[0];
    }

    public static InetAddress[] getAllByName(String host) throws UnknownHostException {
        if (host == null || host.length() == 0 || "localhost".equalsIgnoreCase(host)) {
            return new InetAddress[] {getLoopbackAddress()};
        }
        String literal = host;
        if (literal.charAt(0) == '[') {
            if (literal.length() < 3 || literal.charAt(literal.length() - 1) != ']') {
                throw new UnknownHostException(host + ": invalid IPv6 address literal");
            }
            literal = literal.substring(1, literal.length() - 1);
            byte[] v6 = parseIpv6(literal);
            if (v6 == null) {
                throw new UnknownHostException(host + ": invalid IPv6 address literal");
            }
            return new InetAddress[] {make(null, v6)};
        }
        byte[] a = parseIpv4(literal);
        if (a == null && literal.indexOf(':') >= 0) {
            a = parseIpv6(literal);
            if (a == null) {
                throw new UnknownHostException(host + ": invalid IPv6 address literal");
            }
        }
        if (a == null) {
            // No resolver in this class library; see the class comment.
            throw new UnknownHostException(host);
        }
        return new InetAddress[] {make(null, a)};
    }

    public static InetAddress getByAddress(byte[] addr) throws UnknownHostException {
        return getByAddress(null, addr);
    }

    public static InetAddress getByAddress(String host, byte[] addr) throws UnknownHostException {
        if (addr == null || (addr.length != 4 && addr.length != 16)) {
            throw new UnknownHostException("addr is of illegal length");
        }
        byte[] copy = new byte[addr.length];
        System.arraycopy(addr, 0, copy, 0, addr.length);
        return make(host, copy);
    }

    public static InetAddress getLoopbackAddress() {
        return new Inet4Address("localhost", new byte[] {127, 0, 0, 1});
    }

    /// An IPv4-mapped IPv6 address becomes the IPv4 address, as in the JDK.
    private static InetAddress make(String host, byte[] a) {
        if (a.length == 16) {
            boolean mapped = (a[10] & 0xff) == 0xff && (a[11] & 0xff) == 0xff;
            for (int i = 0; i < 10 && mapped; i++) {
                mapped = a[i] == 0;
            }
            if (mapped) {
                return new Inet4Address(host, new byte[] {a[12], a[13], a[14], a[15]});
            }
            return new Inet6Address(host, a);
        }
        return new Inet4Address(host, a);
    }

    /// Four dot-separated decimal parts of 0..255, or null.
    private static byte[] parseIpv4(String s) {
        byte[] out = new byte[4];
        int part = 0;
        int value = -1;
        for (int i = 0; i <= s.length(); i++) {
            char c = i < s.length() ? s.charAt(i) : '.';
            if (c == '.') {
                if (value < 0 || part > 3) {
                    return null;
                }
                out[part++] = (byte) value;
                value = -1;
            } else if (c >= '0' && c <= '9') {
                value = (value < 0 ? 0 : value * 10) + (c - '0');
                if (value > 255) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return part == 4 ? out : null;
    }

    /// RFC 4291 text form, with `::` and a trailing dotted IPv4 part, or null.
    private static byte[] parseIpv6(String s) {
        if (s.indexOf('%') >= 0) {
            return null;
        }
        int dbl = s.indexOf("::");
        if (dbl >= 0 && s.indexOf("::", dbl + 1) >= 0) {
            return null;
        }
        int[] head = new int[8];
        int[] tail = new int[8];
        int nh;
        int nt = 0;
        if (dbl >= 0) {
            nh = s.substring(0, dbl).indexOf('.') >= 0 ? -1 : groups(s.substring(0, dbl), head);
            nt = groups(s.substring(dbl + 2), tail);
            if (nh < 0 || nt < 0 || nh + nt > 7) {
                return null;
            }
        } else {
            nh = groups(s, head);
            if (nh != 8) {
                return null;
            }
        }
        byte[] out = new byte[16];
        for (int i = 0; i < nh; i++) {
            out[2 * i] = (byte) (head[i] >> 8);
            out[2 * i + 1] = (byte) head[i];
        }
        for (int i = 0; i < nt; i++) {
            int g = 8 - nt + i;
            out[2 * g] = (byte) (tail[i] >> 8);
            out[2 * g + 1] = (byte) tail[i];
        }
        return out;
    }

    /// Parses colon-separated hex groups (the last may be dotted IPv4, worth
    /// two groups) into `out`; the number of groups, or -1.
    private static int groups(String s, int[] out) {
        if (s.length() == 0) {
            return 0;
        }
        int n = 0;
        int start = 0;
        while (true) {
            int colon = s.indexOf(':', start);
            String g = colon < 0 ? s.substring(start) : s.substring(start, colon);
            if (colon < 0 && g.indexOf('.') >= 0) {
                byte[] v4 = parseIpv4(g);
                if (v4 == null || n + 2 > 8) {
                    return -1;
                }
                out[n++] = ((v4[0] & 0xff) << 8) | (v4[1] & 0xff);
                out[n++] = ((v4[2] & 0xff) << 8) | (v4[3] & 0xff);
                return n;
            }
            if (g.length() == 0 || g.length() > 4 || n >= 8) {
                return -1;
            }
            int v = 0;
            for (int i = 0; i < g.length(); i++) {
                // ASCII only: Character.digit also takes other scripts' digits
                char c = g.charAt(i);
                int d = c >= '0' && c <= '9' ? c - '0' : c >= 'a' && c <= 'f' ? c - 'a' + 10
                        : c >= 'A' && c <= 'F' ? c - 'A' + 10 : -1;
                if (d < 0) {
                    return -1;
                }
                v = v * 16 + d;
            }
            out[n++] = v;
            if (colon < 0) {
                return n;
            }
            start = colon + 1;
        }
    }

    /// A copy of the raw address, network byte order.
    public byte[] getAddress() {
        byte[] copy = new byte[addr.length];
        System.arraycopy(addr, 0, copy, 0, addr.length);
        return copy;
    }

    /// The address as text; overridden by both subclasses.
    public String getHostAddress() {
        return "";
    }

    /// The name this address was created with, or its text form; no
    /// reverse lookup is done.
    public String getHostName() {
        return hostName != null ? hostName : getHostAddress();
    }

    public String getCanonicalHostName() {
        return getHostName();
    }

    public boolean isLoopbackAddress() {
        return false;
    }

    public boolean isAnyLocalAddress() {
        return false;
    }

    public boolean isLinkLocalAddress() {
        return false;
    }

    public boolean isSiteLocalAddress() {
        return false;
    }

    public boolean isMulticastAddress() {
        return false;
    }

    /// Equal when the raw addresses are; the host name does not matter.
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof InetAddress)) {
            return false;
        }
        byte[] o = ((InetAddress) obj).addr;
        if (o.length != addr.length) {
            return false;
        }
        for (int i = 0; i < o.length; i++) {
            if (o[i] != addr[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return (hostName != null ? hostName : "") + "/" + getHostAddress();
    }
}
