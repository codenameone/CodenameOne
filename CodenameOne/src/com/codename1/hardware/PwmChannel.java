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

/// A hardware PWM channel, opened through [Hardware#openPwm(int, int)].
///
/// ```java
/// PwmChannel servo = Hardware.getInstance().openPwm(0, 0);
/// servo.configure(20000000L, 1500000L, true);  // 50 Hz, 1.5 ms pulse: centred
/// ```
@com.codename1.impl.SharedWithBackend
public final class PwmChannel {

    private final Hardware hw;
    private final int handle;
    private final int chip;
    private final int channel;
    private long periodNanos;
    private long dutyNanos;
    private boolean enabled;
    private boolean closed;

    PwmChannel(Hardware hw, int handle, int chip, int channel) {
        this.hw = hw;
        this.handle = handle;
        this.chip = chip;
        this.channel = channel;
    }

    /// The PWM controller number.
    public int getChip() {
        return chip;
    }

    /// The channel within the controller.
    public int getChannel() {
        return channel;
    }

    /// Sets the period, the high time and whether the output runs.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the duty exceeds the period, the
    ///   controller rejects the values, or the channel is closed
    public void configure(long periodNanos, long dutyNanos, boolean enabled) throws HardwareException {
        if (closed) {
            throw new HardwareException("PWM channel " + chip + "/" + channel + " is closed");
        }
        if (periodNanos <= 0 || dutyNanos < 0 || dutyNanos > periodNanos) {
            throw new HardwareException("PWM duty must be within 0..period");
        }
        hw.pwmConfigure(handle, periodNanos, dutyNanos, enabled);
        this.periodNanos = periodNanos;
        this.dutyNanos = dutyNanos;
        this.enabled = enabled;
    }

    /// Sets the duty cycle as a fraction of the current period, keeping the
    /// period and enabled state.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when no period was configured yet
    public void setDutyCycle(double fraction) throws HardwareException {
        if (periodNanos <= 0) {
            throw new HardwareException("Configure a PWM period first");
        }
        double f = fraction < 0 ? 0 : (fraction > 1 ? 1 : fraction);
        configure(periodNanos, (long) (periodNanos * f), enabled);
    }

    /// The configured period in nanoseconds, `0` before [#configure(long, long, boolean)].
    public long getPeriodNanos() {
        return periodNanos;
    }

    /// The configured high time in nanoseconds.
    public long getDutyNanos() {
        return dutyNanos;
    }

    /// `true` while the output runs.
    public boolean isEnabled() {
        return enabled;
    }

    /// Disables the output and releases the channel. Closing twice is harmless.
    public void close() {
        if (!closed) {
            if (enabled) {
                try {
                    hw.pwmConfigure(handle, periodNanos, dutyNanos, false);
                } catch (HardwareException ignored) {
                    // Releasing is what matters; the controller may already be gone.
                }
            }
            closed = true;
            hw.close(handle, false);
        }
    }
}
