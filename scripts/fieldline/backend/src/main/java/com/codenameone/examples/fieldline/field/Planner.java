package com.codenameone.examples.fieldline.field;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.fieldline.Ids;
import com.codenameone.examples.fieldline.api.AbsenceDto;
import com.codenameone.examples.fieldline.api.ChangeDto;
import com.codenameone.examples.fieldline.api.KpiDto;
import com.codenameone.examples.fieldline.api.MoveDto;
import com.codenameone.examples.fieldline.api.ScheduleRunDto;
import com.codenameone.examples.fieldline.api.DayLoadDto;
import com.codenameone.examples.fieldline.api.PolicyDto;
import com.codenameone.examples.fieldline.api.ReasonCountDto;
import com.codenameone.examples.fieldline.api.SkillLoadDto;
import com.codenameone.examples.fieldline.api.StaffingDto;
import com.codenameone.examples.fieldline.api.TechLoadDto;
import com.codenameone.examples.fieldline.domain.Job;
import com.codenameone.examples.fieldline.domain.LabourRate;
import com.codenameone.examples.fieldline.domain.PlannedBreak;
import com.codenameone.examples.fieldline.domain.PlannerPolicy;
import com.codenameone.examples.fieldline.domain.ScheduleChange;
import com.codenameone.examples.fieldline.domain.ScheduleRun;
import com.codenameone.examples.fieldline.domain.Site;
import com.codenameone.examples.fieldline.domain.Technician;
import com.codenameone.examples.fieldline.domain.TimeOff;
import com.codenameone.examples.fieldline.domain.WorkWindow;
import com.codenameone.examples.fieldline.live.LiveHub;
import com.codenameone.examples.fieldline.repository.JobRepository;
import com.codenameone.examples.fieldline.repository.LabourRateRepository;
import com.codenameone.examples.fieldline.repository.PlannedBreakRepository;
import com.codenameone.examples.fieldline.repository.PlannerPolicyRepository;
import com.codenameone.examples.fieldline.repository.ScheduleChangeRepository;
import com.codenameone.examples.fieldline.repository.ScheduleRunRepository;
import com.codenameone.examples.fieldline.repository.SiteRepository;
import com.codenameone.examples.fieldline.repository.TechnicianRepository;
import com.codenameone.examples.fieldline.repository.TimeOffRepository;
import com.codenameone.examples.fieldline.repository.WorkWindowRepository;
import com.codenameone.examples.fieldline.scheduler.GeoTravel;
import com.codenameone.examples.fieldline.scheduler.Plan;
import com.codenameone.examples.fieldline.scheduler.Problem;
import com.codenameone.examples.fieldline.scheduler.Reoptimizer;
import com.codenameone.examples.fieldline.scheduler.Solver;
import com.codenameone.examples.fieldline.scheduler.Weights;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The bridge between the database and the scheduler: it builds the problem
/// the next few working days pose, runs the search, writes the plan back onto
/// the jobs, and keeps a record of every run and what it changed.
///
/// Every change to the plan goes through [#publish], one at a time. A run reads
/// the plan that was published, so two at once would each repair the same old
/// plan and the second would silently undo the first.
///
/// The plan is always *repaired*, never thrown away: [Reoptimizer] keeps work
/// that has started where it is, starts from the published routes, and charges
/// for every job it hands to someone else. Optimizing from scratch is tried as
/// well and kept only when it is cheaper after paying for that disruption.
@Component
public class Planner {
    /// How many days ahead the plan reaches: a week, so it always holds five
    /// working days whatever day it is planned on.
    public static final int HORIZON_DAYS = 7;

    public static final String OPTIMIZE = "OPTIMIZE";
    public static final String ABSENCE = "ABSENCE";
    public static final String MOVE = "MOVE";
    public static final String TIME_OFF = "TIME_OFF";
    public static final String AVAILABILITY = "AVAILABILITY";
    public static final String NEW_JOB = "NEW_JOB";
    public static final String CREW = "CREW";
    public static final String POLICY = "POLICY";

    private final TechnicianRepository technicians;
    private final WorkWindowRepository windows;
    private final TimeOffRepository timeOff;
    private final JobRepository jobs;
    private final SiteRepository sites;
    private final ScheduleRunRepository runs;
    private final ScheduleChangeRepository changes;
    private final Crew crew;
    private final LiveHub live;
    private final Notices notices;
    private final PlannerPolicyRepository policies;
    private final PlannedBreakRepository breaks;
    private final LabourRateRepository rates;
    private final ZoneId zone;
    private final int iterations;
    private final Object lock = new Object();

    public Planner(TechnicianRepository technicians, WorkWindowRepository windows,
            TimeOffRepository timeOff, JobRepository jobs, SiteRepository sites,
            ScheduleRunRepository runs, ScheduleChangeRepository changes, Crew crew,
            LiveHub live, Notices notices, PlannerPolicyRepository policies,
            PlannedBreakRepository breaks, LabourRateRepository rates,
            @Value("${fieldline.zone:}") String zone,
            @Value("${fieldline.scheduler.iterations:2500}") int iterations) {
        this.technicians = technicians;
        this.windows = windows;
        this.timeOff = timeOff;
        this.jobs = jobs;
        this.sites = sites;
        this.runs = runs;
        this.changes = changes;
        this.crew = crew;
        this.live = live;
        this.notices = notices;
        this.policies = policies;
        this.breaks = breaks;
        this.rates = rates;
        this.zone = zone == null || zone.trim().length() == 0 ? ZoneId.systemDefault()
                : ZoneId.of(zone.trim());
        this.iterations = Math.max(50, iterations);
    }

    public ZoneId zone() {
        return zone;
    }

    /// The span the board shows and the scheduler plans: today and the days
    /// after it.
    public Horizon horizon() {
        return Horizon.of(System.currentTimeMillis(), HORIZON_DAYS, zone);
    }

    /// The same calendar over `days` days, for forecasts past the board.
    public Horizon horizon(int days) {
        return Horizon.of(System.currentTimeMillis(), days, zone);
    }

    // ------------------------------------------------------------ policy

    /// The company's planning rules; the defaults until an admin saves some.
    public PlannerPolicy policy() throws IOException {
        PlannerPolicy p = policies.findById(PlannerPolicy.ID);
        return p == null ? new PlannerPolicy() : p;
    }

    public static PolicyDto toDto(PlannerPolicy p) {
        return new PolicyDto(p.getBreakMinutes(), p.getBreakAfterMinutes(), p.getBreakBeforeMinutes(),
                p.isStrictTerritories(), p.getTravelWeight(), p.getOvertimeWeight(), p.getLatenessWeight(),
                p.getContinuityWeight(), p.getBalanceWeight(), p.getTerritoryWeight(),
                p.getUrgentSlaHours(), p.getRoutineSlaHours(), p.getRushAmFrom(), p.getRushAmTo(),
                p.getRushPmFrom(), p.getRushPmTo(), p.getRushExtraPct(), p.getTimeRoundingMinutes(),
                p.getAutoClockOutHours());
    }

    /// Saves the rules and re-plans the open work under them. Every value is
    /// held to a range a planner can still use: a weight of 0 switches a
    /// concern off, and nothing may dwarf a missed job.
    public PolicyDto changePolicy(PolicyDto wanted, String actor) throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "No rules sent");
        }
        if (wanted.breakMinutes() < 0 || wanted.breakMinutes() > 90) {
            throw new ResponseStatusException(400, "Keep the break between 0 and 90 minutes");
        }
        if (wanted.breakAfterMinutes() < 60 || wanted.breakBeforeMinutes() < wanted.breakAfterMinutes()
                || wanted.breakBeforeMinutes() > 8 * 60) {
            throw new ResponseStatusException(400,
                    "The break has to start between 1 and 8 hours into the shift, earliest first");
        }
        int[] weights = {wanted.travelWeight(), wanted.overtimeWeight(), wanted.latenessWeight(),
            wanted.continuityWeight(), wanted.balanceWeight(), wanted.territoryWeight()};
        for (int w : weights) {
            if (w < 0 || w > 2000) {
                throw new ResponseStatusException(400, "Each weight is between 0 and 2000");
            }
        }
        if (wanted.urgentSlaHours() < 1 || wanted.routineSlaHours() < wanted.urgentSlaHours()
                || wanted.routineSlaHours() > 24 * 30) {
            throw new ResponseStatusException(400,
                    "Urgent jobs need an SLA of at least an hour, and no longer than routine ones");
        }
        int[] rush = {wanted.rushAmFrom(), wanted.rushAmTo(), wanted.rushPmFrom(), wanted.rushPmTo()};
        for (int m : rush) {
            if (m < 0 || m > 24 * 60) {
                throw new ResponseStatusException(400, "Rush hours are times of the day");
            }
        }
        if (rush[1] < rush[0] || rush[3] < rush[2] || rush[2] < rush[1]) {
            throw new ResponseStatusException(400,
                    "Each rush hour ends after it starts, and the morning one comes first");
        }
        if (wanted.rushExtraPct() < 0 || wanted.rushExtraPct() > 200) {
            throw new ResponseStatusException(400, "Rush-hour traffic adds between 0% and 200%");
        }
        PlannerPolicy stored = policy();
        // 0 is what a client that predates a setting sends: keep the stored one.
        int rounding = wanted.timeRoundingMinutes() == 0 ? stored.getTimeRoundingMinutes()
                : wanted.timeRoundingMinutes();
        int autoOut = wanted.autoClockOutHours() == 0 ? stored.getAutoClockOutHours()
                : wanted.autoClockOutHours();
        if (rounding < 1 || rounding > 30 || 60 % rounding != 0) {
            throw new ResponseStatusException(400, "Round clocked time to 1, 5, 6, 10, 15 or 30 minutes");
        }
        if (autoOut < 4 || autoOut > 24) {
            throw new ResponseStatusException(400, "Close a forgotten shift after 4 to 24 hours");
        }
        PlannerPolicy p = stored;
        boolean fresh = policies.findById(PlannerPolicy.ID) == null;
        p.setBreakMinutes(wanted.breakMinutes());
        p.setBreakAfterMinutes(wanted.breakAfterMinutes());
        p.setBreakBeforeMinutes(wanted.breakBeforeMinutes());
        p.setStrictTerritories(wanted.strictTerritories());
        p.setTravelWeight(wanted.travelWeight());
        p.setOvertimeWeight(wanted.overtimeWeight());
        p.setLatenessWeight(wanted.latenessWeight());
        p.setContinuityWeight(wanted.continuityWeight());
        p.setBalanceWeight(wanted.balanceWeight());
        p.setTerritoryWeight(wanted.territoryWeight());
        p.setUrgentSlaHours(wanted.urgentSlaHours());
        p.setRoutineSlaHours(wanted.routineSlaHours());
        p.setRushAmFrom(rush[0]);
        p.setRushAmTo(rush[1]);
        p.setRushPmFrom(rush[2]);
        p.setRushPmTo(rush[3]);
        p.setRushExtraPct(wanted.rushExtraPct());
        p.setTimeRoundingMinutes(rounding);
        p.setAutoClockOutHours(autoOut);
        p.setUpdatedAt(System.currentTimeMillis());
        if (fresh) {
            policies.insert(p);
        } else {
            policies.save(p);
        }
        repair(POLICY, "Re-planned under new rules", actor);
        return toDto(p);
    }

    /// The problem a model plans, with the company's rush hours on its drives.
    static Problem problem(Model m, List<com.codenameone.examples.fieldline.scheduler.Job> work) {
        PlannerPolicy p = m.policy;
        Problem problem = new Problem(m.crew, work, m.travel, m.weights);
        return p == null ? problem : problem.rushHours(p.getRushExtraPct(), p.getRushAmFrom(), p.getRushAmTo(),
                p.getRushPmFrom(), p.getRushPmTo());
    }

    static Weights weights(PlannerPolicy p) {
        Weights w = new Weights();
        w.travel = p.getTravelWeight();
        w.overtime = p.getOvertimeWeight();
        w.lateness = p.getLatenessWeight();
        w.continuity = p.getContinuityWeight();
        w.balance = p.getBalanceWeight();
        w.outOfTerritory = p.getTerritoryWeight();
        w.strictTerritories = p.isStrictTerritories();
        return w;
    }

    // ------------------------------------------------------------ the model

    /// A drag on the board, or a dispatcher's "give this to her": the job is
    /// pinned where it was put, and its old place is not replayed.
    static final class Pin {
        final String jobId;
        final String techId;
        final long start;

        Pin(String jobId, String techId, long start) {
            this.jobId = jobId;
            this.techId = techId;
            this.start = start;
        }
    }

    /// The problem the database poses now, with the plan that is published.
    static final class Model {
        Horizon horizon;
        Problem problem;
        Plan published;
        int now;
        final Map<String, Job> jobs = new HashMap<String, Job>();
        final Map<String, Technician> techs = new HashMap<String, Technician>();
        final Map<String, Site> sites = new HashMap<String, Site>();
        final Map<String, List<int[]>> available = new HashMap<String, List<int[]>>();
        final List<com.codenameone.examples.fieldline.scheduler.Job> scheduled =
                new ArrayList<com.codenameone.examples.fieldline.scheduler.Job>();
        final List<com.codenameone.examples.fieldline.scheduler.Technician> crew =
                new ArrayList<com.codenameone.examples.fieldline.scheduler.Technician>();
        GeoTravel travel;
        long availableMinutes;
        Weights weights;
        PlannerPolicy policy;
        /// Rostered minutes per horizon day, less time off and breaks.
        int[] capacity;
        final Map<String, Integer> techCapacity = new HashMap<String, Integer>();
    }

    static int micro(double degrees) {
        return (int) Math.round(degrees * 1000000.0);
    }

    Model model(Pin pin) throws IOException {
        return model(pin, horizon());
    }

    Model model(Pin pin, Horizon span) throws IOException {
        Model m = new Model();
        m.horizon = span;
        m.policy = policy();
        m.weights = weights(m.policy);
        m.capacity = new int[span.days()];
        Horizon h = m.horizon;
        m.now = h.minute(System.currentTimeMillis());
        for (Site s : sites.findAll()) {
            m.sites.put(s.getId(), s);
        }
        Map<String, List<WorkWindow>> weeks = new HashMap<String, List<WorkWindow>>();
        for (WorkWindow w : windows.everyone()) {
            List<WorkWindow> list = weeks.get(w.getTechId());
            if (list == null) {
                list = new ArrayList<WorkWindow>();
                weeks.put(w.getTechId(), list);
            }
            list.add(w);
        }
        Map<String, List<TimeOff>> offs = new HashMap<String, List<TimeOff>>();
        for (TimeOff off : timeOff.overlapping(h.origin(), h.end())) {
            if (!Catalog.APPROVED.equals(off.getStatus())) {
                continue;
            }
            List<TimeOff> list = offs.get(off.getTechId());
            if (list == null) {
                list = new ArrayList<TimeOff>();
                offs.put(off.getTechId(), list);
            }
            list.add(off);
        }
        Map<String, Integer> worked = workedThisWeek(h);
        double latSum = 0;
        int horizonMinutes = h.minute(h.end());
        for (Technician t : technicians.active()) {
            m.techs.put(t.getId(), t);
            latSum += t.getHomeLat();
            com.codenameone.examples.fieldline.scheduler.Technician.Builder b =
                    com.codenameone.examples.fieldline.scheduler.Technician.builder(t.getId())
                    .name(t.getName()).skills(Catalog.mask(t.getSkills()))
                    .home(micro(t.getHomeLat()), micro(t.getHomeLng()))
                    .hours(weekly(t.getRegularMinutesPerWeek(), h, worked.get(t.getId())),
                            weekly(t.getMaxMinutesPerWeek(), h, worked.get(t.getId())))
                    .maxDrivePerShift(t.getMaxDriveMinutes())
                    .maxJobsPerShift(Math.max(1, t.getMaxJobsPerDay()))
                    .maxShiftMinutes(Math.max(60, t.getMaxMinutesPerDay()))
                    .territories(Catalog.territoryMask(t.getTerritories()));
            PlannerPolicy policy = m.policy;
            if (policy.getBreakMinutes() > 0) {
                b.lunch(policy.getBreakMinutes(), policy.getBreakAfterMinutes(),
                        policy.getBreakBeforeMinutes());
            }
            for (String[] cert : Catalog.certifications(t.getCertifications())) {
                b.certification(index(Catalog.CERTIFICATIONS, cert[0]), until(cert[1], h));
            }
            List<int[]> spans = new ArrayList<int[]>();
            List<WorkWindow> week = weeks.get(t.getId());
            List<TimeOff> away = offs.get(t.getId());
            for (int d = 0; week != null && d < h.days(); d++) {
                for (WorkWindow w : week) {
                    if (w.getWeekday() != h.weekday(d)) {
                        continue;
                    }
                    int start = h.at(d, w.getStartMinute());
                    int end = h.at(d, w.getEndMinute());
                    b.available(start, end);
                    spans.add(new int[] {start, end});
                    int free = end - start - overlap(away, h, start, end);
                    if (free > m.policy.getBreakAfterMinutes()) {
                        free -= m.policy.getBreakMinutes();
                    }
                    free = Math.max(0, Math.min(free, t.getMaxMinutesPerDay()));
                    m.availableMinutes += free;
                    m.capacity[d] += free;
                    Integer had = m.techCapacity.get(t.getId());
                    m.techCapacity.put(t.getId(), (had == null ? 0 : had.intValue()) + free);
                }
            }
            for (int iter = 0; away != null && iter < away.size(); iter++) {
                TimeOff off = away.get(iter);
                int start = Math.max(0, h.minute(off.getStartAt()));
                int end = Math.min(horizonMinutes, h.minute(off.getEndAt()));
                if (end > start) {
                    b.timeOff(start, end);
                }
            }
            m.available.put(t.getId(), spans);
            m.crew.add(b.build());
        }
        double lat = m.techs.isEmpty() ? Crew.DEPOT_LAT : latSum / m.techs.size();
        // Straight-line distance times 1.3 for the street grid, at 38 km/h
        // across town, and six minutes to park and walk in. A routing service
        // would be a different TravelModel; the search does not change.
        m.travel = new GeoTravel((int) Math.round(Math.cos(Math.toRadians(lat)) * 1000), 1300, 38, 6);

        List<Plan.Visit> visits = new ArrayList<Plan.Visit>();
        for (Job job : jobs.inSpan(h.origin(), h.end())) {
            if (!Catalog.isOpen(job.getStatus())) {
                continue;
            }
            Site site = m.sites.get(job.getSiteId());
            if (site == null) {
                continue;
            }
            boolean pinned = pin != null && pin.jobId.equals(job.getId());
            boolean started = Catalog.isUnderWay(job.getStatus()) && m.techs.containsKey(job.getTechId());
            int earliest = Math.max(0, h.minute(job.getWindowStart()));
            int latest = Math.min(horizonMinutes, h.minute(job.getWindowEnd())) - job.getDurationMinutes();
            if (!started && !pinned) {
                // Nothing is planned in the past: an overdue job is due now.
                earliest = Math.max(earliest, m.now);
            }
            int due = job.getDueAt() > 0 ? h.minute(job.getDueAt()) : Integer.MAX_VALUE / 4;
            if (latest < earliest && !started && !pinned) {
                // The window has closed and the work is still open. Dropping it
                // would hide it; it is planned as soon as possible instead and
                // charged as late from the end of the window it missed.
                due = Math.min(due, Math.max(0, h.minute(job.getWindowEnd())));
                earliest = m.now;
                latest = horizonMinutes - job.getDurationMinutes();
                if (latest < earliest) {
                    continue;
                }
            }
            m.jobs.put(job.getId(), job);
            com.codenameone.examples.fieldline.scheduler.Job.Builder b =
                    com.codenameone.examples.fieldline.scheduler.Job.builder(job.getId())
                    .at(micro(site.getLat()), micro(site.getLng()))
                    .duration(job.getDurationMinutes())
                    .skills(Catalog.mask(job.getSkills()))
                    .priority(Math.max(1, Math.min(5, job.getPriority())))
                    .window(earliest, Math.max(earliest, latest))
                    .certifications(Catalog.certMask(job.getCertifications()))
                    .zone(Catalog.territoryMask(Catalog.territoryOf(site.getLat(), site.getLng())))
                    .due(due);
            for (String x : Catalog.split(job.getExcludedTechs())) {
                b.exclude(x);
            }
            if (job.getAfterJobId().length() > 0) {
                b.after(job.getAfterJobId());
            }
            if (pinned) {
                b.pinned(pin.techId, h.minute(pin.start));
            } else if (started) {
                long began = job.getActualStart() > 0 ? job.getActualStart() : job.getPlannedStart();
                b.pinned(job.getTechId(), Math.max(0, h.minute(began)));
            }
            m.scheduled.add(b.build());
            if (!pinned && m.techs.containsKey(job.getTechId()) && job.getPlannedStart() > 0) {
                visits.add(new Plan.Visit(job.getId(), job.getTechId(), h.minute(job.getPlannedStart()),
                        h.minute(job.getPlannedEnd()), job.getTravelBefore(), job.getSequence()));
            }
        }
        m.published = Plan.published(visits);
        m.problem = problem(m, m.scheduled);
        return m;
    }

    /// A weekly limit over the horizon: its share of the days planned, less
    /// what was already worked since the payroll week began on Monday.
    static int weekly(int perWeek, Horizon h, Integer workedSoFar) {
        int share = perWeek * h.days() / 7;
        if (h.days() <= 7) {
            share = Math.min(share, perWeek - (workedSoFar == null ? 0 : workedSoFar.intValue()));
        }
        return Math.max(0, share);
    }

    /// Minutes each technician has already put in this week: completed and
    /// started work since Monday 00:00, with the driving before it.
    private Map<String, Integer> workedThisWeek(Horizon h) {
        Map<String, Integer> out = new HashMap<String, Integer>();
        int back = h.weekday(0) - 1;
        long monday = h.origin() - back * 24L * 3600000L;
        for (Job job : jobs.inSpan(monday, h.origin() + 24L * 3600000L)) {
            if (job.getTechId().length() == 0 || job.getActualStart() <= 0) {
                continue;
            }
            long end = job.getActualEnd() > 0 ? job.getActualEnd() : System.currentTimeMillis();
            int minutes = (int) ((end - job.getActualStart()) / 60000L) + job.getTravelBefore();
            Integer had = out.get(job.getTechId());
            out.put(job.getTechId(), (had == null ? 0 : had.intValue()) + Math.max(0, minutes));
        }
        return out;
    }

    /// Whether a certificate dated `yyyy-mm-dd` has lapsed by `millis`.
    private boolean lapsedBy(String date, long millis) {
        if (date == null || date.length() == 0) {
            return false;
        }
        try {
            return LocalDate.parse(date).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() <= millis;
        } catch (RuntimeException bad) {
            return true;
        }
    }

    private static int index(String[] table, String code) {
        for (int iter = 0; iter < table.length; iter++) {
            if (table[iter].equals(code)) {
                return iter;
            }
        }
        return 0;
    }

    /// The last minute of the horizon a certificate dated `yyyy-mm-dd` is
    /// valid in: the end of that day. No date never lapses; a bad one has.
    private static int until(String date, Horizon h) {
        if (date == null || date.length() == 0) {
            return Integer.MAX_VALUE;
        }
        try {
            long end = LocalDate.parse(date).plusDays(1).atStartOfDay(h.zone()).toInstant().toEpochMilli();
            if (end >= h.end()) {
                return Integer.MAX_VALUE;
            }
            return Math.max(-1, h.minute(end) - 1);
        } catch (RuntimeException bad) {
            return -1;
        }
    }

    private static int overlap(List<TimeOff> away, Horizon h, int start, int end) {
        int out = 0;
        for (int iter = 0; away != null && iter < away.size(); iter++) {
            int a = Math.max(start, h.minute(away.get(iter).getStartAt()));
            int b = Math.min(end, h.minute(away.get(iter).getEndAt()));
            if (b > a) {
                out += b - a;
            }
        }
        return Math.min(out, end - start);
    }

    // ------------------------------------------------------------ the figures

    KpiDto kpis(Model m, Plan plan) {
        int urgent = 0;
        for (String id : plan.getUnassigned()) {
            Job job = m.jobs.get(id);
            if (job != null && job.getPriority() >= 4) {
                urgent++;
            }
        }
        long busy = 0;
        for (Plan.Visit v : plan.getVisits()) {
            busy += v.end - v.start;
        }
        int utilization = m.availableMinutes <= 0 ? 0
                : (int) Math.min(100L, Math.round(busy * 100.0 / m.availableMinutes));
        return new KpiDto(plan.getVisits().size(), plan.getUnassigned().size(),
                plan.getDriveMinutes(), plan.getOvertimeMinutes(), utilization, urgent,
                plan.getLateJobs());
    }

    /// The figures of the plan as it stands, for the board's header.
    @Transactional(readOnly = true)
    public KpiDto currentKpis() throws IOException {
        Model m = model(null);
        return kpis(m, Reoptimizer.evaluate(m.problem, m.published));
    }

    static String encode(KpiDto k) {
        return k.scheduled() + "," + k.unassigned() + "," + k.driveMinutes() + ","
                + k.overtimeMinutes() + "," + k.utilizationPct() + "," + k.highPriorityOpen() + ","
                + k.lateJobs();
    }

    static KpiDto decode(String text) {
        // Runs recorded before the SLA was planned for have six figures.
        int[] v = new int[7];
        List<String> parts = Catalog.split(text);
        for (int iter = 0; iter < v.length && iter < parts.size(); iter++) {
            try {
                v[iter] = Integer.parseInt(parts.get(iter));
            } catch (NumberFormatException bad) {
                v[iter] = 0;
            }
        }
        return new KpiDto(v[0], v[1], v[2], v[3], v[4], v[5], v[6]);
    }

    // ------------------------------------------------------------ staffing

    /// A healthy day keeps the crew between these shares of its hours busy.
    /// Under the first the day is overstaffed; over the second there is no
    /// slack for an emergency or an overrun, and the work starts to slip.
    public static final int LOW_PCT = 55;
    public static final int HIGH_PCT = 90;

    /// Capacity against demand for the next `days` days. Past the published
    /// horizon nothing is planned yet, so the report plans that far itself --
    /// a forecast, never written to the jobs -- and that is what tells a
    /// manager whether next week's work fits the people rostered for it.
    @Transactional(readOnly = true)
    public StaffingDto staffing(int days) throws IOException {
        Horizon h = horizon(Math.max(1, Math.min(28, days)));
        Model m = model(null, h);
        Plan plan = Reoptimizer.replan(m.problem, m.published, 104729L, Math.max(200, iterations / 3)).plan;
        int n = h.days();
        int[] demand = new int[n];
        int[] work = new int[n];
        int[] drive = new int[n];
        int[] open = new int[n];
        Map<String, int[]> skillDemand = new HashMap<String, int[]>();
        for (Job job : m.jobs.values()) {
            int d = dayOf(h, Math.max(job.getWindowStart(), h.origin()));
            demand[d] += job.getDurationMinutes();
            for (String skill : Catalog.split(job.getSkills())) {
                int[] v = skillDemand.get(skill);
                if (v == null) {
                    v = new int[1];
                    skillDemand.put(skill, v);
                }
                v[0] += job.getDurationMinutes();
            }
        }
        Map<String, int[]> perTech = new HashMap<String, int[]>();
        for (Plan.Visit v : plan.getVisits()) {
            int d = Math.max(0, Math.min(n - 1, v.start / (24 * 60)));
            work[d] += v.end - v.start;
            drive[d] += v.travelBefore;
            int[] t = perTech.get(v.techId);
            if (t == null) {
                t = new int[3];
                perTech.put(v.techId, t);
            }
            t[0] += v.end - v.start;
            t[1] += v.travelBefore;
            t[2]++;
        }
        for (String id : plan.getUnassigned()) {
            Job job = m.jobs.get(id);
            if (job != null) {
                open[dayOf(h, Math.max(job.getWindowStart(), h.origin()))] += job.getDurationMinutes();
            }
        }
        List<DayLoadDto> dayRows = new ArrayList<DayLoadDto>();
        for (int d = 0; d < n; d++) {
            dayRows.add(new DayLoadDto(h.dayStart(d), m.capacity[d], demand[d], work[d], drive[d], open[d]));
        }
        List<SkillLoadDto> skillRows = new ArrayList<SkillLoadDto>();
        for (String skill : Catalog.SKILLS) {
            int people = 0;
            int hours = 0;
            for (Technician t : m.techs.values()) {
                if ((Catalog.mask(t.getSkills()) & Catalog.mask(skill)) != 0) {
                    people++;
                    Integer c = m.techCapacity.get(t.getId());
                    hours += c == null ? 0 : c.intValue();
                }
            }
            int[] want = skillDemand.get(skill);
            skillRows.add(new SkillLoadDto(skill, people, hours, want == null ? 0 : want[0]));
        }
        List<TechLoadDto> techRows = new ArrayList<TechLoadDto>();
        List<Technician> crewList = new ArrayList<Technician>(m.techs.values());
        crewList.sort((a, b) -> a.getName().compareTo(b.getName()));
        for (Technician t : crewList) {
            int[] v = perTech.get(t.getId());
            Integer c = m.techCapacity.get(t.getId());
            int busy = v == null ? 0 : v[0] + v[1];
            int regular = t.getRegularMinutesPerWeek() * n / 7;
            techRows.add(new TechLoadDto(t.getId(), t.getName(), Crew.hex(t.getColor()),
                    c == null ? 0 : c.intValue(), v == null ? 0 : v[0], v == null ? 0 : v[1],
                    Math.max(0, busy - regular), v == null ? 0 : v[2]));
        }
        int due = 0;
        int late = 0;
        for (Plan.Visit v : plan.getVisits()) {
            Job job = m.jobs.get(v.jobId);
            if (job != null && job.getDueAt() > 0) {
                due++;
                if (h.millis(v.end) > job.getDueAt()) {
                    late++;
                }
            }
        }
        Map<String, ReasonCountDto> counts = new HashMap<String, ReasonCountDto>();
        List<String> order = new ArrayList<String>();
        for (Plan.Reason r : plan.getReasons()) {
            ReasonCountDto had = counts.get(r.code);
            if (had == null) {
                order.add(r.code);
            }
            counts.put(r.code, new ReasonCountDto(r.code, r.text, had == null ? 1 : had.jobs() + 1));
        }
        List<ReasonCountDto> reasonRows = new ArrayList<ReasonCountDto>();
        for (String code : order) {
            reasonRows.add(counts.get(code));
        }
        reasonRows.sort((a, b) -> b.jobs() - a.jobs());
        int unplaced = plan.getUnassigned().size();
        Map<String, LabourRate> byId = new HashMap<String, LabourRate>();
        for (LabourRate r : rates.findAll()) {
            byId.put(r.getId(), r);
        }
        long cost = 0;
        for (Map.Entry<String, int[]> e : perTech.entrySet()) {
            Technician tech = m.techs.get(e.getKey());
            if (tech != null) {
                cost += Timekeeping.plannedCost(byId.get(tech.getRateId()), e.getValue()[0] + e.getValue()[1]);
            }
        }
        return new StaffingDto(h.origin(), LOW_PCT, HIGH_PCT, dayRows, skillRows, techRows,
                due + unplaced, due - late, late, unplaced, reasonRows, cost);
    }

    private static int dayOf(Horizon h, long millis) {
        return Math.max(0, Math.min(h.days() - 1, h.minute(millis) / (24 * 60)));
    }

    // ------------------------------------------------------------ runs

    /// The result of one run before it is written, so a validation can look
    /// at it and throw it away.
    static final class Outcome {
        Model model;
        Plan before;
        Plan after;
        List<Reoptimizer.Change> changes;
        String operators;
        long millis;
    }

    Outcome solve(Pin pin, boolean fromScratchToo, int budget) throws IOException {
        Outcome o = new Outcome();
        long t0 = System.currentTimeMillis();
        Model m = model(pin);
        o.model = m;
        o.before = Reoptimizer.evaluate(m.problem, m.published);
        List<com.codenameone.examples.fieldline.scheduler.Job> carried =
                Reoptimizer.carryOver(m.scheduled, m.published, m.now);
        Problem problem = problem(m, carried);
        long seed = 7919L * (runs.count() + 1);
        Reoptimizer.Result repaired = Reoptimizer.replan(problem, m.published, seed, budget);
        o.after = repaired.plan;
        o.changes = repaired.changes;
        o.operators = "repair";
        if (fromScratchToo) {
            Solver solver = new Solver(problem, seed + 1).iterations(budget);
            Plan fresh = solver.solve().toPlan();
            if (fresh.getCost() < repaired.plan.getCost()) {
                o.after = fresh;
                o.changes = Reoptimizer.compare(problem, m.published, fresh);
                o.operators = "from scratch: " + solver.operatorReport();
            }
        }
        m.problem = problem;
        o.millis = System.currentTimeMillis() - t0;
        return o;
    }

    /// Writes a plan onto the jobs and records the run. The plan covers every
    /// open job in the horizon, so each one it does not place is unassigned.
    ScheduleRunDto write(Outcome o, String kind, String summary, String actor) throws IOException {
        Model m = o.model;
        Horizon h = m.horizon;
        long now = System.currentTimeMillis();
        Set<String> placed = new HashSet<String>();
        for (Plan.Visit v : o.after.getVisits()) {
            Job job = m.jobs.get(v.jobId);
            if (job == null) {
                continue;
            }
            placed.add(v.jobId);
            if (!v.techId.equals(job.getTechId()) && job.getTechId().length() > 0) {
                job.setPreviousTechId(job.getTechId());
            }
            job.setTechId(v.techId);
            job.setPlannedStart(h.millis(v.start));
            job.setPlannedEnd(h.millis(v.end));
            job.setTravelBefore(v.travelBefore);
            job.setSequence(v.sequence);
            job.setUnassignedReason("");
            if (Catalog.UNASSIGNED.equals(job.getStatus())) {
                job.setStatus(Catalog.SCHEDULED);
            }
            job.setUpdatedAt(now);
            jobs.save(job);
        }
        Map<String, String> why = new HashMap<String, String>();
        for (Plan.Reason r : o.after.getReasons()) {
            why.put(r.jobId, r.text);
        }
        for (Job job : m.jobs.values()) {
            if (placed.contains(job.getId()) || Catalog.isUnderWay(job.getStatus())) {
                continue;
            }
            String reason = why.get(job.getId());
            job.setUnassignedReason(reason == null ? "" : reason.length() > 160 ? reason.substring(0, 160) : reason);
            if (job.getTechId().length() > 0) {
                job.setPreviousTechId(job.getTechId());
            }
            job.setTechId("");
            job.setPlannedStart(0);
            job.setPlannedEnd(0);
            job.setTravelBefore(0);
            job.setSequence(0);
            job.setStatus(Catalog.UNASSIGNED);
            job.setUpdatedAt(now);
            jobs.save(job);
        }
        // The breaks are the plan's too: they are replaced with it, and the
        // board draws them between the jobs.
        breaks.deleteAll();
        for (Plan.Break br : o.after.getBreaks()) {
            PlannedBreak row = new PlannedBreak();
            row.setId("b" + Ids.next().substring(0, 14));
            row.setTechId(br.techId);
            row.setStartAt(h.millis(br.start));
            row.setEndAt(h.millis(br.end));
            breaks.insert(row);
        }
        KpiDto before = kpis(m, o.before);
        KpiDto after = kpis(m, o.after);
        ScheduleRun run = new ScheduleRun();
        run.setId("r" + Ids.next().substring(0, 12));
        run.setRanAt(now);
        run.setKind(kind);
        run.setSummary(summary.length() > 500 ? summary.substring(0, 500) : summary);
        run.setBeforeKpis(encode(before));
        run.setAfterKpis(encode(after));
        run.setOperators(o.operators.length() > 500 ? o.operators.substring(0, 500) : o.operators);
        run.setIterations(iterations);
        run.setMillis(o.millis);
        run.setActor(actor == null ? "" : actor);
        runs.insert(run);
        List<ChangeDto> out = new ArrayList<ChangeDto>();
        Map<String, String> names = names();
        int n = 0;
        for (Reoptimizer.Change c : o.changes) {
            Job job = m.jobs.get(c.jobId);
            ScheduleChange row = new ScheduleChange();
            row.setId(run.getId() + ":" + (n++));
            row.setRunId(run.getId());
            row.setJobId(c.jobId);
            row.setJobTitle(job == null ? c.jobId : job.getTitle());
            row.setFromTech(c.fromTech == null ? "" : c.fromTech);
            row.setToTech(c.toTech == null ? "" : c.toTech);
            row.setFromStart(c.fromStart < 0 ? 0 : h.millis(c.fromStart));
            row.setToStart(c.toStart < 0 ? 0 : h.millis(c.toStart));
            row.setReason(explain(m, c));
            changes.insert(row);
            out.add(toDto(row, names));
        }
        return new ScheduleRunDto(run.getId(), run.getRanAt(), kind, run.getSummary(), before, after,
                out, run.getOperators(), iterations, o.millis);
    }

    private static String explain(Model m, Reoptimizer.Change c) {
        String why = c.reason == null ? "" : c.reason;
        return why.length() > 255 ? why.substring(0, 255) : why;
    }

    /// A change as the app shows it: the technicians by name, which is what a
    /// dispatcher reads; the rows keep their ids.
    static ChangeDto toDto(ScheduleChange row, Map<String, String> names) {
        return new ChangeDto(row.getJobId(), row.getJobTitle(), name(names, row.getFromTech()),
                name(names, row.getToTech()), row.getFromStart(), row.getToStart(),
                row.getReason());
    }

    private static String name(Map<String, String> names, String id) {
        if (id == null || id.length() == 0) {
            return id;
        }
        String name = names.get(id);
        return name == null ? id : name;
    }

    /// Every technician's name by id, for the changes of a run.
    private Map<String, String> names() {
        Map<String, String> out = new HashMap<String, String>();
        for (Technician t : technicians.findAll()) {
            out.put(t.getId(), t.getName());
        }
        return out;
    }

    ScheduleRunDto toDto(ScheduleRun run) {
        List<ChangeDto> out = new ArrayList<ChangeDto>();
        Map<String, String> names = names();
        for (ScheduleChange row : changes.findByRunId(run.getId())) {
            out.add(toDto(row, names));
        }
        return new ScheduleRunDto(run.getId(), run.getRanAt(), run.getKind(), run.getSummary(),
                decode(run.getBeforeKpis()), decode(run.getAfterKpis()), out, run.getOperators(),
                run.getIterations(), run.getMillis());
    }

    /// The runs so far, newest first, at most `limit`.
    @Transactional(readOnly = true)
    public List<ScheduleRunDto> history(int limit) {
        List<ScheduleRunDto> out = new ArrayList<ScheduleRunDto>();
        for (ScheduleRun run : runs.newestFirst()) {
            if (out.size() >= limit) {
                break;
            }
            out.add(toDto(run));
        }
        return out;
    }

    /// Runs `body` as the one change to the plan under way, then tells
    /// everyone. The telling is after the commit: a technician's app reads the
    /// new route as soon as it hears, and must find it there.
    ScheduleRunDto publish(Change body) throws IOException {
        ScheduleRunDto run;
        synchronized (lock) {
            run = body.run();
        }
        if (run != null && run.id() != null) {
            live.scheduleChanged(run.id(), run.summary());
            notices.scheduleChanged(run);
        }
        return run;
    }

    /// One transactional change to the plan.
    interface Change {
        ScheduleRunDto run() throws IOException;
    }

    // ------------------------------------------------------------ operations

    /// Re-plans the whole horizon. Started work stays; the rest may move if
    /// moving it is worth what the disruption costs.
    public ScheduleRunDto optimize(final String actor) throws IOException {
        return publish(new Change() {
            @Override
            public ScheduleRunDto run() throws IOException {
                return optimizeNow(actor);
            }
        });
    }

    @Transactional(rollbackFor = IOException.class)
    ScheduleRunDto optimizeNow(String actor) throws IOException {
        Outcome o = solve(null, true, iterations);
        return write(o, OPTIMIZE, summarize(o, "Optimized the next " + HORIZON_DAYS + " days"), actor);
    }

    /// Repairs the plan after something outside it changed: a new job, an
    /// approved absence, a technician's new week. Fewer iterations than a full
    /// optimize, and no attempt from scratch: the point is to move little.
    public ScheduleRunDto repair(final String kind, final String headline, final String actor)
            throws IOException {
        return publish(new Change() {
            @Override
            public ScheduleRunDto run() throws IOException {
                return repairNow(kind, headline, actor);
            }
        });
    }

    @Transactional(rollbackFor = IOException.class)
    ScheduleRunDto repairNow(String kind, String headline, String actor) throws IOException {
        Outcome o = solve(null, false, Math.max(200, iterations / 3));
        return write(o, kind, summarize(o, headline), actor);
    }

    /// A technician is out. The time is cut from their availability as
    /// approved time off and the plan repaired around it.
    public ScheduleRunDto absence(final AbsenceDto absence, final String actor) throws IOException {
        if (absence == null || absence.techId() == null) {
            throw new ResponseStatusException(400, "Who is out?");
        }
        return publish(new Change() {
            @Override
            public ScheduleRunDto run() throws IOException {
                return absenceNow(absence, actor);
            }
        });
    }

    @Transactional(rollbackFor = IOException.class)
    ScheduleRunDto absenceNow(AbsenceDto absence, String actor) throws IOException {
        Technician tech = technicians.findById(absence.techId());
        if (tech == null) {
            throw new ResponseStatusException(404, "No such technician");
        }
        crew.addTimeOff(tech.getId(), absence.start(), absence.end(), absence.reason(), Catalog.APPROVED);
        Horizon h = horizon();
        Outcome o = solve(null, false, Math.max(300, iterations / 2));
        String head = tech.getName() + " out " + h.label(absence.start()) + " to "
                + h.label(absence.end());
        return write(o, ABSENCE, summarize(o, head), actor);
    }

    /// Approves or declines a request for time off. Approving re-plans around
    /// it; declining changes nothing but the request.
    public ScheduleRunDto decide(final String id, final boolean approve, final String actor)
            throws IOException {
        return publish(new Change() {
            @Override
            public ScheduleRunDto run() throws IOException {
                return decideNow(id, approve, actor);
            }
        });
    }

    @Transactional(rollbackFor = IOException.class)
    ScheduleRunDto decideNow(String id, boolean approve, String actor) throws IOException {
        TimeOff off = timeOff.findById(id);
        if (off == null) {
            throw new ResponseStatusException(404, "No such request");
        }
        if (!Catalog.PENDING.equals(off.getStatus())) {
            throw new ResponseStatusException(409, "That request was already answered");
        }
        Technician tech = technicians.findById(off.getTechId());
        String name = tech == null ? off.getTechId() : tech.getName();
        Horizon h = horizon();
        if (!approve) {
            off.setStatus(Catalog.DECLINED);
            timeOff.save(off);
            return new ScheduleRunDto(null, System.currentTimeMillis(), TIME_OFF,
                    "Declined " + name + "'s time off", null, null, new ArrayList<ChangeDto>(),
                    "", 0, 0);
        }
        off.setStatus(Catalog.APPROVED);
        timeOff.save(off);
        Outcome o = solve(null, false, Math.max(300, iterations / 2));
        return write(o, TIME_OFF, summarize(o, "Approved " + name + "'s time off from "
                + h.label(off.getStartAt())), actor);
    }

    /// Checks a drag on the board against every hard constraint, and applies
    /// it when asked and it holds. The job is pinned where it was dropped and
    /// the rest of the plan repaired around it; whatever that moves is listed
    /// as a warning, so the dispatcher sees the ripple before committing.
    public Validation move(final MoveDto move, final String actor) throws IOException {
        if (move == null || move.jobId() == null || move.techId() == null) {
            throw new ResponseStatusException(400, "Which job, and to whom?");
        }
        final Validation[] out = new Validation[1];
        ScheduleRunDto run = publish(new Change() {
            @Override
            public ScheduleRunDto run() throws IOException {
                out[0] = moveNow(move, actor);
                return out[0].run;
            }
        });
        out[0].run = run;
        return out[0];
    }

    /// What a move answers: whether it holds, why not, what else it moves,
    /// and the run when it was applied.
    public static final class Validation {
        public boolean ok;
        public String message;
        public List<String> warnings = new ArrayList<String>();
        public ScheduleRunDto run;
    }

    @Transactional(rollbackFor = IOException.class)
    Validation moveNow(MoveDto move, String actor) throws IOException {
        Validation v = new Validation();
        Job job = jobs.findById(move.jobId());
        Technician tech = technicians.findById(move.techId());
        if (job == null || tech == null) {
            throw new ResponseStatusException(404, "No such job or technician");
        }
        Horizon h = horizon();
        String hard = hardRule(job, tech, move.start(), h);
        if (hard != null) {
            v.message = hard;
            return v;
        }
        Pin pin = new Pin(job.getId(), tech.getId(), move.start());
        Outcome o = solve(pin, false, move.apply() ? Math.max(200, iterations / 4) : 150);
        Plan.Visit placed = o.after.visitOf(job.getId());
        int wanted = h.minute(move.start());
        if (placed == null || !placed.techId.equals(tech.getId()) || Math.abs(placed.start - wanted) > 1) {
            v.message = tech.getName() + " cannot reach " + job.getTitle() + " by "
                    + h.time(move.start()) + " after the job before it";
            return v;
        }
        v.ok = true;
        Map<String, String> names = new HashMap<String, String>();
        for (Technician t : o.model.techs.values()) {
            names.put(t.getId(), t.getName());
        }
        for (Reoptimizer.Change c : o.changes) {
            if (c.jobId.equals(job.getId())) {
                continue;
            }
            Job other = o.model.jobs.get(c.jobId);
            String title = other == null ? c.jobId : other.getTitle();
            if (c.toTech == null) {
                v.warnings.add(title + " no longer fits anyone's day");
            } else if (c.fromTech != null && !c.fromTech.equals(c.toTech)) {
                v.warnings.add(title + " moves from " + names.get(c.fromTech) + " to "
                        + names.get(c.toTech));
            } else {
                v.warnings.add(title + " shifts to " + h.time(h.millis(c.toStart)));
            }
        }
        v.message = v.warnings.isEmpty() ? "Fits without moving anything else"
                : "Fits; " + v.warnings.size() + " other job" + (v.warnings.size() == 1 ? "" : "s")
                + " adjust";
        if (move.apply()) {
            v.run = write(o, MOVE, job.getTitle() + " to " + tech.getName() + " at "
                    + h.label(move.start()) + (v.warnings.isEmpty() ? "" : "; " + v.message), actor);
        }
        return v;
    }

    /// The first hard rule a placement breaks, said the way a dispatcher would
    /// say it; null when it breaks none.
    String hardRule(Job job, Technician tech, long start, Horizon h) {
        if (!Catalog.isOpen(job.getStatus())) {
            return "That job is closed";
        }
        if (Catalog.isUnderWay(job.getStatus())) {
            return "That job has started; it stays with " + job.getTechId();
        }
        long need = Catalog.mask(job.getSkills());
        long missing = need & ~Catalog.mask(tech.getSkills());
        if (missing != 0) {
            return tech.getName() + " is not certified for " + Catalog.names(missing).get(0);
        }
        for (String x : Catalog.split(job.getExcludedTechs())) {
            if (x.equals(tech.getId())) {
                return "The customer has asked not to have " + tech.getName() + " on site";
            }
        }
        for (String[] req : Catalog.certifications(job.getCertifications())) {
            String held = null;
            for (String[] has : Catalog.certifications(tech.getCertifications())) {
                if (has[0].equals(req[0])) {
                    held = has[1];
                }
            }
            if (held == null) {
                return tech.getName() + " has no " + Catalog.label(req[0]) + " certificate";
            }
            if (lapsedBy(held, start)) {
                return tech.getName() + "'s " + Catalog.label(req[0]) + " certificate lapses on " + held;
            }
        }
        long end = start + job.getDurationMinutes() * 60000L;
        if (start < job.getWindowStart() || end > job.getWindowEnd()) {
            return "Outside the customer's window, " + h.label(job.getWindowStart()) + " to "
                    + h.time(job.getWindowEnd());
        }
        if (start < System.currentTimeMillis() - 60000L) {
            return "That time has passed";
        }
        for (TimeOff off : timeOff.overlapping(start, end)) {
            if (off.getTechId().equals(tech.getId()) && Catalog.APPROVED.equals(off.getStatus())) {
                return tech.getName() + " is off then: " + off.getReason();
            }
        }
        int from = h.minute(start);
        int to = h.minute(end);
        boolean inside = false;
        for (WorkWindow w : windows.ofTechnician(tech.getId())) {
            for (int d = 0; d < h.days(); d++) {
                if (w.getWeekday() == h.weekday(d) && from >= h.at(d, w.getStartMinute())
                        && to <= h.at(d, w.getEndMinute())) {
                    inside = true;
                }
            }
        }
        if (!inside) {
            return tech.getName() + " does not work then";
        }
        return null;
    }

    private String summarize(Outcome o, String headline) {
        int moved = 0;
        int dropped = 0;
        int added = 0;
        for (Reoptimizer.Change c : o.changes) {
            if (c.toTech == null) {
                dropped++;
            } else if (c.fromTech == null) {
                added++;
            } else {
                moved++;
            }
        }
        StringBuilder out = new StringBuilder(headline);
        out.append(": ");
        if (o.changes.isEmpty()) {
            out.append("no changes needed");
        } else {
            boolean first = true;
            first = part(out, first, added, "scheduled");
            first = part(out, first, moved, "moved");
            part(out, first, dropped, "left unassigned");
        }
        int saved = o.before.getDriveMinutes() - o.after.getDriveMinutes();
        if (saved > 0) {
            out.append(", ").append(saved).append(" min less driving");
        }
        return out.toString();
    }

    private static boolean part(StringBuilder out, boolean first, int n, String what) {
        if (n == 0) {
            return first;
        }
        if (!first) {
            out.append(", ");
        }
        out.append(n).append(n == 1 ? " job " : " jobs ").append(what);
        return false;
    }
}
