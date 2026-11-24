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
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Sections;
import com.codenameone.examples.bazaar.ui.Ui;

/// The order went through: its number, a check that fades in, a short burst
/// of confetti, the points it earns and any badge it unlocked, and the ways
/// on -- to the order (followed there to the door), or back to the store.
final class OrderPlaced {
    private OrderPlaced() {
    }

    static void show(final OrderDto order, final Form home) {
        Form form = Ui.form(Lang.tr("Thank you"), "OrderPlaced");
        form.getToolbar().setBackCommand("", e -> done(home, null));
        Container page = Ui.page();
        page.setName("order-placed");
        Label check = new Label("", "ShopPlacedIcon");
        FontImage.setMaterialIcon(check, FontImage.MATERIAL_CHECK_CIRCLE, 16f);
        page.add(FlowLayout.encloseCenter(check));
        page.add(FlowLayout.encloseCenter(Ui.label("Order placed", "SvTitle")));
        Label number = Ui.plain(Lang.tr("Order #{0}", String.valueOf(order.number())),
                "ShopOrderNumber");
        number.setName("order-number");
        page.add(FlowLayout.encloseCenter(number));
        page.add(Ui.text(Lang.tr("We sent a receipt to {0}. You will hear from us when it ships.",
                order.email() == null ? "" : order.email()), "ShopCentered"));
        page.add(Ui.fact(Lang.tr("Total"), ShopState.money(order.totalCents()), "SvTotal"));
        page.add(RewardsView.placed(order));
        final Container offer = new Container(BoxLayout.y());
        offer.setName("order-upsell");
        page.add(offer);
        upsell(offer, order);
        page.add(Ui.primary("View order", "view-order", e -> done(home, order.id())));
        page.add(Ui.secondary("Keep shopping", "keep-shopping", e -> done(home, null)));
        form.add(BorderLayout.CENTER, page);
        form.show();
        // Fades in after the slide: quiet, and over within half a second.
        page.setVisible(true);
        page.animateHierarchyFade(450, 0);
        // The celebration starts as the page settles.
        CN.setTimeout(350, () -> {
            if (CN.getCurrentForm() == form) {
                ShopFx.confetti(form);
            }
        });
    }

    /// The store's post-purchase offer, when it has one for this order: one
    /// tap places it as a separate order on the same card.
    private static void upsell(final Container offer, final OrderDto order) {
        final String storeId = ShopState.store().id();
        Api.upsells().offer(storeId, order.id(), Net.to(u -> {
            if (u == null || u.productId() == null || u.productId().length() == 0) {
                return;
            }
            offer.add(Ui.label("Add to your order?", "SvHeading"));
            offer.add(Ui.fact(u.title(), ShopState.money(u.priceCents()), "SvLabel"));
            offer.add(Ui.text(Lang.tr("Charged to the same card as a separate order, shipped "
                    + "to the same address."), "SvMuted"));
            final Button add = Ui.secondary("Add to my order", "upsell-add", e -> { });
            add.addActionListener(e -> {
                add.setEnabled(false);
                Api.upsells().accept(storeId, order.id(), Net.to(o -> {
                    offer.removeAll();
                    offer.add(Ui.text(Lang.tr("Added as order #{0}",
                            String.valueOf(o.number())), "ShopCentered"));
                    Ui.relayout(offer);
                }, (status, message) -> {
                    add.setEnabled(true);
                    Ui.fail(status, message);
                }));
            });
            offer.add(add);
            Ui.relayout(offer);
        }, (status, message) -> {
            // No offer is the quiet answer: the order went through.
        }));
    }

    private static void done(Form home, String orderId) {
        if (orderId != null) {
            OrdersPane.open = orderId;
            ShopSections.show(Sections.ORDERS);
        } else {
            ShopSections.show(Sections.HOME);
        }
    }
}
