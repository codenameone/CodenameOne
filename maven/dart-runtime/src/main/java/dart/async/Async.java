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

import dart.runtime.Funcs;

/// dart:async's top-level functions: unawaited, scheduleMicrotask, runZoned.
public final class Async {
    private Async() {
    }

    /// {@code unawaited(future)}: nothing to do; the future runs either way.
    public static void unawaited(Object future) {
    }

    /// {@code scheduleMicrotask(callback)}. This runtime has no microtask queue (see
    /// Future), so the callback runs from a zero-delay timer: after the current
    /// synchronous code, as in Dart, but possibly after other ready futures.
    public static void scheduleMicrotask(Funcs.VoidFunc0 callback) {
        new Timer(null, callback);
    }

    /// {@code runZoned(body)}. There is a single (root) zone, so this just runs the body;
    /// zoneValues and zoneSpecification are not modelled.
    public static <R> R runZoned(Funcs.Func0<R> body) {
        return body.call();
    }

    public static <R> R runZoned(Funcs.Func0<R> body, Object zoneValues) {
        return body.call();
    }

    public static <R> R runZoned(Funcs.Func0<R> body, Object zoneValues, Object zoneSpecification) {
        return body.call();
    }
}
