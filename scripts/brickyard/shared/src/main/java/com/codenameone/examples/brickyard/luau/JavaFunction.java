/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.brickyard.luau;

/// A function written in Java and callable from Luau.
///
/// `call` receives the arguments and returns the results; an empty array or
/// null means no results. A function that suspends the calling coroutine
/// returns [Interp#yield(Object[])] instead.
public abstract class JavaFunction extends LuaFunction {
    private final String name;

    /// A function shown as `name` in errors.
    protected JavaFunction(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    /// Invokes the function.
    public abstract Object[] call(Interp interp, Object[] args);

    /// The argument at `i`, or nil when there are fewer arguments.
    protected static Object arg(Object[] args, int i) {
        return i < args.length ? args[i] : null;
    }
}
