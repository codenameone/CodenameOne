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
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bastion.api.AccountDto;
import com.codenameone.examples.bastion.api.CardDto;
import com.codenameone.examples.bastion.api.CashDto;
import com.codenameone.examples.bastion.api.PostingDto;
import com.codenameone.examples.bastion.api.ReceiptDto;
import com.codenameone.examples.bastion.money.Amounts;
import com.codenameone.examples.bastion.money.Assets;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;
import com.codenameone.examples.bastion.security.RequestSigner;
import java.util.List;

/// One account: its balance, what it can do, and its history.
///
/// The history is the account's side of each ledger entry -- a posting --
/// newest first, as the server keeps them. Every amount is the posting's own
/// signed number of units; nothing is summed here, so what is shown is what
/// the ledger says, not what the app worked out.
public final class AccountForm {
    private AccountForm() {
    }

    public static void show(final String accountId, final Runnable back) {
        Form form = Nav.form("Account", back);
        final Container content = new Container(BoxLayout.y());
        form.add(content);
        Ui.show(form, true);
        load(accountId, content, back);
    }

    private static void load(final String accountId, final Container content, final Runnable back) {
        Ui.loading(content);
        Api.banking().accounts(Net.to((List<AccountDto> accounts) -> {
            AccountDto account = null;
            for (AccountDto one : accounts) {
                if (one.id().equals(accountId)) {
                    account = one;
                }
            }
            if (account == null) {
                Ui.failed(content, "No such account", back);
                return;
            }
            final AccountDto found = account;
            Api.banking().history(accountId, Net.to(
                    (List<PostingDto> postings) -> fill(content, found, postings, back),
                    (status, message) -> Ui.failed(content, message,
                            () -> load(accountId, content, back))));
        }, (status, message) -> Ui.failed(content, message, () -> load(accountId, content, back))));
    }

    private static void fill(final Container content, final AccountDto account,
            List<PostingDto> postings, final Runnable back) {
        content.removeAll();
        final Runnable again = () -> load(account.id(), content, back);
        Container hero = new Container(BoxLayout.y(), "BnHero");
        hero.add(new Label(account.name(), "BnHeroLabel"));
        hero.add(new Label(Amounts.format(account.balance(), account.asset()), "BnHeroAmount"));
        hero.add(new Label(HomeForm.kindName(account.kind()) + " - " + account.asset()
                + (account.frozen() ? " - " + Lang.tr("Frozen") : ""), "BnHeroNote"));
        content.add(hero);

        Button move = Ui.primary("Move money");
        move.addActionListener(evt -> TransferForm.show(account.id(), false, again));
        if (Assets.isCrypto(account.asset())) {
            Button send = Ui.secondary("Send");
            send.addActionListener(evt -> WithdrawForm.show(account.id(), again));
            content.add(Ui.across(move, send));
        } else {
            Button deposit = Ui.secondary("Add money");
            deposit.addActionListener(evt -> cash(account, true, again));
            Button withdraw = Ui.secondary("Withdraw");
            withdraw.addActionListener(evt -> cash(account, false, again));
            content.add(Ui.across(move, deposit, withdraw));
        }
        if ("CHECKING".equals(account.kind())) {
            Button card = Ui.secondary("Order a card");
            card.addActionListener(evt -> Api.banking().issueCard(account.id(), Net.to(
                    (CardDto issued) -> CardsForm.show(again),
                    (status, message) -> Dialog.show(Lang.tr("No card"), Lang.tr(message),
                            Lang.tr("OK"), null))));
            content.add(card);
        }

        content.add(Ui.section("History"));
        if (postings.isEmpty()) {
            content.add(Ui.empty("Nothing has moved in this account yet."));
        }
        for (PostingDto posting : postings) {
            boolean in = posting.amount() > 0;
            String amount = (in ? "+" : "") + Amounts.format(posting.amount(), posting.asset());
            content.add(Ui.row(Lang.tr(in ? "Money in" : "Money out"), Ui.when(posting.createdAt()),
                    amount, in ? "BnAmountIn" : "BnAmountOut", null));
        }
        content.revalidate();
    }

    /// Cash in or out at the counter -- the sample's stand-in for a card top-up
    /// or an ATM. Withdrawing is money leaving the customer, so it is signed;
    /// a deposit is not.
    private static void cash(final AccountDto account, final boolean deposit, final Runnable done) {
        final TextField amount = Ui.field("Amount", TextField.DECIMAL);
        Command go = new Command(Lang.tr(deposit ? "Add" : "Withdraw"));
        if (Dialog.show(Lang.tr(deposit ? "Add money" : "Withdraw cash"), amount, go,
                new Command(Lang.tr("Cancel"))) != go) {
            return;
        }
        long units = Amounts.parse(amount.getText(), account.asset());
        if (units <= 0) {
            Dialog.show(Lang.tr("Amount"), Lang.tr("Enter an amount, like 25.00."), Lang.tr("OK"), null);
            return;
        }
        final CashDto cash = new CashDto(units, deposit ? "Deposit" : "Cash withdrawal");
        // One key for this action, kept across the retry after registering
        // AND across a second dialog for the same amount after a failure whose
        // answer was lost -- as TransferForm does -- so trying again cannot
        // move the money twice. A success forgets it: the next one is new.
        final String key = keyFor(account.id() + (deposit ? "|in|" : "|out|") + units);
        DeviceSetup.withDevice(failed -> {
            Net.Ok<ReceiptDto> ok = receipt -> {
                forgetKey();
                done.run();
            };
            if (deposit) {
                Api.banking().deposit(account.id(), key, cash, Net.to(ok, failed));
            } else {
                String target = "/api/accounts/" + account.id() + "/withdraw";
                Api.banking().withdraw(account.id(), key, RequestSigner.sign("POST", target, key,
                        cash),
                        cash, Net.to(ok, failed));
            }
        }, (status, message) -> Dialog.show(Lang.tr("Not done"), Lang.tr(message),
                Lang.tr("OK"), null));
    }

    private static String triedAction;
    private static String triedKey;

    /// The idempotency key for `action` (account, direction and amount): the
    /// one the last unfinished try of the same action used, else a new one.
    static String keyFor(String action) {
        if (triedKey == null || !action.equals(triedAction)) {
            triedAction = action;
            triedKey = RequestSigner.idempotencyKey();
        }
        return triedKey;
    }

    static void forgetKey() {
        triedAction = null;
        triedKey = null;
    }
}
