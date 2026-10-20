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

import dart.runtime.Funcs;

import java.util.ArrayDeque;

/**
 * Dart's {@code StreamIterator}, and the lowering target of {@code await for}.
 *
 * <p>{@code await for (x in s) body} becomes a loop over {@link #next()}, which parks
 * the current frame the way {@link Await#await$} does until the stream delivers an
 * event, finishes or fails. Events that arrive while the loop body runs are queued, so
 * none is lost. Leaving the loop by any path calls {@link #cancel()} in a finally block,
 * which cancels the subscription as Dart does on {@code break} or an exception.</p>
 */
public final class StreamIterator<T> {

    private static final Object DONE = new Object();
    /** Stands in for a null event: ArrayDeque rejects null, and a stream may carry one. */
    private static final Object NULL = new Object();

    private final Stream<T> stream;
    private final ArrayDeque<Object> queue = new ArrayDeque<Object>();
    private StreamSubscription<T> sub;
    private Completer<Boolean> waiting;
    private volatile T current;
    private boolean finished;
    private Object error;
    private boolean hasError;

    /// Set for a derived stream (Stream.derive): source errors go here, in order, and the
    /// iterator stays subscribed. Dart's StreamIterator ends at an error, which is right
    /// for await for, but take/skip/expand/asyncMap forward an error and carry on.
    private final Funcs.VoidFunc1<Object> forward;

    /** A forwarded error waiting in the queue, in order with the data around it. */
    private static final class Err {
        final Object error;

        Err(Object error) {
            this.error = error;
        }
    }

    public StreamIterator(Stream<T> stream) {
        this(stream, null);
    }

    StreamIterator(Stream<T> stream, Funcs.VoidFunc1<Object> forward) {
        this.stream = stream;
        this.forward = forward;
    }

    public T current() {
        return current;
    }

    /** Dart's {@code moveNext()}: completes with false once the stream is done. */
    public Future<Boolean> moveNext() {
        boolean start;
        synchronized (this) {
            start = sub == null && !finished;
        }
        if (start) {
            subscribe();
        }
        Completer<Boolean> c = new Completer<Boolean>();
        Object outcome;
        java.util.List<Object> errs = forward == null ? null : new java.util.ArrayList<Object>();
        // Checking the queue and publishing the waiter is one step: an async* body
        // pushes from a timer thread, and an event landing between the two was lost,
        // leaving next() parked forever (a rare hang in lang_async_star under load).
        synchronized (this) {
            outcome = take(errs);
            if (outcome == NOTHING) {
                waiting = c;
            }
        }
        forwardAll(errs);
        settle(c, outcome);
        return c.future();
    }

    /** The blocking step {@code await for} is lowered to. */
    public boolean next() {
        Boolean more = Await.await$(moveNext());
        return more != null && more.booleanValue();
    }

    public Future<Object> cancel() {
        Completer<Boolean> w;
        StreamSubscription<T> s;
        synchronized (this) {
            finished = true;
            queue.clear();
            // A moveNext still pending ends now, so a body blocked in next() -- a derived
            // stream whose listener cancelled -- unblocks instead of waiting for an event.
            w = waiting;
            waiting = null;
            s = sub;
            sub = null;
        }
        if (w != null && !w.isCompleted()) {
            w.complete(Boolean.FALSE);
        }
        if (s != null) {
            return s.cancel();
        }
        return Future.value(null);
    }

    private void subscribe() {
        StreamSubscription<T> created = stream.listen(new Funcs.VoidFunc1<T>() {
            @Override
            public void call(T v) {
                push(v);
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                if (forward != null) {
                    push(new Err(e));
                    return;
                }
                synchronized (StreamIterator.this) {
                    if (!hasError) {
                        hasError = true;
                        error = e;
                    }
                }
                push(DONE);
            }
        }, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                push(DONE);
            }
            // cancelOnError only for await for; a forwarding iterator outlives an error
        }, Boolean.valueOf(forward == null));
        boolean stale;
        synchronized (this) {
            stale = finished;
            if (!stale) {
                sub = created;
            }
        }
        if (stale) {
            // cancelled, or already done, while listen ran: nothing should stay subscribed
            created.cancel();
        }
    }

    private void push(Object event) {
        Completer<Boolean> c;
        Object outcome;
        java.util.List<Object> errs = forward == null ? null : new java.util.ArrayList<Object>();
        synchronized (this) {
            queue.addLast(event == null ? NULL : event);
            c = waiting;
            if (c == null) {
                return;
            }
            outcome = take(errs);
            if (outcome != NOTHING) {
                waiting = null;
            }
        }
        forwardAll(errs);
        settle(c, outcome);
    }

    /** No event yet: the caller keeps waiting. */
    private static final Object NOTHING = new Object();
    /** The stream ended (or was cancelled): moveNext answers false. */
    private static final Object END = new Object();

    /// The next outcome for a moveNext, taken off the queue. Called holding the lock;
    /// the completer is settled after it is released, since completing runs listeners.
    @SuppressWarnings("unchecked")
    private Object take(java.util.List<Object> errs) {
        while (errs != null && !queue.isEmpty() && queue.peekFirst() instanceof Err) {
            errs.add(((Err) queue.removeFirst()).error);
        }
        if (queue.isEmpty()) {
            return finished ? END : NOTHING;
        }
        Object e = queue.removeFirst();
        if (e == DONE) {
            finished = true;
            queue.clear();
            sub = null;
            if (hasError) {
                hasError = false;
                Object err = error;
                return new Object[] {err};
            }
            return END;
        }
        current = e == NULL ? null : (T) e;
        return Boolean.TRUE;
    }

    /// Hands drained errors to the derived stream, outside the lock and before the
    /// event that followed them settles the waiter, so the order is the source's.
    private void forwardAll(java.util.List<Object> errs) {
        if (errs != null) {
            for (Object e : errs) {
                forward.call(e);
            }
        }
    }

    private static void settle(Completer<Boolean> c, Object outcome) {
        if (outcome == NOTHING) {
            return;
        }
        if (outcome == END) {
            c.complete(Boolean.FALSE);
        } else if (outcome instanceof Object[]) {
            c.completeError(((Object[]) outcome)[0]);
        } else {
            c.complete(Boolean.TRUE);
        }
    }
}
