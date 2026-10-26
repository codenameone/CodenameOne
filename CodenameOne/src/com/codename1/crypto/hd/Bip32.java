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

import com.codename1.crypto.encoding.Base58Check;
import com.codename1.security.CryptoException;
import com.codename1.security.Hash;
import com.codename1.security.Hmac;
import com.codename1.security.Secp256k1;

/// A BIP-32 hierarchical deterministic key on secp256k1: Bitcoin, every EVM chain and most
/// others derive their accounts this way from one [Mnemonic] seed.
///
/// A node holds a private key, or only the public key once [#neuter] has dropped it; a
/// public node still derives every non-hardened child, which is how a watch-only server
/// hands out deposit addresses without holding a secret. [#toXprv] and [#toXpub] write the
/// standard serialization and [#parse] reads it back.
///
/// Private key material lives in arrays this object owns; call [#destroy] to wipe them.
@com.codename1.impl.SharedWithBackend
public final class Bip32 {
    /// The offset that marks a hardened index.
    public static final long HARDENED = DerivationPath.HARDENED;
    /// The mainnet private version bytes, which print as `xprv`.
    public static final int XPRV = 0x0488ADE4;
    /// The mainnet public version bytes, which print as `xpub`.
    public static final int XPUB = 0x0488B21E;
    /// The testnet private version bytes, which print as `tprv`.
    public static final int TPRV = 0x04358394;
    /// The testnet public version bytes, which print as `tpub`.
    public static final int TPUB = 0x043587CF;

    private final byte[] privateKey;
    private final byte[] publicKey;
    private final byte[] chainCode;
    private final int depth;
    private final int parentFingerprint;
    private final long childNumber;
    private int version;

    private Bip32(byte[] privateKey, byte[] publicKey, byte[] chainCode, int depth, int parentFingerprint,
            long childNumber) {
        this.privateKey = privateKey;
        this.publicKey = publicKey == null ? Secp256k1.publicKey(privateKey, true) : publicKey;
        this.chainCode = chainCode;
        this.depth = depth;
        this.parentFingerprint = parentFingerprint;
        this.childNumber = childNumber;
    }

    /// The master node of a seed (16 to 64 bytes, normally [Mnemonic#toSeed]).
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a seed out of range, or one whose master key is
    ///   invalid (probability below 2^-127)
    public static Bip32 master(byte[] seed) {
        if (seed == null || seed.length < 16 || seed.length > 64) {
            throw new IllegalArgumentException("a seed is 16 to 64 bytes");
        }
        byte[] i = Hmac.sha512(ascii("Bitcoin seed"), seed);
        byte[] key = slice(i, 0, 32);
        byte[] chain = slice(i, 32, 32);
        Mnemonic.wipe(i);
        if (!Secp256k1.isValidPrivateKey(key)) {
            Mnemonic.wipe(key);
            throw new IllegalArgumentException("unusable seed");
        }
        return new Bip32(key, null, chain, 0, 0, 0);
    }

    /// Reads an extended key in its Base58Check form (`xprv...`, `xpub...`, `tprv...` or any
    /// other version bytes); [#version] tells which.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a malformed string, a bad checksum or a key that
    ///   is not valid on the curve
    public static Bip32 parse(String extendedKey) {
        byte[] raw = Base58Check.decode(extendedKey);
        try {
            if (raw.length != 78) {
                throw new IllegalArgumentException("an extended key is 78 bytes");
            }
            int depth = raw[4] & 0xff;
            int parent = readInt(raw, 5);
            long child = readInt(raw, 9) & 0xffffffffL;
            if (depth == 0 && (parent != 0 || child != 0)) {
                throw new IllegalArgumentException("master key with a parent");
            }
            byte[] chain = slice(raw, 13, 32);
            if (raw[45] == 0) {
                byte[] key = slice(raw, 46, 32);
                if (!Secp256k1.isValidPrivateKey(key)) {
                    Mnemonic.wipe(key);
                    throw new IllegalArgumentException("invalid private key");
                }
                return new Bip32(key, null, chain, depth, parent, child).withVersion(readInt(raw, 0));
            }
            byte[] pub = slice(raw, 45, 33);
            if ((pub[0] != 2 && pub[0] != 3) || !Secp256k1.isValidPublicKey(pub)) {
                throw new IllegalArgumentException("invalid public key");
            }
            return new Bip32(null, pub, chain, depth, parent, child).withVersion(readInt(raw, 0));
        } finally {
            Mnemonic.wipe(raw);
        }
    }

    private Bip32 withVersion(int v) {
        version = v;
        return this;
    }

    /// The version bytes this node was [#parse]d with, or 0 for a derived node.
    public int version() {
        return version;
    }

    /// The child at `index`; add [#HARDENED] for a hardened one, which needs the private key.
    /// When an index yields an invalid key, as BIP-32 allows with probability below 2^-127,
    /// the next index is used.
    public Bip32 child(long index) {
        if (index < 0 || index > 0xffffffffL) {
            throw new IllegalArgumentException("index out of range");
        }
        byte[] data = new byte[37];
        if (index >= HARDENED) {
            if (privateKey == null) {
                throw new IllegalStateException("hardened derivation needs the private key");
            }
            System.arraycopy(privateKey, 0, data, 1, 32);
        } else {
            System.arraycopy(publicKey, 0, data, 0, 33);
        }
        writeInt(data, 33, (int) index);
        byte[] i = Hmac.sha512(chainCode, data);
        Mnemonic.wipe(data);
        byte[] tweak = slice(i, 0, 32);
        byte[] chain = slice(i, 32, 32);
        Mnemonic.wipe(i);
        int fp = fingerprint();
        try {
            if (privateKey != null) {
                return new Bip32(Secp256k1.privateKeyTweakAdd(privateKey, tweak), null, chain, depth + 1, fp,
                        index);
            }
            return new Bip32(null, Secp256k1.publicKeyTweakAdd(publicKey, tweak, true), chain, depth + 1, fp,
                    index);
        } catch (CryptoException invalid) {
            if (index == 0xffffffffL || index == HARDENED - 1) {
                throw invalid;
            }
            return child(index + 1);
        } finally {
            Mnemonic.wipe(tweak);
        }
    }

    /// Derives along a path relative to this node, such as `m/84'/0'/0'/0/7`.
    public Bip32 derive(String path) {
        return derive(DerivationPath.parse(path));
    }

    /// Derives along a path relative to this node.
    public Bip32 derive(DerivationPath path) {
        Bip32 node = this;
        for (int iter = 0; iter < path.length(); iter++) {
            Bip32 next = node.child(path.get(iter));
            if (node != this) {
                node.destroy();
            }
            node = next;
        }
        return node;
    }

    /// This node without its private key.
    public Bip32 neuter() {
        return new Bip32(null, publicKey, chainCode, depth, parentFingerprint, childNumber);
    }

    /// Tells whether this node holds its private key.
    public boolean hasPrivateKey() {
        return privateKey != null;
    }

    /// A copy of the 32-byte private key. Wipe it after use.
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: on a public-only node
    public byte[] privateKey() {
        if (privateKey == null) {
            throw new IllegalStateException("public-only node");
        }
        return slice(privateKey, 0, 32);
    }

    /// A copy of the 33-byte compressed public key.
    public byte[] publicKey() {
        return slice(publicKey, 0, 33);
    }

    /// A copy of the 32-byte chain code.
    public byte[] chainCode() {
        return slice(chainCode, 0, 32);
    }

    /// The distance from the master node.
    public int depth() {
        return depth;
    }

    /// The index this node was derived with, including [#HARDENED].
    public long childNumber() {
        return childNumber;
    }

    /// The first four bytes of the parent's key identifier, as a big-endian int; 0 for the
    /// master node.
    public int parentFingerprint() {
        return parentFingerprint;
    }

    /// The first four bytes of `HASH160(publicKey)`, as a big-endian int.
    public int fingerprint() {
        return readInt(Hash.hash160(publicKey), 0);
    }

    /// The mainnet `xprv` serialization.
    public String toXprv() {
        return serialize(XPRV, true);
    }

    /// The mainnet `xpub` serialization.
    public String toXpub() {
        return serialize(XPUB, false);
    }

    /// The serialization under any version bytes (SLIP-132 `zpub`, testnet `tpub`, ...).
    ///
    /// #### Parameters
    ///
    /// - `versionBytes`: the four version bytes as a big-endian int
    ///
    /// - `includePrivate`: `true` for the private form, which needs the private key
    public String serialize(int versionBytes, boolean includePrivate) {
        if (includePrivate && privateKey == null) {
            throw new IllegalStateException("public-only node");
        }
        byte[] raw = new byte[78];
        writeInt(raw, 0, versionBytes);
        raw[4] = (byte) depth;
        writeInt(raw, 5, parentFingerprint);
        writeInt(raw, 9, (int) childNumber);
        System.arraycopy(chainCode, 0, raw, 13, 32);
        if (includePrivate) {
            System.arraycopy(privateKey, 0, raw, 46, 32);
        } else {
            System.arraycopy(publicKey, 0, raw, 45, 33);
        }
        try {
            return Base58Check.encode(raw);
        } finally {
            Mnemonic.wipe(raw);
        }
    }

    /// Wipes the private key and chain code. The node is unusable afterwards.
    public void destroy() {
        Mnemonic.wipe(privateKey);
        Mnemonic.wipe(chainCode);
    }

    static byte[] slice(byte[] in, int from, int len) {
        byte[] out = new byte[len];
        System.arraycopy(in, from, out, 0, len);
        return out;
    }

    static int readInt(byte[] in, int at) {
        return ((in[at] & 0xff) << 24) | ((in[at + 1] & 0xff) << 16) | ((in[at + 2] & 0xff) << 8)
                | (in[at + 3] & 0xff);
    }

    static void writeInt(byte[] out, int at, int value) {
        out[at] = (byte) (value >>> 24);
        out[at + 1] = (byte) (value >>> 16);
        out[at + 2] = (byte) (value >>> 8);
        out[at + 3] = (byte) value;
    }

    static byte[] ascii(String text) {
        byte[] out = new byte[text.length()];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) text.charAt(iter);
        }
        return out;
    }
}
