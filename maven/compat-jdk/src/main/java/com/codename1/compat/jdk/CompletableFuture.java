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

import com.codename1.util.AsyncResource;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/// `java.util.concurrent.CompletableFuture` for the Codename One runtime.
///
/// A stage completes on the thread that completed the one before it, as in
/// the JDK. The `...Async` methods without an executor run on a small pool
/// of this module's own threads -- there is no fork-join pool on a device --
/// and never on the event dispatch thread; a continuation that touches the
/// user interface passes an executor that calls `Display.callSerially`, or
/// calls it itself.
///
/// `get()` and `join()` on the event dispatch thread wait the way
/// `Display.invokeAndBlock` does: events keep being dispatched.
///
/// Exceptions travel as in the JDK: a stage that depends on a failed one
/// fails with a [CompletionException] whose cause is the original; `get()`
/// reports that cause in an [ExecutionException], `join()` throws the
/// `CompletionException`, and both throw [CancellationException] after
/// `cancel`.
///
/// Not provided: `obtrudeValue`, `obtrudeException`,
/// `getNumberOfDependents`, `delayedExecutor` and `state()`.
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {

    /// A null result; null in [#result] is "not completed".
    private static final Object NIL = new Object();

    private static final class Failure {
        final Throwable cause;

        Failure(Throwable cause) {
            this.cause = cause;
        }
    }

    /// One action waiting for this future, in a stack of them.
    private static final class Waiting {
        final Runnable action;
        Waiting next;

        Waiting(Runnable action) {
            this.action = action;
        }
    }

    /// What the stack of waiting actions holds once they have been run.
    private static final Waiting RELEASED = new Waiting(null);

    private static final AtomicReference<AsyncPool> POOL = new AtomicReference<AsyncPool>();
    private static final AtomicReference<Timer> TIMER = new AtomicReference<Timer>();

    private final AtomicReference<Object> result = new AtomicReference<Object>();
    private final AtomicReference<Waiting> waiting = new AtomicReference<Waiting>();

    public CompletableFuture() {
    }

    // ------------------------------------------------------------------
    // Completion
    // ------------------------------------------------------------------

    private boolean completeRaw(Object outcome) {
        if (!result.compareAndSet(null, outcome)) {
            return false;
        }
        Waiting head = waiting.getAndSet(RELEASED);
        // The stack is newest first; run in the order they were added.
        Waiting ordered = null;
        while (head != null && head != RELEASED) {
            Waiting next = head.next;
            head.next = ordered;
            ordered = head;
            head = next;
        }
        while (ordered != null) {
            ordered.action.run();
            ordered = ordered.next;
        }
        return true;
    }

    /// Runs `action` when this future is complete: now if it already is.
    private void whenDone(Runnable action) {
        Waiting w = new Waiting(action);
        while (true) {
            Waiting head = waiting.get();
            if (head == RELEASED) {
                action.run();
                return;
            }
            w.next = head;
            if (waiting.compareAndSet(head, w)) {
                return;
            }
        }
    }

    private static CompletionException wrap(Throwable t) {
        if (t instanceof CompletionException) {
            return (CompletionException) t;
        }
        return new CompletionException(t);
    }

    private boolean fail(Throwable t) {
        return completeRaw(new Failure(t));
    }

    /// Takes the outcome of a stage this one depends on.
    private boolean relay(Object outcome) {
        if (outcome instanceof Failure) {
            return completeRaw(new Failure(wrap(((Failure) outcome).cause)));
        }
        return completeRaw(outcome);
    }

    @SuppressWarnings("unchecked")
    private static <V> V value(Object outcome) {
        return outcome == NIL ? null : (V) outcome;
    }

    private static Throwable failure(Object outcome) {
        return outcome instanceof Failure ? ((Failure) outcome).cause : null;
    }

    public boolean complete(T value) {
        return completeRaw(value == null ? NIL : value);
    }

    public boolean completeExceptionally(Throwable ex) {
        if (ex == null) {
            throw new NullPointerException();
        }
        return fail(ex);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = result.get() == null && fail(new CancellationException());
        return cancelled || isCancelled();
    }

    @Override
    public boolean isCancelled() {
        return failure(result.get()) instanceof CancellationException;
    }

    @Override
    public boolean isDone() {
        return result.get() != null;
    }

    public boolean isCompletedExceptionally() {
        return result.get() instanceof Failure;
    }

    // ------------------------------------------------------------------
    // Reading the result
    // ------------------------------------------------------------------

    /// Waits for completion, for `millis` at most when that is not negative.
    private Object await(long millis) throws InterruptedException, TimeoutException {
        Object outcome = result.get();
        if (outcome != null) {
            return outcome;
        }
        if (millis == 0) {
            throw new TimeoutException();
        }
        final AsyncResource<Boolean> signal = new AsyncResource<Boolean>();
        whenDone(new Runnable() {
            @Override
            public void run() {
                signal.complete(Boolean.TRUE);
            }
        });
        try {
            if (millis < 0) {
                signal.get();
            } else {
                signal.get((int) Math.max(1, Math.min(millis, Integer.MAX_VALUE)));
            }
        } catch (AsyncResource.AsyncExecutionException e) {
            // The signal is only ever completed; nothing to report.
            outcome = result.get();
        } catch (InterruptedException e) {
            // AsyncResource reports running out of time this way.
            if (result.get() == null) {
                if (millis >= 0) {
                    throw new TimeoutException();
                }
                throw e;
            }
        }
        outcome = result.get();
        if (outcome == null) {
            throw new TimeoutException();
        }
        return outcome;
    }

    private T reportGet(Object outcome) throws ExecutionException {
        if (outcome instanceof Failure) {
            Throwable t = ((Failure) outcome).cause;
            if (t instanceof CancellationException) {
                throw (CancellationException) t;
            }
            if (t instanceof CompletionException && t.getCause() != null) {
                t = t.getCause();
            }
            throw new ExecutionException(t);
        }
        return CompletableFuture.<T>value(outcome);
    }

    private T reportJoin(Object outcome) {
        if (outcome instanceof Failure) {
            Throwable t = ((Failure) outcome).cause;
            if (t instanceof CancellationException) {
                throw (CancellationException) t;
            }
            throw wrap(t);
        }
        return CompletableFuture.<T>value(outcome);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return reportGet(await(-1));
        } catch (TimeoutException e) {
            throw new InterruptedException();
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return reportGet(await(Math.max(0, unit.toMillis(timeout))));
    }

    /// The result, waiting for it; a failure is a [CompletionException].
    public T join() {
        Object outcome = result.get();
        while (outcome == null) {
            try {
                outcome = await(-1);
            } catch (InterruptedException e) {
                // join does not give up; ask again.
                outcome = result.get();
            } catch (TimeoutException e) {
                outcome = result.get();
            }
        }
        return reportJoin(outcome);
    }

    public T getNow(T valueIfAbsent) {
        Object outcome = result.get();
        return outcome == null ? valueIfAbsent : reportJoin(outcome);
    }

    /// The result of a future that completed normally.
    public T resultNow() {
        Object outcome = result.get();
        if (outcome == null) {
            throw new IllegalStateException("Task has not completed");
        }
        if (outcome instanceof Failure) {
            throw new IllegalStateException(isCancelled() ? "Task was cancelled"
                    : "Task completed with exception");
        }
        return CompletableFuture.<T>value(outcome);
    }

    /// The exception of a future that failed.
    public Throwable exceptionNow() {
        Object outcome = result.get();
        if (outcome == null) {
            throw new IllegalStateException("Task has not completed");
        }
        if (!(outcome instanceof Failure)) {
            throw new IllegalStateException("Task completed with a result");
        }
        if (isCancelled()) {
            throw new IllegalStateException("Task was cancelled");
        }
        Throwable t = ((Failure) outcome).cause;
        return t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
    }

    // ------------------------------------------------------------------
    // Factories
    // ------------------------------------------------------------------

    /// The executor the `...Async` methods use when given none.
    public Executor defaultExecutor() {
        return pool();
    }

    private static Executor pool() {
        AsyncPool pool = POOL.get();
        if (pool == null) {
            POOL.compareAndSet(null, new AsyncPool());
            pool = POOL.get();
        }
        return pool;
    }

    public <U> CompletableFuture<U> newIncompleteFuture() {
        return new CompletableFuture<U>();
    }

    public static <U> CompletableFuture<U> completedFuture(U value) {
        CompletableFuture<U> f = new CompletableFuture<U>();
        f.complete(value);
        return f;
    }

    public static <U> CompletableFuture<U> failedFuture(Throwable ex) {
        if (ex == null) {
            throw new NullPointerException();
        }
        CompletableFuture<U> f = new CompletableFuture<U>();
        f.fail(ex);
        return f;
    }

    public static <U> CompletionStage<U> completedStage(U value) {
        return completedFuture(value);
    }

    public static <U> CompletionStage<U> failedStage(Throwable ex) {
        return failedFuture(ex);
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, pool());
    }

    public static <U> CompletableFuture<U> supplyAsync(final Supplier<U> supplier, Executor executor) {
        if (supplier == null || executor == null) {
            throw new NullPointerException();
        }
        final CompletableFuture<U> f = new CompletableFuture<U>();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (f.isDone()) {
                    return;
                }
                U value;
                try {
                    value = supplier.get();
                } catch (Throwable t) {
                    f.fail(wrap(t));
                    return;
                }
                f.complete(value);
            }
        });
        return f;
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, pool());
    }

    public static CompletableFuture<Void> runAsync(final Runnable runnable, Executor executor) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        return supplyAsync(new Supplier<Void>() {
            @Override
            public Void get() {
                runnable.run();
                return null;
            }
        }, executor);
    }

    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
        return completeAsync(supplier, pool());
    }

    public CompletableFuture<T> completeAsync(final Supplier<? extends T> supplier, Executor executor) {
        if (supplier == null || executor == null) {
            throw new NullPointerException();
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (isDone()) {
                    return;
                }
                T value;
                try {
                    value = supplier.get();
                } catch (Throwable t) {
                    fail(wrap(t));
                    return;
                }
                complete(value);
            }
        });
        return this;
    }

    public static CompletableFuture<Void> allOf(final CompletableFuture<?>... futures) {
        final CompletableFuture<Void> all = new CompletableFuture<Void>();
        if (futures.length == 0) {
            all.complete(null);
            return all;
        }
        final AtomicInteger remaining = new AtomicInteger(futures.length);
        Runnable one = new Runnable() {
            @Override
            public void run() {
                if (remaining.decrementAndGet() != 0) {
                    return;
                }
                for (CompletableFuture<?> f : futures) {
                    Object outcome = f.result.get();
                    if (outcome instanceof Failure) {
                        all.relay(outcome);
                        return;
                    }
                }
                all.complete(null);
            }
        };
        for (CompletableFuture<?> f : futures) {
            f.whenDone(one);
        }
        return all;
    }

    public static CompletableFuture<Object> anyOf(CompletableFuture<?>... futures) {
        final CompletableFuture<Object> any = new CompletableFuture<Object>();
        for (final CompletableFuture<?> f : futures) {
            f.whenDone(new Runnable() {
                @Override
                public void run() {
                    any.relay(f.result.get());
                }
            });
        }
        return any;
    }

    // ------------------------------------------------------------------
    // Time
    // ------------------------------------------------------------------

    private static void later(long millis, final Runnable action) {
        Timer timer = TIMER.get();
        if (timer == null) {
            TIMER.compareAndSet(null, new Timer());
            timer = TIMER.get();
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                action.run();
            }
        }, Math.max(0, millis));
    }

    /// Fails this future with a [TimeoutException] if it is not complete
    /// when the time has passed.
    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
        if (!isDone()) {
            later(unit.toMillis(timeout), new Runnable() {
                @Override
                public void run() {
                    fail(new TimeoutException());
                }
            });
        }
        return this;
    }

    public CompletableFuture<T> completeOnTimeout(final T value, long timeout, TimeUnit unit) {
        if (!isDone()) {
            later(unit.toMillis(timeout), new Runnable() {
                @Override
                public void run() {
                    complete(value);
                }
            });
        }
        return this;
    }

    // ------------------------------------------------------------------
    // Dependent stages
    // ------------------------------------------------------------------

    /// What a dependent stage does with the outcome of this one.
    private interface Step<U> {
        void run(Object outcome, CompletableFuture<U> stage) throws Throwable;
    }

    /// A new stage completed by `step` once this one is complete, on
    /// `executor` when there is one and on the completing thread otherwise.
    private <U> CompletableFuture<U> then(final Executor executor, final Step<U> step) {
        final CompletableFuture<U> stage = newIncompleteFuture();
        whenDone(new Runnable() {
            @Override
            public void run() {
                final Object outcome = result.get();
                Runnable body = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            step.run(outcome, stage);
                        } catch (Throwable t) {
                            stage.fail(wrap(t));
                        }
                    }
                };
                if (executor == null) {
                    body.run();
                    return;
                }
                try {
                    executor.execute(body);
                } catch (Throwable t) {
                    stage.fail(wrap(t));
                }
            }
        });
        return stage;
    }

    private static Executor required(Executor executor) {
        if (executor == null) {
            throw new NullPointerException();
        }
        return executor;
    }

    private <U> CompletableFuture<U> apply(Executor executor, final Function<? super T, ? extends U> fn) {
        if (fn == null) {
            throw new NullPointerException();
        }
        return then(executor, new Step<U>() {
            @Override
            public void run(Object outcome, CompletableFuture<U> stage) {
                if (outcome instanceof Failure) {
                    stage.relay(outcome);
                } else {
                    stage.complete(fn.apply(CompletableFuture.<T>value(outcome)));
                }
            }
        });
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return apply(null, fn);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return apply(pool(), fn);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return apply(required(executor), fn);
    }

    private CompletableFuture<Void> accept(Executor executor, final Consumer<? super T> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        return apply(executor, new Function<T, Void>() {
            @Override
            public Void apply(T value) {
                action.accept(value);
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return accept(null, action);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return accept(pool(), action);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return accept(required(executor), action);
    }

    private CompletableFuture<Void> run(Executor executor, final Runnable action) {
        if (action == null) {
            throw new NullPointerException();
        }
        return apply(executor, new Function<T, Void>() {
            @Override
            public Void apply(T value) {
                action.run();
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> thenRun(Runnable action) {
        return run(null, action);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return run(pool(), action);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return run(required(executor), action);
    }

    private <U> CompletableFuture<U> compose(Executor executor,
            final Function<? super T, ? extends CompletionStage<U>> fn) {
        if (fn == null) {
            throw new NullPointerException();
        }
        return then(executor, new Step<U>() {
            @Override
            public void run(Object outcome, final CompletableFuture<U> stage) {
                if (outcome instanceof Failure) {
                    stage.relay(outcome);
                    return;
                }
                final CompletableFuture<U> inner = fn.apply(CompletableFuture.<T>value(outcome))
                        .toCompletableFuture();
                inner.whenDone(new Runnable() {
                    @Override
                    public void run() {
                        stage.relay(inner.result.get());
                    }
                });
            }
        });
    }

    @Override
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return compose(null, fn);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return compose(pool(), fn);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
            Executor executor) {
        return compose(required(executor), fn);
    }

    private <U> CompletableFuture<U> handled(Executor executor,
            final BiFunction<? super T, Throwable, ? extends U> fn) {
        if (fn == null) {
            throw new NullPointerException();
        }
        return then(executor, new Step<U>() {
            @Override
            public void run(Object outcome, CompletableFuture<U> stage) {
                if (outcome instanceof Failure) {
                    stage.complete(fn.apply(null, ((Failure) outcome).cause));
                } else {
                    stage.complete(fn.apply(CompletableFuture.<T>value(outcome), null));
                }
            }
        });
    }

    @Override
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return handled(null, fn);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return handled(pool(), fn);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn,
            Executor executor) {
        return handled(required(executor), fn);
    }

    private CompletableFuture<T> completed(Executor executor, final BiConsumer<? super T, ? super Throwable> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        return then(executor, new Step<T>() {
            @Override
            public void run(Object outcome, CompletableFuture<T> stage) throws Throwable {
                if (outcome instanceof Failure) {
                    try {
                        action.accept(null, ((Failure) outcome).cause);
                    } catch (Throwable t) {
                        // The stage keeps the failure it was told about.
                        stage.relay(outcome);
                        return;
                    }
                    stage.relay(outcome);
                } else {
                    action.accept(CompletableFuture.<T>value(outcome), null);
                    stage.completeRaw(outcome);
                }
            }
        });
    }

    @Override
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return completed(null, action);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return completed(pool(), action);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action,
            Executor executor) {
        return completed(required(executor), action);
    }

    private CompletableFuture<T> recovered(Executor executor, final Function<Throwable, ? extends T> fn) {
        if (fn == null) {
            throw new NullPointerException();
        }
        return then(executor, new Step<T>() {
            @Override
            public void run(Object outcome, CompletableFuture<T> stage) {
                if (outcome instanceof Failure) {
                    stage.complete(fn.apply(((Failure) outcome).cause));
                } else {
                    stage.completeRaw(outcome);
                }
            }
        });
    }

    @Override
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return recovered(null, fn);
    }

    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn) {
        return recovered(pool(), fn);
    }

    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn, Executor executor) {
        return recovered(required(executor), fn);
    }

    public CompletableFuture<T> exceptionallyCompose(final Function<Throwable, ? extends CompletionStage<T>> fn) {
        if (fn == null) {
            throw new NullPointerException();
        }
        return then(null, new Step<T>() {
            @Override
            public void run(Object outcome, final CompletableFuture<T> stage) {
                if (!(outcome instanceof Failure)) {
                    stage.completeRaw(outcome);
                    return;
                }
                final CompletableFuture<T> inner = fn.apply(((Failure) outcome).cause).toCompletableFuture();
                inner.whenDone(new Runnable() {
                    @Override
                    public void run() {
                        stage.relay(inner.result.get());
                    }
                });
            }
        });
    }

    private <U, V> CompletableFuture<V> both(final Executor executor, CompletionStage<? extends U> other,
            final BiFunction<? super T, ? super U, ? extends V> fn) {
        if (fn == null || other == null) {
            throw new NullPointerException();
        }
        final CompletableFuture<? extends U> second = other.toCompletableFuture();
        final CompletableFuture<V> stage = newIncompleteFuture();
        final AtomicInteger remaining = new AtomicInteger(2);
        Runnable one = new Runnable() {
            @Override
            public void run() {
                if (remaining.decrementAndGet() != 0) {
                    return;
                }
                final Object a = result.get();
                final Object b = second.result.get();
                if (a instanceof Failure) {
                    stage.relay(a);
                    return;
                }
                if (b instanceof Failure) {
                    stage.relay(b);
                    return;
                }
                Runnable body = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            stage.complete(fn.apply(CompletableFuture.<T>value(a), CompletableFuture.<U>value(b)));
                        } catch (Throwable t) {
                            stage.fail(wrap(t));
                        }
                    }
                };
                if (executor == null) {
                    body.run();
                    return;
                }
                try {
                    executor.execute(body);
                } catch (Throwable t) {
                    stage.fail(wrap(t));
                }
            }
        };
        whenDone(one);
        second.whenDone(one);
        return stage;
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        return both(null, other, fn);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        return both(pool(), other, fn);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return both(required(executor), other, fn);
    }

    private <U> CompletableFuture<Void> acceptBoth(Executor executor, CompletionStage<? extends U> other,
            final BiConsumer<? super T, ? super U> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        return both(executor, other, new BiFunction<T, U, Void>() {
            @Override
            public Void apply(T a, U b) {
                action.accept(a, b);
                return null;
            }
        });
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return acceptBoth(null, other, action);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return acceptBoth(pool(), other, action);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action, Executor executor) {
        return acceptBoth(required(executor), other, action);
    }

    private CompletableFuture<Void> runBoth(Executor executor, CompletionStage<?> other, final Runnable action) {
        if (action == null) {
            throw new NullPointerException();
        }
        return both(executor, other, new BiFunction<T, Object, Void>() {
            @Override
            public Void apply(T a, Object b) {
                action.run();
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return runBoth(null, other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return runBoth(pool(), other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return runBoth(required(executor), other, action);
    }

    private <U> CompletableFuture<U> either(final Executor executor, CompletionStage<? extends T> other,
            final Function<? super T, U> fn) {
        if (fn == null || other == null) {
            throw new NullPointerException();
        }
        final CompletableFuture<? extends T> second = other.toCompletableFuture();
        final CompletableFuture<U> stage = newIncompleteFuture();
        final AtomicBoolean taken = new AtomicBoolean();
        final CompletableFuture<T> self = this;
        Runnable first = new Runnable() {
            @Override
            public void run() {
                if (!taken.compareAndSet(false, true)) {
                    return;
                }
                Object mine = self.result.get();
                final Object outcome = mine != null ? mine : second.result.get();
                if (outcome instanceof Failure) {
                    stage.relay(outcome);
                    return;
                }
                Runnable body = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            stage.complete(fn.apply(CompletableFuture.<T>value(outcome)));
                        } catch (Throwable t) {
                            stage.fail(wrap(t));
                        }
                    }
                };
                if (executor == null) {
                    body.run();
                    return;
                }
                try {
                    executor.execute(body);
                } catch (Throwable t) {
                    stage.fail(wrap(t));
                }
            }
        };
        whenDone(first);
        second.whenDone(first);
        return stage;
    }

    @Override
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return either(null, other, fn);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other,
            Function<? super T, U> fn) {
        return either(pool(), other, fn);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn,
            Executor executor) {
        return either(required(executor), other, fn);
    }

    private CompletableFuture<Void> acceptFirst(Executor executor, CompletionStage<? extends T> other,
            final Consumer<? super T> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        return either(executor, other, new Function<T, Void>() {
            @Override
            public Void apply(T value) {
                action.accept(value);
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return acceptFirst(null, other, action);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other,
            Consumer<? super T> action) {
        return acceptFirst(pool(), other, action);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action,
            Executor executor) {
        return acceptFirst(required(executor), other, action);
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Void> runFirst(Executor executor, CompletionStage<?> other, final Runnable action) {
        if (action == null || other == null) {
            throw new NullPointerException();
        }
        // Only that one of the two completed matters, not with what.
        CompletableFuture<Object> mine = thenApply(new Function<T, Object>() {
            @Override
            public Object apply(T value) {
                return value;
            }
        });
        return mine.either(executor, (CompletionStage<Object>) other, new Function<Object, Void>() {
            @Override
            public Void apply(Object value) {
                action.run();
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return runFirst(null, other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return runFirst(pool(), other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action,
            Executor executor) {
        return runFirst(required(executor), other, action);
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    /// A new future that completes as this one does.
    public CompletableFuture<T> copy() {
        return then(null, new Step<T>() {
            @Override
            public void run(Object outcome, CompletableFuture<T> stage) {
                stage.relay(outcome);
            }
        });
    }

    public CompletionStage<T> minimalCompletionStage() {
        return copy();
    }

    @Override
    public String toString() {
        Object outcome = result.get();
        String state = outcome == null ? "Not completed"
                : outcome instanceof Failure ? "Completed exceptionally: " + ((Failure) outcome).cause
                : "Completed normally";
        return "CompletableFuture[" + state + "]";
    }
}
