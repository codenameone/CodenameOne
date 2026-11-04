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

/// An IPv6 address, sixteen bytes. Scope ids are not supported.
public final class Inet6Address extends InetAddress {

    Inet6Address(String hostName, byte[] address) {
        super(hostName, address);
    }

    private boolean allZeroTo(int end) {
        for (int i = 0; i < end; i++) {
            if (addr[i] != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isLoopbackAddress() {
        return allZeroTo(15) && addr[15] == 1;
    }

    @Override
    public boolean isAnyLocalAddress() {
        return allZeroTo(16);
    }

    @Override
    public boolean isLinkLocalAddress() {
        return (addr[0] & 0xff) == 0xfe && (addr[1] & 0xc0) == 0x80;
    }

    @Override
    public boolean isSiteLocalAddress() {
        return (addr[0] & 0xff) == 0xfe && (addr[1] & 0xc0) == 0xc0;
    }

    @Override
    public boolean isMulticastAddress() {
        return (addr[0] & 0xff) == 0xff;
    }

    /// The JDK's form: eight groups of lowercase hex, not compressed.
    @Override
    public String getHostAddress() {
        StringBuilder sb = new StringBuilder(39);
        for (int i = 0; i < 16; i += 2) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(Integer.toHexString(((addr[i] & 0xff) << 8) | (addr[i + 1] & 0xff)));
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < 16; i += 4) {
            h += ((addr[i] & 0xff) << 24) | ((addr[i + 1] & 0xff) << 16) | ((addr[i + 2] & 0xff) << 8)
                    | (addr[i + 3] & 0xff);
        }
        return h;
    }
}
