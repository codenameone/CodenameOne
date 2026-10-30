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
import com.codenameone.examples.pulse.api.BookingDto;
import com.codenameone.examples.pulse.api.MembershipDto;
import com.codenameone.examples.pulse.api.StatsDto;
import com.codenameone.examples.pulse.api.UserDto;
import com.codenameone.examples.pulse.live.LiveChannel;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.net.Session;
import com.codenameone.examples.pulse.ui.Chart;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Menu;
import com.codenameone.examples.pulse.ui.Nav;
import com.codenameone.examples.pulse.ui.SettingsForm;
import com.codenameone.examples.pulse.ui.StatRow;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// The member's home: the next class, how the training is going, and where the
/// membership stands -- with the timetable and the pass one tap away.
///
/// Three reads fill it, each into its own card, so a slow one does not hold
/// the others back. A booking that changes on the server (a place freed up in
/// a queue, a class the studio cancelled) arrives over the live channel and
/// the cards are read again.
public class MemberForm extends LiveChannel.Adapter implements Nav.Home {
    private final Form form = Ui.home("Member");
    private final Menu menu = new Menu(form, Nav.MEMBER);
    private final Container next = Ui.card();
    private final Container stats = Ui.card();
    private final Container plan = Ui.card();

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        menu.item(FontImage.MATERIAL_HOME, "Home", "home", () -> form.showBack());
        menu.item(FontImage.MATERIAL_EVENT, "Timetable", "schedule",
                () -> ScheduleForm.show(form));
        menu.item(FontImage.MATERIAL_EVENT_AVAILABLE, "My bookings", "bookings",
                () -> BookingsForm.show(form));
        menu.item(FontImage.MATERIAL_QR_CODE, "Membership pass", "pass",
                () -> PassForm.show(form, null));
        menu.item(FontImage.MATERIAL_FAMILY_RESTROOM, "Household", "household",
                () -> HouseholdForm.show(form));
        menu.item(FontImage.MATERIAL_CARD_MEMBERSHIP, "Membership", "membership",
                () -> MembershipForm.show(form));
        menu.item(FontImage.MATERIAL_BEACH_ACCESS, "Away", "away", () -> AwayForm.show(form));
        menu.item(FontImage.MATERIAL_INBOX, "Inbox", "nudges", () -> NudgesForm.show(form));

        UserDto user = Session.user();
        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvHomeBar");
        bar.add(BorderLayout.WEST, menu.flatButton());
        bar.add(BorderLayout.CENTER, Ui.label(Lang.tr("Hi, {0}",
                user == null ? "" : user.displayName()), "SvTitle"));
        bar.add(BorderLayout.EAST, Ui.flat(FontImage.MATERIAL_QR_CODE, "home-pass",
                e -> PassForm.show(form, null)));

        Container page = Ui.page();
        page.setName("home");
        next.setName("next");
        stats.setName("stats");
        plan.setName("plan");
        page.add(next).add(stats).add(plan);
        page.add(Ui.primary("Book a class", "home-book", e -> ScheduleForm.show(form)));
        page.add(Ui.secondary("Settings", "home-settings", e -> SettingsForm.show(form)));
        Ui.pull(page, this::load);

        Container shell = new Container(new BorderLayout());
        shell.add(BorderLayout.NORTH, bar);
        shell.add(BorderLayout.CENTER, page);
        form.add(shell);
        menu.install();
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            load();
        });
    }

    @Override
    public void bookingChanged(String id, String state) {
        load();
    }

    private void load() {
        Api.bookings().upcoming(Net.to(this::showNext, Ui::fail));
        Api.engagement().stats(Net.to(this::showStats, Ui::fail));
        Api.membership().memberships(Net.to(this::showPlan, Ui::fail));
    }

    private void showNext(List<BookingDto> bookings) {
        if (!Nav.isHome(form)) {
            return;
        }
        Reminders.sync(bookings);
        next.removeAll();
        next.add(Ui.label("Next class", "SvHeading"));
        BookingDto first = null;
        for (int iter = 0; bookings != null && iter < bookings.size(); iter++) {
            BookingDto booking = bookings.get(iter);
            if (booking.session() != null && !Classes.CANCELLED.equals(booking.state())
                    && (first == null
                    || booking.session().startsAt() < first.session().startsAt())) {
                first = booking;
            }
        }
        if (first == null) {
            next.add(Ui.text("Nothing booked yet. The timetable is a tap away.", "SvMuted"));
        } else {
            final BookingDto booking = first;
            next.add(Ui.tap(Ui.row(FontImage.MATERIAL_FITNESS_CENTER, booking.session().name(),
                    Classes.detail(booking.session()) + " - " + booking.memberName(),
                    Ui.badge(Classes.WAITLISTED.equals(booking.state())
                            ? Lang.tr("Queue #{0}", Integer.valueOf(booking.position()))
                            : Lang.tr("Booked"), "SvBadgeGood")), "next-class",
                    e -> BookingsForm.show(form)));
        }
        Ui.refresh(form);
    }

    private void showStats(StatsDto found) {
        if (!Nav.isHome(form) || found == null) {
            return;
        }
        stats.removeAll();
        stats.add(Ui.label("Your training", "SvHeading"));
        StatRow row = Ui.stats(3);
        row.stat(String.valueOf(found.weekStreak()), Lang.tr("week streak"), "streak");
        row.stat(String.valueOf(found.visitsThisMonth()), Lang.tr("visits this month"),
                "visits");
        row.stat(String.valueOf(found.minutesThisMonth()), Lang.tr("minutes"), "minutes");
        stats.add(row);
        List<Integer> weekly = found.weeklyVisits();
        if (weekly != null && !weekly.isEmpty()) {
            double[] values = new double[weekly.size()];
            for (int iter = 0; iter < values.length; iter++) {
                Integer value = weekly.get(iter);
                values[iter] = value == null ? 0 : value.intValue();
            }
            stats.add(Chart.of("weekly", values, Lang.tr("{0} weeks ago",
                    Integer.valueOf(values.length)), Lang.tr("This week")));
        }
        Ui.refresh(form);
    }

    private void showPlan(List<MembershipDto> memberships) {
        if (!Nav.isHome(form)) {
            return;
        }
        plan.removeAll();
        plan.add(Ui.label("Membership", "SvHeading"));
        if (memberships == null || memberships.isEmpty()) {
            plan.add(Ui.text("No plan yet. Pick one to start booking classes.", "SvMuted"));
        }
        for (int iter = 0; memberships != null && iter < memberships.size(); iter++) {
            MembershipDto membership = memberships.get(iter);
            plan.add(Ui.row(FontImage.MATERIAL_CARD_MEMBERSHIP, membership.planName(),
                    membership.memberName(), Ui.badge(MembershipForm.state(membership.state()),
                            MembershipForm.badge(membership.state()))));
        }
        plan.add(Ui.link("Manage membership", "home-membership",
                e -> MembershipForm.show(form)));
        Ui.refresh(form);
    }
}
