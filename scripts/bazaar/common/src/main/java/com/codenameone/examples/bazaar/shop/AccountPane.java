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
package com.codenameone.examples.bazaar.shop;

import com.codename1.appkit.Net;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.GiftCardBalanceDto;
import com.codenameone.examples.bazaar.api.UserDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.net.Session;
import com.codenameone.examples.bazaar.pay.WalletForm;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Nav;
import com.codenameone.examples.bazaar.ui.ProfileForm;
import com.codenameone.examples.bazaar.ui.SettingsForm;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The shopper's account: who they are, their saved addresses, their
/// wishlist, the wallet, a gift card's balance, Settings and Sign out.
final class AccountPane extends ShopPane {
    private final Container page = Ui.page();
    private final Container wishes = new Container(BoxLayout.y());

    AccountPane() {
        super(new BorderLayout());
        setName("shop-account");
        add(BorderLayout.CENTER, page);
        reload();
    }

    @Override
    void reload() {
        page.removeAll();
        UserDto user = Session.user();
        if (user != null) {
            page.add(Ui.person(user.displayName(), user.username(), null));
        }
        Container buttons = new Container(new GridLayout(1, 2));
        buttons.add(Ui.secondary("Edit profile", "profile",
                e -> ProfileForm.show(CN.getCurrentForm(), this::reload)));
        buttons.add(Ui.secondary("Wallet", "wallet", e -> WalletForm.show(CN.getCurrentForm())));
        page.add(buttons);

        addresses();
        Ui.section(page, "Wishlist");
        page.add(wishes);
        wishlist();
        giftCard();

        Ui.section(page, "App");
        page.add(Ui.tap(Ui.row(FontImage.MATERIAL_SETTINGS, "Settings", null, null),
                "account-settings", e -> SettingsForm.show(CN.getCurrentForm())));
        page.add(Ui.tap(Ui.row(FontImage.MATERIAL_LOGOUT, "Sign out", null, null),
                "account-signout", e -> Nav.signOut()));
        Ui.relayout(page);
    }

    private void addresses() {
        Ui.section(page, "Saved addresses");
        List<AddressDto> saved = ShopState.addresses();
        if (saved.isEmpty()) {
            page.add(Ui.text("Addresses you ship to are saved here at checkout.", "SvMuted"));
            return;
        }
        for (final AddressDto a : saved) {
            Button forget = new Button("", "ShopIconButton");
            forget.setName("address-forget");
            FontImage.setMaterialIcon(forget, FontImage.MATERIAL_DELETE, 3f);
            forget.addActionListener(e -> {
                ShopState.forgetAddress(a);
                reload();
            });
            page.add(Ui.row(FontImage.MATERIAL_LOCATION_ON, a.name(), Addresses.summary(a), forget));
        }
    }

    private void wishlist() {
        wishes.removeAll();
        List<String> ids = ShopState.wishlist();
        if (ids.isEmpty()) {
            wishes.add(Ui.text("Tap the heart on a product to save it for later.", "SvMuted"));
            return;
        }
        int columns = Layouts.wide() ? 4 : 2;
        final Container grid = new Container(new GridLayout(Math.max(1, (ids.size() + columns - 1)
                / columns), columns));
        wishes.add(grid);
        final int width = Math.min(CN.getDisplayWidth(), Layouts.column()) / columns;
        for (int iter = 0; iter < ids.size() && iter < 24; iter++) {
            final String id = ids.get(iter);
            final Container slot = new Container(new BorderLayout());
            slot.add(BorderLayout.CENTER, ProductCard.placeholder(width));
            grid.add(slot);
            Api.catalog().product(id, Net.to(p -> {
                long low = Long.MAX_VALUE;
                long high = 0;
                long compare = 0;
                int available = 0;
                for (int v = 0; p.variants() != null && v < p.variants().size(); v++) {
                    long price = p.variants().get(v).priceCents();
                    low = Math.min(low, price);
                    high = Math.max(high, price);
                    compare = Math.max(compare, p.variants().get(v).compareAtCents());
                    available += Math.max(0, p.variants().get(v).available());
                }
                if (low == Long.MAX_VALUE) {
                    low = 0;
                }
                slot.removeAll();
                slot.add(BorderLayout.CENTER, ProductCard.create(
                        new com.codenameone.examples.bazaar.api.ProductCardDto(p.id(), p.storeId(),
                                p.handle(), p.title(), p.vendor(), p.productType(), p.tags(),
                                p.status(), p.photoId(), low, high, compare, available, p.rating(),
                                p.ratingCount(), p.variants() == null ? 0 : p.variants().size()),
                        width));
                Ui.relayout(slot);
            }, (status, message) -> {
                // Gone from the store: off the list.
                if (status == 404) {
                    ShopState.toggleWish(id);
                    grid.removeComponent(slot);
                    Ui.relayout(grid);
                }
            }));
        }
    }

    private void giftCard() {
        Ui.section(page, "Gift card balance");
        Container row = new Container(new BorderLayout());
        row.setUIID("ShopCodeRow");
        final TextField code = new TextField("", Lang.tr("Gift card code"), 16, TextField.ANY);
        code.setUIID("SvField");
        code.setName("giftcard-check-code");
        final Label answer = new Label("", "SvMuted");
        answer.setName("giftcard-balance");
        final Runnable check = () -> {
            String c = code.getText().trim();
            if (c.length() == 0) {
                return;
            }
            Api.giftCards().balance(c, Net.to((GiftCardBalanceDto b) -> {
                answer.setText(b.usable()
                        ? Lang.tr("{0} left on the card ending {1}",
                                Money.format(b.balanceCents(), b.currency(), "USD"), b.lastFour())
                        : Lang.tr("The card ending {0} cannot be used", b.lastFour()));
                answer.setUIID(b.usable() ? "ShopStock" : "ShopStockOut");
                Ui.relayout(page);
            }, Ui::fail));
        };
        code.setDoneListener(e -> check.run());
        row.add(BorderLayout.CENTER, code);
        row.add(BorderLayout.EAST, Ui.secondary("Check", "giftcard-check", e -> check.run()));
        page.add(row);
        page.add(answer);
    }
}
