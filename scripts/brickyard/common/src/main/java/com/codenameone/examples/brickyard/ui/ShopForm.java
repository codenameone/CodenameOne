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
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.brickyard.api.CatalogItemDto;
import com.codenameone.examples.brickyard.api.OwnershipDto;
import com.codenameone.examples.brickyard.api.ReceiptDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codenameone.examples.brickyard.AppConfig;
import com.codenameone.examples.brickyard.engage.Ads;
import com.codenameone.examples.brickyard.engage.Store;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        if (Store.available() || CN.isSimulator()) {
            Container packs = new Container(new GridLayout(1, 2));
            packs.add(pack(AppConfig.SKU_BRICKS_400, "400", "Bricks", 400, "$4.99"));
            packs.add(pack(AppConfig.SKU_BRICKS_1000, "1,100", "Bricks  -  10% bonus", 1000, "$9.99"));
            content.add(packs);
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

        content.add(Ui.label("Avatar shop", "RowTitle"));
        final Container items = new Container(new GridLayout(2));
        items.setName("catalog");
        content.add(items);
        catalog(items, f);

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

    static Container pack(final String sku, String amount, String what, final int bricks, String price) {
        Label n = Ui.label(amount, "PackAmount");
        FontImage.setMaterialIcon(n, FontImage.MATERIAL_VIEW_MODULE, 5f);
        Container c = BoxLayout.encloseY(n, Ui.label(what, "CardMeta"),
                Ui.primary(price, "buy" + bricks, e -> {
                    if (!SettingsForm.allowSpend(bricks)) {
                        Ui.toast("That would pass the spending limit a parent set in Settings.");
                        return;
                    }
                    SettingsForm.recordSpend(bricks);
                    if (Store.available()) {
                        Store.buy(sku);
                    } else {
                        simulate(sku);
                    }
                }));
        c.setUIID("PackCard");
        return c;
    }

    /// The simulator has no app store, so a purchase there sends a receipt
    /// marked as simulated. Only a development server accepts one (see the
    /// backend's `Receipts`): production refuses it, so this grants nothing
    /// outside a local run.
    static void simulate(String sku) {
        Api.economy().redeemReceipt(new ReceiptDto("simulator", sku, "sim-" + System.currentTimeMillis(), "simulated"),
                Net.to(wallet -> {
                    Ui.toast("Purchase complete (simulated)");
                    Store.refresh();
                }, Ui::failed));
    }

    /// The avatar items on sale, each with its price or "Owned".
    static void catalog(final Container items, final Form f) {
        Api.economy().inventory(Net.to(owned -> Api.economy().catalog(Net.to(list -> {
            items.removeAll();
            Set<String> mine = new HashSet<String>();
            if (owned != null) {
                for (OwnershipDto o : owned) {
                    if (o.count() > 0) {
                        mine.add(o.productId());
                    }
                }
            }
            fillCatalog(items, f, list, mine);
            f.revalidate();
        }, Ui::failed)), Ui::failed));
    }

    private static void fillCatalog(final Container items, final Form f, List<CatalogItemDto> list, Set<String> mine) {
        if (list == null || list.isEmpty()) {
            items.add(Ui.label("Nothing on sale right now.", "Muted"));
            return;
        }
        for (final CatalogItemDto item : list) {
            Label icon = new Label("", "ItemIcon");
            FontImage.setMaterialIcon(icon, slotIcon(item.slot()), 9f);
            Component action;
            if (mine.contains(item.id())) {
                Label l = Ui.label("Owned", "Muted");
                FontImage.setMaterialIcon(l, FontImage.MATERIAL_CHECK_CIRCLE, 3f);
                action = l;
            } else {
                action = Ui.button(item.price() + " Bricks", "ClaimButton", "item-" + item.id(),
                        e -> Api.economy().buyItem(item.id(), Net.to(o -> {
                            Ui.toast("Bought " + item.name());
                            Store.refresh();
                            catalog(items, f);
                        }, Ui::failed)));
            }
            Container card = new Container(new BorderLayout());
            card.setUIID("ItemCard");
            card.add(BorderLayout.NORTH, icon);
            card.add(BorderLayout.CENTER, BoxLayout.encloseY(Ui.label(item.name(), "CardTitle"),
                    Ui.label(item.slot() == null ? "" : item.slot(), "CardMeta")));
            card.add(BorderLayout.SOUTH, action);
            items.add(card);
        }
    }

    /// The glyph for a catalog slot.
    static char slotIcon(String slot) {
        if ("HAT".equalsIgnoreCase(slot)) {
            return FontImage.MATERIAL_SCHOOL;
        }
        if ("FACE".equalsIgnoreCase(slot)) {
            return FontImage.MATERIAL_FACE;
        }
        if ("SHIRT".equalsIgnoreCase(slot)) {
            return FontImage.MATERIAL_CHECKROOM;
        }
        if ("EMOTE".equalsIgnoreCase(slot)) {
            return FontImage.MATERIAL_EMOJI_PEOPLE;
        }
        return FontImage.MATERIAL_STAR;
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
