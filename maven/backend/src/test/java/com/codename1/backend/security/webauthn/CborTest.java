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
package com.codename1.backend.security.webauthn;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// The CBOR reader: what it reads, what it refuses, and that bytes nobody
/// vouches for can make it do nothing but refuse.
class CborTest {
    private static byte[] hex(String hex) {
        return W3cTestVectors.hex(hex.replace(" ", ""));
    }

    private static String refused(byte[] data) {
        try {
            Object value = Cbor.decode(data);
            return fail("read as " + value);
        } catch (WebAuthnException expected) {
            assertEquals(WebAuthnException.MALFORMED_CBOR, expected.getReason());
            return expected.getMessage();
        }
    }

    private static void refused(String hex, String because) {
        String message = refused(hex(hex));
        assertTrue(message.contains(because), hex + ": " + message);
    }

    @Test
    @DisplayName("integers, strings, arrays, maps, truth values and null")
    void reads() {
        // The examples of RFC 8949, appendix A, that are of the kinds read.
        assertEquals(Long.valueOf(0), Cbor.decode(hex("00")));
        assertEquals(Long.valueOf(1), Cbor.decode(hex("01")));
        assertEquals(Long.valueOf(10), Cbor.decode(hex("0a")));
        assertEquals(Long.valueOf(23), Cbor.decode(hex("17")));
        assertEquals(Long.valueOf(24), Cbor.decode(hex("1818")));
        assertEquals(Long.valueOf(25), Cbor.decode(hex("1819")));
        assertEquals(Long.valueOf(100), Cbor.decode(hex("1864")));
        assertEquals(Long.valueOf(1000), Cbor.decode(hex("1903e8")));
        assertEquals(Long.valueOf(1000000), Cbor.decode(hex("1a000f4240")));
        assertEquals(Long.valueOf(1000000000000L), Cbor.decode(hex("1b000000e8d4a51000")));
        assertEquals(Long.valueOf(Long.MAX_VALUE), Cbor.decode(hex("1b7fffffffffffffff")));
        assertEquals(Long.valueOf(-1), Cbor.decode(hex("20")));
        assertEquals(Long.valueOf(-10), Cbor.decode(hex("29")));
        assertEquals(Long.valueOf(-100), Cbor.decode(hex("3863")));
        assertEquals(Long.valueOf(-1000), Cbor.decode(hex("3903e7")));
        assertEquals(Long.valueOf(Long.MIN_VALUE), Cbor.decode(hex("3b7fffffffffffffff")));
        // The COSE algorithm identifiers.
        assertEquals(Long.valueOf(-7), Cbor.decode(hex("26")));
        assertEquals(Long.valueOf(-257), Cbor.decode(hex("390100")));
        assertEquals(Boolean.FALSE, Cbor.decode(hex("f4")));
        assertEquals(Boolean.TRUE, Cbor.decode(hex("f5")));
        assertNull(Cbor.decode(hex("f6")));
        assertArrayEquals(new byte[0], (byte[]) Cbor.decode(hex("40")));
        assertArrayEquals(new byte[] {1, 2, 3, 4}, (byte[]) Cbor.decode(hex("4401020304")));
        assertEquals("", Cbor.decode(hex("60")));
        assertEquals("a", Cbor.decode(hex("6161")));
        assertEquals("IETF", Cbor.decode(hex("6449455446")));
        assertEquals("\"\\", Cbor.decode(hex("62225c")));
        assertEquals("\u00fc", Cbor.decode(hex("62c3bc")));
        assertEquals("\u6c34", Cbor.decode(hex("63e6b0b4")));
        assertEquals("\ud800\udd51", Cbor.decode(hex("64f0908591")));
        assertEquals(Arrays.asList(), Cbor.decode(hex("80")));
        assertEquals(Arrays.asList(Long.valueOf(1), Long.valueOf(2), Long.valueOf(3)),
                Cbor.decode(hex("83010203")));
        assertEquals("[1, [2, 3], [4, 5]]", String.valueOf(Cbor.decode(hex("8301820203820405"))));
        assertEquals(25, ((List) Cbor.decode(hex(
                "98190102030405060708090a0b0c0d0e0f101112131415161718181819"))).size());
        assertEquals("{}", String.valueOf(Cbor.decode(hex("a0"))));
        assertEquals("{1=2, 3=4}", String.valueOf(Cbor.decode(hex("a201020304"))));
        assertEquals("{a=1, b=[2, 3]}", String.valueOf(Cbor.decode(hex("a26161016162820203"))));
        assertEquals("[a, {b=c}]", String.valueOf(Cbor.decode(hex("826161a161626163"))));
        // A map keeps the order it was sent in.
        assertEquals("[e, a, c]", String.valueOf(new ArrayList<Object>(((Map) Cbor.decode(
                hex("a3616501616102616303"))).keySet())));
        // A longer encoding than a number needs is still that number.
        assertEquals(Long.valueOf(5), Cbor.decode(hex("1805")));
        assertEquals(Long.valueOf(5), Cbor.decode(hex("1b0000000000000005")));
    }

    @Test
    @DisplayName("strings, arrays and maps of indefinite length are what their definite forms are")
    void indefiniteLengths() {
        // RFC 8949, appendix A.
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5},
                (byte[]) Cbor.decode(hex("5f42010243030405ff")));
        assertEquals("streaming", Cbor.decode(hex("7f657374726561646d696e67ff")));
        assertEquals("[]", String.valueOf(Cbor.decode(hex("9fff"))));
        assertEquals("[1, [2, 3], [4, 5]]",
                String.valueOf(Cbor.decode(hex("9f018202039f0405ffff"))));
        assertEquals("[1, [2, 3], [4, 5]]", String.valueOf(Cbor.decode(hex("9f01820203820405ff"))));
        assertEquals("[1, [2, 3], [4, 5]]", String.valueOf(Cbor.decode(hex("83018202039f0405ff"))));
        assertEquals("[1, [2, 3], [4, 5]]", String.valueOf(Cbor.decode(hex("83019f0203ff820405"))));
        assertEquals("{a=1, b=[2, 3]}", String.valueOf(Cbor.decode(hex("bf61610161629f0203ffff"))));
        assertEquals("[a, {b=c}]", String.valueOf(Cbor.decode(hex("826161bf61626163ff"))));
        assertEquals("{Fun=true, Amt=-2}",
                String.valueOf(Cbor.decode(hex("bf6346756ef563416d7421ff"))));
        assertArrayEquals(new byte[0], (byte[]) Cbor.decode(hex("5fff")));
        assertEquals("", Cbor.decode(hex("7fff")));
        // A character may be split between chunks: the whole is what must be text.
        assertEquals("\u00fc", Cbor.decode(hex("7f61c361bcff")));
    }

    @Test
    @DisplayName("reads one value out of a longer buffer and says where it ended")
    void inPlace() {
        int[] next = new int[1];
        byte[] data = hex("ff 83010203 a0 00");
        assertEquals("[1, 2, 3]", String.valueOf(Cbor.decode(data, 1, next)));
        assertEquals(5, next[0]);
        assertEquals("{}", String.valueOf(Cbor.decode(data, next[0], next)));
        assertEquals(6, next[0]);
    }

    @Test
    @DisplayName("what a ceremony has no use for is refused, by name")
    void refusals() {
        refused("c074323031332d30332d32315432303a30343a30305a", "a tag");
        refused("c11a514b67b0", "a tag");
        refused("d74401020304", "a tag");
        refused("f90000", "a floating point number");
        refused("fa47c35000", "a floating point number");
        refused("fb3ff199999999999a", "a floating point number");
        refused("f7", "the simple value 23");
        refused("f0", "the simple value 16");
        refused("f8ff", "the simple value 24");
        refused("1c", "the reserved additional information 28");
        refused("3d", "the reserved additional information 29");
        refused("5e", "the reserved additional information 30");
        refused("1f", "an integer of indefinite length");
        refused("3f", "an integer of indefinite length");
        refused("ff", "a break outside");
        refused("8201ff", "a break inside an array of definite length");
        refused("a10102ff", "follow the value");
        refused("a1ff00", "a break inside a map of definite length");
        refused("bf01ff", "a break where a map value should be");
        refused("5f6161ff", "a chunk of another type");
        refused("7f4161ff", "a chunk of another type");
        refused("5f5f4101ffff", "a chunk of indefinite length");
        refused("5f01ff", "a chunk of another type");
        refused("a2010201 03", "the map key 1 twice");
        refused("a2616101616102", "the map key a twice");
        refused("bf01020103ff", "the map key 1 twice");
        refused("a1410102", "a map key that is neither");
        refused("a1800102", "a map key that is neither");
        refused("a1f501", "a map key that is neither");
        refused("a1f601", "a map key that is neither");
        // More than 63 bits: no length, and no number this reads.
        refused("1b8000000000000000", "a number over 63 bits");
        refused("3bffffffffffffffff", "a number over 63 bits");
        refused("5bffffffffffffffff", "a number over 63 bits");
        refused("0000", "1 bytes follow the value");
        refused("", "the input ends where a value should start");
    }

    @Test
    @DisplayName("text must be well-formed UTF-8")
    void utf8() {
        for (String bad : new String[] {
            "61 80", "61 ff", "62 c080", "62 c1bf", "63 e08080", "63 eda080", "63 edbfbf",
            "64 f0808080", "64 f4908080", "64 f5808080", "61 c3", "62 e282", "63 f09f98",
            "62 c328", "63 e228a1", "7f 61c3 6128 ff"}) {
            refused(bad, "text that is not UTF-8");
        }
        // The edges that are well-formed.
        assertEquals("\u0080", Cbor.decode(hex("62c280")));
        assertEquals("\u0800", Cbor.decode(hex("63e0a080")));
        assertEquals("\ud7ff", Cbor.decode(hex("63ed9fbf")));
        assertEquals("\ue000", Cbor.decode(hex("63ee8080")));
        assertEquals("\udbff\udfff", Cbor.decode(hex("64f48fbfbf")));
    }

    @Test
    @DisplayName("a length is checked against what is there before anything is allocated for it")
    void lengthsAreNotTakenOnTrust() {
        // Each of these announces gigabytes and is a handful of bytes.
        refused("5a7fffffff", "a string of 2147483647 bytes with 0 left");
        refused("5b000000007fffffff00", "a string of 2147483647 bytes with 1 left");
        refused("7b0000000100000000", "a string of 4294967296 bytes");
        refused("9a7fffffff", "an array of 2147483647 elements with 0 bytes left");
        refused("9b00000000ffffffff0102", "an array of 4294967295 elements with 2 bytes left");
        refused("ba7fffffff", "a map of 2147483647 entries");
        refused("bb7fffffffffffffff0102", "a map of 9223372036854775807 entries");
        // A map of n entries needs 2n bytes: three bytes cannot hold two.
        refused("a2010203", "a map of 2 entries with 3 bytes left");
        refused("5f5a7fffffffff", "a string of 2147483647 bytes");
        // An array may claim exactly what is there and not one more.
        assertEquals(3, ((List) Cbor.decode(hex("83000000"))).size());
        refused("84000000", "an array of 4 elements with 3 bytes left");
        refused("82616162 61", "a string of 2 bytes with 1 left");
    }

    @Test
    @DisplayName("nesting is bounded, however it is written")
    void depth() {
        StringBuilder deep = new StringBuilder();
        for (int iter = 0 ; iter < Cbor.MAX_DEPTH ; iter++) {
            deep.append("81");
        }
        // As deep as is read...
        assertTrue(Cbor.decode(hex(deep + "00")) instanceof List);
        // ...and one deeper, in each of the four ways of nesting.
        refused("81" + deep + "00", "nested deeper than " + Cbor.MAX_DEPTH);
        refused(deep.toString().replace("81", "9f") + "9f00", "nested deeper than");
        refused(deep.toString().replace("81", "a100") + "a10000", "nested deeper than");
        refused(deep.toString().replace("81", "bf00") + "bf0000", "nested deeper than");
        // Thousands deep is refused at the same place, not by running out of stack.
        byte[] abyss = new byte[60000];
        Arrays.fill(abyss, (byte) 0x9f);
        assertTrue(refused(abyss).contains("nested deeper than"));
        Arrays.fill(abyss, (byte) 0x81);
        assertTrue(refused(abyss).contains("nested deeper than"));
    }

    @Test
    @DisplayName("an input over the limit is refused before it is read")
    void size() {
        byte[] large = new byte[Cbor.MAX_BYTES + 1];
        large[0] = 0x5a;
        large[1] = 0;
        large[2] = 1;
        large[3] = 0;
        large[4] = 0;
        assertTrue(refused(large).contains("at most " + Cbor.MAX_BYTES + " are read"));
        // At the limit it is read: a byte string that fills it.
        byte[] most = new byte[Cbor.MAX_BYTES];
        most[0] = 0x59;
        most[1] = (byte) ((Cbor.MAX_BYTES - 3) >> 8);
        most[2] = (byte) (Cbor.MAX_BYTES - 3);
        assertEquals(Cbor.MAX_BYTES - 3, ((byte[]) Cbor.decode(most)).length);
        // And an indefinite array of as many elements as fit is read too.
        byte[] many = new byte[Cbor.MAX_BYTES];
        many[0] = (byte) 0x9f;
        many[many.length - 1] = (byte) 0xff;
        assertEquals(Cbor.MAX_BYTES - 2, ((List) Cbor.decode(many)).size());
    }

    /// Valid inputs to break: real attestation objects, a COSE key, and one
    /// written with every indefinite form.
    private static List<byte[]> seeds() throws Exception {
        List<byte[]> seeds = new ArrayList<byte[]>();
        for (String name : new String[] {"none-es256", "packed-self-es256", "packed-rs256",
            "tpm-es256", "fido-u2f-es256"}) {
            seeds.add(W3cTestVectors.hex(W3cTestVectors.get(name)[3]));
        }
        Map<String, Object> nested = new LinkedHashMap<String, Object>();
        nested.put("fmt", "packed");
        Map<String, Object> statement = new LinkedHashMap<String, Object>();
        statement.put("alg", Long.valueOf(-7));
        statement.put("sig", new byte[70]);
        statement.put("x5c", Arrays.asList(new byte[40], new byte[3]));
        nested.put("attStmt", statement);
        nested.put("authData", new byte[164]);
        nested.put("flags", Arrays.asList(Boolean.TRUE, null, Long.valueOf(-257), "\u00fc"));
        seeds.add(SoftAuthenticator.cbor(nested, false));
        seeds.add(SoftAuthenticator.cbor(nested, true));
        seeds.add(SoftAuthenticator.cbor(new SoftAuthenticator(false, "example.org",
                "https://example.org").coseKey(), false));
        seeds.add(SoftAuthenticator.cbor(new SoftAuthenticator(true, "example.org",
                "https://example.org").coseKey(), false));
        return seeds;
    }

    /// Reads `data`; anything but a value or this reader's own refusal fails.
    private static boolean survives(byte[] data, String what) {
        try {
            Cbor.decode(data);
            return true;
        } catch (WebAuthnException refused) {
            assertEquals(WebAuthnException.MALFORMED_CBOR, refused.getReason(), what);
            return false;
        } catch (Throwable other) {
            return fail(what + " made the reader throw " + other, other);
        }
    }

    @Test
    @DisplayName("every truncation of a valid input is refused, and by this reader's own exception")
    void truncations() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            int tried = 0;
            for (byte[] seed : seeds()) {
                // The instrument: the whole thing reads.
                assertTrue(survives(seed, "the seed"));
                for (int length = 0 ; length < seed.length ; length++) {
                    // CBOR is self-delimiting: no proper prefix of a value is a value.
                    assertTrue(!survives(Arrays.copyOf(seed, length), "truncated to " + length),
                            "a truncation to " + length + " of " + seed.length + " was read");
                    tried++;
                }
            }
            assertTrue(tried > 4000, "only " + tried + " truncations tried");
        });
    }

    @Test
    @DisplayName("no flipped bit and no random input makes the reader do anything but read or refuse")
    void bitFlipsAndNoise() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(120), () -> {
            int read = 0;
            int refused = 0;
            for (byte[] seed : seeds()) {
                for (int bit = 0 ; bit < seed.length * 8 ; bit++) {
                    byte[] flipped = seed.clone();
                    flipped[bit / 8] ^= (byte) (1 << (bit % 8));
                    if (survives(flipped, "bit " + bit + " flipped")) {
                        read++;
                    } else {
                        refused++;
                    }
                }
            }
            // Both happen: a flip inside a byte string changes nothing a reader
            // can see, and a flip in a head usually breaks the structure.
            assertTrue(read > 1000 && refused > 1000, read + " read, " + refused + " refused");
            // Noise, from a fixed seed so a failure can be run again. Half of it
            // starts with a head that announces a container, where the damage is.
            Random random = new Random(20261006L);
            int[] openers = {0x5f, 0x7f, 0x9f, 0xbf, 0x9b, 0xbb, 0x5b, 0x98, 0xb8, 0xa5};
            for (int iter = 0 ; iter < 200000 ; iter++) {
                byte[] noise = new byte[1 + random.nextInt(48)];
                random.nextBytes(noise);
                if ((iter & 1) == 0) {
                    noise[0] = (byte) openers[random.nextInt(openers.length)];
                }
                survives(noise, "noise " + iter);
            }
            // The same, as the tail of a real attestation object's head.
            byte[] head = W3cTestVectors.hex("a363666d74646e6f6e656761747453746d74a068617574684461746158a4");
            for (int iter = 0 ; iter < 20000 ; iter++) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                out.write(head, 0, head.length);
                byte[] noise = new byte[random.nextInt(200)];
                random.nextBytes(noise);
                out.write(noise, 0, noise.length);
                survives(out.toByteArray(), "attestation noise " + iter);
            }
        });
    }
}
