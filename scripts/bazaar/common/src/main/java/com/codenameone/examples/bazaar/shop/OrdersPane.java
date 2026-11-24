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
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import com.codenameone.examples.bazaar.ui.Windows;
import java.util.List;

/// The shopper's orders, newest first. On a wide screen the list and the
/// order picked sit side by side; on a phone an order opens as a screen.
/// An order that changes on the server (shipped, cancelled by the store)
/// refreshes the list and the order on screen, heard over the live channel.
final class OrdersPane extends ShopPane {
    private static final float LIST_MM = 84f;

    /// An order to open when the pane is next built: the one just placed.
    static String open;

    /// The tracking stage each order was last seen at, so that a change heard
    /// over the live channel animates from there and is announced once.
    /// The order last drawn on screen, so an identical answer is not drawn again.
    private OrderDto onScreen;

    private static final java.util.Map<String, Integer> SEEN =
            new java.util.HashMap<String, Integer>();

    private final boolean wide = Layouts.split();
    private final Container list = Ui.list("orders");
    private final Container detail = new Container(new BorderLayout());
    private String shown;
    private Form pushed;

    OrdersPane() {
        super(new BorderLayout());
        setName("shop-orders");
        Ui.pull(list, this::reload);
        if (wide) {
            Container left = new Container(new BorderLayout()) {
                @Override
                protected Dimension calcPreferredSize() {
                    Dimension d = super.calcPreferredSize();
                    d.setWidth(CN.convertToPixels(LIST_MM));
                    return d;
                }
            };
            left.setUIID("SvMaster");
            left.add(BorderLayout.CENTER, list);
            add(BorderLayout.WEST, left);
            detail.add(BorderLayout.CENTER, Ui.empty(FontImage.MATERIAL_RECEIPT_LONG,
                    "Pick an order", "Its details show here."));
            add(BorderLayout.CENTER, detail);
        } else {
            add(BorderLayout.CENTER, list);
        }
        reload();
    }

    @Override
    void reload() {
        Api.orders().mine(Net.to(this::fill, Ui.failed(list, this::reload)));
    }

    @Override
    void storeChanged() {
        // Orders are the shopper's across every store.
    }

    @Override
    void orderChanged(String id, String status) {
        reload();
        // On a phone only while that order's screen is still up: after Back
        // a live event must not push it again unasked.
        if (id != null && id.equals(shown) && (wide || CN.getCurrentForm() == pushed)) {
            show(id);
        }
    }

    private void fill(List<OrderDto> orders) {
        list.removeAll();
        if (orders.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_RECEIPT_LONG, "No orders yet",
                    "Orders you place show up here, with where they are."));
        }
        for (final OrderDto o : orders) {
            announce(o);
            Container row = new Container(new BorderLayout());
            row.setUIID(o.id().equals(shown) && wide ? "SvRowPicked" : "SvOrderRow");
            Container words = new Container(BoxLayout.y());
            words.add(Ui.plain(Lang.tr("Order #{0}", String.valueOf(o.number())), "SvRowTitle"));
            int items = 0;
            for (int iter = 0; o.lines() != null && iter < o.lines().size(); iter++) {
                items += o.lines().get(iter).quantity();
            }
            // The list comes without its lines (the detail has them): a count of
            // nothing read "0 items" on every order.
            words.add(Ui.plain(items == 0 ? Ui.day(o.createdAt()) : Ui.day(o.createdAt()) + Ui.DOT
                    + Lang.tr("{0} items", String.valueOf(items)), "SvRowDetail"));
            row.add(BorderLayout.CENTER, words);
            Container end = new Container(BoxLayout.y());
            end.add(Ui.plain(Money.format(o.totalCents(), o.currency(), "USD"), "SvRowValue"));
            end.add(Ui.badge(Lang.tr(TrackStages.chip(o)), OrderView.badge(o)));
            row.add(BorderLayout.EAST, end);
            Container tappable = Ui.tap(row, "order-" + o.number(), e -> show(o.id()));
            RowMenus.attach(tappable, () -> new Command[] {
                Command.create(Lang.tr("Open in new window"), null, e -> window(o.id()))
            });
            list.add(tappable);
        }
        Ui.relayout(list);
        if (open != null) {
            String id = open;
            open = null;
            show(id);
        }
    }

    /// Drops in the banner for an order that moved on while its screen is not
    /// the one up; the screen of the order on show announces its own.
    private void announce(OrderDto o) {
        int now = TrackStages.reached(o);
        Integer was = SEEN.get(o.id());
        if (was == null) {
            SEEN.put(o.id(), Integer.valueOf(now));
            return;
        }
        if (o.id().equals(shown)) {
            return;
        }
        SEEN.put(o.id(), Integer.valueOf(now));
        String banner = TrackStages.banner(was.intValue(), now);
        if (banner != null) {
            ShopFx.banner(CN.getCurrentForm(), FontImage.MATERIAL_LOCAL_SHIPPING, banner,
                    Lang.tr("Order #{0}", String.valueOf(o.number())));
        }
    }

    private void show(final String id) {
        shown = id;
        if (wide) {
            detail.removeAll();
            detail.add(BorderLayout.CENTER, new com.codenameone.examples.bazaar.ui.Shimmer(6, "SvSkeleton"));
            Ui.relayout(detail);
        }
        // A phone pushes a form for the answer; if the user has gone elsewhere
        // by the time it arrives, nothing must be pushed over where they are.
        final Form asking = CN.getCurrentForm();
        Api.orders().mineDetail(id, Net.to(order -> {
            if (!id.equals(shown)) {
                return;
            }
            // One change on the server can be heard more than once (the order
            // and its parcel); the same answer again must not replace the
            // screen while it animates the step.
            if (order.equals(onScreen) && (wide || CN.getCurrentForm() == pushed)) {
                return;
            }
            onScreen = order;
            int now = TrackStages.reached(order);
            Integer was = SEEN.get(order.id());
            if (was == null) {
                SEEN.put(order.id(), Integer.valueOf(now));
            } else if (was.intValue() != now) {
                // Recorded once the step has played, so a second answer for
                // the same change, drawn over the first, still animates it.
                final String key = order.id();
                final Integer seen = Integer.valueOf(now);
                CN.setTimeout(900, () -> SEEN.put(key, seen));
            }
            Container view = OrderView.create(order, wide, was == null ? now : was.intValue(),
                    changed -> {
                reload();
                show(changed.id());
            });
            if (wide) {
                detail.removeAll();
                detail.add(BorderLayout.CENTER, view);
                Ui.relayout(detail);
            } else if (pushed != null && CN.getCurrentForm() == pushed) {
                pushed.removeAll();
                pushed.add(BorderLayout.CENTER, view);
                Ui.refresh(pushed);
            } else if (CN.getCurrentForm() == asking) {
                final Form home = asking;
                pushed = Ui.form(Lang.tr("Order #{0}", String.valueOf(order.number())), "Order");
                Ui.back(pushed, () -> {
                    shown = null;
                    home.showBack();
                });
                pushed.addKeyListener(27, e -> {
                    shown = null;
                    home.showBack();
                });
                pushed.add(BorderLayout.CENTER, view);
                pushed.show();
            } else {
                shown = null;
            }
        }, Ui::fail));
    }

    private void window(final String id) {
        Api.orders().mineDetail(id, Net.to(order -> {
            Container view = OrderView.create(order, false, changed -> reload());
            if (Windows.open(Lang.tr("Order #{0}", String.valueOf(order.number())), view,
                    120f, 150f) == null) {
                show(id);
            }
        }, Ui::fail));
    }
}
