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

import com.codename1.calendar.CalendarAccess;
import com.codename1.calendar.CalendarAlarm;
import com.codename1.calendar.CalendarAuthorizationStatus;
import com.codename1.calendar.CalendarDateTime;
import com.codename1.calendar.CalendarEvent;
import com.codename1.calendar.CalendarMutationScope;
import com.codename1.calendar.LocalCalendarSource;
import com.codename1.ui.Button;
import com.codename1.ui.Sheet;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.pulse.ui.Chart;
import com.codenameone.examples.pulse.Telemetry;
import com.codenameone.examples.pulse.api.BookRequestDto;
import com.codenameone.examples.pulse.api.BookingDto;
import com.codenameone.examples.pulse.api.MemberDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Session;
import com.codenameone.examples.pulse.api.UserDto;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/// What every screen that shows a class does with it: books it, cancels it,
/// puts it in the device's calendar, and says how it stands.
///
/// The server decides whether a booking is a place or a place in the queue,
/// and whether the member is old enough, young enough or paid up enough to
/// have either; nothing is checked here first, so the reason the server gives
/// is the one the member reads.
public final class Classes {
    public static final String BOOKED = "BOOKED";
    public static final String WAITLISTED = "WAITLISTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String CHECKED_IN = "CHECKED_IN";

    /// Age bands for the timetable filter.
    public static final String BAND_ALL = "all";
    public static final String BAND_KIDS = "kids";
    public static final String BAND_TEENS = "teens";
    public static final String BAND_ADULTS = "adults";

    private Classes() {
    }

    /// Books `member` into `session`; a full class puts them in its queue.
    public static void book(SessionDto session, MemberDto member, final Runnable done) {
        book(session, member, done, null);
    }

    /// The same, with `failed` run after a refusal has been shown, so the
    /// caller can offer the button again.
    public static void book(SessionDto session, MemberDto member, final Runnable done,
            final Runnable failed) {
        final boolean child = member != null && "CHILD".equals(member.relation());
        Api.bookings().book(new BookRequestDto(session.id(), member == null ? null : member.id(),
                null), Net.to(booking -> {
                    boolean queued = WAITLISTED.equals(booking.state());
                    Telemetry.booked(queued, child);
                    Ui.say(queued ? Lang.tr("Class is full. You are number {0} in the queue",
                            Integer.valueOf(booking.position())) : Lang.tr("Booked"));
                    Reminders.refresh();
                    done.run();
                }, (status, message) -> {
                    Ui.fail(status, message);
                    if (failed != null) {
                        failed.run();
                    }
                }));
    }

    public static void cancel(BookingDto booking, final Runnable done) {
        Api.bookings().cancel(booking.id(), Net.to(cancelled -> {
            Telemetry.bookingCancelled();
            Ui.say("Cancelled");
            Reminders.refresh();
            done.run();
        }, Ui::fail));
    }

    /// The booking `member` holds in `session`, or null.
    public static BookingDto bookingOf(List<BookingDto> bookings, String sessionId,
            String memberId) {
        for (int iter = 0; bookings != null && iter < bookings.size(); iter++) {
            BookingDto booking = bookings.get(iter);
            if (sessionId.equals(booking.sessionId()) && memberId != null
                    && memberId.equals(booking.memberId())
                    && !CANCELLED.equals(booking.state())) {
                return booking;
            }
        }
        return null;
    }

    /// Whether `session` is for someone of `age`. A maximum of 0 is no maximum.
    public static boolean fits(SessionDto session, int age) {
        return age >= session.minAge() && (session.maxAge() <= 0 || age <= session.maxAge());
    }

    /// Whether `session` belongs in the age band the timetable is filtered to.
    public static boolean inBand(SessionDto session, String band) {
        if (BAND_KIDS.equals(band)) {
            return session.maxAge() > 0 && session.maxAge() <= 12;
        }
        if (BAND_TEENS.equals(band)) {
            return session.minAge() >= 13 && session.maxAge() > 0 && session.maxAge() <= 17;
        }
        if (BAND_ADULTS.equals(band)) {
            return session.minAge() >= 18 || session.maxAge() <= 0 && session.minAge() >= 16;
        }
        return true;
    }

    /// Places left, or the queue, in words.
    public static String places(SessionDto session) {
        int left = session.capacity() - session.booked();
        if ("CANCELLED".equals(session.state())) {
            return Lang.tr("Cancelled by the studio");
        }
        if (left > 0) {
            return Lang.tr("{0} of {1} places left", Integer.valueOf(left),
                    Integer.valueOf(session.capacity()));
        }
        return Lang.tr("Full, {0} waiting", Integer.valueOf(session.waitlisted()));
    }

    /// A class as a card: the time in its own column, a stripe in the class
    /// type's colour, the name with its kind of training, who teaches it and
    /// where, how full it is, and `action` -- a Book button or how the member
    /// stands -- at the end.
    public static Container card(SessionDto session, Component action) {
        return card(session, action, null);
    }

    /// The first name of whoever `booking` is for, or null when it is the
    /// member's own.
    public static String forWhom(BookingDto booking) {
        UserDto user = Session.user();
        String name = booking.memberName();
        if (name == null || user != null && name.equals(user.displayName())) {
            return null;
        }
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }

    /// The same card for a booking made for someone else in the household.
    ///
    /// @param who the first name of the one it is for, or null for the member
    public static Container card(SessionDto session, Component action, String who) {
        return card(session, action, who, null);
    }

    /// The card, with where it is beside who teaches it, for those who see
    /// every location's classes.
    public static Container card(SessionDto session, Component action, String who,
            String where) {
        int color = color(session.color());
        Container card = new Container(new BorderLayout());
        card.setUIID("SvClass");
        card.setName("class-" + session.id());

        Container time = new Container(BoxLayout.y());
        time.setUIID("SvClassTime");
        time.add(new Label(clock(session.startsAt()), "SvClassClock"));
        time.add(new Label(Lang.tr("{0} min", Integer.valueOf(
                (int) ((session.endsAt() - session.startsAt()) / 60000L))), "SvClassLength"));
        // The kind of class sits under its time, which leaves the name the
        // whole width of the card's middle.
        Label glyph = new Label("", "SvClassGlyph");
        glyph.getAllStyles().setFgColor(color);
        glyph.setIcon(FontImage.createMaterial(glyph(session.category()),
                glyph.getUnselectedStyle(), 3.2f));
        time.add(glyph);
        Label stripe = new Label("", "SvClassStripe");
        stripe.getAllStyles().setBgColor(color);
        stripe.getAllStyles().setBgTransparency(255);
        stripe.setPreferredW(com.codename1.ui.CN.convertToPixels(0.9f));
        Container west = new Container(new BorderLayout());
        west.add(BorderLayout.CENTER, time).add(BorderLayout.EAST, stripe);
        card.add(BorderLayout.WEST, west);

        Container body = new Container(BoxLayout.y());
        body.setUIID("SvClassBody");
        body.add(new com.codename1.components.SpanLabel(session.name(), "SvClassName"));
        body.add(new Label(nullToEmpty(session.trainerName()), "SvClassWho"));
        if (where != null) {
            body.add(new Label(where, "SvClassWho"));
        }
        if (who != null) {
            body.add(new Label(Lang.tr("for {0}", who), "SvClassWho"));
        }
        if ("CANCELLED".equals(session.state())) {
            body.add(new Label(Lang.tr("Cancelled by the studio"), "SvClassFull"));
        } else {
            int left = session.capacity() - session.booked();
            Chart.Meter meter = new Chart.Meter(session.capacity() <= 0 ? 0
                    : session.booked() / (double) session.capacity());
            meter.getAllStyles().setFgColor(color);
            Container fill = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER));
            fill.setUIID("SvClassFill");
            fill.add(BorderLayout.CENTER, meter);
            fill.add(BorderLayout.EAST, new Label(left > 0
                    ? Lang.tr("{0} left", Integer.valueOf(left))
                    : Lang.tr("{0} waiting", Integer.valueOf(session.waitlisted())),
                    left > 0 && left <= 3 ? "SvClassScarce" : left > 0 ? "SvClassLeft"
                            : "SvClassFull"));
            body.add(fill);
        }
        if (session.friendsGoing() > 0) {
            Label friends = new Label(session.friendsGoing() == 1 ? Lang.tr("A friend is going")
                    : Lang.tr("{0} friends going", Integer.valueOf(session.friendsGoing())),
                    "SvClassFriends");
            Ui.icon(friends, FontImage.MATERIAL_GROUP, 2.6f);
            body.add(friends);
        }
        card.add(BorderLayout.CENTER, body);
        if (action != null) {
            card.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(action));
        }
        return card;
    }

    private static String clock(long millis) {
        java.time.LocalTime time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                .toLocalTime();
        return (time.getHour() < 10 ? "0" : "") + time.getHour() + ":"
                + (time.getMinute() < 10 ? "0" : "") + time.getMinute();
    }

    /// How `booking` stands, as a pill; a tap on it offers what a member does
    /// with a booking -- put it in the calendar or cancel it.
    public static Button bookedPill(final SessionDto session, final BookingDto booking,
            final Runnable changed) {
        final Button pill = new Button(WAITLISTED.equals(booking.state())
                ? Lang.tr("Queue #{0}", Integer.valueOf(booking.position()))
                : Lang.tr("Booked"), WAITLISTED.equals(booking.state())
                ? "SvPillQueue" : "SvPillBooked");
        pill.setName("booked-" + session.id());
        pill.addActionListener(e -> {
            final Sheet sheet = new Sheet(null, session.name());
            Container content = sheet.getContentPane();
            content.setLayout(BoxLayout.y());
            content.add(Ui.text(detail(session), "SvMuted"));
            content.add(Ui.secondary("Add to calendar", "calendar-" + session.id(), ev -> {
                sheet.back();
                addToCalendar(session);
            }));
            content.add(Ui.link("Cancel this booking", "cancel-" + session.id(), ev -> {
                sheet.back();
                cancel(booking, changed);
            }));
            sheet.show();
        });
        return pill;
    }

    /// The icon for a kind of training.
    /// The kind of class as a small round tile: its glyph in its colour, on
    /// a pale wash of the same colour. Lists of classes and templates lead
    /// with it where a member's card has its stripe.
    public static Label tile(String category, String hex) {
        int color = color(hex);
        Label tile = new Label("", "SvClassTile");
        tile.getAllStyles().setFgColor(color);
        tile.setIcon(FontImage.createMaterial(glyph(category), tile.getUnselectedStyle(), 3.4f));
        tile.getAllStyles().setAlignment(Label.CENTER);
        int wash = mix(color, 0xffffff, 0.84f);
        tile.getAllStyles().setBorder(com.codename1.ui.plaf.RoundBorder.create().color(wash)
                .rectangle(false));
        return tile;
    }

    private static int mix(int a, int b, float share) {
        int r = (int) (((a >> 16) & 0xff) * (1 - share) + ((b >> 16) & 0xff) * share);
        int g = (int) (((a >> 8) & 0xff) * (1 - share) + ((b >> 8) & 0xff) * share);
        int bl = (int) ((a & 0xff) * (1 - share) + (b & 0xff) * share);
        return (r << 16) | (g << 8) | bl;
    }

    public static char glyph(String category) {
        if ("MIND".equals(category)) {
            return FontImage.MATERIAL_SELF_IMPROVEMENT;
        }
        if ("CARDIO".equals(category)) {
            return FontImage.MATERIAL_DIRECTIONS_BIKE;
        }
        if ("KIDS".equals(category)) {
            return FontImage.MATERIAL_CHILD_CARE;
        }
        return FontImage.MATERIAL_FITNESS_CENTER;
    }

    /// A `#rrggbb` colour from the server, or the brand's when it is not one.
    public static int color(String hex) {
        if (hex != null && hex.length() == 7 && hex.charAt(0) == '#') {
            int value = 0;
            for (int iter = 1; iter < 7; iter++) {
                int digit = Character.digit(hex.charAt(iter), 16);
                if (digit < 0) {
                    return 0xdb3416;
                }
                value = value * 16 + digit;
            }
            return value;
        }
        return 0xdb3416;
    }

    /// The trainer, room and time of `session` on one line.
    public static String detail(SessionDto session) {
        return Ui.when(session.startsAt()) + " - " + nullToEmpty(session.trainerName())
                + (session.roomName() == null ? "" : ", " + session.roomName());
    }

    /// Writes `session` into the device's calendar, with an alarm half an hour
    /// before. The device asks the user's permission the first time.
    public static void addToCalendar(final SessionDto session) {
        final LocalCalendarSource calendar = LocalCalendarSource.getInstance();
        if (!calendar.isAvailable()) {
            Ui.say("This device has no calendar to add to");
            return;
        }
        calendar.requestAuthorization(CalendarAccess.EVENTS_WRITE_ONLY).ready(status -> {
            if (status != CalendarAuthorizationStatus.FULL
                    && status != CalendarAuthorizationStatus.WRITE_ONLY) {
                Ui.say("Calendar access was not allowed");
                return;
            }
            ZoneId zone = ZoneId.systemDefault();
            CalendarEvent event = new CalendarEvent()
                    .setTitle(session.name())
                    .setLocation(nullToEmpty(session.locationName()))
                    .setDescription(Lang.tr("With {0}", nullToEmpty(session.trainerName())))
                    .setStart(CalendarDateTime.instant(Instant.ofEpochMilli(session.startsAt()),
                            zone))
                    .setEnd(CalendarDateTime.instant(Instant.ofEpochMilli(session.endsAt()), zone))
                    .addAlarm(new CalendarAlarm().setTimeBefore(Duration.ofMinutes(30)));
            calendar.saveEvent(event, CalendarMutationScope.THIS_INSTANCE).ready(saved -> {
                Telemetry.addedToCalendar();
                Ui.say("Added to your calendar");
            }).except(failed -> Ui.say("Could not add it to your calendar"));
        }).except(failed -> Ui.say("Could not add it to your calendar"));
    }

    static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
