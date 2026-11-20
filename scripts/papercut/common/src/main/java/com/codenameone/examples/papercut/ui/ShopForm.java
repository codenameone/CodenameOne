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
package com.codenameone.examples.papercut.ui;

import com.codename1.appkit.Net;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.papercut.api.AdRewardDto;
import com.codenameone.examples.papercut.api.BuyDto;
import com.codenameone.examples.papercut.api.EquipDto;
import com.codenameone.examples.papercut.api.ShopItemDto;
import com.codenameone.examples.papercut.api.WalletDto;
import com.codenameone.examples.papercut.engage.Ads;
import com.codenameone.examples.papercut.engage.Store;
import com.codenameone.examples.papercut.net.Api;
import com.codenameone.examples.papercut.net.Session;
import java.util.List;

/// The shop: papers to cut the quill from, spare erasers for the next run,
/// ink coins for real money, and coins for watching an ad. Everything is
/// granted by the server; this screen only asks.
public final class ShopForm {
    private ShopForm() {
    }

    public static void show() {
        final Form f = Ui.form("Shop", "shop");
        Ui.desktopColumn(f);
        Ui.back(f, Nav::home);
        final Container body = new Container(BoxLayout.y());
        f.add(body);
        Store.listen(wallet -> Ui.later(() -> {
            com.codename1.io.Preferences.set("papercut.paid", true);
            reload(body);
        }));
        reload(body);
        f.show();
    }

    static void reload(final Container body) {
        Api.shop().wallet(Net.to(wallet -> Api.shop().items(Net.to(items -> Ui.later(() -> fill(body, wallet,
                items)), Ui::failed)), Ui::failed));
    }

    private static void fill(final Container body, WalletDto w, List<ShopItemDto> items) {
        body.removeAll();
        Container top = new Container(new BorderLayout(), "Card");
        top.add(BorderLayout.WEST, Ui.wallet(w.inkCoins()));
        top.add(BorderLayout.EAST, new Label(w.erasers() + " spare erasers", "Muted"));
        body.add(top);
        body.add(Ui.secondary("Watch an ad for 15 ink", "adReward", e -> Ads.rewarded(
                () -> Api.shop().adReward(new AdRewardDto("ad-" + System.currentTimeMillis()),
                        Net.to(after -> Ui.later(() -> reload(body)), Ui::failed)),
                () -> Ui.toast("No ad right now. Try again later."))));
        for (final ShopItemDto item : items) {
            Container row = new Container(new BorderLayout(), "Row");
            Label name = new Label(item.name());
            if (item.colour() != null && item.colour().startsWith("#")) {
                name.getAllStyles().setFgColor(Integer.parseInt(item.colour().substring(1), 16));
            }
            row.add(BorderLayout.CENTER, name);
            row.add(BorderLayout.EAST, action(body, w, item));
            body.add(row);
        }
        body.revalidate();
    }

    private static Button action(final Container body, WalletDto w, final ShopItemDto item) {
        if (item.sku() != null && item.sku().length() > 0) {
            String price = Store.price(item.sku());
            return Ui.primary(price == null ? "Buy" : price, "buy-" + item.id(), e -> Store.buy(item.sku()));
        }
        boolean owned = w.owned() != null && w.owned().contains(item.id());
        if ("QUILL".equals(item.kind()) && owned) {
            boolean on = item.id().equals(w.quill());
            Button b = Ui.secondary(on ? "In use" : "Use", "equip-" + item.id(), e -> Api.shop().equip(
                    new EquipDto(item.id()), Net.to(after -> Ui.later(() -> {
                        Session.refreshUser(() -> { }, Ui::failed);
                        reload(body);
                    }), Ui::failed)));
            b.setEnabled(!on);
            return b;
        }
        Button b = Ui.primary(item.priceCoins() + " ink", "buy-" + item.id(), e -> Api.shop().buy(
                new BuyDto(item.id()), Net.to(after -> Ui.later(() -> reload(body)), Ui::failed)));
        b.setEnabled(w.inkCoins() >= item.priceCoins());
        return b;
    }
}
