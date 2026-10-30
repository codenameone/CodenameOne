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

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.live.LiveChannel;
import com.codenameone.examples.pulse.member.Classes;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Beat;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Menu;
import com.codenameone.examples.pulse.ui.Nav;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// The trainer's home: the classes they teach this week, each leading to its
/// roster, and from there to the live view of the class.
public class TrainerForm extends LiveChannel.Adapter implements Nav.Home {
    private final Form form = Ui.home("Trainer");
    private final Menu menu = new Menu(form, Nav.TRAINER);
    private final Container list = Ui.list("trainer");
    private List<SessionDto> classes;

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        menu.item(FontImage.MATERIAL_EVENT, "My classes", "classes", () -> form.showBack());
        menu.item(FontImage.MATERIAL_FAVORITE, "Live class", "live", this::live);
        menu.item(FontImage.MATERIAL_SCHEDULE, "Shifts", "shifts", () -> ShiftsForm.show(form));
        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvHomeBar");
        bar.add(BorderLayout.WEST, menu.flatButton());
        bar.add(BorderLayout.CENTER, Ui.label("Coaching", "SvTitle"));
        Ui.pull(list, this::load);
        Container shell = new Container(new BorderLayout());
        shell.add(BorderLayout.NORTH, bar);
        shell.add(BorderLayout.CENTER, list);
        form.add(shell);
        menu.install();
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            load();
        });
    }

    private void load() {
        Api.trainer().classes(Net.to(found -> {
            classes = found;
            fill();
        }, Ui.failed(list, this::load)));
    }

    private void fill() {
        list.removeAll();
        long at = System.currentTimeMillis();
        SessionDto featured = current(classes, at);
        if (featured != null) {
            list.add(featured(featured, featured.startsAt() <= at));
        }
        // The rest of the week as the members see it, a day at a time, with
        // the room's fill instead of a booking button.
        String day = null;
        for (int iter = 0; classes != null && iter < classes.size(); iter++) {
            final SessionDto session = classes.get(iter);
            if (session == featured || session.endsAt() < at) {
                continue;
            }
            String heading = dayName(session.startsAt(), at);
            if (!heading.equals(day)) {
                day = heading;
                Ui.section(list, heading);
            }
            boolean cancelled = Classes.CANCELLED.equals(session.state());
            Label fill = Ui.badge(cancelled ? Lang.tr("Cancelled")
                    : session.booked() + "/" + session.capacity(),
                    cancelled ? "SvBadgeBad" : "SvBadge");
            list.add(Ui.tap(Classes.card(session, fill, null), "class-" + session.id(),
                    e -> RosterForm.show(form, session)));
        }
        if (classes == null || classes.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_SPORTS, Lang.tr("No classes this week"),
                    Lang.tr("Classes the studio gives you appear here.")));
        }
        Ui.refresh(form);
    }

    // The class on now, or the next one: the one thing a coach opens the app
    // for, so it leads on the dark card the member's home uses for theirs.
    private Container featured(final SessionDto session, boolean running) {
        Container card = new Container(BoxLayout.y(), "SvInk");
        card.setName("featured");
        Container head = new Container(new BorderLayout(), "SvInkRow");
        head.add(BorderLayout.CENTER, Ui.label(running ? "ON NOW" : "UP NEXT", "SvInkEyebrow"));
        Beat beat = new Beat("SvBeat", running);
        beat.setPreferredW(CN.convertToPixels(14));
        beat.setPreferredH(CN.convertToPixels(5));
        head.add(BorderLayout.EAST, beat);
        card.add(head);
        card.add(Ui.text(session.name(), "SvInkTitle"));
        card.add(Ui.text(Ui.when(session.startsAt()) + Ui.DOT + session.roomName() + Ui.DOT
                + Lang.tr("{0} of {1} booked", Integer.valueOf(session.booked()),
                        Integer.valueOf(session.capacity())), "SvInkText"));
        Button live = new Button(Lang.tr("Open live view"), "SvInkButton");
        live.setName("openLive");
        live.addActionListener(e -> LiveClassForm.show(form, session));
        Button roster = new Button(Lang.tr("Roster"), "SvInkText");
        roster.getAllStyles().setBgTransparency(0);
        roster.setName("openRoster");
        roster.addActionListener(e -> RosterForm.show(form, session));
        Container actions = new Container(new BorderLayout(), "SvInkRow");
        actions.add(BorderLayout.WEST, live).add(BorderLayout.EAST, roster);
        card.add(actions);
        return card;
    }

    private static String dayName(long millis, long now) {
        java.util.Calendar a = java.util.Calendar.getInstance();
        a.setTime(new java.util.Date(millis));
        java.util.Calendar b = java.util.Calendar.getInstance();
        b.setTime(new java.util.Date(now));
        int days = (int) ((start(a) - start(b)) / 86400000L);
        if (days == 0) {
            return Lang.tr("Today");
        }
        if (days == 1) {
            return Lang.tr("Tomorrow");
        }
        return new com.codename1.l10n.SimpleDateFormat("EEEE, MMM d").format(
                new java.util.Date(millis));
    }

    private static long start(java.util.Calendar day) {
        day.set(java.util.Calendar.HOUR_OF_DAY, 12);
        day.set(java.util.Calendar.MINUTE, 0);
        day.set(java.util.Calendar.SECOND, 0);
        day.set(java.util.Calendar.MILLISECOND, 0);
        return day.getTime().getTime();
    }

    /// Opens the class that is on now, or the next one.
    private void live() {
        SessionDto now = current(classes, System.currentTimeMillis());
        if (now == null) {
            Ui.say(Lang.tr("No class today"));
            return;
        }
        LiveClassForm.show(form, now);
    }

    /// The class running at `at`, else the next to start; cancelled ones never.
    static SessionDto current(List<SessionDto> classes, long at) {
        SessionDto next = null;
        for (int iter = 0; classes != null && iter < classes.size(); iter++) {
            SessionDto session = classes.get(iter);
            if (Classes.CANCELLED.equals(session.state()) || session.endsAt() < at) {
                continue;
            }
            if (session.startsAt() <= at) {
                return session;
            }
            if (next == null || session.startsAt() < next.startsAt()) {
                next = session;
            }
        }
        return next;
    }

    @Override
    public void sessionChanged(String id, int booked, int waitlisted, String state) {
        load();
    }
}
