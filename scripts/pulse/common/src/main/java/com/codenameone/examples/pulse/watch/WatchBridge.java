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
package com.codenameone.examples.pulse.watch;

import com.codename1.wearable.WearableConnection;
import com.codename1.wearable.WearableMessage;
import com.codenameone.examples.pulse.api.BookingDto;
import com.codenameone.examples.pulse.api.MemberDto;
import com.codenameone.examples.pulse.api.PassDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.api.StatsDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import java.util.ArrayList;
import java.util.List;

/// The phone's side of the watch app: what the watch shows, kept current.
///
/// The watch shows the next class, the door pass, the member's week and the
/// classes coming up, and none of it needs an answer now, so both are replicated data rather than messages: each is
/// pinned at a path with [WearableConnection#putData(WearableMessage)] and the
/// watch reads it whenever it wakes, even with the phone out of reach. The
/// pass carries its secret rather than a code, because a code is good for
/// half a minute and the watch has to work out the next one itself; the
/// channel between a phone and its own paired watch is the platform's
/// encrypted one.
public final class WatchBridge {
    /// The next class: `name`, `startsAt`, `room`, `trainer`, `booked`,
    /// `capacity` and `color` (`#RRGGBB`); empty when none.
    public static final String NEXT = "/pulse/next";
    /// The pass: `memberId`, `name`, `secret` (hex), `period`, `digits`.
    public static final String PASS = "/pulse/pass";
    /// How the member is doing: `points`, `level`, `floor` and `next` (the
    /// points this level starts at and the next one does), `streak` (weeks)
    /// and `weeks`, visits a week as comma separated numbers, oldest first.
    public static final String STATS = "/pulse/stats";
    /// The classes booked next, soonest first: `count`, then `name<i>`,
    /// `at<i>` and `room<i>` for each.
    public static final String AGENDA = "/pulse/agenda";
    /// How many classes [#AGENDA] carries: what fits on a watch page.
    static final int AGENDA_SIZE = 3;

    private WatchBridge() {
    }

    /// Whether there is a watch to tell; publishing to none is harmless but
    /// wasted calls.
    public static boolean paired() {
        return WearableConnection.isSupported() && WearableConnection.isPaired();
    }

    /// Reads the next booked class and the member's pass, and publishes both,
    /// with the member's points.
    public static void publish() {
        if (!paired()) {
            return;
        }
        Api.engagement().stats(Net.to(WatchBridge::stats, (status, message) -> { }));
        Api.bookings().upcoming(Net.to(WatchBridge::next, (status, message) -> { }));
        Api.household().members(Net.to(members -> {
            String self = self(members);
            if (self.length() > 0) {
                Api.household().pass(self, Net.to(WatchBridge::pass, (status, message) -> { }));
            }
        }, (status, message) -> { }));
    }

    static void next(List<BookingDto> upcoming) {
        List<SessionDto> booked = new ArrayList<SessionDto>();
        long now = System.currentTimeMillis();
        for (int iter = 0; upcoming != null && iter < upcoming.size(); iter++) {
            BookingDto booking = upcoming.get(iter);
            SessionDto session = booking.session();
            if (session == null || !"BOOKED".equals(booking.state()) || session.endsAt() < now) {
                continue;
            }
            int at = booked.size();
            while (at > 0 && booked.get(at - 1).startsAt() > session.startsAt()) {
                at--;
            }
            booked.add(at, session);
        }
        WearableMessage data = new WearableMessage(NEXT);
        if (!booked.isEmpty()) {
            SessionDto next = booked.get(0);
            data.put("name", next.name()).put("startsAt", next.startsAt())
                    .put("room", next.locationName() + ", " + next.roomName())
                    .put("trainer", next.trainerName()).put("booked", next.booked())
                    .put("capacity", next.capacity())
                    .put("color", next.color() == null ? "" : next.color());
        }
        WearableConnection.putData(data);
        // The agenda starts after the class the first page already shows.
        WearableMessage agenda = new WearableMessage(AGENDA);
        int count = 0;
        for (int iter = 1; iter < booked.size() && count < AGENDA_SIZE; iter++) {
            SessionDto session = booked.get(iter);
            agenda.put("name" + count, session.name()).put("at" + count, session.startsAt())
                    .put("room" + count, session.locationName() + ", " + session.roomName());
            count++;
        }
        WearableConnection.putData(agenda.put("count", count));
    }

    static void stats(StatsDto stats) {
        if (stats == null) {
            return;
        }
        StringBuilder weeks = new StringBuilder();
        List<Integer> visits = stats.weeklyVisits();
        for (int iter = 0; visits != null && iter < visits.size(); iter++) {
            if (iter > 0) {
                weeks.append(',');
            }
            weeks.append(visits.get(iter).intValue());
        }
        WearableConnection.putData(new WearableMessage(STATS).put("points", stats.points())
                .put("level", stats.level()).put("floor", stats.levelFloor())
                .put("next", stats.nextLevel()).put("streak", stats.weekStreak())
                .put("weeks", weeks.toString()));
    }

    static void pass(PassDto pass) {
        if (pass == null) {
            return;
        }
        WearableConnection.putData(new WearableMessage(PASS).put("memberId", pass.memberId())
                .put("name", pass.memberName()).put("secret", pass.secret())
                .put("period", pass.periodSeconds()).put("digits", pass.digits()));
    }

    private static String self(List<MemberDto> members) {
        for (int iter = 0; members != null && iter < members.size(); iter++) {
            if ("SELF".equals(members.get(iter).relation())) {
                return members.get(iter).id();
            }
        }
        return members == null || members.isEmpty() ? "" : members.get(0).id();
    }
}
