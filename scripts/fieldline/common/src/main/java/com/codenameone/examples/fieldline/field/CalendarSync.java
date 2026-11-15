package com.codenameone.examples.fieldline.field;

import com.codename1.calendar.CalendarAccess;
import com.codename1.calendar.CalendarAuthorizationStatus;
import com.codename1.calendar.CalendarDateTime;
import com.codename1.calendar.CalendarEvent;
import com.codename1.calendar.CalendarMutationScope;
import com.codename1.calendar.CalendarSource;
import com.codename1.calendar.LocalCalendarSource;
import com.codename1.io.Log;
import com.codenameone.examples.fieldline.Prefs;
import com.codenameone.examples.fieldline.api.JobDto;
import com.codenameone.examples.fieldline.ui.Ui;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Puts a technician's planned jobs into the phone's own calendar.
///
/// Write-only access is all it asks for: the app never reads the calendar.
/// The event made for each job is remembered on this device as
/// `jobId=eventId`, so a job the planner moved updates its event in place
/// instead of adding a second one.
public final class CalendarSync {
    private static boolean asked;

    private CalendarSync() {
    }

    /// The switch in Settings was turned on: ask the platform for access now,
    /// while the user is looking at why.
    public static void request() {
        CalendarSource source = LocalCalendarSource.getInstance();
        if (!source.isAvailable()) {
            Ui.say("This device has no calendar the app can write to");
            Prefs.setCalendarSync(false);
            return;
        }
        asked = true;
        source.requestAuthorization(CalendarAccess.EVENTS_WRITE_ONLY).onResult((status, err) -> {
            if (err != null || status == CalendarAuthorizationStatus.DENIED
                    || status == CalendarAuthorizationStatus.RESTRICTED) {
                Prefs.setCalendarSync(false);
                Ui.say("Calendar access was not allowed");
            }
        });
    }

    /// Writes or updates an event for each of `jobs` that is planned.
    public static void sync(final List<JobDto> jobs) {
        if (!Prefs.calendarSync() || jobs == null || jobs.isEmpty()) {
            return;
        }
        final CalendarSource source = LocalCalendarSource.getInstance();
        if (!source.isAvailable()) {
            return;
        }
        if (!asked) {
            asked = true;
            source.requestAuthorization(CalendarAccess.EVENTS_WRITE_ONLY).onResult((s, e) -> {
                if (e == null && (s == CalendarAuthorizationStatus.WRITE_ONLY
                        || s == CalendarAuthorizationStatus.FULL)) {
                    write(source, jobs);
                }
            });
            return;
        }
        write(source, jobs);
    }

    private static void write(CalendarSource source, List<JobDto> jobs) {
        final Map<String, String> known = read();
        ZoneId zone = ZoneId.systemDefault();
        for (final JobDto job : jobs) {
            if (job.plannedStart() <= 0 || "COMPLETED".equals(job.status())) {
                continue;
            }
            CalendarEvent event = new CalendarEvent();
            String id = known.get(job.id());
            if (id != null) {
                event.setId(id);
            }
            event.setTitle(job.title() + " - " + job.siteName());
            event.setLocation(job.address());
            event.setDescription(job.customer() + "\n" + job.description());
            event.setStart(CalendarDateTime.instant(Instant.ofEpochMilli(job.plannedStart()), zone));
            event.setEnd(CalendarDateTime.instant(Instant.ofEpochMilli(job.plannedEnd()), zone));
            source.saveEvent(event, CalendarMutationScope.THIS_INSTANCE).onResult((saved, err) -> {
                if (err != null) {
                    Log.e(err);
                    return;
                }
                if (saved != null && saved.getId() != null) {
                    Map<String, String> now = read();
                    now.put(job.id(), saved.getId());
                    store(now);
                }
            });
        }
    }

    private static Map<String, String> read() {
        Map<String, String> out = new HashMap<String, String>();
        String lines = Prefs.calendarEvents();
        if (lines == null) {
            return out;
        }
        int at = 0;
        while (at < lines.length()) {
            int end = lines.indexOf('\n', at);
            if (end < 0) {
                end = lines.length();
            }
            String line = lines.substring(at, end);
            int eq = line.indexOf('=');
            if (eq > 0) {
                out.put(line.substring(0, eq), line.substring(eq + 1));
            }
            at = end + 1;
        }
        return out;
    }

    private static void store(Map<String, String> events) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> e : events.entrySet()) {
            out.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        Prefs.setCalendarEvents(out.toString());
    }
}
