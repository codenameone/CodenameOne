/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crypto.evm;

import com.codename1.crypto.UInt256;
import com.codename1.io.JSONParser;
import com.codename1.security.Hash;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// EVM vectors from the specifications: EIP-55, EIP-155, EIP-712 and the Solidity ABI
/// documentation, plus Bastion's recorded EIP-1559 transfer.
class EvmTest {
    private static final String KEY = "4c0883a69102937d6231471b5dbb6204fe5129617082792ae468d01a3f362318";

    @Test
    void eip55() {
        String[] addresses = {
            "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed",
            "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
            "0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB",
            "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
        };
        for (String a : addresses) {
            assertEquals(a, EvmAddress.parse(a.toLowerCase()).toChecksumString());
            assertEquals(a, EvmAddress.parse(a).toString());
        }
        assertFalse(EvmAddress.isValid("0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAeD"));
        assertTrue(EvmAddress.isValid("0x5AAEB6053F3E94C9B9A09F33669435E7EF1BEAED"));
        assertFalse(EvmAddress.isValid("0x5aaeb6053f3e94c9b9a09f33669435e7ef1beae"));
        assertFalse(EvmAddress.isValid("0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaeg"));
        assertEquals("0x2c7536e3605d9c16a7a3d7b1898e529396a65c23",
                EvmAddress.fromPrivateKey(Hash.fromHex(KEY)).toLowerCaseString());
    }

    @Test
    void contractAddresses() {
        // the first contract an account deploys, as every explorer shows it
        EvmAddress deployer = EvmAddress.parse("0x6ac7ea33f8831ea9dcc53393aaa88b25a785dbf0");
        assertEquals("0xcd234a471b72ba2f1ccf0a70fcaba648a5eecd8d",
                EvmAddress.contractAddress(deployer, 0).toLowerCaseString());
        assertEquals("0x343c43a37d37dff08ae8c4a11544c718abb4fcf8",
                EvmAddress.contractAddress(deployer, 1).toLowerCaseString());
        // EIP-1014 example 0
        assertEquals("0x4D1A2e2bB4F88F0250f26Ffff098B0b30B26BF38", EvmAddress.create2Address(
                EvmAddress.parse("0x0000000000000000000000000000000000000000"), new byte[32],
                Hash.keccak256(Hash.fromHex("00"))).toChecksumString());
    }

    @Test
    void eip155Example() {
        EvmTransaction tx = EvmTransaction.legacy(1, 9, UInt256.parseUnits("20", 9), 21000,
                EvmAddress.parse("0x3535353535353535353535353535353535353535"), UInt256.parseUnits("1", 18), null);
        assertEquals("daf5a779ae972f972197303d7b574746c7ef83eadac0f2791ad23db92e4c8e53", Hash.toHex(tx.signingHash()));
        byte[] key = Hash.fromHex("4646464646464646464646464646464646464646464646464646464646464646");
        EvmTransaction signed = tx.sign(key);
        String raw = "f86c098504a817c800825208943535353535353535353535353535353535353535880de0b6b3a7640000"
                + "8025a028ef61340bd939bc2195fe537567866003e1a15d3c71ff63e1590620aa636276a067cbe9d8997f76"
                + "1aecb703304b3800ccf555c9f3dc64214b297fb1966a3b6d83";
        assertEquals(raw, Hash.toHex(signed.encode()));
        EvmTransaction back = EvmTransaction.decode(Hash.fromHex(raw));
        assertEquals(1, back.getChainId());
        assertEquals(9, back.getNonce());
        assertEquals(EvmAddress.fromPrivateKey(key), back.sender());
        assertEquals(raw, Hash.toHex(back.encode()));
    }

    @Test
    void eip1559Transfer() {
        EvmTransaction tx = EvmTransaction.eip1559(1337, 3, UInt256.parseUnits("2", 9), UInt256.parseUnits("100", 9),
                21000, EvmAddress.parse("0x3535353535353535353535353535353535353535"),
                UInt256.parseUnits("1.5", 18), null);
        assertEquals("0b1bd411c9eb4da68dee78e282b1a66831167368952179f857f69a680f642cba", Hash.toHex(tx.signingHash()));
        EvmTransaction signed = tx.sign(Hash.fromHex(KEY));
        String raw = "02f87582053903847735940085174876e8008252089435353535353535353535353535353535"
                + "353535358814d1120d7b16000080c001a0c34cfde89d316325c60fc29e10403b382c28543668c155"
                + "2957112c5496125a30a0453e3bcbf326db30b59ea3015c8faf5c190800bddf6f53c83d1967df5c06"
                + "4dbc";
        assertEquals(raw, Hash.toHex(signed.encode()));
        assertEquals("f0a0c01af56fcf57face413edf956894ebd29855b1143abde08823d6b4827b8e", Hash.toHex(signed.hash()));
        EvmTransaction back = EvmTransaction.decodeHex("0x" + raw);
        assertEquals(EvmTransaction.DYNAMIC_FEE, back.getType());
        assertEquals("0x2c7536e3605d9c16a7a3d7b1898e529396a65c23", back.sender().toLowerCaseString());
        assertEquals(UInt256.parseUnits("1.5", 18), back.getValue());
        // unsigned payloads round trip too
        assertArrayEquals(tx.signingHash(), EvmTransaction.decode(unsignedPayload(tx)).signingHash());
    }

    @Test
    void accessListTransactionsRoundTrip() {
        List<EvmTransaction.AccessEntry> list = new ArrayList<EvmTransaction.AccessEntry>();
        list.add(new EvmTransaction.AccessEntry(EvmAddress.parse("0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae"),
                Collections.singletonList(new byte[32])));
        for (EvmTransaction tx : new EvmTransaction[] {
            EvmTransaction.accessList(5, 0, UInt256.valueOf(1000), 30000, null, UInt256.ZERO, Hash.fromHex("6000"), list),
            EvmTransaction.eip1559(5, 7, UInt256.ONE, UInt256.valueOf(99), 30000,
                    EvmAddress.parse("0x3535353535353535353535353535353535353535"), UInt256.ONE, null).withAccessList(list),
            EvmTransaction.legacy(0, 1, UInt256.ONE, 21000, null, UInt256.ZERO, null),
        }) {
            EvmTransaction signed = tx.sign(Hash.fromHex(KEY));
            EvmTransaction back = EvmTransaction.decode(signed.encode());
            assertArrayEquals(signed.encode(), back.encode());
            assertEquals(EvmAddress.fromPrivateKey(Hash.fromHex(KEY)), back.sender());
            assertEquals(tx.getAccessList().size(), back.getAccessList().size());
        }
        assertThrows(IllegalArgumentException.class, () -> EvmTransaction.decode(Hash.fromHex("03c0")));
        assertThrows(IllegalStateException.class, () -> EvmTransaction.legacy(1, 0, UInt256.ONE, 1, null, null, null).encode());
    }

    private static byte[] unsignedPayload(EvmTransaction tx) {
        // what eth_signTransaction-style tooling hands a signer: the typed envelope without v, r, s
        java.util.List<Object> f = new ArrayList<Object>();
        f.add(com.codename1.crypto.encoding.Rlp.longBytes(tx.getChainId()));
        f.add(com.codename1.crypto.encoding.Rlp.longBytes(tx.getNonce()));
        f.add(tx.getMaxPriorityFeePerGas().toMinimalBytes());
        f.add(tx.getMaxFeePerGas().toMinimalBytes());
        f.add(com.codename1.crypto.encoding.Rlp.longBytes(tx.getGasLimit()));
        f.add(tx.getTo().toBytes());
        f.add(tx.getValue().toMinimalBytes());
        f.add(tx.getData());
        f.add(new ArrayList<Object>());
        byte[] body = com.codename1.crypto.encoding.Rlp.encode(f);
        byte[] out = new byte[body.length + 1];
        out[0] = 2;
        System.arraycopy(body, 0, out, 1, body.length);
        return out;
    }

    @Test
    void personalSign() {
        byte[] key = Hash.fromHex(KEY);
        // the hash every wallet computes for "hello"
        assertEquals("50b2c43fd39106bafbba0da34fc430e1f91e3c96ea2acee2bc34119f92b37750",
                Hash.toHex(PersonalSign.hash("hello")));
        byte[] sig = PersonalSign.sign("hello", key);
        assertTrue((sig[64] & 0xff) == 27 || (sig[64] & 0xff) == 28);
        assertTrue(PersonalSign.verify("hello", sig, EvmAddress.fromPrivateKey(key)));
        assertFalse(PersonalSign.verify("hellO", sig, EvmAddress.fromPrivateKey(key)));
    }

    private static final String MAIL = "{\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},"
            + "{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},"
            + "{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},"
            + "{\"name\":\"wallet\",\"type\":\"address\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},"
            + "{\"name\":\"to\",\"type\":\"Person\"},{\"name\":\"contents\",\"type\":\"string\"}]},"
            + "\"primaryType\":\"Mail\",\"domain\":{\"name\":\"Ether Mail\",\"version\":\"1\",\"chainId\":1,"
            + "\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\"},\"message\":{\"from\":{\"name\":\"Cow\","
            + "\"wallet\":\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\"},\"to\":{\"name\":\"Bob\","
            + "\"wallet\":\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\"},\"contents\":\"Hello, Bob!\"}}";

    @Test
    void eip712Mail() {
        TypedData doc = TypedData.fromJson(MAIL);
        assertEquals("Mail(Person from,Person to,string contents)Person(string name,address wallet)",
                doc.encodeType("Mail"));
        assertEquals("f2cee375fa42b42143804025fc449deafd50cc031ca257e0b194a650a912090f",
                Hash.toHex(doc.domainSeparator()));
        assertEquals("c52c0ee5d84264471806290a3f2c4cecfc5490626bf912d01f240d7a274b371e", Hash.toHex(doc.messageHash()));
        assertEquals("be609aee343fb3c4b28e1df9e632fca64fcfaede20f02e86244efddf30957bd2", Hash.toHex(doc.hash()));
        byte[] cow = Hash.keccak256("cow".getBytes());
        byte[] sig = doc.sign(cow);
        assertEquals("4355c47d63924e8a72e509b65029052eb6c299d53a04e167c5775fd466751c9d"
                + "07299936d304c153f6443dfa05f40ff007d72911b6f72307f996231605b915621c", Hash.toHex(sig));
        assertEquals(EvmAddress.parse("0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826"), doc.recover(sig));
    }

    @Test
    void solidityDocumentationExamples() {
        assertEquals("cdcd77c0"
                + "0000000000000000000000000000000000000000000000000000000000000045"
                + "0000000000000000000000000000000000000000000000000000000000000001",
                Hash.toHex(Abi.encodeCall("baz(uint32,bool)", 69, true)));
        String sam = "a5643bf2"
                + "0000000000000000000000000000000000000000000000000000000000000060"
                + "0000000000000000000000000000000000000000000000000000000000000001"
                + "00000000000000000000000000000000000000000000000000000000000000a0"
                + "0000000000000000000000000000000000000000000000000000000000000004"
                + "6461766500000000000000000000000000000000000000000000000000000000"
                + "0000000000000000000000000000000000000000000000000000000000000003"
                + "0000000000000000000000000000000000000000000000000000000000000001"
                + "0000000000000000000000000000000000000000000000000000000000000002"
                + "0000000000000000000000000000000000000000000000000000000000000003";
        assertEquals(sam, Hash.toHex(Abi.encodeCall("sam(bytes,bool,uint[])", "dave".getBytes(), true,
                Arrays.asList(1, 2, 3))));
        List<Object> back = Abi.decode("(bytes,bool,uint256[])", Hash.fromHex(sam.substring(8)));
        assertEquals("dave", new String((byte[]) back.get(0)));
        assertEquals(Boolean.TRUE, back.get(1));
        assertEquals(Arrays.asList(UInt256.ONE, UInt256.valueOf(2), UInt256.valueOf(3)), back.get(2));
        String f = "8be65246"
                + "0000000000000000000000000000000000000000000000000000000000000123"
                + "0000000000000000000000000000000000000000000000000000000000000080"
                + "3132333435363738393000000000000000000000000000000000000000000000"
                + "00000000000000000000000000000000000000000000000000000000000000e0"
                + "0000000000000000000000000000000000000000000000000000000000000002"
                + "0000000000000000000000000000000000000000000000000000000000000456"
                + "0000000000000000000000000000000000000000000000000000000000000789"
                + "000000000000000000000000000000000000000000000000000000000000000d"
                + "48656c6c6f2c20776f726c642100000000000000000000000000000000000000";
        assertEquals(f, Hash.toHex(Abi.encodeCall("f(uint256,uint32[],bytes10,bytes)", "0x123",
                Arrays.asList(0x456, 0x789), "1234567890".getBytes(), "Hello, world!".getBytes())));
        String g = "2289b18c"
                + "0000000000000000000000000000000000000000000000000000000000000040"
                + "0000000000000000000000000000000000000000000000000000000000000140"
                + "0000000000000000000000000000000000000000000000000000000000000002"
                + "0000000000000000000000000000000000000000000000000000000000000040"
                + "00000000000000000000000000000000000000000000000000000000000000a0"
                + "0000000000000000000000000000000000000000000000000000000000000002"
                + "0000000000000000000000000000000000000000000000000000000000000001"
                + "0000000000000000000000000000000000000000000000000000000000000002"
                + "0000000000000000000000000000000000000000000000000000000000000001"
                + "0000000000000000000000000000000000000000000000000000000000000003"
                + "0000000000000000000000000000000000000000000000000000000000000003"
                + "0000000000000000000000000000000000000000000000000000000000000060"
                + "00000000000000000000000000000000000000000000000000000000000000a0"
                + "00000000000000000000000000000000000000000000000000000000000000e0"
                + "0000000000000000000000000000000000000000000000000000000000000003"
                + "6f6e650000000000000000000000000000000000000000000000000000000000"
                + "0000000000000000000000000000000000000000000000000000000000000003"
                + "74776f0000000000000000000000000000000000000000000000000000000000"
                + "0000000000000000000000000000000000000000000000000000000000000005"
                + "7468726565000000000000000000000000000000000000000000000000000000";
        Object nested = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3));
        assertEquals(g, Hash.toHex(Abi.encodeCall("g(uint256[][],string[])", nested,
                Arrays.asList("one", "two", "three"))));
        List<Object> gb = Abi.decode("(uint256[][],string[])", Hash.fromHex(g.substring(8)));
        assertEquals(Arrays.asList("one", "two", "three"), gb.get(1));
    }

    @Test
    void signedIntegersAndTuples() {
        byte[] enc = Abi.encode("(int8,int256,(uint8,string))", -1L, "-57896044618658097711785492504343953926634992332820282019728792003956564819968",
                Arrays.asList(7, "x"));
        List<Object> back = Abi.decode("(int8,int256,(uint8,string))", enc);
        assertEquals(Long.valueOf(-1), back.get(0));
        assertEquals("-57896044618658097711785492504343953926634992332820282019728792003956564819968", back.get(1));
        assertEquals(Arrays.asList(UInt256.valueOf(7), "x"), back.get(2));
        assertThrows(IllegalArgumentException.class, () -> Abi.encode("(int8)", 128L));
        assertThrows(IllegalArgumentException.class, () -> Abi.encode("(uint8)", 256L));
    }

    @Test
    void erc20() {
        assertEquals("a9059cbb", Hash.toHex(Abi.selector("transfer(address,uint256)")));
        assertEquals(Erc20.TRANSFER_TOPIC, "0x" + Hash.toHex(Abi.eventTopic("Transfer(address,address,uint256)")));
        EvmAddress to = EvmAddress.parse("0x3535353535353535353535353535353535353535");
        byte[] data = Erc20.transfer(to, UInt256.parseUnits("25", 6));
        assertEquals("a9059cbb0000000000000000000000003535353535353535353535353535353535353535"
                + "00000000000000000000000000000000000000000000000000000000017d7840", Hash.toHex(data));
        Erc20.Transfer t = Erc20.decodeTransfer(Arrays.asList(Erc20.TRANSFER_TOPIC,
                "0x000000000000000000000000" + "1111111111111111111111111111111111111111",
                "0x0000000000000000000000003535353535353535353535353535353535353535"),
                UInt256.valueOf(5).toBytes32());
        assertEquals(to, t.getTo());
        assertEquals(UInt256.valueOf(5), t.getAmount());
    }

    @Test
    void uint256() throws Exception {
        assertEquals("1.5", UInt256.parseUnits("1.5", 18).formatUnits(18));
        assertEquals("1500000000000000000", UInt256.parseUnits("1.5", 18).toString());
        assertEquals("0.000001", UInt256.ONE.formatUnits(6));
        assertEquals(UInt256.MAX, UInt256.parse("115792089237316195423570985008687907853269984665640564039457584007913129639935"));
        assertEquals("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", UInt256.MAX.toQuantity());
        assertEquals("0x0", UInt256.ZERO.toQuantity());
        assertThrows(ArithmeticException.class, () -> UInt256.MAX.add(UInt256.ONE));
        assertThrows(NumberFormatException.class, () -> UInt256.parse(
                "115792089237316195423570985008687907853269984665640564039457584007913129639936"));
        assertThrows(NumberFormatException.class, () -> UInt256.parse("-1"));
        assertThrows(NumberFormatException.class, () -> UInt256.parse("0x+1"));
        assertEquals(UInt256.valueOf(26), UInt256.parse("0X1a"));
        assertThrows(ArithmeticException.class, () -> UInt256.MAX.shiftLeft(1));
        assertThrows(ArithmeticException.class, () -> UInt256.tenPow(78));
        assertEquals(32, UInt256.MAX.toMinimalBytes().length);
        assertThrows(ArithmeticException.class, () -> UInt256.ZERO.subtract(UInt256.ONE));
        assertThrows(NumberFormatException.class, () -> UInt256.parseUnits("1.0000001", 6));
        UInt256 a = UInt256.parse("123456789012345678901234567890");
        UInt256 b = UInt256.parse("987654321");
        UInt256[] qr = a.divideAndRemainder(b);
        assertEquals(a, qr[0].multiply(b).add(qr[1]));
        assertEquals(new java.math.BigInteger("123456789012345678901234567890").divide(new java.math.BigInteger("987654321")).toString(),
                qr[0].toString());
        assertEquals(Long.valueOf(5), JSONParser.parseStrict("5", true));
        // past a long the digits are kept exactly, never rounded through a double
        Object max = JSONParser.parseStrict(UInt256.MAX.toString(), true);
        assertEquals(UInt256.MAX.toString(), ((JSONParser.RawJson) max).getJson());
        assertEquals("{\"a\":[1,\"x\",true,null],\"b\":\"0x0102\",\"c\":" + UInt256.MAX + "}",
                JSONParser.toJson(JSONParser.parseStrict(
                "{\"a\":[1,\"x\",true,null],\"b\":\"0x0102\",\"c\":" + UInt256.MAX + "}", true)));
    }
}
