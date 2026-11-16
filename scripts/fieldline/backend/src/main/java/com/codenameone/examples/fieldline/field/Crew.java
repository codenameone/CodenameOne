package com.codenameone.examples.fieldline.field;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.fieldline.Ids;
import com.codenameone.examples.fieldline.api.AvailabilityDto;
import com.codenameone.examples.fieldline.api.CrewMemberDto;
import com.codenameone.examples.fieldline.api.TechnicianDto;
import com.codenameone.examples.fieldline.api.TimeOffDto;
import com.codenameone.examples.fieldline.api.WindowDto;
import com.codenameone.examples.fieldline.domain.Technician;
import com.codenameone.examples.fieldline.domain.TimeOff;
import com.codenameone.examples.fieldline.domain.WorkWindow;
import com.codenameone.examples.fieldline.live.LiveHub;
import com.codenameone.examples.fieldline.repository.LabourRateRepository;
import com.codenameone.examples.fieldline.repository.TechnicianRepository;
import com.codenameone.examples.fieldline.repository.TimeOffRepository;
import com.codenameone.examples.fieldline.repository.WorkWindowRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The technicians: who they are, the week each one says they work, the time
/// they ask to have off, and where they were last seen.
///
/// What a technician declares here is a constraint, not a wish. The scheduler
/// never plans work outside a window, past the drive limit, or through
/// approved time off; it pays to plan past the regular hours, and never past
/// the maximum.
@Component
public class Crew {
    /// Where a technician without a home of their own starts the day.
    public static final double DEPOT_LAT = 45.5231;
    public static final double DEPOT_LNG = -122.6765;
    public static final String DEPOT_LABEL = "Fieldline depot, 1200 NW Front Ave";

    /// One color per technician on the board and the map, picked in order.
    static final int[] PALETTE = {0x6366F1, 0x14B8A6, 0xF59E0B, 0xF43F5E, 0x0EA5E9, 0x8B5CF6,
        0x10B981, 0xF97316, 0xEC4899, 0x84CC16, 0x06B6D4, 0xD946EF};

    private final TechnicianRepository technicians;
    private final WorkWindowRepository windows;
    private final TimeOffRepository timeOff;
    private final LabourRateRepository rates;
    private final LiveHub live;

    public Crew(TechnicianRepository technicians, WorkWindowRepository windows,
            TimeOffRepository timeOff, LiveHub live, LabourRateRepository rates) {
        this.rates = rates;
        this.technicians = technicians;
        this.windows = windows;
        this.timeOff = timeOff;
        this.live = live;
    }

    /// Makes a newly registered account a technician: electrical work, the
    /// depot as home, and the ordinary week of eight to five, Monday to Friday.
    /// They change all of it from the app.
    @Transactional(rollbackFor = IOException.class)
    public Technician enroll(String username, String name) throws IOException {
        Technician existing = technicians.findByUsername(username);
        if (existing != null) {
            return existing;
        }
        Technician tech = new Technician();
        tech.setId("t" + Ids.next().substring(0, 12));
        tech.setUsername(username);
        tech.setName(name);
        tech.setInitials(initials(name));
        tech.setColor(PALETTE[(int) (technicians.count() % PALETTE.length)]);
        tech.setSkills("ELECTRICAL");
        tech.setHomeLat(DEPOT_LAT);
        tech.setHomeLng(DEPOT_LNG);
        tech.setHomeLabel(DEPOT_LABEL);
        tech.setLat(DEPOT_LAT);
        tech.setLng(DEPOT_LNG);
        tech.setMaxDriveMinutes(180);
        tech.setRegularMinutesPerWeek(40 * 60);
        tech.setMaxMinutesPerWeek(50 * 60);
        tech.setActive(true);
        technicians.insert(tech);
        for (int day = 1; day <= 5; day++) {
            addWindow(tech.getId(), day, 8 * 60, 17 * 60);
        }
        return tech;
    }

    void addWindow(String techId, int weekday, int start, int end) throws IOException {
        WorkWindow w = new WorkWindow();
        w.setId(techId + ":" + weekday + ":" + start);
        w.setTechId(techId);
        w.setWeekday(weekday);
        w.setStartMinute(start);
        w.setEndMinute(end);
        windows.insert(w);
    }

    /// "Dana Ortiz" is DO, "Cher" is CH.
    public static String initials(String name) {
        String text = name == null ? "" : name.trim();
        if (text.length() == 0) {
            return "?";
        }
        int space = text.lastIndexOf(' ');
        String out = space > 0 ? "" + text.charAt(0) + text.charAt(space + 1)
                : text.substring(0, Math.min(2, text.length()));
        StringBuilder up = new StringBuilder();
        for (int iter = 0; iter < out.length(); iter++) {
            char c = out.charAt(iter);
            up.append(c >= 'a' && c <= 'z' ? (char) (c - ('a' - 'A')) : c);
        }
        return up.toString();
    }

    /// The technician an account signs in as; 403 for an account that is not one.
    public Technician of(String username) {
        Technician tech = technicians.findByUsername(username);
        if (tech == null) {
            throw new ResponseStatusException(403, "This account is not a technician");
        }
        return tech;
    }

    public static String hex(int color) {
        String digits = Integer.toHexString(color & 0xFFFFFF);
        while (digits.length() < 6) {
            digits = "0" + digits;
        }
        return "#" + digits;
    }

    public static TechnicianDto toDto(Technician t, String status, int jobsToday, int utilization) {
        return new TechnicianDto(t.getId(), t.getName(), t.getInitials(), hex(t.getColor()),
                Catalog.split(t.getSkills()), t.getHomeLat(), t.getHomeLng(), t.getHomeLabel(),
                t.getLat(), t.getLng(), status, jobsToday, utilization, t.getUsername());
    }

    /// Every technician record, active or not, by name.
    @Transactional(readOnly = true)
    public List<CrewMemberDto> members() throws IOException {
        List<Technician> all = new ArrayList<Technician>(technicians.findAll());
        all.sort((a, b) -> a.getName().compareTo(b.getName()));
        List<CrewMemberDto> out = new ArrayList<CrewMemberDto>();
        for (Technician t : all) {
            out.add(member(t));
        }
        return out;
    }

    /// Saves what an admin may change on a technician. The limits are kept
    /// to what a working week can hold, so a typo cannot plan someone for
    /// a hundred hours or forbid every drive.
    @Transactional(rollbackFor = IOException.class)
    public CrewMemberDto change(CrewMemberDto change) throws IOException {
        Technician tech = change == null || change.id() == null ? null
                : technicians.findById(change.id());
        if (tech == null) {
            throw new ResponseStatusException(404, "No such technician");
        }
        String skills = Catalog.join(change.skills());
        if (skills.length() == 0) {
            throw new ResponseStatusException(400, "A technician needs at least one skill");
        }
        if (change.maxDriveMinutes() < 15 || change.maxDriveMinutes() > 8 * 60) {
            throw new ResponseStatusException(400, "Keep the driving a day between 15 minutes and 8 hours");
        }
        if (change.maxMinutesPerWeek() < 60 || change.maxMinutesPerWeek() > 70 * 60
                || change.regularMinutesPerWeek() < 60
                || change.regularMinutesPerWeek() > change.maxMinutesPerWeek()) {
            throw new ResponseStatusException(400,
                    "Regular hours must be at least one and no more than the most, which is at most 70");
        }
        // Zero is "not sent": a client from before the daily caps keeps them.
        int jobsCap = change.maxJobsPerDay() == 0 ? tech.getMaxJobsPerDay() : change.maxJobsPerDay();
        int dayCap = change.maxMinutesPerDay() == 0 ? tech.getMaxMinutesPerDay()
                : change.maxMinutesPerDay();
        if (jobsCap < 1 || jobsCap > 20) {
            throw new ResponseStatusException(400, "Keep the jobs a day between 1 and 20");
        }
        if (dayCap < 4 * 60 || dayCap > 14 * 60) {
            throw new ResponseStatusException(400, "Keep the longest day between 4 and 14 hours");
        }
        List<String> certs = new ArrayList<String>();
        for (String item : change.certifications() == null ? Catalog.split(tech.getCertifications()) : change.certifications()) {
            int at = item.indexOf('=');
            String code = at < 0 ? item : item.substring(0, at);
            String date = at < 0 ? "" : item.substring(at + 1).trim();
            if (Catalog.known(Catalog.CERTIFICATIONS, java.util.Collections.singletonList(code)).isEmpty()) {
                throw new ResponseStatusException(400, "Unknown certification " + code);
            }
            if (date.length() > 0) {
                try {
                    java.time.LocalDate.parse(date);
                } catch (RuntimeException bad) {
                    throw new ResponseStatusException(400, "Write the expiry of " + Catalog.label(code)
                            + " as year-month-day, like 2027-03-31");
                }
            }
            certs.add(date.length() == 0 ? code : code + "=" + date);
        }
        List<String> zones = Catalog.known(Catalog.TERRITORIES,
                change.territories() == null ? Catalog.split(tech.getTerritories()) : change.territories());
        tech.setSkills(skills);
        tech.setCertifications(String.join(",", certs));
        tech.setTerritories(String.join(",", zones));
        tech.setMaxJobsPerDay(jobsCap);
        tech.setMaxMinutesPerDay(dayCap);
        tech.setMaxDriveMinutes(change.maxDriveMinutes());
        tech.setRegularMinutesPerWeek(change.regularMinutesPerWeek());
        tech.setMaxMinutesPerWeek(change.maxMinutesPerWeek());
        tech.setActive(change.active());
        if (change.rateId() != null && !change.rateId().equals(tech.getRateId())) {
            if (change.rateId().length() > 0 && !rates.existsById(change.rateId())) {
                throw new ResponseStatusException(400, "No such labour rate");
            }
            tech.setRateId(change.rateId());
        }
        technicians.save(tech);
        return member(tech);
    }

    static CrewMemberDto member(Technician t) {
        return new CrewMemberDto(t.getId(), t.getName(), t.getInitials(), hex(t.getColor()),
                t.getUsername(), Catalog.split(t.getSkills()), t.getHomeLabel(),
                t.getMaxDriveMinutes(), t.getRegularMinutesPerWeek(), t.getMaxMinutesPerWeek(),
                t.isActive(), Catalog.split(t.getCertifications()), Catalog.split(t.getTerritories()),
                t.getMaxJobsPerDay(), t.getMaxMinutesPerDay(), t.getRateId());
    }

    @Transactional(readOnly = true)
    public AvailabilityDto availability(String username) throws IOException {
        return availabilityOf(of(username));
    }

    AvailabilityDto availabilityOf(Technician tech) {
        List<WindowDto> week = new ArrayList<WindowDto>();
        for (WorkWindow w : windows.ofTechnician(tech.getId())) {
            week.add(new WindowDto(w.getWeekday(), w.getStartMinute(), w.getEndMinute()));
        }
        return new AvailabilityDto(tech.getId(), week, tech.getMaxDriveMinutes(),
                tech.getMaxMinutesPerWeek() / 60, tech.getRegularMinutesPerWeek() / 60);
    }

    /// Replaces the technician's week and limits. Each window is checked: a
    /// weekday, inside the day, at least half an hour long, none overlapping.
    @Transactional(rollbackFor = IOException.class)
    public AvailabilityDto saveAvailability(String username, AvailabilityDto wanted)
            throws IOException {
        if (wanted == null || wanted.week() == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        Technician tech = of(username);
        List<WindowDto> week = wanted.week();
        if (week.size() > 21) {
            throw new ResponseStatusException(400, "At most three windows a day");
        }
        for (int a = 0; a < week.size(); a++) {
            WindowDto w = week.get(a);
            if (w.day() < 1 || w.day() > 7 || w.start() < 0 || w.end() > 24 * 60
                    || w.end() - w.start() < 30) {
                throw new ResponseStatusException(400, "Each window is a weekday and at least "
                        + "half an hour inside it");
            }
            for (int b = a + 1; b < week.size(); b++) {
                WindowDto o = week.get(b);
                if (o.day() == w.day() && o.start() < w.end() && w.start() < o.end()) {
                    throw new ResponseStatusException(400, "Two windows overlap");
                }
            }
        }
        int regular = wanted.regularHoursPerWeek();
        int max = wanted.maxHoursPerWeek();
        if (regular < 1 || regular > 60 || max < regular || max > 72) {
            throw new ResponseStatusException(400, "Regular hours are 1 to 60 a week, and the "
                    + "maximum is between them and 72");
        }
        if (wanted.maxDriveMinutes() < 30 || wanted.maxDriveMinutes() > 600) {
            throw new ResponseStatusException(400, "A drive limit is 30 to 600 minutes a day");
        }
        windows.deleteByTechId(tech.getId());
        for (WindowDto w : week) {
            addWindow(tech.getId(), w.day(), w.start(), w.end());
        }
        // Read again: the bulk delete above cleared what the session held.
        tech = of(username);
        tech.setMaxDriveMinutes(wanted.maxDriveMinutes());
        tech.setRegularMinutesPerWeek(regular * 60);
        tech.setMaxMinutesPerWeek(max * 60);
        technicians.save(tech);
        return availabilityOf(tech);
    }

    public static TimeOffDto toDto(TimeOff off, String techName) {
        return new TimeOffDto(off.getId(), off.getTechId(), techName, off.getStartAt(),
                off.getEndAt(), off.getReason(), off.getStatus());
    }

    @Transactional(readOnly = true)
    public List<TimeOffDto> myTimeOff(String username) throws IOException {
        Technician tech = of(username);
        List<TimeOffDto> out = new ArrayList<TimeOffDto>();
        for (TimeOff off : timeOff.ofTechnician(tech.getId())) {
            out.add(toDto(off, tech.getName()));
        }
        return out;
    }

    /// Asks for time off. It waits for a dispatcher; nothing moves until then.
    @Transactional(rollbackFor = IOException.class)
    public TimeOffDto requestTimeOff(String username, TimeOffDto request) throws IOException {
        Technician tech = of(username);
        return toDto(addTimeOff(tech.getId(), request == null ? 0 : request.start(),
                request == null ? 0 : request.end(), request == null ? null : request.reason(),
                Catalog.PENDING), tech.getName());
    }

    TimeOff addTimeOff(String techId, long start, long end, String reason, String status)
            throws IOException {
        if (end <= start || end - start > 30L * 86400000L) {
            throw new ResponseStatusException(400, "Time off ends after it starts, within 30 days");
        }
        if (end < System.currentTimeMillis()) {
            throw new ResponseStatusException(400, "That time is already over");
        }
        String why = reason == null ? "" : reason.trim();
        if (why.length() > 255) {
            why = why.substring(0, 255);
        }
        TimeOff off = new TimeOff();
        off.setId("o" + Ids.next().substring(0, 12));
        off.setTechId(techId);
        off.setStartAt(start);
        off.setEndAt(end);
        off.setReason(why.length() == 0 ? "Time off" : why);
        off.setStatus(status);
        off.setCreatedAt(System.currentTimeMillis());
        timeOff.insert(off);
        return off;
    }

    /// Where the technician is, from the app. Sent on to the dispatchers' map
    /// after the commit.
    public void reportLocation(String username, double lat, double lng) throws IOException {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new ResponseStatusException(400, "That is not a position");
        }
        String id = record(username, lat, lng);
        live.technicianMoved(id, lat, lng);
    }

    @Transactional(rollbackFor = IOException.class)
    String record(String username, double lat, double lng) throws IOException {
        Technician tech = of(username);
        tech.setLat(lat);
        tech.setLng(lng);
        tech.setSeenAt(System.currentTimeMillis());
        technicians.save(tech);
        return tech.getId();
    }
}
