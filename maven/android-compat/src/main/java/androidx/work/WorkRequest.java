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
import java.util.concurrent.TimeUnit;

public abstract class WorkRequest {

    public static final long DEFAULT_BACKOFF_DELAY_MILLIS = 30000L;
    public static final long MAX_BACKOFF_MILLIS = 5 * 60 * 60 * 1000L;
    public static final long MIN_BACKOFF_MILLIS = 10000L;

    final WorkSpec spec;

    WorkRequest(WorkSpec spec) {
        this.spec = spec;
    }

    public UUID getId() {
        return UUID.fromString(spec.id);
    }

    public String getStringId() {
        return spec.id;
    }

    public Set<String> getTags() {
        return java.util.Collections.unmodifiableSet(spec.tags);
    }

    public abstract static class Builder<B extends Builder<?, ?>, W extends WorkRequest> {

        WorkSpec spec = new WorkSpec();

        Builder(Class<? extends ListenableWorker> workerClass) {
            spec.id = UUID.randomUUID().toString();
            spec.workerClassName = workerClass.getName();
            // Android tags every request with its worker's class name.
            spec.tags.add(workerClass.getName());
        }

        abstract B thisObject();

        abstract W buildInternal();

        public final B setId(UUID id) {
            spec.id = id.toString();
            return thisObject();
        }

        public final B setBackoffCriteria(BackoffPolicy policy, long backoffDelay, TimeUnit timeUnit) {
            spec.backoffPolicy = policy;
            long ms = timeUnit.toMillis(backoffDelay);
            spec.backoffDelayMillis = Math.max(MIN_BACKOFF_MILLIS, Math.min(MAX_BACKOFF_MILLIS, ms));
            return thisObject();
        }

        public final B setConstraints(Constraints constraints) {
            spec.constraints = constraints;
            return thisObject();
        }

        public final B setInputData(Data inputData) {
            spec.input = inputData;
            return thisObject();
        }

        public final B addTag(String tag) {
            spec.tags.add(tag);
            return thisObject();
        }

        public final B keepResultsForAtLeast(long duration, TimeUnit timeUnit) {
            return thisObject();
        }

        public B setInitialDelay(long duration, TimeUnit timeUnit) {
            spec.initialDelayMillis = timeUnit.toMillis(duration);
            return thisObject();
        }

        /// Expedited work runs as soon as its constraints allow, which is
        /// already what this runtime does with all work.
        public B setExpedited(OutOfQuotaPolicy policy) {
            spec.expedited = true;
            return thisObject();
        }

        public final W build() {
            W w = buildInternal();
            // A builder may build more than one request; each gets its own id.
            WorkSpec next = spec.copy();
            next.id = UUID.randomUUID().toString();
            spec = next;
            return w;
        }
    }
}
