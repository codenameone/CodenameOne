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
public final class StreamController<T> {

    private Object onListenHandler;
    private Object onCancelHandler;
    private boolean closed;

    private final Stream<T> theStream = new Stream<T>() {
        @Override
        protected void onFirstListener() {
            Stream.deliverVoid(onListenHandler);
        }

        @Override
        protected void onNoListeners() {
            Stream.deliverVoid(onCancelHandler);
        }
    };

    public StreamController() {
    }

    /** Same as the plain constructor -- see the class comment. */
    public static <T> StreamController<T> broadcast() {
        return new StreamController<T>();
    }

    public void onListen(Object handler) {
        this.onListenHandler = handler;
    }

    public void onCancel(Object handler) {
        this.onCancelHandler = handler;
    }

    /** Accepted for source compatibility; never invoked (see the class comment). */
    public void onPause(Object handler) {
    }

    /** Accepted for source compatibility; never invoked (see the class comment). */
    public void onResume(Object handler) {
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

    /** Dart's {@code close()}: returns an already-completed {@code Future<void>}. */
    public Future<Object> close() {
        if (!closed) {
            closed = true;
            theStream.emitDone();
        }
        return Future.value(null);
    }
}
