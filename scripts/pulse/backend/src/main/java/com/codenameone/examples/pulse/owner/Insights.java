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
package com.codenameone.examples.pulse.owner;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.pulse.Days;
import com.codenameone.examples.pulse.api.CoachStatDto;
import com.codenameone.examples.pulse.api.InsightsDto;
import com.codenameone.examples.pulse.api.InvoiceDto;
import com.codenameone.examples.pulse.api.MemberDetailDto;
import com.codenameone.examples.pulse.api.NameCountDto;
import com.codenameone.examples.pulse.api.OwnerMemberDto;
import com.codenameone.examples.pulse.api.PlanMixDto;
import com.codenameone.examples.pulse.api.TypeFillDto;
import com.codenameone.examples.pulse.api.WeekPointDto;
import com.codenameone.examples.pulse.billing.Billing;
import com.codenameone.examples.pulse.domain.Booking;
import com.codenameone.examples.pulse.domain.CheckIn;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.ClassType;
import com.codenameone.examples.pulse.domain.Invoice;
import com.codenameone.examples.pulse.domain.Location;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.domain.MembershipPlan;
import com.codenameone.examples.pulse.domain.Staff;
import com.codenameone.examples.pulse.engagement.Engagement;
import com.codenameone.examples.pulse.repository.BookingRepository;
import com.codenameone.examples.pulse.repository.CheckInRepository;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.ClassTypeRepository;
import com.codenameone.examples.pulse.repository.InvoiceRepository;
import com.codenameone.examples.pulse.repository.LocationRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.MembershipPlanRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;
import com.codenameone.examples.pulse.repository.StaffRepository;
import com.codenameone.examples.pulse.studio.Bookings;
import com.codenameone.examples.pulse.studio.Household;
import com.codenameone.examples.pulse.studio.Studio;
import com.codenameone.examples.pulse.trainer.Trainers;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The owner's longer view: twelve weeks of growth, churn and attendance,
/// how each kind of class fills, and the member list with each member's
/// story. Weeks start on Monday and hours are read on the clock of the
/// studio's own time zone, never the server's.
@Component
public class Insights {
    public static final String AT_RISK = "AT_RISK";
    public static final String NEW = "NEW";
    static final int WEEKS = 12;
    private static final int BUSY_WEEKS = 8;
    private static final int RECENT_DAYS = 28;

    private final MembershipRepository memberships;
    private final MembershipPlanRepository plans;
    private final InvoiceRepository invoices;
    private final ClassSessionRepository sessions;
    private final ClassTypeRepository types;
    private final BookingRepository bookings;
    private final CheckInRepository checkIns;
    private final MemberRepository members;
    private final LocationRepository locations;
    private final StaffRepository staff;
    private final Engagement engagement;
    private final Studio studio;
    private final Trainers trainers;
    private final Billing billing;

    public Insights(MembershipRepository memberships, MembershipPlanRepository plans,
            InvoiceRepository invoices, ClassSessionRepository sessions, ClassTypeRepository types,
            BookingRepository bookings, CheckInRepository checkIns, MemberRepository members,
            LocationRepository locations, StaffRepository staff, Engagement engagement,
            Studio studio, Trainers trainers, Billing billing) {
        this.memberships = memberships;
        this.plans = plans;
        this.invoices = invoices;
        this.sessions = sessions;
        this.types = types;
        this.bookings = bookings;
        this.checkIns = checkIns;
        this.members = members;
        this.locations = locations;
        this.staff = staff;
        this.engagement = engagement;
        this.studio = studio;
        this.trainers = trainers;
        this.billing = billing;
    }

    // -------------------------------------------------------------- insights

    @Transactional(readOnly = true)
    public InsightsDto insights() {
        long now = System.currentTimeMillis();
        ZoneId zone = studio.zone();
        LocalDate firstWeek = Studio.monday(LocalDate.now(zone)).minusWeeks(WEEKS - 1);
        long from = firstWeek.atStartOfDay(zone).toInstant().toEpochMilli();
        List<CheckIn> visits = checkIns.findByAtGreaterThanEqual(Instant.ofEpochMilli(from));
        List<Membership> all = memberships.findAll();

        // Busy hours: the opening hours of the earliest and the latest site.
        int firstHour = 24;
        int lastHour = 0;
        List<Location> sites = locations.findAll();
        for (int iter = 0; iter < sites.size(); iter++) {
            firstHour = Math.min(firstHour, sites.get(iter).getOpensMinute() / 60);
            lastHour = Math.max(lastHour, (sites.get(iter).getClosesMinute() + 59) / 60);
        }
        if (firstHour >= lastHour) {
            firstHour = 6;
            lastHour = 22;
        }
        int span = lastHour - firstHour;
        int[] busy = new int[7 * span];
        long busyFrom = now - BUSY_WEEKS * 7L * Days.MILLIS;
        int[] weekVisits = new int[WEEKS];
        int recentVisits = 0;
        for (int iter = 0; iter < visits.size(); iter++) {
            Instant at = visits.get(iter).getAt();
            ZonedDateTime local = at.atZone(zone);
            int week = (int) ((local.toLocalDate().toEpochDay() - firstWeek.toEpochDay()) / 7);
            if (week >= 0 && week < WEEKS) {
                weekVisits[week]++;
            }
            if (at.toEpochMilli() >= now - RECENT_DAYS * Days.MILLIS) {
                recentVisits++;
            }
            int hour = local.getHour();
            if (at.toEpochMilli() >= busyFrom && hour >= firstHour && hour < lastHour) {
                busy[(local.getDayOfWeek().getValue() - 1) * span + hour - firstHour]++;
            }
        }
        List<Integer> busyHours = new ArrayList<Integer>();
        for (int iter = 0; iter < busy.length; iter++) {
            busyHours.add(Integer.valueOf(busy[iter]));
        }

        // Growth: what was running at the end of each week, the current one
        // up to now.
        List<WeekPointDto> weeks = new ArrayList<WeekPointDto>();
        for (int week = 0; week < WEEKS; week++) {
            LocalDate monday = firstWeek.plusWeeks(week);
            long start = monday.atStartOfDay(zone).toInstant().toEpochMilli();
            long end = Math.min(now, monday.plusWeeks(1).atStartOfDay(zone).toInstant()
                    .toEpochMilli());
            int active = 0;
            int joined = 0;
            int left = 0;
            for (int iter = 0; iter < all.size(); iter++) {
                Membership row = all.get(iter);
                if (!counted(row)) {
                    continue;
                }
                long started = row.getStartedAt().toEpochMilli();
                long ended = ended(row);
                if (started < end && ended >= end) {
                    active++;
                }
                if (started >= start && started < end) {
                    joined++;
                }
                if (ended >= start && ended < end) {
                    left++;
                }
            }
            weeks.add(new WeekPointDto(monday.toString(), active, joined, left,
                    weekVisits[week]));
        }

        // Plans, revenue and the members behind them, as the dashboard counts
        // monthly recurring revenue.
        Map<String, long[]> byPlan = new HashMap<String, long[]>();
        int running = 0;
        long mrr = 0;
        int cohort = 0;
        int stayed = 0;
        for (int iter = 0; iter < all.size(); iter++) {
            Membership row = all.get(iter);
            long started = row.getStartedAt().toEpochMilli();
            if (counted(row) && started <= now - 90L * Days.MILLIS
                    && started > now - 180L * Days.MILLIS) {
                cohort++;
                if (Billing.ACTIVE.equals(row.getState()) || Billing.FROZEN.equals(row.getState())
                        || Billing.PAST_DUE.equals(row.getState())) {
                    stayed++;
                }
            }
            if (!running(row)) {
                continue;
            }
            long[] mix = byPlan.get(row.getPlanId());
            if (mix == null) {
                mix = new long[2];
                byPlan.put(row.getPlanId(), mix);
            }
            mix[0]++;
            MembershipPlan plan = plans.findById(row.getPlanId());
            if (plan != null && !Household.counted(plan) && plan.getPeriodMonths() > 0
                    && !Billing.FROZEN.equals(row.getState())) {
                long monthly = plan.getPriceCents() / plan.getPeriodMonths();
                mix[1] += monthly;
                mrr += monthly;
            }
            if (!Billing.FROZEN.equals(row.getState())) {
                running++;
            }
        }
        List<PlanMixDto> mixes = new ArrayList<PlanMixDto>();
        for (Map.Entry<String, long[]> entry : byPlan.entrySet()) {
            MembershipPlan plan = plans.findById(entry.getKey());
            mixes.add(new PlanMixDto(plan == null ? entry.getKey() : plan.getName(),
                    plan == null ? "" : plan.getKind(), (int) entry.getValue()[0],
                    entry.getValue()[1]));
        }
        mixes.sort((a, b) -> b.members() - a.members());

        // Classes: the last four weeks of each kind.
        List<ClassSession> month = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                Instant.ofEpochMilli(now - RECENT_DAYS * Days.MILLIS), Instant.ofEpochMilli(now));
        Map<String, long[]> byType = new HashMap<String, long[]>();
        Map<String, String> typeOf = new HashMap<String, String>();
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < month.size(); iter++) {
            ClassSession row = month.get(iter);
            if (Studio.CANCELLED.equals(row.getState()) || row.getEndsAt().toEpochMilli() > now) {
                continue;
            }
            long[] fill = tally(byType, row.getClassTypeId());
            fill[0]++;
            fill[1] += row.getBooked();
            fill[2] += row.getCapacity();
            typeOf.put(row.getId(), row.getClassTypeId());
            ids.add(row.getId());
        }
        long came = 0;
        long missed = 0;
        List<Booking> booked = ids.isEmpty() ? new ArrayList<Booking>() : bookings.findBySessionIdIn(ids);
        for (int iter = 0; iter < booked.size(); iter++) {
            Booking row = booked.get(iter);
            long[] fill = byType.get(typeOf.get(row.getSessionId()));
            if (Bookings.CHECKED_IN.equals(row.getState())) {
                came++;
                fill[3]++;
            } else if (Bookings.NO_SHOW.equals(row.getState())) {
                missed++;
                fill[4]++;
            }
        }
        List<TypeFillDto> classes = new ArrayList<TypeFillDto>();
        for (Map.Entry<String, long[]> entry : byType.entrySet()) {
            ClassType type = types.findById(entry.getKey());
            long[] fill = entry.getValue();
            classes.add(new TypeFillDto(type == null ? entry.getKey() : type.getName(),
                    type == null ? "" : type.getColor(), (int) fill[0],
                    percent(fill[1], fill[2]), percent(fill[4], fill[3] + fill[4])));
        }
        classes.sort((a, b) -> b.fillPercent() - a.fillPercent());

        // Coaches: every active trainer who taught in the four weeks.
        List<CoachStatDto> coaches = new ArrayList<CoachStatDto>();
        List<Staff> team = staff.findByActiveTrueOrderByUsernameAsc();
        for (int iter = 0; iter < team.size(); iter++) {
            CoachStatDto coach = trainers.coach(team.get(iter).getUsername());
            if (coach.classes() > 0) {
                coaches.add(coach);
            }
        }
        coaches.sort((a, b) -> b.attendees() - a.attendees());

        return new InsightsDto(firstHour, lastHour, busyHours, weeks, classes, mixes, coaches,
                percent(stayed, cohort),
                running == 0 ? 0 : (int) (recentVisits * 10L * 7 / RECENT_DAYS / running),
                percent(missed, came + missed), running == 0 ? 0 : mrr / running);
    }

    // --------------------------------------------------------------- members

    /// Running memberships, or those `filter` names, by member name.
    @Transactional(readOnly = true)
    public List<OwnerMemberDto> members(String filter) {
        String wanted = filter == null ? "" : filter.trim();
        long now = System.currentTimeMillis();
        List<Membership> rows = memberships.findAll();
        List<Membership> chosen = new ArrayList<Membership>();
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Membership row = rows.get(iter);
            boolean shown = Billing.CANCELLED.equalsIgnoreCase(wanted)
                    ? Billing.CANCELLED.equals(row.getState()) : running(row);
            if (shown) {
                chosen.add(row);
                ids.add(row.getMemberId());
            }
        }
        Map<String, Member> names = new HashMap<String, Member>();
        if (!ids.isEmpty()) {
            List<Member> found = members.findByIdIn(ids);
            for (int iter = 0; iter < found.size(); iter++) {
                names.put(found.get(iter).getId(), found.get(iter));
            }
        }
        Map<String, Long> last = lastVisits();
        Map<String, int[]> recent = new HashMap<String, int[]>();
        List<CheckIn> month = checkIns.findByAtGreaterThanEqual(
                Instant.ofEpochMilli(now - 30L * Days.MILLIS));
        for (int iter = 0; iter < month.size(); iter++) {
            tallyInt(recent, month.get(iter).getMemberId());
        }
        LocalDate today = LocalDate.now(studio.zone());
        Map<String, Boolean> away = new HashMap<String, Boolean>();
        List<OwnerMemberDto> out = new ArrayList<OwnerMemberDto>();
        for (int iter = 0; iter < chosen.size(); iter++) {
            Membership row = chosen.get(iter);
            OwnerMemberDto dto = dto(row, names.get(row.getMemberId()), last, recent, away,
                    today, now);
            if (AT_RISK.equalsIgnoreCase(wanted) && !dto.atRisk()
                    || NEW.equalsIgnoreCase(wanted)
                            && dto.joinedAt() < now - 30L * Days.MILLIS
                    || (Billing.PAST_DUE.equalsIgnoreCase(wanted)
                            || Billing.FROZEN.equalsIgnoreCase(wanted))
                            && !wanted.equalsIgnoreCase(dto.state())) {
                continue;
            }
            out.add(dto);
        }
        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return out;
    }

    /// One membership's member: their visits a week, the classes they come
    /// to and what they have paid.
    @Transactional(readOnly = true)
    public MemberDetailDto member(String id) {
        Membership row = id == null ? null : memberships.findById(id);
        if (row == null) {
            throw new ResponseStatusException(404, "No such membership");
        }
        long now = System.currentTimeMillis();
        Member member = members.findById(row.getMemberId());
        Map<String, Long> last = lastVisits();
        Map<String, int[]> recent = new HashMap<String, int[]>();
        ZoneId zone = studio.zone();
        LocalDate thisWeek = Studio.monday(LocalDate.now(zone));
        // A year of visits: twelve weeks are drawn, and a streak may be longer.
        LocalDate yearAgo = thisWeek.minusWeeks(52);
        List<CheckIn> visits = checkIns.findByMemberIdAndAtGreaterThanEqualOrderByAtAsc(
                row.getMemberId(), yearAgo.atStartOfDay(zone).toInstant());
        int[] byWeek = new int[53];
        for (int iter = 0; iter < visits.size(); iter++) {
            CheckIn visit = visits.get(iter);
            if (visit.getAt().toEpochMilli() >= now - 30L * Days.MILLIS) {
                tallyInt(recent, visit.getMemberId());
            }
            LocalDate day = visit.getAt().atZone(zone).toLocalDate();
            int week = (int) ((day.toEpochDay() - yearAgo.toEpochDay()) / 7);
            if (week >= 0 && week < byWeek.length) {
                byWeek[week]++;
            }
        }
        List<Integer> visitsByWeek = new ArrayList<Integer>();
        for (int week = byWeek.length - WEEKS; week < byWeek.length; week++) {
            visitsByWeek.add(Integer.valueOf(byWeek[week]));
        }
        // A week not yet over does not break a streak; it only adds to one.
        int streak = 0;
        int week = byWeek.length - 1;
        if (byWeek[week] == 0) {
            week--;
        }
        while (week >= 0 && byWeek[week] > 0) {
            streak++;
            week--;
        }
        if (byWeek[byWeek.length - 1] > 0 && streak == 0) {
            streak = 1;
        }

        Map<String, int[]> byType = new HashMap<String, int[]>();
        List<Booking> came = bookings.findByMemberIdAndStateOrderByCheckedInAtDesc(
                row.getMemberId(), Bookings.CHECKED_IN);
        List<String> sessionIds = new ArrayList<String>();
        for (int iter = 0; iter < came.size(); iter++) {
            sessionIds.add(came.get(iter).getSessionId());
        }
        if (!sessionIds.isEmpty()) {
            List<ClassSession> taken = sessions.findByIdIn(sessionIds);
            for (int iter = 0; iter < taken.size(); iter++) {
                tallyInt(byType, taken.get(iter).getClassTypeId());
            }
        }
        List<NameCountDto> favourites = new ArrayList<NameCountDto>();
        for (Map.Entry<String, int[]> entry : byType.entrySet()) {
            ClassType type = types.findById(entry.getKey());
            favourites.add(new NameCountDto(type == null ? entry.getKey() : type.getName(),
                    entry.getValue()[0]));
        }
        favourites.sort((a, b) -> b.count() - a.count());

        List<Invoice> bills = invoices.findByMemberIdOrderByCreatedAtDesc(row.getMemberId());
        List<InvoiceDto> shown = new ArrayList<InvoiceDto>();
        long lifetime = 0;
        for (int iter = 0; iter < bills.size(); iter++) {
            Invoice bill = bills.get(iter);
            if (Billing.PAID.equals(bill.getStatus())) {
                lifetime += bill.getAmountCents() - bill.getCreditCents();
            }
            shown.add(billing.toDto(bill));
        }
        OwnerMemberDto summary = dto(row, member, last, recent, new HashMap<String, Boolean>(),
                LocalDate.now(zone), now);
        return new MemberDetailDto(summary, visitsByWeek, favourites, streak, lifetime, shown);
    }

    private OwnerMemberDto dto(Membership row, Member member, Map<String, Long> last,
            Map<String, int[]> recent, Map<String, Boolean> away, LocalDate today, long now) {
        MembershipPlan plan = plans.findById(row.getPlanId());
        Long seen = last.get(row.getMemberId());
        long lastVisit = seen == null ? 0 : seen.longValue();
        Boolean isAway = away.get(row.getAccount());
        if (isAway == null) {
            isAway = Boolean.valueOf(engagement.isAway(row.getAccount(), today));
            away.put(row.getAccount(), isAway);
        }
        // Someone who joined this week has not been gone two weeks: a member
        // never seen counts from the day they joined.
        long since = Math.max(lastVisit, row.getStartedAt().toEpochMilli());
        // A frozen membership is a pause the member asked for, not a drift.
        boolean atRisk = (Billing.ACTIVE.equals(row.getState())
                || Billing.PAST_DUE.equals(row.getState())) && since < now - 14L * Days.MILLIS
                && !isAway.booleanValue();
        int[] visits = recent.get(row.getMemberId());
        return new OwnerMemberDto(row.getId(), row.getAccount(),
                member == null ? row.getAccount() : member.getName(),
                plan == null ? "" : plan.getName(), row.getState(),
                row.getStartedAt().toEpochMilli(), lastVisit, visits == null ? 0 : visits[0],
                atRisk, isAway.booleanValue());
    }

    /// When each member last came in.
    private Map<String, Long> lastVisits() {
        List<Object[]> rows = checkIns.lastVisitsByMember();
        Map<String, Long> out = new HashMap<String, Long>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Object value = rows.get(iter)[1];
            long millis = value instanceof Instant ? ((Instant) value).toEpochMilli()
                    : value instanceof Number ? ((Number) value).longValue() : 0;
            out.put(String.valueOf(rows.get(iter)[0]), Long.valueOf(millis));
        }
        return out;
    }

    private static boolean running(Membership row) {
        return Billing.ACTIVE.equals(row.getState()) || Billing.FROZEN.equals(row.getState())
                || Billing.PAST_DUE.equals(row.getState());
    }

    /// A membership that ever ran: not one still waiting for its first payment.
    private static boolean counted(Membership row) {
        return !Billing.PENDING.equals(row.getState());
    }

    /// When a membership stopped running: the last change of a cancelled or
    /// expired one, never for the rest.
    private static long ended(Membership row) {
        return Billing.CANCELLED.equals(row.getState()) || Billing.EXPIRED.equals(row.getState())
                ? row.getUpdatedAt().toEpochMilli() : Long.MAX_VALUE;
    }

    private static int percent(long part, long whole) {
        return whole <= 0 ? 0 : (int) Math.min(100L, part * 100 / whole);
    }

    private static long[] tally(Map<String, long[]> counts, String key) {
        long[] count = counts.get(key);
        if (count == null) {
            count = new long[5];
            counts.put(key, count);
        }
        return count;
    }

    private static void tallyInt(Map<String, int[]> counts, String key) {
        int[] count = counts.get(key);
        if (count == null) {
            count = new int[1];
            counts.put(key, count);
        }
        count[0]++;
    }
}
