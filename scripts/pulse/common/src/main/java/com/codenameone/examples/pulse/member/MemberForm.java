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
import com.codenameone.examples.pulse.ui.Level;
import com.codenameone.examples.pulse.ui.Beat;
import com.codenameone.examples.pulse.ui.Badges;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.Label;
import com.codename1.ui.Button;
import com.codenameone.examples.pulse.api.BookingDto;
import com.codenameone.examples.pulse.api.MembershipDto;
import com.codenameone.examples.pulse.api.StatsDto;
import com.codenameone.examples.pulse.api.UserDto;
import com.codenameone.examples.pulse.live.LiveChannel;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.watch.WatchBridge;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.net.Session;
import com.codenameone.examples.pulse.ui.Chart;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Menu;
import com.codenameone.examples.pulse.ui.Nav;
import com.codenameone.examples.pulse.ui.SettingsForm;
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
        menu.item(FontImage.MATERIAL_GROUP, "Friends", "social", () -> SocialForm.show(form));
        menu.item(FontImage.MATERIAL_DIRECTIONS_RUN, "Workouts", "workouts",
                () -> WorkoutsForm.show(form));

        UserDto user = Session.user();
        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvHomeBar");
        bar.add(BorderLayout.WEST, menu.flatButton());
        bar.add(BorderLayout.CENTER, Ui.label(Lang.tr(greeting(),
                user == null ? "" : first(user.displayName())), "SvTitle"));
        bar.add(BorderLayout.EAST, Ui.flat(FontImage.MATERIAL_QR_CODE, "Membership pass", "home-pass",
                e -> PassForm.show(form, null)));

        Container page = Ui.page();
        page.setName("home");
        next.setName("next");
        stats.setName("stats");
        plan.setName("plan");
        page.add(next).add(stats).add(plan);
        next.setUIID("SvInk");
        Container quick = new Container(new com.codename1.ui.layouts.GridLayout(1, 4));
        quick.add(tile(FontImage.MATERIAL_EVENT, "Timetable", "home-book",
                () -> ScheduleForm.show(form)));
        quick.add(tile(FontImage.MATERIAL_QR_CODE, "Pass", "home-pass-tile",
                () -> PassForm.show(form, null)));
        quick.add(tile(FontImage.MATERIAL_GROUP, "Friends", "home-friends",
                () -> SocialForm.show(form)));
        quick.add(tile(FontImage.MATERIAL_DIRECTIONS_RUN, "Workouts", "home-workouts",
                () -> WorkoutsForm.show(form)));
        page.removeAll();
        page.add(next).add(quick).add(stats).add(plan);
        page.add(Ui.link("Settings", "home-settings", e -> SettingsForm.show(form)));
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
        // The watch shows the next class and the door code; it is told again
        // whenever the home is, which is after anything that changes either.
        WatchBridge.publish();
    }

    private void showNext(List<BookingDto> bookings) {
        if (!Nav.isHome(form)) {
            return;
        }
        Reminders.sync(bookings);
        next.removeAll();
        next.setLayout(BoxLayout.y());
        BookingDto first = null;
        for (int iter = 0; bookings != null && iter < bookings.size(); iter++) {
            BookingDto booking = bookings.get(iter);
            if (booking.session() != null && !Classes.CANCELLED.equals(booking.state())
                    && (first == null
                    || booking.session().startsAt() < first.session().startsAt())) {
                first = booking;
            }
        }
        Container head = new Container(new BorderLayout(), "SvInkRow");
        head.add(BorderLayout.CENTER, Ui.label("NEXT CLASS", "SvInkEyebrow"));
        Beat beat = new Beat("SvBeat", true);
        beat.setPreferredW(com.codename1.ui.CN.convertToPixels(14));
        beat.setPreferredH(com.codename1.ui.CN.convertToPixels(5));
        head.add(BorderLayout.EAST, beat);
        next.add(head);
        if (first == null) {
            next.add(Ui.label("Nothing booked yet", "SvInkTitle"));
            next.add(Ui.text("Pick a class and your pass and reminder are ready.",
                    "SvInkText"));
            Button find = new Button(Lang.tr("Find a class"), "SvInkButton");
            find.setName("next-find");
            find.addActionListener(e -> ScheduleForm.show(form));
            next.add(FlowLayout.encloseIn(find));
        } else {
            final BookingDto booking = first;
            SessionDto session = booking.session();
            next.add(Ui.text(session.name(), "SvInkTitle"));
            // Two short lines rather than one that runs off a narrow card.
            next.add(Ui.plain(Ui.when(session.startsAt()), "SvInkText"));
            next.add(Ui.plain((session.trainerName() == null ? "" : session.trainerName())
                    + (session.roomName() == null ? "" : Ui.DOT + session.roomName()),
                    "SvInkText"));
            Container chips = new Container(new FlowLayout(), "SvInkRow");
            chips.add(Ui.plain(until(session.startsAt()), "SvInkChip"));
            if (Classes.WAITLISTED.equals(booking.state())) {
                chips.add(Ui.label(Lang.tr("Queue #{0}", Integer.valueOf(booking.position())),
                        "SvInkChip"));
            }
            UserDto user = Session.user();
            if (booking.memberName() != null && user != null
                    && !booking.memberName().equals(user.displayName())) {
                chips.add(Ui.label(Lang.tr("for {0}", first(booking.memberName())),
                        "SvInkChip"));
            }
            next.add(chips);
            Button pass = new Button(Lang.tr("Show my pass"), "SvInkButton");
            pass.setName("next-pass");
            pass.addActionListener(e -> PassForm.show(form, null));
            Button more = new Button(Lang.tr("Details"), "SvInkText");
            more.getAllStyles().setBgTransparency(0);
            more.setName("next-class");
            more.addActionListener(e -> BookingsForm.show(form));
            Container actions = new Container(new BorderLayout(), "SvInkRow");
            actions.add(BorderLayout.WEST, pass).add(BorderLayout.EAST,
                    more);
            next.add(actions);
        }
        Ui.refresh(form);
    }

    private void showStats(StatsDto found) {
        if (!Nav.isHome(form) || found == null) {
            return;
        }
        stats.removeAll();
        int span = Math.max(1, found.nextLevel() - found.levelFloor());
        Level ring = new Level(found.level(), Math.min(1.0,
                (found.points() - found.levelFloor()) / (double) span));
        ring.setName("level");
        Container about = new Container(BoxLayout.y(), "SvLevelBox");
        // The ring already shows the level, so the words beside it lead with
        // the points and say what the next level takes.
        about.add(Ui.label(Lang.tr("{0} points", Integer.valueOf(found.points())), "SvHeading"));
        about.add(Ui.label(found.nextLevel() > found.points()
                ? Lang.tr("{0} more to level {1}",
                        Integer.valueOf(found.nextLevel() - found.points()),
                        Integer.valueOf(found.level() + 1))
                : Lang.tr("Top level"), "SvMuted"));
        if (found.weekStreak() > 0) {
            Label streak = Ui.label(Lang.tr("{0} week streak",
                    Integer.valueOf(found.weekStreak())), "SvBadgeGood");
            Ui.icon(streak, FontImage.MATERIAL_LOCAL_FIRE_DEPARTMENT, 2.6f);
            // Flush with the words above it, not indented by the badge margin.
            streak.getAllStyles().setMarginLeft(0);
            about.add(FlowLayout.encloseIn(streak));
        }
        Container levelRow = new Container(new BorderLayout());
        levelRow.add(BorderLayout.WEST, ring).add(BorderLayout.CENTER, about);
        stats.add(levelRow);

        Container tiles = new Container(new com.codename1.ui.layouts.GridLayout(1, 3));
        tiles.add(stat(found.visitsThisMonth(), "visits", "visits"));
        tiles.add(stat(found.minutesThisMonth(), "minutes", "minutes"));
        tiles.add(stat(found.caloriesThisMonth(), "kcal", "calories"));
        Ui.section(stats, "This month");
        stats.add(tiles);
        List<Integer> weekly = found.weeklyVisits();
        if (weekly != null && !weekly.isEmpty()) {
            double[] values = new double[weekly.size()];
            for (int iter = 0; iter < values.length; iter++) {
                Integer value = weekly.get(iter);
                values[iter] = value == null ? 0 : value.intValue();
            }
            stats.add(Ui.label(Lang.tr("Visits each week"), "SvChartTitle"));
            stats.add(Chart.of("weekly", values, Lang.tr("{0} weeks ago",
                    Integer.valueOf(values.length)), Lang.tr("This week")));
        }
        stats.add(Ui.label(Lang.tr("Badges  {0}/{1}", Integer.valueOf(Badges.count(found.badges())),
                Integer.valueOf(Badges.total())), "SvSection"));
        stats.add(Badges.shelf(found.badges()));
        if (!Badges.has(found.badges(), "STRAPPED_IN")) {
            Container callout = new Container(new BorderLayout());
            callout.setUIID("SvCallout");
            callout.setName("strap-callout");
            Label icon = new Label("", "SvCalloutText");
            Ui.icon(icon, FontImage.MATERIAL_MONITOR_HEART, 5f);
            callout.add(BorderLayout.WEST, icon);
            callout.add(BorderLayout.CENTER, Ui.text(
                    "Wear a heart rate strap or your watch to class: +25 points a class "
                    + "and the Strapped in badge.", "SvCalloutText"));
            Ui.tap(callout, "strap-callout", e -> WorkoutsForm.show(form));
            stats.add(callout);
        }
        Ui.refresh(form);
    }

    private static Container stat(int value, String label, String name) {
        Container tile = new Container(BoxLayout.y());
        tile.setUIID("SvTile");
        tile.setName("stat-" + name);
        tile.add(new Label(String.valueOf(value), "SvTileValue"));
        tile.add(new Label(Lang.tr(label), "SvTileLabel"));
        return tile;
    }

    private static Container tile(char icon, String text, String name, Runnable open) {
        Button button = new Button(Lang.tr(text), "SvTileLabel");
        Ui.icon(button, icon, 4.4f);
        button.setTextPosition(Label.BOTTOM);
        button.getAllStyles().setAlignment(Label.CENTER);
        button.setName(name);
        button.addActionListener(e -> open.run());
        Container tile = new Container(new BorderLayout());
        tile.setUIID("SvTile");
        tile.add(BorderLayout.CENTER, button);
        return tile;
    }

    private static String greeting() {
        int hour = java.time.LocalTime.now().getHour();
        return hour < 12 ? "Morning, {0}" : hour < 18 ? "Afternoon, {0}"
                : "Evening, {0}";
    }

    private static String first(String name) {
        int space = name == null ? -1 : name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name == null ? "" : name;
    }

    private static String until(long startsAt) {
        long minutes = (startsAt - System.currentTimeMillis()) / 60000L;
        if (minutes < 0) {
            return Lang.tr("On now");
        }
        if (minutes < 60) {
            return Lang.tr("In {0} min", Long.valueOf(minutes));
        }
        if (minutes < 24 * 60) {
            return Lang.tr("In {0} h", Long.valueOf(minutes / 60));
        }
        return Lang.tr("In {0} days", Long.valueOf(minutes / (24 * 60)));
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
                    first(membership.memberName()), Ui.badge(MembershipForm.state(membership.state()),
                            MembershipForm.badge(membership.state()))));
        }
        plan.add(Ui.link("Manage membership", "home-membership",
                e -> MembershipForm.show(form)));
        Ui.refresh(form);
    }
}
