package java.time;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public final class LocalTime implements Comparable<LocalTime>, TemporalAccessor {
    public static final LocalTime MIDNIGHT = new LocalTime(0, 0, 0, 0);

    private final int hour;
    private final int minute;
    private final int second;
    private final int nano;

    private LocalTime(int hour, int minute, int second, int nano) {
        DateTimeSupport.checkTime(hour, minute, second, nano);
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.nano = nano;
    }

    public static LocalTime now() {
        return now(Clock.systemDefaultZone());
    }

    public static LocalTime now(Clock clock) {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone()).toLocalTime();
    }

    public static LocalTime of(int hour, int minute) {
        return new LocalTime(hour, minute, 0, 0);
    }

    public static LocalTime of(int hour, int minute, int second) {
        return new LocalTime(hour, minute, second, 0);
    }

    public static LocalTime of(int hour, int minute, int second, int nano) {
        return new LocalTime(hour, minute, second, nano);
    }

    public static LocalTime ofSecondOfDay(long secondOfDay) {
        int hours = (int) (secondOfDay / 3600);
        int minutes = (int) ((secondOfDay % 3600) / 60);
        int seconds = (int) (secondOfDay % 60);
        return of(hours, minutes, seconds);
    }

    public static LocalTime ofNanoOfDay(long nanoOfDay) {
        long secondOfDay = nanoOfDay / DateTimeSupport.NANOS_PER_SECOND;
        int nanos = (int) (nanoOfDay % DateTimeSupport.NANOS_PER_SECOND);
        LocalTime base = ofSecondOfDay(secondOfDay);
        return of(base.hour, base.minute, base.second, nanos);
    }

    public static LocalTime parse(CharSequence text) {
        return (LocalTime) DateTimeFormatter.ISO_LOCAL_TIME.parse(text);
    }

    public static LocalTime parse(CharSequence text, DateTimeFormatter formatter) {
        return (LocalTime) formatter.parse(text);
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    public int getNano() {
        return nano;
    }

    public int toSecondOfDay() {
        return hour * 3600 + minute * 60 + second;
    }

    public long toNanoOfDay() {
        return toSecondOfDay() * DateTimeSupport.NANOS_PER_SECOND + nano;
    }

    public LocalTime plusHours(long hoursToAdd) {
        return ofNanoOfDay(DateTimeSupport.floorMod(toNanoOfDay() + hoursToAdd * 3600L * DateTimeSupport.NANOS_PER_SECOND,
                DateTimeSupport.NANOS_PER_DAY));
    }

    public LocalTime plusMinutes(long minutesToAdd) {
        return ofNanoOfDay(DateTimeSupport.floorMod(toNanoOfDay() + minutesToAdd * 60L * DateTimeSupport.NANOS_PER_SECOND,
                DateTimeSupport.NANOS_PER_DAY));
    }

    public LocalTime plusSeconds(long secondsToAdd) {
        return ofNanoOfDay(DateTimeSupport.floorMod(toNanoOfDay() + secondsToAdd * DateTimeSupport.NANOS_PER_SECOND,
                DateTimeSupport.NANOS_PER_DAY));
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    public int compareTo(LocalTime other) {
        if (hour != other.hour) {
            return hour < other.hour ? -1 : 1;
        }
        if (minute != other.minute) {
            return minute < other.minute ? -1 : 1;
        }
        if (second != other.second) {
            return second < other.second ? -1 : 1;
        }
        return nano < other.nano ? -1 : nano > other.nano ? 1 : 0;
    }

    public boolean equals(Object obj) {
        return obj instanceof LocalTime && compareTo((LocalTime) obj) == 0;
    }

    public int hashCode() {
        return hour * 3600 + minute * 60 + second + nano;
    }

    public String toString() {
        return DateTimeFormatter.ISO_LOCAL_TIME.format(this);
    }
}
