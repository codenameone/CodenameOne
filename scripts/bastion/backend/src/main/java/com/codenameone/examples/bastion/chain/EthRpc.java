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

import com.codename1.backend.WebRpcTransport;
import com.codename1.crypto.UInt256;
import com.codename1.crypto.encoding.Hex;
import com.codename1.crypto.rpc.EvmRpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The framework's [EvmRpc] over the backend's web client, with the two rules
/// a custodian adds.
///
/// Every signature is made here, in the bank, and the node only ever sees
/// finished raw transactions (`eth_sendRawTransaction`). Never point this at
/// a node with unlocked accounts, and never call `eth_sendTransaction` or
/// `personal_*`: the node is a public, untrusted relay. Its answers are
/// untrusted too -- the watchers cross-check what matters (a receipt's block
/// hash against the canonical block at that height, a transaction's `to` and
/// `value` against what was signed). A node's refusal arrives as a
/// [com.codename1.crypto.rpc.RpcException].
public class EthRpc extends EvmRpc {
    public EthRpc(String url) {
        super(new WebRpcTransport(), checked(url));
    }

    private static String checked(String url) {
        if (!url.startsWith("https://") && !url.startsWith("http://127.0.0.1")
                && !url.startsWith("http://localhost")) {
            // A node answer decides when deposits are credited: plain HTTP to
            // anything but this machine lets the network forge them.
            throw new IllegalArgumentException("the node must be reached over HTTPS");
        }
        return url;
    }

    /// The block at `number` with full transactions, or null past the head.
    @SuppressWarnings("unchecked")
    public Map<String, Object> block(long number) throws IOException {
        return (Map<String, Object>) call("eth_getBlockByNumber", UInt256.valueOf(number).toQuantity(),
                Boolean.TRUE);
    }

    /// The receipt of `txHash`, or null while it is not mined.
    @SuppressWarnings("unchecked")
    public Map<String, Object> receipt(String txHash) throws IOException {
        return (Map<String, Object>) getTransactionReceipt(txHash);
    }

    /// The base fee of the next block, in wei.
    public UInt256 nextBaseFee() throws IOException {
        Map<?, ?> history = feeHistory(1, "latest", null);
        List<?> fees = (List<?>) history.get("baseFeePerGas");
        return UInt256.parse((String) fees.get(fees.size() - 1));
    }

    /// Broadcasts signed bytes and answers their hash.
    public String sendRaw(byte[] raw) throws IOException {
        return (String) call("eth_sendRawTransaction", "0x" + Hex.encode(raw));
    }
}
