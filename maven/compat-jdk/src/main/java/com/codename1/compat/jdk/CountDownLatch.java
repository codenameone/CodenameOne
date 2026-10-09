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
package com.codename1.compat.jdk;

import java.util.concurrent.atomic.AtomicLong;

/// `java.util.concurrent.CountDownLatch`: threads wait until a count
/// reaches zero. Waiting on the event dispatch thread keeps events being
/// dispatched, the way `Display.invokeAndBlock` does.
public class CountDownLatch {

    private final AtomicLong count;
    private final CompletableFuture<Boolean> open = new CompletableFuture<Boolean>();

    public CountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }
        this.count = new AtomicLong(count);
        if (count == 0) {
            open.complete(Boolean.TRUE);
        }
    }

    public void countDown() {
        while (true) {
            long c = count.get();
            if (c == 0) {
                return;
            }
            if (count.compareAndSet(c, c - 1)) {
                if (c == 1) {
                    open.complete(Boolean.TRUE);
                }
                return;
            }
        }
    }

    public long getCount() {
        return count.get();
    }

    public void await() throws InterruptedException {
        try {
            open.get();
        } catch (ExecutionException e) {
            // The latch is only ever opened; there is no failure to report.
            return;
        }
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            open.get(timeout, unit);
            return true;
        } catch (ExecutionException e) {
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "CountDownLatch[Count = " + count.get() + "]";
    }
}
