package java.time;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public final class OffsetDateTime implements Comparable<OffsetDateTime>, TemporalAccessor {
    private final LocalDateTime dateTime;
    private final ZoneOffset offset;

    private OffsetDateTime(LocalDateTime dateTime, ZoneOffset offset) {
        this.dateTime = dateTime;
        this.offset = offset;
    }

    public static OffsetDateTime of(LocalDate date, LocalTime time, ZoneOffset offset) {
        return new OffsetDateTime(LocalDateTime.of(date, time), offset);
    }

    public static OffsetDateTime of(LocalDateTime dateTime, ZoneOffset offset) {
        return new OffsetDateTime(dateTime, offset);
    }

    public static OffsetDateTime ofInstant(Instant instant, ZoneId zone) {
        ZoneOffset offset = DateTimeSupport.offsetFromInstant(instant, zone);
        LocalDateTime local = LocalDateTime.ofInstant(instant, zone);
        return new OffsetDateTime(local, offset);
    }

    public static OffsetDateTime parse(CharSequence text) {
        return (OffsetDateTime) DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(text);
    }

    public static OffsetDateTime parse(CharSequence text, DateTimeFormatter formatter) {
        return (OffsetDateTime) formatter.parse(text);
    }

    public LocalDateTime toLocalDateTime() {
        return dateTime;
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

    public int compareTo(OffsetDateTime other) {
        int cmp = toInstant().compareTo(other.toInstant());
        return cmp != 0 ? cmp : dateTime.compareTo(other.dateTime);
    }

    public boolean equals(Object obj) {
        return obj instanceof OffsetDateTime && compareTo((OffsetDateTime) obj) == 0;
    }

    public int hashCode() {
        return dateTime.hashCode() * 31 + offset.hashCode();
    }

    public String toString() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this);
    }

    ZoneId getZoneForFormatting() {
        return offset;
    }
}
