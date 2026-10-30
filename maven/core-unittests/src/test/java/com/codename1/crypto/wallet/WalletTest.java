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
import com.codename1.crypto.bitcoin.BitcoinNetwork;
import com.codename1.crypto.bitcoin.BitcoinTransaction;
import com.codename1.crypto.bitcoin.Psbt;
import com.codename1.crypto.evm.EvmAddress;
import com.codename1.crypto.evm.EvmTransaction;
import com.codename1.crypto.evm.PersonalSign;
import com.codename1.crypto.hd.Bip32;
import com.codename1.crypto.hd.DerivationPath;
import com.codename1.crypto.hd.Mnemonic;
import com.codename1.crypto.solana.SolanaMessage;
import com.codename1.crypto.solana.SolanaTransaction;
import com.codename1.crypto.solana.SystemProgram;
import com.codename1.crypto.solana.Instruction;
import com.codename1.crypto.UInt256;
import com.codename1.junit.UITestBase;
import com.codename1.security.vault.KdfProfile;
import com.codename1.security.vault.Vault;
import com.codename1.security.vault.VaultOptions;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// The wallet facade against the addresses other wallets derive from the BIP-39 test mnemonic.
class WalletTest extends UITestBase {
    private static final String WORDS = "abandon abandon abandon abandon abandon abandon abandon abandon "
            + "abandon abandon abandon about";

    @Test
    void addressesMatchOtherWallets() {
        Wallet w = Wallet.fromMnemonic(WORDS, "");
        assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", w.getEvmAddress(0, 0).toChecksumString());
        BitcoinNetwork n = BitcoinNetwork.MAINNET;
        assertEquals("1LqBGSKuX5yYUonjxT5qGfpUsXKYYWeabA", w.getBitcoinAddress(n, Wallet.BIP44, 0, 0, 0).toString());
        assertEquals("37VucYSaXLCAsxYyAPfbSi9eh4iEcbShgf", w.getBitcoinAddress(n, Wallet.BIP49, 0, 0, 0).toString());
        assertEquals("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu",
                w.getBitcoinAddress(n, Wallet.BIP84, 0, 0, 0).toString());
        assertEquals("bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr",
                w.getBitcoinAddress(n, Wallet.BIP86, 0, 0, 0).toString());
        assertEquals("HAgk14JpMQLgt6rVgv7cBQFJWFto5Dqxi472uT3DKpqk", w.getSolanaAddress(0).toString());
    }

    @Test
    void evmSigning() {
        Wallet w = Wallet.fromMnemonic(WORDS, "");
        DerivationPath p = Wallet.evmPath(0, 0);
        EvmAddress me = w.getEvmAddress(p);
        EvmTransaction tx = EvmTransaction.eip1559(1, 0, UInt256.valueOf(1000000000L), UInt256.valueOf(30000000000L),
                21000, me, UInt256.valueOf(1), new byte[0]);
        EvmTransaction signed = w.signEvmTransaction(tx, p);
        assertEquals(me, signed.sender());
        byte[] sig = w.personalSign("hello", p);
        assertTrue(PersonalSign.verify("hello", sig, me));
    }

    @Test
    void psbtSignedByOrigin() {
        Wallet w = Wallet.fromMnemonic(WORDS, "");
        BitcoinNetwork n = BitcoinNetwork.TESTNET;
        DerivationPath segwit = Wallet.bitcoinPath(n, Wallet.BIP84, 0, 0, 3);
        DerivationPath taproot = Wallet.bitcoinPath(n, Wallet.BIP86, 0, 1, 0);
        BitcoinAddress a = w.getBitcoinAddress(n, Wallet.BIP84, 0, 0, 3);
        BitcoinAddress b = w.getBitcoinAddress(n, Wallet.BIP86, 0, 1, 0);
        BitcoinTransaction t = new BitcoinTransaction();
        t.addInput("75ddabb27b8845f5247975c8a5ba7c6f336c4570708ebe230caf6db5217ae858", 0);
        t.addInput("75ddabb27b8845f5247975c8a5ba7c6f336c4570708ebe230caf6db5217ae858", 1);
        t.addOutput(150000L, a);
        Psbt p = Psbt.fromTransaction(t);
        p.setWitnessUtxo(0, new BitcoinTransaction.Output(100000L, a.getScriptPubKey()));
        p.setWitnessUtxo(1, new BitcoinTransaction.Output(60000L, b.getScriptPubKey()));
        int fp = w.getSigner().getMasterFingerprint();
        p.addBip32Derivation(0, w.getSigner().getSecp256k1PublicKey(segwit), fp, indices(segwit));
        byte[] tapPub = w.getSigner().getSecp256k1PublicKey(taproot);
        byte[] x = new byte[32];
        System.arraycopy(tapPub, 1, x, 0, 32);
        p.addTapBip32Derivation(1, x, fp, indices(taproot));
        Psbt q = Psbt.fromBase64(p.toBase64());
        assertEquals(2, q.getBip32Derivations(0).size() + q.getBip32Derivations(1).size());
        assertEquals(2, w.signPsbt(q));
        byte[] walletSig = q.getPartialSignatures(0).get(0)[1];
        assertTrue(q.finalizeAll());
        BitcoinTransaction done = q.extract();
        assertEquals(t.getTxid(), done.getTxid());
        assertEquals(2, done.getInputs().get(0).getWitness().size());
        assertEquals(64, done.getInputs().get(1).getWitness().get(0).length);

        // the same input signed with the raw key gives the same deterministic signature
        Psbt r = Psbt.fromBase64(p.toBase64());
        Bip32 k = Bip32.master(Mnemonic.toSeed(WORDS, "")).derive(segwit);
        assertTrue(r.sign(0, k.privateKey()));
        assertArrayEquals(walletSig, r.getPartialSignatures(0).get(0)[1]);

        byte[] other = new byte[16];
        other[0] = 1;
        Wallet stranger = Wallet.fromMnemonic(Mnemonic.fromEntropy(other), "");
        assertNotEquals(fp, stranger.getSigner().getMasterFingerprint());
        assertEquals(0, stranger.signPsbt(Psbt.fromBase64(p.toBase64())));
    }

    private static long[] indices(DerivationPath p) {
        long[] out = new long[p.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = p.get(i);
        }
        return out;
    }

    @Test
    void solanaSigning() {
        Wallet w = Wallet.fromMnemonic(WORDS, "");
        List<Instruction> ixs = new ArrayList<Instruction>();
        ixs.add(SystemProgram.transfer(w.getSolanaAddress(0), w.getSolanaAddress(1), 5000));
        SolanaTransaction tx = new SolanaTransaction(SolanaMessage.compile(w.getSolanaAddress(0), ixs,
                "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k"));
        w.signSolanaTransaction(tx, 0);
        assertTrue(tx.isFullySigned());
    }

    @Test
    void vaultSignerSignsOnlyWhileUnlocked() {
        VaultOptions fast = new VaultOptions().kdf(KdfProfile.pbkdf2(KdfProfile.MIN_ITERATIONS));
        Vault vault = Vault.named("wallet" + System.nanoTime()).configure(fast);
        vault.enroll("pw".toCharArray(), fast).get();
        VaultSigner.store(vault, "main", WORDS, "").get();
        Wallet w = new Wallet(new VaultSigner(vault, "main"));
        assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", w.getEvmAddress(0, 0).toChecksumString());
        assertEquals(WORDS, new String(new VaultSigner(vault, "main").revealMnemonic().get()));
        vault.lock();
        assertThrows(IllegalStateException.class, () -> w.getSolanaAddress(0));
        vault.unlockWithPassword("pw".toCharArray()).get();
        assertEquals("HAgk14JpMQLgt6rVgv7cBQFJWFto5Dqxi472uT3DKpqk", w.getSolanaAddress(0).toString());
    }

    @Test
    void destroyedSignerRefuses() {
        SeedSigner s = SeedSigner.fromMnemonic(WORDS, "");
        s.destroy();
        assertThrows(IllegalStateException.class, () -> s.getEd25519PublicKey(Wallet.solanaPath(0)));
    }
}
