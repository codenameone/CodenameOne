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
import com.codename1.crypto.UInt256;
import com.codename1.crypto.bitcoin.BitcoinAddress;
import com.codename1.crypto.bitcoin.BitcoinNetwork;
import com.codename1.crypto.encoding.Base58;
import com.codename1.crypto.encoding.Bech32;
import com.codename1.crypto.evm.Abi;
import com.codename1.crypto.evm.EvmAddress;
import com.codename1.crypto.evm.EvmTransaction;
import com.codename1.crypto.hd.Bip32;
import com.codename1.crypto.hd.Mnemonic;
import com.codename1.crypto.hd.Slip10;
import com.codename1.crypto.solana.Instruction;
import com.codename1.crypto.solana.SolanaAddress;
import com.codename1.crypto.solana.SolanaMessage;
import com.codename1.crypto.solana.SolanaTransaction;
import com.codename1.crypto.solana.SystemProgram;
import com.codename1.security.CryptoBatch;
import com.codename1.security.CryptoEngine;
import com.codename1.security.Ed25519;
import com.codename1.security.Hash;
import com.codename1.security.Hmac;
import com.codename1.security.NativeCryptoEngine;
import com.codename1.security.Pbkdf2;
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
        stack(name);
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
        Bulk.check(name + ".batch", () -> {
            // large enough to be cut into chunks over the cores; malformed entries mixed in
            final int n = 40;
            final byte[][] hashes = new byte[n][];
            final Secp256k1.EcdsaSignature[] sigs = new Secp256k1.EcdsaSignature[n];
            final byte[][] keys = new byte[n][];
            final byte[][] msgs = new byte[n][];
            final byte[][] edSigs = new byte[n][];
            final byte[][] edKeys = new byte[n][];
            final byte[][] schSigs = new byte[n][];
            final byte[][] schKeys = new byte[n][];
            for (int i = 0; i < n; i++) {
                byte[] priv = bytes(i + 1000, 32);
                hashes[i] = bytes(i + 1100, 32);
                sigs[i] = Secp256k1.sign(hashes[i], priv);
                keys[i] = Secp256k1.publicKey(priv, i % 2 == 0);
                msgs[i] = bytes(i + 1200, i * 5);
                edKeys[i] = Ed25519.publicKey(priv);
                edSigs[i] = Ed25519.sign(priv, edKeys[i], msgs[i]);
                schKeys[i] = Schnorr.publicKey(priv);
                schSigs[i] = Schnorr.sign(msgs[i], priv, null);
                if (i % 7 == 3) {
                    hashes[i] = flip(hashes[i], 2);
                    edSigs[i] = flip(edSigs[i], 5);
                    schSigs[i] = flip(schSigs[i], 5);
                }
            }
            keys[5] = flip(keys[5], 9);
            keys[6] = new byte[20];
            sigs[8] = null;
            edKeys[9] = new byte[31];
            edSigs[11][63] |= 0x10;
            msgs[12] = null;
            final StringBuilder sb = new StringBuilder();
            boolean[][] all = {CryptoBatch.verifySecp256k1(hashes, sigs, keys),
                CryptoBatch.verifyEd25519(msgs, edSigs, edKeys), CryptoBatch.verifySchnorr(msgs, schSigs, schKeys)};
            for (boolean[] r : all) {
                for (boolean b : r) {
                    sb.append(b ? '1' : '0');
                }
                sb.append('/');
            }
            Bulk.p("verify", sb.toString());
            final byte[][] in = new byte[300][];
            for (int i = 0; i < in.length; i++) {
                in[i] = i == 17 ? null : bytes(i + 1300, 150 + i % 90);
            }
            byte[][] sha = CryptoBatch.sha256(in);
            byte[][] kec = CryptoBatch.keccak256(in);
            byte[] fold = new byte[0];
            for (int i = 0; i < in.length; i++) {
                if (in[i] != null) {
                    byte[] both = new byte[fold.length + 64];
                    System.arraycopy(fold, 0, both, 0, fold.length);
                    System.arraycopy(sha[i], 0, both, fold.length, 32);
                    System.arraycopy(kec[i], 0, both, fold.length + 32, 32);
                    fold = Hash.sha256(both);
                }
            }
            Bulk.p("hashes", Hash.toHex(fold) + (sha[17] == null && kec[17] == null));
            // the packed forms over the same entries, read in place by the kernels
            final int pn = 300;
            byte[] ph = new byte[32 * pn];
            byte[] pc = new byte[64 * pn];
            byte[] pk = new byte[33 * pn];
            byte[] pm = new byte[24 * pn];
            byte[] pes = new byte[64 * pn];
            byte[] pek = new byte[32 * pn];
            byte[] pss = new byte[64 * pn];
            byte[] psk = new byte[32 * pn];
            for (int i = 0; i < pn; i++) {
                byte[] priv = bytes(i % 40 + 1000, 32);
                byte[] h = bytes(i + 1400, 32);
                byte[] m = bytes(i + 1500, 24);
                byte[] ek = Ed25519.publicKey(priv);
                System.arraycopy(h, 0, ph, 32 * i, 32);
                System.arraycopy(Secp256k1.sign(h, priv).toCompact(), 0, pc, 64 * i, 64);
                System.arraycopy(Secp256k1.publicKey(priv, true), 0, pk, 33 * i, 33);
                System.arraycopy(m, 0, pm, 24 * i, 24);
                System.arraycopy(Ed25519.sign(priv, ek, m), 0, pes, 64 * i, 64);
                System.arraycopy(ek, 0, pek, 32 * i, 32);
                System.arraycopy(Schnorr.sign(m, priv, null), 0, pss, 64 * i, 64);
                System.arraycopy(Schnorr.publicKey(priv), 0, psk, 32 * i, 32);
                if (i % 11 == 4) {
                    ph[32 * i] ^= 1;
                    pes[64 * i + 3] ^= 1;
                    pss[64 * i + 3] ^= 1;
                }
            }
            pk[33 * 7 + 5] ^= 1;
            pes[64 * 9 + 63] |= 0x10;
            byte[] r1 = new byte[pn];
            byte[] r2 = new byte[pn];
            byte[] r3 = new byte[pn];
            CryptoBatch.verifySecp256k1(ph, pc, pk, 33, r1, pn);
            CryptoBatch.verifyEd25519(pm, 24, pes, pek, r2, pn);
            CryptoBatch.verifySchnorr(pm, 24, pss, psk, r3, pn);
            Bulk.p("packed.verify", Hash.toHex(Hash.sha256(r1)) + Hash.toHex(Hash.sha256(r2))
                    + Hash.toHex(Hash.sha256(r3)) + r1[0] + r1[4] + r1[7] + r2[9] + r3[15]);
            byte[] level = new byte[32 * pn];
            CryptoBatch.sha256(pm, 24, pn, level);
            byte[] up = new byte[32 * (pn / 2)];
            CryptoBatch.sha256(level, 64, pn / 2, up);
            byte[] kl = new byte[32 * pn];
            CryptoBatch.keccak256(pc, 64, pn, kl);
            Bulk.p("packed.hashes", Hash.toHex(Hash.sha256(up)) + Hash.toHex(Hash.sha256(kl)));
        });
        Bulk.check(name + ".sha2", () -> {
            // every padding boundary of both block sizes, and a key past each block size
            for (final int len : new int[] {0, 1, 55, 56, 63, 64, 65, 111, 112, 127, 128, 129, 200, 1000}) {
                final byte[] in = bytes(len + 900, len);
                Bulk.p("sha256[" + len + "]", run(() -> Hash.sha256(in)));
                Bulk.p("sha512[" + len + "]", run(() -> Hash.sha512(in)));
                Bulk.p("ripemd160[" + len + "]", run(() -> Hash.ripemd160(in)));
                Bulk.p("hmac256[" + len + "]", run(() -> Hmac.sha256(in, bytes(len, 37))));
                Bulk.p("hmac512[" + len + "]", run(() -> Hmac.sha512(in, bytes(len, 37))));
            }
            Bulk.p("pbkdf2-512", run(() -> Pbkdf2.hmacSha512(bytes(1, 48), bytes(2, 14), 2048, 64)));
            Bulk.p("pbkdf2-512-long", run(() -> Pbkdf2.hmacSha512(bytes(3, 200), bytes(4, 300), 3, 150)));
            Bulk.p("pbkdf2-256", run(() -> Pbkdf2.hmacSha256(bytes(5, 70), bytes(6, 16), 1000, 70)));
        });
    }

    /// The layers above the kernels: the long and BigInteger-free arithmetic in
    /// UInt256, the encodings, HD derivation and the chain transaction formats.
    /// Integer division by zero answers 0 on arm64 instead of throwing, so the
    /// division and modulo cases include a zero divisor.
    private static void stack(String name) {
        Bulk.check(name + ".uint256", () -> {
            UInt256 a = UInt256.parse("115792089237316195423570985008687907853269984665640564039457584007908834671663");
            UInt256 b = UInt256.valueOf(0x7fffffffffffffffL);
            Bulk.p("add", run(() -> a.add(b).toHex()));
            Bulk.p("sub", run(() -> b.subtract(a).toHex()));
            Bulk.p("mul", run(() -> a.multiply(b).toHex()));
            Bulk.p("div", run(() -> a.divide(b).toString()));
            Bulk.p("mod", run(() -> a.mod(b).toString()));
            Bulk.p("div0", run(() -> a.divide(UInt256.ZERO).toString()));
            Bulk.p("mod0", run(() -> a.mod(UInt256.ZERO).toString()));
            Bulk.p("units", run(() -> UInt256.parseUnits("1234.000000000000000567", 18).formatUnits(18)));
            Bulk.p("shift", run(() -> a.shiftRight(77).shiftLeft(3).toHex()));
        });
        Bulk.check(name + ".encoding", () -> {
            for (final int len : new int[] {0, 1, 20, 32, 33}) {
                final byte[] in = bytes(len + 900, len);
                if (len > 0) {
                    in[0] = 0;
                }
                Bulk.p("base58[" + len + "]", run(() -> Base58.encode(in)));
                Bulk.p("bech32m[" + len + "]", run(() -> Bech32.encode("bc", in, Bech32.BECH32M)));
            }
            Bulk.p("base58.bad", run(() -> Base58.decode("0OIl")));
        });
        Bulk.check(name + ".hd", () -> {
            final String words = Mnemonic.fromEntropy(bytes(31, 16));
            Bulk.p("mnemonic", words);
            final byte[] seed = Mnemonic.toSeed(words, "TREZOR");
            Bulk.p("seed", run(() -> seed));
            final Bip32 eth = Bip32.master(seed).derive("m/44'/60'/0'/0/7");
            Bulk.p("bip32.xprv", run(() -> eth.toXprv()));
            Bulk.p("bip32.neuter", run(() -> Bip32.master(seed).derive("m/84'/0'/0'").neuter().child(1).toXpub()));
            Bulk.p("slip10", run(() -> Slip10.master(seed).derive("m/44'/501'/0'/0'").publicKey()));
            Bulk.p("bad.mnemonic", run(() -> Mnemonic.toEntropy(words + " abandon")));
        });
        Bulk.check(name + ".chains", () -> {
            final byte[] key = bytes(77, 32);
            final EvmAddress to = EvmAddress.fromPrivateKey(bytes(78, 32));
            Bulk.p("evm.address", run(() -> to.toChecksumString()));
            final EvmTransaction tx = EvmTransaction.eip1559(1, 9, UInt256.parseUnits("2", 9),
                    UInt256.parseUnits("40", 9), 21000, to, UInt256.parseUnits("0.5", 18),
                    Abi.encodeCall("transfer(address,uint256)", to, UInt256.valueOf(5)));
            Bulk.p("evm.signed", run(() -> tx.sign(key).encodeHex()));
            Bulk.p("evm.sender", run(() -> tx.sign(key).sender().toChecksumString()));
            final byte[] pub = Secp256k1.publicKey(key, true);
            Bulk.p("btc.p2wpkh", run(() -> BitcoinAddress.p2wpkh(pub, BitcoinNetwork.MAINNET).toString()));
            final byte[] xOnly = new byte[32];
            System.arraycopy(pub, 1, xOnly, 0, 32);
            Bulk.p("btc.p2tr", run(() -> BitcoinAddress.p2tr(xOnly, BitcoinNetwork.TESTNET).toString()));
            final SolanaAddress from = SolanaAddress.fromPrivateKey(key);
            final java.util.List<Instruction> ix = new java.util.ArrayList<Instruction>();
            ix.add(SystemProgram.transfer(from, SolanaAddress.fromPrivateKey(bytes(79, 32)), 1500000000L));
            Bulk.p("sol.tx", run(() -> {
                SolanaTransaction t = new SolanaTransaction(SolanaMessage.compile(from, ix,
                        Base58.encode(bytes(80, 32))));
                t.sign(key);
                return t.serialize();
            }));
        });
    }
}
