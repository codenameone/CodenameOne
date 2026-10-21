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
import com.codenameone.examples.bastion.api.ReceiptDto;
import com.codenameone.examples.bastion.money.Amounts;

/// What the server booked: the ledger entry's id is the receipt's reference,
/// the one a support agent would ask for.
public final class ReceiptForm {
    private ReceiptForm() {
    }

    public static void show(ReceiptDto receipt, final Runnable done) {
        Form form = Nav.form("Done", null);
        Container hero = new Container(BoxLayout.y(), "BnHero");
        hero.add(new Label(Lang.tr("Sent"), "BnHeroLabel"));
        hero.add(new Label(Amounts.format(receipt.amount(), receipt.asset()), "BnHeroAmount"));
        form.add(hero);
        form.add(Ui.row(Lang.tr("Reference"), null, receipt.entryId(), "BnRowValueMono", null));
        form.add(Ui.row(Lang.tr("When"), null, Ui.when(receipt.createdAt()), null, null));
        if (receipt.memo() != null && receipt.memo().length() > 0) {
            form.add(Ui.row(Lang.tr("Note"), null, receipt.memo(), null, null));
        }
        Button ok = Ui.primary("Done");
        ok.addActionListener(evt -> done.run());
        form.add(ok);
        Ui.show(form, true);
    }
}
