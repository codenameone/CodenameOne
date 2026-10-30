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
package com.codenameone.examples.pulse.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.pulse.domain.Booking;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.repository.AwayPeriodRepository;
import com.codenameone.examples.pulse.repository.BookingRepository;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.FriendRepository;
import com.codenameone.examples.pulse.repository.KudosRepository;
import com.codenameone.examples.pulse.repository.LiveTicketRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;
import com.codenameone.examples.pulse.repository.NudgeRepository;
import com.codenameone.examples.pulse.repository.PassKeyRepository;
import com.codenameone.examples.pulse.repository.PhoneCodeRepository;
import com.codenameone.examples.pulse.repository.PreferenceRepository;
import com.codenameone.examples.pulse.repository.PushDeviceRepository;
import com.codenameone.examples.pulse.repository.ShiftRepository;
import com.codenameone.examples.pulse.repository.WorkoutRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// What closing an account does to the rows other parts of the server keep
/// under its name.
///
/// These are statements over whole sets of rows -- everything of one account
/// in a table -- and not the work of the services that own those tables,
/// which is why they are gathered here. Each deletion is a bulk statement, so
/// whatever the session had loaded before it is stale afterwards. It has no
/// transaction of its own: [Accounts] calls it inside one.
///
/// Invoices and check-ins stay: they are the studio's records of money taken
/// and visits made, and are kept under the closed account's name.
@Component
public class AccountCleanup {
    private final MemberRepository members;
    private final BookingRepository bookings;
    private final ClassSessionRepository sessions;
    private final MembershipRepository memberships;
    private final PassKeyRepository passes;
    private final WorkoutRepository workouts;
    private final KudosRepository kudos;
    private final FriendRepository friends;
    private final NudgeRepository nudges;
    private final AwayPeriodRepository away;
    private final ShiftRepository shifts;
    private final PreferenceRepository preferences;
    private final PhoneCodeRepository codes;
    private final LiveTicketRepository tickets;
    private final PushDeviceRepository pushDevices;

    public AccountCleanup(MemberRepository members, BookingRepository bookings,
            ClassSessionRepository sessions, MembershipRepository memberships,
            PassKeyRepository passes, WorkoutRepository workouts, KudosRepository kudos,
            FriendRepository friends, NudgeRepository nudges, AwayPeriodRepository away,
            ShiftRepository shifts, PreferenceRepository preferences, PhoneCodeRepository codes,
            LiveTicketRepository tickets, PushDeviceRepository pushDevices) {
        this.members = members;
        this.bookings = bookings;
        this.sessions = sessions;
        this.memberships = memberships;
        this.passes = passes;
        this.workouts = workouts;
        this.kudos = kudos;
        this.friends = friends;
        this.nudges = nudges;
        this.away = away;
        this.shifts = shifts;
        this.preferences = preferences;
        this.codes = codes;
        this.tickets = tickets;
        this.pushDevices = pushDevices;
    }

    /// Removes everything an account keeps, and hands back the places its
    /// household held in classes still to come. Returns the ids of the
    /// classes that now have a place free, so the caller can offer each to
    /// the waitlist.
    public List<String> removeEverythingOf(String username) {
        Instant now = Instant.now();
        List<String> freed = new ArrayList<String>();
        List<Booking> held = bookings.findByAccountAndStateIn(username,
                Arrays.asList("BOOKED", "WAITLISTED"));
        for (int iter = 0; iter < held.size(); iter++) {
            Booking booking = held.get(iter);
            ClassSession session = sessions.findById(booking.getSessionId());
            if (session == null || !session.getStartsAt().isAfter(now)) {
                continue;
            }
            if ("BOOKED".equals(booking.getState())) {
                sessions.givePlace(session.getId());
                if (!freed.contains(session.getId())) {
                    freed.add(session.getId());
                }
            } else {
                sessions.moveQueue(session.getId(), -1);
            }
        }
        bookings.deleteByAccount(username);
        List<Member> household = members.findByAccountOrderByCreatedAtAsc(username);
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < household.size(); iter++) {
            ids.add(household.get(iter).getId());
        }
        if (!ids.isEmpty()) {
            passes.deleteByMemberIdIn(ids);
        }
        // A membership is ended rather than deleted: its invoices name it.
        List<Membership> plans = memberships.findByAccountOrderByStartedAtDesc(username);
        for (int iter = 0; iter < plans.size(); iter++) {
            Membership membership = plans.get(iter);
            if (!"CANCELLED".equals(membership.getState())) {
                membership.setState("CANCELLED");
                membership.setUpdatedAt(now);
            }
        }
        members.deleteByAccount(username);
        workouts.deleteByAccount(username);
        kudos.deleteByFromAccount(username);
        kudos.deleteByToAccount(username);
        friends.deleteByAccount(username);
        friends.deleteByFriend(username);
        nudges.deleteByAccount(username);
        away.deleteByAccount(username);
        shifts.deleteByStaff(username);
        preferences.deleteByUsername(username);
        codes.deleteByUsername(username);
        tickets.deleteByUsername(username);
        pushDevices.deleteByUsername(username);
        return freed;
    }
}
