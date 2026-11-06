/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.async;

import dart.core.Duration;
import dart.runtime.Funcs;

/**
 * Dart's {@code dart:async} Timer: a one-shot callback scheduled after a
 * {@link Duration}. With a live CN1 Display the callback fires on the EDT via
 * {@code CN.setTimeout}; headless (tests, plain JVM) the isolate's event
 * loop fires it (see Microtasks). {@link #cancel()} prevents a not-yet-fired callback.
 */
public class Timer {

    private volatile boolean cancelled;
    private volatile boolean fired;
    /** Cleared by cancel(), so a cancelled timer no longer keeps its callback reachable. */
    volatile Funcs.VoidFunc0 callback;

    public Timer(Duration duration, Funcs.VoidFunc0 callback) {
        this.callback = callback;
        // Dart fires a negative delay as soon as possible, as though it were zero.
        // Passed through, it made the Display path throw from the scheduler and
        // the headless thread die in Thread.sleep -- a timer that stayed active
        // and never called back.
        long ms = duration == null ? 0 : Math.max(0L, duration.inMilliseconds());
        final Runnable r = Zone.captureGuarded(new Runnable() {
            @Override
            public void run() {
                if (cancelled) {
                    return;
                }
                fired = true;
                Funcs.VoidFunc0 cb = Timer.this.callback;
                Timer.this.callback = null;
                // A timer callback is a turn of Dart's event loop. It runs from the
                // loop itself (Microtasks.drain), which runs the microtasks it queued
                // before the next timer; draining here as well ran the later timers
                // nested inside this one, ahead of its own zone's error handler.
                if (cb != null) {
                    cb.call();
                }
            }
        });
        // One ordered queue for every timer of the isolate (see Microtasks): equal
        // deadlines fire in creation order, as in Dart, with or without a Display.
        entry = Microtasks.scheduleTimer(ms, r);
    }

    /** The queued timer, so cancel() can remove it. */
    private Microtasks.TimerEntry entry;

    public void cancel() {
        cancelled = true;
        Timer n = next;
        next = null;
        if (n != null) {
            n.cancel();
        }
        callback = null;
        Microtasks.TimerEntry e = entry;
        entry = null;
        if (e != null) {
            Microtasks.cancelTimer(e);
        }
    }

    public boolean isActive() {
        return !cancelled && !fired;
    }

    private Timer next;
    private long tick;

    protected Timer() {
    }

    /// {@code Timer.run(callback)}: runs {@code callback} asynchronously, as a zero
    /// duration timer does -- after the current microtasks, never synchronously.
    public static void run(Funcs.VoidFunc0 callback) {
        new Timer(Duration.zero, callback);
    }

    /// {@code Timer.periodic(period, callback)}: a chain of one-shot timers, each
    /// scheduled when the previous fires, so a slow callback delays the next tick.
    public static Timer periodic(final Duration period, final Funcs.VoidFunc1<Timer> callback) {
        final Timer t = new Timer();
        t.scheduleTick(period, callback);
        return t;
    }

    private void scheduleTick(final Duration period, final Funcs.VoidFunc1<Timer> callback) {
        next = new Timer(period, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (cancelled) {
                    return;
                }
                tick++;
                callback.call(Timer.this);
                if (!cancelled) {
                    scheduleTick(period, callback);
                }
            }
        });
    }

    /// The number of periods that have fired; 0 for a one-shot timer until it fires.
    public long tick() {
        return next != null || tick > 0 ? tick : (fired ? 1 : 0);
    }
}
