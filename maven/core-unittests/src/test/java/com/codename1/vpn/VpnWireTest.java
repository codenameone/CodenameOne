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

import com.codename1.impl.vpn.VpnWire;
import com.codename1.vpn.profile.VpnProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The tab-delimited profile record, and the one thing it must never do:
 * change a credential on the way through.
 *
 * <p>The call wire's sanitizer replaces a tab, carriage return or newline
 * with a space. That is right for the display text it was written for and
 * wrong for a secret -- a password or pre-shared key carrying one of those
 * was installed as a DIFFERENT string, the platform acknowledged the install,
 * and authentication then failed with nothing anywhere to say the credential
 * had been altered.</p>
 */
public class VpnWireTest {

    @Test
    public void aSecretSurvivesTheRoundTripUnchanged() {
        String awkward = "p@ss\tword\nwith\rbreaks\\and a backslash";
        VpnProfile p = new VpnProfile("vpn.example.com")
                .protocol(VpnProtocol.IKEV2)
                .usernamePassword("CORP\\alice", awkward);
        VpnProfile back = VpnWire.decodeProfile(VpnWire.encodeProfile(p));
        assertEquals(awkward, back.getPassword(),
                "the password must arrive at the port byte for byte");
        assertEquals("CORP\\alice", back.getUsername(),
                "a domain username carries a backslash and is the common case");
    }

    @Test
    public void aSharedSecretSurvivesToo() {
        String psk = "pre\tshared\\key";
        VpnProfile p = new VpnProfile("vpn.example.com")
                .protocol(VpnProtocol.IKEV2)
                .sharedSecret(psk);
        assertEquals(psk,
                VpnWire.decodeProfile(VpnWire.encodeProfile(p)).getSharedSecret());
    }

    @Test
    public void anEscapedFieldCarriesNoRawSeparator() {
        // Whatever the escape looks like, the record still has to be
        // splittable on tabs -- the iOS port splits it in C.
        VpnProfile p = new VpnProfile("vpn.example.com")
                .protocol(VpnProtocol.IKEV2)
                .usernamePassword("u", "a\tb\nc");
        String wire = VpnWire.encodeProfile(p);
        assertEquals(11, wire.split("\t", -1).length,
                "the record must still have exactly its own fields");
    }

    @Test
    public void everyOtherFieldSurvivesUnchanged() {
        VpnProfile p = new VpnProfile("vpn.example.com")
                .protocol(VpnProtocol.IKEV2)
                .remoteIdentifier("remote\\id")
                .localIdentifier("local\tid")
                .displayName("Work \\ VPN")
                .onDemand(true);
        VpnProfile back = VpnWire.decodeProfile(VpnWire.encodeProfile(p));
        assertEquals("remote\\id", back.getRemoteIdentifier());
        assertEquals("local\tid", back.getLocalIdentifier());
        assertEquals("Work \\ VPN", back.getDisplayName());
        assertEquals(VpnProtocol.IKEV2, back.getProtocol());
    }

    @Test
    public void theEncodingIsReversibleForEveryAwkwardShape() {
        // Exercised through the public round trip rather than the helpers,
        // which stay package-private: the escape is an implementation detail
        // of this record and nothing outside should reach for it.
        String[] awkward = {
            "a\tb",
            "\\",
            // A literal backslash-t is TEXT, not a tab, and must come back as
            // the two characters it is.
            "\\t",
            "trailing\\",
            "a\\qb",
            "\r\n\t\\",
        };
        for (String value : awkward) {
            VpnProfile p = new VpnProfile("vpn.example.com")
                    .usernamePassword("u", value);
            assertEquals(value,
                    VpnWire.decodeProfile(VpnWire.encodeProfile(p)).getPassword(),
                    "round trip changed " + value);
        }
    }

    @Test
    public void aRecordWithNoServerIsNotAProfile() {
        assertNull(VpnWire.decodeProfile(""));
        assertNull(VpnWire.decodeProfile(null));
    }
}
