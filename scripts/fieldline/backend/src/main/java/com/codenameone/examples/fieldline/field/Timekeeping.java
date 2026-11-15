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
package com.codenameone.examples.fieldline.field;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.fieldline.Ids;
import com.codenameone.examples.fieldline.api.RateDto;
import com.codenameone.examples.fieldline.api.TimeEntryDto;
import com.codenameone.examples.fieldline.api.TimesheetDto;
import com.codenameone.examples.fieldline.api.TimesheetRowDto;
import com.codenameone.examples.fieldline.domain.Job;
import com.codenameone.examples.fieldline.domain.LabourRate;
import com.codenameone.examples.fieldline.domain.PlannerPolicy;
import com.codenameone.examples.fieldline.domain.Technician;
import com.codenameone.examples.fieldline.domain.TimeEntry;
import com.codenameone.examples.fieldline.domain.WorkWindow;
import com.codenameone.examples.fieldline.repository.JobRepository;
import com.codenameone.examples.fieldline.repository.LabourRateRepository;
import com.codenameone.examples.fieldline.repository.TechnicianRepository;
import com.codenameone.examples.fieldline.repository.TimeEntryRepository;
import com.codenameone.examples.fieldline.repository.WorkWindowRepository;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/// Labour rates, the clock and timesheets.
///
/// A technician clocks a SHIFT in and out; the job flow records the rest
/// without being asked: setting off opens a TRAVEL span, arriving closes it
/// and opens a JOB span, finishing closes that. Setting off while off the
/// clock clocks the technician in, since they are plainly working.
///
/// Times are rounded to the company's increment when they are written, and a
/// shift left open longer than the company allows is read as ending there --
/// the forgotten clock-out every timesheet system has to answer for.
@Component
public class Timekeeping {
    public static final String SHIFT = "SHIFT";
    public static final String TRAVEL = "TRAVEL";
    public static final String JOB = "JOB";

    private final LabourRateRepository rates;
    private final TimeEntryRepository entries;
    private final TechnicianRepository technicians;
    private final JobRepository jobs;
    private final Planner planner;
    private final WorkWindowRepository windows;

    public Timekeeping(LabourRateRepository rates, TimeEntryRepository entries,
            TechnicianRepository technicians, JobRepository jobs, Planner planner,
            WorkWindowRepository windows) {
        this.windows = windows;
        this.rates = rates;
        this.entries = entries;
        this.technicians = technicians;
        this.jobs = jobs;
        this.planner = planner;
    }

    // ------------------------------------------------------------ rates

    @Transactional(readOnly = true)
    public List<RateDto> rates() {
        Map<String, int[]> members = new HashMap<String, int[]>();
        for (Technician t : technicians.findAll()) {
            int[] c = members.get(t.getRateId());
            if (c == null) {
                c = new int[1];
                members.put(t.getRateId(), c);
            }
            c[0]++;
        }
        List<LabourRate> all = new ArrayList<LabourRate>(rates.findAll());
        all.sort((a, b) -> a.getHourlyCents() - b.getHourlyCents());
        List<RateDto> out = new ArrayList<RateDto>();
        for (LabourRate r : all) {
            int[] c = members.get(r.getId());
            out.add(new RateDto(r.getId(), r.getName(), r.getHourlyCents(), r.getOvertimePct(),
                    r.getWeekendPct(), r.getCalloutCents(), c == null ? 0 : c[0]));
        }
        return out;
    }

    /// Adds, changes or (with an empty name) removes a rate. A rate someone
    /// is paid at cannot be removed: their cost would silently become zero.
    @Transactional(rollbackFor = IOException.class)
    public List<RateDto> saveRate(RateDto wanted) throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "No rate sent");
        }
        String id = wanted.id() == null ? "" : wanted.id();
        LabourRate r = id.length() == 0 ? null : rates.findById(id);
        if (id.length() > 0 && r == null) {
            throw new ResponseStatusException(404, "No such rate");
        }
        String name = wanted.name() == null ? "" : wanted.name().trim();
        if (name.length() == 0) {
            if (r == null) {
                throw new ResponseStatusException(400, "Name the rate");
            }
            for (Technician t : technicians.findAll()) {
                if (r.getId().equals(t.getRateId())) {
                    throw new ResponseStatusException(409,
                            "Move " + t.getName() + " to another rate first");
                }
            }
            rates.delete(r);
            return rates();
        }
        if (wanted.hourlyCents() < 100 || wanted.hourlyCents() > 100000) {
            throw new ResponseStatusException(400, "The hourly rate is between 1.00 and 1000.00");
        }
        if (wanted.overtimePct() < 100 || wanted.overtimePct() > 300
                || wanted.weekendPct() < 100 || wanted.weekendPct() > 300) {
            throw new ResponseStatusException(400,
                    "Overtime and weekend rates are 100% to 300% of the hourly rate");
        }
        if (wanted.calloutCents() < 0 || wanted.calloutCents() > 100000) {
            throw new ResponseStatusException(400, "The call-out fee is between 0 and 1000.00");
        }
        boolean fresh = r == null;
        if (fresh) {
            r = new LabourRate();
            r.setId("rate" + Ids.next().substring(0, 8));
        }
        r.setName(name.length() > 80 ? name.substring(0, 80) : name);
        r.setHourlyCents(wanted.hourlyCents());
        r.setOvertimePct(wanted.overtimePct());
        r.setWeekendPct(wanted.weekendPct());
        r.setCalloutCents(wanted.calloutCents());
        if (fresh) {
            rates.insert(r);
        } else {
            rates.save(r);
        }
        return rates();
    }

    @Transactional(readOnly = true)
    public boolean rateExists(String id) {
        return id != null && id.length() > 0 && rates.existsById(id);
    }

    // ------------------------------------------------------------ the clock

    /// When the technician's open shift began, or 0 when they are off the
    /// clock. A shift past the automatic clock-out is closed here.
    @Transactional(rollbackFor = IOException.class)
    public long clockedInAt(String techId) throws IOException {
        TimeEntry shift = openShift(techId, planner.policy());
        return shift == null ? 0 : shift.getStartAt();
    }

    @Transactional(rollbackFor = IOException.class)
    public void clock(String techId, boolean in) throws IOException {
        PlannerPolicy p = planner.policy();
        long now = System.currentTimeMillis();
        TimeEntry shift = openShift(techId, p);
        if (in) {
            if (shift == null) {
                open(techId, SHIFT, "", round(now, p));
            }
            return;
        }
        if (shift != null) {
            closeAll(techId, Math.max(shift.getStartAt(), round(now, p)));
        }
    }

    /// Records what a status change means for the clock.
    @Transactional(rollbackFor = IOException.class)
    public void jobStatus(String techId, String jobId, String status) throws IOException {
        if (techId == null || techId.length() == 0) {
            return;
        }
        PlannerPolicy p = planner.policy();
        long now = round(System.currentTimeMillis(), p);
        if (Catalog.EN_ROUTE.equals(status)) {
            if (openShift(techId, p) == null) {
                open(techId, SHIFT, "", now);
            }
            closeWork(techId, now);
            open(techId, TRAVEL, jobId, now);
        } else if (Catalog.ON_SITE.equals(status)) {
            if (openShift(techId, p) == null) {
                open(techId, SHIFT, "", now);
            }
            closeWork(techId, now);
            open(techId, JOB, jobId, now);
        } else if (Catalog.COMPLETED.equals(status)) {
            closeWork(techId, now);
        }
    }

    private TimeEntry openShift(String techId, PlannerPolicy p) throws IOException {
        // Entries come oldest first. Two racing clock-ins can open two
        // shifts; the earliest stands and any later one closes empty.
        TimeEntry shift = null;
        for (TimeEntry e : entries.open(techId)) {
            if (!SHIFT.equals(e.getKind())) {
                continue;
            }
            if (shift == null) {
                shift = e;
            } else {
                e.setEndAt(e.getStartAt());
                entries.save(e);
            }
        }
        if (shift == null) {
            return null;
        }
        long limit = shift.getStartAt() + p.getAutoClockOutHours() * 3600000L;
        if (System.currentTimeMillis() > limit) {
            closeAll(techId, limit);
            return null;
        }
        return shift;
    }

    private void open(String techId, String kind, String jobId, long at) throws IOException {
        TimeEntry e = new TimeEntry();
        e.setId("te" + Ids.next().substring(0, 14));
        e.setTechId(techId);
        e.setKind(kind);
        e.setJobId(jobId == null ? "" : jobId);
        e.setStartAt(at);
        entries.insert(e);
    }

    private void closeWork(String techId, long at) throws IOException {
        for (TimeEntry e : entries.open(techId)) {
            if (!SHIFT.equals(e.getKind())) {
                e.setEndAt(Math.max(e.getStartAt(), at));
                entries.save(e);
            }
        }
    }

    private void closeAll(String techId, long at) throws IOException {
        for (TimeEntry e : entries.open(techId)) {
            e.setEndAt(Math.max(e.getStartAt(), at));
            entries.save(e);
        }
    }

    /// Minutes of `[start, end)` that fall on a local Saturday or Sunday.
    static int weekendMinutes(long start, long end, ZoneId zone) {
        int total = 0;
        long at = start;
        while (at < end) {
            java.time.ZonedDateTime here = Instant.ofEpochMilli(at).atZone(zone);
            long next = Math.min(end, here.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli());
            DayOfWeek day = here.getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                total += (int) ((next - at) / 60000L);
            }
            at = next;
        }
        return total;
    }

    static long round(long millis, PlannerPolicy p) {
        long step = Math.max(1, p.getTimeRoundingMinutes()) * 60000L;
        return (millis + step / 2) / step * step;
    }

    // ------------------------------------------------------------ timesheets

    /// The Monday 00:00 of the week holding `millis` (now when 0).
    public long weekStart(long millis) {
        ZoneId zone = planner.zone();
        LocalDate d = Instant.ofEpochMilli(millis <= 0 ? System.currentTimeMillis() : millis)
                .atZone(zone).toLocalDate().with(DayOfWeek.MONDAY);
        return d.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    /// One week of clocked time and its cost. The pay rules are the simple
    /// ones most crews start with, and the place to change them for local
    /// law: overtime is the weekday time past the technician's regular
    /// weekly hours (weekend time is paid at its own premium and does not
    /// count toward it), the unpaid break comes off any shift long enough to
    /// owe one, and a shift belongs to the week it starts in. There is no
    /// daily overtime threshold.
    @Transactional(readOnly = true)
    public TimesheetDto timesheets(long week) throws IOException {
        PlannerPolicy p = planner.policy();
        ZoneId zone = planner.zone();
        long from = weekStart(week);
        long to = Instant.ofEpochMilli(from).atZone(zone).plusWeeks(1).toInstant().toEpochMilli();
        long now = System.currentTimeMillis();
        Map<String, LabourRate> rateMap = new HashMap<String, LabourRate>();
        for (LabourRate r : rates.findAll()) {
            rateMap.put(r.getId(), r);
        }
        List<Technician> crew = new ArrayList<Technician>(technicians.findAll());
        crew.sort((a, b) -> a.getName().compareTo(b.getName()));
        List<TimesheetRowDto> rows = new ArrayList<TimesheetRowDto>();
        long total = 0;
        for (Technician t : crew) {
            // Someone deactivated mid-week still gets paid for the hours they
            // worked: only an inactive technician with no entries is left out.
            if (!t.isActive() && entries.ofTechnician(t.getId(), from, to).isEmpty()) {
                continue;
            }
            // Rostered: the hours the technician's working week holds, less
            // the unpaid break, so it compares with paid time.
            int planned = 0;
            for (WorkWindow w : windows.ofTechnician(t.getId())) {
                int span = w.getEndMinute() - w.getStartMinute();
                planned += span - (span > p.getBreakAfterMinutes() ? p.getBreakMinutes() : 0);
            }
            int callouts = 0;
            for (Job job : jobs.plannedFor(t.getId(), from, to)) {
                if (job.getPriority() >= 4 && Catalog.COMPLETED.equals(job.getStatus())) {
                    callouts++;
                }
            }
            int shift = 0;
            int travel = 0;
            int onJob = 0;
            int breaks = 0;
            int weekend = 0;
            List<TimeEntryDto> list = new ArrayList<TimeEntryDto>();
            // Shifts are counted once even if two were opened together (a
            // double tap racing a status change): only time past the end of
            // the shifts already counted is added.
            long covered = 0;
            for (TimeEntry e : entries.ofTechnician(t.getId(), from, to)) {
                long end = e.getEndAt() > 0 ? e.getEndAt()
                        : Math.min(now, e.getStartAt() + p.getAutoClockOutHours() * 3600000L);
                int minutes = (int) Math.max(0, (end - e.getStartAt()) / 60000L);
                list.add(new TimeEntryDto(e.getKind(), e.getStartAt(), e.getEndAt(), e.getJobId()));
                if (SHIFT.equals(e.getKind())) {
                    long start = Math.max(e.getStartAt(), covered);
                    if (end <= start) {
                        continue;
                    }
                    covered = end;
                    minutes = (int) ((end - start) / 60000L);
                    // The meal break is unpaid, and owed once a shift runs
                    // past the point the rules say it is taken by.
                    int unpaid = minutes > p.getBreakAfterMinutes() ? p.getBreakMinutes() : 0;
                    shift += minutes;
                    breaks += unpaid;
                    // A shift that crosses midnight into or out of a weekend
                    // is split there; the break comes off in proportion.
                    int onWeekend = weekendMinutes(start, end, zone);
                    weekend += onWeekend - (minutes == 0 ? 0 : unpaid * onWeekend / minutes);
                } else if (TRAVEL.equals(e.getKind())) {
                    travel += minutes;
                } else {
                    onJob += minutes;
                }
            }
            int paid = shift - breaks;
            int weekday = paid - weekend;
            int overtime = Math.max(0, weekday - t.getRegularMinutesPerWeek());
            LabourRate r = rateMap.get(t.getRateId());
            long cost = r == null ? 0 : cost(r, weekday - overtime, overtime, weekend, callouts);
            total += cost;
            rows.add(new TimesheetRowDto(t.getId(), t.getName(), r == null ? "" : r.getName(), planned,
                    shift, travel, onJob, breaks, overtime, weekend, callouts, cost, list));
        }
        return new TimesheetDto(from, rows, total);
    }

    static long cost(LabourRate r, int regular, int overtime, int weekend, int callouts) {
        long weighted = regular * 100L + overtime * (long) r.getOvertimePct() + weekend * (long) r.getWeekendPct();
        return (weighted * r.getHourlyCents() / 100L + 30) / 60 + callouts * (long) r.getCalloutCents();
    }

    /// What `minutes` of weekday work cost at a technician's rate, for the
    /// staffing report's planned cost.
    static long plannedCost(LabourRate r, int minutes) {
        return r == null ? 0 : cost(r, minutes, 0, 0, 0);
    }

    @Transactional(readOnly = true)
    public Map<String, LabourRate> rateOfTechnician() {
        Map<String, LabourRate> byId = new HashMap<String, LabourRate>();
        for (LabourRate r : rates.findAll()) {
            byId.put(r.getId(), r);
        }
        Map<String, LabourRate> out = new HashMap<String, LabourRate>();
        for (Technician t : technicians.findAll()) {
            LabourRate r = byId.get(t.getRateId());
            if (r != null) {
                out.put(t.getId(), r);
            }
        }
        return out;
    }

    // ------------------------------------------------------------ demo data

    /// The three rates the demo company pays at.
    @Transactional(rollbackFor = IOException.class)
    public String[] seedRates() throws IOException {
        Object[][] table = {
            {"rate-apprentice", "Apprentice", 2600, 150, 175, 4000},
            {"rate-standard", "Technician", 4200, 150, 200, 6500},
            {"rate-senior", "Senior / certified", 5600, 150, 200, 8500},
        };
        String[] ids = new String[table.length];
        for (int i = 0; i < table.length; i++) {
            LabourRate r = new LabourRate();
            r.setId((String) table[i][0]);
            r.setName((String) table[i][1]);
            r.setHourlyCents(((Integer) table[i][2]).intValue());
            r.setOvertimePct(((Integer) table[i][3]).intValue());
            r.setWeekendPct(((Integer) table[i][4]).intValue());
            r.setCalloutCents(((Integer) table[i][5]).intValue());
            rates.insert(r);
            ids[i] = r.getId();
        }
        return ids;
    }

    /// Last week's clocked time, and this week's up to yesterday: a shift a
    /// little either side of the roster, split into drives and jobs, with some
    /// overtime and the one Saturday rota so the timesheet has something to
    /// show.
    @Transactional(rollbackFor = IOException.class)
    public void seedHistory(Technician t, int firstHour, int lastHour, int index) throws IOException {
        ZoneId zone = planner.zone();
        Random random = new Random(4241L + index);
        LocalDate today = LocalDate.now(zone);
        LocalDate monday = today.with(DayOfWeek.MONDAY).minusWeeks(1);
        for (LocalDate d = monday; d.isBefore(today); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            boolean saturday = dow == DayOfWeek.SATURDAY && index == 4;
            if (dow == DayOfWeek.SUNDAY || (dow == DayOfWeek.SATURDAY && !saturday)) {
                continue;
            }
            ZonedDateTime base = d.atStartOfDay(zone);
            int start = saturday ? 9 * 60 : firstHour * 60 - 10 + random.nextInt(25);
            int end = saturday ? 13 * 60 : lastHour * 60 - 20 + random.nextInt(index % 3 == 0 ? 110 : 45);
            long s = base.plusMinutes(start).toInstant().toEpochMilli() / 60000L * 60000L;
            long e = base.plusMinutes(end).toInstant().toEpochMilli() / 60000L * 60000L;
            entry(t.getId(), SHIFT, "", s, e);
            long at = s + (5 + random.nextInt(10)) * 60000L;
            boolean drive = true;
            while (at < e - 30 * 60000L) {
                long len = (drive ? 15 + random.nextInt(25) : 50 + random.nextInt(80)) * 60000L;
                long stop = Math.min(e - 10 * 60000L, at + len);
                entry(t.getId(), drive ? TRAVEL : JOB, "", at, stop);
                at = stop + (drive ? 0 : (random.nextInt(4) == 0 ? 30 : 5) * 60000L);
                drive = !drive;
            }
        }
    }

    private void entry(String techId, String kind, String jobId, long start, long end) throws IOException {
        TimeEntry e = new TimeEntry();
        e.setId("te" + Ids.next().substring(0, 14));
        e.setTechId(techId);
        e.setKind(kind);
        e.setJobId(jobId);
        e.setStartAt(start);
        e.setEndAt(end);
        entries.insert(e);
    }
}
