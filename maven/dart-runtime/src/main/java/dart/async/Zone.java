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
    public static final Zone root = new Zone(null, null);

    /// The zone the running code was entered in. A Dart program runs on one isolate and
    /// this runtime on one thread, so runZoned swaps it around its body. Callbacks that
    /// run later (timers, futures) see the zone that is current when they run, not the
    /// one they were registered in: bindCallback is the way to carry a zone forward.
    public static Zone current = root;

    private final Zone parent;
    private final java.util.Map<Object, Object> values;

    private Zone(Zone parent, java.util.Map<Object, Object> values) {
        this.parent = parent;
        this.values = values;
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

    /// A child of this zone holding zoneValues (a Dart Map, a java.util.Map, or null).
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
        return new Zone(this, copy);
    }

    public <R> dart.runtime.Funcs.Func0<R> bindCallback(final dart.runtime.Funcs.Func0<R> callback) {
        final Zone zone = this;
        return new dart.runtime.Funcs.Func0<R>() {
            @Override
            public R call() {
                return zone.run(callback);
            }
        };
    }

    public <R, A> dart.runtime.Funcs.Func1<A, R> bindUnaryCallback(dart.runtime.Funcs.Func1<A, R> callback) {
        return callback;
    }

    public <R, A, B> dart.runtime.Funcs.Func2<A, B, R> bindBinaryCallback(dart.runtime.Funcs.Func2<A, B, R> callback) {
        return callback;
    }

    /// Runs body with this zone current, restoring the previous zone afterwards.
    public <R> R run(dart.runtime.Funcs.Func0<R> body) {
        Zone previous = current;
        current = this;
        try {
            return body.call();
        } finally {
            current = previous;
        }
    }
}
