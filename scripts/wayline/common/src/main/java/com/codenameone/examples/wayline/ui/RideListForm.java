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
package com.codenameone.examples.wayline.ui;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.wayline.api.RideDto;
import com.codenameone.examples.wayline.net.Net;

import java.util.List;

/// A list of rides: a rider's, a driver's, or for an admin everyone's. Which
/// it is depends only on the call it is given -- and on the server, which
/// answers each of the three with the rides that caller may see.
public final class RideListForm {
    private RideListForm() {
    }

    /// @param people whether to name the rider and driver on each row, which
    ///     is for the admin's list
    public static void show(Form previous, String title, Net.Call<List<RideDto>> call,
            final boolean people) {
        show(previous, title, call, people, null);
    }

    /// What a tap on a ride does, given the list to come back to.
    public interface Opened {
        void opened(Form list, RideDto ride);
    }

    public static void show(Form previous, String title, Net.Call<List<RideDto>> call,
            final boolean people, final Opened opened) {
        final Form form = Ui.form(title, "Rides");
        Ui.back(form, previous);
        final Container list = Ui.list("rides");
        form.add(BorderLayout.CENTER, list);
        form.show();
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Net.send(call, rides -> {
            list.removeAll();
            if (rides == null || rides.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_HISTORY, "No rides yet",
                        "A ride shows up here as soon as it is asked for."));
            }
            for (int iter = 0; rides != null && iter < rides.size(); iter++) {
                final RideDto ride = rides.get(iter);
                String detail = Ui.when(ride.requestedAt);
                if (people) {
                    detail += "  -  " + (ride.driverName == null
                            || ride.driverName.length() == 0 ? ride.riderName
                            : Lang.tr("{0} with {1}", ride.riderName, ride.driverName));
                }
                Container value = new Container(BoxLayout.y());
                value.add(Ui.plain(Ui.money(ride.total > 0 ? ride.total : ride.fareCents,
                        ride.currency), "WlRowValue"));
                value.add(Ui.badge(Ui.state(ride.state), Ui.stateBadge(ride.state)));
                Container row = Ui.row(FontImage.MATERIAL_PLACE, ride.dropoffAddress, detail,
                        value);
                if (opened == null) {
                    row.setName("ride-" + iter);
                } else {
                    Ui.tap(row, "ride-" + iter, e -> opened.opened(form, ride));
                }
                list.add(row);
            }
            Ui.refresh(form);
        }, Ui.failed(list, () -> load[0].run()));
        Ui.pull(list, load[0]);
        load[0].run();
    }
}
