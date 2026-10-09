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

import com.codename1.util.EasyThread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/// The threads the `...Async` methods of [CompletableFuture] run on when
/// they are given no executor: a few workers, each task going to the one
/// with the least to do, so a task that waits for another does not hold it
/// up.
final class AsyncPool implements Executor {

    private static final int WORKERS = 8;

    private final List<AtomicReference<EasyThread>> workers = new ArrayList<AtomicReference<EasyThread>>();
    private final AtomicInteger[] pending = new AtomicInteger[WORKERS];

    AsyncPool() {
        for (int i = 0; i < WORKERS; i++) {
            pending[i] = new AtomicInteger();
            workers.add(new AtomicReference<EasyThread>());
        }
    }

    @Override
    public void execute(final Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        int slot = 0;
        int least = Integer.MAX_VALUE;
        for (int i = 0; i < WORKERS; i++) {
            int n = pending[i].get();
            if (n < least) {
                least = n;
                slot = i;
            }
        }
        final AtomicInteger count = pending[slot];
        count.incrementAndGet();
        AtomicReference<EasyThread> holder = workers.get(slot);
        EasyThread worker = holder.get();
        if (worker == null) {
            EasyThread started = EasyThread.start("cn1-async-" + slot);
            if (holder.compareAndSet(null, started)) {
                worker = started;
            } else {
                started.killWhenIdle();
                worker = holder.get();
            }
        }
        worker.run(new Runnable() {
            @Override
            public void run() {
                try {
                    command.run();
                } finally {
                    count.decrementAndGet();
                }
            }
        });
    }
}
