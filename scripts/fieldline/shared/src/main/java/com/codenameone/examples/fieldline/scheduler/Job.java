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

/// A visit to a site: a repair, a maintenance call or an inspection.
///
/// The job must *start* inside `[windowStart, windowEnd]`. A job that cannot be
/// placed stays unassigned and costs `priority` squared times the unassigned
/// weight, so an urgent job displaces routine ones rather than the reverse.
public final class Job {
    final String id;
    final int lat;
    final int lon;
    final int duration;
    final long requiredSkills;
    final int windowStart;
    final int windowEnd;
    final int priority;
    final String preferredTech;
    final String pinnedTech;
    final int pinnedStart;
    final String previousTech;
    /// Certifications the work needs (gas safety, high voltage...), one bit each.
    final long certs;
    /// The territory the site lies in, one bit; 0 for none.
    final long zone;
    /// The SLA: the minute the job should be finished by. Later is allowed and
    /// charged per minute, unlike the window, which is hard.
    final int due;
    /// Technicians the customer will not have on site.
    final String[] excluded;
    /// Jobs that must be finished before this one may start (a parts
    /// delivery, a survey before an install), by id.
    final String[] after;

    private Job(Builder b) {
        id = b.id;
        lat = b.lat;
        lon = b.lon;
        duration = b.duration;
        requiredSkills = b.skills;
        windowStart = b.windowStart;
        windowEnd = b.windowEnd;
        priority = b.priority;
        preferredTech = b.preferredTech;
        pinnedTech = b.pinnedTech;
        pinnedStart = b.pinnedStart;
        previousTech = b.previousTech;
        certs = b.certs;
        zone = b.zone;
        due = b.due;
        excluded = b.excluded;
        after = b.after;
    }

    /// The SLA deadline, or `Integer.MAX_VALUE / 4` when there is none.
    public int getDue() {
        return due;
    }

    public String getId() {
        return id;
    }

    public int getDuration() {
        return duration;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isPinned() {
        return pinnedTech != null;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    /// Returns a builder pre-filled with this job, for deriving a changed copy.
    public Builder toBuilder() {
        Builder b = new Builder(id);
        b.lat = lat;
        b.lon = lon;
        b.duration = duration;
        b.skills = requiredSkills;
        b.windowStart = windowStart;
        b.windowEnd = windowEnd;
        b.priority = priority;
        b.preferredTech = preferredTech;
        b.pinnedTech = pinnedTech;
        b.pinnedStart = pinnedStart;
        b.previousTech = previousTech;
        b.certs = certs;
        b.zone = zone;
        b.due = due;
        b.excluded = excluded;
        b.after = after;
        return b;
    }

    /// Fluent construction.
    public static final class Builder {
        private final String id;
        private int lat;
        private int lon;
        private int duration = 60;
        private long skills;
        private int windowStart;
        private int windowEnd = Integer.MAX_VALUE / 4;
        private int priority = 1;
        private String preferredTech;
        private String pinnedTech;
        private int pinnedStart = -1;
        private String previousTech;
        private long certs;
        private long zone;
        private int due = Integer.MAX_VALUE / 4;
        private String[] excluded = new String[0];
        private String[] after = new String[0];

        Builder(String id) {
            this.id = id;
        }

        public Builder at(int latMicro, int lonMicro) {
            lat = latMicro;
            lon = lonMicro;
            return this;
        }

        public Builder duration(int minutes) {
            duration = minutes;
            return this;
        }

        public Builder skills(long mask) {
            skills = mask;
            return this;
        }

        /// The earliest and latest start.
        public Builder window(int earliest, int latest) {
            windowStart = earliest;
            windowEnd = latest;
            return this;
        }

        /// 1 (routine) to 5 (emergency).
        public Builder priority(int p) {
            priority = p;
            return this;
        }

        /// Continuity: the technician who knows the site. A soft preference.
        public Builder preferredTech(String techId) {
            preferredTech = techId;
            return this;
        }

        /// Work already started or committed: this technician, at this start.
        /// A hard constraint; re-optimization never moves a pinned job.
        public Builder pinned(String techId, int start) {
            pinnedTech = techId;
            pinnedStart = start;
            return this;
        }

        /// Who held the job in the published schedule. Moving it elsewhere costs
        /// the disruption weight, which keeps a re-plan close to the old one.
        public Builder previousTech(String techId) {
            previousTech = techId;
            return this;
        }

        public Builder certifications(long mask) {
            certs = mask;
            return this;
        }

        public Builder zone(long mask) {
            zone = mask;
            return this;
        }

        /// Finish by `minute` or pay the lateness weight per minute late.
        public Builder due(int minute) {
            due = minute;
            return this;
        }

        /// A technician this job must never be given to.
        public Builder exclude(String techId) {
            excluded = grow(excluded, techId);
            return this;
        }

        /// A job that has to be finished before this one starts.
        public Builder after(String jobId) {
            after = grow(after, jobId);
            return this;
        }

        private static String[] grow(String[] a, String v) {
            String[] r = new String[a.length + 1];
            System.arraycopy(a, 0, r, 0, a.length);
            r[a.length] = v;
            return r;
        }

        public Job build() {
            return new Job(this);
        }
    }
}
