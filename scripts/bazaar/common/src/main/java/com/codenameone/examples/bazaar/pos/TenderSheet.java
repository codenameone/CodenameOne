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
package com.codenameone.examples.bazaar.pos;

import com.codename1.appkit.Net;
import com.codename1.components.InfiniteProgress;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.Sheet;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.PosCustomerDto;
import com.codenameone.examples.bazaar.api.PosSaleDto;
import com.codenameone.examples.bazaar.api.PosTenderDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;
import java.util.UUID;

/// Taking the money: any mix of cash (with quick amounts and change), one
/// card on the simulated terminal, one gift card and store credit. When it
/// is paid the sale goes into the outbox -- never straight to the server --
/// so the till keeps selling with the network down.
final class TenderSheet {
    private final Sheet sheet;
    private final Tenders tenders;
    private final PosPricing.Totals totals;
    private final Container paid = new Container(BoxLayout.y());
    private final Label left;
    /// Set once the sale is recorded, so a fast second tap cannot record it
    /// twice.
    private boolean finished;

    private TenderSheet() {
        totals = PosState.totals();
        tenders = new Tenders(totals.totalCents);
        sheet = new Sheet(null, Lang.tr("Charge {0}", PosState.money(totals.totalCents)));
        sheet.setName("pos-tender-sheet");
        left = PosUi.data("", "PosAmount");
        left.setName("pos-remaining");
    }

    static void show() {
        if (PosState.cart().isEmpty()) {
            return;
        }
        final TenderSheet t = new TenderSheet();
        t.build();
        PosKeys.set(PosKeys.CASH, () -> t.cash(t.tenders.remaining()));
        PosKeys.set(PosKeys.CARD, t::card);
        PosKeys.set(PosKeys.GIFT, t::gift);
        PosKeys.set(PosKeys.CREDIT, t::credit);
        t.sheet.addCloseListener(e -> {
            PosKeys.clear(PosKeys.CASH);
            PosKeys.clear(PosKeys.CARD);
            PosKeys.clear(PosKeys.GIFT);
            PosKeys.clear(PosKeys.CREDIT);
        });
        t.sheet.show();
    }

    private void build() {
        Container c = sheet.getContentPane();
        c.setLayout(BoxLayout.y());
        c.setScrollableY(true);
        c.add(left);
        c.add(paid);
        c.add(PosUi.label("Cash", "PosGroup"));
        long[] quick = Tenders.quickCash(totals.totalCents);
        Container cash = new Container(new GridLayout((quick.length + 3) / 4, 4));
        for (int iter = 0; iter < quick.length; iter++) {
            final long q = quick[iter];
            cash.add(PosUi.key(PosState.money(q), "pos-cash-" + q, "PosKey", e -> cash(q)));
        }
        c.add(cash);
        c.add(PosUi.key(Lang.tr("Other cash amount"), "pos-cash-other", "PosChoice",
                e -> SellScreen.askAmount("Cash received", false, this::cash)));
        Container other = new Container(new GridLayout(1, 3));
        other.add(PosUi.icon(FontImage.MATERIAL_CREDIT_CARD, "Card", "pos-card", "PosChoice",
                e -> card()));
        other.add(PosUi.icon(FontImage.MATERIAL_CARD_GIFTCARD, "Gift card", "pos-gift", "PosChoice",
                e -> gift()));
        other.add(PosUi.icon(FontImage.MATERIAL_ACCOUNT_BALANCE_WALLET, "Store credit",
                "pos-credit", "PosChoice", e -> credit()));
        c.add(other);
        refresh();
    }

    private void refresh() {
        paid.removeAll();
        List<PosTenderDto> list = tenders.list();
        for (int iter = 0; iter < list.size(); iter++) {
            final int index = iter;
            PosTenderDto t = list.get(iter);
            Container row = new Container(new BorderLayout());
            row.setUIID("PosTotalRow");
            row.add(BorderLayout.WEST, PosUi.label(kindName(t.kind()), "PosTotalRowLabel"));
            Container east = new Container(BoxLayout.x());
            east.add(PosUi.data(PosState.money(t.amountCents()), "PosTotalRowValue"));
            if (tenders.removable(index)) {
                east.add(Ui.flat(FontImage.MATERIAL_CLOSE, "pos-tender-remove-" + iter, e -> {
                    tenders.remove(index);
                    refresh();
                }));
            }
            row.add(BorderLayout.EAST, east);
            paid.add(row);
        }
        if (tenders.complete()) {
            long change = tenders.change();
            left.setText(change > 0 ? Lang.tr("Change {0}", PosState.money(change))
                    : Lang.tr("Paid"));
        } else {
            left.setText(Lang.tr("Due {0}", PosState.money(tenders.remaining())));
        }
        sheet.getContentPane().revalidateLater();
        if (tenders.complete()) {
            finish();
        }
    }

    static String kindName(String kind) {
        if (Tenders.CARD.equals(kind)) {
            return Lang.tr("Card");
        }
        if (Tenders.GIFT_CARD.equals(kind)) {
            return Lang.tr("Gift card");
        }
        if (Tenders.STORE_CREDIT.equals(kind)) {
            return Lang.tr("Store credit");
        }
        return Lang.tr("Cash");
    }

    private void tender(String kind, long cents, String ref) {
        if (finished) {
            return;
        }
        String problem = tenders.add(kind, cents, ref);
        if (problem != null) {
            Ui.say(problem);
            return;
        }
        refresh();
    }

    private void cash(long cents) {
        if (cents > 0) {
            tender(Tenders.CASH, cents, "");
        }
    }

    /// A simulated terminal: "present the card", a moment, approved.
    /// The tip presets of the store's policy, or 15, 18 and 20%.
    static long[] tipPresets() {
        long[] bp = PosState.tipPresets();
        return bp == null || bp.length == 0 ? new long[] {1500, 1800, 2000} : bp;
    }

    /// Asks for a tip before the card is charged: a preset share of the
    /// total, a typed amount, or none.
    private void card() {
        if (tenders.remaining() <= 0) {
            return;
        }
        final Sheet ask = new Sheet(sheet, Lang.tr("Add a tip?"));
        ask.setName("pos-tip");
        Container c = ask.getContentPane();
        c.setLayout(BoxLayout.y());
        long[] presets = tipPresets();
        Container row = new Container(new GridLayout(1, presets.length));
        for (int iter = 0; iter < presets.length; iter++) {
            final long cents = PosCart.tipFor(totals.totalCents, presets[iter]);
            row.add(PosUi.key(presets[iter] / 100 + "% " + PosState.money(cents),
                    "pos-tip-" + presets[iter], "PosKey", e -> {
                        ask.back();
                        charge(cents);
                    }));
        }
        c.add(row);
        c.add(PosUi.key(Lang.tr("Custom tip"), "pos-tip-custom", "PosChoice",
                e -> SellScreen.askAmount("Tip", false, cents -> {
                    ask.back();
                    charge(cents);
                })));
        c.add(PosUi.key(Lang.tr("No tip"), "pos-tip-none", "PosChoice", e -> {
            ask.back();
            charge(0);
        }));
        ask.show();
    }

    private void charge(long tipCents) {
        String refused = tenders.tip(tipCents);
        if (refused != null) {
            Ui.say(refused);
            return;
        }
        PosState.cart().setTipCents(tipCents);
        refresh();
        final long amount = tenders.remaining();
        if (amount <= 0) {
            return;
        }
        reader(amount);
    }

    /// The register's paired card reader, driven by the server: the amount
    /// is pushed to the reader, and the register asks until the card is
    /// taken or refused. With no reader at the register, or offline, the
    /// standalone terminal takes the card instead.
    private void reader(final long amount) {
        final String clientId = UUID.randomUUID().toString();
        final Sheet wait = new Sheet(sheet, Lang.tr("Card reader"));
        wait.setName("pos-reader");
        Container c = wait.getContentPane();
        c.setLayout(BoxLayout.y());
        c.add(PosUi.data(PosState.money(amount), "PosAmount"));
        final Label state = PosUi.label("Sending to the card reader...", "PosMuted");
        c.add(state);
        c.add(new InfiniteProgress());
        final String[] paymentId = {null};
        final boolean[] over = {false};
        c.add(PosUi.key(Lang.tr("Cancel"), "pos-reader-cancel", "PosChoice", e -> {
            over[0] = true;
            if (paymentId[0] != null) {
                Api.posCounter().cancelCardPayment(PosState.storeId(), paymentId[0],
                        Net.to(p -> { }, (st, m) -> { }));
            }
            wait.back();
            cardLost();
        }));
        // Shown before the payment starts, so Cancel is there while the
        // server reaches the reader; a Cancel then is caught below.
        wait.show();
        Api.posCounter().startCardPayment(PosState.storeId(), PosState.registerId(),
                new com.codenameone.examples.bazaar.api.CardPaymentDto(null, null, null, clientId,
                    amount, PosState.currency(), null, null, null), Net.to(p -> {
                        if (over[0]) {
                            // Cancelled before the reader answered: the
                            // cashier's Cancel had no id to cancel, so the
                            // payment it started is cancelled now.
                            Api.posCounter().cancelCardPayment(PosState.storeId(), p.id(),
                                    Net.to(q -> { }, (st, m) -> { }));
                            return;
                        }
                        paymentId[0] = p.id();
                        state.setText(Lang.tr("Tap, insert or swipe the card"));
                        poll(wait, state, p.id(), amount, over, 0, 0);
                    }, (status, message) -> {
                        if (over[0]) {
                            return;
                        }
                        wait.back();
                        if (ReaderPoll.standalone(status)) {
                            standalone(amount);
                        } else {
                            cardLost();
                            Ui.fail(status, message);
                        }
                    }));
    }

    private void poll(final Sheet wait, final Label state, final String id, final long amount,
            final boolean[] over, final int asks, final int misses) {
        CN.setTimeout(ReaderPoll.EVERY_MS, () -> {
            if (over[0]) {
                return;
            }
            Api.posCounter().cardPayment(PosState.storeId(), id, Net.to(p -> {
                if (!over[0]) {
                    decide(wait, state, id, amount, over, p.status(), p.message(), p.cardLabel(),
                            asks + 1, 0);
                }
            }, (status, message) -> {
                if (!over[0]) {
                    decide(wait, state, id, amount, over, null, message, null, asks + 1,
                            misses + 1);
                }
            }));
        });
    }

    private void decide(Sheet wait, Label state, String id, long amount, boolean[] over,
            String status, String message, String card, int asks, int misses) {
        int next = ReaderPoll.next(status, asks, misses);
        if (next == ReaderPoll.WAIT) {
            poll(wait, state, id, amount, over, asks, misses);
            return;
        }
        over[0] = true;
        wait.back();
        if (next == ReaderPoll.PAID) {
            // The payment's id is the tender's reference: the server spends it
            // on this sale, once.
            tender(Tenders.CARD, amount, id);
        } else {
            cardLost();
            Ui.say(message == null || message.length() == 0 ? "The card was not accepted"
                    : message);
        }
    }

    /// A simulated standalone terminal: "present the card", a moment, approved.
    private void standalone(final long amount) {
        final Sheet terminal = new Sheet(sheet, Lang.tr("Card terminal"));
        terminal.setName("pos-terminal");
        Container c = terminal.getContentPane();
        c.setLayout(BoxLayout.y());
        c.add(PosUi.data(PosState.money(amount), "PosAmount"));
        final Label state = PosUi.label("Tap, insert or swipe the card", "PosMuted");
        c.add(state);
        final InfiniteProgress spin = new InfiniteProgress();
        c.add(spin);
        final boolean[] cancelled = {false};
        c.add(PosUi.key(Lang.tr("Cancel"), "pos-terminal-cancel", "PosChoice", e -> {
            cancelled[0] = true;
            terminal.back();
            cardLost();
        }));
        terminal.show();
        CN.setTimeout(1800, () -> {
            if (cancelled[0]) {
                return;
            }
            state.setText(Lang.tr("Approved"));
            CN.setTimeout(500, () -> {
                if (cancelled[0]) {
                    return;
                }
                terminal.back();
                String ref = "SIM-" + Long.toString(System.currentTimeMillis() % 100000000L, 36);
                tender(Tenders.CARD, amount, ref);
            });
        });
    }

    private void gift() {
        final Sheet ask = new Sheet(sheet, Lang.tr("Gift card"));
        ask.setName("pos-gift-sheet");
        Container c = ask.getContentPane();
        c.setLayout(BoxLayout.y());
        final TextField code = new TextField("", Lang.tr("Gift card code"), 20, TextField.ANY);
        code.setUIID("PosField");
        code.setName("pos-gift-code");
        final Label status = PosUi.label("", "PosMuted");
        code.setDoneListener(e -> {
            final String text = Wedge.clean(code.getText());
            if (text.length() == 0) {
                return;
            }
            status.setText(Lang.tr("Checking..."));
            Api.giftCards().balance(text, Net.to(b -> {
                if (b == null || !b.usable() || b.balanceCents() <= 0) {
                    status.setText(Lang.tr("This gift card cannot be used"));
                    return;
                }
                ask.back();
                tender(Tenders.GIFT_CARD, Math.min(b.balanceCents(), tenders.remaining()), text);
            }, (s, m) -> status.setText(s <= 0 ? Lang.tr("A gift card needs the network")
                    : Lang.tr(m))));
        });
        c.add(code);
        c.add(status);
        ask.show();
        code.startEditingAsync();
    }

    private void credit() {
        PosCustomerDto cu = PosState.cart().customer();
        if (cu == null) {
            Ui.say("Add the customer to the sale first");
            return;
        }
        if (cu.storeCreditCents() <= 0) {
            Ui.say("This customer has no store credit");
            return;
        }
        tender(Tenders.STORE_CREDIT, Math.min(cu.storeCreditCents(), tenders.remaining()), "");
    }

    /// The card was cancelled, declined or failed: the tip it carried goes.
    private void cardLost() {
        tenders.dropTip();
        PosState.cart().setTipCents(tenders.tipCents());
        refresh();
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        PosCart cart = PosState.cart();
        cart.setTipCents(tenders.saleTipCents());
        PosCustomerDto cu = cart.customer();
        PosSaleDto sale = new PosSaleDto(UUID.randomUUID().toString(), PosState.shiftId(),
                cu == null ? "" : cu.id(), cu == null || cu.email() == null ? "" : cu.email(),
                cart.code(), cart.note(), System.currentTimeMillis(), cart.uploadLines(),
                tenders.list(), PosState.staffUsername(), cart.tipCents(), cart.approvalId());
        Receipt receipt = Receipt.of(sale, cart, totals, tenders.change());
        PosState.recordSale(sale);
        cart.clear();
        PosState.fireCart();
        sheet.back();
        ReceiptView.show(receipt);
    }
}
