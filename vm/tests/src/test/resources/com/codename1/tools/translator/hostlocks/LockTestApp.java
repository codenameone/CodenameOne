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
public class LockTestApp {
    private static native void report(String msg);

    static void run(String[] args) {
        testBasicLock();
        testReentrancy();
        testTryLock();
        testCondition();
    }

    private static void testBasicLock() {
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
             report("TEST: Basic Lock OK");
        } finally {
            lock.unlock();
        }
    }

    private static void testReentrancy() {
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
            lock.lock();
            try {
                if (lock.getHoldCount() == 2) {
                    report("TEST: Reentrancy OK");
                }
            } finally {
                lock.unlock();
            }
        } finally {
            lock.unlock();
        }
    }

    private static void testTryLock() {
        ReentrantLock lock = new ReentrantLock();
        if (lock.tryLock()) {
            try {
                report("TEST: TryLock OK");
            } finally {
                lock.unlock();
            }
        } else {
            report("TEST: TryLock FAILED");
        }
    }

    private static void testCondition() {
        final ReentrantLock lock = new ReentrantLock();
        final Condition cond = lock.newCondition();
        final boolean[] signalled = new boolean[1];

        Thread t = new Thread(new Runnable() {
            public void run() {
                lock.lock();
                try {
                    signalled[0] = true;
                    cond.signal();
                } finally {
                    lock.unlock();
                }
            }
        });

        lock.lock();
        try {
            t.start();
            cond.await();
            if (signalled[0]) {
                report("TEST: Condition OK");
            }
        } catch (InterruptedException e) {
            report("TEST: Condition INTERRUPTED");
        } finally {
            lock.unlock();
        }
    }
}
