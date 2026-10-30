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

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.pulse.Days;
import com.codenameone.examples.pulse.Ids;
import com.codenameone.examples.pulse.account.DemoAccounts;
import com.codenameone.examples.pulse.trainer.Trainers;
import com.codenameone.examples.pulse.domain.Booking;
import com.codenameone.examples.pulse.domain.Campaign;
import com.codenameone.examples.pulse.domain.CheckIn;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.ClassType;
import com.codenameone.examples.pulse.domain.Friend;
import com.codenameone.examples.pulse.domain.Invoice;
import com.codenameone.examples.pulse.domain.Location;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.domain.Workout;
import com.codenameone.examples.pulse.domain.Membership;
import com.codenameone.examples.pulse.domain.MembershipPlan;
import com.codenameone.examples.pulse.domain.Room;
import com.codenameone.examples.pulse.domain.Shift;
import com.codenameone.examples.pulse.domain.Staff;
import com.codenameone.examples.pulse.repository.BookingRepository;
import com.codenameone.examples.pulse.repository.CampaignRepository;
import com.codenameone.examples.pulse.repository.CheckInRepository;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.ClassTypeRepository;
import com.codenameone.examples.pulse.repository.FriendRepository;
import com.codenameone.examples.pulse.repository.InvoiceRepository;
import com.codenameone.examples.pulse.repository.LocationRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.MembershipPlanRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;
import com.codenameone.examples.pulse.repository.RoomRepository;
import com.codenameone.examples.pulse.repository.ShiftRepository;
import com.codenameone.examples.pulse.repository.StaffRepository;
import com.codenameone.examples.pulse.repository.WorkoutRepository;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/// The studio a development server starts with: two locations, a timetable
/// of adult and children's classes from three weeks back to three weeks
/// ahead, the plans on sale, and the demo accounts' memberships, children
/// and visits.
///
/// Written once, the first time a server starts against an empty database;
/// the marker is the first location. The history is real rows of the same
/// tables the app writes -- bookings, check-ins, paid invoices -- so every
/// screen that reads them, the owner's dashboard included, has something to
/// show without a special case.
@Component
public class DemoStudio {
    public static final String DOWNTOWN = "loc-downtown";
    public static final String RIVERSIDE = "loc-riverside";
    public static final String ZONE = "America/New_York";
    /// The plan ids the tests name.
    public static final String UNLIMITED = "plan-unlimited";
    public static final String OFF_PEAK = "plan-offpeak";
    public static final String PACK = "plan-pack10";
    public static final String KIDS = "plan-kids";
    public static final String DROP_IN = "plan-dropin";
    public static final String ANNUAL = "plan-annual";
    /// The class type ids the tests name.
    public static final String HIIT = "type-hiit";
    public static final String LITTLE_MOVERS = "type-littlemovers";
    public static final String TEEN = "type-teen";
    /// The member ids of the demo household's children.
    public static final String KAI = "member-kai";
    public static final String MIA = "member-mia";
    static final int WEEKS_BACK = 3;
    static final int WEEKS_AHEAD = 3;

    private final DemoAccounts accounts;
    private final Config config;
    private final LocationRepository locations;
    private final RoomRepository rooms;
    private final ClassTypeRepository types;
    private final ClassSessionRepository sessions;
    private final StaffRepository staff;
    private final MembershipPlanRepository plans;
    private final MembershipRepository memberships;
    private final MemberRepository members;
    private final BookingRepository bookings;
    private final CheckInRepository checkIns;
    private final InvoiceRepository invoices;
    private final WorkoutRepository workouts;
    private final FriendRepository friends;
    private final ShiftRepository shifts;
    private final CampaignRepository campaigns;
    /// A fixed sequence, so that every demo database has the same history.
    private long seed = 20261030L;

    public DemoStudio(DemoAccounts accounts, Config config, LocationRepository locations,
            RoomRepository rooms, ClassTypeRepository types, ClassSessionRepository sessions,
            StaffRepository staff, MembershipPlanRepository plans,
            MembershipRepository memberships, MemberRepository members,
            BookingRepository bookings, CheckInRepository checkIns, InvoiceRepository invoices,
            WorkoutRepository workouts, FriendRepository friends,
            ShiftRepository shifts, CampaignRepository campaigns) {
        this.accounts = accounts;
        this.config = config;
        this.locations = locations;
        this.rooms = rooms;
        this.types = types;
        this.sessions = sessions;
        this.staff = staff;
        this.plans = plans;
        this.memberships = memberships;
        this.members = members;
        this.bookings = bookings;
        this.checkIns = checkIns;
        this.invoices = invoices;
        this.workouts = workouts;
        this.friends = friends;
        this.shifts = shifts;
        this.campaigns = campaigns;
    }

    @PostConstruct
    public void seed() throws IOException {
        accounts.seed();
        if (!config.isDevelopmentProfile() || seeded()) {
            return;
        }
        write();
    }

    @Transactional(readOnly = true)
    boolean seeded() {
        return locations.existsById(DOWNTOWN);
    }

    @Transactional(rollbackFor = IOException.class)
    void write() throws IOException {
        location(DOWNTOWN, "Pulse Downtown", "120 Market Street", 330, 1320);
        location(RIVERSIDE, "Pulse Riverside", "8 Quay Road", 360, 1260);
        room("room-studio-a", DOWNTOWN, "Studio A", 20);
        room("room-ride", DOWNTOWN, "Ride Room", 24);
        room("room-floor", DOWNTOWN, "Strength Floor", 14);
        room("room-kids", DOWNTOWN, "Kids Zone", 12);
        room("room-river", RIVERSIDE, "River Studio", 18);
        room("room-wall", RIVERSIDE, "Climbing Wall", 10);
        type(HIIT, "HIIT Burn", "Forty-five minutes of intervals: hard, rest, again.",
                "STRENGTH", 5, 16, 0, 16, 45, "#E4572E");
        type("type-yoga", "Flow Yoga", "Breath-led vinyasa for every level.", "MIND", 2, 16, 0,
                20, 60, "#6C8EAD");
        type("type-ride", "Rhythm Ride", "Indoor cycling to the beat, lights down.", "CARDIO", 4,
                14, 0, 24, 45, "#F2A541");
        type("type-barbell", "Barbell Club", "Coached lifting: squat, press, pull.", "STRENGTH",
                4, 18, 0, 12, 60, "#3D5A80");
        type("type-pilates", "Core Pilates", "Mat Pilates for posture and core.", "MIND", 3, 16,
                0, 14, 50, "#9C6ADE");
        type("type-steady", "Steady & Strong", "Low impact strength and balance.", "CARDIO", 1,
                60, 0, 16, 45, "#8D99AE");
        type(LITTLE_MOVERS, "Little Movers", "Games, tumbling and balance for ages 4 to 7.",
                "KIDS", 2, 4, 7, 12, 40, "#4CB944");
        type("type-kidsclimb", "Kids Climb", "Roped climbing with an instructor, 8 to 12.",
                "KIDS", 3, 8, 12, 10, 60, "#FF6F59");
        type(TEEN, "Teen Strength", "Safe technique and confidence for 12 to 17.", "KIDS", 3,
                12, 17, 12, 50, "#2EC4B6");
        staffRow(DemoAccounts.TRAINER, DOWNTOWN, "Head coach",
                "Strength and conditioning coach; ten years on the floor.");
        staffRow(DemoAccounts.SECOND_TRAINER, RIVERSIDE, "Yoga and kids lead",
                "Yoga teacher, climbing instructor and parent of two.");
        plan(UNLIMITED, "Unlimited", "Every class, every location.", "MONTHLY", 8900L, 1, 0, 16,
                0, false);
        plan(ANNUAL, "Unlimited Annual", "Twelve months for the price of ten.", "ANNUAL",
                89000L, 12, 0, 16, 0, false);
        plan(OFF_PEAK, "Off-Peak", "Weekday middays and weekends.", "MONTHLY", 5900L, 1, 0, 16,
                0, true);
        plan(PACK, "10-Class Pack", "Ten classes, six months to use them.", "CLASS_PACK",
                15000L, 6, 10, 14, 0, false);
        plan(KIDS, "Kids Monthly", "Every kids' class, ages 4 to 17.", "MONTHLY", 4500L, 1, 0,
                4, 17, false);
        plan(DROP_IN, "Drop-in", "One class, no commitment.", "DROP_IN", 2200L, 1, 1, 14, 0,
                false);
        timetable();
        households();
        liveClass();
        shifts();
        campaigns();
    }

    /// A HIIT of Tess's that started a quarter of an hour ago, whatever the
    /// time the server comes up, with six people checked in: the trainer's
    /// live screen has a roster, and [DemoHeartRates] gives it pulses.
    private void liveClass() throws IOException {
        Instant now = Instant.now();
        ClassSession live = session(now.minusMillis(15L * 60000L), HIIT, DOWNTOWN,
                "room-studio-a", DemoAccounts.TRAINER);
        LocalDate today = LocalDate.now();
        attended(self(DemoAccounts.SECOND_MEMBER), live, true);
        String[] names = {"Jordan Reyes", "Priya Natarajan", "Marcus Bell", "Hannah Okafor",
            "Sam Whitfield"};
        int[] ages = {27, 34, 45, 22, 58};
        // Start days that fall on different days of the billing month, so the
        // owner's revenue chart is not one bar.
        int[] joined = {37, 52, 98, 143, 176};
        List<ClassSession> past = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                now.minusMillis(WEEKS_BACK * 7L * Days.MILLIS), now);
        for (int iter = 0; iter < names.length; iter++) {
            // Households with no sign-in of their own: members the desk
            // signed up, so the roster is not only the demo accounts.
            Member row = new Member();
            row.setId("member-guest-" + (iter + 1));
            row.setAccount("demo-guest-" + (iter + 1) + "@pulse.example");
            row.setName(names[iter]);
            row.setBirthDate(today.minusYears(ages[iter]).minusDays(30L * iter + 11L));
            row.setRelation("SELF");
            row.setCreatedAt(now.minusMillis((200L + 40L * iter) * Days.MILLIS));
            sign(row);
            members.insert(row);
            Membership plan = membership(row, iter == 3 ? OFF_PEAK : UNLIMITED, "ACTIVE",
                    joined[iter], 0, now);
            // The odd drop-in for a friend and a class pack on top, on days
            // of their own.
            purchase(plan, row, DROP_IN, 2 + 5 * iter, now);
            purchase(plan, row, iter % 2 == 0 ? DROP_IN : PACK, 4 + 6 * iter, now);
            for (int each = 0; each < past.size(); each++) {
                ClassSession session = past.get(each);
                if (session.getEndsAt().isBefore(now) && DOWNTOWN.equals(session.getLocationId())
                        && !LITTLE_MOVERS.equals(session.getClassTypeId())
                        && !TEEN.equals(session.getClassTypeId()) && next(7) == 0) {
                    attended(row, session, next(10) != 0);
                }
            }
            attended(row, live, true);
        }
    }

    /// A week of shifts for both trainers, one of Theo's offered for swap so
    /// Tess has something under "Offered by others".
    private void shifts() throws IOException {
        LocalDate today = LocalDate.now(ZoneId.of(ZONE));
        for (int day = 1; day <= 7; day++) {
            LocalDate on = today.plusDays(day);
            if (on.getDayOfWeek() != DayOfWeek.SUNDAY) {
                shift(DemoAccounts.TRAINER, on, 6, 6, day % 2 == 0 ? "Classes" : "Floor",
                        Trainers.ASSIGNED);
            }
            if (day % 2 == 1) {
                shift(DemoAccounts.SECOND_TRAINER, on, 12, 5, day == 3 ? "Front desk" : "Classes",
                        day == 3 ? Trainers.SWAP_REQUESTED : Trainers.ASSIGNED);
            }
        }
    }

    /// A one-off purchase -- a drop-in or a class pack -- paid `daysAgo`.
    private void purchase(Membership membership, Member member, String planId, int daysAgo,
            Instant now) throws IOException {
        if (membership == null) {
            return;
        }
        MembershipPlan plan = plans.findById(planId);
        Instant at = now.minusMillis(daysAgo * Days.MILLIS + 3L * 3600000L);
        Invoice row = invoice(membership, member, plan.getName(), plan.getPriceCents(), at, at);
        row.setStatus("PAID");
        row.setAttempts(1);
        row.setChargeRef("demo-" + row.getId().substring(0, 8));
        row.setMethodId("");
        row.setPaidAt(at);
        invoices.insert(row);
    }

    /// Three campaigns the owner sent over the last weeks: filling a quiet
    /// Barbell Club, calling back the lapsed, and a referral offer.
    private void campaigns() throws IOException {
        Instant now = Instant.now();
        List<ClassSession> past = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                now.minusMillis(14L * Days.MILLIS), now);
        String barbell = "";
        for (int iter = 0; iter < past.size() && barbell.length() == 0; iter++) {
            if ("type-barbell".equals(past.get(iter).getClassTypeId())) {
                barbell = past.get(iter).getId();
            }
        }
        campaign("Fill Barbell Club", "CLASS", barbell, "Four places left at Barbell Club",
                "Squat, press, pull with Tess tonight. Book before it fills.", 12, 38, 29, 6);
        campaign("We miss you", "LAPSED", "", "Your mat misses you",
                "It has been a while. Come back this week, your first class is on us.", 9, 22,
                14, 3);
        campaign("Bring a friend", "ALL", "", "Train with a friend, both get a free week",
                "Share your code: when they join, you both get seven days free.", 3, 146, 97,
                11);
    }

    private void campaign(String name, String kind, String sessionId, String title, String body,
            int daysAgo, int sent, int opened, int booked) throws IOException {
        Campaign row = new Campaign();
        row.setId(Ids.next());
        row.setName(name);
        row.setKind(kind);
        row.setSessionId(sessionId);
        row.setTitle(title);
        row.setBody(body);
        row.setCreatedBy(DemoAccounts.OWNER);
        row.setCreatedAt(Instant.now().minusMillis(daysAgo * Days.MILLIS + 5L * 3600000L));
        row.setSent(sent);
        row.setOpened(opened);
        row.setBooked(booked);
        campaigns.insert(row);
    }

    private void shift(String trainer, LocalDate day, int hour, int hours, String duty,
            String state) throws IOException {
        Instant start = ZonedDateTime.of(day, LocalTime.of(hour, 0), ZoneId.of(ZONE)).toInstant();
        Shift row = new Shift();
        row.setId(Ids.next());
        row.setLocationId(DOWNTOWN);
        row.setStaff(trainer);
        row.setDuty(duty);
        row.setStartsAt(start);
        row.setEndsAt(start.plusMillis(hours * 3600000L));
        row.setState(state);
        shifts.insert(row);
    }

    private void timetable() throws IOException {
        LocalDate today = LocalDate.now(ZoneId.of(ZONE));
        LocalDate first = today.minusWeeks(WEEKS_BACK).with(DayOfWeek.MONDAY);
        LocalDate last = today.plusWeeks(WEEKS_AHEAD);
        String tess = DemoAccounts.TRAINER;
        String theo = DemoAccounts.SECOND_TRAINER;
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            DayOfWeek dow = day.getDayOfWeek();
            boolean weekday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
            if (weekday) {
                session(day, 6, 30, HIIT, DOWNTOWN, "room-studio-a", tess);
                session(day, 7, 30, "type-ride", DOWNTOWN, "room-ride", tess);
                session(day, 12, 15, "type-yoga", DOWNTOWN, "room-studio-a", theo);
                session(day, 17, 30, "type-barbell", DOWNTOWN, "room-floor", tess);
                session(day, 18, 30, "type-ride", DOWNTOWN, "room-ride", tess);
                session(day, 19, 30, "type-pilates", DOWNTOWN, "room-studio-a", theo);
                session(day, 7, 0, "type-yoga", RIVERSIDE, "room-river", theo);
                session(day, 18, 0, HIIT, RIVERSIDE, "room-river", tess);
            }
            if (dow == DayOfWeek.TUESDAY || dow == DayOfWeek.THURSDAY) {
                session(day, 16, 0, LITTLE_MOVERS, DOWNTOWN, "room-kids", theo);
                session(day, 16, 30, TEEN, DOWNTOWN, "room-floor", tess);
            }
            if (dow == DayOfWeek.WEDNESDAY) {
                session(day, 10, 0, "type-steady", DOWNTOWN, "room-studio-a", theo);
            }
            if (dow == DayOfWeek.SATURDAY) {
                session(day, 9, 0, LITTLE_MOVERS, DOWNTOWN, "room-kids", theo);
                session(day, 10, 0, "type-kidsclimb", RIVERSIDE, "room-wall", theo);
                session(day, 11, 0, TEEN, DOWNTOWN, "room-floor", tess);
                session(day, 9, 30, "type-yoga", RIVERSIDE, "room-river", theo);
            }
            if (dow == DayOfWeek.SUNDAY) {
                session(day, 10, 0, "type-ride", DOWNTOWN, "room-ride", tess);
            }
        }
    }

    /// The demo accounts' memberships, children and the visits behind them.
    private void households() throws IOException {
        Instant now = Instant.now();
        String riley = DemoAccounts.MEMBER;
        LocalDate today = LocalDate.now();
        Member rileySelf = self(riley);
        Member kai = child(KAI, riley, "Kai Member", today.minusYears(6).minusDays(40));
        Member mia = child(MIA, riley, "Mia Member", today.minusYears(13).minusDays(100));
        sign(rileySelf);
        sign(kai);
        sign(mia);
        membership(rileySelf, UNLIMITED, "ACTIVE", 124, 0, now);
        membership(kai, KIDS, "ACTIVE", 96, 0, now);
        membership(mia, KIDS, "ACTIVE", 103, 0, now);
        Member rae = self(DemoAccounts.FLAGGED_MEMBER);
        sign(rae);
        Membership raes = membership(rae, OFF_PEAK, "PAST_DUE", 60, 0, now);
        overdue(raes, 5900L, now);
        // Robin trains with Riley: friends both ways, so each sees the
        // other's classes in the feed and on the timetable.
        Member robin = self(DemoAccounts.SECOND_MEMBER);
        if (robin != null) {
            sign(robin);
            membership(robin, UNLIMITED, "ACTIVE", 289, 0, now);
            if (rileySelf != null) {
                befriend(rileySelf.getAccount(), robin.getAccount(), now);
                befriend(robin.getAccount(), rileySelf.getAccount(), now);
            }
        }
        Member lee = self(DemoAccounts.LAPSED_MEMBER);
        sign(lee);
        membership(lee, PACK, "ACTIVE", 200, 4, now);
        // The visits: Riley twice a week or so, the children at their
        // Saturday and after-school classes, Lee not for six weeks.
        List<ClassSession> past = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                now.minusMillis((WEEKS_BACK * 7L + 7L) * Days.MILLIS), now);
        for (int iter = 0; iter < past.size(); iter++) {
            ClassSession session = past.get(iter);
            if (session.getEndsAt().isAfter(now)) {
                continue;
            }
            String type = session.getClassTypeId();
            if (rileySelf != null && (HIIT.equals(type) || "type-yoga".equals(type))
                    && DOWNTOWN.equals(session.getLocationId()) && next(5) == 0) {
                attended(rileySelf, session, next(9) != 0);
            }
            if (robin != null && (HIIT.equals(type) || "type-ride".equals(type))
                    && DOWNTOWN.equals(session.getLocationId()) && next(4) == 0) {
                attended(robin, session, true);
            }
            if (kai != null && LITTLE_MOVERS.equals(type) && next(3) != 0) {
                attended(kai, session, true);
            }
            if (mia != null && TEEN.equals(type) && next(2) == 0) {
                attended(mia, session, true);
            }
            if (rae != null && "type-ride".equals(type) && next(12) == 0) {
                attended(rae, session, false);
            }
        }
        // Classes to come: Riley and the children are booked into a few, and
        // one early HIIT is made small and full, so the waitlist has a queue
        // to show.
        List<ClassSession> ahead = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                now.plusMillis(Days.MILLIS), now.plusMillis(8L * Days.MILLIS));
        boolean full = false;
        for (int iter = 0; iter < ahead.size(); iter++) {
            ClassSession session = ahead.get(iter);
            String type = session.getClassTypeId();
            if (!full && HIIT.equals(type) && DOWNTOWN.equals(session.getLocationId())) {
                session.setCapacity(2);
                session.setNote("Small group: two places");
                book(rae, session);
                book(lee, session);
                full = true;
            } else if (LITTLE_MOVERS.equals(type) && session.getBooked() == 0 && next(2) == 0) {
                book(kai, session);
            } else if ("type-yoga".equals(type) && DOWNTOWN.equals(session.getLocationId())
                    && next(4) == 0) {
                book(rileySelf, session);
            }
        }
    }

    private void location(String id, String name, String address, int opens, int closes) {
        Location row = new Location();
        row.setId(id);
        row.setName(name);
        row.setAddress(address);
        row.setTimeZone(ZONE);
        row.setOpensMinute(opens);
        row.setClosesMinute(closes);
        locations.insert(row);
    }

    private void room(String id, String location, String name, int capacity) {
        Room row = new Room();
        row.setId(id);
        row.setLocationId(location);
        row.setName(name);
        row.setCapacity(capacity);
        rooms.insert(row);
    }

    private void type(String id, String name, String description, String category,
            int intensity, int minAge, int maxAge, int capacity, int minutes, String color) {
        ClassType row = new ClassType();
        row.setId(id);
        row.setName(name);
        row.setDescription(description);
        row.setCategory(category);
        row.setIntensity(intensity);
        row.setMinAge(minAge);
        row.setMaxAge(maxAge);
        row.setCapacity(capacity);
        row.setMinutes(minutes);
        row.setColor(color);
        row.setActive(true);
        types.insert(row);
    }

    private void staffRow(String username, String location, String title, String bio) {
        Staff row = staff.findById(username);
        if (row == null) {
            return;
        }
        row.setLocationId(location);
        row.setTitle(title);
        row.setBio(bio);
        row.setCertifiedUntil(Instant.now().plusMillis(300L * Days.MILLIS));
    }

    private void plan(String id, String name, String description, String kind, long cents,
            int months, int credits, int minAge, int maxAge, boolean offPeak) {
        MembershipPlan row = new MembershipPlan();
        row.setId(id);
        row.setName(name);
        row.setDescription(description);
        row.setKind(kind);
        row.setPriceCents(cents);
        row.setCurrency("USD");
        row.setPeriodMonths(months);
        row.setClassCredits(credits);
        row.setMinAge(minAge);
        row.setMaxAge(maxAge);
        row.setOffPeakOnly(offPeak);
        row.setActive(true);
        plans.insert(row);
    }

    private void session(LocalDate day, int hour, int minute, String typeId, String location,
            String room, String trainer) throws IOException {
        session(ZonedDateTime.of(day, LocalTime.of(hour, minute), ZoneId.of(ZONE)).toInstant(),
                typeId, location, room, trainer);
    }

    private ClassSession session(Instant start, String typeId, String location, String room,
            String trainer) throws IOException {
        ClassType type = types.findById(typeId);
        ClassSession row = new ClassSession();
        row.setId(Ids.next());
        row.setClassTypeId(typeId);
        row.setLocationId(location);
        row.setRoomId(room);
        row.setTrainer(trainer);
        row.setStartsAt(start);
        row.setEndsAt(start.plusMillis(type.getMinutes() * 60000L));
        row.setCapacity(type.getCapacity());
        row.setState(start.plusMillis(type.getMinutes() * 60000L + Bookings.SETTLE_MILLIS)
                .isBefore(Instant.now()) ? Studio.DONE : Studio.SCHEDULED);
        sessions.insert(row);
        return row;
    }

    private Member self(String account) {
        return members.findFirstByAccountAndRelation(account, "SELF");
    }

    private Member child(String id, String account, String name, LocalDate born) {
        if (self(account) == null) {
            return null;
        }
        Member row = new Member();
        row.setId(id);
        row.setAccount(account);
        row.setName(name);
        row.setBirthDate(born);
        row.setRelation("CHILD");
        row.setMedicalNote(KAI.equals(id) ? "Mild asthma; inhaler in bag" : "");
        row.setCreatedAt(Instant.now().minusMillis(90L * Days.MILLIS));
        members.insert(row);
        return row;
    }

    private static void sign(Member member) {
        if (member != null) {
            member.setWaiverAt(member.getCreatedAt());
        }
    }

    /// A membership started `daysAgo`, with a paid invoice for every period
    /// since.
    private Membership membership(Member member, String planId, String state, int daysAgo,
            int creditsLeft, Instant now) throws IOException {
        if (member == null) {
            return null;
        }
        MembershipPlan plan = plans.findById(planId);
        Instant started = now.minusMillis(daysAgo * Days.MILLIS);
        Membership row = new Membership();
        row.setId(Ids.next());
        row.setMemberId(member.getId());
        row.setAccount(member.getAccount());
        row.setPlanId(planId);
        row.setState(state);
        row.setStartedAt(started);
        row.setCreditsLeft(creditsLeft);
        row.setUpdatedAt(started);
        long periodMillis = plan.getPeriodMonths() * 30L * Days.MILLIS;
        Instant periodStart = started;
        Instant periodEnd = started.plusMillis(periodMillis);
        boolean recurring = !Household.counted(plan);
        while (true) {
            if (!"PAST_DUE".equals(state) || periodEnd.isBefore(now)) {
                paid(row, member, plan, periodStart, periodEnd);
            }
            if (!recurring || periodEnd.isAfter(now)) {
                break;
            }
            periodStart = periodEnd;
            periodEnd = periodEnd.plusMillis(periodMillis);
        }
        row.setCurrentPeriodEnd(periodEnd);
        memberships.insert(row);
        return row;
    }

    private void paid(Membership membership, Member member, MembershipPlan plan, Instant from,
            Instant to) throws IOException {
        Invoice row = invoice(membership, member, plan.getName(), plan.getPriceCents(), from, to);
        row.setStatus("PAID");
        row.setAttempts(1);
        row.setChargeRef("demo-" + row.getId().substring(0, 8));
        row.setMethodId("");
        row.setPaidAt(from.plusMillis(60000L));
        invoices.insert(row);
    }

    private void overdue(Membership membership, long cents, Instant now) throws IOException {
        if (membership == null) {
            return;
        }
        Member member = members.findById(membership.getMemberId());
        Invoice row = invoice(membership, member, "Off-Peak", cents,
                now.minusMillis(4L * Days.MILLIS), now.plusMillis(26L * Days.MILLIS));
        row.setStatus("FAILED");
        row.setAttempts(2);
        row.setFailure("Card declined: insufficient funds");
        row.setNextAttemptAt(now.plusMillis(2L * Days.MILLIS));
        invoices.insert(row);
    }

    private Invoice invoice(Membership membership, Member member, String what, long cents,
            Instant from, Instant to) throws IOException {
        Invoice row = new Invoice();
        row.setId(Ids.next());
        row.setAccount(membership.getAccount());
        row.setMembershipId(membership.getId());
        row.setMemberId(membership.getMemberId());
        row.setDescription(what + " for " + (member == null ? "" : member.getName()));
        row.setAmountCents(cents);
        row.setCurrency("USD");
        row.setPeriodStart(from);
        row.setPeriodEnd(to);
        row.setCreatedAt(from);
        return row;
    }

    /// A past class, attended -- or booked and missed, for `came` false.
    private void attended(Member member, ClassSession session, boolean came) throws IOException {
        if (member == null) {
            return;
        }
        Booking row = booking(member, session, came ? Bookings.CHECKED_IN : Bookings.NO_SHOW);
        session.setBooked(session.getBooked() + 1);
        if (came) {
            Instant at = session.getStartsAt().minusMillis(5L * 60000L);
            row.setCheckedInAt(at);
            CheckIn visit = new CheckIn();
            visit.setId(Ids.next());
            visit.setMemberId(member.getId());
            visit.setAccount(member.getAccount());
            visit.setLocationId(session.getLocationId());
            visit.setMethod(next(3) == 0 ? "NFC" : "QR");
            visit.setSessionId(session.getId());
            visit.setAt(at);
            visit.setStaff(DemoAccounts.DESK);
            checkIns.insert(visit);
            if ("SELF".equals(member.getRelation())
                    && session.getEndsAt().isBefore(Instant.now())) {
                worked(member, session);
            }
        }
        bookings.insert(row);
    }

    /// The workout an adult recorded in a class they came to. Most wore a
    /// strap or a watch; some only had the phone's estimate, and a few
    /// recorded nothing at all, so the points and badges have something to
    /// tell apart.
    private void worked(Member member, ClassSession session) throws IOException {
        int kind = next(6);
        if (kind == 0) {
            return;
        }
        int minutes = (int) ((session.getEndsAt().toEpochMilli()
                - session.getStartsAt().toEpochMilli()) / 60000L);
        int avg = kind == 1 ? 0 : 118 + next(30);
        int peak = avg == 0 ? 0 : avg + 18 + next(20);
        Workout row = new Workout();
        row.setId(Ids.next());
        row.setAccount(member.getAccount());
        row.setSessionId(session.getId());
        String name = className(session);
        row.setActivity(name == null ? "Class" : name);
        row.setStartedAt(session.getStartsAt());
        row.setMinutes(minutes);
        row.setAvgHr(avg);
        row.setMaxHr(peak);
        row.setCalories(minutes * (avg == 0 ? 6 : avg / 18));
        if (avg == 0) {
            row.setZones("0,0,0,0,0");
        } else {
            int top = peak > 175 ? 2 + next(4) : 0;
            row.setZones((minutes / 6) + "," + (minutes / 5) + "," + (minutes / 4) + ","
                    + (minutes - minutes / 6 - minutes / 5 - minutes / 4 - top) + "," + top);
        }
        row.setSource(kind == 1 ? "HEALTH" : kind == 2 ? "WATCH" : "STRAP");
        row.setShared(kind != 5);
        row.setCreatedAt(session.getEndsAt());
        workouts.insert(row);
    }

    private void befriend(String account, String other, Instant at) throws IOException {
        Friend row = new Friend();
        row.setId(Ids.next());
        row.setAccount(account);
        row.setFriend(other);
        row.setState("ACCEPTED");
        row.setCreatedAt(at);
        friends.insert(row);
    }

    private String className(ClassSession session) {
        ClassType type = types.findById(session.getClassTypeId());
        return type == null ? null : type.getName();
    }

    private void book(Member member, ClassSession session) throws IOException {
        if (member == null) {
            return;
        }
        boolean room = session.getBooked() < session.getCapacity();
        Booking row = booking(member, session, room ? Bookings.BOOKED : Bookings.WAITLISTED);
        if (room) {
            session.setBooked(session.getBooked() + 1);
        } else {
            session.setWaitlisted(session.getWaitlisted() + 1);
            row.setQueuePosition(session.getWaitlisted());
        }
        bookings.insert(row);
    }

    private static Booking booking(Member member, ClassSession session, String state)
            throws IOException {
        Booking row = new Booking();
        row.setId(Ids.next());
        row.setSessionId(session.getId());
        row.setMemberId(member.getId());
        row.setAccount(member.getAccount());
        row.setState(state);
        Instant made = session.getStartsAt().minusMillis(2L * Days.MILLIS);
        row.setCreatedAt(made);
        row.setUpdatedAt(made);
        return row;
    }

    /// The next number in `[0, bound)` of the fixed sequence.
    private int next(int bound) {
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        return (int) ((seed >>> 33) % bound);
    }
}
