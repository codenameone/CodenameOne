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

import com.codename1.crypto.bitcoin.BitcoinAddress;
import com.codename1.crypto.bitcoin.BitcoinKey;
import com.codename1.crypto.bitcoin.BitcoinNetwork;
import com.codename1.crypto.bitcoin.Psbt;
import com.codename1.crypto.evm.EvmAddress;
import com.codename1.crypto.evm.EvmTransaction;
import com.codename1.crypto.evm.PersonalSign;
import com.codename1.crypto.evm.TypedData;
import com.codename1.crypto.hd.DerivationPath;
import com.codename1.crypto.solana.SolanaAddress;
import com.codename1.crypto.solana.SolanaTransaction;
import com.codename1.security.Secp256k1;
import java.util.Arrays;
import java.util.List;

/// One wallet across chains, over a [Signer]. It knows the standard derivation paths, so the
/// addresses it answers are the ones other wallets restore from the same mnemonic:
///
/// - EVM: `m/44'/60'/account'/0/index`, as MetaMask and Ledger derive
/// - Bitcoin: `m/purpose'/coin'/account'/change/index` with purpose 44 (P2PKH), 49
///   (P2SH-P2WPKH), 84 (P2WPKH) or 86 (P2TR), and the coin type of the network
/// - Solana: `m/44'/501'/account'/0'`, as Phantom and Solflare derive
///
/// ```java
/// String words = Mnemonic.generate(128);
/// Wallet w = Wallet.fromMnemonic(words, "");
/// EvmAddress eth = w.getEvmAddress(0, 0);
/// BitcoinAddress btc = w.getBitcoinAddress(BitcoinNetwork.MAINNET, Wallet.BIP84, 0, 0, 0);
/// SolanaAddress sol = w.getSolanaAddress(0);
/// EvmTransaction tx = EvmTransaction.eip1559(1, 0, UInt256.parseUnits("1", 9),
///         UInt256.parseUnits("30", 9), 21000, eth, UInt256.ZERO, null);
/// EvmTransaction signed = w.signEvmTransaction(tx, Wallet.evmPath(0, 0));
/// ```
@com.codename1.impl.SharedWithBackend
public final class Wallet {
    /// BIP-44 legacy P2PKH accounts.
    public static final int BIP44 = 44;
    /// BIP-49 P2SH-wrapped SegWit accounts.
    public static final int BIP49 = 49;
    /// BIP-84 native SegWit accounts.
    public static final int BIP84 = 84;
    /// BIP-86 Taproot accounts.
    public static final int BIP86 = 86;

    private static final long H = DerivationPath.HARDENED;

    private final Signer signer;

    /// A wallet over a signer.
    public Wallet(Signer signer) {
        if (signer == null) {
            throw new IllegalArgumentException("signer");
        }
        this.signer = signer;
    }

    /// A wallet over an in-memory [SeedSigner] for a mnemonic.
    public static Wallet fromMnemonic(String mnemonic, String passphrase) {
        return new Wallet(SeedSigner.fromMnemonic(mnemonic, passphrase));
    }

    /// The signer.
    public Signer getSigner() {
        return signer;
    }

    // ------------------------------------------------------------------ EVM

    /// `m/44'/60'/account'/0/index`.
    public static DerivationPath evmPath(int account, int index) {
        return DerivationPath.of(new long[] {44 | H, 60 | H, account | H, 0, index & 0xffffffffL});
    }

    /// The EVM address at an account and index.
    public EvmAddress getEvmAddress(int account, int index) {
        return getEvmAddress(evmPath(account, index));
    }

    /// The EVM address at a path.
    public EvmAddress getEvmAddress(DerivationPath path) {
        return EvmAddress.fromPublicKey(signer.getSecp256k1PublicKey(path));
    }

    /// Signs a transaction with the key at a path, answering the signed transaction.
    public EvmTransaction signEvmTransaction(EvmTransaction tx, DerivationPath path) {
        return tx.withSignature(sig65(signer.signSecp256k1(path, tx.signingHash()), 0));
    }

    /// An EIP-191 `personal_sign` signature, `r || s || v` with v 27 or 28.
    public byte[] personalSign(byte[] message, DerivationPath path) {
        return sig65(signer.signSecp256k1(path, PersonalSign.hash(message)), 27);
    }

    /// An EIP-191 signature of UTF-8 text.
    public byte[] personalSign(String message, DerivationPath path) {
        return sig65(signer.signSecp256k1(path, PersonalSign.hash(message)), 27);
    }

    /// An EIP-712 signature, `r || s || v` with v 27 or 28.
    public byte[] signTypedData(TypedData data, DerivationPath path) {
        return sig65(signer.signSecp256k1(path, data.hash()), 27);
    }

    private static byte[] sig65(Secp256k1.EcdsaSignature s, int vBase) {
        byte[] out = new byte[65];
        System.arraycopy(s.r(), 0, out, 0, 32);
        System.arraycopy(s.s(), 0, out, 32, 32);
        out[64] = (byte) (vBase + s.recoveryId());
        return out;
    }

    // -------------------------------------------------------------- Bitcoin

    /// `m/purpose'/coin'/account'/change/index` for a network.
    public static DerivationPath bitcoinPath(BitcoinNetwork network, int purpose, int account, int change,
            int index) {
        return DerivationPath.of(new long[] {purpose | H, network.getCoinType() | H, account | H, change,
            index & 0xffffffffL});
    }

    /// The address of the output type a purpose implies, at a path under it.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for a purpose other than 44, 49, 84 or 86
    public BitcoinAddress getBitcoinAddress(BitcoinNetwork network, int purpose, int account, int change,
            int index) {
        byte[] pub = signer.getSecp256k1PublicKey(bitcoinPath(network, purpose, account, change, index));
        switch (purpose) {
            case BIP44:
                return BitcoinAddress.p2pkh(pub, network);
            case BIP49:
                return BitcoinAddress.p2shP2wpkh(pub, network);
            case BIP84:
                return BitcoinAddress.p2wpkh(pub, network);
            case BIP86:
                byte[] x = new byte[32];
                System.arraycopy(pub, 1, x, 0, 32);
                return BitcoinAddress.p2tr(x, network);
            default:
                throw new IllegalArgumentException("unknown purpose " + purpose);
        }
    }

    /// The key at a path, for [Psbt#sign(int, BitcoinKey)]; it signs through the signer.
    public BitcoinKey getBitcoinKey(DerivationPath path) {
        return new PathKey(signer, path);
    }

    /// Signs every PSBT input whose recorded BIP-32 origin is this wallet's master, answering
    /// how many inputs were signed. The PSBT's creator records the origins
    /// ([Psbt#addBip32Derivation], [Psbt#addTapBip32Derivation]), as every wallet does.
    public int signPsbt(Psbt psbt) {
        int fp = signer.getMasterFingerprint();
        int signed = 0;
        int inputs = psbt.getTransaction().getInputs().size();
        for (int i = 0; i < inputs; i++) {
            List<Psbt.Derivation> origins = psbt.getBip32Derivations(i);
            for (Psbt.Derivation d : origins) {
                if (d.getMasterFingerprint() != fp) {
                    continue;
                }
                BitcoinKey key = getBitcoinKey(d.getPath());
                byte[] want = d.getPublicKey();
                byte[] have = want.length == 32 ? key.getInternalKey() : key.getPublicKey();
                if (Arrays.equals(want, have) && psbt.sign(i, key)) {
                    signed++;
                    break;
                }
            }
        }
        return signed;
    }

    private static final class PathKey extends BitcoinKey {
        private final Signer signer;
        private final DerivationPath path;
        private byte[] pub;

        PathKey(Signer signer, DerivationPath path) {
            this.signer = signer;
            this.path = path;
        }

        @Override
        public byte[] getPublicKey() {
            if (pub == null) {
                pub = signer.getSecp256k1PublicKey(path);
            }
            byte[] copy = new byte[pub.length];
            System.arraycopy(pub, 0, copy, 0, pub.length);
            return copy;
        }

        @Override
        public Secp256k1.EcdsaSignature signEcdsa(byte[] sighash) {
            return signer.signSecp256k1(path, sighash);
        }

        @Override
        public byte[] signTaproot(byte[] sighash, byte[] merkleRoot) {
            return signer.signTaproot(path, sighash, merkleRoot);
        }
    }

    // --------------------------------------------------------------- Solana

    /// `m/44'/501'/account'/0'`.
    public static DerivationPath solanaPath(int account) {
        return DerivationPath.of(new long[] {44 | H, 501 | H, account | H, H});
    }

    /// The Solana address of an account.
    public SolanaAddress getSolanaAddress(int account) {
        return getSolanaAddress(solanaPath(account));
    }

    /// The Solana address at a path.
    public SolanaAddress getSolanaAddress(DerivationPath path) {
        return SolanaAddress.fromBytes(signer.getEd25519PublicKey(path));
    }

    /// Adds the signature of the key at a path to a transaction it must sign.
    public void signSolanaTransaction(SolanaTransaction tx, DerivationPath path) {
        tx.addSignature(getSolanaAddress(path), signer.signEd25519(path, tx.getMessageBytes()));
    }

    /// Adds the signature of an account's key.
    public void signSolanaTransaction(SolanaTransaction tx, int account) {
        signSolanaTransaction(tx, solanaPath(account));
    }
}
