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

import java.util.concurrent.locks.*;
import java.util.concurrent.TimeUnit;
public class StampedLockTestApp {
    private static native void report(String msg);

    static void run(String[] args) {
        testBasic();
        testOptimisticRead();
        testWriteLockExclusion();
    }

    private static void testBasic() {
        StampedLock sl = new StampedLock();
        long stamp = sl.writeLock();
        try {
             report("TEST: Basic Write Lock OK");
        } finally {
             sl.unlockWrite(stamp);
        }

        stamp = sl.readLock();
        try {
             report("TEST: Basic Read Lock OK");
        } finally {
             sl.unlockRead(stamp);
        }
    }

    private static void testOptimisticRead() {
        StampedLock sl = new StampedLock();
        long stamp = sl.tryOptimisticRead();
        if (stamp != 0 && sl.validate(stamp)) {
            report("TEST: Optimistic Read Valid OK");
        } else {
            report("TEST: Optimistic Read Valid FAILED");
        }

        long ws = sl.writeLock();
        if (sl.validate(stamp)) {
            report("TEST: Optimistic Read Invalid (during write) FAILED");
        } else {
            report("TEST: Optimistic Read Invalid (during write) OK");
        }
        sl.unlockWrite(ws);

        if (sl.validate(stamp)) {
             report("TEST: Optimistic Read Invalid (after write) FAILED");
        } else {
             report("TEST: Optimistic Read Invalid (after write) OK");
        }
    }

    private static void testWriteLockExclusion() {
        final StampedLock sl = new StampedLock();
        long stamp = sl.writeLock();

        final boolean[] success = new boolean[1];
        Thread t = new Thread(new Runnable() {
            public void run() {
                long s = sl.readLock();
                sl.unlockRead(s);
                success[0] = true;
            }
        });
        t.start();

        try { Thread.sleep(200); } catch(Exception e) {}
        if (success[0]) {
             report("TEST: Write Lock Exclusion FAILED (Reader acquired lock)");
             return;
        }

        sl.unlockWrite(stamp);
        try { Thread.sleep(200); } catch(Exception e) {}
        if (success[0]) {
             report("TEST: Write Lock Exclusion OK");
        } else {
             report("TEST: Write Lock Exclusion FAILED (Reader did not acquire lock)");
        }
    }
}
