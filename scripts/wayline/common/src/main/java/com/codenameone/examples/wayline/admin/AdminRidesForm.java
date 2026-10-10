/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.admin;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.ReasonDto;
import com.codenameone.examples.wayline.api.RideDto;
import com.codenameone.examples.wayline.api.RideStates;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.Ui;
import java.util.List;

/// Every ride, for an admin: which are under way, which finished and which
/// did not, and for each what can still be done about it -- cancelling one that
/// is under way, returning the money for one that is paid.
public final class AdminRidesForm {
    private static final String[] FILTERS = {"all", "active", "completed", "cancelled"};
    private static final String[] FILTER_NAMES = {"All", "Under way", "Completed", "Cancelled"};

    private final Panes panes;
    private final Container list = Ui.list("rides");
    private List<RideDto> rides;
    private String filter = FILTERS[0];

    private AdminRidesForm(Panes panes) {
        this.panes = panes;
        Container top = new Container(BoxLayout.y());
        top.setUIID("WlPage");
        top.add(Ui.choice("filter", FILTER_NAMES, FILTERS, filter, value -> {
            filter = value;
            fill();
        }));
        Ui.pull(list, this::load);
        panes.list("Rides", "Rides", Panes.listed(top, list), this::load);
    }

    static void show(Form previous) {
        show(Panes.phone(previous));
    }

    static void show(Panes panes) {
        new AdminRidesForm(panes);
    }

    private void load() {
        Api.admin().rides(Net.to(found -> {
            rides = found;
            fill();
        }, Ui.failed(list, this::load)));
    }

    private void fill() {
        list.removeAll();
        int shown = 0;
        for (int iter = 0; rides != null && iter < rides.size(); iter++) {
            final RideDto ride = rides.get(iter);
            if (!passes(ride)) {
                continue;
            }
            String people = ride.driverName == null || ride.driverName.length() == 0
                    ? ride.riderName : Lang.tr("{0} with {1}", ride.riderName, ride.driverName);
            Container value = new Container(BoxLayout.y());
            value.add(Ui.plain(Ui.money(ride.total > 0 ? ride.total : ride.fareCents,
                    ride.currency), "WlRowValue"));
            value.add(Ui.badge(Ui.state(ride.state), Ui.stateBadge(ride.state)));
            final Container picked = Ui.row(FontImage.MATERIAL_PLACE, ride.dropoffAddress,
                    Ui.when(ride.requestedAt) + Ui.DOT + people, value);
            list.add(Ui.tap(picked, "ride-" + shown, e -> {
                panes.picked(picked);
                show(ride);
            }));
            shown++;
        }
        if (shown == 0 && rides != null) {
            list.add(Ui.empty(FontImage.MATERIAL_RECEIPT_LONG, "No rides here", null));
        }
        panes.refresh();
    }

    private boolean passes(RideDto ride) {
        if ("active".equals(filter)) {
            return RideStates.isActive(ride.state);
        }
        if ("completed".equals(filter)) {
            return RideStates.COMPLETED.equals(ride.state);
        }
        if ("cancelled".equals(filter)) {
            return !RideStates.isActive(ride.state) && !RideStates.COMPLETED.equals(ride.state);
        }
        return true;
    }

    private void show(final RideDto ride) {
        Container page = Ui.page();
        Label total = Ui.plain(Ui.money(ride.total > 0 ? ride.total : ride.fareCents,
                ride.currency), "WlAmount");
        page.add(total);
        Label state = Ui.badge(Ui.state(ride.state), Ui.stateBadge(ride.state));
        state.setName("rideState");
        page.add(FlowLayout.encloseCenter(state));
        page.add(FlowLayout.encloseCenter(Ui.plain(Ui.when(ride.requestedAt), "WlMuted")));

        Ui.section(page, "Trip");
        page.add(Ui.row(FontImage.MATERIAL_TRIP_ORIGIN, ride.pickupAddress, null, null));
        page.add(Ui.row(FontImage.MATERIAL_PLACE, ride.dropoffAddress,
                Ui.distance(ride.distanceMeters) + Ui.DOT + Ui.minutes(ride.durationSeconds),
                null));
        Ui.section(page, "People");
        page.add(Ui.row(FontImage.MATERIAL_HAIL, ride.riderName, ride.riderUsername, null));
        if (ride.driverName != null && ride.driverName.length() > 0) {
            page.add(Ui.row(FontImage.MATERIAL_DIRECTIONS_CAR, ride.driverName,
                    ride.vehicle + (ride.plate == null || ride.plate.length() == 0 ? ""
                            : Ui.DOT + ride.plate), null));
        }
        Ui.section(page, "Payment");
        page.add(Ui.fact("Kind of ride", Lang.tr(Ui.product(ride.product))));
        page.add(Ui.fact("Fare", Ui.money(ride.fareCents, ride.currency)));
        page.add(Ui.fact("Service fee", Ui.money(ride.serviceFee, ride.currency)));
        page.add(Ui.fact("Tip", Ui.money(ride.tip, ride.currency)));
        page.add(Ui.fact("Paid with", ride.cash ? Lang.tr("Cash") : ride.paymentMethodLabel));
        page.add(Ui.fact("Payment", Lang.tr(payment(ride.paymentStatus))));

        if (RideStates.isActive(ride.state)) {
            page.add(Ui.danger("Cancel this ride", "cancelRide", e -> panes.reason(
                    "Cancel ride", Lang.tr("The rider and the driver are both told, and "
                            + "nobody is charged."), "Cancel ride", why -> {
                        ReasonDto reason = new ReasonDto();
                        reason.reason = why;
                        Api.admin().cancelRide(ride.id, reason, Net.to(cancelled -> {
                            Telemetry.rideCancelled("admin", ride.state);
                            panes.home();
                        }, Ui::fail));
                    })));
        }
        if ("PAID".equals(ride.paymentStatus) && !ride.cash) {
            page.add(Ui.danger("Refund this ride", "refund", e -> panes.reason("Refund",
                    Lang.tr("{0} goes back to the card it came from.", Ui.money(
                            ride.total > 0 ? ride.total : ride.fareCents, ride.currency)),
                    "Refund", why -> {
                        ReasonDto reason = new ReasonDto();
                        reason.reason = why;
                        Api.admin().refundRide(ride.id, reason, Net.to(refunded -> {
                            Telemetry.rideRefunded("rides");
                            Ui.say("The ride was refunded");
                            panes.home();
                        }, Ui::fail));
                    })));
        }
        panes.detail("Ride", "RideDetail", page);
    }

    private static String payment(String status) {
        return "PAID".equals(status) ? "Paid" : "REFUNDED".equals(status) ? "Refunded"
                : "FAILED".equals(status) ? "Payment failed" : "Payment pending";
    }
}
