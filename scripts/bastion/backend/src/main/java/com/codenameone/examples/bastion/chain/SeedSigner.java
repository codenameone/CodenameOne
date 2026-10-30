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

import com.codename1.security.Secp256k1;
import com.codename1.crypto.evm.EvmAddress;
import com.codename1.crypto.hd.DerivationPath;
import com.codename1.crypto.wallet.Wallet;

/// [ChainSigner] from a BIP-32 seed held in memory, over the framework's
/// [com.codename1.crypto.wallet.SeedSigner]. Each key is derived when it is
/// needed and wiped after the one signature it makes, so a heap dump between
/// signatures holds the seed and nothing more -- which is the reason a
/// product moves the seed itself into a key service.
public final class SeedSigner implements ChainSigner {
    /// The hot wallet: account 1, so it never collides with a deposit index.
    private static final int HOT_ACCOUNT = 1;

    private final com.codename1.crypto.wallet.SeedSigner keys;
    private final Wallet wallet;
    private final String hot;

    public SeedSigner(byte[] seed) {
        this.keys = new com.codename1.crypto.wallet.SeedSigner(seed);
        this.wallet = new Wallet(keys);
        this.hot = wallet.getEvmAddress(Wallet.evmPath(HOT_ACCOUNT, 0)).toLowerCaseString();
    }

    private static DerivationPath depositPath(long index) {
        return DerivationPath.bip44(60, 0, 0, index);
    }

    @Override
    public String hotAddress() {
        return hot;
    }

    @Override
    public String depositAddress(long index) {
        EvmAddress a = wallet.getEvmAddress(depositPath(index));
        return a.toLowerCaseString();
    }

    @Override
    public byte[] signWithHot(byte[] hash32) {
        return sign(Wallet.evmPath(HOT_ACCOUNT, 0), hash32);
    }

    @Override
    public byte[] signWithDeposit(long index, byte[] hash32) {
        return sign(depositPath(index), hash32);
    }

    /// r || s (low-S) || recovery id, 65 bytes.
    private byte[] sign(DerivationPath path, byte[] hash32) {
        Secp256k1.EcdsaSignature sig = keys.signSecp256k1(path, hash32).normalize();
        byte[] out = new byte[65];
        System.arraycopy(sig.toCompact(), 0, out, 0, 64);
        out[64] = (byte) sig.recoveryId();
        return out;
    }
}
