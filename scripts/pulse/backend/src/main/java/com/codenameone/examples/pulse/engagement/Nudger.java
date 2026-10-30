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
package com.codenameone.examples.pulse.engagement;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.pulse.domain.Booking;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.ClassType;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.domain.Nudge;
import com.codenameone.examples.pulse.push.Notifier;
import com.codenameone.examples.pulse.repository.BookingRepository;
import com.codenameone.examples.pulse.repository.CampaignRepository;
import com.codenameone.examples.pulse.repository.CheckInRepository;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.ClassTypeRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;
import com.codenameone.examples.pulse.repository.NudgeRepository;
import com.codenameone.examples.pulse.studio.Studio;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// The marketing that runs by itself.
///
/// - **Fill a class**: a class starting in [#FILL_FROM_HOURS] to
///   [#FILL_UNTIL_HOURS] hours that is under [#FILL_SHARE]% full is offered
///   to members who have taken that kind of class before, a few per empty
///   place.
/// - **We miss you**: a member with a membership who has not been in for
///   [#MISS_DAYS] days is told so, once every [#MISS_AGAIN_DAYS] days.
///
/// Neither ever reaches a member who said they are away
/// ([Engagement#isAway]), one who turned promotions off, or one inside their
/// quiet hours; the last are left for a later run rather than skipped. Each
/// message is its own transaction, re-checked inside it, so two servers
/// running the job together send it once ([Scheduled#lock] makes that the
/// exception anyway).
@Component
public class Nudger {
    static final int FILL_FROM_HOURS = 3;
    static final int FILL_UNTIL_HOURS = 36;
    static final int FILL_SHARE = 60;
    /// Invitations sent per empty place, at most.
    static final int FILL_PER_PLACE = 3;
    /// A member is asked to fill a class at most once in this many hours.
    static final int FILL_AGAIN_HOURS = 48;
    static final int MISS_DAYS = 14;
    static final int MISS_AGAIN_DAYS = 21;
    /// Members gone longer than this are not chased: they have left.
    static final int MISS_GIVE_UP_DAYS = 120;
    private static final long HOUR = 3600000L;
    private static final long DAY = 24L * HOUR;

    private final ClassSessionRepository sessions;
    private final ClassTypeRepository types;
    private final BookingRepository bookings;
    private final CheckInRepository checkIns;
    private final MembershipRepository memberships;
    private final NudgeRepository nudges;
    private final CampaignRepository campaigns;
    private final Engagement engagement;
    private final Notifier notifier;

    public Nudger(ClassSessionRepository sessions, ClassTypeRepository types,
            BookingRepository bookings, CheckInRepository checkIns,
            MembershipRepository memberships, NudgeRepository nudges,
            CampaignRepository campaigns, Engagement engagement, Notifier notifier) {
        this.sessions = sessions;
        this.types = types;
        this.bookings = bookings;
        this.checkIns = checkIns;
        this.memberships = memberships;
        this.nudges = nudges;
        this.campaigns = campaigns;
        this.engagement = engagement;
        this.notifier = notifier;
    }

    @Scheduled(fixedDelay = 900000, initialDelay = 120000, lock = "pulse-nudges")
    public void run() {
        Instant now = Instant.now();
        try {
            fill(now);
            missYou(now);
        } catch (IOException | RuntimeException err) {
            System.err.println("[nudges] " + err);
        }
    }

    /// Offers the places left in classes that need filling.
    ///
    /// @return how many members were told
    public int fill(Instant now) throws IOException {
        List<ClassSession> soon = upcoming(now);
        int told = 0;
        for (int iter = 0; iter < soon.size(); iter++) {
            ClassSession session = soon.get(iter);
            int left = session.getCapacity() - session.getBooked();
            if (!Studio.SCHEDULED.equals(session.getState()) || left <= 0
                    || session.getBooked() * 100 >= session.getCapacity() * FILL_SHARE) {
                continue;
            }
            List<String> who = candidates(session);
            int sent = 0;
            for (int at = 0; at < who.size() && sent < left * FILL_PER_PLACE; at++) {
                if (offer(who.get(at), session.getId(), now)) {
                    sent++;
                }
            }
            told += sent;
        }
        return told;
    }

    @Transactional(readOnly = true)
    List<ClassSession> upcoming(Instant now) {
        return sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                now.plusMillis(FILL_FROM_HOURS * HOUR), now.plusMillis(FILL_UNTIL_HOURS * HOUR));
    }

    /// Who has taken this kind of class and is not in this one.
    @Transactional(readOnly = true)
    List<String> candidates(ClassSession session) {
        Set<String> in = new HashSet<String>();
        List<Booking> rows = bookings.findBySessionIdOrderByCreatedAtAsc(session.getId());
        for (int iter = 0; iter < rows.size(); iter++) {
            in.add(rows.get(iter).getAccount());
        }
        List<String> out = new ArrayList<String>();
        List<String> took = bookings.accountsWhoTook(session.getClassTypeId());
        for (int iter = 0; iter < took.size(); iter++) {
            if (!in.contains(took.get(iter))) {
                out.add(took.get(iter));
            }
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    boolean offer(String account, String sessionId, Instant now) throws IOException {
        ClassSession session = sessions.findById(sessionId);
        LocalDate day = LocalDate.ofEpochDay(session.getStartsAt().toEpochMilli() / DAY);
        if (engagement.isAway(account, day)
                || nudges.existsByAccountAndSessionIdAndKind(account, sessionId, Notifier.FILL)
                || nudges.existsByAccountAndKindAndSentAtGreaterThan(account, Notifier.FILL,
                        now.minusMillis(FILL_AGAIN_HOURS * HOUR))
                || !notifier.reachable(account, now)) {
            return false;
        }
        ClassType type = types.findById(session.getClassTypeId());
        String name = type == null ? "A class" : type.getName();
        int left = session.getCapacity() - session.getBooked();
        notifier.tell(account, Notifier.FILL, "", sessionId, name + " has room",
                left + (left == 1 ? " place is" : " places are") + " left in " + name
                        + ". Want one?", now);
        return true;
    }

    /// Tells members who have not been in for a while that they are missed.
    ///
    /// @return how many were told
    public int missYou(Instant now) throws IOException {
        List<String> gone = missing(now);
        int told = 0;
        for (int iter = 0; iter < gone.size(); iter++) {
            if (miss(gone.get(iter), now)) {
                told++;
            }
        }
        return told;
    }

    @Transactional(readOnly = true)
    List<String> missing(Instant now) {
        long since = now.toEpochMilli() - MISS_DAYS * DAY;
        long giveUp = now.toEpochMilli() - MISS_GIVE_UP_DAYS * DAY;
        List<Object[]> rows = checkIns.lastVisits();
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Object[] row = rows.get(iter);
            long last = millis(row[1]);
            if (row[0] instanceof String && last < since && last > giveUp) {
                out.add((String) row[0]);
            }
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    boolean miss(String account, Instant now) throws IOException {
        if (engagement.isAway(account, LocalDate.ofEpochDay(now.toEpochMilli() / DAY))
                || nudges.existsByAccountAndKindAndSentAtGreaterThan(account, Notifier.MISS_YOU,
                        now.minusMillis(MISS_AGAIN_DAYS * DAY))
                || !member(account) || !notifier.reachable(account, now)) {
            return false;
        }
        notifier.tell(account, Notifier.MISS_YOU, "", "", "We miss you",
                "It has been a while. Your next class is a tap away.", now);
        return true;
    }

    private boolean member(String account) {
        List<Membership> held = memberships.findByAccountOrderByStartedAtDesc(account);
        for (int iter = 0; iter < held.size(); iter++) {
            if ("ACTIVE".equals(held.get(iter).getState())) {
                return true;
            }
        }
        return false;
    }

    /// Credits the message that brought a member to book a class, once. Call
    /// inside the transaction that booked it.
    public void booked(String account, String sessionId, Instant now) {
        Nudge row = nudges.findFirstByAccountAndSessionIdAndBookedAtOrderBySentAtDesc(account,
                sessionId, Instant.EPOCH);
        if (row != null) {
            row.setBookedAt(now);
            if (row.getCampaignId().length() > 0) {
                campaigns.countBooking(row.getCampaignId());
            }
        }
    }

    /// An aggregate's maximum comes back as whatever the driver makes of a
    /// timestamp column.
    static long millis(Object value) {
        if (value instanceof Instant) {
            return ((Instant) value).toEpochMilli();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0;
    }
}
