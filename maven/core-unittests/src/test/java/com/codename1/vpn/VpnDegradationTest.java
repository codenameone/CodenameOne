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

import com.codename1.impl.vpn.VpnRequests;
import com.codename1.vpn.profile.Vpn;
import com.codename1.vpn.profile.VpnProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/** A port with no VPN machinery, which is most of them. */
public class VpnDegradationTest {

    @BeforeEach
    public void noBridge() {
        VpnRequests.resetForTest(null);
    }

    @AfterEach
    public void clear() {
        VpnRequests.resetForTest(null);
    }

    @Test
    public void capabilityQueriesAnswerFalselyRatherThanThrowing() {
        assertFalse(Vpn.isSupported());
        assertEquals(0, Vpn.getCapabilities());
        assertSame(VpnStatus.NOT_CONFIGURED, Vpn.getStatus());
    }

    @Test
    public void everyOperationFailsWithNotSupported() {
        VpnAwait.assertFailedWith(VpnError.NOT_SUPPORTED,
                Vpn.install(new VpnProfile("vpn.example.com")));
        VpnAwait.assertFailedWith(VpnError.NOT_SUPPORTED, Vpn.remove());
        VpnAwait.assertFailedWith(VpnError.NOT_SUPPORTED, Vpn.load());
        VpnAwait.assertFailedWith(VpnError.NOT_SUPPORTED, Vpn.start());
        VpnAwait.assertFailedWith(VpnError.NOT_SUPPORTED, Vpn.stop());
    }

    @Test
    public void aNullProfileIsRefusedAsAConfigurationError() {
        // Distinct from NOT_SUPPORTED so the app can tell its own bug from a
        // platform limitation. Needs a bridge, or NOT_SUPPORTED wins first.
        VpnRequests.resetForTest(new com.codename1.impl.vpn.LocalVpnBridge());
        VpnAwait.assertFailedWith(VpnError.INVALID_CONFIGURATION,
                Vpn.install(null));
    }

    @Test
    public void listenerTeardownIsSafeWithNoBridge() {
        Vpn.addStatusListener(null);
        Vpn.removeStatusListener(null);
    }
}
