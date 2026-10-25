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

import java.util.concurrent.TimeUnit;

public final class PeriodicWorkRequest extends WorkRequest {

    public static final long MIN_PERIODIC_INTERVAL_MILLIS = 15 * 60 * 1000L;
    public static final long MIN_PERIODIC_FLEX_MILLIS = 5 * 60 * 1000L;

    PeriodicWorkRequest(WorkSpec spec) {
        super(spec);
    }

    public static final class Builder extends WorkRequest.Builder<Builder, PeriodicWorkRequest> {

        public Builder(Class<? extends ListenableWorker> workerClass, long repeatInterval, TimeUnit repeatIntervalTimeUnit) {
            super(workerClass);
            // Android raises a shorter interval to the minimum rather than refusing it.
            spec.intervalMillis = Math.max(MIN_PERIODIC_INTERVAL_MILLIS, repeatIntervalTimeUnit.toMillis(repeatInterval));
            spec.flexMillis = spec.intervalMillis;
        }

        public Builder(Class<? extends ListenableWorker> workerClass, long repeatInterval, TimeUnit repeatIntervalTimeUnit,
                long flexInterval, TimeUnit flexIntervalTimeUnit) {
            this(workerClass, repeatInterval, repeatIntervalTimeUnit);
            long flex = flexIntervalTimeUnit.toMillis(flexInterval);
            spec.flexMillis = Math.max(MIN_PERIODIC_FLEX_MILLIS, Math.min(spec.intervalMillis, flex));
        }

        @Override
        Builder thisObject() {
            return this;
        }

        @Override
        PeriodicWorkRequest buildInternal() {
            return new PeriodicWorkRequest(spec.copy());
        }
    }
}
