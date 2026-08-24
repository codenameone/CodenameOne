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
package com.codename1.nearby;

import com.codename1.impl.nearby.LocalNearbyBridge;
import com.codename1.impl.nearby.NearbyRequests;
import com.codename1.nearby.companion.AssociationRequest;
import com.codename1.nearby.companion.CompanionDevices;
import com.codename1.nearby.ranging.Ranging;
import com.codename1.nearby.ranging.RangingCapabilities;
import com.codename1.nearby.ranging.RangingRole;
import com.codename1.nearby.transport.Endpoint;
import com.codename1.nearby.transport.NearbyTransport;
import com.codename1.nearby.transport.Payload;
import com.codename1.nearby.transport.TransportStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.codename1.nearby.NearbyAwait.assertFailedWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every entry point does on a port that implements no bridge at all --
 * which is most of them, and is the state an app hits on a device whose OS is
 * too old.
 *
 * <p>The rule the whole family is built on: <b>nothing returns null and
 * nothing hangs.</b> A query answers a "no" the caller can act on, and an
 * operation fails fast with {@code NOT_SUPPORTED} rather than handing back a
 * resource that never settles. That is what lets application code skip the
 * platform conditionals entirely.</p>
 */
class NearbyDegradationTest {

    @BeforeEach
    void noBridgeAtAll() {
        NearbyRequests.resetForTest(null);
    }

    @AfterEach
    void clear() {
        NearbyRequests.resetForTest(null);
    }

    /// A bridge that does the transport but not ranging, which is what an
    /// ordinary Android phone without a UWB radio is.
    private static final class NoUwbBridge extends LocalNearbyBridge {
        @Override
        public boolean isRangingSupported() {
            return false;
        }
    }

    @Test
    void rangingPermissionsAreRefusedWhereRangingIsUnsupported() {
        // A bridge EXISTING is not the same as ranging working. Asking only
        // whether one was present sent PERMISSION_RANGING to a device with
        // no UWB radio, which either prompted for a permission the hardware
        // cannot use or answered true -- for a capability isSupported()
        // reports it does not have.
        NearbyRequests.resetForTest(new NoUwbBridge());
        assertFalse(Ranging.isSupported());
        assertFailedWith(NearbyError.NOT_SUPPORTED,
                Ranging.requestPermissions(NearbyPermission.RANGING));
        // The transport half of the same device still works, and asks for
        // its own permissions through its own entry point.
        assertTrue(NearbyTransport.isSupported());
    }

    @Test
    void everyEntryPointReportsItselfUnsupported() {
        assertFalse(Ranging.isSupported());
        assertFalse(CompanionDevices.isSupported());
        assertFalse(NearbyTransport.isSupported());
        assertSame(NearbyAvailability.NOT_SUPPORTED, Ranging.getAvailability());
        assertSame(NearbyAvailability.NOT_SUPPORTED,
                CompanionDevices.getAvailability());
        assertSame(NearbyAvailability.NOT_SUPPORTED,
                NearbyTransport.getAvailability());
    }

    @Test
    void capabilitiesAreAllFalseRatherThanNull() {
        RangingCapabilities c = Ranging.getCapabilities();
        assertNotNull(c, "getCapabilities must never return null: the whole"
                + " point is that callers need no null check");
        assertSame(RangingCapabilities.UNSUPPORTED, c);
        assertFalse(c.isDistanceSupported());
        assertFalse(c.isDirectionSupported());
        assertFalse(c.isElevationSupported());
        assertFalse(c.isCameraAssistanceSupported());
        assertFalse(c.isAccessoryRangingSupported());
        assertFalse(c.isBackgroundRangingSupported());
    }

    @Test
    void listQueriesAreEmptyRatherThanNull() {
        assertNotNull(CompanionDevices.getAssociations());
        assertTrue(CompanionDevices.getAssociations().isEmpty());
        assertEquals(0, NearbyTransport.getMaxPayloadSize());
    }

    @Test
    void everyOperationFailsFastRatherThanHanging() {
        assertFailedWith(NearbyError.NOT_SUPPORTED,
                Ranging.requestPermissions(NearbyPermission.RANGING));
        assertFailedWith(NearbyError.NOT_SUPPORTED,
                Ranging.prepareSession(RangingRole.CONTROLLER));
        assertFailedWith(NearbyError.NOT_SUPPORTED, CompanionDevices.associate(
                new AssociationRequest.Builder().build()));
        assertFailedWith(NearbyError.NOT_SUPPORTED,
                CompanionDevices.disassociate("whatever"));
        assertFailedWith(NearbyError.NOT_SUPPORTED,
                NearbyTransport.startAdvertising("svc", "me",
                        TransportStrategy.CLUSTER));
        assertFailedWith(NearbyError.NOT_SUPPORTED,
                NearbyTransport.startDiscovery("svc",
                        TransportStrategy.CLUSTER));
        assertFailedWith(NearbyError.NOT_SUPPORTED,
                NearbyTransport.requestConnection(
                        new Endpoint("e", "n", "svc"), "me"));
        assertFailedWith(NearbyError.NOT_SUPPORTED,
                NearbyTransport.send(new Endpoint("e", "n", "svc"),
                        Payload.fromBytes(new byte[] {1})));
    }

    @Test
    void voidOperationsAreInertRatherThanThrowing() {
        // An app tearing its UI down calls these on the way out, and it must
        // not have to know whether the feature was ever supported.
        CompanionDevices.stopObservingPresence("nope");
        assertFalse(CompanionDevices.startObservingPresence("nope"));
        NearbyTransport.stopAdvertising();
        NearbyTransport.stopDiscovery();
        NearbyTransport.disconnect(new Endpoint("e", "n", "svc"));
        NearbyTransport.cancel(7);
        NearbyTransport.stop();
    }

    @Test
    void deliveriesForRequestsNobodyIsWaitingOnAreIgnored() {
        // A port that answers twice, or answers after the caller cancelled,
        // must not take the process down with it.
        Ranging.deliverPermissionResult(9999, true);
        Ranging.deliverSessionStarted(9999, 1234);
        Ranging.deliverRequestFailed(9999, NearbyError.TIMEOUT.ordinal(), "x");
        CompanionDevices.deliverDisassociated(9999);
        CompanionDevices.deliverRequestFailed(9999, 0, null);
        NearbyTransport.deliverRequestOk(9999);
        NearbyTransport.deliverRequestFailed(9999, 0, null);
    }

    @Test
    void eventsNamingAMalformedRecordAreDroppedNotThrown() {
        // Native code hands these over; a record with no id is a bug in a
        // port, and losing that one event beats taking down the delivery.
        CompanionDevices.deliverPresenceChanged("", true);
        NearbyTransport.deliverEndpointFound("", true);
        NearbyTransport.deliverDisconnected("");
        NearbyTransport.deliverConnectionRequested("", "1234");
        NearbyTransport.deliverPayloadReceived("", 1, 0, new byte[0], null);
    }
}
