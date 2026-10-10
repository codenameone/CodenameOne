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
package com.codename1.compat.jdk;

import java.util.ArrayList;
import java.util.List;

/// What the stages of one pipeline share: the handlers `onClose` registered,
/// which `close()` on any stage runs, once.
final class StreamState {

    private List<Runnable> handlers;
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
    /// Not covered, and left to `close` as for any stream holding a
    /// resource: a terminal operation that ends by throwing, and an
    /// `iterator()` its caller stops reading.
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

    /// Releases what the first `count` such stages hold, even when one
    /// throws; the first exception is thrown once all have run.
    void abandon(int count) {
        List<Runnable> held = abandoned;
        if (held == null) {
            return;
        }
        RuntimeException first = null;
        for (int i = 0; i < count && i < held.size(); i++) {
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

    /// Runs every handler, in the order they were added, even when one
    /// throws; the first exception is thrown once all have run.
    void close() {
        if (closed) {
            return;
        }
        closed = true;
        RuntimeException first = null;
        try {
            abandon(abandonable());
        } catch (RuntimeException e) {
            first = e;
        }
        if (handlers == null) {
            if (first != null) {
                throw first;
            }
            return;
        }
        for (int i = 0; i < handlers.size(); i++) {
            // Read outside the try: the cast a generic get compiles to must
            // not sit under a handler that would swallow its failure.
            Runnable handler = handlers.get(i);
            try {
                handler.run();
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
}
