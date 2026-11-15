package com.codenameone.examples.fieldline.field;

import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.fieldline.Ids;
import com.codenameone.examples.fieldline.ai.Diagnostician;
import com.codenameone.examples.fieldline.ai.JobContext;
import com.codenameone.examples.fieldline.api.BoardDto;
import com.codenameone.examples.fieldline.api.BreakDto;
import com.codenameone.examples.fieldline.api.CheckDto;
import com.codenameone.examples.fieldline.api.ChecklistDto;
import com.codenameone.examples.fieldline.api.DiagnosisDto;
import com.codenameone.examples.fieldline.api.JobDto;
import com.codenameone.examples.fieldline.api.NewJobDto;
import com.codenameone.examples.fieldline.api.PhotoDto;
import com.codenameone.examples.fieldline.api.PhotoUploadDto;
import com.codenameone.examples.fieldline.api.ReportDto;
import com.codenameone.examples.fieldline.api.ScheduleRunDto;
import com.codenameone.examples.fieldline.api.SignatureDto;
import com.codenameone.examples.fieldline.api.SiteDto;
import com.codenameone.examples.fieldline.api.TechnicianDto;
import com.codenameone.examples.fieldline.api.TimeOffDto;
import com.codenameone.examples.fieldline.api.TodayDto;
import com.codenameone.examples.fieldline.api.UserDto;
import com.codenameone.examples.fieldline.domain.Job;
import com.codenameone.examples.fieldline.domain.PlannedBreak;
import com.codenameone.examples.fieldline.domain.PlannerPolicy;
import com.codenameone.examples.fieldline.domain.JobCheck;
import com.codenameone.examples.fieldline.domain.JobPhoto;
import com.codenameone.examples.fieldline.domain.Preference;
import com.codenameone.examples.fieldline.domain.Signature;
import com.codenameone.examples.fieldline.domain.Site;
import com.codenameone.examples.fieldline.domain.Technician;
import com.codenameone.examples.fieldline.domain.TimeOff;
import com.codenameone.examples.fieldline.live.LiveHub;
import com.codenameone.examples.fieldline.repository.JobCheckRepository;
import com.codenameone.examples.fieldline.repository.JobPhotoRepository;
import com.codenameone.examples.fieldline.repository.JobRepository;
import com.codenameone.examples.fieldline.repository.PlannedBreakRepository;
import com.codenameone.examples.fieldline.repository.PreferenceRepository;
import com.codenameone.examples.fieldline.repository.ScheduleRunRepository;
import com.codenameone.examples.fieldline.repository.SignatureRepository;
import com.codenameone.examples.fieldline.repository.SiteRepository;
import com.codenameone.examples.fieldline.repository.TechnicianRepository;
import com.codenameone.examples.fieldline.repository.TimeOffRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The work itself: a technician's day, each job's progress, and the evidence
/// a job is closed on -- the checklist, the photos and the customer's
/// signature.
///
/// A technician reaches the jobs assigned to them and no others; a dispatcher
/// reaches every job. A job moves SCHEDULED, EN_ROUTE, ON_SITE, COMPLETED and
/// never backwards, and once it is under way the scheduler leaves it where it
/// is.
///
/// The signature is verifiable. What the customer signs is the report as it
/// stood -- job, site, every checklist answer, the notes, the photos by id,
/// who signed, when and where -- reduced to one SHA-256. Anything changed
/// afterwards no longer hashes to the stored value, and [#signature] says so.
@Component
public class Jobs {
    /// The largest photo accepted, in Base64 characters: about 1.5 MB of JPEG.
    static final int MAX_PHOTO = 2000000;
    /// The largest signature accepted, in Base64 characters.
    static final int MAX_SIGNATURE = 400000;
    /// The caller the dispatch endpoint reads jobs as: the path's own role
    /// check has already admitted a dispatcher.
    static final UserDto DISPATCHER = new UserDto("", "", "", false, true, false, false, "", "en");

    private final JobRepository jobs;
    private final SiteRepository sites;
    private final TechnicianRepository technicians;
    private final JobCheckRepository checks;
    private final JobPhotoRepository photos;
    private final SignatureRepository signatures;
    private final TimeOffRepository timeOff;
    private final PreferenceRepository preferences;
    private final ScheduleRunRepository runs;
    private final Planner planner;
    private final Diagnostician diagnostician;
    private final LiveHub live;
    private final PlannedBreakRepository breaks;
    private final Timekeeping clock;

    public Jobs(JobRepository jobs, SiteRepository sites, TechnicianRepository technicians,
            JobCheckRepository checks, JobPhotoRepository photos, SignatureRepository signatures,
            TimeOffRepository timeOff, PreferenceRepository preferences, ScheduleRunRepository runs,
            Planner planner, Diagnostician diagnostician, LiveHub live,
            PlannedBreakRepository breaks, Timekeeping clock) {
        this.breaks = breaks;
        this.clock = clock;
        this.jobs = jobs;
        this.sites = sites;
        this.technicians = technicians;
        this.checks = checks;
        this.photos = photos;
        this.signatures = signatures;
        this.timeOff = timeOff;
        this.preferences = preferences;
        this.runs = runs;
        this.planner = planner;
        this.diagnostician = diagnostician;
        this.live = live;
    }

    // ------------------------------------------------------------ reading

    JobDto toDto(Job job, Site site, String techName, boolean detail) {
        List<CheckDto> list = new ArrayList<CheckDto>();
        int photoCount = 0;
        boolean signed = false;
        if (detail) {
            for (JobCheck c : checks.ofJob(job.getId())) {
                list.add(new CheckDto(c.getOrdinal(), c.getLabel(), c.getResult(), c.getNote()));
            }
            photoCount = (int) photos.countByJobId(job.getId());
            signed = signatures.existsById(job.getId());
        }
        return new JobDto(job.getId(), job.getTitle(), job.getKind(), job.getStatus(),
                job.getPriority(), job.getSiteId(), site == null ? "" : site.getName(),
                site == null ? "" : site.getAddress(), site == null ? 0 : site.getLat(),
                site == null ? 0 : site.getLng(), site == null ? "" : site.getCustomer(),
                site == null ? "" : site.getContactPhone(), site == null ? "" : site.getAssetType(),
                site == null ? "" : site.getAssetModel(), job.getDescription(),
                job.getDurationMinutes(), Catalog.split(job.getSkills()), job.getWindowStart(),
                job.getWindowEnd(), job.getTechId(), techName == null ? "" : techName,
                job.getPlannedStart(), job.getPlannedEnd(), job.getTravelBefore(), job.getSequence(),
                job.getActualStart(), job.getActualEnd(), list, photoCount, signed, job.getNotes(),
                job.getDueAt(), Catalog.split(job.getCertifications()), job.getAfterJobId(),
                Catalog.split(job.getExcludedTechs()),
                site == null ? "" : Catalog.territoryOf(site.getLat(), site.getLng()),
                job.getUnassignedReason());
    }

    private Job reachable(UserDto user, String id) {
        Job job = jobs.findById(id);
        if (job == null) {
            throw new ResponseStatusException(404, "No such job");
        }
        if (!user.dispatcher() && !job.getTechId().equals(user.techId())) {
            throw new ResponseStatusException(403, "That job is not assigned to you");
        }
        return job;
    }

    private String nameOf(String techId) {
        if (techId == null || techId.length() == 0) {
            return "";
        }
        Technician t = technicians.findById(techId);
        return t == null ? "" : t.getName();
    }

    @Transactional(readOnly = true)
    public JobDto job(UserDto user, String id) {
        Job job = reachable(user, id);
        return toDto(job, sites.findById(job.getSiteId()), nameOf(job.getTechId()), true);
    }

    /// Today for the signed-in technician: the route in driving order, with
    /// the drive before each stop.
    @Transactional(readOnly = true)
    public TodayDto today(String username) throws IOException {
        Technician tech = technicians.findByUsername(username);
        if (tech == null) {
            throw new ResponseStatusException(403, "This account is not a technician");
        }
        Horizon h = planner.horizon();
        // A route is never left blank while there is work ahead: on an evening,
        // a weekend or a day off the screen shows the next day that has jobs,
        // and says which day that is.
        int shown = 0;
        for (int d = 0; d < h.days(); d++) {
            if (hasWork(jobs.plannedFor(tech.getId(), h.dayStart(d), h.dayStart(d + 1)), d == 0)) {
                shown = d;
                break;
            }
        }
        List<JobDto> out = new ArrayList<JobDto>();
        int drive = 0;
        int done = 0;
        int busy = 0;
        String status = "AVAILABLE";
        for (Job job : jobs.plannedFor(tech.getId(), h.dayStart(shown), h.dayStart(shown + 1))) {
            if (Catalog.CANCELLED.equals(job.getStatus())) {
                continue;
            }
            out.add(toDto(job, sites.findById(job.getSiteId()), tech.getName(), true));
            drive += job.getTravelBefore();
            busy += job.getDurationMinutes();
            if (Catalog.COMPLETED.equals(job.getStatus())) {
                done++;
            }
            if (Catalog.isUnderWay(job.getStatus())) {
                status = job.getStatus();
            }
        }
        TechnicianDto me = Crew.toDto(tech, status, out.size(), Math.min(100, busy * 100 / 540));
        return new TodayDto(me, out, drive, done, h.dayStart(shown), clock.clockedInAt(tech.getId()));
    }

    /// Whether a day is worth showing: any live job does, and today also does
    /// while it holds finished work, so the technician sees what they did.
    private static boolean hasWork(List<Job> day, boolean today) {
        for (Job job : day) {
            if (Catalog.CANCELLED.equals(job.getStatus())) {
                continue;
            }
            if (today || !Catalog.COMPLETED.equals(job.getStatus())) {
                return true;
            }
        }
        return false;
    }

    /// The dispatcher's board: everyone's routes over the horizon, the open
    /// jobs, today's figures, the last run and the time off in the span.
    @Transactional(readOnly = true)
    public BoardDto board() throws IOException {
        Horizon h = planner.horizon();
        Map<String, Site> siteMap = new HashMap<String, Site>();
        for (Site s : sites.findAll()) {
            siteMap.put(s.getId(), s);
        }
        Map<String, Technician> techMap = new HashMap<String, Technician>();
        List<Technician> crew = technicians.active();
        for (Technician t : crew) {
            techMap.put(t.getId(), t);
        }
        Map<String, int[]> today = new HashMap<String, int[]>();
        Map<String, String> state = new HashMap<String, String>();
        List<JobDto> list = new ArrayList<JobDto>();
        long dayEnd = h.dayStart(1);
        for (Job job : jobs.inSpan(h.origin(), h.end())) {
            if (Catalog.CANCELLED.equals(job.getStatus())) {
                continue;
            }
            Technician t = techMap.get(job.getTechId());
            list.add(toDto(job, siteMap.get(job.getSiteId()), t == null ? "" : t.getName(), false));
            if (t != null && job.getPlannedStart() < dayEnd && job.getPlannedStart() >= h.origin()) {
                int[] c = today.get(t.getId());
                if (c == null) {
                    c = new int[2];
                    today.put(t.getId(), c);
                }
                c[0]++;
                c[1] += job.getDurationMinutes();
                if (Catalog.isUnderWay(job.getStatus())) {
                    state.put(t.getId(), job.getStatus());
                }
            }
        }
        List<TimeOffDto> offs = new ArrayList<TimeOffDto>();
        long now = System.currentTimeMillis();
        for (TimeOff off : timeOff.overlapping(h.origin(), h.end())) {
            if (Catalog.DECLINED.equals(off.getStatus())) {
                continue;
            }
            Technician t = techMap.get(off.getTechId());
            offs.add(Crew.toDto(off, t == null ? off.getTechId() : t.getName()));
            if (t != null && Catalog.APPROVED.equals(off.getStatus()) && off.getStartAt() <= now
                    && off.getEndAt() > now) {
                state.put(t.getId(), "OFF");
            }
        }
        List<TechnicianDto> people = new ArrayList<TechnicianDto>();
        for (Technician t : crew) {
            int[] c = today.get(t.getId());
            String s = state.get(t.getId());
            people.add(Crew.toDto(t, s == null ? "AVAILABLE" : s, c == null ? 0 : c[0],
                    c == null ? 0 : Math.min(100, c[1] * 100 / 540)));
        }
        List<BreakDto> pauses = new ArrayList<BreakDto>();
        for (PlannedBreak b : breaks.overlapping(h.origin(), h.end())) {
            pauses.add(new BreakDto(b.getTechId(), b.getStartAt(), b.getEndAt()));
        }
        List<ScheduleRunDto> last = planner.history(1);
        return new BoardDto(h.origin(), h.days(), people, list, planner.currentKpis(),
                last.isEmpty() ? null : last.get(0), offs, pauses);
    }

    /// The requests waiting for a dispatcher's answer, oldest first.
    @Transactional(readOnly = true)
    public List<TimeOffDto> pendingTimeOff() {
        List<TimeOffDto> out = new ArrayList<TimeOffDto>();
        for (TimeOff off : timeOff.withStatus(Catalog.PENDING)) {
            out.add(Crew.toDto(off, nameOf(off.getTechId())));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<SiteDto> sites() {
        List<SiteDto> out = new ArrayList<SiteDto>();
        for (Site s : sites.byName()) {
            out.add(new SiteDto(s.getId(), s.getName(), s.getAddress(), s.getLat(), s.getLng(),
                    s.getCustomer(), s.getAssetType(), s.getAssetModel()));
        }
        return out;
    }

    // ------------------------------------------------------------ writing

    /// A new job, with its checklist from the catalog. The plan is repaired
    /// around it after the commit, by the caller.
    @Transactional(rollbackFor = IOException.class)
    public JobDto create(NewJobDto wanted) throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to create");
        }
        Site site = wanted.siteId() == null ? null : sites.findById(wanted.siteId());
        if (site == null) {
            throw new ResponseStatusException(400, "Pick a site");
        }
        String title = wanted.title() == null ? "" : wanted.title().trim();
        if (title.length() == 0 || title.length() > 160) {
            throw new ResponseStatusException(400, "A title is 1 to 160 characters");
        }
        String kind = Catalog.isKind(wanted.kind()) ? wanted.kind() : Catalog.REPAIR;
        if (wanted.durationMinutes() < 15 || wanted.durationMinutes() > 600) {
            throw new ResponseStatusException(400, "A job takes 15 minutes to 10 hours");
        }
        if (wanted.windowEnd() - wanted.windowStart() < wanted.durationMinutes() * 60000L) {
            throw new ResponseStatusException(400, "The window is shorter than the job");
        }
        String skills = wanted.skills() == null || wanted.skills().isEmpty()
                ? Catalog.skillFor(site.getAssetType()) : Catalog.join(wanted.skills());
        if (wanted.dueAt() != 0 && wanted.dueAt() < wanted.windowStart() + wanted.durationMinutes() * 60000L) {
            throw new ResponseStatusException(400, "The SLA deadline is before the job could be done");
        }
        String after = wanted.afterJobId() == null ? "" : wanted.afterJobId().trim();
        if (after.length() > 0 && jobs.findById(after) == null) {
            throw new ResponseStatusException(400, "The job this waits on does not exist");
        }
        List<String> excluded = new ArrayList<String>();
        if (wanted.excludedTechs() != null) {
            for (String id : wanted.excludedTechs()) {
                if (technicians.findById(id) == null) {
                    throw new ResponseStatusException(400, "Unknown technician " + id);
                }
                excluded.add(id);
            }
        }
        Job job = newJob("j" + Ids.next().substring(0, 12), title, kind, wanted.priority(), site,
                wanted.description(), wanted.durationMinutes(), skills, wanted.windowStart(),
                wanted.windowEnd());
        if (wanted.dueAt() > 0) {
            job.setDueAt(wanted.dueAt());
        }
        if (wanted.certifications() != null) {
            job.setCertifications(String.join(",", Catalog.known(Catalog.CERTIFICATIONS, wanted.certifications())));
        }
        job.setAfterJobId(after);
        job.setExcludedTechs(String.join(",", excluded));
        jobs.save(job);
        return toDto(job, site, "", true);
    }

    /// Writes a job and its checklist. Also how the demo data makes its jobs.
    Job newJob(String id, String title, String kind, int priority, Site site, String description,
            int duration, String skills, long windowStart, long windowEnd) throws IOException {
        Job job = new Job();
        job.setId(id);
        job.setTitle(title);
        job.setKind(kind);
        job.setStatus(Catalog.UNASSIGNED);
        job.setPriority(Math.max(1, Math.min(5, priority)));
        job.setSiteId(site.getId());
        String d = description == null ? "" : description.trim();
        job.setDescription(d.length() > 1000 ? d.substring(0, 1000) : d);
        job.setDurationMinutes(duration);
        job.setSkills(skills);
        job.setWindowStart(windowStart);
        job.setWindowEnd(windowEnd);
        job.setCertifications(Catalog.certificationFor(site.getAssetType()));
        long now = System.currentTimeMillis();
        // The SLA by policy: urgent work within hours of the call, the rest
        // within days -- but never later than the customer's own window.
        PlannerPolicy policy = planner.policy();
        int hours = job.getPriority() >= 4 ? policy.getUrgentSlaHours() : policy.getRoutineSlaHours();
        long sla = Math.max(now, windowStart) + hours * 3600000L;
        job.setDueAt(Math.max(windowStart + duration * 60000L, Math.min(windowEnd, sla)));
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        jobs.insert(job);
        List<String> lines = Catalog.checklist(kind, site.getAssetType());
        for (int iter = 0; iter < lines.size(); iter++) {
            JobCheck c = new JobCheck();
            c.setId(id + ":" + iter);
            c.setJobId(id);
            c.setOrdinal(iter);
            c.setLabel(lines.get(iter));
            checks.insert(c);
        }
        return job;
    }

    /// Moves a job forward. ON_SITE records the arrival, and how far from the
    /// site it was reported, in the notes: a check-in a kilometer away is not
    /// refused -- GPS indoors is poor -- but it is on the record. COMPLETED
    /// needs every checklist line answered and, for an inspection, the
    /// customer's signature.
    public JobDto setStatus(UserDto user, String id, String status, double lat, double lng)
            throws IOException {
        JobDto out = advance(user, id, status, lat, lng);
        clock.jobStatus(out.techId(), out.id(), out.status());
        live.jobChanged(out.id(), out.status(), usernameOf(out.techId()));
        return out;
    }

    @Transactional(readOnly = true)
    String usernameOf(String techId) {
        Technician t = techId == null || techId.length() == 0 ? null : technicians.findById(techId);
        return t == null ? null : t.getUsername();
    }

    @Transactional(rollbackFor = IOException.class)
    JobDto advance(UserDto user, String id, String status, double lat, double lng)
            throws IOException {
        Job job = reachable(user, id);
        String from = job.getStatus();
        int a = order(from);
        int b = order(status);
        if (b < 0 || b <= a || Catalog.CANCELLED.equals(from)) {
            throw new ResponseStatusException(409, "A " + from + " job cannot become " + status);
        }
        if (job.getTechId().length() == 0) {
            throw new ResponseStatusException(409, "Assign the job first");
        }
        Site site = sites.findById(job.getSiteId());
        long now = System.currentTimeMillis();
        if (Catalog.ON_SITE.equals(status) || (Catalog.COMPLETED.equals(status) && job.getActualStart() == 0)) {
            job.setActualStart(now);
            if (site != null && (lat != 0 || lng != 0)) {
                double km = distanceKm(lat, lng, site.getLat(), site.getLng());
                if (km > 0.5) {
                    note(job, "Checked in " + Math.round(km * 10) / 10.0 + " km from the site.");
                }
            }
        }
        if (Catalog.COMPLETED.equals(status)) {
            for (JobCheck c : checks.ofJob(id)) {
                if (c.getResult().length() == 0) {
                    throw new ResponseStatusException(409, "Answer every checklist line first: "
                            + c.getLabel());
                }
            }
            if (Catalog.INSPECTION.equals(job.getKind()) && !signatures.existsById(id)) {
                throw new ResponseStatusException(409, "An inspection needs the customer's signature");
            }
            job.setActualEnd(now);
        }
        job.setStatus(status);
        job.setUpdatedAt(now);
        jobs.save(job);
        return toDto(job, site, nameOf(job.getTechId()), true);
    }

    private static void note(Job job, String line) {
        String notes = job.getNotes().length() == 0 ? line : job.getNotes() + "\n" + line;
        job.setNotes(notes.length() > 2000 ? notes.substring(notes.length() - 2000) : notes);
    }

    private static int order(String status) {
        String[] flow = {Catalog.UNASSIGNED, Catalog.SCHEDULED, Catalog.EN_ROUTE, Catalog.ON_SITE,
            Catalog.COMPLETED};
        for (int iter = 0; iter < flow.length; iter++) {
            if (flow[iter].equals(status)) {
                return iter;
            }
        }
        return -1;
    }

    static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1) * Math.cos(Math.toRadians((lat1 + lat2) / 2));
        return 6371.0 * Math.sqrt(dLat * dLat + dLng * dLng);
    }

    /// Saves the answers by line. A signed job's record is closed: changing it
    /// would break the signature, so it is refused instead.
    @Transactional(rollbackFor = IOException.class)
    public JobDto saveChecklist(UserDto user, String id, ChecklistDto wanted) throws IOException {
        Job job = reachable(user, id);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        if (signatures.existsById(id) || Catalog.COMPLETED.equals(job.getStatus())) {
            throw new ResponseStatusException(409, "This job is signed off; its record is closed");
        }
        Map<Integer, JobCheck> byIndex = new HashMap<Integer, JobCheck>();
        for (JobCheck c : checks.ofJob(id)) {
            byIndex.put(Integer.valueOf(c.getOrdinal()), c);
        }
        if (wanted.checklist() != null) {
            for (CheckDto c : wanted.checklist()) {
                JobCheck row = byIndex.get(Integer.valueOf(c.index()));
                if (row == null) {
                    continue;
                }
                String r = c.result() == null ? "" : c.result();
                if (r.length() > 0 && !Catalog.PASS.equals(r) && !Catalog.FAIL.equals(r)
                        && !Catalog.NA.equals(r)) {
                    throw new ResponseStatusException(400, "A result is PASS, FAIL or NA");
                }
                String n = c.note() == null ? "" : c.note().trim();
                row.setResult(r);
                row.setNote(n.length() > 500 ? n.substring(0, 500) : n);
                checks.save(row);
            }
        }
        if (wanted.notes() != null) {
            String n = wanted.notes().trim();
            job.setNotes(n.length() > 2000 ? n.substring(0, 2000) : n);
        }
        job.setUpdatedAt(System.currentTimeMillis());
        jobs.save(job);
        return toDto(job, sites.findById(job.getSiteId()), nameOf(job.getTechId()), true);
    }

    // ------------------------------------------------------------ evidence

    /// Stores a photo and, when the technician has the assistant on, asks it
    /// what the photo shows. The diagnosis is made outside the transaction --
    /// a model call takes seconds and must not hold the database -- and
    /// stored after it.
    public PhotoDto addPhoto(UserDto user, String id, PhotoUploadDto upload) throws IOException {
        if (upload == null || upload.data() == null || upload.data().length() == 0) {
            throw new ResponseStatusException(400, "No photo");
        }
        if (upload.data().length() > MAX_PHOTO) {
            throw new ResponseStatusException(413, "That photo is too large; send it at most 1.5 MB");
        }
        String mime = "image/png".equals(upload.mime()) ? "image/png" : "image/jpeg";
        try {
            Base64.getDecoder().decode(upload.data());
        } catch (IllegalArgumentException bad) {
            throw new ResponseStatusException(400, "That is not a picture");
        }
        String note = upload.note() == null ? "" : upload.note().trim();
        if (note.length() > 500) {
            note = note.substring(0, 500);
        }
        JobPhoto photo = storePhoto(user, id, upload.data(), mime, note);
        DiagnosisDto diagnosis = null;
        if (wantsAssist(user.username())) {
            diagnosis = diagnostician.diagnose(context(user, id), upload.data(), mime, note);
            keepDiagnosis(photo.getId(), diagnosis);
        }
        live.jobChanged(id, "PHOTO", null);
        return new PhotoDto(photo.getId(), id, photo.getTakenAt(), note, null, diagnosis);
    }

    @Transactional(readOnly = true)
    boolean wantsAssist(String username) {
        Preference p = preferences.findById(username);
        return p == null || p.isAiAssist();
    }

    @Transactional(rollbackFor = IOException.class)
    JobPhoto storePhoto(UserDto user, String id, String data, String mime, String note)
            throws IOException {
        Job job = reachable(user, id);
        if (signatures.existsById(id) || Catalog.COMPLETED.equals(job.getStatus())) {
            throw new ResponseStatusException(409, "This job is signed off; its record is closed");
        }
        if (photos.countByJobId(id) >= 24) {
            throw new ResponseStatusException(409, "A job keeps at most 24 photos");
        }
        JobPhoto photo = new JobPhoto();
        photo.setId("p" + Ids.next().substring(0, 12));
        photo.setJobId(id);
        photo.setTakenAt(System.currentTimeMillis());
        photo.setNote(note);
        photo.setMime(mime);
        photo.setData(data);
        photo.setDiagnosis("");
        photos.insert(photo);
        return photo;
    }

    @Transactional(rollbackFor = IOException.class)
    void keepDiagnosis(String photoId, DiagnosisDto d) throws IOException {
        JobPhoto photo = photos.findById(photoId);
        if (photo != null && d != null) {
            Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
            m.put("issue", d.issue());
            m.put("severity", d.severity());
            m.put("likelyCause", d.likelyCause());
            m.put("recommendedAction", d.recommendedAction());
            m.put("parts", d.parts());
            m.put("confidence", Integer.valueOf(d.confidence()));
            m.put("safetyNote", d.safetyNote());
            m.put("source", d.source());
            photo.setDiagnosis(Json.write(m));
            photos.save(photo);
        }
    }

    static DiagnosisDto readDiagnosis(String json) {
        if (json == null || json.length() == 0) {
            return null;
        }
        try {
            Map m = Json.parseObject(json);
            List<String> parts = new ArrayList<String>();
            Object p = m.get("parts");
            if (p instanceof List) {
                for (Object o : (List) p) {
                    parts.add(String.valueOf(o));
                }
            }
            Object c = m.get("confidence");
            return new DiagnosisDto(text(m, "issue"), text(m, "severity"), text(m, "likelyCause"),
                    text(m, "recommendedAction"), parts, c instanceof Number ? ((Number) c).intValue() : 0,
                    text(m, "safetyNote"), text(m, "source"));
        } catch (IOException bad) {
            return null;
        }
    }

    private static String text(Map m, String key) {
        Object v = m.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    @Transactional(readOnly = true)
    public List<PhotoDto> photos(UserDto user, String id) {
        reachable(user, id);
        List<PhotoDto> out = new ArrayList<PhotoDto>();
        for (JobPhoto p : photos.ofJob(id)) {
            out.add(new PhotoDto(p.getId(), id, p.getTakenAt(), p.getNote(), p.getData(),
                    readDiagnosis(p.getDiagnosis())));
        }
        return out;
    }

    /// What the assistant is told about a job.
    @Transactional(readOnly = true)
    public JobContext context(UserDto user, String id) {
        Job job = reachable(user, id);
        Site site = sites.findById(job.getSiteId());
        JobContext c = new JobContext();
        c.jobId = job.getId();
        c.title = job.getTitle();
        c.kind = job.getKind();
        c.description = job.getDescription();
        c.notes = job.getNotes();
        c.technician = nameOf(job.getTechId());
        if (site != null) {
            c.site = site.getName();
            c.customer = site.getCustomer();
            c.assetType = site.getAssetType();
            c.assetModel = site.getAssetModel();
        }
        for (JobCheck k : checks.ofJob(id)) {
            c.checklist.add(new CheckDto(k.getOrdinal(), k.getLabel(), k.getResult(), k.getNote()));
        }
        for (JobPhoto p : photos.ofJob(id)) {
            DiagnosisDto d = readDiagnosis(p.getDiagnosis());
            if (d != null) {
                c.diagnoses.add(d);
            }
            if (p.getNote().length() > 0) {
                c.photoNotes.add(p.getNote());
            }
        }
        return c;
    }

    public ReportDto draftReport(UserDto user, String id) {
        return diagnostician.report(context(user, id));
    }

    /// The customer signs. The report as it stands is hashed with the
    /// signature's own facts, and both are kept.
    @Transactional(rollbackFor = IOException.class)
    public SignatureDto sign(UserDto user, String id, SignatureDto wanted) throws IOException {
        Job job = reachable(user, id);
        if (wanted == null || wanted.png() == null || wanted.png().length() < 100) {
            throw new ResponseStatusException(400, "No signature");
        }
        if (wanted.png().length() > MAX_SIGNATURE) {
            throw new ResponseStatusException(413, "That signature is too large");
        }
        String signer = wanted.signerName() == null ? "" : wanted.signerName().trim();
        if (signer.length() == 0 || signer.length() > 120) {
            throw new ResponseStatusException(400, "Who is signing?");
        }
        if (signatures.existsById(id)) {
            throw new ResponseStatusException(409, "This job is already signed");
        }
        if (!Catalog.ON_SITE.equals(job.getStatus())) {
            throw new ResponseStatusException(409, "The customer signs while you are on site");
        }
        Signature s = new Signature();
        s.setJobId(id);
        s.setSignerName(signer);
        s.setPng(wanted.png());
        s.setSignedAt(System.currentTimeMillis());
        s.setLat(wanted.lat());
        s.setLng(wanted.lng());
        s.setReportHash(Ids.sha256(canonical(job, s)));
        signatures.insert(s);
        return new SignatureDto(id, signer, s.getPng(), s.getSignedAt(), s.getLat(), s.getLng(),
                s.getReportHash(), true);
    }

    /// The stored signature, and whether the record still hashes to it.
    @Transactional(readOnly = true)
    public SignatureDto signature(UserDto user, String id) throws IOException {
        Job job = reachable(user, id);
        Signature s = signatures.findById(id);
        if (s == null) {
            throw new ResponseStatusException(404, "Not signed yet");
        }
        boolean verified = Ids.sha256(canonical(job, s)).equals(s.getReportHash());
        return new SignatureDto(id, s.getSignerName(), s.getPng(), s.getSignedAt(), s.getLat(),
                s.getLng(), s.getReportHash(), verified);
    }

    /// The text a signature is the hash of: one fact a line, in a fixed
    /// order, so the same record always reads the same. The notes are taken
    /// up to the signing; a line the server adds later (a check-in distance)
    /// is part of what was signed only if it was there before.
    String canonical(Job job, Signature s) {
        StringBuilder out = new StringBuilder("fieldline-report-v1\n");
        out.append("job=").append(job.getId()).append('\n');
        out.append("title=").append(job.getTitle()).append('\n');
        out.append("site=").append(job.getSiteId()).append('\n');
        out.append("tech=").append(job.getTechId()).append('\n');
        out.append("arrived=").append(job.getActualStart()).append('\n');
        for (JobCheck c : checks.ofJob(job.getId())) {
            out.append("check.").append(c.getOrdinal()).append('=').append(c.getResult()).append('|')
                    .append(c.getNote()).append('\n');
        }
        for (JobPhoto p : photos.ofJob(job.getId())) {
            out.append("photo=").append(p.getId()).append('\n');
        }
        out.append("notes=").append(job.getNotes().replace('\n', ' ')).append('\n');
        out.append("signer=").append(s.getSignerName()).append('\n');
        out.append("at=").append(s.getSignedAt()).append('\n');
        out.append("where=").append(Math.round(s.getLat() * 100000)).append(',')
                .append(Math.round(s.getLng() * 100000)).append('\n');
        out.append("png=").append(s.getPng().length()).append(':').append(s.getPng().hashCode());
        return out.toString();
    }
}
