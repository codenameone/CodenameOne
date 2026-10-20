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
package com.codenameone.examples.savor.ui;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.savor.api.DeliveryDto;
import com.codenameone.examples.savor.net.Net;

import java.util.List;

/// A list of deliveries: a customer's, a courier's, or for an admin everyone's. Which
/// it is depends only on the call it is given -- and on the server, which
/// answers each of the three with the deliveries that caller may see.
public final class DeliveryListForm {
    private DeliveryListForm() {
    }

    /// @param people whether to name the customer and courier on each row, which
    ///     is for the admin's list
    public static void show(Form previous, String title, Net.Call<List<DeliveryDto>> call,
            final boolean people) {
        show(previous, title, call, people, null);
    }

    /// What a tap on a delivery does, given the list to come back to.
    public interface Opened {
        void opened(Form list, DeliveryDto delivery);
    }

    public static void show(Form previous, String title, Net.Call<List<DeliveryDto>> call,
            final boolean people, final Opened opened) {
        final Form form = Ui.form(title, "Deliveries");
        Ui.back(form, previous);
        final Container list = Ui.list("deliveries");
        form.add(BorderLayout.CENTER, list);
        form.show();
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Net.send(call, deliveries -> {
            list.removeAll();
            if (deliveries == null || deliveries.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_HISTORY, "No deliveries yet",
                        "A delivery shows up here as soon as it is asked for."));
            }
            for (int iter = 0; deliveries != null && iter < deliveries.size(); iter++) {
                final DeliveryDto delivery = deliveries.get(iter);
                String detail = Ui.when(delivery.requestedAt());
                if (people) {
                    detail += "  -  " + (delivery.courierName() == null
                            || delivery.courierName().length() == 0 ? delivery.customerName()
                            : Lang.tr("{0} with {1}", delivery.customerName(), delivery.courierName()));
                }
                Container value = new Container(BoxLayout.y());
                value.add(Ui.plain(Ui.money(delivery.total() > 0 ? delivery.total() : delivery.fareCents(),
                        delivery.currency()), "SvRowValue"));
                value.add(Ui.badge(Ui.state(delivery.state()), Ui.stateBadge(delivery.state())));
                Container row = Ui.row(FontImage.MATERIAL_PLACE, delivery.dropoffAddress(), detail,
                        value);
                if (opened == null) {
                    row.setName("delivery-" + iter);
                } else {
                    Ui.tap(row, "delivery-" + iter, e -> opened.opened(form, delivery));
                }
                list.add(row);
            }
            Ui.refresh(form);
        }, Ui.failed(list, () -> load[0].run()));
        Ui.pull(list, load[0]);
        load[0].run();
    }
}
