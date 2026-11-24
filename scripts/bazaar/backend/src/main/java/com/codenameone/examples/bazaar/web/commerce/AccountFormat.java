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
package com.codenameone.examples.bazaar.web.commerce;

import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.api.CsvErrorDto;
import com.codenameone.examples.bazaar.api.NetTermDto;
import com.codenameone.examples.bazaar.api.SubscriptionDto;
import com.codenameone.examples.bazaar.api.TradeQuoteDto;
import com.codenameone.examples.bazaar.api.TradeQuoteLineDto;
import com.codenameone.examples.bazaar.subscribe.Subscriptions;
import com.codenameone.examples.bazaar.trade.Trade;
import com.codenameone.examples.bazaar.web.Money;
import java.util.List;

/// Formatting the shopper's subscription and company pages share
/// ([CommerceAccountPages]): every rule is the services', this class only
/// lays their answers out.
final class AccountFormat {
    private AccountFormat() {
    }

    private static AdminRow fact(String title, String detail) {
        AdminRow r = new AdminRow();
        r.title = title;
        r.detail = detail;
        return r;
    }

    private static AdminRow row(String a, String b, String c, String d) {
        AdminRow r = new AdminRow();
        r.title = a;
        r.detail = b;
        r.meta = c;
        r.amount = d;
        return r;
    }

    /// Basis points as a percentage: 1000 is "10", 1250 is "12.5".
    public static String percent(int bps) {
        String whole = Integer.toString(bps / 100);
        int rest = bps % 100;
        if (rest == 0) {
            return whole;
        }
        return whole + "." + (rest % 10 == 0 ? Integer.toString(rest / 10)
                : (rest < 10 ? "0" : "") + rest);
    }


    /// "every month", "every 2 weeks".
    static String cadence(String unit, int n) {
        return com.codenameone.examples.bazaar.web.StorefrontPages.planWords(unit, n);
    }

    /// The buttons a subscription's status allows; the service still decides.
    static ActionCard actions(SubscriptionDto s, String base) {
        ActionCard c = new ActionCard();
        if (Subscriptions.ACTIVE.equals(s.status())) {
            c.button("Skip next delivery", base + "/skip", false);
            c.button("Pause", base + "/pause", false);
        }
        if (Subscriptions.PAUSED.equals(s.status())) {
            c.button("Resume", base + "/resume", false);
        }
        if (!Subscriptions.CANCELLED.equals(s.status())) {
            c.button("Cancel", base + "/cancel", true);
        }
        return c;
    }

    static String terms(int days) {
        return days <= 0 ? "Pays by card" : "Net " + days;
    }

    /// Marks each select's option whose id is the field's value.
    static void selectChosen(FormView form) {
        for (int iter = 0; iter < form.fields.size(); iter++) {
            FormField ff = form.fields.get(iter);
            for (int o = 0; o < ff.options.size(); o++) {
                ff.options.get(o).flag = ff.options.get(o).id.equals(ff.value);
            }
        }
    }


    static String shortId(String id) {
        return id.length() > 8 ? id.substring(id.length() - 8) : id;
    }

    /// A quote's facts and lines, the same in the back office and the account.
    static void quoteFacts(TradeQuoteDto q, MarketingView v) {
        v.facts.add(fact("Company", q.companyName()));
        v.facts.add(fact("Status", words(q.status())));
        if (q.buyerUsername() != null && q.buyerUsername().length() > 0) {
            v.facts.add(fact("Asked for by", q.buyerUsername()));
        }
        if (q.note() != null && q.note().length() > 0) {
            v.facts.add(fact("Note", q.note()));
        }
        v.facts.add(fact("Subtotal at list", Money.format(q.subtotalCents(), q.currency())));
        v.facts.add(fact("Company discount", Money.format(q.discountCents(), q.currency())));
        v.facts.add(fact("Total", Money.format(q.totalCents(), q.currency())));
        v.facts.add(fact("Updated", date(q.updatedAt())));
        v.rowsTitle = "Lines";
        v.headA = "Item";
        v.headB = "SKU";
        v.headC = "Quantity";
        v.headD = "Line total";
        List<TradeQuoteLineDto> ls = q.lines();
        for (int iter = 0; ls != null && iter < ls.size(); iter++) {
            TradeQuoteLineDto l = ls.get(iter);
            v.rows.add(row(l.title(), l.sku(), Integer.toString(l.quantity()),
                    Money.format(l.unitCents() * l.quantity(), q.currency())));
        }
    }

    static ActionCard invoiceCard(NetTermDto t, String cur, String pdf) {
        ActionCard k = new ActionCard();
        k.title = "Order #" + t.orderNumber();
        k.status = Trade.PAID.equals(t.status()) ? "Paid" : t.overdue() ? "Overdue" : "Open";
        k.detail = "net " + t.termsDays();
        k.meta = Trade.PAID.equals(t.status()) ? "paid " + date(t.paidAt())
                : "due " + date(t.dueAt());
        k.amount = Money.format(t.totalCents(), cur);
        k.extraUrl = pdf;
        k.extraLabel = "Invoice (PDF)";
        return k;
    }

    /// The per-line refusals of a CSV or quick order, as the page's table.
    static void csvErrors(List<CsvErrorDto> errors, MarketingView v) {
        v.rowsTitle = "These lines were not accepted";
        v.headA = "Line";
        v.headB = "Problem";
        v.headC = "";
        v.headD = "";
        for (int iter = 0; iter < errors.size(); iter++) {
            CsvErrorDto e = errors.get(iter);
            v.rows.add(row(e.line() == 0 ? "-" : Integer.toString(e.line()), e.message(), "",
                    ""));
        }
    }

    static String date(long millis) {
        return millis <= 0 ? "" : Days.iso(millis);
    }

    /// A status constant in words: "PARTIALLY_PAID" is "Partially paid".
    static String words(String status) {
        if (status == null || status.length() == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(status.length());
        for (int iter = 0; iter < status.length(); iter++) {
            char c = status.charAt(iter);
            if (c == '_') {
                b.append(' ');
            } else if (iter > 0 && c >= 'A' && c <= 'Z') {
                b.append((char) (c + ('a' - 'A')));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }
}
