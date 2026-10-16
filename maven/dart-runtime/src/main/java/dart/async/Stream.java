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

import java.util.ArrayList;
import java.util.List;

/**
 * Dart's {@code Stream<T>} -- the minimal subset needed to deliver platform
 * events ({@code EventChannel.receiveBroadcastStream}) and to feed a
 * {@link StreamController}.
 *
 * <p>{@link #listen}'s {@code onData} is concretely typed
 * {@link Funcs.VoidFunc1}, matching real Dart's own {@code void
 * onData(T event)?}: outside a handful of names the emitter special-cases
 * by name ({@code Future.then/catchError/whenComplete}), a Dart closure
 * literal is emitted as a plain, untyped Java lambda with no wrapping, so
 * whatever parameter it is passed to MUST be a genuine functional
 * interface -- a parameter typed plain {@code Object} is not one, and the
 * generated call fails to compile. {@link #map} and {@link #where} are
 * concretely typed the same way ({@link Funcs.Func1}), and so is
 * {@code onDone} ({@link Funcs.VoidFunc0}, real Dart's own
 * {@code void onDone()?}).
 *
 * <p>{@code onError} stays {@code Object}: real Dart types it as the
 * untyped {@code Function?} and accepts either a one- or a two-argument
 * handler ({@code void onError(e)} or {@code void onError(e, StackTrace
 * st)}), and one concrete Java parameter type cannot express both -- nor
 * can {@link #listen} be overloaded on it the way {@code then}/
 * {@code catchError} are overloaded on a lambda's return shape: the stub
 * convention pads every omitted trailing named parameter with a bare
 * {@code null} (see {@link #listen(Funcs.VoidFunc1, Object, Funcs.VoidFunc0,
 * Boolean)}'s own comment), and a {@code null} given where {@code onError}
 * was not supplied -- the common case -- would be ambiguous between a
 * one- and a two-argument overload equally. The emitter instead pins a
 * transpiled {@code onError:} closure literal to
 * {@link Funcs.VoidFunc1}{@code <Object>} or
 * {@link Funcs.VoidFunc2}{@code <Object, Object>} by the closure's OWN
 * arity before the call (mirroring {@code Future.then}'s {@code onError:}
 * handling) -- never a blind cast, per this runtime's no-ClassCastException
 * rule. A hand-written Java caller may still pass any of
 * {@link Funcs.VoidFunc1}/{@link Funcs.Func1}/{@link Funcs.VoidFunc2}/
 * {@link Funcs.Func2} for {@code onError} directly (it stays {@code Object});
 * {@link #deliverError} dispatches across all of them with
 * {@code instanceof}, never a cast. {@code onDone} itself is concretely
 * {@link Funcs.VoidFunc0} on {@link #listen}, but {@link StreamSubscription}'s
 * own {@code onDone(Object)} setter can still replace it with a
 * value-returning {@link Funcs.Func0}, which {@link #deliverVoid} also
 * dispatches by {@code instanceof}.</p>
 *
 * <p><b>Not implemented</b> (kept out deliberately, not by oversight): a
 * single-subscription stream that buffers events before anyone listens --
 * every stream here behaves as broadcast, so a {@code listen()} with no
 * prior listener misses nothing only because {@link #onFirstListener()}
 * runs synchronously before this call returns; {@code sync: true}
 * zero-latency delivery semantics (events always reach listeners
 * immediately and synchronously here, which is closer to {@code sync:
 * true} than to buffered async dispatch, but is not distinguished from
 * it); {@code Stream.fromIterable}/{@code periodic}/{@code generators};
 * {@code asyncMap}/{@code asyncExpand}/{@code transform}/{@code distinct}/
 * {@code take}/{@code skip}/{@code timeout}/{@code expand}; and multiple
 * independent "listen sessions" are not distinguished from multiple
 * listeners on one session -- {@link #listen} always adds to the same
 * listener set. A {@link #listen} on a stream that already emitted "done"
 * does start a new listen session -- it is handed an already-cancelled
 * subscription and completed with {@code onDone} straight away, matching
 * real Dart's own "listen after done" behaviour, rather than reopening a
 * closed source (an {@code EventChannel}'s platform handler, for one).</p>
 */
public class Stream<T> {

    private final List<Sub> subs = new ArrayList<Sub>();
    private boolean closed;

    /** One event buffered by a paused subscription, delivered in order on resume(). */
    private static final class QueuedEvent {
        static final int DATA = 0;
        static final int ERROR = 1;
        static final int DONE = 2;

        final int kind;
        final Object value;

        QueuedEvent(int kind, Object value) {
            this.kind = kind;
            this.value = value;
        }
    }

    /** Holds one subscriber's handlers and paused state. */
    private static final class Sub {
        Object onData;
        Object onError;
        Object onDone;
        boolean cancelOnError;
        /// How many unmatched pause() calls are outstanding. Dart requires as many
        /// resume() calls as pause() calls before delivery resumes -- a second
        /// pause() while already paused is not a no-op, and the FIRST resume() must
        /// not drain anything while this is still above zero.
        int pauseCount;
        boolean cancelled;
        /// Events raised while paused (`pauseCount > 0`), delivered in order by
        /// `#drain` once `pauseCount` reaches zero again; `null` until the first one
        /// is queued. `#cancelSub` discards it, matching Dart's own
        /// pause()/cancel() interaction.
        List<QueuedEvent> pending;
        /// Set by `#emitDone` when this subscription was paused and its done
        /// event only queued, not yet delivered; cleared once `#drain` or
        /// `#cancelSub` resolves it. `null` the rest of the time.
        PendingClose pendingClose;
    }

    /**
     * What {@link #emitDone} hands back to {@link StreamController#close}: a
     * shared completer plus how many paused subscriptions still owe it a
     * resolution (drained or cancelled) before it can complete. Measured on
     * the Dart 3.9 VM: {@code StreamController.close()}'s returned future
     * does not complete while a paused listener still has the done event
     * queued -- it waits for every listener to either receive done (via
     * {@link Handle#resume}'s drain) or cancel.
     */
    private static final class PendingClose {
        final Completer<Object> completer = new Completer<Object>();
        int remaining;
        /// Run when the last listener has had its done event (or discarded it).
        Runnable whenAllDone;

        void resolve() {
            remaining--;
            if (remaining <= 0) {
                completer.complete();
                if (whenAllDone != null) {
                    Runnable r = whenAllDone;
                    whenAllDone = null;
                    r.run();
                }
            }
        }
    }

    /**
     * Runs once, synchronously, when the first subscriber attaches (0 -> 1
     * live subscriptions). The default does nothing; a stream backing an
     * event channel overrides this to call the platform
     * {@code StreamHandler.onListen}.
     */
    protected void onFirstListener() {
    }

    /**
     * Runs once, synchronously, when the last subscription cancels (1 -> 0
     * live subscriptions). The default does nothing; a stream backing an
     * event channel overrides this to call the platform
     * {@code StreamHandler.onCancel}.
     */
    protected void onNoListeners() {
    }

    public final StreamSubscription<T> listen(Funcs.VoidFunc1<T> onData) {
        return listen(onData, null, null, null);
    }

    public final StreamSubscription<T> listen(Funcs.VoidFunc1<T> onData, Object onError) {
        return listen(onData, onError, null, null);
    }

    public final StreamSubscription<T> listen(Funcs.VoidFunc1<T> onData, Object onError, Funcs.VoidFunc0 onDone) {
        return listen(onData, onError, onDone, null);
    }

    /**
     * Dart's {@code listen(onData, {onError, onDone, cancelOnError})},
     * flattened to its canonical positional order (the stub convention for
     * named parameters) -- which pads every TRAILING parameter the call
     * omits with {@code null}, all the way out to {@code cancelOnError}, so
     * this is the one overload every transpiled call actually reaches;
     * the three above exist for hand-written Java callers.
     * {@code cancelOnError} cancels the subscription right after its
     * {@code onError} handler runs for the first error.
     *
     * <p>{@code onError} stays {@code Object}, unlike {@code onData} and
     * {@code onDone} -- see the class comment for why. {@code null} (the
     * common case, since most listeners give no {@code onError} at all)
     * and any of {@link Funcs.VoidFunc1}/{@link Funcs.Func1}/
     * {@link Funcs.VoidFunc2}/{@link Funcs.Func2} pass through as is; the
     * emitter pins a transpiled closure literal to the concrete type its
     * own arity needs before it ever reaches here (see
     * {@code JavaEmitter}'s {@code onError} handling for {@code Stream.listen},
     * mirroring {@code Future.then}'s).</p>
     */
    public final StreamSubscription<T> listen(Funcs.VoidFunc1<T> onData, Object onError, Funcs.VoidFunc0 onDone,
            Boolean cancelOnError) {
        Sub sub = new Sub();
        sub.onData = onData;
        sub.onError = onError;
        sub.onDone = onDone;
        sub.cancelOnError = cancelOnError != null && cancelOnError.booleanValue();
        if (closed) {
            // The stream already finished (emitDone() already ran, from
            // StreamController#close or an EventSink's endOfStream). subs
            // is empty by now (emitDone() clears it), so treating this
            // like a normal first listener would call onFirstListener()
            // again -- for an EventChannel-backed stream, re-installing
            // the platform handler that the close already tore down.
            // Real Dart still hands back a subscription and completes it
            // with onDone; it never restarts listening. Deliver onDone
            // the same way #emitDone delivers it to everyone else
            // (synchronously -- this runtime has no microtask queue, see
            // Future#onComplete) and leave the sub out of `subs` so no
            // listen session starts.
            sub.cancelled = true;
            deliverVoid(sub.onDone);
            return new Handle(sub);
        }
        boolean first = subs.isEmpty();
        subs.add(sub);
        if (first) {
            onFirstListener();
        }
        return new Handle(sub);
    }

    /** True while at least one live (not cancelled) subscription exists. */
    protected final boolean hasListeners() {
        return !subs.isEmpty();
    }

    /**
     * Delivers a data event to every live subscriber; one that is paused has
     * it queued instead, for {@link #drain} to deliver, in order, on resume().
     */
    @SuppressWarnings("unchecked")
    protected final void emit(T value) {
        for (Sub s : new ArrayList<Sub>(subs)) {
            if (s.cancelled) {
                continue;
            }
            if (s.pauseCount > 0) {
                queue(s, QueuedEvent.DATA, value);
            } else {
                deliverData(s.onData, value);
            }
        }
    }

    /**
     * Delivers an error to every live subscriber (queued instead while
     * paused, like {@link #emit}); a subscriber whose {@code cancelOnError}
     * was set is then cancelled -- at delivery time, so a queued error only
     * cancels it once {@link #drain} actually delivers that error on resume().
     */
    protected final void emitError(Object error) {
        for (Sub s : new ArrayList<Sub>(subs)) {
            if (s.cancelled) {
                continue;
            }
            if (s.pauseCount > 0) {
                queue(s, QueuedEvent.ERROR, error);
                continue;
            }
            deliverError(s.onError, error);
            if (s.cancelOnError) {
                cancelSub(s);
            }
        }
    }

    /**
     * Delivers "done" to every live subscriber (queued instead while paused,
     * like {@link #emit}), then clears the listener set. The stream is done
     * either way -- a paused subscriber's queued done is still the last thing
     * {@link #drain} ever delivers to it.
     *
     * <p>Returns a future that completes once every subscriber has actually
     * seen the done event: immediately for one that was not paused (already
     * delivered above, synchronously), and only once {@link Handle#resume}
     * drains it -- or {@link Handle#cancel} discards it -- for one that was.
     * {@link StreamController#close} hands this straight back; a stream that
     * has no listeners, or none paused, still completes it synchronously,
     * matching the old {@code Future.value(null)}.</p>
     */
    protected final Future<Object> emitDone() {
        closed = true;
        List<Sub> snapshot = new ArrayList<Sub>(subs);
        subs.clear();
        PendingClose pendingClose = new PendingClose();
        boolean hadListeners = false;
        for (Sub s : snapshot) {
            if (s.cancelled) {
                continue;
            }
            hadListeners = true;
            if (s.pauseCount > 0) {
                queue(s, QueuedEvent.DONE, null);
                s.pendingClose = pendingClose;
                pendingClose.remaining++;
            } else {
                deliverVoid(s.onDone);
            }
        }
        // Dart counts delivering the final done event as cancelling the subscription, so
        // the source's cancel hook (StreamController.onCancel, an event channel's
        // StreamHandler.onCancel) runs once the last listener has its done -- now, or when
        // a paused one drains or discards it. It used to be skipped whenever the PRODUCER
        // ended the stream, leaving a platform event source running.
        Runnable cancelHook = hadListeners ? new Runnable() {
            @Override
            public void run() {
                onNoListeners();
            }
        } : null;
        if (pendingClose.remaining == 0) {
            pendingClose.completer.complete();
            if (cancelHook != null) {
                cancelHook.run();
            }
        } else {
            pendingClose.whenAllDone = cancelHook;
        }
        return pendingClose.completer.future();
    }

    private void cancelSub(Sub s) {
        if (s.cancelled) {
            return;
        }
        s.cancelled = true;
        s.pending = null;
        if (s.pendingClose != null) {
            s.pendingClose.resolve();
            s.pendingClose = null;
        }
        subs.remove(s);
        if (subs.isEmpty() && !closed) {
            onNoListeners();
        }
    }

    private static void queue(Sub s, int kind, Object value) {
        if (s.pending == null) {
            s.pending = new ArrayList<QueuedEvent>();
        }
        s.pending.add(new QueuedEvent(kind, value));
    }

    /**
     * Delivers whatever {@link #emit}/{@link #emitError}/{@link #emitDone}
     * queued for {@code s} while it was paused, in the order it arrived.
     * Called by {@link Handle#resume}. Stops early if a handler run from
     * inside this loop re-pauses or cancels the subscription (cancelling
     * already discards the rest of the queue, via {@link #cancelSub}); the
     * remaining queued events simply stay queued for a later resume().
     */
    private void drain(Sub s) {
        while (!s.cancelled && s.pauseCount == 0 && s.pending != null && !s.pending.isEmpty()) {
            QueuedEvent e = s.pending.remove(0);
            switch (e.kind) {
                case QueuedEvent.DATA:
                    deliverData(s.onData, e.value);
                    break;
                case QueuedEvent.ERROR:
                    deliverError(s.onError, e.value);
                    if (s.cancelOnError) {
                        cancelSub(s);
                    }
                    break;
                case QueuedEvent.DONE:
                    deliverVoid(s.onDone);
                    s.cancelled = true;
                    if (s.pendingClose != null) {
                        s.pendingClose.resolve();
                        s.pendingClose = null;
                    }
                    break;
                default:
                    break;
            }
        }
        if (s.pending != null && s.pending.isEmpty()) {
            s.pending = null;
        }
    }

    /**
     * Dart's {@code Stream<T>.map<R>}: a derived stream that applies
     * {@code transform} to every event. The upstream subscription is only
     * live while the derived stream itself has listeners.
     *
     * <p>Concretely typed ({@link Funcs.Func1}, not {@code Object}) for the
     * same reason {@link #listen}'s {@code onData} is -- see the class
     * comment.</p>
     */
    public final <R> Stream<R> map(final Funcs.Func1<T, R> transform) {
        final Stream<T> upstream = this;
        return new Stream<R>() {
            private StreamSubscription<T> upstreamSub;

            @Override
            protected void onFirstListener() {
                final Stream<R> self = this;
                upstreamSub = upstream.listen(new Funcs.VoidFunc1<T>() {
                    @Override
                    public void call(T v) {
                        R mapped;
                        try {
                            mapped = transform.call(v);
                        } catch (Throwable t) {
                            self.emitError(t);
                            return;
                        }
                        self.emit(mapped);
                    }
                }, new Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object e) {
                        self.emitError(e);
                    }
                }, new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        self.emitDone();
                    }
                });
                // The upstream may emit synchronously inside listen(), and a cancelOnError
                // listener can then leave this stream with no listeners -- onNoListeners ran
                // before upstreamSub was assigned, so there was nothing to cancel and the
                // source (a controller, an EventChannel) never got its cancel hook.
                if (!hasListeners() && upstreamSub != null) {
                    upstreamSub.cancel();
                    upstreamSub = null;
                }
            }

            @Override
            protected void onNoListeners() {
                if (upstreamSub != null) {
                    upstreamSub.cancel();
                    upstreamSub = null;
                }
            }
        };
    }

    /**
     * Dart's {@code Stream<T>.where}: a derived stream of events {@code test} accepts.
     *
     * <p>Concretely typed, like {@link #map} -- see the class comment.</p>
     */
    public final Stream<T> where(final Funcs.Func1<T, Boolean> test) {
        final Stream<T> upstream = this;
        return new Stream<T>() {
            private StreamSubscription<T> upstreamSub;

            @Override
            protected void onFirstListener() {
                final Stream<T> self = this;
                upstreamSub = upstream.listen(new Funcs.VoidFunc1<T>() {
                    @Override
                    public void call(T v) {
                        boolean keep;
                        try {
                            keep = Boolean.TRUE.equals(test.call(v));
                        } catch (Throwable t) {
                            self.emitError(t);
                            return;
                        }
                        if (keep) {
                            self.emit(v);
                        }
                    }
                }, new Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object e) {
                        self.emitError(e);
                    }
                }, new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        self.emitDone();
                    }
                });
                // The upstream may emit synchronously inside listen(), and a cancelOnError
                // listener can then leave this stream with no listeners -- onNoListeners ran
                // before upstreamSub was assigned, so there was nothing to cancel and the
                // source (a controller, an EventChannel) never got its cancel hook.
                if (!hasListeners() && upstreamSub != null) {
                    upstreamSub.cancel();
                    upstreamSub = null;
                }
            }

            @Override
            protected void onNoListeners() {
                if (upstreamSub != null) {
                    upstreamSub.cancel();
                    upstreamSub = null;
                }
            }
        };
    }

    // ------------------------------------------------------------------
    // Callback dispatch -- instanceof, never a blind cast (see class comment).
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    static void deliverData(Object onData, Object value) {
        try {
            if (onData instanceof Funcs.VoidFunc1) {
                ((Funcs.VoidFunc1<Object>) onData).call(value);
            } else if (onData instanceof Funcs.Func1) {
                ((Funcs.Func1<Object, Object>) onData).call(value);
            }
        } catch (Throwable t) {
            reportUncaught(t);
        }
    }

    /**
     * Invokes an error handler of any shape a Dart {@code onError} closure
     * transpiles to: {@code (e)}, {@code (e, stackTrace)}, with or without a
     * return value.
     */
    @SuppressWarnings("unchecked")
    static void deliverError(Object onError, Object error) {
        Object err = error instanceof Throwable
                ? dart.runtime.DartRuntime.caught((Throwable) error) : error;
        try {
            if (onError instanceof Funcs.VoidFunc1) {
                ((Funcs.VoidFunc1<Object>) onError).call(err);
            } else if (onError instanceof Funcs.Func1) {
                ((Funcs.Func1<Object, Object>) onError).call(err);
            } else if (onError instanceof Funcs.VoidFunc2) {
                ((Funcs.VoidFunc2<Object, Object>) onError).call(err, null);
            } else if (onError instanceof Funcs.Func2) {
                ((Funcs.Func2<Object, Object, Object>) onError).call(err, null);
            } else if (onError == null) {
                // No handler installed: Dart's default behaviour is to forward the
                // error to the subscription's zone, which for a headless runtime
                // has nowhere else to go but the log.
                reportUncaught(err instanceof Throwable ? (Throwable) err : new RuntimeException(String.valueOf(err)));
            }
        } catch (Throwable t) {
            reportUncaught(t);
        }
    }

    static void deliverVoid(Object cb) {
        try {
            if (cb instanceof Funcs.VoidFunc0) {
                ((Funcs.VoidFunc0) cb).call();
            } else if (cb instanceof Funcs.Func0) {
                ((Funcs.Func0<?>) cb).call();
            }
        } catch (Throwable t) {
            reportUncaught(t);
        }
    }

    /**
     * Reports an exception a listener callback threw, the way Dart itself
     * does: {@code Zone.runUnaryGuarded}/{@code runGuarded} wrap every
     * {@code onData}/{@code onError}/{@code onDone} call, catch whatever it
     * throws, and hand it to the zone's uncaught-error handler instead of
     * letting it propagate out of the event that triggered it -- so a
     * throwing listener never unwinds {@code add()}/{@code emit()} and never
     * stops the remaining listeners (or a later, unrelated event) from being
     * delivered. This runtime has no zone to hand it to, so -- like
     * {@code deliverError}'s own "no handler installed" case and
     * {@link Future}'s uncaught-error path -- it goes to the log.
     *
     * <p>{@code Log.e} itself is swallowed too: it reaches down to
     * {@code CodenameOneImplementation}, which a headless unit test never
     * installs, and reporting an error must never itself throw a new one --
     * that would be exactly the failure this method exists to prevent.</p>
     */
    static void reportUncaught(Throwable t) {
        try {
            com.codename1.io.Log.e(t);
        } catch (Throwable loggingFailed) {
            // No implementation to log through (headless test); nothing more to do.
        }
    }

    /** A live subscription's handle: mutates the {@link Sub} it was created with. */
    private final class Handle implements StreamSubscription<T> {
        private final Sub sub;

        Handle(Sub sub) {
            this.sub = sub;
        }

        @Override
        public Future<Object> cancel() {
            cancelSub(sub);
            return Future.value(null);
        }

        @Override
        public void onData(Object handleData) {
            sub.onData = handleData;
        }

        @Override
        public void onError(Object handleError) {
            sub.onError = handleError;
        }

        @Override
        public void onDone(Object handleDone) {
            sub.onDone = handleDone;
        }

        @Override
        public void pause() {
            sub.pauseCount++;
        }

        @Override
        public void resume() {
            // As many resume() calls as pause() calls are required before delivery
            // actually resumes (Dart's own StreamSubscription contract); an extra,
            // unmatched resume() -- pauseCount already zero -- is a harmless no-op,
            // matching dart:async's _BufferingStreamSubscription.resume(), which only
            // acts "if (_isPaused)". Draining only once the count reaches zero is the
            // whole fix: the old boolean flag let a SECOND resume() look like the
            // first one, draining everything after only one of two pauses lifted.
            if (sub.pauseCount <= 0) {
                return;
            }
            sub.pauseCount--;
            if (sub.pauseCount == 0) {
                drain(sub);
            }
        }

        @Override
        public boolean isPaused() {
            return sub.pauseCount > 0;
        }
    }
}
