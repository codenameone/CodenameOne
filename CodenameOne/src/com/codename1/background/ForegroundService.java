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

/// Runs a long lived task while a persistent system notification is shown to the user.
/// This is an Android foreground service. On iOS, which has no foreground service concept,
/// the task runs under a limited background execution window accompanied by a local
/// notification (best effort); check `#isSupported()` to detect full support.
///
/// Usage
/// ```java
/// ForegroundService svc = ForegroundService.start("downloads", "Downloading", "Please wait", null,
///     service -> {
///         for (int i = 0; i <= 100; i++) {
///             service.updateNotification("Downloading", i + "%");
///             // ... do a chunk of work ...
///         }
///     });
/// // the service auto-stops when the task returns, or call svc.stop() early
/// ```
public class ForegroundService {

    /// The long running task executed by a foreground service.
    public interface Task {

        /// Runs the work. The accompanying notification remains visible until this method
        /// returns or `ForegroundService#stop()` is called.
        ///
        /// #### Parameters
        ///
        /// - `service`: the running service, used to update the notification or stop early
        void run(ForegroundService service);
    }

    private Object nativeHandle;
    private boolean running;

    private ForegroundService() {
    }

    /// Starts a foreground service that shows a persistent notification and runs the task
    /// on a background thread.
    ///
    /// #### Parameters
    ///
    /// - `channelId`: the notification channel id to post the notification on
    ///
    /// - `title`: the notification title
    ///
    /// - `body`: the notification body
    ///
    /// - `iconName`: the small icon resource name, or null for the default app icon
    ///
    /// - `task`: the work to run
    ///
    /// #### Returns
    ///
    /// the running service handle
    public static ForegroundService start(String channelId, String title, String body, String iconName, Task task) {
        ForegroundService svc = new ForegroundService();
        svc.nativeHandle = Display.getInstance().startForegroundService(channelId, title, body, iconName, task, svc);
        svc.running = true;
        return svc;
    }

    /// Updates the text of the foreground service notification.
    ///
    /// #### Parameters
    ///
    /// - `title`: the new title
    ///
    /// - `body`: the new body
    public void updateNotification(String title, String body) {
        if (running) {
            Display.getInstance().updateForegroundServiceNotification(nativeHandle, title, body);
        }
    }

    /// Stops the service and removes its notification.
    public void stop() {
        if (running) {
            running = false;
            Display.getInstance().stopForegroundService(nativeHandle);
        }
    }

    /// Returns true if the service is currently running.
    ///
    /// #### Returns
    ///
    /// true if running
    public boolean isRunning() {
        return running;
    }

    /// Returns true if the current platform fully supports foreground services.
    ///
    /// #### Returns
    ///
    /// true if foreground services are supported
    public static boolean isSupported() {
        return Display.getInstance().isForegroundServiceSupported();
    }
}
