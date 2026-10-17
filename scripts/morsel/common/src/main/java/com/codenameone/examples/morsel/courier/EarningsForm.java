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
package com.codenameone.examples.morsel.courier;

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
import com.codenameone.examples.morsel.Telemetry;
import com.codenameone.examples.morsel.api.EarningsSummaryDto;
import com.codenameone.examples.morsel.api.PayoutAccountDto;
import com.codenameone.examples.morsel.api.PayoutDto;
import com.codenameone.examples.morsel.net.Api;
import com.codenameone.examples.morsel.net.Net;
import com.codenameone.examples.morsel.pay.Cards;
import com.codenameone.examples.morsel.ui.Chart;
import com.codenameone.examples.morsel.ui.Lang;
import com.codenameone.examples.morsel.ui.StatRow;
import com.codenameone.examples.morsel.ui.Ui;
import java.util.List;

/// What onboarding has paid: what is waiting to be cashed out, the last
/// fortnight a day at a time, and the payouts already sent.
///
/// The sums are the server's. It works them out from the deliveries it recorded,
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
        Api.courier().payoutAccount(Net.to(found -> {
            account = found;
            Api.courier().earningsSummary(Net.to(summary -> Api.courier().payouts(Net.to(
                    payouts -> fill(summary, payouts), Ui::fail)), Ui::fail));
        }, Ui::fail));
    }

    private void fill(final EarningsSummaryDto summary, List<PayoutDto> payouts) {
        page.removeAll();
        final String currency = summary.currency();
        Container balance = new Container(BoxLayout.y());
        balance.setUIID("WlPanel");
        balance.add(Ui.label("Ready to cash out", "WlStatLabel"));
        Label amount = Ui.plain(Ui.money(summary.balance(), currency), "WlChartValue");
        amount.setName("balance");
        balance.add(amount);
        final boolean banked = account != null && account.accountLast4() != null
                && account.accountLast4().length() > 0;
        Button cash = Ui.primary("Cash out", "cashOut", e -> {
            if (!banked) {
                account();
                return;
            }
            Api.courier().cashOut(Net.to(sent -> {
                Telemetry.payoutRequested(sent.amount());
                Ui.say(Lang.tr("{0} is on its way to your bank", Ui.money(sent.amount(),
                        currency)));
                load();
            }, Ui::fail));
        });
        cash.setEnabled(summary.balance() > 0);
        balance.add(cash);
        page.add(balance);

        StatRow periods = Ui.stats(3);
        periods.stat(Ui.money(summary.today(), currency), "Today", "earnedToday");
        periods.stat(Ui.money(summary.week(), currency), "This week", "earnedWeek");
        periods.stat(Ui.money(summary.month(), currency), "This month", "earnedMonth");
        page.add(periods);

        Ui.section(page, "The last two weeks");
        int days = summary.daily() == null ? 0 : summary.daily().size();
        double[] amounts = new double[days];
        for (int iter = 0; iter < days; iter++) {
            amounts[iter] = summary.daily().get(iter).amount();
        }
        page.add(Chart.of("daily", amounts, Lang.tr("{0} days ago", String.valueOf(
                Math.max(1, days - 1))), Lang.tr("Today")));

        StatRow numbers = Ui.stats(2);
        numbers.stat(String.valueOf(summary.trips()), "Trips", "trips");
        numbers.stat(Ui.money(summary.tips(), currency), "Tips", "tips");
        numbers.stat(summary.rating() > 0 ? Ui.tenths(summary.rating()) : "-", "Rating",
                "rating");
        numbers.stat(Math.round(summary.acceptanceRate() * 100d) + "%", "Requests accepted",
                "acceptance");
        page.add(numbers);

        Ui.section(page, "Payouts");
        page.add(Ui.tap(Ui.row(FontImage.MATERIAL_ACCOUNT_BALANCE,
                banked ? account.bankName() : Lang.tr("Add a bank account"),
                banked ? Lang.tr("Account ending {0}", account.accountLast4())
                        : Lang.tr("Where your earnings are sent"), null), "payoutAccount",
                e -> account()));
        for (int iter = 0; payouts != null && iter < payouts.size(); iter++) {
            PayoutDto payout = payouts.get(iter);
            Container row = Ui.row(FontImage.MATERIAL_NORTH_EAST, Ui.when(payout.createdAt()),
                    payout.destination(), Ui.plain(Ui.money(payout.amount(), currency),
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
            holder.setText(account.holder());
            bank.setText(account.bankName());
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
            PayoutAccountDto wanted = new PayoutAccountDto(holder.getText().trim(),
                    bank.getText().trim(), digits.substring(digits.length() - 4));
            Api.courier().savePayoutAccount(wanted, Net.to(saved -> {
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
