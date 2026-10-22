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

import dart.runtime.DartRuntime;
import dart.runtime.Funcs;

/**
 * Runtime support for {@code async*} generator bodies.
 *
 * <p>The body is lowered to a method that takes the generator's {@link Sink}:
 * {@code yield v} is {@code sink.yield$(v)} and {@code yield* s} is
 * {@code sink.yieldAll(s)}. As in Dart, the body starts only when the stream is
 * listened to, and it runs on the event loop, so an {@code await} inside it parks
 * through {@link Await} like any other async body. When the listener cancels, the
 * next {@code yield} ends the body the way Dart's does: {@code finally} blocks run
 * and nothing more is delivered.</p>
 *
 * <p>Pause is not modelled: a paused subscription still receives events, which its
 * subscription buffers.</p>
 */
public final class AsyncStar {

    private AsyncStar() {
    }

    /** Thrown out of a yield after the listener cancelled; ends the body silently. */
    static final class Cancelled extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Cancelled() {
            super(null, null, false, false);
        }
    }

    /** The yield target an async* body writes to. */
    public static final class Sink<T> {
        final StreamController<T> controller = new StreamController<T>();
        volatile boolean cancelled;
        private final java.util.List<Funcs.VoidFunc0> cancelHooks = new java.util.ArrayList<Funcs.VoidFunc0>();

        /// Runs {@code hook} when the listener cancels: a body blocked on an upstream
        /// source releases it then, not at the source's next event.
        public void onCancel(Funcs.VoidFunc0 hook) {
            // One lock with the cancel handler: a cancel between the check and the add
            // left the hook unrun, and the body blocked on a quiet source forever.
            boolean now;
            synchronized (cancelHooks) {
                now = cancelled;
                if (!now) {
                    cancelHooks.add(hook);
                }
            }
            if (now) {
                hook.call();
            }
        }

        /// An error event that does not end the stream, as a caught error in an
        /// async* body re-raised to the listener would not.
        public void addError(Object error) {
            if (cancelled) {
                throw new Cancelled();
            }
            controller.addError(error instanceof Throwable ? DartRuntime.caught((Throwable) error) : error);
        }

        public void yield$(T value) {
            if (cancelled) {
                throw new Cancelled();
            }
            controller.add(value);
        }

        public void yieldAll(Stream<? extends T> source) {
            if (source == null) {
                return;
            }
            // yield* forwards the source's errors and keeps delegating until it is done,
            // as Dart does; a plain (await for) iterator would end the generator at the
            // first error. Cancelling the generator cancels the source and wakes a body
            // waiting on it, so the finally below (and the body's own cleanup) runs.
            @SuppressWarnings("unchecked")
            final StreamIterator<T> it = new StreamIterator<T>((Stream<T>) source, new Funcs.VoidFunc1<Object>() {
                @Override
                public void call(Object e) {
                    if (!cancelled) {
                        addError(e);
                    }
                }
            });
            onCancel(new Funcs.VoidFunc0() {
                @Override
                public void call() {
                    it.cancel();
                }
            });
            try {
                while (it.next()) {
                    yield$(it.current());
                }
            } finally {
                it.cancel();
            }
            if (cancelled) {
                throw new Cancelled();
            }
        }
    }

    public static <T> Stream<T> asyncStar(final Funcs.VoidFunc1<Sink<T>> body) {
        return asyncStar(body, null);
    }

    /// A stream transformation: {@code body} reads {@code source} through an iterator
    /// and yields. The iterator subscribes inside the downstream listen(), so an event
    /// the source emits right after it is buffered rather than lost to a broadcast
    /// stream, and source errors are re-emitted in order while the body keeps reading,
    /// as Dart's transformers (take, LineSplitter, utf8.decoder...) do.
    public static <T, R> Stream<R> over(final Stream<T> source,
            final Funcs.VoidFunc2<StreamIterator<T>, Sink<R>> body) {
        final Object[] sinkRef = new Object[1];
        final StreamIterator<T> it = new StreamIterator<T>(source, new Funcs.VoidFunc1<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public void call(Object e) {
                Object sk = sinkRef[0];
                if (sk instanceof Sink) {
                    ((Sink<R>) sk).addError(e);
                }
            }
        });
        return asyncStar(new Funcs.VoidFunc1<Sink<R>>() {
            @Override
            public void call(final Sink<R> s) {
                sinkRef[0] = s;
                s.onCancel(new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        it.cancel();
                    }
                });
                try {
                    body.call(it, s);
                } finally {
                    it.cancel();
                }
            }
        }, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                it.start();
            }
        });
    }

    /// {@code onListen} runs synchronously inside listen(), before the body's timer: a
    /// derived stream subscribes to its source there, so an event the source emits right
    /// after the downstream listen() is buffered rather than lost to a broadcast stream.
    static <T> Stream<T> asyncStar(final Funcs.VoidFunc1<Sink<T>> body, final Funcs.VoidFunc0 onListen) {
        final Sink<T> sink = new Sink<T>();
        sink.controller.onListen(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (onListen != null) {
                    onListen.call();
                }
                // Started from a timer, never inside listen() itself, as Dart does.
                new Timer(dart.core.Duration.zero, new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        run(body, sink);
                    }
                });
            }
        });
        sink.controller.onCancel(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                Funcs.VoidFunc0[] hooks;
                synchronized (sink.cancelHooks) {
                    sink.cancelled = true;
                    hooks = sink.cancelHooks.toArray(new Funcs.VoidFunc0[sink.cancelHooks.size()]);
                    sink.cancelHooks.clear();
                }
                for (Funcs.VoidFunc0 h : hooks) {
                    h.call();
                }
            }
        });
        return sink.controller.stream();
    }

    private static <T> void run(Funcs.VoidFunc1<Sink<T>> body, Sink<T> sink) {
        try {
            body.call(sink);
        } catch (Cancelled c) {
            // the listener went away; nothing is left to deliver to
        } catch (Throwable t) {
            if (!sink.cancelled && !sink.controller.isClosed()) {
                sink.controller.addError(DartRuntime.caught(t));
            }
        }
        if (!sink.controller.isClosed()) {
            sink.controller.close();
        }
    }
}
