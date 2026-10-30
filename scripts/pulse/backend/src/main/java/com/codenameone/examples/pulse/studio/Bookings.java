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
package com.codenameone.examples.pulse.studio;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.pulse.Ids;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.api.BookRequestDto;
import com.codenameone.examples.pulse.api.BookingDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.domain.Booking;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.ClassType;
import com.codenameone.examples.pulse.domain.Location;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.domain.MembershipPlan;
import com.codenameone.examples.pulse.engagement.Nudger;
import com.codenameone.examples.pulse.live.LiveHub;
import com.codenameone.examples.pulse.push.Notifier;
import com.codenameone.examples.pulse.repository.BookingRepository;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.ClassTypeRepository;
import com.codenameone.examples.pulse.repository.LocationRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.MembershipPlanRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Places in classes: booking one, the waitlist behind a full class, and
/// giving a place back.
///
/// The one rule that must never break is that a class is not booked past its
/// capacity, however many people tap Book at the same moment. It is kept by
/// the database, not by this class: the count of places taken is a column of
/// the class, and the only statement that raises it does so on condition
/// that it is still below capacity ([ClassSessionRepository#takePlace]). A
/// booking whose statement changed no row joins the waitlist instead. Two
/// requests for the last place both run that statement; the database makes
/// them take turns on the row, and the second finds it full.
///
/// Everything else is checked before that: that the class is still to come,
/// that the member is the right age for it, has signed the waiver, and holds
/// a membership that covers it.
@Component
public class Bookings {
    public static final String BOOKED = "BOOKED";
    public static final String WAITLISTED = "WAITLISTED";
    public static final String CHECKED_IN = "CHECKED_IN";
    public static final String CANCELLED = "CANCELLED";
    /// Given back too close to the start for the place to be useful to
    /// anyone else. A class from a pack is spent.
    public static final String LATE_CANCEL = "LATE_CANCEL";
    /// Booked, not cancelled, never came.
    public static final String NO_SHOW = "NO_SHOW";
    /// How long before a class a place can be given back for free.
    public static final long LATE_MILLIS = 12L * 3600000L;
    /// How long after a class ends its bookings are settled: anyone still
    /// only booked becomes a no-show, and the class is done.
    static final long SETTLE_MILLIS = 30L * 60000L;
    /// A waiting member is not moved into a place this close to the start:
    /// they could not get there. The place stays free for the door.
    static final long PROMOTE_UNTIL_MILLIS = 30L * 60000L;

    private final ClassSessionRepository sessions;
    private final BookingRepository bookings;
    private final ClassTypeRepository types;
    private final LocationRepository locations;
    private final MemberRepository members;
    private final MembershipRepository memberships;
    private final MembershipPlanRepository plans;
    private final Household household;
    private final Studio studio;
    private final Accounts accounts;
    private final LiveHub live;
    private final Nudger nudger;
    private final Notifier notifier;

    public Bookings(ClassSessionRepository sessions, BookingRepository bookings,
            ClassTypeRepository types, LocationRepository locations, MemberRepository members,
            MembershipRepository memberships, MembershipPlanRepository plans,
            Household household, Studio studio, Accounts accounts, LiveHub live, Nudger nudger,
            Notifier notifier) {
        this.sessions = sessions;
        this.bookings = bookings;
        this.types = types;
        this.locations = locations;
        this.members = members;
        this.memberships = memberships;
        this.plans = plans;
        this.household = household;
        this.studio = studio;
        this.accounts = accounts;
        this.live = live;
        this.nudger = nudger;
        this.notifier = notifier;
    }

    /// Books a member of the caller's household into a class, or puts them on
    /// its waitlist when it is full. Asking again for a member already booked
    /// or waiting answers the booking they have.
    public BookingDto book(String account, BookRequestDto request) throws IOException {
        Booking made = place(account, request);
        ClassSession session = reread(made.getSessionId());
        if (session != null) {
            live.sessionChanged(session.getId(), session.getBooked(), session.getWaitlisted(),
                    session.getState());
        }
        return settled(account, made);
    }

    @Transactional(rollbackFor = IOException.class)
    Booking place(String account, BookRequestDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Choose a class");
        }
        // Held first: a closing account and a booking for it take turns.
        accounts.hold(account);
        Member member = request.memberId() == null || request.memberId().length() == 0
                ? members.findFirstByAccountAndRelation(account, Accounts.SELF)
                : household.mine(account, request.memberId());
        if (member == null) {
            throw new ResponseStatusException(404, "No such member");
        }
        ClassSession session = studio.session(request.sessionId());
        Booking existing = bookings.findFirstBySessionIdAndMemberId(session.getId(),
                member.getId());
        if (existing != null && (BOOKED.equals(existing.getState())
                || WAITLISTED.equals(existing.getState())
                || CHECKED_IN.equals(existing.getState()))) {
            return existing;
        }
        Instant now = Instant.now();
        Membership membership = eligible(member, session, now);
        Booking row = existing == null ? new Booking() : existing;
        if (existing == null) {
            row.setId(Ids.next());
            row.setSessionId(session.getId());
            row.setMemberId(member.getId());
            row.setAccount(account);
            row.setCreatedAt(now);
        }
        row.setUpdatedAt(now);
        row.setCheckedInAt(Instant.EPOCH);
        row.setInvitedBy(cleanInviter(request.invitedBy(), account));
        if (sessions.takePlace(session.getId()) == 1) {
            spend(membership);
            row.setState(BOOKED);
            row.setQueuePosition(0L);
            nudger.booked(account, session.getId(), now);
        } else {
            // Full. The queue's row is held so that two people joining it at
            // once are given different places in it.
            sessions.hold(session.getId());
            Long last = bookings.lastPosition(session.getId());
            sessions.moveQueue(session.getId(), 1);
            row.setState(WAITLISTED);
            row.setQueuePosition(last == null ? 1L : last.longValue() + 1L);
        }
        if (existing == null) {
            bookings.insert(row);
        }
        return row;
    }

    /// Gives a place back, or leaves the waitlist. Inside
    /// [#LATE_MILLIS] of the start a place is a late cancellation: the
    /// member loses the class from a pack, as they would by not coming.
    public BookingDto cancel(String account, String bookingId) throws IOException {
        Booking row = release(account, bookingId);
        promote(row.getSessionId());
        ClassSession session = reread(row.getSessionId());
        if (session != null) {
            live.sessionChanged(session.getId(), session.getBooked(), session.getWaitlisted(),
                    session.getState());
        }
        return settled(account, row);
    }

    @Transactional(rollbackFor = IOException.class)
    Booking release(String account, String bookingId) throws IOException {
        Booking row = bookingId == null || bookingId.length() > 32 ? null
                : bookings.findFirstByIdAndAccount(bookingId, account);
        if (row == null) {
            throw new ResponseStatusException(404, "No such booking");
        }
        ClassSession session = studio.session(row.getSessionId());
        Instant now = Instant.now();
        if (BOOKED.equals(row.getState())) {
            if (!session.getStartsAt().isAfter(now)) {
                throw new ResponseStatusException(409, "That class has started");
            }
            boolean late = session.getStartsAt().toEpochMilli() - now.toEpochMilli()
                    < LATE_MILLIS;
            sessions.givePlace(session.getId());
            if (!late) {
                giveBack(row.getMemberId());
            }
            row.setState(late ? LATE_CANCEL : CANCELLED);
        } else if (WAITLISTED.equals(row.getState())) {
            sessions.moveQueue(session.getId(), -1);
            row.setState(CANCELLED);
        } else {
            return row;
        }
        row.setUpdatedAt(now);
        return row;
    }

    /// Moves the head of a class's waitlist into the places that are free,
    /// and tells each account moved. Safe to call at any time and from
    /// anywhere: with no place or nobody waiting it does nothing.
    public void promote(String sessionId) throws IOException {
        List<Booking> moved = fill(sessionId);
        for (int iter = 0; iter < moved.size(); iter++) {
            live.bookingChanged(moved.get(iter).getAccount(), moved.get(iter).getId(), BOOKED);
        }
        if (!moved.isEmpty()) {
            ClassSession session = reread(sessionId);
            if (session != null) {
                live.sessionChanged(session.getId(), session.getBooked(),
                        session.getWaitlisted(), session.getState());
            }
        }
    }

    @Transactional(rollbackFor = IOException.class)
    List<Booking> fill(String sessionId) throws IOException {
        List<Booking> moved = new ArrayList<Booking>();
        ClassSession session = sessions.findById(sessionId);
        Instant now = Instant.now();
        if (session == null || !Studio.SCHEDULED.equals(session.getState())
                || session.getStartsAt().toEpochMilli() - now.toEpochMilli()
                < PROMOTE_UNTIL_MILLIS) {
            return moved;
        }
        // One promotion at a time per class, so that the queue is walked by
        // one request and nobody is moved twice.
        sessions.hold(sessionId);
        List<Booking> waiting = bookings.findBySessionIdAndStateOrderByQueuePositionAsc(sessionId,
                WAITLISTED);
        for (int iter = 0; iter < waiting.size(); iter++) {
            Booking next = waiting.get(iter);
            Member member = members.findById(next.getMemberId());
            Membership membership;
            try {
                membership = member == null ? null : eligible(member, session, now);
            } catch (ResponseStatusException noLonger) {
                // Their membership lapsed or their pack ran out while they
                // waited. They keep their place in the queue, and the next
                // in line is offered this one.
                continue;
            }
            if (membership == null || sessions.takePlace(sessionId) != 1) {
                break;
            }
            spend(membership);
            sessions.moveQueue(sessionId, -1);
            next.setState(BOOKED);
            next.setQueuePosition(0L);
            next.setUpdatedAt(now);
            moved.add(next);
            ClassType type = types.findById(session.getClassTypeId());
            notifier.tell(next.getAccount(), Notifier.PROMOTED, "", sessionId, "You're in",
                    member.getName() + " has a place in " + (type == null ? "the class"
                            : type.getName()) + " now. Can't make it? Cancel to pass it on.",
                    now);
        }
        return moved;
    }

    /// The bookings of the caller's household still to come, and those on
    /// waitlists, soonest first.
    @Transactional(readOnly = true)
    public List<BookingDto> upcoming(String account) throws IOException {
        return list(account, Arrays.asList(BOOKED, WAITLISTED), true);
    }

    /// Classes the household came to, or missed, most recent first.
    @Transactional(readOnly = true)
    public List<BookingDto> history(String account) throws IOException {
        return list(account, Arrays.asList(CHECKED_IN, NO_SHOW, LATE_CANCEL), false);
    }

    private List<BookingDto> list(String account, List<String> states, boolean ahead)
            throws IOException {
        List<Booking> rows = bookings.findByAccountAndStateIn(account, states);
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            ids.add(rows.get(iter).getSessionId());
        }
        Map<String, SessionDto> byId = new HashMap<String, SessionDto>();
        if (!ids.isEmpty()) {
            List<SessionDto> described = studio.describe(account, sessions.findByIdIn(ids));
            for (int iter = 0; iter < described.size(); iter++) {
                byId.put(described.get(iter).id(), described.get(iter));
            }
        }
        Map<String, String> names = names(account);
        long now = System.currentTimeMillis();
        List<BookingDto> out = new ArrayList<BookingDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Booking row = rows.get(iter);
            SessionDto session = byId.get(row.getSessionId());
            if (session == null || ahead != (session.endsAt() > now)) {
                continue;
            }
            out.add(dto(row, names, session));
        }
        out.sort((a, b) -> ahead ? Long.compare(a.session().startsAt(), b.session().startsAt())
                : Long.compare(b.session().startsAt(), a.session().startsAt()));
        return out;
    }

    /// A class as it stands after a change committed, for telling the live
    /// connections about it.
    @Transactional(readOnly = true)
    ClassSession reread(String id) {
        return sessions.findById(id);
    }

    @Transactional(readOnly = true)
    BookingDto settled(String account, Booking row) throws IOException {
        return describe(account, row);
    }

    private BookingDto describe(String account, Booking row) throws IOException {
        return dto(row, names(account), studio.get(account, row.getSessionId()));
    }

    private Map<String, String> names(String account) {
        Map<String, String> names = new HashMap<String, String>();
        List<Member> household = members.findByAccountOrderByCreatedAtAsc(account);
        for (int iter = 0; iter < household.size(); iter++) {
            names.put(household.get(iter).getId(), household.get(iter).getName());
        }
        return names;
    }

    private static BookingDto dto(Booking row, Map<String, String> names, SessionDto session) {
        String name = names.get(row.getMemberId());
        return new BookingDto(row.getId(), row.getSessionId(), row.getMemberId(),
                name == null ? "" : name, row.getState(), (int) row.getQueuePosition(),
                row.getCreatedAt().toEpochMilli(), row.getCheckedInAt().toEpochMilli(), session);
    }

    /// The membership that lets `member` into `session`, or a 4xx saying why
    /// none does. Every check that does not depend on the class's places.
    Membership eligible(Member member, ClassSession session, Instant now) {
        if (!Studio.SCHEDULED.equals(session.getState())) {
            throw new ResponseStatusException(409, "That class is not running");
        }
        if (!session.getStartsAt().isAfter(now)) {
            throw new ResponseStatusException(409, "That class has started");
        }
        ClassType type = types.findById(session.getClassTypeId());
        LocalDate day = LocalDate.ofEpochDay(session.getStartsAt().toEpochMilli() / 86400000L);
        int age = Accounts.age(member.getBirthDate(), day);
        if (type != null && !inBand(age, type.getMinAge(), type.getMaxAge())) {
            throw new ResponseStatusException(403, member.getName() + " is " + age + "; "
                    + type.getName() + " is for " + band(type.getMinAge(), type.getMaxAge()));
        }
        if (!Household.signed(member)) {
            throw new ResponseStatusException(403, "Sign the waiver for " + member.getName()
                    + " first");
        }
        Membership membership = household.current(member.getId());
        if (membership == null) {
            throw new ResponseStatusException(402, member.getName()
                    + " needs a membership or a class pack");
        }
        if ("FROZEN".equals(membership.getState())
                && !membership.getFrozenUntil().isBefore(session.getStartsAt())) {
            throw new ResponseStatusException(402, member.getName()
                    + "'s membership is frozen for that date");
        }
        if ("PAST_DUE".equals(membership.getState())) {
            throw new ResponseStatusException(402, "Settle the unpaid invoice to book");
        }
        MembershipPlan plan = plans.findById(membership.getPlanId());
        if (plan != null) {
            if (!inBand(age, plan.getMinAge(), plan.getMaxAge())) {
                throw new ResponseStatusException(403, plan.getName() + " does not cover "
                        + member.getName() + "'s age");
            }
            if (plan.isOffPeakOnly() && peak(session)) {
                throw new ResponseStatusException(403, plan.getName()
                        + " covers off-peak classes only");
            }
            if (Household.counted(plan) && membership.getCreditsLeft() <= 0) {
                throw new ResponseStatusException(402, "The class pack is used up");
            }
        }
        return membership;
    }

    /// Takes a class from a pack. Anything else has nothing to spend. A pack
    /// emptied by another booking since [#eligible] read it refuses this one,
    /// and the transaction -- the place included -- rolls back.
    private void spend(Membership membership) {
        MembershipPlan plan = plans.findById(membership.getPlanId());
        if (plan != null && Household.counted(plan)
                && memberships.spendCredit(membership.getId()) != 1) {
            throw new ResponseStatusException(402, "The class pack is used up");
        }
    }

    private void giveBack(String memberId) {
        Membership membership = household.current(memberId);
        if (membership == null) {
            return;
        }
        MembershipPlan plan = plans.findById(membership.getPlanId());
        if (plan != null && Household.counted(plan)) {
            memberships.returnCredit(membership.getId());
        }
    }

    /// Whether a class starts in the busy hours: a weekday from six to nine
    /// in the morning or five to eight in the evening, at the location.
    boolean peak(ClassSession session) {
        Location location = locations.findById(session.getLocationId());
        ZoneId zone;
        try {
            zone = ZoneId.of(location == null || location.getTimeZone().length() == 0 ? "UTC"
                    : location.getTimeZone());
        } catch (RuntimeException unknown) {
            zone = ZoneId.of("UTC");
        }
        ZonedDateTime at = ZonedDateTime.ofInstant(session.getStartsAt(), zone);
        DayOfWeek day = at.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        int hour = at.getHour();
        return hour >= 6 && hour < 9 || hour >= 17 && hour < 20;
    }

    static boolean inBand(int age, int min, int max) {
        return age >= min && (max <= 0 || age <= max);
    }

    static String band(int min, int max) {
        if (max <= 0) {
            return "ages " + min + " and up";
        }
        return "ages " + min + " to " + max;
    }

    /// An inviter is kept only when it is another account: it is what a
    /// friend's invitation is credited by.
    private String cleanInviter(String invitedBy, String account) {
        if (invitedBy == null || invitedBy.length() == 0 || invitedBy.length() > 190
                || invitedBy.equals(account) || !accounts.exists(invitedBy)) {
            return "";
        }
        return invitedBy;
    }

    /// Every minute: classes that have a free place and a queue get it
    /// filled -- a place freed by a closed account or a cancelled membership
    /// is noticed here -- and classes that ended are settled.
    @Scheduled(fixedDelay = 60000, initialDelay = 30000, lock = "pulse-bookings")
    // The two lookups below read through the injected session, which needs a
    // transaction open; promote and settle join it.
    @Transactional(rollbackFor = IOException.class)
    public void sweep() throws IOException {
        Instant now = Instant.now();
        List<ClassSession> soon = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                now, now.plusMillis(14L * 86400000L));
        for (int iter = 0; iter < soon.size(); iter++) {
            ClassSession session = soon.get(iter);
            if (session.getWaitlisted() > 0 && session.getBooked() < session.getCapacity()) {
                promote(session.getId());
            }
        }
        List<ClassSession> ended = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                now.minusMillis(7L * 86400000L), now);
        for (int iter = 0; iter < ended.size(); iter++) {
            ClassSession session = ended.get(iter);
            if (Studio.SCHEDULED.equals(session.getState())
                    && session.getEndsAt().toEpochMilli() + SETTLE_MILLIS < now.toEpochMilli()) {
                settle(session.getId());
            }
        }
    }

    /// Ends a class: who is still only booked did not come, and who is still
    /// waiting never got in.
    @Transactional(rollbackFor = IOException.class)
    void settle(String sessionId) {
        ClassSession session = sessions.findById(sessionId);
        if (session == null || !Studio.SCHEDULED.equals(session.getState())) {
            return;
        }
        Instant now = Instant.now();
        List<Booking> rows = bookings.findBySessionIdOrderByCreatedAtAsc(sessionId);
        for (int iter = 0; iter < rows.size(); iter++) {
            Booking row = rows.get(iter);
            if (BOOKED.equals(row.getState())) {
                row.setState(NO_SHOW);
                row.setUpdatedAt(now);
            } else if (WAITLISTED.equals(row.getState())) {
                row.setState(CANCELLED);
                row.setUpdatedAt(now);
            }
        }
        session.setState(Studio.DONE);
    }
}
