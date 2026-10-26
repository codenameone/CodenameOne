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
import com.codename1.crypto.encoding.Hex;
import com.codename1.crypto.encoding.Rlp;
import com.codename1.security.Hash;
import com.codename1.security.Secp256k1;
import java.util.ArrayList;
import java.util.List;

/// An EVM transaction in any of the three envelopes nodes accept: legacy (with EIP-155
/// replay protection), EIP-2930 access-list (type 1) and EIP-1559 dynamic-fee (type 2).
///
/// Built, signed and serialized locally, so a node receives finished bytes and never a key:
///
/// ```java
/// EvmTransaction tx = EvmTransaction.eip1559(1, nonce, tip, maxFee, 21000,
///         EvmAddress.parse("0x..."), UInt256.parseUnits("0.01", 18), null);
/// byte[] raw = tx.sign(privateKey).encode();   // eth_sendRawTransaction
/// ```
///
/// Instances are immutable; [#sign] and [#withSignature] answer a signed copy, and
/// [#decode] reads any signed or unsigned envelope back, recovering its [#sender].
@com.codename1.impl.SharedWithBackend
public final class EvmTransaction {
    /// The legacy envelope; signed under EIP-155 when the chain id is not zero.
    public static final int LEGACY = 0;
    /// The EIP-2930 envelope, a legacy gas price plus an access list.
    public static final int ACCESS_LIST = 1;
    /// The EIP-1559 envelope with a priority fee and a fee cap.
    public static final int DYNAMIC_FEE = 2;

    private final int type;
    private final long chainId;
    private final long nonce;
    private final UInt256 gasPrice;
    private final UInt256 maxPriorityFeePerGas;
    private final UInt256 maxFeePerGas;
    private final long gasLimit;
    private final EvmAddress to;
    private final UInt256 value;
    private final byte[] data;
    private final List<AccessEntry> accessList;
    private final int recoveryId;
    private final byte[] r;
    private final byte[] s;

    /// One access-list entry: an address and the storage slots the transaction will touch.
    public static final class AccessEntry {
        private final EvmAddress address;
        private final List<byte[]> storageKeys;

        /// An entry for `address` with 32-byte `storageKeys` (may be empty).
        public AccessEntry(EvmAddress address, List<byte[]> storageKeys) {
            this.address = address;
            this.storageKeys = new ArrayList<byte[]>();
            if (storageKeys != null) {
                for (int iter = 0; iter < storageKeys.size(); iter++) {
                    byte[] key = storageKeys.get(iter);
                    if (key.length != 32) {
                        throw new IllegalArgumentException("a storage key is 32 bytes");
                    }
                    this.storageKeys.add(copy(key));
                }
            }
        }

        /// The address.
        public EvmAddress getAddress() {
            return address;
        }

        /// The number of storage keys.
        public int getStorageKeyCount() {
            return storageKeys.size();
        }

        /// A copy of a storage key.
        public byte[] getStorageKey(int index) {
            return copy(storageKeys.get(index));
        }

        Object rlp() {
            List<Object> keys = new ArrayList<Object>();
            for (int iter = 0; iter < storageKeys.size(); iter++) {
                keys.add(storageKeys.get(iter));
            }
            List<Object> out = new ArrayList<Object>();
            out.add(address.toBytes());
            out.add(keys);
            return out;
        }
    }

    private EvmTransaction(int type, long chainId, long nonce, UInt256 gasPrice, UInt256 maxPriorityFeePerGas,
            UInt256 maxFeePerGas, long gasLimit, EvmAddress to, UInt256 value, byte[] data,
            List<AccessEntry> accessList, int recoveryId, byte[] r, byte[] s) {
        if (chainId < 0 || nonce < 0 || gasLimit < 0) {
            throw new IllegalArgumentException("negative transaction field");
        }
        if (type != LEGACY && chainId == 0) {
            throw new IllegalArgumentException("a typed transaction needs a chain id");
        }
        this.type = type;
        this.chainId = chainId;
        this.nonce = nonce;
        this.gasPrice = gasPrice;
        this.maxPriorityFeePerGas = maxPriorityFeePerGas;
        this.maxFeePerGas = maxFeePerGas;
        this.gasLimit = gasLimit;
        this.to = to;
        this.value = value == null ? UInt256.ZERO : value;
        this.data = data == null ? new byte[0] : copy(data);
        this.accessList = accessList == null ? new ArrayList<AccessEntry>() : new ArrayList<AccessEntry>(accessList);
        this.recoveryId = recoveryId;
        this.r = r;
        this.s = s;
    }

    /// A legacy transaction. A `chainId` of 0 signs without EIP-155 replay protection,
    /// which nodes increasingly refuse; pass the real chain id.
    ///
    /// #### Parameters
    ///
    /// - `to`: the recipient, or `null` to deploy `data` as a contract
    public static EvmTransaction legacy(long chainId, long nonce, UInt256 gasPrice, long gasLimit, EvmAddress to,
            UInt256 value, byte[] data) {
        return new EvmTransaction(LEGACY, chainId, nonce, gasPrice, null, null, gasLimit, to, value, data, null,
                -1, null, null);
    }

    /// An EIP-2930 access-list transaction.
    public static EvmTransaction accessList(long chainId, long nonce, UInt256 gasPrice, long gasLimit,
            EvmAddress to, UInt256 value, byte[] data, List<AccessEntry> accessList) {
        return new EvmTransaction(ACCESS_LIST, chainId, nonce, gasPrice, null, null, gasLimit, to, value, data,
                accessList, -1, null, null);
    }

    /// An EIP-1559 transaction, the default on every chain that has it.
    ///
    /// #### Parameters
    ///
    /// - `maxPriorityFeePerGas`: the tip to the block producer, in wei per gas
    ///
    /// - `maxFeePerGas`: the most the sender pays per gas, base fee included
    ///
    /// - `to`: the recipient, or `null` to deploy `data` as a contract
    public static EvmTransaction eip1559(long chainId, long nonce, UInt256 maxPriorityFeePerGas,
            UInt256 maxFeePerGas, long gasLimit, EvmAddress to, UInt256 value, byte[] data) {
        return new EvmTransaction(DYNAMIC_FEE, chainId, nonce, null, maxPriorityFeePerGas, maxFeePerGas, gasLimit,
                to, value, data, null, -1, null, null);
    }

    /// This transaction with an access list (types 1 and 2 only).
    public EvmTransaction withAccessList(List<AccessEntry> entries) {
        if (type == LEGACY) {
            throw new IllegalStateException("a legacy transaction has no access list");
        }
        return new EvmTransaction(type, chainId, nonce, gasPrice, maxPriorityFeePerGas, maxFeePerGas, gasLimit, to,
                value, data, entries, -1, null, null);
    }

    // ----------------------------------------------------------------

    private List<Object> fields(boolean forSigning) {
        List<Object> f = new ArrayList<Object>();
        if (type != LEGACY) {
            f.add(Rlp.longBytes(chainId));
        }
        f.add(Rlp.longBytes(nonce));
        if (type == DYNAMIC_FEE) {
            f.add(maxPriorityFeePerGas.toMinimalBytes());
            f.add(maxFeePerGas.toMinimalBytes());
        } else {
            f.add(gasPrice.toMinimalBytes());
        }
        f.add(Rlp.longBytes(gasLimit));
        f.add(to == null ? new byte[0] : to.toBytes());
        f.add(value.toMinimalBytes());
        f.add(data);
        if (type != LEGACY) {
            List<Object> list = new ArrayList<Object>();
            for (int iter = 0; iter < accessList.size(); iter++) {
                list.add(accessList.get(iter).rlp());
            }
            f.add(list);
        }
        if (forSigning) {
            if (type == LEGACY && chainId != 0) {
                f.add(Rlp.longBytes(chainId));
                f.add(new byte[0]);
                f.add(new byte[0]);
            }
        } else {
            long v;
            if (type != LEGACY) {
                v = recoveryId;
            } else if (chainId != 0) {
                v = recoveryId + chainId * 2 + 35;
            } else {
                v = recoveryId + 27;
            }
            f.add(Rlp.longBytes(v));
            f.add(strip(r));
            f.add(strip(s));
        }
        return f;
    }

    private byte[] envelope(List<Object> fields) {
        byte[] body = Rlp.encode(fields);
        if (type == LEGACY) {
            return body;
        }
        byte[] out = new byte[body.length + 1];
        out[0] = (byte) type;
        System.arraycopy(body, 0, out, 1, body.length);
        return out;
    }

    /// The 32-byte Keccak hash a signer signs.
    public byte[] signingHash() {
        return Hash.keccak256(envelope(fields(true)));
    }

    /// Signs with a secp256k1 private key and answers the signed transaction.
    public EvmTransaction sign(byte[] privateKey) {
        Secp256k1.EcdsaSignature sig = Secp256k1.sign(signingHash(), privateKey);
        return withSignature(sig.r(), sig.s(), sig.recoveryId());
    }

    /// The signed transaction, given the 65-byte `r || s || recoveryId` signature of
    /// [#signingHash] that an external signer (hardware wallet, HSM) produced.
    public EvmTransaction withSignature(byte[] signature65) {
        if (signature65 == null || signature65.length != 65) {
            throw new IllegalArgumentException("a recoverable signature is 65 bytes");
        }
        byte[] rr = new byte[32];
        byte[] ss = new byte[32];
        System.arraycopy(signature65, 0, rr, 0, 32);
        System.arraycopy(signature65, 32, ss, 0, 32);
        int v = signature65[64] & 0xff;
        return withSignature(rr, ss, v >= 27 ? v - 27 : v);
    }

    private EvmTransaction withSignature(byte[] rr, byte[] ss, int recId) {
        if (recId < 0 || recId > 1) {
            throw new IllegalArgumentException("recovery id must be 0 or 1");
        }
        return new EvmTransaction(type, chainId, nonce, gasPrice, maxPriorityFeePerGas, maxFeePerGas, gasLimit, to,
                value, data, accessList, recId, pad32(rr), pad32(ss));
    }

    /// Tells whether the transaction carries a signature.
    public boolean isSigned() {
        return r != null;
    }

    /// The broadcastable bytes of a signed transaction, for `eth_sendRawTransaction`.
    public byte[] encode() {
        if (!isSigned()) {
            throw new IllegalStateException("sign the transaction first");
        }
        return envelope(fields(false));
    }

    /// [#encode] as `0x` hex.
    public String encodeHex() {
        return "0x" + Hex.encode(encode());
    }

    /// The transaction hash nodes and explorers know a signed transaction by.
    public byte[] hash() {
        return Hash.keccak256(encode());
    }

    /// The address that signed the transaction, recovered from the signature.
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when unsigned
    ///
    /// - `com.codename1.security.CryptoException`: when the signature recovers no key
    public EvmAddress sender() {
        if (!isSigned()) {
            throw new IllegalStateException("unsigned transaction");
        }
        byte[] pub = Secp256k1.recoverPublicKey(signingHash(), new Secp256k1.EcdsaSignature(r, s, recoveryId),
                false);
        return EvmAddress.fromPublicKey(pub);
    }

    /// The most this transaction can cost: `value + gasLimit * (maxFeePerGas or gasPrice)`.
    public UInt256 maxCost() {
        UInt256 price = type == DYNAMIC_FEE ? maxFeePerGas : gasPrice;
        return value.add(price.multiply(gasLimit));
    }

    // ----------------------------------------------------------------

    /// Reads a transaction in any envelope, signed or as the unsigned payload a wallet is
    /// asked to sign.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for malformed or non-canonical bytes, or an envelope
    ///   other than 0, 1 and 2
    public static EvmTransaction decode(byte[] raw) {
        if (raw == null || raw.length == 0) {
            throw new IllegalArgumentException("empty transaction");
        }
        int first = raw[0] & 0xff;
        if (first >= 0xc0) {
            return decodeFields(LEGACY, list(Rlp.decode(raw)));
        }
        if (first != ACCESS_LIST && first != DYNAMIC_FEE) {
            throw new IllegalArgumentException("unsupported transaction type " + first);
        }
        byte[] body = new byte[raw.length - 1];
        System.arraycopy(raw, 1, body, 0, body.length);
        return decodeFields(first, list(Rlp.decode(body)));
    }

    /// [#decode] from `0x` hex.
    public static EvmTransaction decodeHex(String hex) {
        return decode(Hex.decode(hex));
    }

    private static EvmTransaction decodeFields(int type, List<?> f) {
        int base = type == LEGACY ? 6 : (type == ACCESS_LIST ? 8 : 9);
        int n = f.size();
        boolean signed = n == base + 3;
        if (!(signed || n == base)) {
            throw new IllegalArgumentException("wrong field count " + n);
        }
        int at = 0;
        long chainId = 0;
        if (type != LEGACY) {
            chainId = num(f.get(at++));
        }
        long nonce = num(f.get(at++));
        UInt256 gasPrice = null;
        UInt256 tip = null;
        UInt256 cap = null;
        if (type == DYNAMIC_FEE) {
            tip = big(f.get(at++));
            cap = big(f.get(at++));
        } else {
            gasPrice = big(f.get(at++));
        }
        long gas = num(f.get(at++));
        byte[] toBytes = bytes(f.get(at++));
        EvmAddress to;
        if (toBytes.length == 0) {
            to = null;
        } else {
            to = EvmAddress.fromBytes(toBytes);
        }
        UInt256 value = big(f.get(at++));
        byte[] data = bytes(f.get(at++));
        List<AccessEntry> access = null;
        if (type != LEGACY) {
            access = new ArrayList<AccessEntry>();
            List<?> entries = list(f.get(at++));
            for (int iter = 0; iter < entries.size(); iter++) {
                List<?> e = list(entries.get(iter));
                if (e.size() != 2) {
                    throw new IllegalArgumentException("bad access list entry");
                }
                List<?> keys = list(e.get(1));
                List<byte[]> keyList = new ArrayList<byte[]>();
                for (int k = 0; k < keys.size(); k++) {
                    keyList.add(bytes(keys.get(k)));
                }
                access.add(new AccessEntry(EvmAddress.fromBytes(bytes(e.get(0))), keyList));
            }
        }
        if (!signed) {
            return new EvmTransaction(type, chainId, nonce, gasPrice, tip, cap, gas, to, value, data, access, -1,
                    null, null);
        }
        long v = num(f.get(at++));
        byte[] rr = bytes(f.get(at++));
        byte[] ss = bytes(f.get(at));
        if (type == LEGACY && rr.length == 0 && ss.length == 0) {
            // the unsigned EIP-155 payload: [..., chainId, 0, 0]
            return new EvmTransaction(type, v, nonce, gasPrice, tip, cap, gas, to, value, data, access, -1, null,
                    null);
        }
        if (rr.length > 32 || ss.length > 32) {
            throw new IllegalArgumentException("signature component too long");
        }
        int recId;
        if (type != LEGACY) {
            recId = (int) v;
        } else if (v == 27 || v == 28) {
            recId = (int) (v - 27);
        } else if (v >= 37) {
            chainId = (v - 35) / 2;
            recId = (int) ((v - 35) % 2);
        } else {
            throw new IllegalArgumentException("bad v " + v);
        }
        if (recId < 0 || recId > 1) {
            throw new IllegalArgumentException("bad y parity " + v);
        }
        return new EvmTransaction(type, chainId, nonce, gasPrice, tip, cap, gas, to, value, data, access, recId,
                pad32(rr), pad32(ss));
    }

    // ----------------------------------------------------------------

    /// The envelope: [#LEGACY], [#ACCESS_LIST] or [#DYNAMIC_FEE].
    public int getType() {
        return type;
    }

    /// The chain id; 0 for a legacy transaction signed without EIP-155.
    public long getChainId() {
        return chainId;
    }

    /// The sender's nonce.
    public long getNonce() {
        return nonce;
    }

    /// The gas price of a legacy or access-list transaction; `null` for EIP-1559.
    public UInt256 getGasPrice() {
        return gasPrice;
    }

    /// The EIP-1559 tip; `null` for the other envelopes.
    public UInt256 getMaxPriorityFeePerGas() {
        return maxPriorityFeePerGas;
    }

    /// The EIP-1559 fee cap; `null` for the other envelopes.
    public UInt256 getMaxFeePerGas() {
        return maxFeePerGas;
    }

    /// The gas limit.
    public long getGasLimit() {
        return gasLimit;
    }

    /// The recipient, or `null` for a contract deployment.
    public EvmAddress getTo() {
        return to;
    }

    /// The value sent, in wei.
    public UInt256 getValue() {
        return value;
    }

    /// A copy of the call data.
    public byte[] getData() {
        return copy(data);
    }

    /// The access list, empty for a legacy transaction.
    public List<AccessEntry> getAccessList() {
        return new ArrayList<AccessEntry>(accessList);
    }

    /// The 65-byte `r || s || recoveryId` signature, or `null` when unsigned.
    public byte[] getSignature() {
        if (r == null) {
            return null;
        }
        byte[] out = new byte[65];
        System.arraycopy(r, 0, out, 0, 32);
        System.arraycopy(s, 0, out, 32, 32);
        out[64] = (byte) recoveryId;
        return out;
    }

    // ----------------------------------------------------------------

    private static List<?> list(Object o) {
        if (!(o instanceof List)) {
            throw new IllegalArgumentException("expected an RLP list");
        }
        return (List<?>) o;
    }

    private static byte[] bytes(Object o) {
        if (!(o instanceof byte[])) {
            throw new IllegalArgumentException("expected an RLP string");
        }
        return (byte[]) o;
    }

    private static byte[] integer(Object o) {
        byte[] b = bytes(o);
        if (b.length > 0 && b[0] == 0) {
            throw new IllegalArgumentException("integer with a leading zero");
        }
        if (b.length > 32) {
            throw new IllegalArgumentException("integer over 256 bits");
        }
        return b;
    }

    private static long num(Object o) {
        byte[] b = integer(o);
        if (b.length > 8 || (b.length == 8 && b[0] < 0)) {
            throw new IllegalArgumentException("integer too large");
        }
        long v = 0;
        for (int iter = 0; iter < b.length; iter++) {
            v = (v << 8) | (b[iter] & 0xff);
        }
        return v;
    }

    private static UInt256 big(Object o) {
        return UInt256.fromBytes(integer(o));
    }

    private static byte[] strip(byte[] b) {
        int start = 0;
        while (start < b.length && b[start] == 0) {
            start++;
        }
        byte[] out = new byte[b.length - start];
        System.arraycopy(b, start, out, 0, out.length);
        return out;
    }

    private static byte[] pad32(byte[] b) {
        if (b.length > 32) {
            throw new IllegalArgumentException("more than 32 bytes");
        }
        byte[] out = new byte[32];
        System.arraycopy(b, 0, out, 32 - b.length, b.length);
        return out;
    }

    static byte[] copy(byte[] b) {
        byte[] out = new byte[b.length];
        System.arraycopy(b, 0, out, 0, b.length);
        return out;
    }
}
