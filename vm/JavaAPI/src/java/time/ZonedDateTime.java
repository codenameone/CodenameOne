package java.time;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public final class ZonedDateTime implements Comparable<ZonedDateTime>, TemporalAccessor {
    private final LocalDateTime dateTime;
    private final ZoneId zone;
    private final ZoneOffset offset;

    private ZonedDateTime(LocalDateTime dateTime, ZoneId zone, ZoneOffset offset) {
        this.dateTime = dateTime;
        this.zone = zone;
        this.offset = offset;
    }

    public static ZonedDateTime now() {
        return now(Clock.systemDefaultZone());
    }

    public static ZonedDateTime now(Clock clock) {
        return ofInstant(clock.instant(), clock.getZone());
    }

    public static ZonedDateTime of(LocalDate date, LocalTime time, ZoneId zone) {
        LocalDateTime ldt = LocalDateTime.of(date, time);
        Instant instant = ldt.toInstant(DateTimeSupport.offsetFromInstant(ldt.toInstant(ZoneOffset.UTC), zone));
        return ofInstant(instant, zone);
    }

    public static ZonedDateTime of(LocalDateTime dateTime, ZoneId zone) {
        Instant instant = dateTime.toInstant(DateTimeSupport.offsetFromInstant(dateTime.toInstant(ZoneOffset.UTC), zone));
        return ofInstant(instant, zone);
    }

    public static ZonedDateTime ofInstant(Instant instant, ZoneId zone) {
        ZoneOffset offset = DateTimeSupport.offsetFromInstant(instant, zone);
        LocalDateTime local = LocalDateTime.ofInstant(instant, zone);
        return new ZonedDateTime(local, zone, offset);
    }

    public static ZonedDateTime parse(CharSequence text) {
        return (ZonedDateTime) DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(text);
    }

    public static ZonedDateTime parse(CharSequence text, DateTimeFormatter formatter) {
        return (ZonedDateTime) formatter.parse(text);
    }

    public LocalDateTime toLocalDateTime() {
        return dateTime;
    }

    public ZoneId getZone() {
        return zone;
    }

    public ZoneOffset getOffset() {
        return offset;
    }

    public Instant toInstant() {
        return dateTime.toInstant(offset);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    public int compareTo(ZonedDateTime other) {
        int cmp = toInstant().compareTo(other.toInstant());
        if (cmp != 0) {
            return cmp;
        }
        cmp = dateTime.compareTo(other.dateTime);
        return cmp != 0 ? cmp : zone.getId().compareTo(other.zone.getId());
    }

    public boolean equals(Object obj) {
        return obj instanceof ZonedDateTime && compareTo((ZonedDateTime) obj) == 0;
    }

    public int hashCode() {
        return dateTime.hashCode() * 31 + zone.hashCode();
    }

    public String toString() {
        return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(this);
    }

    ZoneId getZoneForFormatting() {
        return zone;
    }
}
