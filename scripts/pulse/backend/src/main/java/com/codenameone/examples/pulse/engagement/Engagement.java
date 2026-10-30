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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.pulse.Ids;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.api.AwayDto;
import com.codenameone.examples.pulse.api.HeartRateDto;
import com.codenameone.examples.pulse.api.NudgeDto;
import com.codenameone.examples.pulse.api.StatsDto;
import com.codenameone.examples.pulse.api.WorkoutDto;
import com.codenameone.examples.pulse.billing.Billing;
import com.codenameone.examples.pulse.domain.AwayPeriod;
import com.codenameone.examples.pulse.domain.Booking;
import com.codenameone.examples.pulse.domain.CheckIn;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.domain.MembershipPlan;
import com.codenameone.examples.pulse.domain.Nudge;
import com.codenameone.examples.pulse.domain.Workout;
import com.codenameone.examples.pulse.live.LiveHub;
import com.codenameone.examples.pulse.repository.AwayPeriodRepository;
import com.codenameone.examples.pulse.repository.BookingRepository;
import com.codenameone.examples.pulse.repository.CampaignRepository;
import com.codenameone.examples.pulse.repository.CheckInRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.MembershipPlanRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;
import com.codenameone.examples.pulse.repository.NudgeRepository;
import com.codenameone.examples.pulse.repository.WorkoutRepository;
import com.codenameone.examples.pulse.studio.Bookings;
import com.codenameone.examples.pulse.studio.Household;
import com.codenameone.examples.pulse.studio.Studio;
import com.codenameone.examples.pulse.trainer.HeartRates;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/// What a member does between visits, and what they tell the studio: their
/// visit statistics, their workouts, the heart rate their watch shares in
/// class, the times they are away, and the messages they were sent.
@Component
public class Engagement {
    /// An away period is at most half a year; longer is a cancellation.
    static final int AWAY_MAX_DAYS = 183;
    /// The weeks [StatsDto#weeklyVisits] covers, oldest first.
    static final int WEEKS = 12;
    private static final long DAY = 86400000L;

    private final CheckInRepository checkIns;
    private final WorkoutRepository workouts;
    private final AwayPeriodRepository away;
    private final NudgeRepository nudges;
    private final CampaignRepository campaigns;
    private final BookingRepository bookings;
    private final MemberRepository members;
    private final MembershipRepository memberships;
    private final MembershipPlanRepository plans;
    private final Billing billing;
    private final Studio studio;
    private final HeartRates rates;
    private final LiveHub live;

    public Engagement(CheckInRepository checkIns, WorkoutRepository workouts,
            AwayPeriodRepository away, NudgeRepository nudges, CampaignRepository campaigns,
            BookingRepository bookings, MemberRepository members,
            MembershipRepository memberships, MembershipPlanRepository plans, Billing billing,
            Studio studio, HeartRates rates, LiveHub live) {
        this.checkIns = checkIns;
        this.workouts = workouts;
        this.away = away;
        this.nudges = nudges;
        this.campaigns = campaigns;
        this.bookings = bookings;
        this.members = members;
        this.memberships = memberships;
        this.plans = plans;
        this.billing = billing;
        this.studio = studio;
        this.rates = rates;
        this.live = live;
    }

    // ------------------------------------------------------------- stats

    /// The account's visits and effort: this month, all time, the streak of
    /// weeks with a visit, and a visit count for each of the last
    /// [#WEEKS] weeks.
    @Transactional(readOnly = true)
    public StatsDto stats(String account) {
        long now = System.currentTimeMillis();
        LocalDate today = LocalDate.ofEpochDay(now / DAY);
        Instant month = Instant.ofEpochMilli(today.withDayOfMonth(1).toEpochDay() * DAY);
        long thisWeek = week(today.toEpochDay());
        List<CheckIn> year = checkIns.findByAccountAndAtGreaterThanEqualOrderByAtAsc(account,
                Instant.ofEpochMilli(now - 371L * DAY));
        boolean[] seen = new boolean[54];
        int[] weekly = new int[WEEKS];
        for (int iter = 0; iter < year.size(); iter++) {
            long ago = thisWeek - week(year.get(iter).getAt().toEpochMilli() / DAY);
            if (ago >= 0 && ago < seen.length) {
                seen[(int) ago] = true;
            }
            if (ago >= 0 && ago < WEEKS) {
                weekly[WEEKS - 1 - (int) ago]++;
            }
        }
        List<Integer> weeks = new ArrayList<Integer>();
        for (int iter = 0; iter < WEEKS; iter++) {
            weeks.add(Integer.valueOf(weekly[iter]));
        }
        int minutes = 0;
        int calories = 0;
        List<Workout> done = workouts.findByAccountAndStartedAtGreaterThanEqual(account, month);
        for (int iter = 0; iter < done.size(); iter++) {
            minutes += done.get(iter).getMinutes();
            calories += done.get(iter).getCalories();
        }
        Instant last = checkIns.lastVisit(account);
        return new StatsDto((int) checkIns.countByAccountAndAtGreaterThanEqual(account, month),
                (int) checkIns.countByAccount(account), streak(seen), minutes, calories,
                last == null ? 0 : last.toEpochMilli(), weeks);
    }

    /// Weeks in a row with a visit, counting back from this week -- or from
    /// last week while this one has none yet, so a streak is not lost on a
    /// Monday morning.
    static int streak(boolean[] seen) {
        int at = seen[0] ? 0 : 1;
        int count = 0;
        while (at < seen.length && seen[at]) {
            count++;
            at++;
        }
        return count;
    }

    /// The week a day falls in, counted in Monday-to-Sunday weeks. Day 0,
    /// 1 January 1970, was a Thursday.
    public static long week(long epochDay) {
        return Math.floorDiv(epochDay + 3, 7);
    }

    // ------------------------------------------------------------- away

    @Transactional(readOnly = true)
    public List<AwayDto> away(String account) {
        List<AwayPeriod> rows = away.findByAccountAndEndsOnGreaterThanEqualOrderByStartsOnAsc(account,
                LocalDate.now(ZoneOffset.UTC));
        List<AwayDto> out = new ArrayList<AwayDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter)));
        }
        return out;
    }

    /// Records a time the member will not come: no marketing reaches them
    /// meanwhile, and with `freezeBilling` their memberships are frozen for
    /// it too ([Billing#freeze], which has its own limits).
    @Transactional(rollbackFor = IOException.class)
    public AwayDto addAway(String account, AwayDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Choose the dates");
        }
        LocalDate from = Billing.date(request.startsOn());
        LocalDate until = Billing.date(request.endsOn());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (from.isBefore(today) || until.isBefore(from)
                || until.toEpochDay() - from.toEpochDay() > AWAY_MAX_DAYS) {
            throw new ResponseStatusException(400, "Away starts today or later and lasts at most "
                    + AWAY_MAX_DAYS + " days");
        }
        if (away.findByAccountAndEndsOnGreaterThanEqualOrderByStartsOnAsc(account, today).size()
                >= 10) {
            throw new ResponseStatusException(409, "That is enough time away planned");
        }
        AwayPeriod row = new AwayPeriod();
        row.setId(Ids.next());
        row.setAccount(account);
        row.setStartsOn(from);
        row.setEndsOn(until);
        row.setReason(cut(request.reason() == null ? "" : request.reason().trim(), 120));
        row.setFreezeBilling(request.freezeBilling());
        row.setCreatedAt(Instant.now());
        away.insert(row);
        if (request.freezeBilling()) {
            List<Membership> held = memberships.findByAccountOrderByStartedAtDesc(account);
            int frozen = 0;
            for (int iter = 0; iter < held.size(); iter++) {
                Membership one = held.get(iter);
                MembershipPlan plan = plans.findById(one.getPlanId());
                if (Billing.ACTIVE.equals(one.getState()) && plan != null
                        && !Household.counted(plan)) {
                    // The day after: an away period names the last day away,
                    // a freeze the day billing resumes.
                    billing.freeze(one, from, until.plusDays(1), today);
                    frozen++;
                }
            }
            if (frozen == 0) {
                throw new ResponseStatusException(409, "There is no membership to freeze");
            }
        }
        return toDto(row);
    }

    /// Withdraws an away period. A billing freeze made for it that has not
    /// ended is ended too.
    @Transactional(rollbackFor = IOException.class)
    public AwayDto removeAway(String account, String id) {
        AwayPeriod row = id == null || id.length() > 32 ? null
                : away.findFirstByIdAndAccount(id, account);
        if (row == null) {
            throw new ResponseStatusException(404, "No such time away");
        }
        if (row.isFreezeBilling()) {
            Instant start = Instant.ofEpochMilli(row.getStartsOn().toEpochDay() * DAY);
            List<Membership> held = memberships.findByAccountOrderByStartedAtDesc(account);
            for (int iter = 0; iter < held.size(); iter++) {
                if (held.get(iter).getFrozenFrom().equals(start)) {
                    billing.unfreeze(account, held.get(iter).getId());
                }
            }
        }
        away.deleteById(row.getId());
        return toDto(row);
    }

    /// Whether `account` said they would be away on `day`.
    public boolean isAway(String account, LocalDate day) {
        return away.existsByAccountAndStartsOnLessThanEqualAndEndsOnGreaterThanEqual(account, day,
                day);
    }

    private static AwayDto toDto(AwayPeriod row) {
        return new AwayDto(row.getId(), row.getStartsOn().toString(), row.getEndsOn().toString(),
                row.getReason(), row.isFreezeBilling());
    }

    // ------------------------------------------------------------- inbox

    @Transactional(readOnly = true)
    public List<NudgeDto> nudges(String account) {
        List<Nudge> rows = nudges.findByAccountOrderBySentAtDesc(account);
        List<NudgeDto> out = new ArrayList<NudgeDto>();
        for (int iter = 0; iter < rows.size() && iter < 50; iter++) {
            out.add(toDto(rows.get(iter)));
        }
        return out;
    }

    /// Marks a message read; a campaign counts it opened once.
    @Transactional(rollbackFor = IOException.class)
    public NudgeDto opened(String account, String id) {
        Nudge row = id == null || id.length() > 32 ? null : nudges.findFirstByIdAndAccount(id,
                account);
        if (row == null) {
            throw new ResponseStatusException(404, "No such message");
        }
        if (row.getOpenedAt().equals(Instant.EPOCH)) {
            row.setOpenedAt(Instant.now());
            if (row.getCampaignId().length() > 0) {
                campaigns.countOpen(row.getCampaignId());
            }
        }
        return toDto(row);
    }

    public static NudgeDto toDto(Nudge row) {
        return new NudgeDto(row.getId(), row.getKind(), row.getSessionId(), row.getTitle(),
                row.getBody(), row.getSentAt().toEpochMilli(), row.getOpenedAt().toEpochMilli());
    }

    // ------------------------------------------------------------- workouts

    /// Keeps a workout the member's phone or watch recorded. The same start
    /// sent twice -- a sync that retried -- answers the one already kept.
    @Transactional(rollbackFor = IOException.class)
    public WorkoutDto addWorkout(String account, WorkoutDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Send a workout");
        }
        long now = System.currentTimeMillis();
        if (request.startedAt() <= 0 || request.startedAt() > now + 60000L
                || request.minutes() < 1 || request.minutes() > 600
                || request.avgHr() < 0 || request.avgHr() > 250
                || request.maxHr() < 0 || request.maxHr() > 250
                || request.calories() < 0 || request.calories() > 10000
                || request.zones() != null && request.zones().size() > 5) {
            throw new ResponseStatusException(400, "That workout does not add up");
        }
        Instant started = Instant.ofEpochMilli(request.startedAt());
        List<Workout> mine = workouts.findByAccountAndStartedAtGreaterThanEqual(account, started);
        for (int iter = 0; iter < mine.size(); iter++) {
            if (mine.get(iter).getStartedAt().equals(started)) {
                return toDto(mine.get(iter));
            }
        }
        String sessionId = request.sessionId() == null ? "" : request.sessionId();
        if (sessionId.length() > 0 && studio.sessionOrNull(sessionId) == null) {
            sessionId = "";
        }
        Workout row = new Workout();
        row.setId(Ids.next());
        row.setAccount(account);
        row.setSessionId(sessionId);
        row.setActivity(cut(text(request.activity(), "Workout"), 60));
        row.setStartedAt(started);
        row.setMinutes(request.minutes());
        row.setAvgHr(request.avgHr());
        row.setMaxHr(request.maxHr());
        row.setCalories(request.calories());
        row.setZones(zones(request.zones()));
        row.setSource(cut(text(request.source(), "app"), 30));
        row.setShared(request.shared());
        row.setCreatedAt(Instant.ofEpochMilli(now));
        workouts.insert(row);
        return toDto(row);
    }

    @Transactional(readOnly = true)
    public List<WorkoutDto> workouts(String account) {
        List<Workout> rows = workouts.findByAccountOrderByStartedAtDesc(account);
        List<WorkoutDto> out = new ArrayList<WorkoutDto>();
        for (int iter = 0; iter < rows.size() && iter < 100; iter++) {
            out.add(toDto(rows.get(iter)));
        }
        return out;
    }

    public static WorkoutDto toDto(Workout row) {
        List<Integer> zones = new ArrayList<Integer>();
        String text = row.getZones();
        int at = 0;
        while (at < text.length()) {
            int end = text.indexOf(',', at);
            if (end < 0) {
                end = text.length();
            }
            try {
                zones.add(Integer.valueOf(Integer.parseInt(text.substring(at, end))));
            } catch (NumberFormatException bad) {
                zones.add(Integer.valueOf(0));
            }
            at = end + 1;
        }
        return new WorkoutDto(row.getId(), row.getSessionId(), row.getActivity(),
                row.getStartedAt().toEpochMilli(), row.getMinutes(), row.getAvgHr(),
                row.getMaxHr(), row.getCalories(), zones, row.getSource(), row.isShared());
    }

    private static String zones(List<Integer> minutes) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; minutes != null && iter < minutes.size(); iter++) {
            if (iter > 0) {
                out.append(',');
            }
            Integer value = minutes.get(iter);
            out.append(value == null ? 0 : Math.max(0, Math.min(600, value.intValue())));
        }
        return out.toString();
    }

    // ------------------------------------------------------------- heart rate

    /// One reading from a member's watch during a class they are in, handed
    /// to the trainer's roster. Not stored: see [HeartRates].
    @Transactional(readOnly = true)
    public HeartRateDto heartRate(String account, HeartRateDto reading) {
        if (reading == null || reading.bpm() < 30 || reading.bpm() > 230) {
            throw new ResponseStatusException(400, "That is not a heart rate");
        }
        ClassSession session = studio.sessionOrNull(reading.sessionId());
        long now = System.currentTimeMillis();
        if (session == null || now < session.getStartsAt().toEpochMilli() - 600000L
                || now > session.getEndsAt().toEpochMilli() + 600000L) {
            throw new ResponseStatusException(409, "That class is not under way");
        }
        Member who = null;
        List<Booking> rows = bookings.findBySessionIdOrderByCreatedAtAsc(session.getId());
        for (int iter = 0; iter < rows.size() && who == null; iter++) {
            Booking row = rows.get(iter);
            if (row.getAccount().equals(account) && (Bookings.CHECKED_IN.equals(row.getState())
                    || Bookings.BOOKED.equals(row.getState()))) {
                Member member = members.findById(row.getMemberId());
                // A parent's watch reads the parent: their own place, when
                // they have one, is the one the reading belongs to.
                if (member != null && Accounts.SELF.equals(member.getRelation())) {
                    who = member;
                }
            }
        }
        if (who == null) {
            throw new ResponseStatusException(409, "You are not in that class");
        }
        int zone = HeartRates.zone(reading.bpm(), Accounts.age(who.getBirthDate(),
                LocalDate.ofEpochDay(now / DAY)));
        rates.put(session.getId(), who.getId(), reading.bpm(), zone, now);
        live.heartRate(session.getTrainer(), session.getId(), who.getId(), reading.bpm(), zone);
        return reading;
    }

    private static String text(String value, String otherwise) {
        return value == null || value.trim().length() == 0 ? otherwise : value.trim();
    }

    private static String cut(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }
}
