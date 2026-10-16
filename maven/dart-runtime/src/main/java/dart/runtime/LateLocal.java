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
package dart.runtime;

import dart.core.LateInitializationError;

/**
 * Holder for a Dart {@code late} local of a reference or nullable type.
 *
 * <p>Dart runs a local's {@code late} initializer on the first read rather than at
 * the declaration, and a read of one that was never assigned (and has no
 * initializer) throws LateInitializationError, as does a second assignment to a
 * {@code late final}. The transpiler emits such a local as one of these holders and
 * reads/writes it through {@code get$v()} / {@code set$v(..)}, the same accessor
 * shape as a late field, so every assignment form already lowers correctly. The
 * holder is final, so a closure captures it like any boxed local.</p>
 */
public final class LateLocal<T> {
    private final String name;
    private final boolean isFinal;
    private Funcs.Func0<T> init;
    private boolean set;
    private T v;

    /** {@code init} is null when the declaration has no initializer. */
    public LateLocal(String name, boolean isFinal, Funcs.Func0<T> init) {
        this.name = name;
        this.isFinal = isFinal;
        this.init = init;
    }

    public T get$v() {
        if (!set) {
            Funcs.Func0<T> f = init;
            if (f == null) {
                throw LateInitializationError.localNotInitialized(name);
            }
            // Kept until it completes: an initializer that throws runs again on the
            // next read, as Dart's does.
            T r = f.call();
            if (set && isFinal) {
                throw LateInitializationError.localAlreadyInitialized(name);
            }
            v = r;
            set = true;
            init = null;
        }
        return v;
    }

    public void set$v(T value) {
        if (isFinal && set) {
            throw LateInitializationError.localAlreadyInitialized(name);
        }
        v = value;
        set = true;
        init = null;
    }
}
