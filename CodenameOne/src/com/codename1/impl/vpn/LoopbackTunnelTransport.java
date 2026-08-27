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

import com.codename1.vpn.tunnel.PacketBuffer;
import com.codename1.vpn.tunnel.TunnelTransport;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/// A transport that carries packets nowhere, so a packet loop can be run
/// off-device.
///
/// The simulation's job here is the same as everywhere else in this feature:
/// reproduce what the platforms actually do, including the parts that are
/// awkward. So the buffers are POOLED and handed back round exactly as the
/// real transports do -- a tunnel that keeps one past `onPacket` sees the
/// next packet's bytes here too, rather than only on a device.
///
/// @hidden not part of the public API; test and simulator support.
public final class LoopbackTunnelTransport implements TunnelTransport {

    private final LinkedList<byte[]> inbound = new LinkedList<byte[]>();
    private final List<byte[]> forwarded = new ArrayList<byte[]>();
    private final PacketBuffer[] pool;
    private boolean closed;

    /// @hidden not part of the public API.
    public LoopbackTunnelTransport(int batch, int mtu) {
        this.pool = new PacketBuffer[batch < 1 ? 1 : batch];
        for (int i = 0; i < pool.length; i++) {
            pool[i] = newBuffer(mtu < 1 ? 1500 : mtu);
        }
    }

    /// Queues a packet as though the device had sent it.
    ///
    /// @hidden not part of the public API.
    public void inject(byte[] packet) {
        synchronized (this) {
            inbound.add(packet);
        }
    }

    /// What the tunnel forwarded back, in order.
    ///
    /// @hidden not part of the public API.
    public byte[][] forwarded() {
        synchronized (this) {
            return forwarded.toArray(new byte[forwarded.size()][]);
        }
    }

    @Override
    public boolean isBlocking() {
        // Modelled on ANDROID, which is the platform whose loop owns a
        // thread. A callback-driven simulation would exercise the iOS shape
        // instead; the host reads this rather than assuming either.
        return true;
    }

    @Override
    public int read(PacketBuffer[] into) {
        int n = 0;
        synchronized (this) {
            while (n < into.length && !inbound.isEmpty()) {
                byte[] next = inbound.removeFirst();
                fill(into[n], next);
                n++;
            }
            if (n == 0) {
                // Zero means the link is going down, which is what ends the
                // host's loop; an empty queue is that, here.
                closed = true;
            }
        }
        return n;
    }

    @Override
    public void write(PacketBuffer packet) {
        synchronized (this) {
            forwarded.add(packet.toByteArray());
        }
    }

    @Override
    public PacketBuffer[] buffers() {
        return pool;
    }

    @Override
    public void close() {
        synchronized (this) {
            closed = true;
            inbound.clear();
        }
    }

    /// Whether the transport has been closed or run dry.
    ///
    /// @hidden not part of the public API.
    public boolean isClosed() {
        synchronized (this) {
            return closed;
        }
    }

    // PacketBuffer's constructor and set() are package-private to its own
    // package, so the simulation reaches them the same way the ports do:
    // through the factory the tunnel package exposes for exactly this.
    private static PacketBuffer newBuffer(int mtu) {
        return com.codename1.vpn.tunnel.TunnelBuffers.allocate(mtu);
    }

    private static void fill(PacketBuffer buffer, byte[] packet) {
        com.codename1.vpn.tunnel.TunnelBuffers.fill(buffer, packet, 0,
                packet.length);
    }
}
