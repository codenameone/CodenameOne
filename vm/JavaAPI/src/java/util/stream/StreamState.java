/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package java.util.stream;

import java.util.ArrayList;
import java.util.List;

/// What the stages of one pipeline share: the handlers `onClose` registered,
/// which `close()` on any stage runs, once.
final class StreamState {

    private List<Runnable> handlers;

    /// What `parallel()` and `sequential()` last asked for, which
    /// `isParallel()` reports. The pipeline runs sequentially either way,
    /// which the JDK allows a parallel stream to do.
    boolean parallel;
    private boolean closed;

    /// What closes a resource a stage holds while it is part read: the
    /// mapped stream a `flatMap` is in the middle of. Kept in the order the
    /// stages were added, which is the order the elements travel.
    ///
    /// The JDK closes a mapped stream whether or not all of it was wanted
    /// (`flatMap(...).findFirst()` closes the one it took its answer from).
    /// A pulled pipeline has to be told that nobody will ask again, and
    /// three things know: a `limit` that has handed out its last element
    /// and a `takeWhile` that met its first refusal, about the stages
    /// before them, and a short-circuiting terminal operation or `close`,
    /// about every stage.
    ///
    /// A terminal operation that ends by throwing releases them all too
    /// ([#fail(Throwable)]). Not covered, and left to `close` as for any
    /// stream holding a resource: an `iterator()` its caller stops reading.
    private List<Runnable> abandoned;

    void onAbandon(Runnable release) {
        if (abandoned == null) {
            abandoned = new ArrayList<Runnable>();
        }
        abandoned.add(release);
    }

    /// How many stages so far hold something to release: what a stage keeps
    /// to say later that everything before it is no longer read.
    int abandonable() {
        return abandoned == null ? 0 : abandoned.size();
    }

    /// Releases what the first `count` such stages hold, the latest stage
    /// first as the JDK's nested `flatMap`s close, even when one throws; the
    /// first exception is thrown once all have run.
    void abandon(int count) {
        List<Runnable> held = abandoned;
        if (held == null) {
            return;
        }
        RuntimeException first = null;
        for (int i = Math.min(count, held.size()) - 1; i >= 0; i--) {
            Runnable release = held.get(i);
            try {
                release.run();
            } catch (RuntimeException e) {
                if (first == null) {
                    first = e;
                }
            }
        }
        if (first != null) {
            throw first;
        }
    }

    void onClose(Runnable handler) {
        if (handler == null) {
            throw new NullPointerException();
        }
        if (handlers == null) {
            handlers = new ArrayList<Runnable>();
        }
        handlers.add(handler);
    }

    /// Whether some stage of the pipeline was closed. The stages share this
    /// state, so each of them can tell.
    boolean isClosed() {
        return closed;
    }

    /// Runs every handler, in the order they were added, even when one
    /// throws, an Error included; the first failure is thrown once all have
    /// run, with each later one added to it as suppressed, as the JDK's
    /// stream does.
    void close() {
        if (closed) {
            return;
        }
        closed = true;
        Throwable first = null;
        try {
            abandon(abandonable());
        } catch (Throwable e) {
            first = e;
        }
        if (handlers != null) {
            for (int i = 0; i < handlers.size(); i++) {
                // Read outside the try: the cast a generic get compiles to must
                // not sit under a handler that would swallow its failure.
                Runnable handler = handlers.get(i);
                try {
                    handler.run();
                } catch (Throwable e) {
                    first = suppress(first, e);
                }
            }
        }
        if (first != null) {
            // Thrown as it is, a sneakily thrown checked exception included,
            // which the JDK does not wrap either.
            StreamState.<RuntimeException>rethrow(first);
        }
    }

    /// What a terminal operation that ended by throwing `failure` calls:
    /// every mapped stream still open is closed, the latest first, and what
    /// closing one throws rides along as suppressed, as the JDK's
    /// try-with-resources around each mapped stream does. Always throws
    /// `failure`; declared to answer an exception so a caller can write
    /// `throw state.fail(e)`.
    RuntimeException fail(Throwable failure) {
        List<Runnable> held = abandoned;
        if (held != null) {
            for (int i = held.size() - 1; i >= 0; i--) {
                Runnable release = held.get(i);
                try {
                    release.run();
                } catch (Throwable e) {
                    suppress(failure, e);
                }
            }
        }
        StreamState.<RuntimeException>rethrow(failure);
        return null;
    }

    /// Throws `t` unchanged, checked or not: the erased cast is to
    /// `Throwable`, so nothing is checked at run time.
    @SuppressWarnings("unchecked")
    /// Throws `failure` as it is, checked or not.
    static RuntimeException unchecked(Throwable failure) {
        StreamState.<RuntimeException>rethrow(failure);
        return null;
    }

    private static <E extends Throwable> void rethrow(Throwable t) throws E {
        throw (E) t;
    }

    private static Throwable suppress(Throwable first, Throwable next) {
        if (first == null) {
            return next;
        }
        if (first != next) {
            first.addSuppressed(next);
        }
        return first;
    }
}
