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
                String detail = Ui.when(ride.requestedAt());
                if (people) {
                    detail += "  -  " + (ride.driverName() == null
                            || ride.driverName().length() == 0 ? ride.riderName()
                            : Lang.tr("{0} with {1}", ride.riderName(), ride.driverName()));
                }
                Container value = new Container(BoxLayout.y());
                value.add(Ui.plain(Ui.money(ride.total() > 0 ? ride.total() : ride.fareCents(),
                        ride.currency()), "WlRowValue"));
                value.add(Ui.badge(Ui.state(ride.state()), Ui.stateBadge(ride.state())));
                Container row = Ui.row(FontImage.MATERIAL_PLACE, ride.dropoffAddress(), detail,
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
