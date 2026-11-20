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
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.FulfillmentSummaryDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderEventDto;
import com.codenameone.examples.bazaar.api.OrderLineDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.ProductArt;
import com.codenameone.examples.bazaar.ui.Ui;

/// One of the shopper's orders: where it is (a timeline), what is in it,
/// what it cost, where it goes and its parcels, and Cancel while nothing has
/// shipped.
final class OrderView {
    interface Changed {
        void changed(OrderDto order);
    }

    private OrderView() {
    }

    static Container create(final OrderDto order, final Changed changed) {
        Container page = Ui.page();
        page.setName("order-" + order.number());
        Container head = new Container(new BorderLayout());
        head.add(BorderLayout.CENTER, Ui.label(Lang.tr("Order #{0}", String.valueOf(order.number())),
                "SvTitle"));
        head.add(BorderLayout.EAST, Ui.badge(Lang.tr(OrderSteps.label(order)), badge(order)));
        page.add(head);
        page.add(Ui.plain(Ui.when(order.createdAt()), "SvMuted"));
        page.add(timeline(order));

        Ui.section(page, "Items");
        for (OrderLineDto line : order.lines()) {
            page.add(line(line));
        }

        Ui.section(page, "Total");
        page.add(Ui.fact(Lang.tr("Subtotal"), ShopState.money(order.subtotalCents())));
        if (order.discountCents() > 0) {
            page.add(Ui.fact(order.discountCode() == null ? Lang.tr("Discount")
                    : Lang.tr("Discount") + " (" + order.discountCode() + ")",
                    ShopState.money(-order.discountCents()), "SvDiscount"));
        }
        page.add(Ui.fact(order.shippingTitle() == null ? Lang.tr("Shipping") : order.shippingTitle(),
                order.shippingCents() == 0 ? Lang.tr("Free") : ShopState.money(order.shippingCents())));
        page.add(Ui.fact(Lang.tr("Tax"), ShopState.money(order.taxCents())));
        page.add(Ui.fact(Lang.tr("Total"), ShopState.money(order.totalCents()), "SvTotal"));
        if (order.refundedCents() > 0) {
            page.add(Ui.fact(Lang.tr("Refunded"), ShopState.money(-order.refundedCents()),
                    "SvDiscount"));
        }

        AddressDto a = order.address();
        if (a != null) {
            Ui.section(page, "Ships to");
            page.add(Ui.text(Addresses.summary(a), "SvText"));
        }
        if (order.fulfillments() != null && !order.fulfillments().isEmpty()) {
            Ui.section(page, "Parcels");
            for (FulfillmentSummaryDto f : order.fulfillments()) {
                String detail = f.carrier() == null ? null
                        : f.carrier() + (f.trackingNumber() == null ? "" : Ui.DOT + f.trackingNumber());
                page.add(Ui.row(FontImage.MATERIAL_LOCAL_SHIPPING, Lang.tr(status(f.status())),
                        detail, null));
            }
        }
        if (order.events() != null && !order.events().isEmpty()) {
            Ui.section(page, "History");
            for (OrderEventDto e : order.events()) {
                page.add(Ui.row((char) 0, e.message(), Ui.when(e.createdAt()), null));
            }
        }

        Container actions = new Container(new GridLayout(1, 2));
        // The server renders the invoice; the app previews it, with Print,
        // Share and Save under it.
        Button invoice = Ui.secondary("Download invoice", "invoice", e ->
                com.codenameone.examples.bazaar.docs.Pdfs.open("Invoice", "invoice-"
                        + order.number() + ".pdf", "/api/orders/" + order.id() + "/invoice.pdf"));
        FontImage.setMaterialIcon(invoice, FontImage.MATERIAL_PICTURE_AS_PDF, 3.2f);
        actions.add(invoice);
        if (OrderSteps.cancellable(order)) {
            actions.add(Ui.danger("Cancel order", "cancel-order", e -> {
                if (Dialog.show(Lang.tr("Cancel order"),
                        Lang.tr("Cancel order #{0}? Your card is not charged.",
                                String.valueOf(order.number())),
                        Lang.tr("Cancel order"), Lang.tr("Keep it"))) {
                    Api.orders().cancelMine(order.id(), Net.to(o -> {
                        Ui.say("Order cancelled");
                        changed.changed(o);
                    }, Ui::fail));
                }
            }));
        }
        page.add(actions);
        return page;
    }

    private static Container timeline(OrderDto order) {
        int reached = OrderSteps.reached(order);
        Container row = new Container(new GridLayout(1, OrderSteps.STEPS.length));
        row.setUIID("SvSteps");
        row.setName("timeline");
        if (reached == 0) {
            Container c = new Container(BoxLayout.y());
            c.add(Ui.callout(FontImage.MATERIAL_CANCEL, "Cancelled",
                    Lang.tr("This order was cancelled.")));
            return c;
        }
        for (int iter = 0; iter < OrderSteps.STEPS.length; iter++) {
            boolean done = iter < reached;
            Container s = new Container(BoxLayout.y());
            s.setUIID(done ? "SvStepDone" : "SvStep");
            s.add(new Label("", done ? "SvStepBarDone" : "SvStepBar"));
            s.add(Ui.label(OrderSteps.STEPS[iter], done ? "SvStepText" : "SvStepTextTodo"));
            row.add(s);
        }
        return row;
    }

    private static Container line(OrderLineDto line) {
        Container row = new Container(new BorderLayout());
        row.setUIID("SvCartLine");
        int side = CN.convertToPixels(12);
        row.add(BorderLayout.WEST, ProductArt.label(line.productId(), line.title(), line.title(),
                line.photoId(), side, side, "ShopThumb"));
        Container words = new Container(BoxLayout.y());
        words.add(Ui.plain(line.title(), "SvRowTitle"));
        String detail = (line.variantTitle() == null || "Default".equalsIgnoreCase(line.variantTitle())
                ? "" : line.variantTitle() + Ui.DOT) + Lang.tr("Qty {0}", String.valueOf(line.quantity()));
        words.add(Ui.plain(detail, "SvRowDetail"));
        row.add(BorderLayout.CENTER, words);
        row.add(BorderLayout.EAST, Ui.plain(ShopState.money(line.priceCents() * line.quantity()),
                "SvRowValue"));
        return row;
    }

    static String badge(OrderDto order) {
        if ("CANCELLED".equals(order.status())) {
            return "SvBadgeBad";
        }
        int reached = OrderSteps.reached(order);
        return reached >= 4 ? "SvBadgeGood" : reached >= 3 ? "SvBadge" : "SvBadgeWarn";
    }

    private static String status(String s) {
        if (s == null) {
            return "";
        }
        switch (s) {
            case "PENDING":
                return "Preparing";
            case "PACKED":
                return "Packed";
            case "SHIPPED":
                return "Shipped";
            case "IN_TRANSIT":
                return "In transit";
            case "OUT_FOR_DELIVERY":
                return "Out for delivery";
            case "DELIVERED":
                return "Delivered";
            case "PICKED_UP":
                return "Picked up";
            case "CANCELLED":
                return "Cancelled";
            default:
                return s;
        }
    }
}
