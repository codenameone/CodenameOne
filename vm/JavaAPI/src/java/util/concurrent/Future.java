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
package java.util.concurrent;

/// The result of work that finishes later -- on the backend, what an `@Async`
/// method returns, completed when its body has run on its executor.
///
/// @param <V> the type of the result
public interface Future<V> {
    /// Stops the work if it has not started. Answers whether it was cancelled;
    /// work already running is not interrupted by the backend's executors.
    ///
    /// @param mayInterruptIfRunning whether a running task may be interrupted
    /// @return true when this call cancelled it
    boolean cancel(boolean mayInterruptIfRunning);

    /// Whether [#cancel] cancelled the work before it ran.
    ///
    /// @return true when cancelled
    boolean isCancelled();

    /// Whether the work has finished: completed, failed or cancelled.
    ///
    /// @return true once it will not change again
    boolean isDone();

    /// Waits for the work to finish and returns its result.
    ///
    /// @return the result
    /// @throws InterruptedException when the waiting thread is interrupted
    /// @throws ExecutionException when the work threw; [Throwable#getCause] is
    /// what it threw
    /// @throws CancellationException when the work was cancelled
    V get() throws InterruptedException, ExecutionException;

    /// Waits up to `timeout` for the work to finish and returns its result.
    ///
    /// @param timeout how long to wait, in `unit`
    /// @param unit the unit of `timeout`
    /// @return the result
    /// @throws InterruptedException when the waiting thread is interrupted
    /// @throws ExecutionException when the work threw
    /// @throws TimeoutException when it has not finished in time
    /// @throws CancellationException when the work was cancelled
    V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
