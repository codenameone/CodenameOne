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
import com.codenameone.examples.bastion.api.BalancePointDto;
import com.codenameone.examples.bastion.api.CashflowDayDto;
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
        // Coming back from a screen that may have moved money shows this form
        // again, reloaded; the drill-downs only read, so they show it as it was.
        final Form self = content.getComponentForm();
        final Runnable stay = () -> Ui.show(self, true);
        final Runnable again = () -> {
            stay.run();
            load(account.id(), content, back);
        };
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
            // Signed: a card is one more way for money to leave the account.
            card.addActionListener(evt -> DeviceSetup.withDevice(retry -> Api.banking()
                    .issueCard(account.id(), RequestSigner.sign("POST", "/api/accounts/"
                            + account.id() + "/card", null, null),
                            Net.to((CardDto issued) -> CardsForm.show(again), retry)),
                    (status, message) -> Dialog.show(Lang.tr("No card"), Lang.tr(message),
                            Lang.tr("OK"), null)));
            content.add(card);
        }

        final Container charts = new Container(BoxLayout.y());
        content.add(charts);
        charts(charts, account);

        Container head = new Container(new com.codename1.ui.layouts.BorderLayout());
        head.add(com.codename1.ui.layouts.BorderLayout.CENTER, Ui.section("History"));
        if (!postings.isEmpty()) {
            Button statement = new Button(Lang.tr("Statement"), "BnLink");
            statement.addActionListener(evt -> statement(account, postings, stay));
            head.add(com.codename1.ui.layouts.BorderLayout.EAST, statement);
        }
        content.add(head);
        if (postings.isEmpty()) {
            content.add(Ui.empty("Nothing has moved in this account yet."));
        }
        long balance = account.balance();
        for (final PostingDto posting : postings) {
            boolean in = posting.amount() > 0;
            String amount = (in ? "+" : "") + Amounts.format(posting.amount(), posting.asset());
            final long after = balance;
            balance -= posting.amount();
            content.add(Ui.row(Lang.tr(in ? "Money in" : "Money out"), Ui.when(posting.createdAt()),
                    amount, in ? "BnAmountIn" : "BnAmountOut",
                    () -> detail(account, posting, after, stay)));
        }
        content.revalidate();
    }

    /// The balance over 30 days and the money in and out each day, drawn
    /// once their answers arrive; the screen does not wait for them.
    private static void charts(final Container box, final AccountDto account) {
        final double scale = Amounts.pow10(Assets.decimals(account.asset()));
        Api.insights().balanceHistory(account.id(), "30", Net.to((List<BalancePointDto> points) -> {
            if (points.size() < 2) {
                return;
            }
            long[] days = new long[points.size()];
            double[] values = new double[days.length];
            for (int iter = 0; iter < days.length; iter++) {
                days[iter] = points.get(iter).day();
                values[iter] = points.get(iter).balance() / scale;
            }
            box.addComponent(0, Ui.section("Balance, 30 days"));
            box.addComponent(1, Charts.line("account-balance-chart", days, values));
            refresh(box);
        }, (status, message) -> { }));
        Api.insights().cashflow(account.asset(), "14", Net.to((List<CashflowDayDto> flows) -> {
            if (flows.isEmpty()) {
                return;
            }
            long[] days = new long[flows.size()];
            double[] in = new double[days.length];
            double[] out = new double[days.length];
            for (int iter = 0; iter < days.length; iter++) {
                days[iter] = flows.get(iter).day();
                in[iter] = flows.get(iter).inflow() / scale;
                out[iter] = Math.abs(flows.get(iter).outflow()) / scale;
            }
            box.add(Ui.section("Money in and out, 14 days"));
            box.add(Charts.inOut("cashflow-chart", days, in, out));
            refresh(box);
        }, (status, message) -> { }));
    }

    private static void refresh(Container box) {
        if (box.getComponentForm() != null) {
            box.getComponentForm().revalidate();
        }
    }

    /// One line of the history in full: the account's side of a ledger entry,
    /// with the balance it left.
    private static void detail(final AccountDto account, PostingDto posting, long after,
            final Runnable back) {
        Form form = Nav.form("Transaction", back);
        boolean in = posting.amount() > 0;
        Container hero = new Container(BoxLayout.y(), "BnHero");
        hero.add(new Label(Lang.tr(in ? "Money in" : "Money out"), "BnHeroLabel"));
        hero.add(new Label((in ? "+" : "") + Amounts.format(posting.amount(), posting.asset()),
                "BnHeroAmount"));
        hero.add(new Label(Ui.when(posting.createdAt()), "BnHeroNote"));
        form.add(hero);
        form.add(Ui.row(Lang.tr("Account"), account.name(), account.asset(), "BnRowValueMuted",
                null));
        form.add(Ui.row(Lang.tr("Side"), Lang.tr(in ? "Credit to your account"
                : "Debit from your account"), in ? "Cr" : "Dr", "BnRowValueMono", null));
        form.add(Ui.row(Lang.tr("Balance after"), null, Amounts.format(after, posting.asset()),
                "BnRowValue", null));
        form.add(Ui.row(Lang.tr("Ledger entry"), posting.entryId(), null, null, null));
        form.add(Ui.row(Lang.tr("Posting"), posting.id(), null, null, null));
        form.add(Ui.muted(Lang.tr("The other side of this entry is in the bank's books; every "
                + "entry's debits equal its credits.")));
        Ui.show(form, true);
    }

    /// A statement: every posting oldest first with the running balance, the
    /// way a bank statement lists them.
    private static void statement(final AccountDto account, List<PostingDto> postings,
            final Runnable back) {
        Form form = Nav.form("Statement", back);
        form.add(new Label(account.name() + " - " + account.asset(), "BnTitle"));
        long opening = account.balance();
        long in = 0;
        long out = 0;
        for (PostingDto posting : postings) {
            opening -= posting.amount();
            if (posting.amount() > 0) {
                in += posting.amount();
            } else {
                out -= posting.amount();
            }
        }
        Table table = new Table(new String[] {"Date", "Description", "Money in", "Money out",
            "Balance"}, new int[] {3, 4, 3, 3, 3},
                new boolean[] {false, false, true, true, true});
        String asset = account.asset();
        table.add(new String[] {"", Lang.tr("Opening balance"), "", "",
            Amounts.format(opening, asset)}, "BnTableTotal", null, null);
        long running = opening;
        for (int iter = postings.size() - 1; iter >= 0; iter--) {
            PostingDto posting = postings.get(iter);
            running += posting.amount();
            boolean credit = posting.amount() > 0;
            table.add(new String[] {Ui.when(posting.createdAt()),
                Lang.tr(credit ? "Money in" : "Money out"),
                credit ? Amounts.format(posting.amount(), asset) : "",
                credit ? "" : Amounts.format(-posting.amount(), asset),
                Amounts.format(running, asset)}, null);
        }
        table.add(new String[] {"", Lang.tr("Closing balance"), Amounts.format(in, asset),
            Amounts.format(out, asset), Amounts.format(account.balance(), asset)},
                "BnTableTotal", null, null);
        form.add(table.component());
        Ui.show(form, true);
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
            Dialog.show(Lang.tr("Amount"), Ui.amountHint(), Lang.tr("OK"), null);
            return;
        }
        // A code, not English: the memo is stored and may be read back in
        // another language ([ReceiptForm#memo]).
        final CashDto cash = new CashDto(units, deposit ? ReceiptForm.MEMO_DEPOSIT
                : ReceiptForm.MEMO_WITHDRAWAL);
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
