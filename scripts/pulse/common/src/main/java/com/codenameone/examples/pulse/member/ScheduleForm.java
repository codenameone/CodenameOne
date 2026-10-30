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

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.pulse.api.BookingDto;
import com.codenameone.examples.pulse.api.MemberDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.live.LiveChannel;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Skeleton;
import com.codenameone.examples.pulse.ui.Ui;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/// The week's timetable, a day at a time.
///
/// Two filters narrow it: the age band of the class, and who in the household
/// it is for. Choosing a child shows only the classes the child is old enough
/// for, and books, queues and cancels for the child; choosing yourself does
/// the same for you. The server still has the last word on every booking.
///
/// Places left are kept up to date over the live channel while the screen is
/// open: a class that fills up changes its row without being asked for.
public final class ScheduleForm extends LiveChannel.Adapter {
    private static final long DAY = 24L * 60L * 60L * 1000L;

    private final Form form = Ui.form("Timetable", "Schedule");
    private final Container list = Ui.list("classes");
    private final Container filters = new Container(BoxLayout.y());
    private List<SessionDto> sessions = new ArrayList<SessionDto>();
    private List<BookingDto> bookings = new ArrayList<BookingDto>();
    private List<MemberDto> household = new ArrayList<MemberDto>();
    private long weekStart;
    private int day;
    private String band = Classes.BAND_ALL;
    private String memberId;

    private ScheduleForm() {
    }

    public static void show(final Form previous) {
        new ScheduleForm().open(previous);
    }

    private void open(final Form previous) {
        Ui.back(form, () -> {
            LiveChannel.listen(null);
            previous.showBack();
        });
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        weekStart = today.getTime().getTime();

        Container top = new Container(BoxLayout.y());
        top.setUIID("SvPage");
        top.add(days());
        top.add(filters);
        Ui.pull(list, this::load);
        form.add(BorderLayout.NORTH, top);
        form.add(BorderLayout.CENTER, list);
        form.show();
        LiveChannel.listen(this);
        Api.household().members(Net.to(found -> {
            household = found == null ? new ArrayList<MemberDto>() : found;
            for (MemberDto member : household) {
                if ("SELF".equals(member.relation())) {
                    memberId = member.id();
                }
            }
            if (memberId == null && !household.isEmpty()) {
                memberId = household.get(0).id();
            }
            buildFilters();
            load();
        }, Ui.failed(list, this::load)));
    }

    private Container days() {
        String[] labels = new String[7];
        String[] values = new String[7];
        SimpleDateFormat name = new SimpleDateFormat("EEE d");
        for (int iter = 0; iter < 7; iter++) {
            labels[iter] = iter == 0 ? Lang.tr("Today")
                    : name.format(new Date(weekStart + iter * DAY));
            values[iter] = String.valueOf(iter);
        }
        Container row = Ui.choice("day", labels, values, "0", value -> {
            day = Integer.parseInt(value);
            fill();
        }, 7);
        row.setScrollableX(true);
        return row;
    }

    private void buildFilters() {
        filters.removeAll();
        filters.add(Ui.choice("band", new String[] {Lang.tr("All ages"), Lang.tr("Kids"),
            Lang.tr("Teens"), Lang.tr("Adults")}, new String[] {Classes.BAND_ALL,
                Classes.BAND_KIDS, Classes.BAND_TEENS, Classes.BAND_ADULTS}, band, value -> {
                    band = value;
                    fill();
                }));
        if (household.size() > 1) {
            String[] labels = new String[household.size()];
            String[] values = new String[household.size()];
            for (int iter = 0; iter < labels.length; iter++) {
                labels[iter] = household.get(iter).name();
                values[iter] = household.get(iter).id();
            }
            filters.add(Ui.choice("for", labels, values, memberId, value -> {
                memberId = value;
                fill();
            }, Math.min(labels.length, 3)));
        }
    }

    private void load() {
        Api.bookings().upcoming(Net.to(found -> {
            bookings = found == null ? new ArrayList<BookingDto>() : found;
            Api.studio().classes("", weekStart, 7, Net.to(classes -> {
                sessions = classes == null ? new ArrayList<SessionDto>() : classes;
                fill();
            }, Ui.failed(list, this::load)));
        }, Ui.failed(list, this::load)));
    }

    @Override
    public void sessionChanged(String id, int booked, int waitlisted, String state) {
        for (int iter = 0; iter < sessions.size(); iter++) {
            if (sessions.get(iter).id().equals(id)) {
                load();
                return;
            }
        }
    }

    @Override
    public void bookingChanged(String id, String state) {
        load();
    }

    private MemberDto member() {
        for (MemberDto member : household) {
            if (member.id().equals(memberId)) {
                return member;
            }
        }
        return null;
    }

    private void fill() {
        list.removeAll();
        long from = weekStart + day * DAY;
        long to = from + DAY;
        MemberDto member = member();
        int shown = 0;
        for (final SessionDto session : sessions) {
            if (session.startsAt() < from || session.startsAt() >= to
                    || !Classes.inBand(session, band)
                    || member != null && !Classes.fits(session, member.age())) {
                continue;
            }
            list.add(row(session, member));
            shown++;
        }
        if (shown == 0) {
            list.add(Ui.empty(FontImage.MATERIAL_EVENT, Lang.tr("No classes"),
                    member == null ? Lang.tr("Nothing on this day.")
                            : Lang.tr("Nothing on this day for {0}.", member.name())));
        }
        if (list.getComponentCount() == 0) {
            list.add(new Skeleton(5));
        }
        Ui.refresh(form);
    }

    private Container row(final SessionDto session, final MemberDto member) {
        Container row = Ui.row(FontImage.MATERIAL_FITNESS_CENTER, session.name(),
                Classes.detail(session) + "\n" + Classes.places(session), null);
        row.setName("class-" + session.id());
        Container actions = new Container(new FlowLayout());
        final BookingDto held = member == null ? null
                : Classes.bookingOf(bookings, session.id(), member.id());
        if (held != null) {
            actions.add(Ui.badge(Classes.WAITLISTED.equals(held.state())
                    ? Lang.tr("Queue #{0}", Integer.valueOf(held.position()))
                    : Lang.tr("Booked"), "SvBadgeGood"));
            actions.add(Ui.link("Cancel", "cancel-" + session.id(),
                    e -> Classes.cancel(held, this::load)));
            actions.add(Ui.flat(FontImage.MATERIAL_EVENT_AVAILABLE, "calendar-" + session.id(),
                    e -> Classes.addToCalendar(session)));
        } else if (!"CANCELLED".equals(session.state())) {
            boolean full = session.booked() >= session.capacity();
            final Button book = full
                    ? Ui.secondary("Join the queue", "book-" + session.id(), null)
                    : Ui.primary("Book", "book-" + session.id(), null);
            book.addActionListener(e -> {
                book.setEnabled(false);
                Classes.book(session, member, this::load, () -> book.setEnabled(true));
            });
            actions.add(book);
        }
        Container block = new Container(BoxLayout.y());
        block.add(row).add(actions);
        return block;
    }
}
