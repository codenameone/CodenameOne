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
package com.codename1.crypto.evm;

import com.codename1.crypto.UInt256;
import java.util.List;

/// Call data for ERC-20 tokens (USDC, USDT, DAI...). A token transfer is a transaction to the
/// *token contract* with value 0 and this call data; the recipient is inside it.
///
/// ```java
/// EvmAddress recipient = EvmAddress.parse("0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed");
/// EvmAddress usdc = EvmAddress.parse("0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48");
/// long nonce = 0;
/// UInt256 tip = UInt256.parseUnits("1", 9);
/// UInt256 maxFee = UInt256.parseUnits("30", 9);
/// byte[] data = Erc20.transfer(recipient, UInt256.parseUnits("25", 6));  // 25 USDC
/// EvmTransaction tx = EvmTransaction.eip1559(1, nonce, tip, maxFee, 65000, usdc, UInt256.ZERO, data);
/// ```
@com.codename1.impl.SharedWithBackend
public final class Erc20 {
    /// Topic 0 of `Transfer(address,address,uint256)`.
    public static final String TRANSFER_TOPIC =
            "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    private Erc20() {
    }

    /// `transfer(to, amount)`.
    public static byte[] transfer(EvmAddress to, UInt256 amount) {
        return Abi.encodeCall("transfer(address,uint256)", to, amount);
    }

    /// `approve(spender, amount)`.
    public static byte[] approve(EvmAddress spender, UInt256 amount) {
        return Abi.encodeCall("approve(address,uint256)", spender, amount);
    }

    /// `transferFrom(from, to, amount)`.
    public static byte[] transferFrom(EvmAddress from, EvmAddress to, UInt256 amount) {
        return Abi.encodeCall("transferFrom(address,address,uint256)", from, to, amount);
    }

    /// `balanceOf(owner)`, for `eth_call`.
    public static byte[] balanceOf(EvmAddress owner) {
        return Abi.encodeCall("balanceOf(address)", owner);
    }

    /// `allowance(owner, spender)`, for `eth_call`.
    public static byte[] allowance(EvmAddress owner, EvmAddress spender) {
        return Abi.encodeCall("allowance(address,address)", owner, spender);
    }

    /// `decimals()`, for `eth_call`.
    public static byte[] decimals() {
        return Abi.encodeCall("decimals()");
    }

    /// `symbol()`, for `eth_call`.
    public static byte[] symbol() {
        return Abi.encodeCall("symbol()");
    }

    /// `name()`, for `eth_call`.
    public static byte[] name() {
        return Abi.encodeCall("name()");
    }

    /// `totalSupply()`, for `eth_call`.
    public static byte[] totalSupply() {
        return Abi.encodeCall("totalSupply()");
    }

    /// Reads a `uint256` returned by `balanceOf`, `allowance`, `decimals` or `totalSupply`.
    public static UInt256 decodeUint(byte[] returned) {
        return (UInt256) Abi.decode("(uint256)", returned).get(0);
    }

    /// Reads a `string` returned by `name` or `symbol`.
    public static String decodeString(byte[] returned) {
        return (String) Abi.decode("(string)", returned).get(0);
    }

    /// A decoded `Transfer` event.
    public static final class Transfer {
        private final EvmAddress from;
        private final EvmAddress to;
        private final UInt256 amount;

        Transfer(EvmAddress from, EvmAddress to, UInt256 amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }

        /// The sender.
        public EvmAddress getFrom() {
            return from;
        }

        /// The recipient.
        public EvmAddress getTo() {
            return to;
        }

        /// The amount in the token's base units.
        public UInt256 getAmount() {
            return amount;
        }
    }

    /// Decodes a `Transfer` log from its topics and data, or answers `null` for any other log.
    ///
    /// #### Parameters
    ///
    /// - `topics`: the log's topics as `0x` hex, as `eth_getLogs` returns them
    ///
    /// - `data`: the log's data
    public static Transfer decodeTransfer(List<String> topics, byte[] data) {
        if (topics == null || topics.size() != 3 || !TRANSFER_TOPIC.equalsIgnoreCase(topics.get(0))) {
            return null;
        }
        EvmAddress from = (EvmAddress) Abi.decodeHex("(address)", topics.get(1)).get(0);
        EvmAddress to = (EvmAddress) Abi.decodeHex("(address)", topics.get(2)).get(0);
        return new Transfer(from, to, decodeUint(data));
    }
}
