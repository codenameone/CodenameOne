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
package com.codename1.backend;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/// A [Future] that is already complete: what an `@Async` method
/// returns from its body.
///
/// ```java
/// @Async
///   public Future build(String month) {
///       return AsyncResult.of(compute(month));
///   }
/// ```
///
/// The caller never sees this object. It receives the Future of the call
/// itself at once, which completes with this value when the body has run --
/// the same arrangement as Spring's `AsyncResult` and
/// `CompletableFuture.completedFuture`.
public final class AsyncResult<V> implements Future<V> {
    private final V value;
    private final Throwable failure;

    private AsyncResult(V value, Throwable failure) {
        this.value = value;
        this.failure = failure;
    }

    /// A result holding `value`.
    public static <V> AsyncResult<V> of(V value) {
        return new AsyncResult<V>(value, null);
    }

    /// A result that failed with `failure`, for a body that reports rather than throws.
    public static <V> AsyncResult<V> failed(Throwable failure) {
        return new AsyncResult<V>(null, failure);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public V get() throws ExecutionException {
        if (failure != null) {
            throw new ExecutionException(failure);
        }
        return value;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws ExecutionException {
        return get();
    }
}
