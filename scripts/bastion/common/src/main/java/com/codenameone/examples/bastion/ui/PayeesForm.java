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
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bastion.api.NewPayeeDto;
import com.codenameone.examples.bastion.api.PayeeDto;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;
import com.codenameone.examples.bastion.net.Session;
import com.codenameone.examples.bastion.security.RequestSigner;
import java.util.List;

/// The people the user pays.
///
/// Adding one is signed by the device, because a payee is where stolen money
/// goes: an attacker with a session alone cannot add their own account. A
/// new payee then **cools off** -- for a while it may receive only a small
/// amount -- so even a signed add made under duress buys the owner time. The
/// list shows each payee's cooling-off end; renaming and removing are not
/// signed, since neither can send money anywhere new.
public final class PayeesForm {
    private static final String TARGET = "/api/payees";

    private PayeesForm() {
    }

    public static void show(final Runnable back) {
        Form form = Nav.form("Payees", back);
        final Container content = new Container(BoxLayout.y());
        form.add(content);
        Ui.show(form, true);
        load(content);
    }

    private static void load(final Container content) {
        Ui.loading(content);
        Api.banking().payees(Net.to((List<PayeeDto> payees) -> fill(content, payees),
                (status, message) -> Ui.failed(content, message, () -> load(content))));
    }

    private static void fill(final Container content, List<PayeeDto> payees) {
        content.removeAll();
        Button add = Ui.primary("Add a payee");
        add.addActionListener(evt -> add(content));
        content.add(add);
        if (payees.isEmpty()) {
            content.add(Ui.empty("No payees yet. Add one by the e-mail address they bank with."));
        }
        long now = System.currentTimeMillis();
        for (final PayeeDto payee : payees) {
            boolean cooling = payee.coolingOffUntil() > now;
            String detail = payee.email() + (cooling
                    ? "\n" + Lang.tr("New payee: limited until {0}", Ui.when(payee.coolingOffUntil()))
                    : "");
            content.add(Ui.row(payee.nickname(), detail, Lang.tr(cooling ? "Cooling off" : "Ready"),
                    cooling ? "BnBadgeWarn" : "BnBadgeGood", () -> manage(content, payee)));
        }
        content.revalidate();
    }

    private static void add(final Container content) {
        final TextField email = Ui.field("Their e-mail", TextField.EMAILADDR);
        final TextField nickname = Ui.field("Name to show", TextField.ANY);
        Container body = new Container(BoxLayout.y());
        body.addAll(email, nickname);
        Command ok = new Command(Lang.tr("Add"));
        if (Dialog.show(Lang.tr("Add a payee"), body, ok, new Command(Lang.tr("Cancel"))) != ok) {
            return;
        }
        final NewPayeeDto payee = new NewPayeeDto(Session.accountName(email.getText()),
                nickname.getText().trim());
        DeviceSetup.withDevice(failed -> Api.banking().addPayee(
                RequestSigner.sign("POST", TARGET, null, payee), payee,
                Net.to((PayeeDto added) -> {
                    Ui.toast("Payee added. It is limited while it cools off.");
                    load(content);
                }, failed)),
                (status, message) -> Dialog.show(Lang.tr("Not added"), Lang.tr(message),
                        Lang.tr("OK"), null));
    }

    private static void manage(final Container content, final PayeeDto payee) {
        Command rename = new Command(Lang.tr("Rename"));
        Command remove = new Command(Lang.tr("Remove"));
        Command picked = Dialog.show(payee.nickname(), payee.email(), rename, remove,
                new Command(Lang.tr("Cancel")));
        Net.Failed failed = (status, message) -> Dialog.show(Lang.tr("Not changed"),
                Lang.tr(message), Lang.tr("OK"), null);
        if (picked == rename) {
            TextField name = Ui.field("Name to show", TextField.ANY);
            name.setText(payee.nickname());
            Command ok = new Command(Lang.tr("Save"));
            if (Dialog.show(Lang.tr("Rename"), name, ok, new Command(Lang.tr("Cancel"))) == ok) {
                final NewPayeeDto renamed = new NewPayeeDto(payee.email(), name.getText().trim());
                DeviceSetup.withDevice(retry -> Api.banking().renamePayee(payee.id(),
                        RequestSigner.sign("PUT", TARGET + "/" + payee.id(), null, renamed), renamed,
                        Net.to((PayeeDto done) -> load(content), retry)), failed);
            }
        } else if (picked == remove && Ui.confirm("Remove payee",
                Lang.tr("Remove {0}? Adding them again starts a new cooling-off.", payee.nickname()),
                "Remove")) {
            DeviceSetup.withDevice(retry -> Api.banking().removePayee(payee.id(),
                    RequestSigner.sign("DELETE", TARGET + "/" + payee.id(), null, null),
                    Net.to((PayeeDto done) -> load(content), retry)), failed);
        }
    }
}
