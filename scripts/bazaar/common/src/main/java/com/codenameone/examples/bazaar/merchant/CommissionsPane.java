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
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.CommissionDto;
import com.codenameone.examples.bazaar.api.StaffRateDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// Staff commissions: each member's rate, and what they sold at the counter
/// in the last 30 days with the commission earned. A sale keeps the rate of
/// the day it was made, so changing a rate changes only sales to come.
final class CommissionsPane extends MerchantPane {
    CommissionsPane() {
        super("Commissions", "merchant-commissions");
    }

    private Container page;

    @Override
    void load() {
        page = new Container(BoxLayout.y());
        page.setName("commissions-page");
        page.setScrollableY(true);
        root(page);
        final String store = MerchantState.storeId();
        Api.posTools().staffRates(store, Net.to(rates -> {
            Ui.section(page, Lang.tr("Rates"));
            for (int iter = 0; rates != null && iter < rates.size(); iter++) {
                final StaffRateDto r = rates.get(iter);
                page.add(Ui.tap(Ui.row(FontImage.MATERIAL_BADGE, name(r.displayName(),
                        r.username()), r.role(), Ui.label(CommissionRate.show(r.rateBp()),
                        "SvValue")), "commission-rate-" + iter, e -> edit(r)));
            }
            Api.posTools().commissions(store, "", "", Net.to(this::report, Ui::fail));
            page.revalidateLater();
        }, Ui::fail));
    }

    private void report(List<CommissionDto> rows) {
        Ui.section(page, Lang.tr("Counter sales, last 30 days"));
        if (rows == null || rows.isEmpty()) {
            page.add(Ui.label("No counter sales yet", "SvMuted"));
        }
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            CommissionDto c = rows.get(iter);
            page.add(Ui.row(FontImage.MATERIAL_POINT_OF_SALE, name(c.displayName(), c.username()),
                    Lang.tr("{0} sales, {1} net", c.sales(), Ui.money(c.netCents(), null)),
                    Ui.label(Ui.money(c.commissionCents(), null), "SvValue")));
        }
        page.revalidateLater();
    }

    private static String name(String display, String username) {
        return display == null || display.length() == 0 ? username : display;
    }

    private void edit(final StaffRateDto r) {
        Container c = new Container(BoxLayout.y());
        final TextField rate = Ui.field(c, Lang.tr("Commission rate (%)"), "commission-rate-field",
                "5");
        rate.setText(CommissionRate.show(r.rateBp()));
        if (!Dialog.show(Lang.tr("Commission"), c, new com.codename1.ui.Command(Lang.tr("Cancel")),
                new com.codename1.ui.Command(Lang.tr("Save"))).getCommandName()
                .equals(Lang.tr("Save"))) {
            return;
        }
        long bp = CommissionRate.parse(rate.getText());
        if (bp < 0) {
            Ui.say("Enter a percentage from 0 to 100");
            return;
        }
        Api.posTools().saveStaffRate(MerchantState.storeId(), new StaffRateDto(r.username(),
                r.displayName(), r.role(), bp), Net.to(saved -> load(), Ui::fail));
    }
}
