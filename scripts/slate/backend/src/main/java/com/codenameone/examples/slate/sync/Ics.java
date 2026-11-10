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
package com.codenameone.examples.slate.sync;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/// iCalendar (RFC 5545), as much of it as Slate writes and reads.
///
/// Written: a provider's feed of their bookings, which any calendar app can
/// subscribe to, and one booking as a file a client opens to add it. Read:
/// a provider's own feed of busy time, of which only the event times matter.
///
/// What the reader does not do: expand `RRULE` repeats, or honour
/// `EXDATE`. A weekly event in a subscribed feed is busy on its first
/// occurrence only. Google and Microsoft links read busy time already
/// expanded by the calendar itself, which is why they are the ones to use for
/// a calendar with repeats in it.
public final class Ics {
    private static final DateTimeFormatter UTC = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter LOCAL = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private Ics() {
    }

    /// One event of a calendar being written.
    public static final class Event {
        public String uid = "";
        public long start;
        public long end;
        public String summary = "";
        public String description = "";
        public String location = "";
        public String url = "";
        /// Minutes before the start to remind, or 0 for none.
        public int alarmMinutes;
        public boolean cancelled;
    }

    // tag::slate-ics-write[]
    /// A whole calendar of `events`, with CRLF line ends and long lines folded.
    public static String write(String name, List<Event> events) {
        StringBuilder out = new StringBuilder();
        line(out, "BEGIN:VCALENDAR");
        line(out, "VERSION:2.0");
        line(out, "PRODID:-//Codename One//Slate//EN");
        line(out, "CALSCALE:GREGORIAN");
        line(out, "METHOD:PUBLISH");
        line(out, "X-WR-CALNAME:" + text(name));
        String stamp = UTC.format(Instant.now());
        for (int iter = 0; iter < events.size(); iter++) {
            Event e = events.get(iter);
            line(out, "BEGIN:VEVENT");
            line(out, "UID:" + e.uid + "@slate");
            line(out, "DTSTAMP:" + stamp);
            line(out, "DTSTART:" + UTC.format(Instant.ofEpochMilli(e.start)));
            line(out, "DTEND:" + UTC.format(Instant.ofEpochMilli(e.end)));
            line(out, "SUMMARY:" + text(e.summary));
            if (e.description.length() > 0) {
                line(out, "DESCRIPTION:" + text(e.description));
            }
            if (e.location.length() > 0) {
                line(out, "LOCATION:" + text(e.location));
            }
            if (e.url.length() > 0) {
                line(out, "URL:" + e.url);
            }
            line(out, "STATUS:" + (e.cancelled ? "CANCELLED" : "CONFIRMED"));
            if (e.alarmMinutes > 0 && !e.cancelled) {
                line(out, "BEGIN:VALARM");
                line(out, "ACTION:DISPLAY");
                line(out, "DESCRIPTION:" + text(e.summary));
                line(out, "TRIGGER:-PT" + e.alarmMinutes + "M");
                line(out, "END:VALARM");
            }
            line(out, "END:VEVENT");
        }
        line(out, "END:VCALENDAR");
        return out.toString();
    }
    // end::slate-ics-write[]

    /// The busy intervals of a calendar between `from` and `to`, each
    /// `{start, end}`. A date without a time is the whole day in `zone`;
    /// a transparent or cancelled event is not busy.
    public static List<long[]> busy(String calendar, ZoneId zone, long from, long to) {
        List<long[]> out = new ArrayList<long[]>();
        List<String> lines = unfold(calendar);
        boolean inEvent = false;
        long start = -1;
        long end = -1;
        long duration = -1;
        boolean free = false;
        for (int iter = 0; iter < lines.size(); iter++) {
            String line = lines.get(iter);
            if ("BEGIN:VEVENT".equals(line)) {
                inEvent = true;
                start = -1;
                end = -1;
                duration = -1;
                free = false;
                continue;
            }
            if (!inEvent) {
                continue;
            }
            if ("END:VEVENT".equals(line)) {
                inEvent = false;
                if (end < 0 && start >= 0 && duration >= 0) {
                    end = start + duration;
                }
                if (!free && start >= 0 && end > start && start < to && end > from) {
                    out.add(new long[] {start, end});
                }
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String head = line.substring(0, colon);
            String value = line.substring(colon + 1).trim();
            String name = head;
            String tz = "";
            int semi = head.indexOf(';');
            if (semi >= 0) {
                name = head.substring(0, semi);
                int at = head.indexOf("TZID=");
                if (at >= 0) {
                    int stop = head.indexOf(';', at);
                    tz = head.substring(at + 5, stop < 0 ? head.length() : stop);
                }
            }
            if ("DTSTART".equals(name)) {
                start = time(value, tz, zone);
            } else if ("DTEND".equals(name)) {
                end = time(value, tz, zone);
            } else if ("DURATION".equals(name)) {
                duration = duration(value);
            } else if ("TRANSP".equals(name)) {
                free = free || "TRANSPARENT".equals(value);
            } else if ("STATUS".equals(name)) {
                free = free || "CANCELLED".equals(value);
            }
        }
        return out;
    }

    static long time(String value, String tz, ZoneId fallback) {
        try {
            if (value.length() == 8) {
                return LocalDate.parse(value, DATE).atStartOfDay(fallback).toInstant()
                        .toEpochMilli();
            }
            if (value.endsWith("Z")) {
                return Instant.from(UTC.parse(value)).toEpochMilli();
            }
            ZoneId zone = fallback;
            if (tz.length() > 0) {
                try {
                    zone = ZoneId.of(tz);
                } catch (RuntimeException unknown) {
                    zone = fallback;
                }
            }
            return LocalDateTime.parse(value, LOCAL).atZone(zone).toInstant().toEpochMilli();
        } catch (RuntimeException bad) {
            return -1;
        }
    }

    /// `PT1H30M`, `P1D` and their sums, in milliseconds, or -1.
    static long duration(String value) {
        long total = 0;
        long number = 0;
        boolean seen = false;
        for (int iter = 0; iter < value.length(); iter++) {
            char c = value.charAt(iter);
            if (c >= '0' && c <= '9') {
                number = number * 10 + (c - '0');
                seen = true;
            } else if (c == 'W') {
                total += number * 7L * 86400000L;
                number = 0;
            } else if (c == 'D') {
                total += number * 86400000L;
                number = 0;
            } else if (c == 'H') {
                total += number * 3600000L;
                number = 0;
            } else if (c == 'M') {
                total += number * 60000L;
                number = 0;
            } else if (c == 'S') {
                total += number * 1000L;
                number = 0;
            }
        }
        return seen ? total : -1;
    }

    private static List<String> unfold(String text) {
        List<String> out = new ArrayList<String>();
        // No String.split: the compiled server's class library has no regex.
        String all = text == null ? "" : text;
        int from = 0;
        while (from < all.length()) {
            int nl = all.indexOf('\n', from);
            int end = nl < 0 ? all.length() : nl;
            String line = all.substring(from, end > from && all.charAt(end - 1) == '\r' ? end - 1 : end);
            from = end + 1;
            if ((line.startsWith(" ") || line.startsWith("\t")) && !out.isEmpty()) {
                out.set(out.size() - 1, out.get(out.size() - 1) + line.substring(1));
            } else if (line.length() > 0) {
                out.add(line);
            }
        }
        return out;
    }

    /// Escapes a TEXT value: backslash, comma, semicolon and line breaks.
    static String text(String value) {
        StringBuilder out = new StringBuilder();
        String v = value == null ? "" : value;
        for (int iter = 0; iter < v.length(); iter++) {
            char c = v.charAt(iter);
            if (c == '\\' || c == ',' || c == ';') {
                out.append('\\').append(c);
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c != '\r') {
                out.append(c);
            }
        }
        return out.toString();
    }

    /// Appends a content line folded at 74 characters, as the RFC asks.
    private static void line(StringBuilder out, String line) {
        int at = 0;
        while (line.length() - at > 74) {
            out.append(line, at, at + 74).append("\r\n ");
            at += 74;
        }
        out.append(line.substring(at)).append("\r\n");
    }
}
