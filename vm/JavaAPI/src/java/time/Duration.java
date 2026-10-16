/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package java.time;

import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// An amount of time in seconds and nanoseconds, such as 34.5 seconds.
///
/// Arithmetic is exact: a result longer than a `Duration` holds throws
/// `ArithmeticException` rather than wrapping around, as the JDK's does.
public final class Duration implements TemporalAmount, Comparable<Duration> {
    /// A duration of zero.
    public static final Duration ZERO = new Duration(0L, 0);

    private static final long NANOS_PER_SECOND = TimeArithmetic.NANOS_PER_SECOND;

    private final long seconds;
    private final int nanos;

    private Duration(long seconds, int nanos) {
        this.seconds = seconds;
        this.nanos = nanos;
    }

    private static Duration create(long seconds, int nanos) {
        if ((seconds | nanos) == 0) {
            return ZERO;
        }
        return new Duration(seconds, nanos);
    }

    /// A duration of `days` standard 24 hour days.
    public static Duration ofDays(long days) {
        return create(TimeArithmetic.multiplyExact(days, 86400L), 0);
    }

    /// A duration of `hours` hours.
    public static Duration ofHours(long hours) {
        return create(TimeArithmetic.multiplyExact(hours, 3600L), 0);
    }

    /// A duration of `minutes` minutes.
    public static Duration ofMinutes(long minutes) {
        return create(TimeArithmetic.multiplyExact(minutes, 60L), 0);
    }

    /// A duration of `seconds` seconds.
    public static Duration ofSeconds(long seconds) {
        return create(seconds, 0);
    }

    /// A duration of `seconds` seconds adjusted by `nanoAdjustment`
    /// nanoseconds, which may take it across any number of seconds.
    public static Duration ofSeconds(long seconds, long nanoAdjustment) {
        long secs = TimeArithmetic.addExact(seconds, TimeArithmetic.floorDiv(nanoAdjustment, NANOS_PER_SECOND));
        int nos = (int) TimeArithmetic.floorMod(nanoAdjustment, NANOS_PER_SECOND);
        return create(secs, nos);
    }

    /// A duration of `millis` milliseconds.
    public static Duration ofMillis(long millis) {
        long secs = millis / 1000;
        int mos = (int) (millis % 1000);
        if (mos < 0) {
            mos += 1000;
            secs--;
        }
        return create(secs, mos * 1000000);
    }

    /// A duration of `nanos` nanoseconds.
    public static Duration ofNanos(long nanos) {
        long secs = nanos / NANOS_PER_SECOND;
        int nos = (int) (nanos % NANOS_PER_SECOND);
        if (nos < 0) {
            nos += (int) NANOS_PER_SECOND;
            secs--;
        }
        return create(secs, nos);
    }

    /// A duration of `amount` of `unit`. The unit must have an exact length:
    /// a day or anything shorter.
    public static Duration of(long amount, TemporalUnit unit) {
        return ZERO.plus(amount, unit);
    }

    /// The duration `amount` adds up to. Every unit it is made of must have
    /// an exact length.
    public static Duration from(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        Duration duration = ZERO;
        List<TemporalUnit> units = amount.getUnits();
        for (int i = 0; i < units.size(); i++) {
            TemporalUnit unit = units.get(i);
            duration = duration.plus(amount.get(unit), unit);
        }
        return duration;
    }

    /// The duration from one value to another of the same kind, negative
    /// when `endExclusive` comes first.
    public static Duration between(Temporal startInclusive, Temporal endExclusive) {
        try {
            return ofNanos(startInclusive.until(endExclusive, ChronoUnit.NANOS));
        } catch (DateTimeException ex) {
            return betweenInSeconds(startInclusive, endExclusive);
        } catch (ArithmeticException ex) {
            return betweenInSeconds(startInclusive, endExclusive);
        }
    }

    private static Duration betweenInSeconds(Temporal startInclusive, Temporal endExclusive) {
        long secs = startInclusive.until(endExclusive, ChronoUnit.SECONDS);
        long nanoDiff = TimeArithmetic.nanoOfSecond(endExclusive) - TimeArithmetic.nanoOfSecond(startInclusive);
        if (secs > 0 && nanoDiff < 0) {
            secs++;
        } else if (secs < 0 && nanoDiff > 0) {
            secs--;
        }
        return ofSeconds(secs, nanoDiff);
    }

    public static Duration parse(CharSequence text) {
        if (text == null) {
            throw new NullPointerException();
        }
        String value = text.toString();
        int length = value.length();
        int position = 0;
        int overallSign = 1;
        if (position < length && (value.charAt(position) == '+' || value.charAt(position) == '-')) {
            if (value.charAt(position++) == '-') {
                overallSign = -1;
            }
        }
        if (position >= length) {
            throw invalidDuration(value);
        }
        char prefix = value.charAt(position++);
        if (prefix != 'P' && prefix != 'p') {
            throw invalidDuration(value);
        }
        boolean time = false;
        boolean found = false;
        boolean foundTime = false;
        boolean foundDays = false;
        int lastTimeUnit = 0;
        long seconds = 0L;
        int nanos = 0;
        while (position < length) {
            if (value.charAt(position) == 'T' || value.charAt(position) == 't') {
                if (time) {
                    throw invalidDuration(value);
                }
                time = true;
                position++;
                continue;
            }
            int numberStart = position;
            if (value.charAt(position) == '+' || value.charAt(position) == '-') {
                position++;
            }
            int digitStart = position;
            while (position < length && Character.isDigit(value.charAt(position))) {
                position++;
            }
            boolean hasWholeDigits = position > digitStart;
            int fractionStart = -1;
            if (position < length && (value.charAt(position) == '.' || value.charAt(position) == ',')) {
                fractionStart = ++position;
                while (position < length && Character.isDigit(value.charAt(position))) {
                    position++;
                }
            }
            if (!hasWholeDigits || position >= length) {
                throw invalidDuration(value);
            }
            char unit = value.charAt(position++);
            if (unit >= 'a' && unit <= 'z') {
                unit = (char) (unit - ('a' - 'A'));
            }
            String wholeText = value.substring(numberStart, fractionStart < 0 ? position - 1 : fractionStart - 1);
            long amount;
            try {
                amount = Long.parseLong(wholeText);
            } catch (NumberFormatException ex) {
                throw invalidDuration(value);
            }
            if (unit == 'D' && !time && !foundDays && fractionStart < 0) {
                seconds = parsedSum(value, seconds, amount, 86400L);
                foundDays = true;
            } else if (unit == 'H' && time && lastTimeUnit < 1 && fractionStart < 0) {
                seconds = parsedSum(value, seconds, amount, 3600L);
                lastTimeUnit = 1;
                foundTime = true;
            } else if (unit == 'M' && time && lastTimeUnit < 2 && fractionStart < 0) {
                seconds = parsedSum(value, seconds, amount, 60L);
                lastTimeUnit = 2;
                foundTime = true;
            } else if (unit == 'S' && time && lastTimeUnit < 3) {
                seconds = parsedSum(value, seconds, amount, 1L);
                lastTimeUnit = 3;
                foundTime = true;
                if (fractionStart >= 0) {
                    String fraction = value.substring(fractionStart, position - 1);
                    if (fraction.length() == 0 || fraction.length() > 9) {
                        throw invalidDuration(value);
                    }
                    while (fraction.length() < 9) {
                        fraction += "0";
                    }
                    nanos = Integer.parseInt(fraction);
                    if (amount < 0 || wholeText.startsWith("-")) {
                        nanos = -nanos;
                    }
                }
            } else {
                throw invalidDuration(value);
            }
            found = true;
        }
        if (!found || (time && !foundTime)) {
            throw invalidDuration(value);
        }
        try {
            return ofSeconds(TimeArithmetic.multiplyExact(overallSign, seconds), overallSign * (long) nanos);
        } catch (ArithmeticException e) {
            throw invalidDuration(value);
        }
    }

    /// `seconds` plus `amount` units of `unitSeconds` each. A field too large
    /// for a duration is refused as the JDK refuses it, rather than wrapping
    /// around to an unrelated one.
    private static long parsedSum(String value, long seconds, long amount, long unitSeconds) {
        try {
            return TimeArithmetic.addExact(seconds, TimeArithmetic.multiplyExact(amount, unitSeconds));
        } catch (ArithmeticException e) {
            throw invalidDuration(value);
        }
    }

    private static DateTimeParseException invalidDuration(String value) {
        return new DateTimeParseException("Text cannot be parsed to a Duration", value, 0);
    }

    /// The amount of `unit` held: the seconds or the nanoseconds.
    @Override
    public long get(TemporalUnit unit) {
        if (unit == ChronoUnit.SECONDS) {
            return seconds;
        }
        if (unit == ChronoUnit.NANOS) {
            return nanos;
        }
        throw TimeArithmetic.unsupported(unit);
    }

    /// Seconds and nanoseconds, the two units a duration is made of.
    @Override
    public List<TemporalUnit> getUnits() {
        List<TemporalUnit> units = new ArrayList<TemporalUnit>(2);
        units.add(ChronoUnit.SECONDS);
        units.add(ChronoUnit.NANOS);
        return Collections.unmodifiableList(units);
    }

    @Override
    public Temporal addTo(Temporal temporal) {
        if (seconds != 0) {
            temporal = temporal.plus(seconds, ChronoUnit.SECONDS);
        }
        if (nanos != 0) {
            temporal = temporal.plus(nanos, ChronoUnit.NANOS);
        }
        return temporal;
    }

    @Override
    public Temporal subtractFrom(Temporal temporal) {
        if (seconds != 0) {
            temporal = temporal.minus(seconds, ChronoUnit.SECONDS);
        }
        if (nanos != 0) {
            temporal = temporal.minus(nanos, ChronoUnit.NANOS);
        }
        return temporal;
    }

    /// Whether this is zero long.
    public boolean isZero() {
        return (seconds | nanos) == 0;
    }

    /// Whether this is shorter than zero.
    public boolean isNegative() {
        return seconds < 0;
    }

    /// The whole seconds, rounded towards negative infinity.
    public long getSeconds() {
        return seconds;
    }

    /// The nanoseconds within the second, 0 to 999,999,999.
    public int getNano() {
        return nanos;
    }

    /// This duration with `seconds` whole seconds.
    public Duration withSeconds(long seconds) {
        return create(seconds, nanos);
    }

    /// This duration with `nanoOfSecond` nanoseconds within the second.
    public Duration withNanos(int nanoOfSecond) {
        if (nanoOfSecond < 0 || nanoOfSecond >= NANOS_PER_SECOND) {
            throw new DateTimeException("Invalid value for NanoOfSecond: " + nanoOfSecond);
        }
        return create(seconds, nanoOfSecond);
    }

    public Duration plus(Duration duration) {
        return plus(duration.getSeconds(), duration.getNano());
    }

    /// This duration plus `amountToAdd` of `unit`, which must have an exact
    /// length.
    public Duration plus(long amountToAdd, TemporalUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (unit == ChronoUnit.DAYS) {
            return plus(TimeArithmetic.multiplyExact(amountToAdd, 86400L), 0);
        }
        if (unit.isDurationEstimated()) {
            throw new java.time.temporal.UnsupportedTemporalTypeException(
                    "Unit must not have an estimated duration");
        }
        if (amountToAdd == 0) {
            return this;
        }
        if (unit instanceof ChronoUnit) {
            ChronoUnit u = (ChronoUnit) unit;
            if (u == ChronoUnit.NANOS) {
                return plusNanos(amountToAdd);
            }
            if (u == ChronoUnit.MICROS) {
                return plusSeconds((amountToAdd / (1000000L * 1000)) * 1000)
                        .plusNanos((amountToAdd % (1000000L * 1000)) * 1000);
            }
            if (u == ChronoUnit.MILLIS) {
                return plusMillis(amountToAdd);
            }
            if (u == ChronoUnit.SECONDS) {
                return plusSeconds(amountToAdd);
            }
            return plusSeconds(TimeArithmetic.multiplyExact(TimeArithmetic.seconds(u), amountToAdd));
        }
        Duration duration = unit.getDuration().multipliedBy(amountToAdd);
        return plusSeconds(duration.getSeconds()).plusNanos(duration.getNano());
    }

    public Duration plusDays(long daysToAdd) {
        return plus(TimeArithmetic.multiplyExact(daysToAdd, 86400L), 0);
    }

    public Duration plusHours(long hoursToAdd) {
        return plus(TimeArithmetic.multiplyExact(hoursToAdd, 3600L), 0);
    }

    public Duration plusMinutes(long minutesToAdd) {
        return plus(TimeArithmetic.multiplyExact(minutesToAdd, 60L), 0);
    }

    public Duration plusSeconds(long secondsToAdd) {
        return plus(secondsToAdd, 0);
    }

    public Duration plusMillis(long millisToAdd) {
        return plus(millisToAdd / 1000, (millisToAdd % 1000) * 1000000);
    }

    public Duration plusNanos(long nanosToAdd) {
        return plus(0, nanosToAdd);
    }

    private Duration plus(long secondsToAdd, long nanosToAdd) {
        if ((secondsToAdd | nanosToAdd) == 0) {
            return this;
        }
        long epochSec = TimeArithmetic.addExact(seconds, secondsToAdd);
        epochSec = TimeArithmetic.addExact(epochSec, nanosToAdd / NANOS_PER_SECOND);
        nanosToAdd = nanosToAdd % NANOS_PER_SECOND;
        long nanoAdjustment = nanos + nanosToAdd;
        return ofSeconds(epochSec, nanoAdjustment);
    }

    public Duration minus(Duration duration) {
        long secsToSubtract = duration.getSeconds();
        int nanosToSubtract = duration.getNano();
        if (secsToSubtract == Long.MIN_VALUE) {
            return plus(Long.MAX_VALUE, -nanosToSubtract).plus(1, 0);
        }
        return plus(-secsToSubtract, -nanosToSubtract);
    }

    public Duration minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit);
    }

    public Duration minusDays(long daysToSubtract) {
        return daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract);
    }

    public Duration minusHours(long hoursToSubtract) {
        return hoursToSubtract == Long.MIN_VALUE ? plusHours(Long.MAX_VALUE).plusHours(1)
                : plusHours(-hoursToSubtract);
    }

    public Duration minusMinutes(long minutesToSubtract) {
        return minutesToSubtract == Long.MIN_VALUE ? plusMinutes(Long.MAX_VALUE).plusMinutes(1)
                : plusMinutes(-minutesToSubtract);
    }

    public Duration minusSeconds(long secondsToSubtract) {
        return secondsToSubtract == Long.MIN_VALUE ? plusSeconds(Long.MAX_VALUE).plusSeconds(1)
                : plusSeconds(-secondsToSubtract);
    }

    public Duration minusMillis(long millisToSubtract) {
        return millisToSubtract == Long.MIN_VALUE ? plusMillis(Long.MAX_VALUE).plusMillis(1)
                : plusMillis(-millisToSubtract);
    }

    public Duration minusNanos(long nanosToSubtract) {
        return nanosToSubtract == Long.MIN_VALUE ? plusNanos(Long.MAX_VALUE).plusNanos(1)
                : plusNanos(-nanosToSubtract);
    }

    /// This duration multiplied by `multiplicand`, or `ArithmeticException`
    /// when the product is longer than a duration holds.
    public Duration multipliedBy(long multiplicand) {
        if (multiplicand == 1) {
            return this;
        }
        // A negative duration borrows a second first, so its seconds and
        // nanoseconds share a sign and neither partial product overflows while
        // the whole fits; the factor is split at a billion so the nanosecond
        // product fits a long.
        long secs = seconds;
        long nos = nanos;
        if (secs < 0 && nos > 0) {
            secs++;
            nos -= NANOS_PER_SECOND;
        }
        long high = TimeArithmetic.floorDiv(multiplicand, NANOS_PER_SECOND);
        long rest = nos * TimeArithmetic.floorMod(multiplicand, NANOS_PER_SECOND);
        // The nanosecond terms are summed first: together they are the carry
        // of nanos * factor, which is never larger than the factor, while the
        // first alone can push a result that fits past the range.
        long carry = TimeArithmetic.addExact(nos * high, TimeArithmetic.floorDiv(rest, NANOS_PER_SECOND));
        long total = TimeArithmetic.addExact(TimeArithmetic.multiplyExact(secs, multiplicand), carry);
        return ofSeconds(total, TimeArithmetic.floorMod(rest, NANOS_PER_SECOND));
    }

    /// This duration divided by `divisor`, the fraction of a nanosecond
    /// dropped towards zero. Exact for anything shorter than 292 years; a
    /// longer one throws `ArithmeticException`.
    public Duration dividedBy(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        if (divisor == 1) {
            return this;
        }
        return ofNanos(toNanos() / divisor);
    }

    /// This duration with its sign reversed. The most negative duration has
    /// no positive counterpart and throws `ArithmeticException`.
    public Duration negated() {
        return multipliedBy(-1);
    }

    /// This duration without its sign.
    public Duration abs() {
        return isNegative() ? negated() : this;
    }

    /// The whole days, of 24 hours.
    public long toDays() {
        return seconds / 86400L;
    }

    /// The whole hours.
    public long toHours() {
        return seconds / 3600L;
    }

    /// The whole minutes.
    public long toMinutes() {
        return seconds / 60L;
    }

    /// The whole seconds; the same as `getSeconds()`.
    public long toSeconds() {
        return seconds;
    }

    /// The whole milliseconds, or `ArithmeticException` past what a long
    /// holds.
    public long toMillis() {
        long tempSeconds = seconds;
        long tempNanos = nanos;
        if (tempSeconds < 0) {
            // A negative duration borrows a second first, so the most negative
            // value a long holds is reached without an intermediate overflow.
            tempSeconds = tempSeconds + 1;
            tempNanos = tempNanos - NANOS_PER_SECOND;
        }
        long millis = TimeArithmetic.multiplyExact(tempSeconds, 1000L);
        return TimeArithmetic.addExact(millis, tempNanos / 1000000L);
    }

    /// The whole nanoseconds, or `ArithmeticException` past what a long
    /// holds (about 292 years).
    public long toNanos() {
        long tempSeconds = seconds;
        long tempNanos = nanos;
        if (tempSeconds < 0) {
            tempSeconds = tempSeconds + 1;
            tempNanos = tempNanos - NANOS_PER_SECOND;
        }
        long totalNanos = TimeArithmetic.multiplyExact(tempSeconds, NANOS_PER_SECOND);
        return TimeArithmetic.addExact(totalNanos, tempNanos);
    }

    /// The whole days; the same as `toDays()`.
    public long toDaysPart() {
        return seconds / 86400L;
    }

    /// The hours left over after the whole days.
    public int toHoursPart() {
        return (int) (toHours() % 24);
    }

    /// The minutes left over after the whole hours.
    public int toMinutesPart() {
        return (int) (toMinutes() % 60);
    }

    /// The seconds left over after the whole minutes.
    public int toSecondsPart() {
        return (int) (seconds % 60);
    }

    /// The milliseconds within the second.
    public int toMillisPart() {
        return nanos / 1000000;
    }

    /// The nanoseconds within the second; the same as `getNano()`.
    public int toNanosPart() {
        return nanos;
    }

    @Override
    public int compareTo(Duration other) {
        if (seconds != other.seconds) {
            return seconds < other.seconds ? -1 : 1;
        }
        return nanos - other.nanos;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Duration)) {
            return false;
        }
        Duration other = (Duration) obj;
        return seconds == other.seconds && nanos == other.nanos;
    }

    @Override
    public int hashCode() {
        return ((int) (seconds ^ (seconds >>> 32))) + (51 * nanos);
    }

    public String toString() {
        long effectiveTotalSecs = seconds;
        if (seconds < 0L && nanos > 0) {
            effectiveTotalSecs++;
        }
        long hours = effectiveTotalSecs / 3600L;
        int minutes = (int) ((effectiveTotalSecs % 3600L) / 60L);
        int secs = (int) (effectiveTotalSecs % 60L);
        StringBuffer buf = new StringBuffer(24);
        buf.append("PT");
        if (hours != 0L) {
            buf.append(hours).append('H');
        }
        if (minutes != 0) {
            buf.append(minutes).append('M');
        }
        if (secs == 0 && nanos == 0 && buf.length() > 2) {
            return buf.toString();
        }
        if (seconds < 0L && nanos > 0) {
            if (secs == 0) {
                buf.append("-0");
            } else {
                buf.append(secs);
            }
        } else {
            buf.append(secs);
        }
        if (nanos > 0) {
            int pos = buf.length();
            if (seconds < 0L) {
                buf.append(2000000000L - nanos);
            } else {
                buf.append(nanos + 1000000000L);
            }
            while (buf.charAt(buf.length() - 1) == '0') {
                buf.setLength(buf.length() - 1);
            }
            buf.setCharAt(pos, '.');
        }
        buf.append('S');
        return buf.toString();
    }
}
