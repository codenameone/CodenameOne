/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.chain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Known answers for everything between a ledger amount and the bytes a node
/// accepts. The transaction, address and derivation vectors were produced by
/// an independent implementation (the eth-account library) and by the
/// published EIP-55 and BIP-44 examples; signing is deterministic (RFC 6979),
/// so a signed transaction must match byte for byte.
class ChainVectorTest {
    private static final String KEY = "4c0883a69102937d6231471b5dbb6204fe5129617082792ae468d01a3f362318";
    /// BIP-39 seed of "abandon abandon ... about", no passphrase.
    private static final String SEED = "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc1"
            + "9a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4";

    @Test
    void rlpMatchesTheSpecificationExamples() {
        assertEquals("83646f67", hex(Rlp.string(ascii("dog"))));
        List<byte[]> catDog = new ArrayList<byte[]>();
        catDog.add(Rlp.string(ascii("cat")));
        catDog.add(Rlp.string(ascii("dog")));
        assertEquals("c88363617483646f67", hex(Rlp.list(catDog)));
        assertEquals("80", hex(Rlp.string(new byte[0])));
        assertEquals("c0", hex(Rlp.list(new ArrayList<byte[]>())));
        assertEquals("80", hex(Rlp.quantity(0)));
        assertEquals("0f", hex(Rlp.quantity(15)));
        assertEquals("820400", hex(Rlp.quantity(1024)));
        String lorem = "Lorem ipsum dolor sit amet, consectetur adipisicing elit";
        assertEquals("b838" + hex(ascii(lorem)), hex(Rlp.string(ascii(lorem))));
    }

    @Test
    void weiAndGweiConvertExactlyAndRoundDown() {
        assertEquals("14d1120d7b160000", hex(Wei.fromGwei(1500000000L)));
        assertEquals(1500000000L, Wei.toGwei(Wei.fromGwei(1500000000L)));
        byte[] dusty = Wei.parseQuantity("0x14d1120d7b160001");
        assertEquals(1500000000L, Wei.toGwei(dusty));
        assertTrue(Wei.hasDust(dusty));
        assertEquals(1500000001L, Chain.ceilGwei(dusty));
        assertEquals("0x0", Wei.quantity(0));
        assertEquals("0x539", Wei.quantity(1337));
        assertEquals(1337L, Wei.parseLong("0x539"));
        assertThrows(ArithmeticException.class, () -> Wei.toGwei(Hex32.decode("ffffffffffffffffffffffffffffffff")));
        assertEquals("0202", hex(Chain.add(Hex32.decode("ff"), Hex32.decode("0103"))));
    }

    @Test
    void addressesFollowEip55() {
        String[] good = {
            "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed",
            "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
            "0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB",
            "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
        };
        for (String a : good) {
            assertEquals(a, EthAddress.checksum(a.toLowerCase()));
            assertTrue(EthAddress.valid(a), a);
        }
        // One character's case flipped: the checksum catches it.
        assertFalse(EthAddress.valid("0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAeD"));
        // No checksum at all is accepted, as wallets do.
        assertTrue(EthAddress.valid("0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed"));
        assertFalse(EthAddress.valid("0x5aaeb6053f3e94c9b9a09f33669435e7ef1beae"));
        assertFalse(EthAddress.valid("5aaeb6053f3e94c9b9a09f33669435e7ef1beaed00"));
        assertFalse(EthAddress.valid("0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaeg"));
    }

    @Test
    void aKeyHasItsKnownAddress() {
        assertEquals("0x2c7536e3605d9c16a7a3d7b1898e529396a65c23",
                EthAddress.of(EthKeys.publicKey(Hex32.decode(KEY))));
    }

    @Test
    void anEip1559TransferSignsToTheKnownBytes() {
        Eip1559Tx tx = new Eip1559Tx(1337, 3, Wei.fromGwei(2), Wei.fromGwei(100), 21000,
                "0x3535353535353535353535353535353535353535", Wei.fromGwei(1500000000L), null);
        assertEquals("0b1bd411c9eb4da68dee78e282b1a66831167368952179f857f69a680f642cba",
                hex(tx.signingHash()));
        byte[] signature = EthKeys.sign(Hex32.decode(KEY), tx.signingHash());
        byte[] raw = tx.raw(signature);
        assertEquals("02f87582053903847735940085174876e8008252089435353535353535353535353535353535"
                + "353535358814d1120d7b16000080c001a0c34cfde89d316325c60fc29e10403b382c28543668c155"
                + "2957112c5496125a30a0453e3bcbf326db30b59ea3015c8faf5c190800bddf6f53c83d1967df5c06"
                + "4dbc", hex(raw));
        assertEquals("0xf0a0c01af56fcf57face413edf956894ebd29855b1143abde08823d6b4827b8e",
                Eip1559Tx.hash(raw));
        // The signature names its signer: what a node checks.
        assertArrayEquals(EthKeys.publicKey(Hex32.decode(KEY)), EthKeys.recover(tx.signingHash(), signature));
    }

    @Test
    void depositAndHotKeysDeriveAsStandardWalletsDo() {
        byte[] seed = Hex32.decode(SEED);
        Bip32 first = Bip32.ethereum(seed, 0);
        assertEquals("1ab42cc412b618bdea3a599e3c9bae199ebf030895b039e9db1e30dafb12b727",
                hex(first.privateKey()));
        assertEquals("0x9858effd232b4033e47d90003d41ec34ecaeda94", first.address());
        SeedSigner signer = new SeedSigner(seed);
        assertEquals("0x593814d3309e2df31d112824f0bb5aa7cb0d7d47", signer.depositAddress(7));
        assertEquals("0x78839f6054d7ed13918bae0473ba31b1ca9d7265", signer.hotAddress());
    }

    @Test
    void aTransactionRefusesBadFields() {
        assertThrows(IllegalArgumentException.class, () -> new Eip1559Tx(0, 0, new byte[0],
                new byte[0], 21000, "0x3535353535353535353535353535353535353535", new byte[0], null));
        assertThrows(IllegalArgumentException.class, () -> new Eip1559Tx(1, 0, new byte[0],
                new byte[0], 21000, "0x35", new byte[0], null));
        Eip1559Tx tx = new Eip1559Tx(1, 0, new byte[0], new byte[0], 21000,
                "0x3535353535353535353535353535353535353535", new byte[0], null);
        byte[] bad = new byte[65];
        bad[64] = 2;
        assertThrows(IllegalArgumentException.class, () -> tx.raw(bad));
        assertThrows(IllegalArgumentException.class, () -> new EthRpc("http://node.example:8545"));
    }

    private static byte[] ascii(String text) {
        byte[] out = new byte[text.length()];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) text.charAt(iter);
        }
        return out;
    }

    private static String hex(byte[] bytes) {
        return Hex32.encode(bytes);
    }
}
