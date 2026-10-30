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
import com.codenameone.examples.pulse.Ids;
import com.codenameone.examples.pulse.Roles;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.api.CampaignDto;
import com.codenameone.examples.pulse.api.ClassTypeDto;
import com.codenameone.examples.pulse.api.DayAmountDto;
import com.codenameone.examples.pulse.api.NameCountDto;
import com.codenameone.examples.pulse.api.NewSessionDto;
import com.codenameone.examples.pulse.api.OwnerDashboardDto;
import com.codenameone.examples.pulse.api.PlanDto;
import com.codenameone.examples.pulse.api.RoomDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.api.ShiftDto;
import com.codenameone.examples.pulse.api.StaffDto;
import com.codenameone.examples.pulse.api.UserDto;
import com.codenameone.examples.pulse.billing.Billing;
import com.codenameone.examples.pulse.domain.Booking;
import com.codenameone.examples.pulse.domain.Campaign;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.ClassType;
import com.codenameone.examples.pulse.domain.Invoice;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.domain.MembershipPlan;
import com.codenameone.examples.pulse.domain.Profile;
import com.codenameone.examples.pulse.domain.Room;
import com.codenameone.examples.pulse.domain.Shift;
import com.codenameone.examples.pulse.domain.Staff;
import com.codenameone.examples.pulse.engagement.Engagement;
import com.codenameone.examples.pulse.live.LiveHub;
import com.codenameone.examples.pulse.push.Notifier;
import com.codenameone.examples.pulse.repository.BookingRepository;
import com.codenameone.examples.pulse.repository.CampaignRepository;
import com.codenameone.examples.pulse.repository.CheckInRepository;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.ClassTypeRepository;
import com.codenameone.examples.pulse.repository.InvoiceRepository;
import com.codenameone.examples.pulse.repository.LocationRepository;
import com.codenameone.examples.pulse.repository.MembershipPlanRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;
import com.codenameone.examples.pulse.repository.ProfileRepository;
import com.codenameone.examples.pulse.repository.RoomRepository;
import com.codenameone.examples.pulse.repository.ShiftRepository;
import com.codenameone.examples.pulse.repository.StaffRepository;
import com.codenameone.examples.pulse.studio.Bookings;
import com.codenameone.examples.pulse.studio.Household;
import com.codenameone.examples.pulse.studio.Studio;
import com.codenameone.examples.pulse.trainer.Trainers;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Running the studio: the numbers, the plans and classes on offer, the
/// timetable, the staff and their shifts, and the campaigns that fill it.
@Component
public class Owner {
    /// Plan kinds; [Household#counted] are the two that are used up.
    static final List<String> PLAN_KINDS = Arrays.asList("MONTHLY", "ANNUAL", "CLASS_PACK",
            "DROP_IN");
    /// Campaign audiences: those who took the class's kind, every member
    /// with an active membership, or those who have not been in for a month.
    public static final String CLASS = "CLASS";
    public static final String ALL = "ALL";
    public static final String LAPSED = "LAPSED";
    static final int MOST_WEEKS = 12;
    static final int MOST_RECIPIENTS = 5000;
    private static final long HOUR = 3600000L;
    private static final long DAY = 24L * HOUR;

    private final MembershipRepository memberships;
    private final MembershipPlanRepository plans;
    private final InvoiceRepository invoices;
    private final ClassSessionRepository sessions;
    private final ClassTypeRepository types;
    private final RoomRepository rooms;
    private final LocationRepository locations;
    private final StaffRepository staff;
    private final ShiftRepository shifts;
    private final BookingRepository bookings;
    private final CheckInRepository checkIns;
    private final CampaignRepository campaigns;
    private final ProfileRepository profiles;
    private final Accounts accounts;
    private final Household household;
    private final Studio studio;
    private final Trainers trainers;
    private final Engagement engagement;
    private final Notifier notifier;
    private final LiveHub live;

    public Owner(MembershipRepository memberships, MembershipPlanRepository plans,
            InvoiceRepository invoices, ClassSessionRepository sessions, ClassTypeRepository types,
            RoomRepository rooms, LocationRepository locations, StaffRepository staff,
            ShiftRepository shifts, BookingRepository bookings, CheckInRepository checkIns,
            CampaignRepository campaigns, ProfileRepository profiles, Accounts accounts,
            Household household, Studio studio, Trainers trainers, Engagement engagement,
            Notifier notifier, LiveHub live) {
        this.memberships = memberships;
        this.plans = plans;
        this.invoices = invoices;
        this.sessions = sessions;
        this.types = types;
        this.rooms = rooms;
        this.locations = locations;
        this.staff = staff;
        this.shifts = shifts;
        this.bookings = bookings;
        this.checkIns = checkIns;
        this.campaigns = campaigns;
        this.profiles = profiles;
        this.accounts = accounts;
        this.household = household;
        this.studio = studio;
        this.trainers = trainers;
        this.engagement = engagement;
        this.notifier = notifier;
        this.live = live;
    }

    // ------------------------------------------------------------- dashboard

    /// The studio on one screen. Monthly recurring revenue counts what each
    /// running membership brings in a month -- an annual plan a twelfth of
    /// its price -- and leaves packs out, which are paid once.
    @Transactional(readOnly = true)
    public OwnerDashboardDto dashboard() throws IOException {
        long now = System.currentTimeMillis();
        Instant month = Instant.ofEpochMilli(now - 30L * DAY);
        List<Membership> running = memberships.findByStateIn(Arrays.asList(Billing.ACTIVE,
                Billing.FROZEN, Billing.PAST_DUE));
        Map<String, MembershipPlan> planById = new HashMap<String, MembershipPlan>();
        long mrr = 0;
        int pastDue = 0;
        Set<String> members = new HashSet<String>();
        for (int iter = 0; iter < running.size(); iter++) {
            Membership row = running.get(iter);
            members.add(row.getAccount());
            if (Billing.PAST_DUE.equals(row.getState())) {
                pastDue++;
            }
            MembershipPlan plan = planById.get(row.getPlanId());
            if (plan == null) {
                plan = plans.findById(row.getPlanId());
                if (plan == null) {
                    continue;
                }
                planById.put(plan.getId(), plan);
            }
            if (!Household.counted(plan) && plan.getPeriodMonths() > 0
                    && !Billing.FROZEN.equals(row.getState())) {
                mrr += plan.getPriceCents() / plan.getPeriodMonths();
            }
        }
        List<Invoice> paid = invoices.findByStatusAndPaidAtGreaterThanEqual(Billing.PAID, month);
        Map<String, long[]> byDay = new HashMap<String, long[]>();
        long revenue = 0;
        for (int iter = 0; iter < paid.size(); iter++) {
            Invoice bill = paid.get(iter);
            long amount = bill.getAmountCents() - bill.getCreditCents();
            revenue += amount;
            String day = LocalDate.ofEpochDay(bill.getPaidAt().toEpochMilli() / DAY).toString();
            long[] total = byDay.get(day);
            if (total == null) {
                total = new long[2];
                byDay.put(day, total);
            }
            total[0] += amount;
            total[1]++;
        }
        List<DayAmountDto> days = new ArrayList<DayAmountDto>();
        for (int iter = 29; iter >= 0; iter--) {
            String day = LocalDate.ofEpochDay((now - iter * DAY) / DAY).toString();
            long[] total = byDay.get(day);
            days.add(new DayAmountDto(day, total == null ? 0 : total[0],
                    total == null ? 0 : (int) total[1]));
        }
        return new OwnerDashboardDto(members.size(),
                (int) memberships.countByStartedAtGreaterThanEqual(month),
                (int) memberships.countByStateAndUpdatedAtGreaterThanEqual(Billing.CANCELLED, month),
                mrr, revenue, utilisation(now), (int) checkIns.countByAtGreaterThanEqual(
                        Instant.ofEpochMilli(now - 7L * DAY)), pastDue, atRisk(members, now),
                days, popular(now), underfilled(now));
    }

    /// Places taken of places offered in the last week's classes, in percent.
    private int utilisation(long now) {
        List<ClassSession> week = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                Instant.ofEpochMilli(now - 7L * DAY), Instant.ofEpochMilli(now));
        long taken = 0;
        long offered = 0;
        for (int iter = 0; iter < week.size(); iter++) {
            ClassSession row = week.get(iter);
            if (!Studio.CANCELLED.equals(row.getState())) {
                taken += row.getBooked();
                offered += row.getCapacity();
            }
        }
        return offered == 0 ? 0 : (int) (taken * 100 / offered);
    }

    /// Members who have not been in for two weeks and did not say they
    /// would be away.
    private int atRisk(Set<String> members, long now) {
        List<Object[]> last = checkIns.lastVisits();
        Map<String, Long> seen = new HashMap<String, Long>();
        for (int iter = 0; iter < last.size(); iter++) {
            seen.put(String.valueOf(last.get(iter)[0]), Long.valueOf(millis(last.get(iter)[1])));
        }
        LocalDate today = LocalDate.ofEpochDay(now / DAY);
        int count = 0;
        for (String account : members) {
            Long when = seen.get(account);
            if ((when == null || when.longValue() < now - 14L * DAY)
                    && !engagement.isAway(account, today)) {
                count++;
            }
        }
        return count;
    }

    private static long millis(Object value) {
        if (value instanceof Instant) {
            return ((Instant) value).toEpochMilli();
        }
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }

    /// The five kinds of class most booked in the last month.
    private List<NameCountDto> popular(long now) {
        List<ClassSession> month = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                Instant.ofEpochMilli(now - 30L * DAY), Instant.ofEpochMilli(now));
        Map<String, int[]> byType = new HashMap<String, int[]>();
        for (int iter = 0; iter < month.size(); iter++) {
            ClassSession row = month.get(iter);
            int[] count = byType.get(row.getClassTypeId());
            if (count == null) {
                count = new int[1];
                byType.put(row.getClassTypeId(), count);
            }
            count[0] += row.getBooked();
        }
        List<NameCountDto> out = new ArrayList<NameCountDto>();
        for (Map.Entry<String, int[]> entry : byType.entrySet()) {
            ClassType type = types.findById(entry.getKey());
            out.add(new NameCountDto(type == null ? entry.getKey() : type.getName(),
                    entry.getValue()[0]));
        }
        out.sort((a, b) -> b.count() - a.count());
        return out.size() > 5 ? new ArrayList<NameCountDto>(out.subList(0, 5)) : out;
    }

    /// Classes in the next two days under half full.
    private List<SessionDto> underfilled(long now) throws IOException {
        List<ClassSession> soon = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                Instant.ofEpochMilli(now), Instant.ofEpochMilli(now + 2L * DAY));
        List<ClassSession> out = new ArrayList<ClassSession>();
        for (int iter = 0; iter < soon.size(); iter++) {
            ClassSession row = soon.get(iter);
            if (Studio.SCHEDULED.equals(row.getState()) && row.getBooked() * 2 < row.getCapacity()) {
                out.add(row);
            }
        }
        return studio.describe("", out);
    }

    // ------------------------------------------------------------- catalogue

    /// Adds a plan, or changes one. A change reaches members at their next
    /// renewal, never the period they paid for; a retired plan
    /// (`active` false) is offered to nobody new and renews for those on it.
    @Transactional(rollbackFor = IOException.class)
    public PlanDto savePlan(PlanDto wanted) throws IOException {
        if (wanted == null || blank(wanted.name()) || !PLAN_KINDS.contains(wanted.kind())
                || wanted.priceCents() < 0 || wanted.priceCents() > 10000000L
                || wanted.minAge() < 0 || wanted.maxAge() < 0
                || wanted.maxAge() > 0 && wanted.maxAge() < wanted.minAge()
                || blank(wanted.currency()) || wanted.currency().trim().length() != 3) {
            throw new ResponseStatusException(400, "A plan needs a name, a kind, a price and a"
                    + " currency, and an age band that makes sense");
        }
        boolean counted = "CLASS_PACK".equals(wanted.kind()) || "DROP_IN".equals(wanted.kind());
        if (counted ? wanted.classCredits() < 1 || wanted.classCredits() > 200
                : wanted.periodMonths() < 1 || wanted.periodMonths() > 24) {
            throw new ResponseStatusException(400, counted ? "A pack holds 1 to 200 classes"
                    : "A plan renews every 1 to 24 months");
        }
        MembershipPlan row = blank(wanted.id()) ? null : plans.findById(wanted.id());
        boolean fresh = row == null;
        if (fresh) {
            row = new MembershipPlan();
            row.setId(Ids.next());
        } else if (!row.getKind().equals(wanted.kind())) {
            throw new ResponseStatusException(409, "A plan's kind cannot change; add a new plan");
        }
        row.setName(cut(wanted.name().trim(), 60));
        row.setDescription(cut(wanted.description() == null ? "" : wanted.description().trim(),
                400));
        row.setKind(wanted.kind());
        row.setPriceCents(wanted.priceCents());
        row.setCurrency(wanted.currency().trim());
        row.setPeriodMonths(counted ? 0 : wanted.periodMonths());
        row.setClassCredits(counted ? wanted.classCredits() : 0);
        row.setMinAge(wanted.minAge());
        row.setMaxAge(wanted.maxAge());
        row.setOffPeakOnly(wanted.offPeakOnly());
        row.setActive(wanted.active());
        if (fresh) {
            plans.insert(row);
        }
        return Billing.plan(row);
    }

    @Transactional(rollbackFor = IOException.class)
    public ClassTypeDto saveClassType(ClassTypeDto wanted) throws IOException {
        if (wanted == null || blank(wanted.name()) || wanted.minutes() < 10
                || wanted.minutes() > 240 || wanted.capacity() < 1 || wanted.capacity() > 200
                || wanted.intensity() < 1 || wanted.intensity() > 5 || wanted.minAge() < 0
                || wanted.maxAge() < 0 || wanted.maxAge() > 0 && wanted.maxAge() < wanted.minAge()) {
            throw new ResponseStatusException(400, "A class needs a name, 10 to 240 minutes, a"
                    + " capacity, an intensity of 1 to 5 and an age band that makes sense");
        }
        ClassType row = blank(wanted.id()) ? null : types.findById(wanted.id());
        boolean fresh = row == null;
        if (fresh) {
            row = new ClassType();
            row.setId(Ids.next());
        }
        row.setName(cut(wanted.name().trim(), 60));
        row.setDescription(cut(text(wanted.description()), 400));
        row.setCategory(cut(text(wanted.category()), 30));
        row.setIntensity(wanted.intensity());
        row.setMinAge(wanted.minAge());
        row.setMaxAge(wanted.maxAge());
        row.setCapacity(wanted.capacity());
        row.setMinutes(wanted.minutes());
        row.setColor(cut(text(wanted.color()), 9));
        row.setActive(wanted.active());
        if (fresh) {
            types.insert(row);
        }
        return Studio.type(row);
    }

    @Transactional(readOnly = true)
    public List<RoomDto> rooms() {
        List<Room> rows = rooms.findAllByOrderByLocationIdAscNameAsc();
        List<RoomDto> out = new ArrayList<RoomDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Room row = rows.get(iter);
            out.add(new RoomDto(row.getId(), row.getLocationId(), row.getName(), row.getCapacity()));
        }
        return out;
    }

    // ------------------------------------------------------------- timetable

    /// Puts a class on the timetable, the same time each week for `weeks`
    /// weeks. Nothing is added when any week clashes with a class in the
    /// same room or with the same trainer; the answer names the first.
    @Transactional(rollbackFor = IOException.class)
    public List<SessionDto> schedule(NewSessionDto wanted) throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "Describe the class");
        }
        ClassType type = blank(wanted.classTypeId()) ? null : types.findById(wanted.classTypeId());
        Room room = blank(wanted.roomId()) ? null : rooms.findById(wanted.roomId());
        if (type == null || !type.isActive() || room == null) {
            throw new ResponseStatusException(404, "No such class or room");
        }
        teacher(wanted.trainer());
        int weeks = wanted.weeks() <= 0 ? 1 : wanted.weeks();
        int capacity = wanted.capacity() <= 0 ? type.getCapacity() : wanted.capacity();
        if (weeks > MOST_WEEKS || capacity > room.getCapacity()
                || wanted.startsAt() <= System.currentTimeMillis()) {
            throw new ResponseStatusException(400, "A class starts in the future, fits its room"
                    + " (" + room.getCapacity() + ") and repeats for at most " + MOST_WEEKS
                    + " weeks");
        }
        List<ClassSession> made = new ArrayList<ClassSession>();
        for (int week = 0; week < weeks; week++) {
            Instant start = Instant.ofEpochMilli(wanted.startsAt() + week * 7L * DAY);
            Instant end = start.plusMillis(type.getMinutes() * 60000L);
            Long clash = sessions.clashes(room.getId(), wanted.trainer(), start, end);
            if (clash != null && clash.longValue() > 0) {
                throw new ResponseStatusException(409, "The room or the trainer is taken on "
                        + LocalDate.ofEpochDay(start.toEpochMilli() / DAY));
            }
            ClassSession row = new ClassSession();
            row.setId(Ids.next());
            row.setClassTypeId(type.getId());
            row.setLocationId(room.getLocationId());
            row.setRoomId(room.getId());
            row.setTrainer(wanted.trainer());
            row.setStartsAt(start);
            row.setEndsAt(end);
            row.setCapacity(capacity);
            row.setState(Studio.SCHEDULED);
            sessions.insert(row);
            made.add(row);
        }
        return studio.describe("", made);
    }

    /// Cancels a class: every place is given back -- with its credit, for a
    /// pack -- and everyone booked or waiting is told.
    public SessionDto cancelClass(String id, String reason) throws IOException {
        List<Booking> told = cancel(id, reason);
        for (int iter = 0; iter < told.size(); iter++) {
            live.bookingChanged(told.get(iter).getAccount(), told.get(iter).getId(),
                    Bookings.CANCELLED);
        }
        live.sessionChanged(id, 0, 0, Studio.CANCELLED);
        return described(id);
    }

    @Transactional(rollbackFor = IOException.class)
    List<Booking> cancel(String id, String reason) throws IOException {
        ClassSession session = studio.session(id);
        Instant now = Instant.now();
        if (!Studio.SCHEDULED.equals(session.getState()) || !session.getStartsAt().isAfter(now)) {
            throw new ResponseStatusException(409, "Only a class still to come can be cancelled");
        }
        String why = cut(text(reason), 200);
        session.setState(Studio.CANCELLED);
        session.setNote(why);
        session.setBooked(0);
        session.setWaitlisted(0);
        ClassType type = types.findById(session.getClassTypeId());
        String name = type == null ? "A class" : type.getName();
        List<Booking> rows = bookings.findBySessionIdOrderByCreatedAtAsc(id);
        List<Booking> told = new ArrayList<Booking>();
        Set<String> accountsTold = new LinkedHashSet<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Booking row = rows.get(iter);
            boolean booked = Bookings.BOOKED.equals(row.getState());
            if (!booked && !Bookings.WAITLISTED.equals(row.getState())) {
                continue;
            }
            if (booked) {
                Membership held = household.current(row.getMemberId());
                MembershipPlan plan = held == null ? null : plans.findById(held.getPlanId());
                if (plan != null && Household.counted(plan)) {
                    memberships.returnCredit(held.getId());
                }
            }
            row.setState(Bookings.CANCELLED);
            row.setUpdatedAt(now);
            told.add(row);
            if (accountsTold.add(row.getAccount())) {
                notifier.tell(row.getAccount(), Notifier.CANCELLED, "", id, name + " is cancelled",
                        why.length() > 0 ? why : "Sorry -- the class will not run. Your place"
                                + " has been given back.", now);
            }
        }
        return told;
    }

    /// Hands a class to another trainer, who must be free then.
    @Transactional(rollbackFor = IOException.class)
    public SessionDto substitute(String id, String trainer) throws IOException {
        ClassSession session = studio.session(id);
        if (!Studio.SCHEDULED.equals(session.getState())
                || !session.getStartsAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(409, "Only a class still to come can change hands");
        }
        teacher(trainer);
        if (trainer.equals(session.getTrainer())) {
            return studio.get("", id);
        }
        // The room is the class's own, so it is left out of the clash check
        // by naming none.
        Long clash = sessions.clashes("", trainer, session.getStartsAt(), session.getEndsAt());
        if (clash != null && clash.longValue() > 0) {
            throw new ResponseStatusException(409, "That trainer teaches at the same time");
        }
        session.setTrainer(trainer);
        return studio.get("", id);
    }

    private Staff teacher(String trainer) {
        Staff row = blank(trainer) ? null : staff.findById(trainer);
        if (row == null || !row.isActive()) {
            throw new ResponseStatusException(404, "No such trainer");
        }
        return row;
    }

    // ------------------------------------------------------------- staff

    @Transactional(readOnly = true)
    public List<StaffDto> staff() throws IOException {
        List<Staff> rows = staff.findAllByOrderByUsernameAsc();
        long now = System.currentTimeMillis();
        Instant from = Instant.ofEpochMilli(now - now % (7L * DAY));
        List<StaffDto> out = new ArrayList<StaffDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter), from));
        }
        return out;
    }

    private StaffDto toDto(Staff row, Instant weekFrom) throws IOException {
        UserDto user = accounts.describe(row.getUsername());
        List<String> roles = new ArrayList<String>();
        if (user != null && user.trainer()) {
            roles.add(Roles.TRAINER);
        }
        if (user != null && user.desk()) {
            roles.add(Roles.DESK);
        }
        if (user != null && user.owner()) {
            roles.add(Roles.OWNER);
        }
        int classes = sessions.findByTrainerAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                row.getUsername(), weekFrom, weekFrom.plusMillis(7L * DAY)).size();
        Profile who = profiles.findById(row.getUsername());
        return new StaffDto(row.getUsername(), who == null ? "" : who.getDisplayName(),
                row.getTitle(), row.getBio(), row.getLocationId(),
                row.getCertifiedUntil().toEpochMilli(), roles, classes);
    }

    /// Changes what is known about a member of staff and what they may do.
    /// The account must exist; the roles go through [Accounts#setRoles],
    /// which keeps a trainer with classes to come from losing the role.
    public StaffDto saveStaff(String by, StaffDto wanted) throws IOException {
        if (wanted == null || blank(wanted.username())) {
            throw new ResponseStatusException(400, "Choose the person");
        }
        List<String> roles = wanted.roles() == null ? new ArrayList<String>() : wanted.roles();
        accounts.setRoles(by, wanted.username(), roles.contains(Roles.TRAINER),
                roles.contains(Roles.DESK), roles.contains(Roles.OWNER));
        return details(wanted);
    }

    @Transactional(readOnly = true)
    SessionDto described(String id) throws IOException {
        return studio.get("", id);
    }

    @Transactional(rollbackFor = IOException.class)
    StaffDto details(StaffDto wanted) throws IOException {
        if (!blank(wanted.locationId()) && locations.findById(wanted.locationId()) == null) {
            throw new ResponseStatusException(404, "No such location");
        }
        Staff row = staff.findById(wanted.username());
        boolean fresh = row == null;
        if (fresh) {
            row = new Staff();
            row.setUsername(wanted.username());
            row.setActive(true);
        }
        row.setTitle(cut(text(wanted.title()), 60));
        row.setBio(cut(text(wanted.bio()), 600));
        row.setLocationId(text(wanted.locationId()));
        row.setCertifiedUntil(Instant.ofEpochMilli(Math.max(0, wanted.certifiedUntil())));
        if (fresh) {
            staff.insert(row);
        }
        long now = System.currentTimeMillis();
        return toDto(row, Instant.ofEpochMilli(now - now % (7L * DAY)));
    }

    @Transactional(readOnly = true)
    public List<ShiftDto> shifts(String location) {
        if (blank(location) || locations.findById(location) == null) {
            throw new ResponseStatusException(404, "No such location");
        }
        List<Shift> rows = shifts.findByLocationIdAndEndsAtGreaterThanOrderByStartsAtAsc(location,
                Instant.now().minusMillis(DAY));
        List<ShiftDto> out = new ArrayList<ShiftDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(trainers.dto(rows.get(iter)));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public ShiftDto addShift(ShiftDto wanted) throws IOException {
        if (wanted == null || blank(wanted.duty()) || wanted.endsAt() <= wanted.startsAt()
                || wanted.endsAt() - wanted.startsAt() > 12L * HOUR
                || wanted.startsAt() <= System.currentTimeMillis()) {
            throw new ResponseStatusException(400, "A shift has a duty and is up to 12 hours,"
                    + " in the future");
        }
        if (blank(wanted.locationId()) || locations.findById(wanted.locationId()) == null) {
            throw new ResponseStatusException(404, "No such location");
        }
        Staff who = blank(wanted.staff()) ? null : staff.findById(wanted.staff());
        if (who == null || !who.isActive()) {
            throw new ResponseStatusException(404, "No such member of staff");
        }
        Shift row = new Shift();
        row.setId(Ids.next());
        row.setLocationId(wanted.locationId());
        row.setStaff(who.getUsername());
        row.setDuty(cut(wanted.duty().trim(), 60));
        row.setStartsAt(Instant.ofEpochMilli(wanted.startsAt()));
        row.setEndsAt(Instant.ofEpochMilli(wanted.endsAt()));
        row.setState(Trainers.ASSIGNED);
        shifts.insert(row);
        return trainers.dto(row);
    }

    // ------------------------------------------------------------- campaigns

    @Transactional(readOnly = true)
    public List<CampaignDto> campaigns() {
        List<Campaign> rows = campaigns.findAllByOrderByCreatedAtDesc();
        List<CampaignDto> out = new ArrayList<CampaignDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter)));
        }
        return out;
    }

    /// Sends a campaign to its audience ([#CLASS], [#ALL], [#LAPSED]) at
    /// once. Members away today are left out; the rest get it in their inbox,
    /// pushed when their preferences allow ([Notifier]).
    @Transactional(rollbackFor = IOException.class)
    public CampaignDto send(String by, CampaignDto wanted) throws IOException {
        if (wanted == null || blank(wanted.name()) || blank(wanted.title())
                || blank(wanted.body())) {
            throw new ResponseStatusException(400, "A campaign needs a name, a title and a"
                    + " message");
        }
        Instant now = Instant.now();
        String sessionId = "";
        Set<String> audience = new LinkedHashSet<String>();
        if (CLASS.equals(wanted.kind())) {
            ClassSession session = studio.session(wanted.sessionId());
            if (!Studio.SCHEDULED.equals(session.getState()) || !session.getStartsAt().isAfter(now)) {
                throw new ResponseStatusException(409, "That class has started or is off");
            }
            sessionId = session.getId();
            audience.addAll(bookings.accountsWhoTook(session.getClassTypeId()));
            List<Booking> in = bookings.findBySessionIdOrderByCreatedAtAsc(sessionId);
            for (int iter = 0; iter < in.size(); iter++) {
                audience.remove(in.get(iter).getAccount());
            }
        } else if (ALL.equals(wanted.kind()) || LAPSED.equals(wanted.kind())) {
            List<Membership> running = memberships.findByStateIn(Arrays.asList(Billing.ACTIVE));
            for (int iter = 0; iter < running.size(); iter++) {
                audience.add(running.get(iter).getAccount());
            }
            if (LAPSED.equals(wanted.kind())) {
                List<Object[]> last = checkIns.lastVisits();
                long cut = now.toEpochMilli() - 30L * DAY;
                for (int iter = 0; iter < last.size(); iter++) {
                    if (millis(last.get(iter)[1]) >= cut) {
                        audience.remove(String.valueOf(last.get(iter)[0]));
                    }
                }
            }
        } else {
            throw new ResponseStatusException(400, "Choose who it goes to");
        }
        Campaign row = new Campaign();
        row.setId(Ids.next());
        row.setName(cut(wanted.name().trim(), 80));
        row.setKind(wanted.kind());
        row.setSessionId(sessionId);
        row.setTitle(cut(wanted.title().trim(), 120));
        row.setBody(cut(wanted.body().trim(), 400));
        row.setCreatedBy(by);
        row.setCreatedAt(now);
        LocalDate today = LocalDate.ofEpochDay(now.toEpochMilli() / DAY);
        int sent = 0;
        for (String account : audience) {
            if (sent >= MOST_RECIPIENTS) {
                break;
            }
            if (!engagement.isAway(account, today)) {
                notifier.tell(account, Notifier.CAMPAIGN, row.getId(), sessionId, row.getTitle(),
                        row.getBody(), now);
                sent++;
            }
        }
        row.setSent(sent);
        campaigns.insert(row);
        return toDto(row);
    }

    private static CampaignDto toDto(Campaign row) {
        return new CampaignDto(row.getId(), row.getName(), row.getKind(), row.getSessionId(),
                row.getTitle(), row.getBody(), row.getCreatedAt().toEpochMilli(), row.getSent(),
                row.getOpened(), row.getBooked());
    }

    private static boolean blank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String cut(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }
}
