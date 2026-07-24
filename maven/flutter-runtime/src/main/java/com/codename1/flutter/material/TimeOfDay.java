package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;

import dart.core.DateTime;

/**
 * A wall-clock time — hour and minute, no date ({@code TimeOfDay} in Flutter).
 * new_gallery's picker demo builds one from a {@link DateTime}, compares
 * instances for equality and renders it via {@link #format(BuildContext)}.
 *
 * <p>Named {@code hour:}/{@code minute:} constructor parameters map to same-named
 * setters; {@link #fromDateTime(Object)} extracts the time-of-day from a
 * {@code DateTime}.</p>
 */
public class TimeOfDay {

    private int hour;
    private int minute;

    public TimeOfDay() {
    }

    // Named-parameter setters. Accept Dart's `int` (Java long) and narrow to the
    // small hour/minute range.
    public void hour(long v) {
        this.hour = (int) v;
    }

    public void minute(long v) {
        this.minute = (int) v;
    }

    public static TimeOfDay fromDateTime(Object time) {
        TimeOfDay t = new TimeOfDay();
        if (time instanceof DateTime) {
            DateTime dt = (DateTime) time;
            t.hour = (int) dt.hour();
            t.minute = (int) dt.minute();
        }
        return t;
    }

    public static TimeOfDay now() {
        return fromDateTime(DateTime.now());
    }

    public int hour() {
        return hour;
    }

    public int minute() {
        return minute;
    }

    /** Returns a copy with the supplied fields overridden (null keeps current). */
    public TimeOfDay replacing(Integer hour, Integer minute) {
        TimeOfDay t = new TimeOfDay();
        t.hour = hour != null ? hour.intValue() : this.hour;
        t.minute = minute != null ? minute.intValue() : this.minute;
        return t;
    }

    /** Formats using a 24-hour {@code HH:mm} pattern; localization is layered later. */
    public String format(BuildContext context) {
        return pad(hour) + ":" + pad(minute);
    }

    private static String pad(int v) {
        return v < 10 ? "0" + v : Integer.toString(v);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimeOfDay)) {
            return false;
        }
        TimeOfDay other = (TimeOfDay) o;
        return hour == other.hour && minute == other.minute;
    }

    @Override
    public int hashCode() {
        return hour * 60 + minute;
    }
}
