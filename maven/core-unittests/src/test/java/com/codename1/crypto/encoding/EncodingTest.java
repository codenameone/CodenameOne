package com.codename1.crypto.encoding;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/// Hex, Base58, Base58Check, Bech32/Bech32m and RLP against published vectors.
class EncodingTest {

    private static byte[] ascii(String s) {
        try {
            return s.getBytes("US-ASCII");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    // ---- Hex ----

    @Test
    void hex_roundTripAndPrefix() {
        byte[] b = {0, 1, (byte) 0xab, (byte) 0xff};
        assertEquals("0001abff", Hex.encode(b));
        assertArrayEquals(b, Hex.decode("0001abff"));
        assertArrayEquals(b, Hex.decode("0x0001ABFF"));
        assertArrayEquals(b, Hex.decode("0X0001aBfF"));
        assertEquals(0, Hex.decode("").length);
        assertEquals(0, Hex.decode("0x").length);
    }

    @Test
    void hex_rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> Hex.decode("abc"));
        assertThrows(IllegalArgumentException.class, () -> Hex.decode("0xabc"));
        assertThrows(IllegalArgumentException.class, () -> Hex.decode("zz"));
    }

    // ---- Base58 ----

    @Test
    void base58_vectors() {
        assertEquals("", Base58.encode(new byte[0]));
        assertEquals("2g", Base58.encode(Hex.decode("61")));
        assertEquals("ZiCa", Base58.encode(ascii("abc")));
        assertEquals("2NEpo7TZRRrLZSi2U", Base58.encode(ascii("Hello World!")));
        assertEquals("USm3fpXnKG5EUBx2ndxBDMPVciP5hGey2Jh4NDv6gmeo1LkMeiKrLJUUBk6Z",
                Base58.encode(ascii("The quick brown fox jumps over the lazy dog.")));
        assertEquals("11233QC4", Base58.encode(Hex.decode("0000287fb4cd")));
        assertEquals("1111", Base58.encode(new byte[4]));
        assertArrayEquals(new byte[4], Base58.decode("1111"));
        assertArrayEquals(Hex.decode("0000287fb4cd"), Base58.decode("11233QC4"));
        assertEquals(0, Base58.decode("").length);
    }

    @Test
    void base58_roundTripRandom() {
        Random r = new Random(42);
        for (int i = 0; i < 300; i++) {
            byte[] b = new byte[r.nextInt(80)];
            r.nextBytes(b);
            if (i % 3 == 0 && b.length > 2) {
                b[0] = 0;
                b[1] = 0;
            }
            assertArrayEquals(b, Base58.decode(Base58.encode(b)), "len " + b.length);
        }
    }

    @Test
    void base58_rejectsBadCharacters() {
        assertThrows(IllegalArgumentException.class, () -> Base58.decode("0OIl"));
        assertThrows(IllegalArgumentException.class, () -> Base58.decode("ab\u00e9"));
    }

    // ---- Base58Check ----

    @Test
    void base58check_knownAddresses() {
        String genesis = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa";
        byte[] payload = Base58Check.decode(genesis);
        assertEquals("0062e907b15cbf27d5425399ebf6f0fb50ebb88f18", Hex.encode(payload));
        assertEquals(genesis, Base58Check.encode(payload));

        String other = "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2";
        assertEquals("0077bff20c60e522dfaa3350c39b030a5d004e839a", Hex.encode(Base58Check.decode(other)));
        assertEquals(other, Base58Check.encode(Hex.decode("0077bff20c60e522dfaa3350c39b030a5d004e839a")));
    }

    @Test
    void base58check_rejectsBadChecksum() {
        assertThrows(IllegalArgumentException.class,
                () -> Base58Check.decode("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNb"));
        assertThrows(IllegalArgumentException.class, () -> Base58Check.decode("11"));
    }

    // ---- Bech32 / Bech32m ----

    @Test
    void bech32_validStrings() {
        String[] valid = {
            "A12UEL5L",
            "a12uel5l",
            "an83characterlonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio1tt5tgs",
            "abcdef1qpzry9x8gf2tvdw0s3jn54khce6mua7lmqqqxw",
            "split1checkupstagehandshakeupstreamerranterredcaperred2y9e3w",
        };
        for (String s : valid) {
            Bech32.Decoded d = Bech32.decode(s);
            assertEquals(Bech32.BECH32, d.getVariant(), s);
            assertEquals(s.toLowerCase(java.util.Locale.ROOT),
                    Bech32.encode(d.getHrp(), d.getData(), Bech32.BECH32), s);
        }
        String longest = Bech32.encode("1", new byte[82], Bech32.BECH32);
        assertEquals(90, longest.length());
        assertTrue(longest.endsWith("c8247j"));
        assertEquals(Bech32.BECH32, Bech32.decode(longest).getVariant());
        assertThrows(IllegalArgumentException.class, () -> Bech32.encode("1", new byte[83], Bech32.BECH32));
        assertEquals("a", Bech32.decode("A12UEL5L").getHrp());
        assertEquals(0, Bech32.decode("A12UEL5L").getData().length);
    }

    @Test
    void bech32m_validStrings() {
        String[] valid = {
            "A1LQFN3A",
            "a1lqfn3a",
            "an83characterlonghumanreadablepartthatcontainsthetheexcludedcharactersbioandnumber11sg7hg6",
            "abcdef1l7aum6echk45nj3s0wdvt2fg8x9yrzpqzd3ryx",
            "11llllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll",
            "split1checkupstagehandshakeupstreamerranterredcaperredlc445v",
            "?1v759aa",
        };
        for (String s : valid) {
            if (s.length() > 90) {
                continue;
            }
            Bech32.Decoded d = Bech32.decode(s);
            assertEquals(Bech32.BECH32M, d.getVariant(), s);
            assertEquals(s.toLowerCase(java.util.Locale.ROOT),
                    Bech32.encode(d.getHrp(), d.getData(), Bech32.BECH32M), s);
        }
    }

    @Test
    void bech32_invalidStrings() {
        String[] invalid = {
            "\u00201nwldj5",
            "an84characterslonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio1569pvx",
            "pzry9x0s0muk",
            "1pzry9x0s0muk",
            "x1b4n0q5v",
            "li1dgmt3",
            "A1G7SGD8",
            "10a06t8",
            "1qzzfhee",
            "A12uEL5L",
            "a1lqfn3A",
            "",
        };
        for (final String s : invalid) {
            assertThrows(IllegalArgumentException.class, () -> Bech32.decode(s), s);
        }
        // a Bech32m checksum on the original constant and vice versa decode as the other variant
        assertEquals(Bech32.BECH32M, Bech32.decode("a1lqfn3a").getVariant());
    }

    @Test
    void bech32_convertBits() {
        byte[] ff = {(byte) 0xff};
        assertArrayEquals(new byte[] {31, 28}, Bech32.convertBits(ff, 8, 5, true));
        assertArrayEquals(ff, Bech32.convertBits(new byte[] {31, 28}, 5, 8, false));
        assertThrows(IllegalArgumentException.class,
                () -> Bech32.convertBits(new byte[] {31, 29}, 5, 8, false));
        assertThrows(IllegalArgumentException.class,
                () -> Bech32.convertBits(new byte[] {32}, 5, 8, true));
        Random r = new Random(7);
        for (int i = 0; i < 100; i++) {
            byte[] b = new byte[r.nextInt(40)];
            r.nextBytes(b);
            assertArrayEquals(b, Bech32.convertBits(Bech32.convertBits(b, 8, 5, true), 5, 8, false));
        }
    }

    @Test
    void segwit_validAddresses() {
        check("bc", "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4", 0,
                "751e76e8199196d454941c45d1b3a323f1433bd6");
        check("tb", "tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7", 0,
                "1863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262");
        check("bc", "bc1pw508d6qejxtdg4y5r3zarvary0c5xw7kw508d6qejxtdg4y5r3zarvary0c5xw7kt5nd6y", 1,
                "751e76e8199196d454941c45d1b3a323f1433bd6751e76e8199196d454941c45d1b3a323f1433bd6");
        check("bc", "BC1SW50QGDZ25J", 16, "751e");
        check("bc", "bc1zw508d6qejxtdg4y5r3zarvaryvaxxpcs", 2, "751e76e8199196d454941c45d1b3a323");
        check("bc", "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0", 1,
                "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798");
    }

    private static void check(String hrp, String addr, int version, String programHex) {
        Bech32.SegwitAddress a = Bech32.decodeSegwitAddress(hrp, addr);
        assertEquals(version, a.getVersion(), addr);
        assertEquals(programHex, Hex.encode(a.getProgram()), addr);
        assertEquals(addr.toLowerCase(java.util.Locale.ROOT),
                Bech32.encodeSegwitAddress(hrp, version, Hex.decode(programHex)), addr);
    }

    @Test
    void segwit_invalidAddresses() {
        String[][] invalid = {
            {"bc", "tc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vq5zuyut"},
            {"bc", "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqh2y7hd"},
            {"tb", "tb1z0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqglt7rf"},
            {"bc", "BC1S0XLXVLHEMJA6C4DQV22UAPCTQUPFHLXM9H8Z3K2E72Q4K9HCZ7VQ54WELL"},
            {"bc", "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kemeawh"},
            {"tb", "tb1q0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vq24jc47"},
            {"bc", "bc1p38j9r5y49hruaue7wxjce0updqjuyyx0kh56v8s25huc6995vvpql3jow4"},
            {"bc", "BC130XLXVLHEMJA6C4DQV22UAPCTQUPFHLXM9H8Z3K2E72Q4K9HCZ7VQ7ZWS8R"},
            {"bc", "bc1pw5dgrnzv"},
            {"bc", "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7v8n0nx0muaewav253zgeav"},
            {"bc", "BC1QR508D6QEJXTDG4Y5R3ZARVARYV98GJ9P"},
            {"tb", "tb1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vq47Zagq"},
            {"bc", "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7v07qwwzcrf"},
            {"tb", "tb1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vpggkg4j"},
            {"bc", "bc1gmk9yu"},
        };
        for (final String[] c : invalid) {
            assertThrows(IllegalArgumentException.class,
                    () -> Bech32.decodeSegwitAddress(c[0], c[1]), c[1]);
        }
        assertThrows(IllegalArgumentException.class,
                () -> Bech32.decodeSegwitAddress("tb", "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4"));
    }

    @Test
    void segwit_encodeRejectsBadPrograms() {
        assertThrows(IllegalArgumentException.class, () -> Bech32.encodeSegwitAddress("bc", 0, new byte[16]));
        assertThrows(IllegalArgumentException.class, () -> Bech32.encodeSegwitAddress("bc", 1, new byte[1]));
        assertThrows(IllegalArgumentException.class, () -> Bech32.encodeSegwitAddress("bc", 1, new byte[41]));
        assertThrows(IllegalArgumentException.class, () -> Bech32.encodeSegwitAddress("bc", 17, new byte[20]));
    }

    // ---- RLP ----

    private static String rlpHex(Object o) {
        return Hex.encode(Rlp.encode(o));
    }

    @Test
    void rlp_wikiExamples() {
        assertEquals("83646f67", rlpHex(ascii("dog")));
        assertEquals("c88363617483646f67", rlpHex(Arrays.asList((Object) ascii("cat"), ascii("dog"))));
        assertEquals("80", rlpHex(new byte[0]));
        assertEquals("c0", rlpHex(new ArrayList<Object>()));
        assertEquals("80", Hex.encode(Rlp.encodeLong(0)));
        assertEquals("0f", Hex.encode(Rlp.encodeLong(15)));
        assertEquals("820400", Hex.encode(Rlp.encodeLong(1024)));
        assertEquals("820400", Hex.encode(Rlp.encodeBigInteger(BigInteger.valueOf(1024))));
        assertEquals("80", Hex.encode(Rlp.encodeBigInteger(BigInteger.ZERO)));
        assertEquals("8180", Hex.encode(Rlp.encodeLong(128)));
        assertEquals("88ffffffffffffffff", Hex.encode(Rlp.encodeBigInteger(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE))));
        List<Object> empty = new ArrayList<Object>();
        List<Object> one = new ArrayList<Object>();
        one.add(empty);
        List<Object> three = new ArrayList<Object>();
        three.add(empty);
        three.add(one);
        List<Object> root = new ArrayList<Object>();
        root.add(empty);
        root.add(one);
        root.add(three);
        assertEquals("c7c0c1c0c3c0c1c0", rlpHex(root));
    }

    @Test
    void rlp_longStringAndList() {
        String lorem = "Lorem ipsum dolor sit amet, consectetur adipisicing elit";
        assertEquals(56, lorem.length());
        byte[] enc = Rlp.encode(ascii(lorem));
        assertEquals("b838", Hex.encode(enc).substring(0, 4));
        assertEquals(58, enc.length);
        assertArrayEquals(ascii(lorem), (byte[]) Rlp.decode(enc));

        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < 30; i++) {
            list.add(ascii("ab"));
        }
        byte[] le = Rlp.encode(list);
        assertEquals("f85a", Hex.encode(le).substring(0, 4));
        Object back = Rlp.decode(le);
        assertEquals(30, ((List) back).size());
    }

    @Test
    void rlp_decodeRoundTrip() {
        List<Object> inner = new ArrayList<Object>();
        inner.add(ascii("cat"));
        inner.add(new byte[0]);
        List<Object> outer = new ArrayList<Object>();
        outer.add(inner);
        outer.add(new byte[] {0x7f});
        outer.add(new byte[] {(byte) 0x80});
        byte[] enc = Rlp.encode(outer);
        Object d = Rlp.decode(enc);
        assertArrayEquals(enc, Rlp.encode(d));
        List l = (List) d;
        assertEquals(3, l.size());
        assertArrayEquals(ascii("cat"), (byte[]) ((List) l.get(0)).get(0));
        assertArrayEquals(new byte[] {0x7f}, (byte[]) l.get(1));
    }

    @Test
    void rlp_rejectsNonCanonical() {
        String[] bad = {
            "8100",            // single byte below 0x80 as string
            "817f",
            "b80100",          // long form for short string
            "b900010000",      // leading zero length
            "f80100",          // long list with short length
            "83646f",          // truncated
            "83646f6700",      // trailing byte
            "c88363617483646f", // list payload truncated
            "",                // empty input
            "b8",              // length missing
        };
        for (final String h : bad) {
            assertThrows(IllegalArgumentException.class, () -> Rlp.decode(Hex.decode(h)), h);
        }
    }

    @Test
    void rlp_rejectsUnsupportedItems() {
        assertThrows(IllegalArgumentException.class, () -> Rlp.encode("string"));
        assertThrows(IllegalArgumentException.class, () -> Rlp.encodeLong(-1));
        assertThrows(IllegalArgumentException.class, () -> Rlp.encodeBigInteger(BigInteger.valueOf(-1)));
    }
}
