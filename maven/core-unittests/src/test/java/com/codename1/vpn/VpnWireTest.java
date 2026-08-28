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

import com.codename1.impl.vpn.TunnelWire;
import com.codename1.impl.vpn.VpnWire;
import com.codename1.vpn.profile.VpnProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void theLoadedRecordLayoutIsPinnedToItsFieldIndices() {
        // The iOS load path builds this record in Objective-C, where nothing
        // checks it against decodeProfile. The indices are the contract
        // between them, so they are pinned here: server, protocol, remote id,
        // local id, username, then the password and shared secret iOS never
        // returns, then the two reserved slots, then on-demand and the
        // display name.
        String record = "vpn.example.com\t0\tremote\tlocal\talice"
                + "\t\t\t\t\t1\tWork VPN";
        VpnProfile p = VpnWire.decodeProfile(record);
        assertNotNull(p);
        assertEquals("vpn.example.com", p.getServerAddress());
        assertEquals("remote", p.getRemoteIdentifier());
        assertEquals("local", p.getLocalIdentifier());
        assertEquals("alice", p.getUsername());
        assertTrue(p.isOnDemand(), "on-demand is field 9");
        assertEquals("Work VPN", p.getDisplayName(), "the name is field 10");
    }

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
    public void aProfileFromTheWireDoesNotClaimToCarrySecrets() {
        // decodeProfile is used for BOTH directions -- the ports decode an
        // install wire that carries its secrets, and Vpn.load() decodes a
        // platform description that carries none -- so the withheld marker
        // belongs on the load path, not here.
        VpnProfile p = new VpnProfile("vpn.example.com")
                .usernamePassword("u", "p");
        VpnProfile back = VpnWire.decodeProfile(VpnWire.encodeProfile(p));
        assertEquals("p", back.getPassword());
        assertTrue(back.isPasswordKnown());
    }

    @Test
    public void aRecordWithNoServerIsNotAProfile() {
        assertNull(VpnWire.decodeProfile(""));
        assertNull(VpnWire.decodeProfile(null));
    }

    @Test
    public void anAbsentPrefixIsFilledByFamily() {
        // A bare address is the ordinary way to write a host block, and
        // Builder.addAddress demands a number, so absence is filled rather
        // than refused.
        assertEquals(32, TunnelWire.prefix("10.0.0.2", "address"));
        assertEquals(128, TunnelWire.prefix("fd00::2", "address"));
        assertEquals("10.0.0.2", TunnelWire.host("10.0.0.2/32"));
        assertEquals("fd00::", TunnelWire.host("fd00::/8"));
    }

    @Test
    public void zeroIsTheDefaultRouteAndNotAnError() {
        // /0 is what a full-tunnel VPN asks for and what the documentation
        // shows. Reading it as unparseable handed back a host route.
        assertEquals(0, TunnelWire.prefix("0.0.0.0/0", "route"));
        assertEquals(0, TunnelWire.prefix("::/0", "route"));
        assertEquals(24, TunnelWire.prefix("10.0.0.0/24", "route"));
        assertEquals(64, TunnelWire.prefix("fd00::/64", "route"));
    }

    @Test
    public void anAddressThatIsNotOneIsRefused() {
        // The simulation used to check the prefix and accept any host, so
        // route("not-an-ip/32") started here and failed on Android, where
        // VpnService.Builder.addRoute throws on a literal it cannot parse.
        // A setup approved in the simulator that cannot come up on a phone
        // is the divergence this arrangement exists to remove.
        String[] bad = {"not-an-ip/32", "10.0.0/24", "10.0.0.256/24",
                "10.0.0.1.2/24", "vpn.example.com/32", "/24", "10.0.0.-1/24",
                "10.0..1/24"};
        for (String block : bad) {
            IllegalArgumentException e = assertThrows(
                    IllegalArgumentException.class,
                    () -> TunnelWire.validate(block, "route"),
                    block + " does not start with an address");
            assertTrue(e.getMessage().contains(block),
                    "the message names the block: " + e.getMessage());
        }
        // And every ordinary literal is still accepted, in both families and
        // with the prefix left off.
        String[] good = {"0.0.0.0/0", "10.0.0.2/32", "255.255.255.255",
                "::/0", "fd00::2/128", "fd00::", "::ffff:10.0.0.2/128",
                "2001:db8:0:0:0:0:0:1/64"};
        for (String block : good) {
            TunnelWire.validate(block, "route");
        }
    }

    @Test
    public void anUnreadablePrefixIsRefusedRatherThanShrunk() {
        // The most dangerous answer available was the one this used to give.
        // "0.0.0.0/o" is a typo for the default route, and falling back to
        // the family width turned it into a HOST route -- so the tunnel came
        // up acknowledged and carried one address, and every packet the app
        // believed it was protecting went out in the clear. A misread
        // full-tunnel route has to fail, not shrink.
        String[] bad = {"0.0.0.0/o", "0.0.0.0/", "10.0.0.0/33",
                "fd00::/129", "10.0.0.0/1234", "10.0.0.0/-1",
                "10.0.0.0/ 24 x"};
        for (String block : bad) {
            IllegalArgumentException e = assertThrows(
                    IllegalArgumentException.class,
                    () -> TunnelWire.prefix(block, "route"),
                    block + " is not a prefix and must be refused");
            assertTrue(e.getMessage().contains(block),
                    "the message names the block: " + e.getMessage());
            assertTrue(e.getMessage().contains("route"),
                    "and the field it came from: " + e.getMessage());
        }
    }
}
