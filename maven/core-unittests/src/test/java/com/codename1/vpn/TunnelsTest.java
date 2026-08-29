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
    public void aLinkThatDiesOnItsOwnStillTellsTheTunnel() {
        // A descriptor that fails without a stop and without the platform
        // revoking the VPN used to end the read loop and nothing else: the
        // tunnel never heard onStop, and the port kept a published host and
        // a foreground notification for a link nobody was serving.
        Echo tunnel = new Echo();
        VpnAwait.value(Tunnels.start(tunnel, new TunnelSetup()
                .address("10.0.0.2/32")));
        bridge.simulateTransportFailure();
        assertTrue(tunnel.events.contains("stop:NETWORK_LOST"),
                "the tunnel is told the link went, and why: "
                        + tunnel.events);
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

    @Test
    public void theSimulationRefusesABlockADeviceWouldRefuse() {
        // The simulation used to accept any route text, so "0.0.0.0/o" -- a
        // typo for the default route -- ran here and failed on a device.
        // That is the one divergence a simulation must not have: the app
        // meets it after it ships. Refused with the same error and the same
        // message the port answers with, because both read the block through
        // TunnelWire.
        VpnAwait.assertFailedWith(VpnError.INVALID_CONFIGURATION,
                Tunnels.start(new Echo(), new TunnelSetup()
                        .address("10.0.0.2/32")
                        .route("0.0.0.0/o")));
        assertNull(Tunnels.getRegistered(),
                "and a refused start leaves nothing registered");
        // The ADDRESS too, which is read by the same call.
        VpnAwait.assertFailedWith(VpnError.INVALID_CONFIGURATION,
                Tunnels.start(new Echo(), new TunnelSetup()
                        .address("10.0.0.2/nope")
                        .route("0.0.0.0/0")));
        // And a setup that is fine still starts, so the guard is not simply
        // refusing everything.
        Echo good = new Echo();
        VpnAwait.value(Tunnels.start(good, new TunnelSetup()
                .address("10.0.0.2/32").route("0.0.0.0/0")));
        assertEquals("start", good.events.get(0));
        VpnAwait.value(Tunnels.stop());
    }

    /// A bridge that records whether it was asked, and refuses to do
    /// anything else. Stands in for the two device ports, which cannot run
    /// here.
    private static final class Recording
            implements com.codename1.vpn.spi.VpnBridge {
        private int starts;

        public boolean isVpnSupported() {
            return true;
        }

        public boolean isCustomTunnelSupported() {
            return true;
        }

        public int getVpnCapabilities() {
            return CAPABILITY_CUSTOM_TUNNEL;
        }

        public int getVpnStatus() {
            return 0;
        }

        public void installProfile(int requestId, String profileWire) {
        }

        public void removeProfile(int requestId) {
        }

        public void loadProfile(int requestId) {
        }

        public void startVpn(int requestId) {
        }

        public void stopVpn(int requestId) {
        }

        public void setStatusListening(boolean listening) {
        }

        public void startCustomTunnel(int requestId, String setupWire) {
            starts++;
        }

        public void stopCustomTunnel(int requestId) {
        }
    }

    @Test
    public void aTunnelWithNoAddressIsRefusedRatherThanSimulated() {
        // route("0.0.0.0/0") and nothing else used to start here: the empty
        // address was read as "none given" and skipped. Android skips
        // Builder.addAddress for it and hands establish() an interface with
        // no addresses, which its addAddress documentation rules out -- at
        // least one address must be set before establish(). So this was a
        // setup the simulator approved and a device could not bring up,
        // which is the one divergence this validation exists to remove.
        Recording device = new Recording();
        VpnRequests.resetForTest(device);
        VpnAwait.assertFailedWith(VpnError.INVALID_CONFIGURATION,
                Tunnels.start(new Echo(), new TunnelSetup()
                        .route("0.0.0.0/0")));
        assertEquals(0, device.starts,
                "and it must not reach the bridge on the way to failing");

        // The SIMULATION too, which is where the divergence showed: it is
        // the same call, so it cannot answer differently.
        VpnRequests.resetForTest(bridge);
        VpnAwait.assertFailedWith(VpnError.INVALID_CONFIGURATION,
                Tunnels.start(new Echo(), new TunnelSetup()
                        .route("0.0.0.0/0")));
        assertNull(Tunnels.getRegistered(),
                "a refused start leaves nothing registered");
    }

    @Test
    public void aMalformedSetupNeverReachesAnyBridge() {
        // The refusal used to live in LocalVpnBridge, so it was a property of
        // the SIMULATION rather than of the API: the same setup handed to the
        // Android bridge threw out of VpnService.Builder inside the service,
        // and on iOS crossed into a separate extension process that read the
        // unreadable prefix as zero and installed a DEFAULT route -- an app
        // asking for one subnet silently capturing all traffic.
        //
        // Asserted against a bridge that is NOT the simulation, because a
        // check that only the simulation performs is exactly the defect. The
        // count is what makes it non-vacuous: refusing after asking the
        // bridge would satisfy the error assertion and none of the point.
        Recording device = new Recording();
        VpnRequests.resetForTest(device);
        VpnAwait.assertFailedWith(VpnError.INVALID_CONFIGURATION,
                Tunnels.start(new Echo(), new TunnelSetup()
                        .address("10.0.0.2/32")
                        .route("10.0.0.0/foo")));
        assertEquals(0, device.starts,
                "the bridge must not be asked to start a setup it cannot"
                + " read");
        assertNull(Tunnels.getRegistered(),
                "and nothing is left registered against it");

        // A DNS server too -- Android hands each to addDnsServer, which
        // throws on a literal it cannot parse.
        VpnAwait.assertFailedWith(VpnError.INVALID_CONFIGURATION,
                Tunnels.start(new Echo(), new TunnelSetup()
                        .address("10.0.0.2/32")
                        .dnsServer("not-an-ip")));
        assertEquals(0, device.starts);

        // And a setup this bridge can read does reach it, so the guard is
        // not simply refusing everything.
        Tunnels.start(new Echo(), new TunnelSetup()
                .address("10.0.0.2/32").route("0.0.0.0/0"));
        assertEquals(1, device.starts,
                "a readable setup has to get through");
    }
}
