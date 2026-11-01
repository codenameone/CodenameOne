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
 * call sites compile. {@code onPause}/{@code onResume} are accepted for
 * source compatibility but never invoked: this controller has no concept
 * of "the whole stream is paused", only a per-subscription pause (see
 * {@link Stream}). {@code sync} is accepted and ignored -- delivery here is
 * always synchronous on the calling thread.</p>
 */
public final class StreamController<T> implements StreamSink<T> {

    private Object onListenHandler;
    private Object onCancelHandler;
    private boolean closed;
    private Future<Object> closeFuture;

    private final Stream<T> theStream = new Stream<T>() {
        @Override
        protected void onFirstListener() {
            Stream.deliverVoid(onListenHandler);
        }

        @Override
        protected void onNoListeners() {
            Stream.deliverVoid(onCancelHandler);
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

    public StreamController() {
    }

    /** Same as the plain constructor -- see the class comment. */
    public static <T> StreamController<T> broadcast() {
        return new StreamController<T>();
    }

    /**
     * Same as the plain constructor -- see the class comment. Takes the stub's three
     * named parameters in canonical positional order ({@code onListen, onCancel, sync}):
     * the stub declares {@code broadcast({onListen, onCancel, sync})}, and a transpiled
     * call with ANY of them given -- {@code StreamController<T>.broadcast(sync: true)},
     * say -- is flattened to all three positionally, nulls filling in whichever were
     * not given. The no-argument {@link #broadcast()} above answers only the
     * zero-argument call and would fail to compile every other one.
     */
    public static <T> StreamController<T> broadcast(Object onListen, Object onCancel, Boolean sync) {
        StreamController<T> c = new StreamController<T>();
        c.onListen(onListen);
        c.onCancel(onCancel);
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

    /** Accepted for source compatibility; never invoked (see the class comment). */
    /// The closure form, so a Dart closure literal has a functional interface to land on.
    public void onPause(dart.runtime.Funcs.VoidFunc0 handler) {
        onPause((Object) handler);
    }

    public void onPause(Object handler) {
        this.onPauseHandler = handler;
    }

    /** Accepted for source compatibility; never invoked (see the class comment). */
    /// The closure form, so a Dart closure literal has a functional interface to land on.
    public void onResume(dart.runtime.Funcs.VoidFunc0 handler) {
        onResume((Object) handler);
    }

    public void onResume(Object handler) {
        this.onResumeHandler = handler;
    }

    /** Accepted for source compatibility; ignored (see the class comment). */
    public void sync(boolean v) {
    }

    public Stream<T> stream() {
        return theStream;
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
        theStream.emit(event);
    }

    public void addError(Object error) {
        if (closed) {
            throw new dart.core.StateError("Cannot add event after closing");
        }
        theStream.emitError(error);
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
            closeFuture = theStream.emitDone();
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
