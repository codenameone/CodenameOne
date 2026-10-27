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

/// What the runtime itself can report about the process: the packaged server's
/// half. ParparVM has no heap limit to report -- `Runtime.maxMemory()` answers
/// the total -- no platform thread or class registry, and a collector that runs
/// concurrently rather than in counted, timed pauses. Each of those reads NaN,
/// so its gauge is never registered and the figure is listed as unavailable,
/// never reported as zero; see [com.codename1.backend.metrics.MetricSchema].
public final class RuntimeGauges {
    private RuntimeGauges() {
    }

    public static String runtime() {
        return "native";
    }

    public static double heapLimit() {
        return Double.NaN;
    }

    public static double threadCount() {
        return Double.NaN;
    }

    public static double classCount() {
        return Double.NaN;
    }

    /// NaN rather than `availableProcessors()`, which answers a fixed 1 here.
    public static double cpuCount() {
        return Double.NaN;
    }

    public static double gcCount() {
        return Double.NaN;
    }

    public static double gcTime() {
        return Double.NaN;
    }
}
