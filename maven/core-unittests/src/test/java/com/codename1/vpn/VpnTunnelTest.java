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
package com.codename1.vpn;

import com.codename1.impl.vpn.LoopbackTunnelTransport;
import com.codename1.vpn.tunnel.PacketBuffer;
import com.codename1.vpn.tunnel.TunnelConfiguration;
import com.codename1.vpn.tunnel.TunnelHost;
import com.codename1.vpn.tunnel.TunnelStopReason;
import com.codename1.vpn.tunnel.VpnTunnel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The packet loop, off-device. The transport is a loopback, but the SHAPE is
 * the real one: pooled buffers handed back round, a blocking read loop that
 * ends on a zero-length read, and lifecycle calls the application cannot
 * invoke itself.
 */
public class VpnTunnelTest {

    private static final class Recording extends VpnTunnel {
        private final List<String> events = new ArrayList<String>();
        private final List<byte[]> seen = new ArrayList<byte[]>();
        private TunnelConfiguration configuration;
        private boolean forward = true;
        private boolean throwOnPacket;

        @Override
        protected void onStart(TunnelConfiguration cfg) {
            this.configuration = cfg;
            events.add("start");
        }

        @Override
        protected void onPacket(PacketBuffer p) {
            seen.add(p.toByteArray());
            if (throwOnPacket) {
                throw new IllegalStateException("a badly behaved tunnel");
            }
            if (forward) {
                forward(p);
            }
        }

        @Override
        protected void onStop(TunnelStopReason r) {
            events.add("stop:" + r);
        }
    }

    private static byte[] ipv4(int marker) {
        byte[] p = new byte[20];
        p[0] = 0x45;
        p[19] = (byte) marker;
        return p;
    }

    @Test
    public void aPacketLoopForwardsWhatItIsGiven() {
        Recording t = new Recording();
        LoopbackTunnelTransport transport = new LoopbackTunnelTransport(4, 1500);
        transport.inject(ipv4(1));
        transport.inject(ipv4(2));

        new TunnelHost(t, transport).start("vpn.example.com",
                new String[]{"0.0.0.0/0"}, new String[]{"1.1.1.1"}, 1400,
                "opaque");

        assertEquals(2, t.seen.size(), "both packets reached the tunnel");
        assertArrayEquals(ipv4(1), t.seen.get(0));
        byte[][] out = transport.forwarded();
        assertEquals(2, out.length, "and both were forwarded");
        assertArrayEquals(ipv4(2), out[1]);
        assertEquals("start", t.events.get(0));
        assertEquals("vpn.example.com", t.configuration.getServer());
        assertEquals(1400, t.configuration.getMtu());
        assertEquals("opaque", t.configuration.getData(),
                "the app's own data reaches a tunnel in another process");
    }

    @Test
    public void theBuffersAreReusedAcrossPackets() {
        // The pooling is the contract, not an implementation detail: an
        // extension that allocated per packet is the one iOS kills. A tunnel
        // that keeps a buffer therefore has to see this off-device too.
        final List<PacketBuffer> identities = new ArrayList<PacketBuffer>();
        VpnTunnel t = new VpnTunnel() {
            protected void onStart(TunnelConfiguration cfg) { }
            protected void onPacket(PacketBuffer p) {
                identities.add(p);
            }
            protected void onStop(TunnelStopReason r) { }
        };
        LoopbackTunnelTransport transport = new LoopbackTunnelTransport(1, 1500);
        transport.inject(ipv4(1));
        transport.inject(ipv4(2));
        new TunnelHost(t, transport).start("s", null, null, 1500, null);

        assertEquals(2, identities.size());
        assertSame(identities.get(0), identities.get(1),
                "one pooled buffer, handed back round");
    }

    @Test
    public void aTunnelThatThrowsDoesNotStopTheLoop() {
        // One bad packet must not take the VPN down: the user would see a
        // tunnel that stopped for no reason it could act on.
        Recording t = new Recording();
        t.throwOnPacket = true;
        LoopbackTunnelTransport transport = new LoopbackTunnelTransport(4, 1500);
        transport.inject(ipv4(1));
        transport.inject(ipv4(2));

        new TunnelHost(t, transport).start("s", null, null, 1500, null);
        assertEquals(2, t.seen.size(),
                "the second packet still arrived after the first threw");
    }

    @Test
    public void stoppingIsAnnouncedOnceAndClosesTheTransport() {
        Recording t = new Recording();
        LoopbackTunnelTransport transport = new LoopbackTunnelTransport(4, 1500);
        TunnelHost host = new TunnelHost(t, transport);
        host.start("s", null, null, 1500, null);

        host.stop(TunnelStopReason.USER_DISABLED.ordinal());
        host.stop(TunnelStopReason.NETWORK_LOST.ordinal());

        int stops = 0;
        for (String e : t.events) {
            if (e.startsWith("stop:")) {
                stops++;
            }
        }
        assertEquals(1, stops, "a second stop is not announced again");
        assertTrue(t.events.contains("stop:USER_DISABLED"),
                "and it carries the reason the first stop gave");
        assertTrue(transport.isClosed(), "the platform's link is released");
    }
}
