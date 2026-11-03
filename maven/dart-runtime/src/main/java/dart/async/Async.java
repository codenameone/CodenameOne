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

/// dart:async's top-level functions: unawaited, scheduleMicrotask, runZoned.
public final class Async {
    private Async() {
    }

    /// {@code unawaited(future)}: nothing to do; the future runs either way.
    public static void unawaited(Object future) {
    }

    /// {@code scheduleMicrotask(callback)}: queued with controller deliveries, run once
    /// the current synchronous code ends (see Microtasks).
    public static void scheduleMicrotask(final Funcs.VoidFunc0 callback) {
        if (callback == null) {
            throw new dart.core.ArgumentError("scheduleMicrotask: callback must not be null");
        }
        // Runs in the zone that scheduled it, as Dart's does. Uncaptured, it ran in
        // whatever zone the draining frame was in: a microtask scheduled inside
        // runZoned read the root zone's values once timer callbacks stopped draining
        // from their own (zoned) threads.
        Microtasks.schedule(Zone.capture(new Runnable() {
            @Override
            public void run() {
                callback.call();
            }
        }));
    }

    /// Runs every queued microtask now, on the calling thread: what the end of main()
    /// is to Dart. FlutterRegistry calls it after main, and so do the headless runners
    /// and tests. Off the EDT of a live Display it does nothing (the EDT drains).
    /// Runs what is queued for the isolate's thread now -- microtasks and any timer that
    /// has already fired -- without waiting for pending timers, as one turn of Dart's
    /// event loop does. {@link #runMicrotasks()} instead runs until nothing is pending.
    public static void drainMicrotasks() {
        Microtasks.drain();
    }

    public static void runMicrotasks() {
        Microtasks.runUntilIdle();
    }

    /// {@code runZoned(body)}: runs the body in a child of the current zone that holds
    /// zoneValues, which Zone.current[key] then reads. A zoneSpecification's handlers are
    /// not modelled (see ZoneSpecification).
    public static <R> R runZoned(Funcs.Func0<R> body) {
        return runZoned(body, null, null);
    }

    public static <R> R runZoned(Funcs.Func0<R> body, Object zoneValues) {
        return runZoned(body, zoneValues, null);
    }

    public static <R> R runZoned(Funcs.Func0<R> body, Object zoneValues, Object zoneSpecification) {
        return Zone.current.fork(zoneSpecification, zoneValues).run(body);
    }

    /// {@code runZonedGuarded(body, onError)}: body in a child zone whose uncaught
    /// errors -- thrown synchronously, from a timer, a listener, or an unhandled
    /// future -- go to onError(error, stackTrace). Answers null when body throws.
    public static <R> R runZonedGuarded(Funcs.Func0<R> body, Funcs.VoidFunc2<Object, dart.core.StackTrace> onError) {
        return runZonedGuarded(body, onError, null, null);
    }

    public static <R> R runZonedGuarded(Funcs.Func0<R> body, Funcs.VoidFunc2<Object, dart.core.StackTrace> onError, Object zoneValues) {
        return runZonedGuarded(body, onError, zoneValues, null);
    }

    public static <R> R runZonedGuarded(Funcs.Func0<R> body, final Funcs.VoidFunc2<Object, dart.core.StackTrace> onError, Object zoneValues,
            Object zoneSpecification) {
        Funcs.VoidFunc5<Object, Object, Object, Object, Object> handler =
                new Funcs.VoidFunc5<Object, Object, Object, Object, Object>() {
            @Override
            public void call(Object self, Object parent, Object zone, Object error, Object stackTrace) {
                onError.call(error, stackTrace instanceof dart.core.StackTrace
                        ? (dart.core.StackTrace) stackTrace : dart.core.StackTrace.empty);
            }
        };
        ZoneSpecification given = zoneSpecification instanceof ZoneSpecification
                ? (ZoneSpecification) zoneSpecification : null;
        Zone zone = Zone.current.fork(ZoneSpecification.from(given, handler, null, null, null), zoneValues);
        try {
            return zone.run(body);
        } catch (Throwable t) {
            zone.handleUncaughtError(DartRuntime.caught(t), Zone.stackOf(t));
            return null;
        }
    }
}
