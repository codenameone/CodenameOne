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

import com.codename1.security.Hash;
import com.codename1.security.Secp256k1;

/// EIP-191 `personal_sign` messages, the "Sign in with Ethereum" and wallet-ownership proof,
/// and the 65-byte `r || s || v` signatures EVM wallets exchange.
///
/// `v` is written as 27 or 28, the form `personal_sign` and `eth_sign` return; the
/// recovering methods also accept 0 and 1.
@com.codename1.impl.SharedWithBackend
public final class PersonalSign {
    private PersonalSign() {
    }

    /// `keccak256("\x19Ethereum Signed Message:\n" + len(message) + message)`.
    public static byte[] hash(byte[] message) {
        byte[] prefix = EvmAddress.ascii("\u0019Ethereum Signed Message:\n" + message.length);
        byte[] all = new byte[prefix.length + message.length];
        System.arraycopy(prefix, 0, all, 0, prefix.length);
        System.arraycopy(message, 0, all, prefix.length, message.length);
        return Hash.keccak256(all);
    }

    /// [#hash] of a text message, UTF-8 encoded.
    public static byte[] hash(String message) {
        return hash(Abi.utf8(message));
    }

    /// Signs a message as `personal_sign` does.
    public static byte[] sign(byte[] message, byte[] privateKey) {
        return signHash(hash(message), privateKey);
    }

    /// Signs a text message as `personal_sign` does.
    public static byte[] sign(String message, byte[] privateKey) {
        return sign(Abi.utf8(message), privateKey);
    }

    /// The address that signed a message.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a malformed signature
    ///
    /// - `com.codename1.security.CryptoException`: when the signature recovers no key
    public static EvmAddress recover(byte[] message, byte[] signature65) {
        return recoverHash(hash(message), signature65);
    }

    /// Tells whether `address` signed the text `message`.
    public static boolean verify(String message, byte[] signature65, EvmAddress address) {
        try {
            return recover(Abi.utf8(message), signature65).equals(address);
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    /// Signs a 32-byte digest, answering `r || s || v` with `v` 27 or 28.
    public static byte[] signHash(byte[] hash32, byte[] privateKey) {
        Secp256k1.EcdsaSignature sig = Secp256k1.sign(hash32, privateKey);
        byte[] out = new byte[65];
        System.arraycopy(sig.r(), 0, out, 0, 32);
        System.arraycopy(sig.s(), 0, out, 32, 32);
        out[64] = (byte) (27 + sig.recoveryId());
        return out;
    }

    /// The address whose key produced `signature65` over a 32-byte digest.
    public static EvmAddress recoverHash(byte[] hash32, byte[] signature65) {
        if (signature65 == null || signature65.length != 65) {
            throw new IllegalArgumentException("a signature is 65 bytes");
        }
        int v = signature65[64] & 0xff;
        int recId = v >= 27 ? v - 27 : v;
        if (recId < 0 || recId > 1) {
            throw new IllegalArgumentException("bad v " + v);
        }
        byte[] compact = new byte[64];
        System.arraycopy(signature65, 0, compact, 0, 64);
        byte[] pub = Secp256k1.recoverPublicKey(hash32, Secp256k1.EcdsaSignature.fromCompact(compact, recId), false);
        return EvmAddress.fromPublicKey(pub);
    }
}
