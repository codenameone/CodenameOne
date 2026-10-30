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

import com.codename1.crypto.UInt256;
import com.codename1.security.Hash;
import com.codename1.security.Schnorr;
import com.codename1.security.Secp256k1;

/// Taproot (BIP-341) key tweaking and script trees.
///
/// A Taproot output commits to an internal key and, optionally, the Merkle root of a tree
/// of scripts: `Q = P + H_TapTweak(P || root) * G`. A key-path spend signs with the tweaked
/// private key; [#tweakPrivateKey] gives it. BIP-86 wallets use no script tree.
@com.codename1.impl.SharedWithBackend
public final class Taproot {
    /// The leaf version of a Tapscript leaf.
    public static final int LEAF_VERSION_TAPSCRIPT = 0xc0;

    private static final UInt256 N = UInt256.parse(
            "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141");

    private Taproot() {
    }

    /// `SHA256(SHA256(tag) || SHA256(tag) || data)`, BIP-340's tagged hash.
    public static byte[] taggedHash(String tag, byte[] data) {
        byte[] t = Hash.sha256(ascii(tag));
        byte[] all = new byte[64 + data.length];
        System.arraycopy(t, 0, all, 0, 32);
        System.arraycopy(t, 0, all, 32, 32);
        System.arraycopy(data, 0, all, 64, data.length);
        return Hash.sha256(all);
    }

    /// The tweak `H_TapTweak(internalKey || merkleRoot)`; pass `null` for a key-only output.
    public static byte[] tweak(byte[] internalKey32, byte[] merkleRoot) {
        return taggedHash("TapTweak", merkleRoot == null ? internalKey32 : Wire.concat(internalKey32, merkleRoot));
    }

    /// The x-only output key of an internal key and an optional Merkle root.
    public static byte[] outputKey(byte[] internalKey32, byte[] merkleRoot) {
        return tweakPublicKey(internalKey32, merkleRoot)[0];
    }

    /// The output key and its parity: `{x-only key, {0 or 1}}`. The parity goes into the
    /// control block of a script-path spend.
    public static byte[][] tweakPublicKey(byte[] internalKey32, byte[] merkleRoot) {
        if (internalKey32 == null || internalKey32.length != 32) {
            throw new IllegalArgumentException("an x-only key is 32 bytes");
        }
        byte[] even = new byte[33];
        even[0] = 2;
        System.arraycopy(internalKey32, 0, even, 1, 32);
        byte[] q = Secp256k1.publicKeyTweakAdd(even, tweak(internalKey32, merkleRoot), true);
        byte[] x = new byte[32];
        System.arraycopy(q, 1, x, 0, 32);
        return new byte[][] {x, {(byte) (q[0] == 3 ? 1 : 0)}};
    }

    /// The private key that signs for [#outputKey] on the key path.
    public static byte[] tweakPrivateKey(byte[] privateKey, byte[] merkleRoot) {
        byte[] full = Secp256k1.publicKey(privateKey, true);
        byte[] x = new byte[32];
        System.arraycopy(full, 1, x, 0, 32);
        byte[] d = privateKey;
        boolean negated = false;
        if (full[0] == 3) {
            d = N.subtract(UInt256.fromBytes(privateKey)).toBytes32();
            negated = true;
        }
        try {
            return Secp256k1.privateKeyTweakAdd(d, tweak(x, merkleRoot));
        } finally {
            if (negated) {
                for (int iter = 0; iter < d.length; iter++) {
                    d[iter] = 0;
                }
            }
        }
    }

    /// The x-only internal key of a private key, as BIP-86 derives it.
    public static byte[] internalKey(byte[] privateKey) {
        return Schnorr.publicKey(privateKey);
    }

    /// The hash of a script leaf: `H_TapLeaf(version || compactSize(len) || script)`.
    public static byte[] leafHash(int leafVersion, Script script) {
        Wire w = new Wire().u8(leafVersion).varBytes(script.raw());
        return taggedHash("TapLeaf", w.toBytes());
    }

    /// The hash of a branch: `H_TapBranch` of the two children in byte order.
    public static byte[] branchHash(byte[] a, byte[] b) {
        boolean aFirst = compare(a, b) <= 0;
        return taggedHash("TapBranch", aFirst ? Wire.concat(a, b) : Wire.concat(b, a));
    }

    /// The P2TR output script of an internal key and optional Merkle root.
    public static Script outputScript(byte[] internalKey32, byte[] merkleRoot) {
        return Script.p2tr(outputKey(internalKey32, merkleRoot));
    }

    private static int compare(byte[] a, byte[] b) {
        for (int iter = 0; iter < 32; iter++) {
            int x = a[iter] & 0xff;
            int y = b[iter] & 0xff;
            if (x != y) {
                return x - y;
            }
        }
        return 0;
    }

    static byte[] ascii(String text) {
        byte[] out = new byte[text.length()];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) text.charAt(iter);
        }
        return out;
    }
}
