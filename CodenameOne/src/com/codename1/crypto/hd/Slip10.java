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
package com.codename1.crypto.hd;

import com.codename1.security.Ed25519;
import com.codename1.security.Hash;
import com.codename1.security.Hmac;

/// SLIP-10 key derivation for Ed25519, the scheme Solana, Stellar, Cardano-style and
/// Aptos/Sui wallets use on a [Mnemonic] seed. Ed25519 has no public derivation, so every
/// index is hardened: `m/44'/501'/0'/0'` is the usual Solana account.
///
/// (SLIP-10 over secp256k1 is BIP-32 itself; use [Bip32] for that curve.)
@com.codename1.impl.SharedWithBackend
public final class Slip10 {
    private final byte[] privateKey;
    private final byte[] chainCode;
    private final int depth;
    private final int parentFingerprint;
    private final long childNumber;
    private byte[] publicKey;

    private Slip10(byte[] privateKey, byte[] chainCode, int depth, int parentFingerprint, long childNumber) {
        this.privateKey = privateKey;
        this.chainCode = chainCode;
        this.depth = depth;
        this.parentFingerprint = parentFingerprint;
        this.childNumber = childNumber;
    }

    /// The master node of a seed (16 to 64 bytes).
    public static Slip10 master(byte[] seed) {
        if (seed == null || seed.length < 16 || seed.length > 64) {
            throw new IllegalArgumentException("a seed is 16 to 64 bytes");
        }
        byte[] i = Hmac.sha512(Bip32.ascii("ed25519 seed"), seed);
        Slip10 out = new Slip10(Bip32.slice(i, 0, 32), Bip32.slice(i, 32, 32), 0, 0, 0);
        Mnemonic.wipe(i);
        return out;
    }

    /// The hardened child at `index`. An index below [DerivationPath#HARDENED] is hardened
    /// for you, since Ed25519 has no other kind.
    public Slip10 child(long index) {
        if (index < 0 || index > 0xffffffffL) {
            throw new IllegalArgumentException("index out of range");
        }
        long hardened = index < DerivationPath.HARDENED ? index + DerivationPath.HARDENED : index;
        byte[] data = new byte[37];
        System.arraycopy(privateKey, 0, data, 1, 32);
        Bip32.writeInt(data, 33, (int) hardened);
        byte[] i = Hmac.sha512(chainCode, data);
        Mnemonic.wipe(data);
        Slip10 out = new Slip10(Bip32.slice(i, 0, 32), Bip32.slice(i, 32, 32), depth + 1, fingerprint(), hardened);
        Mnemonic.wipe(i);
        return out;
    }

    /// Derives along a path such as `m/44'/501'/0'/0'`.
    public Slip10 derive(String path) {
        return derive(DerivationPath.parse(path));
    }

    /// Derives along a path; every index is taken as hardened.
    public Slip10 derive(DerivationPath path) {
        Slip10 node = this;
        for (int iter = 0; iter < path.length(); iter++) {
            Slip10 next = node.child(path.get(iter));
            if (node != this) { // NOPMD CompareObjectsWithEquals - identity: never destroy the receiver
                node.destroy();
            }
            node = next;
        }
        return node;
    }

    /// A copy of the 32-byte Ed25519 private key (the RFC 8032 seed). Wipe it after use.
    public byte[] privateKey() {
        return Bip32.slice(privateKey, 0, 32);
    }

    /// The 32-byte Ed25519 public key.
    public byte[] publicKey() {
        if (publicKey == null) {
            publicKey = Ed25519.publicKey(privateKey);
        }
        return Bip32.slice(publicKey, 0, 32);
    }

    /// A copy of the 32-byte chain code.
    public byte[] chainCode() {
        return Bip32.slice(chainCode, 0, 32);
    }

    /// The distance from the master node.
    public int depth() {
        return depth;
    }

    /// The hardened index this node was derived with.
    public long childNumber() {
        return childNumber;
    }

    /// The parent's fingerprint; 0 for the master node.
    public int parentFingerprint() {
        return parentFingerprint;
    }

    /// The first four bytes of `HASH160(0x00 || publicKey)`, as SLIP-10 defines it.
    public int fingerprint() {
        byte[] pub = publicKey();
        byte[] data = new byte[33];
        System.arraycopy(pub, 0, data, 1, 32);
        return Bip32.readInt(Hash.hash160(data), 0);
    }

    /// Wipes the private key and chain code.
    public void destroy() {
        Mnemonic.wipe(privateKey);
        Mnemonic.wipe(chainCode);
    }
}
