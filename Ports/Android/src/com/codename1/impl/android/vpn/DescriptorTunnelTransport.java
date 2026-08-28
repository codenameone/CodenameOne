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
package com.codename1.impl.android.vpn;

import android.os.ParcelFileDescriptor;

import com.codename1.vpn.tunnel.PacketBuffer;
import com.codename1.vpn.tunnel.TunnelBuffers;
import com.codename1.vpn.tunnel.TunnelTransport;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/// The Android half of a packet tunnel: a blocking read on the TUN
/// descriptor `VpnService.Builder.establish()` handed over.
///
/// #### One packet per read, and why there is no batching
///
/// A TUN descriptor is not a stream. Each `read` returns exactly one IP
/// packet, however large the buffer is, and a short read is not a partial
/// packet -- so the batch this fills is always one deep. `read` still takes
/// the array the interface gives it, because the interface is shared with
/// iOS where a single callback really does deliver many.
///
/// #### The buffer is reused, deliberately
///
/// One buffer, refilled. The tunnel contract says a `PacketBuffer` is only
/// valid inside `onPacket`, and honouring that here is what lets a
/// pass-through tunnel run without allocating: `forward(packet)` writes the
/// same array back out.
final class DescriptorTunnelTransport implements TunnelTransport {

    private final ParcelFileDescriptor descriptor;
    private final FileInputStream in;
    private final FileOutputStream out;
    private final PacketBuffer[] pool;
    private final byte[] scratch;
    private volatile boolean closed;

    DescriptorTunnelTransport(ParcelFileDescriptor descriptor, int mtu) {
        this.descriptor = descriptor;
        this.in = new FileInputStream(descriptor.getFileDescriptor());
        this.out = new FileOutputStream(descriptor.getFileDescriptor());
        int size = mtu > 0 ? mtu : 1400;
        this.scratch = new byte[size];
        this.pool = new PacketBuffer[]{TunnelBuffers.allocate(size)};
    }

    @Override
    public boolean isBlocking() {
        // The read below parks the calling thread until a packet arrives,
        // which is what the host's loop is built around.
        return true;
    }

    @Override
    public int read(PacketBuffer[] into) {
        if (closed || into == null || into.length == 0) {
            return 0;
        }
        int n;
        try {
            n = in.read(scratch);
        } catch (IOException closedOrGone) {
            // Closing the descriptor from stop() is how this loop is ended,
            // and it surfaces here as an exception rather than as a return.
            // Zero is the contract's "the link is going down".
            return 0;
        }
        if (n <= 0) {
            return 0;
        }
        TunnelBuffers.fill(into[0], scratch, 0, n);
        return 1;
    }

    @Override
    public void write(PacketBuffer packet) {
        if (closed || packet == null || packet.getLength() <= 0) {
            return;
        }
        try {
            // Written straight from the buffer's own array. A tunnel that
            // forwards what it was given copies nothing at all, which is the
            // pooling's whole purpose.
            out.write(packet.getData(), packet.getOffset(), packet.getLength());
        } catch (IOException gone) {
            // A write to a torn-down interface is not an error the app can
            // do anything about, and throwing here would take down the
            // packet loop for a link that is already going.
            closed = true;
        }
    }

    @Override
    public PacketBuffer[] buffers() {
        return pool;
    }

    @Override
    public void close() {
        closed = true;
        // The DESCRIPTOR first: it is what the read is parked on, and
        // closing the streams alone left the loop blocked until the next
        // packet arrived on a link nobody was serving any more.
        try {
            descriptor.close();
        } catch (IOException alreadyGone) {
            // Nothing useful to do; the link is going either way.
        }
        try {
            in.close();
        } catch (IOException alreadyGone) {
            // As above.
        }
        try {
            out.close();
        } catch (IOException alreadyGone) {
            // As above.
        }
    }
}
