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
    private T value;
    private Throwable error;
    private List<Runnable> listeners;

    Future() {
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
        return delayed(duration, null);
    }

    public static Future<Object> delayed(dart.core.Duration duration, final Funcs.Func0<Object> computation) {
        final Future<Object> f = new Future<Object>();
        long ms = duration == null ? 0 : duration.inMilliseconds();
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
            com.codename1.ui.CN.setTimeout((int) ms, complete);
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

    /** Dart's Future.wait — completes with the values in order. */
    public static Future<DartList<Object>> wait(Iterable<? extends Future<?>> futures) {
        DartList<Object> results = new DartList<Object>();
        for (Future<?> f : futures) {
            results.add(Await.await$(f));
        }
        return Future.value(results);
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

    public Future<T> whenComplete(final Funcs.VoidFunc0 action) {
        onComplete(new Runnable() {
            @Override
            public void run() {
                action.call();
            }
        });
        return this;
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

    void complete(T v) {
        List<Runnable> toRun;
        synchronized (lock) {
            if (done) {
                throw new dart.core.StateError("Future already completed");
            }
            done = true;
            value = v;
            toRun = listeners;
            listeners = null;
            lock.notifyAll();
        }
        runAll(toRun);
    }

    void completeError(Object err) {
        List<Runnable> toRun;
        synchronized (lock) {
            if (done) {
                throw new dart.core.StateError("Future already completed");
            }
            done = true;
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
