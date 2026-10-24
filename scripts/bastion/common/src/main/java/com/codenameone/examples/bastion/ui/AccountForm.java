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
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
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
            box.addComponent(1, Charts.line("account-balance-chart", days, values,
                    Assets.REPORTING.equals(account.asset()) ? "$" : ""));
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
        form.add(Ui.row(describe(posting), category(posting.kind()), null, null, null));
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

    /// The statement's columns, by key; the header of each is its English
    /// name in [#COLUMN_NAMES], and [#COLUMN_WEIGHTS] its share of the width.
    static final String[] COLUMNS = {"date", "description", "category", "reference", "in", "out",
        "amount", "balance"};
    private static final String[] COLUMN_NAMES = {"Date", "Description", "Category", "Reference",
        "Money in", "Money out", "Amount", "Balance"};
    private static final int[] COLUMN_WEIGHTS = {3, 5, 3, 3, 3, 3, 3, 3};
    private static final boolean[] COLUMN_NUMERIC = {false, false, false, false, true, true, true,
        true};
    /// What a phone in portrait shows until the user picks: the date, what
    /// the line was and its signed amount. A tap on a line shows the rest.
    static final String NARROW_DEFAULT = "date,description,amount";
    /// What a wider window shows: everything but the signed amount, which
    /// the money in and money out columns already say.
    static final String WIDE_DEFAULT = "date,description,category,reference,in,out,balance";

    /// A window this wide (in mm) has room for the wide set of columns.
    private static final float WIDE_MM = 120f;

    /// A statement: a header with the account, the period and its totals,
    /// then every posting oldest first with the running balance, in the
    /// columns the window has room for -- or the ones the user picked.
    private static void statement(final AccountDto account, final List<PostingDto> postings,
            final Runnable back) {
        final Form form = Nav.form("Statement", back);
        final Runnable stay = () -> Ui.show(form, true);
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
        final String asset = account.asset();
        Container head = new Container(BoxLayout.y(), "BnStatementHead");
        head.add(new Label(account.name(), "BnRowTitle"));
        String period = postings.isEmpty() ? "" : Lang.tr("{0} to {1}",
                Ui.day(postings.get(postings.size() - 1).createdAt()),
                Ui.day(postings.get(0).createdAt()));
        Ui.addDetail(head, HomeForm.kindName(account.kind()) + " - " + asset
                + (period.length() > 0 ? " - " + period : ""));
        boolean wide = CN.getDisplayWidth() >= CN.convertToPixels(WIDE_MM);
        Container totals = new Container(new com.codename1.ui.layouts.GridLayout(wide ? 1 : 2,
                wide ? 4 : 2));
        totals.add(fact("Opening balance", Amounts.format(opening, asset)));
        totals.add(fact("Closing balance", Amounts.format(account.balance(), asset)));
        totals.add(fact("Money in", Amounts.format(in, asset)));
        totals.add(fact("Money out", Amounts.format(out, asset)));
        head.add(totals);
        form.add(head);
        final Container holder = new Container(BoxLayout.y());
        form.add(holder);
        final long openingBalance = opening;
        final Runnable fill = () -> {
            holder.removeAll();
            holder.add(statementTable(account, postings, openingBalance, stay));
            form.revalidate();
        };
        fill.run();
        form.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_VIEW_COLUMN,
                evt -> {
                    if (chooseColumns()) {
                        fill.run();
                    }
                });
        // Turning a phone, or resizing a desktop window, changes what fits.
        final boolean[] wasWide = {wide};
        form.addSizeChangedListener(evt -> {
            boolean now = CN.getDisplayWidth() >= CN.convertToPixels(WIDE_MM);
            if (now != wasWide[0]) {
                wasWide[0] = now;
                fill.run();
            }
        });
        Ui.show(form, true);
    }

    private static Container fact(String label, String value) {
        Container fact = new Container(BoxLayout.y(), "BnFact");
        com.codename1.components.SpanLabel name = new com.codename1.components.SpanLabel(
                Lang.tr(label));
        name.setTextUIID("BnFactLabel");
        fact.add(name);
        fact.add(new Label(value, "BnFactValue"));
        return fact;
    }

    /// The preference that holds the columns picked for this width of window.
    private static String columnsKey() {
        return CN.getDisplayWidth() >= CN.convertToPixels(WIDE_MM)
                ? "bastion.statement.columns.wide" : "bastion.statement.columns.narrow";
    }

    /// The column keys to show, in table order.
    static String[] chosenColumns() {
        boolean wide = CN.getDisplayWidth() >= CN.convertToPixels(WIDE_MM);
        String saved = com.codename1.io.Preferences.get(columnsKey(),
                wide ? WIDE_DEFAULT : NARROW_DEFAULT);
        return pickColumns(saved);
    }

    /// The known keys in `csv`, in the table's order; the date always.
    static String[] pickColumns(String csv) {
        String list = "," + (csv == null ? "" : csv) + ",";
        int count = 0;
        boolean[] on = new boolean[COLUMNS.length];
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            on[iter] = iter == 0 || list.indexOf("," + COLUMNS[iter] + ",") >= 0;
            if (on[iter]) {
                count++;
            }
        }
        String[] out = new String[count];
        int at = 0;
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            if (on[iter]) {
                out[at++] = COLUMNS[iter];
            }
        }
        return out;
    }

    /// Asks which columns to show; answers whether the choice changed.
    private static boolean chooseColumns() {
        String[] current = chosenColumns();
        Container box = new Container(BoxLayout.y());
        com.codename1.ui.CheckBox[] boxes = new com.codename1.ui.CheckBox[COLUMNS.length];
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            boxes[iter] = new com.codename1.ui.CheckBox(Lang.tr(COLUMN_NAMES[iter]));
            boxes[iter].setUIID("BnCheck");
            boxes[iter].setSelected(Ui.indexOf(current, COLUMNS[iter]) >= 0);
            // The date says which line is which; it is always there.
            boxes[iter].setEnabled(iter != 0);
            box.add(boxes[iter]);
        }
        Command ok = new Command(Lang.tr("Show"));
        if (Dialog.show(Lang.tr("Columns"), box, ok, new Command(Lang.tr("Cancel"))) != ok) {
            return false;
        }
        StringBuilder csv = new StringBuilder();
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            if (boxes[iter].isSelected() || iter == 0) {
                csv.append(csv.length() == 0 ? "" : ",").append(COLUMNS[iter]);
            }
        }
        com.codename1.io.Preferences.set(columnsKey(), csv.toString());
        return true;
    }

    private static Component statementTable(final AccountDto account, List<PostingDto> postings,
            long opening, final Runnable stay) {
        String asset = account.asset();
        String[] keys = chosenColumns();
        String[] headers = new String[keys.length];
        int[] weights = new int[keys.length];
        boolean[] numeric = new boolean[keys.length];
        int weight = 0;
        for (int iter = 0; iter < keys.length; iter++) {
            int column = Ui.indexOf(COLUMNS, keys[iter]);
            headers[iter] = COLUMN_NAMES[column];
            weights[iter] = COLUMN_WEIGHTS[column];
            numeric[iter] = COLUMN_NUMERIC[column];
            weight += weights[iter];
        }
        // Up to the width of the narrow default the columns are fitted to the
        // window; more than that scrolls sideways rather than squeeze.
        final boolean fitted = weight <= 11;
        Table table = new Table(headers, weights, numeric, fitted);
        long running = opening;
        for (int iter = postings.size() - 1; iter >= 0; iter--) {
            final PostingDto posting = postings.get(iter);
            running += posting.amount();
            final long after = running;
            boolean credit = posting.amount() > 0;
            String[] cells = new String[keys.length];
            String[] styles = new String[keys.length];
            for (int c = 0; c < keys.length; c++) {
                String key = keys[c];
                if ("date".equals(key)) {
                    // A fitted phone column has room for the day; the time
                    // is on the line's own screen, a tap away.
                    cells[c] = fitted ? Ui.day(posting.createdAt()) : Ui.when(posting.createdAt());
                } else if ("description".equals(key)) {
                    cells[c] = describe(posting);
                } else if ("category".equals(key)) {
                    cells[c] = category(posting.kind());
                } else if ("reference".equals(key)) {
                    cells[c] = shortReference(posting.entryId());
                } else if ("in".equals(key)) {
                    cells[c] = credit ? Amounts.format(posting.amount(), asset) : "";
                } else if ("out".equals(key)) {
                    cells[c] = credit ? "" : Amounts.format(-posting.amount(), asset);
                    styles[c] = "BnCellNumOut";
                } else if ("amount".equals(key)) {
                    cells[c] = (credit ? "+" : "") + Amounts.format(posting.amount(), asset);
                    styles[c] = credit ? null : "BnCellNumOut";
                } else {
                    cells[c] = Amounts.format(after, asset);
                }
            }
            // Every line opens in full: on a phone that is how the columns
            // left out are read.
            table.add(cells, "BnTableRow", styles, () -> detail(account, posting, after, stay));
        }
        return table.component();
    }

    /// What a line was: the entry's description (a merchant, the sender's
    /// note), else what kind of movement it was.
    static String describe(PostingDto posting) {
        String memo = posting.memo();
        if (memo != null && memo.length() > 0) {
            return ReceiptForm.memo(memo);
        }
        return category(posting.kind());
    }

    /// The ledger's kind of an entry, in words.
    static String category(String kind) {
        if ("CARD".equals(kind)) {
            return Lang.tr("Card");
        }
        if ("TRANSFER".equals(kind) || "PAYMENT".equals(kind)) {
            return Lang.tr("Transfer");
        }
        if ("DEPOSIT".equals(kind) || "CAPITAL".equals(kind)) {
            return Lang.tr("Deposit");
        }
        if ("WITHDRAWAL".equals(kind)) {
            return Lang.tr("Cash withdrawal");
        }
        if ("CRYPTO_OUT".equals(kind)) {
            return Lang.tr("Crypto withdrawal");
        }
        if ("TRADE".equals(kind) || "ORDER_HOLD".equals(kind) || "ORDER_RELEASE".equals(kind)) {
            return Lang.tr("Trade");
        }
        return Lang.tr("Other");
    }

    /// A reference short enough for a column: its first eight characters,
    /// which a support agent can search by. The full one is on the line's
    /// own screen.
    static String shortReference(String entryId) {
        if (entryId == null) {
            return "";
        }
        return entryId.length() <= 8 ? entryId : entryId.substring(0, 8);
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
