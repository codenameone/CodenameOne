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
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bastion.api.AccountDto;
import com.codenameone.examples.bastion.api.PayeeDto;
import com.codenameone.examples.bastion.api.ReceiptDto;
import com.codenameone.examples.bastion.api.TransferDto;
import com.codenameone.examples.bastion.money.Amounts;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;
import com.codenameone.examples.bastion.security.RequestSigner;
import java.util.ArrayList;
import java.util.List;

/// Moving money: between the user's own accounts, or to a payee.
///
/// Three things make this screen safe to press twice, or to lose the network
/// on:
///
/// - **An idempotency key per payment**, made when the user reviews it and
///   sent with every try of it. A retry after a lost answer is answered with
///   the first receipt; the money moves once.
/// - **A device signature** ([RequestSigner]) over the exact body sent. The
///   server wants one for anything leaving the customer, and -- above a
///   threshold -- for a move between their own accounts too. Without a
///   registered device the refusal is turned into an offer to register.
/// - **The server's word on limits.** A new payee may receive only so much
///   while it cools off, a frozen account sends nothing, a device that lost
///   its trust signs nothing; each is a refusal with its reason, shown as it
///   is rather than second-guessed here.
public final class TransferForm {
    private static final String TARGET = "/api/transfers";

    private TransferForm() {
    }

    /// `fromId` preselects the paying account (null: the first); `toPayee`
    /// opens on paying someone rather than a move between own accounts.
    public static void show(final String fromId, final boolean toPayee, final Runnable back) {
        Form form = Nav.form(toPayee ? "Pay someone" : "Move money", back);
        final Container content = new Container(BoxLayout.y());
        form.add(content);
        Ui.show(form, true);
        load(content, fromId, toPayee, back);
    }

    private static void load(final Container content, final String fromId, final boolean toPayee,
            final Runnable back) {
        Ui.loading(content);
        Api.banking().accounts(Net.to((List<AccountDto> accounts) ->
                Api.banking().payees(Net.to((List<PayeeDto> payees) ->
                        fill(content, accounts, payees, fromId, toPayee, back),
                        (status, message) -> Ui.failed(content, message,
                                () -> load(content, fromId, toPayee, back)))),
                (status, message) -> Ui.failed(content, message,
                        () -> load(content, fromId, toPayee, back))));
    }

    /// The account a move from `fromId` most likely goes to: the first other
    /// account in the same currency (a move cannot change currency; that is a
    /// trade), else the first other account at all.
    static String partner(List<AccountDto> accounts, String fromId) {
        String asset = null;
        for (AccountDto account : accounts) {
            if (account.id().equals(fromId)) {
                asset = account.asset();
            }
        }
        String other = null;
        for (AccountDto account : accounts) {
            if (!account.id().equals(fromId)) {
                if (account.asset().equals(asset)) {
                    return account.id();
                }
                if (other == null) {
                    other = account.id();
                }
            }
        }
        return other != null ? other : fromId;
    }

    private static void fill(final Container content, final List<AccountDto> all,
            final List<PayeeDto> payees, String fromId, boolean toPayee, final Runnable back) {
        content.removeAll();
        // Only accounts that can send: not frozen, and holding something.
        final List<AccountDto> accounts = new ArrayList<AccountDto>();
        int initial = 0;
        for (AccountDto account : all) {
            if (!account.frozen()) {
                if (account.id().equals(fromId)) {
                    initial = accounts.size();
                }
                accounts.add(account);
            }
        }
        if (accounts.isEmpty()) {
            content.add(Ui.empty("You have no account that can send money."));
            content.revalidate();
            return;
        }
        final String[] fromLabels = labels(accounts);
        final String[] accountIds = ids(accounts);
        final Picker from = Ui.picker(accountIds, fromLabels, accountIds[Math.min(initial, accountIds.length - 1)]);
        final Container target = new Container(BoxLayout.y());
        final Picker[] to = new Picker[1];
        final boolean[] payee = {toPayee};
        final String[][] toLabels = new String[1][];
        final String[][] toIds = new String[1][];
        Container mode = Ui.segments(new String[] {"My accounts", "A payee"}, toPayee ? 1 : 0,
                index -> {
                    payee[0] = index == 1;
                    target.removeAll();
                    toLabels[0] = payee[0] ? payeeLabels(payees) : labels(accounts);
                    toIds[0] = payee[0] ? payeeIds(payees) : accountIds;
                    if (toLabels[0].length == 0) {
                        target.add(Ui.empty("Add a payee first, under Payees."));
                        to[0] = null;
                    } else {
                        to[0] = Ui.picker(toIds[0], toLabels[0],
                                payee[0] ? toIds[0][0] : partner(accounts, Ui.pickedId(from)));
                        target.add(to[0]);
                    }
                    target.revalidate();
                });
        final TextField amount = Ui.field("Amount", TextField.DECIMAL);
        final TextField memo = Ui.field("Note (optional)", TextField.ANY);
        final Label error = Ui.error();
        final Button review = Ui.primary("Review");
        // The payment last tried and its key: pressing Send again for the
        // same payment -- after a timeout, say -- reuses the key, so the
        // server answers with the first receipt if the first try got through.
        final TransferDto[] tried = new TransferDto[1];
        final String[] key = new String[1];
        content.add(mode);
        content.add(Ui.section("From"));
        content.add(from);
        content.add(Ui.section("To"));
        content.add(target);
        content.add(Ui.section("Amount"));
        content.addAll(amount, memo, error, review);
        review.addActionListener(evt -> {
            Ui.showError(error, null);
            AccountDto source = accounts.get(Math.max(0, Ui.indexOf(accountIds, Ui.pickedId(from))));
            if (to[0] == null) {
                Ui.showError(error, Lang.tr("Choose who to pay."));
                return;
            }
            int picked = Ui.indexOf(toIds[0], Ui.pickedId(to[0]));
            if (picked < 0) {
                Ui.showError(error, Lang.tr("Choose who to pay."));
                return;
            }
            long units = Amounts.parse(amount.getText(), source.asset());
            if (units <= 0) {
                Ui.showError(error, Lang.tr("Enter an amount, like 25.00."));
                return;
            }
            if (units > source.balance()) {
                Ui.showError(error, Lang.tr("That is more than this account holds."));
                return;
            }
            String toId = null;
            String payeeId = null;
            String toName;
            if (payee[0]) {
                PayeeDto p = payees.get(picked);
                payeeId = p.id();
                toName = p.nickname();
            } else {
                AccountDto dest = accounts.get(picked);
                if (dest.id().equals(source.id())) {
                    Ui.showError(error, Lang.tr("Choose two different accounts."));
                    return;
                }
                toId = dest.id();
                toName = dest.name();
            }
            String note = memo.getText().trim();
            TransferDto transfer = new TransferDto(source.id(), toId, payeeId, units,
                    note.length() == 0 ? null : note);
            if (!Ui.confirm("Confirm", Lang.tr("Send {0} from {1} to {2}?",
                    Amounts.format(units, source.asset()), source.name(), toName), "Send")) {
                return;
            }
            if (tried[0] == null || !tried[0].equals(transfer)) {
                tried[0] = transfer;
                key[0] = RequestSigner.idempotencyKey();
            }
            send(review, error, transfer, key[0], back);
        });
        content.revalidate();
    }

    /// Sends `transfer`; `key` is the payment's idempotency key and stays the
    /// same for the retry that follows registering the device.
    private static void send(final Button review, final Label error, final TransferDto transfer,
            final String key, final Runnable back) {
        review.setEnabled(false);
        DeviceSetup.withDevice(failed -> Api.banking().transfer(key,
                RequestSigner.sign("POST", TARGET, key, transfer), transfer,
                Net.to((ReceiptDto receipt) -> ReceiptForm.show(receipt, back), failed)),
                (status, message) -> {
                    review.setEnabled(true);
                    Ui.showError(error, Lang.tr(message));
                });
    }

    static String[] labels(List<AccountDto> accounts) {
        String[] out = new String[accounts.size()];
        for (int iter = 0; iter < out.length; iter++) {
            AccountDto a = accounts.get(iter);
            out[iter] = a.name() + " (" + Amounts.format(a.balance(), a.asset()) + ")";
        }
        return out;
    }

    static String[] ids(List<AccountDto> accounts) {
        String[] out = new String[accounts.size()];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = accounts.get(iter).id();
        }
        return out;
    }

    private static String[] payeeIds(List<PayeeDto> payees) {
        String[] out = new String[payees.size()];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = payees.get(iter).id();
        }
        return out;
    }

    private static String[] payeeLabels(List<PayeeDto> payees) {
        String[] out = new String[payees.size()];
        for (int iter = 0; iter < out.length; iter++) {
            PayeeDto p = payees.get(iter);
            out[iter] = p.nickname() + " - " + p.email();
        }
        return out;
    }
}
