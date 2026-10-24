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
package com.codenameone.examples.bastion.books;

import com.codenameone.examples.bastion.api.ChartAccountDto;
import com.codenameone.examples.bastion.ledger.AccountKinds;

import java.util.ArrayList;
import java.util.List;

/// The chart of accounts: every ledger account kind, seen from the bank's own
/// books.
///
/// The ledger stores one signed number per posting. Read as accounting, a
/// **positive amount is a credit and a negative one a debit**, which is why
/// every entry summing to zero is the same statement as "debits equal
/// credits". A customer's deposit is money the bank OWES, so customer
/// accounts are liabilities with a credit normal balance; the cash that came
/// in is an asset with a debit normal balance -- the reason `SYS_CASH` reads
/// negative in the raw table.
///
/// Classes, codes and normal balances follow from the kind, in code rather
/// than a table, so no row can ever put a customer's deposit among the bank's
/// assets.
public final class Chart {
    public static final String ASSET = "ASSET";
    public static final String LIABILITY = "LIABILITY";
    public static final String EQUITY = "EQUITY";
    public static final String REVENUE = "REVENUE";
    public static final String EXPENSE = "EXPENSE";

    public static final String DEBIT = "DEBIT";
    public static final String CREDIT = "CREDIT";

    /// A kind nothing here knows: shown, never hidden, so a new kind that
    /// was not classified shows up on the trial balance at once.
    public static final String SUSPENSE_CODE = "9999";

    private static final String[][] ACCOUNTS = {
        {"1000", "Cash and reserves", ASSET, AccountKinds.SYS_CASH},
        {"1100", "Digital assets in custody", ASSET, AccountKinds.SYS_CHAIN},
        {"2000", "Customer deposits - checking", LIABILITY, AccountKinds.CHECKING},
        {"2010", "Customer deposits - savings", LIABILITY, AccountKinds.SAVINGS},
        {"2100", "Customer digital asset custody", LIABILITY, AccountKinds.WALLET},
        {"2200", "Order escrow", LIABILITY, AccountKinds.HOLD},
        {"3000", "Paid-in capital", EQUITY, AccountKinds.SYS_EQUITY},
        {"3100", "Retained earnings", EQUITY, AccountKinds.SYS_RETAINED},
        {"4000", "Fee and trading revenue", REVENUE, AccountKinds.SYS_FEES},
        {"5000", "Interest expense", EXPENSE, AccountKinds.SYS_INTEREST},
        {"5100", "Operational adjustments", EXPENSE, AccountKinds.SYS_ADJUST},
    };

    private Chart() {
    }

    public static List<ChartAccountDto> all() {
        List<ChartAccountDto> out = new ArrayList<ChartAccountDto>();
        for (String[] a : ACCOUNTS) {
            out.add(new ChartAccountDto(a[0], a[1], a[2], normalOf(a[2]), a[3]));
        }
        return out;
    }

    public static String code(String kind) {
        String[] a = find(kind);
        return a == null ? SUSPENSE_CODE : a[0];
    }

    public static String name(String kind) {
        String[] a = find(kind);
        return a == null ? "Suspense (" + kind + ")" : a[1];
    }

    public static String classOf(String kind) {
        String[] a = find(kind);
        return a == null ? ASSET : a[2];
    }

    /// The kind a code stands for, or null.
    public static String kindOf(String code) {
        for (String[] a : ACCOUNTS) {
            if (a[0].equals(code)) {
                return a[3];
            }
        }
        return null;
    }

    public static boolean isCustomer(String kind) {
        return AccountKinds.isCustomer(kind);
    }

    public static boolean isNominal(String kind) {
        String c = classOf(kind);
        return REVENUE.equals(c) || EXPENSE.equals(c);
    }

    public static String normalOf(String accountClass) {
        return ASSET.equals(accountClass) || EXPENSE.equals(accountClass) ? DEBIT : CREDIT;
    }

    /// The balance as the class reads it: positive when the account is on
    /// its normal side. `net` is the ledger's credit-minus-debit sum.
    public static long normal(String kind, long net) {
        return DEBIT.equals(normalOf(classOf(kind))) ? -net : net;
    }

    public static long debit(long amount) {
        return amount < 0 ? -amount : 0L;
    }

    public static long credit(long amount) {
        return amount > 0 ? amount : 0L;
    }

    private static String[] find(String kind) {
        for (String[] a : ACCOUNTS) {
            if (a[3].equals(kind)) {
                return a;
            }
        }
        return null;
    }
}
