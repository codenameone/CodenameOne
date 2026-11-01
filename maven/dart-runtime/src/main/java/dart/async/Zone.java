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
    public static final Zone root = new Zone();

    /// {@code Zone.current}, read as a static field by transpiled code.
    public static final Zone current = root;

    private Zone() {
    }

    public static Zone current() {
        return root;
    }

    /// The root zone has no parent.
    public Zone parent() {
        return null;
    }

    public Object $index(Object key) {
        return null;
    }

    /// {@code bindCallback}: the root zone runs a callback as it is.
    public <R> dart.runtime.Funcs.Func0<R> bindCallback(dart.runtime.Funcs.Func0<R> callback) {
        return callback;
    }

    public <R, A> dart.runtime.Funcs.Func1<A, R> bindUnaryCallback(dart.runtime.Funcs.Func1<A, R> callback) {
        return callback;
    }

    public <R, A, B> dart.runtime.Funcs.Func2<A, B, R> bindBinaryCallback(dart.runtime.Funcs.Func2<A, B, R> callback) {
        return callback;
    }

    /// {@code run}: the root zone calls the body directly.
    public <R> R run(dart.runtime.Funcs.Func0<R> body) {
        return body.call();
    }
}
