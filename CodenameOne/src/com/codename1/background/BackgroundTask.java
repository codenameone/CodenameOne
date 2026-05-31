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

import com.codename1.ui.Display;
import java.util.Date;

/// Schedules a one-shot deferrable processing task that the operating system runs at a
/// convenient time after an earliest-begin date. On iOS this maps to a
/// `BGProcessingTaskRequest`; on Android it maps to a one-shot WorkManager request with an
/// initial delay; in the simulator it runs on a timer.
///
/// The `Runnable` passed here runs in the foreground simulator and during a live app
/// session. On iOS, after the app process has been killed, the registered task identifier
/// is relaunched by the OS and the work is reconstructed from the persisted schedule; for
/// that cold-launch path prefer `BackgroundWork` with a `BackgroundWorker` class, which is
/// reconstructed via its no-arg constructor.
///
/// #### See also
///
/// - BackgroundWork
public class BackgroundTask {

    private BackgroundTask() {
    }

    /// Schedules a processing task.
    ///
    /// #### Parameters
    ///
    /// - `id`: a stable unique id for the task; it must be declared in the build hint listing permitted background task identifiers on iOS
    ///
    /// - `earliestBeginDate`: the earliest date at which the task may run, or null for as soon as convenient
    ///
    /// - `requiresNetwork`: true if the task needs network connectivity
    ///
    /// - `requiresPower`: true if the task needs the device to be charging
    ///
    /// - `task`: the work to run
    public static void scheduleProcessing(String id, Date earliestBeginDate, boolean requiresNetwork, boolean requiresPower, Runnable task) {
        long earliest = earliestBeginDate == null ? 0 : earliestBeginDate.getTime();
        Display.getInstance().scheduleBackgroundProcessing(id, earliest, requiresNetwork, requiresPower, task);
    }

    /// Cancels a previously scheduled processing task.
    ///
    /// #### Parameters
    ///
    /// - `id`: the task id
    public static void cancel(String id) {
        Display.getInstance().cancelBackgroundProcessing(id);
    }

    /// Returns true if the current platform supports deferrable background processing.
    ///
    /// #### Returns
    ///
    /// true if background processing is supported
    public static boolean isSupported() {
        return Display.getInstance().isBackgroundProcessingSupported();
    }
}
