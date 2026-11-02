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
import com.codename1.backend.annotations.Value;
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
import com.codenameone.examples.pulse.domain.WalkIn;
import com.codenameone.examples.pulse.domain.Kudos;
import com.codenameone.examples.pulse.domain.Profile;
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
import com.codenameone.examples.pulse.repository.KudosRepository;
import com.codenameone.examples.pulse.repository.ProfileRepository;
import com.codenameone.examples.pulse.repository.InvoiceRepository;
import com.codenameone.examples.pulse.repository.LocationRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.MembershipPlanRepository;
import com.codenameone.examples.pulse.repository.MembershipRepository;
import com.codenameone.examples.pulse.repository.RoomRepository;
import com.codenameone.examples.pulse.repository.ShiftRepository;
import com.codenameone.examples.pulse.repository.WalkInRepository;
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
    /// The studio's time zone when `pulse.demo.zone` names none. The timetable
    /// is laid out in studio hours, so a screenshot run sets the zone to one
    /// where it is morning, and the device to the same zone, rather than
    /// showing an evening of late classes.
    public static final String DEFAULT_ZONE = "America/New_York";
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
    private final KudosRepository kudos;
    private final ShiftRepository shifts;
    private final CampaignRepository campaigns;
    /// A fixed sequence, so that every demo database has the same history.
    private long seed = 20261030L;
    private final String zone;
    private final WalkInRepository walkIns;
    private final ProfileRepository profiles;

    public DemoStudio(DemoAccounts accounts, Config config, LocationRepository locations,
            RoomRepository rooms, ClassTypeRepository types, ClassSessionRepository sessions,
            StaffRepository staff, MembershipPlanRepository plans,
            MembershipRepository memberships, MemberRepository members,
            BookingRepository bookings, CheckInRepository checkIns, InvoiceRepository invoices,
            WorkoutRepository workouts, FriendRepository friends, KudosRepository kudos,
            ShiftRepository shifts, CampaignRepository campaigns, WalkInRepository walkIns,
            ProfileRepository profiles,
            @Value("${pulse.demo.zone:" + DEFAULT_ZONE + "}") String zone) {
        this.zone = zone;
        this.walkIns = walkIns;
        this.profiles = profiles;
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
        this.kudos = kudos;
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
        staffRow(DemoAccounts.PILATES_TRAINER, DOWNTOWN, "Pilates and barre",
                "Former dancer; teaches Pilates and the gentler strength classes.");
        staffRow(DemoAccounts.RIDE_TRAINER, DOWNTOWN, "Ride and HIIT coach",
                "Track cyclist turned coach; runs the evening rides.");
        staffRow(DemoAccounts.RIVERSIDE_TRAINER, RIVERSIDE, "Yoga and climbing",
                "Yoga teacher and route setter at the Riverside wall.");
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
        walkIns();
        shifts();
        campaigns();
        // The tests assert exact counts against the studio above -- members,
        // bookings, invoices -- and both the JVM and the compiled ParparVM run
        // of them seed a fresh database for every suite, so the crowd and its
        // twelve weeks of history are for the servers people look at.
        if (!"test".equals(config.getProfile())) {
            crowd();
            rileysWeek();
        }
    }

    /// Riley's next few days: a class tomorrow and two more after it, so the
    /// watch's "coming up" page and the phone's bookings have a week in them.
    /// Kept out of the tests' studio with the crowd, since it books places
    /// the tests count.
    private void rileysWeek() throws IOException {
        Member riley = self(DemoAccounts.MEMBER);
        if (riley == null) {
            return;
        }
        Instant now = Instant.now();
        List<ClassSession> ahead = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                now.plusMillis(12L * 3600000L), now.plusMillis(6L * Days.MILLIS));
        int booked = 0;
        long last = 0;
        // A different kind of class each time: three lunchtime yogas read as a seed.
        StringBuilder kinds = new StringBuilder();
        for (int iter = 0; iter < ahead.size() && booked < 3; iter++) {
            ClassSession session = ahead.get(iter);
            String type = session.getClassTypeId();
            boolean likes = HIIT.equals(type) || "type-yoga".equals(type)
                    || "type-pilates".equals(type);
            // One a day at most, and only where there is room.
            if (likes && kinds.indexOf(type + ";") < 0
                    && DOWNTOWN.equals(session.getLocationId())
                    && session.getBooked() < session.getCapacity()
                    && session.getStartsAt().toEpochMilli() - last > 20L * 3600000L
                    && bookings.findFirstBySessionIdAndMemberId(session.getId(),
                            riley.getId()) == null) {
                book(riley, session);
                last = session.getStartsAt().toEpochMilli();
                kinds.append(type).append(';');
                booked++;
            }
        }
    }

    /// A HIIT of Tess's that started a quarter of an hour ago, whatever the
    /// time the server comes up, with six people checked in: the trainer's
    /// live screen has a roster, and [DemoHeartRates] gives it pulses.
    private void liveClass() throws IOException {
        Instant now = Instant.now();
        ClassSession live = session(now.minusMillis(15L * 60000L), HIIT, DOWNTOWN,
                "room-studio-a", DemoAccounts.TRAINER);
        LocalDate today = LocalDate.now(ZoneId.of(zone));
        attended(self(DemoAccounts.SECOND_MEMBER), live, true);
        String[] names = {"Jordan Reyes", "Priya Natarajan", "Marcus Bell", "Hannah Okafor",
            "Sam Whitfield", "Elena Rossi", "Daniel Kim", "Grace Mensah", "Tom Fischer",
            "Aisha Rahman"};
        int[] ages = {27, 34, 45, 22, 58, 31, 39, 26, 49, 36};
        // Start days that fall on different days of the billing month, so the
        // owner's revenue chart is not one bar.
        int[] joined = {37, 52, 98, 143, 176, 12, 64, 121, 158, 201};
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
            // A profile, as signing up would have made: the friends list, the
            // feed and kudos name a person by it.
            Profile profile = new Profile();
            profile.setUsername(row.getAccount());
            profile.setDisplayName(names[iter]);
            profile.setCreatedAt(row.getCreatedAt());
            profiles.insert(profile);
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
                        && !TEEN.equals(session.getClassTypeId()) && next(9) == 0) {
                    attended(row, session, next(10) != 0);
                }
            }
            attended(row, live, true);
            // Three of them train with Riley too, so the friends' feed is a
            // handful of people and classes rather than one friend's week.
            if (iter == 0 || iter == 1 || iter == 3) {
                befriend(DemoAccounts.MEMBER, row.getAccount(), now);
                befriend(row.getAccount(), DemoAccounts.MEMBER, now);
            }
        }
    }

    /// This morning's walk-ins at the downtown desk, so the desk's day has
    /// people and takings in it: a day pass, a guest of a member and a free
    /// trial, the latest a few minutes ago.
    private void walkIns() throws IOException {
        Instant now = Instant.now();
        Instant midnight = LocalDate.now(ZoneId.of(zone)).atStartOfDay(ZoneId.of(zone)).toInstant();
        String[] names = {"Alex Romero", "Noor Haddad", "Ben Kowalski"};
        String[] kinds = {"DAY_PASS", "GUEST", "TRIAL"};
        String[] paidBy = {"CARD", "NONE", "NONE"};
        long[] cents = {2500L, 0L, 0L};
        long[] minutesAgo = {95L, 40L, 6L};
        for (int iter = 0; iter < names.length; iter++) {
            Instant at = now.minusMillis(minutesAgo[iter] * 60000L);
            if (at.isBefore(midnight)) {
                continue;
            }
            WalkIn row = new WalkIn();
            row.setId(Ids.next());
            row.setLocationId(DOWNTOWN);
            row.setName(names[iter]);
            row.setEmail("");
            row.setPhone("");
            row.setKind(kinds[iter]);
            row.setAmountCents(cents[iter]);
            row.setPaidBy(paidBy[iter]);
            row.setStaff(DemoAccounts.DESK);
            row.setAt(at);
            walkIns.insert(row);
        }
    }

    /// A week of shifts for both trainers, one of Theo's offered for swap so
    /// Tess has something under "Offered by others".
    private void shifts() throws IOException {
        LocalDate today = LocalDate.now(ZoneId.of(zone));
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
        Instant start = ZonedDateTime.of(day, LocalTime.of(hour, 0), ZoneId.of(zone)).toInstant();
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
        LocalDate today = LocalDate.now(ZoneId.of(zone));
        timetable(Studio.monday(today.minusWeeks(WEEKS_BACK)), today.plusWeeks(WEEKS_AHEAD));
    }

    /// The weekly timetable on every day from `first` to `last`, both included.
    private void timetable(LocalDate first, LocalDate last) throws IOException {
        String tess = DemoAccounts.TRAINER;
        String theo = DemoAccounts.SECOND_TRAINER;
        String maya = DemoAccounts.PILATES_TRAINER;
        String andre = DemoAccounts.RIDE_TRAINER;
        String sofia = DemoAccounts.RIVERSIDE_TRAINER;
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            DayOfWeek dow = day.getDayOfWeek();
            boolean weekday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
            if (weekday) {
                session(day, 6, 30, HIIT, DOWNTOWN, "room-studio-a", tess);
                session(day, 7, 30, "type-ride", DOWNTOWN, "room-ride", andre);
                session(day, 12, 15, "type-yoga", DOWNTOWN, "room-studio-a", theo);
                session(day, 17, 30, "type-barbell", DOWNTOWN, "room-floor", tess);
                session(day, 18, 30, "type-ride", DOWNTOWN, "room-ride", andre);
                session(day, 19, 30, "type-pilates", DOWNTOWN, "room-studio-a", maya);
                session(day, 7, 0, "type-yoga", RIVERSIDE, "room-river", sofia);
                session(day, 18, 0, HIIT, RIVERSIDE, "room-river", andre);
            }
            if (dow == DayOfWeek.TUESDAY || dow == DayOfWeek.THURSDAY) {
                session(day, 16, 0, LITTLE_MOVERS, DOWNTOWN, "room-kids", theo);
                session(day, 16, 30, TEEN, DOWNTOWN, "room-floor", tess);
            }
            if (dow == DayOfWeek.WEDNESDAY) {
                session(day, 10, 0, "type-steady", DOWNTOWN, "room-studio-a", maya);
            }
            if (dow == DayOfWeek.SATURDAY) {
                session(day, 9, 0, LITTLE_MOVERS, DOWNTOWN, "room-kids", theo);
                session(day, 10, 0, "type-kidsclimb", RIVERSIDE, "room-wall", sofia);
                session(day, 11, 0, TEEN, DOWNTOWN, "room-floor", tess);
                session(day, 9, 30, "type-yoga", RIVERSIDE, "room-river", sofia);
            }
            if (dow == DayOfWeek.SUNDAY) {
                session(day, 10, 0, "type-ride", DOWNTOWN, "room-ride", andre);
            }
        }
    }

    /// The demo accounts' memberships, children and the visits behind them.
    private void households() throws IOException {
        Instant now = Instant.now();
        String riley = DemoAccounts.MEMBER;
        LocalDate today = LocalDate.now(ZoneId.of(zone));
        Member rileySelf = self(riley);
        Member kai = child(KAI, riley, "Kai Park", today.minusYears(6).minusDays(40));
        Member mia = child(MIA, riley, "Mia Park", today.minusYears(13).minusDays(100));
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
        row.setTimeZone(zone);
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
        session(ZonedDateTime.of(day, LocalTime.of(hour, minute), ZoneId.of(zone)).toInstant(),
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
        return membership(member, planId, state, daysAgo, creditsLeft, now, now);
    }

    /// A membership whose bills stop with the period running at `stop`: the
    /// day a cancelled one was cancelled, or now.
    private Membership membership(Member member, String planId, String state, int daysAgo,
            int creditsLeft, Instant now, Instant stop) throws IOException {
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
            if (!recurring || periodEnd.isAfter(stop)) {
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
        attended(member, session, came, true);
    }

    /// The same, with the workout an adult recorded left out for `record`
    /// false.
    private void attended(Member member, ClassSession session, boolean came, boolean record)
            throws IOException {
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
            if (record && "SELF".equals(member.getRelation())
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
        // A shared workout gathers a little applause from the class, so the
        // friends' feed shows what kudos look like.
        int cheers = row.isShared() ? next(4) : 0;
        for (int iter = 0; iter < cheers; iter++) {
            Kudos one = new Kudos();
            one.setId(Ids.next());
            // The guests seeded in liveClass(): members with an account of
            // their own, so a feed that names the sender finds them.
            String from = "demo-guest-" + (iter + 1) + "@pulse.example";
            if (from.equals(member.getAccount())) {
                // Nobody cheers their own workout.
                from = "demo-guest-" + (iter + 6) + "@pulse.example";
            }
            one.setFromAccount(from);
            one.setToAccount(member.getAccount());
            one.setWorkoutId(row.getId());
            one.setCreatedAt(session.getEndsAt().plusSeconds(600L * (iter + 1)));
            kudos.insert(one);
        }
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

    // ----------------------------------------------------------------- crowd

    // Sixty of each, so a first name comes round at most twice in the crowd
    // and an alphabetical list does not read as one name over and over.
    private static final String[] FIRST = {"Alex", "Bea", "Carlos", "Dana", "Eli", "Fatima",
        "Gabe", "Hana", "Ivan", "Jade", "Kofi", "Lena", "Mateo", "Nora", "Omar", "Paula",
        "Quinn", "Rosa", "Sven", "Tara", "Umar", "Vera", "Wes", "Yusuf", "Zoe", "Amara",
        "Ben", "Chloe", "Diego", "Emma", "Farid", "Greta", "Hugo", "Ines", "Jonas", "Kira",
        "Luca", "Maya", "Nikhil", "Olivia", "Pablo", "Rina", "Sara", "Tomas", "Uma", "Victor",
        "Wendy", "Yara", "Zain", "Anya", "Bruno", "Celine", "Dev", "Esme", "Felix", "Gia",
        "Hiro", "Isla", "Jamal", "Keira"};
    private static final String[] LAST = {"Abbott", "Baker", "Castillo", "Dubois", "Evans",
        "Ferreira", "Gupta", "Holm", "Ibrahim", "Jensen", "Kowalski", "Larsen", "Mendez",
        "Nakamura", "Olsen", "Petrov", "Quigley", "Ruiz", "Sato", "Tanaka", "Ueda", "Varga",
        "Walsh", "Young", "Zhang", "Adeyemi", "Bauer", "Costa", "Novak", "Duarte", "Eriksen",
        "Fontaine", "Garcia", "Hughes", "Iyer", "Jovanovic", "Khan", "Lindgren", "Moreau",
        "Nguyen", "O'Brien", "Pham", "Quinlan", "Rossi", "Silva", "Torres", "Vogel", "Weber",
        "Yilmaz", "Ziegler", "Alvarado", "Brennan", "Chowdhury", "Dalton", "Esposito",
        "Fischer", "Greene", "Haddad", "Ito", "Keller"};
    private static final String[] KIDS_NAMES = {"Leo", "Ava", "Max", "Ivy", "Noah", "Lily",
        "Finn", "Ruby", "Theo", "Nina", "Otto", "Mila"};
    /// The kinds of class each sort of regular comes for. The evening crowd
    /// rides and does HIIT, which is why those classes fill.
    private static final String[][] TASTES = {
        {"type-ride", HIIT, "type-ride"},
        {HIIT, "type-barbell", "type-ride"},
        {"type-yoga", "type-pilates"},
        {"type-yoga", "type-yoga", "type-steady"},
        {"type-yoga", "type-ride"},
    };
    static final int EARLY = 0;
    static final int LUNCH = 1;
    static final int EVENING = 2;
    static final int WEEKEND = 3;
    static final int CROWD = 110;
    static final int HISTORY_WEEKS = 12;

    /// One synthetic member's habit: what they come to, when, how often, and
    /// the days between which they came.
    private static final class Regular {
        Member member;
        String[] tastes;
        int band;
        int perWeekTenths;
        Instant from;
        Instant until;
        int matches;
    }

    /// About a hundred and ten more members with a year of joining, a few
    /// months of leaving and twelve weeks of classes, so the owner's
    /// insights have a studio to describe. Every number comes from the fixed
    /// sequence, so every demo database tells the same story.
    private void crowd() throws IOException {
        Instant now = Instant.now();
        ZoneId here = ZoneId.of(zone);
        LocalDate today = LocalDate.now(here);
        // Nine older weeks of the same timetable, before the three every
        // database has.
        LocalDate oldest = Studio.monday(today.minusWeeks(HISTORY_WEEKS - 1));
        LocalDate base = Studio.monday(today.minusWeeks(WEEKS_BACK));
        timetable(oldest, base.minusDays(1));
        List<Regular> regulars = new ArrayList<Regular>();
        int cancelled = 0;
        int frozen = 0;
        int pastDue = 0;
        int kids = 0;
        for (int iter = 0; iter < CROWD; iter++) {
            String first = FIRST[iter % FIRST.length];
            String last = LAST[(iter * 37 + iter / FIRST.length * 7 + 11) % LAST.length];
            String name = first + " " + last;
            String account = "crowd-" + (iter + 1) + "@pulse.example";
            // More joined lately than a year ago: the square root bunches
            // the days towards now.
            double spread = Math.sqrt(next(10000) / 10000.0);
            int daysAgo = 2 + (int) (360 * (1.0 - spread));
            Member self = new Member();
            self.setId("member-crowd-" + (iter + 1));
            self.setAccount(account);
            self.setName(name);
            self.setBirthDate(today.minusYears(19 + next(45)).minusDays(next(365)));
            self.setRelation("SELF");
            self.setCreatedAt(now.minusMillis(daysAgo * Days.MILLIS));
            sign(self);
            members.insert(self);
            Profile profile = new Profile();
            profile.setUsername(account);
            profile.setDisplayName(name);
            profile.setCreatedAt(self.getCreatedAt());
            profiles.insert(profile);

            Regular habit = new Regular();
            habit.member = self;
            int taste = next(20);
            habit.tastes = TASTES[taste < 9 ? 0 : taste < 13 ? 1 : taste < 17 ? 2 : taste < 19
                    ? 3 : 4];
            habit.band = habit.tastes == TASTES[3] ? LUNCH : habit.tastes == TASTES[4] ? WEEKEND
                    : next(10) < (habit.tastes == TASTES[2] ? 4 : 2) ? EARLY : EVENING;
            // The evening crowd comes most, two to four times a week.
            habit.perWeekTenths = habit.band == EVENING ? 20 + next(21) : 5 + next(36);
            habit.from = self.getCreatedAt();
            habit.until = now;

            // The plan: mostly Unlimited, then Off-Peak for the daytime
            // crowd, Annual, and a few packs; one in eight is a parent who
            // signed a child up instead.
            int pick = next(100);
            if (pick < 14 && kids < KIDS_NAMES.length) {
                // Made here rather than by child(), which looks the parent up
                // and would not find one inserted in this transaction yet.
                Member child = new Member();
                child.setId("member-crowd-kid-" + (kids + 1));
                child.setAccount(account);
                child.setName(KIDS_NAMES[kids] + " " + last);
                child.setBirthDate(today.minusYears(5 + next(10)));
                child.setRelation("CHILD");
                child.setCreatedAt(self.getCreatedAt());
                sign(child);
                members.insert(child);
                kids++;
                membership(child, KIDS, "ACTIVE", daysAgo, 0, now);
                Regular young = new Regular();
                young.member = child;
                young.tastes = new String[] {LITTLE_MOVERS, "type-kidsclimb", TEEN};
                young.band = WEEKEND;
                young.perWeekTenths = 10 + next(11);
                young.from = habit.from;
                young.until = now;
                regulars.add(young);
                continue;
            }
            String plan = pick < 55 ? UNLIMITED : pick < 75 ? OFF_PEAK : pick < 88 ? ANNUAL
                    : PACK;
            if (OFF_PEAK.equals(plan) && habit.band != LUNCH && habit.band != WEEKEND) {
                // Off-Peak is weekday middays and weekends: its members come
                // then, for what is on then.
                boolean lunch = next(2) == 0;
                habit.band = lunch ? LUNCH : WEEKEND;
                habit.tastes = lunch ? TASTES[3] : TASTES[4];
                habit.perWeekTenths = 5 + next(21);
            }
            String state = "ACTIVE";
            Instant stop = now;
            if (cancelled < 15 && daysAgo > 40 && iter % 5 == 1) {
                // Left at some point in the last ninety days.
                cancelled++;
                state = "CANCELLED";
                stop = now.minusMillis((3L + next(Math.min(87, daysAgo - 30))) * Days.MILLIS);
                habit.until = stop;
            } else if (frozen < 4 && daysAgo > 45 && iter % 7 == 4) {
                frozen++;
                state = "FROZEN";
                habit.until = now.minusMillis((5L + next(10)) * Days.MILLIS);
            } else if (pastDue < 3 && OFF_PEAK.equals(plan) && daysAgo > 35 && iter % 5 == 3) {
                pastDue++;
                state = "PAST_DUE";
            } else if (iter % 13 == 5 && daysAgo > 30) {
                // At risk: has not been in for two weeks or more.
                habit.until = now.minusMillis((15L + next(15)) * Days.MILLIS);
            }
            Membership row = membership(self, plan, state, daysAgo,
                    PACK.equals(plan) ? 2 + next(7) : 0, now, stop);
            if ("CANCELLED".equals(state)) {
                row.setUpdatedAt(stop);
            } else if ("FROZEN".equals(state)) {
                row.setFrozenFrom(habit.until);
                row.setFrozenUntil(habit.until.plusMillis(30L * Days.MILLIS));
                row.setUpdatedAt(habit.until);
            } else if ("PAST_DUE".equals(state)) {
                overdue(row, 5900L, now);
                row.setUpdatedAt(now.minusMillis(4L * Days.MILLIS));
            }
            regulars.add(habit);
        }

        // The desk's guests from liveClass() kept their habit in the older
        // weeks too: half the adult classes Downtown, up to where their
        // history there begins.
        List<ClassSession> older = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                oldest.atStartOfDay(here).toInstant(),
                now.minusMillis(WEEKS_BACK * 7L * Days.MILLIS));
        for (int iter = 0; iter < 10; iter++) {
            Member guest = members.findById("member-guest-" + (iter + 1));
            for (int each = 0; guest != null && each < older.size(); each++) {
                ClassSession session = older.get(each);
                if (session.getStartsAt().isAfter(guest.getCreatedAt())
                        && DOWNTOWN.equals(session.getLocationId())
                        && !LITTLE_MOVERS.equals(session.getClassTypeId())
                        && !TEEN.equals(session.getClassTypeId()) && next(9) == 0) {
                    attended(guest, session, next(10) != 0, next(5) == 0);
                }
            }
        }

        // The classes: each regular comes to about as many a week as their
        // habit says, spread over the classes that suit them, while there
        // is a place.
        List<ClassSession> all = sessions.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                oldest.atStartOfDay(here).toInstant(), now.plusMillis(8L * Days.MILLIS));
        for (int each = 0; each < all.size(); each++) {
            ClassSession session = all.get(each);
            int band = band(session, here);
            for (int iter = 0; iter < regulars.size(); iter++) {
                Regular habit = regulars.get(iter);
                if (suits(habit, session, band) && session.getStartsAt().isAfter(habit.from)
                        && session.getStartsAt().isBefore(habit.until)) {
                    habit.matches++;
                }
            }
        }
        for (int each = 0; each < all.size(); each++) {
            ClassSession session = all.get(each);
            boolean over = session.getEndsAt().isBefore(now);
            boolean ahead = session.getStartsAt().isAfter(now);
            if (!over && !ahead || !Studio.DONE.equals(session.getState()) && !ahead) {
                continue;
            }
            int band = band(session, here);
            for (int iter = 0; iter < regulars.size(); iter++) {
                Regular habit = regulars.get(iter);
                if (session.getBooked() >= session.getCapacity()) {
                    break;
                }
                if (!suits(habit, session, band) || habit.matches == 0) {
                    continue;
                }
                Instant start = session.getStartsAt();
                if (!start.isAfter(habit.from) || over && !start.isBefore(habit.until)
                        || ahead && habit.until.isBefore(now)) {
                    continue;
                }
                long weeks = Math.max(1L, (habit.until.toEpochMilli()
                        - habit.from.toEpochMilli()) / (7L * Days.MILLIS));
                long active = Math.min(weeks, HISTORY_WEEKS + 1);
                // Visits wanted over the classes that suit, in thousandths.
                long chance = habit.perWeekTenths * active * 100L / habit.matches;
                if (next(1000) >= chance) {
                    continue;
                }
                if (ahead) {
                    book(habit.member, session);
                } else {
                    attended(habit.member, session, next(12) != 0, next(5) == 0);
                }
            }
        }
    }

    /// Early, lunch, evening, or the weekend, on the studio's clock.
    private static int band(ClassSession session, ZoneId here) {
        ZonedDateTime local = session.getStartsAt().atZone(here);
        DayOfWeek dow = local.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return WEEKEND;
        }
        int hour = local.getHour();
        return hour < 9 ? EARLY : hour < 15 ? LUNCH : EVENING;
    }

    private static boolean suits(Regular habit, ClassSession session, int band) {
        if (habit.band != band) {
            return false;
        }
        for (int iter = 0; iter < habit.tastes.length; iter++) {
            if (habit.tastes[iter].equals(session.getClassTypeId())) {
                return true;
            }
        }
        return false;
    }

    /// The next number in `[0, bound)` of the fixed sequence.
    private int next(int bound) {
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        return (int) ((seed >>> 33) % bound);
    }
}
