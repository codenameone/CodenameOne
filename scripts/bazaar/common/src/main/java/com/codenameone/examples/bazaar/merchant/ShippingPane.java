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
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
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
final class ShippingPane extends MerchantPane {
    ShippingPane() {
        super("Shipping", "merchant-shipping");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "Add zone", "zone-add", () -> zone(null));
        Api.shipping().zones(MerchantState.storeId(), Net.to(this::fill, Ui::fail));
    }

    private void fill(List<ShippingZoneDto> zones) {
        Container page = MUi.page("shipping-list");
        for (final ShippingZoneDto z : zones) {
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
            card.add(MUi.buttons(
                    Ui.secondary(Lang.tr("Edit zone"), "zone-edit-" + z.id(), e -> zone(z)),
                    Ui.secondary(Lang.tr("Add rate"), "rate-add-" + z.id(), e -> rate(z, null))));
            page.add(card);
        }
        if (zones.isEmpty()) {
            page.add(Ui.empty(FontImage.MATERIAL_LOCAL_SHIPPING, "No shipping zones",
                    "Add a zone for the countries you ship to, then its rates."));
        }
        root(page);
    }

    private void zone(final ShippingZoneDto z) {
        Container body = new Container(BoxLayout.y());
        TextField name = MUi.text(body, "Name", "zone-name", z == null ? "" : z.name());
        TextField countries = MUi.text(body, "Country codes, comma separated", "zone-countries",
                z == null ? "" : z.countries());
        Command ok = Command.create(Lang.tr("Save"), null, null);
        Command delete = Command.create(Lang.tr("Delete"), null, null);
        Command cancel = Command.create(Lang.tr("Cancel"), null, null);
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
                z == null ? new ArrayList<ShippingRateDto>() : z.rates()), Net.to(x -> load(), Ui::fail));
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
        TextField etaMin = MUi.number(body, "Fastest delivery, days", "rate-eta-min",
                r == null ? 0 : r.etaDaysMin());
        TextField etaMax = MUi.number(body, "Slowest delivery, days", "rate-eta-max",
                r == null ? 0 : r.etaDaysMax());
        Command ok = Command.create(Lang.tr("Save"), null, null);
        Command cancel = Command.create(Lang.tr("Cancel"), null, null);
        if (Dialog.show(Lang.tr("Shipping rate"), body, cancel, ok) != ok
                || name.getText().trim().length() == 0) {
            return;
        }
        int lo = MUi.integer(etaMin);
        int hi = Math.max(lo, MUi.integer(etaMax));
        Api.shipping().saveRate(MerchantState.storeId(), new ShippingRateDto(r == null ? null : r.id(),
                z.id(), name.getText().trim(), MUi.code(kind, Words.RATE_KINDS), MUi.cents(price),
                Math.max(0, MUi.integer(min)), Math.max(0, MUi.integer(max)),
                r == null ? null : r.carrier(), r == null ? null : r.service(), lo, hi,
                r == null ? null : r.locationId(), r == null ? 0 : r.radiusMeters(),
                r == null ? z.rates().size() : r.position()), Net.to(x -> load(), Ui::fail));
    }
}
