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

import com.codename1.ui.CheckBox;
import com.codename1.ui.Container;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bazaar.api.TrialDto;
import com.codenameone.examples.bazaar.api.UpsellDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codename1.appkit.Net;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The product editor's offers card: "Try before you buy" (off, or 1 to 6
/// days of the card only being authorized) and whether this is the store's
/// one post-purchase upsell. Each change is saved as it is made.
final class ProductOffers {
    /// The longest trial the server takes: a card hold lapses after about 7 days.
    static final int MAX_DAYS = 6;

    private ProductOffers() {
    }

    /// Fills `card` for `productId`.
    static void add(final Container card, final String productId) {
        final String storeId = MerchantState.storeId();
        card.add(Ui.text(Lang.tr("Checkout only authorizes the card; it is charged when the "
                + "trial ends, and a return inside the trial releases the hold."), "SvMuted"));
        final Picker trial = new Picker();
        trial.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        trial.setUIID("SvField");
        trial.setName("product-trial-days");
        String[] labels = new String[MAX_DAYS + 1];
        labels[0] = Lang.tr("Off");
        for (int iter = 1; iter <= MAX_DAYS; iter++) {
            labels[iter] = iter == 1 ? Lang.tr("1 day") : Lang.tr("{0} days",
                    String.valueOf(iter));
        }
        trial.setStrings(labels);
        trial.setSelectedStringIndex(0);
        card.add(Ui.label("Try before you buy", "SvLabel"));
        card.add(trial);
        final CheckBox upsell = new CheckBox(Lang.tr("Offer after checkout"));
        upsell.setName("product-upsell");
        card.add(upsell);
        card.add(Ui.text(Lang.tr("One product per store is offered on the order placed page, "
                + "added in one tap as a separate order on the same card."), "SvMuted"));
        Api.trials().trials(storeId, Net.to(rows -> {
            trial.setSelectedStringIndex(days(rows, productId));
            trial.addActionListener(e -> Api.trials().setTrial(storeId, productId,
                    new TrialDto(productId, trial.getSelectedStringIndex()),
                    Net.to(saved -> Ui.say(Lang.tr("Saved")), Ui::fail)));
        }, Ui::fail));
        Api.upsells().upsell(storeId, Net.to(u -> {
            upsell.setSelected(u != null && productId.equals(u.productId()));
            upsell.addActionListener(e -> Api.upsells().setUpsell(storeId,
                    new UpsellDto(upsell.isSelected() ? productId : "", "", 0, 0),
                    Net.to(saved -> Ui.say(Lang.tr("Saved")), Ui::fail)));
        }, Ui::fail));
    }

    /// The trial days of `productId` among the store's `rows`, 0 for none.
    static int days(List<TrialDto> rows, String productId) {
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            TrialDto t = rows.get(iter);
            if (productId.equals(t.productId())) {
                return Math.max(0, Math.min(MAX_DAYS, t.days()));
            }
        }
        return 0;
    }
}
