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

import com.codename1.impl.nearby.NearbyWire;
import com.codename1.nearby.companion.CompanionDevice;
import com.codename1.nearby.companion.CompanionProfile;
import com.codename1.nearby.companion.DeviceFilter;
import com.codename1.nearby.transport.Endpoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The encoding the SPI speaks.
 *
 * <p>The property that matters most is that <b>every decoder is total</b>.
 * Records arrive from native code in batches, so a decoder that threw on a bad
 * row would discard the good rows next to it -- and the bad row is most often
 * a port from a newer build naming something this one has not heard of.</p>
 */
class NearbyWireTest {

    @Test
    void splitPreservesTrailingEmptyFields() {
        // String.split drops them, which would shift every index for a device
        // that has no address and is not present.
        String[] f = NearbyWire.split("a\tb\t\t");
        assertEquals(4, f.length);
        assertEquals("a", f[0]);
        assertEquals("b", f[1]);
        assertEquals("", f[2]);
        assertEquals("", f[3]);
    }

    @Test
    void readingPastTheEndOfARecordGivesEmptyRatherThanThrowing() {
        String[] f = NearbyWire.split("only");
        assertEquals("", NearbyWire.field(f, 7));
        assertEquals("", NearbyWire.field(null, 0));
        assertEquals("", NearbyWire.field(f, -1));
        assertEquals(5, NearbyWire.integer(f, 7, 5));
        assertEquals(5L, NearbyWire.integer64(f, 7, 5L));
        assertTrue(!NearbyWire.flag(f, 7));
    }

    @Test
    void aFieldThatIsNotANumberFallsBackRatherThanThrowing() {
        String[] f = NearbyWire.split("abc\t12");
        assertEquals(-1, NearbyWire.integer(f, 0, -1));
        assertEquals(12, NearbyWire.integer(f, 1, -1));
        assertEquals(-1L, NearbyWire.integer64(f, 0, -1L));
    }

    @Test
    void aSeparatorInsideAFieldCannotSplitTheRecord() {
        String encoded = NearbyWire.join(new String[] {
            "id", "a\tname\nwith\rcontrol chars", "svc"
        });
        String[] f = NearbyWire.split(encoded);
        assertEquals(3, f.length);
        assertEquals("a name with control chars", f[1]);
    }

    @Test
    void aNullFieldEncodesAsEmpty() {
        assertEquals("", NearbyWire.sanitize(null));
        assertEquals("a\t\tb", NearbyWire.join(new String[] {"a", null, "b"}));
        assertEquals("", NearbyWire.join(null));
    }

    @Test
    void aCompanionDeviceSurvivesTheRoundTrip() {
        CompanionDevice d = new CompanionDevice("assoc-1", "Watch",
                "00:11:22:33:44:55", CompanionProfile.WATCH, true);
        CompanionDevice back = NearbyWire.decodeCompanionDevice(
                NearbyWire.encodeCompanionDevice(d));
        assertNotNull(back);
        assertEquals("assoc-1", back.getId());
        assertEquals("Watch", back.getDisplayName());
        assertEquals("00:11:22:33:44:55", back.getAddress());
        assertSame(CompanionProfile.WATCH, back.getProfile());
        assertTrue(back.isPresent());
    }

    @Test
    void anAbsentAddressDecodesToNullRatherThanEmpty() {
        // getAddress() documents null as "the platform withholds it", and an
        // empty string here would be handed straight to
        // BluetoothLE.getPeripheral.
        CompanionDevice d = new CompanionDevice("assoc-2", "Tag", null,
                CompanionProfile.GENERIC, false);
        CompanionDevice back = NearbyWire.decodeCompanionDevice(
                NearbyWire.encodeCompanionDevice(d));
        assertNotNull(back);
        assertNull(back.getAddress());
        assertTrue(!back.isPresent());
    }

    @Test
    void aRecordWithNoIdDecodesToNullSoTheCallerCanSkipIt() {
        assertNull(NearbyWire.decodeCompanionDevice(""));
        assertNull(NearbyWire.decodeCompanionDevice("\tname\t\t0\t0"));
        assertNull(NearbyWire.decodeCompanionDevice(null));
        assertNull(NearbyWire.decodeEndpoint(""));
        assertNull(NearbyWire.decodeEndpoint(null));
    }

    @Test
    void aProfileOrdinalFromANewerBuildDegradesRatherThanLosingTheRecord() {
        CompanionDevice back = NearbyWire.decodeCompanionDevice(
                "assoc-3\tSomething\t\t97\t1");
        assertNotNull(back, "an unknown profile must not cost us the device");
        assertSame(CompanionProfile.GENERIC, back.getProfile());
        assertSame(CompanionProfile.GENERIC, NearbyWire.profileFor(-1));
    }

    @Test
    void anEndpointSurvivesTheRoundTrip() {
        Endpoint e = new Endpoint("ep-1", "Phone", "svc");
        Endpoint back = NearbyWire.decodeEndpoint(NearbyWire.encodeEndpoint(e));
        assertNotNull(back);
        assertEquals("ep-1", back.getId());
        assertEquals("Phone", back.getName());
        assertEquals("svc", back.getServiceId());
        assertEquals(e, back);
    }

    @Test
    void aFilterEncodesAsItsKindAndValue() {
        String[] f = NearbyWire.split(
                NearbyWire.encodeFilter(DeviceFilter.bleService("180D")));
        assertEquals(DeviceFilter.KIND_BLE_SERVICE,
                NearbyWire.integer(f, 0, -1));
        assertEquals("180D", NearbyWire.field(f, 1));
    }

    @Test
    void decodeErrorIsTheOneDecoderThatAlwaysProducesSomething() {
        NearbyException known = NearbyWire.decodeError(
                NearbyError.TIMEOUT.ordinal(), "took too long");
        assertSame(NearbyError.TIMEOUT, known.getError());
        assertEquals("took too long", known.getMessage());

        NearbyException unknown = NearbyWire.decodeError(9999, null);
        assertSame(NearbyError.UNKNOWN, unknown.getError());
        assertEquals("UNKNOWN", unknown.getMessage());

        NearbyException blank = NearbyWire.decodeError(
                NearbyError.BUSY.ordinal(), "");
        assertEquals("BUSY", blank.getMessage());
    }
}
