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

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;

/// A moment on the time line, in seconds and nanoseconds from
/// 1970-01-01T00:00:00Z.
public final class Instant implements Temporal, Comparable<Instant> {
    private static final long MIN_SECOND = -31557014167219200L;
    private static final long MAX_SECOND = 31556889864403199L;

    /// 1970-01-01T00:00:00Z.
    public static final Instant EPOCH = new Instant(0L, 0);

    /// The earliest instant, -1000000000-01-01T00:00Z.
    public static final Instant MIN = new Instant(MIN_SECOND, 0);

    /// The latest instant, 1000000000-12-31T23:59:59.999999999Z.
    public static final Instant MAX = new Instant(MAX_SECOND, 999999999);

    private final long epochSecond;
    private final int nano;

    private Instant(long epochSecond, int nano) {
        this.epochSecond = epochSecond;
        this.nano = nano;
    }

    private static Instant create(long seconds, int nanoOfSecond) {
        if ((seconds | nanoOfSecond) == 0) {
            return EPOCH;
        }
        if (seconds < MIN_SECOND || seconds > MAX_SECOND) {
            throw TimeArithmetic.instantOutOfRange();
        }
        return new Instant(seconds, nanoOfSecond);
    }

    public static Instant now() {
        return ofEpochMilli(System.currentTimeMillis());
    }

    /// The moment now on `clock`.
    public static Instant now(Clock clock) {
        return clock.instant();
    }

    public static Instant ofEpochMilli(long epochMilli) {
        long secs = TimeArithmetic.floorDiv(epochMilli, 1000L);
        int mos = (int) TimeArithmetic.floorMod(epochMilli, 1000L);
        return create(secs, mos * 1000000);
    }

    public static Instant ofEpochSecond(long epochSecond) {
        return create(epochSecond, 0);
    }

    public static Instant ofEpochSecond(long epochSecond, long nanoAdjustment) {
        long secs = TimeArithmetic.addExact(epochSecond,
                TimeArithmetic.floorDiv(nanoAdjustment, TimeArithmetic.NANOS_PER_SECOND));
        int nos = (int) TimeArithmetic.floorMod(nanoAdjustment, TimeArithmetic.NANOS_PER_SECOND);
        return create(secs, nos);
    }

    /// The moment `temporal` is.
    public static Instant from(TemporalAccessor temporal) {
        if (temporal instanceof Instant) {
            return (Instant) temporal;
        }
        if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toInstant();
        }
        if (temporal instanceof OffsetDateTime) {
            return ((OffsetDateTime) temporal).toInstant();
        }
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        throw new DateTimeException("Unable to obtain Instant from TemporalAccessor: " + temporal
                + " of type " + temporal.getClass().getName());
    }

    public static Instant parse(CharSequence text) {
        TemporalAccessor parsed = DateTimeFormatter.ISO_INSTANT.parse(text);
        try {
            return from(parsed);
        } catch (DateTimeException ex) {
            throw TimeArithmetic.unparsable(text, ex);
        }
    }

    public long getEpochSecond() {
        return epochSecond;
    }

    public int getNano() {
        return nano;
    }

    /// The milliseconds since the epoch, or `ArithmeticException` past what
    /// a long holds.
    public long toEpochMilli() {
        if (epochSecond < 0 && nano > 0) {
            long millis = TimeArithmetic.multiplyExact(epochSecond + 1, 1000L);
            long adjustment = nano / 1000000 - 1000;
            return TimeArithmetic.addExact(millis, adjustment);
        }
        long millis = TimeArithmetic.multiplyExact(epochSecond, 1000L);
        return TimeArithmetic.addExact(millis, nano / 1000000);
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isTimeBased() || unit == ChronoUnit.DAYS;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public Instant plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            ChronoUnit u = (ChronoUnit) unit;
            if (u == ChronoUnit.NANOS) {
                return plusNanos(amountToAdd);
            }
            if (u == ChronoUnit.MICROS) {
                return plus(amountToAdd / 1000000, (amountToAdd % 1000000) * 1000);
            }
            if (u == ChronoUnit.MILLIS) {
                return plusMillis(amountToAdd);
            }
            if (u == ChronoUnit.SECONDS) {
                return plusSeconds(amountToAdd);
            }
            if (u == ChronoUnit.MINUTES || u == ChronoUnit.HOURS || u == ChronoUnit.HALF_DAYS
                    || u == ChronoUnit.DAYS) {
                return plusSeconds(TimeArithmetic.multiplyExact(amountToAdd, TimeArithmetic.seconds(u)));
            }
            throw TimeArithmetic.unsupported(unit);
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return unit.addTo(this, amountToAdd);
    }

    @Override
    public Instant minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit);
    }

    @Override
    public Instant plus(TemporalAmount amount) {
        return (Instant) amount.addTo(this);
    }

    @Override
    public Instant minus(TemporalAmount amount) {
        return (Instant) amount.subtractFrom(this);
    }

    public Instant plusSeconds(long secondsToAdd) {
        return plus(secondsToAdd, 0);
    }

    public Instant plusMillis(long millisToAdd) {
        return plus(millisToAdd / 1000, (millisToAdd % 1000) * 1000000);
    }

    public Instant plusNanos(long nanosToAdd) {
        return plus(0, nanosToAdd);
    }

    private Instant plus(long secondsToAdd, long nanosToAdd) {
        if ((secondsToAdd | nanosToAdd) == 0) {
            return this;
        }
        long epochSec = TimeArithmetic.addExact(epochSecond, secondsToAdd);
        epochSec = TimeArithmetic.addExact(epochSec, nanosToAdd / TimeArithmetic.NANOS_PER_SECOND);
        nanosToAdd = nanosToAdd % TimeArithmetic.NANOS_PER_SECOND;
        long nanoAdjustment = nano + nanosToAdd;
        return ofEpochSecond(epochSec, nanoAdjustment);
    }

    public Instant minusSeconds(long secondsToSubtract) {
        if (secondsToSubtract == Long.MIN_VALUE) {
            return plusSeconds(Long.MAX_VALUE).plusSeconds(1);
        }
        return plusSeconds(-secondsToSubtract);
    }

    public Instant minusMillis(long millisToSubtract) {
        if (millisToSubtract == Long.MIN_VALUE) {
            return plusMillis(Long.MAX_VALUE).plusMillis(1);
        }
        return plusMillis(-millisToSubtract);
    }

    public Instant minusNanos(long nanosToSubtract) {
        if (nanosToSubtract == Long.MIN_VALUE) {
            return plusNanos(Long.MAX_VALUE).plusNanos(1);
        }
        return plusNanos(-nanosToSubtract);
    }

    /// This moment with everything smaller than `unit` dropped. The unit has
    /// to divide a day.
    public Instant truncatedTo(TemporalUnit unit) {
        ChronoUnit u = TimeArithmetic.chrono(unit);
        if (u == ChronoUnit.NANOS) {
            return this;
        }
        if (u.compareTo(ChronoUnit.DAYS) > 0) {
            throw new java.time.temporal.UnsupportedTemporalTypeException(
                    "Unit is too large to be used for truncation");
        }
        long dur = TimeArithmetic.seconds(u) * TimeArithmetic.NANOS_PER_SECOND + TimeArithmetic.nanos(u);
        long nod = (epochSecond % TimeArithmetic.SECONDS_PER_DAY) * TimeArithmetic.NANOS_PER_SECOND + nano;
        long result = TimeArithmetic.floorDiv(nod, dur) * dur;
        return plusNanos(result - nod);
    }

    /// The whole units of `unit` from this moment to `endExclusive`.
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        Instant end = from(endExclusive);
        if (unit instanceof ChronoUnit) {
            ChronoUnit u = (ChronoUnit) unit;
            if (u.compareTo(ChronoUnit.DAYS) > 0) {
                throw TimeArithmetic.unsupported(unit);
            }
            if (u == ChronoUnit.NANOS || u == ChronoUnit.MICROS) {
                // Counted in nanoseconds first, as the JDK does: more than
                // 292 years apart is an overflow in either unit.
                long secsDiff = TimeArithmetic.subtractExact(end.epochSecond, epochSecond);
                long nanos = TimeArithmetic.addExact(TimeArithmetic.multiplyExact(secsDiff,
                        TimeArithmetic.NANOS_PER_SECOND), end.nano - nano);
                return u == ChronoUnit.NANOS ? nanos : nanos / 1000;
            }
            if (u == ChronoUnit.MILLIS) {
                // Between the two moments each cut to its millisecond.
                return TimeArithmetic.subtractExact(end.toEpochMilli(), toEpochMilli());
            }
            return TimeArithmetic.timeUntil(epochSecond, nano, end.epochSecond, end.nano, u);
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return unit.between(this, end);
    }

    /// This moment in `zone`.
    public ZonedDateTime atZone(ZoneId zone) {
        return ZonedDateTime.ofInstant(this, zone);
    }

    /// This moment at `offset`.
    public OffsetDateTime atOffset(ZoneOffset offset) {
        return OffsetDateTime.ofInstant(this, offset);
    }

    public boolean isAfter(Instant otherInstant) {
        return compareTo(otherInstant) > 0;
    }

    public boolean isBefore(Instant otherInstant) {
        return compareTo(otherInstant) < 0;
    }

    @Override
    public int compareTo(Instant other) {
        if (epochSecond != other.epochSecond) {
            return epochSecond < other.epochSecond ? -1 : 1;
        }
        return nano - other.nano;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Instant)) {
            return false;
        }
        Instant other = (Instant) obj;
        return epochSecond == other.epochSecond && nano == other.nano;
    }

    @Override
    public int hashCode() {
        return ((int) (epochSecond ^ (epochSecond >>> 32))) + 51 * nano;
    }

    @Override
    public String toString() {
        return DateTimeFormatter.ISO_INSTANT.format(this);
    }
}
