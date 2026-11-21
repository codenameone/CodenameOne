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
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.Sheet;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderEditDto;
import com.codenameone.examples.bazaar.api.OrderEditLineDto;
import com.codenameone.examples.bazaar.api.OrderLineDto;
import com.codenameone.examples.bazaar.api.PosCatalogDto;
import com.codenameone.examples.bazaar.api.PosItemDto;
import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.api.PosTenderDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// Staff edit a placed order: change quantities, remove lines, add items.
/// The pane shows the difference as it goes (before tax and discounts; the
/// server answers the exact one), and on save the server collects the extra
/// (cash or card, as picked) or refunds the difference to the original
/// payment.
final class OrderEditPane {
    private OrderEditPane() {
    }

    static Component create() {
        final Container root = new Container(new BorderLayout());
        root.setName("pos-order-edit");
        root.setUIID("PosRoot");
        root.add(BorderLayout.NORTH, PosUi.header("Edit an order", null));
        Container page = new Container(BoxLayout.y());
        page.setScrollableY(true);
        page.setUIID("PosPage");
        final TextField q = new TextField("", Lang.tr("Order number"), 20, TextField.ANY);
        q.setUIID("PosScan");
        q.setName("pos-edit-q");
        final Container result = new Container(BoxLayout.y());
        q.setDoneListener(e -> find(q.getText(), result));
        page.add(q).add(result);
        root.add(BorderLayout.CENTER, page);
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
                            Net.to(o -> new Editor(o, result).fill(), Ui::fail));
                }, (s, m) -> Ui.fail(s, s <= 0 ? "Editing an order needs the network" : m)));
    }

    /// One order being edited.
    static final class Editor {
        final OrderDto order;
        final Container result;
        final List<OrderLineDto> lines;
        final int[] qty;
        final List<PosItemDto> added = new ArrayList<PosItemDto>();
        final List<Integer> addedQty = new ArrayList<Integer>();
        String collect = Tenders.CARD;

        Editor(OrderDto order, Container result) {
            this.order = order;
            this.result = result;
            lines = order.lines() == null ? new ArrayList<OrderLineDto>() : order.lines();
            qty = new int[lines.size()];
            for (int iter = 0; iter < qty.length; iter++) {
                qty[iter] = lines.get(iter).quantity();
            }
        }

        /// The difference before tax and discounts: what changed, at the
        /// prices the order was sold at, plus the added items at today's.
        long estimate() {
            long d = 0;
            for (int iter = 0; iter < qty.length; iter++) {
                d += (long) (qty[iter] - lines.get(iter).quantity()) * lines.get(iter).priceCents();
            }
            for (int iter = 0; iter < added.size(); iter++) {
                d += added.get(iter).priceCents() * addedQty.get(iter).intValue();
            }
            return d;
        }

        void fill() {
            result.removeAll();
            result.add(PosUi.data(Lang.tr("Order #{0}", order.number()) + Ui.DOT
                    + PosState.money(order.totalCents()), "PosTitle"));
            for (int iter = 0; iter < lines.size(); iter++) {
                final int index = iter;
                OrderLineDto l = lines.get(iter);
                result.add(row(l.title() + Ui.DOT + PosState.money(l.priceCents()), qty[iter],
                        "pos-edit-line-" + iter, d -> {
                            qty[index] = Math.max(0, qty[index] + d);
                            fill();
                        }));
            }
            for (int iter = 0; iter < added.size(); iter++) {
                final int index = iter;
                PosItemDto it = added.get(iter);
                result.add(row(Lang.tr("New: {0}", it.title()) + Ui.DOT
                        + PosState.money(it.priceCents()), addedQty.get(iter).intValue(),
                        "pos-edit-added-" + iter, d -> {
                            int q = addedQty.get(index).intValue() + d;
                            if (q <= 0) {
                                added.remove(index);
                                addedQty.remove(index);
                            } else {
                                addedQty.set(index, Integer.valueOf(q));
                            }
                            fill();
                        }));
            }
            result.add(PosUi.icon(FontImage.MATERIAL_ADD, "Add an item", "pos-edit-add",
                    "PosChoice", e -> addItem()));
            long d = estimate();
            Label delta = PosUi.data(d == 0 ? Lang.tr("No change yet") : d > 0
                    ? Lang.tr("Customer pays about {0} more", PosState.money(d))
                    : Lang.tr("Customer gets about {0} back", PosState.money(-d)), "PosAmount");
            delta.setName("pos-edit-delta");
            result.add(delta);
            if (d > 0) {
                Container kinds = new Container(new GridLayout(1, 2));
                kinds.add(PosUi.key(Lang.tr("Collect cash"), "pos-edit-cash",
                        Tenders.CASH.equals(collect) ? "PosChipOn" : "PosChip", e -> {
                            collect = Tenders.CASH;
                            fill();
                        }));
                kinds.add(PosUi.key(Lang.tr("Collect by card"), "pos-edit-card",
                        Tenders.CARD.equals(collect) ? "PosChipOn" : "PosChip", e -> {
                            collect = Tenders.CARD;
                            fill();
                        }));
                result.add(kinds);
            }
            Button save = PosUi.icon(FontImage.MATERIAL_SAVE, "Save the change", "pos-edit-save",
                    "PosPay", e -> save(""));
            save.setEnabled(changed());
            result.add(save);
            result.revalidateLater();
        }

        boolean changed() {
            if (!added.isEmpty()) {
                return true;
            }
            for (int iter = 0; iter < qty.length; iter++) {
                if (qty[iter] != lines.get(iter).quantity()) {
                    return true;
                }
            }
            return false;
        }

        OrderEditDto dto(String approvalId) {
            List<PosLineDto> add = new ArrayList<PosLineDto>();
            for (int iter = 0; iter < added.size(); iter++) {
                PosItemDto it = added.get(iter);
                add.add(new PosLineDto(it.variantId(), addedQty.get(iter).intValue(),
                        it.priceCents(), "", it.taxable(), false));
            }
            List<OrderEditLineDto> change = new ArrayList<OrderEditLineDto>();
            for (int iter = 0; iter < qty.length; iter++) {
                if (qty[iter] != lines.get(iter).quantity()) {
                    change.add(new OrderEditLineDto(lines.get(iter).id(), qty[iter]));
                }
            }
            // -1 leaves the order's discount as it was.
            return new OrderEditDto(add, change, -1, new PosTenderDto(collect, 0, ""),
                    approvalId, "Edited at the register");
        }

        void save(String approvalId) {
            if (approvalId.length() == 0
                    && PosRules.editNeedsApproval(PosState.policy(), PosState.role())) {
                ManagerApproval.ask(PosRules.ORDER_EDIT, Math.abs(estimate()),
                        "Order #" + order.number(), this::save);
                return;
            }
            Api.orderEdits().edit(PosState.storeId(), order.id(), dto(approvalId), Net.to(r -> {
                result.removeAll();
                result.add(PosUi.data(Lang.tr("Order #{0} is now {1}", order.number(),
                        PosState.money(r.totalCents())), "PosTitle"));
                long delta = r.deltaCents();
                if (delta > 0) {
                    result.add(PosUi.data(Lang.tr("Collected {0}", PosState.money(delta))
                            + Ui.DOT + TenderSheet.kindName(r.settlement()), "PosAmount"));
                } else if (delta < 0) {
                    result.add(PosUi.data(Lang.tr("Refunded {0}", PosState.money(-delta))
                            + Ui.DOT + TenderSheet.kindName(r.settlement()), "PosAmount"));
                    if (Tenders.CASH.equals(r.settlement())) {
                        result.add(PosUi.data(Lang.tr("Give {0} from the drawer",
                                PosState.money(-delta)), "PosTitle"));
                    }
                }
                result.revalidateLater();
                PosState.refreshCatalog(null);
            }, (s, m) -> {
                if (approvalId.length() == 0 && ManagerApproval.wanted(s, m)) {
                    ManagerApproval.ask(PosRules.ORDER_EDIT, Math.abs(estimate()),
                            "Order #" + order.number(), this::save);
                } else {
                    Ui.fail(s, s <= 0 ? "Editing an order needs the network" : m);
                }
            }));
        }

        private void addItem() {
            final Sheet sheet = new Sheet(null, Lang.tr("Add an item"));
            sheet.setName("pos-edit-add-sheet");
            Container c = sheet.getContentPane();
            c.setLayout(new BorderLayout());
            final TextField q = new TextField("", Lang.tr("Search"), 20, TextField.ANY);
            q.setUIID("PosField");
            final Container hits = new Container(BoxLayout.y());
            hits.setScrollableY(true);
            q.setDoneListener(e -> {
                hits.removeAll();
                PosCatalogDto cat = PosState.catalog();
                List<PosItemDto> found = cat == null || cat.items() == null
                        ? new ArrayList<PosItemDto>() : Snapshot.filter(cat.items(), q.getText(),
                        null);
                for (int iter = 0; iter < found.size() && iter < 30; iter++) {
                    final PosItemDto it = found.get(iter);
                    Button b = new Button(it.title() + Ui.DOT + PosState.money(it.priceCents()),
                            "PosChoice");
                    b.setShouldLocalize(false);
                    b.setName("pos-edit-hit-" + iter);
                    b.addActionListener(ev -> {
                        sheet.back();
                        added.add(it);
                        addedQty.add(Integer.valueOf(1));
                        fill();
                    });
                    hits.add(b);
                }
                hits.revalidateLater();
            });
            c.add(BorderLayout.NORTH, q);
            c.add(BorderLayout.CENTER, hits);
            sheet.show();
            q.startEditingAsync();
        }
    }

    interface Step {
        void by(int delta);
    }

    private static Component row(String text, int quantity, String name, final Step step) {
        Container row = new Container(new BorderLayout());
        row.setUIID("PosCartRow");
        row.setName(name);
        row.add(BorderLayout.CENTER, PosUi.data(text, "PosCartTitle"));
        Container s = new Container(BoxLayout.x());
        s.setUIID("PosStepper");
        s.add(PosUi.icon(FontImage.MATERIAL_REMOVE, "", name + "-minus", "PosStep", e -> step.by(-1)));
        s.add(PosUi.data(String.valueOf(quantity), "PosQty"));
        s.add(PosUi.icon(FontImage.MATERIAL_ADD, "", name + "-plus", "PosStep", e -> step.by(1)));
        row.add(BorderLayout.EAST, s);
        return row;
    }
}
