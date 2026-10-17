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
package com.codenameone.examples.morsel.admin;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.morsel.Telemetry;
import com.codenameone.examples.morsel.api.ReasonDto;
import com.codenameone.examples.morsel.api.DeliveryDto;
import com.codenameone.examples.morsel.api.DeliveryState;
import com.codenameone.examples.morsel.net.Api;
import com.codenameone.examples.morsel.net.Net;
import com.codenameone.examples.morsel.ui.Lang;
import com.codenameone.examples.morsel.ui.Ui;
import java.util.List;

/// Every delivery, for an admin: which are under way, which finished and which
/// did not, and for each what can still be done about it -- cancelling one that
/// is under way, returning the money for one that is paid.
public final class AdminDeliveriesForm {
    private static final String[] FILTERS = {"all", "active", "completed", "cancelled"};
    private static final String[] FILTER_NAMES = {"All", "Under way", "Completed", "Cancelled"};

    private final Panes panes;
    private final Container list = Ui.list("deliveries");
    private List<DeliveryDto> deliveries;
    private String filter = FILTERS[0];

    private AdminDeliveriesForm(Panes panes) {
        this.panes = panes;
        Container top = new Container(BoxLayout.y());
        top.setUIID("WlPage");
        top.add(Ui.choice("filter", FILTER_NAMES, FILTERS, filter, value -> {
            filter = value;
            fill();
        }));
        Ui.pull(list, this::load);
        panes.list("Deliveries", "Deliveries", Panes.listed(top, list), this::load);
    }

    static void show(Form previous) {
        show(Panes.phone(previous));
    }

    static void show(Panes panes) {
        new AdminDeliveriesForm(panes);
    }

    private void load() {
        Api.admin().deliveries(Net.to(found -> {
            deliveries = found;
            fill();
        }, Ui.failed(list, this::load)));
    }

    private void fill() {
        list.removeAll();
        int shown = 0;
        for (int iter = 0; deliveries != null && iter < deliveries.size(); iter++) {
            final DeliveryDto delivery = deliveries.get(iter);
            if (!passes(delivery)) {
                continue;
            }
            String people = delivery.courierName() == null || delivery.courierName().length() == 0
                    ? delivery.customerName()
                    : Lang.tr("{0} with {1}", delivery.customerName(), delivery.courierName());
            Container value = new Container(BoxLayout.y());
            value.add(Ui.plain(Ui.money(delivery.total() > 0 ? delivery.total() : delivery.fareCents(),
                    delivery.currency()), "WlRowValue"));
            value.add(Ui.badge(Ui.state(delivery.state()), Ui.stateBadge(delivery.state())));
            final Container picked = Ui.row(FontImage.MATERIAL_PLACE, delivery.dropoffAddress(),
                    Ui.when(delivery.requestedAt()) + Ui.DOT + people, value);
            list.add(Ui.tap(picked, "delivery-" + shown, e -> {
                panes.picked(picked);
                show(delivery);
            }));
            shown++;
        }
        if (shown == 0 && deliveries != null) {
            list.add(Ui.empty(FontImage.MATERIAL_RECEIPT_LONG, "No deliveries here", null));
        }
        panes.refresh();
    }

    private boolean passes(DeliveryDto delivery) {
        if ("active".equals(filter)) {
            return delivery.state().isActive();
        }
        if ("completed".equals(filter)) {
            return DeliveryState.COMPLETED == delivery.state();
        }
        if ("cancelled".equals(filter)) {
            return !delivery.state().isActive() && DeliveryState.COMPLETED != delivery.state();
        }
        return true;
    }

    private void show(final DeliveryDto delivery) {
        Container page = Ui.page();
        Label total = Ui.plain(Ui.money(delivery.total() > 0 ? delivery.total() : delivery.fareCents(),
                delivery.currency()), "WlAmount");
        page.add(total);
        Label state = Ui.badge(Ui.state(delivery.state()), Ui.stateBadge(delivery.state()));
        state.setName("deliveryState");
        page.add(FlowLayout.encloseCenter(state));
        page.add(FlowLayout.encloseCenter(Ui.plain(Ui.when(delivery.requestedAt()), "WlMuted")));

        Ui.section(page, "Trip");
        page.add(Ui.row(FontImage.MATERIAL_TRIP_ORIGIN, delivery.pickupAddress(), null, null));
        page.add(Ui.row(FontImage.MATERIAL_PLACE, delivery.dropoffAddress(),
                Ui.distance(delivery.distanceMeters()) + Ui.DOT + Ui.minutes(delivery.durationSeconds()),
                null));
        Ui.section(page, "People");
        page.add(Ui.row(FontImage.MATERIAL_HAIL, delivery.customerName(), delivery.customerUsername(), null));
        if (delivery.courierName() != null && delivery.courierName().length() > 0) {
            page.add(Ui.row(FontImage.MATERIAL_DIRECTIONS_CAR, delivery.courierName(),
                    delivery.vehicle() + (delivery.plate() == null || delivery.plate().length() == 0 ? ""
                            : Ui.DOT + delivery.plate()), null));
        }
        Ui.section(page, "Payment");
        page.add(Ui.fact("Kind of delivery", Lang.tr(Ui.product(delivery.product()))));
        page.add(Ui.fact("Fare", Ui.money(delivery.fareCents(), delivery.currency())));
        page.add(Ui.fact("Service fee", Ui.money(delivery.serviceFee(), delivery.currency())));
        page.add(Ui.fact("Tip", Ui.money(delivery.tip(), delivery.currency())));
        page.add(Ui.fact("Paid with", delivery.cash() ? Lang.tr("Cash") : delivery.paymentMethodLabel()));
        page.add(Ui.fact("Payment", Lang.tr(payment(delivery.paymentStatus()))));

        if (delivery.state().isActive()) {
            page.add(Ui.danger("Cancel this delivery", "cancelDelivery", e -> panes.reason(
                    "Cancel delivery", Lang.tr("The customer and the courier are both told, and "
                            + "nobody is charged."), "Cancel delivery", why -> {
                        ReasonDto reason = new ReasonDto(why);
                        Api.admin().cancelDelivery(delivery.id(), reason, Net.to(cancelled -> {
                            Telemetry.deliveryCancelled("admin", delivery.state());
                            panes.home();
                        }, Ui::fail));
                    })));
        }
        if ("PAID".equals(delivery.paymentStatus()) && !delivery.cash()) {
            page.add(Ui.danger("Refund this delivery", "refund", e -> panes.reason("Refund",
                    Lang.tr("{0} goes back to the card it came from.", Ui.money(
                            delivery.total() > 0 ? delivery.total() : delivery.fareCents(), delivery.currency())),
                    "Refund", why -> {
                        ReasonDto reason = new ReasonDto(why);
                        Api.admin().refundDelivery(delivery.id(), reason, Net.to(refunded -> {
                            Telemetry.deliveryRefunded("deliveries");
                            // The processor may not have returned the money
                            // yet; the receipt says which.
                            Ui.say("refund_pending".equals(refunded.status())
                                    ? "The refund is pending" : "The delivery was refunded");
                            panes.home();
                        }, Ui::fail));
                    })));
        }
        panes.detail("Delivery", "DeliveryDetail", page);
    }

    private static String payment(String status) {
        return "PAID".equals(status) ? "Paid" : "REFUNDED".equals(status) ? "Refunded"
                : "REFUND_PENDING".equals(status) ? "Refund pending"
                : "REFUND_FAILED".equals(status) ? "Refund failed"
                : "FAILED".equals(status) ? "Payment failed" : "Payment pending";
    }
}
