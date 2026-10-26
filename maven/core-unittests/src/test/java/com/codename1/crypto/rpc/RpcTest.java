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

import com.codename1.crypto.UInt256;
import com.codename1.crypto.bitcoin.BitcoinAddress;
import com.codename1.crypto.bitcoin.BitcoinNetwork;
import com.codename1.crypto.encoding.Json;
import com.codename1.crypto.evm.EvmAddress;
import com.codename1.crypto.solana.SolanaAddress;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// The chain clients against a transport that answers recorded replies and keeps the requests.
class RpcTest {
    private static final class Stub implements RpcTransport {
        final List<String> requests = new ArrayList<String>();
        final List<String> replies = new ArrayList<String>();
        int status = 200;

        Stub reply(String json) {
            replies.add(json);
            return this;
        }

        @Override
        public String send(String method, String url, String contentType, String body) throws IOException {
            requests.add(method + " " + url + (body == null ? "" : " " + body));
            if (status != 200) {
                throw RpcException.http(status, "busy");
            }
            return replies.remove(0);
        }

        Map<?, ?> lastBody() {
            String r = requests.get(requests.size() - 1);
            return (Map<?, ?>) Json.parse(r.substring(r.indexOf('{')));
        }
    }

    private static final EvmAddress ME = EvmAddress.parse("0x2c7536e3605d9c16a7a3d7b1898e529396a65c23");

    @Test
    void evm() throws IOException {
        Stub s = new Stub();
        EvmRpc rpc = new EvmRpc(s, "https://node");
        s.reply("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"0xaa36a7\"}");
        assertEquals(11155111L, rpc.chainId());
        assertEquals("eth_chainId", s.lastBody().get("method"));
        s.reply("{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":\"0xde0b6b3a7640000\"}");
        assertEquals(UInt256.parse("1000000000000000000"), rpc.getBalance(ME));
        assertEquals("[0x2c7536e3605d9c16a7a3d7b1898e529396a65c23, latest]",
                String.valueOf(s.lastBody().get("params")));
        s.reply("{\"jsonrpc\":\"2.0\",\"id\":3,\"result\":\"0x3b9aca00\"}");
        s.reply("{\"jsonrpc\":\"2.0\",\"id\":4,\"result\":{\"baseFeePerGas\":\"0x2540be400\"}}");
        UInt256[] fees = rpc.suggestFees();
        assertEquals(UInt256.valueOf(1000000000L), fees[0]);
        assertEquals(UInt256.valueOf(21000000000L), fees[1]);
        s.reply("{\"jsonrpc\":\"2.0\",\"id\":5,\"result\":null}");
        assertNull(rpc.getTransactionReceipt("0x01"));
        assertEquals("[0x01]", String.valueOf(s.lastBody().get("params")));
        s.reply("{\"jsonrpc\":\"2.0\",\"id\":6,\"error\":{\"code\":-32000,\"message\":\"nonce too low\"}}");
        RpcException e = assertThrows(RpcException.class, () -> rpc.getTransactionCount(ME, "pending"));
        assertEquals(-32000, e.getCode());
        assertFalse(e.isHttp());
        assertEquals("nonce too low", e.getMessage());
        s.reply("{\"jsonrpc\":\"2.0\",\"id\":7,\"result\":"
                + "\"0x00000000000000000000000000000000000000000000000000000000000f4240\"}");
        assertEquals(UInt256.valueOf(1000000), rpc.erc20BalanceOf(ME, ME));
        s.status = 429;
        RpcException http = assertThrows(RpcException.class, () -> rpc.blockNumber());
        assertTrue(http.isHttp());
        assertEquals(429, http.getCode());
    }

    @Test
    void esplora() throws IOException {
        Stub s = new Stub();
        EsploraClient c = new EsploraClient(s, "https://mempool.space/api/");
        BitcoinAddress a = BitcoinAddress.parse("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu", BitcoinNetwork.MAINNET);
        s.reply("[{\"txid\":\"ab\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":800000},"
                + "\"value\":5000},{\"txid\":\"cd\",\"vout\":0,\"status\":{\"confirmed\":false},\"value\":7}]");
        List<EsploraClient.Utxo> u = c.getUtxos(a);
        assertEquals("GET https://mempool.space/api/address/" + a + "/utxo", s.requests.get(0));
        assertEquals(2, u.size());
        assertEquals(800000L, u.get(0).getHeight());
        assertFalse(u.get(1).isConfirmed());
        assertEquals(a.getScriptPubKey(), u.get(0).toOutput().getScriptPubKey());
        s.reply("{\"1\":20.5,\"6\":8}");
        assertEquals(8.0, c.getFeeEstimates().get("6").doubleValue(), 0);
        s.reply("{\"chain_stats\":{\"funded_txo_sum\":10,\"spent_txo_sum\":4},"
                + "\"mempool_stats\":{\"funded_txo_sum\":0,\"spent_txo_sum\":3}}");
        long[] bal = c.getBalance(a);
        assertEquals(6, bal[0]);
        assertEquals(-3, bal[1]);
        s.reply("812345\n");
        assertEquals(812345L, c.getTipHeight());
    }

    @Test
    void solana() throws IOException {
        Stub s = new Stub();
        SolanaRpc rpc = new SolanaRpc(s, SolanaRpc.DEVNET);
        SolanaAddress a = SolanaAddress.SYSTEM_PROGRAM;
        s.reply("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"context\":{\"slot\":1},\"value\":2039280}}");
        assertEquals(2039280L, rpc.getBalance(a));
        assertEquals("[11111111111111111111111111111111, {commitment=confirmed}]",
                String.valueOf(s.lastBody().get("params")));
        s.reply("{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{\"context\":{\"slot\":1},\"value\":{\"blockhash\":"
                + "\"EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k\",\"lastValidBlockHeight\":3090}}}");
        assertEquals("EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k", rpc.getLatestBlockhash());
        s.reply("{\"jsonrpc\":\"2.0\",\"id\":3,\"result\":{\"context\":{\"slot\":1},\"value\":[null]}}");
        assertNull(rpc.getSignatureStatus("sig"));
        s.reply("{\"jsonrpc\":\"2.0\",\"id\":4,\"result\":{\"context\":{\"slot\":1},\"value\":[{\"slot\":5,"
                + "\"err\":null,\"confirmationStatus\":\"finalized\"}]}}");
        assertEquals("finalized", rpc.getSignatureStatus("sig"));
        s.reply("{\"jsonrpc\":\"2.0\",\"id\":5,\"result\":{\"context\":{\"slot\":1},\"value\":null}}");
        assertFalse(rpc.accountExists(a));
    }
}
