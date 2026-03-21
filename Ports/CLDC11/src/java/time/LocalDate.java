package java.time;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public final class LocalDate implements Comparable<LocalDate>, TemporalAccessor {
    private final int year;
    private final int month;
    private final int dayOfMonth;

    private LocalDate(int year, int month, int dayOfMonth) {
        DateTimeSupport.checkDate(year, month, dayOfMonth);
        this.year = year;
        this.month = month;
        this.dayOfMonth = dayOfMonth;
    }

    public static LocalDate now() {
        return now(Clock.systemDefaultZone());
    }

    public static LocalDate now(Clock clock) {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone()).toLocalDate();
    }

    public static LocalDate of(int year, int month, int dayOfMonth) {
        return new LocalDate(year, month, dayOfMonth);
    }

    public static LocalDate ofEpochDay(long epochDay) {
        int[] parts = DateTimeSupport.epochDayToDate(epochDay);
        return of(parts[0], parts[1], parts[2]);
    }

    public static LocalDate parse(CharSequence text) {
        return (LocalDate) DateTimeFormatter.ISO_LOCAL_DATE.parse(text);
    }

    public static LocalDate parse(CharSequence text, DateTimeFormatter formatter) {
        return (LocalDate) formatter.parse(text);
    }

    public int getYear() {
        return year;
    }

    public int getMonthValue() {
        return month;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public boolean isLeapYear() {
        return DateTimeSupport.isLeapYear(year);
    }

    public int lengthOfMonth() {
        return DateTimeSupport.lengthOfMonth(year, month);
    }

    public long toEpochDay() {
        return DateTimeSupport.toEpochDay(year, month, dayOfMonth);
    }

    public LocalDate plusDays(long daysToAdd) {
        return ofEpochDay(toEpochDay() + daysToAdd);
    }

    public LocalDate plusMonths(long monthsToAdd) {
        long monthCount = year * 12L + (month - 1);
        long calcMonths = monthCount + monthsToAdd;
        int newYear = (int) DateTimeSupport.floorDiv(calcMonths, 12);
        int newMonth = (int) DateTimeSupport.floorMod(calcMonths, 12) + 1;
        int newDay = Math.min(dayOfMonth, DateTimeSupport.lengthOfMonth(newYear, newMonth));
        return of(newYear, newMonth, newDay);
    }

    public LocalDate plusYears(long yearsToAdd) {
        int newYear = (int) (year + yearsToAdd);
        int newDay = Math.min(dayOfMonth, DateTimeSupport.lengthOfMonth(newYear, month));
        return of(newYear, month, newDay);
    }

    public LocalDate minusDays(long daysToSubtract) {
        return plusDays(-daysToSubtract);
    }

    public LocalDateTime atTime(int hour, int minute) {
        return LocalDateTime.of(this, LocalTime.of(hour, minute));
    }

    public LocalDateTime atTime(int hour, int minute, int second) {
        return LocalDateTime.of(this, LocalTime.of(hour, minute, second));
    }

    public LocalDateTime atTime(int hour, int minute, int second, int nano) {
        return LocalDateTime.of(this, LocalTime.of(hour, minute, second, nano));
    }

    public LocalDateTime atTime(LocalTime time) {
        return LocalDateTime.of(this, time);
    }

    public ZonedDateTime atStartOfDay(ZoneId zone) {
        return ZonedDateTime.of(this, LocalTime.MIDNIGHT, zone);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    public int compareTo(LocalDate other) {
        if (year != other.year) {
            return year < other.year ? -1 : 1;
        }
        if (month != other.month) {
            return month < other.month ? -1 : 1;
        }
        return dayOfMonth < other.dayOfMonth ? -1 : dayOfMonth > other.dayOfMonth ? 1 : 0;
    }

    public boolean equals(Object obj) {
        return obj instanceof LocalDate && compareTo((LocalDate) obj) == 0;
    }

    public int hashCode() {
        return year * 37 + month * 13 + dayOfMonth;
    }

    public String toString() {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(this);
    }
}
