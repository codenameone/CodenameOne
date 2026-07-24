package dart.core;

/**
 * Dart's {@code DateTimeRange}: an inclusive-start, inclusive-end pair of
 * {@link DateTime} instants used by the Material date-range picker.
 */
public final class DateTimeRange {

    private DateTime start;
    private DateTime end;

    /** Named-parameter constructor {@code DateTimeRange({start, end})}. */
    public DateTimeRange(DateTime start, DateTime end) {
        this.start = start;
        this.end = end;
    }

    public DateTime start() {
        return start;
    }

    public DateTime end() {
        return end;
    }

    public Duration duration() {
        return end.difference(start);
    }
}
