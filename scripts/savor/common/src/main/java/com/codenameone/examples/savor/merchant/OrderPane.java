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

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.savor.api.AcceptDto;
import com.codenameone.examples.savor.api.DeliveryDto;
import com.codenameone.examples.savor.api.OrderDto;
import com.codenameone.examples.savor.api.OrderLineDto;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.api.ReasonDto;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Ui;

/// One order, for the kitchen: what is in it, and the one thing to do with
/// it next -- take it with a time to cook, turn it down, or say it is ready.
final class OrderPane {
    interface Changed {
        void changed(OrderDto order);
    }

    private static final String[] PREP = {"10", "15", "20", "30"};
    private static final String[] PREP_NAMES = {"10 min", "15 min", "20 min", "30 min"};

    private OrderPane() {
    }

    /// Opens `order` as a screen of its own over `previous`: the phone's way.
    static void show(final Form previous, OrderDto order, final Runnable changed) {
        final Form form = Ui.form("#" + Orders.shortId(order.id()), "merchant-order");
        Ui.back(form, previous);
        form.add(BorderLayout.CENTER, page(order, done -> {
            changed.run();
            previous.showBack();
        }));
        form.show();
    }

    /// The order as a page, for a screen or for the pane beside the queue.
    static Container page(final OrderDto order, final Changed changed) {
        final Container page = Ui.page();
        page.setName("merchant-order-page");
        page.add(Ui.plain("#" + Orders.shortId(order.id()) + Ui.DOT + order.customerName(),
                "SvRestaurantTitle"));
        // The state rides at the end of the line that says when the order
        // came in: a pill keeps the space before it that it has after words.
        Container placed = new Container(new BorderLayout());
        placed.add(BorderLayout.WEST, Ui.plain(Lang.tr("Placed {0}", Ui.when(order.placedAt())), "SvMutedSmall"));
        placed.add(BorderLayout.EAST, Ui.badge(Orders.state(order.state()), Orders.badge(order.state())));
        page.add(placed);
        Ui.section(page, Lang.tr("Items"));
        for (OrderLineDto line : order.lines()) {
            Container item = Ui.fact(line.quantity() + " x " + line.name(),
                    Ui.money(line.lineCents(), order.currency()));
            page.add(item);
            if (line.options() != null && line.options().length() > 0) {
                page.add(Ui.plain(line.options(), "SvMutedSmall"));
            }
        }
        page.add(Ui.fact(Lang.tr("Food"), Ui.money(order.subtotalCents(), order.currency()),
                "SvTotal"));
        if (order.note() != null && order.note().length() > 0) {
            page.add(Ui.callout(FontImage.MATERIAL_STICKY_NOTE_2, Lang.tr("Note"), order.note()));
        }
        DeliveryDto delivery = order.delivery();
        if (delivery != null && delivery.courierName() != null) {
            Ui.section(page, Lang.tr("Courier"));
            page.add(Ui.person(delivery.courierName(), delivery.courierUsername(),
                    Ui.plain(delivery.etaMinutes() > 0 && order.state() != OrderState.PICKED_UP
                            ? Lang.tr("Here in {0} min", String.valueOf(delivery.etaMinutes()))
                            : Ui.state(delivery.state()), "SvMutedSmall")));
        } else if (order.state() == OrderState.ACCEPTED || order.state() == OrderState.READY) {
            page.add(Ui.plain(Lang.tr("Finding a courier"), "SvMutedSmall"));
        }

        Net.Ok<OrderDto> done = saved -> changed.changed(saved);
        if (order.state() == OrderState.PLACED) {
            Ui.section(page, Lang.tr("Ready in"));
            final String[] prep = {String.valueOf(Math.max(10, order.prepMinutes()))};
            if (!contains(PREP, prep[0])) {
                prep[0] = PREP[1];
            }
            page.add(Ui.choice("prep", trAll(PREP_NAMES), PREP, prep[0], value -> prep[0] = value));
            page.add(Ui.primary("Accept", "accept", e -> Api.merchant().accept(order.id(),
                    new AcceptDto(Integer.parseInt(prep[0])), Net.to(done, Ui::fail))));
            page.add(Ui.danger("Reject", "reject", e -> {
                // The reason is asked on a screen of its own; with the order
                // turned down, back to the one that asked.
                final Form owner = page.getComponentForm();
                Ui.reason(owner, Lang.tr("Reject this order?"),
                        Lang.tr("The customer is told why, and nothing is charged."), "Reject",
                        reason -> Api.merchant().reject(order.id(), new ReasonDto(reason),
                                Net.to(saved -> {
                                    owner.showBack();
                                    changed.changed(saved);
                                }, Ui::fail)));
            }));
        } else if (order.state() == OrderState.ACCEPTED) {
            page.add(Ui.primary("Mark ready", "ready",
                    e -> Api.merchant().ready(order.id(), Net.to(done, Ui::fail))));
        }
        return page;
    }

    private static boolean contains(String[] values, String value) {
        for (String each : values) {
            if (each.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static String[] trAll(String[] texts) {
        String[] done = new String[texts.length];
        for (int iter = 0; iter < texts.length; iter++) {
            done[iter] = Lang.tr(texts[iter]);
        }
        return done;
    }
}
