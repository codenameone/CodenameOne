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
package com.codenameone.examples.pulse.member;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.pulse.api.BookingDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// The household's places: what is coming up, with a way to cancel it or put
/// it in the calendar, and what was attended before.
public final class BookingsForm {
    private BookingsForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("My bookings", "Bookings");
        Ui.back(form, previous);
        final Container list = Ui.list("bookings");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.bookings().upcoming(Net.to(upcoming ->
                Api.bookings().history(Net.to(history -> {
                    fill(form, list, upcoming, history, load[0]);
                }, Ui.failed(list, load[0]))), Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.show();
        load[0].run();
    }

    private static void fill(Form form, Container list, List<BookingDto> upcoming,
            List<BookingDto> history, final Runnable load) {
        list.removeAll();
        Ui.section(list, "Coming up");
        int shown = 0;
        for (int iter = 0; upcoming != null && iter < upcoming.size(); iter++) {
            final BookingDto booking = upcoming.get(iter);
            if (booking.session() == null || Classes.CANCELLED.equals(booking.state())) {
                continue;
            }
            Container row = Ui.row(FontImage.MATERIAL_FITNESS_CENTER, booking.session().name(),
                    Classes.detail(booking.session()) + "\n" + booking.memberName(),
                    Ui.badge(Classes.WAITLISTED.equals(booking.state())
                            ? Lang.tr("Queue #{0}", Integer.valueOf(booking.position()))
                            : Lang.tr("Booked"), Classes.WAITLISTED.equals(booking.state())
                            ? "SvBadgeWarn" : "SvBadgeGood"));
            row.setName("booking-" + iter);
            list.add(row);
            list.add(FlowLayout.encloseRight(
                    Ui.link("Add to calendar", "calendar-" + iter,
                            e -> Classes.addToCalendar(booking.session())),
                    Ui.link("Cancel", "cancel-" + iter, e -> Classes.cancel(booking, load))));
            shown++;
        }
        if (shown == 0) {
            list.add(Ui.text("Nothing booked.", "SvMuted"));
        }
        Ui.section(list, "History");
        for (int iter = 0; history != null && iter < history.size(); iter++) {
            BookingDto booking = history.get(iter);
            if (booking.session() == null) {
                continue;
            }
            list.add(Ui.row(Classes.CHECKED_IN.equals(booking.state())
                    ? FontImage.MATERIAL_CHECK_CIRCLE : FontImage.MATERIAL_SCHEDULE,
                    booking.session().name(), Ui.when(booking.session().startsAt()) + " - "
                    + booking.memberName(), null));
        }
        if (history == null || history.isEmpty()) {
            list.add(Ui.text("No classes attended yet.", "SvMuted"));
        }
        Ui.refresh(form);
    }
}
