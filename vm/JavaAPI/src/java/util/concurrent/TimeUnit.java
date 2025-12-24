package java.util.concurrent;

public enum TimeUnit {
    NANOSECONDS(0),
    MICROSECONDS(1),
    MILLISECONDS(2),
    SECONDS(3),
    MINUTES(4),
    HOURS(5),
    DAYS(6);

    private final int index;
    TimeUnit(int i) { index = i; }

    static final long C0 = 1L;
    static final long C1 = C0 * 1000L;
    static final long C2 = C1 * 1000L;
    static final long C3 = C2 * 1000L;
    static final long C4 = C3 * 60L;
    static final long C5 = C4 * 60L;
    static final long C6 = C5 * 24L;

    static final long MAX = Long.MAX_VALUE;

    static long x(long d, long m, long over) {
        if (d >  over) return Long.MAX_VALUE;
        if (d < -over) return Long.MIN_VALUE;
        return d * m;
    }

    public long convert(long sourceDuration, TimeUnit sourceUnit) {
        switch(this) {
            case NANOSECONDS: return sourceUnit.toNanos(sourceDuration);
            case MICROSECONDS: return sourceUnit.toMicros(sourceDuration);
            case MILLISECONDS: return sourceUnit.toMillis(sourceDuration);
            case SECONDS: return sourceUnit.toSeconds(sourceDuration);
            case MINUTES: return sourceUnit.toMinutes(sourceDuration);
            case HOURS: return sourceUnit.toHours(sourceDuration);
            case DAYS: return sourceUnit.toDays(sourceDuration);
            default: throw new RuntimeException("Unknown unit");
        }
    }

    public long toNanos(long d) {
        if (this == NANOSECONDS) return d;
        if (this == MICROSECONDS) return x(d, C1/C0, MAX/(C1/C0));
        if (this == MILLISECONDS) return x(d, C2/C0, MAX/(C2/C0));
        if (this == SECONDS) return x(d, C3/C0, MAX/(C3/C0));
        if (this == MINUTES) return x(d, C4/C0, MAX/(C4/C0));
        if (this == HOURS) return x(d, C5/C0, MAX/(C5/C0));
        return x(d, C6/C0, MAX/(C6/C0));
    }

    public long toMicros(long d) {
        if (this == NANOSECONDS) return d / (C1/C0);
        if (this == MICROSECONDS) return d;
        if (this == MILLISECONDS) return x(d, C2/C1, MAX/(C2/C1));
        if (this == SECONDS) return x(d, C3/C1, MAX/(C3/C1));
        if (this == MINUTES) return x(d, C4/C1, MAX/(C4/C1));
        if (this == HOURS) return x(d, C5/C1, MAX/(C5/C1));
        return x(d, C6/C1, MAX/(C6/C1));
    }

    public long toMillis(long d) {
        if (this == NANOSECONDS) return d / (C2/C0);
        if (this == MICROSECONDS) return d / (C2/C1);
        if (this == MILLISECONDS) return d;
        if (this == SECONDS) return x(d, C3/C2, MAX/(C3/C2));
        if (this == MINUTES) return x(d, C4/C2, MAX/(C4/C2));
        if (this == HOURS) return x(d, C5/C2, MAX/(C5/C2));
        return x(d, C6/C2, MAX/(C6/C2));
    }

    public long toSeconds(long d) {
        if (this == NANOSECONDS) return d / (C3/C0);
        if (this == MICROSECONDS) return d / (C3/C1);
        if (this == MILLISECONDS) return d / (C3/C2);
        if (this == SECONDS) return d;
        if (this == MINUTES) return x(d, C4/C3, MAX/(C4/C3));
        if (this == HOURS) return x(d, C5/C3, MAX/(C5/C3));
        return x(d, C6/C3, MAX/(C6/C3));
    }

    public long toMinutes(long d) {
        if (this == NANOSECONDS) return d / (C4/C0);
        if (this == MICROSECONDS) return d / (C4/C1);
        if (this == MILLISECONDS) return d / (C4/C2);
        if (this == SECONDS) return d / (C4/C3);
        if (this == MINUTES) return d;
        if (this == HOURS) return x(d, C5/C4, MAX/(C5/C4));
        return x(d, C6/C4, MAX/(C6/C4));
    }

    public long toHours(long d) {
        if (this == NANOSECONDS) return d / (C5/C0);
        if (this == MICROSECONDS) return d / (C5/C1);
        if (this == MILLISECONDS) return d / (C5/C2);
        if (this == SECONDS) return d / (C5/C3);
        if (this == MINUTES) return d / (C5/C4);
        if (this == HOURS) return d;
        return x(d, C6/C5, MAX/(C6/C5));
    }

    public long toDays(long d) {
        if (this == NANOSECONDS) return d / (C6/C0);
        if (this == MICROSECONDS) return d / (C6/C1);
        if (this == MILLISECONDS) return d / (C6/C2);
        if (this == SECONDS) return d / (C6/C3);
        if (this == MINUTES) return d / (C6/C4);
        if (this == HOURS) return d / (C6/C5);
        return d;
    }

    private int excessNanos(long d, long m) {
        if (this == NANOSECONDS) return (int)(d - (m*C2));
        if (this == MICROSECONDS) return (int)((d*C1) - (m*C2));
        return 0;
    }

    public void timedWait(Object obj, long timeout) throws InterruptedException {
        if (timeout > 0) {
            long ms = toMillis(timeout);
            int ns = excessNanos(timeout, ms);
            obj.wait(ms, ns);
        }
    }

    public void timedJoin(Thread thread, long timeout) throws InterruptedException {
        if (timeout > 0) {
            long ms = toMillis(timeout);
            int ns = excessNanos(timeout, ms);
            thread.join(ms, ns);
        }
    }

    public void sleep(long timeout) throws InterruptedException {
        if (timeout > 0) {
            long ms = toMillis(timeout);
            int ns = excessNanos(timeout, ms);
            Thread.sleep(ms, ns);
        }
    }
}
