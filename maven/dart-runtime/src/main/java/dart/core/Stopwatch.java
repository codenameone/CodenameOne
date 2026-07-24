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
package dart.core;

/**
 * Java implementation of the Dart {@code dart:core} {@code Stopwatch}: a monotonic elapsed-time
 * measurement backed by {@link System#nanoTime()}. Getters are exposed as methods (the transpiler
 * lowers Dart getters to no-arg calls).
 */
public final class Stopwatch {
    private long startNanos;
    private long accumulatedNanos;
    private boolean running;

    /** Creates a stopped stopwatch with zero elapsed time. */
    public Stopwatch() {
    }

    /** Starts (or resumes) measuring. */
    public void start() {
        if (!running) {
            running = true;
            startNanos = System.nanoTime();
        }
    }

    /** Stops measuring, retaining the elapsed time. */
    public void stop() {
        if (running) {
            accumulatedNanos += System.nanoTime() - startNanos;
            running = false;
        }
    }

    /** Resets the elapsed time to zero (keeps the running state). */
    public void reset() {
        accumulatedNanos = 0;
        startNanos = System.nanoTime();
    }

    private long elapsedNanos() {
        return running ? accumulatedNanos + (System.nanoTime() - startNanos) : accumulatedNanos;
    }

    /** Whether the stopwatch is currently running. */
    public boolean isRunning() {
        return running;
    }

    /** Elapsed whole microseconds. */
    public long elapsedMicroseconds() {
        return elapsedNanos() / 1000L;
    }

    /** Elapsed whole milliseconds. */
    public long elapsedMilliseconds() {
        return elapsedNanos() / 1000000L;
    }

    /** Elapsed raw ticks (nanoseconds, matching {@link #frequency()}). */
    public long elapsedTicks() {
        return elapsedNanos();
    }

    /** Elapsed time as a {@link Duration}. */
    public Duration elapsed() {
        return Duration.ofMicroseconds(elapsedMicroseconds());
    }

    /** Ticks per second (nanosecond resolution). */
    public long frequency() {
        return 1000000000L;
    }
}
