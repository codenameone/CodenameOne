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
package com.codenameone.examples.savor.merchant;

import com.codename1.components.Switch;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.savor.api.OrderDto;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.api.RestaurantDto;
import com.codenameone.examples.savor.live.LiveChannel;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Layouts;
import com.codenameone.examples.savor.ui.Menu;
import com.codenameone.examples.savor.ui.Nav;
import com.codenameone.examples.savor.ui.Ui;

import java.util.List;

/// The restaurant's side of the app: the orders coming in, the menu they are
/// ordered from, and whether the kitchen is taking any.
///
/// Which restaurant is the server's to say. An account is a merchant because it
/// is on a restaurant's staff, and every call this screen makes is answered for
/// that restaurant alone.
///
/// On a tablet or a desktop the queue stays down the left and the order picked
/// in it fills the rest; on a phone the order opens over the queue.
public class MerchantForm extends LiveChannel.Adapter implements Nav.Home {
    private static final Net.Failed QUIET = (status, message) -> { };
    /// How often the queue is read while the live channel is down.
    private static final int POLL_MILLIS = 8000;
    private static final float QUEUE_MM = 92f;

    private final Form form = Ui.home("Restaurant");
    private final Menu menu = new Menu(form, Nav.MERCHANT);
    private final Container body = new Container(new BorderLayout());
    private final Label title = Ui.label("Your restaurant", "SvHeading");
    private final Switch open = new Switch();
    private RestaurantDto restaurant;
    private Container queue;
    private Container detail;
    private String picked;
    private int waiting = -1;
    private String place = "orders";

    public MerchantForm() {
        menu.item(FontImage.MATERIAL_RECEIPT_LONG, "Orders", "home", () -> go("orders"));
        menu.item(FontImage.MATERIAL_RESTAURANT_MENU, "Menu", "menu-editor", () -> go("menu"));
        menu.item(FontImage.MATERIAL_INSIGHTS, "Sales", "sales", () -> go("sales"));
        menu.item(FontImage.MATERIAL_STOREFRONT, "Store details", "store",
                () -> StoreForm.show(form, restaurant, saved -> shown(saved)));
        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvHomeBar");
        bar.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(menu.flatButton()));
        bar.add(BorderLayout.CENTER, FlowLayout.encloseLeftMiddle(title));
        open.setName("accepting");
        open.addActionListener(e -> pause(!open.isValue()));
        Container accepting = new Container(BoxLayout.x());
        accepting.add(Ui.plain(Lang.tr("Taking orders"), "SvMutedSmall")).add(open);
        bar.add(BorderLayout.EAST, FlowLayout.encloseRightMiddle(accepting));
        Container screen = new Container(new BorderLayout());
        screen.setUIID("SvForm");
        screen.setSafeArea(true);
        screen.add(BorderLayout.NORTH, bar);
        screen.add(BorderLayout.CENTER, body);
        form.add(screen);
        menu.install();
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            reload();
        });
        Layouts.onChange(form, () -> go(place));
        UITimer.timer(POLL_MILLIS, true, form, () -> {
            if (!LiveChannel.connected() && "orders".equals(place)) {
                loadQueue();
            }
        });
    }

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        go("orders");
    }

    private void reload() {
        Api.merchant().restaurant(Net.to(this::shown, QUIET));
        if ("orders".equals(place)) {
            loadQueue();
        }
    }

    private void shown(RestaurantDto found) {
        restaurant = found;
        title.setText(found.name());
        open.setValue(!found.paused());
        title.getParent().revalidateLater();
    }

    private void pause(final boolean paused) {
        Api.merchant().pause(paused, Net.to(saved -> {
            shown(saved);
            Ui.say(paused ? "New orders are paused" : "Taking orders again");
        }, (status, message) -> {
            // Back to what it was: the switch says whether orders are taken.
            open.setValue(paused);
            Ui.fail(status, message);
        }));
    }

    private void go(String where) {
        place = where;
        body.removeAll();
        queue = null;
        detail = null;
        if ("menu".equals(where)) {
            body.add(BorderLayout.CENTER, new MenuEditor(form).page());
        } else if ("sales".equals(where)) {
            body.add(BorderLayout.CENTER, SalesPage.page());
        } else {
            queue = Ui.list("queue");
            Ui.pull(queue, this::loadQueue);
            if (Layouts.wide()) {
                Container left = new Container(new BorderLayout()) {
                    @Override
                    protected com.codename1.ui.geom.Dimension calcPreferredSize() {
                        return new com.codename1.ui.geom.Dimension(CN.convertToPixels(QUEUE_MM),
                                super.calcPreferredSize().getHeight());
                    }
                };
                left.setUIID("SvQueuePane");
                left.add(BorderLayout.CENTER, queue);
                detail = new Container(new BorderLayout());
                detail.setUIID("SvDetailPane");
                detail.add(BorderLayout.CENTER, Ui.empty(FontImage.MATERIAL_RECEIPT_LONG,
                        Lang.tr("Pick an order"), null));
                body.add(BorderLayout.WEST, left);
                body.add(BorderLayout.CENTER, detail);
            } else {
                body.add(BorderLayout.CENTER, queue);
            }
            loadQueue();
        }
        form.revalidate();
    }

    private void loadQueue() {
        final Container into = queue;
        if (into == null) {
            return;
        }
        Api.merchant().orders(Net.to(found -> fill(into, found),
                Ui.failed(into, this::loadQueue)));
    }

    private void fill(Container into, List<OrderDto> found) {
        int placed = 0;
        into.removeAll();
        if (found.isEmpty()) {
            into.add(Ui.empty(FontImage.MATERIAL_STOREFRONT, Lang.tr("No orders right now"),
                    Lang.tr("New orders appear here as customers place them.")));
        }
        OrderDto again = null;
        for (final OrderDto order : found) {
            if (order.state() == OrderState.PLACED) {
                placed++;
            }
            if (order.id().equals(picked)) {
                again = order;
            }
            into.add(row(order));
        }
        // A new order is the one thing on this screen worth interrupting
        // the kitchen for.
        if (waiting >= 0 && placed > waiting) {
            Display.getInstance().vibrate(400);
            Ui.say("New order");
        }
        waiting = placed;
        into.revalidateLater();
        if (detail != null) {
            if (again != null) {
                open(again);
            } else if (picked != null) {
                picked = null;
                detail.removeAll();
                detail.add(BorderLayout.CENTER, Ui.empty(FontImage.MATERIAL_RECEIPT_LONG,
                        Lang.tr("Pick an order"), null));
                detail.revalidateLater();
            }
        }
    }

    private Component row(final OrderDto order) {
        Container row = new Container(new BorderLayout());
        row.setUIID(order.id().equals(picked) ? "SvQueueRowPicked" : "SvQueueRow");
        Container words = new Container(BoxLayout.y());
        words.add(Ui.plain(order.customerName() + Ui.DOT + "#" + Orders.shortId(order.id()),
                "SvRowTitle"));
        words.add(Ui.plain(Orders.summary(order), "SvMutedSmall"));
        words.add(Ui.plain(Ui.when(order.placedAt()), "SvMutedSmall"));
        row.add(BorderLayout.CENTER, words);
        row.add(BorderLayout.EAST, Ui.badge(Orders.state(order.state()),
                Orders.badge(order.state())));
        return Ui.tap(row, "queue-" + order.id(), e -> {
            picked = order.id();
            if (detail == null) {
                OrderPane.show(form, order, this::loadQueue);
            } else {
                open(order);
                // Read again, which also marks the picked row.
                loadQueue();
            }
        });
    }

    private void open(OrderDto order) {
        detail.removeAll();
        detail.add(BorderLayout.CENTER, OrderPane.page(order, changed -> loadQueue()));
        detail.revalidateLater();
    }

    @Override
    public void orderChanged(String id, String state) {
        if (CN.getCurrentForm() == form && "orders".equals(place)) {
            loadQueue();
        }
    }
}
