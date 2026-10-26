/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
import com.codename1.security.CryptoEngine;
import com.codename1.security.Ed25519;
import com.codename1.security.Hash;
import com.codename1.security.NativeCryptoEngine;
import com.codename1.security.Schnorr;
import com.codename1.security.Secp256k1;

/// The elliptic-curve and Keccak kernels, twice: once on the portable Java
/// engine and once on the engine an application gets. On ParparVM the second
/// is the C kernels (libsecp256k1, ed25519-donna), which this program is
/// translated with, so both runs printing what HotSpot prints proves the
/// natives and the Java engine agree byte for byte.
public final class CryptoChecks {
    private CryptoChecks() {
    }

    private interface Op {
        Object run() throws Exception;
    }

    private static String run(Op op) {
        try {
            Object o = op.run();
            return o instanceof byte[] ? Hash.toHex((byte[]) o) : String.valueOf(o);
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        }
    }

    /// Deterministic pseudo-random bytes, so both runs see the same inputs.
    private static byte[] bytes(int seed, int length) {
        byte[] out = new byte[length];
        int x = seed * 0x9E3779B1 + 0x7F4A7C15;
        for (int i = 0; i < length; i++) {
            x ^= x << 13;
            x ^= x >>> 17;
            x ^= x << 5;
            out[i] = (byte) x;
        }
        return out;
    }

    private static byte[] flip(byte[] a, int index) {
        byte[] b = new byte[a.length];
        System.arraycopy(a, 0, b, 0, a.length);
        b[index] ^= 1;
        return b;
    }

    /// Runs the group.
    public static void run() {
        Bulk.cn1Only("Crypto.kernelsAreLinked", () -> {
            Bulk.expect(NativeCryptoEngine.create(null) instanceof NativeCryptoEngine,
                    "the translation was asked for the kernels and they answer available");
        });
        CryptoEngine.install(null);
        vectors("Crypto.portable");
        if (!Bulk.onHotSpot()) {
            CryptoEngine.install(NativeCryptoEngine.create(null));
        }
        vectors("Crypto.engine");
        CryptoEngine.install(null);
    }

    private static void vectors(String name) {
        Bulk.check(name + ".secp256k1", () -> {
            for (int k = 0; k < 12; k++) {
                final int i = k;
                final byte[] priv = bytes(i, 32);
                final byte[] hash = bytes(i + 100, 32);
                final byte[] tweak = bytes(i + 200, 32);
                Bulk.p("pub[" + i + "]", run(() -> Secp256k1.publicKey(priv, true)));
                Bulk.p("pubFull[" + i + "]", run(() -> Secp256k1.publicKey(priv, false)));
                Bulk.p("sign[" + i + "]", run(() -> {
                    Secp256k1.EcdsaSignature s = Secp256k1.sign(hash, priv);
                    return Hash.toHex(s.toCompact()) + "/" + s.recoveryId();
                }));
                Bulk.p("verify[" + i + "]", run(() -> {
                    byte[] pub = Secp256k1.publicKey(priv, i % 2 == 0);
                    Secp256k1.EcdsaSignature s = Secp256k1.sign(hash, priv);
                    return Secp256k1.verify(hash, s, pub) + "," + Secp256k1.verify(flip(hash, i), s, pub)
                            + "," + Secp256k1.verify(hash, Secp256k1.EcdsaSignature.fromCompact(
                                    flip(s.toCompact(), 40), 0), pub);
                }));
                Bulk.p("recover[" + i + "]", run(() -> {
                    Secp256k1.EcdsaSignature s = Secp256k1.sign(hash, priv);
                    return Secp256k1.recoverPublicKey(hash, s, i % 2 == 1);
                }));
                Bulk.p("tweakPriv[" + i + "]", run(() -> Secp256k1.privateKeyTweakAdd(priv, tweak)));
                Bulk.p("tweakPub[" + i + "]", run(() ->
                        Secp256k1.publicKeyTweakAdd(Secp256k1.publicKey(priv, true), tweak, i % 2 == 0)));
            }
            final byte[] zero = new byte[32];
            final byte[] high = new byte[32];
            java.util.Arrays.fill(high, (byte) 0xff);
            Bulk.p("pub[zero]", run(() -> Secp256k1.publicKey(zero, true)));
            Bulk.p("pub[high]", run(() -> Secp256k1.publicKey(high, true)));
            Bulk.p("verify[badKey]", run(() -> Secp256k1.verify(zero,
                    Secp256k1.sign(bytes(1, 32), bytes(2, 32)), flip(Secp256k1.publicKey(bytes(2, 32), true), 5))));
        });
        Bulk.check(name + ".schnorr", () -> {
            for (int k = 0; k < 12; k++) {
                final int i = k;
                final byte[] priv = bytes(i + 300, 32);
                final byte[] msg = bytes(i + 400, i % 3 == 0 ? 32 : i * 7);
                final byte[] aux = i % 2 == 0 ? null : bytes(i + 500, 32);
                Bulk.p("pub[" + i + "]", run(() -> Schnorr.publicKey(priv)));
                Bulk.p("sign[" + i + "]", run(() -> Schnorr.sign(msg, priv, aux)));
                Bulk.p("verify[" + i + "]", run(() -> {
                    byte[] sig = Schnorr.sign(msg, priv, aux);
                    byte[] pub = Schnorr.publicKey(priv);
                    return Schnorr.verify(msg, sig, pub) + "," + Schnorr.verify(msg, flip(sig, i), pub)
                            + "," + Schnorr.verify(msg, sig, flip(pub, 3));
                }));
            }
        });
        Bulk.check(name + ".ed25519", () -> {
            for (int k = 0; k < 12; k++) {
                final int i = k;
                final byte[] seed = bytes(i + 600, 32);
                final byte[] msg = bytes(i + 700, i * 13);
                Bulk.p("pub[" + i + "]", run(() -> Ed25519.publicKey(seed)));
                Bulk.p("sign[" + i + "]", run(() -> Ed25519.sign(seed, msg)));
                Bulk.p("verify[" + i + "]", run(() -> {
                    byte[] sig = Ed25519.sign(seed, msg);
                    byte[] pub = Ed25519.publicKey(seed);
                    // S + L: the same point equation, refused as non-canonical
                    byte[] big = flip(sig, 63);
                    big[63] ^= 1;
                    big[63] |= 0x10;
                    return Ed25519.verify(msg, sig, pub) + "," + Ed25519.verify(msg, flip(sig, i), pub)
                            + "," + Ed25519.verify(msg, sig, flip(pub, 2)) + "," + Ed25519.verify(msg, big, pub);
                }));
            }
            // a key encoding y = p (non-canonical) and y = 1 with the sign bit set
            final byte[] yp = new byte[32];
            java.util.Arrays.fill(yp, (byte) 0xff);
            yp[0] = (byte) 0xed;
            yp[31] = (byte) 0x7f;
            final byte[] negOne = new byte[32];
            negOne[0] = 1;
            negOne[31] = (byte) 0x80;
            final byte[] sig = Ed25519.sign(bytes(9, 32), new byte[0]);
            Bulk.p("verify[y=p]", run(() -> Ed25519.verify(new byte[0], sig, yp)));
            Bulk.p("verify[x=0,sign]", run(() -> Ed25519.verify(new byte[0], sig, negOne)));
        });
        Bulk.check(name + ".keccak256", () -> {
            for (final int len : new int[] {0, 1, 3, 55, 135, 136, 137, 272, 1000}) {
                final byte[] in = bytes(len + 800, len);
                Bulk.p("keccak[" + len + "]", run(() -> Hash.keccak256(in)));
            }
        });
    }
}
