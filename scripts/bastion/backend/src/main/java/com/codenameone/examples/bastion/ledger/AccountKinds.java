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
package com.codenameone.examples.bastion.ledger;

/// What an account is for. The `SYS_` kinds belong to the bank, are owned by
/// [#BANK], and are the only ones allowed below zero.
public final class AccountKinds {
    /// The owner of every account the bank keeps for itself. Not a valid
    /// username, so no customer can ever be it.
    public static final String BANK = "@bank";

    public static final String CHECKING = "CHECKING";
    public static final String SAVINGS = "SAVINGS";
    /// A customer's balance in a crypto asset.
    public static final String WALLET = "WALLET";
    /// Funds a customer has committed to an open order and can no longer
    /// spend; see the exchange.
    public static final String HOLD = "HOLD";

    /// The world outside the ledger: money deposited comes out of it, money
    /// withdrawn goes into it.
    public static final String SYS_CASH = "SYS_CASH";
    public static final String SYS_FEES = "SYS_FEES";
    /// The blockchain, simulated: coins withdrawn to an outside address go
    /// here, as fiat withdrawn goes to SYS_CASH.
    public static final String SYS_CHAIN = "SYS_CHAIN";
    /// The bank's own capital: what its owners paid in.
    public static final String SYS_EQUITY = "SYS_EQUITY";
    /// Profit kept from closed periods; period close moves revenue and
    /// expense here.
    public static final String SYS_RETAINED = "SYS_RETAINED";
    /// Interest the bank pays on savings.
    public static final String SYS_INTEREST = "SYS_INTEREST";
    /// Goodwill credits and corrections an administrator posts.
    public static final String SYS_ADJUST = "SYS_ADJUST";

    private AccountKinds() {
    }

    public static boolean isSystem(String kind) {
        return kind != null && kind.startsWith("SYS_");
    }

    public static boolean isCustomer(String kind) {
        return CHECKING.equals(kind) || SAVINGS.equals(kind) || WALLET.equals(kind)
                || HOLD.equals(kind);
    }
}
