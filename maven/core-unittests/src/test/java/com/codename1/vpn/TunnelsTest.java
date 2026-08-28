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

import com.codename1.impl.vpn.LocalVpnBridge;
import com.codename1.impl.vpn.TunnelWire;
import com.codename1.impl.vpn.VpnRequests;
import com.codename1.vpn.tunnel.PacketBuffer;
import com.codename1.vpn.tunnel.TunnelConfiguration;
import com.codename1.vpn.tunnel.TunnelSetup;
import com.codename1.vpn.tunnel.Tunnels;
import com.codename1.vpn.tunnel.VpnTunnel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starting and stopping a tunnel through the facade an application uses.
 *
 * <p>{@link VpnTunnelTest} drives {@code TunnelHost} directly, which proves
 * the packet loop. This drives {@code Tunnels}, which is the path an app
 * takes -- registration, the wire the setup crosses on, and the bridge
 * bringing it up. Before the ports existed there was nothing on the other end
 * of that path to test.</p>
 */
public class TunnelsTest {

    private LocalVpnBridge bridge;

    private static final class Echo extends VpnTunnel {
        private final List<String> events = new ArrayList<String>();
        private TunnelConfiguration configuration;

        @Override
        protected void onStart(TunnelConfiguration cfg) {
            configuration = cfg;
            events.add("start");
        }

        @Override
        protected void onPacket(PacketBuffer p) {
            events.add("packet:" + p.getLength());
            forward(p);
        }

        @Override
        protected void onStop(com.codename1.vpn.tunnel.TunnelStopReason r) {
            events.add("stop:" + r);
        }
    }

    @BeforeEach
    public void setUp() {
        bridge = new LocalVpnBridge();
        VpnRequests.resetForTest(bridge);
    }

    @AfterEach
    public void tearDown() {
        VpnRequests.resetForTest(null);
    }

    @Test
    public void aTunnelStartsAndReceivesTheSetupItWasGiven() {
        Echo tunnel = new Echo();
        VpnAwait.value(Tunnels.start(tunnel, new TunnelSetup()
                .address("10.0.0.2/32")
                .server("vpn.example.com")
                .route("0.0.0.0/0")
                .dnsServer("10.0.0.1")
                .mtu(1280)
                .data("session-token")));
        assertEquals("start", tunnel.events.get(0));
        // The CONFIGURATION, not the setup object: on iOS these are different
        // objects in different processes, and an app that read the setup back
        // would be relying on something only Android does.
        assertEquals("vpn.example.com", tunnel.configuration.getServer());
        assertArrayEquals(new String[]{"0.0.0.0/0"},
                tunnel.configuration.getRoutes());
        assertArrayEquals(new String[]{"10.0.0.1"},
                tunnel.configuration.getDnsServers());
        assertEquals(1280, tunnel.configuration.getMtu());
        assertEquals("session-token", tunnel.configuration.getData());
    }

    @Test
    public void aPacketReachesTheTunnelAndWhatItForwardsComesBack() {
        Echo tunnel = new Echo();
        VpnAwait.value(Tunnels.start(tunnel, new TunnelSetup()
                .address("10.0.0.2/32").route("0.0.0.0/0")));
        byte[] packet = new byte[]{0x45, 0x00, 0x00, 0x1c, 0x7f};
        bridge.simulateInboundPacket(packet);
        assertEquals("packet:5", tunnel.events.get(1));
        byte[][] out = bridge.forwardedPackets();
        assertEquals(1, out.length, "the echo forwarded exactly one packet");
        assertArrayEquals(packet, out[0]);
    }

    @Test
    public void stoppingTellsTheTunnelWhy() {
        Echo tunnel = new Echo();
        VpnAwait.value(Tunnels.start(tunnel, new TunnelSetup()
                .address("10.0.0.2/32")));
        VpnAwait.value(Tunnels.stop());
        assertTrue(tunnel.events.contains("stop:REQUESTED"),
                "the reason is what tells an app whether to reconnect: "
                        + tunnel.events);
    }

    @Test
    public void theSetupSurvivesTheWireItCrossesOn() {
        // The record is what reaches the platform -- and on iOS what reaches
        // ANOTHER PROCESS -- so every field has to come back out of it.
        String wire = TunnelWire.encodeSetup(new TunnelSetup()
                .address("10.0.0.2/32")
                .server("vpn.example.com")
                .route("10.0.0.0/8")
                .route("192.168.0.0/16")
                .dnsServer("10.0.0.1")
                .dnsServer("10.0.0.2")
                .searchDomain("corp.example.com")
                .mtu(1300)
                .sessionName("Work")
                .data("token\twith\ta\ttab"));
        String[] f = TunnelWire.split(wire);
        assertEquals("10.0.0.2/32", TunnelWire.address(f));
        assertEquals("vpn.example.com", TunnelWire.server(f));
        assertArrayEquals(new String[]{"10.0.0.0/8", "192.168.0.0/16"},
                TunnelWire.routes(f));
        assertArrayEquals(new String[]{"10.0.0.1", "10.0.0.2"},
                TunnelWire.dnsServers(f));
        assertArrayEquals(new String[]{"corp.example.com"},
                TunnelWire.searchDomains(f));
        assertEquals(1300, TunnelWire.mtu(f));
        assertEquals("Work", TunnelWire.sessionName(f));
        // The tabs survive. This field carries whatever the app put in it --
        // a token, a serialized blob -- and a wire that mangled it would
        // corrupt the one channel iOS gives the tunnel.
        assertEquals("token\twith\ta\ttab", TunnelWire.data(f));
    }

    @Test
    public void anUnusableMtuFallsBackRatherThanFailingTheTunnel() {
        String[] f = TunnelWire.split(TunnelWire.encodeSetup(
                new TunnelSetup().address("10.0.0.2/32")));
        assertEquals(TunnelSetup.DEFAULT_MTU, TunnelWire.mtu(f));
    }

    @Test
    public void twoStartsDoNotSwapEachOthersTunnels() {
        // The registration used to be one field. On Android the service reads
        // it back after startService returns, so a second start landing in
        // that window ran the WRONG tunnel object under the first setup and
        // acknowledged the first request. Each start carries its own now.
        Echo first = new Echo();
        Echo second = new Echo();
        VpnAwait.value(Tunnels.start(first, new TunnelSetup()
                .address("10.0.0.2/32").data("first")));
        VpnAwait.value(Tunnels.start(second, new TunnelSetup()
                .address("10.0.0.3/32").data("second")));
        assertEquals("first", first.configuration.getData(),
                "the first tunnel got the first setup");
        assertEquals("second", second.configuration.getData(),
                "and the second got its own");
    }

    @Test
    public void aRefusedStartDoesNotStrandItsTunnel() {
        // A start that never reaches the platform has to release what it
        // registered, or the application's tunnel is held for the life of
        // the process.
        VpnRequests.resetForTest(null);
        VpnAwait.assertFailedWith(VpnError.NOT_SUPPORTED,
                Tunnels.start(new Echo(), new TunnelSetup()));
        VpnRequests.resetForTest(bridge);
        assertNull(Tunnels.getRegistered(),
                "nothing is running, so nothing is registered");
    }

    @Test
    public void startingWithoutABridgeDegradesRatherThanThrowing() {
        VpnRequests.resetForTest(null);
        assertFalse(Tunnels.isSupported());
        VpnAwait.assertFailedWith(VpnError.NOT_SUPPORTED,
                Tunnels.start(new Echo(), new TunnelSetup()));
    }
}
