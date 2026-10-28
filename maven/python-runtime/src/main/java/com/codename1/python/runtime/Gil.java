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

/// The global interpreter lock: Python code runs on one thread at a time.
///
/// The runtime's caches, containers and module state assume a single
/// running thread, as CPython's do, and a torn update of one could hand an
/// object of the wrong class to the next instruction -- a native crash on a
/// device, where casts are not checked. Every way into Python takes this
/// lock (the program's start, a callback from Java, a `threading` thread),
/// and every operation that waits releases it for the wait (`time.sleep`, a
/// lock or condition, an HTTP request), so threads take turns at the points
/// where a Python program waits. CPU-bound threads do not run in parallel,
/// as under CPython.
///
/// Reentrant: a callback into Python from Java code that Python called runs
/// on the same thread, which already holds the lock.
public final class Gil {
    private static final Object MONITOR = new Object();
    private static Thread owner;
    private static int depth;

    private Gil() {
    }

    /// Takes the lock, waiting for the thread that holds it to wait or
    /// leave Python.
    public static void enter() {
        Thread t = Thread.currentThread();
        synchronized (MONITOR) {
            if (owner == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                depth++;
                return;
            }
            while (owner != null) {
                try {
                    MONITOR.wait();
                } catch (InterruptedException e) {
                    // keep waiting: the lock is the only way back into Python
                }
            }
            owner = t;
            depth = 1;
        }
    }

    /// Leaves one [#enter()].
    public static void exit() {
        synchronized (MONITOR) {
            if (owner != Thread.currentThread()) { //NOPMD CompareObjectsWithEquals - identity is meant
                return;
            }
            if (--depth == 0) {
                owner = null;
                MONITOR.notifyAll();
            }
        }
    }

    /// Releases the lock entirely before a wait, answering what
    /// [#restore(int)] takes back afterwards (0 when this thread did not
    /// hold it).
    public static int release() {
        synchronized (MONITOR) {
            if (owner != Thread.currentThread()) { //NOPMD CompareObjectsWithEquals - identity is meant
                return 0;
            }
            int d = depth;
            owner = null;
            depth = 0;
            MONITOR.notifyAll();
            return d;
        }
    }

    /// Takes the lock back after a wait.
    public static void restore(int saved) {
        if (saved > 0) {
            enter();
            synchronized (MONITOR) {
                depth = saved;
            }
        }
    }

    /// Lets another thread waiting for the lock run, as `time.sleep(0)`
    /// does.
    public static void yieldNow() {
        int saved = release();
        Thread.yield();
        restore(saved);
    }
}
