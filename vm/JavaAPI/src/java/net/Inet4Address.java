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

/// An IPv4 address, four bytes.
public final class Inet4Address extends InetAddress {

    Inet4Address(String hostName, byte[] address) {
        super(hostName, address);
    }

    @Override
    public boolean isLoopbackAddress() {
        return (addr[0] & 0xff) == 127;
    }

    @Override
    public boolean isAnyLocalAddress() {
        return addr[0] == 0 && addr[1] == 0 && addr[2] == 0 && addr[3] == 0;
    }

    @Override
    public boolean isLinkLocalAddress() {
        return (addr[0] & 0xff) == 169 && (addr[1] & 0xff) == 254;
    }

    @Override
    public boolean isSiteLocalAddress() {
        int a = addr[0] & 0xff;
        int b = addr[1] & 0xff;
        return a == 10 || (a == 172 && (b & 0xf0) == 16) || (a == 192 && b == 168);
    }

    @Override
    public boolean isMulticastAddress() {
        return (addr[0] & 0xf0) == 0xe0;
    }

    @Override
    public String getHostAddress() {
        return (addr[0] & 0xff) + "." + (addr[1] & 0xff) + "." + (addr[2] & 0xff) + "." + (addr[3] & 0xff);
    }

    @Override
    public int hashCode() {
        return ((addr[0] & 0xff) << 24) | ((addr[1] & 0xff) << 16) | ((addr[2] & 0xff) << 8) | (addr[3] & 0xff);
    }
}
