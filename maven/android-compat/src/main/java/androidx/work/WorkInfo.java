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
package androidx.work;

import java.util.Set;
import java.util.UUID;

/// A snapshot of one request's state.
public final class WorkInfo {

    public enum State {
        ENQUEUED, RUNNING, SUCCEEDED, FAILED, BLOCKED, CANCELLED;

        public boolean isFinished() {
            return this == SUCCEEDED || this == FAILED || this == CANCELLED;
        }
    }

    public static final int STOP_REASON_NOT_STOPPED = -256;

    private final UUID id;
    private final State state;
    private final Set<String> tags;
    private final Data outputData;
    private final Data progress;
    private final int runAttemptCount;
    private final int generation;
    private final Constraints constraints;
    private final long nextScheduleTimeMillis;
    private final long intervalMillis;
    private final long flexMillis;

    WorkInfo(UUID id, State state, Set<String> tags, Data outputData, Data progress, int runAttemptCount,
            int generation, Constraints constraints, long nextScheduleTimeMillis, long intervalMillis,
            long flexMillis) {
        this.id = id;
        this.state = state;
        this.tags = tags;
        this.outputData = outputData;
        this.progress = progress;
        this.runAttemptCount = runAttemptCount;
        this.generation = generation;
        this.constraints = constraints;
        this.nextScheduleTimeMillis = nextScheduleTimeMillis;
        this.intervalMillis = intervalMillis;
        this.flexMillis = flexMillis;
    }

    public UUID getId() {
        return id;
    }

    public State getState() {
        return state;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Data getOutputData() {
        return outputData;
    }

    public Data getProgress() {
        return progress;
    }

    public int getRunAttemptCount() {
        return runAttemptCount;
    }

    public int getGeneration() {
        return generation;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public long getNextScheduleTimeMillis() {
        return state == State.ENQUEUED ? nextScheduleTimeMillis : Long.MAX_VALUE;
    }

    /// The repeat interval of a periodic request, or null for one-time work.
    public PeriodicityInfo getPeriodicityInfo() {
        return intervalMillis > 0 ? new PeriodicityInfo(intervalMillis, flexMillis) : null;
    }

    public static final class PeriodicityInfo {
        private final long repeatIntervalMillis;
        private final long flexIntervalMillis;

        public PeriodicityInfo(long repeatIntervalMillis, long flexIntervalMillis) {
            this.repeatIntervalMillis = repeatIntervalMillis;
            this.flexIntervalMillis = flexIntervalMillis;
        }

        public long getRepeatIntervalMillis() {
            return repeatIntervalMillis;
        }

        public long getFlexIntervalMillis() {
            return flexIntervalMillis;
        }
    }

    public int getStopReason() {
        return STOP_REASON_NOT_STOPPED;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WorkInfo)) {
            return false;
        }
        WorkInfo w = (WorkInfo) o;
        return id.equals(w.id) && state == w.state && runAttemptCount == w.runAttemptCount
                && generation == w.generation && tags.equals(w.tags) && outputData.equals(w.outputData)
                && progress.equals(w.progress);
    }

    @Override
    public int hashCode() {
        return id.hashCode() * 31 + state.hashCode();
    }

    @Override
    public String toString() {
        return "WorkInfo{id='" + id + "', state=" + state + ", tags=" + tags + "}";
    }
}
