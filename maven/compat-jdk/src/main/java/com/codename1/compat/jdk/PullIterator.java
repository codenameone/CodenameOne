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

import java.util.Iterator;
import java.util.NoSuchElementException;

/// The iterator every stream stage is: it produces its next element only
/// when asked whether there is one, which is what makes a pipeline lazy --
/// `limit` stops asking the stage before it, and nothing further upstream
/// runs.
abstract class PullIterator<T> implements Iterator<T> {

    private boolean ready;
    private boolean done;
    private T value;

    /// Produces the next element by calling [#emit], or answers false when
    /// there is none. Not called again once it answered false.
    abstract boolean advance();

    final void emit(T next) {
        value = next;
    }

    @Override
    public final boolean hasNext() {
        if (!ready && !done) {
            if (advance()) {
                ready = true;
            } else {
                done = true;
            }
        }
        return ready;
    }

    @Override
    public final T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        ready = false;
        T out = value;
        value = null;
        return out;
    }

    /// The device's `Iterator` declares `remove` without a default.
    @Override
    public final void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
