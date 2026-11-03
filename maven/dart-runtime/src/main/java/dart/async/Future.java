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

    /// The type argument the construction named (`Future<int>.value(1)` is a
    /// `Future<int>`), which `is Future<X>` and `as Future<X>` read; null when nothing
    /// recorded it, which those tests take as unknown.
    private dart.runtime.TypeWitness typeArg;

    /// The recorded type argument, or null.
    public final dart.runtime.TypeWitness typeArgWitness() {
        return typeArg;
    }

    /// Records {@code w} as {@code f}'s type argument and answers {@code f}; the emitter
    /// wraps a construction whose type argument it knows. A null witness records nothing.
    /// Records {@code w} as {@code f}'s type argument unless one is recorded already, and
    /// answers it. The emitter calls it on the result of a call whose static type is
    /// Future<X>: a future a member or a function value made without recording X takes the
    /// declared X, and one that recorded its own (narrower) X keeps it.
    public static <F extends Future<?>> F reifyUnknown(F f, dart.runtime.TypeWitness w) {
        if (f != null && w != null && ((Future<?>) f).typeArg == null) {
            ((Future<?>) f).typeArg = w;
        }
        return f;
    }

    public static <F extends Future<?>> F reify(F f, dart.runtime.TypeWitness w) {
        if (f != null && w != null) {
            ((Future<?>) f).typeArg = w;
        }
        return f;
    }

    private final Object lock = new Object();
    private boolean done;
    /// Set while this future waits on another it was completed WITH. It counts
    /// as completed for a second complete() -- Dart refuses that too -- but not
    /// yet for listeners, who must see the adopted outcome.
    private boolean adopting;
    private T value;
    private Throwable error;
    private List<Runnable> listeners;
    /// The zone this future was created in: an error nothing listens for is its
    /// uncaught error, as Dart reports it.
    private final Zone zone = Zone.current;
    /// Set once anything listened to, awaited or read this future's outcome.
    private boolean errorHandled;

    /// Set once cn1Drive() has run, so a program subclass is driven at most once.
    private boolean driven;
    /// A future completed the way Dart's StreamIterator completes moveNext() (an
    /// internal synchronous completion): an await on it resumes inside the completion
    /// rather than a microtask later.
    boolean resumeInline;

    /// Also the constructor of a program class declared {@code implements Future<T>}:
    /// it extends this class, and its inherited state is completed by {@link #cn1Drive}.
    protected Future() {
    }

    /// Completes a program subclass's inherited state from its own then(); see
    /// {@code JavaEmitter.implementsRuntimeFuture}. The runtime's futures complete
    /// themselves, so the default does nothing.
    protected void cn1Drive() {
    }

    /// Adopts {@code source}'s value or error, for {@link #cn1Drive} overrides.
    protected final void cn1Adopt(Future<?> source) {
        adopt(source);
    }

    /// Runs cn1Drive() the first time this future is listened to or awaited.
    final void driveOnce() {
        synchronized (lock) {
            if (driven || done || adopting) {
                return;
            }
            driven = true;
        }
        cn1Drive();
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

    /**
     * The value of a future that has already completed with one, else null. Lets Java
     * code act on a {@code FutureOr} answer that is ready now without waiting for it.
     */
    /** Whether this future has already completed, with a value or an error (Java side only). */
    public boolean completedNow() {
        synchronized (lock) {
            return done;
        }
    }

    public T completedValue() {
        synchronized (lock) {
            return done && error == null ? value : null;
        }
    }

    /** The error of a future that has already failed, else null (Java side only). */
    public Throwable completedError() {
        synchronized (lock) {
            errorHandled = true;
            return done ? error : null;
        }
    }

    /// Dart's {@code Future.any}: completes with the outcome of the first of
    /// {@code futures} to complete, value or error; the later ones are ignored.
    public static <T> Future<T> any(Iterable<? extends Future<? extends T>> futures) {
        final Future<T> result = new Future<T>();
        final boolean[] settled = new boolean[1];
        for (Future<? extends T> f : futures) {
            @SuppressWarnings("unchecked")
            Future<T> ft = (Future<T>) f;
            ft.then(new Funcs.Func1<T, Object>() {
                public Object call(T v) {
                    if (!settled[0]) {
                        settled[0] = true;
                        result.complete(v);
                    }
                    return null;
                }
            }).catchError(new Funcs.VoidFunc1<Object>() {
                public void call(Object e) {
                    if (!settled[0]) {
                        settled[0] = true;
                        result.completeError(e);
                    }
                }
            });
        }
        return result;
    }

    /** An already-completed future. */
    public static <T> Future<T> value(T v) {
        Future<T> f = new Future<T>();
        f.complete(v);
        return f;
    }

    /** Dart's {@code Future.error(error, stackTrace)}: the trace travels with the error. */
    public static <T> Future<T> error(Object err, Object stackTrace) {
        Future<T> f = new Future<T>();
        f.completeError(dart.core.StackTrace.carry(err, stackTrace));
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

    /**
     * Dart's unnamed {@code Future(computation)} constructor: the computation runs
     * in a later timer event, as {@code Future.delayed(Duration.zero, computation)}
     * does. A computation that returns a future makes this one follow it, and one
     * that throws completes it with that error.
     */
    @SuppressWarnings("unchecked")
    public static <T> Future<T> create(final Funcs.Func0<? extends Object> computation) {
        return (Future<T>) (Future<?>) delayed(dart.core.Duration.zero, new Funcs.Func0<Object>() {
            @Override
            public Object call() {
                return computation.call();
            }
        });
    }

    /** {@link #create(Funcs.Func0)} for a computation with a statement body. */
    public static <T> Future<T> create(final Funcs.VoidFunc0 computation) {
        return create(new Funcs.Func0<Object>() {
            @Override
            public Object call() {
                computation.call();
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
        final Runnable complete = Zone.capture(new Runnable() {
            @Override
            public void run() {
                try {
                    f.complete(computation == null ? null : computation.call());
                } catch (Throwable t) {
                    f.completeError(t);
                }
                // The end of a timer turn (see Microtasks).
                Microtasks.drain();
            }
        });
        if (com.codename1.ui.Display.isInitialized()) {
            Timers.schedule(ms, complete);
        } else {
            final long sleepMs = ms;
            // The thread only waits out the delay; completion is posted to the isolate's
            // thread like any timer event (see Microtasks), and the program cannot end
            // while it is pending.
            Microtasks.timerStarted();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ignore) {
                        // fall through and complete anyway
                    }
                    Microtasks.postTimerEvent(complete);
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
    /**
     * The result of {@link #wait} with its list reified at the element type Dart
     * infers from the inputs ({@code Future.wait(<Future<int>>[..])} completes with a
     * {@code List<int>}); the values themselves are unchanged.
     */
    public static Future<DartList<Object>> typedList(Future<DartList<Object>> f,
            final dart.runtime.TypeWitness elem) {
        return f.<DartList<Object>>then(new Funcs.Func1<DartList<Object>, Object>() {
            @Override
            public Object call(DartList<Object> l) {
                return DartList.typed(l, elem);
            }
        });
    }

    public static Future<DartList<Object>> wait(Iterable<?> futures) {
        return wait(futures, false, null);
    }

    /**
     * Future.wait with its named arguments, which the transpiler used to drop.
     * {@code eagerError} fails the result at the FIRST error instead of waiting for
     * every input -- without it, an error beside an input that never completes left
     * the result pending forever. {@code cleanUp}, once any input has failed, receives
     * every non-null successful value: the ones already in and the ones that arrive
     * later, as Dart hands them over.
     */
    /// Takes any Iterable: a Dart {@code List<Future>} built from dynamic values is a
    /// {@code DartList<Object>} in Java. An element that is not a future fails the result
    /// with a TypeError, as Dart's cast does.
    public static Future<DartList<Object>> wait(Iterable<?> futures,
                                                final boolean eagerError,
                                                final Funcs.VoidFunc1<Object> cleanUp) {
        final Future<DartList<Object>> result = new Future<DartList<Object>>();
        final List<Future<?>> inputs = new ArrayList<Future<?>>();
        try {
            for (Object f : futures) {
                if (!(f instanceof Future)) {
                    throw new dart.core.TypeError("Future.wait element " + f + " is not a Future");
                }
                inputs.add((Future<?>) f);
            }
        } catch (Throwable t) {
            // Dart returns a future failed with the traversal's error; letting it escape
            // made Future.wait(bad).catchError(...) throw from the call itself.
            result.completeError(t);
            return result;
        }
        final int count = inputs.size();
        if (count == 0) {
            result.complete(new DartList<Object>(dart.runtime.TypeWitness.DYNAMIC));
            return result;
        }
        final Object[] values = new Object[count];
        final boolean[] arrived = new boolean[count];
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
                    boolean last;
                    boolean failNow = false;
                    List<Object> toClean = new ArrayList<Object>();
                    synchronized (pending) {
                        if (f.error != null) {
                            if (firstError[0] == null) {
                                firstError[0] = f.error;
                                failNow = eagerError;
                                // The values already in are the application's to release.
                                for (int j = 0; j < count; j++) {
                                    if (arrived[j] && values[j] != null) {
                                        toClean.add(values[j]);
                                    }
                                }
                            }
                        } else if (firstError[0] != null) {
                            if (f.value != null) {
                                toClean.add(f.value);
                            }
                        } else {
                            values[index] = f.value;
                            arrived[index] = true;
                        }
                        last = --pending[0] == 0;
                        error = firstError[0];
                    }
                    if (cleanUp != null) {
                        for (Object v : toClean) {
                            try {
                                cleanUp.call(v);
                            } catch (Throwable t) {
                                // Uncaught in Dart too: nothing awaits a cleanUp callback.
                                com.codename1.io.Log.e(t);
                            }
                        }
                    }
                    if (failNow) {
                        result.completeError(error);
                        return;
                    }
                    if (!last || (error != null && eagerError)) {
                        return;
                    }
                    if (error != null) {
                        result.completeError(error);
                        return;
                    }
                    DartList<Object> out = new DartList<Object>(dart.runtime.TypeWitness.DYNAMIC);
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <S> Future<S> then(final Funcs.Func1<T, Object> onValue) {
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
        return (Future<S>) (Future) next;
    }

    /**
     * Dart's {@code then(onValue, onError: handler)}. The handler receives THIS
     * future's error, never one thrown by onValue -- that one fails the returned
     * future, as in Dart. The named onError used to be dropped by the transpiler, so
     * a failure skipped the handler and the chain stayed failed. The handler may take
     * the error, or the error and a stack trace, and may return a value or nothing.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <S> Future<S> then(final Funcs.Func1<T, Object> onValue, final Object onError) {
        if (onError == null) {
            return (Future<S>) (Future) then(onValue);
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
        return (Future<S>) (Future) next;
    }

    /** {@link #then(Funcs.Func1, Object)} with a void onValue body. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <S> Future<S> then(final Funcs.VoidFunc1<T> onValue, final Object onError) {
        return (Future<S>) (Future) then(new Funcs.Func1<T, Object>() {
            @Override
            public Object call(T v) {
                onValue.call(v);
                return null;
            }
        }, onError);
    }

    /**
     * Dart's {@code onError<E>(handleError, {test})}: like {@code catchError}, but the
     * handler takes {@code (error, stackTrace)}. E is erased, so every error matches the
     * type filter; {@code test}, when it is a function, still decides.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Future<T> onError(final Object handleError, final Object test) {
        final Future<T> next = new Future<T>();
        onComplete(new Runnable() {
            @Override
            public void run() {
                if (error == null) {
                    next.complete(value);
                    return;
                }
                Object err = DartRuntime.caught(error);
                try {
                    if (test instanceof Funcs.Func1 && !Boolean.TRUE.equals(((Funcs.Func1) test).call(err))) {
                        next.completeError(error);
                        return;
                    }
                    next.complete((T) callErrorHandler(handleError, error));
                } catch (Throwable t) {
                    next.completeError(t);
                }
            }
        });
        return next;
    }

    /** {@code onError((error, stackTrace) => ...)}: the lambda form of the handler. */
    public Future<T> onError(final Funcs.Func2<Object, Object, Object> handleError, final Object test) {
        return onError((Object) handleError, test);
    }

    public Future<T> onError(final Funcs.Func2<Object, Object, Object> handleError) {
        return onError((Object) handleError, null);
    }

    /** {@code onError(handleError)} with no test. */
    public Future<T> onError(final Object handleError) {
        return onError(handleError, null);
    }

    /** Invokes an error handler of any shape a Dart handler transpiles to. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object callErrorHandler(Object handler, Object thrown) {
        // The handler gets what was thrown, not the carrier: Future.error('x') hands it 'x'.
        Object err = thrown instanceof Throwable ? DartRuntime.caught((Throwable) thrown) : thrown;
        if (handler instanceof Funcs.Func1) {
            return ((Funcs.Func1) handler).call(err);
        }
        if (handler instanceof Funcs.VoidFunc1) {
            ((Funcs.VoidFunc1) handler).call(err);
            return null;
        }
        if (handler instanceof Funcs.Func2) {
            return ((Funcs.Func2) handler).call(err, dart.core.StackTrace.forError(thrown));
        }
        if (handler instanceof Funcs.VoidFunc2) {
            ((Funcs.VoidFunc2) handler).call(err, dart.core.StackTrace.forError(thrown));
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <S> Future<S> then(final Funcs.VoidFunc1<T> onValue) {
        return (Future<S>) (Future) then(new Funcs.Func1<T, Object>() {
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
                    onError.call(DartRuntime.caught(error));
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
                        next.complete(recoveryValue(onError.call(DartRuntime.caught(error))));
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
                        if (!catches(test, DartRuntime.caught(error))) {
                            next.completeError(error);
                            return;
                        }
                        onError.call(DartRuntime.caught(error));
                        next.complete(null);
                    } catch (Throwable t) {
                        next.completeError(t);
                    }
                }
            }
        });
        return next;
    }

    /// {@code catchError((e, st) => ...)}: a handler that also takes the trace -- the one
    /// this future's error carries (the handler runs once it has failed).
    public Future<Object> catchError(final Funcs.Func2<Object, ? super dart.core.StackTrace, ?> onError,
            final Object test) {
        return catchError(new Funcs.Func1<Object, Object>() {
            @Override
            public Object call(Object e) {
                return onError.call(e, dart.core.StackTrace.forError(completedError()));
            }
        }, test);
    }

    public Future<Object> catchError(final Funcs.Func2<Object, ? super dart.core.StackTrace, ?> onError) {
        return catchError(onError, (Object) null);
    }

    /// {@code catchError((e, st) { ... })} with a void handler.
    public Future<T> catchError(final Funcs.VoidFunc2<Object, ? super dart.core.StackTrace> onError,
            final Object test) {
        return catchError(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                onError.call(e, dart.core.StackTrace.forError(completedError()));
            }
        }, test);
    }

    public Future<T> catchError(final Funcs.VoidFunc2<Object, ? super dart.core.StackTrace> onError) {
        return catchError(onError, (Object) null);
    }

    /// A value-returning catchError handler's result, checked against this future's
    /// recorded T: Dart's `Future<int>.error(e).catchError((e) => 'x')` fails with an
    /// ArgumentError, since the recovered future must still complete with a T. A future
    /// that recorded no T accepts any value, as before.
    private Object recoveryValue(Object v) {
        dart.runtime.TypeWitness w = typeArg;
        if (w != null && !w.test(v)) {
            throw dart.core.ArgumentError.value(v, "onError",
                    "The error handler of Future.catchError must return a value of the future's type");
        }
        return v;
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
                        if (!catches(test, DartRuntime.caught(error))) {
                            next.completeError(error);
                            return;
                        }
                        next.complete(recoveryValue(onError.call(DartRuntime.caught(error))));
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

    /// Registers a completion listener. As in Dart, a listener added to a future that has
    /// already completed does not run inside the call: it is queued on the microtask queue,
    /// so {@code Future.value(1).then(f); print('sync');} prints sync first. A listener
    /// added before completion runs when the future settles.
    void onComplete(Runnable r) {
        // A listener runs in the zone it was registered in, not the one that
        // happens to complete the future.
        r = Zone.capture(r);
        driveOnce();
        boolean immediate;
        synchronized (lock) {
            immediate = done;
            errorHandled = true;
            if (!done) {
                if (listeners == null) {
                    listeners = new ArrayList<Runnable>();
                }
                listeners.add(r);
            }
        }
        if (immediate) {
            Microtasks.schedule(r);
        }
    }

    /// The future an {@code await v} waits on: v itself when it is a Future, else a
    /// future already completed with v ({@code await 42}, {@code await null}).
    @SuppressWarnings("unchecked")
    public static Future<Object> awaitable(Object v) {
        if (v instanceof Future) {
            return (Future<Object>) v;
        }
        Future<Object> f = new Future<Object>();
        f.settle(v);
        return f;
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
        if (toRun == null && zone != Zone.root) {
            reportIfUnhandled();
        }
    }

    /// Dart hands an error no one has listened for by the next turn of the event loop to
    /// the future's zone. Only a zone with its own handler is told: the root zone's
    /// report would be a log line for every future this runtime's blocking await
    /// model reads without listening.
    private void reportIfUnhandled() {
        final Zone z = zone;
        new Timer(null, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                Throwable e;
                synchronized (lock) {
                    if (errorHandled) {
                        return;
                    }
                    errorHandled = true;
                    e = error;
                }
                z.handleUncaughtError(DartRuntime.caught(e), Zone.stackOf(e));
            }
        });
    }

    void markHandled() {
        synchronized (lock) {
            errorHandled = true;
        }
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
                if (error instanceof Error) {
                    throw (Error) error;   // an await rethrows an Error as itself
                }
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

    /// {@code future.timeout(limit)}: a TimeoutException when this has not completed in time.
    public Future<T> timeout(dart.core.Duration limit) {
        return timeout(limit, null);
    }

    /// {@code future.timeout(limit, onTimeout: f)}: f's value (or the future it returns)
    /// when this has not completed in time.
    @SuppressWarnings("unchecked")
    public Future<T> timeout(final dart.core.Duration limit, final Funcs.Func0<Object> onTimeout) {
        final Future<T> out = new Future<T>();
        final boolean[] settled = new boolean[1];
        final Timer timer = new Timer(limit, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (settled[0]) {
                    return;
                }
                settled[0] = true;
                if (onTimeout == null) {
                    out.completeError(new TimeoutException("Future not completed", limit));
                    return;
                }
                Object v;
                try {
                    v = onTimeout.call();
                } catch (Throwable t) {
                    out.completeError(t);
                    return;
                }
                if (v instanceof Future) {
                    final Future<T> f = (Future<T>) v;
                    f.onComplete(new Runnable() {
                        @Override
                        public void run() {
                            if (f.completedError() != null) {
                                out.completeError(f.completedError());
                            } else {
                                out.complete(f.completedValue());
                            }
                        }
                    });
                } else {
                    out.complete((T) v);
                }
            }
        });
        onComplete(new Runnable() {
            @Override
            public void run() {
                if (settled[0]) {
                    return;
                }
                settled[0] = true;
                timer.cancel();
                if (completedError() != null) {
                    out.completeError(completedError());
                } else {
                    out.complete(completedValue());
                }
            }
        });
        return out;
    }

    /// {@code future.ignore()}: its error, if any, is dropped rather than reported.
    public void ignore() {
        onComplete(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    /// {@code future.asStream()}: a single-subscription stream of this one value (or error).
    public Stream<T> asStream() {
        final StreamController<T> c = new StreamController<T>();
        c.onListen(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                onComplete(new Runnable() {
                    @Override
                    public void run() {
                        if (completedError() != null) {
                            c.addError(completedError());
                        } else {
                            c.add(completedValue());
                        }
                        c.close();
                    }
                });
            }
        });
        return c.stream();
    }
}
