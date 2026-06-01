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

/// Entry point for scheduling constraint-aware background work. On Android this maps to
/// WorkManager and on iOS to `BGTaskScheduler`. In the simulator the work runs on a timer
/// and honors the constraint toggles in the Simulate menu.
///
/// #### See also
///
/// - WorkRequest
///
/// - BackgroundWorker
public final class BackgroundWork {

    private BackgroundWork() {
    }

    /// Schedules a unit of background work. If a request with the same id was previously
    /// scheduled it is replaced. On platforms that do not support background work this is
    /// a no-op; check `#isSupported()` first.
    ///
    /// #### Parameters
    ///
    /// - `request`: the work request to schedule
    public static void schedule(WorkRequest request) {
        Display.getInstance().scheduleBackgroundWork(request);
    }

    /// Cancels previously scheduled work by id.
    ///
    /// #### Parameters
    ///
    /// - `workId`: the id of the work to cancel
    public static void cancel(String workId) {
        Display.getInstance().cancelBackgroundWork(workId);
    }

    /// Returns true if the current platform supports constraint-aware background work.
    ///
    /// #### Returns
    ///
    /// true if background work is supported
    public static boolean isSupported() {
        return Display.getInstance().isBackgroundWorkSupported();
    }
}
