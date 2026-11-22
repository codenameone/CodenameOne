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
package com.codename1.db.sql;

import com.codename1.security.Hash;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// NTLMv2, the Windows challenge-response sign-in, for SQL Server logins that
/// name a Windows account rather than a SQL one. Written from MS-NLMP.
///
/// Three messages: the client's NEGOTIATE, the server's CHALLENGE and the
/// client's AUTHENTICATE, which proves knowledge of the password without
/// sending it. Only version 2 is spoken; NTLMv1 and LM responses are never
/// produced, whatever the server asks for.
///
/// MD4 and HMAC-MD5 live here and not in the backend's `Crypto`: this protocol is their
/// only use, both are broken as general-purpose primitives, and here an
/// application that is not built for SQL Server does not carry them.
@com.codename1.impl.SharedWithBackend
final class Ntlm {
    private static final byte[] SIGNATURE = {'N', 'T', 'L', 'M', 'S', 'S', 'P', 0};

    private static final int NEGOTIATE_UNICODE = 0x00000001;
    private static final int REQUEST_TARGET = 0x00000004;
    private static final int NEGOTIATE_NTLM = 0x00000200;
    private static final int NEGOTIATE_ALWAYS_SIGN = 0x00008000;
    private static final int NEGOTIATE_EXTENDED_SESSIONSECURITY = 0x00080000;
    private static final int NEGOTIATE_TARGET_INFO = 0x00800000;

    private static final int FLAGS = NEGOTIATE_UNICODE | REQUEST_TARGET | NEGOTIATE_NTLM
            | NEGOTIATE_ALWAYS_SIGN | NEGOTIATE_EXTENDED_SESSIONSECURITY | NEGOTIATE_TARGET_INFO;

    /// MsvAvTimestamp, the attribute of the server's target info that carries
    /// its clock.
    private static final int AV_TIMESTAMP = 7;

    private Ntlm() {
    }

    /// The NEGOTIATE message, which names no domain and no workstation.
    static byte[] negotiate() {
        ByteArrayOutputStream out = new ByteArrayOutputStream(40);
        out.write(SIGNATURE, 0, SIGNATURE.length);
        int32(out, 1);
        int32(out, FLAGS);
        // Empty domain and workstation fields, both pointing past the header.
        field(out, 0, 32);
        field(out, 0, 32);
        return out.toByteArray();
    }

    /// The AUTHENTICATE message answering a server CHALLENGE.
    ///
    /// @param challenge the server's message, as it arrived
    /// @param domain the account's domain, or an empty string
    /// @param clientChallenge eight random bytes
    /// @param fileTime the time to stamp the response with, in 100ns units
    ///        since 1601, used only when the server sent no clock of its own
    static byte[] authenticate(byte[] challenge, String domain, String user, String password,
                               byte[] clientChallenge, long fileTime) throws IOException {
        if (challenge == null || challenge.length < 48 || !startsWithSignature(challenge)
                || int32(challenge, 8) != 2) {
            throw new IOException("The server's NTLM challenge is malformed");
        }
        byte[] serverChallenge = new byte[8];
        System.arraycopy(challenge, 24, serverChallenge, 0, 8);
        int infoLength = int16(challenge, 40);
        int infoOffset = int32(challenge, 44);
        if (infoOffset < 0 || infoLength < 0 || infoOffset > challenge.length - infoLength) {
            throw new IOException("The server's NTLM challenge is malformed");
        }
        byte[] targetInfo = new byte[infoLength];
        System.arraycopy(challenge, infoOffset, targetInfo, 0, infoLength);

        // The server's own clock when it sends one: a response stamped with a
        // time the domain controller disagrees with is refused.
        long stamp = fileTime;
        boolean serverClock = false;
        int at = 0;
        while (at + 4 <= targetInfo.length) {
            int id = int16(targetInfo, at);
            int length = int16(targetInfo, at + 2);
            if (id == 0 || at + 4 + length > targetInfo.length) {
                break;
            }
            if (id == AV_TIMESTAMP && length == 8) {
                stamp = int64(targetInfo, at + 4);
                serverClock = true;
            }
            at += 4 + length;
        }

        byte[] key = ntowfv2(domain, user, password);
        ByteArrayOutputStream temp = new ByteArrayOutputStream(48 + targetInfo.length);
        temp.write(1);
        temp.write(1);
        for (int i = 0; i < 6; i++) {
            temp.write(0);
        }
        int64(temp, stamp);
        temp.write(clientChallenge, 0, 8);
        int32(temp, 0);
        temp.write(targetInfo, 0, targetInfo.length);
        int32(temp, 0);
        byte[] blob = temp.toByteArray();

        byte[] proof = hmacMd5(key, concat(serverChallenge, blob));
        byte[] ntResponse = concat(proof, blob);
        // MS-NLMP 3.1.5.1.2: with the server's timestamp in play the LM field
        // is 24 zero bytes; otherwise it is the LMv2 response.
        byte[] lmResponse;
        if (serverClock) {
            lmResponse = new byte[24];
        } else {
            lmResponse = concat(hmacMd5(key, concat(serverChallenge, clientChallenge)), clientChallenge);
        }

        byte[] domainBytes = Wire.utf16le(domain);
        byte[] userBytes = Wire.utf16le(user);
        int header = 64;
        ByteArrayOutputStream out = new ByteArrayOutputStream(header + lmResponse.length
                + ntResponse.length + domainBytes.length + userBytes.length);
        out.write(SIGNATURE, 0, SIGNATURE.length);
        int32(out, 3);
        int offset = header;
        field(out, lmResponse.length, offset);
        offset += lmResponse.length;
        field(out, ntResponse.length, offset);
        offset += ntResponse.length;
        field(out, domainBytes.length, offset);
        offset += domainBytes.length;
        field(out, userBytes.length, offset);
        offset += userBytes.length;
        // No workstation name and no session key.
        field(out, 0, offset);
        field(out, 0, offset);
        int32(out, FLAGS);
        out.write(lmResponse, 0, lmResponse.length);
        out.write(ntResponse, 0, ntResponse.length);
        out.write(domainBytes, 0, domainBytes.length);
        out.write(userBytes, 0, userBytes.length);
        return out.toByteArray();
    }

    /// The NTLMv2 key: HMAC-MD5, keyed with MD4 of the password, over the
    /// upper-cased user name followed by the domain as given.
    static byte[] ntowfv2(String domain, String user, String password) {
        // Character by character: String.toUpperCase is locale sensitive, and
        // on a Turkish host would fold the i of "admin" to a dotted capital
        // the server never derived.
        StringBuilder identity = new StringBuilder(user.length() + domain.length());
        for (int i = 0; i < user.length(); i++) {
            identity.append(Character.toUpperCase(user.charAt(i)));
        }
        identity.append(domain);
        return hmacMd5(md4(Wire.utf16le(password)), Wire.utf16le(identity.toString()));
    }

    /// The NTProofStr of a response, for the tests: the first 16 bytes of the
    /// NT field of an AUTHENTICATE message.
    static byte[] proofOf(byte[] authenticate) {
        byte[] out = new byte[16];
        System.arraycopy(authenticate, int32(authenticate, 24), out, 0, 16);
        return out;
    }

    /// Windows FILETIME for a Java clock reading.
    static long fileTime(long millis) {
        return (millis + 11644473600000L) * 10000L;
    }

    /// HMAC over MD5 (RFC 2104).
    static byte[] hmacMd5(byte[] key, byte[] data) {
        byte[] block = new byte[64];
        byte[] used = key.length > 64 ? Hash.md5(key) : key;
        System.arraycopy(used, 0, block, 0, used.length);
        byte[] inner = new byte[64 + data.length];
        byte[] outer = new byte[64 + 16];
        for (int i = 0; i < 64; i++) {
            inner[i] = (byte) (block[i] ^ 0x36);
            outer[i] = (byte) (block[i] ^ 0x5c);
        }
        System.arraycopy(data, 0, inner, 64, data.length);
        System.arraycopy(Hash.md5(inner), 0, outer, 64, 16);
        return Hash.md5(outer);
    }

    /// MD4 (RFC 1320).
    static byte[] md4(byte[] data) {
        int padded = ((data.length + 8) / 64 + 1) * 64;
        byte[] message = new byte[padded];
        System.arraycopy(data, 0, message, 0, data.length);
        message[data.length] = (byte) 0x80;
        long bits = (long) data.length * 8;
        for (int i = 0; i < 8; i++) {
            message[padded - 8 + i] = (byte) (bits >>> (8 * i));
        }
        int a = 0x67452301;
        int b = 0xefcdab89;
        int c = 0x98badcfe;
        int d = 0x10325476;
        int[] x = new int[16];
        for (int offset = 0; offset < padded; offset += 64) {
            for (int i = 0; i < 16; i++) {
                x[i] = int32(message, offset + i * 4);
            }
            int aa = a;
            int bb = b;
            int cc = c;
            int dd = d;
            for (int i = 0; i < 16; i += 4) {
                a = rotl(a + ((b & c) | (~b & d)) + x[i], 3);
                d = rotl(d + ((a & b) | (~a & c)) + x[i + 1], 7);
                c = rotl(c + ((d & a) | (~d & b)) + x[i + 2], 11);
                b = rotl(b + ((c & d) | (~c & a)) + x[i + 3], 19);
            }
            for (int i = 0; i < 4; i++) {
                a = rotl(a + ((b & c) | (b & d) | (c & d)) + x[i] + 0x5a827999, 3);
                d = rotl(d + ((a & b) | (a & c) | (b & c)) + x[i + 4] + 0x5a827999, 5);
                c = rotl(c + ((d & a) | (d & b) | (a & b)) + x[i + 8] + 0x5a827999, 9);
                b = rotl(b + ((c & d) | (c & a) | (d & a)) + x[i + 12] + 0x5a827999, 13);
            }
            for (int i = 0; i < 4; i++) {
                int j = ORDER[i];
                a = rotl(a + (b ^ c ^ d) + x[j] + 0x6ed9eba1, 3);
                d = rotl(d + (a ^ b ^ c) + x[j + 8] + 0x6ed9eba1, 9);
                c = rotl(c + (d ^ a ^ b) + x[j + 4] + 0x6ed9eba1, 11);
                b = rotl(b + (c ^ d ^ a) + x[j + 12] + 0x6ed9eba1, 15);
            }
            a += aa;
            b += bb;
            c += cc;
            d += dd;
        }
        byte[] out = new byte[16];
        put32(out, 0, a);
        put32(out, 4, b);
        put32(out, 8, c);
        put32(out, 12, d);
        return out;
    }

    /// The word order of MD4's third round, by group of four.
    private static final int[] ORDER = {0, 2, 1, 3};

    private static int rotl(int value, int bits) {
        return (value << bits) | (value >>> (32 - bits));
    }

    private static boolean startsWithSignature(byte[] message) {
        for (int i = 0; i < SIGNATURE.length; i++) {
            if (message[i] != SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] out = new byte[first.length + second.length];
        System.arraycopy(first, 0, out, 0, first.length);
        System.arraycopy(second, 0, out, first.length, second.length);
        return out;
    }

    /// A security buffer: length, allocated length, offset.
    private static void field(ByteArrayOutputStream out, int length, int offset) {
        out.write(length);
        out.write(length >> 8);
        out.write(length);
        out.write(length >> 8);
        int32(out, offset);
    }

    private static void int32(ByteArrayOutputStream out, int value) {
        out.write(value);
        out.write(value >> 8);
        out.write(value >> 16);
        out.write(value >>> 24);
    }

    private static void int64(ByteArrayOutputStream out, long value) {
        for (int i = 0; i < 8; i++) {
            out.write((int) (value >>> (8 * i)));
        }
    }

    private static int int16(byte[] data, int at) {
        return (data[at] & 0xff) | ((data[at + 1] & 0xff) << 8);
    }

    private static int int32(byte[] data, int at) {
        return (data[at] & 0xff) | ((data[at + 1] & 0xff) << 8)
                | ((data[at + 2] & 0xff) << 16) | ((data[at + 3] & 0xff) << 24);
    }

    private static long int64(byte[] data, int at) {
        return (int32(data, at) & 0xffffffffL) | ((long) int32(data, at + 4) << 32);
    }

    private static void put32(byte[] out, int at, int value) {
        out[at] = (byte) value;
        out[at + 1] = (byte) (value >> 8);
        out[at + 2] = (byte) (value >> 16);
        out[at + 3] = (byte) (value >>> 24);
    }
}
