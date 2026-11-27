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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.PurchaseLineDto;
import com.codenameone.examples.bazaar.api.PurchaseOrderDto;
import com.codenameone.examples.bazaar.api.ReceiveDto;
import com.codenameone.examples.bazaar.api.ReceiveLineDto;
import com.codenameone.examples.bazaar.api.SupplierDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// Buying stock from a supplier: draft, place, then receive into a location
/// as it arrives -- a partial delivery leaves the rest incoming.
final class PurchaseOrders extends Container {
    private final InventoryPane pane;
    private final List<LocationDto> locations;
    private final List<SupplierDto> suppliers = new ArrayList<SupplierDto>();
    private final Container list = new Container(BoxLayout.y());

    PurchaseOrders(InventoryPane pane, List<LocationDto> locations) {
        super(new BorderLayout());
        this.pane = pane;
        this.locations = locations;
        setName("purchase-orders");
        Container page = MUi.page("purchase-orders-page");
        page.add(MUi.buttons(Ui.primary(Lang.tr("New purchase order"), "po-new", e -> create())));
        list.setUIID("MeCard");
        page.add(list);
        add(BorderLayout.CENTER, page);
        Api.inventory().suppliers(MerchantState.storeId(), Net.to(found -> {
            suppliers.clear();
            suppliers.addAll(found);
        }, (s, m) -> { }));
        reload();
    }

    private void reload() {
        Api.inventory().purchaseOrders(MerchantState.storeId(), Net.to(found -> {
            list.removeAll();
            for (final PurchaseOrderDto po : found) {
                list.add(Ui.tap(Ui.row(FontImage.MATERIAL_RECEIPT_LONG,
                        "PO-" + po.number() + Ui.DOT + po.supplierName(),
                        MerchantState.money(po.totalCents()) + Ui.DOT
                        + InventoryPane.locationName(po.locationId(), locations),
                        MUi.badge(po.status())), "po-" + po.number(), e -> detail(po)));
            }
            if (found.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_RECEIPT_LONG, "No purchase orders",
                        "Order stock from a supplier and receive it when it arrives."));
            }
            Ui.relayout(this);
        }, Ui::fail));
    }

    private void create() {
        if (suppliers.isEmpty() || locations.isEmpty()) {
            Ui.say("Add a supplier and a location first");
            return;
        }
        Container page = MUi.page("po-new-page");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        String[] names = new String[suppliers.size()];
        for (int iter = 0; iter < names.length; iter++) {
            names[iter] = suppliers.get(iter).name();
        }
        final Picker supplier = new Picker();
        supplier.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        supplier.setUIID("SvField");
        supplier.setName("po-supplier");
        supplier.setStrings(names);
        supplier.setSelectedStringIndex(0);
        card.add(Ui.label("Supplier", "SvLabel")).add(supplier);
        final Picker where = InventoryPane.locationPicker(card, "Deliver to", "po-location",
                locations, locations.get(0).id());
        final TextField note = MUi.text(card, "Note", "po-note", "");
        final Lines lines = new Lines(card, true);
        page.add(card);
        page.add(MUi.buttons(Ui.primary(Lang.tr("Create purchase order"), "po-create", e -> {
            if (lines.lines.isEmpty()) {
                Ui.say("Add at least one item");
                return;
            }
            SupplierDto s = suppliers.get(Math.max(0, supplier.getSelectedStringIndex()));
            List<PurchaseLineDto> out = new ArrayList<PurchaseLineDto>();
            long total = 0;
            for (Lines.Line l : lines.lines) {
                out.add(new PurchaseLineDto(null, l.hit.variant().id(), l.hit.variant().sku(),
                        l.hit.productTitle(), l.quantity, 0, l.costCents));
                total += l.costCents * l.quantity;
            }
            Api.inventory().createPurchaseOrder(MerchantState.storeId(), new PurchaseOrderDto(null, 0,
                    s.id(), s.name(), InventoryPane.location(where, locations), null, 0,
                    note.getText(), total, out, 0), Net.to(made -> done(), Ui::fail));
        })));
        pane.open(Lang.tr("New purchase order"), page);
    }

    private void done() {
        pane.back();
        reload();
    }

    private void detail(final PurchaseOrderDto po) {
        Container page = MUi.page("po-detail");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        card.add(Ui.fact(Lang.tr("Status"), Lang.tr(Words.of(po.status()))));
        card.add(Ui.fact(Lang.tr("Supplier"), po.supplierName()));
        card.add(Ui.fact(Lang.tr("Deliver to"), InventoryPane.locationName(po.locationId(), locations)));
        card.add(Ui.fact(Lang.tr("Total"), MerchantState.money(po.totalCents())));
        for (PurchaseLineDto l : po.lines()) {
            card.add(Ui.row((char) 0, l.title(), l.sku() + Ui.DOT + MerchantState.money(l.costCents()),
                    Ui.plain(l.received() + " / " + l.quantity(), "SvRowValue")));
        }
        page.add(card);
        page.add(TimelineView.load(false, po.id()));
        Container buttons = MUi.buttons();
        if ("DRAFT".equals(po.status())) {
            buttons.add(Ui.primary(Lang.tr("Place order"), "po-place", e -> Api.inventory()
                    .placePurchaseOrder(MerchantState.storeId(), po.id(), Net.to(x -> done(), Ui::fail))));
        }
        if ("ORDERED".equals(po.status()) || "PARTIAL".equals(po.status())) {
            buttons.add(Ui.primary(Lang.tr("Receive"), "po-receive", e -> receive(po)));
        }
        if ("DRAFT".equals(po.status()) || "ORDERED".equals(po.status())) {
            buttons.add(Ui.danger(Lang.tr("Cancel order"), "po-cancel", e -> Api.inventory()
                    .cancelPurchaseOrder(MerchantState.storeId(), po.id(), Net.to(x -> done(), Ui::fail))));
        }
        buttons.add(Ui.secondary(Lang.tr("PDF"), "po-pdf", e -> com.codenameone.examples.bazaar.docs
                .Pdfs.open("Purchase order", "po-" + po.number() + ".pdf", "/api/merchant/stores/"
                + MerchantState.storeId() + "/purchase-orders/" + po.id() + "/po.pdf")));
        page.add(buttons);
        pane.open("PO-" + po.number(), page);
    }

    private void receive(PurchaseOrderDto po) {
        List<int[]> left = new ArrayList<int[]>();
        List<String> ids = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        for (PurchaseLineDto l : po.lines()) {
            left.add(new int[] {l.quantity() - l.received()});
            ids.add(l.id());
            names.add(l.title());
        }
        List<ReceiveLineDto> got = Transfers.receiveDialog(ids, names, left);
        if (got != null) {
            Api.inventory().receivePurchaseOrder(MerchantState.storeId(), po.id(), new ReceiveDto(got),
                    Net.to(x -> done(), Ui::fail));
        }
    }
}
