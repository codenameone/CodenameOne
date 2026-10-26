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
package com.codename1.crypto.rpc;

import com.codename1.crypto.solana.SolanaAddress;
import com.codename1.crypto.solana.SolanaTransaction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A Solana JSON-RPC client: balances, the recent blockhash a transaction needs, rent,
/// token balances, sending and confirmation.
///
/// ```java
/// SolanaRpc sol = new SolanaRpc(new NetworkTransport(), SolanaRpc.DEVNET);
/// SolanaMessage m = SolanaMessage.compile(me, instructions, sol.getLatestBlockhash());
/// SolanaTransaction tx = new SolanaTransaction(m);
/// wallet.signSolanaTransaction(tx, 0);
/// String signature = sol.sendTransaction(tx);
/// ```
///
/// Reads use the commitment given to the constructor; `confirmed` by default.
@com.codename1.impl.SharedWithBackend
public class SolanaRpc extends JsonRpc {
    /// The public mainnet-beta endpoint, rate limited.
    public static final String MAINNET = "https://api.mainnet-beta.solana.com";
    /// The public devnet endpoint.
    public static final String DEVNET = "https://api.devnet.solana.com";
    /// The public testnet endpoint.
    public static final String TESTNET = "https://api.testnet.solana.com";

    private final String commitment;

    /// A client reading at `confirmed`.
    public SolanaRpc(RpcTransport transport, String url) {
        this(transport, url, "confirmed");
    }

    /// A client reading at a commitment: `processed`, `confirmed` or `finalized`.
    public SolanaRpc(RpcTransport transport, String url, String commitment) {
        super(transport, url);
        this.commitment = commitment;
    }

    private Map<String, Object> config() {
        Map<String, Object> c = new LinkedHashMap<String, Object>();
        c.put("commitment", commitment);
        return c;
    }

    private static Object value(Object result, String what) throws IOException {
        Map<?, ?> m = asMap(result, what);
        if (!m.containsKey("value")) {
            throw new IOException(what + ": no value");
        }
        return m.get("value");
    }

    /// `getBalance` in lamports.
    public long getBalance(SolanaAddress address) throws IOException {
        return asLong(value(call("getBalance", address.toString(), config()), "getBalance"), "getBalance");
    }

    /// `getLatestBlockhash`: the Base58 blockhash for a new message.
    public String getLatestBlockhash() throws IOException {
        Map<?, ?> v = asMap(value(call("getLatestBlockhash", config()), "getLatestBlockhash"), "getLatestBlockhash");
        return asString(v.get("blockhash"), "blockhash");
    }

    /// The block height after which a transaction using the latest blockhash expires.
    public long getLastValidBlockHeight() throws IOException {
        Map<?, ?> v = asMap(value(call("getLatestBlockhash", config()), "getLatestBlockhash"), "getLatestBlockhash");
        return asLong(v.get("lastValidBlockHeight"), "lastValidBlockHeight");
    }

    /// `getMinimumBalanceForRentExemption` for an account of `space` bytes.
    public long getMinimumBalanceForRentExemption(long space) throws IOException {
        return asLong(call("getMinimumBalanceForRentExemption", Long.valueOf(space), config()),
                "getMinimumBalanceForRentExemption");
    }

    /// `getTokenAccountBalance`: the raw amount in base units, as the decimal string the
    /// node answers (a token amount may exceed a signed `long`).
    public String getTokenAccountBalance(SolanaAddress tokenAccount) throws IOException {
        Map<?, ?> v = asMap(value(call("getTokenAccountBalance", tokenAccount.toString(), config()),
                "getTokenAccountBalance"), "getTokenAccountBalance");
        return asString(v.get("amount"), "amount");
    }

    /// Whether an account exists, which a sender checks before paying to create the
    /// recipient's token account.
    public boolean accountExists(SolanaAddress address) throws IOException {
        Map<String, Object> c = config();
        c.put("encoding", "base64");
        return value(call("getAccountInfo", address.toString(), c), "getAccountInfo") != null;
    }

    /// `sendTransaction` with preflight at this client's commitment, answering the signature
    /// (the transaction id).
    public String sendTransaction(SolanaTransaction signed) throws IOException {
        Map<String, Object> c = new LinkedHashMap<String, Object>();
        c.put("encoding", "base64");
        c.put("preflightCommitment", commitment);
        return asString(call("sendTransaction", signed.toBase64(), c), "sendTransaction");
    }

    /// `getSignatureStatuses` for one signature: `null` when the cluster has not seen it,
    /// otherwise its `confirmationStatus` (`processed`, `confirmed`, `finalized`), or
    /// `"failed"` when it landed with an error.
    public String getSignatureStatus(String signature) throws IOException {
        List<Object> sigs = new ArrayList<Object>();
        sigs.add(signature);
        Map<String, Object> c = new LinkedHashMap<String, Object>();
        c.put("searchTransactionHistory", Boolean.TRUE);
        List<?> v = asList(value(call("getSignatureStatuses", sigs, c), "getSignatureStatuses"),
                "getSignatureStatuses");
        if (v.isEmpty() || v.get(0) == null) {
            return null;
        }
        Map<?, ?> s = asMap(v.get(0), "status");
        if (s.get("err") != null) {
            return "failed";
        }
        Object st = s.get("confirmationStatus");
        return st instanceof String ? (String) st : "processed";
    }

    /// `requestAirdrop` on devnet or testnet, answering the airdrop's signature.
    public String requestAirdrop(SolanaAddress to, long lamports) throws IOException {
        return asString(call("requestAirdrop", to.toString(), Long.valueOf(lamports)), "requestAirdrop");
    }
}
