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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/// Everything this runtime knows about one request: what it was built with
/// and where it is now. Persisted by [WorkStore] while it is unfinished, so a
/// cold launch picks it up again.
final class WorkSpec {

    String id;
    String workerClassName;
    final Set<String> tags = new LinkedHashSet<String>();
    Data input = Data.EMPTY;
    Constraints constraints = Constraints.NONE;
    long initialDelayMillis;
    long intervalMillis;
    long flexMillis;
    BackoffPolicy backoffPolicy = BackoffPolicy.EXPONENTIAL;
    long backoffDelayMillis = WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS;
    boolean expedited;
    String uniqueName;
    final List<String> prerequisites = new ArrayList<String>();

    WorkInfo.State state = WorkInfo.State.ENQUEUED;
    int runAttemptCount;
    int generation;
    long nextRunAt;
    Data output = Data.EMPTY;
    Data progress = Data.EMPTY;

    boolean isPeriodic() {
        return intervalMillis > 0;
    }

    WorkSpec copy() {
        WorkSpec s = new WorkSpec();
        s.id = id;
        s.workerClassName = workerClassName;
        s.tags.addAll(tags);
        s.input = input;
        s.constraints = constraints;
        s.initialDelayMillis = initialDelayMillis;
        s.intervalMillis = intervalMillis;
        s.flexMillis = flexMillis;
        s.backoffPolicy = backoffPolicy;
        s.backoffDelayMillis = backoffDelayMillis;
        s.expedited = expedited;
        s.uniqueName = uniqueName;
        s.prerequisites.addAll(prerequisites);
        s.state = state;
        s.runAttemptCount = runAttemptCount;
        s.generation = generation;
        s.nextRunAt = nextRunAt;
        s.output = output;
        s.progress = progress;
        return s;
    }

    /// The delay before attempt `runAttemptCount + 1` after a retry, capped
    /// at Android's five hours.
    long backoffMillis() {
        long d;
        if (backoffPolicy == BackoffPolicy.LINEAR) {
            d = backoffDelayMillis * runAttemptCount;
        } else {
            int shift = Math.max(0, Math.min(30, runAttemptCount - 1));
            d = backoffDelayMillis << shift;
            if (d < 0) {
                d = WorkRequest.MAX_BACKOFF_MILLIS;
            }
        }
        return Math.min(WorkRequest.MAX_BACKOFF_MILLIS, d);
    }

    WorkInfo toInfo() {
        return new WorkInfo(java.util.UUID.fromString(id), state, new LinkedHashSet<String>(tags), output, progress,
                runAttemptCount, generation, constraints, nextRunAt, intervalMillis, flexMillis);
    }
}
