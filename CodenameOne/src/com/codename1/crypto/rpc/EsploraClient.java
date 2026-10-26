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

import com.codename1.crypto.bitcoin.BitcoinAddress;
import com.codename1.crypto.bitcoin.BitcoinTransaction;
import com.codename1.crypto.encoding.Json;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A Bitcoin client for the Esplora REST API, which Blockstream and mempool.space run in public
/// and anyone can self-host: unspent outputs, balances, fee estimates and broadcasting.
///
/// ```java
/// EsploraClient btc = new EsploraClient(new NetworkTransport(), EsploraClient.MEMPOOL_TESTNET);
/// List<EsploraClient.Utxo> coins = btc.getUtxos(address);
/// double satPerVbyte = btc.getFeeEstimates().get("6");
/// String txid = btc.broadcast(signed);
/// ```
@com.codename1.impl.SharedWithBackend
public class EsploraClient {
    /// mempool.space, mainnet.
    public static final String MEMPOOL_MAINNET = "https://mempool.space/api";
    /// mempool.space, testnet4.
    public static final String MEMPOOL_TESTNET = "https://mempool.space/testnet4/api";
    /// mempool.space, signet.
    public static final String MEMPOOL_SIGNET = "https://mempool.space/signet/api";
    /// Blockstream, mainnet.
    public static final String BLOCKSTREAM_MAINNET = "https://blockstream.info/api";

    private final RpcTransport transport;
    private final String base;

    /// A client for an Esplora base URL, such as [#MEMPOOL_MAINNET].
    public EsploraClient(RpcTransport transport, String baseUrl) {
        if (transport == null || baseUrl == null) {
            throw new IllegalArgumentException("transport and baseUrl are required");
        }
        this.transport = transport;
        this.base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String get(String path) throws IOException {
        return transport.send("GET", base + path, null, null);
    }

    private Object getJson(String path) throws IOException {
        String body = get(path);
        try {
            return Json.parse(body);
        } catch (IllegalArgumentException e) {
            throw new IOException("not JSON from " + base + path, e);
        }
    }

    /// The unspent outputs paying an address, confirmed and in the mempool.
    public List<Utxo> getUtxos(BitcoinAddress address) throws IOException {
        List<?> list = JsonRpc.asList(getJson("/address/" + address + "/utxo"), "utxo");
        List<Utxo> out = new ArrayList<Utxo>();
        for (Object listItem : list) {
            Map<?, ?> u = JsonRpc.asMap(listItem, "utxo");
            Map<?, ?> status = u.get("status") instanceof Map ? (Map<?, ?>) u.get("status") : null;
            boolean confirmed = status != null && Boolean.TRUE.equals(status.get("confirmed"));
            long height = confirmed && status.get("block_height") instanceof Long
                    ? ((Long) status.get("block_height")).longValue() : -1;
            out.add(new Utxo(JsonRpc.asString(u.get("txid"), "txid"), JsonRpc.asLong(u.get("vout"), "vout"),
                    JsonRpc.asLong(u.get("value"), "value"), confirmed, height, address));
        }
        return out;
    }

    /// The balance of an address in satoshis: `{confirmed, unconfirmed}`, the second the net
    /// effect of mempool transactions and possibly negative.
    public long[] getBalance(BitcoinAddress address) throws IOException {
        Map<?, ?> a = JsonRpc.asMap(getJson("/address/" + address), "address");
        return new long[] {net(a.get("chain_stats")), net(a.get("mempool_stats"))};
    }

    private static long net(Object stats) throws IOException {
        Map<?, ?> s = JsonRpc.asMap(stats, "stats");
        return JsonRpc.asLong(s.get("funded_txo_sum"), "funded_txo_sum")
                - JsonRpc.asLong(s.get("spent_txo_sum"), "spent_txo_sum");
    }

    /// Fee estimates in sat/vB keyed by confirmation target in blocks (`"1"`, `"3"`, `"6"`,
    /// `"144"` and the others the server answers).
    public Map<String, Double> getFeeEstimates() throws IOException {
        Map<?, ?> m = JsonRpc.asMap(getJson("/fee-estimates"), "fee-estimates");
        Map<String, Double> out = new LinkedHashMap<String, Double>();
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (e.getValue() instanceof Number) {
                out.put(String.valueOf(e.getKey()), Double.valueOf(((Number) e.getValue()).doubleValue()));
            }
        }
        return out;
    }

    /// The height of the chain tip.
    public long getTipHeight() throws IOException {
        String s = get("/blocks/tip/height").trim();
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new IOException("not a height: " + s, e);
        }
    }

    /// A transaction by id.
    public BitcoinTransaction getTransaction(String txid) throws IOException {
        return BitcoinTransaction.fromHex(get("/tx/" + txid + "/hex").trim());
    }

    /// Whether a transaction is confirmed, and at what height: `-1` while it is in the
    /// mempool.
    public long getConfirmationHeight(String txid) throws IOException {
        Map<?, ?> s = JsonRpc.asMap(getJson("/tx/" + txid + "/status"), "status");
        if (!Boolean.TRUE.equals(s.get("confirmed"))) {
            return -1;
        }
        return JsonRpc.asLong(s.get("block_height"), "block_height");
    }

    /// Broadcasts a signed transaction, answering its txid.
    public String broadcast(BitcoinTransaction signed) throws IOException {
        return transport.send("POST", base + "/tx", "text/plain", signed.toHex()).trim();
    }

    /// An unspent output.
    public static final class Utxo {
        private final String txid;
        private final long vout;
        private final long value;
        private final boolean confirmed;
        private final long height;
        private final BitcoinAddress address;

        Utxo(String txid, long vout, long value, boolean confirmed, long height, BitcoinAddress address) {
            this.txid = txid;
            this.vout = vout;
            this.value = value;
            this.confirmed = confirmed;
            this.height = height;
            this.address = address;
        }

        /// The id of the transaction that created the output.
        public String getTxid() {
            return txid;
        }

        /// The output's index in that transaction.
        public long getVout() {
            return vout;
        }

        /// The value in satoshis.
        public long getValue() {
            return value;
        }

        /// Whether the creating transaction is in a block.
        public boolean isConfirmed() {
            return confirmed;
        }

        /// The block height, or `-1` while unconfirmed.
        public long getHeight() {
            return height;
        }

        /// The output as a PSBT's witness UTXO wants it.
        public BitcoinTransaction.Output toOutput() {
            return new BitcoinTransaction.Output(value, address.getScriptPubKey());
        }
    }
}
