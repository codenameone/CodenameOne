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
package com.codenameone.examples.slate.booking;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.slate.Ids;
import com.codenameone.examples.slate.account.DemoAccounts;
import com.codenameone.examples.slate.api.ConfirmDto;
import com.codenameone.examples.slate.api.DaySlotsDto;
import com.codenameone.examples.slate.api.HoldRequestDto;
import com.codenameone.examples.slate.api.SlotDto;
import com.codenameone.examples.slate.catalog.Catalog;
import com.codenameone.examples.slate.catalog.Verticals;
import com.codenameone.examples.slate.domain.Appointment;
import com.codenameone.examples.slate.domain.AppointmentParty;
import com.codenameone.examples.slate.domain.Business;
import com.codenameone.examples.slate.domain.Location;
import com.codenameone.examples.slate.domain.Provider;
import com.codenameone.examples.slate.domain.Resource;
import com.codenameone.examples.slate.domain.Service;
import com.codenameone.examples.slate.domain.TravelTime;
import com.codenameone.examples.slate.domain.WorkingHours;
import com.codenameone.examples.slate.pay.Payments;
import com.codenameone.examples.slate.repository.AppointmentPartyRepository;
import com.codenameone.examples.slate.repository.AppointmentRepository;
import com.codenameone.examples.slate.repository.BusinessRepository;
import com.codenameone.examples.slate.repository.LocationRepository;
import com.codenameone.examples.slate.repository.ProviderRepository;
import com.codenameone.examples.slate.repository.ResourceRepository;
import com.codenameone.examples.slate.repository.ServiceRepository;
import com.codenameone.examples.slate.repository.TravelTimeRepository;
import com.codenameone.examples.slate.repository.WorkingHoursRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

// tag::slate-demo-business[]
/// The business a server starts with.
///
/// Everywhere, the business row itself, since nothing can be booked without
/// one: named, in `slate.timezone`, with the rules a small business would
/// start from. On a development profile also everything a demo needs -- two
/// locations twenty minutes apart, a handful of services of the vertical
/// `slate.demo.vertical` names (`SALON`, `TUTOR`, `TRAINER`, `CONSULTANT` or
/// `CLINIC`), three providers with a working week, the rooms or chairs, four
/// weeks of history and a few bookings to come.
///
/// The bookings to come are made through [Bookings] like any client's, so the
/// demo starts with nothing the slot engine would not have allowed. The
/// `test` profile gets the business and no bookings: each test books its own.
// end::slate-demo-business[]
@Component
public class DemoBusiness {
    public static final String MAIN_LOCATION = "loc-main";
    public static final String SECOND_LOCATION = "loc-north";

    private final Config config;
    private final DemoAccounts accounts;
    private final Bookings bookings;
    private final Availability availability;
    private final BusinessRepository businesses;
    private final LocationRepository locations;
    private final TravelTimeRepository travel;
    private final ServiceRepository services;
    private final ResourceRepository resources;
    private final ProviderRepository providers;
    private final WorkingHoursRepository hours;
    private final AppointmentRepository appointments;
    private final AppointmentPartyRepository parties;
    private final Catalog catalog;
    private final String vertical;
    private final String zone;
    private final boolean history;

    public DemoBusiness(Config config, DemoAccounts accounts, Bookings bookings,
            Availability availability, BusinessRepository businesses,
            LocationRepository locations, TravelTimeRepository travel, ServiceRepository services,
            ResourceRepository resources, ProviderRepository providers,
            WorkingHoursRepository hours, AppointmentRepository appointments,
            AppointmentPartyRepository parties, Catalog catalog,
            @Value("${slate.demo.vertical:SALON}") String vertical,
            @Value("${slate.timezone:Europe/London}") String zone,
            @Value("${slate.demo.history:false}") String history) {
        this.history = "true".equals(history == null ? "" : history.trim());
        this.config = config;
        this.accounts = accounts;
        this.bookings = bookings;
        this.availability = availability;
        this.businesses = businesses;
        this.locations = locations;
        this.travel = travel;
        this.services = services;
        this.resources = resources;
        this.providers = providers;
        this.hours = hours;
        this.appointments = appointments;
        this.parties = parties;
        this.catalog = catalog;
        String v = vertical.trim();
        this.vertical = Verticals.known(v) ? v : Verticals.SALON;
        this.zone = Catalog.zone(zone).getId();
    }

    @PostConstruct
    public void seed() throws IOException {
        boolean fresh = business();
        accounts.seed();
        if (!config.isDevelopmentProfile() || !fresh) {
            return;
        }
        write();
        // The test profile starts empty so the server tests count exactly what
        // they made; the simulator tests turn the history back on, because
        // their screenshots are the guide's pictures of a business in use.
        if (!"test".equals(config.getProfile()) || history) {
            history();
            upcoming();
        }
    }

    /// Makes the business row unless it exists. True when this call made it.
    @Transactional(rollbackFor = IOException.class)
    boolean business() {
        if (businesses.existsById(Catalog.MAIN)) {
            return false;
        }
        Business b = new Business();
        b.setId(Catalog.MAIN);
        b.setName(name());
        b.setVertical(vertical);
        b.setAbout("Book in seconds, see every free time, and get a reminder before you come.");
        b.setTimeZone(zone);
        b.setCurrency("GBP");
        b.setStepMinutes(15);
        b.setMinNoticeMinutes(120);
        b.setHorizonDays(60);
        b.setHoldMinutes(10);
        b.setCancelHours(24);
        b.setRescheduleHours(12);
        b.setDepositPercent(20);
        b.setNoShowFeePercent(50);
        b.setClientGapMinutes(0);
        b.setClientPerDay(2);
        b.setMinUsableGapMinutes(30);
        b.setPacking(Catalog.ANY);
        b.setReminderHours(24);
        b.setUpdatedAt(Instant.now());
        businesses.insert(b);
        return true;
    }

    @Transactional(rollbackFor = IOException.class)
    void write() throws IOException {
        String[] words = Verticals.words(vertical);
        location(MAIN_LOCATION, name(), "12 Market Street");
        location(SECOND_LOCATION, name() + " North", "48 Hill Road");
        trip(MAIN_LOCATION, SECOND_LOCATION, 20);
        trip(SECOND_LOCATION, MAIN_LOCATION, 20);
        for (int iter = 1; iter <= 3; iter++) {
            resource("res-" + iter, MAIN_LOCATION, words[5], words[5] + " " + iter);
        }
        resource("res-4", SECOND_LOCATION, words[5], words[5] + " 4");
        String[][] menu = menu(vertical, words[5]);
        for (int iter = 0; iter < menu.length; iter++) {
            service(iter, menu[iter]);
        }
        provider(DemoAccounts.PROVIDER, "#c2185b", MAIN_LOCATION, 8, 240, 15);
        provider(DemoAccounts.SECOND_PROVIDER, "#3949ab", MAIN_LOCATION, 0, 0, 0);
        provider(DemoAccounts.THIRD_PROVIDER, "#00897b", SECOND_LOCATION, 6, 0, 0);
        for (int day = 1; day <= 5; day++) {
            shift(DemoAccounts.PROVIDER, MAIN_LOCATION, day, 9 * 60, 17 * 60);
            shift(DemoAccounts.SECOND_PROVIDER, MAIN_LOCATION, day, 10 * 60, 18 * 60);
        }
        shift(DemoAccounts.SECOND_PROVIDER, MAIN_LOCATION, 6, 9 * 60, 13 * 60);
        // Mornings at the north site and afternoons at the main one on two
        // days: what the travel-time rule is for.
        shift(DemoAccounts.THIRD_PROVIDER, SECOND_LOCATION, 2, 9 * 60, 12 * 60);
        shift(DemoAccounts.THIRD_PROVIDER, MAIN_LOCATION, 2, 13 * 60, 17 * 60);
        shift(DemoAccounts.THIRD_PROVIDER, SECOND_LOCATION, 4, 9 * 60, 12 * 60);
        shift(DemoAccounts.THIRD_PROVIDER, MAIN_LOCATION, 4, 13 * 60, 17 * 60);
        shift(DemoAccounts.THIRD_PROVIDER, SECOND_LOCATION, 3, 9 * 60, 15 * 60);
        for (int iter = 0; iter < menu.length; iter++) {
            String id = "svc-" + (iter + 1);
            catalog.offer(DemoAccounts.PROVIDER, id);
            catalog.offer(DemoAccounts.SECOND_PROVIDER, id);
            if (iter % 2 == 0 || "2".equals(menu[iter][6])) {
                catalog.offer(DemoAccounts.THIRD_PROVIDER, id);
            }
        }
    }

    /// name, description, category, minutes, price, buffer after, providers
    /// needed, capacity, resource kind, online
    static String[][] menu(String vertical, String resource) {
        if (Verticals.TUTOR.equals(vertical)) {
            return new String[][] {
                {"Maths lesson", "One to one, at the pace that suits.", "Lessons", "60", "4500",
                    "10", "1", "1", "", "false"},
                {"Science lesson", "Physics, chemistry or biology.", "Lessons", "60", "4500",
                    "10", "1", "1", "", "false"},
                {"Exam workshop", "A small group working through past papers.", "Groups", "90",
                    "2500", "15", "1", "6", resource, "false"},
                {"Essay review", "Your essay, marked and talked through, online.", "Online",
                    "45", "3500", "0", "1", "1", "", "true"},
                {"Progress meeting", "With two tutors, for a parent and student.", "Meetings",
                    "30", "0", "0", "2", "1", resource, "false"},
            };
        }
        if (Verticals.TRAINER.equals(vertical)) {
            return new String[][] {
                {"Personal training", "An hour, one to one.", "Training", "60", "6000", "10",
                    "1", "1", resource, "false"},
                {"Fitness assessment", "Where you are, and a plan from there.", "Training",
                    "45", "4000", "15", "1", "1", resource, "false"},
                {"Small group", "Up to four people, one coach.", "Groups", "45", "2000", "15",
                    "1", "4", resource, "false"},
                {"Online check-in", "A video call to adjust your plan.", "Online", "30", "2500",
                    "0", "1", "1", "", "true"},
                {"Duo session", "Two coaches, for a pair or a hard lift.", "Training", "60",
                    "9000", "10", "2", "1", resource, "false"},
            };
        }
        if (Verticals.CONSULTANT.equals(vertical)) {
            return new String[][] {
                {"Introductory call", "Half an hour to see if we can help.", "Calls", "30", "0",
                    "0", "1", "1", "", "true"},
                {"Strategy session", "Ninety minutes on one problem.", "Sessions", "90",
                    "30000", "15", "1", "1", resource, "false"},
                {"Workshop", "A half day for a team, up to eight.", "Workshops", "120", "15000",
                    "30", "1", "8", resource, "false"},
                {"Review", "An hour on where things stand.", "Sessions", "60", "18000", "10",
                    "1", "1", resource, "false"},
                {"Joint session", "Two consultants, one room.", "Sessions", "60", "40000", "15",
                    "2", "1", resource, "false"},
            };
        }
        if (Verticals.CLINIC.equals(vertical)) {
            return new String[][] {
                {"Consultation", "A first appointment.", "Appointments", "30", "6000", "10",
                    "1", "1", resource, "false"},
                {"Follow-up", "Checking in after treatment.", "Appointments", "20", "4000",
                    "5", "1", "1", resource, "false"},
                {"Physiotherapy", "Assessment and treatment.", "Treatments", "45", "7000",
                    "15", "1", "1", resource, "false"},
                {"Video consultation", "From home.", "Online", "20", "4000", "0", "1", "1", "",
                    "true"},
                {"Rehab class", "A guided class of up to six.", "Classes", "60", "1500", "15",
                    "1", "6", resource, "false"},
            };
        }
        return new String[][] {
            {"Cut and finish", "Wash, cut and blow-dry.", "Hair", "45", "5500", "10", "1", "1",
                resource, "false"},
            {"Colour", "Full colour with a gloss.", "Hair", "120", "14000", "15", "1", "1",
                resource, "false"},
            {"Blow-dry", "Wash and style.", "Hair", "30", "3000", "5", "1", "1", resource,
                "false"},
            {"Style consultation", "A video call before a big change.", "Online", "20", "0",
                "0", "1", "1", "", "true"},
            {"Bridal trial", "Two stylists, hair and make-up together.", "Occasions", "90",
                "12000", "15", "2", "1", resource, "false"},
        };
    }

    /// Four weeks of finished bookings, so the dashboard has something to say.
    @Transactional(rollbackFor = IOException.class)
    void history() throws IOException {
        ZoneId z = ZoneId.of(zone);
        LocalDate today = LocalDate.now(z);
        String[] clients = {DemoAccounts.CLIENT, DemoAccounts.FLAGGED_CLIENT};
        String[] names = {"Riley Park", "Rae Okafor"};
        String[] who = {DemoAccounts.PROVIDER, DemoAccounts.SECOND_PROVIDER,
            DemoAccounts.THIRD_PROVIDER};
        // Three bookings a provider a day at most, two and a half hours apart:
        // past the longest service and its buffers, so no lane overlaps itself.
        int[] starts = {10 * 60, 12 * 60 + 30, 15 * 60};
        int n = 0;
        for (int back = 27; back >= 1; back--) {
            LocalDate day = today.minusDays(back);
            int weekday = day.getDayOfWeek().getValue();
            if (weekday > 5) {
                continue;
            }
            for (int p = 0; p < who.length; p++) {
                for (int slot = 0; slot < starts.length; slot++) {
                    // A salon's week: a quiet Monday and Tuesday, a full Friday, and
                    // gaps that move from week to week.
                    int quiet = weekday <= 2 ? 6 : weekday == 5 ? 0 : 4;
                    if ((back * 7 + p * 3 + slot * 5) % 10 < quiet) {
                        continue;
                    }
                    n++;
                    Service s = services.findById("svc-" + (1 + (n + p) % 3));
                    String state = n % 13 == 0 ? States.NO_SHOW : n % 9 == 0 ? States.CANCELLED
                            : States.COMPLETED;
                    past(s, clients[n % 2], names[n % 2], who[p], day, starts[slot], state);
                }
            }
        }
        // Today too: every day view opens on today, and a demo that opens on an empty
        // day shows nothing of the timeline. What has ended is finished, the rest is
        // still to come. They belong to the second demo client: the end-to-end tests
        // cancel whatever the first one holds between tests.
        String[] walkIns = {"Rae Okafor", "Sam Whitfield", "Ana Costa", "Jordan Lee",
            "Mira Haddad", "Noor Saleh", "Ben Carter", "Lena Vogel"};
        long now = System.currentTimeMillis();
        int k = 0;
        for (int p = 0; p < who.length; p++) {
            for (int slot = 0; slot < starts.length; slot++) {
                if ((p + slot) % 4 == 3) {
                    continue;
                }
                Service s = services.findById("svc-" + (1 + (k + p) % 3));
                long end = Availability.at(today, starts[slot], z) + s.getMinutes() * 60000L;
                String state = end > now ? States.CONFIRMED : k == 1 ? States.NO_SHOW
                        : States.COMPLETED;
                past(s, DemoAccounts.FLAGGED_CLIENT, walkIns[k % walkIns.length], who[p], today,
                        starts[slot], state);
                k++;
            }
        }
    }

    private void past(Service s, String client, String clientName, String provider,
            LocalDate day, int minute, String state) throws IOException {
        long start = Availability.at(day, minute, ZoneId.of(zone));
        long end = start + s.getMinutes() * 60000L;
        Appointment a = new Appointment();
        a.setId(Ids.next());
        a.setServiceId(s.getId());
        a.setLocationId(MAIN_LOCATION);
        a.setClient(client);
        a.setClientName(clientName);
        a.setStartsAt(Instant.ofEpochMilli(start));
        a.setEndsAt(Instant.ofEpochMilli(end));
        a.setBlockStart(Instant.ofEpochMilli(start - s.getBufferBefore() * 60000L));
        a.setBlockEnd(Instant.ofEpochMilli(end + s.getBufferAfter() * 60000L));
        a.setState(state);
        a.setHoldExpiresAt(Instant.ofEpochMilli(start));
        a.setPriceCents(s.getPriceCents());
        long deposit = s.getPriceCents() * 20 / 100;
        a.setDepositCents(deposit);
        a.setCurrency("GBP");
        a.setPaymentState(deposit > 0 && !States.CANCELLED.equals(state) ? States.PAY_CAPTURED
                : States.PAY_NONE);
        a.setShareToken(Ids.next());
        a.setRemindedAt(Instant.ofEpochMilli(start - 86400000L));
        a.setCreatedAt(Instant.ofEpochMilli(start - 7 * 86400000L));
        a.setUpdatedAt(Instant.ofEpochMilli(end));
        if (States.CANCELLED.equals(state)) {
            a.setCancelReason("Something came up");
        }
        appointments.insert(a);
        AppointmentParty p = new AppointmentParty();
        p.setId(Ids.next());
        p.setAppointmentId(a.getId());
        p.setKind(States.PROVIDER);
        p.setRef(provider);
        Provider row = providers.findById(provider);
        p.setName(row == null ? provider : row.getDisplayName());
        parties.insert(p);
    }

    /// A few bookings to come, made the way a client makes them.
    private void upcoming() throws IOException {
        ZoneId z = ZoneId.of(zone);
        LocalDate day = LocalDate.now(z).plusDays(1);
        int made = 0;
        for (int tries = 0; tries < 14 && made < 4; tries++, day = day.plusDays(1)) {
            String service = "svc-" + (1 + made % 3);
            String client = made % 2 == 0 ? DemoAccounts.CLIENT : DemoAccounts.FLAGGED_CLIENT;
            DaySlotsDto free = availability.day(client, service, MAIN_LOCATION, day.toString(),
                    "");
            if (free.slots().isEmpty()) {
                continue;
            }
            SlotDto slot = free.slots().get(Math.min(free.slots().size() - 1, 2 + made));
            String id = bookings.hold(client, made % 2 == 0 ? "Riley Park" : "Rae Okafor",
                    new HoldRequestDto(service, MAIN_LOCATION, slot.start(), slot.providers()))
                    .id();
            bookings.confirm(client, id, new ConfirmDto(Payments.CASH, "", new ArrayList<String>()));
            made++;
        }
    }

    private String name() {
        if (Verticals.TUTOR.equals(vertical)) {
            return "Brightside Tutoring";
        }
        if (Verticals.TRAINER.equals(vertical)) {
            return "Northfield Coaching";
        }
        if (Verticals.CONSULTANT.equals(vertical)) {
            return "Harbour Advisory";
        }
        if (Verticals.CLINIC.equals(vertical)) {
            return "Elm Street Clinic";
        }
        return "Slate Salon";
    }

    private void location(String id, String name, String address) {
        Location l = new Location();
        l.setId(id);
        l.setName(name);
        l.setAddress(address);
        l.setTimeZone(zone);
        l.setActive(true);
        locations.insert(l);
    }

    private void trip(String from, String to, int minutes) {
        TravelTime t = new TravelTime();
        t.setId(from + "|" + to);
        t.setFromLocation(from);
        t.setToLocation(to);
        t.setMinutes(minutes);
        travel.insert(t);
    }

    private void resource(String id, String location, String kind, String name) {
        Resource r = new Resource();
        r.setId(id);
        r.setLocationId(location);
        r.setKind(kind);
        r.setName(name);
        r.setActive(true);
        resources.insert(r);
    }

    private void service(int order, String[] row) {
        Service s = new Service();
        s.setId("svc-" + (order + 1));
        s.setName(row[0]);
        s.setDescription(row[1]);
        s.setCategory(row[2]);
        s.setMinutes(Integer.parseInt(row[3]));
        s.setPriceCents(Long.parseLong(row[4]));
        s.setBufferBefore(0);
        s.setBufferAfter(Integer.parseInt(row[5]));
        s.setProvidersNeeded(Integer.parseInt(row[6]));
        s.setCapacity(Integer.parseInt(row[7]));
        s.setResourceKind(row[8]);
        s.setOnline(Boolean.parseBoolean(row[9]));
        s.setMinNoticeMinutes(-1);
        s.setDepositPercent(-1);
        s.setColor(COLORS[order % COLORS.length]);
        s.setIntakePrompt(order == 0 ? "Anything we should know before you come?" : "");
        s.setActive(true);
        s.setSortOrder(order);
        services.insert(s);
    }

    private static final String[] COLORS = {"#e57373", "#64b5f6", "#81c784", "#ffb74d",
        "#9575cd"};

    private void provider(String username, String color, String location, int maxPerDay,
            int maxContinuous, int breakMinutes) {
        Provider p = providers.findById(username);
        if (p == null) {
            return;
        }
        p.setColor(color);
        p.setDefaultLocation(location);
        p.setBio("Ten years in the trade, and still loves the first appointment of the day.");
        p.setMaxPerDay(maxPerDay);
        p.setMaxContinuousMinutes(maxContinuous);
        p.setBreakMinutes(breakMinutes);
        providers.save(p);
    }

    private void shift(String username, String location, int weekday, int start, int end)
            throws IOException {
        WorkingHours w = new WorkingHours();
        w.setId(Ids.next());
        w.setUsername(username);
        w.setLocationId(location);
        w.setWeekday(weekday);
        w.setStartMinute(start);
        w.setEndMinute(end);
        hours.insert(w);
    }
}
