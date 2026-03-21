package java.time;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public final class Instant implements Comparable<Instant>, TemporalAccessor {
    private final long epochSecond;
    private final int nano;

    private Instant(long epochSecond, int nano) {
        this.epochSecond = epochSecond;
        this.nano = nano;
    }

    public static Instant now() {
        return ofEpochMilli(System.currentTimeMillis());
    }

    public static Instant ofEpochMilli(long epochMilli) {
        long secs = DateTimeSupport.floorDiv(epochMilli, 1000L);
        int nanos = (int) (DateTimeSupport.floorMod(epochMilli, 1000L) * 1000000L);
        return new Instant(secs, nanos);
    }

    public static Instant ofEpochSecond(long epochSecond) {
        return new Instant(epochSecond, 0);
    }

    public static Instant ofEpochSecond(long epochSecond, long nanoAdjustment) {
        long secs = epochSecond + DateTimeSupport.floorDiv(nanoAdjustment, DateTimeSupport.NANOS_PER_SECOND);
        int nanos = (int) DateTimeSupport.floorMod(nanoAdjustment, DateTimeSupport.NANOS_PER_SECOND);
        return new Instant(secs, nanos);
    }

    public static Instant parse(CharSequence text) {
        return (Instant) DateTimeFormatter.ISO_INSTANT.parse(text);
    }

    public long getEpochSecond() {
        return epochSecond;
    }

    public int getNano() {
        return nano;
    }

    public long toEpochMilli() {
        return epochSecond * 1000L + nano / 1000000L;
    }

    public Instant plusSeconds(long secondsToAdd) {
        return ofEpochSecond(epochSecond + secondsToAdd, nano);
    }

    public Instant plusMillis(long millisToAdd) {
        return ofEpochMilli(toEpochMilli() + millisToAdd);
    }

    public Instant minusSeconds(long secondsToSubtract) {
        return plusSeconds(-secondsToSubtract);
    }

    public Instant minusMillis(long millisToSubtract) {
        return plusMillis(-millisToSubtract);
    }

    public int compareTo(Instant other) {
        if (epochSecond != other.epochSecond) {
            return epochSecond < other.epochSecond ? -1 : 1;
        }
        return nano < other.nano ? -1 : nano > other.nano ? 1 : 0;
    }

    public boolean equals(Object obj) {
        return obj instanceof Instant && compareTo((Instant) obj) == 0;
    }

    public int hashCode() {
        return (int) (epochSecond ^ (epochSecond >>> 32)) + nano * 51;
    }

    public String toString() {
        return DateTimeFormatter.ISO_INSTANT.format(this);
    }

    Instant toInstant() {
        return this;
    }

    ZoneId getZoneForFormatting() {
        return ZoneOffset.UTC;
    }
}
