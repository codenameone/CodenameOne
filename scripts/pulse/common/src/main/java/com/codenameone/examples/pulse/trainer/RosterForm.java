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

import com.codename1.components.Switch;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.pulse.api.AttendanceDto;
import com.codenameone.examples.pulse.api.RosterEntryDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.member.Classes;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// Who is booked into a class, with what the trainer should know before it
/// starts: a first visit, a medical note. A switch per person marks them
/// present, for someone the door did not see.
public final class RosterForm {
    private RosterForm() {
    }

    public static void show(final Form previous, final SessionDto session) {
        final Form form = Ui.form(session.name(), "Roster");
        Ui.back(form, previous);
        final Container list = Ui.list("roster");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.trainer().roster(session.id(), Net.to(
                roster -> fill(form, list, session, roster),
                Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.add(BorderLayout.SOUTH, Ui.primary("Live view", "live",
                e -> LiveClassForm.show(form, session)));
        form.show();
        load[0].run();
    }

    private static void fill(Form form, Container list, SessionDto session,
            List<RosterEntryDto> roster) {
        list.removeAll();
        list.add(Ui.text(Ui.when(session.startsAt()) + Ui.DOT + session.roomName(), "SvMuted"));
        Ui.section(list, "Booked");
        int queued = 0;
        for (int iter = 0; roster != null && iter < roster.size(); iter++) {
            final RosterEntryDto entry = roster.get(iter);
            if (Classes.WAITLISTED.equals(entry.state())) {
                queued++;
                continue;
            }
            if (Classes.CANCELLED.equals(entry.state())) {
                continue;
            }
            StringBuilder detail = new StringBuilder(Lang.tr("Age {0}",
                    Integer.valueOf(entry.age())));
            if (entry.heartRate() > 0) {
                detail.append(Ui.DOT).append(entry.heartRate()).append(" bpm");
            }
            if (entry.medicalNote() != null && entry.medicalNote().length() > 0) {
                detail.append('\n').append(Lang.tr("Medical: {0}", entry.medicalNote()));
            }
            final Switch present = Ui.toggle("present-" + entry.bookingId(),
                    Classes.CHECKED_IN.equals(entry.state()), on -> mark(session, entry, on));
            Container row = Ui.row(entry.firstVisit() ? FontImage.MATERIAL_NEW_RELEASES
                    : FontImage.MATERIAL_PERSON, entry.name(), detail.toString(), present);
            row.setName("roster-" + iter);
            list.add(row);
            if (entry.firstVisit()) {
                list.add(Ui.badge(Lang.tr("First visit"), "SvBadgeWarn"));
            }
        }
        if (queued > 0) {
            list.add(Ui.text(Lang.tr("{0} waiting for a place", Integer.valueOf(queued)),
                    "SvMuted"));
        }
        if (roster == null || roster.isEmpty()) {
            list.add(Ui.text("Nobody booked yet.", "SvMuted"));
        }
        Ui.refresh(form);
    }

    private static void mark(SessionDto session, RosterEntryDto entry,
            final boolean present) {
        Api.trainer().attendance(session.id(), new AttendanceDto(entry.bookingId(), present),
                Net.to(saved -> Ui.say(present ? Lang.tr("Marked present")
                        : Lang.tr("Marked absent")), Ui::fail));
    }
}
