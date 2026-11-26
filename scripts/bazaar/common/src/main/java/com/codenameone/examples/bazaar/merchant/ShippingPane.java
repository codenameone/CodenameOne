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
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bazaar.api.FulfilmentServiceOrderDto;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.ProductCardDto;
import com.codenameone.examples.bazaar.api.RoutingDto;
import com.codenameone.examples.bazaar.api.RoutingRuleDto;
import com.codenameone.examples.bazaar.api.ShippingProfileDto;
import com.codenameone.examples.bazaar.api.ShippingRateDto;
import com.codenameone.examples.bazaar.api.ShippingZoneDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// Shipping zones -- a list of countries -- and the rates each offers: flat,
/// by weight or order value, free over an amount, pickup and local delivery.
/// Shipping profiles group products with zones of their own, and the order
/// routing rules and the fulfilment service decide where orders ship from.
final class ShippingPane extends MerchantPane {
    ShippingPane() {
        super("Shipping", "merchant-shipping");
    }

    private final List<ShippingProfileDto> profiles = new ArrayList<ShippingProfileDto>();
    private final List<ProductCardDto> catalog = new ArrayList<ProductCardDto>();
    private final List<LocationDto> places = new ArrayList<LocationDto>();
    private RoutingDto routing;

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "Add zone", "zone-add", () -> zone(null, ""));
        tool(FontImage.MATERIAL_INVENTORY_2, "Add shipping profile", "profile-add",
                () -> profile(null));
        final String store = MerchantState.storeId();
        Api.catalog().merchantProducts(store, null, null, null, null, null, null, "title", "0",
                "500", Net.to(page -> {
                    catalog.clear();
                    catalog.addAll(page.items());
                }, (s, m) -> { }));
        Api.stores().locations(store, Net.to(found -> {
            places.clear();
            places.addAll(found);
        }, (s, m) -> { }));
        Api.shipping().profiles(store, Net.to(found -> {
            profiles.clear();
            profiles.addAll(found);
            Api.routing().routing(store, Net.to(r -> {
                routing = r;
                Api.shipping().zones(store, Net.to(this::fill, Ui::fail));
            }, Ui::fail));
        }, Ui::fail));
    }

    private void fill(List<ShippingZoneDto> zones) {
        Container page = MUi.page("shipping-list");
        List<ShippingZoneDto> general = ShippingSetup.zonesOf(zones, "");
        for (final ShippingZoneDto z : general) {
            zoneCard(page, z);
        }
        if (general.isEmpty()) {
            page.add(Ui.empty(FontImage.MATERIAL_LOCAL_SHIPPING, "No shipping zones",
                    "Add a zone for the countries you ship to, then its rates."));
        }
        for (final ShippingProfileDto p : profiles) {
            Ui.section(page, Lang.tr("Shipping profile: {0}", p.name()));
            Container card = new Container(BoxLayout.y());
            card.setUIID("MeCard");
            card.add(Ui.text(Lang.tr("{0} products", String.valueOf(p.productIds().size())),
                    "SvMuted"));
            card.add(MUi.cardButtons(
                    Ui.secondary(Lang.tr("Edit profile"), "profile-edit-" + p.id(), e -> profile(p)),
                    Ui.secondary(Lang.tr("Add zone"), "profile-zone-add-" + p.id(),
                            e -> zone(null, p.id()))));
            page.add(card);
            for (final ShippingZoneDto z : ShippingSetup.zonesOf(zones, p.id())) {
                zoneCard(page, z);
            }
        }
        routingSection(page);
        root(page);
    }

    private void zoneCard(Container page, final ShippingZoneDto z) {
        Ui.section(page, z.name());
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        card.add(Ui.text(z.countries(), "SvMuted"));
        for (final ShippingRateDto r : z.rates()) {
            Container tap = Ui.tap(Ui.row((char) 0, r.name(), Lang.tr(Words.of(r.kind()))
                    + (r.etaDaysMax() > 0 ? Ui.DOT + Lang.tr("{0}-{1} days",
                    String.valueOf(r.etaDaysMin()), String.valueOf(r.etaDaysMax())) : ""),
                    Ui.plain(r.priceCents() == 0 ? Lang.tr("Free") : MerchantState.money(r.priceCents()),
                            "SvRowValue")), "rate-" + r.id(), e -> rate(z, r));
            RowMenus.attach(tap, Command.create(Lang.tr("Delete"), null, e -> Api.shipping()
                    .deleteRate(MerchantState.storeId(), r.id(), Net.to(x -> load(), Ui::fail))));
            card.add(tap);
        }
        card.add(MUi.cardButtons(
                Ui.secondary(Lang.tr("Edit zone"), "zone-edit-" + z.id(), e -> zone(z, z.profileId())),
                Ui.secondary(Lang.tr("Add rate"), "rate-add-" + z.id(), e -> rate(z, null))));
        page.add(card);
    }

    private void zone(final ShippingZoneDto z, final String profileId) {
        Container body = new Container(BoxLayout.y());
        TextField name = MUi.text(body, "Name", "zone-name", z == null ? "" : z.name());
        TextField countries = MUi.text(body, "Country codes, comma separated", "zone-countries",
                z == null ? "" : z.countries());
        Command ok = new com.codename1.ui.Command(Lang.tr("Save"));
        Command delete = new com.codename1.ui.Command(Lang.tr("Delete"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        Command answer = z == null ? Dialog.show(Lang.tr("Shipping zone"), body, cancel, ok)
                : Dialog.show(Lang.tr("Shipping zone"), body, cancel, delete, ok);
        if (answer == delete) {
            Api.shipping().deleteZone(MerchantState.storeId(), z.id(), Net.to(x -> load(), Ui::fail));
            return;
        }
        if (answer != ok || name.getText().trim().length() == 0) {
            return;
        }
        Api.shipping().saveZone(MerchantState.storeId(), new ShippingZoneDto(z == null ? null : z.id(),
                name.getText().trim(), countries.getText().trim(), z == null ? 0 : z.position(),
                z == null ? new ArrayList<ShippingRateDto>() : z.rates(), profileId),
                Net.to(x -> load(), Ui::fail));
    }

    /// A shipping profile: its name and the products in it. A product ticked
    /// here leaves whatever profile it was in.
    private void profile(final ShippingProfileDto p) {
        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        TextField name = MUi.text(body, "Name", "profile-name", p == null ? "" : p.name());
        body.add(Ui.label(Lang.tr("Products in this profile"), "SvLabel"));
        final List<CheckBox> boxes = new ArrayList<CheckBox>();
        for (ProductCardDto c : catalog) {
            ShippingProfileDto in = ShippingSetup.profileOf(profiles, c.id());
            CheckBox box = new CheckBox(in == null || (p != null && in.id().equals(p.id()))
                    ? c.title() : c.title() + Ui.DOT + in.name());
            box.setName("profile-product-" + c.id());
            box.putClientProperty("id", c.id());
            box.setSelected(p != null && p.productIds().contains(c.id()));
            boxes.add(box);
            body.add(box);
        }
        Command ok = new com.codename1.ui.Command(Lang.tr("Save"));
        Command delete = new com.codename1.ui.Command(Lang.tr("Delete"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        Command answer = p == null ? Dialog.show(Lang.tr("Shipping profile"), body, cancel, ok)
                : Dialog.show(Lang.tr("Shipping profile"), body, cancel, delete, ok);
        if (answer == delete) {
            Api.shipping().deleteProfile(MerchantState.storeId(), p.id(),
                    Net.to(x -> load(), Ui::fail));
            return;
        }
        if (answer != ok || name.getText().trim().length() == 0) {
            return;
        }
        List<String> ids = new ArrayList<String>();
        for (CheckBox box : boxes) {
            if (box.isSelected()) {
                ids.add((String) box.getClientProperty("id"));
            }
        }
        Api.shipping().saveProfile(MerchantState.storeId(), new ShippingProfileDto(
                p == null ? null : p.id(), name.getText().trim(),
                p == null ? profiles.size() : p.position(), ids), Net.to(x -> load(), Ui::fail));
    }

    // -------------------------------------------------------------- routing

    private String placeName(String id) {
        for (LocationDto l : places) {
            if (l.id().equals(id)) {
                return l.name();
            }
        }
        return id;
    }

    private void routingSection(Container page) {
        Ui.section(page, Lang.tr("Order routing"));
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        RoutingDto r = routing;
        if (r != null) {
            card.add(Ui.text(r.allowSplit() ? Lang.tr("Orders may ship from several locations")
                    : Lang.tr("Whole orders ship from one location when one can fill them"),
                    "SvMuted"));
            if (r.service() != null && r.service().length() > 0) {
                card.add(Ui.text(Lang.tr("Fulfilment service at {0}",
                        placeName(r.serviceLocationId())), "SvMuted"));
            }
            for (final RoutingRuleDto rule : r.rules()) {
                String detail = "PRIORITY".equals(rule.kind()) ? names(rule.locationIds()) : "";
                Container tap = Ui.tap(Ui.row((char) 0, Lang.tr(Words.of(rule.kind())), detail,
                        Ui.plain(String.valueOf(rule.position() + 1), "SvRowValue")),
                        "routing-rule-" + rule.id(), e -> rule(rule));
                RowMenus.attach(tap, Command.create(Lang.tr("Delete"), null, e -> Api.routing()
                        .deleteRule(MerchantState.storeId(), rule.id(), Net.to(x -> load(),
                                Ui::fail))));
                card.add(tap);
            }
        }
        card.add(MUi.cardButtons(
                Ui.secondary(Lang.tr("Routing settings"), "routing-settings", e -> settings()),
                Ui.secondary(Lang.tr("Add rule"), "routing-rule-add", e -> rule(null))));
        card.add(Ui.secondary(Lang.tr("Fulfilment service orders"), "routing-service-orders",
                e -> serviceOrders()));
        page.add(card);
    }

    private String names(String list) {
        List<String> ids = ShippingSetup.ids(list);
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < ids.size(); iter++) {
            if (iter > 0) {
                b.append(", ");
            }
            b.append(placeName(ids.get(iter)));
        }
        return b.toString();
    }

    private static Picker strings(Container body, String caption, String name, String[] labels,
            int selected) {
        Picker p = new Picker();
        p.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        p.setUIID("SvField");
        p.setName(name);
        p.setStrings(labels);
        p.setSelectedStringIndex(selected);
        body.add(Ui.label(caption, "SvLabel"));
        body.add(p);
        return p;
    }

    private void settings() {
        if (routing == null) {
            return;
        }
        Container body = new Container(BoxLayout.y());
        CheckBox split = new CheckBox(Lang.tr("Allow an order to ship from several locations"));
        split.setName("routing-split");
        split.setSelected(routing.allowSplit());
        body.add(split);
        Picker service = strings(body, Lang.tr("Fulfilment service (3PL)"), "routing-service",
                new String[] {Lang.tr("None"), Lang.tr("Simulated fulfilment service")},
                "SIMULATED".equals(routing.service()) ? 1 : 0);
        String[] ids = new String[places.size() + 1];
        String[] labels = new String[places.size() + 1];
        ids[0] = "";
        labels[0] = Lang.tr("None");
        int at = 0;
        for (int iter = 0; iter < places.size(); iter++) {
            ids[iter + 1] = places.get(iter).id();
            labels[iter + 1] = places.get(iter).name();
            if (places.get(iter).id().equals(routing.serviceLocationId())) {
                at = iter + 1;
            }
        }
        Picker where = strings(body, Lang.tr("Location the service runs"),
                "routing-service-location", labels, at);
        Command ok = new com.codename1.ui.Command(Lang.tr("Save"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Routing settings"), body, cancel, ok) != ok) {
            return;
        }
        int w = Math.max(0, Math.min(ids.length - 1, where.getSelectedStringIndex()));
        int sv = Math.max(0, Math.min(ShippingSetup.SERVICES.length - 1,
                service.getSelectedStringIndex()));
        Api.routing().saveRouting(MerchantState.storeId(), new RoutingDto(split.isSelected(),
                ids[w], ShippingSetup.SERVICES[sv], new ArrayList<RoutingRuleDto>()),
                Net.to(x -> load(), Ui::fail));
    }

    private void rule(final RoutingRuleDto rule) {
        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        Picker kind = MUi.picker(body, Lang.tr("Rule"), "routing-rule-kind",
                ShippingSetup.RULE_KINDS, rule == null ? "CLOSEST" : rule.kind());
        body.add(Ui.label(Lang.tr("Locations, first preferred (priority rules)"), "SvLabel"));
        List<String> chosen = ShippingSetup.ids(rule == null ? "" : rule.locationIds());
        final List<CheckBox> boxes = new ArrayList<CheckBox>();
        for (LocationDto l : places) {
            CheckBox box = new CheckBox(l.name());
            box.setName("routing-rule-location-" + l.id());
            box.putClientProperty("id", l.id());
            box.setSelected(chosen.contains(l.id()));
            boxes.add(box);
            body.add(box);
        }
        Command ok = new com.codename1.ui.Command(Lang.tr("Save"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Routing rule"), body, cancel, ok) != ok) {
            return;
        }
        // Locations already in the rule keep their order; new ones go after.
        List<String> ticked = new ArrayList<String>();
        for (CheckBox box : boxes) {
            if (box.isSelected()) {
                ticked.add((String) box.getClientProperty("id"));
            }
        }
        Api.routing().saveRule(MerchantState.storeId(), new RoutingRuleDto(
                rule == null ? null : rule.id(), MUi.code(kind, ShippingSetup.RULE_KINDS),
                ShippingSetup.join(ShippingSetup.keepOrder(chosen, ticked)), rule == null
                    ? ShippingSetup.nextPosition(routing == null ? null : routing.rules())
                    : rule.position()), Net.to(x -> load(), Ui::fail));
    }

    private void serviceOrders() {
        Api.routing().serviceOrders(MerchantState.storeId(), Net.to(list -> {
            Container body = new Container(BoxLayout.y());
            body.setScrollableY(true);
            for (FulfilmentServiceOrderDto o : list) {
                body.add(Ui.row((char) 0, "#" + o.orderNumber(), Lang.tr(Words.of(o.status()))
                        + (o.trackingNumber() == null || o.trackingNumber().length() == 0 ? ""
                            : Ui.DOT + o.trackingNumber()), Ui.plain(o.externalId(), "SvMuted")));
            }
            if (list.isEmpty()) {
                body.add(Ui.text(Lang.tr("Nothing has been handed to the service yet."),
                        "SvMuted"));
            }
            Dialog.show(Lang.tr("Fulfilment service orders"), body,
                    new com.codename1.ui.Command(Lang.tr("Close")));
        }, Ui::fail));
    }

    private void rate(ShippingZoneDto z, ShippingRateDto r) {
        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        TextField name = MUi.text(body, "Name", "rate-name", r == null ? "" : r.name());
        Picker kind = MUi.picker(body, Lang.tr("Type"), "rate-kind", Words.RATE_KINDS,
                r == null ? "FLAT" : r.kind());
        TextField price = MUi.money(body, "Price", "rate-price", r == null ? 0 : r.priceCents());
        TextField min = Ui.field(body, Lang.tr("From (grams, or order value)"), "rate-min",
                r == null ? "0" : String.valueOf(r.minValue()), TextField.NUMERIC);
        TextField max = Ui.field(body, Lang.tr("Up to (0 for no limit)"), "rate-max",
                r == null ? "0" : String.valueOf(r.maxValue()), TextField.NUMERIC);
        // CARRIER rates: the carrier prices each service live at checkout.
        TextField services = Ui.field(body, Lang.tr("Carrier services (blank for all)"),
                "rate-services", r == null ? "" : r.service(), TextField.ANY);
        TextField markup = MUi.number(body, "Carrier markup, %", "rate-markup",
                r == null ? 0 : r.markupBps() / 100);
        TextField fallback = MUi.money(body, "Price if the carrier is down", "rate-fallback",
                r == null ? 0 : r.fallbackCents());
        TextField etaMin = MUi.number(body, "Fastest delivery, days", "rate-eta-min",
                r == null ? 0 : r.etaDaysMin());
        TextField etaMax = MUi.number(body, "Slowest delivery, days", "rate-eta-max",
                r == null ? 0 : r.etaDaysMax());
        Command ok = new com.codename1.ui.Command(Lang.tr("Save"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Shipping rate"), body, cancel, ok) != ok
                || name.getText().trim().length() == 0) {
            return;
        }
        int lo = MUi.integer(etaMin);
        int hi = Math.max(lo, MUi.integer(etaMax));
        Api.shipping().saveRate(MerchantState.storeId(), new ShippingRateDto(r == null ? null : r.id(),
                z.id(), name.getText().trim(), MUi.code(kind, Words.RATE_KINDS), MUi.cents(price),
                Math.max(0, MUi.integer(min)), Math.max(0, MUi.integer(max)),
                r == null ? null : r.carrier(), services.getText().trim(), lo, hi,
                r == null ? null : r.locationId(), r == null ? 0 : r.radiusMeters(),
                r == null ? z.rates().size() : r.position(),
                Math.max(0, Math.min(1000, MUi.integer(markup))) * 100,
                Math.max(0, MUi.cents(fallback))), Net.to(x -> load(), Ui::fail));
    }
}
