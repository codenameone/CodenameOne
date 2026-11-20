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

import com.codenameone.examples.bazaar.api.PosTenderDto;
import java.util.ArrayList;
import java.util.List;

/// How a sale is being paid: split across cash, a card, a gift card and
/// store credit. Pure: no display.
///
/// The rules are the server's, so a sale the register accepts is one the
/// server accepts: one card and one gift card at most, the card, gift card
/// and credit together never more than the total, and cash covering the rest
/// with the difference handed back as change.
public final class Tenders {
    public static final String CASH = "CASH";
    public static final String CARD = "CARD";
    public static final String GIFT_CARD = "GIFT_CARD";
    public static final String STORE_CREDIT = "STORE_CREDIT";

    private final long due;
    private final List<PosTenderDto> tenders = new ArrayList<PosTenderDto>();

    public Tenders(long dueCents) {
        this.due = Math.max(0, dueCents);
    }

    public long due() {
        return due;
    }

    public List<PosTenderDto> list() {
        return new ArrayList<PosTenderDto>(tenders);
    }

    long sum(String kind) {
        long s = 0;
        for (int iter = 0; iter < tenders.size(); iter++) {
            if (tenders.get(iter).kind().equals(kind)) {
                s += tenders.get(iter).amountCents();
            }
        }
        return s;
    }

    boolean has(String kind) {
        return sum(kind) > 0;
    }

    /// Card, gift card and credit together.
    public long nonCash() {
        return sum(CARD) + sum(GIFT_CARD) + sum(STORE_CREDIT);
    }

    /// What is still to pay.
    public long remaining() {
        return Math.max(0, due - nonCash() - sum(CASH));
    }

    /// Cash handed back.
    public long change() {
        long rest = Math.max(0, due - nonCash());
        long cash = sum(CASH);
        return cash > rest ? cash - rest : 0;
    }

    public boolean complete() {
        return due - nonCash() >= 0 && remaining() == 0;
    }

    /// Adds a payment; answers why it cannot be taken, or null when it was.
    /// A non-cash payment more than what is left is cut to what is left.
    public String add(String kind, long amountCents, String reference) {
        if (amountCents <= 0) {
            return "Enter an amount";
        }
        if (CASH.equals(kind)) {
            tenders.add(new PosTenderDto(CASH, amountCents, ""));
            return null;
        }
        if (CARD.equals(kind) && has(CARD)) {
            return "One card payment per sale";
        }
        if (GIFT_CARD.equals(kind) && has(GIFT_CARD)) {
            return "One gift card per sale";
        }
        long left = due - nonCash();
        if (left <= 0) {
            return "Nothing left to pay";
        }
        tenders.add(new PosTenderDto(kind, Math.min(amountCents, left),
                reference == null ? "" : reference));
        return null;
    }

    public void remove(int index) {
        if (index >= 0 && index < tenders.size()) {
            tenders.remove(index);
        }
    }

    /// Amounts a cashier is likely to be handed for `due`: the exact amount,
    /// then the next whole unit and the next 5, 10, 20, 50 and 100, each
    /// once and only when more than the amount due.
    public static long[] quickCash(long due) {
        List<Long> out = new ArrayList<Long>();
        if (due <= 0) {
            return new long[0];
        }
        out.add(Long.valueOf(due));
        long[] steps = {100, 500, 1000, 2000, 5000, 10000};
        for (int iter = 0; iter < steps.length; iter++) {
            long s = steps[iter];
            long up = ((due + s - 1) / s) * s;
            if (up > due && !out.contains(Long.valueOf(up))) {
                out.add(Long.valueOf(up));
            }
        }
        long[] arr = new long[Math.min(out.size(), 6)];
        for (int iter = 0; iter < arr.length; iter++) {
            arr[iter] = out.get(iter).longValue();
        }
        return arr;
    }
}
