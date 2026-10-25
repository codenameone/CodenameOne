/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.chain;

import com.codename1.backend.Json;
import com.codename1.backend.Web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/// A JSON-RPC client for an Ethereum node: the handful of calls a custodian
/// needs, nothing that would let the node hold a key.
///
/// Every signature is made here, in the bank, and the node only ever sees
/// finished raw transactions (`eth_sendRawTransaction`). Never point this at
/// a node with unlocked accounts, and never call `eth_sendTransaction` or
/// `personal_*`: the node is a public, untrusted relay. Its answers are
/// untrusted too -- the watchers cross-check what matters (a receipt's block
/// hash against the canonical block at that height, a transaction's `to` and
/// `value` against what was signed).
public class EthRpc {
    private final String url;
    private final AtomicInteger nextId = new AtomicInteger(1);

    public EthRpc(String url) {
        if (!url.startsWith("https://") && !url.startsWith("http://127.0.0.1")
                && !url.startsWith("http://localhost")) {
            // A node answer decides when deposits are credited: plain HTTP to
            // anything but this machine lets the network forge them.
            throw new IllegalArgumentException("the node must be reached over HTTPS");
        }
        this.url = url;
    }

    public long chainId() throws IOException {
        return Wei.parseLong((String) call("eth_chainId"));
    }

    public long blockNumber() throws IOException {
        return Wei.parseLong((String) call("eth_blockNumber"));
    }

    /// The next nonce for `address`, counting transactions still pending.
    public long pendingNonce(String address) throws IOException {
        return Wei.parseLong((String) call("eth_getTransactionCount", address, "pending"));
    }

    public byte[] balance(String address) throws IOException {
        return Wei.parseQuantity((String) call("eth_getBalance", address, "latest"));
    }

    /// The block at `number` with full transactions, or null past the head.
    @SuppressWarnings("unchecked")
    public Map<String, Object> block(long number) throws IOException {
        return (Map<String, Object>) call("eth_getBlockByNumber", Wei.quantity(number), Boolean.TRUE);
    }

    /// The receipt of `txHash`, or null while it is not mined.
    @SuppressWarnings("unchecked")
    public Map<String, Object> receipt(String txHash) throws IOException {
        return (Map<String, Object>) call("eth_getTransactionReceipt", txHash);
    }

    /// The base fee of the next block, in wei.
    @SuppressWarnings("unchecked")
    public byte[] nextBaseFee() throws IOException {
        Map<String, Object> history = (Map<String, Object>) call("eth_feeHistory", "0x1", "latest",
                new ArrayList<Object>());
        List<Object> fees = (List<Object>) history.get("baseFeePerGas");
        return Wei.parseQuantity((String) fees.get(fees.size() - 1));
    }

    /// The node's suggested priority fee, in wei.
    public byte[] priorityFee() throws IOException {
        return Wei.parseQuantity((String) call("eth_maxPriorityFeePerGas"));
    }

    /// Broadcasts a signed transaction and answers its hash.
    public String sendRaw(byte[] raw) throws IOException {
        return (String) call("eth_sendRawTransaction", "0x" + Hex32.encode(raw));
    }

    private Object call(String method, Object... params) throws IOException {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        List<Object> list = new ArrayList<Object>();
        for (Object p : params) {
            list.add(p);
        }
        request.put("params", list);
        int id = nextId.getAndIncrement();
        request.put("id", Integer.valueOf(id));
        Web.Result result = Web.postJson(url, Json.write(request), null);
        if (!result.isSuccess()) {
            throw new IOException(method + ": the node answered HTTP " + result.getStatus());
        }
        Map answer = Json.parseObject(result.getBodyAsString());
        Object error = answer.get("error");
        if (error instanceof Map) {
            Object message = ((Map) error).get("message");
            throw new NodeException(method, message == null ? "error" : String.valueOf(message));
        }
        return answer.get("result");
    }

    /// The node refused a call; for `eth_sendRawTransaction` its message says
    /// why (nonce too low, underpriced, insufficient funds).
    public static class NodeException extends IOException {
        private static final long serialVersionUID = 1L;

        public NodeException(String method, String message) {
            super(method + ": " + message);
        }
    }
}
