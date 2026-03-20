package java.time;

public final class Duration implements Comparable<Duration> {
    private final long seconds;
    private final int nanos;

    private Duration(long seconds, int nanos) {
        this.seconds = seconds;
        this.nanos = nanos;
    }

    public static Duration ofDays(long days) {
        return ofSeconds(days * 86400L);
    }

    public static Duration ofHours(long hours) {
        return ofSeconds(hours * 3600L);
    }

    public static Duration ofMinutes(long minutes) {
        return ofSeconds(minutes * 60L);
    }

    public static Duration ofSeconds(long seconds) {
        return new Duration(seconds, 0);
    }

    public static Duration ofSeconds(long seconds, long nanoAdjustment) {
        long secs = seconds + DateTimeSupport.floorDiv(nanoAdjustment, DateTimeSupport.NANOS_PER_SECOND);
        int nanos = (int) DateTimeSupport.floorMod(nanoAdjustment, DateTimeSupport.NANOS_PER_SECOND);
        return new Duration(secs, nanos);
    }

    public static Duration ofMillis(long millis) {
        return ofSeconds(DateTimeSupport.floorDiv(millis, 1000L), DateTimeSupport.floorMod(millis, 1000L) * 1000000L);
    }

    public long getSeconds() {
        return seconds;
    }

    public int getNano() {
        return nanos;
    }

    public long toMillis() {
        return seconds * 1000L + nanos / 1000000L;
    }

    public Duration plus(Duration other) {
        return ofSeconds(seconds + other.seconds, nanos + other.nanos);
    }

    public Duration minus(Duration other) {
        return ofSeconds(seconds - other.seconds, nanos - other.nanos);
    }

    public int compareTo(Duration other) {
        if (seconds != other.seconds) {
            return seconds < other.seconds ? -1 : 1;
        }
        return nanos < other.nanos ? -1 : nanos > other.nanos ? 1 : 0;
    }

    public boolean equals(Object obj) {
        return obj instanceof Duration && compareTo((Duration) obj) == 0;
    }

    public int hashCode() {
        return (int) (seconds ^ (seconds >>> 32)) + nanos * 31;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("PT");
        long absSeconds = Math.abs(seconds);
        if (seconds < 0 && absSeconds > 0) {
            sb.append('-');
        }
        sb.append(absSeconds);
        if (nanos != 0) {
            sb.append('.');
            String frac = String.valueOf(1000000000L + nanos).substring(1);
            while (frac.endsWith("0")) {
                frac = frac.substring(0, frac.length() - 1);
            }
            sb.append(frac);
        }
        sb.append('S');
        return sb.toString();
    }
}
