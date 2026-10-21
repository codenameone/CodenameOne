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
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bastion.api.VerificationDto;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;
import com.codenameone.examples.bastion.net.Session;

/// The two checks an operator runs: that the audit log's hash chain is
/// unbroken, and that every ledger entry balances and every balance is the
/// sum of its postings.
///
/// Each is recomputed by the server from the rows themselves, not read from a
/// flag, so a row edited behind the application's back shows up here as a
/// named problem. Reached only from the menu of an account with the admin
/// role, and refused by the server to anyone else.
public final class AdminForm {
    private AdminForm() {
    }

    /// `back` is null when this is the admin's home screen.
    public static void show(Runnable back) {
        Form form = Nav.form("Administration", back);
        if (Session.user() == null || !Session.user().admin()) {
            form.add(Ui.empty("Administrators only."));
            Ui.show(form, false);
            return;
        }
        form.add(Ui.muted(Lang.tr("Recomputed from the stored rows each time you ask.")));
        final Container audit = new Container(BoxLayout.y());
        final Container ledger = new Container(BoxLayout.y());
        Button verifyAudit = Ui.primary("Verify the audit log");
        verifyAudit.addActionListener(evt -> {
            Ui.loading(audit);
            Api.admin().verifyAudit(Net.to((VerificationDto v) -> result(audit, v,
                    "The audit chain is intact."),
                    (status, message) -> Ui.failed(audit, message, () -> { })));
        });
        Button verifyLedger = Ui.primary("Verify the ledger");
        verifyLedger.addActionListener(evt -> {
            Ui.loading(ledger);
            Api.admin().verifyLedger(Net.to((VerificationDto v) -> result(ledger, v,
                    "Every entry balances and every balance matches its postings."),
                    (status, message) -> Ui.failed(ledger, message, () -> { })));
        });
        form.add(Ui.section("Audit log"));
        form.addAll(verifyAudit, audit);
        form.add(Ui.section("Ledger"));
        form.addAll(verifyLedger, ledger);
        Ui.show(form, true);
    }

    private static void result(Container box, VerificationDto v, String good) {
        box.removeAll();
        if (v.intact()) {
            box.add(Ui.row(Lang.tr(good), null, Lang.tr("Intact"), "BnBadgeGood", null));
        } else {
            box.add(Ui.row(Lang.tr("Problems found"), null, String.valueOf(v.problems().size()),
                    "BnBadgeBad", null));
            for (String problem : v.problems()) {
                box.add(Ui.muted(problem));
            }
        }
        box.revalidate();
    }
}
