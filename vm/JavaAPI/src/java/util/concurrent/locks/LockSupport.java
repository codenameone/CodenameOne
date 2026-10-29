/*
 * Copyright (c) 2018, Codename One and/or its affiliates. All rights reserved.
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
package java.util.concurrent.locks;

import java.util.WeakHashMap;

/// Thread parking, with the JDK's permit semantics: [#unpark(Thread)] before
/// [#park()] makes that park return at once, and a park may also return spuriously
/// or because the thread was interrupted, whose status it leaves set.
///
/// Each thread gets a monitor of its own the first time it parks or is unparked,
/// kept in a weak map so a finished thread does not hold it.
public class LockSupport {
    private static final WeakHashMap<Thread, Parker> PARKERS = new WeakHashMap<Thread, Parker>();

    private LockSupport() {
    }

    private static final class Parker {
        boolean permit;
    }

    private static Parker parker(Thread t) {
        synchronized (PARKERS) {
            Parker p = PARKERS.get(t);
            if (p == null) {
                p = new Parker();
                PARKERS.put(t, p);
            }
            return p;
        }
    }

    public static void unpark(Thread thread) {
        if (thread == null) {
            return;
        }
        Parker p = parker(thread);
        synchronized (p) {
            p.permit = true;
            p.notifyAll();
        }
    }

    public static void park() {
        block(0, true);
    }

    public static void park(Object blocker) {
        park();
    }

    public static void parkNanos(long nanos) {
        if (nanos > 0) {
            // Object.wait has millisecond resolution here; round up so a short park
            // still waits rather than spinning.
            block((nanos + 999999L) / 1000000L, false);
        }
    }

    public static void parkNanos(Object blocker, long nanos) {
        parkNanos(nanos);
    }

    public static void parkUntil(long deadline) {
        long millis = deadline - System.currentTimeMillis();
        if (millis > 0) {
            block(millis, false);
        }
    }

    public static void parkUntil(Object blocker, long deadline) {
        parkUntil(deadline);
    }

    public static Object getBlocker(Thread t) {
        return null;
    }

    private static void block(long millis, boolean forever) {
        Thread self = Thread.currentThread();
        Parker p = parker(self);
        synchronized (p) {
            if (!p.permit && !self.isInterrupted()) {
                try {
                    if (forever) {
                        p.wait();
                    } else {
                        p.wait(millis);
                    }
                } catch (InterruptedException e) {
                    // park reports an interrupt by returning with the status still set.
                    self.interrupt();
                }
            }
            p.permit = false;
        }
    }
}
