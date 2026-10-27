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
package com.codenameone.developerguide.snippets;

import com.codename1.crypto.UInt256;
import com.codename1.crypto.bitcoin.BitcoinAddress;
import com.codename1.crypto.bitcoin.BitcoinNetwork;
import com.codename1.crypto.bitcoin.BitcoinTransaction;
import com.codename1.crypto.bitcoin.Psbt;
import com.codename1.crypto.evm.EvmAddress;
import com.codename1.crypto.evm.EvmTransaction;
import com.codename1.crypto.hd.DerivationPath;
import com.codename1.crypto.hd.Mnemonic;
import com.codename1.crypto.market.CachingPriceFeed;
import com.codename1.crypto.market.CoinGeckoFeed;
import com.codename1.crypto.market.PriceFeed;
import com.codename1.crypto.market.Quote;
import com.codename1.crypto.rpc.EsploraClient;
import com.codename1.crypto.rpc.EvmRpc;
import com.codename1.crypto.rpc.NetworkTransport;
import com.codename1.crypto.rpc.SolanaRpc;
import com.codename1.crypto.solana.Instruction;
import com.codename1.crypto.solana.SolanaAddress;
import com.codename1.crypto.solana.SolanaMessage;
import com.codename1.crypto.solana.SolanaTransaction;
import com.codename1.crypto.solana.SystemProgram;
import com.codename1.crypto.wallet.VaultSigner;
import com.codename1.crypto.wallet.Wallet;
import com.codename1.security.CryptoBatch;
import com.codename1.security.Secp256k1;
import com.codename1.security.vault.Vault;
import com.codename1.ui.Label;
import com.codename1.util.Simd;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Compiled source snippets for the Crypto wallets guide chapter. */
public class CryptoWalletSnippets {

    /** A new recovery phrase, and the wallet it opens. */
    public Wallet create() {
        // tag::crypto-mnemonic[]
        // 24 words; 128 gives 12.
        String words = Mnemonic.generate(256);

        // Words typed back in by the user are checked before anything is derived from them.
        if (!Mnemonic.isValid(words)) {
            throw new IllegalArgumentException("not a valid recovery phrase");
        }
        // The passphrase is the optional "25th word"; "" when the user set none.
        Wallet wallet = Wallet.fromMnemonic(words, "");
        // end::crypto-mnemonic[]
        return wallet;
    }

    /** One address per chain, on the standard derivation paths. */
    public String[] addresses() {
        // tag::crypto-addresses[]
        Wallet wallet = Wallet.fromMnemonic(Mnemonic.generate(128), "");
        EvmAddress eth = wallet.getEvmAddress(0, 0);            // m/44'/60'/0'/0/0
        BitcoinAddress btc = wallet.getBitcoinAddress(BitcoinNetwork.MAINNET,
                Wallet.BIP84, 0, 0, 0);                         // m/84'/0'/0'/0/0, bc1q...
        SolanaAddress sol = wallet.getSolanaAddress(0);         // m/44'/501'/0'/0'

        String shown = eth.toChecksumString();                  // EIP-55 mixed case
        // end::crypto-addresses[]
        return new String[] {shown, btc.toString(), sol.toString()};
    }

    /** Sends ether: nonce, fees and gas from the node, signed on the device. */
    public String sendEther() throws IOException {
        // tag::crypto-evm-send[]
        Wallet wallet = Wallet.fromMnemonic(Mnemonic.generate(128), "");
        String recipient = "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed";
        EvmRpc rpc = new EvmRpc(new NetworkTransport(), "https://ethereum-sepolia-rpc.publicnode.com");
        DerivationPath path = Wallet.evmPath(0, 0);
        EvmAddress from = wallet.getEvmAddress(path);
        EvmAddress to = EvmAddress.parse(recipient);
        UInt256 value = UInt256.parseUnits("0.01", 18);

        long nonce = rpc.getTransactionCount(from, "pending");
        UInt256[] fees = rpc.suggestFees();                     // {tip, fee cap}
        long gas = rpc.estimateGas(from, to, value, null);

        EvmTransaction tx = EvmTransaction.eip1559(rpc.chainId(), nonce, fees[0], fees[1],
                gas, to, value, null);
        String hash = rpc.sendRawTransaction(wallet.signEvmTransaction(tx, path));
        // end::crypto-evm-send[]
        return hash;
    }

    /** Spends one native SegWit coin through a PSBT. */
    public String sendBitcoin() throws IOException {
        // tag::crypto-btc-send[]
        Wallet wallet = Wallet.fromMnemonic(Mnemonic.generate(128), "");
        String recipient = "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx";
        long feeSats = 1500;
        BitcoinNetwork net = BitcoinNetwork.TESTNET;
        EsploraClient esplora = new EsploraClient(new NetworkTransport(), EsploraClient.MEMPOOL_TESTNET);
        DerivationPath path = Wallet.bitcoinPath(net, Wallet.BIP84, 0, 0, 0);
        BitcoinAddress from = wallet.getBitcoinAddress(net, Wallet.BIP84, 0, 0, 0);

        EsploraClient.Utxo coin = esplora.getUtxos(from).get(0);
        BitcoinTransaction tx = new BitcoinTransaction();
        tx.addInput(coin.getTxid(), coin.getVout());
        tx.addOutput(coin.getValue() - feeSats, BitcoinAddress.parse(recipient, net));

        // The PSBT carries what a signer needs to know about the coin being spent.
        Psbt psbt = Psbt.fromTransaction(tx);
        psbt.setWitnessUtxo(0, coin.toOutput());
        psbt.signAll(wallet.getBitcoinKey(path));
        psbt.finalizeAll();
        String txid = esplora.broadcast(psbt.extract());
        // end::crypto-btc-send[]
        return txid;
    }

    /** Sends SOL with a recent blockhash. */
    public String sendSol() throws IOException {
        // tag::crypto-sol-send[]
        Wallet wallet = Wallet.fromMnemonic(Mnemonic.generate(128), "");
        String recipient = "9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM";
        SolanaRpc rpc = new SolanaRpc(new NetworkTransport(), SolanaRpc.DEVNET);
        SolanaAddress from = wallet.getSolanaAddress(0);

        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(SystemProgram.transfer(from, SolanaAddress.parse(recipient), 10000000L));
        SolanaMessage message = SolanaMessage.compile(from, instructions, rpc.getLatestBlockhash());

        SolanaTransaction tx = new SolanaTransaction(message);
        wallet.signSolanaTransaction(tx, 0);
        String signature = rpc.sendTransaction(tx);
        // end::crypto-sol-send[]
        return signature;
    }

    /** A price, cached so a list of balances asks once. */
    public double priced() throws IOException {
        // tag::crypto-price[]
        UInt256 weiBalance = UInt256.parseUnits("1.5", 18);
        PriceFeed prices = new CachingPriceFeed(new CoinGeckoFeed(new NetworkTransport()),
                60000, 600000);                                 // fresh 1 min, usable 10 min
        Quote q = prices.getQuote("ETH", "USD");
        double usd = q.valueOf(weiBalance, 18);
        // end::crypto-price[]
        return usd;
    }

    /** Keeps the recovery phrase in a vault instead of in memory. */
    public Wallet fromVault() {
        // tag::crypto-vault[]
        Vault vault = Vault.named("wallet");
        String words = Mnemonic.generate(256);
        // Once, when the wallet is created or restored:
        VaultSigner.store(vault, "main", words, "");

        // Every later launch: the phrase is opened only while the vault is unlocked.
        Wallet wallet = new Wallet(new VaultSigner(vault, "main"));
        // end::crypto-vault[]
        return wallet;
    }

    /** Checks every signature of a block of deposits in one call. */
    public int countValid() {
        // tag::crypto-batch[]
        // A block's deposits; filled from the transactions being scanned.
        byte[][] hashes = new byte[0][];
        Secp256k1.EcdsaSignature[] signatures = new Secp256k1.EcdsaSignature[0];
        byte[][] publicKeys = new byte[0][];
        boolean[] valid = CryptoBatch.verifySecp256k1(hashes, signatures, publicKeys);
        int good = 0;
        for (boolean v : valid) {
            if (v) {
                good++;
            }
        }
        // end::crypto-batch[]
        return good;
    }

    /** Scans a block without blocking the EDT, and shows the count when it is done. */
    public void countValidAsync() {
        // tag::crypto-batch-async[]
        byte[][] hashes = new byte[0][];
        Secp256k1.EcdsaSignature[] signatures = new Secp256k1.EcdsaSignature[0];
        byte[][] publicKeys = new byte[0][];
        final Label status = new Label("Scanning...");
        CryptoBatch.verifySecp256k1Async(hashes, signatures, publicKeys, new CryptoBatch.Callback<boolean[]>() {
            public void done(boolean[] valid, Throwable error) {
                // on the EDT: safe to update the UI
                if (error != null) {
                    status.setText("Scan failed: " + error.getMessage());
                    return;
                }
                int good = 0;
                for (boolean v : valid) {
                    if (v) {
                        good++;
                    }
                }
                status.setText(good + " of " + valid.length + " deposits verified");
            }
        });
        // end::crypto-batch-async[]
    }

    /** The packed form: fixed-stride arrays, filled in place, one result byte per entry. */
    public void verifyPacked() {
        // tag::crypto-batch-packed[]
        int count = 1024;
        Simd simd = Simd.get();
        byte[] hashes = simd.allocByte(32 * count);
        byte[] signatures = simd.allocByte(64 * count);
        byte[] publicKeys = simd.allocByte(33 * count);
        byte[] results = simd.allocByte(count);
        // ... fill entry i at 32 * i, 64 * i and 33 * i ...
        CryptoBatch.verifySecp256k1(hashes, signatures, publicKeys, 33, results, count);
        // results[i] is 1 for a valid entry and 0 for any other
        // end::crypto-batch-packed[]
    }
}
