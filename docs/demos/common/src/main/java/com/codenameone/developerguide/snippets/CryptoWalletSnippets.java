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
import com.codename1.security.vault.Vault;
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
    public String[] addresses(Wallet wallet) {
        // tag::crypto-addresses[]
        EvmAddress eth = wallet.getEvmAddress(0, 0);            // m/44'/60'/0'/0/0
        BitcoinAddress btc = wallet.getBitcoinAddress(BitcoinNetwork.MAINNET,
                Wallet.BIP84, 0, 0, 0);                         // m/84'/0'/0'/0/0, bc1q...
        SolanaAddress sol = wallet.getSolanaAddress(0);         // m/44'/501'/0'/0'

        String shown = eth.toChecksumString();                  // EIP-55 mixed case
        // end::crypto-addresses[]
        return new String[] {shown, btc.toString(), sol.toString()};
    }

    /** Sends ether: nonce, fees and gas from the node, signed on the device. */
    public String sendEther(Wallet wallet, String recipient) throws IOException {
        // tag::crypto-evm-send[]
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
    public String sendBitcoin(Wallet wallet, String recipient, long feeSats) throws IOException {
        // tag::crypto-btc-send[]
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
    public String sendSol(Wallet wallet, String recipient) throws IOException {
        // tag::crypto-sol-send[]
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
    public double priced(UInt256 weiBalance) throws IOException {
        // tag::crypto-price[]
        PriceFeed prices = new CachingPriceFeed(new CoinGeckoFeed(new NetworkTransport()),
                60000, 600000);                                 // fresh 1 min, usable 10 min
        Quote q = prices.getQuote("ETH", "USD");
        double usd = q.valueOf(weiBalance, 18);
        // end::crypto-price[]
        return usd;
    }

    /** Keeps the recovery phrase in a vault instead of in memory. */
    public Wallet fromVault(Vault vault, String words) {
        // tag::crypto-vault[]
        // Once, when the wallet is created or restored:
        VaultSigner.store(vault, "main", words, "");

        // Every later launch: the phrase is opened only while the vault is unlocked.
        Wallet wallet = new Wallet(new VaultSigner(vault, "main"));
        // end::crypto-vault[]
        return wallet;
    }
}
