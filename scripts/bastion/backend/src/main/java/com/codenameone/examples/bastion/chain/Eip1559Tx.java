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

import java.util.ArrayList;
import java.util.List;

/// An EIP-1559 (type 2) Ethereum transaction: built, signed and serialized
/// here, so the node receives finished bytes and never a key.
///
/// The signed payload is `keccak256(0x02 || rlp([chainId, nonce,
/// maxPriorityFeePerGas, maxFeePerGas, gasLimit, to, value, data,
/// accessList]))`. The chain id inside it is the replay protection: a
/// withdrawal signed for a test network cannot be replayed on mainnet, and
/// the reverse. The access list is always empty.
public final class Eip1559Tx {
    private final long chainId;
    private final long nonce;
    private final byte[] maxPriorityFee;
    private final byte[] maxFee;
    private final long gasLimit;
    private final byte[] to;
    private final byte[] value;
    private final byte[] data;

    public Eip1559Tx(long chainId, long nonce, byte[] maxPriorityFee, byte[] maxFee,
            long gasLimit, String to, byte[] value, byte[] data) {
        if (chainId <= 0 || nonce < 0 || gasLimit <= 0) {
            throw new IllegalArgumentException("bad transaction field");
        }
        this.chainId = chainId;
        this.nonce = nonce;
        this.maxPriorityFee = Wei.trim(maxPriorityFee);
        this.maxFee = Wei.trim(maxFee);
        this.gasLimit = gasLimit;
        this.to = EthAddress.bytes(to);
        this.value = Wei.trim(value);
        this.data = data == null ? new byte[0] : data;
    }

    private List<byte[]> fields() {
        List<byte[]> f = new ArrayList<byte[]>();
        f.add(Rlp.quantity(chainId));
        f.add(Rlp.quantity(nonce));
        f.add(Rlp.quantity(maxPriorityFee));
        f.add(Rlp.quantity(maxFee));
        f.add(Rlp.quantity(gasLimit));
        f.add(Rlp.string(to));
        f.add(Rlp.quantity(value));
        f.add(Rlp.string(data));
        f.add(Rlp.list(new ArrayList<byte[]>()));
        return f;
    }

    /// The 32-byte hash a signer signs.
    public byte[] signingHash() {
        return EthKeys.keccak256(typed(Rlp.list(fields())));
    }

    /// The broadcastable bytes, given the 65-byte r || s || recovery id
    /// signature of [#signingHash].
    public byte[] raw(byte[] signature) {
        if (signature.length != 65 || (signature[64] & 0xff) > 1) {
            throw new IllegalArgumentException("not a recoverable signature");
        }
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(signature, 0, r, 0, 32);
        System.arraycopy(signature, 32, s, 0, 32);
        List<byte[]> f = fields();
        f.add(Rlp.quantity(signature[64] & 0xff));
        f.add(Rlp.quantity(r));
        f.add(Rlp.quantity(s));
        return typed(Rlp.list(f));
    }

    /// The transaction hash nodes and explorers know it by.
    public static String hash(byte[] raw) {
        return "0x" + Hex32.encode(EthKeys.keccak256(raw));
    }

    /// The upper bound this transaction can cost in fees, in wei.
    public byte[] maxCost() {
        return Wei.mulSmall(maxFee, gasLimit);
    }

    private static byte[] typed(byte[] rlp) {
        return Rlp.concat(new byte[] {0x02}, rlp);
    }
}
