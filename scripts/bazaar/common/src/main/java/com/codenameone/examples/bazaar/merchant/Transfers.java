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
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.ReceiveDto;
import com.codenameone.examples.bazaar.api.ReceiveLineDto;
import com.codenameone.examples.bazaar.api.TransferDto;
import com.codenameone.examples.bazaar.api.TransferLineDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// Stock moving between two locations: drafted, sent (it leaves the
/// source), then received at the destination -- all of it or part.
final class Transfers extends Container {
    private final InventoryPane pane;
    private final List<LocationDto> locations;
    private final Container list = new Container(BoxLayout.y());

    Transfers(InventoryPane pane, List<LocationDto> locations) {
        super(new BorderLayout());
        this.pane = pane;
        this.locations = locations;
        setName("transfers");
        Container page = MUi.page("transfers-page");
        page.add(MUi.buttons(Ui.primary(Lang.tr("New transfer"), "transfer-new", e -> create())));
        list.setUIID("MeCard");
        page.add(list);
        add(BorderLayout.CENTER, page);
        reload();
    }

    private void reload() {
        Api.inventory().transfers(MerchantState.storeId(), Net.to(found -> {
            list.removeAll();
            for (final TransferDto t : found) {
                int units = 0;
                for (TransferLineDto l : t.lines()) {
                    units += l.quantity();
                }
                list.add(Ui.tap(Ui.row(FontImage.MATERIAL_SWAP_HORIZ,
                        InventoryPane.locationName(t.fromLocationId(), locations) + " -> "
                        + InventoryPane.locationName(t.toLocationId(), locations),
                        MUi.when(t.createdAt()) + Ui.DOT + Lang.tr("{0} units", String.valueOf(units)),
                        MUi.badge(t.status())), "transfer-" + t.id(), e -> detail(t)));
            }
            if (found.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_SWAP_HORIZ, "No transfers",
                        "Move stock between your locations."));
            }
            Ui.relayout(this);
        }, Ui::fail));
    }

    private void create() {
        if (locations.size() < 2) {
            Ui.say("A transfer needs two locations");
            return;
        }
        Container page = MUi.page("transfer-new-page");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        final Picker from = InventoryPane.locationPicker(card, "From", "transfer-from", locations,
                locations.get(0).id());
        final Picker to = InventoryPane.locationPicker(card, "To", "transfer-to", locations,
                locations.get(1).id());
        final TextField note = MUi.text(card, "Note", "transfer-note", "");
        final Lines lines = new Lines(card, false);
        page.add(card);
        page.add(MUi.buttons(Ui.primary(Lang.tr("Create transfer"), "transfer-create", e -> {
            String a = InventoryPane.location(from, locations);
            String b = InventoryPane.location(to, locations);
            if (a == null || a.equals(b)) {
                Ui.say("Pick two different locations");
                return;
            }
            if (lines.lines.isEmpty()) {
                Ui.say("Add at least one item");
                return;
            }
            List<TransferLineDto> out = new ArrayList<TransferLineDto>();
            for (Lines.Line l : lines.lines) {
                out.add(new TransferLineDto(null, l.hit.variant().id(), l.hit.variant().sku(),
                        l.hit.productTitle(), l.quantity, 0));
            }
            Api.inventory().createTransfer(MerchantState.storeId(), new TransferDto(null, a, b,
                    null, note.getText(), out, 0), Net.to(made -> {
                        pane.back();
                        reload();
                    }, Ui::fail));
        })));
        pane.open(Lang.tr("New transfer"), page);
    }

    private void detail(final TransferDto t) {
        Container page = MUi.page("transfer-detail");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        card.add(Ui.fact(Lang.tr("Status"), Lang.tr(Words.of(t.status()))));
        card.add(Ui.fact(Lang.tr("From"), InventoryPane.locationName(t.fromLocationId(), locations)));
        card.add(Ui.fact(Lang.tr("To"), InventoryPane.locationName(t.toLocationId(), locations)));
        for (TransferLineDto l : t.lines()) {
            card.add(Ui.row((char) 0, l.title(), l.sku(), Ui.plain(l.received() + " / " + l.quantity(),
                    "SvRowValue")));
        }
        page.add(card);
        Container buttons = MUi.buttons();
        if ("DRAFT".equals(t.status())) {
            buttons.add(Ui.primary(Lang.tr("Send"), "transfer-send", e -> Api.inventory()
                    .sendTransfer(MerchantState.storeId(), t.id(), Net.to(x -> done(), Ui::fail))));
        }
        if ("IN_TRANSIT".equals(t.status()) || "PARTIAL".equals(t.status())) {
            buttons.add(Ui.primary(Lang.tr("Receive"), "transfer-receive", e -> receive(t)));
        }
        if (!"RECEIVED".equals(t.status()) && !"CANCELLED".equals(t.status())) {
            buttons.add(Ui.danger(Lang.tr("Cancel transfer"), "transfer-cancel", e -> Api.inventory()
                    .cancelTransfer(MerchantState.storeId(), t.id(), Net.to(x -> done(), Ui::fail))));
        }
        page.add(buttons);
        pane.open(Lang.tr("Transfer"), page);
    }

    private void done() {
        pane.back();
        reload();
    }

    private void receive(final TransferDto t) {
        List<int[]> left = new ArrayList<int[]>();
        List<String> ids = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        for (TransferLineDto l : t.lines()) {
            left.add(new int[] {l.quantity() - l.received()});
            ids.add(l.id());
            names.add(l.title());
        }
        List<ReceiveLineDto> got = receiveDialog(ids, names, left);
        if (got != null) {
            Api.inventory().receiveTransfer(MerchantState.storeId(), t.id(), new ReceiveDto(got),
                    Net.to(x -> done(), Ui::fail));
        }
    }

    /// Asks how many of each line arrived, defaulting to all that is left.
    /// Null when cancelled or nothing was entered.
    static List<ReceiveLineDto> receiveDialog(List<String> ids, List<String> names, List<int[]> left) {
        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        List<TextField> fields = new ArrayList<TextField>();
        for (int iter = 0; iter < ids.size(); iter++) {
            TextField f = Ui.field(body, Lang.tr("{0} ({1} left)", names.get(iter),
                    String.valueOf(left.get(iter)[0])), "receive-" + iter,
                    String.valueOf(Math.max(0, left.get(iter)[0])), TextField.NUMERIC);
            fields.add(f);
        }
        Command ok = Command.create(Lang.tr("Receive"), null, null);
        Command cancel = Command.create(Lang.tr("Cancel"), null, null);
        if (Dialog.show(Lang.tr("Receive items"), body, cancel, ok) != ok) {
            return null;
        }
        List<ReceiveLineDto> out = new ArrayList<ReceiveLineDto>();
        for (int iter = 0; iter < ids.size(); iter++) {
            int q = Math.min(Math.max(0, MUi.integer(fields.get(iter))), Math.max(0, left.get(iter)[0]));
            if (q > 0) {
                out.add(new ReceiveLineDto(ids.get(iter), q));
            }
        }
        return out.isEmpty() ? null : out;
    }
}
