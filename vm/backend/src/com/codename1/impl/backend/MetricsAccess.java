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
package com.codename1.impl.backend;

import com.codename1.backend.DataSource;
import com.codename1.backend.HttpServer;
import com.codename1.backend.metrics.Gauge;
import com.codename1.backend.metrics.Metrics;

/// The server's own hooks into `com.codename1.backend.metrics`: counting a
/// request, a job, the gauges of a running server. An application records with
/// [Metrics] and reads with its accessors; these are the calls only the runtime
/// makes, which is why they are package-private there and reached through here.
/// See [BackendAccess] for the pattern.
public abstract class MetricsAccess {
    private static MetricsAccess instance;

    /// Called once, by the implementation, from [Metrics]'s static initializer.
    public static synchronized void install(MetricsAccess access) {
        if (instance != null) {
            throw new IllegalStateException("MetricsAccess is already installed");
        }
        instance = access;
    }

    /// The implementation, initializing [Metrics] first if nothing has yet.
    public static MetricsAccess get() {
        MetricsAccess a = instance;
        if (a == null) {
            // A static call initializes the class; startTimeMillis() has no other effect.
            Metrics.startTimeMillis();
            a = instance;
            if (a == null) {
                throw new IllegalStateException("The metrics runtime did not install its access");
            }
        }
        return a;
    }

    /// Starts reporting a server's own instruments: requests, connections, pool.
    public abstract void enableServer(HttpServer server, DataSource pool);

    /// Stops reporting a server's own instruments.
    public abstract void disableServer(HttpServer server, DataSource pool);

    /// Names the route template the current request matched, for its labels.
    public abstract void route(String template);

    /// A request began; the result is handed back to [#requestEnded].
    public abstract long requestStarted();

    /// A request [#requestStarted] counted has been answered.
    public abstract void requestEnded(long started, String method, int status);

    /// A scheduled job ran.
    public abstract void jobRan(String job, long millis, boolean failed);

    /// Adds one server's contribution to a gauge several servers may share.
    public abstract void addSource(String name, String description, String unit,
                                   Gauge.Source source);

    /// Removes a contribution [#addSource] made.
    public abstract void removeSource(String name, Gauge.Source source);
}
