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
package com.codenameone.examples.bazaar.pos;

import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.api.PosPolicyDto;
import java.util.List;

/// The register's copy of the store's POS policy and the pure decisions made
/// from it: when the server will want a manager's approval, and the money of
/// a layaway. Everything here mirrors the server (`PosSales.approvalFor`,
/// `PosTools.refundApproval`, `OrderEdits`, `Layaways`), which stays the
/// judge: a register that guessed wrong still gets a 403 and asks then.
public final class PosRules {
    /// The approval kinds the server grants.
    public static final String DISCOUNT = "DISCOUNT";
    /// A hand-changed price.
    public static final String PRICE_OVERRIDE = "PRICE_OVERRIDE";
    /// A counter refund.
    public static final String REFUND = "REFUND";
    /// An edit of a placed order.
    public static final String ORDER_EDIT = "ORDER_EDIT";

    private PosRules() {
    }

    // ---------------------------------------------------------------- cache
    /// The policy as one line of comma separated numbers, for storage.
    public static String encode(PosPolicyDto p) {
        if (p == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        b.append(p.discountApprovalBp()).append(',').append(p.approvePriceOverrides() ? 1 : 0)
                .append(',').append(p.approveRefunds() ? 1 : 0).append(',')
                .append(p.approveOrderEdits() ? 1 : 0).append(',').append(p.tip1Bp()).append(',')
                .append(p.tip2Bp()).append(',').append(p.tip3Bp()).append(',')
                .append(p.layawayDepositBp()).append(',').append(p.layawayCancelFeeBp());
        return b.toString();
    }

    /// The policy [#encode] wrote, or null for anything else.
    public static PosPolicyDto decode(String text) {
        if (text == null || text.length() == 0) {
            return null;
        }
        long[] n = new long[9];
        int field = 0;
        long v = 0;
        boolean digits = false;
        for (int iter = 0; iter <= text.length(); iter++) {
            char ch = iter == text.length() ? ',' : text.charAt(iter);
            if (ch >= '0' && ch <= '9') {
                if (v > 100000000L) {
                    return null;
                }
                v = v * 10 + (ch - '0');
                digits = true;
            } else if (ch == ',') {
                if (!digits || field >= n.length) {
                    return null;
                }
                n[field++] = v;
                v = 0;
                digits = false;
            } else {
                return null;
            }
        }
        if (field != n.length) {
            return null;
        }
        return new PosPolicyDto(bp(n[0]), n[1] != 0, n[2] != 0, n[3] != 0, bp(n[4]), bp(n[5]),
                bp(n[6]), bp(n[7]), bp(n[8]));
    }

    private static long bp(long v) {
        return Math.max(0, Math.min(10000L, v));
    }

    // ------------------------------------------------------------ approvals
    /// Whether a member of `role` approves themselves, as owners and managers do.
    public static boolean approvesItself(String role) {
        return "OWNER".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
    }

    /// The approval kind a sale of `lines` needs, or null when it needs none.
    /// `catalogUnits` are the list prices of the lines, index for index (for
    /// a custom line, the price it is uploaded at, as the server has no other),
    /// and `discountCents` what automatic discounts and codes take off. Any
    /// line sold under its list price counts towards the threshold, so the
    /// sale-wide percent or amount does; only the price-override rule reads
    /// the lines' `overridden` flag.
    public static String saleApproval(PosPolicyDto p, String role, List<PosLineDto> lines,
            long[] catalogUnits, long discountCents) {
        if (p == null || approvesItself(role) || lines == null || catalogUnits == null) {
            return null;
        }
        long list = 0;
        long byHand = 0;
        boolean overridden = false;
        for (int iter = 0; iter < lines.size() && iter < catalogUnits.length; iter++) {
            PosLineDto l = lines.get(iter);
            list += catalogUnits[iter] * l.quantity();
            // Flagged or not: the sale's own percent or amount off lowers
            // the unit prices of lines it never flags, and counts all the same.
            byHand += Math.max(0, (catalogUnits[iter] - l.unitCents()) * l.quantity());
            if (l.overridden()) {
                overridden = true;
            }
        }
        long off = byHand + Math.max(0, discountCents);
        long bp = p.discountApprovalBp();
        if (bp > 0 && list > 0 && off * 10000L > bp * list) {
            return DISCOUNT;
        }
        return overridden && p.approvePriceOverrides() ? PRICE_OVERRIDE : null;
    }

    /// Whether a refund at the counter needs a manager.
    public static boolean refundNeedsApproval(PosPolicyDto p, String role) {
        return p != null && !approvesItself(role) && p.approveRefunds();
    }

    /// Whether editing a placed order needs a manager.
    public static boolean editNeedsApproval(PosPolicyDto p, String role) {
        return p != null && !approvesItself(role) && p.approveOrderEdits();
    }

    // -------------------------------------------------------------- layaway
    /// The least deposit the policy asks of a layaway of `totalCents`,
    /// rounded as the server rounds it, never more than the total.
    public static long minimumDeposit(long totalCents, long depositBp) {
        if (totalCents <= 0 || depositBp <= 0) {
            return 0;
        }
        return Math.min(totalCents, PosPricing.basisPoints(totalCents, Math.min(10000L,
                depositBp)));
    }

    /// What is still owed, never below zero.
    public static long balance(long totalCents, long paidCents) {
        return Math.max(0, totalCents - Math.max(0, paidCents));
    }

    /// The share of the total paid, in whole percent, 0 for an empty total.
    public static int percentPaid(long totalCents, long paidCents) {
        if (totalCents <= 0) {
            return 0;
        }
        return (int) (Math.max(0, Math.min(paidCents, totalCents)) * 100L / totalCents);
    }

    /// A payment toward the balance, clamped to what is owed.
    public static long payment(long wantedCents, long totalCents, long paidCents) {
        return Math.max(0, Math.min(wantedCents, balance(totalCents, paidCents)));
    }

    /// What cancelling gives back: the paid amount less the fee on the
    /// total, the fee never more than was paid.
    public static long cancelRefund(long totalCents, long paidCents, long feeBp) {
        long paid = Math.max(0, paidCents);
        long fee = Math.min(paid, PosPricing.basisPoints(Math.max(0, totalCents),
                Math.max(0, Math.min(10000L, feeBp))));
        return paid - fee;
    }
}
