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
import com.codenameone.examples.bazaar.api.TaxRateDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// Tax rates by country and region. Whether prices include tax is a store
/// setting; these say how much.
final class TaxesPane extends MerchantPane {
    TaxesPane() {
        super("Taxes", "merchant-taxes");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "Add tax rate", "tax-add", () -> edit(null));
        Api.taxes().list(MerchantState.storeId(), Net.to(this::fill, Ui::fail));
    }

    private void fill(List<TaxRateDto> list) {
        Container page = MUi.page("taxes-list");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        {
            boolean inclusive = MerchantState.store() != null && MerchantState.store().taxInclusive();
            page.add(Ui.text(Lang.tr(inclusive ? "Prices include tax." : "Tax is added at checkout."),
                    "SvMuted"));
        }
        for (final TaxRateDto t : list) {
            String where = t.country() + (t.region() == null || t.region().length() == 0 ? ""
                    : " / " + t.region());
            Container tap = Ui.tap(Ui.row(FontImage.MATERIAL_RECEIPT, t.name(), where
                    + (t.appliesToShipping() ? Ui.DOT + Lang.tr("Also on shipping") : ""),
                    Ui.plain(ProductCsv.major(t.basisPoints()) + "%", "SvRowValue")),
                    "tax-" + t.id(), e -> edit(t));
            RowMenus.attach(tap, Command.create(Lang.tr("Delete"), null, e -> Api.taxes()
                    .delete(MerchantState.storeId(), t.id(), Net.to(x -> load(), Ui::fail))));
            card.add(tap);
        }
        if (list.isEmpty()) {
            card.add(Ui.empty(FontImage.MATERIAL_RECEIPT, "No tax rates",
                    "Add the rates you collect, by country and region."));
        }
        page.add(card);
        root(page);
    }

    private void edit(TaxRateDto t) {
        Container body = new Container(BoxLayout.y());
        TextField name = MUi.text(body, "Name", "tax-name", t == null ? "" : t.name());
        TextField country = MUi.text(body, "Country code", "tax-country", t == null ? "US" : t.country());
        TextField region = MUi.text(body, "Region (optional)", "tax-region", t == null ? "" : t.region());
        TextField rate = Ui.field(body, Lang.tr("Rate in percent"), "tax-rate",
                t == null ? "" : ProductCsv.major(t.basisPoints()), TextField.DECIMAL);
        CheckBox shipping = new CheckBox(Lang.tr("Charge on shipping too"));
        shipping.setSelected(t != null && t.appliesToShipping());
        body.add(shipping);
        Command ok = Command.create(Lang.tr("Save"), null, null);
        Command cancel = Command.create(Lang.tr("Cancel"), null, null);
        if (Dialog.show(Lang.tr("Tax rate"), body, cancel, ok) != ok) {
            return;
        }
        long bp = ProductCsv.cents(rate.getText());
        if (bp < 0 || bp > 10000 || country.getText().trim().length() == 0) {
            Ui.say("Enter a country and a rate between 0 and 100");
            return;
        }
        Api.taxes().save(MerchantState.storeId(), new TaxRateDto(t == null ? null : t.id(),
                country.getText().trim(), region.getText().trim(), name.getText().trim(), (int) bp,
                shipping.isSelected()), Net.to(x -> load(), Ui::fail));
    }
}
