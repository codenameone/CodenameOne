/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

/// `_thread.LockType`: a lock that is not owned by the thread that took it,
/// so any thread may release it. Waiting for it releases the [Gil].
///
/// A compare-and-set, never a monitor: `Object.wait` or a contended
/// `synchronized` on a backend virtual thread blocks the HOST thread, and
/// with tasks queued on every host the whole server wedges. A waiter steps
/// aside through [Gil#pauseWaiter(int)] between attempts instead.
final class PyLock {
    private final AtomicBoolean locked = new AtomicBoolean();

    /// Takes the lock: at once, without waiting when `blocking` is false,
    /// or waiting at most `timeoutMillis` when that is not negative.
    boolean acquire(boolean blocking, long timeoutMillis) {
        if (locked.compareAndSet(false, true)) {
            return true;
        }
        if (!blocking) {
            return false;
        }
        long deadline = timeoutMillis < 0 ? 0 : System.currentTimeMillis() + timeoutMillis;
        int saved = Gil.release();
        try {
            int attempt = 0;
            while (!locked.compareAndSet(false, true)) {
                if (timeoutMillis >= 0 && System.currentTimeMillis() >= deadline) {
                    return false;
                }
                Gil.pauseWaiter(attempt++);
            }
            return true;
        } finally {
            Gil.restore(saved);
        }
    }

    void release() {
        if (!locked.compareAndSet(true, false)) {
            throw Py.error(Py.RUNTIME_ERROR, "release unlocked lock");
        }
    }

    boolean locked() {
        return locked.get();
    }
}
