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

/// dart:async's {@code Zone}. Only the root zone exists here.
public final class Zone {
    public static final Zone root = new Zone(null, null, null);

    /// The zone the running code was entered in. A Dart program runs on one isolate and
    /// this runtime on one thread, so run() swaps it around its body. Callbacks that run
    /// later -- timers, future listeners, stream listeners -- capture the zone current
    /// when they were registered and run in it (see {@link #capture}), as in Dart.
    public static Zone current = root;

    private final Zone parent;
    private final java.util.Map<Object, Object> values;
    private final ZoneSpecification spec;

    private Zone(Zone parent, java.util.Map<Object, Object> values, ZoneSpecification spec) {
        this.parent = parent;
        this.values = values;
        this.spec = spec;
    }

    public static Zone current() {
        return current;
    }

    public Zone parent() {
        return parent;
    }

    /// Dart's zone[key]: this zone's value for the key, else its parent's.
    public Object $index(Object key) {
        for (Zone z = this; z != null; z = z.parent) {
            if (z.values != null && z.values.containsKey(key)) {
                return z.values.get(key);
            }
        }
        return null;
    }

    public Object idx(Object key) {
        return $index(key);
    }

    /// A child of this zone holding zoneValues (a Dart Map, a java.util.Map, or null)
    /// and the handlers of specification (a ZoneSpecification, or null).
    public Zone fork(Object specification, Object zoneValues) {
        java.util.Map<Object, Object> copy = null;
        if (zoneValues instanceof dart.core.DartMap) {
            copy = new java.util.HashMap<>();
            dart.core.DartMap<?, ?> m = (dart.core.DartMap<?, ?>) zoneValues;
            for (Object k : m.keys()) {
                copy.put(k, m.idx(k));
            }
        } else if (zoneValues instanceof java.util.Map) {
            copy = new java.util.HashMap<>((java.util.Map<?, ?>) zoneValues);
        }
        ZoneSpecification sp = specification instanceof ZoneSpecification ? (ZoneSpecification) specification : null;
        return new Zone(this, copy, sp);
    }

    public Zone fork() {
        return fork(null, null);
    }

    /// Wraps r so that it runs in the zone current now, wherever it is called from
    /// later. Every deferred-callback registration point calls this.
    static Runnable capture(final Runnable r) {
        final Zone z = current;
        if (z == root) {
            return r;
        }
        return new Runnable() {
            @Override
            public void run() {
                Zone previous = current;
                current = z;
                try {
                    r.run();
                } finally {
                    current = previous;
                }
            }
        };
    }

    /// Like capture, for a callback whose exceptions are uncaught errors of that zone
    /// (a Timer or scheduleMicrotask body): they go to its handleUncaughtError.
    static Runnable captureGuarded(final Runnable r) {
        final Zone z = current;
        return new Runnable() {
            @Override
            public void run() {
                Zone previous = current;
                current = z;
                try {
                    r.run();
                } catch (Throwable t) {
                    z.handleUncaughtError(dart.runtime.DartRuntime.caught(t), stackOf(t));
                } finally {
                    current = previous;
                }
            }
        };
    }

    /// A Dart callback value (any Funcs shape this runtime delivers events through)
    /// bound to the current zone, keeping its interface so dispatch by instanceof
    /// still recognises it. Null, and anything in the root zone, is returned as is.
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object bindAny(final Object cb) {
        final Zone z = current;
        if (cb == null || z == root) {
            return cb;
        }
        if (cb instanceof dart.runtime.Funcs.VoidFunc0) {
            return new dart.runtime.Funcs.VoidFunc0() {
                @Override
                public void call() {
                    Zone p = enter(z);
                    try {
                        ((dart.runtime.Funcs.VoidFunc0) cb).call();
                    } finally {
                        current = p;
                    }
                }
            };
        }
        if (cb instanceof dart.runtime.Funcs.VoidFunc1) {
            return new dart.runtime.Funcs.VoidFunc1() {
                @Override
                public void call(Object a) {
                    Zone p = enter(z);
                    try {
                        ((dart.runtime.Funcs.VoidFunc1) cb).call(a);
                    } finally {
                        current = p;
                    }
                }
            };
        }
        if (cb instanceof dart.runtime.Funcs.VoidFunc2) {
            return new dart.runtime.Funcs.VoidFunc2() {
                @Override
                public void call(Object a, Object b) {
                    Zone p = enter(z);
                    try {
                        ((dart.runtime.Funcs.VoidFunc2) cb).call(a, b);
                    } finally {
                        current = p;
                    }
                }
            };
        }
        if (cb instanceof dart.runtime.Funcs.Func0) {
            return new dart.runtime.Funcs.Func0() {
                @Override
                public Object call() {
                    Zone p = enter(z);
                    try {
                        return ((dart.runtime.Funcs.Func0) cb).call();
                    } finally {
                        current = p;
                    }
                }
            };
        }
        if (cb instanceof dart.runtime.Funcs.Func1) {
            return new dart.runtime.Funcs.Func1() {
                @Override
                public Object call(Object a) {
                    Zone p = enter(z);
                    try {
                        return ((dart.runtime.Funcs.Func1) cb).call(a);
                    } finally {
                        current = p;
                    }
                }
            };
        }
        if (cb instanceof dart.runtime.Funcs.Func2) {
            return new dart.runtime.Funcs.Func2() {
                @Override
                public Object call(Object a, Object b) {
                    Zone p = enter(z);
                    try {
                        return ((dart.runtime.Funcs.Func2) cb).call(a, b);
                    } finally {
                        current = p;
                    }
                }
            };
        }
        return cb;
    }

    private static Zone enter(Zone z) {
        Zone p = current;
        current = z;
        return p;
    }

    static dart.core.StackTrace stackOf(Throwable t) {
        java.io.StringWriter w = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(w));
        return new dart.core.StackTrace(w.toString());
    }

    private static Zone handlerZone(Zone from, int kind) {
        for (Zone z = from; z != null; z = z.parent) {
            ZoneSpecification sp = z.spec;
            if (sp != null) {
                Object h = kind == 0 ? sp.handleUncaughtError : kind == 1 ? sp.print
                        : kind == 2 ? sp.run : sp.registerCallback;
                if (h != null) {
                    return z;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static <R> R dispatchRun(Zone from, Zone zone, dart.runtime.Funcs.Func0<R> f) {
        Zone h = handlerZone(from, 2);
        if (h == null) {
            Zone previous = current;
            current = zone;
            try {
                return f.call();
            } finally {
                current = previous;
            }
        }
        return (R) invoke(h.spec.run, h, new ZoneDelegate(h.parent), zone, f);
    }

    @SuppressWarnings("unchecked")
    static <R> dart.runtime.Funcs.Func0<R> dispatchRegister(Zone from, Zone zone, dart.runtime.Funcs.Func0<R> f) {
        Zone h = handlerZone(from, 3);
        if (h == null) {
            return f;
        }
        return (dart.runtime.Funcs.Func0<R>) invoke(h.spec.registerCallback, h, new ZoneDelegate(h.parent), zone, f);
    }

    static void dispatchPrint(Zone from, Zone zone, String line) {
        Zone h = handlerZone(from, 1);
        if (h == null) {
            dart.runtime.DartRuntime.printRaw(line);
            return;
        }
        invoke(h.spec.print, h, new ZoneDelegate(h.parent), zone, line);
    }

    static void dispatchError(Zone from, Zone zone, Object error, Object stackTrace) {
        Zone h = handlerZone(from, 0);
        if (h == null) {
            Throwable t = error instanceof Throwable ? (Throwable) error : dart.runtime.DartRuntime.asError(error);
            try {
                com.codename1.io.Log.e(t);
            } catch (Throwable loggingFailed) {
                System.err.println("Uncaught error: " + error);
            }
            return;
        }
        // Dart runs the handler in the parent of the zone that declared it, so an error
        // the handler throws is not handed straight back to it.
        Zone previous = current;
        current = h.parent;
        try {
            invoke(h.spec.handleUncaughtError, h, new ZoneDelegate(h.parent), zone, error,
                    stackTrace != null ? stackTrace : dart.core.StackTrace.empty);
        } catch (Throwable t) {
            if (h.parent != null) {
                dispatchError(h.parent, h.parent, dart.runtime.DartRuntime.caught(t), stackOf(t));
            }
        } finally {
            current = previous;
        }
    }

    /// Calls a Dart function value of any arity this runtime binds closures to.
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object invoke(Object fn, Object... a) {
        switch (a.length) {
            case 4:
                if (fn instanceof dart.runtime.Funcs.Func4) {
                    return ((dart.runtime.Funcs.Func4) fn).call(a[0], a[1], a[2], a[3]);
                }
                if (fn instanceof dart.runtime.Funcs.VoidFunc4) {
                    ((dart.runtime.Funcs.VoidFunc4) fn).call(a[0], a[1], a[2], a[3]);
                    return null;
                }
                break;
            case 5:
                if (fn instanceof dart.runtime.Funcs.Func5) {
                    return ((dart.runtime.Funcs.Func5) fn).call(a[0], a[1], a[2], a[3], a[4]);
                }
                if (fn instanceof dart.runtime.Funcs.VoidFunc5) {
                    ((dart.runtime.Funcs.VoidFunc5) fn).call(a[0], a[1], a[2], a[3], a[4]);
                    return null;
                }
                break;
            case 2:
                if (fn instanceof dart.runtime.Funcs.Func2) {
                    return ((dart.runtime.Funcs.Func2) fn).call(a[0], a[1]);
                }
                if (fn instanceof dart.runtime.Funcs.VoidFunc2) {
                    ((dart.runtime.Funcs.VoidFunc2) fn).call(a[0], a[1]);
                    return null;
                }
                break;
            case 1:
                if (fn instanceof dart.runtime.Funcs.Func1) {
                    return ((dart.runtime.Funcs.Func1) fn).call(a[0]);
                }
                if (fn instanceof dart.runtime.Funcs.VoidFunc1) {
                    ((dart.runtime.Funcs.VoidFunc1) fn).call(a[0]);
                    return null;
                }
                break;
            default:
                break;
        }
        throw new dart.core.ArgumentError("zone handler does not take " + a.length + " arguments: " + fn);
    }

    /// Runs body with this zone current (through any run handler), restoring the
    /// previous zone afterwards.
    public <R> R run(dart.runtime.Funcs.Func0<R> body) {
        return dispatchRun(this, this, body);
    }

    public <R, A> R runUnary(final dart.runtime.Funcs.Func1<A, R> body, final A arg) {
        return run(new dart.runtime.Funcs.Func0<R>() {
            @Override
            public R call() {
                return body.call(arg);
            }
        });
    }

    /// {@code runGuarded(body)}: run, with anything thrown handed to handleUncaughtError.
    public void runGuarded(dart.runtime.Funcs.Func0<?> body) {
        try {
            run(body);
        } catch (Throwable t) {
            handleUncaughtError(dart.runtime.DartRuntime.caught(t), stackOf(t));
        }
    }

    public void handleUncaughtError(Object error, Object stackTrace) {
        dispatchError(this, this, error, stackTrace);
    }

    public void print(String line) {
        dispatchPrint(this, this, line);
    }

    public <R> dart.runtime.Funcs.Func0<R> registerCallback(dart.runtime.Funcs.Func0<R> callback) {
        return dispatchRegister(this, this, callback);
    }

    public <R> dart.runtime.Funcs.Func0<R> bindCallback(dart.runtime.Funcs.Func0<R> callback) {
        final Zone zone = this;
        final dart.runtime.Funcs.Func0<R> registered = registerCallback(callback);
        return new dart.runtime.Funcs.Func0<R>() {
            @Override
            public R call() {
                return zone.run(registered);
            }
        };
    }

    public <R, A> dart.runtime.Funcs.Func1<A, R> bindUnaryCallback(final dart.runtime.Funcs.Func1<A, R> callback) {
        final Zone zone = this;
        return new dart.runtime.Funcs.Func1<A, R>() {
            @Override
            public R call(A a) {
                return zone.runUnary(callback, a);
            }
        };
    }

    public <R, A, B> dart.runtime.Funcs.Func2<A, B, R> bindBinaryCallback(
            final dart.runtime.Funcs.Func2<A, B, R> callback) {
        final Zone zone = this;
        return new dart.runtime.Funcs.Func2<A, B, R>() {
            @Override
            public R call(final A a, final B b) {
                return zone.run(new dart.runtime.Funcs.Func0<R>() {
                    @Override
                    public R call() {
                        return callback.call(a, b);
                    }
                });
            }
        };
    }

    public boolean inSameErrorZone(Zone other) {
        return handlerZone(this, 0) == handlerZone(other, 0);
    }
}
