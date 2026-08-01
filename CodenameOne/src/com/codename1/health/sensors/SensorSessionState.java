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

/// The lifecycle of a [SensorSession].
public enum SensorSessionState {

    /// Connecting and discovering services.
    CONNECTING,

    /// Connected and subscribed; measurements are arriving.
    STREAMING,

    /// The link dropped and the session is trying to re-establish it.
    /// Reached only when [SensorSessionOptions#setAutoReconnect(boolean)]
    /// is on; the session recovers on its own and returns to
    /// [#STREAMING].
    ///
    /// Straps routinely drop out for a few seconds when a rider's chest
    /// moves the sensor, so treat this as a transient display state rather
    /// than an error.
    RECONNECTING,

    /// The session was stopped by the app.
    STOPPED,

    /// The session ended with an error and will not recover.
    FAILED
}
