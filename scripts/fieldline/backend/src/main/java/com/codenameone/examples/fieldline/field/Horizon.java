package com.codenameone.examples.fieldline.field;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/// The span the scheduler plans, and the conversion between the database's
/// epoch milliseconds and the scheduler's minutes.
///
/// The scheduler counts whole minutes from the start of the span, which is a
/// midnight in the service's time zone. A minute is a minute of real time, so
/// a day that changes the clocks is 23 or 25 hours long here, and the start of
/// each day is asked of the zone rather than computed as `day * 1440`.
public final class Horizon {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private final ZoneId zone;
    private final LocalDate first;
    private final int days;
    private final long origin;

    private Horizon(ZoneId zone, LocalDate first, int days) {
        this.zone = zone;
        this.first = first;
        this.days = days;
        this.origin = first.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    /// The `days` days starting with the one `millis` falls in.
    public static Horizon of(long millis, int days, ZoneId zone) {
        LocalDate day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate();
        return new Horizon(zone, day, Math.max(1, Math.min(14, days)));
    }

    public long origin() {
        return origin;
    }

    public long end() {
        return dayStart(days);
    }

    public int days() {
        return days;
    }

    public ZoneId zone() {
        return zone;
    }

    /// The scheduler's minute for an instant; before the span is negative.
    public int minute(long millis) {
        long m = (millis - origin) / 60000L;
        return (int) Math.max(-1000000L, Math.min(1000000L, m));
    }

    public long millis(int minute) {
        return origin + minute * 60000L;
    }

    /// The midnight that starts day `d` of the span.
    public long dayStart(int d) {
        return first.plusDays(d).atStartOfDay(zone).toInstant().toEpochMilli();
    }

    /// The weekday of day `d`, 1 for Monday to 7 for Sunday.
    public int weekday(int d) {
        return first.plusDays(d).getDayOfWeek().getValue();
    }

    /// The instant `minuteOfDay` minutes after the local midnight of day `d`,
    /// as a scheduler minute. Read off the zone, so 8:00 is 8:00 on the day the
    /// clocks change too.
    public int at(int d, int minuteOfDay) {
        ZonedDateTime t = first.plusDays(d).atStartOfDay(zone).plusMinutes(minuteOfDay);
        return minute(t.toInstant().toEpochMilli());
    }

    /// "Thu 09:30", in the service's zone, for messages people read.
    public String label(long millis) {
        return CLOCK.format(Instant.ofEpochMilli(millis).atZone(zone));
    }

    public String time(long millis) {
        return TIME.format(Instant.ofEpochMilli(millis).atZone(zone));
    }
}
