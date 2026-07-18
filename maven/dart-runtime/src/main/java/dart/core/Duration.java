package dart.core;

/**
 * Dart's Duration: an immutable span of time stored in microseconds.
 */
public final class Duration implements Comparable<Duration> {

    public static final long microsecondsPerMillisecond = 1000;
    public static final long microsecondsPerSecond = 1000000;
    public static final long microsecondsPerMinute = 60 * microsecondsPerSecond;
    public static final long microsecondsPerHour = 60 * microsecondsPerMinute;
    public static final long microsecondsPerDay = 24 * microsecondsPerHour;

    public static final Duration zero = new Duration(0);

    private final long micros;

    private Duration(long micros) {
        this.micros = micros;
    }

    public static Duration ofMicroseconds(long micros) {
        return new Duration(micros);
    }

    /** Canonical constructor mirroring Dart's named parameters, in declared order. */
    public static Duration of(long days, long hours, long minutes, long seconds, long milliseconds, long microseconds) {
        return new Duration(days * microsecondsPerDay
                + hours * microsecondsPerHour
                + minutes * microsecondsPerMinute
                + seconds * microsecondsPerSecond
                + milliseconds * microsecondsPerMillisecond
                + microseconds);
    }

    public long inMicroseconds() {
        return micros;
    }

    public long inMilliseconds() {
        return micros / microsecondsPerMillisecond;
    }

    public long inSeconds() {
        return micros / microsecondsPerSecond;
    }

    public long inMinutes() {
        return micros / microsecondsPerMinute;
    }

    public long inHours() {
        return micros / microsecondsPerHour;
    }

    public long inDays() {
        return micros / microsecondsPerDay;
    }

    public Duration plus(Duration other) {
        return new Duration(micros + other.micros);
    }

    public Duration minus(Duration other) {
        return new Duration(micros - other.micros);
    }

    public Duration times(long factor) {
        return new Duration(micros * factor);
    }

    public boolean isNegative() {
        return micros < 0;
    }

    public Duration abs() {
        return micros < 0 ? new Duration(-micros) : this;
    }

    @Override
    public int compareTo(Duration other) {
        return Long.compare(micros, other.micros);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Duration d && d.micros == micros;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(micros);
    }

    @Override
    public String toString() {
        long us = micros;
        String sign = "";
        if (us < 0) {
            sign = "-";
            us = -us;
        }
        long hours = us / microsecondsPerHour;
        long minutes = (us % microsecondsPerHour) / microsecondsPerMinute;
        long seconds = (us % microsecondsPerMinute) / microsecondsPerSecond;
        long microsRem = us % microsecondsPerSecond;
        return sign + hours + ":" + pad2(minutes) + ":" + pad2(seconds) + "." + pad6(microsRem);
    }

    private static String pad2(long v) {
        return v < 10 ? "0" + v : Long.toString(v);
    }

    private static String pad6(long v) {
        StringBuilder sb = new StringBuilder(Long.toString(v));
        while (sb.length() < 6) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }
}
