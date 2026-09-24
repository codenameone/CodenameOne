/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.async;

import dart.core.DartList;
import dart.runtime.DartRuntime;
import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Dart's Future&lt;T&gt; for the M3 blocking-await model: the transpiler
 * lowers {@code await f} to {@link Await#await$(Future)}, which parks the
 * caller (legally, via invokeAndBlock on the EDT) until completion. Async
 * function bodies therefore run to completion synchronously from the
 * caller's perspective; completion ordering of simultaneously resumed
 * awaits is a documented divergence until the M5 CPS backend.
 *
 * <p>Interop: {@link #fromAsyncResource} bridges CN1 async APIs.</p>
 */
public class Future<T> {

    private final Object lock = new Object();
    private boolean done;
    /// Set while this future waits on another it was completed WITH. It counts
    /// as completed for a second complete() -- Dart refuses that too -- but not
    /// yet for listeners, who must see the adopted outcome.
    private boolean adopting;
    private T value;
    private Throwable error;
    private List<Runnable> listeners;

    Future() {
    }

    /**
     * Creates a future already completed with {@code v}. Lets an immediately
     * available subclass in another package (e.g. Flutter foundation's
     * {@code SynchronousFuture}) bridge in, since the no-arg constructor and
     * {@link #complete} are package-private.
     */
    protected Future(T v) {
        complete(v);
    }

    /**
     * The completed value if this future has already resolved synchronously
     * (e.g. a {@code SynchronousFuture}), otherwise null. Lets synchronous
     * consumers such as the Localizations delegate pipeline read the result
     * without parking the EDT.
     */
    public T getNow() {
        synchronized (lock) {
            return done ? value : null;
        }
    }

    /** An already-completed future. */
    public static <T> Future<T> value(T v) {
        Future<T> f = new Future<T>();
        f.complete(v);
        return f;
    }

    /** An already-failed future. */
    public static <T> Future<T> error(Object err) {
        Future<T> f = new Future<T>();
        f.completeError(err);
        return f;
    }

    /**
     * Dart's Future.delayed. With a live CN1 Display the callback fires on
     * the EDT via CN.setTimeout; headless (tests, plain JVM) a short-lived
     * thread sleeps and completes — no daemon flags, so the JVM can exit.
     */
    public static Future<Object> delayed(dart.core.Duration duration) {
        return delayed(duration, (Funcs.Func0<Object>) null);
    }

    /**
     * Future.delayed with a void computation body. Dart's {@code computation}
     * returns {@code FutureOr<T>}; a statement-body closure transpiles to a
     * {@link Funcs.VoidFunc0}, so this overload lets those bind without forcing
     * an artificial return value.
     */
    public static Future<Object> delayed(dart.core.Duration duration, final Funcs.VoidFunc0 computation) {
        return delayed(duration, new Funcs.Func0<Object>() {
            @Override
            public Object call() {
                if (computation != null) {
                    computation.call();
                }
                return null;
            }
        });
    }

    public static Future<Object> delayed(dart.core.Duration duration, final Funcs.Func0<Object> computation) {
        final Future<Object> f = new Future<Object>();
        // Dart treats a negative delay as zero. Passed through, it reached
        // Thread.sleep -- which throws -- on the headless path, so the worker died
        // without completing and an await on the result hung forever; Timer
        // rejects it the same way on the Display path.
        long ms = duration == null ? 0 : Math.max(0L, duration.inMilliseconds());
        final Runnable complete = new Runnable() {
            @Override
            public void run() {
                try {
                    f.complete(computation == null ? null : computation.call());
                } catch (Throwable t) {
                    f.completeError(t);
                }
            }
        };
        if (com.codename1.ui.Display.isInitialized()) {
            Timers.schedule(ms, complete);
        } else {
            final long sleepMs = ms;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ignore) {
                        // fall through and complete anyway
                    }
                    complete.run();
                }
            }, "dart-future-delayed").start();
        }
        return f;
    }

    /**
     * Dart's Future.wait: a future of every value, in input order, once all
     * the inputs have completed -- or, if any failed, of the FIRST error,
     * still only after all of them completed (Dart's default, non-eager
     * behaviour; the later errors are dropped).
     *
     * It returns immediately and settles through listeners. It used to await
     * each input before returning, which blocked the caller -- and when an
     * input failed, the error was thrown from the call itself, so
     * {@code Future.wait(fs).catchError(...)} never got a future to install
     * its handler on.
     */
    public static Future<DartList<Object>> wait(Iterable<? extends Future<?>> futures) {
        final List<Future<?>> inputs = new ArrayList<Future<?>>();
        for (Future<?> f : futures) {
            inputs.add(f);
        }
        final Future<DartList<Object>> result = new Future<DartList<Object>>();
        final int count = inputs.size();
        if (count == 0) {
            result.complete(new DartList<Object>());
            return result;
        }
        final Object[] values = new Object[count];
        // Inputs still pending, and the first error. Guarded by `pending`: on
        // the headless path inputs complete on their own threads.
        final int[] pending = {count};
        final Throwable[] firstError = {null};
        for (int i = 0; i < count; i++) {
            final int index = i;
            final Future<?> f = inputs.get(i);
            f.onComplete(new Runnable() {
                @Override
                public void run() {
                    Throwable error;
                    synchronized (pending) {
                        if (f.error != null) {
                            if (firstError[0] == null) {
                                firstError[0] = f.error;
                            }
                        } else {
                            values[index] = f.value;
                        }
                        if (--pending[0] > 0) {
                            return;
                        }
                        error = firstError[0];
                    }
                    if (error != null) {
                        result.completeError(error);
                        return;
                    }
                    DartList<Object> out = new DartList<Object>();
                    for (Object v : values) {
                        out.add(v);
                    }
                    result.complete(out);
                }
            });
        }
        return result;
    }

    /** Registers a completion callback (fires immediately if already done). */
    public Future<Object> then(final Funcs.Func1<T, Object> onValue) {
        final Future<Object> next = new Future<Object>();
        onComplete(new Runnable() {
            @Override
            public void run() {
                if (error != null) {
                    next.completeError(error);
                    return;
                }
                try {
                    next.complete(onValue.call(value));
                } catch (Throwable t) {
                    next.completeError(t);
                }
            }
        });
        return next;
    }

    /**
     * Dart's {@code then(onValue, onError: handler)}. The handler receives THIS
     * future's error, never one thrown by onValue -- that one fails the returned
     * future, as in Dart. The named onError used to be dropped by the transpiler, so
     * a failure skipped the handler and the chain stayed failed. The handler may take
     * the error, or the error and a stack trace, and may return a value or nothing.
     */
    public Future<Object> then(final Funcs.Func1<T, Object> onValue, final Object onError) {
        if (onError == null) {
            return then(onValue);
        }
        final Future<Object> next = new Future<Object>();
        onComplete(new Runnable() {
            @Override
            public void run() {
                if (error != null) {
                    try {
                        next.complete(callErrorHandler(onError, error));
                    } catch (Throwable t) {
                        next.completeError(t);
                    }
                    return;
                }
                try {
                    next.complete(onValue.call(value));
                } catch (Throwable t) {
                    next.completeError(t);
                }
            }
        });
        return next;
    }

    /** {@link #then(Funcs.Func1, Object)} with a void onValue body. */
    public Future<Object> then(final Funcs.VoidFunc1<T> onValue, final Object onError) {
        return then(new Funcs.Func1<T, Object>() {
            @Override
            public Object call(T v) {
                onValue.call(v);
                return null;
            }
        }, onError);
    }

    /** Invokes an error handler of any shape a Dart handler transpiles to. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object callErrorHandler(Object handler, Object err) {
        if (handler instanceof Funcs.Func1) {
            return ((Funcs.Func1) handler).call(err);
        }
        if (handler instanceof Funcs.VoidFunc1) {
            ((Funcs.VoidFunc1) handler).call(err);
            return null;
        }
        if (handler instanceof Funcs.Func2) {
            return ((Funcs.Func2) handler).call(err, null);
        }
        if (handler instanceof Funcs.VoidFunc2) {
            ((Funcs.VoidFunc2) handler).call(err, null);
            return null;
        }
        throw new dart.core.ArgumentError("onError is not a function");
    }

    /**
     * {@code then} with a void callback body — the common statement-body
     * {@code .then((_) { ... })} shape, which transpiles to a
     * {@link Funcs.VoidFunc1}. Mirrors {@link #then(Funcs.Func1)} but discards
     * the (absent) callback result.
     */
    public Future<Object> then(final Funcs.VoidFunc1<T> onValue) {
        return then(new Funcs.Func1<T, Object>() {
            @Override
            public Object call(T v) {
                onValue.call(v);
                return null;
            }
        });
    }

    /**
     * {@code catchError} with a void handler {@code (error) { ... }}: runs the
     * handler if this future completed with an error, recovering the chain.
     */
    public Future<T> catchError(final Funcs.VoidFunc1<Object> onError) {
        // A NEW future, as Dart returns one: the handler recovers the chain, so
        // `await op.catchError((e) { recover(); })` completes with null rather than
        // rethrowing. Returning this future handed back the original failure, and a
        // handler that threw escaped into listener dispatch instead of failing the
        // chain.
        final Future<T> next = new Future<T>();
        onComplete(new Runnable() {
            @Override
            public void run() {
                if (error == null) {
                    next.complete(value);
                    return;
                }
                try {
                    onError.call(error);
                    next.complete(null);
                } catch (Throwable t) {
                    next.completeError(t);
                }
            }
        });
        return next;
    }

    /**
     * {@code catchError} with a value-returning handler {@code (error) => v}:
     * substitutes the recovery value when this future completed with an error.
     */
    public Future<Object> catchError(final Funcs.Func1<Object, Object> onError) {
        final Future<Object> next = new Future<Object>();
        onComplete(new Runnable() {
            @Override
            public void run() {
                if (error != null) {
                    try {
                        next.complete(onError.call(error));
                    } catch (Throwable t) {
                        next.completeError(t);
                    }
                } else {
                    next.complete(value);
                }
            }
        });
        return next;
    }

    /** {@code catchError(onError, test: ...)} — the optional {@code test} filter is accepted
     *  for API shape (all errors are handled here). Void-handler form. */
    public Future<T> catchError(final Funcs.VoidFunc1<Object> onError, final Object test) {
        final Future<T> next = new Future<T>();
        onComplete(new Runnable() {
            @Override
            public void run() {
                if (error == null) {
                    next.complete(value);
                } else {
                    // The predicate runs inside the guard: when it throws, Dart completes
                    // the returned future with that error. Outside it, the throw escaped
                    // the completing call (or broke listener dispatch) and left this
                    // future pending forever.
                    try {
                        if (!catches(test, error)) {
                            next.completeError(error);
                            return;
                        }
                        onError.call(error);
                        next.complete(null);
                    } catch (Throwable t) {
                        next.completeError(t);
                    }
                }
            }
        });
        return next;
    }

    /** {@code catchError(onError, test: ...)} -- value-handler form. */
    public Future<Object> catchError(final Funcs.Func1<Object, Object> onError, final Object test) {
        final Future<Object> next = new Future<Object>();
        onComplete(new Runnable() {
            @Override
            public void run() {
                if (error == null) {
                    next.complete(value);
                } else {
                    // The predicate runs inside the guard: when it throws, Dart completes
                    // the returned future with that error. Outside it, the throw escaped
                    // the completing call (or broke listener dispatch) and left this
                    // future pending forever.
                    try {
                        if (!catches(test, error)) {
                            next.completeError(error);
                            return;
                        }
                        next.complete(onError.call(error));
                    } catch (Throwable t) {
                        next.completeError(t);
                    }
                }
            }
        });
        return next;
    }

    /// Dart's catchError `test:`: when the predicate rejects the error, the handler
    /// is NOT run and the error continues down the chain unchanged. Both overloads
    /// used to ignore it and recover every failure, so a selective handler --
    /// `test: (e) => e is TimeoutException` -- swallowed unrelated errors and
    /// turned a failed operation into a successful one. No test catches all.
    @SuppressWarnings("unchecked")
    private static boolean catches(Object test, Object err) {
        if (test instanceof Funcs.Func1) {
            Object verdict = ((Funcs.Func1<Object, Object>) test).call(err);
            return Boolean.TRUE.equals(verdict);
        }
        return true;
    }

    public Future<T> whenComplete(final Funcs.VoidFunc0 action) {
        // A NEW future carrying this one's outcome -- unless the action throws, in
        // which case Dart completes it with that error instead. Returning this
        // future reported the old outcome regardless, and the throw either escaped
        // synchronously (already completed) or broke listener dispatch on the
        // completing thread, so later listeners were never told.
        final Future<T> next = new Future<T>();
        onComplete(new Runnable() {
            @Override
            public void run() {
                try {
                    action.call();
                } catch (Throwable t) {
                    next.completeError(t);
                    return;
                }
                if (error != null) {
                    next.completeError(error);
                } else {
                    next.complete(value);
                }
            }
        });
        return next;
    }

    /**
     * {@code whenComplete} whose action may return a value: when that value is a
     * Future, Dart waits for it before forwarding this future's outcome, and a
     * cleanup that fails replaces the outcome with its error. The void overload
     * above discarded it, so {@code whenComplete(() => asyncCleanup())}
     * completed while the cleanup was still running and lost its failure. A
     * separate name rather than an overload, because a lambda that fits both a
     * Func0 and a VoidFunc0 is ambiguous to javac.
     */
    public Future<T> whenCompleteFuture(final Funcs.Func0<?> action) {
        final Future<T> next = new Future<T>();
        onComplete(new Runnable() {
            @Override
            public void run() {
                Object r;
                try {
                    r = action.call();
                } catch (Throwable t) {
                    next.completeError(t);
                    return;
                }
                if (r instanceof Future) {
                    final Future<?> cleanup = (Future<?>) r;
                    cleanup.onComplete(new Runnable() {
                        @Override
                        public void run() {
                            if (cleanup.error != null) {
                                next.completeError(cleanup.error);
                            } else {
                                forwardTo(next);
                            }
                        }
                    });
                    return;
                }
                forwardTo(next);
            }
        });
        return next;
    }

    private void forwardTo(Future<T> next) {
        if (error != null) {
            next.completeError(error);
        } else {
            next.complete(value);
        }
    }

    void onComplete(Runnable r) {
        boolean immediate;
        synchronized (lock) {
            immediate = done;
            if (!done) {
                if (listeners == null) {
                    listeners = new ArrayList<Runnable>();
                }
                listeners.add(r);
            }
        }
        if (immediate) {
            r.run();
        }
    }

    /// Completes with {@code v}, or -- when {@code v} is itself a Future --
    /// with whatever that future completes with.
    ///
    /// Dart types this {@code FutureOr<T>}: a {@code then} callback that returns
    /// a future, a {@code catchError} handler that recovers asynchronously and
    /// {@code Completer.complete(someFuture)} all hand one over, and each must
    /// wait for it and adopt its value OR its error. Storing the future itself
    /// as the value completed {@code await a.then((v) => fetch(v))} at once with
    /// a Future object where the fetched result belonged, and dropped any error
    /// fetch raised. Adopting here covers every caller rather than each in turn.
    void complete(T v) {
        if (v instanceof Future) {
            adopt((Future<?>) v);
            return;
        }
        synchronized (lock) {
            if (done || adopting) {
                throw new dart.core.StateError("Future already completed");
            }
        }
        settle(v);
    }

    private void adopt(final Future<?> source) {
        synchronized (lock) {
            if (done || adopting) {
                throw new dart.core.StateError("Future already completed");
            }
            adopting = true;
        }
        if (source == this) {
            // What Dart reports for a future completed with itself; waiting would
            // simply never finish.
            settleError(new dart.core.TypeError("Chaining cycle detected: a Future was completed with itself"));
            return;
        }
        source.onComplete(new Runnable() {
            @Override
            public void run() {
                Throwable e;
                Object val;
                synchronized (source.lock) {
                    e = source.error;
                    val = source.value;
                }
                if (e != null) {
                    settleError(e);
                } else {
                    @SuppressWarnings("unchecked")
                    T adopted = (T) val;
                    settle(adopted);
                }
            }
        });
    }

    private void settle(T v) {
        List<Runnable> toRun;
        synchronized (lock) {
            if (done) {
                throw new dart.core.StateError("Future already completed");
            }
            done = true;
            adopting = false;
            value = v;
            toRun = listeners;
            listeners = null;
            lock.notifyAll();
        }
        runAll(toRun);
    }

    void completeError(Object err) {
        synchronized (lock) {
            if (done || adopting) {
                throw new dart.core.StateError("Future already completed");
            }
        }
        settleError(err);
    }

    private void settleError(Object err) {
        List<Runnable> toRun;
        synchronized (lock) {
            if (done) {
                throw new dart.core.StateError("Future already completed");
            }
            done = true;
            adopting = false;
            error = err instanceof Throwable ? (Throwable) err : DartRuntime.asError(err);
            toRun = listeners;
            listeners = null;
            lock.notifyAll();
        }
        runAll(toRun);
    }

    private void runAll(List<Runnable> rs) {
        if (rs != null) {
            for (Runnable r : rs) {
                r.run();
            }
        }
    }

    /** True once completed, or once completed WITH a future that has not settled yet. */
    boolean isCompletedOrAdopting() {
        synchronized (lock) {
            return done || adopting;
        }
    }

    boolean isDone() {
        synchronized (lock) {
            return done;
        }
    }

    T valueOrThrow() {
        synchronized (lock) {
            if (error != null) {
                throw error instanceof RuntimeException ? (RuntimeException) error
                        : new RuntimeException(error);
            }
            return value;
        }
    }

    Object monitor() {
        return lock;
    }

    /** Bridge from CN1's AsyncResource. */
    public static <T> Future<T> fromAsyncResource(com.codename1.util.AsyncResource<T> ar) {
        final Future<T> f = new Future<T>();
        ar.ready(new com.codename1.util.SuccessCallback<T>() {
            @Override
            public void onSucess(T v) {
                f.complete(v);
            }
        });
        ar.except(new com.codename1.util.SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable t) {
                f.completeError(t);
            }
        });
        return f;
    }
}
