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

public abstract class Clock {
    public abstract ZoneId getZone();

    public abstract Instant instant();

    /// This clock, reporting its instants in `zone`.
    public abstract Clock withZone(ZoneId zone);

    public long millis() {
        return instant().toEpochMilli();
    }

    public static Clock systemUTC() {
        return new SystemClock(ZoneOffset.UTC);
    }

    public static Clock systemDefaultZone() {
        return new SystemClock(ZoneId.systemDefault());
    }

    /// The system clock, seen in `zone`.
    public static Clock system(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return new SystemClock(zone);
    }

    public static Clock fixed(Instant fixedInstant, ZoneId zone) {
        return new FixedClock(fixedInstant, zone);
    }

    /// A clock that reads `base` moved on by `offsetDuration`.
    public static Clock offset(Clock base, Duration offsetDuration) {
        if (base == null) {
            throw new NullPointerException("baseClock");
        }
        if (offsetDuration == null) {
            throw new NullPointerException("offsetDuration");
        }
        if (offsetDuration.isZero()) {
            return base;
        }
        return new OffsetClock(base, offsetDuration);
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

        public Clock withZone(ZoneId zone) {
            return zone.equals(this.zone) ? this : new SystemClock(zone);
        }

        public boolean equals(Object obj) {
            return obj instanceof SystemClock && zone.equals(((SystemClock) obj).zone);
        }

        public int hashCode() {
            return zone.hashCode() + 1;
        }

        public String toString() {
            return "SystemClock[" + zone + "]";
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

        public Clock withZone(ZoneId zone) {
            return zone.equals(this.zone) ? this : new FixedClock(instant, zone);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof FixedClock)) {
                return false;
            }
            FixedClock other = (FixedClock) obj;
            return instant.equals(other.instant) && zone.equals(other.zone);
        }

        public int hashCode() {
            return instant.hashCode() ^ zone.hashCode();
        }

        public String toString() {
            return "FixedClock[" + instant + "," + zone + "]";
        }
    }

    private static final class OffsetClock extends Clock {
        private final Clock base;
        private final Duration offset;

        private OffsetClock(Clock base, Duration offset) {
            this.base = base;
            this.offset = offset;
        }

        public ZoneId getZone() {
            return base.getZone();
        }

        public Instant instant() {
            return base.instant().plus(offset);
        }

        public Clock withZone(ZoneId zone) {
            return zone.equals(base.getZone()) ? this : new OffsetClock(base.withZone(zone), offset);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof OffsetClock)) {
                return false;
            }
            OffsetClock other = (OffsetClock) obj;
            return base.equals(other.base) && offset.equals(other.offset);
        }

        public int hashCode() {
            return base.hashCode() ^ offset.hashCode();
        }

        public String toString() {
            return "OffsetClock[" + base + "," + offset + "]";
        }
    }
}
