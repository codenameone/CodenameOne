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
package com.codenameone.examples.fieldline.dispatch;

import com.codename1.appkit.Net;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.fieldline.api.AccountChangeDto;
import com.codenameone.examples.fieldline.api.CrewMemberDto;
import com.codenameone.examples.fieldline.api.PolicyDto;
import com.codenameone.examples.fieldline.api.JobDto;
import com.codenameone.examples.fieldline.api.ScheduleRunDto;
import com.codenameone.examples.fieldline.api.UserDto;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.net.Session;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Layouts;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The back office's lists, each with what is opened from it: the jobs, the
/// planner's runs, and for an admin the crew's limits and the accounts.
///
/// Each list is filled in as it is made and shown through a [Host], which is
/// the console beside a wide screen's list or a screen of its own on a phone.
/// What an admin changes here the server checks again: a dispatcher who calls
/// `/api/admin` is refused, whatever the app shows.
final class BackOffice {
    /// The skills a technician can be given, as the server's catalog has them.
    private static final String[] SKILLS = {"ELECTRICAL", "HVAC", "PLUMBING", "FIRE",
        "ELEVATOR", "GAS", "SOLAR", "CONTROLS"};
    private static final String ALL = "all";
    private static final String WAITING = "waiting";
    private static final String URGENT = "urgent";
    private static final String DONE = "done";

    private BackOffice() {
    }

    // ---- Jobs ---------------------------------------------------------------

    /// Every job on the board, filtered.
    static Component jobs(final Host host) {
        final Container list = Ui.list("jobs");
        final String[] filter = {ALL};
        final List<JobDto> all = new ArrayList<JobDto>();
        Container filters = Ui.choice("jobFilter",
                new String[] {"All", "Unassigned", "Urgent", "Done"},
                new String[] {ALL, WAITING, URGENT, DONE}, ALL, v -> {
                    filter[0] = v;
                    fillJobs(host, list, all, v);
                });
        BoardForm.segments(filters);
        Api.dispatch().board(0, 0, Net.to(b -> {
            all.clear();
            all.addAll(b.jobs());
            fillJobs(host, list, all, filter[0]);
        }, Ui::fail));
        return listed(filters, list);
    }

    private static boolean matches(JobDto j, String filter) {
        boolean waiting = j.techId() == null || j.techId().length() == 0;
        boolean done = "COMPLETED".equals(j.status());
        if (WAITING.equals(filter)) {
            return waiting && !done;
        }
        if (URGENT.equals(filter)) {
            return j.priority() >= 4 && !done;
        }
        if (DONE.equals(filter)) {
            return done;
        }
        return true;
    }

    private static void fillJobs(final Host host, Container list, List<JobDto> all,
            String filter) {
        list.removeAll();
        int shown = 0;
        for (final JobDto j : all) {
            if (!matches(j, filter)) {
                continue;
            }
            shown++;
            boolean waiting = j.techId() == null || j.techId().length() == 0;
            final Container row = Ui.row(waiting ? FontImage.MATERIAL_ERROR_OUTLINE
                    : FontImage.MATERIAL_HANDYMAN, j.title(), j.siteName() + Ui.DOT
                    + (waiting ? (j.unassignedReason() == null || j.unassignedReason().length() == 0
                            ? Lang.tr("Unassigned") : Lang.tr(j.unassignedReason())) : j.techName()),
                    Ui.badge(Ui.status(j.status()), Ui.statusBadge(j.status())));
            list.add(Ui.tap(row, "job-" + j.id(), e -> {
                host.picked(row);
                host.detail(j.title(), "Job", job(j));
            }));
        }
        if (shown == 0) {
            list.add(Ui.empty(FontImage.MATERIAL_INBOX, "Nothing here",
                    "No job on the board matches."));
        }
        Layouts.dress(list);
        list.revalidate();
    }

    private static Component job(JobDto j) {
        Container page = Ui.page();
        Container chips = new Container(new FlowLayout());
        chips.add(Ui.badge(Ui.status(j.status()), Ui.statusBadge(j.status())));
        chips.add(Ui.badge(Lang.tr("Priority {0}", String.valueOf(j.priority())),
                j.priority() >= 4 ? "FlBadgeUrgent" : "FlBadge"));
        for (String skill : j.skills()) {
            chips.add(Ui.badge(skill, "FlBadgeKind"));
        }
        page.add(chips);
        if (j.description() != null && j.description().length() > 0) {
            page.add(Ui.text(j.description(), "FlCardText"));
        }
        Ui.section(page, "Where");
        page.add(Ui.fact("Site", j.siteName()));
        page.add(Ui.fact("Address", j.address()));
        page.add(Ui.fact("Customer", j.customer()));
        page.add(Ui.fact("Contact", j.contactPhone()));
        Ui.section(page, "When");
        page.add(Ui.fact("Customer window", Ui.when(j.windowStart()) + " - "
                + Ui.clock(j.windowEnd())));
        if (j.plannedStart() > 0) {
            page.add(Ui.fact("Planned", Ui.when(j.plannedStart()) + " - "
                    + Ui.clock(j.plannedEnd())));
            page.add(Ui.fact("Technician", j.techName()));
            page.add(Ui.fact("Drive before", Ui.duration(j.travelBefore())));
        }
        page.add(Ui.fact("Duration", Ui.duration(j.durationMinutes())));
        if (j.dueAt() > 0) {
            boolean late = j.plannedEnd() > j.dueAt();
            page.add(Ui.fact("Due by the SLA", Ui.when(j.dueAt()), late ? "FlFactBad" : null));
        }
        Ui.section(page, "Constraints");
        page.add(Ui.fact("Territory", Lang.tr(zoneLabel(j.territory()))));
        if (j.certifications() != null && !j.certifications().isEmpty()) {
            StringBuilder certs = new StringBuilder();
            for (String c : j.certifications()) {
                certs.append(certs.length() > 0 ? ", " : "").append(Lang.tr(certLabel(c)));
            }
            page.add(Ui.fact("Certificate needed", certs.toString()));
        }
        if (j.afterJobId() != null && j.afterJobId().length() > 0) {
            page.add(Ui.fact("Only after job", j.afterJobId()));
        }
        if (j.excludedTechs() != null && !j.excludedTechs().isEmpty()) {
            page.add(Ui.fact("Customer declined", String.valueOf(j.excludedTechs().size())
                    + " " + Lang.tr("technician(s)")));
        }
        if (j.unassignedReason() != null && j.unassignedReason().length() > 0) {
            page.add(Ui.banner(FontImage.MATERIAL_ERROR_OUTLINE, Lang.tr("Why it has no technician"),
                    Lang.tr(j.unassignedReason()), "WlBannerIconBad"));
        }
        if (j.assetType() != null && j.assetType().length() > 0) {
            Ui.section(page, "Equipment");
            page.add(Ui.fact("Asset", j.assetType()));
            page.add(Ui.fact("Model", j.assetModel()));
        }
        return page;
    }

    // ---- Planner runs -------------------------------------------------------

    /// Every optimization and re-plan, newest first.
    static Component runs(final Host host) {
        final Container list = Ui.list("runs");
        Api.dispatch().runs(Net.to(runs -> {
            list.removeAll();
            if (runs.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_HISTORY, "No runs yet",
                        "Every optimization and re-plan is kept here."));
            }
            for (final ScheduleRunDto r : runs) {
                final Container row = Ui.row(FontImage.MATERIAL_AUTO_FIX_HIGH,
                        RunView.kind(r.kind()), Ui.when(r.at()) + Ui.DOT + r.summary(), null);
                list.add(Ui.tap(row, "run-" + r.id(), e -> {
                    host.picked(row);
                    host.detail(RunView.kind(r.kind()), "Run", RunForm.page(r));
                }));
            }
            Layouts.dress(list);
            list.revalidate();
        }, Ui::fail));
        return list;
    }

    // ---- Crew (admin) -------------------------------------------------------

    /// The crew and what the planner may ask of each of them.
    static Component crew(final Host host) {
        final Container list = Ui.list("crew");
        Api.admin().technicians(Net.to(crew -> {
            list.removeAll();
            for (final CrewMemberDto c : crew) {
                final Container row = Ui.row(FontImage.MATERIAL_ENGINEERING, c.name(),
                        join(c.skills()) + Ui.DOT + c.homeLabel(), c.active() ? null
                                : Ui.badge(Lang.tr("Inactive"), "FlBadgeWarn"));
                list.add(Ui.tap(row, "tech-" + c.id(), e -> {
                    host.picked(row);
                    host.detail(c.name(), "CrewMember", member(host, c));
                }));
            }
            Layouts.dress(list);
            list.revalidate();
        }, Ui::fail));
        return list;
    }

    private static Component member(final Host host, final CrewMemberDto c) {
        Container page = Ui.page();
        page.add(Ui.person(c.name(), c.username() == null ? "" : c.username(), null));
        page.add(Ui.fact("Starts from", c.homeLabel()));
        Ui.section(page, "Skills");
        final List<CheckBox> skills = new ArrayList<CheckBox>();
        Container boxes = new Container(new FlowLayout());
        for (String s : SKILLS) {
            CheckBox box = CheckBox.createToggle(s);
            box.setUIID("FlSkillToggle");
            box.setName("skill-" + s);
            box.setSelected(c.skills().contains(s));
            skills.add(box);
            boxes.add(box);
        }
        page.add(boxes);
        Ui.section(page, "Certificates");
        page.add(Ui.text("A job on gas, high voltage, a roof or a fire system goes only to "
                + "someone whose certificate is valid on the day. Leave the date empty for one "
                + "that does not lapse.", "WlMuted"));
        final List<CheckBox> certs = new ArrayList<CheckBox>();
        final List<TextField> expiries = new ArrayList<TextField>();
        for (String code : CERTIFICATIONS) {
            String held = held(c.certifications(), code);
            CheckBox box = CheckBox.createToggle(Lang.tr(certLabel(code)));
            box.setUIID("FlSkillToggle");
            box.setName("cert-" + code);
            box.setSelected(held != null);
            TextField until = new TextField(held == null ? "" : held, "", 10, TextArea.ANY);
            until.setHint(Lang.tr("Valid until, 2027-03-31"));
            until.setName("certUntil-" + code);
            until.setUIID("WlField");
            Container line = new Container(new BorderLayout());
            line.add(BorderLayout.WEST, box);
            line.add(BorderLayout.CENTER, until);
            if (held != null && held.length() > 0 && lapsesSoon(held)) {
                line.add(BorderLayout.SOUTH, Ui.label(Lang.tr("Lapses on {0}", held), "FlBadgeUrgent"));
            }
            certs.add(box);
            expiries.add(until);
            page.add(line);
        }
        Ui.section(page, "Territories");
        page.add(Ui.text("Where they are sent first. Outside them costs the plan extra, or is "
                + "refused when the rules say territories are strict; none means anywhere.",
                "WlMuted"));
        final List<CheckBox> zones = new ArrayList<CheckBox>();
        Container zoneBoxes = new Container(new FlowLayout());
        for (String zone : TERRITORIES) {
            CheckBox box = CheckBox.createToggle(Lang.tr(zoneLabel(zone)));
            box.setUIID("FlSkillToggle");
            box.setName("zone-" + zone);
            box.setSelected(c.territories() != null && c.territories().contains(zone));
            zones.add(box);
            zoneBoxes.add(box);
        }
        page.add(zoneBoxes);
        Ui.section(page, "A day");
        final TextField jobsADay = Ui.field(page, "Most jobs in a day", "maxJobs", "",
                TextArea.NUMERIC);
        jobsADay.setText(String.valueOf(c.maxJobsPerDay()));
        final TextField longest = Ui.field(page, "Longest day, hours (8.5 or 8:30)", "maxDay", "",
                TextArea.ANY);
        longest.setText(hours(c.maxMinutesPerDay()));
        final TextField drive = Ui.field(page, "Most driving in a day, minutes", "maxDrive",
                "", TextArea.NUMERIC);
        drive.setText(String.valueOf(c.maxDriveMinutes()));
        Ui.section(page, "A week");
        final TextField regular = Ui.field(page, "Regular hours a week", "regularHours", "",
                TextArea.ANY);
        regular.setText(hours(c.regularMinutesPerWeek()));
        final TextField most = Ui.field(page, "Most hours a week, overtime included",
                "maxHours", "", TextArea.ANY);
        most.setText(hours(c.maxMinutesPerWeek()));
        Ui.section(page, "Pay");
        final String[] rate = {c.rateId() == null ? "" : c.rateId()};
        final Container rates = new Container(BoxLayout.y());
        page.add(rates);
        Api.admin().rates(Net.to(all -> {
            String[] labels = new String[all.size() + 1];
            String[] ids = new String[all.size() + 1];
            labels[0] = Lang.tr("None");
            ids[0] = "";
            for (int iter = 0; iter < all.size(); iter++) {
                labels[iter + 1] = all.get(iter).name();
                ids[iter + 1] = all.get(iter).id();
            }
            rates.add(Ui.choice("rate", labels, ids, rate[0], v -> rate[0] = v, 2));
            Layouts.dress(rates);
            if (rates.getComponentForm() != null) {
                rates.getComponentForm().revalidate();
            }
        }, Ui::fail));
        final boolean[] active = {c.active()};
        page.add(Ui.row(FontImage.MATERIAL_EVENT_AVAILABLE, Lang.tr("Takes work"),
                Lang.tr("Off, the planner gives them nothing"),
                Ui.toggle("active", c.active(), on -> active[0] = on)));
        page.add(Ui.text("Saving re-plans the open work around the new limits.", "WlMuted"));
        page.add(Ui.primary("Save", "saveTech", e -> {
            List<String> chosen = new ArrayList<String>();
            for (CheckBox box : skills) {
                if (box.isSelected()) {
                    chosen.add(box.getText());
                }
            }
            List<String> heldCerts = new ArrayList<String>();
            for (int iter = 0; iter < certs.size(); iter++) {
                if (certs.get(iter).isSelected()) {
                    String until = expiries.get(iter).getText() == null ? ""
                            : expiries.get(iter).getText().trim();
                    heldCerts.add(until.length() == 0 ? CERTIFICATIONS[iter]
                            : CERTIFICATIONS[iter] + "=" + until);
                }
            }
            List<String> chosenZones = new ArrayList<String>();
            for (int iter = 0; iter < zones.size(); iter++) {
                if (zones.get(iter).isSelected()) {
                    chosenZones.add(TERRITORIES[iter]);
                }
            }
            CrewMemberDto changed = new CrewMemberDto(c.id(), c.name(), c.initials(),
                    c.color(), c.username(), chosen, c.homeLabel(), number(drive),
                    minutes(regular, c.regularMinutesPerWeek()),
                    minutes(most, c.maxMinutesPerWeek()), active[0], heldCerts, chosenZones,
                    number(jobsADay), minutes(longest, c.maxMinutesPerDay()), rate[0]);
            Api.admin().changeTechnician(changed, Net.to(saved -> {
                Ui.say(Lang.tr("{0} saved", saved.name()));
                host.reload();
            }, Ui::fail));
        }));
        return page;
    }

    // ---- Rules (admin) ------------------------------------------------------

    /// The company's planning rules: the break, the SLA it promises, whether
    /// territories bind, and how the optimizer trades one cost against another.
    static Component rules() {
        final Container page = Ui.page();
        page.setName("rules");
        Api.admin().policy(Net.to(p -> {
            page.removeAll();
            fillRules(page, p);
            Layouts.dress(page);
            if (page.getComponentForm() != null) {
                page.getComponentForm().revalidate();
            }
        }, Ui::fail));
        return page;
    }

    private static void fillRules(final Container page, final PolicyDto p) {
        Ui.section(page, "Break");
        page.add(Ui.text("Everyone gets an unpaid break. The planner fits it between jobs, "
                + "inside the window below, counted from the start of the shift.", "WlMuted"));
        final TextField brk = Ui.field(page, "Break, minutes", "breakMinutes", "", TextArea.NUMERIC);
        brk.setText(String.valueOf(p.breakMinutes()));
        final TextField from = Ui.field(page, "Not before, hours into the shift", "breakAfter", "",
                TextArea.ANY);
        from.setText(hours(p.breakAfterMinutes()));
        final TextField by = Ui.field(page, "Taken by, hours into the shift", "breakBefore", "",
                TextArea.ANY);
        by.setText(hours(p.breakBeforeMinutes()));
        Ui.section(page, "Rush hours");
        page.add(Ui.text("Traffic in these hours makes every drive that sets off inside them take "
                + "longer, so the planner books fewer jobs across town at the start and end of "
                + "the day.", "WlMuted"));
        final TextField amFrom = Ui.field(page, "Morning rush from, 7:00", "rushAmFrom", "", TextArea.ANY);
        amFrom.setText(dayTime(p.rushAmFrom()));
        final TextField amTo = Ui.field(page, "Morning rush until", "rushAmTo", "", TextArea.ANY);
        amTo.setText(dayTime(p.rushAmTo()));
        final TextField pmFrom = Ui.field(page, "Evening rush from", "rushPmFrom", "", TextArea.ANY);
        pmFrom.setText(dayTime(p.rushPmFrom()));
        final TextField pmTo = Ui.field(page, "Evening rush until", "rushPmTo", "", TextArea.ANY);
        pmTo.setText(dayTime(p.rushPmTo()));
        final TextField extra = Ui.field(page, "Extra driving time in rush hour, percent", "rushExtra",
                "", TextArea.NUMERIC);
        extra.setText(String.valueOf(p.rushExtraPct()));
        Ui.section(page, "Time tracking");
        page.add(Ui.text("Technicians clock in and out on their Today screen, and setting off "
                + "or arriving records the drive and the time on site. Times round to the "
                + "nearest step, and a shift nobody clocked out of closes itself.", "WlMuted"));
        final TextField rounding = Ui.field(page, "Round clocked time to, minutes", "rounding", "",
                TextArea.NUMERIC);
        rounding.setText(String.valueOf(p.timeRoundingMinutes()));
        final TextField autoOut = Ui.field(page, "Close a forgotten shift after, hours", "autoClockOut",
                "", TextArea.NUMERIC);
        autoOut.setText(String.valueOf(p.autoClockOutHours()));
        Ui.section(page, "Service levels");
        page.add(Ui.text("When a job is due from the moment it is booked. A job planned past "
                + "it still goes ahead, and the board counts it late.", "WlMuted"));
        final TextField urgent = Ui.field(page, "Urgent work, hours", "urgentSla", "",
                TextArea.NUMERIC);
        urgent.setText(String.valueOf(p.urgentSlaHours()));
        final TextField routine = Ui.field(page, "Routine work, hours", "routineSla", "",
                TextArea.NUMERIC);
        routine.setText(String.valueOf(p.routineSlaHours()));
        Ui.section(page, "Territories");
        final boolean[] strict = {p.strictTerritories()};
        page.add(Ui.row(FontImage.MATERIAL_MAP, Lang.tr("Strict territories"),
                Lang.tr("On, nobody is sent outside their own; off, it only costs extra"),
                Ui.toggle("strictTerritories", p.strictTerritories(), on -> strict[0] = on)));
        Ui.section(page, "What the planner weighs");
        page.add(Ui.text("Each figure is what one unit costs the plan, against a minute of "
                + "driving. Raise one and the optimizer gives up more of the others to avoid it.",
                "WlMuted"));
        final TextField travel = weight(page, "A minute of driving", "wTravel", p.travelWeight());
        final TextField overtime = weight(page, "A minute of overtime", "wOvertime", p.overtimeWeight());
        final TextField late = weight(page, "A minute past the SLA, per priority point", "wLate",
                p.latenessWeight());
        final TextField territory = weight(page, "A job outside someone's territory", "wTerritory",
                p.territoryWeight());
        final TextField continuity = weight(page, "Moving a job someone already had", "wContinuity",
                p.continuityWeight());
        final TextField balance = weight(page, "An uneven day across the crew", "wBalance",
                p.balanceWeight());
        page.add(Ui.text("Saving re-plans the open work under the new rules.", "WlMuted"));
        page.add(Ui.primary("Save", "saveRules", e -> {
            PolicyDto changed = new PolicyDto(number(brk), minutes(from, p.breakAfterMinutes()),
                    minutes(by, p.breakBeforeMinutes()), strict[0], number(travel),
                    number(overtime), number(late), number(continuity), number(balance),
                    number(territory), number(urgent), number(routine), minutes(amFrom, -1),
                    minutes(amTo, -1), minutes(pmFrom, -1), minutes(pmTo, -1), number(extra),
                    number(rounding), number(autoOut));
            Api.admin().changePolicy(changed, Net.to(saved -> {
                Ui.say(Lang.tr("Rules saved; the plan was repaired"));
                page.removeAll();
                fillRules(page, saved);
                Layouts.dress(page);
                page.getComponentForm().revalidate();
            }, Ui::fail));
        }));
    }

    private static TextField weight(Container page, String caption, String name, int value) {
        TextField field = Ui.field(page, caption, name, "", TextArea.NUMERIC);
        field.setText(String.valueOf(value));
        return field;
    }

    // ---- Accounts (admin) ---------------------------------------------------

    /// Everyone who can sign in, and what each may do.
    static Component accounts(final Host host) {
        final Container list = Ui.list("accounts");
        Api.admin().accounts(Net.to(users -> {
            list.removeAll();
            for (final UserDto u : users) {
                final Container row = Ui.row(FontImage.MATERIAL_PERSON, u.displayName(),
                        u.username() + Ui.DOT + roles(u), u.suspended()
                                ? Ui.badge(Lang.tr("Suspended"), "FlBadgeUrgent") : null);
                list.add(Ui.tap(row, "account-" + u.username(), e -> {
                    host.picked(row);
                    host.detail(u.displayName(), "Account", account(host, u));
                }));
            }
            Layouts.dress(list);
            list.revalidate();
        }, Ui::fail));
        return list;
    }

    private static Component account(final Host host, final UserDto u) {
        Container page = Ui.page();
        page.add(Ui.person(u.displayName(), u.username(), null));
        final boolean[] r = {u.technician(), u.dispatcher(), u.admin(), u.suspended()};
        UserDto me = Session.user();
        boolean self = me != null && me.username().equals(u.username());
        Ui.section(page, "Roles");
        page.add(Ui.row(FontImage.MATERIAL_ENGINEERING, Lang.tr("Technician"),
                Lang.tr("Has a day of jobs and is planned for"),
                Ui.toggle("role-technician", r[0], on -> r[0] = on)));
        page.add(Ui.row(FontImage.MATERIAL_VIEW_TIMELINE, Lang.tr("Dispatcher"),
                Lang.tr("Runs the board and the planner"),
                Ui.toggle("role-dispatcher", r[1], on -> r[1] = on)));
        page.add(Ui.row(FontImage.MATERIAL_ADMIN_PANEL_SETTINGS, Lang.tr("Admin"),
                Lang.tr("Manages accounts and the crew"),
                Ui.toggle("role-admin", r[2], on -> r[2] = on)));
        Ui.section(page, "Access");
        page.add(Ui.row(FontImage.MATERIAL_BLOCK, Lang.tr("Suspended"),
                Lang.tr("Signed out everywhere and cannot sign in"),
                Ui.toggle("suspended", r[3], on -> r[3] = on)));
        if (self) {
            page.add(Ui.text("This is your own account: you cannot take away your own admin "
                    + "role or suspend yourself.", "WlMuted"));
        }
        Button save = Ui.primary("Save", "saveAccount", e -> Api.admin().changeAccount(
                new AccountChangeDto(u.username(), r[0], r[1], r[2], r[3]), Net.to(saved -> {
                    Ui.say(Lang.tr("{0} saved", saved.displayName()));
                    host.reload();
                }, Ui::fail)));
        page.add(save);
        return page;
    }

    // ---- Helpers ------------------------------------------------------------

    /// A list with what filters it above it.
    static Container listed(Component filters, Component list) {
        Container content = new Container(new BorderLayout());
        if (filters != null) {
            Container top = new Container(BoxLayout.y());
            top.setUIID("FlFiltersBar");
            top.add(filters);
            content.add(BorderLayout.NORTH, top);
        }
        content.add(BorderLayout.CENTER, list);
        return content;
    }

    private static String roles(UserDto u) {
        StringBuilder out = new StringBuilder();
        if (u.admin()) {
            out.append(Lang.tr("Admin"));
        }
        if (u.dispatcher()) {
            out.append(out.length() > 0 ? ", " : "").append(Lang.tr("Dispatcher"));
        }
        if (u.technician()) {
            out.append(out.length() > 0 ? ", " : "").append(Lang.tr("Technician"));
        }
        return out.toString();
    }

    private static String join(List<String> skills) {
        StringBuilder out = new StringBuilder();
        for (String s : skills) {
            out.append(out.length() > 0 ? ", " : "").append(s);
        }
        return out.toString();
    }

    static final String[] CERTIFICATIONS = {"GAS_SAFE", "HIGH_VOLTAGE", "WORK_AT_HEIGHT",
        "CONFINED_SPACE", "REFRIGERANT", "FIRE_INSPECTOR"};
    static final String[] TERRITORIES = {"CENTRAL", "NORTH", "EAST", "SOUTH", "WEST"};

    static String certLabel(String code) {
        String[][] names = {{"GAS_SAFE", "Gas safe"}, {"HIGH_VOLTAGE", "High voltage"},
            {"WORK_AT_HEIGHT", "Work at height"}, {"CONFINED_SPACE", "Confined space"},
            {"REFRIGERANT", "Refrigerant handling"}, {"FIRE_INSPECTOR", "Fire inspector"}};
        for (String[] n : names) {
            if (n[0].equals(code)) {
                return n[1];
            }
        }
        return code;
    }

    static String zoneLabel(String code) {
        String[][] names = {{"CENTRAL", "Central"}, {"NORTH", "North"}, {"EAST", "East"},
            {"SOUTH", "South"}, {"WEST", "West"}};
        for (String[] n : names) {
            if (n[0].equals(code)) {
                return n[1];
            }
        }
        return code;
    }

    /// The expiry recorded for `code` ("" for one that does not lapse), or
    /// null when it is not held.
    static String held(List<String> certs, String code) {
        if (certs != null) {
            for (String c : certs) {
                if (c.equals(code)) {
                    return "";
                }
                if (c.startsWith(code + "=")) {
                    return c.substring(code.length() + 1);
                }
            }
        }
        return null;
    }

    /// Whether a year-month-day date is within the next two weeks. Compared as
    /// text, which orders correctly for that format.
    private static boolean lapsesSoon(String date) {
        java.text.SimpleDateFormat iso = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String soon = iso.format(new java.util.Date(System.currentTimeMillis() + 14 * 86400000L));
        return date.length() == 10 && date.compareTo(soon) <= 0;
    }

    /// A minute of the day as a clock time: 7:00, 16:30.
    static String dayTime(int minute) {
        int m = minute % 60;
        return (minute / 60) + ":" + (m < 10 ? "0" : "") + m;
    }

    /// Minutes as hours, with the minutes only when there are some: 40, 8:30.
    static String hours(int minutes) {
        int m = minutes % 60;
        return (minutes / 60) + (m == 0 ? "" : ":" + (m < 10 ? "0" : "") + m);
    }

    /// Hours written as 8, 8.5 or 8:30, in minutes. Text that is none of
    /// those keeps `previous` only when the field was left as it was shown;
    /// anything else is 0, which the server refuses with its reason.
    static int minutes(TextField field, int previous) {
        String text = field.getText() == null ? "" : field.getText().trim();
        if (text.equals(hours(previous))) {
            return previous;
        }
        int colon = text.indexOf(':');
        int dot = text.indexOf('.');
        if (colon > 0) {
            int h = digits(text.substring(0, colon));
            int m = digits(text.substring(colon + 1));
            return h < 0 || m < 0 || m > 59 ? 0 : h * 60 + m;
        }
        if (dot > 0) {
            int h = digits(text.substring(0, dot));
            String frac = text.substring(dot + 1);
            int f = digits(frac);
            if (h < 0 || f < 0 || frac.length() > 2) {
                return 0;
            }
            int scale = frac.length() == 1 ? 10 : 100;
            return h * 60 + (f * 60 + scale / 2) / scale;
        }
        int h = digits(text);
        return h < 0 ? 0 : h * 60;
    }

    private static int digits(String text) {
        if (text.length() == 0 || text.length() > 5) {
            return -1;
        }
        int value = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char ch = text.charAt(iter);
            if (ch < '0' || ch > '9') {
                return -1;
            }
            value = value * 10 + (ch - '0');
        }
        return value;
    }

    /// The whole number in `field`, or 0 for anything else, which the server
    /// then refuses with its reason.
    private static int number(TextField field) {
        String text = field.getText() == null ? "" : field.getText().trim();
        if (text.length() == 0 || text.length() > 6) {
            return 0;
        }
        int value = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char ch = text.charAt(iter);
            if (ch < '0' || ch > '9') {
                return 0;
            }
            value = value * 10 + (ch - '0');
        }
        return value;
    }
}
