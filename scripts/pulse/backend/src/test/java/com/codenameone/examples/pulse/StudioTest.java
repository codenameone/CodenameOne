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
package com.codenameone.examples.pulse;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.api.BookRequestDto;
import com.codenameone.examples.pulse.api.BookingDto;
import com.codenameone.examples.pulse.api.CheckInRequestDto;
import com.codenameone.examples.pulse.api.CheckInResultDto;
import com.codenameone.examples.pulse.api.MemberDto;
import com.codenameone.examples.pulse.api.NewMemberDto;
import com.codenameone.examples.pulse.api.PassDto;
import com.codenameone.examples.pulse.desk.Desk;
import com.codenameone.examples.pulse.domain.Booking;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.domain.Room;
import com.codenameone.examples.pulse.repository.BookingRepository;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;
import com.codenameone.examples.pulse.repository.RoomRepository;
import com.codenameone.examples.pulse.studio.Bookings;
import com.codenameone.examples.pulse.studio.Household;
import com.codenameone.examples.pulse.studio.Passes;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The studio's promises: a class never holds more people than it has
/// places, a place given back goes to the first in the queue, a class is for
/// the ages it says, and the door lets in each pass once.
@BackendTest
class StudioTest {
    @Autowired
    private Accounts accounts;
    @Autowired
    private MemberRepository members;
    @Autowired
    private MembershipRepository memberships;
    @Autowired
    private RoomRepository rooms;
    @Autowired
    private ClassSessionRepository sessions;
    @Autowired
    private BookingRepository bookings;
    @Autowired
    private Bookings booking;
    @Autowired
    private Household household;
    @Autowired
    private Desk desk;

    private ClassSession hiit(int capacity, int days) throws Exception {
        Room room = People.room(rooms, 30);
        String trainer = People.trainer(accounts, "Coach");
        return People.session(sessions, "type-hiit", room, trainer, capacity,
                People.daysAhead(days));
    }

    private String member(String name) throws Exception {
        return People.member(accounts, members, memberships, name);
    }

    @Test
    void manyBookingAtOnceNeverOverfillAClass() throws Exception {
        ClassSession session = hiit(3, 5);
        int people = 8;
        final List<String> who = new ArrayList<String>();
        for (int iter = 0; iter < people; iter++) {
            who.add(member("Racer"));
        }
        final String id = session.getId();
        final AtomicInteger failed = new AtomicInteger();
        List<Thread> threads = new ArrayList<Thread>();
        for (int iter = 0; iter < people; iter++) {
            final String account = who.get(iter);
            Thread thread = new Thread(() -> {
                try {
                    booking.book(account, new BookRequestDto(id, null, null));
                } catch (Exception err) {
                    failed.incrementAndGet();
                }
            });
            threads.add(thread);
        }
        for (int iter = 0; iter < threads.size(); iter++) {
            threads.get(iter).start();
        }
        for (int iter = 0; iter < threads.size(); iter++) {
            threads.get(iter).join(60000L);
        }
        int booked = 0;
        int waiting = 0;
        List<Booking> rows = People.tx(() -> bookings.findBySessionIdOrderByCreatedAtAsc(id));
        for (int iter = 0; iter < rows.size(); iter++) {
            if (Bookings.BOOKED.equals(rows.get(iter).getState())) {
                booked++;
            } else if (Bookings.WAITLISTED.equals(rows.get(iter).getState())) {
                waiting++;
            }
        }
        ClassSession after = People.tx(() -> sessions.findById(id));
        assertEquals(3, booked, "places taken");
        assertEquals(3, after.getBooked());
        assertEquals(waiting, after.getWaitlisted());
        assertEquals(people, booked + waiting + failed.get());
    }

    @Test
    void aPlaceGivenBackGoesToTheFirstInTheQueue() throws Exception {
        ClassSession session = hiit(1, 6);
        String first = member("First");
        String second = member("Second");
        String third = member("Third");
        BookingDto placed = booking.book(first, new BookRequestDto(session.getId(), null, null));
        BookingDto queued = booking.book(second, new BookRequestDto(session.getId(), null, null));
        BookingDto later = booking.book(third, new BookRequestDto(session.getId(), null, null));
        assertEquals(Bookings.BOOKED, placed.state());
        assertEquals(Bookings.WAITLISTED, queued.state());
        assertEquals(1, queued.position());
        assertEquals(2, later.position());

        // Asking again answers the booking already held, not a second one.
        assertEquals(queued.id(),
                booking.book(second, new BookRequestDto(session.getId(), null, null)).id());

        booking.cancel(first, placed.id());
        assertEquals(Bookings.BOOKED, People.tx(() -> bookings.findById(queued.id())).getState());
        assertEquals(Bookings.WAITLISTED, People.tx(() -> bookings.findById(later.id())).getState());
        ClassSession after = People.tx(() -> sessions.findById(session.getId()));
        assertEquals(1, after.getBooked());
        assertEquals(1, after.getWaitlisted());
    }

    @Test
    void aChildCannotBookAnAdultsClass() throws Exception {
        ClassSession session = hiit(10, 7);
        String parent = member("Parent");
        LocalDate born = LocalDate.now().minusYears(7);
        MemberDto child = household.addChild(parent, new NewMemberDto("Little One",
                born.toString(), ""));
        household.signWaiver(parent, child.id());
        ResponseStatusException refused = assertThrows(ResponseStatusException.class,
                () -> booking.book(parent, new BookRequestDto(session.getId(), child.id(), null)));
        assertEquals(403, refused.getStatus());
        assertTrue(refused.getReason().contains("Little One"), refused.getReason());

        // The parent, on the same membership, is welcome.
        assertEquals(Bookings.BOOKED,
                booking.book(parent, new BookRequestDto(session.getId(), null, null)).state());
    }

    @Test
    void aMemberOfAnotherHouseholdCannotBeBookedFor() throws Exception {
        ClassSession session = hiit(10, 8);
        String one = member("One");
        String other = member("Other");
        Member theirs = People.self(members, other);
        ResponseStatusException refused = assertThrows(ResponseStatusException.class,
                () -> booking.book(one, new BookRequestDto(session.getId(), theirs.getId(), null)));
        assertEquals(404, refused.getStatus());
    }

    @Test
    void aPassLetsSomeoneInOnceAndNotAfterItExpires() throws Exception {
        String account = member("Door");
        Member self = People.self(members, account);
        PassDto pass = household.pass(account, self.getId());
        long now = System.currentTimeMillis();
        String scanned = "pulse:" + self.getId() + ":" + Passes.code(pass.secret(), now);

        CheckInResultDto first = desk.checkIn("desk", new CheckInRequestDto(scanned, null, null,
                "loc-downtown"));
        assertTrue(first.admitted(), first.reason());
        CheckInResultDto replay = desk.checkIn("desk", new CheckInRequestDto(scanned, null, null,
                "loc-downtown"));
        assertFalse(replay.admitted());
        assertTrue(replay.reason().contains("already used"), replay.reason());

        String stale = "pulse:" + self.getId() + ":"
                + Passes.code(pass.secret(), now - 10L * 60000L);
        CheckInResultDto expired = desk.checkIn("desk", new CheckInRequestDto(stale, null, null,
                "loc-downtown"));
        assertFalse(expired.admitted());

        // A code made with someone else's secret is not theirs.
        String forged = "pulse:" + self.getId() + ":"
                + Passes.code("00112233445566778899aabbccddeeff00112233", now);
        assertFalse(desk.checkIn("desk", new CheckInRequestDto(forged, null, null,
                "loc-downtown")).admitted());
    }

    @Test
    void theDoorTurnsAwayAMemberWhoOwes() throws Exception {
        String account = member("Owes");
        Member self = People.self(members, account);
        Membership held = People.tx(() -> household.current(self.getId()));
        held.setState("PAST_DUE");
        People.tx(() -> { memberships.save(held); return null; });
        PassDto pass = household.pass(account, self.getId());
        String scanned = "pulse:" + self.getId() + ":"
                + Passes.code(pass.secret(), System.currentTimeMillis());
        CheckInResultDto result = desk.checkIn("desk", new CheckInRequestDto(scanned, null, null,
                "loc-downtown"));
        assertFalse(result.admitted());
        assertTrue(result.reason().contains("overdue"), result.reason());
        assertEquals("PAST_DUE", result.membershipState());

        // And the app will not book them either.
        ClassSession session = hiit(10, 9);
        ResponseStatusException refused = assertThrows(ResponseStatusException.class,
                () -> booking.book(account, new BookRequestDto(session.getId(), null, null)));
        assertEquals(402, refused.getStatus());
    }

    @Test
    void anNfcFobIsNotEnoughWithoutAMembership() throws Exception {
        String account = People.account(accounts, "NoPlan", false);
        Member self = People.self(members, account);
        self.setWaiverAt(java.time.Instant.now());
        People.tx(() -> { members.save(self); return null; });
        PassDto pass = household.pass(account, self.getId());
        CheckInResultDto result = desk.checkIn("desk", new CheckInRequestDto(null, pass.nfcId(),
                null, "loc-downtown"));
        assertFalse(result.admitted());
        assertTrue(result.reason().contains("No membership"), result.reason());
    }
}
