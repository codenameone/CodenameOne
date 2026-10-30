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
package com.codenameone.examples.pulse.member;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.pulse.Telemetry;
import com.codenameone.examples.pulse.api.FreezeDto;
import com.codenameone.examples.pulse.api.InvoiceDto;
import com.codenameone.examples.pulse.api.JoinDto;
import com.codenameone.examples.pulse.api.MemberDto;
import com.codenameone.examples.pulse.api.MembershipDto;
import com.codenameone.examples.pulse.api.PayInvoiceDto;
import com.codenameone.examples.pulse.api.PlanDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.pay.WalletForm;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// The plan: who in the household is on which, and what can be done with it --
/// freeze it for a while, cancel it at the end of the period, pay an invoice
/// that failed. And the plans on offer, to join.
///
/// Money is the server's business. It prorates, retries a failed card on its
/// dunning ladder and decides when a membership is past due; this screen shows
/// what it says and sends what the member chose. Cards come from the wallet.
public final class MembershipForm {
    private MembershipForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Membership", "Membership");
        Ui.back(form, previous);
        final Container list = Ui.list("membership");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.membership().memberships(Net.to(memberships ->
                Api.membership().invoices(Net.to(invoices -> fill(form, list, memberships,
                        invoices, load[0]), Ui.failed(list, load[0]))),
                Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.add(BorderLayout.SOUTH, Ui.primary("See plans", "plans",
                e -> plans(form, load[0])));
        form.show();
        load[0].run();
    }

    /// A membership state, in words.
    public static String state(String state) {
        if ("ACTIVE".equals(state)) {
            return Lang.tr("Active");
        }
        if ("FROZEN".equals(state)) {
            return Lang.tr("Frozen");
        }
        if ("PAST_DUE".equals(state)) {
            return Lang.tr("Payment due");
        }
        if ("CANCELLED".equals(state)) {
            return Lang.tr("Cancelled");
        }
        return state == null ? "" : state;
    }

    /// The badge style for a membership state.
    public static String badge(String state) {
        return "ACTIVE".equals(state) ? "SvBadgeGood"
                : "PAST_DUE".equals(state) || "CANCELLED".equals(state) ? "SvBadgeBad"
                : "SvBadgeWarn";
    }

    private static void fill(final Form form, Container list, List<MembershipDto> memberships,
            List<InvoiceDto> invoices, final Runnable load) {
        list.removeAll();
        Ui.section(list, "Plans");
        if (memberships == null || memberships.isEmpty()) {
            list.add(Ui.text("Nobody in the household is on a plan yet.", "SvMuted"));
        }
        for (int iter = 0; memberships != null && iter < memberships.size(); iter++) {
            final MembershipDto membership = memberships.get(iter);
            String detail = membership.memberName();
            if ("FROZEN".equals(membership.state())) {
                detail += " - " + Lang.tr("frozen until {0}", Ui.day(membership.frozenUntil()));
            } else if (membership.cancelAtPeriodEnd()) {
                detail += " - " + Lang.tr("ends {0}", Ui.day(membership.currentPeriodEnd()));
            } else if (membership.currentPeriodEnd() > 0) {
                detail += " - " + Lang.tr("renews {0}", Ui.day(membership.currentPeriodEnd()));
            }
            if ("CLASS_PACK".equals(membership.kind())) {
                detail += " - " + Lang.tr("{0} classes left",
                        Integer.valueOf(membership.creditsLeft()));
            }
            Container row = Ui.row(FontImage.MATERIAL_CARD_MEMBERSHIP, membership.planName(),
                    detail, Ui.badge(state(membership.state()), badge(membership.state())));
            row.setName("membership-" + iter);
            list.add(row);
            Container actions = new Container(new FlowLayout(com.codename1.ui.Component.RIGHT));
            if ("FROZEN".equals(membership.state())) {
                actions.add(Ui.link("Unfreeze", "unfreeze-" + iter,
                        e -> Api.membership().unfreeze(membership.id(),
                                Net.to(done -> load.run(), Ui::fail))));
            } else if ("ACTIVE".equals(membership.state())) {
                actions.add(Ui.link("Freeze", "freeze-" + iter,
                        e -> freeze(form, membership, load)));
            }
            if (!membership.cancelAtPeriodEnd() && !"CANCELLED".equals(membership.state())) {
                actions.add(Ui.link("Cancel", "cancelPlan-" + iter,
                        e -> Api.membership().cancel(membership.id(), Net.to(done -> {
                            Ui.say("Your plan ends at the end of this period");
                            load.run();
                        }, Ui::fail))));
            }
            list.add(actions);
        }
        Ui.section(list, "Invoices");
        if (invoices == null || invoices.isEmpty()) {
            list.add(Ui.text("No invoices yet.", "SvMuted"));
        }
        for (int iter = 0; invoices != null && iter < invoices.size(); iter++) {
            final InvoiceDto invoice = invoices.get(iter);
            boolean due = "OPEN".equals(invoice.status()) || "FAILED".equals(invoice.status());
            String detail = Ui.day(invoice.createdAt()) + " - " + invoice.memberName();
            if (invoice.failure() != null && invoice.failure().length() > 0) {
                detail += "\n" + invoice.failure();
            }
            list.add(Ui.row(FontImage.MATERIAL_RECEIPT_LONG, invoice.description(), detail,
                    Ui.plain(Ui.money(invoice.amountCents() - invoice.creditCents(),
                            invoice.currency()), "SvAmount")));
            if (due) {
                list.add(FlowLayout.encloseRight(Ui.link("Pay now", "pay-" + iter,
                        e -> WalletForm.pick(form, null, method ->
                                Api.membership().pay(invoice.id(), new PayInvoiceDto(method.id()),
                                        Net.to(paid -> {
                                            Telemetry.invoicePaid();
                                            Ui.say("Paid. Thank you");
                                            form.showBack();
                                            load.run();
                                        }, Ui::fail))))));
            }
        }
        Ui.section(list, "Time away");
        list.add(Ui.tap(Ui.row(FontImage.MATERIAL_BEACH_ACCESS, Lang.tr("Away periods"),
                Lang.tr("Travelling or injured? No nudges, and billing can pause"), null),
                "away", e -> AwayForm.show(form)));
        Ui.refresh(form);
    }

    private static void freeze(final Form previous, final MembershipDto membership,
            final Runnable done) {
        final Form form = Ui.form("Freeze", "Freeze");
        Ui.back(form, previous);
        Container page = Ui.page();
        page.add(Ui.text("A frozen plan is not billed and its classes cannot be booked. "
                + "It starts again by itself on the last day.", "SvMuted"));
        final TextField from = Ui.field(page, "From", "from", "YYYY-MM-DD");
        final TextField until = Ui.field(page, "Until", "until", "YYYY-MM-DD");
        page.add(Ui.primary("Freeze", "submit", e -> Api.membership().freeze(membership.id(),
                new FreezeDto(from.getText().trim(), until.getText().trim()), Net.to(frozen -> {
                    previous.showBack();
                    done.run();
                }, Ui::fail))));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    private static void plans(final Form previous, final Runnable joined) {
        final Form form = Ui.form("Plans", "Plans");
        Ui.back(form, previous);
        final Container list = Ui.list("plans");
        form.add(BorderLayout.CENTER, list);
        form.show();
        Api.household().members(Net.to(members -> Api.membership().plans(Net.to(plans -> {
            list.removeAll();
            for (int iter = 0; plans != null && iter < plans.size(); iter++) {
                final PlanDto plan = plans.get(iter);
                if (!plan.active()) {
                    continue;
                }
                String detail = plan.description() == null ? "" : plan.description();
                if (plan.maxAge() > 0) {
                    detail += "\n" + Lang.tr("Ages {0} to {1}", Integer.valueOf(plan.minAge()),
                            Integer.valueOf(plan.maxAge()));
                }
                list.add(Ui.row(FontImage.MATERIAL_CARD_MEMBERSHIP, plan.name(), detail,
                        Ui.plain(Ui.money(plan.priceCents(), plan.currency()), "SvPrice")));
                Container buttons = new Container(new FlowLayout(com.codename1.ui.Component.RIGHT));
                for (int who = 0; members != null && who < members.size(); who++) {
                    final MemberDto member = members.get(who);
                    if (plan.maxAge() > 0 && member.age() > plan.maxAge()
                            || member.age() < plan.minAge()) {
                        continue;
                    }
                    buttons.add(Ui.link(Lang.tr("Join for {0}", member.name()),
                            "join-" + iter + "-" + who, e -> WalletForm.pick(form, null,
                                    method -> Api.membership().join(new JoinDto(member.id(),
                                            plan.id(), method.id()), Net.to(membership -> {
                                                Telemetry.joined(plan.kind());
                                                Ui.say("Welcome to Pulse");
                                                previous.showBack();
                                                joined.run();
                                            }, Ui::fail)))));
                }
                list.add(buttons);
            }
            Ui.refresh(form);
        }, Ui::fail)), Ui::fail));
    }
}
