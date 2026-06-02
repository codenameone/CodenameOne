/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.background;

import java.util.HashMap;
import java.util.Map;

/// Describes a unit of constraint-aware background work to be scheduled with
/// `BackgroundWork#schedule(WorkRequest)`. The constraints map to Android WorkManager
/// constraints and iOS `BGTaskScheduler` request options.
///
/// Input data is a string-keyed string map so that it can survive serialization into the
/// platform scheduler and a subsequent cold launch of the app process.
///
/// Usage
/// ```java
/// WorkRequest req = WorkRequest.builder("sync", SyncWorker.class)
///         .setRequiresNetwork(true)
///         .setRequiresCharging(true)
///         .setPeriodic(6 * 60 * 60 * 1000L)
///         .putInputData("account", "primary")
///         .build();
/// BackgroundWork.schedule(req);
/// ```
///
/// #### See also
///
/// - BackgroundWork
///
/// - BackgroundWorker
public final class WorkRequest {

    private final String id;
    private final String workerClass;
    private final boolean requiresNetwork;
    private final boolean requiresUnmeteredNetwork;
    private final boolean requiresCharging;
    private final boolean requiresIdle;
    private final boolean requiresBatteryNotLow;
    private final boolean periodic;
    private final long minIntervalMillis;
    private final long initialDelayMillis;
    private final Map<String, String> inputData;

    private WorkRequest(Builder b) {
        this.id = b.id;
        this.workerClass = b.workerClass;
        this.requiresNetwork = b.requiresNetwork;
        this.requiresUnmeteredNetwork = b.requiresUnmeteredNetwork;
        this.requiresCharging = b.requiresCharging;
        this.requiresIdle = b.requiresIdle;
        this.requiresBatteryNotLow = b.requiresBatteryNotLow;
        this.periodic = b.periodic;
        this.minIntervalMillis = b.minIntervalMillis;
        this.initialDelayMillis = b.initialDelayMillis;
        this.inputData = new HashMap<String, String>(b.inputData);
    }

    /// Returns the unique work id.
    ///
    /// #### Returns
    ///
    /// the work id
    public String getId() {
        return id;
    }

    /// Returns the fully qualified class name of the `BackgroundWorker` that performs
    /// the work.
    ///
    /// #### Returns
    ///
    /// the worker class name
    public String getWorkerClass() {
        return workerClass;
    }

    /// Returns true if the work requires any network connection.
    ///
    /// #### Returns
    ///
    /// true if a network is required
    public boolean isRequiresNetwork() {
        return requiresNetwork;
    }

    /// Returns true if the work requires an unmetered (for example Wi-Fi) network.
    ///
    /// #### Returns
    ///
    /// true if an unmetered network is required
    public boolean isRequiresUnmeteredNetwork() {
        return requiresUnmeteredNetwork;
    }

    /// Returns true if the work requires the device to be charging.
    ///
    /// #### Returns
    ///
    /// true if charging is required
    public boolean isRequiresCharging() {
        return requiresCharging;
    }

    /// Returns true if the work requires the device to be idle (Android only).
    ///
    /// #### Returns
    ///
    /// true if device idle is required
    public boolean isRequiresIdle() {
        return requiresIdle;
    }

    /// Returns true if the work requires the battery to not be low (Android only).
    ///
    /// #### Returns
    ///
    /// true if battery-not-low is required
    public boolean isRequiresBatteryNotLow() {
        return requiresBatteryNotLow;
    }

    /// Returns true if the work repeats periodically.
    ///
    /// #### Returns
    ///
    /// true if periodic
    public boolean isPeriodic() {
        return periodic;
    }

    /// Returns the minimum interval between periodic executions in milliseconds, or 0 for
    /// one-shot work.
    ///
    /// #### Returns
    ///
    /// the minimum periodic interval in milliseconds
    public long getMinIntervalMillis() {
        return minIntervalMillis;
    }

    /// Returns the initial delay before the first execution in milliseconds.
    ///
    /// #### Returns
    ///
    /// the initial delay in milliseconds
    public long getInitialDelayMillis() {
        return initialDelayMillis;
    }

    /// Returns an immutable copy of the input data.
    ///
    /// #### Returns
    ///
    /// the input data, never null
    public Map<String, String> getInputData() {
        return new HashMap<String, String>(inputData);
    }

    /// Creates a new work request builder.
    ///
    /// #### Parameters
    ///
    /// - `id`: a stable unique id for the work; scheduling another request with the same id replaces it
    ///
    /// - `worker`: the worker class that performs the work; it must have a public no-arg constructor
    ///
    /// #### Returns
    ///
    /// a new builder
    public static Builder builder(String id, Class<? extends BackgroundWorker> worker) {
        return new Builder(id, worker.getName());
    }

    /// Builder for `WorkRequest`.
    public static final class Builder {
        private final String id;
        private final String workerClass;
        private boolean requiresNetwork;
        private boolean requiresUnmeteredNetwork;
        private boolean requiresCharging;
        private boolean requiresIdle;
        private boolean requiresBatteryNotLow;
        private boolean periodic;
        private long minIntervalMillis;
        private long initialDelayMillis;
        private final Map<String, String> inputData = new HashMap<String, String>();

        Builder(String id, String workerClass) {
            this.id = id;
            this.workerClass = workerClass;
        }

        /// Requires any network connection.
        ///
        /// #### Parameters
        ///
        /// - `b`: true to require a network
        ///
        /// #### Returns
        ///
        /// this builder for chaining
        public Builder setRequiresNetwork(boolean b) {
            this.requiresNetwork = b;
            return this;
        }

        /// Requires an unmetered network connection.
        ///
        /// #### Parameters
        ///
        /// - `b`: true to require an unmetered network
        ///
        /// #### Returns
        ///
        /// this builder for chaining
        public Builder setRequiresUnmeteredNetwork(boolean b) {
            this.requiresUnmeteredNetwork = b;
            return this;
        }

        /// Requires the device to be charging.
        ///
        /// #### Parameters
        ///
        /// - `b`: true to require charging
        ///
        /// #### Returns
        ///
        /// this builder for chaining
        public Builder setRequiresCharging(boolean b) {
            this.requiresCharging = b;
            return this;
        }

        /// Requires the device to be idle (Android only).
        ///
        /// #### Parameters
        ///
        /// - `b`: true to require device idle
        ///
        /// #### Returns
        ///
        /// this builder for chaining
        public Builder setRequiresIdle(boolean b) {
            this.requiresIdle = b;
            return this;
        }

        /// Requires the battery to not be low (Android only).
        ///
        /// #### Parameters
        ///
        /// - `b`: true to require battery-not-low
        ///
        /// #### Returns
        ///
        /// this builder for chaining
        public Builder setRequiresBatteryNotLow(boolean b) {
            this.requiresBatteryNotLow = b;
            return this;
        }

        /// Makes the work periodic with the given minimum interval. Note that platforms
        /// enforce a minimum period (for example 15 minutes on Android) and iOS only
        /// approximates periodic work by resubmission.
        ///
        /// #### Parameters
        ///
        /// - `minIntervalMillis`: the minimum interval between executions in milliseconds
        ///
        /// #### Returns
        ///
        /// this builder for chaining
        public Builder setPeriodic(long minIntervalMillis) {
            this.periodic = true;
            this.minIntervalMillis = minIntervalMillis;
            return this;
        }

        /// Sets an initial delay before the first execution.
        ///
        /// #### Parameters
        ///
        /// - `millis`: the delay in milliseconds
        ///
        /// #### Returns
        ///
        /// this builder for chaining
        public Builder setInitialDelay(long millis) {
            this.initialDelayMillis = millis;
            return this;
        }

        /// Adds a key-value pair to the input data passed to the worker.
        ///
        /// #### Parameters
        ///
        /// - `key`: the key
        ///
        /// - `value`: the value
        ///
        /// #### Returns
        ///
        /// this builder for chaining
        public Builder putInputData(String key, String value) {
            inputData.put(key, value);
            return this;
        }

        /// Builds the immutable work request.
        ///
        /// #### Returns
        ///
        /// the work request
        public WorkRequest build() {
            return new WorkRequest(this);
        }
    }
}
