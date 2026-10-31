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
package com.codenameone.examples.pulse.owner;

import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.pulse.api.ClassTypeDto;
import com.codenameone.examples.pulse.api.NewSessionDto;
import com.codenameone.examples.pulse.api.ReasonDto;
import com.codenameone.examples.pulse.api.RoomDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.api.TrainerDto;
import com.codenameone.examples.pulse.member.Classes;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/// The coming week's timetable across every location, from where the owner
/// schedules a class, cancels one or hands it to another trainer.
public final class TimetableForm {
    private static final long DAY = 24L * 3600000L;

    private TimetableForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Timetable", "Timetable");
        Ui.back(form, previous);
        final Container list = Ui.list("timetable");
        final Runnable[] load = new Runnable[1];
        // From the start of today, so the morning's classes are still there to
        // check and nothing from yesterday leads the list.
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        final long from = today.getTime().getTime();
        load[0] = () -> Api.studio().classes("", from, 8,
                Net.to(classes -> fill(form, list, classes, load[0]), Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.add(BorderLayout.SOUTH, Ui.dock(Ui.primary("Schedule a class", "scheduleClass",
                e -> schedule(form, load[0]))));
        form.show();
        load[0].run();
    }

    private static void fill(final Form form, Container list, List<SessionDto> classes,
            final Runnable load) {
        list.removeAll();
        String day = null;
        for (int iter = 0; classes != null && iter < classes.size(); iter++) {
            final SessionDto session = classes.get(iter);
            String today = Ui.day(session.startsAt());
            if (!today.equals(day)) {
                Ui.section(list, today);
                day = today;
            }
            boolean cancelled = Classes.CANCELLED.equals(session.state());
            // The members' card, with the room's fill where they have a
            // booking button, and the location as "who" since an owner runs
            // more than one.
            list.add(Ui.tap(Classes.card(session, Ui.badge(cancelled ? Lang.tr("Cancelled")
                    : session.booked() + "/" + session.capacity(),
                    cancelled ? "SvBadgeBad" : "SvBadge"), null, session.locationName()),
                    "session-" + iter, e -> manage(form, session, load)));
        }
        if (classes == null || classes.isEmpty()) {
            list.add(Ui.text("Nothing on the timetable this week.", "SvMuted"));
        }
        Ui.refresh(form);
    }

    /// One class: cancel it, with a reason the members are told, or give it
    /// to another trainer.
    private static void manage(final Form previous, final SessionDto session,
            final Runnable changed) {
        final Form form = Ui.form(session.name(), "ManageClass");
        Ui.back(form, previous);
        final Container page = Ui.page();
        page.add(Ui.fact("When", Ui.when(session.startsAt())));
        page.add(Ui.fact("Where", session.locationName() + Ui.DOT + session.roomName()));
        page.add(Ui.fact("Trainer", session.trainerName()));
        page.add(Ui.fact("Booked", session.booked() + "/" + session.capacity()));
        if (!Classes.CANCELLED.equals(session.state())) {
            Ui.section(page, "Substitute");
            Api.studio().trainers(Net.to(trainers -> {
                for (int iter = 0; trainers != null && iter < trainers.size(); iter++) {
                    final TrainerDto trainer = trainers.get(iter);
                    if (trainer.username().equals(session.trainer())) {
                        continue;
                    }
                    page.add(Ui.tap(Ui.row(FontImage.MATERIAL_PERSON, trainer.name(),
                            trainer.title(), null), "sub-" + iter,
                            e -> Api.owner().substitute(session.id(), trainer.username(),
                                    Net.to(done -> {
                                        Ui.say(Lang.tr("{0} takes the class", trainer.name()));
                                        previous.showBack();
                                        changed.run();
                                    }, Ui::fail))));
                }
                page.add(Ui.danger("Cancel class", "cancelClass", e -> Ui.reason(form,
                        "Cancel class", Lang.tr("Everyone booked is told, with your reason, "
                        + "and their credit is returned."), "Cancel class",
                        reason -> Api.owner().cancelClass(session.id(), new ReasonDto(reason),
                                Net.to(done -> {
                                    Ui.say(Lang.tr("Class cancelled"));
                                    previous.showBack();
                                    changed.run();
                                }, Ui::fail)))));
                Ui.refresh(form);
            }, Ui::fail));
        }
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    /// Puts a class on the timetable: a template, a room, a trainer and a
    /// first time, repeated weekly.
    static void schedule(final Form previous, final Runnable scheduled) {
        final Form form = Ui.form("Schedule a class", "ScheduleClass");
        Ui.back(form, previous);
        final Container page = Ui.page();
        page.setName("scheduleClass");
        form.add(BorderLayout.CENTER, page);
        form.show();
        Api.studio().classTypes(Net.to(types -> Api.owner().rooms(Net.to(rooms ->
                Api.studio().trainers(Net.to(trainers -> {
                    build(form, page, previous, types, rooms, trainers, scheduled);
                }, Ui::fail)), Ui::fail)), Ui::fail));
    }

    private static void build(final Form form, Container page, final Form previous,
            List<ClassTypeDto> types, List<RoomDto> rooms, List<TrainerDto> trainers,
            final Runnable scheduled) {
        if (types == null || types.isEmpty() || rooms == null || rooms.isEmpty()
                || trainers == null || trainers.isEmpty()) {
            page.add(Ui.text("Add a class template, a room and a trainer first.", "SvMuted"));
            Ui.refresh(form);
            return;
        }
        final String[] type = {types.get(0).id()};
        final String[] room = {rooms.get(0).id()};
        final String[] trainer = {trainers.get(0).username()};
        page.add(Ui.label("Class", "SvLabel"));
        page.add(Ui.choice("type", names(types, null, null), ids(types, null, null), type[0],
                value -> type[0] = value, 2));
        page.add(Ui.label("Room", "SvLabel"));
        page.add(Ui.choice("room", names(null, rooms, null), ids(null, rooms, null), room[0],
                value -> room[0] = value, 2));
        page.add(Ui.label("Trainer", "SvLabel"));
        page.add(Ui.choice("trainer", names(null, null, trainers), ids(null, null, trainers),
                trainer[0], value -> trainer[0] = value, 2));
        page.add(Ui.label("First class", "SvLabel"));
        final Picker when = new Picker();
        when.setType(Display.PICKER_TYPE_DATE_AND_TIME);
        when.setName("startsAt");
        when.setUIID("SvField");
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.setTime(new Date(System.currentTimeMillis() + DAY));
        tomorrow.set(Calendar.HOUR_OF_DAY, 18);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);
        when.setDate(tomorrow.getTime());
        page.add(when);
        final TextField capacity = Ui.field(page, "Places (blank for the template's)", "capacity",
                "", TextArea.NUMERIC);
        final TextField weeks = Ui.field(page, "Weeks", "weeks", "1", TextArea.NUMERIC);
        weeks.setText("1");
        page.add(Ui.primary("Schedule", "schedule", e -> {
            NewSessionDto wanted = new NewSessionDto(type[0], room[0], trainer[0],
                    when.getDate().getTime(), PlansForm.number(capacity.getText(), 0),
                    Math.max(1, PlansForm.number(weeks.getText(), 1)));
            Api.owner().schedule(wanted, Net.to(made -> {
                Ui.say(Lang.tr("{0} classes scheduled",
                        Integer.valueOf(made == null ? 0 : made.size())));
                previous.showBack();
                scheduled.run();
            }, Ui::fail));
        }));
        Ui.refresh(form);
    }

    private static String[] names(List<ClassTypeDto> types, List<RoomDto> rooms,
            List<TrainerDto> trainers) {
        int size = types != null ? types.size() : rooms != null ? rooms.size() : trainers.size();
        String[] out = new String[size];
        for (int iter = 0; iter < size; iter++) {
            out[iter] = types != null ? types.get(iter).name() : rooms != null
                    ? rooms.get(iter).name() : trainers.get(iter).name();
        }
        return out;
    }

    private static String[] ids(List<ClassTypeDto> types, List<RoomDto> rooms,
            List<TrainerDto> trainers) {
        int size = types != null ? types.size() : rooms != null ? rooms.size() : trainers.size();
        String[] out = new String[size];
        for (int iter = 0; iter < size; iter++) {
            out[iter] = types != null ? types.get(iter).id() : rooms != null
                    ? rooms.get(iter).id() : trainers.get(iter).username();
        }
        return out;
    }
}
