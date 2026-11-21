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
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.ForecastDto;
import com.codenameone.examples.bazaar.api.SuggestionApplyDto;
import com.codenameone.examples.bazaar.api.SuggestionDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ops.Forecast;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// Demand forecasts and days of cover per item and location, in a grid
/// sorted by the server, with the reorder and rebalancing suggestions and
/// the two buttons that turn them into draft purchase orders and transfers.
final class ForecastPane extends MerchantPane {
    ForecastPane() {
        super("Forecast", "merchant-forecast");
    }

    static String hundredths(long x100) {
        long whole = x100 / 100;
        long frac = Math.abs(x100 % 100);
        return whole + "." + (frac < 10 ? "0" : "") + frac;
    }

    static String cover(int days) {
        return days >= Forecast.NO_DEMAND ? "-" : String.valueOf(days);
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "forecast-refresh", this::load);
        tool(FontImage.MATERIAL_SHOPPING_CART, "Draft POs", "forecast-reorder", () -> Api.ops()
                .draftReorders(MerchantState.storeId(), new SuggestionApplyDto(""), Net.to(made -> {
                    Ui.say(made.size() + " " + Lang.tr("draft purchase orders"));
                    load();
                }, Ui::fail)));
        tool(FontImage.MATERIAL_SWAP_HORIZ, "Draft transfers", "forecast-rebalance", () -> Api.ops()
                .draftTransfers(MerchantState.storeId(), new SuggestionApplyDto(""), Net.to(made -> {
                    Ui.say(made.size() + " " + Lang.tr("draft transfers"));
                    load();
                }, Ui::fail)));
        Api.ops().forecast(MerchantState.storeId(), Net.to(rows -> {
            Rows<ForecastDto> grid = new Rows<ForecastDto>()
                    .col(Lang.tr("Item"), ForecastDto::title)
                    .col(Lang.tr("Location"), ForecastDto::locationName)
                    .col(Lang.tr("Available"), f -> Integer.valueOf(f.available()))
                    .col(Lang.tr("Per day"), f -> hundredths(f.dailyX100()))
                    .col(Lang.tr("Next 7 days"), f -> hundredths(f.nextWeekX100()))
                    .col(Lang.tr("Days of cover"), f -> cover(f.daysOfCover()))
                    .col(Lang.tr("Reorder"), f -> Integer.valueOf(f.reorderQuantity()));
            grid.set(rows);
            final Container page = new Container(BoxLayout.y());
            page.setName("forecast-page");
            page.add(MUi.grid(grid, "forecast-grid"));
            Api.ops().suggestions(MerchantState.storeId(), Net.to(found -> {
                for (int iter = 0; iter < found.size(); iter++) {
                    SuggestionDto s = found.get(iter);
                    page.add(Ui.row((char) 0, s.title(), Lang.tr(Words.of(s.kind())) + ": " + s.reason(),
                            Ui.plain(String.valueOf(s.quantity()), "SvRowValue")));
                }
                Ui.relayout(page);
            }, Ui::fail));
            root(page);
        }, Ui::fail));
    }
}
