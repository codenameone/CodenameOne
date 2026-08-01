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
package com.codename1.health.sensors;

import com.codename1.health.HealthException;
import com.codename1.health.HealthSample;

/// Receives measurements and state changes from a [SensorSession]. All
/// callbacks arrive on the EDT.
public interface SensorSampleListener {

    /// A new measurement arrived, already decoded into a health sample in
    /// its canonical unit.
    ///
    /// A single notification can produce more than one sample -- a cycling
    /// power meter reports power and cadence together -- so this is called
    /// once per sample, not once per notification.
    void sensorSample(SensorSession session, HealthSample sample);

    /// The session changed state.
    void sensorStateChanged(SensorSession session, SensorSessionState state);

    /// An error occurred. The session may still recover if its state is
    /// [SensorSessionState#RECONNECTING].
    void sensorError(SensorSession session, HealthException error);
}
