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
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// The store's B2B companies: name, VAT number, payment terms and the price
/// list each one buys at.
final class CompaniesPane extends MerchantPane {
    CompaniesPane() {
        super("Companies", "merchant-companies");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "companies-refresh", this::load);
        Api.trade().companies(MerchantState.storeId(), Net.to(list -> {
            Container page = new Container(new BorderLayout());
            Rows<com.codenameone.examples.bazaar.api.CompanyDto> rows =
                    new Rows<com.codenameone.examples.bazaar.api.CompanyDto>()
                    .col(Lang.tr("Company"), c -> c.name())
                    .col(Lang.tr("VAT number"), c -> c.vatId() == null ? "" : c.vatId())
                    .col(Lang.tr("Terms (days)"), c -> String.valueOf(c.termsDays()))
                    .col(Lang.tr("Price list"), c -> c.priceListId() == null ? Lang.tr("Retail")
                            : c.priceListId());
            rows.set(list);
            if (list.isEmpty()) {
                page.add(BorderLayout.CENTER, Ui.empty(FontImage.MATERIAL_BUSINESS,
                        "No companies yet", "A buyer's company appears once it is set up."));
            } else {
                page.add(BorderLayout.CENTER, MUi.grid(rows, "companies-grid"));
            }
            root(page);
        }, Ui::fail));
    }
}
