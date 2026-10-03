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
 * Holder for a Dart {@code late double} local, unboxed.
 */
public final class LateDouble {
    private final String name;
    private final boolean isFinal;
    private Funcs.Func0<Double> init;
    private boolean set;
    private double v;

    /** {@code init} is null when the declaration has no initializer. */
    public LateDouble(String name, boolean isFinal, Funcs.Func0<Double> init) {
        this.name = name;
        this.isFinal = isFinal;
        this.init = init;
    }

    public double get$v() {
        if (!set) {
            Funcs.Func0<Double> f = init;
            if (f == null) {
                throw LateInitializationError.localNotInitialized(name);
            }
            // Kept until it completes: an initializer that throws runs again on the
            // next read, as Dart's does.
            double r = f.call();
            if (set && isFinal) {
                throw LateInitializationError.localAlreadyInitialized(name);
            }
            v = r;
            set = true;
            init = null;
        }
        return v;
    }

    public void set$v(double value) {
        if (isFinal && set) {
            throw LateInitializationError.localAlreadyInitialized(name);
        }
        v = value;
        set = true;
        init = null;
    }
}
