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
package com.codename1.crypto.wallet;

import com.codename1.crypto.bitcoin.Taproot;
import com.codename1.crypto.hd.Bip32;
import com.codename1.crypto.hd.DerivationPath;
import com.codename1.crypto.hd.Mnemonic;
import com.codename1.crypto.hd.Slip10;
import com.codename1.security.Ed25519;
import com.codename1.security.Schnorr;
import com.codename1.security.Secp256k1;

/// A [Signer] over a BIP-39 seed held in memory. Each key is derived when it is needed and
/// wiped after the one signature it makes, so between signatures the process holds the seed
/// and nothing more. [#destroy] wipes the seed.
///
/// Holding a seed in memory is right for a test network, a recovery tool or a wallet the user
/// unlocks for a session; a custodial service keeps the seed in a key service instead.
@com.codename1.impl.SharedWithBackend
public final class SeedSigner implements Signer {
    private final byte[] seed;
    private final int fingerprint;
    private boolean destroyed;

    /// A signer over a seed, which is copied.
    public SeedSigner(byte[] seed) {
        if (seed == null || seed.length < 16 || seed.length > 64) {
            throw new IllegalArgumentException("a seed is 16 to 64 bytes");
        }
        this.seed = new byte[seed.length];
        System.arraycopy(seed, 0, this.seed, 0, seed.length);
        Bip32 m = Bip32.master(this.seed);
        fingerprint = m.fingerprint();
        m.destroy();
    }

    /// A signer over the seed of a mnemonic and an optional passphrase.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a mnemonic whose words or checksum are wrong
    public static SeedSigner fromMnemonic(String mnemonic, String passphrase) {
        if (!Mnemonic.isValid(mnemonic)) {
            throw new IllegalArgumentException("not a valid BIP-39 mnemonic");
        }
        byte[] s = Mnemonic.toSeed(mnemonic, passphrase == null ? "" : passphrase);
        try {
            return new SeedSigner(s);
        } finally {
            wipe(s);
        }
    }

    @Override
    public int getMasterFingerprint() {
        return fingerprint;
    }

    private Bip32 bip32(DerivationPath path) {
        if (destroyed) {
            throw new IllegalStateException("the signer was destroyed");
        }
        Bip32 m = Bip32.master(seed);
        try {
            return m.derive(path);
        } finally {
            m.destroy();
        }
    }

    private Slip10 slip10(DerivationPath path) {
        if (destroyed) {
            throw new IllegalStateException("the signer was destroyed");
        }
        Slip10 m = Slip10.master(seed);
        try {
            return m.derive(path);
        } finally {
            m.destroy();
        }
    }

    @Override
    public byte[] getSecp256k1PublicKey(DerivationPath path) {
        Bip32 k = bip32(path);
        try {
            return k.publicKey();
        } finally {
            k.destroy();
        }
    }

    @Override
    public Secp256k1.EcdsaSignature signSecp256k1(DerivationPath path, byte[] hash32) {
        Bip32 k = bip32(path);
        byte[] priv = k.privateKey();
        try {
            return Secp256k1.sign(hash32, priv);
        } finally {
            wipe(priv);
            k.destroy();
        }
    }

    @Override
    public byte[] signTaproot(DerivationPath path, byte[] sighash, byte[] merkleRoot) {
        Bip32 k = bip32(path);
        byte[] priv = k.privateKey();
        byte[] tweaked = null;
        try {
            tweaked = Taproot.tweakPrivateKey(priv, merkleRoot);
            return Schnorr.sign(sighash, tweaked, com.codename1.security.CryptoEngine.random(32));
        } finally {
            wipe(priv);
            wipe(tweaked);
            k.destroy();
        }
    }

    @Override
    public byte[] getEd25519PublicKey(DerivationPath path) {
        Slip10 k = slip10(path);
        try {
            return k.publicKey();
        } finally {
            k.destroy();
        }
    }

    @Override
    public byte[] signEd25519(DerivationPath path, byte[] message) {
        Slip10 k = slip10(path);
        byte[] priv = k.privateKey();
        try {
            return Ed25519.sign(priv, message);
        } finally {
            wipe(priv);
            k.destroy();
        }
    }

    /// Wipes the seed; the signer cannot sign afterwards.
    public void destroy() {
        wipe(seed);
        destroyed = true;
    }

    static void wipe(byte[] b) {
        if (b != null) {
            for (int i = 0; i < b.length; i++) {
                b[i] = 0;
            }
        }
    }
}
