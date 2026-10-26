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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/// What the runtime itself can report about the process, for the built-in
/// gauges: the Java SE arm's half, which asks the JVM's management beans.
///
/// Every reading answers NaN when the runtime has no such figure, and a gauge
/// whose reading is NaN reports no point at all. That is how a dashboard tells a
/// figure that is not measured here from one that is measured and zero; see
/// [com.codename1.backend.metrics.MetricSchema].
public final class RuntimeGauges {
    private RuntimeGauges() {
    }

    /// The runtime's name in [com.codename1.backend.metrics.MetricSchema]:
    /// `jvm` here, `native` in a packaged server.
    public static String runtime() {
        return "jvm";
    }

    /// The most heap the process may use, in bytes, or NaN when it is unbounded.
    public static double heapLimit() {
        long max = Runtime.getRuntime().maxMemory();
        return max == Long.MAX_VALUE || max <= 0 ? Double.NaN : max;
    }

    /// Live platform threads.
    public static double threadCount() {
        return ManagementFactory.getThreadMXBean().getThreadCount();
    }

    /// Classes currently loaded.
    public static double classCount() {
        return ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
    }

    /// Processors the runtime may schedule threads on.
    public static double cpuCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    /// Garbage collections since start, over every collector.
    public static double gcCount() {
        return gcSum(false);
    }

    /// Milliseconds spent collecting since start, over every collector.
    public static double gcTime() {
        return gcSum(true);
    }

    private static double gcSum(boolean time) {
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        double sum = 0;
        boolean any = false;
        for (GarbageCollectorMXBean bean : beans) {
            long v = time ? bean.getCollectionTime() : bean.getCollectionCount();
            // -1 is a collector that does not report the figure.
            if (v >= 0) {
                sum += v;
                any = true;
            }
        }
        return any ? sum : Double.NaN;
    }
}
