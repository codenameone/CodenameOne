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
package com.codename1.hardware;

/// The hardware watchdog, opened through [Hardware#openWatchdog(int)].
///
/// Once open, the device resets unless [#kick()] is called within the
/// timeout. Kick it from a timer that only runs while the application is
/// healthy, so a hung event dispatch thread causes a reset:
///
/// ```java
/// final Watchdog wd = Hardware.getInstance().openWatchdog(15);
/// UITimer.timer(5000, true, form, () -> wd.kickQuietly());
/// ```
@com.codename1.impl.SharedWithBackend
public final class Watchdog {

    private final Hardware hw;
    private final int handle;
    private boolean closed;

    Watchdog(Hardware hw, int handle) {
        this.hw = hw;
        this.handle = handle;
    }

    /// Restarts the countdown.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the watchdog is closed or the kick fails
    public void kick() throws HardwareException {
        if (closed) {
            throw new HardwareException("Watchdog is closed");
        }
        hw.watchdogKick(handle);
    }

    /// [#kick()], ignoring failures, for use from a timer.
    public void kickQuietly() {
        try {
            kick();
        } catch (HardwareException ignored) {
            // A failed kick is what the watchdog exists to catch.
        }
    }

    /// Closes the watchdog. With `disarm` the timer stops where the driver
    /// allows it; without, the device resets when the timeout runs out, which
    /// is the right choice on the way down after an unrecoverable error.
    public void close(boolean disarm) {
        if (!closed) {
            closed = true;
            hw.close(handle, disarm);
        }
    }
}
