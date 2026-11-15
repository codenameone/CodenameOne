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
package com.codename1.rbx;

import com.codename1.luau.Interp;

/// What a class does beyond holding properties: methods, computed
/// properties and reactions. Each hook answers [#PASS] (or false) to let the
/// superclass's behaviour, and then the generic one, handle it.
public abstract class Behavior {
    /// "Not handled here".
    public static final Object PASS = new Object();
    /// A method result meaning "not handled here".
    public static final Object[] PASS_CALL = new Object[0];

    /// A computed or overridden property read.
    public Object get(Instance self, Interp in, String key) {
        return PASS;
    }

    /// A computed or validated property write; true when handled.
    public boolean set(Instance self, Interp in, String key, Object v) {
        return false;
    }

    /// A method call; [#PASS_CALL] when not handled.
    public Object[] call(Instance self, String method, Interp in, Object[] a) {
        return PASS_CALL;
    }

    /// After a stored property changed.
    public void changed(Instance self, String prop) {
    }

    /// After the instance was created.
    public void created(Instance self) {
    }

    /// After the instance's ancestry changed (it was parented or removed).
    public void ancestryChanged(Instance self) {
    }
}
