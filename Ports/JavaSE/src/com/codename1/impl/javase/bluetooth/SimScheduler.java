/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase.bluetooth;

/**
 * The single execution lane of the simulated Bluetooth stack. Every state
 * mutation and every callback of {@link SimulatedBluetoothStack} runs
 * through one scheduler, which makes the whole simulation single-threaded
 * and deterministic.
 *
 * <p>Two implementations exist: {@link AutoScheduler} (a real daemon
 * thread, used by the running simulator) and {@link ManualScheduler}
 * (a virtual clock pumped explicitly, used by unit tests and anything
 * else that needs full determinism).</p>
 */
public interface SimScheduler {

    /**
     * Enqueues a task to run as soon as possible, after every task already
     * queued. Safe to call from any thread, including from within a task.
     */
    void post(Runnable task);

    /**
     * Enqueues a task to run after the given delay in (possibly virtual)
     * milliseconds. Tasks with equal due times run in submission order.
     */
    void postDelayed(Runnable task, long millis);

    /**
     * Discards pending tasks and stops accepting new ones. Idempotent.
     */
    void shutdown();
}
