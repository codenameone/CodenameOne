/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.fieldline.scheduler;

/// A technician or inspector the solver can place jobs on.
///
/// Times are minutes from the start of the planning horizon. `availability`
/// holds `[start, end)` pairs, typically one per working day, from which
/// `timeOff` is subtracted when the problem is built. Coordinates are
/// microdegrees so the engine never needs floating point.
public final class Technician {
    final String id;
    final String name;
    final long skills;
    final int homeLat;
    final int homeLon;
    final int[] availability;
    final int[] timeOff;
    final int regularMinutes;
    final int maxMinutes;
    final int maxDriveMinutesPerShift;
    /// Certifications held, one bit each, and for every bit the last minute
    /// it is valid. A job needing a certification is only given on a shift
    /// that ends before it lapses.
    final long certs;
    final int[] certUntil;
    /// Territories the technician covers, one bit each; 0 covers everywhere.
    final long territories;
    /// The meal break: its length, and how long after the shift starts it may
    /// begin at the earliest and at the latest. 0 minutes means none.
    final int breakMinutes;
    final int breakAfter;
    final int breakBefore;
    final int maxJobsPerShift;
    /// Work plus driving in a single shift, the daily limit beside the weekly one.
    final int maxShiftMinutes;

    private Technician(Builder b) {
        id = b.id;
        name = b.name;
        skills = b.skills;
        homeLat = b.homeLat;
        homeLon = b.homeLon;
        availability = b.availability;
        timeOff = b.timeOff;
        regularMinutes = b.regularMinutes;
        maxMinutes = b.maxMinutes;
        maxDriveMinutesPerShift = b.maxDrive;
        certs = b.certs;
        certUntil = b.certUntil;
        territories = b.territories;
        breakMinutes = b.breakMinutes;
        breakAfter = b.breakAfter;
        breakBefore = b.breakBefore;
        maxJobsPerShift = b.maxJobs;
        maxShiftMinutes = b.maxShift;
    }

    /// Whether certification bit `bit` is held and still valid at `minute`.
    boolean certified(int bit, int minute) {
        return (certs & (1L << bit)) != 0 && certUntil[bit] >= minute;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSkills() {
        return skills;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    /// Fluent construction; every limit has a permissive default.
    public static final class Builder {
        private final String id;
        private String name;
        private long skills;
        private int homeLat;
        private int homeLon;
        private int[] availability = new int[0];
        private int[] timeOff = new int[0];
        private int regularMinutes = Integer.MAX_VALUE / 4;
        private int maxMinutes = Integer.MAX_VALUE / 4;
        private int maxDrive = Integer.MAX_VALUE / 4;
        private long certs;
        private final int[] certUntil = new int[64];
        private long territories;
        private int breakMinutes;
        private int breakAfter;
        private int breakBefore;
        private int maxJobs = Integer.MAX_VALUE / 4;
        private int maxShift = Integer.MAX_VALUE / 4;

        Builder(String id) {
            this.id = id;
            this.name = id;
        }

        public Builder name(String n) {
            name = n;
            return this;
        }

        public Builder skills(long mask) {
            skills = mask;
            return this;
        }

        public Builder home(int latMicro, int lonMicro) {
            homeLat = latMicro;
            homeLon = lonMicro;
            return this;
        }

        public Builder available(int start, int end) {
            availability = append(availability, start, end);
            return this;
        }

        public Builder timeOff(int start, int end) {
            timeOff = append(timeOff, start, end);
            return this;
        }

        /// Minutes of work before overtime is charged, and the hard ceiling.
        public Builder hours(int regular, int max) {
            regularMinutes = regular;
            maxMinutes = max;
            return this;
        }

        public Builder maxDrivePerShift(int minutes) {
            maxDrive = minutes;
            return this;
        }

        /// A certification (bit 0..63) valid until `untilMinute`; pass
        /// `Integer.MAX_VALUE` for one that does not lapse.
        public Builder certification(int bit, int untilMinute) {
            certs |= 1L << bit;
            certUntil[bit] = untilMinute;
            return this;
        }

        public Builder territories(long mask) {
            territories = mask;
            return this;
        }

        /// A break of `minutes` that starts between `after` and `before`
        /// minutes into each shift. Unpaid: it is not counted as work.
        public Builder lunch(int minutes, int after, int before) {
            breakMinutes = minutes;
            breakAfter = after;
            breakBefore = Math.max(after, before);
            return this;
        }

        public Builder maxJobsPerShift(int n) {
            maxJobs = n;
            return this;
        }

        public Builder maxShiftMinutes(int minutes) {
            maxShift = minutes;
            return this;
        }

        public Technician build() {
            return new Technician(this);
        }

        private static int[] append(int[] a, int x, int y) {
            int[] r = new int[a.length + 2];
            System.arraycopy(a, 0, r, 0, a.length);
            r[a.length] = x;
            r[a.length + 1] = y;
            return r;
        }
    }
}
