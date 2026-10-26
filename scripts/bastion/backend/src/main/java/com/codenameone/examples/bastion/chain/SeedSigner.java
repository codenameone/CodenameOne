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

/// [ChainSigner] from a BIP-32 seed held in memory. Each key is derived when
/// it is needed and wiped after the one signature it makes, so a heap dump
/// between signatures holds the seed and nothing more -- which is the reason
/// a product moves the seed itself into a key service.
public final class SeedSigner implements ChainSigner {
    /// The hot wallet: account 1, so it never collides with a deposit index.
    private static final long HOT_ACCOUNT = 1;

    private final byte[] seed;
    private final String hot;

    public SeedSigner(byte[] seed) {
        this.seed = seed;
        this.hot = hotKey().address();
    }

    private Bip32 hotKey() {
        return Bip32.master(seed).child(44 + Bip32.HARDENED).child(60 + Bip32.HARDENED)
                .child(HOT_ACCOUNT + Bip32.HARDENED).child(0).child(0);
    }

    @Override
    public String hotAddress() {
        return hot;
    }

    @Override
    public String depositAddress(long index) {
        return Bip32.ethereum(seed, index).address();
    }

    @Override
    public byte[] signWithHot(byte[] hash32) {
        return signWith(hotKey().privateKey(), hash32);
    }

    @Override
    public byte[] signWithDeposit(long index, byte[] hash32) {
        return signWith(Bip32.ethereum(seed, index).privateKey(), hash32);
    }

    private static byte[] signWith(byte[] key, byte[] hash32) {
        try {
            return EthKeys.sign(key, hash32);
        } finally {
            for (int iter = 0; iter < key.length; iter++) {
                key[iter] = 0;
            }
        }
    }
}
