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

/**
 * Dart's {@code StreamController<T>}. Named constructor parameters
 * ({@code onListen}, {@code onPause}, {@code onResume}, {@code onCancel},
 * {@code sync}) become void setter methods, the stub convention for
 * named-parameter constructors -- so the generated Java is
 * {@code new StreamController(); c.onListen(fn);} rather than a single
 * constructor call.
 *
 * <p>{@link #stream()} always behaves as broadcast (see {@link Stream}'s
 * class comment), so {@link #broadcast()} is the same as the plain
 * constructor here; it exists only so {@code StreamController<T>.broadcast()}
 * call sites compile. All four handlers are called: onListen on the first
 * listener, onCancel when the last one leaves, onPause/onResume as the stream's
 * pause state changes. Each is also readable as a getter, as in Dart.</p>
 *
 * <p>Delivery follows Dart's: a {@code sync: true} controller hands an event to its
 * listeners inside add(); any other queues it and delivers it later, ONE event per
 * microtask (see {@link Microtasks}), so two controllers fed back to back interleave
 * exactly as they do on the Dart VM. A controller that is not broadcast keeps what was
 * added before anyone listened and delivers it to the first listener, as Dart's
 * single-subscription controller does.</p>
 */
public final class StreamController<T> implements StreamSink<T> {

    private Object onListenHandler;
    private Object onCancelHandler;
    private Future<Object> cancelResult;
    private boolean closed;
    private Future<Object> closeFuture;

    private final Stream<T> theStream = new Stream<T>() {
        @Override
        protected void onFirstListener() {
            Stream.deliverVoid(onListenHandler);
            schedulePump();
        }

        @Override
        protected void onNoListeners() {
            Object h = onCancelHandler;
            if (h instanceof dart.runtime.Funcs.Func0 && !(h instanceof dart.runtime.Funcs.VoidFunc0)) {
                // onCancel: () => future -- cancel() completes when it does.
                try {
                    Object r = ((dart.runtime.Funcs.Func0<?>) h).call();
                    if (r instanceof Future) {
                        @SuppressWarnings("unchecked")
                        Future<Object> f = (Future<Object>) r;
                        cancelResult = f;
                    }
                } catch (Throwable t) {
                    Stream.reportUncaught(t);
                }
            } else {
                Stream.deliverVoid(h);
            }
        }

        @Override
        Future<Object> takeCancelResult() {
            Future<Object> r = cancelResult;
            cancelResult = null;
            return r;
        }

        @Override
        protected void onPaused() {
            paused = true;
            Stream.deliverVoid(onPauseHandler);
        }

        @Override
        protected void onResumed() {
            paused = false;
            Stream.deliverVoid(onResumeHandler);
        }
    };

    private Object onPauseHandler;
    private Object onResumeHandler;
    private boolean paused;
    private final Completer<Object> doneCompleter = new Completer<Object>();
    private boolean sync;
    private boolean broadcast;
    /// Events add()/addError()/close() queued for delivery. Headless, an async* body
    /// adds from a thread of its own while the isolate's thread delivers, so the queue
    /// and pumpScheduled are only touched under the lock; delivery runs outside it.
    private final java.util.ArrayDeque<Object[]> outbox = new java.util.ArrayDeque<Object[]>();
    private final Object outboxLock = new Object();
    private boolean pumpScheduled;
    /// An event pumpOne() has taken off the outbox and not finished delivering.
    /// close() from an async* body's own thread (see AsyncStar) read an empty outbox
    /// while the isolate thread was still between the poll and the emit of the last
    /// data event, delivered done first, and that event was lost: take(3) over a
    /// periodic stream answered two elements. Guarded by outboxLock.
    private boolean delivering;
    private static final Integer DATA = Integer.valueOf(0);
    private static final Integer ERROR = Integer.valueOf(1);
    private static final Integer DONE = Integer.valueOf(2);

    private void enqueue(Integer kind, Object value) {
        synchronized (outboxLock) {
            outbox.addLast(new Object[] {kind, value});
        }
        schedulePump();
    }

    private boolean outboxEmpty() {
        synchronized (outboxLock) {
            return outbox.isEmpty() && !delivering;
        }
    }

    private void schedulePump() {
        if (!broadcast && !theStream.hasListeners()) {
            // Single subscription: held for the first listener (onFirstListener).
            return;
        }
        synchronized (outboxLock) {
            if (pumpScheduled || outbox.isEmpty()) {
                return;
            }
            pumpScheduled = true;
        }
        Microtasks.schedule(new Runnable() {
            @Override
            public void run() {
                synchronized (outboxLock) {
                    pumpScheduled = false;
                }
                pumpOne();
            }
        });
    }

    /// Delivers the oldest queued event, then queues itself again behind whatever
    /// else is waiting -- one event per microtask, as Dart delivers them.
    @SuppressWarnings("unchecked")
    private void pumpOne() {
        if (!broadcast && !theStream.hasListeners()) {
            return;
        }
        Object[] e;
        synchronized (outboxLock) {
            e = outbox.pollFirst();
            delivering = e != null;
        }
        if (e == null) {
            return;
        }
        try {
            deliver(e);
        } finally {
            synchronized (outboxLock) {
                delivering = false;
            }
        }
        schedulePump();
    }

    @SuppressWarnings("unchecked")
    private void deliver(Object[] e) {
        if (e[0] == DATA) {
            theStream.emit((T) e[1]);
        } else if (e[0] == ERROR) {
            theStream.emitError(e[1]);
        } else {
            final Future<Object> pending = (Future<Object>) e[1];
            Future<Object> delivered = theStream.emitDone();
            delivered.onComplete(new Runnable() {
                @Override
                public void run() {
                    if (!pending.isDone()) {
                        pending.complete(null);
                    }
                }
            });
        }
    }

    public StreamController() {
    }

    /** Same as the plain constructor -- see the class comment. */
    public static <T> StreamController<T> broadcast() {
        StreamController<T> c = new StreamController<T>();
        c.broadcast = true;
        return c;
    }

    /**
     * Same as the plain constructor -- see the class comment. Takes the stub's three
     * named parameters in canonical positional order ({@code onListen, onCancel, sync}):
     * the stub declares {@code broadcast({onListen, onCancel, sync})}, and a transpiled
     * call with ANY of them given -- {@code StreamController<T>.broadcast(sync: true)},
     * say -- is flattened to all three positionally, nulls filling in whichever were
     * not given. The no-argument {@link #broadcast()} above answers only the
     * zero-argument call and would fail to compile every other one.
     *
     * The handlers are typed as callbacks, not {@code Object}: a method tear-off
     * ({@code broadcast(onListen: this._onListen)}) becomes a Java method reference,
     * which has no target type in an {@code Object} slot and does not compile there.
     */
    public static <T> StreamController<T> broadcast(dart.runtime.Funcs.VoidFunc0 onListen,
            dart.runtime.Funcs.VoidFunc0 onCancel, Boolean sync) {
        StreamController<T> c = new StreamController<T>();
        c.broadcast = true;
        c.sync = sync != null && sync.booleanValue();
        c.onListen((Object) onListen);
        c.onCancel((Object) onCancel);
        return c;
    }

    /// The closure form, so a Dart closure literal has a functional interface to land on.
    public void onListen(dart.runtime.Funcs.VoidFunc0 handler) {
        onListen((Object) handler);
    }

    public void onListen(Object handler) {
        this.onListenHandler = handler;
    }

    /// The closure form, so a Dart closure literal has a functional interface to land on.
    public void onCancel(dart.runtime.Funcs.VoidFunc0 handler) {
        onCancel((Object) handler);
    }

    public void onCancel(Object handler) {
        this.onCancelHandler = handler;
    }

    /// The closure form, so a Dart closure literal has a functional interface to land on.
    public void onPause(dart.runtime.Funcs.VoidFunc0 handler) {
        onPause((Object) handler);
    }

    public void onPause(Object handler) {
        this.onPauseHandler = handler;
    }

    /// The closure form, so a Dart closure literal has a functional interface to land on.
    public void onResume(dart.runtime.Funcs.VoidFunc0 handler) {
        onResume((Object) handler);
    }

    public void onResume(Object handler) {
        this.onResumeHandler = handler;
    }

    /// Dart's {@code onListen} getter: the handler set last, or null.
    public dart.runtime.Funcs.VoidFunc0 onListen() {
        return asVoidFunc(onListenHandler);
    }

    /// Dart's {@code onCancel} getter: the handler set last, or null.
    public dart.runtime.Funcs.VoidFunc0 onCancel() {
        return asVoidFunc(onCancelHandler);
    }

    /// Dart's {@code onPause} getter: the handler set last, or null.
    public dart.runtime.Funcs.VoidFunc0 onPause() {
        return asVoidFunc(onPauseHandler);
    }

    /// Dart's {@code onResume} getter: the handler set last, or null.
    public dart.runtime.Funcs.VoidFunc0 onResume() {
        return asVoidFunc(onResumeHandler);
    }

    private static dart.runtime.Funcs.VoidFunc0 asVoidFunc(final Object handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof dart.runtime.Funcs.VoidFunc0) {
            return (dart.runtime.Funcs.VoidFunc0) handler;
        }
        return new dart.runtime.Funcs.VoidFunc0() {
            @Override
            public void call() {
                Stream.deliverVoid(handler);
            }
        };
    }

    /// {@code sync: true}: deliver inside add() rather than in a later microtask.
    public void sync(boolean v) {
        this.sync = v;
    }

    public Stream<T> stream() {
        return theStream;
    }

    /// The type argument the construction named, held by its stream (which is a
    /// `Stream<X>` of the same X); null when nothing recorded it.
    public dart.runtime.TypeWitness typeArgWitness() {
        return theStream.typeArgWitness();
    }

    /// Records {@code w} as {@code c}'s type argument, and so its stream's, and answers
    /// {@code c}. A null witness records nothing.
    public static <C extends StreamController<?>> C reify(C c, dart.runtime.TypeWitness w) {
        if (c != null) {
            Stream.reify(((StreamController<?>) c).theStream, w);
        }
        return c;
    }

    /** True once any subscriber is listening to {@link #stream()}. */
    public boolean hasListener() {
        return theStream.hasListeners();
    }

    public boolean isClosed() {
        return closed;
    }

    public void add(T event) {
        if (closed) {
            throw new dart.core.StateError("Cannot add event after closing");
        }
        if (sync) {
            theStream.emit(event);
        } else {
            enqueue(DATA, event);
        }
    }

    /// Dart's {@code addError(error, stackTrace)}: the trace travels with the error.
    public void addError(Object error, Object stackTrace) {
        addError(stackTrace == null ? error : dart.core.StackTrace.carry(error, stackTrace));
    }

    public void addError(Object error) {
        if (closed) {
            throw new dart.core.StateError("Cannot add event after closing");
        }
        if (sync) {
            theStream.emitError(error);
        } else {
            enqueue(ERROR, error);
        }
    }

    /**
     * Dart's {@code close()}. Completes once every listener has handled the
     * done event -- immediately when none is paused, which is the common
     * case, but only once a paused listener resumes and drains it (or
     * cancels) otherwise: measured on the Dart 3.9 VM, a paused listener
     * with the done event still queued delays {@code close()}'s own future,
     * it is not handed back already complete.
     */
    public Future<Object> close() {
        if (!closed) {
            closed = true;
            if (sync || outboxEmpty()) {
                // Nothing waits ahead of done: it is delivered now. Events a
                // single-subscription stream holds for its first listener stay ahead
                // of done, as in Dart, and close() completes once they are delivered.
                closeFuture = theStream.emitDone();
            } else {
                closeFuture = new Future<Object>();
                enqueue(DONE, closeFuture);
            }
            closeFuture.onComplete(new Runnable() {
                @Override
                public void run() {
                    if (!doneCompleter.isCompleted()) {
                        doneCompleter.complete(null);
                    }
                }
            });
        }
        return closeFuture;
    }

    /// {@code controller.sink}: the controller itself, as Dart's write-only view.
    public StreamSink<T> sink() {
        return this;
    }

    /// {@code controller.isPaused}: the subscription is paused (or there is none yet).
    public boolean isPaused() {
        return paused || !theStream.hasListeners();
    }

    /// {@code controller.done}: completes once close() has delivered the done event.
    @Override
    public Future<Object> done() {
        return doneCompleter.future();
    }

    /// {@code controller.addStream(source)}: forwards every event of source; the future
    /// completes when source is done.
    /// {@code addStream(source, cancelOnError: ...)}: the flag only matters for a source
    /// that errors, and this controller forwards errors and keeps listening either way.
    public Future<Object> addStream(Stream<T> source, Object cancelOnError) {
        return addStream(source);
    }

    @Override
    public Future<Object> addStream(Stream<T> source) {
        final Future<Object> f = new Future<Object>();
        source.listen(new dart.runtime.Funcs.VoidFunc1<T>() {
            @Override
            public void call(T v) {
                add(v);
            }
        }, new dart.runtime.Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                addError(e);
            }
        }, new dart.runtime.Funcs.VoidFunc0() {
            @Override
            public void call() {
                f.complete(null);
            }
        });
        return f;
    }
}
