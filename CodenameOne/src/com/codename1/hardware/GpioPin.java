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

import java.util.concurrent.atomic.AtomicBoolean;

/// A claimed GPIO line, opened through [Hardware#openGpioOutput(String, int, boolean)]
/// or [Hardware#openGpioInput(String, int, int, int)].
///
/// The line stays claimed until [#close()]; another process cannot drive it in
/// the meantime.
@com.codename1.impl.SharedWithBackend
public final class GpioPin {

    /// Leave the line's bias as the board configured it.
    public static final int BIAS_DEFAULT = 0;
    /// Disable internal pull resistors.
    public static final int BIAS_DISABLED = 1;
    /// Enable the internal pull-up resistor.
    public static final int BIAS_PULL_UP = 2;
    /// Enable the internal pull-down resistor.
    public static final int BIAS_PULL_DOWN = 3;

    /// Report no edges.
    public static final int EDGE_NONE = 0;
    /// Report low-to-high transitions.
    public static final int EDGE_RISING = 1;
    /// Report high-to-low transitions.
    public static final int EDGE_FALLING = 2;
    /// Report both transitions.
    public static final int EDGE_BOTH = 3;

    private static final int WAIT_SLICE_MS = 250;

    private final Hardware hw;
    private final int handle;
    private final String chip;
    private final int line;
    private final boolean output;
    private boolean high;
    private boolean closed;
    private GpioListener listener;
    // Read by the edge thread, written on the caller's thread.
    private AtomicBoolean edgeThreadRunning;

    GpioPin(Hardware hw, int handle, String chip, int line, boolean output, boolean initialHigh) {
        this.hw = hw;
        this.handle = handle;
        this.chip = chip;
        this.line = line;
        this.output = output;
        this.high = initialHigh;
    }

    /// The controller this line belongs to.
    public String getChip() {
        return chip;
    }

    /// The line offset within its controller.
    public int getLine() {
        return line;
    }

    /// `true` when the line was opened as an output.
    public boolean isOutput() {
        return output;
    }

    /// Reads the line level. For an output this is the level it is driven to.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the read fails or the pin is closed
    public boolean isHigh() throws HardwareException {
        checkOpen();
        if (output) {
            return high;
        }
        return hw.gpioRead(handle);
    }

    /// Drives an output line.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the line is an input, closed, or the write fails
    public void setHigh(boolean value) throws HardwareException {
        checkOpen();
        if (!output) {
            throw new HardwareException("GPIO line " + line + " is an input");
        }
        hw.gpioWrite(handle, value);
        high = value;
    }

    /// Delivers the edges this input was opened for to `l` on the event
    /// dispatch thread, or stops delivery when `l` is `null`. Edges are
    /// waited for on a background thread owned by this pin.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the line is an output or closed
    public void setListener(GpioListener l) throws HardwareException {
        checkOpen();
        if (output) {
            throw new HardwareException("GPIO line " + line + " is an output");
        }
        stopEdgeThread();
        listener = l;
        if (l != null) {
            final AtomicBoolean running = new AtomicBoolean(true);
            final GpioListener target = l;
            edgeThreadRunning = running;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    edgeLoop(running, target);
                }
            }, "GPIO-" + chip + "-" + line);
            t.start();
        }
    }

    /// The listener set with [#setListener(GpioListener)], or `null`.
    public GpioListener getListener() {
        return listener;
    }

    private void edgeLoop(AtomicBoolean running, final GpioListener target) {
        long[] ts = new long[1];
        while (running.get()) {
            int r;
            try {
                r = hw.gpioWaitEdge(handle, WAIT_SLICE_MS, ts);
            } catch (HardwareException e) {
                return;
            }
            if (r == Hardware.EDGE_TIMEOUT || !running.get()) {
                continue;
            }
            final boolean rising = r == Hardware.EDGE_RESULT_RISING;
            final long when = ts[0];
            hw.deliver(new Runnable() {
                @Override
                public void run() {
                    target.edgeDetected(GpioPin.this, rising, when);
                }
            });
        }
    }

    private void stopEdgeThread() {
        if (edgeThreadRunning != null) {
            edgeThreadRunning.set(false);
            edgeThreadRunning = null;
        }
    }

    /// Releases the line. Further calls on this pin throw; closing twice is
    /// harmless.
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        stopEdgeThread();
        listener = null;
        hw.close(handle, false);
    }

    private void checkOpen() throws HardwareException {
        if (closed) {
            throw new HardwareException("GPIO line " + line + " is closed");
        }
    }
}
