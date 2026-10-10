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

import java.util.Collection;
import java.util.List;

/// `java.util.concurrent.ExecutorService` for the Codename One runtime: an
/// [Executor] that answers a [Future] for a submitted task and can be shut
/// down. [Executors] creates the implementations.
///
/// `invokeAny` and the `invokeAll` overload with a timeout are not provided.
public interface ExecutorService extends Executor {

    /// Stops accepting tasks. The ones already submitted still run.
    void shutdown();

    /// Stops accepting tasks and drops the ones that have not started,
    /// answering them. A task that is running is left to finish: a device
    /// thread cannot be interrupted.
    List<Runnable> shutdownNow();

    boolean isShutdown();

    /// Whether the service was shut down and every task it accepted has
    /// finished or been dropped.
    boolean isTerminated();

    /// Waits until the service has terminated or the time is up, and answers
    /// which of the two it was.
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    <T> Future<T> submit(Callable<T> task);

    <T> Future<T> submit(Runnable task, T result);

    Future<?> submit(Runnable task);

    /// Runs every task and answers their futures, all of them done.
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException;
}
