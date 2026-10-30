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
package com.codenameone.examples.pulse.trainer;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.pulse.api.ShiftDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.net.Session;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// The trainer's shifts still to come, and those colleagues offered up. A
/// shift of one's own can be offered for a swap (and the offer withdrawn);
/// someone else's offered shift can be taken.
public final class ShiftsForm {
    /// The shift states the server uses.
    public static final String ASSIGNED = "ASSIGNED";
    public static final String SWAP_REQUESTED = "SWAP_REQUESTED";

    private ShiftsForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Shifts", "Shifts");
        Ui.back(form, previous);
        final Container list = Ui.list("shifts");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.trainer().shifts(Net.to(shifts -> fill(form, list, shifts, load[0]),
                Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.show();
        load[0].run();
    }

    private static void fill(Form form, Container list, List<ShiftDto> shifts,
            final Runnable load) {
        list.removeAll();
        String me = Session.user() == null ? "" : Session.user().username();
        Ui.section(list, "Mine");
        int mine = 0;
        for (int iter = 0; shifts != null && iter < shifts.size(); iter++) {
            final ShiftDto shift = shifts.get(iter);
            if (!me.equals(shift.staff())) {
                continue;
            }
            boolean offered = SWAP_REQUESTED.equals(shift.state());
            list.add(row(shift, offered ? Ui.badge(Lang.tr("Offered"), "SvBadgeWarn") : null));
            list.add(FlowLayout.encloseRight(Ui.link(offered ? "Withdraw offer" : "Offer swap",
                    "offer-" + iter, e -> Api.trainer().offer(shift.id(), Net.to(changed -> {
                        Ui.say(SWAP_REQUESTED.equals(changed.state())
                                ? Lang.tr("Offered to the team") : Lang.tr("Offer withdrawn"));
                        load.run();
                    }, Ui::fail)))));
            mine++;
        }
        if (mine == 0) {
            list.add(Ui.text("No shifts to come.", "SvMuted"));
        }
        Ui.section(list, "Offered by others");
        int open = 0;
        for (int iter = 0; shifts != null && iter < shifts.size(); iter++) {
            final ShiftDto shift = shifts.get(iter);
            if (me.equals(shift.staff()) || !SWAP_REQUESTED.equals(shift.state())) {
                continue;
            }
            list.add(row(shift, null));
            list.add(FlowLayout.encloseRight(Ui.link("Take", "take-" + iter,
                    e -> Api.trainer().take(shift.id(), Net.to(taken -> {
                        Ui.say(Lang.tr("The shift is yours"));
                        load.run();
                    }, Ui::fail)))));
            open++;
        }
        if (open == 0) {
            list.add(Ui.text("Nothing offered right now.", "SvMuted"));
        }
        Ui.refresh(form);
    }

    /// A shift as a row of a list, with `value` at its end.
    public static Container row(ShiftDto shift, com.codename1.ui.Component value) {
        return Ui.row(FontImage.MATERIAL_SCHEDULE, shift.duty(), Ui.when(shift.startsAt())
                + " - " + Ui.when(shift.endsAt()) + Ui.DOT + shift.staffName(), value);
    }
}
