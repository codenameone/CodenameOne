package java.time;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public final class LocalDateTime implements Comparable<LocalDateTime>, TemporalAccessor {
    private final LocalDate date;
    private final LocalTime time;

    private LocalDateTime(LocalDate date, LocalTime time) {
        this.date = date;
        this.time = time;
    }

    public static LocalDateTime now() {
        return now(Clock.systemDefaultZone());
    }

    public static LocalDateTime now(Clock clock) {
        return ofInstant(clock.instant(), clock.getZone());
    }

    public static LocalDateTime of(LocalDate date, LocalTime time) {
        return new LocalDateTime(date, time);
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute) {
        return of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute));
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        return of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute, second));
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second, int nano) {
        return of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute, second, nano));
    }

    public static LocalDateTime ofInstant(Instant instant, ZoneId zone) {
        return DateTimeSupport.localDateTimeFromInstant(instant, zone);
    }

    public static LocalDateTime parse(CharSequence text) {
        return (LocalDateTime) DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(text);
    }

    public static LocalDateTime parse(CharSequence text, DateTimeFormatter formatter) {
        return (LocalDateTime) formatter.parse(text);
    }

    public LocalDate toLocalDate() {
        return date;
    }

    public LocalTime toLocalTime() {
        return time;
    }

    public int getYear() {
        return date.getYear();
    }

    public int getMonthValue() {
        return date.getMonthValue();
    }

    public int getDayOfMonth() {
        return date.getDayOfMonth();
    }

    public int getHour() {
        return time.getHour();
    }

    public int getMinute() {
        return time.getMinute();
    }

    public int getSecond() {
        return time.getSecond();
    }

    public int getNano() {
        return time.getNano();
    }

    public LocalDateTime plusDays(long days) {
        return of(date.plusDays(days), time);
    }

    public LocalDateTime plusHours(long hours) {
        long totalNanos = time.toNanoOfDay() + hours * 3600L * DateTimeSupport.NANOS_PER_SECOND;
        long dayAdjust = DateTimeSupport.floorDiv(totalNanos, DateTimeSupport.NANOS_PER_DAY);
        long nanoOfDay = DateTimeSupport.floorMod(totalNanos, DateTimeSupport.NANOS_PER_DAY);
        return of(date.plusDays(dayAdjust), LocalTime.ofNanoOfDay(nanoOfDay));
    }

    public LocalDateTime plusMinutes(long minutes) {
        return plusHours(minutes / 60).plusSeconds((minutes % 60) * 60);
    }

    public LocalDateTime plusSeconds(long seconds) {
        long totalNanos = time.toNanoOfDay() + seconds * DateTimeSupport.NANOS_PER_SECOND;
        long dayAdjust = DateTimeSupport.floorDiv(totalNanos, DateTimeSupport.NANOS_PER_DAY);
        long nanoOfDay = DateTimeSupport.floorMod(totalNanos, DateTimeSupport.NANOS_PER_DAY);
        return of(date.plusDays(dayAdjust), LocalTime.ofNanoOfDay(nanoOfDay));
    }

    public Instant toInstant(ZoneOffset offset) {
        return Instant.ofEpochSecond(DateTimeSupport.toEpochSecond(date, time, offset), time.getNano());
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    public int compareTo(LocalDateTime other) {
        int cmp = date.compareTo(other.date);
        return cmp != 0 ? cmp : time.compareTo(other.time);
    }

    public boolean equals(Object obj) {
        return obj instanceof LocalDateTime && compareTo((LocalDateTime) obj) == 0;
    }

    public int hashCode() {
        return date.hashCode() * 31 + time.hashCode();
    }

    public String toString() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(this);
    }

    Instant toInstant() {
        return toInstant(ZoneOffset.UTC);
    }

    ZoneId getZoneForFormatting() {
        return ZoneOffset.UTC;
    }
}
