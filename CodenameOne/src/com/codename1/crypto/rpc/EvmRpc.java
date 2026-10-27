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
import com.codename1.crypto.encoding.Hex;
import com.codename1.crypto.evm.Erc20;
import com.codename1.crypto.evm.EvmAddress;
import com.codename1.crypto.evm.EvmTransaction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// An Ethereum JSON-RPC client, for any EVM chain: balances, nonces, fees, gas estimates,
/// calls, broadcasting and receipts.
///
/// ```java
/// EvmRpc rpc = new EvmRpc(new NetworkTransport(), "https://ethereum-sepolia-rpc.publicnode.com");
/// Wallet wallet = Wallet.fromMnemonic(Mnemonic.generate(128), "");
/// EvmAddress me = wallet.getEvmAddress(0, 0);
/// EvmAddress to = EvmAddress.parse("0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed");
/// UInt256 value = UInt256.parseUnits("0.01", 18);
/// long nonce = rpc.getTransactionCount(me, "pending");
/// UInt256[] fees = rpc.suggestFees();
/// EvmTransaction tx = EvmTransaction.eip1559(rpc.chainId(), nonce, fees[0], fees[1], 21000, to, value, null);
/// String hash = rpc.sendRawTransaction(wallet.signEvmTransaction(tx, Wallet.evmPath(0, 0)));
/// ```
///
/// Block tags are the node's: `latest`, `pending`, `safe`, `finalized`, or a quantity.
@com.codename1.impl.SharedWithBackend
public class EvmRpc extends JsonRpc {
    /// A client for a node.
    public EvmRpc(RpcTransport transport, String url) {
        super(transport, url);
    }

    static UInt256 quantity(Object o, String what) throws IOException {
        if (o instanceof String) {
            try {
                return UInt256.parse((String) o);
            } catch (IllegalArgumentException e) {
                throw new IOException(what + ": not a quantity: " + o, e);
            }
        }
        throw new IOException(what + ": expected a quantity");
    }

    static long smallQuantity(Object o, String what) throws IOException {
        try {
            return quantity(o, what).longValueExact();
        } catch (ArithmeticException e) {
            throw new IOException(what + ": out of range", e);
        }
    }

    /// `eth_chainId`.
    public long chainId() throws IOException {
        return smallQuantity(call("eth_chainId"), "eth_chainId");
    }

    /// `eth_blockNumber`.
    public long blockNumber() throws IOException {
        return smallQuantity(call("eth_blockNumber"), "eth_blockNumber");
    }

    /// `eth_getBalance` in wei at a block tag.
    public UInt256 getBalance(EvmAddress address, String block) throws IOException {
        return quantity(call("eth_getBalance", address.toLowerCaseString(), block), "eth_getBalance");
    }

    /// The latest balance in wei.
    public UInt256 getBalance(EvmAddress address) throws IOException {
        return getBalance(address, "latest");
    }

    /// `eth_getTransactionCount`: the next nonce at a block tag; `pending` counts what is
    /// queued in the node's pool.
    public long getTransactionCount(EvmAddress address, String block) throws IOException {
        return smallQuantity(call("eth_getTransactionCount", address.toLowerCaseString(), block),
                "eth_getTransactionCount");
    }

    /// `eth_gasPrice` in wei.
    public UInt256 gasPrice() throws IOException {
        return quantity(call("eth_gasPrice"), "eth_gasPrice");
    }

    /// `eth_maxPriorityFeePerGas` in wei.
    public UInt256 maxPriorityFeePerGas() throws IOException {
        return quantity(call("eth_maxPriorityFeePerGas"), "eth_maxPriorityFeePerGas");
    }

    /// `eth_feeHistory`, as the node answers it.
    public Map<?, ?> feeHistory(int blocks, String newestBlock, double[] rewardPercentiles) throws IOException {
        List<Object> p = new ArrayList<Object>();
        if (rewardPercentiles != null) {
            for (double rewardPercentilesItem : rewardPercentiles) {
                p.add(Double.valueOf(rewardPercentilesItem));
            }
        }
        return asMap(call("eth_feeHistory", UInt256.valueOf(blocks).toQuantity(), newestBlock, p), "eth_feeHistory");
    }

    /// EIP-1559 fees for a transaction to be included soon: `{maxPriorityFeePerGas,
    /// maxFeePerGas}`, the priority fee the node suggests and a fee cap of twice the latest base
    /// fee plus it, which covers six full blocks of base-fee growth.
    public UInt256[] suggestFees() throws IOException {
        UInt256 tip = maxPriorityFeePerGas();
        Map<?, ?> block = asMap(call("eth_getBlockByNumber", "latest", Boolean.FALSE), "eth_getBlockByNumber");
        Object base = block.get("baseFeePerGas");
        if (base == null) {
            UInt256 price = gasPrice();
            return new UInt256[] {price, price};
        }
        UInt256 b = quantity(base, "baseFeePerGas");
        return new UInt256[] {tip, b.add(b).add(tip)};
    }

    private static Map<String, Object> callObject(EvmAddress from, EvmAddress to, UInt256 value, byte[] data) {
        Map<String, Object> o = new LinkedHashMap<String, Object>();
        if (from != null) {
            o.put("from", from.toLowerCaseString());
        }
        if (to != null) {
            o.put("to", to.toLowerCaseString());
        }
        if (value != null && !value.isZero()) {
            o.put("value", value.toQuantity());
        }
        if (data != null && data.length > 0) {
            o.put("data", "0x" + Hex.encode(data));
        }
        return o;
    }

    /// `eth_estimateGas` for a call; `to` is `null` for a contract creation.
    public long estimateGas(EvmAddress from, EvmAddress to, UInt256 value, byte[] data) throws IOException {
        return smallQuantity(call("eth_estimateGas", callObject(from, to, value, data)), "eth_estimateGas");
    }

    /// `eth_call` at a block tag, answering the returned bytes.
    public byte[] call(EvmAddress to, byte[] data, String block) throws IOException {
        String r = asString(call("eth_call", callObject(null, to, null, data), block), "eth_call");
        return Hex.decode(r.startsWith("0x") ? r.substring(2) : r);
    }

    /// `eth_call` at the latest block.
    public byte[] call(EvmAddress to, byte[] data) throws IOException {
        return call(to, data, "latest");
    }

    /// An ERC-20 token balance in the token's base units.
    public UInt256 erc20BalanceOf(EvmAddress token, EvmAddress owner) throws IOException {
        return Erc20.decodeUint(call(token, Erc20.balanceOf(owner)));
    }

    /// `eth_sendRawTransaction`, answering the transaction hash.
    public String sendRawTransaction(EvmTransaction signed) throws IOException {
        return asString(call("eth_sendRawTransaction", signed.encodeHex()), "eth_sendRawTransaction");
    }

    /// `eth_getTransactionReceipt`: the receipt, or `null` while the transaction is unmined.
    public Map<?, ?> getTransactionReceipt(String txHash) throws IOException {
        Object r = call("eth_getTransactionReceipt", txHash);
        return r == null ? null : asMap(r, "eth_getTransactionReceipt");
    }

    /// Whether a mined receipt reports success (`status` 1).
    public static boolean isSuccess(Map<?, ?> receipt) {
        return receipt != null && "0x1".equals(receipt.get("status"));
    }

    /// `eth_getLogs` for a filter object (`address`, `topics`, `fromBlock`, `toBlock`).
    public List<?> getLogs(Map<String, Object> filter) throws IOException {
        return asList(call("eth_getLogs", filter), "eth_getLogs");
    }
}
