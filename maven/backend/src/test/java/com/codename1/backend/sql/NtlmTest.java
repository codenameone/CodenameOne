/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.sql;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/// NTLMv2 against published vectors: RFC 1320 for MD4, RFC 2202 for HMAC-MD5
/// and MS-NLMP 4.2.4 for the protocol itself. A SQL Server that runs on Linux
/// accepts no NTLM login, so these are what holds the arithmetic.
class NtlmTest {
    private static String hex(byte[] data) {
        StringBuilder out = new StringBuilder();
        for (byte b : data) {
            out.append(Character.forDigit((b >> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
        }
        return out.toString();
    }

    private static byte[] bytes(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    @Test
    void md4MatchesRfc1320() {
        assertEquals("31d6cfe0d16ae931b73c59d7e0c089c0", hex(Ntlm.md4(new byte[0])));
        assertEquals("bde52cb31de33e46245e05fbdbd6fb24", hex(Ntlm.md4(Wire.utf8("a"))));
        assertEquals("a448017aaf21d8525fc10ae87aa6729d", hex(Ntlm.md4(Wire.utf8("abc"))));
        assertEquals("d9130a8164549fe818874806e1c7014b", hex(Ntlm.md4(Wire.utf8("message digest"))));
        assertEquals("e33b4ddc9c38f2199c3e7b164fcc0536", hex(Ntlm.md4(Wire.utf8(
                "12345678901234567890123456789012345678901234567890123456789012345678901234567890"))));
    }

    @Test
    void hmacMd5MatchesRfc2202() {
        assertEquals("9294727a3638bb1c13f48ef8158bfc9d",
                hex(Ntlm.hmacMd5(bytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"), Wire.utf8("Hi There"))));
        assertEquals("750c783e6ab0b503eaa86e310a5db738",
                hex(Ntlm.hmacMd5(Wire.utf8("Jefe"), Wire.utf8("what do ya want for nothing?"))));
        byte[] longKey = new byte[80];
        java.util.Arrays.fill(longKey, (byte) 0xaa);
        assertEquals("6b1ab7fe4bd7bf8f0b62e6ce61b9d0cd", hex(Ntlm.hmacMd5(longKey,
                Wire.utf8("Test Using Larger Than Block-Size Key - Hash Key First"))));
    }

    @Test
    void utf16leRoundTrips() {
        assertEquals("500061007300730077006f0072006400", hex(Wire.utf16le("Password")));
        String mixed = "a\u00e9\u4e2d\ud83d\ude00";
        byte[] encoded = Wire.utf16le(mixed);
        assertEquals(mixed.length() * 2, encoded.length);
        assertEquals(mixed, Wire.fromUtf16le(encoded, 0, encoded.length));
    }

    /// MS-NLMP 4.2.4.1.1: User "User", domain "Domain", password "Password".
    @Test
    void keyMatchesTheSpecification() {
        assertEquals("0c868a403bfd7a93a3001ef22ef02e3f",
                hex(Ntlm.ntowfv2("Domain", "User", "Password")));
    }

    /// MS-NLMP 4.2.4: server challenge 0123456789abcdef, client challenge all
    /// 0xaa, time zero, and a target info naming the domain and the server.
    @Test
    void responseMatchesTheSpecification() throws IOException {
        byte[] targetInfo = bytes("02000c0044006f006d00610069006e00"
                + "01000c00530065007200760065007200" + "00000000");
        ByteArrayOutputStream challenge = new ByteArrayOutputStream();
        challenge.write(bytes("4e544c4d53535000" + "02000000"), 0, 12);
        // target name field (empty), flags, server challenge, reserved
        challenge.write(bytes("0000000038000000" + "33828ae2" + "0123456789abcdef"
                + "0000000000000000"), 0, 28);
        // target info: length, allocated, offset 56
        challenge.write(bytes("2400240038000000"), 0, 8);
        // version, which pads the header to 56 bytes
        challenge.write(bytes("0600700017000000" + "0f"), 0, 8);
        challenge.write(targetInfo, 0, targetInfo.length);
        byte[] message = Ntlm.authenticate(challenge.toByteArray(), "Domain", "User", "Password",
                bytes("aaaaaaaaaaaaaaaa"), 0L);
        assertEquals("68cd0ab851e51c96aabc927bebef6a1c", hex(Ntlm.proofOf(message)));
        // The LMv2 response is the first field, 24 bytes at offset 64.
        byte[] lm = new byte[24];
        System.arraycopy(message, 64, lm, 0, 24);
        assertEquals("86c35097ac9cec102554764a57cccc19aaaaaaaaaaaaaaaa", hex(lm));
    }

    @Test
    void negotiateIsWellFormed() {
        byte[] message = Ntlm.negotiate();
        assertEquals(32, message.length);
        assertArrayEquals(bytes("4e544c4d5353500001000000"), java.util.Arrays.copyOf(message, 12));
    }

    @Test
    void aMalformedChallengeIsRefused() {
        assertThrows(IOException.class, () -> Ntlm.authenticate(new byte[10], "", "u", "p",
                new byte[8], 0L));
        byte[] wrongType = new byte[64];
        System.arraycopy(bytes("4e544c4d5353500003000000"), 0, wrongType, 0, 12);
        assertThrows(IOException.class, () -> Ntlm.authenticate(wrongType, "", "u", "p",
                new byte[8], 0L));
    }

    @Test
    void fileTimeCountsFrom1601() {
        assertEquals(116444736000000000L, Ntlm.fileTime(0L));
    }
}
