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
 * The lowering target of Dart's {@code await} (M3 blocking model).
 *
 * <p>On the EDT the wait runs inside {@code Display.invokeAndBlock}, which
 * keeps dispatching events — other Dart callbacks, timers and UI input keep
 * running while this frame is parked, matching Dart's "other events run
 * during an await" semantics. Off the EDT (background threads, headless
 * tests) it is a plain monitor wait.</p>
 */
public final class Await {

    private Await() {
    }

    /**
     * {@code await v} where v is a FutureOr or dynamic: a Future is awaited, anything
     * else -- {@code await 42}, {@code await null} -- is the value itself, as in Dart.
     * Only the Future overload existed, so awaiting a non-Future did not compile.
     */
    @SuppressWarnings("unchecked")
    public static Object awaitAny(Object v) {
        if (v instanceof Future) {
            return await$((Future<Object>) v);
        }
        // `await 42` yields too: inside a coroutine it suspends like any await.
        return Coroutine.current() != null ? await$(Future.value(v)) : v;
    }

    /// An async function's body: {@code return Await.async(() -> { body })}. The body
    /// runs now, up to its first await; the Future it returns is answered when it
    /// finishes there, and otherwise one that completes with what the rest of it returns.
    /// The lowered body already turns its throws into Future.error.
    @SuppressWarnings("unchecked")
    public static <F> F async(dart.runtime.Funcs.Func0<F> body) {
        final Coroutine c = Coroutine.start(body);
        if (c.isFinished()) {
            return finish(c);
        }
        final Future<Object> pending = new Future<Object>();
        c.onFinish(new Runnable() {
            @Override
            public void run() {
                Object r;
                try {
                    r = finish(c);
                } catch (RuntimeException e) {
                    pending.completeError(e);
                    return;
                }
                pending.complete(r);
            }
        });
        return (F) pending;
    }

    /// The body of an async function or closure whose Future nobody can see
    /// ({@code void f() async}, {@code onPressed: () async {...}}): it runs now up to its
    /// first await, and an error it throws -- before or after that await -- is an
    /// uncaught error of the zone it ran in, never an exception at the call.
    public static void asyncVoid(final dart.runtime.Funcs.VoidFunc0 body) {
        final Coroutine c = Coroutine.start(new dart.runtime.Funcs.Func0<Object>() {
            @Override
            public Object call() {
                body.call();
                return null;
            }
        });
        if (c.isFinished()) {
            reportVoid(c);
            return;
        }
        c.onFinish(new Runnable() {
            @Override
            public void run() {
                reportVoid(c);
            }
        });
    }

    private static void reportVoid(Coroutine c) {
        Throwable t = c.failure();
        if (t != null) {
            if (!(t instanceof RuntimeException)) {
                throwUnchecked(t);
            }
            // Reported from the event loop, as Dart reports the unhandled error of the
            // Future the closure returned: after the code that called it.
            final Zone z = c.zone();
            final Throwable err = t;
            new Timer(null, new dart.runtime.Funcs.VoidFunc0() {
                @Override
                public void call() {
                    z.handleUncaughtError(dart.runtime.DartRuntime.caught(err), Zone.stackOf(err));
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static <F> F finish(Coroutine c) {
        Throwable t = c.failure();
        if (t != null) {
            throwUnchecked(t);
        }
        return (F) c.result();
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }

    /// {@code await} of a void expression, already evaluated: only the yield remains.
    public static void yield$() {
        if (Coroutine.current() != null) {
            await$(Future.value(null));
        }
    }

    /// {@code await v} of a statically non-Future v: v, once the body has yielded.
    public static <T> T pass(T v) {
        if (Coroutine.current() != null) {
            await$(Future.value(null));
        }
        return v;
    }

    public static <T> T await$(final Future<T> f) {
        if (f == null) {
            throw new dart.core.TypeError("await on null Future");
        }
        f.markHandled();
        Coroutine co = Coroutine.current();
        if (co != null) {
            // Inside an async body started by async(): suspend, so the caller gets its
            // Future now and this frame resumes from the event loop (see Coroutine).
            f.driveOnce();
            co.suspendUntil(f);
            return f.valueOrThrow();
        }
        // An await ends Dart's synchronous turn: what a controller queued for delivery
        // runs now, before the frame blocks (see Microtasks).
        Microtasks.drain();
        f.driveOnce();
        if (f.isDone()) {
            return f.valueOrThrow();
        }
        if (com.codename1.ui.Display.isInitialized()
                && com.codename1.ui.Display.getInstance().isEdt()) {
            com.codename1.ui.Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    parkUntilDone(f);
                }
            });
        } else {
            parkUntilDone(f);
        }
        return f.valueOrThrow();
    }

    private static void parkUntilDone(Future<?> f) {
        if (com.codename1.ui.Display.isInitialized()) {
            // Off the EDT of a live Display (or inside invokeAndBlock) the EDT runs the
            // loop; this frame only waits for the result.
            while (!f.isDone()) {
                synchronized (f.monitor()) {
                    if (!f.isDone()) {
                        try {
                            f.monitor().wait(20);
                        } catch (InterruptedException ignore) {
                            // spurious wakeup handling via the loop condition
                        }
                    }
                }
            }
            return;
        }
        // Headless, a parked frame is where Dart's event loop runs: microtasks and fired
        // timers run here (see Microtasks).
        Microtasks.parkUntil(f);
    }
}
