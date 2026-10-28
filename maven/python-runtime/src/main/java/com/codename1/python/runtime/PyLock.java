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

/// `_thread.LockType`: a lock that is not owned by the thread that took it,
/// so any thread may release it. Waiting for it releases the [Gil].
final class PyLock {
    private boolean locked;

    /// Takes the lock: at once, without waiting when `blocking` is false,
    /// or waiting at most `timeoutMillis` when that is not negative.
    boolean acquire(boolean blocking, long timeoutMillis) {
        synchronized (this) {
            if (!locked) {
                locked = true;
                return true;
            }
            if (!blocking) {
                return false;
            }
        }
        int saved = Gil.release();
        try {
            synchronized (this) {
                long deadline = timeoutMillis < 0 ? 0 : System.currentTimeMillis() + timeoutMillis;
                while (locked) {
                    long wait = 0;
                    if (timeoutMillis >= 0) {
                        wait = deadline - System.currentTimeMillis();
                        if (wait <= 0) {
                            return false;
                        }
                    }
                    try {
                        wait(wait);
                    } catch (InterruptedException e) {
                        // a spurious wake-up: the loop checks again
                    }
                }
                locked = true;
                return true;
            }
        } finally {
            Gil.restore(saved);
        }
    }

    void release() {
        synchronized (this) {
            if (!locked) {
                throw Py.error(Py.RUNTIME_ERROR, "release unlocked lock");
            }
            locked = false;
            notifyAll();
        }
    }

    boolean locked() {
        synchronized (this) {
            return locked;
        }
    }
}
