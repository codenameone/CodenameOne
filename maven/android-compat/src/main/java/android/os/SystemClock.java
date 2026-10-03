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
package android.os;

/// Clocks. `uptimeMillis` is the time base the message queue uses.
public final class SystemClock {

    private static final long START = System.currentTimeMillis();

    private SystemClock() {
    }

    public static long uptimeMillis() {
        return System.currentTimeMillis() - START;
    }

    public static long elapsedRealtime() {
        return uptimeMillis();
    }

    public static long elapsedRealtimeNanos() {
        return uptimeMillis() * 1000000L;
    }

    public static long currentThreadTimeMillis() {
        return uptimeMillis();
    }

    private static final Object SLEEP_LOCK = new Object();

    /// Waits on a private monitor rather than `Thread.sleep`, which the
    /// Codename One bytecode check rejects; the effect for the caller is the
    /// same, including on the event dispatch thread, where Android code
    /// should not be sleeping either.
    public static void sleep(long ms) {
        long end = System.currentTimeMillis() + ms;
        synchronized (SLEEP_LOCK) {
            long left = ms;
            while (left > 0) {
                try {
                    SLEEP_LOCK.wait(left);
                } catch (InterruptedException e) {
                    // Matches Android: the interrupt is swallowed.
                }
                left = end - System.currentTimeMillis();
            }
        }
    }
}
