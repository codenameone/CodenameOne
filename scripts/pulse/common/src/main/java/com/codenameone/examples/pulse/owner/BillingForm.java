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
package com.codenameone.examples.pulse.owner;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.pulse.api.InvoiceDto;
import com.codenameone.examples.pulse.api.ReasonDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.StatRow;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// Every bill the studio has sent: what came in over the last 30 days, what
/// is still owed and which cards failed. A bill nobody should pay -- an open
/// one, or a failed one being retried -- can be voided, with a reason.
public final class BillingForm {
    private static final String[] STATUSES = {"ALL", "PAID", "FAILED", "OPEN", "VOID"};
    private static final String[] STATUS_NAMES = {"All", "Paid", "Failed", "Open", "Void"};
    private static final long MONTH = 30L * 86400000L;

    private final Form form = Ui.form("Billing", "OwnerBilling");
    private final StatRow summary = Ui.stats(3);
    private final Container list = Ui.list("invoices");
    private String status = "ALL";
    private List<InvoiceDto> invoices;

    private BillingForm() {
    }

    public static void show(Form previous) {
        new BillingForm().open(previous);
    }

    private void open(Form previous) {
        Ui.back(form, previous);
        String[] names = new String[STATUSES.length];
        for (int iter = 0; iter < names.length; iter++) {
            names[iter] = Lang.tr(STATUS_NAMES[iter]);
        }
        Container top = new Container(BoxLayout.y(), "SvSearchBar");
        top.add(summary);
        top.add(Ui.pills("status", names, STATUSES, status, value -> {
            status = value;
            fill();
        }));
        form.add(BorderLayout.NORTH, top);
        form.add(BorderLayout.CENTER, list);
        Ui.pull(list, this::load);
        form.show();
        load();
    }

    // Every invoice at once: the summary is of all of them, and the chips
    // only narrow what is listed.
    private void load() {
        Api.owner().invoices("", Net.to(found -> {
            invoices = found;
            fill();
        }, Ui.failed(list, this::load)));
    }

    private void fill() {
        if (invoices == null) {
            return;
        }
        long since = System.currentTimeMillis() - MONTH;
        long collected = 0;
        long outstanding = 0;
        int failed = 0;
        for (int iter = 0; iter < invoices.size(); iter++) {
            InvoiceDto invoice = invoices.get(iter);
            if ("PAID".equals(invoice.status()) && invoice.paidAt() >= since) {
                collected += invoice.amountCents();
            } else if ("OPEN".equals(invoice.status()) || "FAILED".equals(invoice.status())) {
                outstanding += invoice.amountCents();
            }
            if ("FAILED".equals(invoice.status())) {
                failed++;
            }
        }
        summary.clear();
        summary.stat(Ui.moneyShort(collected, null), "Collected in 30 days", "collected")
                .stat(Ui.moneyShort(outstanding, null), "Outstanding", "outstanding")
                .stat(String.valueOf(failed), "Failed", "failedCount");
        list.removeAll();
        int shown = 0;
        for (int iter = 0; iter < invoices.size(); iter++) {
            InvoiceDto invoice = invoices.get(iter);
            if ("ALL".equals(status) || status.equals(invoice.status())) {
                list.add(row(form, invoice, "invoice-" + shown, this::load));
                shown++;
            }
        }
        if (shown == 0) {
            list.add(Ui.empty(FontImage.MATERIAL_RECEIPT_LONG, Lang.tr("No invoices"),
                    Lang.tr("Try another filter.")));
        }
        Ui.refresh(form);
    }

    /// One bill: who, what for, how much and where it stands; a failed one
    /// says how often it was tried and when it will be again. An open or
    /// failed one offers to void it.
    ///
    /// @param voided run after the bill was voided, to load what changed
    static Container row(final Form form, final InvoiceDto invoice, String name,
            final Runnable voided) {
        return row(form, invoice, name, voided, true);
    }

    /// One invoice. `withMember` false is the member's own page, where the name and the
    /// avatar on every line would repeat the header: the plan becomes the title instead.
    static Container row(final Form form, final InvoiceDto invoice, String name,
            final Runnable voided, boolean withMember) {
        String plan = invoice.description() == null ? "" : invoice.description();
        String suffix = " for " + invoice.memberName();
        if (invoice.memberName() != null && plan.endsWith(suffix)) {
            // The backend writes "<plan> for <member>"; the row already names the member.
            plan = plan.substring(0, plan.length() - suffix.length());
        }
        String detail = withMember ? plan : "";
        if ("FAILED".equals(invoice.status())) {
            detail += (detail.length() > 0 ? Ui.DOT : "") + (invoice.nextAttemptAt() > 0
                    ? Lang.tr("Attempt {0}, next {1}", Integer.valueOf(invoice.attempts()),
                    Ui.when(invoice.nextAttemptAt()))
                    : Lang.tr("Attempt {0}, no more retries", Integer.valueOf(invoice.attempts())));
        } else {
            detail += (detail.length() > 0 ? Ui.DOT : "") + Ui.day(invoice.createdAt());
        }
        Container value = new Container(BoxLayout.y());
        Label amount = Ui.plain(Ui.money(invoice.amountCents(), invoice.currency()), "SvRowAmount");
        value.add(amount);
        value.add(status(invoice.status()));
        Container row = withMember ? MembersForm.person(invoice.memberName(), detail, value)
                : MembersForm.line(plan, detail, value);
        if (!"FAILED".equals(invoice.status()) && !"OPEN".equals(invoice.status())) {
            row.setName(name);
            return row;
        }
        return Ui.tap(row, name, e -> Ui.reason(form, Lang.tr("Void invoice"),
                Lang.tr("{0} for {1} will not be charged. A membership it held up runs again.",
                Ui.money(invoice.amountCents(), invoice.currency()), invoice.memberName()),
                Lang.tr("Void"), reason -> Api.owner().voidInvoice(invoice.id(),
                new ReasonDto(reason), Net.to(done -> {
                    Ui.say(Lang.tr("Invoice voided"));
                    form.showBack();
                    voided.run();
                }, Ui::fail))));
    }

    static Label status(String status) {
        if ("PAID".equals(status)) {
            return Ui.badge(Lang.tr("Paid"), "SvBadgeGood");
        }
        if ("FAILED".equals(status)) {
            return Ui.badge(Lang.tr("Failed"), "SvBadgeBad");
        }
        if ("OPEN".equals(status)) {
            return Ui.badge(Lang.tr("Open"), "SvBadgeWarn");
        }
        if ("VOID".equals(status)) {
            return Ui.badge(Lang.tr("Void"), "SvBadge");
        }
        return Ui.badge(status == null ? "" : status, "SvBadge");
    }
}
