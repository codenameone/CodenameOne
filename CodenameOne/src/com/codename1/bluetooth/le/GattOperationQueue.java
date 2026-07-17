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
package com.codename1.bluetooth.le;

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/// Serializes GATT operations against one peripheral: platform stacks
/// (Android in particular) silently drop a second in-flight GATT request,
/// so every operation is started only after the previous one completed,
/// failed, or hit the safety timeout. Each operation owns an independent
/// `AsyncResource`, so callers may issue any number of concurrent requests
/// and correlate results per call.
final class GattOperationQueue {

    /// One queued operation: `start()` invokes the port SPI which must
    /// eventually complete or fail [#result].
    abstract static class Op {
        final AsyncResource<?> result;
        TimerTask timeoutTask;
        Timer timeoutTimer;

        Op(AsyncResource<?> result) {
            this.result = result;
        }

        abstract void start();
    }

    private final Object lock = new Object();
    private final LinkedList<Op> pending = new LinkedList<Op>();
    private Op current;
    private int timeoutMillis = 30000;

    /// Sets the per-operation safety timeout; `0` or negative disables it.
    void setTimeoutMillis(int millis) {
        timeoutMillis = millis;
    }

    int getTimeoutMillis() {
        return timeoutMillis;
    }

    /// Enqueues the operation; it starts immediately when the queue is
    /// idle. Safe to call from any thread.
    void enqueue(final Op op) {
        op.result.onResult(new AsyncResult() {
            public void onReady(Object value, Throwable err) {
                advance(op);
            }
        });
        boolean startNow;
        synchronized (lock) {
            if (current == null) {
                current = op;
                startNow = true;
            } else {
                pending.add(op);
                startNow = false;
            }
        }
        if (startNow) {
            startOp(op);
        }
    }

    /// Fails the in-flight and all queued operations -- called when the
    /// connection drops.
    void failAll(BluetoothException reason) {
        ArrayList<Op> toFail = new ArrayList<Op>();
        synchronized (lock) {
            if (current != null) {
                toFail.add(current);
                current = null;
            }
            toFail.addAll(pending);
            pending.clear();
        }
        int size = toFail.size();
        for (int i = 0; i < size; i++) {
            Op op = toFail.get(i);
            cancelTimeout(op);
            if (!op.result.isDone()) {
                op.result.error(reason);
            }
        }
    }

    private void startOp(final Op op) {
        // an operation cancelled while it waited in the queue never fires
        // its callbacks, so it must be skipped explicitly
        if (op.result.isDone()) {
            advance(op);
            return;
        }
        armTimeout(op);
        try {
            op.start();
        } catch (RuntimeException ex) {
            if (!op.result.isDone()) {
                op.result.error(new BluetoothException(BluetoothError.UNKNOWN,
                        "GATT operation failed to start: " + ex, ex));
            }
        }
    }

    private void advance(Op op) {
        Op next;
        synchronized (lock) {
            if (current != op) {
                return;
            }
            cancelTimeout(op);
            current = pending.poll();
            next = current;
        }
        if (next != null) {
            startOp(next);
        }
    }

    private void armTimeout(final Op op) {
        final int t = timeoutMillis;
        if (t <= 0) {
            return;
        }
        TimerTask task = new TimerTask() {
            public void run() {
                boolean isCurrent;
                synchronized (lock) {
                    isCurrent = current == op;
                }
                if (isCurrent && !op.result.isDone()) {
                    op.result.error(new BluetoothException(
                            BluetoothError.TIMEOUT,
                            "GATT operation timed out after " + t + "ms"));
                }
            }
        };
        op.timeoutTask = task;
        op.timeoutTimer = schedule(task, t);
    }

    private void cancelTimeout(Op op) {
        TimerTask task = op.timeoutTask;
        if (task != null) {
            op.timeoutTask = null;
            task.cancel();
        }
        Timer timer = op.timeoutTimer;
        if (timer != null) {
            op.timeoutTimer = null;
            // ends the timer thread rather than leaving it parked for the
            // life of the process
            timer.cancel();
        }
    }

    /// Schedules a one-shot task on its own timer and returns it so the
    /// caller can cancel both task and timer. A timer per pending timeout
    /// mirrors `Display.setTimeout` and keeps the device API surface
    /// (CLDC11) satisfied -- it declares no daemon-thread constructor, and
    /// a shared non-daemon timer would keep a desktop JVM alive after the
    /// app closes. Also used by [BlePeripheral] for connect timeouts.
    static Timer schedule(TimerTask task, int delayMillis) {
        Timer timer = new Timer();
        timer.schedule(task, delayMillis);
        return timer;
    }
}
