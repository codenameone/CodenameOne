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

import com.codename1.backend.Transactions;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.domain.Room;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;
import com.codenameone.examples.pulse.repository.RoomRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.jwt;

/// The people, rooms and classes a test needs, made fresh for it.
///
/// Test classes share a running server and its database for as long as they
/// can, so a test that borrowed a demo account or a demo class would meet
/// whatever the test before it left behind: a place already taken, a pass
/// code already used. Each test makes its own instead, in a room of its own,
/// with a trainer of its own, so nothing it schedules can clash with anything
/// another test scheduled.
public final class People {
    public static final String PASSWORD = "a-long-password";
    private static final AtomicInteger NEXT = new AtomicInteger();
    private static final long DAY = 86400000L;

    private People() {
    }

    /// Work a test does on the database directly, which has to be inside a
    /// transaction as any repository call has.
    public interface Step<T> {
        T run() throws Exception;
    }

    /// Runs `step` in a transaction of its own and answers what it answered.
    public static <T> T tx(Step<T> step) throws Exception {
        final Object[] out = new Object[1];
        Transactions.execute(() -> {
            out[0] = step.run();
            return null;
        });
        @SuppressWarnings("unchecked")
        T answer = (T) out[0];
        return answer;
    }

    /// A name no other test has used.
    public static String unique(String stem) {
        return stem + System.nanoTime() % 1000000000L + "-" + NEXT.incrementAndGet();
    }

    /// Opens an adult member's account with the waiver signed and an
    /// unlimited membership, and returns its name.
    public static String member(Accounts accounts, MemberRepository members,
            MembershipRepository memberships, String name) throws Exception {
        return member(accounts, members, memberships, name, "plan-unlimited", 0);
    }

    /// The same on any plan; `credits` for a pack.
    public static String member(Accounts accounts, MemberRepository members,
            MembershipRepository memberships, String name, String planId, int credits)
            throws Exception {
        String username = account(accounts, name, false);
        Member self = self(members, username);
        self.setWaiverAt(Instant.now());
        tx(() -> { members.save(self); return null; });
        tx(() -> { memberships.insert(membership(username, self.getId(), planId, credits)); return null; });
        return username;
    }

    public static Membership membership(String account, String memberId, String planId,
            int credits) throws Exception {
        Instant now = Instant.now();
        Membership row = new Membership();
        row.setId(Ids.next());
        row.setMemberId(memberId);
        row.setAccount(account);
        row.setPlanId(planId);
        row.setState("ACTIVE");
        row.setStartedAt(now);
        row.setCurrentPeriodEnd(now.plusMillis(30L * DAY));
        row.setCreditsLeft(credits);
        row.setUpdatedAt(now);
        return row;
    }

    /// A trainer of the test's own.
    public static String trainer(Accounts accounts, String name) throws Exception {
        return account(accounts, name, true);
    }

    public static String account(Accounts accounts, String name, boolean trainer)
            throws Exception {
        String username = unique(name) + "@test.example";
        accounts.create(username, PASSWORD, name, "+15550111", true, LocalDate.of(1990, 5, 1),
                trainer, false, false, System.currentTimeMillis());
        return username;
    }

    /// The member who is the account holder.
    public static Member self(MemberRepository members, String username) throws Exception {
        return tx(() -> members.findFirstByAccountAndRelation(username, Accounts.SELF));
    }

    /// A room of the test's own, downtown.
    public static Room room(RoomRepository rooms, int capacity) throws Exception {
        Room row = new Room();
        row.setId(Ids.next());
        row.setLocationId("loc-downtown");
        row.setName(unique("Room "));
        row.setCapacity(capacity);
        tx(() -> { rooms.insert(row); return null; });
        return row;
    }

    /// A class `days` days from now, inserted as the timetable would.
    public static ClassSession session(ClassSessionRepository sessions, String type, Room room,
            String trainer, int capacity, long startsAt) throws Exception {
        ClassSession row = new ClassSession();
        row.setId(Ids.next());
        row.setClassTypeId(type);
        row.setLocationId(room.getLocationId());
        row.setRoomId(room.getId());
        row.setTrainer(trainer);
        row.setStartsAt(Instant.ofEpochMilli(startsAt));
        row.setEndsAt(Instant.ofEpochMilli(startsAt + 45L * 60000L));
        row.setCapacity(capacity);
        row.setState("SCHEDULED");
        tx(() -> { sessions.insert(row); return null; });
        return row;
    }

    /// A time `days` days ahead at 11:00 UTC, off peak at the studio.
    public static long daysAhead(int days) {
        long now = System.currentTimeMillis();
        return (now / DAY + days) * DAY + 11L * 3600000L;
    }

    /// The request, made as a member. The authorities are stated here and not
    /// read from the account: these are the tests of what a role may do, and
    /// `AccessTest` is the one that signs in for real.
    public static MockRequestBuilder asMember(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_MEMBER"));
    }

    public static MockRequestBuilder asTrainer(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_MEMBER", "ROLE_TRAINER"));
    }

    public static MockRequestBuilder asDesk(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_MEMBER", "ROLE_DESK"));
    }

    public static MockRequestBuilder asOwner(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_MEMBER", "ROLE_OWNER"));
    }
}
