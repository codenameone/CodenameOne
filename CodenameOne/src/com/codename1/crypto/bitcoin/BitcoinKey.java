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
package com.codename1.crypto.bitcoin;

import com.codename1.security.Secp256k1;

/// A secp256k1 key that signs Bitcoin inputs without necessarily exposing its private key.
/// [Psbt#sign(int, BitcoinKey)] takes one, so an input can be signed by a key held in memory
/// ([#fromPrivateKey]), by a wallet that derives it per signature, or by a hardware device or
/// key service that implements the two signing methods.
@com.codename1.impl.SharedWithBackend
public abstract class BitcoinKey {
    /// The compressed 33-byte public key.
    public abstract byte[] getPublicKey();

    /// An ECDSA signature over a 32-byte sighash, RFC 6979 deterministic and low-S.
    public abstract Secp256k1.EcdsaSignature signEcdsa(byte[] sighash);

    /// A BIP-340 signature over a 32-byte Taproot sighash, made with the key tweaked by
    /// BIP-341 for `merkleRoot` (`null` for a key-path-only output, as BIP-86 derives).
    public abstract byte[] signTaproot(byte[] sighash, byte[] merkleRoot);

    /// The uncompressed 65-byte public key, which some legacy outputs pay to.
    public byte[] getUncompressedPublicKey() {
        return Secp256k1.convertPublicKey(getPublicKey(), false);
    }

    /// The 32-byte x-only Taproot internal key.
    public byte[] getInternalKey() {
        byte[] pub = getPublicKey();
        byte[] x = new byte[32];
        System.arraycopy(pub, 1, x, 0, 32);
        return x;
    }

    /// Wipes any key material the instance holds. The default holds none.
    public void destroy() {
    }

    /// A key over a 32-byte private key, which is copied; [#destroy] wipes the copy.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the bytes are not a valid private key
    public static BitcoinKey fromPrivateKey(byte[] privateKey) {
        if (!Secp256k1.isValidPrivateKey(privateKey)) {
            throw new IllegalArgumentException("not a secp256k1 private key");
        }
        return new Local(Wire.copy(privateKey));
    }

    private static final class Local extends BitcoinKey {
        private final byte[] key;
        private final byte[] pub;

        Local(byte[] key) {
            this.key = key;
            this.pub = Secp256k1.publicKey(key, true);
        }

        @Override
        public byte[] getPublicKey() {
            return Wire.copy(pub);
        }

        @Override
        public Secp256k1.EcdsaSignature signEcdsa(byte[] sighash) {
            return Secp256k1.sign(sighash, key);
        }

        @Override
        public byte[] signTaproot(byte[] sighash, byte[] merkleRoot) {
            byte[] tweaked = Taproot.tweakPrivateKey(key, merkleRoot);
            try {
                return com.codename1.security.Schnorr.sign(sighash, tweaked,
                        com.codename1.security.SecureRandom.bytes(32));
            } finally {
                Wif.wipe(tweaked);
            }
        }

        @Override
        public void destroy() {
            Wif.wipe(key);
        }
    }
}
