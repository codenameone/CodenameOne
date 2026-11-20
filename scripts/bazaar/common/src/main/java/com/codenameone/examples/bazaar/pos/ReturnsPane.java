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
import com.codename1.codescan.CodeScanner;
import com.codename1.codescan.ScanResult;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderLineDto;
import com.codenameone.examples.bazaar.api.PosReturnDto;
import com.codenameone.examples.bazaar.api.RefundLineDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// Returns at the counter: find the order (scan its receipt QR code or type
/// the number), pick what comes back, and refund to cash or the original
/// card. Needs the network: the refund is the server's.
final class ReturnsPane {
    private static int pendingNumber;

    private ReturnsPane() {
    }

    /// Looks `number` up the next time the pane opens.
    static void lookUpLater(int number) {
        pendingNumber = number;
    }

    static Component create() {
        final Container root = new Container(new BorderLayout());
        root.setName("pos-returns");
        root.setUIID("PosRoot");
        root.add(BorderLayout.NORTH, PosUi.header("Returns", null));
        final Container page = new Container(BoxLayout.y());
        page.setScrollableY(true);
        page.setUIID("PosPage");
        final TextField q = new TextField("", Lang.tr("Order number or scan the receipt"), 20,
                TextField.ANY);
        q.setUIID("PosScan");
        q.setName("pos-return-q");
        final Container result = new Container(BoxLayout.y());
        q.setDoneListener(e -> find(q.getText(), result));
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.CENTER, q);
        if (SellScreen.cameraScan()) {
            top.add(BorderLayout.EAST, PosUi.icon(FontImage.MATERIAL_QR_CODE_SCANNER, "",
                    "pos-return-camera", "PosTool", ev -> CodeScanner.getInstance().scanBarCode(
                            new ScanResult() {
                                @Override
                                public void scanCompleted(String contents, String f, byte[] raw) {
                                    CN.callSerially(() -> find(contents, result));
                                }

                                @Override
                                public void scanCanceled() {
                                }

                                @Override
                                public void scanError(int code, String message) {
                                }
                            })));
        }
        page.add(top).add(result);
        root.add(BorderLayout.CENTER, page);
        if (pendingNumber > 0) {
            int n = pendingNumber;
            pendingNumber = 0;
            find("#" + n, result);
        } else if (PosUi.holdFocus()) {
            CN.callSerially(q::requestFocus);
        }
        return root;
    }

    private static void find(String text, final Container result) {
        int n = Wedge.orderNumber(text);
        if (n <= 0) {
            n = (int) Math.min(Integer.MAX_VALUE, Wedge.digits(text));
        }
        if (n <= 0) {
            return;
        }
        final int number = n;
        result.removeAll();
        result.add(PosUi.label("Looking...", "PosMuted"));
        result.revalidateLater();
        Api.orders().list(PosState.storeId(), null, null, null, null, null, null,
                String.valueOf(number), Net.to(list -> {
                    OrderDto hit = null;
                    for (int iter = 0; list != null && iter < list.size(); iter++) {
                        if (list.get(iter).number() == number) {
                            hit = list.get(iter);
                        }
                    }
                    if (hit == null) {
                        result.removeAll();
                        result.add(PosUi.data(Lang.tr("No order #{0}", number), "PosError"));
                        result.revalidateLater();
                        return;
                    }
                    Api.orders().detail(PosState.storeId(), hit.id(),
                            Net.to(o -> show(o, result), Ui::fail));
                }, (s, m) -> Ui.fail(s, s <= 0 ? "Returns need the network" : m)));
    }

    private static void show(final OrderDto o, final Container result) {
        result.removeAll();
        result.add(PosUi.data(Lang.tr("Order #{0}", o.number()) + Ui.DOT
                + PosState.money(o.totalCents()), "PosTitle"));
        final List<OrderLineDto> lines = o.lines() == null ? new ArrayList<OrderLineDto>()
                : o.lines();
        final int[] back = new int[lines.size()];
        for (int iter = 0; iter < lines.size(); iter++) {
            final int index = iter;
            final OrderLineDto l = lines.get(iter);
            final int most = l.quantity() - l.refundedQty();
            Container row = new Container(new BorderLayout());
            row.setUIID("PosCartRow");
            row.add(BorderLayout.CENTER, PosUi.data(l.title() + Ui.DOT + PosState.money(
                    l.priceCents()) + Ui.DOT + Lang.tr("{0} returnable", most), "PosCartTitle"));
            Container step = new Container(BoxLayout.x());
            final com.codename1.ui.Label qty = PosUi.data("0", "PosQty");
            step.add(PosUi.icon(FontImage.MATERIAL_REMOVE, "", "pos-return-minus-" + iter, "PosStep",
                    e -> {
                        back[index] = Math.max(0, back[index] - 1);
                        qty.setText(String.valueOf(back[index]));
                    }));
            step.add(qty);
            step.add(PosUi.icon(FontImage.MATERIAL_ADD, "", "pos-return-plus-" + iter, "PosStep",
                    e -> {
                        back[index] = Math.min(most, back[index] + 1);
                        qty.setText(String.valueOf(back[index]));
                    }));
            row.add(BorderLayout.EAST, step);
            result.add(row);
        }
        final boolean[] restock = {true};
        result.add(com.codenameone.examples.bazaar.ui.Ui.toggle("pos-return-restock", true,
                v -> restock[0] = v));
        Container to = new Container(new GridLayout(1, 2));
        to.add(PosUi.icon(FontImage.MATERIAL_PAYMENTS, "Refund cash", "pos-refund-cash", "PosChoice",
                e -> refund(o, lines, back, "CASH", restock[0], result)));
        to.add(PosUi.icon(FontImage.MATERIAL_CREDIT_CARD, "Refund to card", "pos-refund-card",
                "PosChoice", e -> refund(o, lines, back, "ORIGINAL", restock[0], result)));
        result.add(to);
        result.revalidateLater();
    }

    private static void refund(OrderDto o, List<OrderLineDto> lines, int[] back, String to,
            boolean restock, final Container result) {
        List<RefundLineDto> picked = new ArrayList<RefundLineDto>();
        for (int iter = 0; iter < lines.size(); iter++) {
            if (back[iter] > 0) {
                picked.add(new RefundLineDto(lines.get(iter).id(), back[iter]));
            }
        }
        if (picked.isEmpty()) {
            Ui.say("Pick what is coming back");
            return;
        }
        Api.pos().counterReturn(PosState.storeId(), PosState.registerId(), o.id(),
                new PosReturnDto(PosState.shiftId(), picked, to, "Counter return", restock),
                Net.to(r -> {
                    result.removeAll();
                    result.add(PosUi.data(Lang.tr("Refunded {0}", PosState.money(r.refundCents())),
                            "PosAmount"));
                    if (r.cashCents() > 0) {
                        result.add(PosUi.data(Lang.tr("Give {0} from the drawer",
                                PosState.money(r.cashCents())), "PosTitle"));
                    }
                    result.revalidateLater();
                    PosState.refreshCatalog(null);
                }, Ui::fail));
    }
}
