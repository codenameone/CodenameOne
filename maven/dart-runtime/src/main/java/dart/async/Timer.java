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
 * {@code CN.setTimeout}; headless (tests, plain JVM) it falls back to a
 * short-lived thread. {@link #cancel()} prevents a not-yet-fired callback.
 */
public final class Timer {

    private volatile boolean cancelled;
    private volatile boolean fired;

    public Timer(Duration duration, final Funcs.VoidFunc0 callback) {
        // Dart fires a negative delay as soon as possible, as though it were zero.
        // Passed through, it made the Display path throw from the scheduler and
        // the headless thread die in Thread.sleep -- a timer that stayed active
        // and never called back.
        long ms = duration == null ? 0 : Math.max(0L, duration.inMilliseconds());
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (cancelled) {
                    return;
                }
                fired = true;
                if (callback != null) {
                    callback.call();
                }
            }
        };
        if (com.codename1.ui.Display.isInitialized()) {
            com.codename1.ui.CN.setTimeout((int) ms, r);
        } else {
            final long delay = ms;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ignore) {
                        // fall through
                    }
                    r.run();
                }
            }, "dart-timer").start();
        }
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isActive() {
        return !cancelled && !fired;
    }
}
