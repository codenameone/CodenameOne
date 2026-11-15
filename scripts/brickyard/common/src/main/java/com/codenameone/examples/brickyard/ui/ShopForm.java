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
package com.codenameone.examples.brickyard.ui;

import com.codename1.payment.CommerceManager;
import com.codename1.payment.Offering;
import com.codename1.payment.Offerings;
import com.codename1.payment.center.CustomerCenter;
import com.codename1.payment.paywall.Paywall;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codenameone.examples.brickyard.AppConfig;
import com.codenameone.examples.brickyard.engage.Ads;
import com.codenameone.examples.brickyard.engage.Store;

/// Bricks, the currency experiences sell their items for, and Brickyard Plus,
/// the subscription. A parental spending limit set in Settings blocks a
/// purchase that would pass it.
public final class ShopForm {
    private ShopForm() {
    }

    public static void show() {
        final Form f = Ui.section("Shop", "shop", 3);
        final Container content = Ui.content(f);
        final com.codename1.ui.Label balance = Ui.bricks();
        content.add(balance);
        Store.listen(() -> {
            com.codename1.ui.Label now = Ui.bricks();
            balance.setText(now.getText());
            f.revalidate();
        });

        content.add(Ui.label("Bricks", "RowTitle"));
        if (Store.available()) {
            content.add(pack(AppConfig.SKU_BRICKS_400, "400 Bricks", 400));
            content.add(pack(AppConfig.SKU_BRICKS_1000, "1,000 Bricks", 1000));
        } else {
            content.add(Ui.text("Purchases are made through the app store on a phone.", "Muted"));
        }
        if (Ads.rewardedAvailable()) {
            content.add(Ui.secondary("Watch an ad for free Bricks", "rewardedButton",
                    e -> Ads.rewarded(() -> {
                        Ui.toast("Thanks! Your Bricks are on the way.");
                        Store.refresh();
                    }, () -> Ui.toast("No ad is available right now."))));
        }

        content.add(Ui.label("Brickyard Plus", "RowTitle"));
        if (Store.plus()) {
            content.add(Ui.text("You are a Plus member: a monthly Bricks stipend and member-only items.",
                    "Body"));
        } else {
            content.add(Ui.text("A monthly Bricks stipend and member-only items.", "Body"));
            content.add(Ui.primary("See Plus", "plusButton", e -> plus()));
        }
        content.add(Ui.link("Restore or manage purchases", "manageButton", e -> CustomerCenter.show()));
        f.show();
    }

    static Container pack(final String sku, String title, final int bricks) {
        return Ui.panel(Ui.label(title, "CardTitle"), Ui.primary("Buy", "buy" + bricks, e -> {
            if (!SettingsForm.allowSpend(bricks)) {
                Ui.toast("That would pass the spending limit a parent set in Settings.");
                return;
            }
            SettingsForm.recordSpend(bricks);
            Store.buy(sku);
        }));
    }

    static void plus() {
        Offerings offerings = CommerceManager.getInstance().getOfferings();
        Offering current = offerings == null ? null : offerings.getCurrent();
        if (current == null) {
            Ui.toast("Plus is not on sale right now.");
            return;
        }
        Paywall.show(current, null);
    }
}
