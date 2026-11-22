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
import com.codename1.appkit.Skeleton;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.GiftCardBalanceDto;
import com.codenameone.examples.bazaar.api.ReturnDto;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.api.UserDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.net.Session;
import com.codenameone.examples.bazaar.pay.WalletForm;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Nav;
import com.codenameone.examples.bazaar.ui.ProfileForm;
import com.codenameone.examples.bazaar.ui.Sections;
import com.codenameone.examples.bazaar.ui.SettingsForm;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The shopper's account: who they are, their saved addresses, their
/// wishlist, the stores they follow, the returns they asked for, the wallet,
/// a gift card's balance, Settings and Sign out. On a wide screen the
/// sections stand in two columns.
final class AccountPane extends ShopPane {
    private final Container page = Ui.page();
    private final Container wishes = new Container(BoxLayout.y());
    private final Container followed = new Container(BoxLayout.y());
    private final Container returned = new Container(BoxLayout.y());
    private final boolean split = Layouts.wide();

    AccountPane() {
        super(new BorderLayout());
        setName("shop-account");
        add(BorderLayout.CENTER, page);
        reload();
        // The address book and wishlist come from the server; show them once
        // they arrive.
        ShopState.syncAccount(() -> {
            if (getComponentForm() != null || isInitialized()) {
                reload();
            }
        });
    }

    @Override
    void reload() {
        page.removeAll();
        UserDto user = Session.user();
        if (user != null) {
            page.add(Ui.person(user.displayName(), user.username(), null));
        }
        Container buttons = new Container(new GridLayout(1, 2));
        Button edit = Ui.secondary("Edit profile", "profile",
                e -> ProfileForm.show(CN.getCurrentForm(), this::reload));
        edit.setUIID("SvPairStart");
        Button wallet = Ui.secondary("Wallet", "wallet", e -> WalletForm.show(CN.getCurrentForm()));
        wallet.setUIID("SvPairEnd");
        buttons.add(edit).add(wallet);
        page.add(buttons);

        Container main = page;
        Container side = page;
        if (split) {
            Container columns = new Container(new GridLayout(1, 2));
            main = new Container(BoxLayout.y());
            main.setUIID("ShopColumnStart");
            side = new Container(BoxLayout.y());
            side.setUIID("ShopColumnEnd");
            columns.add(main).add(side);
            page.add(columns);
        }
        // The kept lists may still sit in the columns of the last build,
        // which removeAll above does not reach.
        wishes.remove();
        followed.remove();
        returned.remove();
        addresses(main);
        Ui.section(main, "Wishlist");
        main.add(wishes);
        wishlist();
        Ui.section(side, "Stores you follow");
        side.add(followed);
        follows();
        Ui.section(side, "Returns");
        side.add(returned);
        returns();
        giftCard(side);

        Ui.section(side, "App");
        side.add(Ui.tap(Ui.row(FontImage.MATERIAL_SETTINGS, "Settings", null, null),
                "account-settings", e -> SettingsForm.show(CN.getCurrentForm())));
        side.add(Ui.tap(Ui.row(FontImage.MATERIAL_LOGOUT, "Sign out", null, null),
                "account-signout", e -> Nav.signOut()));
        Ui.relayout(page);
    }

    private void addresses(Container into) {
        Ui.section(into, "Saved addresses");
        List<AddressDto> saved = ShopState.addresses();
        if (saved.isEmpty()) {
            into.add(Ui.text("Addresses you ship to are saved here at checkout.", "SvMuted"));
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
            into.add(Ui.row(FontImage.MATERIAL_LOCATION_ON, a.name(), Addresses.summary(a), forget));
        }
    }

    private void wishlist() {
        wishes.removeAll();
        List<String> ids = ShopState.wishlist();
        if (ids.isEmpty()) {
            wishes.add(Ui.text("Tap the heart on a product to save it for later.", "SvMuted"));
            return;
        }
        // Two across: a phone's width, or half the column beside the others.
        int columns = 2;
        final Container grid = new Container(new GridLayout(Math.max(1, (ids.size() + columns - 1)
                / columns), columns));
        wishes.add(grid);
        final int width = Math.min(CN.getDisplayWidth(), Layouts.column()) / (split ? 2 : 1)
                / columns;
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

    /// The stores the shopper follows, newest first: a tap opens one, and
    /// Unfollow drops it from the list there and then.
    private void follows() {
        followed.removeAll();
        followed.add(new Skeleton(1, "SvSkeleton"));
        ShopState.loadFollows(this::fillFollows);
    }

    private void fillFollows() {
        followed.removeAll();
        List<String> ids = ShopState.followed();
        for (int iter = 0; iter < ids.size(); iter++) {
            final StoreDto s = ShopState.known(ids.get(iter));
            if (s == null) {
                continue;
            }
            Container row = Ui.row(FontImage.MATERIAL_STOREFRONT, s.name(), s.tagline(), null);
            row.setUIID("Container");
            Container whole = new Container(new BorderLayout());
            whole.setUIID("SvRow");
            // Outside the row's tap: a lead component takes every press in
            // its container.
            whole.add(BorderLayout.CENTER, Ui.tap(row, "account-store-" + s.handle(), e -> {
                ShopState.useStore(s);
                ShopState.setInStore(true);
                ShopSections.show(Sections.HOME);
            }));
            Button drop = Ui.link("Unfollow", "account-unfollow-" + s.handle(),
                    e -> ShopState.toggleFollow(s, this::fillFollows));
            whole.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(drop));
            followed.add(whole);
        }
        if (followed.getComponentCount() == 0) {
            followed.add(Ui.text("Follow a store to see its new products first.", "SvMuted"));
        }
        Ui.relayout(page);
    }

    /// The returns the shopper asked for, newest first, each with how far it
    /// got; a tap opens its order.
    private void returns() {
        returned.removeAll();
        returned.add(new Skeleton(1, "SvSkeleton"));
        Api.returns().requested(Net.to(list -> {
            returned.removeAll();
            for (int iter = 0; list != null && iter < list.size(); iter++) {
                returned.add(returnRow(list.get(iter)));
            }
            if (returned.getComponentCount() == 0) {
                returned.add(Ui.text("Returns you start from a delivered order show here.",
                        "SvMuted"));
            }
            Ui.relayout(page);
        }, (status, message) -> {
            returned.removeAll();
            returned.add(Ui.text(message, "SvError"));
            Ui.relayout(page);
        }));
    }

    private static Component returnRow(final ReturnDto r) {
        StoreDto store = ShopState.known(r.storeId());
        String when = Ui.day(r.createdAt());
        String detail = store == null ? when : store.name() + " \u00b7 " + when;
        if (r.refundedCents() > 0) {
            detail = detail + " \u00b7 " + Lang.tr("{0} refunded",
                    ShopState.money(r.storeId(), r.refundedCents()));
        }
        Label status = Ui.badge(Lang.tr(returnStatus(r.status())), returnBadge(r.status()));
        Container row = Ui.row(FontImage.MATERIAL_ASSIGNMENT_RETURN,
                Lang.tr("Order #{0}", String.valueOf(r.orderNumber())), detail, status);
        return Ui.tap(row, "account-return-" + r.id(), e -> {
            OrdersPane.open = r.orderId();
            ShopSections.show(Sections.ORDERS);
        });
    }

    /// The words for a return's status (`Returns` on the server).
    static String returnStatus(String code) {
        if ("APPROVED".equals(code)) {
            return "Approved";
        }
        if ("DECLINED".equals(code)) {
            return "Declined";
        }
        if ("RECEIVED".equals(code)) {
            return "Received";
        }
        if ("REFUNDED".equals(code)) {
            return "Refunded";
        }
        return "Requested";
    }

    /// Waiting on the store is a warning; the money back, good news.
    private static String returnBadge(String code) {
        if ("DECLINED".equals(code)) {
            return "SvBadgeBad";
        }
        if ("REQUESTED".equals(code)) {
            return "SvBadgeWarn";
        }
        return "SvBadgeGood";
    }

    private void giftCard(Container into) {
        Ui.section(into, "Gift card balance");
        Container row = new Container(new BorderLayout());
        row.setUIID("ShopCodeRow");
        final TextField code = new TextField("", Lang.tr("Gift card code"), 16, TextField.ANY);
        code.setUIID("SvField");
        code.getHintLabel().setUIID("SvFieldHint");
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
        Button go = Ui.secondary("Check", "giftcard-check", e -> check.run());
        go.setUIID("SvFieldButton");
        row.add(BorderLayout.EAST, go);
        into.add(row);
        into.add(answer);
    }
}
