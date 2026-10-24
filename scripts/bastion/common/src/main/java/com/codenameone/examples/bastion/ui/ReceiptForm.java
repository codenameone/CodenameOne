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
package com.codenameone.examples.bastion.ui;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bastion.api.AccountDto;
import com.codenameone.examples.bastion.api.ReceiptDto;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;
import java.util.List;
import com.codenameone.examples.bastion.money.Amounts;

/// What the server booked: the ledger entry's id is the receipt's reference,
/// the one a support agent would ask for.
public final class ReceiptForm {
    /// The memo the app sends with a counter deposit and withdrawal: a code,
    /// so the text is the reader's language, not the sender's.
    static final String MEMO_DEPOSIT = "@cash-in";
    static final String MEMO_WITHDRAWAL = "@cash-out";

    private ReceiptForm() {
    }

    /// A memo as shown: the app's own codes translated, anything a customer
    /// typed as it is.
    static String memo(String memo) {
        if (MEMO_DEPOSIT.equals(memo)) {
            return Lang.tr("Deposit");
        }
        if (MEMO_WITHDRAWAL.equals(memo)) {
            return Lang.tr("Cash withdrawal");
        }
        return memo;
    }

    /// The receipt of a movement the user just made. `toName` is who or
    /// what it went to, as the screen that sent it named it (a payee's
    /// nickname, an account, an address); `done` closes it.
    public static void show(final ReceiptDto receipt, String toName, final Runnable done) {
        // The end of a flow: a close button, no menu, and a title that says
        // what this is rather than what was pressed.
        Form form = Ui.finished("Receipt", done);
        String asset = receipt.asset();
        Container hero = new Container(BoxLayout.y(), "BnHero");
        hero.add(new Label(Lang.tr("Sent"), "BnHeroLabel"));
        hero.add(new Label(Amounts.format(receipt.amount(), asset), "BnHeroAmount"));
        hero.add(new Label(Ui.when(receipt.createdAt()), "BnHeroNote"));
        form.add(hero);
        // Booked is final: the ledger entry exists, and the receipt is its.
        form.add(Ui.row(Lang.tr("Status"), null, Lang.tr("Completed"), "BnAmountIn", null));
        final Container from = Ui.row(Lang.tr("From"), null, "", "BnRowValue", null);
        form.add(from);
        if (toName != null && toName.length() > 0) {
            // A name fits beside its label; an address wraps under it.
            form.add(toName.length() <= 24 ? Ui.row(Lang.tr("To"), null, toName, null, null)
                    : Ui.fact(Lang.tr("To"), toName));
        }
        form.add(Ui.row(Lang.tr("Amount"), null, Amounts.format(receipt.amount(), asset), null,
                null));
        // The bank charges nothing for a movement; the line says so rather
        // than leave the reader wondering.
        form.add(Ui.row(Lang.tr("Fee"), null, Amounts.format(0L, asset), "BnRowValueMuted", null));
        final Container balance = Ui.row(Lang.tr("Balance after"), null, "", null, null);
        form.add(balance);
        form.add(Ui.row(Lang.tr("When"), null, Ui.when(receipt.createdAt()), null, null));
        if (receipt.memo() != null && receipt.memo().length() > 0) {
            form.add(Ui.fact(Lang.tr("Note"), memo(receipt.memo())));
        }
        form.add(Ui.fact(Lang.tr("Reference"), receipt.entryId()));
        Button ok = Ui.primary("Done");
        ok.addActionListener(evt -> done.run());
        form.add(ok);
        Ui.show(form, true);
        // The account it left and what that account holds now.
        Api.banking().accounts(Net.to((List<AccountDto> accounts) -> {
            for (AccountDto account : accounts) {
                if (account.id().equals(receipt.fromAccountId())) {
                    setValue(from, account.name());
                    setValue(balance, Amounts.format(account.balance(), account.asset()));
                }
            }
        }, (status, message) -> { }));
    }

    /// Sets the value of a row made by [Ui#row].
    private static void setValue(Container row, String value) {
        com.codename1.ui.Component east = row.getLayout() instanceof
                com.codename1.ui.layouts.BorderLayout
                ? ((com.codename1.ui.layouts.BorderLayout) row.getLayout()).getEast() : null;
        if (east instanceof Label) {
            ((Label) east).setText(value);
            row.revalidate();
        }
    }
}
