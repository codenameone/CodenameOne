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

/// How a transport allocates and fills the buffers it pools.
///
/// PacketBuffer keeps its constructor and its mutators package-private so an
/// application cannot manufacture or rewrite one; the transports live
/// outside this package and still have to. This is the one door, and it is
/// hidden rather than public API.
///
/// @hidden not part of the public API.
public final class TunnelBuffers {

    private TunnelBuffers() {
    }

    /// A buffer sized for one packet on a link of this MTU.
    ///
    /// @hidden not part of the public API.
    public static PacketBuffer allocate(int mtu) {
        return new PacketBuffer(mtu < 1 ? 1500 : mtu);
    }

    /// Copies a packet the platform read into a pooled buffer.
    ///
    /// @hidden not part of the public API.
    public static void fill(PacketBuffer buffer, byte[] source, int offset,
            int length) {
        buffer.fill(source, offset, length);
    }
}
