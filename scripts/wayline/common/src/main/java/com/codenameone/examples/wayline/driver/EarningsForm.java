/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.wayline.driver;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.EarningsSummaryDto;
import com.codenameone.examples.wayline.api.PayoutAccountDto;
import com.codenameone.examples.wayline.api.PayoutDto;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.pay.Cards;
import com.codenameone.examples.wayline.ui.Chart;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.StatRow;
import com.codenameone.examples.wayline.ui.Ui;
import java.util.List;

/// What driving has paid: what is waiting to be cashed out, the last
/// fortnight a day at a time, and the payouts already sent.
///
/// The sums are the server's. It works them out from the rides it recorded,
/// less its commission, so this screen can show them and cannot change them.
public final class EarningsForm {
    private final Form form = Ui.form("Earnings", "Earnings");
    private final Container page = Ui.page();
    private PayoutAccountDto account;

    private EarningsForm(Form previous) {
        Ui.back(form, previous);
        form.add(BorderLayout.CENTER, page);
    }

    public static void show(Form previous) {
        EarningsForm earnings = new EarningsForm(previous);
        earnings.form.show();
        earnings.load();
    }

    private void load() {
        Api.driver().payoutAccount(Net.to(found -> {
            account = found;
            Api.driver().earningsSummary(Net.to(summary -> Api.driver().payouts(Net.to(
                    payouts -> fill(summary, payouts), Ui::fail)), Ui::fail));
        }, Ui::fail));
    }

    private void fill(final EarningsSummaryDto summary, List<PayoutDto> payouts) {
        page.removeAll();
        final String currency = summary.currency;
        Container balance = new Container(BoxLayout.y());
        balance.setUIID("WlPanel");
        balance.add(Ui.label("Ready to cash out", "WlStatLabel"));
        Label amount = Ui.plain(Ui.money(summary.balance, currency), "WlChartValue");
        amount.setName("balance");
        balance.add(amount);
        final boolean banked = account != null && account.accountLast4 != null
                && account.accountLast4.length() > 0;
        Button cash = Ui.primary("Cash out", "cashOut", e -> {
            if (!banked) {
                account();
                return;
            }
            Api.driver().cashOut(Net.to(sent -> {
                Telemetry.payoutRequested(sent.amount);
                Ui.say(Lang.tr("{0} is on its way to your bank", Ui.money(sent.amount,
                        currency)));
                load();
            }, Ui::fail));
        });
        cash.setEnabled(summary.balance > 0);
        balance.add(cash);
        page.add(balance);

        StatRow periods = Ui.stats(3);
        periods.stat(Ui.money(summary.today, currency), "Today", "earnedToday");
        periods.stat(Ui.money(summary.week, currency), "This week", "earnedWeek");
        periods.stat(Ui.money(summary.month, currency), "This month", "earnedMonth");
        page.add(periods);

        Ui.section(page, "The last two weeks");
        int days = summary.daily == null ? 0 : summary.daily.size();
        double[] amounts = new double[days];
        for (int iter = 0; iter < days; iter++) {
            amounts[iter] = summary.daily.get(iter).amount;
        }
        page.add(Chart.of("daily", amounts, Lang.tr("{0} days ago", String.valueOf(
                Math.max(1, days - 1))), Lang.tr("Today")));

        StatRow numbers = Ui.stats(2);
        numbers.stat(String.valueOf(summary.trips), "Trips", "trips");
        numbers.stat(Ui.money(summary.tips, currency), "Tips", "tips");
        numbers.stat(summary.rating > 0 ? Ui.tenths(summary.rating) : "-", "Rating",
                "rating");
        numbers.stat(Math.round(summary.acceptanceRate * 100d) + "%", "Requests accepted",
                "acceptance");
        page.add(numbers);

        Ui.section(page, "Payouts");
        page.add(Ui.tap(Ui.row(FontImage.MATERIAL_ACCOUNT_BALANCE,
                banked ? account.bankName : Lang.tr("Add a bank account"),
                banked ? Lang.tr("Account ending {0}", account.accountLast4)
                        : Lang.tr("Where your earnings are sent"), null), "payoutAccount",
                e -> account()));
        for (int iter = 0; payouts != null && iter < payouts.size(); iter++) {
            PayoutDto payout = payouts.get(iter);
            Container row = Ui.row(FontImage.MATERIAL_NORTH_EAST, Ui.when(payout.createdAt),
                    payout.destination, Ui.plain(Ui.money(payout.amount, currency),
                            "WlRowValue"));
            row.setName("payout-" + iter);
            page.add(row);
        }
        if (payouts == null || payouts.isEmpty()) {
            page.add(Ui.label("Nothing has been paid out yet.", "WlMuted"));
        }
        Ui.refresh(form);
    }

    /// Where payouts go. The account number is typed in full and only its
    /// last four digits leave the phone: the rest is the bank's business, and
    /// a sample server has no bank.
    private void account() {
        final Form edit = Ui.form("Bank account", "PayoutAccount");
        Ui.back(edit, form);
        Container fields = Ui.page();
        final TextField holder = Ui.field(fields, "Account holder", "holder", "");
        final TextField bank = Ui.field(fields, "Bank", "bankName", "");
        final TextField number = Ui.field(fields, "Account number", "accountNumber", "",
                TextArea.NUMERIC);
        if (account != null) {
            holder.setText(account.holder);
            bank.setText(account.bankName);
        }
        final SpanLabel error = Ui.text("", "WlError");
        fields.add(error);
        fields.add(Ui.primary("Save", "submit", e -> {
            String digits = Cards.digits(number.getText());
            if (digits.length() < 4) {
                error.setText(Lang.tr("Type the account number."));
                edit.revalidate();
                return;
            }
            PayoutAccountDto wanted = new PayoutAccountDto();
            wanted.holder = holder.getText().trim();
            wanted.bankName = bank.getText().trim();
            wanted.accountLast4 = digits.substring(digits.length() - 4);
            Api.driver().savePayoutAccount(wanted, Net.to(saved -> {
                form.showBack();
                load();
            }, (status, message) -> {
                error.setText(message);
                edit.revalidate();
            }));
        }));
        edit.add(BorderLayout.CENTER, fields);
        edit.show();
    }
}
