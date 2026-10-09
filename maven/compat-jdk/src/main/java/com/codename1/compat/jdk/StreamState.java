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
        if (handlers == null) {
            return;
        }
        RuntimeException first = null;
        for (int i = 0; i < handlers.size(); i++) {
            try {
                handlers.get(i).run();
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
