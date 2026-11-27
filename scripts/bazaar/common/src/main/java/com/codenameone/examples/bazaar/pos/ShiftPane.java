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
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.CashMoveDto;
import com.codenameone.examples.bazaar.api.CloseShiftDto;
import com.codenameone.examples.bazaar.api.ShiftDto;
import com.codenameone.examples.bazaar.docs.Pdfs;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// The cashier's shift: what the drawer should hold, cash in and out, and
/// closing with a count by denomination, the variance and the Z report.
final class ShiftPane {
    private ShiftPane() {
    }

    static Component create() {
        final Container root = new Container(new BorderLayout());
        root.setName("pos-shift");
        root.setUIID("PosRoot");
        root.add(BorderLayout.NORTH, PosUi.header("Shift", null));
        final Container page = new Container(BoxLayout.y());
        page.setScrollableY(true);
        page.setUIID("PosPage");
        root.add(BorderLayout.CENTER, page);
        fill(page);
        ShiftDto s = PosState.shift();
        if (s != null) {
            Api.pos().shift(PosState.storeId(), s.id(), Net.to(fresh -> {
                PosState.setShift(fresh);
                fill(page);
            }, (status, message) -> { }));
        }
        return root;
    }

    private static void fill(final Container page) {
        page.removeAll();
        ShiftDto s = PosState.shift();
        if (s == null) {
            page.add(Ui.empty(FontImage.MATERIAL_SCHEDULE, "No shift open",
                    "Open a shift on the Register to start selling."));
            page.revalidateLater();
            return;
        }
        page.add(Ui.fact("Cashier", s.username()));
        page.add(Ui.fact("Float", PosState.money(s.openingCents())));
        page.add(Ui.fact("Cash sales", PosState.money(s.cashSalesCents())));
        page.add(Ui.fact("Card sales", PosState.money(s.cardSalesCents())));
        page.add(Ui.fact("Other", PosState.money(s.otherSalesCents())));
        page.add(Ui.fact("Cash in and out", PosState.money(s.cashMovesCents())));
        page.add(Ui.fact("Expected in drawer", PosState.money(CashCount.expected(s.openingCents(),
                s.cashSalesCents(), s.cashMovesCents()))));
        int waiting = PosState.outbox().pending();
        if (waiting > 0) {
            page.add(PosUi.data(Lang.tr("{0} sales are not uploaded yet; they count once they are",
                    waiting), "PosError"));
        }
        Container moves = new Container(new GridLayout(1, 2));
        moves.add(PosUi.icon(FontImage.MATERIAL_ADD, "Cash in", "pos-cash-in", "PosChoice",
                e -> move(page, 1)));
        moves.add(PosUi.icon(FontImage.MATERIAL_REMOVE, "Cash out", "pos-cash-out", "PosChoice",
                e -> move(page, -1)));
        page.add(moves);
        page.add(PosUi.label("Close the shift", "PosGroup"));
        page.add(counter(page, s));
        page.revalidateLater();
    }

    private static void move(final Container page, final int sign) {
        SellScreen.askAmount(sign > 0 ? "Cash in" : "Cash out", false, cents -> {
            if (cents <= 0) {
                return;
            }
            Api.pos().moveCash(PosState.storeId(), PosState.shiftId(),
                    new CashMoveDto("", sign * cents, sign > 0 ? "Cash in" : "Cash out", "", 0),
                    Net.to(shift -> {
                        PosState.setShift(shift);
                        fill(page);
                    }, (status, m) -> Ui.fail(status, status <= 0
                            ? "Cash in and out needs the network" : m)));
        });
    }

    /// A count per denomination, the running total and the variance.
    private static Container counter(final Container page, final ShiftDto s) {
        Container box = new Container(BoxLayout.y());
        final int[] counts = new int[CashCount.DENOMINATIONS.length];
        final long expected = CashCount.expected(s.openingCents(), s.cashSalesCents(),
                s.cashMovesCents());
        final Label total = PosUi.data("", "PosCountTotal");
        final Label variance = PosUi.data("", "PosMuted");
        final Runnable sum = () -> {
            long counted = CashCount.total(counts);
            long v = CashCount.variance(counted, expected);
            total.setText(Lang.tr("Counted {0}", PosState.money(counted)));
            variance.setText(v == 0 ? Lang.tr("Matches the expected cash")
                    : v > 0 ? Lang.tr("Over by {0}", PosState.money(v))
                    : Lang.tr("Short by {0}", PosState.money(-v)));
            variance.setUIID(v == 0 ? "PosMuted" : "PosError");
            total.getParent().revalidateLater();
        };
        // Two columns only where there is room: on a phone half a row could
        // not hold "$100.00" and its field, and the label ran under the field.
        int cols = PosUi.wide() ? 2 : 1;
        Container grid = new Container(new GridLayout((CashCount.DENOMINATIONS.length + cols - 1) / cols,
                cols));
        for (int iter = 0; iter < CashCount.DENOMINATIONS.length; iter++) {
            final int index = iter;
            Container cell = new Container(new BorderLayout());
            cell.add(BorderLayout.CENTER, PosUi.data(PosState.money(CashCount.DENOMINATIONS[iter]),
                    "PosCountLabel"));
            final TextField f = new TextField("", "0", 4, TextField.NUMERIC);
            f.setUIID("PosField");
            f.setName("pos-count-" + CashCount.DENOMINATIONS[iter]);
            f.addDataChangedListener((t, i) -> {
                counts[index] = (int) Math.min(100000, Wedge.digits(f.getText()));
                sum.run();
            });
            cell.add(BorderLayout.EAST, f);
            grid.add(cell);
        }
        box.add(grid).add(total).add(variance);
        sum.run();
        box.add(PosUi.icon(FontImage.MATERIAL_LOCK, "Close shift", "pos-close-shift", "PosPay", e -> {
            if (PosState.outbox().pending() > 0) {
                Ui.say("Upload the waiting sales before closing the shift");
                PosSync.kick();
                return;
            }
            final String shiftId = s.id();
            Api.pos().closeShift(PosState.storeId(), shiftId,
                    new CloseShiftDto(CashCount.total(counts), ""), Net.to(closed -> {
                        PosState.setShift(null);
                        closedPage(page, closed);
                        PosHome.refresh();
                    }, (status, m) -> Ui.fail(status, status <= 0
                            ? "Closing a shift needs the network" : m)));
        }));
        return box;
    }

    private static void closedPage(Container page, final ShiftDto closed) {
        page.removeAll();
        // What the shift sold sets the next one's goal (ShiftGoal.targetFrom).
        PosState.recordClosedShift(closed.id(), closed.cashSalesCents() + closed.cardSalesCents()
                + closed.otherSalesCents());
        page.add(PosUi.label("Shift closed", "PosTitle"));
        page.add(Ui.fact("Expected", PosState.money(closed.expectedCents())));
        page.add(Ui.fact("Counted", PosState.money(closed.countedCents())));
        page.add(Ui.fact("Variance", PosState.money(closed.varianceCents()),
                closed.varianceCents() == 0 ? null : "PosError"));
        page.add(PosUi.icon(FontImage.MATERIAL_PICTURE_AS_PDF, "Z report", "pos-z-report",
                "PosChoice", e -> Pdfs.open("Z report", "z-report-" + closed.id() + ".pdf",
                        "/api/merchant/stores/" + PosState.storeId() + "/shifts/" + closed.id()
                        + "/report.pdf")));
        page.revalidateLater();
    }
}
