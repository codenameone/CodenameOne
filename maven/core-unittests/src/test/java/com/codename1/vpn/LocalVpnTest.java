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
import com.codename1.impl.vpn.VpnRequests;
import com.codename1.vpn.profile.Vpn;
import com.codename1.vpn.profile.VpnProfile;
import com.codename1.vpn.profile.VpnStatusListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VPN configuration management, against the simulation. */
public class LocalVpnTest {

    private LocalVpnBridge bridge;

    @BeforeEach
    public void install() {
        bridge = new LocalVpnBridge();
        VpnRequests.resetForTest(bridge);
    }

    @AfterEach
    public void clear() {
        VpnRequests.resetForTest(null);
    }

    private static VpnProfile profile() {
        return new VpnProfile("vpn.example.com")
                .protocol(VpnProtocol.IKEV2)
                .localIdentifier("alice")
                .usernamePassword("alice", "hunter2");
    }

    @Test
    public void nothingIsConfiguredToBeginWith() {
        assertTrue(Vpn.isSupported());
        assertSame(VpnStatus.NOT_CONFIGURED, Vpn.getStatus());
    }

    @Test
    public void installingLeavesADisconnectedProfile() {
        VpnAwait.value(Vpn.install(profile()));
        assertSame(VpnStatus.DISCONNECTED, Vpn.getStatus());
    }

    @Test
    public void aDeclinedPromptIsAnOrdinaryFailure() {
        // Both platforms prompt and neither lets an app avoid it, so a
        // decline is a normal outcome the app has to handle, not a bug.
        bridge.setUserAccepts(false);
        VpnAwait.assertFailedWith(VpnError.USER_DECLINED, Vpn.install(profile()));
        assertSame(VpnStatus.NOT_CONFIGURED, Vpn.getStatus());
    }

    @Test
    public void connectingPassesThroughConnecting() {
        // An app that only watches for CONNECTED and never shows progress
        // looks frozen for the length of a real negotiation.
        VpnAwait.value(Vpn.install(profile()));
        final List<VpnStatus> seen = new ArrayList<VpnStatus>();
        Vpn.addStatusListener(new Collector(seen));
        VpnAwait.value(Vpn.start());
        waitFor(seen, 2);
        assertTrue(seen.contains(VpnStatus.CONNECTING),
                "CONNECTING must be observable, not skipped");
        assertSame(VpnStatus.CONNECTED, Vpn.getStatus());
    }

    @Test
    public void startingWithNoProfileIsRefused() {
        VpnAwait.assertFailedWith(VpnError.NOT_CONFIGURED, Vpn.start());
    }

    @Test
    public void badCredentialsFailAndLeaveTheTunnelDown() {
        bridge.setAuthenticates(false);
        VpnAwait.value(Vpn.install(profile()));
        VpnAwait.assertFailedWith(VpnError.AUTHENTICATION_FAILED, Vpn.start());
        assertSame(VpnStatus.DISCONNECTED, Vpn.getStatus());
    }

    @Test
    public void aLoadedProfileNeverCarriesThePassword() {
        // Both platforms keep the secret in their own keychain and do not
        // hand it back. A simulation that returned it would let an app depend
        // on something no device does.
        VpnAwait.value(Vpn.install(profile()));
        VpnProfile back = VpnAwait.value(Vpn.load());
        assertNotNull(back);
        assertEquals("vpn.example.com", back.getServerAddress());
        assertEquals("alice", back.getUsername());
        assertNull(back.getPassword(), "the platform does not return secrets");
    }

    @Test
    public void loadingWithNothingInstalledAnswersNull() {
        assertNull(VpnAwait.value(Vpn.load()));
    }

    @Test
    public void removingClearsTheConfiguration() {
        VpnAwait.value(Vpn.install(profile()));
        VpnAwait.value(Vpn.remove());
        assertSame(VpnStatus.NOT_CONFIGURED, Vpn.getStatus());
        assertNull(VpnAwait.value(Vpn.load()));
    }

    @Test
    public void aProfileWithNoServerIsRefusedBeforeInstalling() {
        try {
            new VpnProfile("");
            throw new AssertionError("an empty server address must be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("address"));
        }
    }

    @Test
    public void stoppingBringsTheTunnelDown() {
        VpnAwait.value(Vpn.install(profile()));
        VpnAwait.value(Vpn.start());
        VpnAwait.value(Vpn.stop());
        assertSame(VpnStatus.DISCONNECTED, Vpn.getStatus());
    }

    @Test
    public void removingTheLastListenerStopsDelivery() {
        VpnAwait.value(Vpn.install(profile()));
        List<VpnStatus> seen = new ArrayList<VpnStatus>();
        Collector c = new Collector(seen);
        Vpn.addStatusListener(c);
        Vpn.removeStatusListener(c);
        seen.clear();
        VpnAwait.value(Vpn.start());
        assertEquals(0, seen.size(),
                "a removed listener must stop hearing about the tunnel");
    }

    @Test
    public void noPortClaimsACustomPacketTunnel() {
        // com.codename1.vpn.tunnel is not shipped: on iOS the tunnel body
        // would run in an extension with no Java virtual machine in it. The
        // simulation must not be the one place it appears to work.
        assertEquals(0, Vpn.getCapabilities()
                & com.codename1.vpn.spi.VpnBridge.CAPABILITY_CUSTOM_TUNNEL);
    }

    @Test
    public void noPortClaimsAlwaysOn() {
        // Always-on needs a supervised device and MDM on iOS, and a Settings
        // toggle or device-owner API on Android; an app cannot ask for it.
        assertEquals(0, Vpn.getCapabilities()
                & com.codename1.vpn.spi.VpnBridge.CAPABILITY_ALWAYS_ON);
    }

    /** Collects statuses. A named class so it holds no outer reference. */
    private static final class Collector implements VpnStatusListener {
        private final List<VpnStatus> sink;

        Collector(List<VpnStatus> sink) {
            this.sink = sink;
        }

        public void vpnStatusChanged(VpnStatus status) {
            sink.add(status);
        }
    }

    private static void waitFor(List<?> sink, int count) {
        long limit = System.currentTimeMillis() + 5000;
        while (sink.size() < count && System.currentTimeMillis() < limit) {
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertTrue(sink.size() >= count,
                "expected " + count + " event(s) and saw " + sink.size());
    }
}
