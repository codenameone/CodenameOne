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
import com.codename1.appkit.Skeleton;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.FulfillmentDto;
import com.codenameone.examples.bazaar.api.LabelRequestDto;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OpenLineDto;
import com.codenameone.examples.bazaar.api.OrderCardDto;
import com.codenameone.examples.bazaar.api.PackDto;
import com.codenameone.examples.bazaar.api.RateQuoteDto;
import com.codenameone.examples.bazaar.api.SerialAssignDto;
import com.codenameone.examples.bazaar.api.TrackingEventDto;
import com.codenameone.examples.bazaar.api.UnfulfilledDto;
import com.codenameone.examples.bazaar.docs.Pdfs;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// One order's fulfilment, by hand: which lines and how many go in a parcel
/// and from which location, then pack it (its weight), get the carriers'
/// quotes, buy a label and ship it -- or, for a pickup, say it is ready and
/// that it was collected. Every step is a call to the server, which answers
/// with the fulfilment as it now is.
final class FulfilmentFlow extends Container {
    static final String[] METHODS = {"SHIP", "PICKUP", "LOCAL"};

    private final OrderCardDto card;
    private UnfulfilledDto open;
    private final List<LocationDto> locations;
    private final Runnable changed;
    private List<FulfillmentDto> fulfillments = new ArrayList<FulfillmentDto>();

    FulfilmentFlow(OrderCardDto card, UnfulfilledDto open, List<LocationDto> locations,
            Runnable changed) {
        super(new BorderLayout());
        setName("fulfilment-flow");
        this.card = card;
        this.open = open;
        this.locations = locations;
        this.changed = changed;
        add(BorderLayout.CENTER, new Skeleton(4, "SvSkeleton"));
        reload();
    }

    private String store() {
        return MerchantState.storeId();
    }

    private void reload() {
        Api.fulfilment().ofOrder(store(), card.orderId(), Net.to(list -> {
            fulfillments = list;
            Api.fulfilment().queue(store(), null, Net.to(q -> {
                open = null;
                for (int iter = 0; iter < q.size(); iter++) {
                    if (q.get(iter).orderId().equals(card.orderId())) {
                        open = q.get(iter);
                    }
                }
                paint();
            }, Ui::fail));
        }, Ui::fail));
    }

    /// After a step: this screen and the board behind it.
    private void stepped(FulfillmentDto f) {
        reload();
        if (changed != null) {
            changed.run();
        }
    }

    private void paint() {
        removeAll();
        Container page = MUi.page("fulfilment-page");
        Container head = new Container(BoxLayout.y());
        head.setUIID("MeCard");
        head.add(Ui.plain(Lang.tr("Order #{0}", String.valueOf(card.number())) + Ui.DOT
                + (card.customer() == null ? "" : card.customer()), "SvHeading"));
        head.add(Ui.plain(Lang.tr(Fulfil.items(card.items())) + Ui.DOT
                + MerchantState.money(card.totalCents()) + Ui.DOT
                + Fulfil.age(card.createdAt(), System.currentTimeMillis()), "SvRowDetail"));
        Container docs = new Container(new FlowLayout());
        docs.add(doc("Invoice", "fu-invoice", () -> FulfilmentPane.invoice(card)));
        docs.add(doc("Packing slip", "fu-slip", () -> FulfilmentPane.packingSlip(card)));
        head.add(docs);
        page.add(head);
        if (open != null && hasOpen(open)) {
            Ui.section(page, "To fulfil");
            page.add(newFulfilment(open));
        }
        if (!fulfillments.isEmpty()) {
            Ui.section(page, "Parcels");
        }
        for (int iter = 0; iter < fulfillments.size(); iter++) {
            page.add(parcel(fulfillments.get(iter)));
        }
        if ((open == null || !hasOpen(open)) && fulfillments.isEmpty()) {
            page.add(Ui.empty(FontImage.MATERIAL_INVENTORY, "Nothing to fulfil", null));
        }
        add(BorderLayout.CENTER, page);
        Ui.relayout(this);
    }

    /// Records the serial numbers shipped for `line`: scanned one at a time
    /// (camera or keyboard-wedge scanner) or typed, one per line.
    private void serials(final OpenLineDto line) {
        Container body = new Container(BoxLayout.y());
        body.add(Ui.plain(line.title(), "SvRowTitle"));
        final TextArea all = new TextArea("", 4, 20);
        all.setUIID("SvField");
        all.setName("fu-serials-list");
        final TextField scan = Scan.field("fu-serial-scan", null);
        final Runnable add = () -> {
            String code = scan.getText().trim();
            if (code.length() > 0) {
                String now = all.getText();
                all.setText(now.length() == 0 ? code : now + "\n" + code);
                scan.setText("");
            }
        };
        scan.setDoneListener(e -> add.run());
        Container scanRow = new Container(new BorderLayout());
        scanRow.add(BorderLayout.CENTER, scan);
        scanRow.add(BorderLayout.EAST, Scan.button("fu-serial-camera", scan, add));
        body.add(scanRow);
        body.add(all);
        com.codename1.ui.Command ok = com.codename1.ui.Command.create(Lang.tr("Save"), null, null);
        com.codename1.ui.Command cancel = com.codename1.ui.Command.create(Lang.tr("Cancel"),
                null, null);
        if (com.codename1.ui.Dialog.show(Lang.tr("Serial numbers"), body, cancel, ok) != ok) {
            return;
        }
        add.run();
        List<String> list = OrderBulk.serials(all.getText());
        if (list.isEmpty()) {
            Ui.say("Scan at least one serial number");
            return;
        }
        Api.stockStates().assignSerials(store(), card.orderId(),
                new SerialAssignDto(line.orderLineId(), list), Net.to(saved -> Ui.say(
                Lang.tr("{0} serial numbers recorded", String.valueOf(saved.size()))), Ui::fail));
    }

    private static Button doc(String text, String name, Runnable run) {
        Button b = Ui.secondary(Lang.tr(text), name, e -> run.run());
        FontImage.setMaterialIcon(b, FontImage.MATERIAL_PICTURE_AS_PDF, 3.2f);
        return b;
    }

    private static boolean hasOpen(UnfulfilledDto u) {
        for (int iter = 0; u.lines() != null && iter < u.lines().size(); iter++) {
            if (u.lines().get(iter).open() > 0) {
                return true;
            }
        }
        return false;
    }

    /// The open lines with how many of each go in the parcel, where it is
    /// sent from and how.
    private Container newFulfilment(final UnfulfilledDto u) {
        Container box = new Container(BoxLayout.y());
        box.setUIID("MeCard");
        box.setName("fu-new");
        final List<OpenLineDto> lines = new ArrayList<OpenLineDto>();
        final List<TextField> quantities = new ArrayList<TextField>();
        String firstLocation = null;
        for (int iter = 0; u.lines() != null && iter < u.lines().size(); iter++) {
            OpenLineDto l = u.lines().get(iter);
            if (l.open() <= 0) {
                continue;
            }
            if (firstLocation == null && l.locationId() != null && l.locationId().length() > 0) {
                firstLocation = l.locationId();
            }
            Container row = new Container(new BorderLayout());
            row.setUIID("MeListRow");
            Container words = new Container(BoxLayout.y());
            words.add(Ui.plain(l.title(), "SvRowTitle"));
            words.add(Ui.plain((l.sku() == null ? "" : l.sku() + Ui.DOT)
                    + Lang.tr("{0} open", String.valueOf(l.open())), "SvRowDetail"));
            row.add(BorderLayout.CENTER, words);
            TextField qty = new TextField(String.valueOf(l.open()), "0", 3, TextArea.NUMERIC);
            qty.setUIID("SvField");
            qty.setName("fu-qty-" + iter);
            final OpenLineDto line = l;
            Button serials = Ui.secondary(Lang.tr("Serials"), "fu-serials-" + iter,
                    e -> serials(line));
            Container end = new Container(new FlowLayout());
            end.add(qty);
            end.add(serials);
            row.add(BorderLayout.EAST, end);
            box.add(row);
            lines.add(l);
            quantities.add(qty);
        }
        String[] ids = new String[locations.size()];
        String[] names = new String[locations.size()];
        int chosen = 0;
        for (int iter = 0; iter < locations.size(); iter++) {
            ids[iter] = locations.get(iter).id();
            names[iter] = locations.get(iter).name();
            if (ids[iter].equals(firstLocation)) {
                chosen = iter;
            }
        }
        final Picker where = new Picker();
        where.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        where.setUIID("SvField");
        where.setName("fu-location");
        if (names.length > 0) {
            where.setStrings(names);
            where.setSelectedStringIndex(chosen);
            box.add(Ui.label("Ship from", "SvLabel"));
            box.add(where);
        }
        final Picker method = MUi.picker(box, "Method", "fu-method", METHODS, "SHIP");
        box.add(MUi.buttons(Ui.primary(Lang.tr("Create parcel"), "fu-create", e -> {
            List<FulfilLineDto> out = new ArrayList<FulfilLineDto>();
            for (int iter = 0; iter < lines.size(); iter++) {
                int q = Math.min(MUi.integer(quantities.get(iter)), lines.get(iter).open());
                if (q > 0) {
                    OpenLineDto l = lines.get(iter);
                    out.add(new FulfilLineDto(l.orderLineId(), l.title(), l.sku(), q));
                }
            }
            if (out.isEmpty()) {
                Ui.say("Choose at least one item");
                return;
            }
            int at = where.getSelectedStringIndex();
            String location = at >= 0 && at < ids.length ? ids[at] : null;
            Api.fulfilment().create(store(), card.orderId(), new NewFulfillmentDto(location,
                    MUi.code(method, METHODS), out), Net.to(this::stepped, Ui::fail));
        })));
        return box;
    }

    private static boolean is(String status, String... any) {
        for (int iter = 0; iter < any.length; iter++) {
            if (any[iter].equals(status)) {
                return true;
            }
        }
        return false;
    }

    private static boolean has(String s) {
        return s != null && s.length() > 0;
    }

    /// A parcel: what is in it, where it is, and its next step.
    private Container parcel(final FulfillmentDto f) {
        final Container box = new Container(BoxLayout.y());
        box.setUIID("MeCard");
        box.setName("fu-parcel-" + f.id());
        Container top = new Container(new BorderLayout());
        Container badges = new Container(BoxLayout.x());
        badges.add(MUi.badge(f.status()));
        badges.add(Ui.badge(Lang.tr(Words.of(f.method())), "FuChip"));
        top.add(BorderLayout.WEST, badges);
        String where = f.locationId();
        for (int iter = 0; iter < locations.size(); iter++) {
            if (locations.get(iter).id().equals(f.locationId())) {
                where = locations.get(iter).name();
            }
        }
        top.add(BorderLayout.EAST, Ui.plain(where, "SvRowDetail"));
        box.add(top);
        for (int iter = 0; f.lines() != null && iter < f.lines().size(); iter++) {
            FulfilLineDto l = f.lines().get(iter);
            box.add(Ui.plain(l.quantity() + " x " + l.title()
                    + (has(l.sku()) ? Ui.DOT + l.sku() : ""), "SvRowDetail"));
        }
        if (has(f.trackingNumber())) {
            box.add(Ui.fact(Lang.tr("Tracking"), (has(f.carrier()) ? f.carrier() + " " : "")
                    + (has(f.service()) ? f.service() + Ui.DOT : "") + f.trackingNumber()));
        }
        if (f.weightGrams() > 0) {
            box.add(Ui.fact(Lang.tr("Weight"), f.weightGrams() + " g"));
        }
        List<TrackingEventDto> events = f.events();
        if (events != null && !events.isEmpty()) {
            TrackingEventDto last = events.get(events.size() - 1);
            box.add(Ui.plain(last.message() + (has(last.place()) ? Ui.DOT + last.place() : "")
                    + Ui.DOT + MUi.when(last.at()), "SvRowDetail"));
        }
        Container buttons = MUi.buttons();
        final String s = f.status();
        boolean pickup = "PICKUP".equals(f.method());
        if ("PENDING".equals(s)) {
            final TextField grams = MUi.number(box, "Weight (grams)", "fu-weight-" + f.id(),
                    f.weightGrams());
            buttons.add(Ui.primary(Lang.tr("Pack"), "fu-pack", e -> Api.fulfilment().pack(store(),
                    f.id(), new PackDto(Math.max(0, MUi.integer(grams))),
                    Net.to(this::stepped, Ui::fail))));
        }
        if (pickup && is(s, "PENDING", "PACKED")) {
            buttons.add(Ui.secondary(Lang.tr("Ready for pickup"), "fu-ready", e ->
                    Api.fulfilment().pickupReady(store(), f.id(), Net.to(this::stepped, Ui::fail))));
        }
        if (pickup && "PACKED".equals(s)) {
            buttons.add(Ui.primary(Lang.tr("Picked up"), "fu-picked", e ->
                    Api.fulfilment().pickedUp(store(), f.id(), Net.to(this::stepped, Ui::fail))));
        }
        if ("SHIP".equals(f.method()) && "PACKED".equals(s) && !has(f.trackingNumber())) {
            buttons.add(Ui.primary(Lang.tr("Get rates"), "fu-rates", e -> rates(f, box)));
        }
        if (!pickup && "PACKED".equals(s) && (has(f.trackingNumber()) || "LOCAL".equals(f.method()))) {
            buttons.add(Ui.primary(Lang.tr("Ship"), "fu-ship", e ->
                    Api.fulfilment().ship(store(), f.id(), Net.to(this::stepped, Ui::fail))));
        }
        if (has(f.trackingNumber())) {
            buttons.add(doc("Label", "fu-label", () -> Pdfs.open("Shipping label",
                    "label-" + card.number() + ".pdf", "/api/merchant/stores/" + store()
                    + "/fulfillments/" + f.id() + "/label.pdf")));
        }
        if (is(s, "SHIPPED", "IN_TRANSIT", "OUT_FOR_DELIVERY")) {
            buttons.add(Ui.secondary(Lang.tr("Mark delivered"), "fu-delivered", e ->
                    Api.fulfilment().delivered(store(), f.id(), Net.to(this::stepped, Ui::fail))));
        }
        if (is(s, "PENDING", "PACKED")) {
            buttons.add(Ui.danger(Lang.tr("Cancel"), "fu-cancel", e -> {
                if (MUi.confirm("Cancel parcel", "Its items go back to the open lines.",
                        "Cancel parcel")) {
                    Api.fulfilment().cancel(store(), f.id(), Net.to(this::stepped, Ui::fail));
                }
            }));
        }
        box.add(buttons);
        return box;
    }

    /// The carriers' quotes, cheapest first, one to choose, and Buy label.
    private void rates(final FulfillmentDto f, final Container box) {
        Api.fulfilment().rates(store(), f.id(), Net.to(list -> {
            final List<RateQuoteDto> sorted = Fulfil.sortRates(list);
            if (sorted.isEmpty()) {
                Ui.say("No carrier service fits this parcel");
                return;
            }
            RateQuoteDto fastest = Fulfil.fastest(sorted);
            Container quotes = new Container(BoxLayout.y());
            quotes.setName("fu-quotes");
            ButtonGroup group = new ButtonGroup();
            final RadioButton[] options = new RadioButton[sorted.size()];
            for (int iter = 0; iter < sorted.size(); iter++) {
                RateQuoteDto r = sorted.get(iter);
                Container row = new Container(new BorderLayout());
                row.setUIID("FuRate");
                RadioButton pick = new RadioButton("");
                pick.setName("fu-rate-" + iter);
                group.add(pick);
                pick.setSelected(iter == 0);
                options[iter] = pick;
                row.add(BorderLayout.WEST, pick);
                Container words = new Container(BoxLayout.y());
                words.add(Ui.plain(has(r.title()) ? r.title() : r.carrier() + " " + r.service(),
                        "SvRowTitle"));
                String eta = r.days() == 1 ? Lang.tr("1 day") : Lang.tr("{0} days",
                        String.valueOf(r.days()));
                words.add(Ui.plain(r.carrier() + Ui.DOT + r.service() + Ui.DOT + eta,
                        "SvRowDetail"));
                if (iter == 0 || r == fastest) {
                    Container tags = new Container(BoxLayout.x());
                    if (iter == 0) {
                        tags.add(Ui.badge(Lang.tr("Cheapest"), "SvBadgeGood"));
                    }
                    if (r == fastest) {
                        tags.add(Ui.badge(Lang.tr("Fastest"), "FuChip"));
                    }
                    words.add(tags);
                }
                row.add(BorderLayout.CENTER, words);
                row.add(BorderLayout.EAST, Ui.plain(MerchantState.money(r.cents()), "SvRowValue"));
                row.setLeadComponent(pick);
                quotes.add(row);
            }
            quotes.add(MUi.buttons(Ui.primary(Lang.tr("Buy label"), "fu-buy", e -> {
                RateQuoteDto chosen = sorted.get(0);
                for (int iter = 0; iter < options.length; iter++) {
                    if (options[iter].isSelected()) {
                        chosen = sorted.get(iter);
                    }
                }
                Api.fulfilment().buyLabel(store(), f.id(), new LabelRequestDto(chosen.service()),
                        Net.to(this::stepped, Ui::fail));
            })));
            Label heading = Ui.label("Choose a service", "SvLabel");
            box.add(heading);
            box.add(quotes);
            Ui.relayout(box);
        }, Ui::fail));
    }
}
