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

/// One IP packet on its way through the tunnel.
///
/// #### These are REUSED
///
/// The transport hands the same buffers back round rather than allocating
/// per packet, because a packet loop allocating at line rate is the fastest
/// way to exhaust the memory an iOS Network Extension is given. A packet you
/// want after [VpnTunnel#onPacket] returns has to be copied, with
/// [#toByteArray]; the buffer itself belongs to the transport again the
/// moment that call ends.
public final class PacketBuffer {
    private byte[] data;
    private int offset;
    private int length;
    private int family;

    /// IPv4, as reported by [#getFamily].
    public static final int FAMILY_IPV4 = 4;

    /// IPv6, as reported by [#getFamily].
    public static final int FAMILY_IPV6 = 6;

    PacketBuffer(int capacity) {
        this.data = new byte[capacity];
    }

    /// The bytes this packet lives in, which extend beyond it.
    ///
    /// Read between [#getOffset] and `getOffset() + getLength()`; the rest is
    /// whatever the previous packet left.
    public byte[] getData() {
        return data;
    }

    /// Where this packet starts in [#getData].
    public int getOffset() {
        return offset;
    }

    /// How many bytes this packet occupies.
    public int getLength() {
        return length;
    }

    /// `FAMILY_IPV4` or `FAMILY_IPV6`.
    ///
    /// iOS reports the family alongside the packet and Android does not, so
    /// on Android it is read from the header's version nibble. Either way it
    /// is the same answer, which is why it is a field here rather than
    /// something each tunnel works out again.
    public int getFamily() {
        return family;
    }

    /// A copy of just this packet, for keeping past `onPacket`.
    public byte[] toByteArray() {
        byte[] out = new byte[length];
        System.arraycopy(data, offset, out, 0, length);
        return out;
    }

    /// Fills this buffer from an array the platform read into.
    ///
    /// Always a copy. A transport that reads STRAIGHT into this buffer's
    /// backing array would want to keep it and move the offset instead, and
    /// there was a mutator here that took the array and compared it by
    /// identity to decide which of the two had happened. Nothing called it:
    /// no transport reads in place yet, and the caller is in a better
    /// position to say which it means than a reference comparison is. When
    /// one exists it gets its own door in [TunnelBuffers], named for the
    /// thing it does.
    void fill(byte[] source, int off, int len) {
        if (data.length < len) {
            data = new byte[len];
        }
        System.arraycopy(source, off, data, 0, len);
        this.offset = 0;
        this.length = len;
        this.family = len > 0 && ((source[off] & 0xf0) >> 4) == 6
                ? FAMILY_IPV6 : FAMILY_IPV4;
    }
}
