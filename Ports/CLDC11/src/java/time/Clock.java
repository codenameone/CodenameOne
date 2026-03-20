package java.time;

public abstract class Clock {
    public abstract ZoneId getZone();

    public abstract Instant instant();

    public long millis() {
        return instant().toEpochMilli();
    }

    public static Clock systemUTC() {
        return new SystemClock(ZoneOffset.UTC);
    }

    public static Clock systemDefaultZone() {
        return new SystemClock(ZoneId.systemDefault());
    }

    public static Clock fixed(Instant fixedInstant, ZoneId zone) {
        return new FixedClock(fixedInstant, zone);
    }

    private static final class SystemClock extends Clock {
        private final ZoneId zone;

        private SystemClock(ZoneId zone) {
            this.zone = zone;
        }

        public ZoneId getZone() {
            return zone;
        }

        public Instant instant() {
            return Instant.now();
        }
    }

    private static final class FixedClock extends Clock {
        private final Instant instant;
        private final ZoneId zone;

        private FixedClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        public ZoneId getZone() {
            return zone;
        }

        public Instant instant() {
            return instant;
        }
    }
}
