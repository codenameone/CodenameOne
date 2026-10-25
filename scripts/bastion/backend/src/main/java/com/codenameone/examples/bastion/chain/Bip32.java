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

import com.codename1.security.Hmac;

/// BIP-32 hierarchical deterministic keys, private derivation only.
///
/// Every customer deposit address is a child of one seed, at the BIP-44
/// Ethereum path m/44'/60'/0'/0/i. One secret backs them all, so the seed is
/// what goes in an HSM or a sealed backup, and any standard wallet given it
/// finds the same addresses -- the funds survive this server.
public final class Bip32 {
    /// The offset that marks a hardened index.
    public static final long HARDENED = 0x80000000L;

    private final byte[] key;
    private final byte[] chainCode;

    private Bip32(byte[] key, byte[] chainCode) {
        this.key = key;
        this.chainCode = chainCode;
    }

    /// The master key of `seed` (16 to 64 bytes).
    public static Bip32 master(byte[] seed) {
        if (seed.length < 16 || seed.length > 64) {
            throw new IllegalArgumentException("a seed is 16 to 64 bytes");
        }
        byte[] i = Hmac.sha512(ascii("Bitcoin seed"), seed);
        byte[] k = slice(i, 0);
        if (!EthKeys.validPrivate(k)) {
            throw new IllegalArgumentException("unusable seed");
        }
        return new Bip32(k, slice(i, 32));
    }

    /// The child at `index`; add [#HARDENED] for a hardened one.
    public Bip32 child(long index) {
        if (index < 0 || index > 0xffffffffL) {
            throw new IllegalArgumentException("index out of range");
        }
        byte[] data = new byte[37];
        if (index >= HARDENED) {
            data[0] = 0;
            System.arraycopy(key, 0, data, 1, 32);
        } else {
            System.arraycopy(EthKeys.compressedPublicKey(key), 0, data, 0, 33);
        }
        data[33] = (byte) (index >>> 24);
        data[34] = (byte) (index >>> 16);
        data[35] = (byte) (index >>> 8);
        data[36] = (byte) index;
        byte[] i = Hmac.sha512(chainCode, data);
        byte[] child = EthKeys.addPrivate(key, slice(i, 0));
        if (child == null) {
            // Probability below 2^-127; the specification says use the next index.
            return child(index + 1);
        }
        return new Bip32(child, slice(i, 32));
    }

    /// The key at m/44'/60'/0'/0/`index` under `seed`.
    public static Bip32 ethereum(byte[] seed, long index) {
        return master(seed).child(44 + HARDENED).child(60 + HARDENED).child(HARDENED).child(0)
                .child(index);
    }

    /// A copy of the 32-byte private key. Wipe it after use.
    public byte[] privateKey() {
        byte[] out = new byte[32];
        System.arraycopy(key, 0, out, 0, 32);
        return out;
    }

    public String address() {
        return EthAddress.of(EthKeys.publicKey(key));
    }

    private static byte[] slice(byte[] in, int from) {
        byte[] out = new byte[32];
        System.arraycopy(in, from, out, 0, 32);
        return out;
    }

    private static byte[] ascii(String text) {
        byte[] out = new byte[text.length()];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) text.charAt(iter);
        }
        return out;
    }
}
